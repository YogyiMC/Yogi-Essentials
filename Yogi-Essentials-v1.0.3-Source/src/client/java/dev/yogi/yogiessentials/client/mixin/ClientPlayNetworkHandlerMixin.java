package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.util.AnchorPredictionManager;
import dev.yogi.yogiessentials.client.util.HitRegistrationManager;
import net.minecraft.class_2626;
import net.minecraft.class_2637;
import net.minecraft.class_634;
import net.minecraft.class_8143;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;





@Mixin(class_634.class)
public abstract class ClientPlayNetworkHandlerMixin {


    @Inject(method = "onBlockUpdate", at = @At("TAIL"))
    private void yogiessentials$clearAnchorPredictionOnBlockUpdate(
            class_2626 packet,
            CallbackInfo ci
    ) {
        AnchorPredictionManager.onServerBlockUpdate(packet.method_11309());
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("TAIL"))
    private void yogiessentials$clearAnchorPredictionOnChunkDelta(
            class_2637 packet,
            CallbackInfo ci
    ) {
        packet.method_30621((pos, state) ->
                AnchorPredictionManager.onServerBlockUpdate(pos));
    }
    @Inject(method = "onEntityDamage", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$suppressPredictedHitAnimation(
            class_8143 packet,
            CallbackInfo ci
    ) {
        if (HitRegistrationManager.shouldSuppressDamageFeedback(packet)) {
            ci.cancel();
        }
    }

}
