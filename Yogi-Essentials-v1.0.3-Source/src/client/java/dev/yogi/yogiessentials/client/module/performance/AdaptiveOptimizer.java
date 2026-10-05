package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_12393;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_4063;
import net.minecraft.class_4066;
import net.minecraft.class_5365;
import net.minecraft.class_6597;

/**
 * One measured optimizer path. There are intentionally no profiles: every run
 * tests the strongest broadly-safe Minecraft settings, keeps changes that help
 * throughput/1% lows/frame-time stability, then enables Yogi Essentials' adaptive runtime
 * safeguards. The selected FPS target is used as a goal rather than a profile.
 */
public final class AdaptiveOptimizer {
    private static final int GROUPS = 10;
    private static final int BASELINE_WINDOWS = 1;
    private static final int FINAL_WINDOWS = 1;
    private static final long HEADLINE_WARMUP_NANOS = 5_000_000_000L;
    private static final long HEADLINE_MEASURE_NANOS = 10_000_000_000L;
    private static final long CANDIDATE_WARMUP_NANOS = 1_500_000_000L;
    private static final long CANDIDATE_MEASURE_NANOS = 2_500_000_000L;
    private static final double MAX_STABLE_WINDOW_SPREAD = 0.20;

    private final int targetFps;
    private final List<String> changes = new ArrayList<>();
    private FrameTelemetry.Sample before;
    private FrameTelemetry.Sample previous;
    private FrameTelemetry.Sample after;
    private final List<FrameTelemetry.Sample> baselineWindows = new ArrayList<>();
    private final List<FrameTelemetry.Sample> finalWindows = new ArrayList<>();
    private int baselineRetries;
    private int finalRetries;
    private boolean finalRecoveryVerification;
    private PerformanceOptimizer.OptimizationResult result;
    private long mark;
    private long stageStarted;
    private long measurementStarted;
    private int finalStackChangeStart = -1;
    private int group;
    private Runnable rollback;
    private String pendingChange;
    private String status = "Stabilizing baseline in the current world";
    private boolean finished;
    private boolean verifying;
    private boolean focusCancelled;
    private final long focusEpochAtStart;
    private static volatile long focusLossEpoch;
    private static volatile AdaptiveOptimizer activeSession;

    public AdaptiveOptimizer(int targetFps) {
        this.targetFps = targetFps;
        this.focusEpochAtStart = focusLossEpoch;
        activeSession = this;
        if (!PerformanceOptimizer.beginMeasuredOptimization()) {
            finishFailure("A previous optimization must be undone first.");
        } else {
            beginWindow();
        }
    }

    public boolean finished() { return finished; }
    public String status() { return status; }
    public FrameTelemetry.Sample before() { return before; }
    public FrameTelemetry.Sample after() { return after; }
    public PerformanceOptimizer.OptimizationResult result() { return result; }
    public boolean cancelledForFocusLoss() { return focusCancelled; }

    public float progress() {
        if (finished) return 1.0F;
        long warmup = currentWarmupNanos();
        long measure = currentMeasureNanos();
        double fraction = Math.min(0.98, (System.nanoTime() - stageStarted) / (double) (warmup + measure));
        int stagesDone = before == null ? 0 : group;
        if (verifying) stagesDone = GROUPS + 1;
        double linear = Math.min(0.99, (stagesDone + fraction) / (GROUPS + 2.0));
        return (float) Math.min(0.99, 1.0 - Math.pow(1.0 - linear, 1.55));
    }

