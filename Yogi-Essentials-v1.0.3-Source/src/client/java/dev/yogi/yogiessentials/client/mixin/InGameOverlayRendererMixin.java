package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.visual.LowFireModule;
import dev.yogi.yogiessentials.client.render.YogiDirectFireRenderer;
import net.minecraft.class_1058;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4603;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the first-person fire draw CALL, not its atlas sprite.
 *
 * <p>When Yogi Low First-Person Fire is active, vanilla renderFireOverlay is
 * not entered at all. This prevents an invisible resource-pack sprite/model
 * from influencing the result. The replacement binds a runtime texture that
 * was loaded directly from the mod jar.</p>
 */
@Mixin(class_4603.class)
public abstract class InGameOverlayRendererMixin {

    @Redirect(
            method = "renderOverlays",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameOverlayRenderer;renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V"
            )
    )
    private void yogiessentials$renderProtectedLowFire(
            class_4587 matrices,
            class_4597 vertexConsumers,
            class_1058 vanillaOrPackSprite
    ) {
        LowFireModule module = yogiessentials$getLowFire();
        if (module != null && module.isEnabled() && module.isFirstPersonLowFire()) {
            YogiDirectFireRenderer.renderFirstPerson(matrices, vertexConsumers);
            return;
        }

        InGameOverlayRendererInvoker.yogi$renderFireOverlay(
                matrices,
                vertexConsumers,
                vanillaOrPackSprite
        );
    }

    private static LowFireModule yogiessentials$getLowFire() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient.getModuleManager().getModule(LowFireModule.class);
    }
}
