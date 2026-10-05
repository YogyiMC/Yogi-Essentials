package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;











public class InputOptimizerModule extends OptimizationModule {

    private final BooleanSetting autoSprintRecovery =
            new BooleanSetting("Auto Sprint Recovery", true);
    private final BooleanSetting keepSprintThroughUse =
            new BooleanSetting("Keep Sprint Through Item Use", true);

    public InputOptimizerModule() {
        super(
                "Input Optimizer",
                "Recovers dropped sprint and keeps sprint through item use, without acting "
                        + "during attacks. Real movement input, server-validated."
        );
        addSetting(autoSprintRecovery);
        addSetting(keepSprintThroughUse);
    }

    public boolean autoSprintRecovery() {
        return isEnabled() && autoSprintRecovery.get();
    }

    public boolean keepSprintThroughUse() {
        return isEnabled() && keepSprintThroughUse.get();
    }
}
