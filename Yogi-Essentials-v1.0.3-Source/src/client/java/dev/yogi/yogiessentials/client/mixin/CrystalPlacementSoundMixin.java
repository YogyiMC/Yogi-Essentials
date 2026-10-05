package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.CombatSoundMixerModule;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3419;
import net.minecraft.class_3965;
import net.minecraft.class_636;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_636.class)
public abstract class CrystalPlacementSoundMixin {
    private static final class_3414 CRYSTAL_CUE = class_3414.method_47908(class_2960.method_60655("yogiessentials", "crystal_place"));
    @Unique private boolean yogiessentials$wasHoldingCrystal;

    @Inject(method = "interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"))
    private void yogiessentials$rememberCrystal(class_746 player, class_1268 hand,
                                                  class_3965 hit, CallbackInfoReturnable<class_1269> cir) {
        yogiessentials$wasHoldingCrystal = player != null && player.method_5998(hand).method_31574(class_1802.field_8301);
    }

    @Inject(method = "interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;", at = @At("RETURN"))
    private void yogiessentials$crystalPlacementCue(class_746 player, class_1268 hand,
                                                      class_3965 hit, CallbackInfoReturnable<class_1269> cir) {
        var world = class_310.method_1551().field_1687;
        if (player == null || world == null || hit == null || !yogiessentials$wasHoldingCrystal
                || cir.getReturnValue() == null || !cir.getReturnValue().method_23665()
                || YogiEssentialsClient.getModuleManager() == null) return;
        if (!world.method_8320(hit.method_17777()).method_27852(class_2246.field_10540)
                && !world.method_8320(hit.method_17777()).method_27852(class_2246.field_9987)) return;
        CombatSoundMixerModule module = YogiEssentialsClient.getModuleManager().getModule(CombatSoundMixerModule.class);
        if (module == null || !module.isEnabled()) return;
        class_2338 pos = hit.method_17777().method_10093(hit.method_17780());
        world.method_8486(pos.method_10263() + 0.5, pos.method_10264() + 0.5, pos.method_10260() + 0.5,
                CRYSTAL_CUE, class_3419.field_15245, 0.7F, 1.0F, false);
    }
}
