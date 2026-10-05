package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.ToggleSprintModule;
import net.minecraft.class_1297;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the Toggle Sprint off-state authoritative for the local player so
 * vanilla or another client-side helper cannot immediately relatch sprint.
 */
@Mixin(class_1297.class)
public abstract class EntitySprintAuthorityMixin {
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$guardLocalSprint(boolean sprinting, CallbackInfo ci) {
        if (!sprinting) return;
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || (Object) this != client.field_1724) return;
        if (YogiEssentialsClient.getModuleManager() == null) return;
        ToggleSprintModule module = YogiEssentialsClient.getModuleManager().getModule(ToggleSprintModule.class);
        if (module != null && module.isEnabled() && !module.shouldForceSprint()) {
            ci.cancel();
        }
    }
}
