import importlib.util
import io
import json
import os
import select
from pathlib import Path
import subprocess
import sys
from types import SimpleNamespace
import unittest

PATH = Path(__file__).parents[2] / 'main/resources/com/sitionix/forgeit/ros/adapter.py'
spec = importlib.util.spec_from_file_location('adapter', PATH)
adapter = importlib.util.module_from_spec(spec)
if PATH.exists():
    spec.loader.exec_module(adapter)

QOS = dict(reliability='RELIABLE', durability='VOLATILE', history='KEEP_LAST', depth=10)


class Publisher:
    def __init__(self):
        self.matched = 0
        self.messages = []
        self.acked = True

    def get_subscription_count(self):
        return self.matched

    def publish(self, message):
        self.messages.append(message)

    def wait_for_all_acked(self, duration):
        return self.acked


class Node:
    def __init__(self):
        self.publishers = []
        self.subscriptions = []
        self.destroyed = False

    def create_publisher(self, cls, topic, qos):
        pub = Publisher()
        self.publishers.append(pub)
        return pub

    def create_subscription(self, cls, topic, callback, qos):
        self.subscriptions.append(callback)
        return callback

    def destroy_subscription(self, sub):
        self.subscriptions.remove(sub)

    def destroy_publisher(self, pub):
        self.publishers.remove(pub)

    def destroy_node(self):
        self.destroyed = True


class AdapterTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(hasattr(adapter, 'Runtime'), 'Runtime implementation is missing')
        self.out = io.StringIO()
        self.node = Node()
        self.now = 1.0
        policy = SimpleNamespace(RELIABLE=1, BEST_EFFORT=2, VOLATILE=3, TRANSIENT_LOCAL=4, KEEP_LAST=5)
        self.ros = SimpleNamespace(
            get_message=lambda name: SimpleNamespace,
            set_message_fields=lambda msg, fields: msg.__dict__.update(fields),
            message_to_ordereddict=lambda msg: msg.__dict__,
            QoSProfile=lambda **kwargs: kwargs,
            ReliabilityPolicy=policy, DurabilityPolicy=policy, HistoryPolicy=policy,
            Duration=lambda **kwargs: kwargs,
        )
        self.runtime = adapter.Runtime(self.node, self.ros, self.out, clock=lambda: self.now)

    def frames(self):
        return [json.loads(line) for line in self.out.getvalue().splitlines()]

    def command(self, kind='PUBLISH', **kwargs):
        return dict(type=kind, id='r1', topic='/test', messageType='std_msgs/msg/String',
                    qos=QOS.copy(), message={'data': 'secret'}, timeoutMs=100, **kwargs)

    def test_publish_waits_for_discovery_and_keeps_publisher_alive(self):
        self.runtime.command(self.command())
        self.runtime.poll()
        self.assertEqual([], self.frames())
        self.assertEqual([], self.node.publishers[0].messages)
        self.node.publishers[0].matched = 1
        self.runtime.poll()
        self.assertEqual([{'type': 'READY', 'id': 'r1'}], self.frames())
        self.assertEqual('secret', self.node.publishers[0].messages[0].data)
        self.runtime.command(self.command())
        self.runtime.poll()
        self.assertEqual(1, len(self.node.publishers))

    def test_publish_deadline_is_explicit(self):
        self.runtime.command(self.command())
        self.now += 1
        self.runtime.poll()
        self.assertEqual('PUBLISH_TIMEOUT', self.frames()[0]['code'])

    def test_ack_wait_does_not_block_and_has_deadline(self):
        self.runtime.command(self.command())
        self.node.publishers[0].matched = 1
        self.node.publishers[0].acked = False
        self.runtime.poll()
        self.assertEqual([], self.frames())
        self.now += 1
        self.runtime.poll()
        self.assertEqual('PUBLISH_TIMEOUT', self.frames()[0]['code'])

    def test_subscription_stream_and_stop(self):
        self.runtime.command(self.command('START_SUBSCRIPTION', subscriptionId='s1'))
        self.node.subscriptions[0](SimpleNamespace(data='hello'))
        self.runtime.command(dict(type='STOP_SUBSCRIPTION', id='r2', subscriptionId='s1'))
        self.assertEqual({'type': 'MESSAGE', 'subscriptionId': 's1', 'message': {'data': 'hello'}}, self.frames()[1])
        self.assertEqual([], self.node.subscriptions)

    def test_qos_rejects_unknown_and_invalid_depth_without_exception_text(self):
        for field, value in [('reliability', 'secret'), ('durability', 'secret'), ('depth', 0), ('depth', True), ('history', 'KEEP_ALL')]:
            command = self.command()
            command['qos'][field] = value
            self.runtime.command(command)
        self.assertTrue(all(frame['code'] == 'INVALID_QOS' for frame in self.frames()))
        self.assertNotIn('secret', self.out.getvalue())

    def test_qos_maps_every_supported_reliability_and_durability_pair(self):
        for reliability, expected_reliability in [('RELIABLE', 1), ('BEST_EFFORT', 2)]:
            for durability, expected_durability in [('VOLATILE', 3), ('TRANSIENT_LOCAL', 4)]:
                with self.subTest(reliability=reliability, durability=durability):
                    actual = self.runtime.qos(dict(reliability=reliability, durability=durability,
                                                   history='KEEP_LAST', depth=37))
                    self.assertEqual(dict(reliability=expected_reliability,
                                          durability=expected_durability, history=5, depth=37), actual)

    def test_conversion_errors_hide_payload_and_exception(self):
        def fail(*args):
            raise ValueError('secret')
        self.ros.set_message_fields = fail
        self.runtime.command(self.command())
        self.assertEqual([{'type': 'ERROR', 'id': 'r1', 'code': 'INVALID_MESSAGE'}], self.frames())

    def test_large_subscription_message_is_explicit_failure(self):
        self.runtime.command(self.command('START_SUBSCRIPTION', subscriptionId='s1'))
        self.node.subscriptions[0](SimpleNamespace(data='x' * adapter.MAX_FRAME))
        self.assertEqual({'type': 'ERROR', 'subscriptionId': 's1', 'code': 'FRAME_TOO_LARGE'}, self.frames()[1])

    def test_cleanup_releases_every_ros_entity(self):
        self.runtime.command(self.command())
        self.runtime.command(self.command('START_SUBSCRIPTION', subscriptionId='s1'))
        self.runtime.close()
        self.assertEqual([], self.node.publishers)
        self.assertEqual([], self.node.subscriptions)
        self.assertTrue(self.node.destroyed)

    def test_shutdown_does_not_acknowledge_before_cleanup(self):
        self.runtime.command(dict(type='SHUTDOWN', id='stop1'))
        self.assertTrue(self.runtime.stopping)
        self.assertEqual([], self.frames())

    def test_cleanup_attempts_all_entities_after_failure(self):
        self.runtime.command(self.command())
        self.runtime.command(self.command('START_SUBSCRIPTION', subscriptionId='s1'))
        def fail(sub):
            raise RuntimeError('private cleanup secret')
        self.node.destroy_subscription = fail
        with self.assertRaises(adapter.ProtocolError) as failure:
            self.runtime.close()
        self.assertEqual('SHUTDOWN_FAILED', failure.exception.code)
        self.assertEqual([], self.node.publishers)
        self.assertTrue(self.node.destroyed)

    def test_reader_bounds_queue_and_frame(self):
        reader = adapter.CommandReader(io.BytesIO(b'{}\n{}\n'), capacity=1)
        reader.run()
        self.assertEqual('QUEUE_OVERFLOW', reader.failure)
        reader = adapter.CommandReader(io.BytesIO(b'x' * (adapter.MAX_FRAME + 1)))
        reader.run()
        self.assertEqual('FRAME_TOO_LARGE', reader.failure)

    def test_reader_rejects_nonobjects_and_invalid_utf8(self):
        for data in [b'[]\n', b'\xff\n', b'{"x":NaN}\n']:
            reader = adapter.CommandReader(io.BytesIO(data))
            reader.run()
            self.assertEqual('INVALID_FRAME', reader.failure)

    def test_resource_limit_fails_explicitly(self):
        for i in range(adapter.MAX_PENDING + 1):
            frame = self.command()
            frame['id'] = 'r' + str(i)
            self.runtime.command(frame)
        self.assertEqual('RESOURCE_LIMIT', self.frames()[-1]['code'])
        self.assertEqual(adapter.MAX_PENDING, len(self.runtime.pending))

    def test_unknown_type_has_safe_error(self):
        def fail(name):
            raise ImportError('private environment secret')
        self.ros.get_message = fail
        self.runtime.command(self.command())
        self.assertEqual([{'type': 'ERROR', 'id': 'r1', 'code': 'MESSAGE_TYPE_UNAVAILABLE'}], self.frames())


