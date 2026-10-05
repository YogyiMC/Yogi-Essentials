package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.performance.RenderSubmissionBudgetModule;
import dev.yogi.yogiessentials.client.module.performance.DynamicBlockEntityOptimizerModule;
import dev.yogi.yogiessentials.client.module.performance.ViewportCullingModule;
import dev.yogi.yogiessentials.client.util.ViewportCulling;
import net.minecraft.class_11659;
import net.minecraft.class_11954;
import net.minecraft.class_12075;
import net.minecraft.class_4587;
import net.minecraft.class_824;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_824.class)
public abstract class BlockEntityRenderManagerCullingMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <S extends class_11954> void yogi$cullOutsideViewport(
            S state, class_4587 matrices, class_11659 queue,
            class_12075 cameraRenderState, CallbackInfo ci) {
        if (state == null || state.field_62673 == null) return;

        if (YogiEssentialsClient.getModuleManager() != null) {
            DynamicBlockEntityOptimizerModule optimizer = YogiEssentialsClient.getModuleManager()
                    .getModule(DynamicBlockEntityOptimizerModule.class);
            if (optimizer != null && !optimizer.allow(state)) {
                ci.cancel();
                return;
            }
        }

        ViewportCullingModule module = ViewportCulling.module();
        if (module != null && module.isEnabled() && module.cullBlockEntities()) {
            if (!ViewportCulling.pointVisible(
                    state.field_62673.method_10263() + 0.5,
                    state.field_62673.method_10264() + 0.5,
                    state.field_62673.method_10260() + 0.5,
                    1.25,
                    module.edgeMarginDegrees())) {
                ViewportCulling.recordOffscreenBlockEntityReject();
                ci.cancel();
                return;
            }

            if (module.occludeBlockEntities()
                    && !ViewportCulling.blockEntityTerrainVisible(state.field_62673, module.occlusionSafetyRadiusBlocks())) {
                ViewportCulling.recordOccludedBlockEntityReject();
                ci.cancel();
                return;
            }
        }

        if (YogiEssentialsClient.getModuleManager() != null) {
            RenderSubmissionBudgetModule budget = YogiEssentialsClient.getModuleManager()
                    .getModule(RenderSubmissionBudgetModule.class);
            if (budget != null && !budget.allowBlockEntity(state.field_62673)) {
                ci.cancel();
            }
        }
    }
}
