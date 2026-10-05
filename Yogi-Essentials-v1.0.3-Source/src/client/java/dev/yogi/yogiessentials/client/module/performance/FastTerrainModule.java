package dev.yogi.yogiessentials.client.module.performance;

import net.minecraft.class_315;

/** Keeps expensive terrain/weather presentation options on their fast paths. */
public final class FastTerrainModule extends OptionBackedPerformanceModule {
    private Integer previousWeatherRadius;
    private Boolean previousCutoutLeaves;
    private Boolean previousImprovedTransparency;

    public FastTerrainModule() {
        super(
                "Fast Terrain Rendering",
                "Reduces weather range and uses cheaper leaf/transparency paths to lower chunk and fragment rendering cost."
        );
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previousWeatherRadius = options.method_75333().method_41753();
        previousCutoutLeaves = options.method_75334().method_41753();
        previousImprovedTransparency = options.method_75337().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        options.method_75333().method_41748(0);
        options.method_75334().method_41748(false);
        options.method_75337().method_41748(false);
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previousWeatherRadius != null) options.method_75333().method_41748(previousWeatherRadius);
        if (previousCutoutLeaves != null) options.method_75334().method_41748(previousCutoutLeaves);
        if (previousImprovedTransparency != null) options.method_75337().method_41748(previousImprovedTransparency);
    }

    @Override
    protected void clearPrevious() {
        previousWeatherRadius = null;
        previousCutoutLeaves = null;
        previousImprovedTransparency = null;
    }
}
