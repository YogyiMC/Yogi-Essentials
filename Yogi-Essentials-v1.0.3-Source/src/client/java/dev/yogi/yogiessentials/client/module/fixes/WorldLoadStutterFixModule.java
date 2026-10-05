package dev.yogi.yogiessentials.client.module.fixes;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_310;

/**
 * A short, non-invasive warmup window after world/dimension changes. Expensive
 * optional visibility probes and chunk work are temporarily reduced instead of
 * competing with the initial world-load burst.
 */
public final class WorldLoadStutterFixModule extends Module {
    private final NumberSetting warmupSeconds = new NumberSetting("Warmup Duration (seconds)", 3.0, 1.0, 10.0, 0.5);
    private final NumberSetting warmupBudget = new NumberSetting("Warmup Work Budget (%)", 55.0, 25.0, 100.0, 5.0);
    private final BooleanSetting pauseTerrainOcclusion = new BooleanSetting("Pause Terrain Occlusion During Warmup", true);

    private Object lastWorld;
    private long warmupUntil;

    public WorldLoadStutterFixModule() {
        super(
                "World Load Stutter Fix",
                "Smooths joins, respawns, and dimension changes by temporarily reducing optional Yogi Essentials render work during the initial load burst.",
                Category.FIXES
        );
        addSetting(warmupSeconds);
        addSetting(warmupBudget);
        addSetting(pauseTerrainOcclusion);
        setEnabled(true);
    }

    public void tick(class_310 client) {
        if (!isEnabled() || client == null) {
            lastWorld = null;
            warmupUntil = 0L;
            return;
        }
        if (client.field_1687 != lastWorld) {
            lastWorld = client.field_1687;
            if (client.field_1687 != null) {
                warmupUntil = System.currentTimeMillis() + Math.round(warmupSeconds.get() * 1000.0);
            } else {
                warmupUntil = 0L;
            }
        }
    }

    public boolean isWarmupActive() {
        return isEnabled() && warmupUntil > System.currentTimeMillis();
    }

    public double workBudgetScale() {
        return isWarmupActive() ? Math.max(0.25, Math.min(1.0, warmupBudget.get() / 100.0)) : 1.0;
    }

    public boolean shouldPauseTerrainOcclusion() {
        return isWarmupActive() && pauseTerrainOcclusion.get();
    }

    public static double activeWorkScale() {
        if (YogiEssentialsClient.getModuleManager() == null) return 1.0;
        WorldLoadStutterFixModule module = YogiEssentialsClient.getModuleManager().getModule(WorldLoadStutterFixModule.class);
        return module == null ? 1.0 : module.workBudgetScale();
    }

    public static boolean pauseTerrainOcclusionNow() {
        if (YogiEssentialsClient.getModuleManager() == null) return false;
        WorldLoadStutterFixModule module = YogiEssentialsClient.getModuleManager().getModule(WorldLoadStutterFixModule.class);
        return module != null && module.shouldPauseTerrainOcclusion();
    }

    @Override
    protected void onDisable() {
        warmupUntil = 0L;
        lastWorld = null;
    }
}
