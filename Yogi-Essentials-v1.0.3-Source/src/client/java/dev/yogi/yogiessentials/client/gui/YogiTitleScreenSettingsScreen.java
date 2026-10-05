package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.config.ConfigManager;
import net.minecraft.class_10799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_442;
import org.lwjgl.glfw.GLFW;

public final class YogiTitleScreenSettingsScreen extends class_437 {
    private static final class_2960 BACKGROUND = class_2960.method_60655("yogiessentials", "textures/gui/title/background.png");
    private static final class_2960 PANEL = class_2960.method_60655("yogiessentials", "textures/gui/title/panel.png");
    private static final class_2960 LOGO = class_2960.method_60655("yogiessentials", "textures/gui/brand_icon.png");
    private static final class_2960 TOGGLE_NORMAL = class_2960.method_60655("yogiessentials", "textures/gui/title/home_toggle_normal.png");
    private static final class_2960 TOGGLE_HOVER = class_2960.method_60655("yogiessentials", "textures/gui/title/home_toggle_hover.png");
    private static final class_2960 TOGGLE_PRESSED = class_2960.method_60655("yogiessentials", "textures/gui/title/home_toggle_pressed.png");

    private final class_437 parent;
    private class_4185 toggleButton;

    public YogiTitleScreenSettingsScreen(class_437 parent) {
        super(class_2561.method_43470("Yogi Essentials Home Screen"));
        this.parent = parent;
    }

    @Override
    protected void method_25426() {
        float scale = Math.max(0.75F, Math.min(2.0F, Math.min(field_22789 / 1024.0F, field_22790 / 576.0F)));
        int buttonW = Math.round(330 * scale);
        int buttonH = Math.round(62 * scale);
        int buttonX = field_22789 / 2 - buttonW / 2;
        int buttonY = field_22790 / 2 - buttonH / 2 + Math.round(35 * scale);

        toggleButton = method_37063(class_4185.method_46430(class_2561.method_43473(), button -> {
            ConfigManager.setCustomTitleScreenEnabled(!ConfigManager.isCustomTitleScreenEnabled());
            if (field_22787 != null) field_22787.method_1507(new class_442());
        }).method_46434(buttonX, buttonY, buttonW, buttonH).method_46431());
        toggleButton.method_25350(0.0F);
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float deltaTicks) {
        context.method_25302(class_10799.field_56883, BACKGROUND,
                0, 0, 0.0F, 0.0F, field_22789, field_22790,
                2048, 1152, 2048, 1152);
        context.method_25294(0, 0, field_22789, field_22790, 0x66000000);

        float scale = Math.max(0.75F, Math.min(2.0F, Math.min(field_22789 / 1024.0F, field_22790 / 576.0F)));
        int panelW = Math.round(430 * scale);
        int panelH = Math.round(250 * scale);
        int panelX = field_22789 / 2 - panelW / 2;
        int panelY = field_22790 / 2 - panelH / 2;
        context.method_25302(class_10799.field_56883, PANEL,
                panelX, panelY, 0.0F, 0.0F, panelW, panelH,
                1024, 720, 1024, 720);

        int logoSize = Math.round(72 * scale);
        int logoY = panelY + Math.round(18 * scale);
        context.method_25302(class_10799.field_56883, LOGO,
                field_22789 / 2 - logoSize / 2, logoY,
                0.0F, 0.0F, logoSize, logoSize,
                256, 256, 256, 256);

        drawCentered(context, "HOME SCREEN", field_22789 / 2,
                logoY + logoSize + Math.round(5 * scale), 0xFFF4F1EF, true);
        drawCentered(context,
                "Current: " + (ConfigManager.isCustomTitleScreenEnabled() ? "Yogi Essentials" : "Default Minecraft"),
                field_22789 / 2,
                logoY + logoSize + Math.round(18 * scale),
                0xFFFF8A22,
                false);

        super.method_25394(context, mouseX, mouseY, deltaTicks);

        if (toggleButton != null) {
            boolean down = toggleButton.method_49606()
                    && field_22787 != null
                    && GLFW.glfwGetMouseButton(field_22787.method_22683().method_4490(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            class_2960 texture = down ? TOGGLE_PRESSED : (toggleButton.method_49606() ? TOGGLE_HOVER : TOGGLE_NORMAL);
            int y = toggleButton.method_46427() + (down ? 1 : 0);
            int h = down ? Math.max(1, toggleButton.method_25364() - 1) : toggleButton.method_25364();
            context.method_25302(class_10799.field_56883, texture,
                    toggleButton.method_46426(), y,
                    0.0F, 0.0F,
                    toggleButton.method_25368(), h,
                    1024, 192, 1024, 192);
        }

        drawCentered(context, "Click to switch between Yogi Essentials and the vanilla Minecraft title screen.",
                field_22789 / 2,
                panelY + panelH - Math.round(28 * scale),
                0xFFB8B3AF,
                false);
    }

    private void drawCentered(class_332 context, String text, int centerX, int y, int color, boolean shadow) {
        context.method_51433(field_22793, text, centerX - field_22793.method_1727(text) / 2, y, color, shadow);
    }

    @Override
    public void method_25419() {
        if (field_22787 != null) {
            field_22787.method_1507(parent == null ? new class_442() : parent);
        }
    }
}
