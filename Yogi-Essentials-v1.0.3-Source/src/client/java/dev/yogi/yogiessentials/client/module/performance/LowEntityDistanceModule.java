package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;

public class LowEntityDistanceModule extends OptionBackedPerformanceModule {

    private Double previous;

    public LowEntityDistanceModule() {
        super(
                "Low Entity Distance",
                "Reduces how far away entities are rendered."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_42517().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        double current = options.method_42517().method_41753();
        options.method_42517().method_41748(Math.min(current, 0.50));
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_42517().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
