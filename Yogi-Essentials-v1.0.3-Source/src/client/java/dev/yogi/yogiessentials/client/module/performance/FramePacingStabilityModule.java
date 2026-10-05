package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_4063;
import net.minecraft.class_6597;

/** Conservative adaptive tuning focused on 1% lows rather than peak FPS. */
public final class FramePacingStabilityModule extends Module {

    private final BooleanSetting prioritizeOnePercentLows =
            new BooleanSetting("Prioritize 1% Lows", true);
    private final BooleanSetting reduceMicrostutters =
            new BooleanSetting("Reduce Microstutters", true);
    private final BooleanSetting adaptivePerformance =
            new BooleanSetting("Adaptive Performance", true);
    private final NumberSetting targetFps =
            new NumberSetting("Target FPS", 120.0, 30.0, 360.0, 5.0);
    private final NumberSetting sensitivity =
            new NumberSetting("Sensitivity", 0.50, 0.0, 1.0, 0.05);

    private Baseline baseline;
    private long lastEvaluationNanos;
    private int badSeconds;
    private int goodSeconds;
    private int adaptiveStage;

    public FramePacingStabilityModule() {
        super(
                "Frame Pacing / Stability Optimizer",
                "Improves 1% lows with slow, reversible adaptive changes. Target FPS is only a goal; this module never changes the FPS limit.",
                Category.PERFORMANCE
        );
        addSetting(prioritizeOnePercentLows);
        addSetting(reduceMicrostutters);
        addSetting(adaptivePerformance);
        addSetting(targetFps);
        addSetting(sensitivity);
    }

    @Override
    protected void onEnable() {
        resetTimers();
        captureBaseline();
    }

    @Override
    protected void onDisable() {
        restoreBaseline();
        resetTimers();
    }

    public void reconcile(class_310 client) {
        if (!isEnabled() || client == null || client.field_1690 == null) {
            return;
        }

        if (!adaptivePerformance.get()) {
            if (baseline != null) {
                restoreBaseline();
                resetTimers();
            }
            return;
        }

        if (client.field_1687 == null) {
            return;
        }

        long now = System.nanoTime();
        if (lastEvaluationNanos != 0L
                && now - lastEvaluationNanos < 1_000_000_000L) {
            return;
        }
        lastEvaluationNanos = now;

        if (baseline == null) {
            captureBaseline();
        }

        PerformanceTelemetry.Snapshot data = PerformanceTelemetry.snapshot();
        if (data.samples() < 120 || data.averageFps() <= 0.0) {
            return;
        }

        double goal = targetFps.get();
        double lowRatio = data.onePercentLowFps() / Math.max(1.0, data.averageFps());
        boolean unstable = prioritizeOnePercentLows.get() && lowRatio < 0.62;
        boolean stuttering = reduceMicrostutters.get()
                && (data.majorStutters() > 0 || data.stutters() >= 4);
        boolean belowGoal = data.averageFps() < goal * 0.72;
        boolean bad = unstable || stuttering || belowGoal;

        if (bad) {
            badSeconds++;
            goodSeconds = 0;
        } else {
            goodSeconds++;
            badSeconds = 0;
        }

        int badRequired = 10 - (int) Math.round(sensitivity.get() * 5.0);
        int goodRequired = 50 - (int) Math.round(sensitivity.get() * 20.0);

        if (badSeconds >= badRequired) {
            badSeconds = 0;
            applyNextStage(client.field_1690);
        } else if (goodSeconds >= goodRequired && adaptiveStage > 0) {
            goodSeconds = 0;
            restoreOneStage(client.field_1690);
        }
    }

    private void applyNextStage(class_315 options) {
        if (baseline == null || adaptiveStage >= 4) {
            return;
        }

        adaptiveStage++;
        switch (adaptiveStage) {
            case 1 -> {
                options.method_41798().method_41748(class_6597.field_34788);
                options.method_76253().method_41748(0.0);
            }
            case 2 -> options.method_75333().method_41748(
                    Math.min(options.method_75333().method_41753(), 12));
            case 3 -> {
                options.method_42435().method_41748(false);
                options.method_42528().method_41748(class_4063.field_18162);
            }
            case 4 -> {
                int current = options.method_42503().method_41753();
                if (current >= 16) {
                    options.method_42503().method_41748(Math.max(10, current - 2));
                }
            }
            default -> {
            }
        }
        options.method_1640();
    }

    private void restoreOneStage(class_315 options) {
        if (baseline == null || adaptiveStage <= 0) {
            return;
        }

        switch (adaptiveStage) {
            case 4 -> options.method_42503().method_41748(baseline.renderDistance());
            case 3 -> {
                options.method_42435().method_41748(baseline.entityShadows());
                options.method_42528().method_41748(baseline.clouds());
            }
            case 2 -> options.method_75333().method_41748(baseline.weatherRadius());
            case 1 -> {
                options.method_41798().method_41748(baseline.chunkBuilderMode());
                options.method_76253().method_41748(baseline.chunkFade());
            }
            default -> {
            }
        }
        adaptiveStage--;
        options.method_1640();
    }

    private void captureBaseline() {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1690 == null || baseline != null) {
            return;
        }
        class_315 options = client.field_1690;
        baseline = new Baseline(
                options.method_42503().method_41753(),
                options.method_75333().method_41753(),
                options.method_42435().method_41753(),
                options.method_42528().method_41753(),
                options.method_41798().method_41753(),
                options.method_76253().method_41753()
        );
    }

    private void restoreBaseline() {
        class_310 client = class_310.method_1551();
        if (baseline == null || client == null || client.field_1690 == null) {
            baseline = null;
            adaptiveStage = 0;
            return;
        }

        class_315 options = client.field_1690;
        options.method_42503().method_41748(baseline.renderDistance());
        options.method_75333().method_41748(baseline.weatherRadius());
        options.method_42435().method_41748(baseline.entityShadows());
        options.method_42528().method_41748(baseline.clouds());
        options.method_41798().method_41748(baseline.chunkBuilderMode());
        options.method_76253().method_41748(baseline.chunkFade());
        options.method_1640();
        baseline = null;
        adaptiveStage = 0;
    }

    private void resetTimers() {
        lastEvaluationNanos = 0L;
        badSeconds = 0;
        goodSeconds = 0;
    }

    private record Baseline(
            int renderDistance,
            int weatherRadius,
            boolean entityShadows,
            class_4063 clouds,
            class_6597 chunkBuilderMode,
            double chunkFade
    ) {
    }
}
