#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s forge-it-ros/src/test/python -v
exec mvn -B -ntp -pl forge-it-consumer-it -am \
  '-Dtest=RosContractTest,RosMessagingTest,RosShutdownPropertiesTest,RosContextLifecycleTest,PythonRosTransportTest,RosContextSelfTest,RosItContextSelfTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test "$@"
