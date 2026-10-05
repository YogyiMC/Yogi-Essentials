package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.ToggleSprintModule;
import net.minecraft.class_310;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Final post-movement authority for Toggle Sprint. Vanilla sprint logic runs
 * first, then Yogi Essentials reapplies the user's explicit toggle state.
 */
@Mixin(class_746.class)
public abstract class ClientPlayerSprintStateMixin {
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void yogiessentials$enforceToggleSprintAfterMovement(CallbackInfo ci) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || (Object) this != client.field_1724) return;
        if (YogiEssentialsClient.getModuleManager() == null) return;

        ToggleSprintModule module = YogiEssentialsClient.getModuleManager().getModule(ToggleSprintModule.class);
        if (module == null || !module.isEnabled()) return;

        boolean shouldSprint = module.shouldForceSprint();
        if (client.field_1690 != null) {
            client.field_1690.field_1867.method_23481(shouldSprint);
        }

        class_746 player = client.field_1724;
        if (!shouldSprint) {
            if (player.method_5624()) player.method_5728(false);
            return;
        }

        boolean canSprint = client.field_1690 != null
                && client.field_1690.field_1894.method_1434()
                && !player.method_5715()
                && !player.field_5976
                && player.method_7344().method_7586() > 6;
        if (canSprint && !player.method_5624()) {
            player.method_5728(true);
        }
    }
}
