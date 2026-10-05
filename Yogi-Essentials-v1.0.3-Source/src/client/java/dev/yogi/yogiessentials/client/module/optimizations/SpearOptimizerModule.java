package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;




public class SpearOptimizerModule extends OptimizationModule {

    private final BooleanSetting diagnostics =
            new BooleanSetting("Diagnostics", true);

    public SpearOptimizerModule() {
        super(
                "Spear Optimizer",
                "Improves manual spear lunge-swaps by batching the vanilla slot update and STAB action."
        );
        addSetting(diagnostics);
    }

    public boolean diagnostics() {
        return diagnostics.get();
    }
}
