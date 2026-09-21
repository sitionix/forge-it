# ROS 2 implementation report

Base: `4c4c445e0a0f9bc447edcbdb31ba094646658ce9`.
Branch: `feature/SITIONIX-68`. No release or remote publication was performed.

## Module structure

- `api`: RosSupport, RosMessaging, publish/consume builders, immutable topic contract and QoS.
- `config` / `internal/config`: default properties and explicit feature installation.
- `internal/service`: fixture resolution and first-message/streaming assertions.
- `internal/transport`: one process/context, bounded JSONL handoff and lifecycle.
- `src/main/resources/com/sitionix/forgeit/ros/adapter.py`: actual rclpy/DDS runtime.
- Module tests + consumer generated-context tests + opt-in real ROS tests.

## Verification

All final verification commands succeeded; no test failures/errors.

```bash
./scripts/test-ros-self.sh
# Python: 14 tests OK. Java: 25 module + 3 context tests, all passed.

./scripts/test-e2e-self.sh -ntp
# Existing HTTP/MockMvc/E2E: 46 tests passed.

mvn -B -ntp -pl forge-it-consumer-it -am \
  '-Dtest=AuthControllerIT,SelectiveSupportIT,HttpE2eSelfTest,E2eBootstrapTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dapi.version=1.40 test
# Existing consumer IT/E2E: 16 tests passed.

source /opt/ros/lyrical/setup.bash
ROS_DOMAIN_ID=187 ./scripts/test-ros-real.sh
# Real ROS: 4 tests, Failures: 0, Errors: 0, Skipped: 0.

mvn -B -ntp -pl forge-it-bundle -am -DskipTests package
# BUILD SUCCESS. Inspected shaded JAR: RosSupport, PythonRosTransport,
# adapter.py, feature whitelist and installer metadata all present.
```

Actual ROS checks used unique temporary topics with `std_msgs/msg/String`: first
message succeeds, first mismatch fails immediately, a later matching message passes,
and unknown message type fails explicitly. No Ancestor services, hardware, Gazebo,
PX4, or Dockerized ROS were started.

Independent review found and verified a fix for STOP cleanup extending the assertion
deadline. An independent stalled-STOP probe with a 50ms receive budget returned in
59ms after the fix (1062ms before), preserving the primary assertion outcome.

## Protocol / QoS / limits

See README ROS section and implementation plan for exact JSONL commands and resource
bounds. Reliability supports RELIABLE and BEST_EFFORT; durability VOLATILE and
TRANSIENT_LOCAL; history KEEP_LAST with positive depth. Unsupported values fail.
Python resolves message types dynamically; no project-specific message classes exist
in production code. Finite JSON objects only; ROS conversion failures are explicit.

The runtime must already have sourced ROS and message overlays. Tested on Linux with
ROS Lyrical; actual DDS tests are opt-in for CI without ROS. Publishing requires a
matched subscriber and is bounded by startup-timeout. DDS acknowledgment does not
mean application processing; BEST_EFFORT does not guarantee delivery. Java queues
hold 64 messages/subscription and frames are capped at 1 MiB. Overflow is explicit,
never silent message eviction. STOP acknowledgment is asynchronous and bounded;
context shutdown still terminates/reaps the managed process.

## Files changed

- `.github/workflows/ci.yml`
- `README.md`
- `docs/ros-implementation-report.md`
- `docs/superpowers/plans/2026-09-18-ros-support.md`
- `forge-it-annotation-processor/src/main/resources/META-INF/forge-it/features`
- `forge-it-bundle/pom.xml`
- `forge-it-consumer-it/pom.xml`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/ros/RosContextSelfTest.java`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/ros/RosItContextSelfTest.java`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/ros/RosOnlySupport.java`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/ros/RosRealSelfTest.java`
- `forge-it-consumer-it/src/test/resources/forge-it/ros/default/expected/ready.json`
- `forge-it-consumer-it/src/test/resources/forge-it/ros/default/publish/ready.json`
- `forge-it-consumer-it/src/test/resources/forge-it/ros/publish/starting.json`
- `forge-it-consumer-it/src/test/resources/ros-context-adapter.py`
- `forge-it-core/pom.xml`
- `forge-it-core/src/main/java/com/sitionix/forgeit/core/internal/test/IntegrationTestContextCustomizerFactory.java`
- `forge-it-ros/pom.xml`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/api/RosConsumeBuilder.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/api/RosMessaging.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/api/RosPublishBuilder.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/api/RosQos.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/api/RosSupport.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/api/RosTopicContract.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/config/RosDefaultsEnvironmentPostProcessor.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/internal/config/RosFeatureInstaller.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/internal/config/RosProperties.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/internal/service/RosMessagingFacade.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/internal/transport/PythonRosTransport.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/internal/transport/RosSubscription.java`
- `forge-it-ros/src/main/java/com/sitionix/forgeit/ros/internal/transport/RosTransport.java`
- `forge-it-ros/src/main/resources/META-INF/forge-it/features`
- `forge-it-ros/src/main/resources/META-INF/spring.factories`
- `forge-it-ros/src/main/resources/com/sitionix/forgeit/ros/adapter.py`
- `forge-it-ros/src/main/resources/forge-it-ros-default.yml`
- `forge-it-ros/src/test/java/com/sitionix/forgeit/ros/api/RosContractTest.java`
- `forge-it-ros/src/test/java/com/sitionix/forgeit/ros/internal/service/RosMessagingTest.java`
- `forge-it-ros/src/test/java/com/sitionix/forgeit/ros/internal/transport/PythonRosTransportTest.java`
- `forge-it-ros/src/test/python/test_adapter.py`
- `forge-it-ros/src/test/resources/ros/protocol/runtime.py`
- `pom.xml`
- `scripts/test-ros-real.sh`
- `scripts/test-ros-self.sh`
