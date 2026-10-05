package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import net.minecraft.class_310;

/**
 * Keeps Yogi Essentials HUD rendering responsive while avoiding unnecessary
 * high-frequency inventory/effect/list recomputation on very high-FPS clients.
 * Drawing still happens every frame; only expensive source-data refreshes are
 * rate-limited and cached.
 */
public final class HudUpdateOptimizerModule extends Module {
    private final NumberSetting generalRate =
            new NumberSetting("HUD Data Update Rate (FPS)", 30, 10, 120, 5);
    private final NumberSetting inventoryRate =
            new NumberSetting("Inventory Scan Rate (FPS)", 15, 5, 60, 5);
    private final NumberSetting effectsRate =
            new NumberSetting("Effects Scan Rate (FPS)", 20, 5, 60, 5);
    private final BooleanSetting adaptive =
            new BooleanSetting("Adaptive HUD Throttling", true);
    private final NumberSetting minimumRatePercent =
            new NumberSetting("Minimum Adaptive Rate (%)", 60, 40, 100, 5);

    private volatile double adaptiveScale = 1.0;
    private long lastEvaluation;

    public HudUpdateOptimizerModule() {
        super(
                "HUD Update Optimizer",
                "Reduces repeated HUD inventory, effect-list and warning recomputation at very high FPS while continuing to draw the HUD every frame.",
                Category.PERFORMANCE
        );
        addSetting(generalRate);
        addSetting(inventoryRate);
        addSetting(effectsRate);
        addSetting(adaptive);
        addSetting(minimumRatePercent);
    }

    @Override
    protected void onDisable() {
        adaptiveScale = 1.0;
        lastEvaluation = 0L;
    }

    public void tick(class_310 client) {
        if (!isEnabled() || !adaptive.get() || client == null || client.field_1687 == null) {
            adaptiveScale = 1.0;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastEvaluation < 750L) return;
        lastEvaluation = now;

        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        if (!sample.available()) return;

        double floor = minimumRatePercent.get() / 100.0;
        double lowRatio = sample.onePercentLow() / Math.max(1.0, sample.averageFps());
        double target = 1.0;
        if (sample.stutters() >= 3 || lowRatio < 0.58 || sample.varianceMsSquared() > 24.0) {
            target = floor;
        } else if (sample.stutters() >= 1 || lowRatio < 0.75 || sample.varianceMsSquared() > 12.0) {
            target = Math.max(floor, 0.80);
        }

        if (target < adaptiveScale) {
            adaptiveScale = Math.max(target, adaptiveScale - 0.10);
        } else {
            adaptiveScale = Math.min(target, adaptiveScale + 0.05);
        }
    }

    public long generalIntervalNanos() {
        return intervalNanos(generalRate.get());
    }

    public long inventoryIntervalNanos() {
        return intervalNanos(inventoryRate.get());
    }

    public long effectsIntervalNanos() {
        return intervalNanos(effectsRate.get());
    }

    private long intervalNanos(double configuredRate) {
        double scale = adaptive.get() ? adaptiveScale : 1.0;
        double effectiveRate = Math.max(1.0, configuredRate * scale);
        return Math.max(1_000_000L, Math.round(1_000_000_000.0 / effectiveRate));
    }
}
