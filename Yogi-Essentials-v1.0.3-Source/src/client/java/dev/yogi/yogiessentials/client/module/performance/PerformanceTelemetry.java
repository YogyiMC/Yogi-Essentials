package dev.yogi.yogiessentials.client.module.performance;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_310;
import net.minecraft.class_315;

/** Shared, low-overhead real-frame telemetry for the dashboard and optimizer. */
public final class PerformanceTelemetry {

    private static final int CAPACITY = 1_200;
    private static final double[] FRAME_TIMES_MS = new double[CAPACITY];

    private static int nextIndex;
    private static int sampleCount;
    private static long lastFrameNanos;

    private PerformanceTelemetry() {
    }

    public static void recordFrame() {
        long now = System.nanoTime();
        long previous = lastFrameNanos;
        lastFrameNanos = now;

        if (previous == 0L) {
            return;
        }

        double millis = (now - previous) / 1_000_000.0;
        if (!Double.isFinite(millis) || millis <= 0.0 || millis > 1_000.0) {
            return;
        }

        FRAME_TIMES_MS[nextIndex] = millis;
        nextIndex = (nextIndex + 1) % CAPACITY;
        sampleCount = Math.min(CAPACITY, sampleCount + 1);
    }

    public static Snapshot snapshot() {
        class_310 client = class_310.method_1551();
        int count = sampleCount;
        double[] values = new double[count];
        for (int i = 0; i < count; i++) {
            int index = (nextIndex - count + i + CAPACITY) % CAPACITY;
            values[i] = FRAME_TIMES_MS[index];
        }

        double averageMs = 0.0;
        double maximumMs = 0.0;
        for (double value : values) {
            averageMs += value;
            maximumMs = Math.max(maximumMs, value);
        }
        averageMs = count == 0 ? 0.0 : averageMs / count;

        double variance = 0.0;
        for (double value : values) {
            double difference = value - averageMs;
            variance += difference * difference;
        }
        variance = count == 0 ? 0.0 : variance / count;

        double onePercentLow = 0.0;
        int stutters = 0;
        int majorStutters = 0;
        if (count > 0) {
            double[] sorted = Arrays.copyOf(values, count);
            Arrays.sort(sorted);
            int worstFrames = Math.max(1, (int) Math.ceil(count * 0.01));
            double worstAverageMs = 0.0;
            for (int i = count - worstFrames; i < count; i++) {
                worstAverageMs += sorted[i];
            }
            worstAverageMs /= worstFrames;
            onePercentLow = fpsFromMs(worstAverageMs);

            double median = sorted[count / 2];
            double stutterThreshold = Math.max(33.3, median * 2.5);
            for (double value : values) {
                if (value >= stutterThreshold) {
                    stutters++;
                }
                if (value >= 100.0) {
                    majorStutters++;
                }
            }
        }

        Runtime runtime = Runtime.getRuntime();
        long usedMb = toMb(runtime.totalMemory() - runtime.freeMemory());
        long committedMb = toMb(runtime.totalMemory());
        long maxMb = toMb(runtime.maxMemory());

        int currentFps = client == null ? 0 : Math.max(0, client.method_47599());
        int renderDistance = 0;
        int simulationDistance = 0;
        double entityDistance = 0.0;
        int fpsLimit = 0;
        String resolution = "Unavailable";

        if (client != null) {
            if (client.method_22683() != null) {
                resolution = client.method_22683().method_4489()
                        + "x" + client.method_22683().method_4506();
            }
            class_315 options = client.field_1690;
            if (options != null) {
                renderDistance = options.method_42503().method_41753();
                simulationDistance = options.method_42510().method_41753();
                entityDistance = options.method_42517().method_41753();
                fpsLimit = options.method_42524().method_41753();
            }
        }

        double averageFps = fpsFromMs(averageMs);
        double minimumFps = fpsFromMs(maximumMs);
        String bottleneck = detectBottlenecks(
                averageFps,
                onePercentLow,
                variance,
                stutters,
                majorStutters,
                renderDistance,
                simulationDistance,
                entityDistance,
                usedMb,
                maxMb
        );
        String recommendation = recommendationFor(bottleneck);

        return new Snapshot(
                currentFps, averageFps, minimumFps, onePercentLow,
                averageMs, variance, stutters, majorStutters, count,
                usedMb, committedMb, maxMb, resolution,
                renderDistance, simulationDistance, entityDistance, fpsLimit,
                bottleneck, recommendation
        );
    }

