"""ForgeIT private JSON-lines ROS 2 bridge. stdout is reserved for protocol v1."""

import json
import os
import queue
import re
import sys
import threading
import time
import uuid
from types import SimpleNamespace

MAX_FRAME = 1024 * 1024
MAX_ENTITIES = 256
MAX_PENDING = 128
ID = re.compile(r'[A-Za-z0-9_-]{1,64}\Z')


class ProtocolError(Exception):
    def __init__(self, code):
        self.code = code


def emit(output, frame):
    encoded = json.dumps(frame, separators=(',', ':'), ensure_ascii=True, allow_nan=False) + '\n'
    if len(encoded.encode('utf-8')) > MAX_FRAME:
        raise ProtocolError('FRAME_TOO_LARGE')
    output.write(encoded)
    output.flush()


class CommandReader(threading.Thread):
    """Read bounded binary frames; failure is an out-of-band terminal signal."""

    def __init__(self, source, capacity=MAX_PENDING):
        super().__init__(daemon=True)
        self.source = source
        self.commands = queue.Queue(maxsize=capacity)
        self.failure = None
        self.done = threading.Event()

    def run(self):
        try:
            while True:
                line = self.source.readline(MAX_FRAME + 1)
                if not line:
                    return
                if len(line) > MAX_FRAME:
                    self.failure = 'FRAME_TOO_LARGE'
                    return
                if not line.endswith(b'\n'):
                    self.failure = 'INVALID_FRAME'
                    return
                try:
                    def reject_constant(value):
                        raise ValueError()
                    frame = json.loads(line.decode('utf-8'), parse_constant=reject_constant)
                    if not isinstance(frame, dict):
                        raise ValueError()
                except (ValueError, UnicodeError, RecursionError):
                    self.failure = 'INVALID_FRAME'
                    return
                try:
                    self.commands.put_nowait(frame)
                    # No more commands follow shutdown. Do not hold stdin's buffered
                    # lock in a daemon thread while Python finalizes the interpreter.
                    if frame.get('type') == 'SHUTDOWN':
                        return
                except queue.Full:
                    self.failure = 'QUEUE_OVERFLOW'
                    return
        except Exception:
            self.failure = 'INVALID_FRAME'
        finally:
            self.done.set()


