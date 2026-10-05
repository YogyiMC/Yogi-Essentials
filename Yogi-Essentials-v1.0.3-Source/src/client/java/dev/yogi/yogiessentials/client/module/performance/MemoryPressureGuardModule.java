package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_310;

/**
 * Avoids requesting full GC during gameplay. Instead, sustained heap pressure
 * trims simulation work so allocation bursts are less likely to turn into long
 * collection pauses.
 */
public final class MemoryPressureGuardModule extends Module {
    private final NumberSetting pressurePercent =
            new NumberSetting("Heap Pressure Trigger (%)", 84.0, 70.0, 95.0, 1.0);
    private final NumberSetting minimumSimulation =
            new NumberSetting("Minimum Simulation Distance", 5.0, 5.0, 12.0, 1.0);

    private Integer originalSimulation;
    private Integer appliedSimulation;
    private long pressureSince;
    private long healthySince;
    private long lastCheck;
    private long lastChange;

    public MemoryPressureGuardModule() {
        super(
                "Memory Pressure Guard",
                "Reduces simulation load during sustained heap pressure to lower allocation bursts and long garbage-collection freezes.",
                Category.PERFORMANCE
        );
        addSetting(pressurePercent);
        addSetting(minimumSimulation);
    }

    @Override
    protected void onDisable() {
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1690 != null && originalSimulation != null
                && appliedSimulation != null
                && client.field_1690.method_42510().method_41753().equals(appliedSimulation)) {
            client.field_1690.method_42510().method_41748(originalSimulation);
        }
        reset();
    }

    public void tick(class_310 client) {
        if (PerformanceOptimizer.optimizerOwnsRuntimeOptions()) return;
        if (!isEnabled() || client == null || client.field_1687 == null || client.field_1690 == null) {
            pressureSince = 0L;
            healthySince = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastCheck < 1500L) return;
        lastCheck = now;

        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        if (max <= 0L) return;
        long used = runtime.totalMemory() - runtime.freeMemory();
        double pressure = used * 100.0 / max;

        if (pressure >= pressurePercent.get()) {
            healthySince = 0L;
            if (pressureSince == 0L) pressureSince = now;
            if (now - pressureSince >= 4000L && now - lastChange >= 10000L) {
                reduceSimulation(client);
                pressureSince = now;
            }
        } else if (pressure <= pressurePercent.get() - 15.0) {
            pressureSince = 0L;
            if (healthySince == 0L) healthySince = now;
            if (now - healthySince >= 60000L && now - lastChange >= 30000L) {
                restoreStep(client);
                healthySince = now;
            }
        } else {
            pressureSince = 0L;
            healthySince = 0L;
        }
    }

    private void reduceSimulation(class_310 client) {
        int current = client.field_1690.method_42510().method_41753();
        int floor = minimumSimulation.get().intValue();
        if (current <= floor) return;
        if (originalSimulation == null) originalSimulation = current;
        if (appliedSimulation != null && current != appliedSimulation) return;
        appliedSimulation = current - 1;
        client.field_1690.method_42510().method_41748(appliedSimulation);
        lastChange = System.currentTimeMillis();
    }

    private void restoreStep(class_310 client) {
        if (originalSimulation == null || appliedSimulation == null) return;
        int current = client.field_1690.method_42510().method_41753();
        if (current != appliedSimulation) return;
        int restored = Math.min(originalSimulation, current + 1);
        client.field_1690.method_42510().method_41748(restored);
        appliedSimulation = restored == originalSimulation ? null : restored;
        lastChange = System.currentTimeMillis();
    }

    private void reset() {
        originalSimulation = null;
        appliedSimulation = null;
        pressureSince = 0L;
        healthySince = 0L;
        lastCheck = 0L;
        lastChange = 0L;
    }
}