    public static void reset() {
        nextIndex = 0;
        sampleCount = 0;
        lastFrameNanos = 0L;
        Arrays.fill(FRAME_TIMES_MS, 0.0);
    }

    private static String detectBottlenecks(
            double averageFps,
            double onePercentLow,
            double variance,
            int stutters,
            int majorStutters,
            int renderDistance,
            int simulationDistance,
            double entityDistance,
            long usedMb,
            long maxMb
    ) {
        List<String> detected = new ArrayList<>();
        double lowRatio = averageFps <= 0.0
                ? 1.0
                : onePercentLow / Math.max(1.0, averageFps);

        if (maxMb > 0 && usedMb / (double) maxMb >= 0.82) {
            detected.add("Memory pressure");
        }

        if (renderDistance >= 16 && averageFps > 0.0
                && (lowRatio < 0.78 || stutters >= 3)) {
            detected.add("Chunk rendering");
        }

        if (simulationDistance >= 10 && averageFps > 0.0
                && averageFps < 100.0) {
            detected.add("CPU / simulation");
        }

        if (entityDistance > 0.75 && averageFps > 0.0
                && averageFps < 100.0) {
            detected.add("Entity rendering");
        }

        if (averageFps > 0.0
                && (lowRatio < 0.72 || variance >= 25.0
                || stutters >= 4 || majorStutters > 0)) {
            detected.add("Frame pacing");
        }

        if (averageFps > 0.0 && averageFps < 60.0) {
            detected.add("Rendering load");
        }

        return detected.isEmpty()
                ? "No strong bottleneck"
                : String.join(" + ", detected);
    }

    private static String recommendationFor(String bottlenecks) {
        if ("No strong bottleneck".equals(bottlenecks)) {
            return "No single limit dominates. Compare 1% lows before and after each reversible optimizer change.";
        }

        List<String> recommendations = new ArrayList<>();
        if (bottlenecks.contains("Memory pressure")) {
            recommendations.add("verify the launcher Game RAM limit and close memory-heavy background apps");
        }
        if (bottlenecks.contains("Chunk rendering")) {
            recommendations.add("reduce chunk rebuild spikes or test a slightly lower render distance");
        }
        if (bottlenecks.contains("CPU / simulation")) {
            recommendations.add("reduce simulation distance before lowering visual quality");
        }
        if (bottlenecks.contains("Entity rendering")) {
            recommendations.add("reduce entity distance or enable compatible entity culling");
        }
        if (bottlenecks.contains("Frame pacing")) {
            recommendations.add("prioritize 1% lows and chunk stability");
        }
        if (bottlenecks.contains("Rendering load")) {
            recommendations.add("reduce safe visual costs with Auto Optimizer");
        }

        return "Recommended: " + String.join("; ", recommendations) + ".";
    }

    private static double fpsFromMs(double millis) {
        return millis <= 0.0 ? 0.0 : 1_000.0 / millis;
    }

    private static long toMb(long bytes) {
        return Math.max(0L, bytes / (1024L * 1024L));
    }

    public static String format(double value, int decimals) {
        return String.format(Locale.ROOT, "% ." + decimals + "f", value).trim();
    }

    public record Snapshot(
            int currentFps,
            double averageFps,
            double minimumFps,
            double onePercentLowFps,
            double averageFrameTimeMs,
            double frameTimeVariance,
            int stutters,
            int majorStutters,
            int samples,
            long heapUsedMb,
            long heapCommittedMb,
            long gameMemoryLimitMb,
            String resolution,
            int renderDistance,
            int simulationDistance,
            double entityDistance,
            int fpsLimit,
            String bottleneck,
            String recommendation
    ) {
    }
}
