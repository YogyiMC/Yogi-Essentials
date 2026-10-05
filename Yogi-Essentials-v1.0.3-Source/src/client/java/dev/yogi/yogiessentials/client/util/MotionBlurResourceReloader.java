package dev.yogi.yogiessentials.client.util;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.class_2960;
import net.minecraft.class_3300;
import net.minecraft.class_4013;

public final class MotionBlurResourceReloader
        implements IdentifiableResourceReloadListener, class_4013 {

    private static final class_2960 ID =
            class_2960.method_60655("yogiessentials", "motion_blur_reload");

    @Override
    public class_2960 getFabricId() {
        return ID;
    }

    @Override
    public void method_14491(class_3300 manager) {
        MotionBlurManager.invalidate();
    }
}
