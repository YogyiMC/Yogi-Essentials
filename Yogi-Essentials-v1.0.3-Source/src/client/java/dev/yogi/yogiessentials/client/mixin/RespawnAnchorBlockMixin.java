package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.util.AnchorPredictionManager;
import net.minecraft.class_12206;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_4969;
import net.minecraft.class_638;
import dev.yogi.yogiessentials.client.module.pvp.CombatSoundContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;






@Mixin(class_4969.class)
public abstract class RespawnAnchorBlockMixin {

    @Inject(
            method = "onUse(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD")
    )
    private void yogiessentials$predictManualAnchorExplosion(
            class_2680 state,
            class_1937 world,
            class_2338 pos,
            class_1657 player,
            class_3965 hit,
            CallbackInfoReturnable<class_1269> cir
    ) {
        if (world instanceof class_638 clientWorld && state.method_27852(class_2246.field_23152)
                && state.method_11654(class_4969.field_23153) > 0
                && !world.method_75728().method_75697(
                        class_12206.field_63757, pos)) {
            CombatSoundContext.onAnchorActivated(clientWorld, pos);
        }
        AnchorPredictionManager.onManualAnchorUse(state, world, pos, player);
    }
}
