package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.performance.RenderSubmissionBudgetModule;
import dev.yogi.yogiessentials.client.module.performance.EntityRenderDistanceModule;
import dev.yogi.yogiessentials.client.module.performance.ViewportCullingModule;
import dev.yogi.yogiessentials.client.module.visual.NametagCustomizerModule;
import dev.yogi.yogiessentials.client.util.ViewportCulling;
import net.minecraft.class_1297;
import net.minecraft.class_238;
import net.minecraft.class_310;
import net.minecraft.class_4604;
import net.minecraft.class_898;
import dev.yogi.yogiessentials.client.util.ServerNametagDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_898.class)
public abstract class EntityRenderManagerCullingMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends class_1297> void yogi$cullOutsideViewport(
            E entity, class_4604 frustum, double x, double y, double z,
            CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) return;

        class_310 client = class_310.method_1551();
        ServerNametagDetector.observeCarrier(entity, client);
        if (yogiessentials$hideServerNametagCarrier(entity, client)) {
            cir.setReturnValue(false);
            return;
        }
        if (client != null && client.field_1773 != null && client.field_1773.method_19418() != null
                && entity == client.field_1773.method_19418().method_19331()) {
            return;
        }

        if (YogiEssentialsClient.getModuleManager() != null) {
            EntityRenderDistanceModule distances = YogiEssentialsClient.getModuleManager()
                    .getModule(EntityRenderDistanceModule.class);
            if (distances != null && !distances.allow(entity)) {
                cir.setReturnValue(false);
                return;
            }
        }

        ViewportCullingModule module = ViewportCulling.module();
        if (module != null && module.isEnabled() && module.cullEntities()) {
            class_238 box = entity.method_5829();
            // The live renderer frustum is the authoritative on-screen test and has
            double marginBlocks = Math.min(0.35, module.edgeMarginDegrees() * 0.025);
            if (!ViewportCulling.boxVisible(frustum, box, marginBlocks)) {
                ViewportCulling.recordOffscreenEntityReject();
                cir.setReturnValue(false);
                return;
            }

            double centerX = (box.field_1323 + box.field_1320) * 0.5;
            double centerY = (box.field_1322 + box.field_1325) * 0.5;
            double centerZ = (box.field_1321 + box.field_1324) * 0.5;
            if (!ViewportCulling.pointVisible(centerX, centerY, centerZ, ViewportCulling.boundingRadius(box),
                    module.edgeMarginDegrees())) {
                ViewportCulling.recordOffscreenEntityReject();
                cir.setReturnValue(false);
                return;
            }

            if (module.occludeEntities()
                    && !ViewportCulling.entityTerrainVisible(entity, module.occlusionSafetyRadiusBlocks())) {
                ViewportCulling.recordOccludedEntityReject();
                cir.setReturnValue(false);
                return;
            }
        }

        if (YogiEssentialsClient.getModuleManager() != null) {
            RenderSubmissionBudgetModule budget = YogiEssentialsClient.getModuleManager()
                    .getModule(RenderSubmissionBudgetModule.class);
            if (budget != null && !budget.allowEntity(entity)) {
                cir.setReturnValue(false);
            }
        }
    }
    private static boolean yogiessentials$hideServerNametagCarrier(class_1297 entity, class_310 client) {
        if (client == null || client.field_1687 == null || YogiEssentialsClient.getModuleManager() == null) return false;
        NametagCustomizerModule nametags = YogiEssentialsClient.getModuleManager()
                .getModule(NametagCustomizerModule.class);
        if (nametags == null || !nametags.isEnabled() || !nametags.getShowNametag().get()
                || nametags.getShowServerNametags().get()) return false;

        return ServerNametagDetector.isServerNametagCarrier(entity);
    }

}