    public void tick() {
        if (finished) return;

        class_310 client = class_310.method_1551();
        if (client == null || !client.method_1569() || focusLossEpoch != focusEpochAtStart) {
            cancelForFocusLoss();
            return;
        }

        long now = System.nanoTime();
        long elapsed = now - stageStarted;
        long warmupNanos = currentWarmupNanos();
        long measureNanos = currentMeasureNanos();

        if (measurementStarted == 0L) {
            if (elapsed < warmupNanos) return;
            mark = FrameTelemetry.mark();
            measurementStarted = now;
            return;
        }

        if (now - measurementStarted < measureNanos) return;

        FrameTelemetry.Sample sample = FrameTelemetry.snapshotSince(mark);
        if (sample.frames() < 120) {
            if (elapsed > 24_000_000_000L) {
                status = "Waiting for enough clean world frames; restarting this measurement window";
                beginWindow();
            }
            return;
        }

        if (before == null) {
            baselineWindows.add(sample);
            if (baselineWindows.size() < BASELINE_WINDOWS) {
                status = "Stabilizing baseline (" + baselineWindows.size() + "/" + BASELINE_WINDOWS + ")";
                beginWindow();
                return;
            }

            double spread = measurementSpread(baselineWindows);
            if (spread > MAX_STABLE_WINDOW_SPREAD && baselineRetries < 1) {
                baselineRetries++;
                baselineWindows.clear();
                status = "Baseline was unstable; re-measuring clean frame windows";
                beginWindow();
                return;
            }

            before = medianByAverageFps(baselineWindows);
            previous = before;
            nextGroup();
            return;
        }

        if (verifying) {
            finalWindows.add(sample);
            if (finalWindows.size() < FINAL_WINDOWS) {
                status = "Validating final performance (" + finalWindows.size() + "/" + FINAL_WINDOWS + ")";
                beginWindow();
                return;
            }

            double finalSpread = measurementSpread(finalWindows);
            FrameTelemetry.Sample stableFinal = medianByAverageFps(finalWindows);
            finalWindows.clear();

            if (finalSpread > MAX_STABLE_WINDOW_SPREAD && finalRetries < 1) {
                finalRetries++;
                status = "Final measurement was unstable; repeating validation before accepting anything";
                beginWindow();
                return;
            }
            if (finalSpread > MAX_STABLE_WINDOW_SPREAD) {
                status = "Final frame windows were noisy; using the median result and validating regressions";
            }

            if (isFinalRegression(stableFinal, previous != null ? previous : before)) {
                if (!finalRecoveryVerification) {
                    PerformanceOptimizer.restorePerformanceStackSnapshot();
                    trimChangesToAcceptedCandidates();
                    finalRecoveryVerification = true;
                    finalRetries = 0;
                    status = "Final performance stack regressed; validating only the settings that measured as gains";
                    beginWindow();
                    return;
                }

                after = previous != null ? previous : stableFinal;
                status = "Final validation was noisy; keeping the last individually validated settings";
                finish();
                return;
            }

            after = stableFinal;
            finish();
            return;
        }

        if (rollback != null) {
            boolean keep = isBetterOrSafer(sample, previous);
            if (keep) {
                for (String change : pendingChange.split("\\|\\|")) {
                    String trimmed = change.trim();
                    if (!trimmed.isEmpty()) changes.add(trimmed);
                }
                previous = sample;
                status = "Kept: " + pendingChange.replace("||", ", ");
            } else {
                rollback.run();
                status = "Reverted: " + pendingChange + " (no reliable gain)";
            }
            rollback = null;
        }
        nextGroup();
    }

    private boolean isBetterOrSafer(FrameTelemetry.Sample sample, FrameTelemetry.Sample old) {
        double avgGain = sample.averageFps() / Math.max(1.0, old.averageFps());
        double lowGain = sample.onePercentLow() / Math.max(1.0, old.onePercentLow());
        double varianceRatio = sample.varianceMsSquared() / Math.max(0.01, old.varianceMsSquared());

        if (avgGain < 1.000 || lowGain < 0.985 || sample.stutters() > old.stutters() + 1) {
            return false;
        }

        boolean throughputGain = avgGain >= 1.010;
        boolean strongLowGain = lowGain >= 1.030 && avgGain >= 1.000;
        boolean strongVarianceGain = varianceRatio <= 0.86 && avgGain >= 1.000
                && lowGain >= 0.98;
        boolean spikeGain = sample.stutters() + 1 <= old.stutters()
                && avgGain >= 1.000 && lowGain >= 0.98;

        if (targetFps > 0 && old.averageFps() < targetFps) {
            boolean reachesTargetBetter = sample.averageFps() > old.averageFps() * 1.005
                    && sample.onePercentLow() >= old.onePercentLow() * 0.98;
            return throughputGain || strongLowGain || strongVarianceGain || spikeGain || reachesTargetBetter;
        }
        return throughputGain || strongLowGain || strongVarianceGain || spikeGain;
    }

    private boolean isFinalRegression(FrameTelemetry.Sample sample, FrameTelemetry.Sample reference) {
        if (reference == null) return false;
        double avg = sample.averageFps() / Math.max(1.0, reference.averageFps());
        double low = sample.onePercentLow() / Math.max(1.0, reference.onePercentLow());
        double variance = sample.varianceMsSquared() / Math.max(0.01, reference.varianceMsSquared());

        if (avg < 1.005 || low < 0.97) return true;
        if (sample.stutters() > reference.stutters() + 2) return true;
        return variance > 1.30 && low < 0.99;
    }


    private static double measurementSpread(List<FrameTelemetry.Sample> samples) {
        double minAverage = Double.POSITIVE_INFINITY;
        double maxAverage = 0.0;
        for (FrameTelemetry.Sample sample : samples) {
            minAverage = Math.min(minAverage, sample.averageFps());
            maxAverage = Math.max(maxAverage, sample.averageFps());
        }
        return maxAverage <= 0.0 ? 0.0 : (maxAverage - minAverage) / maxAverage;
    }

