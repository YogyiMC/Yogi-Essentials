package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.util.HitRegistrationManager;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures vanilla attacks without cancelling or replacing them. */
@Mixin(class_636.class)
public abstract class ClientPlayerInteractionManagerHitRegistrationMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void yogiessentials$immediateHitFeedback(
            class_1657 player,
            class_1297 target,
            CallbackInfo ci
    ) {
        HitRegistrationManager.onAttack(player, target);
    }
}
