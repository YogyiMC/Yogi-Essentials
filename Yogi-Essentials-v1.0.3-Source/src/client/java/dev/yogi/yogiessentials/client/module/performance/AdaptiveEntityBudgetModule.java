package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import net.minecraft.class_310;

/**
 * Runtime entity-distance budget. Entity rendering is one of the largest
 * variable costs at high view distances, so this reacts to sustained frame
 * pressure without changing terrain render distance.
 */
public final class AdaptiveEntityBudgetModule extends Module {
    private final NumberSetting minimumPercent =
            new NumberSetting("Minimum Entity Distance (%)", 45.0, 25.0, 100.0, 5.0);
    private final NumberSetting reactionSeconds =
            new NumberSetting("Reaction Time (seconds)", 4.0, 2.0, 15.0, 1.0);
    private final NumberSetting recoverySeconds =
            new NumberSetting("Recovery Time (seconds)", 45.0, 15.0, 120.0, 5.0);

    private double original = -1.0;
    private double applied = -1.0;
    private long badSince;
    private long goodSince;
    private long lastChange;
    private long lastEvaluation;

    public AdaptiveEntityBudgetModule() {
        super(
                "Adaptive Entity Budget",
                "Dynamically reduces entity render distance during sustained frame-time pressure, then restores it slowly when performance is stable.",
                Category.PERFORMANCE
        );
        addSetting(minimumPercent);
        addSetting(reactionSeconds);
        addSetting(recoverySeconds);
    }

    @Override
    protected void onDisable() {
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1690 != null && original >= 0.0
                && applied >= 0.0
                && Math.abs(client.field_1690.method_42517().method_41753() - applied) < 0.001) {
            client.field_1690.method_42517().method_41748(original);
        }
        reset();
    }

    public void tick(class_310 client) {
        if (PerformanceOptimizer.optimizerOwnsRuntimeOptions()) return;
        if (!isEnabled() || client == null || client.field_1687 == null || client.field_1755 != null
                || client.field_1690 == null) {
            badSince = 0L;
            goodSince = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastEvaluation < 1000L) return;
        lastEvaluation = now;

        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        if (!sample.available()) return;

        int target = PerformanceOptimizer.activeTargetFps();
        boolean targetPressure = target > 0
                && (sample.onePercentLow() < target * 0.72 || sample.averageFps() < target * 0.82);
        boolean framePressure = sample.stutters() >= 2
                || sample.varianceMsSquared() > 42.0
                || sample.onePercentLow() < sample.averageFps() * 0.58;
        boolean poor = targetPressure || framePressure;

        if (poor) {
            goodSince = 0L;
            if (badSince == 0L) badSince = now;
            if (now - badSince >= Math.round(reactionSeconds.get() * 1000.0)
                    && now - lastChange >= 8000L) {
                reduce(client);
                badSince = now;
            }
            return;
        }

        badSince = 0L;
        if (goodSince == 0L) goodSince = now;
        if (now - goodSince >= Math.round(recoverySeconds.get() * 1000.0)
                && now - lastChange >= 15000L) {
            restoreStep(client);
            goodSince = now;
        }
    }

    private void reduce(class_310 client) {
        double current = client.field_1690.method_42517().method_41753();
        if (original < 0.0) original = current;
        double floor = minimumPercent.get() / 100.0;
        if ((applied < 0.0 || Math.abs(current - applied) < 0.001) && current > floor + 0.01) {
            applied = Math.max(floor, Math.round((current - 0.10) * 20.0) / 20.0);
            client.field_1690.method_42517().method_41748(applied);
            lastChange = System.currentTimeMillis();
        }
    }

    private void restoreStep(class_310 client) {
        if (original < 0.0 || applied < 0.0) return;
        double current = client.field_1690.method_42517().method_41753();
        if (Math.abs(current - applied) >= 0.001) return;
        double restored = Math.min(original, Math.round((applied + 0.05) * 20.0) / 20.0);
        client.field_1690.method_42517().method_41748(restored);
        applied = Math.abs(restored - original) < 0.001 ? -1.0 : restored;
        lastChange = System.currentTimeMillis();
    }

    private void reset() {
        original = -1.0;
        applied = -1.0;
        badSince = 0L;
        goodSince = 0L;
        lastChange = 0L;
        lastEvaluation = 0L;
    }
}
