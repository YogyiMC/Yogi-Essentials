package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

/**
 * Strict per-frame viewport culling plus optional terrain occlusion checks.
 * Off-screen culling never uses distance as an exemption: a renderable that is
 * outside the live camera view is rejected whether it is close or far away.
 */
public final class ViewportCullingModule extends Module {
    private final BooleanSetting entities =
            new BooleanSetting("Cull Off-Screen Entities", true);
    private final BooleanSetting blockEntities =
            new BooleanSetting("Cull Off-Screen Block Entities", true);
    private final BooleanSetting occlusion =
            new BooleanSetting("Terrain Occlusion Culling", false);
    private final BooleanSetting occludeEntities =
            new BooleanSetting("Occlude Hidden Entities", true);
    private final BooleanSetting occludeBlockEntities =
            new BooleanSetting("Occlude Hidden Block Entities", true);
    private final NumberSetting edgeMargin =
            new NumberSetting("Viewport Edge Margin (degrees)", 0.0, 0.0, 12.0, 1.0);
    private final NumberSetting occlusionSafetyRadius =
            new NumberSetting("Occlusion Safety Radius (blocks)", 2.0, 0.0, 8.0, 1.0);
    private final NumberSetting entityOcclusionBudget =
            new NumberSetting("Entity Occlusion Checks / Frame", 16.0, 4.0, 128.0, 4.0);
    private final NumberSetting blockEntityOcclusionBudget =
            new NumberSetting("Block Entity Occlusion Checks / Frame", 8.0, 2.0, 64.0, 2.0);

    public ViewportCullingModule() {
        super(
                "Viewport Culling",
                "Strictly skips off-screen entities at any distance and can also skip entities hidden behind solid terrain.",
                Category.PERFORMANCE
        );
        addSetting(entities);
        addSetting(blockEntities);
        addSetting(occlusion);
        addSetting(occludeEntities);
        addSetting(occludeBlockEntities);
        addSetting(edgeMargin);
        addSetting(occlusionSafetyRadius);
        addSetting(entityOcclusionBudget);
        addSetting(blockEntityOcclusionBudget);
    }

    public boolean cullEntities() {
        return entities.get();
    }

    public boolean cullBlockEntities() {
        return blockEntities.get();
    }

    public boolean terrainOcclusion() {
        return occlusion.get();
    }

    /**
     * Auto Optimizer favors cheap frustum/cone culling. Per-object world raycasts
     * can cost more CPU than they save in dense scenes, so they are disabled in
     * the measured stack while remaining available as a manual advanced option.
     */
    public boolean prepareForAutoOptimizer() {
        boolean changed = occlusion.get() || occludeEntities.get() || occludeBlockEntities.get();
        occlusion.set(false);
        occludeEntities.set(false);
        occludeBlockEntities.set(false);
        return changed;
    }

    public boolean occludeEntities() {
        return occlusion.get() && occludeEntities.get();
    }

    public boolean occludeBlockEntities() {
        return occlusion.get() && occludeBlockEntities.get();
    }

    public double edgeMarginDegrees() {
        return edgeMargin.get();
    }

    public double occlusionSafetyRadiusBlocks() {
        return occlusionSafetyRadius.get();
    }

    public int entityOcclusionChecksPerFrame() {
        return entityOcclusionBudget.get().intValue();
    }

    public int blockEntityOcclusionChecksPerFrame() {
        return blockEntityOcclusionBudget.get().intValue();
    }
}
