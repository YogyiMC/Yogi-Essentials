package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;
import net.minecraft.class_4066;

public class MinimalParticlesModule extends OptionBackedPerformanceModule {

    private class_4066 previous;

    public MinimalParticlesModule() {
        super(
                "Minimal Particles",
                "Reduces particle rendering for better performance."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_42475().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_42475().method_41748(class_4066.field_18199);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_42475().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
