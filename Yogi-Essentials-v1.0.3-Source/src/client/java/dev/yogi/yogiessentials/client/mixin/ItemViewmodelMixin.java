package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.visual.ItemViewmodelModule;
import dev.yogi.yogiessentials.client.render.YogiFirstPersonTransforms;
import net.minecraft.class_11659;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;










@Mixin(class_759.class)
public abstract class ItemViewmodelMixin {

    @Unique
    private boolean yogi$itemTransformPushed;

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void yogi$applyItemViewmodel(
            class_742 player,
            float tickProgress,
            float pitch,
            class_1268 hand,
            float swingProgress,
            class_1799 item,
            float equipProgress,
            class_4587 matrices,
            class_11659 queue,
            int light,
            CallbackInfo ci
    ) {
        yogi$itemTransformPushed = false;

        if (YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        ItemViewmodelModule module =
                YogiEssentialsClient
                        .getModuleManager()
                        .getModule(ItemViewmodelModule.class);

        if (module == null
                || !module.isEnabled()
                || item == null
                || item.method_7960()) {
            return;
        }

        ItemViewmodelModule.ViewmodelSettings settings =
                module.getSettingsFor(item);

        if (settings == null || settings.isRegular()) {
            return;
        }

        matrices.method_22903();
        YogiFirstPersonTransforms.applyItemViewmodel(hand, settings, matrices);
        yogi$itemTransformPushed = true;
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void yogi$restoreItemViewmodel(
            class_742 player,
            float tickProgress,
            float pitch,
            class_1268 hand,
            float swingProgress,
            class_1799 item,
            float equipProgress,
            class_4587 matrices,
            class_11659 queue,
            int light,
            CallbackInfo ci
    ) {
        if (yogi$itemTransformPushed) {
            matrices.method_22909();
            yogi$itemTransformPushed = false;
        }
    }
}
