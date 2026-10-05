package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.render.YogiGroundFireRenderer;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tracks fire block updates so Small Ground Fire appears/disappears immediately. */
@Mixin(class_638.class)
public abstract class ClientWorldLowFireMixin {

    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void yogiessentials$trackGroundFire(
            class_2338 pos,
            class_2680 state,
            int flags,
            CallbackInfo ci
    ) {
        YogiGroundFireRenderer.onBlockStateChanged(pos, state);
    }
}
