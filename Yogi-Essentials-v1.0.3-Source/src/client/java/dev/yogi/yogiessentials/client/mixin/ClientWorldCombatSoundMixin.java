package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.module.pvp.CombatSoundContext;
import net.minecraft.class_1297;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_638.class)
public abstract class ClientWorldCombatSoundMixin {
    @Inject(method = "addEntity(Lnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
    private void yogiessentials$trackCrystalAdded(class_1297 entity, CallbackInfo ci) {
        CombatSoundContext.onEntityAdded((class_638) (Object) this, entity);
    }

    @Inject(method = "removeEntity(ILnet/minecraft/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void yogiessentials$trackCrystalRemoved(int id, class_1297.class_5529 reason, CallbackInfo ci) {
        class_638 world = (class_638) (Object) this;
        CombatSoundContext.onEntityRemoved(world, world.method_8469(id));
    }

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("HEAD"))
    private void yogiessentials$trackAnchorRemoved(class_2338 pos, class_2680 state, int flags,
                                                     int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        class_638 world = (class_638) (Object) this;
        if (world.method_8320(pos).method_27852(class_2246.field_23152) && !state.method_27852(class_2246.field_23152)) {
            CombatSoundContext.onAnchorActivated(world, pos);
        }
    }
}
