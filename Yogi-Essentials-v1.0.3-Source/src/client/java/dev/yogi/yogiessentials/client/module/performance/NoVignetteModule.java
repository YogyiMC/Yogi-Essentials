package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;

public class NoVignetteModule extends OptionBackedPerformanceModule {

    private Boolean previous;

    public NoVignetteModule() {
        super(
                "No Vignette",
                "Disables the dark vignette around the screen."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_75335().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_75335().method_41748(false);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_75335().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