    private static FrameTelemetry.Sample medianByAverageFps(List<FrameTelemetry.Sample> samples) {
        List<FrameTelemetry.Sample> sorted = new ArrayList<>(samples);
        sorted.sort((a, b) -> Double.compare(a.averageFps(), b.averageFps()));
        return sorted.get(sorted.size() / 2);
    }

    private void trimChangesToAcceptedCandidates() {
        if (finalStackChangeStart >= 0 && finalStackChangeStart < changes.size()) {
            changes.subList(finalStackChangeStart, changes.size()).clear();
        }
    }

    private void nextGroup() {
        class_315 options = class_310.method_1551().field_1690;
        while (group < GROUPS) {
            int step = group++;
            rollback = switch (step) {
                case 0 -> tuneGraphicsMode(options);
                case 1 -> tuneRenderDistance(options);
                case 2 -> tuneChunks(options);
                case 3 -> tuneEffects(options);
                case 4 -> tuneEntities(options);
                case 5 -> tuneLighting(options);
                case 6 -> tuneTerrain(options);
                case 7 -> tuneTextures(options);
                case 8 -> tuneTransitions(options);
                default -> tuneChunkScheduling(options);
            };
            if (rollback != null) {
                beginWindow();
                status = "Measuring: " + pendingChange;
                return;
            }
        }

        finalStackChangeStart = changes.size();
        changes.addAll(PerformanceOptimizer.enablePerformanceModules());
        changes.addAll(PerformanceOptimizer.optimizeCompatibleMods(true));
        changes.addAll(CompatibleModOptimizer.optimizeSodium());
        verifying = true;
        status = "Measuring final adaptive settings";
        beginWindow();
    }


    private Runnable tuneGraphicsMode(class_315 options) {
        class_5365 graphics = options.method_75329().method_41753();
        if (graphics == class_5365.field_25427) return null;
        options.method_75317(class_5365.field_25427);
        pendingChange = "Graphics mode " + graphics + " -> Fast";
        return () -> options.method_75317(graphics);
    }

    private Runnable tuneRenderDistance(class_315 options) {
        int render = options.method_42503().method_41753();
        int targetCap = targetFps >= 180 ? 10 : 12;
        int newRender = Math.min(render, targetCap);
        if (render == newRender) return null;
        options.method_42503().method_41748(newRender);
        int simulation = options.method_42510().method_41753();
        if (simulation > newRender) {
            options.method_42510().method_41748(newRender);
        }
        pendingChange = "Render distance " + render + " -> " + newRender;
        return () -> {
            options.method_42503().method_41748(render);
            options.method_42510().method_41748(simulation);
        };
    }

    private Runnable tuneChunks(class_315 options) {
        int render = options.method_42503().method_41753();
        int simulation = options.method_42510().method_41753();
        int simulationCap = render >= 24 ? 6 : 8;
        int newSimulation = Math.min(simulation, Math.min(render, simulationCap));
        if (simulation == newSimulation) return null;
        options.method_42510().method_41748(newSimulation);
        pendingChange = "Simulation distance " + simulation + " -> " + newSimulation
                + " (render distance preserved at " + render + ")";
        return () -> options.method_42510().method_41748(simulation);
    }

    private Runnable tuneEffects(class_315 options) {
        class_4066 particles = options.method_42475().method_41753();
        class_4063 clouds = options.method_42528().method_41753();
        if (particles == class_4066.field_18199 && clouds == class_4063.field_18162) return null;
        options.method_42475().method_41748(class_4066.field_18199);
        options.method_42528().method_41748(class_4063.field_18162);
        pendingChange = "Particles -> Minimal||Clouds -> Off";
        return () -> {
            options.method_42475().method_41748(particles);
            options.method_42528().method_41748(clouds);
        };
    }

    private Runnable tuneEntities(class_315 options) {
        double distance = options.method_42517().method_41753();
        boolean shadows = options.method_42435().method_41753();
        double newDistance = Math.min(distance, 0.65);
        if (distance == newDistance && !shadows) return null;
        options.method_42517().method_41748(newDistance);
        options.method_42435().method_41748(false);
        pendingChange = "Entity distance -> 65% max||Entity shadows -> Off";
        return () -> {
            options.method_42517().method_41748(distance);
            options.method_42435().method_41748(shadows);
        };
    }

    private Runnable tuneLighting(class_315 options) {
        boolean ao = options.method_41792().method_41753();
        boolean vignette = options.method_75335().method_41753();
        int blend = options.method_41805().method_41753();
        if (!ao && !vignette && blend == 0) return null;
        options.method_41792().method_41748(false);
        options.method_75335().method_41748(false);
        options.method_41805().method_41748(0);
        pendingChange = "Ambient occlusion -> Off||Vignette -> Off||Biome blend -> 0";
        return () -> {
            options.method_41792().method_41748(ao);
            options.method_75335().method_41748(vignette);
            options.method_41805().method_41748(blend);
        };
    }

