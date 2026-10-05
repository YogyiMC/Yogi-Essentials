package dev.yogi.yogiessentials.client.render;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.visual.LowFireModule;
import java.util.List;
import net.minecraft.class_1058;
import net.minecraft.class_1087;
import net.minecraft.class_10889;
import net.minecraft.class_5819;

/**
 * Suppresses the normal terrain-model fire only while Small Ground Fire is
 * active. The replacement visual is rendered separately by
 * YogiGroundFireRenderer with a standalone runtime texture, so no resource-pack
 * block model or atlas sprite is involved.
 */
public final class LowGroundFireBlockStateModel implements class_1087 {

    private final class_1087 delegate;

    public LowGroundFireBlockStateModel(class_1087 delegate) {
        this.delegate = delegate;
    }

    public class_1087 delegate() {
        return delegate;
    }

    @Override
    public void method_68513(class_5819 random, List<class_10889> parts) {
        if (!isSmallGroundFireActive()) {
            delegate.method_68513(random, parts);
        }
    }

    @Override
    public class_1058 method_68511() {
        return delegate.method_68511();
    }

    private static boolean isSmallGroundFireActive() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return false;
        }

        LowFireModule module = YogiEssentialsClient.getModuleManager().getModule(LowFireModule.class);
        return module != null && module.isEnabled() && module.isSmallGroundFire();
    }
}
