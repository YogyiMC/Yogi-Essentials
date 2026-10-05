package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.CombatSoundMixerModule;
import net.minecraft.class_1102;
import net.minecraft.class_1113;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1102.class)
public abstract class AbstractSoundInstanceMixin {
    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void yogiessentials$mixSoundVolume(CallbackInfoReturnable<Float> cir) {
        if (YogiEssentialsClient.getModuleManager() == null) return;
        CombatSoundMixerModule module = YogiEssentialsClient.getModuleManager()
                .getModule(CombatSoundMixerModule.class);
        if (module == null || !module.isEnabled()) return;
        class_1113 sound = (class_1113) this;
        cir.setReturnValue(cir.getReturnValueF() * module.volume(sound));
    }
}
