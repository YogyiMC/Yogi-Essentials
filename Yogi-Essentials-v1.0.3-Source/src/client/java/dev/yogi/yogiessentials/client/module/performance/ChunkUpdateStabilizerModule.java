package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;
import net.minecraft.class_6597;

/**
 * Uses Minecraft's least bursty chunk rebuild policy. This does not change
 * render distance; it only avoids aggressive rebuild prioritization spikes.
 */
public final class ChunkUpdateStabilizerModule extends OptionBackedPerformanceModule {
    private class_6597 previous;

    public ChunkUpdateStabilizerModule() {
        super(
                "Chunk Update Stabilizer",
                "Uses an even chunk rebuild queue to reduce chunk-update spikes and microstutters without lowering render distance."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_41798().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_41798().method_41748(class_6597.field_34788);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_41798().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
