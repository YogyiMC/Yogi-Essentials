package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import net.minecraft.class_310;

/**
 * Adaptive frame-time guard. It reacts to sustained bad 1% lows/stutter by
 * reducing simulation distance first. Render distance is preserved by default
 * so high-distance setups can keep far terrain visible.
 */
public final class FramePacingModule extends Module {
    private final BooleanSetting prioritizeLows =
            new BooleanSetting("Prioritize 1% Lows", true);
    private final BooleanSetting reduceStutters =
            new BooleanSetting("Reduce Microstutters", true);
    private final BooleanSetting adaptive =
            new BooleanSetting("Adaptive Performance", true);
    private final BooleanSetting preserveRenderDistance =
            new BooleanSetting("Preserve Render Distance", true);
    private final NumberSetting target =
            new NumberSetting("Target FPS (0 = Unlimited)", 0, 0, 360, 10);
    private final NumberSetting sensitivity =
            new NumberSetting("Stutter Detection Sensitivity", 2.0, 1.5, 3.0, 0.25);
    private final NumberSetting reactionSeconds =
            new NumberSetting("Sustained Slowdown Seconds", 8.0, 4.0, 30.0, 1.0);

    private long badSince;
    private long goodSince;
    private long lastChange;
    private long lastEvaluation;

    private int originalRenderDistance = -1;
    private int appliedRenderDistance = -1;
    private int originalSimulationDistance = -1;
    private int appliedSimulationDistance = -1;

    public FramePacingModule() {
        super("Frame Pacing / Stability Optimizer",
                "Monitors frame times, protects 1% lows, and reduces simulation load before touching render distance.",
                Category.PERFORMANCE);
        addSetting(prioritizeLows);
        addSetting(reduceStutters);
        addSetting(adaptive);
        addSetting(preserveRenderDistance);
        addSetting(target);
        addSetting(sensitivity);
        addSetting(reactionSeconds);
    }

    @Override
    protected void onDisable() {
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1690 != null) {
            if (appliedSimulationDistance >= 0
                    && client.field_1690.method_42510().method_41753() == appliedSimulationDistance
                    && originalSimulationDistance >= 0) {
                client.field_1690.method_42510().method_41748(originalSimulationDistance);
            }
            if (appliedRenderDistance >= 0
                    && client.field_1690.method_42503().method_41753() == appliedRenderDistance
                    && originalRenderDistance >= 0) {
                client.field_1690.method_42503().method_41748(originalRenderDistance);
            }
        }
        resetAdaptiveState();
    }

    public void tick(class_310 client) {
        if (PerformanceOptimizer.optimizerOwnsRuntimeOptions()) return;
        double effectiveTarget = target.get();
        if (!isEnabled() || !adaptive.get() || client.field_1687 == null
                || client.field_1755 != null || client.field_1690 == null) {
            badSince = 0;
            goodSince = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastEvaluation < 750) {
            return;
        }
        lastEvaluation = now;

        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        if (!sample.available()) {
            return;
        }

        boolean severeStutter = reduceStutters.get()
                && sample.stutters() >= 3
                && sample.varianceMsSquared() > sensitivity.get() * 24.0;
        boolean lowInstability = prioritizeLows.get()
                && sample.onePercentLow() < sample.averageFps() * 0.58;
        boolean targetPressure = effectiveTarget >= 30
                && (sample.onePercentLow() < effectiveTarget * 0.72
                || sample.averageFps() < effectiveTarget * 0.78);
        boolean poor = severeStutter || lowInstability || targetPressure;

        if (poor) {
            goodSince = 0;
            if (badSince == 0) {
                badSince = now;
            }

            long reactionMs = Math.round(reactionSeconds.get() * 1000.0);
            if (now - badSince >= reactionMs && now - lastChange >= 15_000L) {
                if (!reduceSimulationLoad(client)) {
                    reduceRenderLoadIfAllowed(client);
                }
                badSince = now;
            }
            return;
        }

        badSince = 0;
        if (goodSince == 0) {
            goodSince = now;
        }

        if (now - goodSince >= 90_000L && now - lastChange >= 30_000L) {
            restoreOneStep(client);
            goodSince = now;
        }
    }

    private boolean reduceSimulationLoad(class_310 client) {
        int current = client.field_1690.method_42510().method_41753();
        if (originalSimulationDistance < 0) {
            originalSimulationDistance = current;
        }

        int floor = Math.max(5, originalSimulationDistance - 5);
        if ((appliedSimulationDistance < 0 || current == appliedSimulationDistance)
                && current > floor) {
            appliedSimulationDistance = current - 1;
            client.field_1690.method_42510().method_41748(appliedSimulationDistance);
            lastChange = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private boolean reduceRenderLoadIfAllowed(class_310 client) {
        if (preserveRenderDistance.get()) {
            return false;
        }

        int current = client.field_1690.method_42503().method_41753();
        if (originalRenderDistance < 0) {
            originalRenderDistance = current;
        }

        int floor = Math.max(6, originalRenderDistance - 4);
        if ((appliedRenderDistance < 0 || current == appliedRenderDistance)
                && current > floor) {
            appliedRenderDistance = current - 1;
            client.field_1690.method_42503().method_41748(appliedRenderDistance);
            lastChange = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private void restoreOneStep(class_310 client) {
        if (appliedRenderDistance >= 0
                && originalRenderDistance >= 0
                && client.field_1690.method_42503().method_41753() == appliedRenderDistance) {
            int restored = Math.min(originalRenderDistance, appliedRenderDistance + 1);
            client.field_1690.method_42503().method_41748(restored);
            appliedRenderDistance = restored == originalRenderDistance ? -1 : restored;
            lastChange = System.currentTimeMillis();
            return;
        }

        if (appliedSimulationDistance >= 0
                && originalSimulationDistance >= 0
                && client.field_1690.method_42510().method_41753() == appliedSimulationDistance) {
            int restored = Math.min(originalSimulationDistance, appliedSimulationDistance + 1);
            client.field_1690.method_42510().method_41748(restored);
            appliedSimulationDistance = restored == originalSimulationDistance ? -1 : restored;
            lastChange = System.currentTimeMillis();
        }
    }

    private void resetAdaptiveState() {
        originalRenderDistance = -1;
        appliedRenderDistance = -1;
        originalSimulationDistance = -1;
        appliedSimulationDistance = -1;
        badSince = 0;
        goodSince = 0;
        lastChange = 0;
    }
}
