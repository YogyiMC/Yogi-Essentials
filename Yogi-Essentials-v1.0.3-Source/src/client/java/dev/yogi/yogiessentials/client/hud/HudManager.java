package dev.yogi.yogiessentials.client.hud;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.gui.ColorPickerScreen;
import dev.yogi.yogiessentials.client.gui.HudEditorScreen;
import dev.yogi.yogiessentials.client.gui.ModuleSettingsScreen;
import dev.yogi.yogiessentials.client.gui.YogiEssentialsScreen;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.hud.*;
import dev.yogi.yogiessentials.client.module.pvp.*;
import dev.yogi.yogiessentials.client.module.performance.HudUpdateOptimizerModule;
import dev.yogi.yogiessentials.client.module.smp.StreamerPrivacyModule;
import dev.yogi.yogiessentials.client.util.WarningSoundManager;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.class_10799;
import net.minecraft.class_1293;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_329;
import net.minecraft.class_332;
import org.joml.Matrix3x2fStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HudManager {

    public static final int FPS_WIDTH = 90;
    public static final int FPS_HEIGHT = 28;

    public static final int PING_WIDTH = 100;
    public static final int PING_HEIGHT = 28;

    public static final int COORDS_WIDTH = 155;
    public static final int COORDS_HEIGHT = 28;

    public static final int ARMOR_WIDTH = 124;
    public static final int ARMOR_HEIGHT = 85;

    public static final int TOTEM_WIDTH = 94;
    public static final int TOTEM_HEIGHT = 28;

    public static final int SHIELD_WIDTH = 116;
    public static final int SHIELD_HEIGHT = 28;

    public static final int ATTACK_WIDTH = 126;
    public static final int ATTACK_HEIGHT = 34;

    public static final int POTION_WIDTH = 160;
    public static final int POTION_HEIGHT = 112;

    public static final int WARNING_WIDTH = 220;
    public static final int WARNING_HEIGHT = 24;

    private static final int ORANGE = 0xFFFF6A00;
    private static final int TEXT = 0xFFF4F4F5;
    private static final int MUTED = 0xFF9A9AA1;
    private static final int BORDER = 0xAA333339;

    private static final Map<String, Boolean> WARNING_ACTIVE =
            new HashMap<>();

    private static final ArrayDeque<WarningModule> WARNING_SOUND_QUEUE = new ArrayDeque<>();
    private static final Set<String> WARNING_SOUND_QUEUED = new HashSet<>();

    private static Object warningWorldKey;
    private static String warningQueueSignature = "";
    private static int warningQueueStartIndex;
    private static long warningQueueLastRotateMillis;
    private static long warningQueueLastSoundMillis;

    private static Object hudDataWorldKey;
    private static int cachedTotemCount;
    private static boolean cachedTotemCountValid;
    private static long cachedTotemCountNanos;
    private static List<class_1293> cachedPotionEffects = List.of();
    private static boolean cachedPotionEffectsValid;
    private static long cachedPotionEffectsNanos;

    private HudManager() {
    }

    public static void initialize() {
        HudElementRegistry.addLast(
                class_2960.method_60655(
                        "yogiessentials",
                        "main_hud"
                ),
                (context, tickCounter) ->
                        render(context)
        );
    }

    
    public static void clearTransientState() {
        clearTransientState(false);
    }

    




    public static void clearTransientState(
            boolean preserveConnectionPing
    ) {
        clearWarningTransientState();
        clearHudDataCaches();

        if (YogiEssentialsClient.getModuleManager() != null) {
            PingHudModule ping = YogiEssentialsClient
                    .getModuleManager()
                    .getModule(PingHudModule.class);

            if (ping != null) {
                ping.clearTransientState(preserveConnectionPing);
            }
        }
    }

    private static void clearWarningTransientState() {
        WARNING_ACTIVE.clear();
        WARNING_SOUND_QUEUE.clear();
        WARNING_SOUND_QUEUED.clear();
        warningWorldKey = null;
        warningQueueSignature = "";
        warningQueueStartIndex = 0;
        warningQueueLastRotateMillis = 0L;
        warningQueueLastSoundMillis = 0L;
    }

    private static void render(
            class_332 context
    ) {
        class_310 client =
                class_310.method_1551();

        if (
                client.field_1724 == null
                        ||
                client.field_1687 == null
        ) {
            return;
        }

        if (
                client.field_1755
                        instanceof YogiEssentialsScreen
                        ||
                client.field_1755
                        instanceof ModuleSettingsScreen
                        ||
                client.field_1755
                        instanceof ColorPickerScreen
                        ||
                client.field_1755
                        instanceof HudEditorScreen
        ) {
            return;
        }

        renderHudModules(
                context,
                client
        );

        renderWarnings(
                context,
                client
        );
    }

    public static void renderEditorHud(
            class_332 context
    ) {
        class_310 client =
                class_310.method_1551();

        if (
                client.field_1724 == null
                        ||
                client.field_1687 == null
        ) {
            return;
        }

        renderHudModules(
                context,
                client
        );

        renderWarningEditorPreviews(
                context,
                client
        );
    }

    private static void renderHudModules(
            class_332 context,
            class_310 client
    ) {
        renderFps(
                context,
                client
        );

        renderPing(
                context,
                client
        );

        renderCoordinates(
                context,
                client
        );

        renderArmor(
                context,
                client
        );

        renderTotems(
                context,
                client
        );

        renderShieldStatus(
                context,
                client
        );

        renderAttackCooldown(
                context,
                client
        );

        renderPotionEffects(
                context,
                client
        );
    }

    
    
    

    public static int getRenderedWidth(
            HudModule module
    ) {
        int base = getBaseWidth(module);

        if (
                module
                        instanceof StyledHudModule styled
        ) {
            return Math.max(
                    1,
                    Math.round(
                            base
                                    * styled
                                    .getScale()
                                    .get()
                                    .floatValue()
                    )
            );
        }

        return base;
    }

    public static int getRenderedHeight(
            HudModule module
    ) {
        int base = getBaseHeight(module);

        if (
                module
                        instanceof StyledHudModule styled
        ) {
            return Math.max(
                    1,
                    Math.round(
                            base
                                    * styled
                                    .getScale()
                                    .get()
                                    .floatValue()
                    )
            );
        }

        return base;
    }

    public static int getBaseWidth(
            HudModule module
    ) {
        
        
        
        return getNaturalBaseWidth(module);
    }

    public static int getBaseHeight(
            HudModule module
    ) {
        return getNaturalBaseHeight(module);
    }

    private static int getContainerBaseWidth(StyledHudModule module) {
        return module.hasCustomEditorWidth()
                ? Math.max(1, module.getEditorWidth().get().intValue())
                : getNaturalBaseWidth(module);
    }

    private static int getContainerBaseHeight(StyledHudModule module) {
        return module.hasCustomEditorHeight()
                ? Math.max(1, module.getEditorHeight().get().intValue())
                : getNaturalBaseHeight(module);
    }

    





    public static boolean usesContainerResize(Module module) {
        return module instanceof ArmorHudModule
                || module instanceof PotionEffectsHudModule;
    }

    public static boolean resolveArmorHorizontal(ArmorHudModule armor) {
        if (!armor.getAutoLayout().get()) {
            return armor.getLayout().get() == ArmorHudModule.Layout.HORIZONTAL;
        }

        if (armor.hasCustomEditorWidth() || armor.hasCustomEditorHeight()) {
            int width = armor.hasCustomEditorWidth()
                    ? armor.getEditorWidth().get().intValue()
                    : getNaturalArmorWidthForOrientation(armor, false);
            int height = armor.hasCustomEditorHeight()
                    ? armor.getEditorHeight().get().intValue()
                    : getNaturalArmorHeightForOrientation(armor, false);
            return width >= height * 1.35;
        }

        return false;
    }

    private static int getNaturalArmorWidthForOrientation(ArmorHudModule armor, boolean horizontal) {
        int padding = armor.getPadding().get().intValue();
        int spacing = armor.getIconSpacing().get().intValue();
        int count = armor.getConfiguredItemCount();
        ArmorHudModule.DisplayMode mode = armor.getDisplayMode().get();
        if (mode == ArmorHudModule.DisplayMode.PERCENT_ONLY) {
            return horizontal ? padding * 2 + 30 * count + spacing * Math.max(0, count - 1) : padding * 2 + 30;
        }
        int icon = armor.getIconSize().get().intValue();
        boolean percentages = mode == ArmorHudModule.DisplayMode.ICONS_PERCENT && armor.getShowValue().get();
        int cellWidth = percentages ? Math.max(icon, 30) : icon;
        return horizontal ? padding * 2 + cellWidth * count + spacing * Math.max(0, count - 1) : padding * 2 + icon + (percentages ? 38 : 0);
    }

    private static int getNaturalArmorHeightForOrientation(ArmorHudModule armor, boolean horizontal) {
        int padding = armor.getPadding().get().intValue();
        int spacing = armor.getIconSpacing().get().intValue();
        int count = armor.getConfiguredItemCount();
        ArmorHudModule.DisplayMode mode = armor.getDisplayMode().get();
        if (mode == ArmorHudModule.DisplayMode.PERCENT_ONLY) {
            return horizontal ? padding * 2 + 11 : padding * 2 + 11 * count + spacing * Math.max(0, count - 1);
        }
        int icon = armor.getIconSize().get().intValue();
        int labelRoom = armor.getShowLabel().get() ? 14 : 0;
        boolean percentages = mode == ArmorHudModule.DisplayMode.ICONS_PERCENT && armor.getShowValue().get();
        return horizontal
                ? padding * 2 + labelRoom + icon + (percentages ? 12 : 2)
                : padding * 2 + labelRoom + Math.max(icon, 10) * count + spacing * Math.max(0, count - 1);
    }

    public static boolean resolvePotionIconsHorizontal(PotionEffectsHudModule potion) {
        if (!potion.getAutoLayout().get()) {
            return potion.getLayout().get() == PotionEffectsHudModule.Layout.HORIZONTAL;
        }
        if (potion.hasCustomEditorWidth() || potion.hasCustomEditorHeight()) {
            int width = potion.hasCustomEditorWidth() ? potion.getEditorWidth().get().intValue() : 120;
            int height = potion.hasCustomEditorHeight() ? potion.getEditorHeight().get().intValue() : 32;
            return width >= height * 1.35;
        }
        return true;
    }

    private static int getNaturalBaseWidth(
            HudModule module
    ) {
        if (module instanceof FpsHudModule) {
            return FPS_WIDTH;
        }

        if (module instanceof PingHudModule) {
            return PING_WIDTH;
        }

        if (module instanceof CoordinatesHudModule) {
            return COORDS_WIDTH;
        }

        if (
                module
                        instanceof ArmorHudModule armor
        ) {
            ArmorHudModule.DisplayMode mode = armor.getDisplayMode().get();

            if (mode == ArmorHudModule.DisplayMode.TEXT) {
                return ARMOR_WIDTH;
            }

            int padding = armor.getPadding().get().intValue();
            int spacing = armor.getIconSpacing().get().intValue();
            int count = armor.getConfiguredItemCount();
            boolean horizontal = resolveArmorHorizontal(armor);

            if (mode == ArmorHudModule.DisplayMode.PERCENT_ONLY) {
                int cellWidth = 30;
                return horizontal
                        ? padding * 2 + cellWidth * count + spacing * Math.max(0, count - 1)
                        : padding * 2 + cellWidth;
            }

            int icon = armor.getIconSize().get().intValue();
            boolean percentages = mode == ArmorHudModule.DisplayMode.ICONS_PERCENT
                    && armor.getShowValue().get();
            int cellWidth = percentages ? Math.max(icon, 30) : icon;

            if (horizontal) {
                return padding * 2 + cellWidth * count + spacing * Math.max(0, count - 1);
            }

            return padding * 2 + icon + (percentages ? 38 : 0);
        }

        if (
                module
                        instanceof TotemHudModule totem
        ) {
            TotemHudModule.DisplayMode mode =
                    totem
                            .getDisplayMode()
                            .get();

            if (
                    mode
                            == TotemHudModule.DisplayMode.TEXT
            ) {
                return TOTEM_WIDTH;
            }

            int icon =
                    totem
                            .getIconSize()
                            .get()
                            .intValue();

            int padding =
                    totem
                            .getPadding()
                            .get()
                            .intValue();

            if (mode == TotemHudModule.DisplayMode.ICON_OVERLAY_COUNT) {
                return padding * 2 + icon;
            }

            return padding * 2 + icon + 32;
        }

        if (
                module
                        instanceof ShieldStatusHudModule shield
        ) {
            ShieldStatusHudModule.DisplayMode mode =
                    shield
                            .getDisplayMode()
                            .get();

            if (
                    mode
                            == ShieldStatusHudModule.DisplayMode.TEXT
            ) {
                return SHIELD_WIDTH;
            }

            int icon =
                    shield
                            .getIconSize()
                            .get()
                            .intValue();

            int padding =
                    shield
                            .getPadding()
                            .get()
                            .intValue();

            return Math.max(
                    92,
                    padding * 2
                            + icon
                            + 54
            );
        }

        if (module instanceof AttackCooldownHudModule) {
            return ATTACK_WIDTH;
        }

        if (
                module
                        instanceof PotionEffectsHudModule potion
        ) {
            PotionEffectsHudModule.DisplayMode mode = potion.getDisplayMode().get();

            if (mode == PotionEffectsHudModule.DisplayMode.ICONS) {
                int padding = potion.getPadding().get().intValue();
                int icon = potion.getIconSize().get().intValue();
                int spacing = potion.getIconSpacing().get().intValue();
                int count = getPotionIconCountForDimensions();
                return resolvePotionIconsHorizontal(potion)
                        ? padding * 2 + icon * count + spacing * Math.max(0, count - 1)
                        : padding * 2 + icon;
            }

            if (mode == PotionEffectsHudModule.DisplayMode.ICON_TIMER) {
                return 78;
            }

            return POTION_WIDTH;
        }

        return 110;
    }

    private static int getNaturalBaseHeight(
            HudModule module
    ) {
        if (module instanceof FpsHudModule) {
            return FPS_HEIGHT;
        }

        if (module instanceof PingHudModule) {
            return PING_HEIGHT;
        }

        if (module instanceof CoordinatesHudModule) {
            return COORDS_HEIGHT;
        }

        if (
                module
                        instanceof ArmorHudModule armor
        ) {
            ArmorHudModule.DisplayMode mode = armor.getDisplayMode().get();

            if (mode == ArmorHudModule.DisplayMode.TEXT) {
                return ARMOR_HEIGHT + Math.max(0, armor.getConfiguredItemCount() - 4) * 14;
            }

            int padding = armor.getPadding().get().intValue();
            int spacing = armor.getIconSpacing().get().intValue();
            int count = armor.getConfiguredItemCount();
            int labelRoom = mode == ArmorHudModule.DisplayMode.PERCENT_ONLY
                    ? 0
                    : (armor.getShowLabel().get() ? 14 : 0);
            boolean horizontal = resolveArmorHorizontal(armor);

            if (mode == ArmorHudModule.DisplayMode.PERCENT_ONLY) {
                int rowHeight = 11;
                return horizontal
                        ? padding * 2 + rowHeight
                        : padding * 2 + rowHeight * count + spacing * Math.max(0, count - 1);
            }

            int icon = armor.getIconSize().get().intValue();
            boolean percentages = mode == ArmorHudModule.DisplayMode.ICONS_PERCENT
                    && armor.getShowValue().get();

            if (horizontal) {
                return padding * 2 + labelRoom + icon + (percentages ? 12 : 2);
            }

            int rowHeight = Math.max(icon, 10);
            return padding * 2 + labelRoom + rowHeight * count + spacing * Math.max(0, count - 1);
        }

        if (
                module
                        instanceof TotemHudModule totem
        ) {
            if (
                    totem
                            .getDisplayMode()
                            .get()
                            == TotemHudModule.DisplayMode.TEXT
            ) {
                return TOTEM_HEIGHT;
            }

            return totem
                    .getPadding()
                    .get()
                    .intValue()
                    * 2
                    + totem
                    .getIconSize()
                    .get()
                    .intValue()
                    + (
                    totem
                            .getShowLabel()
                            .get()
                            ? 14
                            : 0
            );
        }

        if (
                module
                        instanceof ShieldStatusHudModule shield
        ) {
            if (
                    shield
                            .getDisplayMode()
                            .get()
                            == ShieldStatusHudModule.DisplayMode.TEXT
            ) {
                return SHIELD_HEIGHT;
            }

            return shield
                    .getPadding()
                    .get()
                    .intValue()
                    * 2
                    + shield
                    .getIconSize()
                    .get()
                    .intValue()
                    + (
                    shield
                            .getShowLabel()
                            .get()
                            ? 14
                            : 0
            );
        }

        if (module instanceof AttackCooldownHudModule) {
            return ATTACK_HEIGHT;
        }

        if (
                module
                        instanceof PotionEffectsHudModule potion
        ) {
            int padding = potion.getPadding().get().intValue();

            if (potion.getDisplayMode().get() == PotionEffectsHudModule.DisplayMode.ICONS) {
                int icon = potion.getIconSize().get().intValue();
                int spacing = potion.getIconSpacing().get().intValue();
                int count = getPotionIconCountForDimensions();
                return resolvePotionIconsHorizontal(potion)
                        ? padding * 2 + icon
                        : padding * 2 + icon * count + spacing * Math.max(0, count - 1);
            }

            int spacing = potion.getRowSpacing().get().intValue();
            return Math.max(
                    66,
                    padding * 2 + (potion.getShowLabel().get() ? 16 : 0) + spacing * 4
            );
        }

        return 28;
    }

    public static boolean isCustomWarningEditorElement(
            Module module
    ) {
        return module instanceof WarningModule warning
                && warning.isEnabled()
                && warning.getScreenWarning().get()
                && warning.getPlacement().get()
                == WarningModule.Placement.CUSTOM;
    }

    public static int getEditorRenderedWidth(
            Module module
    ) {
        if (module instanceof HudModule hud) {
            EditorBounds bounds = editorLocalBounds(hud);
            float scale =
                    hud instanceof StyledHudModule styled
                            ? styled.getScale().get().floatValue()
                            : 1.0F;

            return Math.max(
                    1,
                    Math.round(bounds.width() * scale)
            );
        }

        if (module instanceof WarningModule warning) {
            return Math.max(
                    1,
                    Math.round(
                            WARNING_WIDTH
                                    * warning
                                    .getScale()
                                    .get()
                                    .floatValue()
                    )
            );
        }

        return 1;
    }

    public static int getEditorRenderedHeight(
            Module module
    ) {
        if (module instanceof HudModule hud) {
            EditorBounds bounds = editorLocalBounds(hud);
            float scale =
                    hud instanceof StyledHudModule styled
                            ? styled.getScale().get().floatValue()
                            : 1.0F;

            return Math.max(
                    1,
                    Math.round(bounds.height() * scale)
            );
        }

        if (module instanceof WarningModule warning) {
            return Math.max(
                    1,
                    Math.round(
                            WARNING_HEIGHT
                                    * warning
                                    .getScale()
                                    .get()
                                    .floatValue()
                    )
            );
        }

        return 1;
    }

    public static int getEditorPixelX(
            Module module,
            int screenWidth
    ) {
        if (module instanceof StyledHudModule styled
                && styled.hasCustomEditorSize()) {
            return HudLayout.pixelX(
                    styled,
                    screenWidth,
                    getEditorRenderedWidth(styled)
            );
        }

        if (module instanceof HudModule hud) {
            int baseRenderedWidth = getRenderedWidth(hud);
            int baseX = HudLayout.pixelX(
                    hud,
                    screenWidth,
                    baseRenderedWidth
            );

            EditorBounds bounds = editorLocalBounds(hud);
            float scale =
                    hud instanceof StyledHudModule styled
                            ? styled.getScale().get().floatValue()
                            : 1.0F;

            return baseX + Math.round(bounds.minX() * scale);
        }

        if (module instanceof WarningModule warning) {
            int renderedWidth = getEditorRenderedWidth(module);
            int available = Math.max(0, screenWidth - renderedWidth);

            return (int) Math.round(
                    warning.getCustomX()
                            * available
            );
        }

        return 0;
    }

    public static int getEditorPixelY(
            Module module,
            int screenHeight
    ) {
        if (module instanceof StyledHudModule styled
                && styled.hasCustomEditorSize()) {
            return HudLayout.pixelY(
                    styled,
                    screenHeight,
                    getEditorRenderedHeight(styled)
            );
        }

        if (module instanceof HudModule hud) {
            int baseRenderedHeight = getRenderedHeight(hud);
            int baseY = HudLayout.pixelY(
                    hud,
                    screenHeight,
                    baseRenderedHeight
            );

            EditorBounds bounds = editorLocalBounds(hud);
            float scale =
                    hud instanceof StyledHudModule styled
                            ? styled.getScale().get().floatValue()
                            : 1.0F;

            return baseY + Math.round(bounds.minY() * scale);
        }

        if (module instanceof WarningModule warning) {
            int renderedHeight = getEditorRenderedHeight(module);
            int available = Math.max(0, screenHeight - renderedHeight);

            return (int) Math.round(
                    warning.getCustomY()
                            * available
            );
        }

        return 0;
    }

    public static void setEditorBoxBounds(
            Module module,
            int pixelX,
            int pixelY,
            int renderedWidth,
            int renderedHeight,
            int screenWidth,
            int screenHeight
    ) {
        if (!(module instanceof StyledHudModule styled)) {
            return;
        }

        float scale = Math.max(0.001F, styled.getScale().get().floatValue());
        int contentX = getStyledContentAnchorX(styled, screenWidth);
        int contentY = getStyledContentAnchorY(styled, screenHeight);

        double offsetX = (pixelX - contentX) / (double) scale;
        double offsetY = (pixelY - contentY) / (double) scale;
        styled.setEditorBox(
                offsetX,
                offsetY,
                Math.max(1, renderedWidth) / scale,
                Math.max(1, renderedHeight) / scale
        );
        styled.setHudPosition(
                HudLayout.normalizedX(
                        pixelX,
                        screenWidth,
                        getEditorRenderedWidth(styled)
                ),
                HudLayout.normalizedY(
                        pixelY,
                        screenHeight,
                        getEditorRenderedHeight(styled)
                )
        );
    }

    public static void setEditorPixelPosition(
            Module module,
            int pixelX,
            int pixelY,
            int screenWidth,
            int screenHeight
    ) {
        if (module instanceof StyledHudModule styled
                && styled.hasCustomEditorSize()) {
            styled.setHudPosition(
                    HudLayout.normalizedX(
                            pixelX,
                            screenWidth,
                            getEditorRenderedWidth(styled)
                    ),
                    HudLayout.normalizedY(
                            pixelY,
                            screenHeight,
                            getEditorRenderedHeight(styled)
                    )
            );
            return;
        }

        if (module instanceof HudModule hud) {
            int baseRenderedWidth = getRenderedWidth(hud);
            int baseRenderedHeight = getRenderedHeight(hud);
            EditorBounds bounds = editorLocalBounds(hud);
            float scale =
                    hud instanceof StyledHudModule styled
                            ? styled.getScale().get().floatValue()
                            : 1.0F;

            int basePixelX = pixelX - Math.round(bounds.minX() * scale);
            int basePixelY = pixelY - Math.round(bounds.minY() * scale);

            hud.setHudPosition(
                    HudLayout.normalizedX(
                            basePixelX,
                            screenWidth,
                            baseRenderedWidth
                    ),
                    HudLayout.normalizedY(
                            basePixelY,
                            screenHeight,
                            baseRenderedHeight
                    )
            );

            return;
        }

        if (module instanceof WarningModule warning) {
            int renderedWidth = getEditorRenderedWidth(module);
            int renderedHeight = getEditorRenderedHeight(module);
            int availableX = Math.max(0, screenWidth - renderedWidth);
            int availableY = Math.max(0, screenHeight - renderedHeight);

            double normalizedX =
                    availableX == 0
                            ? 0.0
                            : pixelX / (double) availableX;

            double normalizedY =
                    availableY == 0
                            ? 0.0
                            : pixelY / (double) availableY;

            warning.setCustomPosition(
                    Math.max(0.0, Math.min(1.0, normalizedX)),
                    Math.max(0.0, Math.min(1.0, normalizedY))
            );
        }
    }

    




    private static EditorBounds editorLocalBounds(
            HudModule module
    ) {
        int baseWidth = getBaseWidth(module);
        int baseHeight = getBaseHeight(module);

        
        
        
        
        if (module instanceof StyledHudModule styled
                && styled.hasCustomEditorSize()) {
            int minX = styled.getEditorOffsetX().get().intValue();
            int minY = styled.getEditorOffsetY().get().intValue();
            int width = styled.hasCustomEditorWidth()
                    ? Math.max(1, styled.getEditorWidth().get().intValue())
                    : baseWidth;
            int height = styled.hasCustomEditorHeight()
                    ? Math.max(1, styled.getEditorHeight().get().intValue())
                    : baseHeight;

            return new EditorBounds(
                    minX,
                    minY,
                    minX + width,
                    minY + height
            );
        }

        
        
        
        return new EditorBounds(
                0,
                0,
                baseWidth,
                baseHeight
        );
    }

    



    private static EditorBounds contentLocalBounds(
            HudModule module
    ) {
        int baseWidth = getBaseWidth(module);
        int baseHeight = getBaseHeight(module);

        if (!(module instanceof StyledHudModule styled)) {
            return new EditorBounds(
                    0,
                    0,
                    baseWidth,
                    baseHeight
            );
        }

        class_310 client = class_310.method_1551();

        if (client == null || client.field_1772 == null) {
            return expandedOffsetBounds(
                    styled,
                    baseWidth,
                    baseHeight
            );
        }

        String label = null;
        String value = null;

        if (module instanceof FpsHudModule) {
            label = "FPS";
            value = Integer.toString(
                    client.method_47599()
            );
        } else if (module instanceof PingHudModule) {
            label = "PING";
            value = getPing(client) + " ms";
        } else if (
                module instanceof CoordinatesHudModule
                        && client.field_1724 != null
        ) {
            label = "XYZ";
            value =
                    (int) Math.floor(client.field_1724.method_23317())
                            + "  "
                            + (int) Math.floor(client.field_1724.method_23318())
                            + "  "
                            + (int) Math.floor(client.field_1724.method_23321());
        }

        if (label == null || value == null) {
            return expandedOffsetBounds(
                    styled,
                    baseWidth,
                    baseHeight
            );
        }

        int padding = styled.getPadding().get().intValue();
        boolean compact = styled.getCompactMode().get();
        String renderedLabel = hudLabel(
                styled,
                label
        );
        int valueWidth = client.field_1772.method_1727(
                value
        );

        int labelX =
                padding
                        + styled.getLabelOffsetX().get().intValue();
        int labelY =
                padding
                        + 2
                        + styled.getLabelOffsetY().get().intValue();
        int valueX =
                (compact
                        ? padding
                        : baseWidth - padding - valueWidth)
                        + styled.getValueOffsetX().get().intValue();
        int valueY =
                padding
                        + 2
                        + styled.getValueOffsetY().get().intValue();

        valueX = resolveValueX(
                client,
                renderedLabel,
                labelX,
                labelY,
                valueX,
                valueY,
                valueWidth,
                styled.getShowLabel().get()
                        && styled.getShowValue().get()
        );

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        if (
                styled.getShowLabel().get()
                        && !renderedLabel.isEmpty()
        ) {
            minX = Math.min(
                    minX,
                    labelX
            );
            minY = Math.min(
                    minY,
                    labelY
            );
            maxX = Math.max(
                    maxX,
                    labelX
                            + client.field_1772.method_1727(
                            renderedLabel
                    )
            );
            maxY = Math.max(
                    maxY,
                    labelY + client.field_1772.field_2000
            );
        }

        if (styled.getShowValue().get()) {
            minX = Math.min(
                    minX,
                    valueX
            );
            minY = Math.min(
                    minY,
                    valueY
            );
            maxX = Math.max(
                    maxX,
                    valueX + valueWidth
            );
            maxY = Math.max(
                    maxY,
                    valueY + client.field_1772.field_2000
            );
        }

        if (minX == Integer.MAX_VALUE) {
            return new EditorBounds(
                    0,
                    0,
                    baseWidth,
                    baseHeight
            );
        }

        int margin = 4;

        return new EditorBounds(
                minX - margin,
                minY - margin,
                maxX + margin,
                maxY + margin
        );
    }

    public static int getEditorContentRenderedWidth(
            Module module
    ) {
        if (!(module instanceof HudModule hud)) {
            return getEditorRenderedWidth(module);
        }

        EditorBounds bounds = contentLocalBounds(hud);
        float scale = hud instanceof StyledHudModule styled
                ? styled.getScale().get().floatValue()
                : 1.0F;

        return Math.max(
                1,
                Math.round(bounds.width() * scale)
        );
    }

    public static int getEditorContentRenderedHeight(
            Module module
    ) {
        if (!(module instanceof HudModule hud)) {
            return getEditorRenderedHeight(module);
        }

        EditorBounds bounds = contentLocalBounds(hud);
        float scale = hud instanceof StyledHudModule styled
                ? styled.getScale().get().floatValue()
                : 1.0F;

        return Math.max(
                1,
                Math.round(bounds.height() * scale)
        );
    }

    public static int getEditorContentPixelX(
            Module module,
            int screenWidth
    ) {
        if (!(module instanceof HudModule hud)) {
            return getEditorPixelX(
                    module,
                    screenWidth
            );
        }

        int baseX = hud instanceof StyledHudModule styled
                ? getStyledContentAnchorX(styled, screenWidth)
                : HudLayout.pixelX(
                        hud,
                        screenWidth,
                        getRenderedWidth(hud)
                );
        EditorBounds bounds = contentLocalBounds(hud);
        float scale = hud instanceof StyledHudModule styled
                ? styled.getScale().get().floatValue()
                : 1.0F;

        return baseX
                + Math.round(bounds.minX() * scale);
    }

    public static int getEditorContentPixelY(
            Module module,
            int screenHeight
    ) {
        if (!(module instanceof HudModule hud)) {
            return getEditorPixelY(
                    module,
                    screenHeight
            );
        }

        int baseY = hud instanceof StyledHudModule styled
                ? getStyledContentAnchorY(styled, screenHeight)
                : HudLayout.pixelY(
                        hud,
                        screenHeight,
                        getRenderedHeight(hud)
                );
        EditorBounds bounds = contentLocalBounds(hud);
        float scale = hud instanceof StyledHudModule styled
                ? styled.getScale().get().floatValue()
                : 1.0F;

        return baseY
                + Math.round(bounds.minY() * scale);
    }

    private static EditorBounds expandedOffsetBounds(
            StyledHudModule module,
            int baseWidth,
            int baseHeight
    ) {
        int minX = Math.min(0, Math.min(
                module.getLabelOffsetX().get().intValue(),
                module.getValueOffsetX().get().intValue()
        ));
        int minY = Math.min(0, Math.min(
                module.getLabelOffsetY().get().intValue(),
                module.getValueOffsetY().get().intValue()
        ));
        int maxOffsetX = Math.max(0, Math.max(
                module.getLabelOffsetX().get().intValue(),
                module.getValueOffsetX().get().intValue()
        ));
        int maxOffsetY = Math.max(0, Math.max(
                module.getLabelOffsetY().get().intValue(),
                module.getValueOffsetY().get().intValue()
        ));

        return new EditorBounds(
                minX,
                minY,
                baseWidth + maxOffsetX,
                baseHeight + maxOffsetY
        );
    }

    private record EditorBounds(
            int minX,
            int minY,
            int maxX,
            int maxY
    ) {
        int width() {
            return Math.max(1, maxX - minX);
        }

        int height() {
            return Math.max(1, maxY - minY);
        }
    }

    
    
    

    private static void renderFps(
            class_332 context,
            class_310 client
    ) {
        FpsHudModule module =
                get(FpsHudModule.class);

        if (!enabled(module)) {
            return;
        }

        renderTextCard(
                context,
                client,
                module,
                "FPS",
                String.valueOf(
                        client.method_47599()
                )
        );
    }

    private static void renderPing(
            class_332 context,
            class_310 client
    ) {
        PingHudModule module =
                get(PingHudModule.class);

        if (!enabled(module)) {
            return;
        }

        renderTextCard(
                context,
                client,
                module,
                "PING",
                getPing(client) + " ms"
        );
    }

    private static void renderCoordinates(
            class_332 context,
            class_310 client
    ) {
        CoordinatesHudModule module =
                get(CoordinatesHudModule.class);

        if (!enabled(module)) {
            return;
        }

        StreamerPrivacyModule privacy = get(StreamerPrivacyModule.class);
        boolean privacyEnabled = enabled(privacy);

        
        
        
        
        String value = privacyEnabled
                ? "HIDDEN"
                : (int) Math.floor(
                        client.field_1724.method_23317()
                )
                        + "  "
                        + (int) Math.floor(
                        client.field_1724.method_23318()
                )
                        + "  "
                        + (int) Math.floor(
                        client.field_1724.method_23321()
                );

        renderTextCard(
                context,
                client,
                module,
                "XYZ",
                value
        );
    }

    private static void renderTotems(
            class_332 context,
            class_310 client
    ) {
        TotemHudModule module =
                get(
                        TotemHudModule.class
                );

        if (!enabled(module)) {
            return;
        }

        if (
                module
                        .getDisplayMode()
                        .get()
                        == TotemHudModule.DisplayMode.TEXT
        ) {
            renderTextCard(
                    context,
                    client,
                    module,
                    "TOTEMS",
                    Integer.toString(countTotems(client)),
                    module.getTextBackground().get()
            );

            return;
        }

        withHudTransform(
                context,
                module,
                () -> {
                    int width =
                            getBaseWidth(module);

                    int height =
                            getBaseHeight(module);

                    drawCard(
                            context,
                            module,
                            0,
                            0,
                            width,
                            height
                    );

                    int padding =
                            module
                                    .getPadding()
                                    .get()
                                    .intValue();

                    int icon =
                            module
                                    .getIconSize()
                                    .get()
                                    .intValue();

                    int contentY =
                            padding;

                    if (
                            module
                                    .getShowLabel()
                                    .get()
                    ) {
                        drawText(
                                context,
                                client,
                                module,
                                hudLabel(
                                        module,
                                        "TOTEMS"
                                ),
                                padding
                                        + module
                                        .getLabelOffsetX()
                                        .get()
                                        .intValue(),
                                padding
                                        + module
                                        .getLabelOffsetY()
                                        .get()
                                        .intValue(),
                                module
                                        .getLabelColor()
                                        .getArgb()
                        );

                        contentY += 14;
                    }

                    drawScaledItem(
                            context,
                            new class_1799(
                                    class_1802.field_8288
                            ),
                            padding,
                            contentY,
                            icon
                    );

                    if (module.getDisplayMode().get() == TotemHudModule.DisplayMode.ICON_OVERLAY_COUNT
                            && module.getShowValue().get()) {
                        drawTotemOverlayCount(
                                context,
                                client,
                                Integer.toString(countTotems(client)),
                                padding,
                                contentY,
                                icon,
                                module.getValueColor().getArgb(),
                                module.getTextShadow().get()
                        );
                        return;
                    }

                    if (
                            module
                                    .getShowValue()
                                    .get()
                    ) {
                        String value =
                                Integer.toString(
                                        countTotems(
                                                client
                                        )
                                );

                        String renderedLabel = hudLabel(
                                module,
                                "TOTEMS"
                        );
                        int labelX =
                                padding
                                        + module.getLabelOffsetX().get().intValue();
                        int labelY =
                                padding
                                        + module.getLabelOffsetY().get().intValue();
                        int valueWidth =
                                client.field_1772.method_1727(value);
                        int valueX =
                                padding
                                        + icon
                                        + 6
                                        + module.getValueOffsetX().get().intValue();
                        int valueY =
                                contentY
                                        + Math.max(
                                        0,
                                        (icon - client.field_1772.field_2000) / 2
                                )
                                        + module.getValueOffsetY().get().intValue();

                        valueX = resolveValueX(
                                client,
                                renderedLabel,
                                labelX,
                                labelY,
                                valueX,
                                valueY,
                                valueWidth,
                                module.getShowLabel().get()
                                        && module.getShowValue().get()
                        );

                        drawText(
                                context,
                                client,
                                module,
                                value,
                                valueX,
                                valueY,
                                module.getValueColor().getArgb()
                        );
                    }
                }
        );
    }

    private static void drawTotemOverlayCount(
            class_332 context,
            class_310 client,
            String value,
            int iconX,
            int iconY,
            int iconSize,
            int color,
            boolean shadow
    ) {
        if (value == null || value.isBlank()) return;
        int textWidth = Math.max(1, client.field_1772.method_1727(value));
        float scale = Math.max(0.55F, Math.min(0.82F, (iconSize - 3.0F) / textWidth));
        Matrix3x2fStack matrices = context.method_51448();
        matrices.pushMatrix();
        float right = iconX + iconSize - 1.0F;
        float bottom = iconY + iconSize - 1.0F;
        matrices.translate(right, bottom);
        matrices.scale(scale, scale);
        int x = -textWidth;
        int y = -client.field_1772.field_2000;
        if (shadow) {
            context.method_51433(client.field_1772, value, x + 2, y + 2, 0xB0000000, false);
        }
        context.method_51433(client.field_1772, value, x, y, color, false);
        matrices.popMatrix();
    }

    private static void renderArmor(
            class_332 context,
            class_310 client
    ) {
        ArmorHudModule module = get(ArmorHudModule.class);
        if (!enabled(module)) {
            return;
        }

        ArmorHudModule.DisplayMode mode = module.getDisplayMode().get();
        if (mode == ArmorHudModule.DisplayMode.TEXT) {
            renderArmorText(context, client, module);
            return;
        }

        boolean individual = module.getIndividualArmorPositions().get() && module.isIconMode();
        if (individual) {
            module.enforceIndividualModeStyle();
        }

        List<class_1799> stackList = new ArrayList<>();
        stackList.add(client.field_1724.method_6118(class_1304.field_6169));
        stackList.add(client.field_1724.method_6118(class_1304.field_6174));
        stackList.add(client.field_1724.method_6118(class_1304.field_6172));
        stackList.add(client.field_1724.method_6118(class_1304.field_6166));
        if (module.getShowTools().get()) stackList.add(client.field_1724.method_6047().method_7963() ? client.field_1724.method_6047() : class_1799.field_8037);
        if (module.getShowOffhand().get()) stackList.add(client.field_1724.method_6079());
        class_1799[] stacks = stackList.toArray(class_1799[]::new);

        boolean anyArmor = false;
        for (class_1799 stack : stacks) {
            if (!stack.method_7960()) {
                anyArmor = true;
                break;
            }
        }
        if (!anyArmor) {
            return;
        }

        withHudTransform(
                context,
                module,
                () -> {
                    int width = getBaseWidth(module);
                    int height = getBaseHeight(module);
                    if (!individual) {
                        drawCard(context, module, 0, 0, width, height);
                    }

                    int padding = module.getPadding().get().intValue();
                    int spacing = module.getIconSpacing().get().intValue();
                    boolean horizontal = resolveArmorHorizontal(module);

                    if (mode == ArmorHudModule.DisplayMode.PERCENT_ONLY) {
                        int count = Math.max(1, stacks.length);
                        int totalSpacing = spacing * Math.max(0, count - 1);
                        int cellWidth = horizontal
                                ? Math.max(1, (width - padding * 2 - totalSpacing) / count)
                                : Math.max(1, width - padding * 2);
                        int cellHeight = horizontal
                                ? Math.max(client.field_1772.field_2000, height - padding * 2)
                                : Math.max(client.field_1772.field_2000, (height - padding * 2 - totalSpacing) / count);

                        for (int index = 0; index < stacks.length; index++) {
                            if (stacks[index].method_7960() || !stacks[index].method_7963()) continue;
                            String value = durabilityPercent(stacks[index]) + "%";
                            int valueWidth = client.field_1772.method_1727(value);
                            int x = horizontal
                                    ? padding + index * (cellWidth + spacing) + Math.max(0, (cellWidth - valueWidth) / 2)
                                    : padding + Math.max(0, (cellWidth - valueWidth) / 2);
                            int y = horizontal
                                    ? padding + Math.max(0, (cellHeight - client.field_1772.field_2000) / 2)
                                    : padding + index * (cellHeight + spacing) + Math.max(0, (cellHeight - client.field_1772.field_2000) / 2);
                            drawText(context, client, module, value, x, y, module.getValueColor().getArgb());
                        }
                        return;
                    }

                    if (!individual && module.getShowLabel().get()) {
                        drawText(
                                context,
                                client,
                                module,
                                hudLabel(module, "ARMOR"),
                                padding + module.getLabelOffsetX().get().intValue(),
                                padding + module.getLabelOffsetY().get().intValue(),
                                module.getLabelColor().getArgb()
                        );
                    }

                    boolean percentages = mode == ArmorHudModule.DisplayMode.ICONS_PERCENT
                            && module.getShowValue().get();

                    for (int index = 0; index < stacks.length; index++) {
                        class_1799 stack = stacks[index];
                        if (stack.method_7960()) continue;
                        ArmorPieceGeometry geometry = armorPieceGeometry(module, index, true);
                        int iconX = geometry.x();
                        int iconY = geometry.y();
                        int icon = geometry.size();

                        drawScaledItem(context, stack, iconX, iconY, icon);

                        
                        
                        if (module.getShowDurabilityBar().get()
                                && !stack.method_7960()
                                && stack.method_7963()) {
                            int percent = durabilityPercent(stack);
                            if (percent < 100) {
                                int barY = iconY + icon - 2;
                                context.method_25294(iconX, barY, iconX + icon, barY + 2, 0xCC25252B);
                                context.method_25294(
                                        iconX,
                                        barY,
                                        iconX + Math.round(icon * percent / 100.0F),
                                        barY + 2,
                                        module.getDurabilityBarColorForPercent(percent)
                                );
                            }
                        }

                        if (percentages && stack.method_7963()) {
                            String value = durabilityPercent(stack) + "%";
                            int valueWidth = client.field_1772.method_1727(value);
                            int valueX = horizontal
                                    ? iconX + Math.max(0, (icon - valueWidth) / 2)
                                    : iconX + icon + 6;
                            int valueY = horizontal
                                    ? iconY + icon + 2
                                    : iconY + Math.max(0, (icon - client.field_1772.field_2000) / 2);
                            drawText(context, client, module, value, valueX, valueY, module.getValueColor().getArgb());
                        }
                    }
                }
        );
    }

    private static ArmorPieceGeometry armorPieceGeometry(
            ArmorHudModule module,
            int index,
            boolean includeIndividualOffset
    ) {
        int width = getBaseWidth(module);
        int height = getBaseHeight(module);
        int padding = module.getPadding().get().intValue();
        int requestedIcon = module.getIconSize().get().intValue();
        int spacing = module.getIconSpacing().get().intValue();
        int count = Math.max(1, module.getConfiguredItemCount());
        boolean horizontal = resolveArmorHorizontal(module);
        boolean percentages = module.getDisplayMode().get() == ArmorHudModule.DisplayMode.ICONS_PERCENT
                && module.getShowValue().get();
        int labelRoom = module.getShowLabel().get() ? 14 : 0;
        int contentY = padding + labelRoom;

        int icon;
        int x;
        int y;

        if (horizontal) {
            int available = Math.max(count * 10, width - padding * 2 - spacing * Math.max(0, count - 1));
            int cellWidth = Math.max(10, available / count);
            icon = Math.max(10, Math.min(requestedIcon, cellWidth));
            x = padding + index * (cellWidth + spacing) + Math.max(0, (cellWidth - icon) / 2);
            y = Math.min(Math.max(0, height - icon), contentY);
        } else {
            int valueRoom = percentages ? 38 : 0;
            int availableHeight = Math.max(count * 10, height - contentY - padding - spacing * Math.max(0, count - 1));
            int cellHeight = Math.max(10, availableHeight / count);
            icon = Math.max(10, Math.min(requestedIcon, cellHeight));
            x = Math.min(Math.max(0, width - icon - valueRoom), padding);
            y = contentY + index * (cellHeight + spacing) + Math.max(0, (cellHeight - icon) / 2);
        }

        if (includeIndividualOffset && module.getIndividualArmorPositions().get()) {
            x += module.getPieceOffsetXValue(index);
            y += module.getPieceOffsetYValue(index);
            double pieceScale = module.getPieceScale(index).get();
            icon = Math.max(4, (int) Math.round(icon * pieceScale));
        }

        return new ArmorPieceGeometry(x, y, icon);
    }

    public static int getArmorPieceEditorX(
            ArmorHudModule module,
            int index,
            int screenWidth
    ) {
        int baseX = getStyledContentAnchorX(module, screenWidth);
        float scale = module.getScale().get().floatValue();
        return baseX + Math.round(armorPieceGeometry(module, index, true).x() * scale);
    }

    public static int getArmorPieceEditorY(
            ArmorHudModule module,
            int index,
            int screenHeight
    ) {
        int baseY = getStyledContentAnchorY(module, screenHeight);
        float scale = module.getScale().get().floatValue();
        return baseY + Math.round(armorPieceGeometry(module, index, true).y() * scale);
    }

    public static int getArmorPieceEditorSize(ArmorHudModule module, int index) {
        return Math.max(1, Math.round(
                armorPieceGeometry(module, index, true).size()
                        * module.getScale().get().floatValue()
        ));
    }

    public static void setArmorPieceEditorPixelPosition(
            ArmorHudModule module,
            int index,
            int pixelX,
            int pixelY,
            int screenWidth,
            int screenHeight
    ) {
        int baseX = getStyledContentAnchorX(module, screenWidth);
        int baseY = getStyledContentAnchorY(module, screenHeight);
        float scale = Math.max(0.001F, module.getScale().get().floatValue());
        ArmorPieceGeometry natural = armorPieceGeometry(module, index, false);

        double localX = (pixelX - baseX) / scale;
        double localY = (pixelY - baseY) / scale;
        module.setPieceOffset(index, localX - natural.x(), localY - natural.y());
    }

    private record ArmorPieceGeometry(int x, int y, int size) {
    }

    private static void renderArmorText(
            class_332 context,
            class_310 client,
            ArmorHudModule module
    ) {
        boolean noArmor = client.field_1724.method_6118(class_1304.field_6169).method_7960()
                && client.field_1724.method_6118(class_1304.field_6174).method_7960()
                && client.field_1724.method_6118(class_1304.field_6172).method_7960()
                && client.field_1724.method_6118(class_1304.field_6166).method_7960();
        boolean noTool = !module.getShowTools().get() || client.field_1724.method_6047().method_7960() || !client.field_1724.method_6047().method_7963();
        boolean noOffhand = !module.getShowOffhand().get() || client.field_1724.method_6079().method_7960();
        if (noArmor && noTool && noOffhand) return;
        withHudTransform(
                context,
                module,
                () -> {
                    int width =
                            ARMOR_WIDTH;

                    int height =
                            ARMOR_HEIGHT + Math.max(0, module.getConfiguredItemCount() - 4) * 14;

                    drawCard(
                            context,
                            module,
                            0,
                            0,
                            width,
                            height
                    );

                    int padding =
                            module
                                    .getPadding()
                                    .get()
                                    .intValue();

                    int labelOffsetX =
                            module
                                    .getLabelOffsetX()
                                    .get()
                                    .intValue();

                    int labelOffsetY =
                            module
                                    .getLabelOffsetY()
                                    .get()
                                    .intValue();

                    int rowY =
                            module
                                    .getShowLabel()
                                    .get()
                                    ? padding + 17
                                    : padding;

                    if (
                            module
                                    .getShowLabel()
                                    .get()
                    ) {
                        drawText(
                                context,
                                client,
                                module,
                                hudLabel(
                                        module,
                                        "ARMOR"
                                ),
                                padding
                                        + labelOffsetX,
                                padding
                                        + labelOffsetY,
                                module
                                        .getLabelColor()
                                        .getArgb()
                        );
                    }

                    renderArmorRow(
                            context,
                            client,
                            module,
                            width,
                            rowY,
                            "HELMET",
                            client.field_1724
                                    .method_6118(
                                            class_1304.field_6169
                                    )
                    );

                    renderArmorRow(
                            context,
                            client,
                            module,
                            width,
                            rowY + 14,
                            "CHEST",
                            client.field_1724
                                    .method_6118(
                                            class_1304.field_6174
                                    )
                    );

                    renderArmorRow(
                            context,
                            client,
                            module,
                            width,
                            rowY + 28,
                            "LEGS",
                            client.field_1724
                                    .method_6118(
                                            class_1304.field_6172
                                    )
                    );

                    renderArmorRow(
                            context,
                            client,
                            module,
                            width,
                            rowY + 42,
                            "BOOTS",
                            client.field_1724
                                    .method_6118(
                                            class_1304.field_6166
                                    )
                    );

                    int extraY = rowY + 56;
                    if (module.getShowTools().get()) {
                        class_1799 tool = client.field_1724.method_6047().method_7963()
                                ? client.field_1724.method_6047() : class_1799.field_8037;
                        renderArmorRow(context, client, module, width, extraY, "TOOL", tool);
                        extraY += 14;
                    }
                    if (module.getShowOffhand().get()) {
                        renderArmorRow(context, client, module, width, extraY, "OFFHAND", client.field_1724.method_6079());
                    }
                }
        );
    }

    
    
    

    private static void renderShieldStatus(
            class_332 context,
            class_310 client
    ) {
        ShieldStatusHudModule module =
                get(
                        ShieldStatusHudModule.class
                );

        if (!enabled(module)) {
            return;
        }

        class_1799 shield =
                findShield(client);

        String state;

        if (
                shield == null
                        ||
                shield.method_7960()
        ) {
            state =
                    "NO SHIELD";

        } else if (
                client.field_1724.method_6115()
                        &&
                client.field_1724
                        .method_6030()
                        .method_31574(
                                class_1802.field_8255
                        )
        ) {
            state =
                    "BLOCKING";

        } else if (
                client.field_1724
                        .method_7357()
                        .method_7904(
                                shield
                        )
        ) {
            state =
                    "COOLDOWN";

        } else {
            state =
                    "READY";
        }

        if (
                module
                        .getDisplayMode()
                        .get()
                        == ShieldStatusHudModule.DisplayMode.TEXT
        ) {
            renderTextCard(
                    context,
                    client,
                    module,
                    "SHIELD",
                    state
            );

            return;
        }

        withHudTransform(
                context,
                module,
                () -> {
                    int width =
                            getBaseWidth(module);

                    int height =
                            getBaseHeight(module);

                    drawCard(
                            context,
                            module,
                            0,
                            0,
                            width,
                            height
                    );

                    int padding =
                            module
                                    .getPadding()
                                    .get()
                                    .intValue();

                    int icon =
                            module
                                    .getIconSize()
                                    .get()
                                    .intValue();

                    int contentY =
                            padding;

                    if (
                            module
                                    .getShowLabel()
                                    .get()
                    ) {
                        drawText(
                                context,
                                client,
                                module,
                                hudLabel(
                                        module,
                                        "SHIELD"
                                ),
                                padding
                                        + module
                                        .getLabelOffsetX()
                                        .get()
                                        .intValue(),
                                padding
                                        + module
                                        .getLabelOffsetY()
                                        .get()
                                        .intValue(),
                                module
                                        .getLabelColor()
                                        .getArgb()
                        );

                        contentY += 14;
                    }

                    drawScaledItem(
                            context,
                            new class_1799(
                                    class_1802.field_8255
                            ),
                            padding,
                            contentY,
                            icon
                    );

                    if (
                            module
                                    .getShowValue()
                                    .get()
                    ) {
                        String renderedLabel = hudLabel(
                                module,
                                "SHIELD"
                        );
                        int labelX =
                                padding
                                        + module.getLabelOffsetX().get().intValue();
                        int labelY =
                                padding
                                        + module.getLabelOffsetY().get().intValue();
                        int valueWidth =
                                client.field_1772.method_1727(state);
                        int valueX =
                                padding
                                        + icon
                                        + 6
                                        + module.getValueOffsetX().get().intValue();
                        int valueY =
                                contentY
                                        + Math.max(
                                        0,
                                        (icon - client.field_1772.field_2000) / 2
                                )
                                        + module.getValueOffsetY().get().intValue();

                        valueX = resolveValueX(
                                client,
                                renderedLabel,
                                labelX,
                                labelY,
                                valueX,
                                valueY,
                                valueWidth,
                                module.getShowLabel().get()
                                        && module.getShowValue().get()
                        );

                        drawText(
                                context,
                                client,
                                module,
                                state,
                                valueX,
                                valueY,
                                module.getValueColor().getArgb()
                        );
                    }
                }
        );
    }

    
    
    

    private static void renderAttackCooldown(
            class_332 context,
            class_310 client
    ) {
        AttackCooldownHudModule module =
                get(
                        AttackCooldownHudModule.class
                );

        if (!enabled(module)) {
            return;
        }

        float progress =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                client.field_1724
                                        .method_7261(
                                                0.0F
                                        )
                        )
                );

        withHudTransform(
                context,
                module,
                () -> {
                    int width =
                            getBaseWidth(module);

                    int height =
                            getBaseHeight(module);

                    drawCard(
                            context,
                            module,
                            0,
                            0,
                            width,
                            height
                    );

                    int padding =
                            module
                                    .getPadding()
                                    .get()
                                    .intValue();

                    int labelX =
                            padding
                                    + module
                                    .getLabelOffsetX()
                                    .get()
                                    .intValue();

                    int labelY =
                            padding
                                    + module
                                    .getLabelOffsetY()
                                    .get()
                                    .intValue();

                    AttackCooldownHudModule.DisplayMode mode =
                            module
                                    .getDisplayMode()
                                    .get();

                    if (
                            mode
                                    != AttackCooldownHudModule.DisplayMode.BAR
                                    &&
                            module
                                    .getShowLabel()
                                    .get()
                    ) {
                        drawText(
                                context,
                                client,
                                module,
                                hudLabel(
                                        module,
                                        "ATTACK"
                                ),
                                labelX,
                                labelY,
                                module
                                        .getLabelColor()
                                        .getArgb()
                        );
                    }

                    if (
                            mode
                                    != AttackCooldownHudModule.DisplayMode.BAR
                                    &&
                            module
                                    .getShowValue()
                                    .get()
                    ) {
                        String value =
                                Math.round(
                                        progress * 100.0F
                                )
                                        + "%";

                        int valueWidth =
                                client
                                        .field_1772
                                        .method_1727(
                                                value
                                        );

                        int valueX =
                                width
                                        - padding
                                        - valueWidth
                                        + module.getValueOffsetX().get().intValue();
                        int valueY =
                                padding
                                        + module.getValueOffsetY().get().intValue();

                        valueX = resolveValueX(
                                client,
                                hudLabel(module, "ATTACK"),
                                labelX,
                                labelY,
                                valueX,
                                valueY,
                                valueWidth,
                                module.getShowLabel().get()
                                        && module.getShowValue().get()
                        );

                        drawText(
                                context,
                                client,
                                module,
                                value,
                                valueX,
                                valueY,
                                module.getValueColor().getArgb()
                        );
                    }

                    if (
                            mode
                                    != AttackCooldownHudModule.DisplayMode.PERCENT
                                    &&
                            module
                                    .getShowValue()
                                    .get()
                    ) {
                        int barX =
                                padding
                                        + module
                                        .getValueOffsetX()
                                        .get()
                                        .intValue();

                        int barWidth =
                                Math.max(
                                        4,
                                        width
                                                - padding * 2
                                );

                        int barY =
                                height
                                        - padding
                                        - 4
                                        + module
                                        .getValueOffsetY()
                                        .get()
                                        .intValue();

                        context.method_25294(
                                barX,
                                barY,
                                barX + barWidth,
                                barY + 4,
                                0xFF35353B
                        );

                        context.method_25294(
                                barX,
                                barY,
                                barX
                                        + Math.round(
                                        barWidth
                                                * progress
                                ),
                                barY + 4,
                                module
                                        .getAccentColor()
                                        .getArgb()
                        );
                    }
                }
        );
    }

    
    
    

    private static void renderPotionEffects(
            class_332 context,
            class_310 client
    ) {
        PotionEffectsHudModule module = get(PotionEffectsHudModule.class);
        if (!enabled(module)) {
            return;
        }

        List<class_1293> effects = getSortedPotionEffects(client);
        if (effects.isEmpty()) {
            return;
        }
        PotionEffectsHudModule.DisplayMode mode = module.getDisplayMode().get();

        withHudTransform(
                context,
                module,
                () -> {
                    int width = getBaseWidth(module);
                    int height = getBaseHeight(module);
                    boolean renderBackground = mode != PotionEffectsHudModule.DisplayMode.TEXT
                            || module.getTextBackground().get();
                    drawCard(context, module, 0, 0, width, height, renderBackground);

                    int padding = module.getPadding().get().intValue();
                    int icon = module.getIconSize().get().intValue();

                    if (mode == PotionEffectsHudModule.DisplayMode.ICONS) {
                        int spacing = module.getIconSpacing().get().intValue();
                        boolean horizontal = resolvePotionIconsHorizontal(module);
                        int shown = 0;

                        for (class_1293 effect : effects) {
                            if (shown >= 12) break;

                            int iconX = horizontal ? padding + shown * (icon + spacing) : padding;
                            int iconY = horizontal ? padding : padding + shown * (icon + spacing);

                            if (iconX + icon > width - 1 || iconY + icon > height - 1) {
                                break;
                            }

                            drawStatusEffectIcon(context, effect, iconX, iconY, icon);

                            if (module.getShowTimer().get()) {
                                drawSmallPotionTimer(
                                        context,
                                        client,
                                        formatTicks(effect.method_5584()),
                                        iconX,
                                        iconY,
                                        icon,
                                        module.getValueColor().getArgb(),
                                        module.getIconTimerShadow().get()
                                );
                            }

                            shown++;
                        }
                        return;
                    }

                    int labelOffsetX = module.getLabelOffsetX().get().intValue();
                    int labelOffsetY = module.getLabelOffsetY().get().intValue();
                    int valueOffsetX = module.getValueOffsetX().get().intValue();
                    int valueOffsetY = module.getValueOffsetY().get().intValue();
                    int rowY = padding;

                    if (module.getShowLabel().get()) {
                        drawText(
                                context,
                                client,
                                module,
                                hudLabel(module, "EFFECTS"),
                                padding + labelOffsetX,
                                padding + labelOffsetY,
                                module.getLabelColor().getArgb()
                        );
                        rowY += 16;
                    }

                    int spacing = module.getRowSpacing().get().intValue();
                    int shown = 0;

                    for (class_1293 effect : effects) {
                        if (shown >= 6 || rowY + spacing > height - 4) break;

                        String name = class_2561.method_43471(effect.method_5586()).getString();
                        if (module.getShowAmplifier().get() && effect.method_5578() > 0) {
                            name += " " + roman(effect.method_5578() + 1);
                        }

                        String timer = formatTicks(effect.method_5584());
                        int centeredTextY = rowY + Math.max(0, (spacing - client.field_1772.field_2000) / 2);
                        int timerWidth = client.field_1772.method_1727(timer);
                        int timerX = width - padding - timerWidth + valueOffsetX;
                        boolean showTimer = module.getShowTimer().get() && module.getShowValue().get();

                        int nameX;
                        if (mode == PotionEffectsHudModule.DisplayMode.TEXT) {
                            nameX = padding + labelOffsetX;
                        } else {
                            int iconY = rowY + Math.max(0, (spacing - icon) / 2);
                            drawStatusEffectIcon(context, effect, padding, iconY, icon);
                            nameX = padding + icon + 5 + labelOffsetX;
                        }

                        boolean showName = mode == PotionEffectsHudModule.DisplayMode.TEXT
                                || mode == PotionEffectsHudModule.DisplayMode.ICON_NAME_TIMER;
                        if (showName) {
                            timerX = resolveValueX(
                                    client, name, nameX, centeredTextY + labelOffsetY,
                                    timerX, centeredTextY + valueOffsetY, timerWidth, showTimer
                            );
                            drawText(
                                    context,
                                    client,
                                    module,
                                    name,
                                    nameX,
                                    centeredTextY + labelOffsetY,
                                    module.getEffectTextColor().getArgb()
                            );
                        }

                        if (showTimer) {
                            drawText(
                                    context,
                                    client,
                                    module,
                                    timer,
                                    timerX,
                                    centeredTextY + valueOffsetY,
                                    module.getValueColor().getArgb()
                            );
                        }

                        rowY += spacing;
                        shown++;
                    }
                }
        );
    }

    private static void drawSmallPotionTimer(
            class_332 context,
            class_310 client,
            String timer,
            int iconX,
            int iconY,
            int iconSize,
            int color,
            boolean shadow
    ) {
        if (timer == null || timer.isBlank()) return;

        int textWidth = Math.max(1, client.field_1772.method_1727(timer));
        float maxScale = 0.62F;
        float fitScale = (iconSize - 2.0F) / textWidth;
        float scale = Math.max(0.42F, Math.min(maxScale, fitScale));
        int stripHeight = Math.max(5, Math.round(client.field_1772.field_2000 * scale) + 1);

        context.method_25294(
                iconX,
                iconY + iconSize - stripHeight,
                iconX + iconSize,
                iconY + iconSize,
                0x99000000
        );

        Matrix3x2fStack matrices = context.method_51448();
        matrices.pushMatrix();
        float centerX = iconX + iconSize / 2.0F;
        float baselineY = iconY + iconSize - stripHeight + 1.0F;
        matrices.translate(centerX, baselineY);
        matrices.scale(scale, scale);
        
        
        
        
        if (shadow) {
            context.method_51433(
                    client.field_1772,
                    timer,
                    -textWidth / 2 + 2,
                    2,
                    0xB0000000,
                    false
            );
        }
        context.method_51433(
                client.field_1772,
                timer,
                -textWidth / 2,
                0,
                color,
                false
        );
        matrices.popMatrix();
    }

    
    private static int vanillaShadowColor(int color) {
        return (color & 0xFF000000) | ((color >> 2) & 0x003F3F3F);
    }

    private static int getPotionIconCountForDimensions() {
        class_310 client =
                class_310.method_1551();

        if (
                client != null
                        && client.field_1755
                        instanceof ModuleSettingsScreen
        ) {
            return 3;
        }

        if (
                client == null
                        || client.field_1724 == null
        ) {
            return 1;
        }

        return Math.max(
                1,
                Math.min(
                        12,
                        getSortedPotionEffects(client).size()
                )
        );
    }

    
    
    

    private static void renderWarnings(
            class_332 context,
            class_310 client
    ) {
        if (warningWorldKey != client.field_1687) {
            clearWarningTransientState();
            warningWorldKey = client.field_1687;
        }

        List<WarningEntry> warnings = new ArrayList<>();
        collectArmorWarning(warnings, client);
        collectTotemWarning(warnings, client);
        collectHealthWarning(warnings, client);
        collectEffectWarning(warnings, client);
        collectFoodWarning(warnings, client);

        WarningQueueModule queue = get(WarningQueueModule.class);
        updateWarningSounds(warnings, queue);

        List<WarningEntry> visibleWarnings = new ArrayList<>();
        for (WarningEntry warning : warnings) {
            if (warning.module().getScreenWarning().get()) {
                visibleWarnings.add(warning);
            }
        }

        int spacing = 5;
        if (enabled(queue)) {
            visibleWarnings.sort(Comparator.comparingInt(entry -> warningPriority(entry.module())));
            int maxVisible = Math.max(1, queue.getMaxVisible().get().intValue());
            spacing = Math.max(0, queue.getSpacing().get().intValue());

            if (visibleWarnings.size() > maxVisible) {
                String signature = visibleWarnings.stream()
                        .map(entry -> entry.module().getName())
                        .reduce((a, b) -> a + "|" + b)
                        .orElse("");
                long now = System.currentTimeMillis();

                if (!signature.equals(warningQueueSignature)) {
                    warningQueueSignature = signature;
                    warningQueueStartIndex = 0;
                    warningQueueLastRotateMillis = now;
                } else {
                    long rotateMillis = Math.max(500L, Math.round(queue.getRotateSeconds().get() * 1000.0));
                    if (queue.getRotate().get() && now - warningQueueLastRotateMillis >= rotateMillis) {
                        warningQueueStartIndex = (warningQueueStartIndex + maxVisible) % visibleWarnings.size();
                        warningQueueLastRotateMillis = now;
                    }
                }

                List<WarningEntry> rotated = new ArrayList<>();
                for (int i = 0; i < maxVisible; i++) {
                    rotated.add(visibleWarnings.get((warningQueueStartIndex + i) % visibleWarnings.size()));
                }
                visibleWarnings = rotated;
            } else {
                warningQueueSignature = "";
                warningQueueStartIndex = 0;
                warningQueueLastRotateMillis = 0L;
            }
        } else {
            warningQueueSignature = "";
            warningQueueStartIndex = 0;
            warningQueueLastRotateMillis = 0L;
        }

        int screenWidth = context.method_51421();
        int screenHeight = context.method_51443();
        int bossY = 24;
        int actionBottom = screenHeight - 64;
        List<WarningRect> occupied = new ArrayList<>();

        for (WarningEntry warning : visibleWarnings) {
            WarningModule module = warning.module();
            if (!module.getScreenWarning().get()) continue;

            int renderedWidth = getEditorRenderedWidth(module);
            int renderedHeight = getEditorRenderedHeight(module);
            int desiredX;
            int desiredY;

            switch (module.getPlacement().get()) {
                case BOSS_BAR -> {
                    desiredX = (screenWidth - renderedWidth) / 2;
                    desiredY = bossY;
                    bossY += renderedHeight + spacing;
                }
                case ACTION_BAR -> {
                    desiredX = (screenWidth - renderedWidth) / 2;
                    desiredY = actionBottom - renderedHeight;
                    actionBottom = desiredY - spacing;
                }
                case CUSTOM -> {
                    desiredX = getEditorPixelX(module, screenWidth);
                    desiredY = getEditorPixelY(module, screenHeight);
                }
                default -> {
                    desiredX = (screenWidth - renderedWidth) / 2;
                    desiredY = bossY;
                }
            }

            int x = Math.max(
                    0,
                    Math.min(Math.max(0, screenWidth - renderedWidth), desiredX)
            );
            Integer y = findNonOverlappingWarningY(
                    x,
                    desiredY,
                    renderedWidth,
                    renderedHeight,
                    screenHeight,
                    spacing,
                    occupied
            );

            
            
            
            if (y == null) {
                continue;
            }

            renderWarningAt(context, client, module, warning.text(), x, y);
            occupied.add(new WarningRect(x, y, renderedWidth, renderedHeight));
        }

        clearInactiveWarningStates(warnings);
    }

    private static Integer findNonOverlappingWarningY(
            int x,
            int desiredY,
            int width,
            int height,
            int screenHeight,
            int spacing,
            List<WarningRect> occupied
    ) {
        int maxY = Math.max(0, screenHeight - height);
        int startY = Math.max(0, Math.min(maxY, desiredY));

        for (int distance = 0; distance <= maxY; distance++) {
            int down = startY + distance;
            if (down <= maxY
                    && warningSlotFree(x, down, width, height, spacing, occupied)) {
                return down;
            }

            if (distance == 0) {
                continue;
            }

            int up = startY - distance;
            if (up >= 0
                    && warningSlotFree(x, up, width, height, spacing, occupied)) {
                return up;
            }
        }

        return null;
    }

    private static boolean warningSlotFree(
            int x,
            int y,
            int width,
            int height,
            int spacing,
            List<WarningRect> occupied
    ) {
        int right = x + width;
        int bottom = y + height;

        for (WarningRect rect : occupied) {
            boolean horizontalOverlap =
                    x < rect.x() + rect.width() + spacing
                            && right + spacing > rect.x();
            boolean verticalOverlap =
                    y < rect.y() + rect.height() + spacing
                            && bottom + spacing > rect.y();

            if (horizontalOverlap && verticalOverlap) {
                return false;
            }
        }

        return true;
    }

    private static int warningPriority(WarningModule module) {
        if (module instanceof LowHealthWarningModule) return 0;
        if (module instanceof ArmorBreakWarningModule) return 1;
        if (module instanceof LowTotemWarningModule) return 2;
        if (module instanceof EffectExpiryWarningModule) return 3;
        if (module instanceof FoodWarningModule) return 4;
        return 10;
    }

    private static void renderWarningEditorPreviews(
            class_332 context,
            class_310 client
    ) {
        for (
                Module module
                : YogiEssentialsClient
                .getModuleManager()
                .getModules()
        ) {
            if (
                    !(module instanceof WarningModule warning)
                            ||
                    !isCustomWarningEditorElement(
                            warning
                    )
            ) {
                continue;
            }

            renderWarningAt(
                    context,
                    client,
                    warning,
                    sampleWarningText(
                            warning
                    ),
                    getEditorPixelX(
                            warning,
                            context
                                    .method_51421()
                    ),
                    getEditorPixelY(
                            warning,
                            context
                                    .method_51443()
                    )
            );
        }
    }

    public static String sampleWarningText(
            WarningModule module
    ) {
        if (
                module
                        instanceof ArmorBreakWarningModule
        ) {
            return "CHESTPLATE LOW — 12%";
        }

        if (
                module
                        instanceof LowTotemWarningModule
        ) {
            return "LOW TOTEMS — 2";
        }

        if (
                module
                        instanceof LowHealthWarningModule
        ) {
            return "LOW HEALTH — 5.0 ❤";
        }

        if (
                module
                        instanceof EffectExpiryWarningModule
        ) {
            return "STRENGTH EXPIRES IN 6s";
        }

        if (
                module
                        instanceof FoodWarningModule
        ) {
            return "FOOD 6  •  SAT 1.5";
        }

        return module.getName();
    }

    private static void collectArmorWarning(
            List<WarningEntry> warnings,
            class_310 client
    ) {
        ArmorBreakWarningModule module =
                get(
                        ArmorBreakWarningModule.class
                );

        if (!enabled(module)) {
            return;
        }

        int threshold =
                module
                        .getThreshold()
                        .get()
                        .intValue();

        class_1799[] stacks = {
                client.field_1724
                        .method_6118(
                                class_1304.field_6169
                        ),
                client.field_1724
                        .method_6118(
                                class_1304.field_6174
                        ),
                client.field_1724
                        .method_6118(
                                class_1304.field_6172
                        ),
                client.field_1724
                        .method_6118(
                                class_1304.field_6166
                        )
        };

        String[] names = {
                "HELMET",
                "CHESTPLATE",
                "LEGGINGS",
                "BOOTS"
        };

        int lowest =
                101;

        String lowestName =
                "";

        for (
                int i = 0;
                i < stacks.length;
                i++
        ) {
            class_1799 stack =
                    stacks[i];

            if (
                    stack.method_7960()
                            ||
                    !stack.method_7963()
            ) {
                continue;
            }

            int percent =
                    durabilityPercent(
                            stack
                    );

            if (percent < lowest) {
                lowest = percent;
                lowestName =
                        names[i];
            }
        }

        if (
                lowest <= threshold
                        &&
                lowest < 101
        ) {
            warnings.add(
                    new WarningEntry(
                            module,
                            lowestName
                                    + " LOW — "
                                    + lowest
                                    + "%"
                    )
            );
        }
    }

    private static void collectTotemWarning(
            List<WarningEntry> warnings,
            class_310 client
    ) {
        LowTotemWarningModule module =
                get(
                        LowTotemWarningModule.class
                );

        if (!enabled(module)) {
            return;
        }

        int count =
                countTotems(
                        client
                );

        if (
                count
                        <= module
                        .getThreshold()
                        .get()
                        .intValue()
        ) {
            warnings.add(
                    new WarningEntry(
                            module,
                            "LOW TOTEMS — "
                                    + count
                    )
            );
        }
    }

    private static void collectHealthWarning(
            List<WarningEntry> warnings,
            class_310 client
    ) {
        LowHealthWarningModule module =
                get(
                        LowHealthWarningModule.class
                );

        if (!enabled(module)) {
            return;
        }

        float hearts =
                client.field_1724
                        .method_6032()
                        / 2.0F;

        if (
                hearts
                        <= module
                        .getThreshold()
                        .get()
                        .floatValue()
        ) {
            warnings.add(
                    new WarningEntry(
                            module,
                            "LOW HEALTH — "
                                    + String.format(
                                    "%.1f",
                                    hearts
                            )
                                    + " ❤"
                    )
            );
        }
    }

    private static void collectEffectWarning(
            List<WarningEntry> warnings,
            class_310 client
    ) {
        EffectExpiryWarningModule module =
                get(
                        EffectExpiryWarningModule.class
                );

        if (!enabled(module)) {
            return;
        }

        int thresholdTicks =
                module
                        .getSeconds()
                        .get()
                        .intValue()
                        * 20;

        class_1293 closest =
                null;

        for (
                class_1293 effect
                : client.field_1724
                .method_6026()
        ) {
            int duration =
                    effect.method_5584();

            if (
                    duration <= 0
                            ||
                    duration > thresholdTicks
            ) {
                continue;
            }

            if (
                    closest == null
                            ||
                    duration
                            < closest
                            .method_5584()
            ) {
                closest = effect;
            }
        }

        if (closest != null) {
            String name =
                    class_2561.method_43471(
                            closest
                                    .method_5586()
                    )
                            .getString();

            warnings.add(
                    new WarningEntry(
                            module,
                            name
                                    + " EXPIRES IN "
                                    + Math.max(
                                    1,
                                    closest
                                            .method_5584()
                                            / 20
                            )
                                    + "s"
                    )
            );
        }
    }

    private static void collectFoodWarning(
            List<WarningEntry> warnings,
            class_310 client
    ) {
        FoodWarningModule module =
                get(
                        FoodWarningModule.class
                );

        if (!enabled(module)) {
            return;
        }

        int food =
                client.field_1724
                        .method_7344()
                        .method_7586();

        float saturation =
                client.field_1724
                        .method_7344()
                        .method_7589();

        boolean foodLow =
                food
                        <= module
                        .getFoodThreshold()
                        .get()
                        .intValue();

        boolean saturationLow =
                saturation
                        <= module
                        .getSaturationThreshold()
                        .get()
                        .floatValue();

        boolean trigger =
                switch (
                        module
                                .getMode()
                                .get()
                ) {
                    case FOOD ->
                            foodLow;

                    case SATURATION ->
                            saturationLow;

                    case EITHER ->
                            foodLow
                                    ||
                                    saturationLow;
                };

        if (trigger) {
            warnings.add(
                    new WarningEntry(
                            module,
                            "FOOD "
                                    + food
                                    + "  •  SAT "
                                    + String.format(
                                    "%.1f",
                                    saturation
                            )
                    )
            );
        }
    }

    private static void renderWarningAt(
            class_332 context,
            class_310 client,
            WarningModule module,
            String text,
            int x,
            int y
    ) {
        float scale =
                module
                        .getScale()
                        .get()
                        .floatValue();

        Matrix3x2fStack matrices =
                context.method_51448();

        matrices.pushMatrix();

        matrices.translate(
                x,
                y
        );

        matrices.scale(
                scale,
                scale
        );

        if (
                module
                        .getBackground()
                        .get()
        ) {
            context.method_25294(
                    0,
                    0,
                    WARNING_WIDTH,
                    WARNING_HEIGHT,
                    withOpacity(
                            module
                                    .getBackgroundColor()
                                    .getArgb(),
                            module
                                    .getBackgroundOpacity()
                                    .get()
                                    .floatValue()
                    )
            );
        }

        context.method_25294(
                0,
                0,
                3,
                WARNING_HEIGHT,
                module
                        .getAccentColor()
                        .getArgb()
        );

        int maxTextWidth =
                WARNING_WIDTH - 20;

        String visibleText =
                client
                        .field_1772
                        .method_27523(
                                text,
                                maxTextWidth
                        );

        int textWidth =
                client
                        .field_1772
                        .method_1727(
                                visibleText
                        );

        int textX =
                Math.max(
                        10,
                        (
                                WARNING_WIDTH
                                        - textWidth
                        ) / 2
                );

        if (module.getTextShadow().get()) {
            context.method_51433(client.field_1772, visibleText, textX + 1, 9, 0xB0000000, false);
        }
        context.method_51433(client.field_1772, visibleText, textX, 8,
                module.getTextColor().getArgb(), false);

        matrices.popMatrix();
    }

    
    
    

    private static void renderTextCard(
            class_332 context,
            class_310 client,
            StyledHudModule module,
            String label,
            String value
    ) {
        renderTextCard(context, client, module, label, value, true);
    }

    private static void renderTextCard(
            class_332 context,
            class_310 client,
            StyledHudModule module,
            String label,
            String value,
            boolean renderBackground
    ) {
        withHudTransform(
                context,
                module,
                () -> {
                    int width = getBaseWidth(module);
                    int height = getBaseHeight(module);
                    drawCard(context, module, 0, 0, width, height, renderBackground);

                    int padding = module.getPadding().get().intValue();
                    boolean compact = module.getCompactMode().get();
                    boolean showLabel = module.getShowLabel().get();
                    boolean showValue = module.getShowValue().get();
                    String renderedLabel = hudLabel(module, label);

                    
                    
                    
                    String visibleValue = value == null ? "" : value;
                    int valueWidth = client.field_1772.method_1727(visibleValue);
                    int labelX = padding + module.getLabelOffsetX().get().intValue();
                    int labelY = padding + 2 + module.getLabelOffsetY().get().intValue();
                    int valueX = (compact ? padding : width - padding - valueWidth)
                            + module.getValueOffsetX().get().intValue();
                    int valueY = padding + 2 + module.getValueOffsetY().get().intValue();

                    
                    
                    
                    
                    valueX = resolveValueX(
                            client, renderedLabel, labelX, labelY, valueX, valueY,
                            valueWidth, showLabel && showValue
                    );

                    if (showLabel && !renderedLabel.isEmpty()) {
                        drawText(
                                context,
                                client,
                                module,
                                renderedLabel,
                                labelX,
                                labelY,
                                module.getLabelColor().getArgb()
                        );
                    }

                    if (showValue && !visibleValue.isEmpty()) {
                        drawText(
                                context,
                                client,
                                module,
                                visibleValue,
                                valueX,
                                valueY,
                                module.getValueColor().getArgb()
                        );
                    }
                }
        );
    }

    private static String trimTextToWidth(class_310 client, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (client.field_1772.method_1727(text) <= maxWidth) return text;

        String ellipsis = "…";
        int ellipsisWidth = client.field_1772.method_1727(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return client.field_1772.method_27523(text, maxWidth);
        }

        String body = client.field_1772.method_27523(text, maxWidth - ellipsisWidth);
        return body.isEmpty() ? ellipsis : body + ellipsis;
    }

    private static int getStyledContentAnchorX(
            StyledHudModule module,
            int screenWidth
    ) {
        if (!module.hasCustomEditorSize()) {
            return HudLayout.pixelX(
                    module,
                    screenWidth,
                    getRenderedWidth(module)
            );
        }

        int boxX = HudLayout.pixelX(
                module,
                screenWidth,
                getEditorRenderedWidth(module)
        );
        float scale = Math.max(0.001F, module.getScale().get().floatValue());
        return boxX - Math.round(editorLocalBounds(module).minX() * scale);
    }

    private static int getStyledContentAnchorY(
            StyledHudModule module,
            int screenHeight
    ) {
        if (!module.hasCustomEditorSize()) {
            return HudLayout.pixelY(
                    module,
                    screenHeight,
                    getRenderedHeight(module)
            );
        }

        int boxY = HudLayout.pixelY(
                module,
                screenHeight,
                getEditorRenderedHeight(module)
        );
        float scale = Math.max(0.001F, module.getScale().get().floatValue());
        return boxY - Math.round(editorLocalBounds(module).minY() * scale);
    }

    private static void withHudTransform(
            class_332 context,
            StyledHudModule module,
            Runnable renderer
    ) {
        int x = getStyledContentAnchorX(
                module,
                context.method_51421()
        );

        int y = getStyledContentAnchorY(
                module,
                context.method_51443()
        );

        float scale =
                module
                        .getScale()
                        .get()
                        .floatValue();

        Matrix3x2fStack matrices =
                context.method_51448();

        matrices.pushMatrix();

        matrices.translate(
                x,
                y
        );

        matrices.scale(
                scale,
                scale
        );

        renderer.run();

        matrices.popMatrix();
    }

    private static void drawCard(
            class_332 context,
            StyledHudModule module,
            int x,
            int y,
            int width,
            int height
    ) {
        drawCard(context, module, x, y, width, height, true);
    }

    private static void drawCard(
            class_332 context,
            StyledHudModule module,
            int x,
            int y,
            int width,
            int height,
            boolean renderBackground
    ) {
        if (x == 0 && y == 0 && module.hasCustomEditorSize()) {
            x = module.getEditorOffsetX().get().intValue();
            y = module.getEditorOffsetY().get().intValue();
            width = getContainerBaseWidth(module);
            height = getContainerBaseHeight(module);
        }
        if (
                renderBackground
                        && module
                        .getBackground()
                        .get()
        ) {
            context.method_25294(
                    x,
                    y,
                    x + width,
                    y + height,
                    withOpacity(
                            module
                                    .getBackgroundColor()
                                    .getArgb(),
                            module
                                    .getBackgroundOpacity()
                                    .get()
                                    .floatValue()
                    )
            );
        }

        if (
                module
                        .getBorder()
                        .get()
        ) {
            int color =
                    module
                            .getBorderColor()
                            .getArgb();

            context.method_25294(
                    x,
                    y,
                    x + width,
                    y + 1,
                    color
            );

            context.method_25294(
                    x,
                    y + height - 1,
                    x + width,
                    y + height,
                    color
            );

            context.method_25294(
                    x,
                    y,
                    x + 1,
                    y + height,
                    color
            );

            context.method_25294(
                    x + width - 1,
                    y,
                    x + width,
                    y + height,
                    color
            );
        }

        if (
                module
                        .getAccentLine()
                        .get()
        ) {
            context.method_25294(
                    x,
                    y,
                    x + 2,
                    y + height,
                    module
                            .getAccentColor()
                            .getArgb()
            );
        }
    }

    private static void drawText(
            class_332 context,
            class_310 client,
            StyledHudModule module,
            String value,
            int x,
            int y,
            int color
    ) {
        boolean shadow = module.getTextShadow().get();
        if (shadow) {
            context.method_51433(client.field_1772, value, x + 1, y + 1, 0xB0000000, false);
        }
        context.method_51433(client.field_1772, value, x, y, color, false);
    }

    private static String hudLabel(
            StyledHudModule module,
            String label
    ) {
        if (
                module
                        .getShowColon()
                        .get()
                &&
                !label.endsWith(
                        ":"
                )
        ) {
            return label + ":";
        }

        return label;
    }

    private static void renderArmorRow(
            class_332 context,
            class_310 client,
            ArmorHudModule module,
            int width,
            int y,
            String label,
            class_1799 stack
    ) {
        if (stack.method_7960()) return;
        int padding =
                module
                        .getPadding()
                        .get()
                        .intValue();

        int labelX =
                padding
                        + module
                        .getLabelOffsetX()
                        .get()
                        .intValue();

        int labelOffsetY =
                module
                        .getLabelOffsetY()
                        .get()
                        .intValue();

        int valueOffsetX =
                module
                        .getValueOffsetX()
                        .get()
                        .intValue();

        int valueOffsetY =
                module
                        .getValueOffsetY()
                        .get()
                        .intValue();

        String renderedLabel =
                hudLabel(
                        module,
                        label
                );

        String durability = stack.method_7963()
                ? durabilityPercent(stack) + "%"
                : "";

        int valueWidth =
                client
                        .field_1772
                        .method_1727(
                                durability
                        );

        int color =
                module
                        .getValueColor()
                        .getArgb();

        int valueX =
                width
                        - padding
                        - valueWidth
                        + valueOffsetX;

        valueX = resolveValueX(
                client, renderedLabel, labelX, y + labelOffsetY,
                valueX, y + valueOffsetY, valueWidth,
                module.getShowLabel().get() && module.getShowValue().get()
        );

        if (module.getShowLabel().get() && !renderedLabel.isEmpty()) {
            drawText(
                    context,
                    client,
                    module,
                    renderedLabel,
                    labelX,
                    y + labelOffsetY,
                    module.getLabelColor().getArgb()
            );
        }

        if (
                module
                        .getShowValue()
                        .get()
                        && !durability.isEmpty()
        ) {
            drawText(
                    context,
                    client,
                    module,
                    durability,
                    valueX,
                    y + valueOffsetY,
                    color
            );
        }
    }

    




    private static int resolveValueX(
            class_310 client,
            String label,
            int labelX,
            int labelY,
            int valueX,
            int valueY,
            int valueWidth,
            boolean bothVisible
    ) {
        if (!bothVisible || client == null || client.field_1772 == null
                || label == null || label.isEmpty()) {
            return valueX;
        }

        int fontHeight = client.field_1772.field_2000;
        boolean verticalOverlap = labelY < valueY + fontHeight
                && valueY < labelY + fontHeight;
        if (!verticalOverlap) {
            return valueX;
        }

        int labelRight = labelX + client.field_1772.method_1727(label);
        int valueRight = valueX + valueWidth;
        if (valueRight <= labelX - 3 || valueX >= labelRight + 3) {
            return valueX;
        }

        
        
        
        int labelCenter = (labelX + labelRight) / 2;
        int valueCenter = valueX + valueWidth / 2;
        return valueCenter < labelCenter
                ? labelX - 3 - valueWidth
                : labelRight + 3;
    }

    private static void drawStatusEffectIcon(
            class_332 context,
            class_1293 effect,
            int x,
            int y,
            int size
    ) {
        class_2960 texture =
                class_329.method_71644(
                        effect.method_5579()
                );

        context.method_52706(
                class_10799.field_56883,
                texture,
                x,
                y,
                size,
                size
        );
    }

    private static void drawScaledItem(
            class_332 context,
            class_1799 stack,
            int x,
            int y,
            int size
    ) {
        float scale =
                Math.max(
                        0.25F,
                        size / 16.0F
                );

        Matrix3x2fStack matrices =
                context.method_51448();

        matrices.pushMatrix();

        matrices.translate(
                x,
                y
        );

        matrices.scale(
                scale,
                scale
        );

        context.method_51427(
                stack,
                0,
                0
        );

        matrices.popMatrix();
    }

    
    
    

    private static void updateWarningSounds(
            List<WarningEntry> warnings,
            WarningQueueModule queue
    ) {
        List<WarningEntry> prioritized = new ArrayList<>(warnings);
        prioritized.sort(Comparator.comparingInt(entry -> warningPriority(entry.module())));

        Set<String> activeNames = prioritized.stream()
                .map(entry -> entry.module().getName())
                .collect(java.util.stream.Collectors.toSet());

        boolean queueEnabled = enabled(queue) && queue.getStaggerSounds().get();

        if (!queueEnabled) {
            WARNING_SOUND_QUEUE.clear();
            WARNING_SOUND_QUEUED.clear();
            warningQueueLastSoundMillis = 0L;
        }

        for (WarningEntry entry : prioritized) {
            WarningModule module = entry.module();
            String name = module.getName();
            boolean previous = WARNING_ACTIVE.getOrDefault(name, false);

            if (!previous && module.getSound().get()) {
                if (queueEnabled) {
                    if (WARNING_SOUND_QUEUED.add(name)) {
                        WARNING_SOUND_QUEUE.addLast(module);
                    }
                } else {
                    playWarningSound(module);
                }
            }

            WARNING_ACTIVE.put(name, true);
        }

        if (queueEnabled && !WARNING_SOUND_QUEUE.isEmpty()) {
            long now = System.currentTimeMillis();
            long gapMillis = Math.max(250L, Math.round(queue.getSoundGapSeconds().get() * 1000.0));

            if (warningQueueLastSoundMillis == 0L || now - warningQueueLastSoundMillis >= gapMillis) {
                while (!WARNING_SOUND_QUEUE.isEmpty()) {
                    WarningModule next = WARNING_SOUND_QUEUE.removeFirst();
                    WARNING_SOUND_QUEUED.remove(next.getName());

                    
                    
                    if (!activeNames.contains(next.getName()) || !next.isEnabled() || !next.getSound().get()) {
                        continue;
                    }

                    playWarningSound(next);
                    warningQueueLastSoundMillis = now;
                    break;
                }
            }
        }
    }

    private static void playWarningSound(WarningModule module) {
        WarningSoundManager.play(
                module.getWarningSound().get(),
                module.getSoundVolume().get()
        );
    }

    private static void clearInactiveWarningStates(
            List<WarningEntry> warnings
    ) {
        List<String> activeNames =
                warnings
                        .stream()
                        .map(
                                entry ->
                                        entry
                                                .module()
                                                .getName()
                        )
                        .toList();

        for (
                String key
                : new ArrayList<>(
                        WARNING_ACTIVE.keySet()
                )
        ) {
            if (
                    !activeNames.contains(
                            key
                    )
            ) {
                WARNING_ACTIVE.put(
                        key,
                        false
                );
            }
        }
    }

    
    
    

    private static class_1799 findShield(
            class_310 client
    ) {
        class_1799 offhand =
                client.field_1724
                        .method_6079();

        if (
                offhand.method_31574(
                        class_1802.field_8255
                )
        ) {
            return offhand;
        }

        class_1799 main =
                client.field_1724
                        .method_6047();

        if (
                main.method_31574(
                        class_1802.field_8255
                )
        ) {
            return main;
        }

        return class_1799.field_8037;
    }

    private static int getPing(
            class_310 client
    ) {
        PingHudModule ping = get(PingHudModule.class);
        return ping == null ? 0 : ping.resolvePing(client);
    }

    private static int countTotems(
            class_310 client
    ) {
        HudUpdateOptimizerModule optimizer = get(HudUpdateOptimizerModule.class);
        if (!enabled(optimizer)) {
            return countTotemsLive(client);
        }

        ensureHudDataWorld(client);
        long now = System.nanoTime();
        if (cachedTotemCountValid
                && now - cachedTotemCountNanos < optimizer.inventoryIntervalNanos()) {
            return cachedTotemCount;
        }

        cachedTotemCount = countTotemsLive(client);
        cachedTotemCountNanos = now;
        cachedTotemCountValid = true;
        return cachedTotemCount;
    }

    private static int countTotemsLive(
            class_310 client
    ) {
        int total = 0;

        for (int i = 0; i < client.field_1724.method_31548().method_5439(); i++) {
            class_1799 stack = client.field_1724.method_31548().method_5438(i);
            if (stack.method_31574(class_1802.field_8288)) {
                total += stack.method_7947();
            }
        }

        return total;
    }

    private static List<class_1293> getSortedPotionEffects(
            class_310 client
    ) {
        HudUpdateOptimizerModule optimizer = get(HudUpdateOptimizerModule.class);
        if (!enabled(optimizer)) {
            List<class_1293> effects = new ArrayList<>(client.field_1724.method_6026());
            effects.sort(Comparator.comparingInt(class_1293::method_5584));
            return effects;
        }

        ensureHudDataWorld(client);
        long now = System.nanoTime();
        if (cachedPotionEffectsValid
                && now - cachedPotionEffectsNanos < optimizer.effectsIntervalNanos()) {
            return cachedPotionEffects;
        }

        List<class_1293> effects = new ArrayList<>(client.field_1724.method_6026());
        effects.sort(Comparator.comparingInt(class_1293::method_5584));
        cachedPotionEffects = List.copyOf(effects);
        cachedPotionEffectsNanos = now;
        cachedPotionEffectsValid = true;
        return cachedPotionEffects;
    }

    private static void ensureHudDataWorld(class_310 client) {
        Object worldKey = client == null ? null : client.field_1687;
        if (hudDataWorldKey != worldKey) {
            clearHudDataCaches();
            hudDataWorldKey = worldKey;
        }
    }

    private static void clearHudDataCaches() {
        hudDataWorldKey = null;
        cachedTotemCount = 0;
        cachedTotemCountValid = false;
        cachedTotemCountNanos = 0L;
        cachedPotionEffects = List.of();
        cachedPotionEffectsValid = false;
        cachedPotionEffectsNanos = 0L;
    }

    private static int durabilityPercent(
            class_1799 stack
    ) {
        if (
                stack.method_7960()
                        ||
                !stack.method_7963()
                        ||
                stack.method_7936()
                        <= 0
        ) {
            return 0;
        }

        int remaining =
                stack.method_7936()
                        - stack.method_7919();

        return Math.max(
                0,
                Math.min(
                        100,
                        Math.round(
                                remaining
                                        * 100.0F
                                        / stack
                                        .method_7936()
                        )
                )
        );
    }


    private static int durabilityColor(
            int percent
    ) {
        if (percent >= 70) {
            return 0xFF74E08C;
        }

        if (percent >= 35) {
            return 0xFFFFC857;
        }

        return 0xFFFF6464;
    }

    private static String formatTicks(
            int ticks
    ) {
        if (
                ticks
                        == class_1293.field_42106
        ) {
            return "∞";
        }

        int seconds =
                Math.max(
                        0,
                        ticks / 20
                );

        int minutes =
                seconds / 60;

        int remaining =
                seconds % 60;

        return minutes
                + ":"
                + String.format(
                        "%02d",
                        remaining
                );
    }

    private static String roman(
            int value
    ) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default ->
                    Integer.toString(value);
        };
    }

    private static boolean enabled(
            Module module
    ) {
        return module != null
                &&
                module.isEnabled();
    }

    private static <
            T extends Module
            > T get(
            Class<T> clazz
    ) {
        return YogiEssentialsClient
                .getModuleManager()
                .getModule(clazz);
    }

    private static int withOpacity(
            int argb,
            float opacity
    ) {
        int alpha =
                Math.round(
                        255.0F
                                * Math.max(
                                0.0F,
                                Math.min(
                                        1.0F,
                                        opacity
                                )
                        )
                );

        return (
                alpha << 24
        )
                |
                (
                        argb
                                & 0x00FFFFFF
                );
    }

    private record WarningEntry(
            WarningModule module,
            String text
    ) {
    }

    private record WarningRect(
            int x,
            int y,
            int width,
            int height
    ) {
    }
}
