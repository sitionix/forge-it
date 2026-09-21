package com.sitionix.forgeit.ros.internal.adapter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sitionix.forgeit.ros.api.RosQos;
import com.sitionix.forgeit.ros.internal.port.RosConsumerPort;
import com.sitionix.forgeit.ros.internal.port.RosPublisherPort;
import com.sitionix.forgeit.ros.internal.port.RosSubscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/** One bounded, JSONL-only child process per context. Runtime text is never propagated. */
public final class PythonRosTransport implements RosPublisherPort, RosConsumerPort, AutoCloseable {

    private static final int MAX_FRAME = 1024 * 1024;
    private static final int MAX_PENDING = 128;
    private static final Set<String> CODES = Set.of("INVALID_COMMAND", "INVALID_QOS", "INVALID_MESSAGE",
            "MESSAGE_TYPE_UNAVAILABLE", "ROS_ERROR", "PUBLISH_TIMEOUT", "RESOURCE_LIMIT",
            "FRAME_TOO_LARGE", "QUEUE_OVERFLOW", "INVALID_FRAME", "SHUTDOWN_FAILED");

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CompletableFuture<Void>> pending = new ConcurrentHashMap<>();
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final AtomicLong requestIds = new AtomicLong();
    private final AtomicLong subscriptionIds = new AtomicLong();
    private final CompletableFuture<Void> startup = new CompletableFuture<>();
    private final CompletableFuture<Void> readerStopped = new CompletableFuture<>();
    private final ThreadPoolExecutor writer = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING), runnable -> daemon(runnable, "forgeit-ros-writer"));
    private volatile String terminal;
    private volatile boolean closing;
    private volatile boolean shutdownAcknowledged;
    private volatile String shutdownRequestId;
    private final Duration shutdownTimeout;
    private final Object closeLock = new Object();
    private boolean closeCompleted;
    private Process process;
    private Path extracted;

    public PythonRosTransport(
            final String pythonCommand,
            final Duration startupTimeout,
            final Duration shutdownTimeout,
            final String domainId) {
        this(pythonCommand, startupTimeout, shutdownTimeout, domainId, null);
    }

    PythonRosTransport(
            final String pythonCommand,
            final Duration startupTimeout,
            final Duration shutdownTimeout,
            final String domainId,
            Path adapter) {
        if (shutdownTimeout == null || shutdownTimeout.isZero() || shutdownTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "forge-it.modules.ros.shutdown-timeout must be positive");
        }
        this.shutdownTimeout = shutdownTimeout;
        final long deadline = deadline(startupTimeout);
        try {
            if (pythonCommand == null || pythonCommand.isBlank()) {
                throw failure("INVALID_CONFIGURATION");
            }
            if (domainId != null
                    && !domainId.isBlank()
                    && (!domainId.matches("[0-9]{1,3}") || Integer.parseInt(domainId) > 232)) {
                throw failure("INVALID_CONFIGURATION");
            }
            if (adapter == null) {
                this.extracted = Files.createTempFile("forgeit-ros-", ".py");
                try (final InputStream source =
                        PythonRosTransport.class.getResourceAsStream(
                                "/com/sitionix/forgeit/ros/adapter.py")) {
                    if (source == null) {
                        throw failure("ADAPTER_UNAVAILABLE");
                    }
                    Files.copy(source, this.extracted, StandardCopyOption.REPLACE_EXISTING);
                }
                adapter = this.extracted;
            }
            final ProcessBuilder builder =
                    new ProcessBuilder(pythonCommand, "-u", adapter.toAbsolutePath().toString());
            if (domainId != null && !domainId.isBlank()) {
                builder.environment().put("ROS_DOMAIN_ID", domainId);
            }
            this.process = builder.start();
            final Thread reader = daemon(this::read, "forgeit-ros-reader");
            final Thread stderr =
                    daemon(
                            () -> {
                                try (final InputStream stream = this.process.getErrorStream()) {
                                    final byte[] buffer = new byte[4096];
                                    while (stream.read(buffer) != -1) {
                                        /* Deliberately discard diagnostics. */
                                    }
                                } catch (final IOException ignored) {
                                }
                            },
                            "forgeit-ros-stderr");
            reader.start();
            stderr.start();
            this.await(this.startup, deadline, "STARTUP_TIMEOUT");
            this.checkOpen();
        } catch (final Exception error) {
            final String code = this.terminal == null ? "STARTUP_FAILED" : this.terminal;
            this.terminate(code);
            throw failure(code);
        }
    }

    @Override
    public RosSubscription subscribe(
            final String topic,
            final String messageType,
            final RosQos qos,
            final Duration timeout) {
        final long deadline = deadline(timeout);
        final Subscription subscription;
        synchronized (this) {
            this.checkOpen();
            if (this.subscriptions.size() >= MAX_PENDING) {
                throw failure("RESOURCE_LIMIT");
            }
            final String id = "s" + this.subscriptionIds.incrementAndGet();
            subscription = new Subscription(id);
            this.subscriptions.put(id, subscription);
        }
        final ObjectNode command = this.command("START_SUBSCRIPTION", topic, messageType, qos);
        command.put("subscriptionId", subscription.id);
        try {
            this.request(command, deadline);
            return subscription;
        } catch (final RuntimeException error) {
            this.subscriptions.remove(subscription.id);
            subscription.fail("SUBSCRIPTION_FAILED");
            throw error;
        }
    }

    @Override
    public void publish(
            final String topic,
            final String messageType,
            final RosQos qos,
            final JsonNode message,
            final Duration timeout) {
        final long deadline = deadline(timeout);
        final ObjectNode command = this.command("PUBLISH", topic, messageType, qos);
        command.set("message", message);
        command.put("timeoutMs", Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining(deadline))));
        this.request(command, deadline);
    }

    private ObjectNode command(
            final String type, final String topic, final String messageType, final RosQos qos) {
        final ObjectNode command =
                this.mapper
                        .createObjectNode()
                        .put("type", type)
                        .put("topic", topic)
                        .put("messageType", messageType);
        command.putObject("qos")
                .put("reliability", qos.reliability().name())
                .put("durability", qos.durability().name())
                .put("history", qos.history().name())
                .put("depth", qos.depth());
        return command;
    }

    private void request(final ObjectNode command, final long deadline) {
        this.await(this.sendRequest(command), deadline, "REQUEST_TIMEOUT");
    }

    // Registration and enqueue share a lock, so command IDs and wire order agree.
    private synchronized CompletableFuture<Void> sendRequest(final ObjectNode command) {
        final boolean shutdown = "SHUTDOWN".equals(command.path("type").asText());
        if (this.terminal != null) {
            throw failure(this.terminal);
        }
        if (this.closing && !shutdown) {
            throw failure("CLOSING");
        }
        if (this.pending.size() >= MAX_PENDING) {
            throw failure("RESOURCE_LIMIT");
        }
        final CompletableFuture<Void> response = new CompletableFuture<>();
        final String id = "r" + this.requestIds.incrementAndGet();
        command.put("id", id);
        if (shutdown) {
            this.shutdownRequestId = id;
        }
        this.pending.put(id, response);
        response.whenComplete((ignored, error) -> this.pending.remove(id, response));
        try {
            final byte[] frame = this.mapper.writeValueAsBytes(command);
            if (frame.length > MAX_FRAME) {
                throw failure("FRAME_TOO_LARGE");
            }
            this.writer.execute(
                    () -> {
                        if (this.terminal != null) {
                            return;
                        }
                        try {
                            this.process.getOutputStream().write(frame);
                            this.process.getOutputStream().write('\n');
                            this.process.getOutputStream().flush();
                            if (shutdown) {
                                this.process.getOutputStream().close();
                            }
                        } catch (final IOException ignored) {
                            this.terminate("PROCESS_IO");
                        }
                    });
            return response;
        } catch (final RejectedExecutionException error) {
            response.completeExceptionally(failure("RESOURCE_LIMIT"));
            throw failure("RESOURCE_LIMIT");
        } catch (final IOException error) {
            response.completeExceptionally(failure("INVALID_MESSAGE"));
            throw failure("INVALID_MESSAGE");
        } catch (final RuntimeException error) {
            response.completeExceptionally(failure("INVALID_MESSAGE"));
            throw error;
        }
    }

    private void await(
            final CompletableFuture<Void> response, final long deadline, final String timeoutCode) {
        try {
            response.get(remaining(deadline), TimeUnit.NANOSECONDS);
        } catch (final TimeoutException error) {
            this.terminate(timeoutCode);
            throw failure(timeoutCode);
        } catch (final InterruptedException error) {
            Thread.currentThread().interrupt();
            this.terminate("INTERRUPTED");
            throw failure("INTERRUPTED");
        } catch (final ExecutionException error) {
            if (error.getCause() instanceof final IllegalStateException safe) {
                throw safe;
            }
            throw failure("ROS_ERROR");
        }
    }

    private void read() {
        try (final InputStream stream = this.process.getInputStream()) {
            final ByteArrayOutputStream frame = new ByteArrayOutputStream();
            int next;
            while ((next = stream.read()) != -1) {
                if (next == '\n') {
                    final String json =
                            StandardCharsets.UTF_8
                                    .newDecoder()
                                    .onMalformedInput(CodingErrorAction.REPORT)
                                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                                    .decode(ByteBuffer.wrap(frame.toByteArray()))
                                    .toString();
                    this.accept(
                            this.mapper
                                    .reader()
                                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                                    .readTree(json));
                    frame.reset();
                } else {
                    if (frame.size() >= MAX_FRAME) {
                        this.terminate("FRAME_TOO_LARGE");
                        return;
                    }
                    frame.write(next);
                }
            }
            // Closing the protocol output is part of normal adapter teardown. Let
            // close() wait for process exit rather than killing Python mid-finalization.
            if (!this.closing || frame.size() != 0) {
                this.terminate(frame.size() == 0 ? "PROCESS_EXITED" : "INVALID_FRAME");
            } else if (!this.shutdownAcknowledged && this.shutdownRequestId != null) {
                final CompletableFuture<Void> completion = this.pending.get(this.shutdownRequestId);
                if (completion != null) {
                    completion.completeExceptionally(failure("SHUTDOWN_INCOMPLETE"));
                }
            }
        } catch (final Exception ignored) {
            this.terminate("INVALID_FRAME");
        } finally {
            this.readerStopped.complete(null);
        }
    }

    private void accept(final JsonNode frame) {
        if (frame == null || !frame.isObject() || !frame.path("type").isTextual()) {
            this.terminate("INVALID_FRAME");
            return;
        }
        final String type = frame.path("type").asText();
        final String id = frame.path("id").asText("");
        switch (type) {
            case "SHUTDOWN_COMPLETE" -> {
                final CompletableFuture<Void> future = this.pending.get(id);
                if (!this.closing || !id.equals(this.shutdownRequestId) || future == null) {
                    this.terminate("INVALID_FRAME");
                    return;
                }
                this.shutdownAcknowledged = true;
                future.complete(null);
            }
            case "READY" -> {
                if (id.equals(this.shutdownRequestId)) {
                    this.terminate("INVALID_FRAME");
                    return;
                }
                if ("0".equals(id)) {
                    this.startup.complete(null);
                } else {
                    final CompletableFuture<Void> future = this.pending.get(id);
                    if (future == null) {
                        this.terminate("INVALID_FRAME");
                        return;
                    }
                    future.complete(null);
                }
            }
            case "MESSAGE" -> {
                final Subscription subscription =
                        this.subscriptions.get(frame.path("subscriptionId").asText(""));
                if (!frame.path("message").isObject()) {
                    this.terminate("INVALID_FRAME");
                    return;
                }
                if (subscription != null) {
                    subscription.offer(frame.get("message"));
                }
            }
            case "ERROR" -> {
                String code = frame.path("code").asText("");
                if (!CODES.contains(code)) {
                    code = "ROS_ERROR";
                }
                if ("0".equals(id)) {
                    this.terminate(code);
                    return;
                }
                final CompletableFuture<Void> future = this.pending.get(id);
                final Subscription subscription =
                        this.subscriptions.get(frame.path("subscriptionId").asText(""));
                if (future != null) {
                    future.completeExceptionally(failure(code));
                } else if (subscription != null) {
                    subscription.fail(code);
                } else {
                    this.terminate("INVALID_FRAME");
                }
            }
            default -> this.terminate("INVALID_FRAME");
        }
    }

    private void checkOpen() {
        if (this.terminal != null) {
            throw failure(this.terminal);
        }
        if (this.closing) {
            throw failure("CLOSING");
        }
    }

    private static IllegalStateException failure(final String code) {
        return new IllegalStateException("ROS transport: " + code);
    }

    private static long deadline(final Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw failure("INVALID_TIMEOUT");
        }
        long nanos;
        try {
            nanos = timeout.toNanos();
        } catch (final ArithmeticException error) {
            nanos = Long.MAX_VALUE / 2;
        }
        return System.nanoTime() + Math.min(nanos, Long.MAX_VALUE / 2);
    }

    private static long remaining(final long deadline) {
        return Math.max(0, deadline - System.nanoTime());
    }

    private static Thread daemon(final Runnable action, final String name) {
        final Thread thread = new Thread(action, name);
        thread.setDaemon(true);
        return thread;
    }

    private void terminate(final String code) {
        synchronized (this) {
            if (this.terminal != null) {
                return;
            }
            this.terminal = code;
            this.startup.completeExceptionally(failure(code));
            this.pending.values().forEach(future -> future.completeExceptionally(failure(code)));
            this.subscriptions.values().forEach(subscription -> subscription.fail(code));
            this.pending.clear();
            this.subscriptions.clear();
        }
        this.writer.shutdownNow();
        if (this.process == null) {
            this.deleteAdapter();
            return;
        }
        this.process.destroyForcibly();
        this.process
                .onExit()
                .thenRun(
                        () -> {
                            try {
                                this.process.getInputStream().close();
                            } catch (final IOException ignored) {
                            }
                            try {
                                this.process.getErrorStream().close();
                            } catch (final IOException ignored) {
                            }
                            try {
                                this.process.getOutputStream().close();
                            } catch (final IOException ignored) {
                            }
                            this.deleteAdapter();
                        });
    }

    private void deleteAdapter() {
        if (this.extracted != null) {
            try {
                Files.deleteIfExists(this.extracted);
            } catch (final IOException ignored) {
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.closeLock) {
            if (this.closeCompleted) {
                return;
            }
            final long shutdownDeadline = deadline(this.shutdownTimeout);
            String phase = "cleanup-confirmation";
            IllegalStateException shutdownError = null;
            try {
                if (this.terminal != null) {
                    if (!"CLOSED".equals(this.terminal)) {
                        phase = "transport";
                        throw failure(this.terminal);
                    }
                    return;
                }
                final CompletableFuture<Void> completion;
                synchronized (this) {
                    this.closing = true;
                    completion =
                            this.sendRequest(
                                    this.mapper.createObjectNode().put("type", "SHUTDOWN"));
                }
                // Both protocol completion and exit share the same monotonic deadline.
                completion.get(remaining(shutdownDeadline), TimeUnit.NANOSECONDS);
                phase = "process-exit";
                if (!this.process.waitFor(remaining(shutdownDeadline), TimeUnit.NANOSECONDS)) {
                    throw new TimeoutException();
                }
                if (this.process.exitValue() != 0) {
                    shutdownError =
                            this.shutdownFailure(phase, "NONZERO_EXIT_" + this.process.exitValue());
                }
                phase = "protocol-completion";
                this.readerStopped.get(remaining(shutdownDeadline), TimeUnit.NANOSECONDS);
                if (shutdownError == null && this.terminal != null) {
                    shutdownError = this.shutdownFailure(phase, this.terminal);
                }
            } catch (final TimeoutException error) {
                shutdownError = this.shutdownFailure(phase, "TIMEOUT");
            } catch (final InterruptedException error) {
                Thread.currentThread().interrupt();
                shutdownError = this.shutdownFailure(phase, "INTERRUPTED");
            } catch (final ExecutionException error) {
                shutdownError = this.shutdownFailure(phase, "ADAPTER_FAILED");
            } catch (final IllegalStateException error) {
                shutdownError =
                        this.shutdownFailure(
                                phase, this.terminal == null ? "TRANSPORT_FAILED" : this.terminal);
            } finally {
                this.terminate("CLOSED");
                this.closeCompleted = true;
            }
            if (shutdownError != null) {
                throw shutdownError;
            }
        }
    }

    private IllegalStateException shutdownFailure(final String phase, final String reason) {
        return new IllegalStateException("ROS shutdown failed: phase=" + phase + "; reason=" + reason
                + "; shutdown-timeout=" + this.shutdownTimeout);
    }

    private final class Subscription implements RosSubscription {

        private final String id;
        private final ArrayDeque<JsonNode> queue = new ArrayDeque<>();
        private String error;
        private boolean closed;

        private Subscription(final String id) {
            this.id = id;
        }

        synchronized void offer(final JsonNode message) {
            if (this.closed || this.error != null) {
                return;
            }
            if (this.queue.size() == 64) {
                this.fail("QUEUE_OVERFLOW");
                return;
            }
            this.queue.addLast(message);
            this.notifyAll();
        }

        synchronized void fail(final String code) {
            if (this.error == null) {
                this.error = code;
            }
            this.queue.clear();
            this.notifyAll();
        }

        @Override
        public synchronized JsonNode next(final Duration timeout) {
            final long deadline = deadline(timeout);
            while (this.queue.isEmpty() && this.error == null && !this.closed) {
                final long left = remaining(deadline);
                if (left == 0) {
                    return null;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(this, left);
                } catch (final InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    throw failure("INTERRUPTED");
                }
            }
            if (this.error != null) {
                throw failure(this.error);
            }
            if (this.closed) {
                throw failure("SUBSCRIPTION_CLOSED");
            }
            return this.queue.removeFirst();
        }

        @Override
        public void close() {
            synchronized (this) {
                if (this.closed) {
                    return;
                }
                this.closed = true;
                this.queue.clear();
                this.notifyAll();
            }
            PythonRosTransport.this.subscriptions.remove(this.id);
            synchronized (PythonRosTransport.this) {
                if (PythonRosTransport.this.terminal != null || PythonRosTransport.this.closing) {
                    return;
                }
                try {
                    // STOP shares shutdown's lock, so it cannot be enqueued after SHUTDOWN.
                    PythonRosTransport.this.sendRequest(PythonRosTransport.this.mapper.createObjectNode()
                                    .put("type", "STOP_SUBSCRIPTION").put("subscriptionId", this.id))
                            .orTimeout(remaining(deadline(PythonRosTransport.this.shutdownTimeout)),
                                    TimeUnit.NANOSECONDS)
                            .whenCompleteAsync((ignored, error) -> {
                                if (error != null) {
                                    PythonRosTransport.this.terminate("SUBSCRIPTION_STOP_FAILED");
                                }
                            });
                } catch (final RuntimeException ignored) {
                    CompletableFuture.runAsync(
                            () -> PythonRosTransport.this.terminate("SUBSCRIPTION_STOP_FAILED"));
                }
            }
        }
    }
}
