package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.module.smp.FogCustomizerModule;
import net.minecraft.class_7285;

/** Shared bridge used by the individual 1.21.11 vanilla FogModifier mixins. */
final class FogMixinSupport {
    private FogMixinSupport() {
    }

    static void apply(FogCustomizerModule.FogProfile profile, class_7285 data) {
        FogCustomizerModule module = FogCustomizerModule.enabledInstance();
        if (module != null) {
            module.applyToFogData(profile, data);
        }
    }
}
