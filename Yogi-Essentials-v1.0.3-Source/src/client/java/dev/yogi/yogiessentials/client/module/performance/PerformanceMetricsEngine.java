package dev.yogi.yogiessentials.client.module.performance;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_4063;
import net.minecraft.class_4066;
import net.minecraft.class_5365;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Shared, conservative performance telemetry used by the profiler, Auto Optimizer 2.0,
 * and the frame-pacing optimizer. It intentionally does not invent GPU utilization.
 */
public final class PerformanceMetricsEngine {
    private static final long SAMPLE_RETENTION_NANOS = 30_000_000_000L;
    private static final int MAX_SAMPLES = 7_200;
    private static final double MAX_VALID_FRAME_MS = 1_000.0;
    private static final double MIN_VALID_FRAME_MS = 0.05;

    private static final Deque<FrameSample> SAMPLES = new ArrayDeque<>();

    private static boolean initialized;
    private static long lastFrameNanos;
    private static int lastCompletedChunks = -1;
    private static int chunkCountChanges;
    private static long chunkActivityWindowStartedNanos;
    private static double recentChunkActivityPerSecond;

    private PerformanceMetricsEngine() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        WorldRenderEvents.END_MAIN.register(context -> recordWorldFrame());
        ClientTickEvents.END_CLIENT_TICK.register(PerformanceMetricsEngine::sampleChunkActivity);
    }

    private static synchronized void recordWorldFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos > 0L) {
            double frameMs = (now - lastFrameNanos) / 1_000_000.0;
            if (frameMs >= MIN_VALID_FRAME_MS && frameMs <= MAX_VALID_FRAME_MS) {
                SAMPLES.addLast(new FrameSample(now, frameMs));
            }
        }
        lastFrameNanos = now;
        trim(now);
    }

    private static synchronized void sampleChunkActivity(class_310 client) {
        if (client == null || client.field_1687 == null || client.field_1769 == null) {
            lastCompletedChunks = -1;
            chunkCountChanges = 0;
            chunkActivityWindowStartedNanos = 0L;
            recentChunkActivityPerSecond = 0.0;
            return;
        }

        int completed;
        try {
            completed = client.field_1769.method_3246();
        } catch (Throwable ignored) {
            return;
        }

        long now = System.nanoTime();
        if (chunkActivityWindowStartedNanos == 0L) {
            chunkActivityWindowStartedNanos = now;
        }

        if (lastCompletedChunks >= 0 && completed != lastCompletedChunks) {
            chunkCountChanges += Math.abs(completed - lastCompletedChunks);
        }
        lastCompletedChunks = completed;

        long elapsed = now - chunkActivityWindowStartedNanos;
        if (elapsed >= 2_000_000_000L) {
            recentChunkActivityPerSecond = chunkCountChanges / (elapsed / 1_000_000_000.0);
            chunkCountChanges = 0;
            chunkActivityWindowStartedNanos = now;
        }
    }

    public static BenchmarkSession beginBenchmark() {
        return new BenchmarkSession(System.nanoTime());
    }

    public static MetricsSnapshot snapshot() {
        return snapshot(10_000L);
    }

    public static MetricsSnapshot snapshot(long windowMillis) {
        long now = System.nanoTime();
        long start = now - Math.max(250L, windowMillis) * 1_000_000L;
        return snapshotSinceNanos(start, now);
    }

    private static synchronized MetricsSnapshot snapshotSinceNanos(long startNanos, long nowNanos) {
        trim(nowNanos);
        List<Double> frameTimes = new ArrayList<>();
        for (FrameSample sample : SAMPLES) {
            if (sample.timestampNanos() >= startNanos) {
                frameTimes.add(sample.frameTimeMs());
            }
        }
        return buildSnapshot(frameTimes);
    }

    private static MetricsSnapshot buildSnapshot(List<Double> frameTimes) {
        class_310 client = class_310.method_1551();
        class_315 options = client == null ? null : client.field_1690;

        double averageFrameMs = 0.0;
        double variance = 0.0;
        double averageFps = 0.0;
        double onePercentLow = 0.0;
        double minimumFps = 0.0;
        int stutters = 0;
        int majorStutters = 0;

        if (!frameTimes.isEmpty()) {
            double total = 0.0;
            double maxFrame = 0.0;
            for (double value : frameTimes) {
                total += value;
                maxFrame = Math.max(maxFrame, value);
            }
            averageFrameMs = total / frameTimes.size();
            averageFps = averageFrameMs > 0.0 ? 1_000.0 / averageFrameMs : 0.0;
            minimumFps = maxFrame > 0.0 ? 1_000.0 / maxFrame : 0.0;

            for (double value : frameTimes) {
                double diff = value - averageFrameMs;
                variance += diff * diff;
            }
            variance /= frameTimes.size();

            List<Double> sorted = new ArrayList<>(frameTimes);
            sorted.sort(Comparator.reverseOrder());
            int slowCount = Math.max(1, (int) Math.ceil(sorted.size() * 0.01));
            if (sorted.size() >= 100) {
                slowCount = Math.max(2, slowCount);
            }
            slowCount = Math.min(sorted.size(), slowCount);
            double slowTotal = 0.0;
            for (int i = 0; i < slowCount; i++) {
                slowTotal += sorted.get(i);
            }
            double slowAverageMs = slowTotal / slowCount;
            onePercentLow = slowAverageMs > 0.0 ? 1_000.0 / slowAverageMs : 0.0;

            List<Double> medianSorted = new ArrayList<>(frameTimes);
            medianSorted.sort(Comparator.naturalOrder());
            double median = medianSorted.get(medianSorted.size() / 2);
            double stutterThreshold = Math.max(33.34, median * 1.8);
            double majorThreshold = Math.max(50.0, median * 2.75);
            for (double value : frameTimes) {
                if (value >= stutterThreshold) {
                    stutters++;
                }
                if (value >= majorThreshold) {
                    majorStutters++;
                }
            }
        }

        int currentFps = 0;
        if (client != null) {
            try {
                currentFps = client.method_47599();
            } catch (Throwable ignored) {
            }
        }

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = bytesToMb(runtime.totalMemory() - runtime.freeMemory());
        long allocatedMemory = bytesToMb(runtime.totalMemory());
        long maxMemory = bytesToMb(runtime.maxMemory());

        int resolutionWidth = 0;
        int resolutionHeight = 0;
        int refreshRate = 0;
        if (client != null && client.method_22683() != null) {
            resolutionWidth = client.method_22683().method_4489();
            resolutionHeight = client.method_22683().method_4506();
            try {
                refreshRate = client.method_22683().method_22092();
            } catch (Throwable ignored) {
            }
        }

        int renderDistance = 0;
        int simulationDistance = 0;
        double entityDistance = 0.0;
        String particles = "Unavailable";
        String graphics = "Unavailable";
        String clouds = "Unavailable";
        int biomeBlend = 0;
        int fpsLimit = 0;
        boolean vsync = false;
        if (options != null) {
            renderDistance = options.method_42503().method_41753();
            simulationDistance = options.method_42510().method_41753();
            entityDistance = options.method_42517().method_41753();
            class_4066 particleMode = options.method_42475().method_41753();
            class_5365 graphicsMode = options.method_75329().method_41753();
            class_4063 cloudMode = options.method_42528().method_41753();
            particles = pretty(particleMode);
            graphics = pretty(graphicsMode);
            clouds = pretty(cloudMode);
            biomeBlend = options.method_41805().method_41753();
            fpsLimit = options.method_42524().method_41753();
            vsync = options.method_42433().method_41753();
        }

        int loadedEntities = -1;
        int completedChunks = -1;
        String chunkDebug = "Unavailable";
        String entityDebug = "Unavailable";
        if (client != null && client.field_1687 != null) {
            try {
                loadedEntities = client.field_1687.method_18120();
            } catch (Throwable ignored) {
            }
            if (client.field_1769 != null) {
                try {
                    completedChunks = client.field_1769.method_3246();
                } catch (Throwable ignored) {
                }
                try {
                    String value = client.field_1769.method_3289();
                    if (value != null && !value.isBlank()) {
                        chunkDebug = value;
                    }
                } catch (Throwable ignored) {
                }
                try {
                    String value = client.field_1769.method_3272();
                    if (value != null && !value.isBlank()) {
                        entityDebug = value;
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        double score = performanceScore(
                averageFps,
                onePercentLow,
                averageFrameMs,
                variance,
                majorStutters,
                frameTimes.size(),
                0
        );

        Bottleneck bottleneck = classifyBottleneck(
                averageFps,
                onePercentLow,
                variance,
                majorStutters,
                usedMemory,
                maxMemory,
                renderDistance,
                simulationDistance,
                entityDistance,
                loadedEntities,
                resolutionWidth,
                resolutionHeight,
                graphics,
                recentChunkActivityPerSecond
        );

        String recommendation = recommendationFor(
                bottleneck,
                renderDistance,
                simulationDistance,
                loadedEntities,
                usedMemory,
                maxMemory,
                resolutionWidth,
                resolutionHeight
        );

        return new MetricsSnapshot(
                currentFps,
                averageFps,
                minimumFps,
                onePercentLow,
                averageFrameMs,
                variance,
                stutters,
                majorStutters,
                frameTimes.size(),
                usedMemory,
                allocatedMemory,
                maxMemory,
                resolutionWidth,
                resolutionHeight,
                refreshRate,
                renderDistance,
                simulationDistance,
                entityDistance,
                particles,
                graphics,
                clouds,
                biomeBlend,
                fpsLimit,
                vsync,
                loadedEntities,
                completedChunks,
                recentChunkActivityPerSecond,
                chunkDebug,
                entityDebug,
                bottleneck,
                recommendation,
                score
        );
    }

    public static double performanceScore(MetricsSnapshot metrics, int targetFps) {
        if (metrics == null) {
            return 0.0;
        }
        return performanceScore(
                metrics.averageFps(),
                metrics.onePercentLowFps(),
                metrics.averageFrameTimeMs(),
                metrics.frameTimeVariance(),
                metrics.majorStutters(),
                metrics.sampleCount(),
                targetFps
        );
    }

    private static double performanceScore(
            double averageFps,
            double onePercentLow,
            double frameMs,
            double variance,
            int majorStutters,
            int sampleCount,
            int targetFps
    ) {
        if (sampleCount < 10 || averageFps <= 0.0 || onePercentLow <= 0.0) {
            return 0.0;
        }

        double avg = averageFps;
        double low = onePercentLow;
        if (targetFps > 0) {
            avg = Math.min(avg, targetFps * 1.20);
            low = Math.min(low, targetFps * 1.10);
        }

        double consistency = Math.max(0.0, Math.min(1.0, onePercentLow / Math.max(1.0, averageFps)));
        double variancePenalty = Math.sqrt(Math.max(0.0, variance)) * 2.6;
        double stutterPenalty = majorStutters * 18.0 * Math.max(1.0, 120.0 / Math.max(30.0, sampleCount));
        double framePenalty = frameMs > 0.0 ? Math.max(0.0, frameMs - 16.67) * 0.7 : 0.0;

        return Math.max(0.0,
                avg * 0.38
                        + low * 0.46
                        + consistency * 85.0
                        + averageFps * 0.04
                        - variancePenalty
                        - stutterPenalty
                        - framePenalty
        );
    }

    public static boolean hasEnoughSamples(MetricsSnapshot metrics) {
        return metrics != null && metrics.sampleCount() >= 30;
    }

    private static Bottleneck classifyBottleneck(
            double averageFps,
            double onePercentLow,
            double variance,
            int majorStutters,
            long usedMemory,
            long maxMemory,
            int renderDistance,
            int simulationDistance,
            double entityDistance,
            int entities,
            int width,
            int height,
            String graphics,
            double chunkActivity
    ) {
        double memoryPressure = maxMemory > 0L ? usedMemory / (double) maxMemory : 0.0;
        if (memoryPressure >= 0.86) {
            return Bottleneck.MEMORY_PRESSURE;
        }

        double lowRatio = averageFps > 0.0 ? onePercentLow / averageFps : 1.0;
        boolean unstable = averageFps > 0.0 && (lowRatio < 0.70 || variance > 18.0 || majorStutters >= 2);

        if (entities >= 250 && unstable) {
            return Bottleneck.ENTITY_RENDERING;
        }
        if ((renderDistance >= 18 || simulationDistance >= 12 || chunkActivity >= 4.0) && unstable) {
            return Bottleneck.CHUNK_RENDERING;
        }

        long pixels = (long) Math.max(0, width) * Math.max(0, height);
        boolean expensiveGraphics = graphics != null
                && (graphics.toLowerCase(Locale.ROOT).contains("fabulous")
                || graphics.toLowerCase(Locale.ROOT).contains("fancy"));
        if (averageFps > 0.0 && averageFps < 120.0 && pixels >= 3_000_000L && expensiveGraphics && lowRatio >= 0.72) {
            return Bottleneck.GPU;
        }

        if (unstable && (simulationDistance >= 10 || entities >= 140 || entityDistance >= 0.9)) {
            return Bottleneck.CPU;
        }
        if (unstable) {
            return Bottleneck.MIXED;
        }
        return Bottleneck.NONE;
    }

    private static String recommendationFor(
            Bottleneck bottleneck,
            int renderDistance,
            int simulationDistance,
            int entities,
            long usedMemory,
            long maxMemory,
            int width,
            int height
    ) {
        return switch (bottleneck) {
            case CPU -> "CPU-side pressure is most likely. Simulation distance and entity work are the safest first reductions.";
            case GPU -> "GPU-side render pressure is likely from resolution/visual settings. This is a heuristic; Yogi does not report fake GPU utilization.";
            case CHUNK_RENDERING -> "Chunk rendering is the strongest signal. Lower render distance a little and favor stable chunk-update settings.";
            case ENTITY_RENDERING -> "Entity rendering is the strongest signal. Lower entity distance/shadows before sacrificing terrain quality.";
            case MEMORY_PRESSURE -> "Java heap pressure is high (" + usedMemory + "/" + maxMemory + " MB). Reduce memory-heavy view settings before forcing garbage collection.";
            case MIXED -> "No single bottleneck dominates. Favor 1% lows and make small reversible changes instead of aggressively lowering everything.";
            case NONE -> {
                if (renderDistance >= 24) {
                    yield "Performance is currently stable; render distance " + renderDistance + " is the largest obvious headroom setting.";
                }
                if (simulationDistance >= 12) {
                    yield "Performance is currently stable; simulation distance " + simulationDistance + " is the main CPU-side setting to watch.";
                }
                if (entities >= 0) {
                    yield "Performance is currently stable. Keep the current profile unless 1% lows fall in a busier area.";
                }
                yield "Join a world to collect enough real frame data for a bottleneck recommendation.";
            }
        };
    }

    private static synchronized void trim(long nowNanos) {
        long cutoff = nowNanos - SAMPLE_RETENTION_NANOS;
        while (!SAMPLES.isEmpty() && (SAMPLES.peekFirst().timestampNanos() < cutoff || SAMPLES.size() > MAX_SAMPLES)) {
            SAMPLES.removeFirst();
        }
    }

    private static long bytesToMb(long value) {
        return Math.max(0L, value / (1024L * 1024L));
    }

    private static String pretty(Enum<?> value) {
        if (value == null) {
            return "Unavailable";
        }
        String raw = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder(raw.length());
        boolean capitalize = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (capitalize && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                result.append(c);
            }
            if (c == ' ') {
                capitalize = true;
            }
        }
        return result.toString();
    }

    public enum Bottleneck {
        CPU("CPU"),
        GPU("GPU"),
        CHUNK_RENDERING("Chunk Rendering"),
        ENTITY_RENDERING("Entity Rendering"),
        MEMORY_PRESSURE("Memory Pressure"),
        MIXED("Mixed"),
        NONE("No strong bottleneck");

        private final String displayName;

        Bottleneck(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public record MetricsSnapshot(
            int currentFps,
            double averageFps,
            double minimumFps,
            double onePercentLowFps,
            double averageFrameTimeMs,
            double frameTimeVariance,
            int stutters,
            int majorStutters,
            int sampleCount,
            long usedMemoryMb,
            long allocatedMemoryMb,
            long maxMemoryMb,
            int resolutionWidth,
            int resolutionHeight,
            int refreshRate,
            int renderDistance,
            int simulationDistance,
            double entityDistance,
            String particles,
            String graphics,
            String clouds,
            int biomeBlend,
            int fpsLimit,
            boolean vsync,
            int loadedEntities,
            int completedChunks,
            double chunkActivityPerSecond,
            String chunkDebug,
            String entityDebug,
            Bottleneck bottleneck,
            String recommendation,
            double score
    ) {
        public boolean hasFrameData() {
            return sampleCount >= 10 && averageFps > 0.0;
        }

        public String resolutionText() {
            return resolutionWidth > 0 && resolutionHeight > 0
                    ? resolutionWidth + "×" + resolutionHeight
                    : "Unavailable";
        }
    }

    public static final class BenchmarkSession {
        private final long startNanos;

        private BenchmarkSession(long startNanos) {
            this.startNanos = startNanos;
        }

        public MetricsSnapshot finish() {
            return snapshotSinceNanos(startNanos, System.nanoTime());
        }

        public long elapsedMillis() {
            return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        }
    }

    private record FrameSample(long timestampNanos, double frameTimeMs) {
    }
}
