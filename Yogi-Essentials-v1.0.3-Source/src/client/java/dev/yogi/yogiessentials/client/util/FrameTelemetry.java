package dev.yogi.yogiessentials.client.util;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_310;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FrameTelemetry {
    private static final int CAPACITY = 60000;
    private static final int RUNTIME_WINDOW_FRAMES = 600;
    private static final double[] FRAMES_MS = new double[CAPACITY];
    private static final double[] RUNTIME_SLOWEST = new double[16];
    private static int next;
    private static int count;
    private static long lastFrameNanos;
    private static long totalFrames;
    private static long lastSnapshotNanos;
    private static Sample cachedSnapshot;

    public record Sample(int frames, double averageFps, double onePercentLow,
                         double averageMs, double varianceMsSquared, int stutters,
                         long usedMemoryMb, long allocatedMemoryMb, long maxMemoryMb,
                         int renderDistance, int simulationDistance,
                         int scaledWidth, int scaledHeight, List<String> bottlenecks) {
        public boolean available() {
            return frames >= 120;
        }
    }

    private FrameTelemetry() {
    }

    public static void initialize() {
        WorldRenderEvents.END_MAIN.register(context -> {
            recordFrame(System.nanoTime());
            FpsCapController.paceFrame();
        });
    }

    private static void recordFrame(long now) {
        if (lastFrameNanos != 0L) {
            double ms = (now - lastFrameNanos) / 1_000_000.0;
            if (ms >= 0.02 && ms <= 1000.0) {
                FRAMES_MS[next] = ms;
                next = (next + 1) % CAPACITY;
                count = Math.min(CAPACITY, count + 1);
                totalFrames++;
            }
        }
        lastFrameNanos = now;
    }

    public static void resetFrameClock() {
        lastFrameNanos = 0L;
        lastSnapshotNanos = 0L;
        cachedSnapshot = null;
    }

    public static Sample snapshot() {
        long now = System.nanoTime();
        if (cachedSnapshot == null || now - lastSnapshotNanos >= 500_000_000L) {
            cachedSnapshot = snapshotRecentFast(RUNTIME_WINDOW_FRAMES);
            lastSnapshotNanos = now;
        }
        return cachedSnapshot;
    }

    /**
     * Allocation-light runtime telemetry. The old runtime path copied and sorted
     * hundreds of frame times several times per second. Benchmarks still use the
     * exact sorted snapshotSince(mark), while adaptive guards use this cheaper
     * one-pass calculation to avoid telemetry becoming a source of microstutter.
     */
    private static Sample snapshotRecentFast(int requestedFrames) {
        class_310 client = class_310.method_1551();
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L;
        long allocated = runtime.totalMemory() / 1_048_576L;
        long max = runtime.maxMemory() / 1_048_576L;
        int render = client.field_1690 == null ? 0 : client.field_1690.method_42503().method_41753();
        int simulation = client.field_1690 == null ? 0 : client.field_1690.method_42510().method_41753();
        int width = client.method_22683().method_4489();
        int height = client.method_22683().method_4506();
        int available = Math.min(count, Math.max(0, requestedFrames));
        if (available == 0) {
            return new Sample(0, 0, 0, 0, 0, 0, used, allocated, max,
                    render, simulation, width, height, List.of());
        }

        double total = 0.0;
        for (int i = 0; i < available; i++) {
            total += FRAMES_MS[(next - available + i + CAPACITY) % CAPACITY];
        }
        double mean = total / available;
        double variance = 0.0;
        int spikes = 0;
        int slowCount = Math.min(RUNTIME_SLOWEST.length,
                Math.max(1, (int) Math.ceil(available * 0.01)));
        Arrays.fill(RUNTIME_SLOWEST, 0, slowCount, 0.0);
        for (int i = 0; i < available; i++) {
            double value = FRAMES_MS[(next - available + i + CAPACITY) % CAPACITY];
            double delta = value - mean;
            variance += delta * delta;
            if (value > Math.max(8.0, mean * 3.0)) spikes++;

            if (value > RUNTIME_SLOWEST[0]) {
                RUNTIME_SLOWEST[0] = value;
                for (int j = 1; j < slowCount && RUNTIME_SLOWEST[j - 1] > RUNTIME_SLOWEST[j]; j++) {
                    double t = RUNTIME_SLOWEST[j - 1];
                    RUNTIME_SLOWEST[j - 1] = RUNTIME_SLOWEST[j];
                    RUNTIME_SLOWEST[j] = t;
                }
            }
        }
        double slowTotal = 0.0;
        for (int i = 0; i < slowCount; i++) slowTotal += RUNTIME_SLOWEST[i];

        boolean chunkIssue = render >= 16 && (mean > 12 || spikes > 3);
        boolean resolutionIssue = (long) width * height >= 3_600_000L && mean > 12;
        boolean memoryIssue = max > 0 && used > max * 0.8;
        boolean pacingIssue = spikes >= Math.max(3, available / 100);
        List<String> detected;
        if (!chunkIssue && !resolutionIssue && !memoryIssue && !pacingIssue) {
            detected = List.of();
        } else {
            ArrayList<String> issues = new ArrayList<>(4);
            if (chunkIssue) issues.add("Chunk rendering may contribute");
            if (resolutionIssue) issues.add("High resolution may contribute");
            if (memoryIssue) issues.add("JVM memory pressure");
            if (pacingIssue) issues.add("Frame pacing spikes");
            detected = List.copyOf(issues);
        }

        double slowMean = slowTotal / slowCount;
        return new Sample(available, 1000.0 / mean, slowMean > 0.0 ? 1000.0 / slowMean : 0.0,
                mean, variance / available, spikes, used, allocated, max,
                render, simulation, width, height, detected);
    }

    public static long mark() {
        return totalFrames;
    }

    /** Stable identifier for the frame currently being submitted. */
    public static long frameId() {
        return totalFrames;
    }

    public static Sample snapshotSince(long mark) {
        class_310 client = class_310.method_1551();
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L;
        long allocated = runtime.totalMemory() / 1_048_576L;
        long max = runtime.maxMemory() / 1_048_576L;
        int render = client.field_1690 == null ? 0 : client.field_1690.method_42503().method_41753();
        int simulation = client.field_1690 == null ? 0 : client.field_1690.method_42510().method_41753();
        int width = client.method_22683().method_4489();
        int height = client.method_22683().method_4506();
        int available = (int) Math.min(count, Math.max(0, totalFrames - mark));
        if (available == 0) {
            return new Sample(0, 0, 0, 0, 0, 0, used, allocated, max,
                    render, simulation, width, height, List.of());
        }
        double[] values = new double[available];
        for (int i = 0; i < available; i++) {
            values[i] = FRAMES_MS[(next - available + i + CAPACITY) % CAPACITY];
        }
        double total = 0;
        for (double value : values) {
            total += value;
        }
        double mean = total / available;
        double variance = 0;
        int spikes = 0;
        for (double value : values) {
            variance += (value - mean) * (value - mean);
            if (value > Math.max(8.0, mean * 3.0)) {
                spikes++;
            }
        }
        Arrays.sort(values);
        int slowCount = Math.max(1, (int) Math.ceil(available * 0.01));
        double slowTotal = 0;
        for (int i = available - slowCount; i < available; i++) {
            slowTotal += values[i];
        }
        List<String> detected = new ArrayList<>();
        if (render >= 16 && (mean > 12 || spikes > 3)) {
            detected.add("Chunk rendering may contribute");
        }
        if ((long) width * height >= 3_600_000L && mean > 12) {
            detected.add("High resolution may contribute");
        }
        if (max > 0 && used > max * 0.8) {
            detected.add("JVM memory pressure");
        }
        if (spikes >= Math.max(3, available / 100)) {
            detected.add("Frame pacing spikes");
        }
        return new Sample(available, 1000.0 / mean, 1000.0 / (slowTotal / slowCount),
                mean, variance / available, spikes, used, allocated, max,
                render, simulation, width, height, List.copyOf(detected));
    }
}
