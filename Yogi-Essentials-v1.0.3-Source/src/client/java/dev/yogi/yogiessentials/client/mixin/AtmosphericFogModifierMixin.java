package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.module.smp.FogCustomizerModule;
import dev.yogi.yogiessentials.client.render.fog.FogMixinSupport;
import net.minecraft.class_11398;
import net.minecraft.class_1937;
import net.minecraft.class_4184;
import net.minecraft.class_638;
import net.minecraft.class_7285;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_11398.class)
public abstract class AtmosphericFogModifierMixin {
    @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
    private void yogiessentials$applyCustomDimensionFog(
            class_7285 data,
            class_4184 camera,
            class_638 world,
            float viewDistance,
            class_9779 tickCounter,
            CallbackInfo ci
    ) {
        if (world == null) return;

        FogCustomizerModule.FogProfile profile;
        if (class_1937.field_25180.equals(world.method_27983())) {
            profile = FogCustomizerModule.FogProfile.NETHER;
        } else if (class_1937.field_25181.equals(world.method_27983())) {
            profile = FogCustomizerModule.FogProfile.END;
        } else if (class_1937.field_25179.equals(world.method_27983())) {
            profile = FogCustomizerModule.FogProfile.OVERWORLD;
        } else {
            return;
        }

        FogMixinSupport.apply(profile, data);
    }
}
