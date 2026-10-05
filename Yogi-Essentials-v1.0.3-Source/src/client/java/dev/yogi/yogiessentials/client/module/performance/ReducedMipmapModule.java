package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_315;

/** Reduces texture mip work and memory bandwidth without disabling textures. */
public final class ReducedMipmapModule extends OptionBackedPerformanceModule {
    private final NumberSetting maximum =
            new NumberSetting("Maximum Mipmap Level", 2.0, 0.0, 4.0, 1.0);
    private Integer previous;

    public ReducedMipmapModule() {
        super(
                "Reduced Mipmap Load",
                "Caps mipmap levels to reduce texture upload, memory, and sampling cost while keeping distance textures usable."
        );
        addSetting(maximum);
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_42563().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        int cap = maximum.get().intValue();
        if (options.method_42563().method_41753() > cap) {
            options.method_42563().method_41748(cap);
        }
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_42563().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