    private Runnable tuneTerrain(class_315 options) {
        int weather = options.method_75333().method_41753();
        boolean leaves = options.method_75334().method_41753();
        boolean transparency = options.method_75337().method_41753();
        if (weather == 0 && !leaves && !transparency) return null;
        options.method_75333().method_41748(0);
        options.method_75334().method_41748(false);
        options.method_75337().method_41748(false);
        pendingChange = "Improved transparency -> Off||Cutout leaves -> Off||Weather radius -> 0";
        return () -> {
            options.method_75333().method_41748(weather);
            options.method_75334().method_41748(leaves);
            options.method_75337().method_41748(transparency);
        };
    }

    private Runnable tuneTextures(class_315 options) {
        int mipmaps = options.method_42563().method_41753();
        int anisotropy = options.method_76247().method_41753();
        class_12393 filtering = options.method_76747().method_41753();
        int newMipmaps = Math.min(mipmaps, 2);
        if (mipmaps == newMipmaps && anisotropy == 1 && filtering == class_12393.field_64663) return null;
        options.method_42563().method_41748(newMipmaps);
        options.method_76247().method_41748(1);
        options.method_76747().method_41748(class_12393.field_64663);
        pendingChange = "Mipmap levels -> 2 max||Anisotropic sampling -> 1x||Texture filtering -> None";
        return () -> {
            options.method_42563().method_41748(mipmaps);
            options.method_76247().method_41748(anisotropy);
            options.method_76747().method_41748(filtering);
        };
    }

    private Runnable tuneTransitions(class_315 options) {
        double fade = options.method_76253().method_41753();
        int blur = options.method_57702().method_41753();
        if (fade == 0.0 && blur == 0) return null;
        options.method_76253().method_41748(0.0);
        options.method_57702().method_41748(0);
        pendingChange = "Chunk fade -> Off||Menu background blur -> Off";
        return () -> {
            options.method_76253().method_41748(fade);
            options.method_57702().method_41748(blur);
        };
    }

    private Runnable tuneChunkScheduling(class_315 options) {
        class_6597 builder = options.method_41798().method_41753();
        if (builder == class_6597.field_34788) return null;
        options.method_41798().method_41748(class_6597.field_34788);
        pendingChange = "Chunk rebuild scheduling -> Even / non-prioritized";
        return () -> options.method_41798().method_41748(builder);
    }

    private boolean isHeadlineMeasurement() {
        return before == null || verifying;
    }

    private long currentWarmupNanos() {
        return isHeadlineMeasurement() ? HEADLINE_WARMUP_NANOS : CANDIDATE_WARMUP_NANOS;
    }

    private long currentMeasureNanos() {
        return isHeadlineMeasurement() ? HEADLINE_MEASURE_NANOS : CANDIDATE_MEASURE_NANOS;
    }

    private void beginWindow() {
        mark = -1L;
        stageStarted = System.nanoTime();
        measurementStarted = 0L;
    }

    private void finish() {
        if (activeSession == this) activeSession = null;
        changes.addAll(PerformanceOptimizer.applyFrameTarget(targetFps, before));
        finished = true;
        result = PerformanceOptimizer.completeMeasuredOptimization(changes);
        if (after == null) after = previous;
        status = changes.isEmpty() ? "Current settings were kept." : "Optimization complete.";
    }

    private void finishNoGain() {
        if (activeSession == this) activeSession = null;
        changes.addAll(PerformanceOptimizer.applyFrameTarget(targetFps, before));
        finished = true;
        status = "Safe measured settings kept; slower tuning was rejected.";
        result = PerformanceOptimizer.completeMeasuredOptimization(changes);
    }

    private void finishFailure(String message) {
        finished = true;
        if (activeSession == this) activeSession = null;
        status = message;
        result = new PerformanceOptimizer.OptimizationResult(false, message, List.of());
    }


    /** Called directly from Minecraft's focus callback so a tab-out cannot be missed even if client ticks pause. */
    public static void onWindowFocusChanged(boolean focused) {
        if (focused) return;
        focusLossEpoch++;
        AdaptiveOptimizer active = activeSession;
        if (active != null && !active.finished) {
            active.cancelForFocusLoss();
        }
    }

    private void cancelForFocusLoss() {
        if (finished) return;
        focusCancelled = true;
        PerformanceOptimizer.cancelMeasuredOptimization();
        finishFailure("Optimizer canceled: Minecraft lost focus. Stay in the Minecraft window for the entire test, then run Auto Optimize again.");
    }

    public void cancel() {
        if (!finished) {
            PerformanceOptimizer.cancelMeasuredOptimization();
            finishFailure("Optimization canceled; previous settings kept.");
        }
    }
}
