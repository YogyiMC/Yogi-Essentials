package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;
import net.minecraft.class_5365;

/** Pins Minecraft's renderer to its lowest-overhead graphics preset. */
public final class FastGraphicsModule extends OptionBackedPerformanceModule {
    private class_5365 previous;

    public FastGraphicsModule() {
        super(
                "Fast Graphics Pipeline",
                "Keeps Minecraft on the Fast graphics preset to reduce expensive foliage, transparency, and render-pass work."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_75329().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        if (options.method_75329().method_41753() != class_5365.field_25427) {
            options.method_75317(class_5365.field_25427);
        }
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_75317(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
