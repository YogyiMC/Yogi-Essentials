package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.config.ConfigManager;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_10799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_442;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(value = class_442.class, priority = 500)
public abstract class TitleScreenMixin extends class_437 {
    private static final class_2960 BACKGROUND = id("background");
    private static final class_2960 PANEL = id("panel");
    private static final class_2960 LOGO = class_2960.method_60655("yogiessentials", "textures/gui/brand_icon.png");
    private static final class_2960 BRAND_TEXT = id("brand_text");
    private static final class_2960 HOME_ICON = class_2960.method_60655("yogiessentials", "textures/gui/home_settings.png");

    private static final class_2960 HOME_NORMAL = id("home_toggle_normal");
    private static final class_2960 HOME_HOVER = id("home_toggle_hover");
    private static final class_2960 HOME_PRESSED = id("home_toggle_pressed");

    private static final class_2960 SINGLE_NORMAL = button("singleplayer", "normal");
    private static final class_2960 SINGLE_HOVER = button("singleplayer", "hover");
    private static final class_2960 SINGLE_PRESSED = button("singleplayer", "pressed");
    private static final class_2960 MULTI_NORMAL = button("multiplayer", "normal");
    private static final class_2960 MULTI_HOVER = button("multiplayer", "hover");
    private static final class_2960 MULTI_PRESSED = button("multiplayer", "pressed");
    private static final class_2960 REALMS_NORMAL = button("realms", "normal");
    private static final class_2960 REALMS_HOVER = button("realms", "hover");
    private static final class_2960 REALMS_PRESSED = button("realms", "pressed");
    private static final class_2960 MODS_NORMAL = button("mods", "normal");
    private static final class_2960 MODS_HOVER = button("mods", "hover");
    private static final class_2960 MODS_PRESSED = button("mods", "pressed");
    private static final class_2960 OPTIONS_NORMAL = button("options", "normal");
    private static final class_2960 OPTIONS_HOVER = button("options", "hover");
    private static final class_2960 OPTIONS_PRESSED = button("options", "pressed");
    private static final class_2960 QUIT_NORMAL = button("quit", "normal");
    private static final class_2960 QUIT_HOVER = button("quit", "hover");
    private static final class_2960 QUIT_PRESSED = button("quit", "pressed");

    @Unique private class_4185 yogiessentials$single;
    @Unique private class_4185 yogiessentials$multi;
    @Unique private class_4185 yogiessentials$realms;
    @Unique private class_4185 yogiessentials$mods;
    @Unique private class_4185 yogiessentials$options;
    @Unique private class_4185 yogiessentials$quit;
    @Unique private class_4185 yogiessentials$homeToggle;

    @Unique private float yogiessentials$layoutScale = 1.0F;
    @Unique private int yogiessentials$panelX;
    @Unique private int yogiessentials$panelY;
    @Unique private int yogiessentials$panelW;
    @Unique private int yogiessentials$panelH;

