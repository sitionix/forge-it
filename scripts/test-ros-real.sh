#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
# Source the desired ROS installation/overlay before invoking this opt-in test.
exec mvn -B -ntp -pl forge-it-consumer-it -am \
  -Dtest=RosRealSelfTest -Dforgeit.ros.real=true \
  -Dsurefire.failIfNoSpecifiedTests=false test "$@"
