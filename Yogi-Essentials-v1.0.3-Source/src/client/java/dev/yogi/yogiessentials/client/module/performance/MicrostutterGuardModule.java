package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import net.minecraft.class_310;

/**
 * Fast transient pressure signal for bursty scenes. It never rewrites Minecraft
 * options. Instead it temporarily tightens Yogi Essentials' optional render,
 * particle and chunk-submission work after a real frame-time collapse.
 */
public final class MicrostutterGuardModule extends Module {
    private final NumberSetting triggerRatio = new NumberSetting("1% Low Trigger (%)", 62, 30, 85, 5);
    private final NumberSetting stressedBudget = new NumberSetting("Stressed Work Budget (%)", 55, 30, 90, 5);
    private final NumberSetting emergencyDropPercent = new NumberSetting("Emergency FPS Drop Trigger (%)", 58, 35, 80, 5);
    private final NumberSetting emergencyBudget = new NumberSetting("Emergency Work Budget (%)", 35, 20, 70, 5);
    private final NumberSetting holdSeconds = new NumberSetting("Pressure Hold (seconds)", 4, 1, 12, 1);

    private volatile double pressureScale = 1.0;
    private static volatile MicrostutterGuardModule cachedInstance;
    private double learnedHealthyFps;
    private long pressureUntil;
    private long lastEvaluation;
    private long previousUsedBytes;
    private long previousUsedAt;

    public MicrostutterGuardModule() {
        super(
                "Microstutter Guard",
                "Detects sudden FPS collapses and temporarily reduces optional Yogi Essentials render, particle, and chunk work without changing Minecraft graphics settings.",
                Category.PERFORMANCE
        );
        addSetting(triggerRatio);
        addSetting(stressedBudget);
        addSetting(emergencyDropPercent);
        addSetting(emergencyBudget);
        addSetting(holdSeconds);
    }

    @Override
    protected void onDisable() {
        pressureScale = 1.0;
        learnedHealthyFps = 0.0;
        pressureUntil = 0L;
        lastEvaluation = 0L;
        previousUsedBytes = 0L;
        previousUsedAt = 0L;
    }

    public void tick(class_310 client) {
        if (!isEnabled() || client == null || client.field_1687 == null || client.field_1755 != null
                || !client.method_1569()) {
            pressureScale = 1.0;
            pressureUntil = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastEvaluation >= 100L) {
            lastEvaluation = now;
            FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
            if (sample.available()) {
                double average = Math.max(1.0, sample.averageFps());
                double lowRatio = sample.onePercentLow() / average;
                int reportedFps = Math.max(1, client.method_47599());

                double healthyCandidate = Math.max(average, reportedFps);
                if (learnedHealthyFps <= 0.0) {
                    learnedHealthyFps = healthyCandidate;
                } else if (healthyCandidate >= learnedHealthyFps) {
                    learnedHealthyFps += (healthyCandidate - learnedHealthyFps) * 0.16;
                } else if (sample.stutters() == 0 && lowRatio >= 0.82) {
                    learnedHealthyFps += (healthyCandidate - learnedHealthyFps) * 0.002;
                }

                boolean emergencyDrop = learnedHealthyFps >= 90.0
                        && (average <= learnedHealthyFps * (emergencyDropPercent.get() / 100.0)
                        || reportedFps <= learnedHealthyFps * (emergencyDropPercent.get() / 100.0)
                        || sample.onePercentLow() <= learnedHealthyFps * 0.32);

                boolean burst = sample.stutters() >= 2
                        || lowRatio < triggerRatio.get() / 100.0
                        || sample.varianceMsSquared() > 18.0;

                double requestedScale = 1.0;
                if (emergencyDrop) {
                    requestedScale = Math.min(requestedScale, emergencyBudget.get() / 100.0);
                } else if (burst) {
                    requestedScale = Math.min(requestedScale, stressedBudget.get() / 100.0);
                }

                if (client.field_1724 != null && client.field_1690 != null
                        && client.field_1690.method_42503().method_41753() >= 16) {
                    double speed = client.field_1724.method_18798().method_1033();
                    if (speed >= 0.90) requestedScale = Math.min(requestedScale, 0.50);
                    else if (speed >= 0.40) requestedScale = Math.min(requestedScale, 0.70);
                }

                Runtime runtime = Runtime.getRuntime();
                long max = runtime.maxMemory();
                long usedBytes = runtime.totalMemory() - runtime.freeMemory();
                if (max > 0L) {
                    double heap = usedBytes / (double) max;
                    if (heap >= 0.82) requestedScale = Math.min(requestedScale, 0.58);
                    else if (heap >= 0.74) requestedScale = Math.min(requestedScale, 0.76);

                    if (previousUsedAt > 0L && now > previousUsedAt) {
                        long growth = usedBytes - previousUsedBytes;
                        long burstThreshold = Math.max(32L * 1024L * 1024L, max / 50L);
                        if (growth >= burstThreshold && now - previousUsedAt <= 1000L) {
                            requestedScale = Math.min(requestedScale, 0.62);
                        }
                    }
                    previousUsedBytes = usedBytes;
                    previousUsedAt = now;
                }

                if (requestedScale < 1.0) {
                    pressureScale = Math.min(pressureScale, requestedScale);
                    pressureUntil = now + Math.round(holdSeconds.get() * 1000.0);
                }
            }
        }

        if (now > pressureUntil && pressureScale < 1.0) {
            pressureScale = Math.min(1.0, pressureScale + 0.02);
        }
    }

    public double workBudgetScale() {
        return isEnabled() ? Math.max(0.20, Math.min(1.0, pressureScale)) : 1.0;
    }

    public double learnedHealthyFps() {
        return learnedHealthyFps;
    }

    public static double activeScale() {
        MicrostutterGuardModule module = cachedInstance;
        if (module == null) {
            if (dev.yogi.yogiessentials.client.YogiEssentialsClient.getModuleManager() == null) return 1.0;
            module = dev.yogi.yogiessentials.client.YogiEssentialsClient.getModuleManager()
                    .getModule(MicrostutterGuardModule.class);
            cachedInstance = module;
        }
        return module == null ? 1.0 : module.workBudgetScale();
    }
}
