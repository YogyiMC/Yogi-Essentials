package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;
import net.minecraft.class_4063;

public class NoCloudsModule extends OptionBackedPerformanceModule {

    private class_4063 previous;

    public NoCloudsModule() {
        super(
                "No Clouds",
                "Disables cloud rendering for a small FPS improvement."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_42528().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_42528().method_41748(class_4063.field_18162);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_42528().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
