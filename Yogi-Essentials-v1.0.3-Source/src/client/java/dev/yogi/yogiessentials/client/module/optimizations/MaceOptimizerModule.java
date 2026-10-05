package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;











public class MaceOptimizerModule extends OptimizationModule {

    private final BooleanSetting keepSprint =
            new BooleanSetting("Keep Sprint Through Combos", true);

    public MaceOptimizerModule() {
        super(
                "Mace Optimizer",
                "Keeps your sprint momentum through mace combos instead of letting it drop. "
                        + "Movement input only; does not touch damage, reach, or timing."
        );
        addSetting(keepSprint);
    }

    public boolean shouldKeepSprint() {
        return isEnabled() && keepSprint.get();
    }
}
