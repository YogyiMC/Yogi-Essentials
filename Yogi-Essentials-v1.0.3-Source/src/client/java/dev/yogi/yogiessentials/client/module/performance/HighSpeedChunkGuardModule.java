package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_310;

/**
 * Reduces simulation work only while the player is moving quickly enough to
 * make chunk arrival/rebuild pressure the dominant source of stutter. Terrain
 * render distance is never changed, and the original simulation distance is
 * restored after movement settles.
 */
public final class HighSpeedChunkGuardModule extends Module {
    private final NumberSetting speedThreshold = new NumberSetting("Activation Speed (blocks/tick)", 0.65, 0.25, 2.0, 0.05);
    private final NumberSetting simulationCap = new NumberSetting("High-Speed Simulation Cap", 5, 5, 10, 1);
    private final NumberSetting recoverySeconds = new NumberSetting("Recovery Delay (seconds)", 5, 1, 20, 1);

    private Integer originalSimulation;
    private Integer appliedSimulation;
    private long lastFastMillis;

    public HighSpeedChunkGuardModule() {
        super(
                "High-Speed Chunk Guard",
                "Reduces simulation pressure during fast travel to keep chunk loading from causing large frame spikes without lowering render distance.",
                Category.PERFORMANCE
        );
        addSetting(speedThreshold);
        addSetting(simulationCap);
        addSetting(recoverySeconds);
    }

    @Override
    protected void onDisable() {
        restoreIfOwned(class_310.method_1551());
        originalSimulation = null;
        appliedSimulation = null;
        lastFastMillis = 0L;
    }

    public void tick(class_310 client) {
        if (PerformanceOptimizer.optimizerOwnsRuntimeOptions()) return;
        if (!isEnabled() || client == null || client.field_1687 == null || client.field_1724 == null || client.field_1690 == null) return;
        var velocity = client.field_1724.method_18798();
        double speedSq = velocity.field_1352 * velocity.field_1352 + velocity.field_1351 * velocity.field_1351 + velocity.field_1350 * velocity.field_1350;
        double threshold = speedThreshold.get();
        long now = System.currentTimeMillis();
        if (speedSq >= threshold * threshold) {
            lastFastMillis = now;
            int current = client.field_1690.method_42510().method_41753();
            int cap = Math.min(client.field_1690.method_42503().method_41753(), simulationCap.get().intValue());
            if (current > cap) {
                if (originalSimulation == null) originalSimulation = current;
                client.field_1690.method_42510().method_41748(cap);
                appliedSimulation = cap;
                }
            return;
        }

        if (lastFastMillis != 0L && now - lastFastMillis >= Math.round(recoverySeconds.get() * 1000.0)) {
            restoreIfOwned(client);
            lastFastMillis = 0L;
        }
    }

    private void restoreIfOwned(class_310 client) {
        if (client == null || client.field_1690 == null || originalSimulation == null || appliedSimulation == null) return;
        if (client.field_1690.method_42510().method_41753().equals(appliedSimulation)) {
            client.field_1690.method_42510().method_41748(originalSimulation);
        }
        originalSimulation = null;
        appliedSimulation = null;
    }
}
