package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_310;

/** Keeps high render distance visible while preventing simulation radius from scaling with it. */
public final class HighDistanceGuardModule extends Module {
    private final NumberSetting threshold =
            new NumberSetting("High Distance Threshold", 20.0, 12.0, 32.0, 1.0);
    private final NumberSetting simulationCap =
            new NumberSetting("High Distance Simulation Cap", 6.0, 5.0, 12.0, 1.0);
    private Integer originalSimulation;
    private Integer appliedSimulation;

    public HighDistanceGuardModule() {
        super(
                "High Distance Guard",
                "Keeps large terrain render distances intact while capping nearby simulation work that commonly causes CPU spikes and chunk stutter.",
                Category.PERFORMANCE
        );
        addSetting(threshold);
        addSetting(simulationCap);
    }

    @Override
    protected void onDisable() {
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1690 != null && originalSimulation != null
                && appliedSimulation != null
                && client.field_1690.method_42510().method_41753().equals(appliedSimulation)) {
            client.field_1690.method_42510().method_41748(originalSimulation);
        }
        originalSimulation = null;
        appliedSimulation = null;
    }

    public void tick(class_310 client) {
        if (PerformanceOptimizer.optimizerOwnsRuntimeOptions()) return;
        if (!isEnabled() || client == null || client.field_1690 == null || client.field_1687 == null) return;
        int render = client.field_1690.method_42503().method_41753();
        if (render < threshold.get().intValue()) return;
        int current = client.field_1690.method_42510().method_41753();
        int cap = Math.min(render, simulationCap.get().intValue());
        if (current <= cap) return;
        if (originalSimulation == null) originalSimulation = current;
        client.field_1690.method_42510().method_41748(cap);
        appliedSimulation = cap;
    }
}
