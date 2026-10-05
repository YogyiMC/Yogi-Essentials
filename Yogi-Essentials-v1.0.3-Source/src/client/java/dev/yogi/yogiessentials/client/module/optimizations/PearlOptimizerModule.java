package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;











public class PearlOptimizerModule extends OptimizationModule {

    private final BooleanSetting instantPearl =
            new BooleanSetting("Instant Pearl", true);

    public PearlOptimizerModule() {
        super(
                "Pearl Optimizer",
                "Shows your ender pearl instantly on throw while the server stays in control "
                        + "of the real pearl. Client-side visual prediction only."
        );
        addSetting(instantPearl);
    }

    public boolean shouldPredict() {
        return isEnabled() && instantPearl.get();
    }
}
