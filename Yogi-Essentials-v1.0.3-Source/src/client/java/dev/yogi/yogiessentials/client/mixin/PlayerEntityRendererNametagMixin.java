package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.visual.NametagCustomizerModule;
import dev.yogi.yogiessentials.client.util.NametagRenderContext;
import net.minecraft.class_10055;
import net.minecraft.class_1007;
import net.minecraft.class_11659;
import net.minecraft.class_11890;
import net.minecraft.class_12075;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stable native-label implementation used by Yogi Essentials nametag styling. */
@Mixin(class_1007.class)
public abstract class PlayerEntityRendererNametagMixin {
    @Inject(
            method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void yogiEssentials$controlPlayerLabelVisibility(
            class_11890 entity,
            double squaredDistanceToCamera,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(entity instanceof class_1657 player)) return;

        NametagCustomizerModule module = yogiEssentials$getModule();
        class_310 client = class_310.method_1551();
        if (module == null || !module.isEnabled() || client.field_1724 == null) return;
        if (!module.appliesToEntityId(player.method_5628(), client.field_1724.method_5628())) return;

        if (player == client.field_1724 && client.field_1690.method_31044().method_31034()) {
            cir.setReturnValue(false);
            return;
        }

        if (!module.getShowNametag().get()) {
            cir.setReturnValue(false);
            return;
        }

        if (module.getShowServerNametags().get()) {
            return;
        }

        boolean visible = !player.method_5767() && squaredDistanceToCamera <= 4096.0D;
        cir.setReturnValue(visible);
    }

    @Inject(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void yogiEssentials$beginPlayerNametag(
            class_10055 state,
            class_4587 matrices,
            class_11659 queue,
            class_12075 cameraState,
            CallbackInfo ci
    ) {
        NametagRenderContext.endPlayerNametag();

        NametagCustomizerModule module = yogiEssentials$getModule();
        class_310 client = class_310.method_1551();
        if (module == null || !module.isEnabled() || client.field_1724 == null) return;
        if (!module.appliesToEntityId(state.field_53528, client.field_1724.method_5628())) return;

        if (state.field_53528 == client.field_1724.method_5628() && client.field_1690.method_31044().method_31034()) {
            ci.cancel();
            return;
        }

        if (!module.getShowNametag().get() || state.field_53333 || state.field_53337 == null) {
            ci.cancel();
            return;
        }

        if (module.getShowServerNametags().get()) {
            return;
        }

        NametagRenderContext.beginPlayerNametag(state.field_53337);
    }

    @Inject(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("RETURN")
    )
    private void yogiEssentials$endPlayerNametag(
            class_10055 state,
            class_4587 matrices,
            class_11659 queue,
            class_12075 cameraState,
            CallbackInfo ci
    ) {
        NametagRenderContext.endPlayerNametag();
    }

    private static NametagCustomizerModule yogiEssentials$getModule() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        return YogiEssentialsClient.getModuleManager().getModule(NametagCustomizerModule.class);
    }
}
