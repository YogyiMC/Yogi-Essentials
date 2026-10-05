package dev.yogi.yogiessentials.client.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1060;
import net.minecraft.class_10799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4011;
import net.minecraft.class_425;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_425.class)
public abstract class SplashOverlayMixin {
    /**
     * VulkanMod replaces Minecraft's normal OpenGL render backend and compiles GUI
     * pipelines itself. The custom Yogi startup splash adds extra GUI pipeline draws
     * during the earliest resource-reload frames, which is exactly where VulkanMod
     * can still have incomplete shader sources. Keep the splash completely vanilla
     * on VulkanMod while leaving the rest of Yogi Essentials active.
     */
    private static final boolean YOGI_DISABLE_CUSTOM_SPLASH =
            FabricLoader.getInstance().isModLoaded("vulkanmod");

    private static final class_2960 YOGI_LOGO =
            class_2960.method_60655("yogiessentials", "textures/gui/brand_icon.png");

    private static final int BACKGROUND_TOP = 0xFF111114;
    private static final int BACKGROUND_BOTTOM = 0xFF08080A;
    private static final int PANEL = 0xE6151519;
    private static final int PANEL_BORDER = 0xFF2B2B31;
    private static final int ORANGE = 0xFFFF6A00;
    private static final int ORANGE_BRIGHT = 0xFFFF8A22;
    private static final int TEXT = 0xFFF5F5F7;
    private static final int MUTED = 0xFFA2A2AA;
    private static final int TRACK = 0xFF29292F;

    @Shadow @Final private class_310 client;
    @Shadow @Final private class_4011 reload;
    @Shadow @Final private boolean reloading;
    @Shadow private float progress;

    @Inject(method = "init", at = @At("TAIL"))
    private static void yogiessentials$registerBrandTexture(
            class_1060 textureManager,
            CallbackInfo ci
    ) {
        if (YOGI_DISABLE_CUSTOM_SPLASH) {
            return;
        }

        try {
            textureManager.method_65875(YOGI_LOGO);
        } catch (RuntimeException ignored) {
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void yogiessentials$renderCustomSplash(
            class_332 context,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        if (YOGI_DISABLE_CUSTOM_SPLASH) {
            return;
        }

        int width = context.method_51421();
        int height = context.method_51443();
        if (width <= 0 || height <= 0) {
            return;
        }

        context.method_71048();

        context.method_25296(0, 0, width, height, BACKGROUND_TOP, BACKGROUND_BOTTOM);

        long now = System.nanoTime();
        float pulse = 0.5F + 0.5F * (float) Math.sin(now / 520_000_000.0);
        int pulseAlpha = 28 + Math.round(pulse * 28.0F);
        int pulseColor = (pulseAlpha << 24) | 0x00FF6A00;

        context.method_25294(0, 0, width, 2, ORANGE);
        context.method_25294(0, height - 2, width, height, 0xFFB84C00);
        context.method_25294(0, 2, width, 4, pulseColor);

        int centerX = width / 2;
        int logoSize = Math.max(68, Math.min(124, height / 3));
        int contentHeight = logoSize + 82;
        int logoY = Math.max(24, (height - contentHeight) / 2 - 14);
        drawBrandLogo(context, centerX, logoY, logoSize, pulseColor);

        int titleY = logoY + logoSize + 11;
        drawCenteredScaledText(context, "YOGI ESSENTIALS", centerX, titleY, 1.22F, TEXT, true);
        drawCenteredText(context, "v1.0.3", centerX, titleY + 16, 0xFFFF8A22, false);

        float liveProgress = Math.max(progress, reload.method_18229());
        liveProgress = Math.max(0.0F, Math.min(1.0F, liveProgress));

        int panelWidth = Math.min(430, Math.max(150, width - 40));
        int panelX = centerX - panelWidth / 2;
        int panelY = Math.min(height - 64, titleY + 36);
        int panelHeight = 43;

        context.method_25294(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BORDER);
        context.method_25294(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, PANEL);

        String status = statusFor(liveProgress, reloading);
        drawCenteredText(context, status, centerX, panelY + 8, TEXT, false);

        int barX = panelX + 13;
        int barY = panelY + 26;
        int barWidth = panelWidth - 26;
        int barHeight = 5;
        context.method_25294(barX, barY, barX + barWidth, barY + barHeight, TRACK);

        int fillWidth = Math.max(0, Math.min(barWidth, Math.round(barWidth * liveProgress)));
        if (fillWidth > 0) {
            context.method_25294(barX, barY, barX + fillWidth, barY + barHeight, ORANGE);
            if (fillWidth > 2) {
                context.method_25294(barX, barY, barX + fillWidth, barY + 1, ORANGE_BRIGHT);
            }
        }

        String percent = Math.round(liveProgress * 100.0F) + "%";
        int percentWidth = client.field_1772.method_1727(percent);
        context.method_51433(
                client.field_1772,
                percent,
                panelX + panelWidth - 13 - percentWidth,
                panelY + 7,
                MUTED,
                false
        );

        if (height >= 260) {
            drawCenteredText(
                    context,
                    reloading ? "Applying resource changes" : "Preparing your client",
                    centerX,
                    height - 25,
                    0xFF74747C,
                    false
            );
        }
    }

    private void drawBrandLogo(
            class_332 context,
            int centerX,
            int top,
            int size,
            int pulseColor
    ) {
        int left = centerX - size / 2;

        try {
            context.method_25302(
                    class_10799.field_56883,
                    YOGI_LOGO,
                    left,
                    top,
                    0.0F,
                    0.0F,
                    size,
                    size,
                    256,
                    256,
                    256,
                    256
            );
        } catch (IllegalStateException ignored) {
        }
    }

    private String statusFor(float value, boolean isReloading) {
        if (isReloading) {
            if (value < 0.34F) return "Reading resource packs";
            if (value < 0.72F) return "Reloading game resources";
            if (value < 0.94F) return "Building render resources";
            return "Finishing resource reload";
        }

        if (value < 0.18F) return "Starting Yogi Essentials";
        if (value < 0.42F) return "Loading client resources";
        if (value < 0.72F) return "Preparing textures and models";
        if (value < 0.94F) return "Finalizing Minecraft";
        return "Ready";
    }

    private void drawCenteredText(
            class_332 context,
            String text,
            int centerX,
            int y,
            int color,
            boolean shadow
    ) {
        int textWidth = client.field_1772.method_1727(text);
        context.method_51433(
                client.field_1772,
                text,
                centerX - textWidth / 2,
                y,
                color,
                shadow
        );
    }

    private void drawCenteredScaledText(
            class_332 context,
            String text,
            int centerX,
            int y,
            float scale,
            int color,
            boolean shadow
    ) {
        int rawWidth = client.field_1772.method_1727(text);
        Matrix3x2fStack matrices = context.method_51448();
        matrices.pushMatrix();
        matrices.translate(centerX, y);
        matrices.scale(scale, scale);
        context.method_51433(
                client.field_1772,
                text,
                -rawWidth / 2,
                0,
                color,
                shadow
        );
        matrices.popMatrix();
    }
}