    protected TitleScreenMixin(class_2561 title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void yogiessentials$rebuildTitleControls(CallbackInfo ci) {
        List<class_364> snapshot = new ArrayList<>(((ScreenAccessor) (Object) this).yogiessentials$getChildren());
        boolean custom = ConfigManager.isCustomTitleScreenEnabled();

        class_4185 originalSingle = null;
        class_4185 originalMulti = null;
        class_4185 originalRealms = null;
        class_4185 originalMods = null;
        class_4185 originalOptions = null;
        class_4185 originalQuit = null;

        for (class_364 element : snapshot) {
            if (!(element instanceof class_339 widget)) continue;

            String label = widget.method_25369() == null
                    ? ""
                    : widget.method_25369().getString().trim().toLowerCase(Locale.ROOT);

            boolean recognized = false;
            if (widget instanceof class_4185 button) {
                if (label.contains("singleplayer")) {
                    originalSingle = button;
                    recognized = true;
                } else if (label.contains("multiplayer")) {
                    originalMulti = button;
                    recognized = true;
                } else if (label.contains("realms")) {
                    originalRealms = button;
                    recognized = true;
                } else if (label.equals("mods") || label.contains("mod menu")) {
                    originalMods = button;
                    recognized = true;
                } else if (label.contains("options")) {
                    originalOptions = button;
                    recognized = true;
                } else if (label.contains("quit")) {
                    originalQuit = button;
                    recognized = true;
                }
            }

            if (custom) {
                widget.field_22764 = false;
                widget.field_22763 = false;
            } else if (!recognized) {
                if (label.contains("account") || label.contains("switch")) {
                    widget.field_22764 = false;
                    widget.field_22763 = false;
                }
            }
        }

        yogiessentials$single = null;
        yogiessentials$multi = null;
        yogiessentials$realms = null;
        yogiessentials$mods = null;
        yogiessentials$options = null;
        yogiessentials$quit = null;
        yogiessentials$homeToggle = null;

        if (custom) {
            yogiessentials$updateLayout();
            float s = yogiessentials$layoutScale;

            int longW = Math.min(Math.round(690 * s), yogiessentials$panelW - Math.round(82 * s));
            int longH = Math.round(74 * s);
            int rowGap = Math.round(14 * s);
            int rowX = field_22789 / 2 - longW / 2;
            int firstY = yogiessentials$panelY + Math.round(44 * s);

            yogiessentials$single = yogiessentials$proxy(originalSingle, rowX, firstY, longW, longH);
            yogiessentials$multi = yogiessentials$proxy(originalMulti, rowX, firstY + (longH + rowGap), longW, longH);
            yogiessentials$realms = yogiessentials$proxy(originalRealms, rowX, firstY + 2 * (longH + rowGap), longW, longH);

            if (originalMods != null) {
                yogiessentials$mods = yogiessentials$proxy(originalMods,
                        rowX, firstY + 3 * (longH + rowGap), longW, longH);
            } else if (FabricLoader.getInstance().isModLoaded("modmenu")) {
                yogiessentials$mods = yogiessentials$actionButton(
                        rowX, firstY + 3 * (longH + rowGap), longW, longH,
                        yogiessentials$openModMenu()
                );
            }

            int homeW = Math.min(Math.round(560 * s), yogiessentials$panelW - Math.round(160 * s));
            int homeH = Math.round(62 * s);
            int homeY = firstY + 4 * (longH + rowGap) + Math.round(18 * s);
            yogiessentials$homeToggle = yogiessentials$actionButton(
                    field_22789 / 2 - homeW / 2,
                    homeY,
                    homeW,
                    homeH,
                    this::yogiessentials$toggleTitleScreenMode
            );

            int shortW = Math.round(228 * s);
            int shortH = Math.round(54 * s);
            int shortGap = Math.round(18 * s);
            int shortY = homeY + homeH + Math.round(26 * s);
            yogiessentials$options = yogiessentials$proxy(originalOptions,
                    field_22789 / 2 - shortGap / 2 - shortW, shortY, shortW, shortH);
            yogiessentials$quit = yogiessentials$proxy(originalQuit,
                    field_22789 / 2 + shortGap / 2, shortY, shortW, shortH);
        } else {
            yogiessentials$createVanillaHomeToggle(originalMods);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$renderOwnedTitleScreen(
            class_332 context,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        if (!ConfigManager.isCustomTitleScreenEnabled()) return;

        yogiessentials$updateLayout();
        float s = yogiessentials$layoutScale;
        int centerX = field_22789 / 2;

        context.method_25302(
                class_10799.field_56883,
                BACKGROUND,
                0, 0,
                0.0F, 0.0F,
                field_22789, field_22790,
                1672, 941,
                1672, 941
        );
        context.method_25294(0, 0, field_22789, field_22790, 0x16000000);

        int logoSize = Math.round(176 * s);
        int logoY = Math.round(18 * s);
        context.method_25302(
                class_10799.field_56883,
                LOGO,
                centerX - logoSize / 2,
                logoY,
                0.0F, 0.0F,
                logoSize, logoSize,
                256, 256,
                256, 256
        );

        int brandW = Math.round(432 * s);
        int brandH = Math.round(144 * s);
        int brandY = logoY + logoSize - Math.round(6 * s);
        context.method_25302(
                class_10799.field_56883,
                BRAND_TEXT,
                centerX - brandW / 2,
                brandY,
                0.0F, 0.0F,
                brandW, brandH,
                2048, 682,
                2048, 682
        );

        int versionY = brandY + brandH - Math.round(8 * s);
        yogiessentials$drawCentered(context, "v1.0.3  •  Minecraft 1.21.11",
                centerX, versionY, 0xFFC7C1BC, false);

        context.method_25302(
                class_10799.field_56883,
                PANEL,
                yogiessentials$panelX,
                yogiessentials$panelY,
                0.0F, 0.0F,
                yogiessentials$panelW,
                yogiessentials$panelH,
                1024, 720,
                1024, 720
        );

        yogiessentials$renderMenuButton(context, yogiessentials$single, mouseX, mouseY,
                SINGLE_NORMAL, SINGLE_HOVER, SINGLE_PRESSED, 1024, 256);
        yogiessentials$renderMenuButton(context, yogiessentials$multi, mouseX, mouseY,
                MULTI_NORMAL, MULTI_HOVER, MULTI_PRESSED, 1024, 256);
        yogiessentials$renderMenuButton(context, yogiessentials$realms, mouseX, mouseY,
                REALMS_NORMAL, REALMS_HOVER, REALMS_PRESSED, 1024, 256);
        yogiessentials$renderMenuButton(context, yogiessentials$mods, mouseX, mouseY,
                MODS_NORMAL, MODS_HOVER, MODS_PRESSED, 1024, 256);
        yogiessentials$renderHomeToggleButton(context, mouseX, mouseY);
        yogiessentials$renderMenuButton(context, yogiessentials$options, mouseX, mouseY,
                OPTIONS_NORMAL, OPTIONS_HOVER, OPTIONS_PRESSED, 512, 256);
        yogiessentials$renderMenuButton(context, yogiessentials$quit, mouseX, mouseY,
                QUIT_NORMAL, QUIT_HOVER, QUIT_PRESSED, 512, 256);

        ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void yogiessentials$renderHomeButtonOnVanilla(
            class_332 context,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        if (ConfigManager.isCustomTitleScreenEnabled()) return;
        yogiessentials$renderVanillaToggle(context, mouseX, mouseY);
    }

    @Unique
    private void yogiessentials$updateLayout() {
        float s = Math.min(field_22789 / 1920.0F, field_22790 / 1080.0F);
        s = Math.max(0.72F, Math.min(1.42F, s));
        yogiessentials$layoutScale = s;

        yogiessentials$panelW = Math.round(820 * s);
        yogiessentials$panelH = Math.round(600 * s);
        yogiessentials$panelX = field_22789 / 2 - yogiessentials$panelW / 2;
        yogiessentials$panelY = Math.round(300 * s);

        int bottomMargin = Math.round(28 * s);
        if (yogiessentials$panelY + yogiessentials$panelH > field_22790 - bottomMargin) {
            yogiessentials$panelY = Math.max(Math.round(248 * s), field_22790 - yogiessentials$panelH - bottomMargin);
        }
    }

    @Unique
    private void yogiessentials$createVanillaHomeToggle(class_4185 originalMods) {
        int size = 20;
        int x = field_22789 - size - 6;
        int y = 6;

        if (originalMods != null) {
            size = Math.max(20, originalMods.method_25364());
            x = Math.min(field_22789 - size - 4, originalMods.method_46426() + originalMods.method_25368() + 4);
            y = originalMods.method_46427();
        }

        yogiessentials$homeToggle = method_37063(class_4185.method_46430(
                class_2561.method_43473(),
                button -> yogiessentials$toggleTitleScreenMode()
        ).method_46434(x, y, size, size).method_46431());
        yogiessentials$homeToggle.method_25350(0.0F);
    }

    @Unique
    private void yogiessentials$toggleTitleScreenMode() {
        ConfigManager.setCustomTitleScreenEnabled(!ConfigManager.isCustomTitleScreenEnabled());
        if (field_22787 != null) {
            field_22787.method_1507(new class_442());
        }
    }

    @Unique
    private class_4185 yogiessentials$proxy(class_4185 original, int x, int y, int w, int h) {
        if (original == null) return null;
        class_4185 proxy = yogiessentials$actionButton(x, y, w, h, () -> {
            class_4185.class_4241 action = ((ButtonWidgetAccessor) (Object) original).yogiessentials$getPressAction();
            if (action != null) action.onPress(original);
        });
        proxy.field_22763 = original.field_22763;
        return proxy;
    }

    @Unique
    private class_4185 yogiessentials$actionButton(int x, int y, int w, int h, Runnable action) {
        class_4185 proxy = method_37063(class_4185.method_46430(class_2561.method_43473(), button -> action.run())
                .method_46434(x, y, w, h)
                .method_46431());
        proxy.method_25350(0.0F);
        return proxy;
    }

    @Unique
    private Runnable yogiessentials$openModMenu() {
        return () -> {
            if (field_22787 == null) return;
            try {
                Class<?> modsScreenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                Constructor<?> constructor = modsScreenClass.getConstructor(class_437.class);
                Object screen = constructor.newInstance((class_437) (Object) this);
                if (screen instanceof class_437 modScreen) {
                    field_22787.method_1507(modScreen);
                }
            } catch (ReflectiveOperationException exception) {
                System.err.println("[Yogi Essentials] Could not open Mod Menu: " + exception.getMessage());
            }
        };
    }

    @Unique
    private void yogiessentials$renderMenuButton(
            class_332 context,
            class_4185 button,
            int mouseX,
            int mouseY,
            class_2960 normal,
            class_2960 hover,
            class_2960 pressed,
            int textureWidth,
            int textureHeight
    ) {
        if (button == null || !button.field_22764) return;

        boolean hovered = button.method_25405(mouseX, mouseY);
        boolean down = yogiessentials$isPressed(button, mouseX, mouseY);
        class_2960 texture = down ? pressed : (hovered ? hover : normal);
        int pressOffset = down ? 1 : 0;

        context.method_25302(
                class_10799.field_56883,
                texture,
                button.method_46426(), button.method_46427() + pressOffset,
                0.0F, 0.0F,
                button.method_25368(), button.method_25364(),
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }

    @Unique
    private void yogiessentials$renderHomeToggleButton(class_332 context, int mouseX, int mouseY) {
        if (yogiessentials$homeToggle == null || !yogiessentials$homeToggle.field_22764) return;
        boolean hovered = yogiessentials$homeToggle.method_25405(mouseX, mouseY);
        boolean down = yogiessentials$isPressed(yogiessentials$homeToggle, mouseX, mouseY);
        class_2960 texture = down ? HOME_PRESSED : (hovered ? HOME_HOVER : HOME_NORMAL);

        context.method_25302(
                class_10799.field_56883,
                texture,
                yogiessentials$homeToggle.method_46426(),
                yogiessentials$homeToggle.method_46427() + (down ? 1 : 0),
                0.0F, 0.0F,
                yogiessentials$homeToggle.method_25368(), yogiessentials$homeToggle.method_25364(),
                1024, 192,
                1024, 192
        );
    }

    @Unique
    private void yogiessentials$renderVanillaToggle(class_332 context, int mouseX, int mouseY) {
        if (yogiessentials$homeToggle == null || !yogiessentials$homeToggle.field_22764) return;

        int x = yogiessentials$homeToggle.method_46426();
        int y = yogiessentials$homeToggle.method_46427();
        int w = yogiessentials$homeToggle.method_25368();
        int h = yogiessentials$homeToggle.method_25364();
        boolean hovered = yogiessentials$homeToggle.method_25405(mouseX, mouseY);
        boolean down = yogiessentials$isPressed(yogiessentials$homeToggle, mouseX, mouseY);

        int fill = down ? 0xEE17120C : (hovered ? 0xEE1E1810 : 0xD5120F0A);
        int border = hovered ? 0xFFFFA22E : 0xFF9A661E;
        if (!yogiessentials$homeToggle.field_22763) {
            fill = 0xAA111111;
            border = 0xFF555555;
        }

        context.method_25294(x, y, x + w, y + h, fill);
        context.method_25294(x, y, x + w, y + 1, border);
        context.method_25294(x, y + h - 1, x + w, y + h, border);
        context.method_25294(x, y, x + 1, y + h, border);
        context.method_25294(x + w - 1, y, x + w, y + h, border);

        int iconSize = Math.max(10, h - 6);
        context.method_25302(
                class_10799.field_56883,
                HOME_ICON,
                x + (w - iconSize) / 2,
                y + (h - iconSize) / 2 + (down ? 1 : 0),
                0.0F, 0.0F,
                iconSize, iconSize,
                31, 31,
                31, 31
        );
    }

    @Unique
    private boolean yogiessentials$isPressed(class_4185 button, int mouseX, int mouseY) {
        return button.method_25405(mouseX, mouseY)
                && field_22787 != null
                && GLFW.glfwGetMouseButton(field_22787.method_22683().method_4490(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
    }

    @Unique
    private void yogiessentials$drawCentered(
            class_332 context,
            String text,
            int centerX,
            int y,
            int color,
            boolean shadow
    ) {
        context.method_51433(field_22793, text, centerX - field_22793.method_1727(text) / 2, y, color, shadow);
    }

    @Unique
    private static class_2960 id(String name) {
        return class_2960.method_60655("yogiessentials", "textures/gui/title/" + name + ".png");
    }

    @Unique
    private static class_2960 button(String name, String state) {
        return class_2960.method_60655("yogiessentials", "textures/gui/title/" + name + "_" + state + ".png");
    }
}
