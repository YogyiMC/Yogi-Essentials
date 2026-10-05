package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;

public class NoChunkFadeModule extends OptionBackedPerformanceModule {

    private Double previous;

    public NoChunkFadeModule() {
        super(
                "No Chunk Fade",
                "Disables chunk fade transitions for a simpler renderer."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_76253().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_76253().method_41748(0.0);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_76253().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
