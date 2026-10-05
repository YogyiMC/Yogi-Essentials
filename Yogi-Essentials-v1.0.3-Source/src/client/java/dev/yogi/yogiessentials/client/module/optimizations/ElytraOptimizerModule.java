package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;











public class ElytraOptimizerModule extends OptimizationModule {

    private final BooleanSetting instantStop =
            new BooleanSetting("Instant Stop Gliding", true);

    public ElytraOptimizerModule() {
        super(
                "Elytra Optimizer",
                "Stops the glide pose instantly when your elytra is no longer equipped, "
                        + "instead of waiting for the server. Matches server behaviour, no rubber-band."
        );
        addSetting(instantStop);
    }

    public boolean shouldPredictStop() {
        return isEnabled() && instantStop.get();
    }
}
