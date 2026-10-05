package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;










public class WindChargeOptimizerModule extends OptimizationModule {

    private final BooleanSetting instantWindCharge =
            new BooleanSetting("Instant Wind Charge", true);

    public WindChargeOptimizerModule() {
        super(
                "Wind Charge Optimizer",
                "Shows your wind charge instantly on throw while the server controls the real "
                        + "burst and knockback. Client-side visual prediction only."
        );
        addSetting(instantWindCharge);
    }

    public boolean shouldPredict() {
        return isEnabled() && instantWindCharge.get();
    }
}