class ProcessTest(unittest.TestCase):
    # Fake only the unavailable ROS packages. Run the actual main/process I/O.
    BOOTSTRAP = '''
import importlib.util, os, sys, types
control = (int(sys.argv[3]), int(sys.argv[4])) if len(sys.argv) > 3 else None
failed_stage = sys.argv[2] if len(sys.argv) > 2 else ""
def cleanup(stage):
    print(stage + "_CLOSED", file=sys.stderr, flush=True)
    if control is not None:
        os.write(control[1], stage.encode() + b"\\n")
        assert os.read(control[0], 1) == b"+"
    if stage == failed_stage:
        raise RuntimeError("private cleanup secret")
    return failed_stage != stage + "_FALSE"
def module(name, **values):
    mod = types.ModuleType(name)
    mod.__dict__.update(values)
    sys.modules[name] = mod
    return mod
def init(**kwargs):
    os.write(1, b'NATIVE_LOG\\n')
    print('PYTHON_LOG', flush=True)
class Node:
    def destroy_node(self):
        cleanup('NODE')
    def destroy_subscription(self, sub): return cleanup('SUBSCRIPTION')
    def destroy_publisher(self, pub): return cleanup('PUBLISHER')
class Executor:
    def add_node(self, node): pass
    def spin_once(self, **kwargs): pass
    def shutdown(self, timeout_sec=None):
        assert timeout_sec is None
        return cleanup('EXECUTOR')
module('rclpy', init=init, create_node=lambda *a, **k: Node(), ok=lambda: True,
       try_shutdown=lambda: cleanup('ROS'))
module('rclpy.duration', Duration=object)
module('rclpy.executors', SingleThreadedExecutor=Executor)
module('rclpy.qos', DurabilityPolicy=object, HistoryPolicy=object,
       QoSProfile=object, ReliabilityPolicy=object)
module('rosidl_runtime_py')
module('rosidl_runtime_py.utilities', get_message=lambda: None)
module('rosidl_runtime_py.set_message', set_message_fields=lambda: None)
module('rosidl_runtime_py.convert', message_to_ordereddict=lambda: None)
spec = importlib.util.spec_from_file_location('adapter', sys.argv[1])
adapter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(adapter)
if control is not None:
    original_run = adapter.CommandReader.run
    def reader_run(self):
        original_run(self)
        cleanup('READER')
    adapter.CommandReader.run = reader_run
if len(sys.argv) > 2:
    original_init = adapter.Runtime.__init__
    def runtime_init(self, *args, **kwargs):
        original_init(self, *args, **kwargs)
        self.subscriptions['s1'] = object()
        self.publishers['p1'] = object()
    adapter.Runtime.__init__ = runtime_init
sys.exit(adapter.main())
'''

    def test_native_stdout_is_redirected_and_shutdown_and_eof_cleanup(self):
        for commands in ['', '{"type":"SHUTDOWN","id":"r1"}\n']:
            process = subprocess.run([sys.executable, '-c', self.BOOTSTRAP, str(PATH)],
                                     input=commands, text=True, capture_output=True, timeout=5)
            self.assertEqual(0, process.returncode, process.stderr)
            frames = [json.loads(line) for line in process.stdout.splitlines()]
            self.assertEqual({'type': 'READY', 'id': '0'}, frames[0])
            self.assertEqual(2 if commands else 1, len(frames))
            if commands:
                self.assertEqual({'type': 'SHUTDOWN_COMPLETE', 'id': 'r1'}, frames[-1])
            for marker in ['NATIVE_LOG', 'PYTHON_LOG', 'NODE_CLOSED', 'ROS_CLOSED']:
                self.assertIn(marker, process.stderr)

        # A SHUTDOWN must also stop the input thread while the parent pipe is open.
        process = subprocess.Popen([sys.executable, '-c', self.BOOTSTRAP, str(PATH)],
                                   stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE, text=True)
        try:
            self.assertEqual('READY', json.loads(process.stdout.readline())['type'])
            process.stdin.write('{"type":"SHUTDOWN","id":"r1"}\n')
            process.stdin.flush()
            self.assertEqual({'type': 'SHUTDOWN_COMPLETE', 'id': 'r1'},
                             json.loads(process.stdout.readline()))
            self.assertEqual(0, process.wait(timeout=5), process.stderr.read())
        finally:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=5)
            process.stdin.close()
            process.stdout.close()
            process.stderr.close()

    def test_shutdown_waits_for_each_cleanup_stage_with_stdin_open(self):
        release_read, release_write = os.pipe()
        stage_read, stage_write = os.pipe()
        process = subprocess.Popen(
            [sys.executable, '-c', self.BOOTSTRAP, str(PATH), '', str(release_read), str(stage_write)],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            pass_fds=(release_read, stage_write), bufsize=0)
        os.close(release_read)
        os.close(stage_write)
        control = os.fdopen(stage_read, 'rb', buffering=0)
        try:
            self.assertEqual({'type': 'READY', 'id': '0'}, json.loads(process.stdout.readline()))
            process.stdin.write(b'{"type":"SHUTDOWN","id":"stop1"}\n')
            for stage in ['READER', 'EXECUTOR', 'SUBSCRIPTION', 'PUBLISHER', 'NODE', 'ROS']:
                self.assertTrue(select.select([control], [], [], 5)[0], 'Cleanup did not reach ' + stage)
                self.assertEqual(stage.encode() + b'\n', control.readline())
                self.assertEqual([], select.select([process.stdout], [], [], 0)[0],
                                 'Shutdown acknowledged before ' + stage + ' finished')
                self.assertIsNone(process.poll())
                os.write(release_write, b'+')
            self.assertEqual({'type': 'SHUTDOWN_COMPLETE', 'id': 'stop1'},
                             json.loads(process.stdout.readline()))
            self.assertEqual(0, process.wait(timeout=5), process.stderr.read())
            self.assertEqual(b'', process.stdout.read())
        finally:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=5)
            control.close()
            os.close(release_write)
            for stream in (process.stdin, process.stdout, process.stderr):
                stream.close()

    def test_cleanup_failures_emit_safe_shutdown_error_and_nonzero_exit(self):
        for failed_stage in ['EXECUTOR', 'SUBSCRIPTION', 'PUBLISHER', 'NODE', 'ROS',
                             'EXECUTOR_FALSE', 'SUBSCRIPTION_FALSE', 'PUBLISHER_FALSE']:
            with self.subTest(failed_stage=failed_stage):
                process = subprocess.run(
                    [sys.executable, '-c', self.BOOTSTRAP, str(PATH), failed_stage],
                    input='{"type":"SHUTDOWN","id":"stop1"}\n',
                    capture_output=True, text=True, timeout=5)
                self.assertNotEqual(0, process.returncode)
                self.assertEqual([
                    {'type': 'READY', 'id': '0'},
                    {'type': 'ERROR', 'id': 'stop1', 'code': 'SHUTDOWN_FAILED'},
                ], [json.loads(line) for line in process.stdout.splitlines()])
                self.assertNotIn('secret', process.stdout + process.stderr)
                for stage in ['EXECUTOR', 'SUBSCRIPTION', 'PUBLISHER', 'NODE', 'ROS']:
                    self.assertIn(stage + '_CLOSED', process.stderr)


if __name__ == '__main__':
    unittest.main()
