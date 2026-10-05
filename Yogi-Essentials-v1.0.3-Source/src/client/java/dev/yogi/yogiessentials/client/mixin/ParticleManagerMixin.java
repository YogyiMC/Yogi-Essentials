package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.NoPotionParticlesModule;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_238;
import net.minecraft.class_2394;
import net.minecraft.class_2396;
import net.minecraft.class_2398;
import net.minecraft.class_310;
import net.minecraft.class_702;
import net.minecraft.class_703;
import dev.yogi.yogiessentials.client.module.performance.ParticleBudgetModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_702.class)
public abstract class ParticleManagerMixin {

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void yogiessentials$filterPotionParticles(
            class_2394 parameters,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            CallbackInfoReturnable<class_703> cir
    ) {
        if (YogiEssentialsClient.getModuleManager() == null || parameters == null) return;

        ParticleBudgetModule budget = YogiEssentialsClient.getModuleManager()
                .getModule(ParticleBudgetModule.class);
        if (budget != null && budget.shouldCull(x, y, z)) {
            cir.setReturnValue(null);
            return;
        }

        NoPotionParticlesModule module = YogiEssentialsClient.getModuleManager()
                .getModule(NoPotionParticlesModule.class);
        if (module == null || !module.isEnabled()) return;

        class_2396<?> type = parameters.method_10295();
        if (type != class_2398.field_11226
                && type != class_2398.field_11245
                && type != class_2398.field_11213) {
            return;
        }

        class_310 client = class_310.method_1551();
        if (client == null || client.field_1687 == null || client.field_1724 == null) return;

        class_238 ownBox = client.field_1724.method_5829().method_1014(1.15);
        boolean insideOwn = ownBox.method_1008(x, y, z);
        boolean likelySplash = type == class_2398.field_11245
                || type == class_2398.field_11213;

        if (insideOwn && module.getHideOwnEffects().get() && !likelySplash) {
            cir.setReturnValue(null);
            return;
        }

        if (module.getHideOtherEntityEffects().get() && !likelySplash) {
            class_238 pointBox = new class_238(
                    x - 0.30, y - 0.30, z - 0.30,
                    x + 0.30, y + 0.30, z + 0.30
            );

            for (class_1297 entity : client.field_1687.method_8333(
                    client.field_1724,
                    pointBox,
                    candidate -> candidate instanceof class_1309 && candidate.method_5805()
            )) {
                if (entity.method_5829().method_1014(0.30).method_1008(x, y, z)) {
                    cir.setReturnValue(null);
                    return;
                }
            }
        }

        if (module.getHideSplashEffects().get() && likelySplash) {
            cir.setReturnValue(null);
        }
    }
}