class Runtime:
    def __init__(self, node, ros, output, clock=time.monotonic, timestamp_clock=lambda: time.time_ns() // 1000):
        self.node = node
        self.ros = ros
        self.output = output
        self.clock = clock
        self.timestamp_clock = timestamp_clock
        self.subscriptions = {}
        self.publishers = {}
        self.pending = []
        self.periodic = {}
        self.periodic_failures = set()
        self.stopping = False
        self.shutdown_id = None

    def ready(self, request_id):
        emit(self.output, dict(type='READY', id=request_id))

    def error(self, code, **identity):
        emit(self.output, dict(type='ERROR', code=code, **identity))

    def qos(self, value):
        try:
            if not isinstance(value, dict) or set(value) != {'reliability', 'durability', 'history', 'depth'}:
                raise ValueError()
            if value['reliability'] not in ('RELIABLE', 'BEST_EFFORT'):
                raise ValueError()
            if value['durability'] not in ('VOLATILE', 'TRANSIENT_LOCAL'):
                raise ValueError()
            if value['history'] != 'KEEP_LAST':
                raise ValueError()
            if type(value['depth']) is not int or not 0 < value['depth'] <= 2147483647:
                raise ValueError()
            return self.ros.QoSProfile(
                reliability=getattr(self.ros.ReliabilityPolicy, value['reliability']),
                durability=getattr(self.ros.DurabilityPolicy, value['durability']),
                history=self.ros.HistoryPolicy.KEEP_LAST, depth=value['depth'])
        except Exception:
            raise ProtocolError('INVALID_QOS') from None

    def identifier(self, value):
        if not isinstance(value, str) or not ID.fullmatch(value):
            raise ProtocolError('INVALID_COMMAND')
        return value

    def command(self, frame):
        request_id = '0'
        try:
            request_id = self.identifier(frame.get('id'))
            kind = frame.get('type')
            if kind == 'SHUTDOWN':
                self.shutdown_id = request_id
                self.stopping = True
                return
            if kind == 'STOP_SUBSCRIPTION':
                subscription_id = self.identifier(frame.get('subscriptionId'))
                sub = self.subscriptions.pop(subscription_id, None)
                if sub is not None:
                    self.node.destroy_subscription(sub)
                self.ready(request_id)
                return
            if kind == 'STOP_PERIODIC':
                publication_id = self.identifier(frame.get('publicationId'))
                self.periodic.pop(publication_id, None)
                if publication_id in self.periodic_failures:
                    self.periodic_failures.remove(publication_id)
                    raise ProtocolError('ROS_ERROR')
                self.ready(request_id)
                return
            if kind not in ('START_SUBSCRIPTION', 'PUBLISH', 'START_PERIODIC'):
                raise ProtocolError('INVALID_COMMAND')
            topic, type_name = frame.get('topic'), frame.get('messageType')
            if not isinstance(topic, str) or not topic or len(topic) > 1024:
                raise ProtocolError('INVALID_COMMAND')
            if not isinstance(type_name, str) or not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*/msg/[A-Za-z][A-Za-z0-9_]*', type_name):
                raise ProtocolError('INVALID_COMMAND')
            qos = self.qos(frame.get('qos'))
            try:
                message_class = self.ros.get_message(type_name)
            except Exception:
                raise ProtocolError('MESSAGE_TYPE_UNAVAILABLE') from None
            if kind == 'START_SUBSCRIPTION':
                subscription_id = self.identifier(frame.get('subscriptionId'))
                if subscription_id in self.subscriptions:
                    raise ProtocolError('INVALID_COMMAND')
                if len(self.subscriptions) >= MAX_ENTITIES:
                    raise ProtocolError('RESOURCE_LIMIT')
                callback = lambda message: self.message(subscription_id, message)
                self.subscriptions[subscription_id] = self.node.create_subscription(message_class, topic, callback, qos)
                self.ready(request_id)
                return
            timeout = frame.get('timeoutMs')
            if type(timeout) is not int or not 0 < timeout <= 2147483647:
                raise ProtocolError('INVALID_COMMAND')
            frequency = None
            publication_id = None
            if kind == 'START_PERIODIC':
                frequency = frame.get('frequency')
                if type(frequency) is not int or not 0 < frequency <= 100:
                    raise ProtocolError('INVALID_FREQUENCY')
                publication_id = self.identifier(frame.get('publicationId'))
                if publication_id in self.periodic or any(
                        item['publication_id'] == publication_id for item in self.pending):
                    raise ProtocolError('INVALID_COMMAND')
            if len(self.pending) + len(self.periodic) >= MAX_PENDING:
                raise ProtocolError('RESOURCE_LIMIT')
            try:
                if not isinstance(frame.get('message'), dict):
                    raise ValueError()
                message = message_class()
                self.ros.set_message_fields(message, frame['message'])
            except Exception:
                raise ProtocolError('INVALID_MESSAGE') from None
            key = (topic, type_name, tuple(sorted(frame['qos'].items())))
            if key not in self.publishers:
                if len(self.publishers) >= MAX_ENTITIES:
                    raise ProtocolError('RESOURCE_LIMIT')
                self.publishers[key] = self.node.create_publisher(message_class, topic, qos)
            self.pending.append(dict(id=request_id, publisher=self.publishers[key], message=message,
                                     deadline=self.clock() + timeout / 1000, published=False,
                                     reliable=frame['qos']['reliability'] == 'RELIABLE',
                                     frequency=frequency, publication_id=publication_id))
        except ProtocolError as exc:
            self.error(exc.code, id=request_id)
        except Exception:
            self.error('ROS_ERROR', id=request_id)

    def message(self, subscription_id, message):
        try:
            converted = self.ros.message_to_ordereddict(message)
            emit(self.output, dict(type='MESSAGE', subscriptionId=subscription_id, message=converted))
        except ProtocolError as exc:
            self.error(exc.code, subscriptionId=subscription_id)
        except Exception:
            self.error('INVALID_MESSAGE', subscriptionId=subscription_id)

    def poll(self):
        for operation in self.pending[:]:
            try:
                if self.clock() >= operation['deadline']:
                    raise ProtocolError('PUBLISH_TIMEOUT')
                publisher = operation['publisher']
                if not operation['published']:
                    if publisher.get_subscription_count() == 0:
                        continue
                    self.refresh_timestamp(operation)
                    publisher.publish(operation['message'])
                    operation['published'] = True
                if operation['reliable'] and hasattr(publisher, 'wait_for_all_acked'):
                    if not publisher.wait_for_all_acked(self.ros.Duration(nanoseconds=0)):
                        continue
                self.ready(operation['id'])
                if operation['frequency'] is not None:
                    operation['next'] = self.clock() + 1 / operation['frequency']
                    self.periodic[operation['publication_id']] = operation
            except ProtocolError as exc:
                self.error(exc.code, id=operation['id'])
            except Exception:
                self.error('ROS_ERROR', id=operation['id'])
            else:
                self.pending.remove(operation)
                continue
            self.pending.remove(operation)
        for publication_id, operation in list(self.periodic.items()):
            if self.clock() < operation['next']:
                continue
            try:
                self.refresh_timestamp(operation)
                operation['publisher'].publish(operation['message'])
                operation['next'] = self.clock() + 1 / operation['frequency']
            except Exception:
                self.periodic.pop(publication_id, None)
                self.periodic_failures.add(publication_id)

    def refresh_timestamp(self, operation):
        message = operation['message']
        if operation['frequency'] is not None and hasattr(message, 'timestamp'):
            if type(message.timestamp) is not int:
                raise ProtocolError('INVALID_MESSAGE')
            message.timestamp = self.timestamp_clock()

    def close(self):
        self.pending.clear()
        self.periodic.clear()
        self.periodic_failures.clear()
        failed = False
        for sub in list(self.subscriptions.values()):
            try:
                if self.node.destroy_subscription(sub) is False:
                    failed = True
            except Exception:
                failed = True
        self.subscriptions.clear()
        for publisher in list(self.publishers.values()):
            try:
                if self.node.destroy_publisher(publisher) is False:
                    failed = True
            except Exception:
                failed = True
        self.publishers.clear()
        try:
            self.node.destroy_node()
        except Exception:
            failed = True
        if failed:
            raise ProtocolError('SHUTDOWN_FAILED')


def main():
    # Redirect the OS descriptor before importing native ROS modules: Python-level
    # redirect_stdout alone cannot intercept middleware printf/logging output.
    sys.stdout.flush()
    output = os.fdopen(os.dup(sys.stdout.fileno()), 'w', encoding='utf-8', buffering=1)
    os.dup2(sys.stderr.fileno(), sys.stdout.fileno())
    runtime = None
    rclpy = None
    executor = None
    reader = None
    initialized = False
    exit_code = 0
    try:
        import rclpy
        from rclpy.duration import Duration
        from rclpy.executors import SingleThreadedExecutor
        from rclpy.qos import DurabilityPolicy, HistoryPolicy, QoSProfile, ReliabilityPolicy
        from rosidl_runtime_py.utilities import get_message
        from rosidl_runtime_py.set_message import set_message_fields
        from rosidl_runtime_py.convert import message_to_ordereddict

        ros = SimpleNamespace(Duration=Duration, QoSProfile=QoSProfile, ReliabilityPolicy=ReliabilityPolicy,
                              DurabilityPolicy=DurabilityPolicy, HistoryPolicy=HistoryPolicy,
                              get_message=get_message, set_message_fields=set_message_fields,
                              message_to_ordereddict=message_to_ordereddict)
        # rclpy inherits ROS_DOMAIN_ID and the sourced ROS environment unchanged.
        rclpy.init(args=[])
        initialized = True
        node = rclpy.create_node('forgeit_' + uuid.uuid4().hex, enable_rosout=False)
        runtime = Runtime(node, ros, output)
        executor = SingleThreadedExecutor()
        executor.add_node(node)
        reader = CommandReader(sys.stdin.buffer)
        reader.start()
        runtime.ready('0')
        while not runtime.stopping and rclpy.ok():
            if reader.failure:
                runtime.error(reader.failure, id='0')
                break
            if reader.done.is_set() and reader.commands.empty():
                break
            try:
                runtime.command(reader.commands.get_nowait())
            except queue.Empty:
                pass
            if runtime.stopping:
                break
            executor.spin_once(timeout_sec=0.01)
            runtime.poll()
    except (Exception, KeyboardInterrupt):
        exit_code = 1
        try:
            emit(output, dict(type='ERROR', id='0', code='ROS_ERROR'))
        except Exception:
            pass
    finally:
        cleanup_failed = False
        if reader is not None and runtime is not None and runtime.stopping:
            try:
                # The reader stops after enqueuing SHUTDOWN. The parent owns
                # the overall deadline, including a stalled reader or ROS cleanup.
                reader.join()
            except (Exception, KeyboardInterrupt):
                cleanup_failed = True
        if executor is not None:
            try:
                if executor.shutdown() is False:
                    cleanup_failed = True
            except (Exception, KeyboardInterrupt):
                cleanup_failed = True
        if runtime is not None:
            try:
                runtime.close()
            except (Exception, KeyboardInterrupt):
                cleanup_failed = True
        if initialized:
            try:
                rclpy.try_shutdown()
            except (Exception, KeyboardInterrupt):
                cleanup_failed = True
        if cleanup_failed:
            exit_code = 1
        try:
            shutdown_id = runtime.shutdown_id if runtime is not None else None
            if shutdown_id is not None:
                if exit_code:
                    emit(output, dict(type='ERROR', id=shutdown_id, code='SHUTDOWN_FAILED'))
                else:
                    emit(output, dict(type='SHUTDOWN_COMPLETE', id=shutdown_id))
            elif cleanup_failed:
                emit(output, dict(type='ERROR', id='0', code='SHUTDOWN_FAILED'))
        except (Exception, KeyboardInterrupt):
            exit_code = 1
        finally:
            output.close()
    return exit_code


if __name__ == '__main__':
    sys.exit(main())
