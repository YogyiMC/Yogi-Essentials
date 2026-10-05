package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;

public class NoEntityShadowsModule extends OptionBackedPerformanceModule {

    private Boolean previous;

    public NoEntityShadowsModule() {
        super(
                "No Entity Shadows",
                "Disables entity shadows to reduce unnecessary rendering."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_42435().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_42435().method_41748(false);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_42435().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
