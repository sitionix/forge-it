package com.sitionix.forgeit.ros.internal.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sitionix.forgeit.ros.api.RosQos;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** One bounded, JSONL-only child process per context. Runtime text is never propagated. */
public final class PythonRosTransport implements RosTransport {
    private static final int MAX_FRAME = 1024 * 1024;
    private static final int MAX_PENDING = 128;
    private static final Set<String> CODES = Set.of("INVALID_COMMAND", "INVALID_QOS", "INVALID_MESSAGE",
            "MESSAGE_TYPE_UNAVAILABLE", "ROS_ERROR", "PUBLISH_TIMEOUT", "RESOURCE_LIMIT",
            "FRAME_TOO_LARGE", "QUEUE_OVERFLOW", "INVALID_FRAME");
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CompletableFuture<Void>> pending = new ConcurrentHashMap<>();
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final AtomicLong requestIds = new AtomicLong();
    private final AtomicLong subscriptionIds = new AtomicLong();
    private final CompletableFuture<Void> startup = new CompletableFuture<>();
    private final ThreadPoolExecutor writer = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING), runnable -> daemon(runnable, "forgeit-ros-writer"));
    private volatile String terminal;
    private volatile boolean closing;
    private Process process;
    private Thread reader;
    private Thread stderr;
    private Path extracted;

    public PythonRosTransport(String pythonCommand, Duration startupTimeout, String domainId) {
        this(pythonCommand, startupTimeout, domainId, null);
    }

    PythonRosTransport(String pythonCommand, Duration startupTimeout, String domainId, Path adapter) {
        long deadline = deadline(startupTimeout);
        try {
            if (pythonCommand == null || pythonCommand.isBlank()) throw failure("INVALID_CONFIGURATION");
            if (domainId != null && !domainId.isBlank() &&
                    (!domainId.matches("[0-9]{1,3}") || Integer.parseInt(domainId) > 232)) {
                throw failure("INVALID_CONFIGURATION");
            }
            if (adapter == null) {
                extracted = Files.createTempFile("forgeit-ros-", ".py");
                try (InputStream source = PythonRosTransport.class.getResourceAsStream("/com/sitionix/forgeit/ros/adapter.py")) {
                    if (source == null) throw failure("ADAPTER_UNAVAILABLE");
                    Files.copy(source, extracted, StandardCopyOption.REPLACE_EXISTING);
                }
                adapter = extracted;
            }
            ProcessBuilder builder = new ProcessBuilder(pythonCommand, "-u", adapter.toAbsolutePath().toString());
            if (domainId != null && !domainId.isBlank()) builder.environment().put("ROS_DOMAIN_ID", domainId);
            process = builder.start();
            reader = daemon(this::read, "forgeit-ros-reader");
            stderr = daemon(() -> {
                try (InputStream stream = process.getErrorStream()) {
                    byte[] buffer = new byte[4096];
                    while (stream.read(buffer) != -1) { /* Deliberately discard diagnostics. */ }
                } catch (IOException ignored) { }
            }, "forgeit-ros-stderr");
            reader.start();
            stderr.start();
            await(startup, deadline, "STARTUP_TIMEOUT");
            checkOpen();
        } catch (Exception error) {
            String code = terminal == null ? "STARTUP_FAILED" : terminal;
            terminate(code);
            throw failure(code);
        }
    }

    @Override public RosSubscription subscribe(String topic, String messageType, RosQos qos, Duration timeout) {
        long deadline = deadline(timeout);
        Subscription subscription;
        synchronized (this) {
            checkOpen();
            if (subscriptions.size() >= MAX_PENDING) throw failure("RESOURCE_LIMIT");
            String id = "s" + subscriptionIds.incrementAndGet();
            subscription = new Subscription(id);
            subscriptions.put(id, subscription);
        }
        ObjectNode command = command("START_SUBSCRIPTION", topic, messageType, qos);
        command.put("subscriptionId", subscription.id);
        try {
            request(command, deadline);
            return subscription;
        } catch (RuntimeException error) {
            subscriptions.remove(subscription.id);
            subscription.fail("SUBSCRIPTION_FAILED");
            throw error;
        }
    }

    @Override public void publish(String topic, String messageType, RosQos qos, JsonNode message, Duration timeout) {
        long deadline = deadline(timeout);
        ObjectNode command = command("PUBLISH", topic, messageType, qos);
        command.set("message", message);
        command.put("timeoutMs", Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining(deadline))));
        request(command, deadline);
    }

    private ObjectNode command(String type, String topic, String messageType, RosQos qos) {
        ObjectNode command = mapper.createObjectNode().put("type", type).put("topic", topic).put("messageType", messageType);
        command.putObject("qos").put("reliability", qos.reliability().name())
                .put("durability", qos.durability().name()).put("history", qos.history().name()).put("depth", qos.depth());
        return command;
    }

    private void request(ObjectNode command, long deadline) {
        await(sendRequest(command), deadline, "REQUEST_TIMEOUT");
    }

    // Registration and enqueue share a lock, so command IDs and wire order agree.
    private synchronized CompletableFuture<Void> sendRequest(ObjectNode command) {
        checkOpen();
        if (pending.size() >= MAX_PENDING) throw failure("RESOURCE_LIMIT");
        CompletableFuture<Void> response = new CompletableFuture<>();
        String id = "r" + requestIds.incrementAndGet();
        command.put("id", id);
        pending.put(id, response);
        response.whenComplete((ignored, error) -> pending.remove(id, response));
        try {
            byte[] frame = mapper.writeValueAsBytes(command);
            if (frame.length > MAX_FRAME) throw failure("FRAME_TOO_LARGE");
            writer.execute(() -> {
                if (terminal != null) return;
                try {
                    process.getOutputStream().write(frame);
                    process.getOutputStream().write('\n');
                    process.getOutputStream().flush();
                } catch (IOException ignored) { terminate("PROCESS_IO"); }
            });
            return response;
        } catch (RejectedExecutionException error) {
            response.completeExceptionally(failure("RESOURCE_LIMIT"));
            throw failure("RESOURCE_LIMIT");
        } catch (IOException error) {
            response.completeExceptionally(failure("INVALID_MESSAGE"));
            throw failure("INVALID_MESSAGE");
        } catch (RuntimeException error) {
            response.completeExceptionally(failure("INVALID_MESSAGE"));
            throw error;
        }
    }

    private void await(CompletableFuture<Void> response, long deadline, String timeoutCode) {
        try { response.get(remaining(deadline), TimeUnit.NANOSECONDS); }
        catch (TimeoutException error) { terminate(timeoutCode); throw failure(timeoutCode); }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt(); terminate("INTERRUPTED"); throw failure("INTERRUPTED");
        } catch (ExecutionException error) {
            if (error.getCause() instanceof IllegalStateException safe) throw safe;
            throw failure("ROS_ERROR");
        }
    }

    private void read() {
        try (InputStream stream = process.getInputStream()) {
            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            int next;
            while ((next = stream.read()) != -1) {
                if (next == '\n') {
                    String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(frame.toByteArray())).toString();
                    accept(mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json));
                    frame.reset();
                } else {
                    if (frame.size() >= MAX_FRAME) { terminate("FRAME_TOO_LARGE"); return; }
                    frame.write(next);
                }
            }
            // Closing the protocol output is part of normal adapter teardown. Let
            // close() wait for process exit rather than killing Python mid-finalization.
            if (!closing || frame.size() != 0) {
                terminate(frame.size() == 0 ? "PROCESS_EXITED" : "INVALID_FRAME");
            }
        } catch (Exception ignored) { terminate("INVALID_FRAME"); }
    }

    private void accept(JsonNode frame) {
        if (frame == null || !frame.isObject() || !frame.path("type").isTextual()) {
            terminate("INVALID_FRAME"); return;
        }
        String type = frame.path("type").asText();
        String id = frame.path("id").asText("");
        switch (type) {
            case "READY" -> {
                if ("0".equals(id)) startup.complete(null);
                else {
                    CompletableFuture<Void> future = pending.get(id);
                    if (future == null) { terminate("INVALID_FRAME"); return; }
                    future.complete(null);
                }
            }
            case "MESSAGE" -> {
                Subscription subscription = subscriptions.get(frame.path("subscriptionId").asText(""));
                if (!frame.path("message").isObject()) { terminate("INVALID_FRAME"); return; }
                if (subscription != null) subscription.offer(frame.get("message"));
            }
            case "ERROR" -> {
                String code = frame.path("code").asText("");
                if (!CODES.contains(code)) code = "ROS_ERROR";
                if ("0".equals(id)) { terminate(code); return; }
                CompletableFuture<Void> future = pending.get(id);
                Subscription subscription = subscriptions.get(frame.path("subscriptionId").asText(""));
                if (future != null) future.completeExceptionally(failure(code));
                else if (subscription != null) subscription.fail(code);
                else terminate("INVALID_FRAME");
            }
            default -> terminate("INVALID_FRAME");
        }
    }

    private void checkOpen() { if (terminal != null) throw failure(terminal); }
    private static IllegalStateException failure(String code) { return new IllegalStateException("ROS transport: " + code); }
    private static long deadline(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) throw failure("INVALID_TIMEOUT");
        long nanos;
        try { nanos = timeout.toNanos(); } catch (ArithmeticException error) { nanos = Long.MAX_VALUE / 2; }
        return System.nanoTime() + Math.min(nanos, Long.MAX_VALUE / 2);
    }
    private static long remaining(long deadline) { return Math.max(0, deadline - System.nanoTime()); }
    private static Thread daemon(Runnable action, String name) { Thread thread = new Thread(action, name); thread.setDaemon(true); return thread; }

    private void terminate(String code) {
        synchronized (this) {
            if (terminal != null) return;
            terminal = code;
            startup.completeExceptionally(failure(code));
            pending.values().forEach(future -> future.completeExceptionally(failure(code)));
            subscriptions.values().forEach(subscription -> subscription.fail(code));
            pending.clear();
            subscriptions.clear();
        }
        writer.shutdownNow();
        if (process != null) {
            process.destroyForcibly();
            try { process.waitFor(100, TimeUnit.MILLISECONDS); }
            catch (InterruptedException ignored) { process.destroyForcibly(); Thread.currentThread().interrupt(); }
            try { process.getInputStream().close(); } catch (IOException ignored) { }
            try { process.getErrorStream().close(); } catch (IOException ignored) { }
            try { process.getOutputStream().close(); } catch (IOException ignored) { }
        }
        if (extracted != null) try { Files.deleteIfExists(extracted); } catch (IOException ignored) { }
    }

    @Override public void close() {
        if (terminal == null) {
            closing = true;
            long shutdownDeadline = deadline(Duration.ofSeconds(2));
            try {
                request(mapper.createObjectNode().put("type", "SHUTDOWN"), deadline(Duration.ofMillis(250)));
                // Release the Python command-reader thread before interpreter teardown.
                process.getOutputStream().close();
                process.waitFor(remaining(shutdownDeadline), TimeUnit.NANOSECONDS);
            } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            catch (IOException | RuntimeException ignored) { }
        }
        terminate("CLOSED");
        join(reader);
        join(stderr);
    }

    private static void join(Thread thread) {
        if (thread == null || thread == Thread.currentThread()) return;
        try { thread.join(200); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private final class Subscription implements RosSubscription {
        private final String id;
        private final ArrayDeque<JsonNode> queue = new ArrayDeque<>();
        private String error;
        private boolean closed;
        private Subscription(String id) { this.id = id; }
        synchronized void offer(JsonNode message) {
            if (closed || error != null) return;
            if (queue.size() == 64) { fail("QUEUE_OVERFLOW"); return; }
            queue.addLast(message); notifyAll();
        }
        synchronized void fail(String code) { if (error == null) error = code; queue.clear(); notifyAll(); }
        @Override public synchronized JsonNode next(Duration timeout) {
            long deadline = deadline(timeout);
            while (queue.isEmpty() && error == null && !closed) {
                long left = remaining(deadline);
                if (left == 0) return null;
                try { TimeUnit.NANOSECONDS.timedWait(this, left); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); throw failure("INTERRUPTED"); }
            }
            if (error != null) throw failure(error);
            if (closed) throw failure("SUBSCRIPTION_CLOSED");
            return queue.removeFirst();
        }
        @Override public void close() {
            synchronized (this) { if (closed) return; closed = true; queue.clear(); notifyAll(); }
            subscriptions.remove(id);
            if (terminal != null) return;
            try {
                // Cleanup must not consume the caller's assertion deadline or replace its result.
                sendRequest(mapper.createObjectNode().put("type", "STOP_SUBSCRIPTION").put("subscriptionId", id))
                        .orTimeout(1, TimeUnit.SECONDS)
                        .whenCompleteAsync((ignored, error) -> {
                            if (error != null) terminate("SUBSCRIPTION_STOP_FAILED");
                        });
            } catch (RuntimeException ignored) {
                CompletableFuture.runAsync(() -> terminate("SUBSCRIPTION_STOP_FAILED"));
            }
        }
    }
}
