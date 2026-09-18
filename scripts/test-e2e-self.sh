#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
exec mvn -B -pl forge-it-consumer-it -am \
  '-Dtest=HttpE2eSelfTest,E2eBootstrapTest,ServiceContractTest,JournalBindingTest,TransportParityTest,HttpExecutorTest,HttpDeadlineTest,MockMvcBuilder*Test,MockMvcJournal*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false test "$@"
