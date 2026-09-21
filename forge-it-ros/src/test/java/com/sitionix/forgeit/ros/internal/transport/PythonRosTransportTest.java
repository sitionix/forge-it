package com.sitionix.forgeit.ros.internal.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.ros.api.RosQos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class PythonRosTransportTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final RosQos QOS = new RosQos(RosQos.Reliability.RELIABLE,
            RosQos.Durability.VOLATILE, RosQos.History.KEEP_LAST, 10);
    @TempDir Path directory;

    private PythonRosTransport runtime(String mode) throws Exception {
        return runtime(mode, "", TIMEOUT);
    }

    private PythonRosTransport runtime(String mode, String domainId, Duration startupTimeout) throws Exception {
        Path script = directory.resolve(mode + ".py");
        try (var source = getClass().getResourceAsStream("/ros/protocol/runtime.py")) { Files.copy(source, script); }
        return new PythonRosTransport("python3", startupTimeout, domainId, script);
    }

    @Test void acknowledgedShutdownClosesStdinAndAllowsExpectedProtocolEof() throws Exception {
        long pid;
        try (var transport = runtime("shutdown_eof")) {
            pid = Long.parseLong(Files.readString(directory.resolve("shutdown_eof.py.pid")));
        }
        assertThat(directory.resolve("shutdown_eof.py.clean_exit")).exists();
        assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
    }

    @Test void acknowledgedButStalledShutdownRemainsBounded() throws Exception {
        var transport = runtime("shutdown_stalled");
        long pid = Long.parseLong(Files.readString(directory.resolve("shutdown_stalled.py.pid")));
        long start = System.nanoTime();
        transport.close();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(4));
        assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        transport.close();
    }

    @Test void reusesRuntimeForMultipleTopicsAndConcurrentConsumption() throws Exception {
        try (var transport = runtime("runtime"); var sub = transport.subscribe("/a", "std_msgs/msg/String", QOS, TIMEOUT)) {
            var expected = new ObjectMapper().readTree("{\"data\":\"private payload\"}");
            CompletableFuture<com.fasterxml.jackson.databind.JsonNode> received = CompletableFuture.supplyAsync(() -> sub.next(TIMEOUT));
            transport.publish("/a", "std_msgs/msg/String", QOS, expected, TIMEOUT);
            assertThat(received.get(3, TimeUnit.SECONDS)).isEqualTo(expected);
            assertThat(sub.next(Duration.ofMillis(20))).isNull();
            try (var second = transport.subscribe("/b", "std_msgs/msg/String", QOS, TIMEOUT)) {
                transport.publish("/b", "std_msgs/msg/String", QOS, expected, TIMEOUT);
                assertThat(second.next(TIMEOUT)).isEqualTo(expected);
            }
        }
    }

    @Test void reportsOnlySafeCodesAndCanContinueAfterRequestError() throws Exception {
        try (var transport = runtime("runtime")) {
            assertThatThrownBy(() -> transport.subscribe("/a", "missing/msg/Type", QOS, TIMEOUT))
                    .hasMessage("ROS transport: MESSAGE_TYPE_UNAVAILABLE").hasNoCause();
            assertThatThrownBy(() -> transport.publish("/error", "std_msgs/msg/String", QOS,
                    new ObjectMapper().createObjectNode(), TIMEOUT)).hasMessage("ROS transport: ROS_ERROR").hasNoCause();
            try (var sub = transport.subscribe("/a", "std_msgs/msg/String", QOS, TIMEOUT)) {
                assertThat(sub.next(Duration.ofMillis(10))).isNull();
            }
        }
    }

    @Test void overflowFailsInsteadOfDroppingMessages() throws Exception {
        try (var transport = runtime("runtime"); var sub = transport.subscribe("/overflow", "std_msgs/msg/String", QOS, TIMEOUT)) {
            transport.publish("/overflow", "std_msgs/msg/String", QOS, new ObjectMapper().createObjectNode(), TIMEOUT);
            assertThatThrownBy(() -> sub.next(TIMEOUT)).hasMessage("ROS transport: QUEUE_OVERFLOW");
        }
    }

    @Test void startupFailuresAreBoundedAndPrivate() {
        for (String mode : new String[] {"startup_error", "malformed", "large"}) {
            assertThatThrownBy(() -> runtime(mode)).isInstanceOf(IllegalStateException.class)
                    .hasMessageStartingWith("ROS transport: ").hasMessageNotContaining("secret").hasNoCause();
        }
    }

    @Test void missingPythonExecutableFailsWithoutLeakingItsPath() {
        String missingExecutable = directory.resolve("private-host-secret-python").toString();
        assertThatThrownBy(() -> new PythonRosTransport(missingExecutable, TIMEOUT, "", directory.resolve("adapter.py")))
                .hasMessage("ROS transport: STARTUP_FAILED").hasNoCause();
    }

    @Test void startupDeadlineTerminatesUnresponsiveProcess() throws Exception {
        long start = System.nanoTime();
        assertThatThrownBy(() -> runtime("startup_timeout", "", Duration.ofMillis(400)))
                .hasMessage("ROS transport: STARTUP_TIMEOUT").hasNoCause();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
        Path pidFile = directory.resolve("startup_timeout.py.pid");
        if (Files.exists(pidFile)) {
            long pid = Long.parseLong(Files.readString(pidFile));
            assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        }
    }

    @Test void configuredDomainOverridesEnvironmentAndBlankDomainInheritsIt() throws Exception {
        var mapper = new ObjectMapper();
        try (var configured = runtime("configured_domain", "42", TIMEOUT)) {
            assertThat(mapper.readTree(Files.readString(directory.resolve("configured_domain.py.domain"))).asText())
                    .isEqualTo("42");
        }
        try (var inherited = runtime("inherited_domain", "  ", TIMEOUT)) {
            assertThat(mapper.readTree(Files.readString(directory.resolve("inherited_domain.py.domain"))))
                    .isEqualTo(mapper.valueToTree(System.getenv("ROS_DOMAIN_ID")));
        }
        try (var inherited = runtime("null_domain", null, TIMEOUT)) {
            assertThat(mapper.readTree(Files.readString(directory.resolve("null_domain.py.domain"))))
                    .isEqualTo(mapper.valueToTree(System.getenv("ROS_DOMAIN_ID")));
        }
    }

    @Test void eofFailsPendingRequest() throws Exception {
        try (var transport = runtime("runtime")) {
            assertThatThrownBy(() -> transport.publish("/exit", "std_msgs/msg/String", QOS,
                    new ObjectMapper().createObjectNode(), TIMEOUT)).hasMessage("ROS transport: PROCESS_EXITED");
        }
    }

    @Test void blockedPipeCannotDefeatRequestDeadline() throws Exception {
        try (var transport = runtime("blocked")) {
            var message = new ObjectMapper().createObjectNode().put("data", "x".repeat(900_000));
            long start = System.nanoTime();
            assertThatThrownBy(() -> transport.publish("/a", "std_msgs/msg/String", QOS, message, Duration.ofMillis(200)))
                    .hasMessage("ROS transport: REQUEST_TIMEOUT");
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
        }
    }

    @Test void writesDeterministicCommandsAndShutsDownActualChild() throws Exception {
        var mapper = new ObjectMapper();
        long pid;
        try (var transport = runtime("generation")) {
            pid = Long.parseLong(Files.readString(directory.resolve("generation.py.pid")));
            try (var subscription = transport.subscribe("/a", "std_msgs/msg/String", QOS, TIMEOUT)) {
                transport.publish("/a", "std_msgs/msg/String", QOS, mapper.createObjectNode().put("data", "hello"), TIMEOUT);
                assertThat(subscription.next(TIMEOUT).path("data").asText()).isEqualTo("hello");
            }
        }
        var commands = Files.readAllLines(directory.resolve("generation.py.commands"));
        assertThat(commands).hasSize(4);
        for (int index = 0; index < commands.size(); index++) {
            assertThat(mapper.readTree(commands.get(index)).path("id").asText()).isEqualTo("r" + (index + 1));
        }
        assertThat(mapper.readTree(commands.get(0)).path("subscriptionId").asText()).isEqualTo("s1");
        var publish = mapper.readTree(commands.get(1));
        assertThat(publish.path("qos").path("reliability").asText()).isEqualTo("RELIABLE");
        assertThat(publish.path("qos").path("depth").asInt()).isEqualTo(10);
        assertThat(publish.path("timeoutMs").asLong()).isBetween(1L, 3000L);
        assertThat(publish.path("message").path("data").asText()).isEqualTo("hello");
        assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
    }

    @Test void stalledStopDoesNotDelayCloseOrReplaceCompletedResult() throws Exception {
        long pid;
        try (var transport = runtime("stalled_stop")) {
            pid = Long.parseLong(Files.readString(directory.resolve("stalled_stop.py.pid")));
            var subscription = transport.subscribe("/a", "std_msgs/msg/String", QOS, TIMEOUT);
            var expected = new ObjectMapper().createObjectNode().put("data", "completed");
            transport.publish("/a", "std_msgs/msg/String", QOS, expected, TIMEOUT);
            var received = subscription.next(TIMEOUT);
            long start = System.nanoTime();
            assertThatCode(subscription::close).doesNotThrowAnyException();
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(500));
            assertThat(received).isEqualTo(expected);
            subscription.close();
        }
        assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
    }

    @Test void unacknowledgedStopEventuallyTerminatesRuntimeWithoutFurtherRequests() throws Exception {
        try (var transport = runtime("stalled_stop")) {
            long pid = Long.parseLong(Files.readString(directory.resolve("stalled_stop.py.pid")));
            var exited = ProcessHandle.of(pid).orElseThrow().onExit();
            transport.subscribe("/a", "std_msgs/msg/String", QOS, TIMEOUT).close();
            assertThat(exited.get(3, TimeUnit.SECONDS).isAlive()).isFalse();
        }
    }

    @Test void stopErrorDoesNotReplaceCallerFailure() throws Exception {
        try (var transport = runtime("error_stop")) {
            assertThatThrownBy(() -> {
                try (var subscription = transport.subscribe("/a", "std_msgs/msg/String", QOS, TIMEOUT)) {
                    throw new AssertionError("original mismatch");
                }
            }).isInstanceOf(AssertionError.class).hasMessage("original mismatch");
        }
    }

    @Test void closeWakesWaitingSubscriberAndRejectsFurtherWork() throws Exception {
        var transport = runtime("runtime");
        var subscription = transport.subscribe("/a", "std_msgs/msg/String", QOS, TIMEOUT);
        transport.close();
        transport.close();
        assertThatThrownBy(() -> subscription.next(TIMEOUT)).hasMessageStartingWith("ROS transport: ");
        assertThatThrownBy(() -> transport.subscribe("/a", "std_msgs/msg/String", QOS, TIMEOUT)).hasMessageStartingWith("ROS transport: ");
        subscription.close();
    }
}
