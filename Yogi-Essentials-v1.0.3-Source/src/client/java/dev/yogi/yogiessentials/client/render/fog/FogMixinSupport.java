package dev.yogi.yogiessentials.client.render.fog;

import dev.yogi.yogiessentials.client.module.smp.FogCustomizerModule;
import net.minecraft.class_7285;







public final class FogMixinSupport {
    private FogMixinSupport() {
    }

    public static void apply(FogCustomizerModule.FogProfile profile, class_7285 data) {
        FogCustomizerModule module = FogCustomizerModule.enabledInstance();
        if (module != null) {
            module.applyToFogData(profile, data);
        }
    }
}
