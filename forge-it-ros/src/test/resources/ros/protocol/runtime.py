import json, sys, threading, signal, os
from pathlib import Path
Path(__file__ + ".pid").write_text(str(os.getpid()))
Path(__file__ + ".domain").write_text(json.dumps(os.environ.get("ROS_DOMAIN_ID")))
mode = __file__.split('/')[-1].removesuffix('.py')
def emit(value):
    print(json.dumps(value), flush=True)
if mode == 'startup_timeout':
    threading.Event().wait()
if mode == 'startup_error':
    emit({'type': 'ERROR', 'id': '0', 'code': 'secret-host-token'})
    sys.exit(1)
if mode == 'malformed':
    print('secret-host-token', flush=True)
    threading.Event().wait()
if mode == 'large':
    print('x' * (1024 * 1024 + 1), flush=True)
    threading.Event().wait()
emit({'type': 'READY', 'id': '0'})
if mode == 'blocked':
    signal.signal(signal.SIGTERM, signal.SIG_IGN)
    threading.Event().wait()
subscriptions = {}
for line in sys.stdin:
    request = json.loads(line)
    if mode == 'generation':
        with open(__file__ + '.commands', 'a') as log:
            log.write(line)
    kind = request['type']
    if kind == 'START_SUBSCRIPTION':
        if request['messageType'] == 'missing/msg/Type':
            emit({'type': 'ERROR', 'id': request['id'], 'code': 'MESSAGE_TYPE_UNAVAILABLE', 'detail': 'secret-host-token'})
            continue
        subscriptions[request['subscriptionId']] = request['topic']
    if kind == 'STOP_SUBSCRIPTION' and mode == 'stalled_stop':
        threading.Event().wait()
    if kind == 'STOP_SUBSCRIPTION' and mode == 'error_stop':
        emit({'type': 'ERROR', 'id': request['id'], 'code': 'ROS_ERROR'})
        continue
    if kind == 'PUBLISH':
        if request['topic'] == '/exit':
            sys.exit(1)
        if request['topic'] == '/error':
            emit({'type': 'ERROR', 'id': request['id'], 'code': 'secret-host-token'})
            continue
        for sub, topic in subscriptions.items():
            if topic == request['topic']:
                for _ in range(65 if topic == '/overflow' else 1):
                    emit({'type': 'MESSAGE', 'subscriptionId': sub, 'message': request['message']})
    if kind == 'SHUTDOWN':
        if mode == 'shutdown_no_ack':
            threading.Event().wait()
        if mode == 'shutdown_error':
            emit({'type': 'ERROR', 'id': request['id'], 'code': 'SHUTDOWN_FAILED', 'detail': 'private-secret'})
            sys.exit(1)
        if mode == 'shutdown_gate':
            import socket
            control = socket.create_connection(('127.0.0.1', int(Path(__file__ + '.port').read_text())))
            control.sendall(b'cleanup\n')
            control.recv(1)
        emit({'type': 'SHUTDOWN_COMPLETE', 'id': request['id']})
        if mode == 'shutdown_eof':
            sys.stdout.close()
            os.close(1)
            sys.stdin.read()
            Path(__file__ + '.clean_exit').touch()
            os._exit(0)
        if mode == 'shutdown_stalled':
            threading.Event().wait()
        if mode == 'shutdown_gate':
            control.sendall(b'exit\n')
            control.recv(1)
        Path(__file__ + '.shutdown').touch()
        sys.exit(7 if mode == 'shutdown_nonzero' else 0)
    emit({'type': 'READY', 'id': request['id']})
