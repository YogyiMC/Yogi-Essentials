package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.smp.FullbrightModule;
import net.minecraft.class_765;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;







@Mixin(class_765.class)
public abstract class LightmapTextureManagerMixin {

    @ModifyArg(
            method = "update(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
                    ordinal = 6
            ),
            index = 0
    )
    private float yogiessentials$fullbrightGamma(float vanillaGamma) {
        FullbrightModule module = module();
        return module == null ? vanillaGamma : module.applyLightmapGamma(vanillaGamma);
    }

    private static FullbrightModule module() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        return YogiEssentialsClient.getModuleManager().getModule(FullbrightModule.class);
    }
}
