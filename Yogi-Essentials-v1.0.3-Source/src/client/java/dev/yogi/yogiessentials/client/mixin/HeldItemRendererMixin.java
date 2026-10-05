package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.render.FirstPersonModelTransformOverride;
import dev.yogi.yogiessentials.client.render.YogiFirstPersonTransforms;
import net.minecraft.class_11659;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;










@Mixin(value = class_759.class, priority = 1500)
public abstract class HeldItemRendererMixin {

    private static final ThreadLocal<Boolean> YOGI_MODEL_PUSHED =
            ThreadLocal.withInitial(() -> false);

    
    
    

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/render/item/HeldItemRenderer;"
                                    + "renderItem("
                                    + "Lnet/minecraft/entity/LivingEntity;"
                                    + "Lnet/minecraft/item/ItemStack;"
                                    + "Lnet/minecraft/item/ItemDisplayContext;"
                                    + "Lnet/minecraft/client/util/math/MatrixStack;"
                                    + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
                                    + "I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void yogiessentials$beforeItemRender(
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
        FirstPersonModelTransformOverride.beginGameplay(player, hand, item);

        boolean shield = item.method_31574(class_1802.field_8255);
        boolean totem = item.method_31574(class_1802.field_8288);

        if (!shield && !totem) {
            YOGI_MODEL_PUSHED.set(false);
            return;
        }

        matrices.method_22903();
        YOGI_MODEL_PUSHED.set(true);

        if (shield) {
            YogiFirstPersonTransforms.applyShieldGameplay(player, hand, matrices);
        }

        if (totem) {
            YogiFirstPersonTransforms.applyTotemGameplay(hand, matrices);
        }
    }

    
    
    

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/render/item/HeldItemRenderer;"
                                    + "renderItem("
                                    + "Lnet/minecraft/entity/LivingEntity;"
                                    + "Lnet/minecraft/item/ItemStack;"
                                    + "Lnet/minecraft/item/ItemDisplayContext;"
                                    + "Lnet/minecraft/client/util/math/MatrixStack;"
                                    + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
                                    + "I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void yogiessentials$afterItemRender(
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
        FirstPersonModelTransformOverride.end();

        if (!YOGI_MODEL_PUSHED.get()) {
            return;
        }

        matrices.method_22909();
        YOGI_MODEL_PUSHED.set(false);
    }
}
