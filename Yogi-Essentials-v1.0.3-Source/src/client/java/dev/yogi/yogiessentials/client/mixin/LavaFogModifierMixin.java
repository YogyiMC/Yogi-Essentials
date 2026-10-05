package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.module.smp.FogCustomizerModule;
import dev.yogi.yogiessentials.client.render.fog.FogMixinSupport;
import net.minecraft.class_11401;
import net.minecraft.class_4184;
import net.minecraft.class_638;
import net.minecraft.class_7285;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_11401.class)
public abstract class LavaFogModifierMixin {
    @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
    private void yogiessentials$applyCustomFog(
            class_7285 data,
            class_4184 camera,
            class_638 world,
            float viewDistance,
            class_9779 tickCounter,
            CallbackInfo ci
    ) {
        FogMixinSupport.apply(FogCustomizerModule.FogProfile.LAVA, data);
    }
}
