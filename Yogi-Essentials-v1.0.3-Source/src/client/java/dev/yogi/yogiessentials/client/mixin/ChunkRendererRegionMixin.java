package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.util.AnchorPredictionManager;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_853;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;









@Mixin(class_853.class)
public abstract class ChunkRendererRegionMixin {

    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$hidePredictedAnchorRender(
            class_2338 pos,
            CallbackInfoReturnable<class_2680> cir
    ) {
        if (AnchorPredictionManager.shouldHideRender(pos)) {
            cir.setReturnValue(class_2246.field_10124.method_9564());
        }
    }
}
