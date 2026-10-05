package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;

public class NoBiomeBlendModule extends OptionBackedPerformanceModule {

    private Integer previous;

    public NoBiomeBlendModule() {
        super(
                "No Biome Blend",
                "Disables biome color blending for less rendering work."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_41805().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_41805().method_41748(0);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_41805().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
