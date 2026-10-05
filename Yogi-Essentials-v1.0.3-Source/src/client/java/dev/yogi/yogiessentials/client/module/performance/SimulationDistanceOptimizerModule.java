package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_315;

/**
 * Caps simulation distance independently of render distance. This is useful
 * for high render-distance setups because chunks can stay visible farther out
 * without asking the client/server to simulate the same radius.
 */
public final class SimulationDistanceOptimizerModule extends OptionBackedPerformanceModule {
    private final NumberSetting maximum =
            new NumberSetting("Maximum Simulation Distance", 8.0, 5.0, 16.0, 1.0);
    private Integer previous;

    public SimulationDistanceOptimizerModule() {
        super(
                "Simulation Distance Optimizer",
                "Caps simulation distance independently from render distance to reduce CPU spikes while keeping far terrain visible."
        );
        addSetting(maximum);
    }

    @Override
    protected void capturePrevious(class_315 options) {
        previous = options.method_42510().method_41753();
    }

    @Override
    protected void applyEnabledValue(class_315 options) {
        int cap = maximum.get().intValue();
        int current = options.method_42510().method_41753();
        if (current > cap) {
            options.method_42510().method_41748(cap);
        }
    }

    @Override
    protected void restorePrevious(class_315 options) {
        if (previous != null) {
            options.method_42510().method_41748(previous);
        }
    }

    @Override
    protected void clearPrevious() {
        previous = null;
    }
}
