package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.util.AnchorPredictionManager;














public final class AnchorOptimizerModule extends OptimizationModule {

    public AnchorOptimizerModule() {
        super(
                "Anchor Optimizer",
                "Hides the stale anchor render after a manual explosion until the real server update arrives."
        );
    }

    public boolean shouldPredictVisualRemoval() {
        return isEnabled();
    }

    @Override
    protected void onDisable() {
        AnchorPredictionManager.clearPredictions();
    }
}
