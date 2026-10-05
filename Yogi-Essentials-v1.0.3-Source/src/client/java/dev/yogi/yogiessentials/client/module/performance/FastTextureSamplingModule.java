package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_12393;
import net.minecraft.class_315;

/** Trims optional texture sampling work while keeping normal texture rendering intact. */
public final class FastTextureSamplingModule extends OptionBackedPerformanceModule {
    private Integer previousAnisotropy;
    private class_12393 previousFiltering;

    public FastTextureSamplingModule() {
        super(
                "Fast Texture Sampling",
                "Disables extra texture filtering and anisotropic sampling to reduce GPU sampling cost at high view distances."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previousAnisotropy = options.method_76247().method_41753();
        previousFiltering = options.method_76747().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_76247().method_41748(1);
        options.method_76747().method_41748(class_12393.field_64663);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previousAnisotropy != null) options.method_76247().method_41748(previousAnisotropy);
        if (previousFiltering != null) options.method_76747().method_41748(previousFiltering);
    }

    @Override
    protected void clearPrevious() {
        previousAnisotropy = null;
        previousFiltering = null;
    }
}
