import importlib.util
import io
import json
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
    def test_native_stdout_is_redirected_and_shutdown_and_eof_cleanup(self):
        # Fake only the unavailable ROS packages. Run the actual main/process I/O.
        bootstrap = '''
import importlib.util, os, sys, types
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
        print('NODE_CLOSED', file=sys.stderr)
class Executor:
    def add_node(self, node): pass
    def spin_once(self, **kwargs): pass
    def shutdown(self, **kwargs): pass
module('rclpy', init=init, create_node=lambda *a, **k: Node(), ok=lambda: True,
       try_shutdown=lambda: print('ROS_CLOSED', file=sys.stderr))
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
adapter.main()
'''
        for commands in ['', '{"type":"SHUTDOWN","id":"r1"}\n']:
            process = subprocess.run([sys.executable, '-c', bootstrap, str(PATH)],
                                     input=commands, text=True, capture_output=True, timeout=5)
            self.assertEqual(0, process.returncode, process.stderr)
            frames = [json.loads(line) for line in process.stdout.splitlines()]
            self.assertEqual({'type': 'READY', 'id': '0'}, frames[0])
            self.assertEqual(2 if commands else 1, len(frames))
            for marker in ['NATIVE_LOG', 'PYTHON_LOG', 'NODE_CLOSED', 'ROS_CLOSED']:
                self.assertIn(marker, process.stderr)

        # A SHUTDOWN must also stop the input thread while the parent pipe is open.
        process = subprocess.Popen([sys.executable, '-c', bootstrap, str(PATH)],
                                   stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE, text=True)
        try:
            self.assertEqual('READY', json.loads(process.stdout.readline())['type'])
            process.stdin.write('{"type":"SHUTDOWN","id":"r1"}\n')
            process.stdin.flush()
            self.assertEqual('r1', json.loads(process.stdout.readline())['id'])
            self.assertEqual(0, process.wait(timeout=5), process.stderr.read())
        finally:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=5)
            process.stdin.close()
            process.stdout.close()
            process.stderr.close()


if __name__ == '__main__':
    unittest.main()
