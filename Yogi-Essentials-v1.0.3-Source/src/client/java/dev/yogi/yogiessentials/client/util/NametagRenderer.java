package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.visual.NametagCustomizerModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_5250;
import net.minecraft.class_742;
import net.minecraft.class_765;

/**
 * Renders Yogi Essentials player nametags independently from the server/vanilla
 * label visibility decision. This is cosmetic only and uses the normal depth-tested
 * text layer, so labels do not render through terrain.
 */
public final class NametagRenderer {
    private static boolean initialized;

    private NametagRenderer() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(NametagRenderer::render);
    }

    private static NametagCustomizerModule module() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        return YogiEssentialsClient.getModuleManager().getModule(NametagCustomizerModule.class);
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        class_310 client = class_310.method_1551();
        NametagCustomizerModule module = module();
        if (module == null || !module.isEnabled() || !module.getShowNametag().get()) return;
        if (client.field_1687 == null || client.field_1724 == null) return;

        class_4587 matrices = context.matrices();
        class_4597 consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        class_4184 camera = client.field_1773.method_19418();
        class_243 cameraPos = camera.method_71156();
        float tickProgress = client.method_61966().method_60637(false);
        boolean firstPerson = client.field_1690.method_31044().method_31034();

        for (class_742 player : client.field_1687.method_18456()) {
            if (player == null || player.method_31481()) continue;
            if (!module.appliesToEntityId(player.method_5628(), client.field_1724.method_5628())) continue;
            if (player == client.field_1724 && firstPerson) continue;
            if (player.method_5767()) continue;

            class_243 pos = player.method_30950(tickProgress);
            double dx = pos.field_1352 - cameraPos.field_1352;
            double dy = pos.field_1351 - cameraPos.field_1351;
            double dz = pos.field_1350 - cameraPos.field_1350;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > 4096.0D) continue; // vanilla-like 64 block nameplate range

            class_2561 raw = player.method_5476();
            if (raw == null) raw = player.method_5477();
            class_5250 text = raw.method_27661();
            text.method_10862(text.method_10866()
                    .method_10982(module.getBold().get())
                    .method_10978(module.getItalic().get())
                    .method_30938(module.getUnderline().get()));
            if (module.getCustomTextColor().get()) {
                text.method_10862(text.method_10866().method_36139(module.getTextColor().getRgb()));
            }

            matrices.method_22903();
            matrices.method_22904(dx, dy + player.method_17682() + 0.45D, dz);
            matrices.method_22907(camera.method_23767());

            float scale = 0.025F * module.getScale().get().floatValue();
            matrices.method_22905(-scale, -scale, scale);

            float x = -client.field_1772.method_27525(text) / 2.0F;
            float y = -client.field_1772.field_2000 / 2.0F + module.getVerticalOffset().get().floatValue();
            int color = module.getCustomTextColor().get()
                    ? (0xFF000000 | module.getTextColor().getRgb())
                    : 0xFFFFFFFF;
            int background = module.getBackgroundArgb();
            boolean shadow = !module.getBackground().get() || ((background >>> 24) & 0xFF) < 40;

            client.field_1772.method_27522(
                    text,
                    x,
                    y,
                    color,
                    shadow,
                    matrices.method_23760().method_23761(),
                    consumers,
                    class_327.class_6415.field_33993,
                    background,
                    class_765.field_32767
            );
            matrices.method_22909();
        }
    }
}
