package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.hud.HudManager;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import net.minecraft.class_310;







public final class MemoryStabilityModule extends Module {

    private static final int CLEANUP_DELAY_TICKS = 60;
    private static final double HEAP_PRESSURE_THRESHOLD = 0.78;
    private static final long MIN_CLEANUP_INTERVAL_MS = 30_000L;

    private boolean hadWorld;
    private int cleanupTicks = -1;
    private long lastCleanupMillis;

    public MemoryStabilityModule() {
        super(
                "Memory Stability",
                "Clears Yogi Essentials transient state between worlds and only requests JVM cleanup outside gameplay when heap pressure is unusually high.",
                Category.PERFORMANCE
        );
    }

    @Override
    protected void onEnable() {
        class_310 client = class_310.method_1551();
        hadWorld = client != null && client.field_1687 != null;
        cleanupTicks = -1;
    }

    @Override
    protected void onDisable() {
        cleanupTicks = -1;
        hadWorld = false;
    }

    public void reconcileMemory(
            class_310 client
    ) {
        if (!isEnabled() || client == null) {
            return;
        }

        boolean hasWorld = client.field_1687 != null;

        if (hadWorld && !hasWorld) {
            HudManager.clearTransientState();
            cleanupTicks = CLEANUP_DELAY_TICKS;
        }

        hadWorld = hasWorld;

        if (hasWorld || cleanupTicks < 0) {
            return;
        }

        if (cleanupTicks > 0) {
            cleanupTicks--;
            return;
        }

        cleanupTicks = -1;

        long now = System.currentTimeMillis();

        if (now - lastCleanupMillis < MIN_CLEANUP_INTERVAL_MS) {
            return;
        }

        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();

        if (max <= 0L) {
            return;
        }

        double pressure = used / (double) max;

        if (pressure >= HEAP_PRESSURE_THRESHOLD) {
            HudManager.clearTransientState();
            dev.yogi.yogiessentials.client.util.ViewportCulling.clearCaches();
            YogiTerrainScheduler.clear();
            lastCleanupMillis = now;
        }
    }
}
