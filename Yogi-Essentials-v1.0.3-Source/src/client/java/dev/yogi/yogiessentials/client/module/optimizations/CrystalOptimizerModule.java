package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;

public class CrystalOptimizerModule extends OptimizationModule {

    private final BooleanSetting instantRemoval =
            new BooleanSetting("Instant Crystal Visual Removal", true);

    public CrystalOptimizerModule() {
        super(
                "Crystal Optimizer",
                "Hides a crystal visually after your manual hit until the server responds."
        );
        addSetting(instantRemoval);
    }

    public boolean shouldPredictRemoval() {
        return isEnabled() && instantRemoval.get();
    }
}
