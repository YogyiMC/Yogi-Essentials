package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

/** Front door for the v1.0.3 renderer/backend architecture. */
public final class CustomTerrainRendererModule extends Module {
    private final EnumSetting<TerrainBackendManager.RequestedMode> backendMode =
            new EnumSetting<>("Backend Mode", TerrainBackendManager.RequestedMode.AUTO,
                    TerrainBackendManager.RequestedMode.class);
    private final BooleanSetting compatibilityLayer =
            new BooleanSetting("Keep Yogi Essentials Optimizations With Other Renderers", true);
    private final NumberSetting chunkFrameBudget =
            new NumberSetting("Chunk Frame Budget (%)", 8, 3, 35, 1);
    private final NumberSetting maxRebuildsPerFrame =
            new NumberSetting("Max Rebuilds / Frame", 4, 1, 24, 1);
    private final NumberSetting maxQueuedRebuilds =
            new NumberSetting("Max Queued Rebuilds", 2048, 256, 16384, 256);
    private final BooleanSetting coalesceRebuilds =
            new BooleanSetting("Coalesce Duplicate Rebuilds", true);
    private final BooleanSetting prioritizeNearby =
            new BooleanSetting("Prioritize Nearby Chunks", true);
    private final BooleanSetting prioritizeCameraFacing =
            new BooleanSetting("Prioritize Camera Direction", true);
    private final BooleanSetting deferBehindCamera =
            new BooleanSetting("Defer Behind-Camera Rebuilds", true);
    private final NumberSetting behindCameraShare =
            new NumberSetting("Behind-Camera Rebuild Share (%)", 10, 0, 50, 5);
    private final BooleanSetting prioritizeTravelDirection =
            new BooleanSetting("Prioritize Travel Direction", true);
    private final BooleanSetting adaptiveQueueBackpressure =
            new BooleanSetting("Adaptive Queue Backpressure", true);
    private final NumberSetting queuePressureThreshold =
            new NumberSetting("Queue Pressure Threshold (%)", 65, 25, 95, 5);
    private final NumberSetting healthyBurstRebuilds =
            new NumberSetting("Healthy Burst Rebuilds", 2, 0, 8, 1);

    public CustomTerrainRendererModule() {
        super(
                "Custom Terrain Renderer",
                "Selects the Yogi Essentials terrain backend or a Sodium/Nvidium/Vulkan-aware compatibility path without disabling Yogi Essentials optimizations.",
                Category.PERFORMANCE
        );
        addSetting(backendMode);
        addSetting(compatibilityLayer);
        addSetting(chunkFrameBudget);
        addSetting(maxRebuildsPerFrame);
        addSetting(maxQueuedRebuilds);
        addSetting(coalesceRebuilds);
        addSetting(prioritizeNearby);
        addSetting(prioritizeCameraFacing);
        addSetting(deferBehindCamera);
        addSetting(behindCameraShare);
        addSetting(prioritizeTravelDirection);
        addSetting(adaptiveQueueBackpressure);
        addSetting(queuePressureThreshold);
        addSetting(healthyBurstRebuilds);
    }

    public TerrainBackendManager.RequestedMode getBackendMode() { return backendMode.get(); }
    public boolean keepCompatibilityLayer() { return compatibilityLayer.get(); }
    public double chunkFrameBudgetPercent() { return chunkFrameBudget.get(); }
    public int maxRebuildsPerFrame() { return maxRebuildsPerFrame.get().intValue(); }
    public int maxQueuedRebuilds() { return maxQueuedRebuilds.get().intValue(); }
    public boolean coalesceRebuilds() { return coalesceRebuilds.get(); }
    public boolean prioritizeNearby() { return prioritizeNearby.get(); }
    public boolean prioritizeCameraFacing() { return prioritizeCameraFacing.get(); }
    public boolean deferBehindCamera() { return deferBehindCamera.get(); }
    public int behindCameraSharePercent() { return behindCameraShare.get().intValue(); }
    public boolean prioritizeTravelDirection() { return prioritizeTravelDirection.get(); }
    public boolean adaptiveQueueBackpressure() { return adaptiveQueueBackpressure.get(); }
    public int queuePressureThresholdPercent() { return queuePressureThreshold.get().intValue(); }
    public int healthyBurstRebuilds() { return healthyBurstRebuilds.get().intValue(); }

    public TerrainBackendManager.BackendStatus getBackendStatus() {
        return TerrainBackendManager.resolve(getBackendMode(), isEnabled());
    }

    public void refreshBackendSelection() {
        TerrainBackendManager.BackendStatus status = TerrainBackendManager.resolve(getBackendMode(), isEnabled());
        if (status.backend() != TerrainBackendManager.EffectiveBackend.YOGI_OPENGL) {
            YogiTerrainScheduler.clear();
        }
    }

    @Override protected void onEnable() { refreshBackendSelection(); }
    @Override protected void onDisable() { refreshBackendSelection(); }
    @Override protected void onSettingsReset() { refreshBackendSelection(); }
}
