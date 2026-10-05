package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;











public class GeneralPvpOptimizerModule extends OptimizationModule {

    private final BooleanSetting combatSprint =
            new BooleanSetting("Combat Sprint Retention", true);

    public GeneralPvpOptimizerModule() {
        super(
                "General PvP Optimizer",
                "Keeps sprint momentum while aiming at a player, without acting during "
                        + "attacks. Movement input only."
        );
        addSetting(combatSprint);
    }

    public boolean shouldKeepCombatSprint() {
        return isEnabled() && combatSprint.get();
    }
}
