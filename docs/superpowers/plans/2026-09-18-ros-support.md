# ROS support implementation plan

Goal: first-class fixture-based ROS 2 DSL with one Python rclpy process per selected ForgeIT context.
Spec: user request of 2026-09-18. Java owns assertions and lifecycle; no ROS Java or bundled ROS.

Architecture: immutable RosTopicContract/RosQos -> RosMessaging builders -> RosTransport -> JSON Lines adapter -> rclpy.

Tasks:
1. Java API, validation, fixture comparison and consumer deadline semantics; tests use deterministic stream handoff.
2. Python runtime and protocol unit tests, dynamic ROS message resolution and QoS, publisher discovery, clean shutdown.
3. Java process transport and protocol tests: deterministic IDs, bounded queue, hard deadlines, safe errors, cleanup.
4. Register module/feature in parent/core/bundle/processor; enable ROS in E2E allowlist; generated-support context tests.
5. Run Docker-free suites and opt-in actual ROS self-test after sourcing /opt/ros/lyrical/setup.bash; review and report.

Protocol v1: UTF-8 JSON object per line, max 1 MiB per frame. stdout only protocol, diagnostics stderr without payloads.
Runtime sends {type:READY,id:"0"} after startup.
Commands have deterministic IDs (r1,r2,...) and subscription IDs (s1,s2,...).
START_SUBSCRIPTION: id,subscriptionId,topic,messageType,qos; READY same id means subscriber created.
STOP_SUBSCRIPTION: id,subscriptionId -> READY.
PUBLISH: id,topic,messageType,qos,message,timeoutMs -> READY after matched DDS subscriber and publish (reliable ACK if supported).
MESSAGE: subscriptionId,message (JSON object).
ERROR: id (request) or subscriptionId, code (safe fixed code), no raw exception text.
SHUTDOWN: id -> READY then exit.
QoS keys: reliability RELIABLE/BEST_EFFORT; durability VOLATILE/TRANSIENT_LOCAL; history KEEP_LAST; depth positive integer.
No retries or fallback. Queue overflow fails explicitly; no oldest-message eviction.

Internal Java seam:
RosTransport.subscribe(String topic,String messageType,RosQos qos,Duration timeout) -> RosSubscription
RosTransport.publish(String topic,String messageType,RosQos qos,JsonNode message,Duration timeout) -> void
RosSubscription.next(Duration remaining) -> JsonNode (null on timeout), close() stops subscription.
PythonRosTransport(String pythonCommand,Duration startupTimeout,String domainId) starts runtime; close() terminates.
Resource: /com/sitionix/forgeit/ros/adapter.py.

One overall consumer deadline includes subscription setup; no cached first message. await is first-message budget;
waitUntilAsserted selects its own overall budget. Last assertion summary contains mismatch counts, never values.
JSON comparison uses JSONAssert LENIENT (same library/semantics as existing ForgeIT), recursively ignored field names.

Progress:
- Repository clean; base origin/develop includes released timing work.
- ROS Lyrical and std_msgs/msg/String available locally.

Completed implementation/verification:
- Public API, contract/property validation, explicit QoS and fixture comparison implemented.
- Python adapter: 14 Docker-free tests pass; finite bounded JSONL protocol and native stdout isolation.
- Java transport and DSL: 25 module tests pass, including missing interpreter/domain propagation and stalled STOP.
- Generated ROS-only E2E context: 2 tests pass. ROS-only IntegrationTest context: 1 test passes.
- Existing HTTP self-tests: 46 pass. Existing Auth/Selective IT plus E2E regression: 16 pass.
- Actual ROS Lyrical on domain187: 4 tests pass (first success, first mismatch, later match, unknown type).
- Independent review found cleanup extending consumer deadline; fixed via bounded asynchronous STOP tracking.
  Scoped review reran a stalled STOP probe: 50ms budget finishes in59ms, preserving primary outcome.
- Python protocol suite added to PR CI; actual ROS tests remain explicitly opt-in.
