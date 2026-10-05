package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.render.FirstPersonModelTransformOverride;
import net.minecraft.class_10444;
import net.minecraft.class_4587;
import net.minecraft.class_804;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;





@Mixin(value = class_10444.class_10446.class, priority = 1500)
public abstract class ItemRenderStateLayerMixin {

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/model/json/Transformation;apply(ZLnet/minecraft/client/util/math/MatrixStack$Entry;)V"
            )
    )
    private void yogi$useVanillaFirstPersonBaseline(
            class_804 transformation,
            boolean leftHand,
            class_4587.class_4665 entry
    ) {
        FirstPersonModelTransformOverride.apply(transformation, leftHand, entry);
    }
}
