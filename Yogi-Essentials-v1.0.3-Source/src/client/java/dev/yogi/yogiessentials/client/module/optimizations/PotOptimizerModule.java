package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;










public class PotOptimizerModule extends OptimizationModule {

    private final BooleanSetting instantPotion =
            new BooleanSetting("Instant Splash Potion", true);

    public PotOptimizerModule() {
        super(
                "Pot Optimizer",
                "Shows your splash/lingering potion instantly on throw while the server "
                        + "controls the real splash and effects. Client-side prediction only."
        );
        addSetting(instantPotion);
    }

    public boolean shouldPredict() {
        return isEnabled() && instantPotion.get();
    }
}
