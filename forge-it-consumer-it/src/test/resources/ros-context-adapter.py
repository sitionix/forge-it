#!/usr/bin/env python3
# Protocol fixture only: no ROS emulation is used by the real self-test.
import json
import sys

def send(value):
    print(json.dumps(value), flush=True)

send({"type": "READY", "id": "0"})
for line in sys.stdin:
    command = json.loads(line)
    send({"type": "SHUTDOWN_COMPLETE" if command["type"] == "SHUTDOWN" else "READY", "id": command["id"]})
    if command["type"] == "START_SUBSCRIPTION":
        for value in ["STARTING", "FUSION_READY"]:
            send({"type": "MESSAGE", "subscriptionId": command["subscriptionId"], "message": {"data": value}})
    if command["type"] == "SHUTDOWN":
        break
