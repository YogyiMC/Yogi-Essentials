package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.chat.ChatPingsModule;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.ModuleRules;
import dev.yogi.yogiessentials.client.module.hud.ArmorHudModule;
import dev.yogi.yogiessentials.client.module.smp.WaypointModule;
import dev.yogi.yogiessentials.client.module.pvp.WarningModule;
import dev.yogi.yogiessentials.client.module.visual.ExtendedFovModule;
import dev.yogi.yogiessentials.client.render.ModulePreviewRenderer;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.HudPositionSetting;
import dev.yogi.yogiessentials.client.setting.KeybindSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.PercentSetting;
import dev.yogi.yogiessentials.client.setting.SectionSetting;
import dev.yogi.yogiessentials.client.setting.Setting;
import dev.yogi.yogiessentials.client.setting.StringSetting;
import dev.yogi.yogiessentials.client.util.ChatEnhancementManager;
import dev.yogi.yogiessentials.client.util.UiSoundManager;
import dev.yogi.yogiessentials.client.util.WarningSoundManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5481;

public class ModuleSettingsScreen extends class_437 {

    private static final int ORANGE =
            0xFFFF6A00;

    private static final int BACKGROUND =
            0xFF09090D;

    private static final int PANEL =
            0xFF101014;

    private static final int CARD =
            0xFF17171C;

    private static final int CARD_HOVER =
            0xFF202027;

    private static final int BORDER =
            0xFF303038;

    private static final int TEXT =
            0xFFF4F4F5;

    private static final int MUTED =
            0xFF9999A1;

    private static final int ROW_HEIGHT =
            42;

    private final class_437 parent;
    private final Module module;

    private NumberSetting draggingSlider;
    private int draggingSliderX;
    private int draggingSliderWidth;

    private KeybindSetting listeningKeybind;
    private StringSetting editingString;

    private int scrollOffset;

    public ModuleSettingsScreen(
            class_437 parent,
            Module module
    ) {
        super(
                class_2561.method_43470(
                        module.getName()
                )
        );

        this.parent = parent;
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    @Override
    public void method_25394(
            class_332 context,
            int mouseX,
            int mouseY,
            float deltaTicks
    ) {
        context.method_25294(
                0,
                0,
                field_22789,
                field_22790,
                BACKGROUND
        );

        int margin = screenMargin();
        int top = screenTop();
        int headerHeight = headerHeight();
        int headerWidth = Math.max(
                1,
                field_22789 - margin * 2
        );

        renderHeader(
                context,
                margin,
                top,
                headerWidth,
                headerHeight,
                mouseX,
                mouseY
        );

        int contentTop =
                top
                        + headerHeight
                        + 8;
        int contentBottom =
                Math.max(
                        contentTop + 1,
                        field_22790 - top
                );
        int availableHeight =
                Math.max(
                        1,
                        contentBottom - contentTop
                );
        int innerWidth =
                Math.max(
                        1,
                        field_22789 - margin * 2
                );

        boolean showPreview = shouldShowPreview(
                innerWidth,
                availableHeight
        );

        if (showPreview) {
            if (innerWidth < 680) {
                int gap = 8;
                int previewHeight = stackedPreviewHeight(
                        availableHeight
                );
                int settingsY =
                        contentTop
                                + previewHeight
                                + gap;
                int settingsHeight =
                        Math.max(
                                1,
                                contentBottom - settingsY
                        );

                ModulePreviewRenderer.render(
                        context,
                        module,
                        margin,
                        contentTop,
                        innerWidth,
                        previewHeight
                );
                renderSettingsPanel(
                        context,
                        margin,
                        settingsY,
                        innerWidth,
                        settingsHeight,
                        mouseX,
                        mouseY
                );
            } else {
                int previewWidth =
                        Math.max(
                                260,
                                Math.min(
                                        500,
                                        (int) Math.round(
                                                innerWidth * 0.43
                                        )
                                )
                        );
                int gap = 12;
                int settingsX =
                        margin
                                + previewWidth
                                + gap;
                int settingsWidth =
                        Math.max(
                                1,
                                field_22789 - margin - settingsX
                        );

                ModulePreviewRenderer.render(
                        context,
                        module,
                        margin,
                        contentTop,
                        previewWidth,
                        availableHeight
                );
                renderSettingsPanel(
                        context,
                        settingsX,
                        contentTop,
                        settingsWidth,
                        availableHeight,
                        mouseX,
                        mouseY
                );
            }
        } else {
            renderSettingsPanel(
                    context,
                    margin,
                    contentTop,
                    innerWidth,
                    availableHeight,
                    mouseX,
                    mouseY
            );
        }
        outline(context, 0, 0, field_22789, field_22790, ORANGE);
    }


    private void renderHeader(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY
    ) {
        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                PANEL
        );

        outline(
                context,
                x,
                y,
                width,
                height,
                ORANGE
        );

        context.method_25294(
                x,
                y,
                x + 3,
                y + height,
                ORANGE
        );

        boolean compactHeader = width < 540;
        int backWidth = compactHeader ? 54 : 66;
        int backY = y + (compactHeader ? 10 : 15);

        renderButton(
                context,
                x + 12,
                backY,
                backWidth,
                28,
                "Back",
                mouseX,
                mouseY
        );

        if (compactHeader) {
            int titleX = x + backWidth + 22;
            int titleRight = x + width - 10;

            context.method_25303(
                    field_22793,
                    field_22793.method_27523(
                            module.getName(),
                            Math.max(
                                    8,
                                    titleRight - titleX
                            )
                    ),
                    titleX,
                    y + 15,
                    TEXT
            );

            String description = field_22793.method_27523(
                    module.getDescription(),
                    Math.max(
                            8,
                            width - 92
                    )
            );

            context.method_51433(
                    field_22793,
                    description,
                    x + 12,
                    y + 43,
                    MUTED,
                    true
            );

            int toggleX = x + width - 50;
            renderToggle(
                    context,
                    toggleX,
                    y + height - 25,
                    module.isEnabled()
            );

            context.method_25303(
                    field_22793,
                    module.isEnabled()
                            ? "ON"
                            : "OFF",
                    Math.max(
                            x + 12,
                            toggleX - 24
                    ),
                    y + height - 20,
                    module.isEnabled()
                            ? 0xFF74E08C
                            : MUTED
            );

            return;
        }

        int titleX = x + 96;
        int toggleX = x + width - 116;
        int titleRight = toggleX - 78;

        context.method_25303(
                field_22793,
                field_22793.method_27523(
                        module.getName(),
                        Math.max(
                                20,
                                titleRight - titleX
                        )
                ),
                titleX,
                y + 14,
                TEXT
        );

        List<class_5481> lines =
                field_22793.method_1728(
                        class_2561.method_43470(
                                module.getDescription()
                        ),
                        Math.max(
                                100,
                                width - 330
                        )
                );

        if (!lines.isEmpty()) {
            context.method_51430(
                    field_22793,
                    lines.get(0),
                    titleX,
                    y + 32,
                    MUTED,
                    true
            );
        }

        context.method_25303(
                field_22793,
                module.isEnabled()
                        ? "ENABLED"
                        : "DISABLED",
                toggleX - 72,
                y + 24,
                module.isEnabled()
                        ? 0xFF74E08C
                        : MUTED
        );

        renderToggle(
                context,
                toggleX,
                y + 19,
                module.isEnabled()
        );
    }

    private void renderSettingsPanel(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY
    ) {
        width = Math.max(1, width);
        height = Math.max(1, height);

        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                PANEL
        );

        outline(
                context,
                x,
                y,
                width,
                height,
                ORANGE
        );

        if (height >= 20) {
            context.method_25303(
                    field_22793,
                    field_22793.method_27523(
                            "SETTINGS",
                            Math.max(8, width - 24)
                    ),
                    x + 12,
                    y + 9,
                    ORANGE
            );
        }

        List<Setting<?>> visible =
                visibleSettings();

        boolean hasColorSetting =
                visible.stream()
                        .anyMatch(
                                setting ->
                                        setting
                                                instanceof ColorSetting
                        );

        String helperText;

        if (module instanceof ArmorHudModule armor
                && armor.getIndividualArmorPositions().get()) {
            helperText =
                    "Individual items are positioned directly in the HUD Editor; drag each item separately.";

        } else if (
                module instanceof WarningModule warning
                        &&
                warning
                        .getPlacement()
                        .get()
                        == WarningModule.Placement.CUSTOM
        ) {
            helperText =
                    "Custom position is moved and resized directly in the HUD Editor.";

        } else if (visible.isEmpty()) {
            helperText =
                    "No additional settings for this toggle.";

        } else if (hasColorSetting) {
            helperText =
                    "Adjust settings below. Click a color swatch for the exact picker.";

        } else {
            helperText =
                    "Adjust this module's behavior below.";
        }

        if (height >= 58) {
            context.method_51433(
                    field_22793,
                    field_22793.method_27523(
                            helperText,
                            Math.max(8, width - 24)
                    ),
                    x + 12,
                    y + 27,
                    MUTED,
                    true
            );
        }

        int clipTop = settingsClipTop(
                y,
                height
        );
        int clipBottom = settingsClipBottom(
                y,
                height
        );

        if (clipBottom > clipTop) {
            context.method_44379(
                    x + 1,
                    clipTop,
                    x + width - 1,
                    clipBottom
            );

            int rowY =
                    clipTop
                            + 4
                            - scrollOffset;

            for (
                    Setting<?> setting
                    : visible
            ) {
                if (
                        rowY + ROW_HEIGHT
                                >= clipTop
                                &&
                        rowY <= clipBottom
                ) {
                    renderSettingRow(
                            context,
                            setting,
                            x + 10,
                            rowY,
                            Math.max(
                                    1,
                                    width - 20
                            ),
                            mouseX,
                            mouseY
                    );
                }

                rowY +=
                        ROW_HEIGHT;
            }

            context.method_44380();
        }

        int manageY = settingsManageY(y, height);
        if (manageY >= 0) {
            renderButton(
                    context,
                    x + 10,
                    manageY,
                    Math.max(1, width - 20),
                    24,
                    "Manage Waypoints",
                    mouseX,
                    mouseY
            );
        }

        int resetY = settingsResetY(
                y,
                height
        );

        if (resetY >= 0) {
            renderButton(
                    context,
                    x + 10,
                    resetY,
                    Math.max(
                            1,
                            width - 20
                    ),
                    24,
                    "Reset to Default",
                    mouseX,
                    mouseY
            );
        }
    }

    private void renderSettingRow(
            class_332 context,
            Setting<?> setting,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY
    ) {
        if (setting instanceof SectionSetting) {
            context.method_25294(
                    x,
                    y,
                    x + width,
                    y + ROW_HEIGHT - 4,
                    PANEL
            );

            String label = field_22793.method_27523(
                    setting.getName().toUpperCase(),
                    Math.max(8, width - 22)
            );

            context.method_25300(
                    field_22793,
                    label,
                    x + width / 2,
                    y + 14,
                    ORANGE
            );
            return;
        }

        boolean hovered =
                inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        ROW_HEIGHT - 4
                );

        context.method_25294(
                x,
                y,
                x + width,
                y + ROW_HEIGHT - 4,
                hovered
                        ? CARD_HOVER
                        : CARD
        );

        outline(
                context,
                x,
                y,
                width,
                ROW_HEIGHT - 4,
                BORDER
        );

        int reservedRight = 18;

        if (setting instanceof NumberSetting) {
            reservedRight = Math.max(
                    72,
                    width / 3
            );
        } else if (setting instanceof BooleanSetting) {
            reservedRight = 62;
        } else if (setting instanceof KeybindSetting) {
            reservedRight = Math.min(
                    150,
                    Math.max(96, width / 2)
            );
        } else if (setting instanceof ColorSetting) {
            reservedRight = Math.min(
                    118,
                    Math.max(86, width / 2)
            );
        } else if (setting instanceof StringSetting) {
            reservedRight = Math.min(220, Math.max(120, width / 2));
        }

        String settingName = field_22793.method_27523(
                setting.getName(),
                Math.max(
                        8,
                        width - reservedRight - 18
                )
        );

        context.method_25303(
                field_22793,
                settingName,
                x + 11,
                y + 8,
                TEXT
        );

        if (
                setting
                        instanceof NumberSetting number
        ) {
            renderNumber(
                    context,
                    number,
                    x,
                    y,
                    width
            );

            return;
        }

        if (
                setting
                        instanceof BooleanSetting bool
        ) {
            renderToggle(
                    context,
                    x + width - 48,
                    y + 11,
                    bool.get()
            );

            return;
        }

        if (
                setting
                        instanceof KeybindSetting keybind
        ) {
            String value =
                    listeningKeybind == keybind
                            ? "Press a key..."
                            : keyLabel(keybind.get());

            int maxValueWidth =
                    Math.max(
                            32,
                            Math.min(
                                    138,
                                    width / 2
                            )
                    );

            value = field_22793.method_27523(
                    value,
                    maxValueWidth
            );

            int valueWidth =
                    field_22793.method_1727(value);

            context.method_25303(
                    field_22793,
                    value,
                    x + width - valueWidth - 12,
                    y + 15,
                    listeningKeybind == keybind
                            ? ORANGE
                            : MUTED
            );

            return;
        }

        if (
                setting
                        instanceof EnumSetting<?> enumSetting
        ) {
            String value =
                    prettyEnum(
                            enumSetting.get()
                    );

            int valueWidth =
                    field_22793
                            .method_1727(
                                    value
                            );

            context.method_25303(
                    field_22793,
                    value,
                    x + width - valueWidth - 12,
                    y + 22,
                    ORANGE
            );

            return;
        }

        if (setting instanceof StringSetting stringSetting) {
            String value = stringSetting.get();
            if (editingString == stringSetting) {
                value = value + ((System.currentTimeMillis() / 500L) % 2L == 0L ? "_" : "");
            }
            value = field_22793.method_27523(value.isEmpty() ? "Click to edit" : value, Math.max(40, width / 2 - 18));
            int valueWidth = field_22793.method_1727(value);
            context.method_25303(
                    field_22793,
                    value,
                    x + width - valueWidth - 12,
                    y + 15,
                    editingString == stringSetting ? ORANGE : MUTED
            );
            return;
        }

        if (
                setting
                        instanceof ColorSetting color
        ) {
            int swatchSize =
                    22;

            int swatchX =
                    x + width - swatchSize - 12;

            int swatchY =
                    y + 8;

            context.method_25294(
                    swatchX,
                    swatchY,
                    swatchX + swatchSize,
                    swatchY + swatchSize,
                    color.getArgb()
            );

            outline(
                    context,
                    swatchX,
                    swatchY,
                    swatchSize,
                    swatchSize,
                    0xFFFFFFFF
            );

            String hex =
                    color.getHexRgb();

            context.method_51433(
                    field_22793,
                    hex,
                    swatchX
                            - field_22793
                            .method_1727(hex)
                            - 10,
                    y + 15,
                    MUTED,
                    true
            );
        }
    }

    private void renderNumber(
            class_332 context,
            NumberSetting setting,
            int x,
            int y,
            int width
    ) {
        String value =
                formatNumber(
                        setting
                );

        int valueWidth =
                field_22793
                        .method_1727(
                                value
                        );

        context.method_51433(
                field_22793,
                value,
                x + width - valueWidth - 12,
                y + 8,
                MUTED,
                true
        );

        int sliderX =
                x + 11;

        int sliderWidth =
                width - 22;

        int sliderY =
                y + 27;

        context.method_25294(
                sliderX,
                sliderY,
                sliderX + sliderWidth,
                sliderY + 4,
                0xFF34343A
        );

        double normalized =
                (
                        setting.get()
                                - setting.getMin()
                )
                        /
                        Math.max(
                                0.00001,
                                setting.getMax()
                                        - setting.getMin()
                        );

        normalized =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                normalized
                        )
                );

        int filled =
                (int) Math.round(
                        sliderWidth
                                * normalized
                );

        context.method_25294(
                sliderX,
                sliderY,
                sliderX + filled,
                sliderY + 4,
                ORANGE
        );

        context.method_25294(
                sliderX + filled - 2,
                sliderY - 2,
                sliderX + filled + 3,
                sliderY + 6,
                0xFFF4F4F5
        );
    }

    @Override
    public boolean method_25402(
            class_11909 click,
            boolean doubled
    ) {
        if (click.method_74245() != 0) {
            return super.method_25402(
                    click,
                    doubled
            );
        }

        int margin = screenMargin();
        int top = screenTop();
        int headerHeight = headerHeight();
        int headerWidth = Math.max(
                1,
                field_22789 - margin * 2
        );
        boolean compactHeader = headerWidth < 540;
        int backWidth = compactHeader ? 54 : 66;
        int backY = top + (compactHeader ? 10 : 15);

        if (inside(
                click.comp_4798(),
                click.comp_4799(),
                margin + 12,
                backY,
                backWidth,
                28
        )) {
            goBack();
            return true;
        }

        int toggleX =
                margin
                        + headerWidth
                        - (compactHeader ? 50 : 116);
        int toggleY = compactHeader
                ? top + headerHeight - 25
                : top + 19;

        if (inside(
                click.comp_4798(),
                click.comp_4799(),
                toggleX,
                toggleY,
                38,
                20
        )) {
            if (!module.isEnabled() && !ModuleRules.activeConflicts(module).isEmpty()) {
                if (field_22787 != null) {
                    field_22787.method_1507(new ModuleConflictScreen(this, module));
                }
                return true;
            }
            if (!module.isEnabled() && !ModuleRules.canEnable(module)) {
                return true;
            }
            module.toggle();

            if (module.isEnabled()) {
                UiSoundManager.moduleEnabled();
            } else {
                UiSoundManager.moduleDisabled();
            }

            ConfigManager.save();
            return true;
        }

        SettingsArea area = settingsArea();
        int clipTop = settingsClipTop(
                area.y(),
                area.height()
        );
        int clipBottom = settingsClipBottom(
                area.y(),
                area.height()
        );

        if (click.comp_4799() >= clipTop
                && click.comp_4799() <= clipBottom) {
            int rowY =
                    clipTop
                            + 4
                            - scrollOffset;
            int rowX = area.x() + 10;
            int rowWidth = Math.max(
                    1,
                    area.width() - 20
            );

            for (Setting<?> setting : visibleSettings()) {
                if (inside(
                        click.comp_4798(),
                        click.comp_4799(),
                        rowX,
                        rowY,
                        rowWidth,
                        ROW_HEIGHT - 4
                )) {
                    if (setting instanceof NumberSetting number) {
                        int sliderX = rowX + 11;
                        int sliderWidth = Math.max(
                                1,
                                rowWidth - 22
                        );
                        int sliderY = rowY + 22;

                        if (!inside(
                                click.comp_4798(),
                                click.comp_4799(),
                                sliderX,
                                sliderY,
                                sliderWidth,
                                16
                        )) {
                            return false;
                        }

                        draggingSlider = number;
                        draggingSliderX = sliderX;
                        draggingSliderWidth = sliderWidth;

                        updateSlider(
                                number,
                                click.comp_4798(),
                                draggingSliderX,
                                draggingSliderWidth
                        );

                        UiSoundManager.slider();
                        ConfigManager.save();
                        return true;
                    }

                    if (setting instanceof BooleanSetting bool) {
                        bool.toggle();
                        if (module instanceof ArmorHudModule armor
                                && setting == armor.getIndividualArmorPositions()
                                && bool.get()) {
                            armor.resetIndividualEditorLayout();
                            armor.enforceIndividualModeStyle();
                        }
                        UiSoundManager.click();
                        ConfigManager.save();
                        return true;
                    }

                    if (setting instanceof KeybindSetting keybind) {
                        listeningKeybind = keybind;
                        UiSoundManager.click();
                        return true;
                    }

                    if (setting instanceof StringSetting stringSetting) {
                        editingString = stringSetting;
                        listeningKeybind = null;
                        UiSoundManager.click();
                        return true;
                    }

                    if (setting instanceof EnumSetting enumSetting) {
                        enumSetting.next();

                        if (
                                module instanceof WarningModule warning
                                        && setting == warning.getWarningSound()
                        ) {
                            WarningSoundManager.play(
                                    warning.getWarningSound().get(),
                                    warning.getSoundVolume().get()
                            );
                        } else if (
                                module instanceof ChatPingsModule chatPings
                                        && setting == chatPings.getSound()
                        ) {
                            ChatEnhancementManager.previewPingSound(chatPings);
                        } else {
                            UiSoundManager.click();
                        }

                        ConfigManager.save();
                        return true;
                    }

                    if (setting instanceof ColorSetting color) {
                        UiSoundManager.click();

                        if (field_22787 != null) {
                            field_22787.method_1507(
                                    new ColorPickerScreen(
                                            this,
                                            module,
                                            color
                                    )
                            );
                        }

                        return true;
                    }
                }

                rowY += ROW_HEIGHT;
            }
        }

        int manageY = settingsManageY(area.y(), area.height());
        if (manageY >= 0
                && inside(
                click.comp_4798(), click.comp_4799(),
                area.x() + 10, manageY,
                Math.max(1, area.width() - 20), 24
        )) {
            UiSoundManager.click();
            if (field_22787 != null) field_22787.method_1507(new WaypointScreen(this));
            return true;
        }

        int resetY = settingsResetY(
                area.y(),
                area.height()
        );

        if (
                resetY >= 0
                        && inside(
                        click.comp_4798(),
                        click.comp_4799(),
                        area.x() + 10,
                        resetY,
                        Math.max(1, area.width() - 20),
                        24
                )
        ) {
            module.resetToDefaults();
            UiSoundManager.click();
            ConfigManager.save();
            scrollOffset = 0;
            return true;
        }

        return super.method_25402(
                click,
                doubled
        );
    }

    @Override
    public boolean method_25400(class_11905 input) {
        if (editingString == null) {
            return super.method_25400(input);
        }
        if (!input.method_74227()) {
            return false;
        }
        String value = editingString.get();
        if (value.length() >= editingString.getMaxLength()) {
            return true;
        }
        editingString.set(value + input.method_74226());
        ConfigManager.save();
        return true;
    }

    @Override
    public boolean method_25404(
            class_11908 input
    ) {
        if (editingString != null) {
            int key = input.comp_4795();
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                editingString = null;
                ConfigManager.save();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String value = editingString.get();
                if (!value.isEmpty()) {
                    int cp = value.codePointBefore(value.length());
                    editingString.set(value.substring(0, value.length() - Character.charCount(cp)));
                    ConfigManager.save();
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_DELETE) {
                editingString.set("");
                ConfigManager.save();
                return true;
            }
        }

        if (listeningKeybind != null) {
            int key = input.comp_4795();

            if (key == GLFW.GLFW_KEY_ESCAPE) {
                listeningKeybind = null;
                UiSoundManager.click();
                return true;
            }

            if (
                    key == GLFW.GLFW_KEY_BACKSPACE
                            ||
                    key == GLFW.GLFW_KEY_DELETE
            ) {
                listeningKeybind.set(
                        GLFW.GLFW_KEY_UNKNOWN
                );
            } else {
                listeningKeybind.set(key);
            }

            listeningKeybind = null;
            UiSoundManager.click();
            ConfigManager.save();
            return true;
        }

        return super.method_25404(input);
    }

    @Override
    public boolean method_25403(
            class_11909 click,
            double offsetX,
            double offsetY
    ) {
        if (
                draggingSlider != null
                        &&
                click.method_74245()
                        == 0
        ) {
            updateSlider(
                    draggingSlider,
                    click.comp_4798(),
                    draggingSliderX,
                    draggingSliderWidth
            );

            return true;
        }

        return super.method_25403(
                click,
                offsetX,
                offsetY
        );
    }

    @Override
    public boolean method_25406(
            class_11909 click
    ) {
        if (draggingSlider != null) {
            if (
                    module instanceof WarningModule warning
                            && draggingSlider == warning.getSoundVolume()
                            && warning.getSound().get()
            ) {
                WarningSoundManager.play(
                        warning.getWarningSound().get(),
                        warning.getSoundVolume().get()
                );
            }

            ConfigManager.save();
        }

        draggingSlider = null;

        return super.method_25406(
                click
        );
    }

    @Override
    public boolean method_25401(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        int maxScroll =
                getMaxScroll();

        scrollOffset -=
                (int) Math.round(
                        verticalAmount * 42.0
                );

        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                maxScroll,
                                scrollOffset
                        )
                );

        return true;
    }

    private int getMaxScroll() {
        SettingsArea area = settingsArea();
        int clipTop = settingsClipTop(
                area.y(),
                area.height()
        );
        int clipBottom = settingsClipBottom(
                area.y(),
                area.height()
        );
        int visibleRowsHeight = Math.max(
                1,
                clipBottom - clipTop - 8
        );
        int total =
                visibleSettings().size()
                        * ROW_HEIGHT;

        return Math.max(
                0,
                total - visibleRowsHeight
        );
    }

    private int screenMargin() {
        return Math.max(
                6,
                Math.min(
                        18,
                        field_22789 / 50
                )
        );
    }

    private int screenTop() {
        return Math.max(
                6,
                Math.min(
                        18,
                        field_22790 / 24
                )
        );
    }

    private int headerHeight() {
        int usableWidth =
                Math.max(
                        1,
                        field_22789 - screenMargin() * 2
                );

        return usableWidth < 540
                ? 76
                : 58;
    }

    private boolean shouldShowPreview(
            int innerWidth,
            int availableHeight
    ) {
        if (module instanceof ExtendedFovModule) {
            return false;
        }

        
        
        return availableHeight >= 150;
    }

    private int stackedPreviewHeight(
            int availableHeight
    ) {
        return Math.max(
                54,
                Math.min(
                        150,
                        Math.max(
                                54,
                                (availableHeight - 8) / 3
                        )
                )
        );
    }

    private SettingsArea settingsArea() {
        int margin = screenMargin();
        int top = screenTop();
        int headerHeight = headerHeight();
        int contentTop =
                top
                        + headerHeight
                        + 8;
        int contentBottom =
                Math.max(
                        contentTop + 1,
                        field_22790 - top
                );
        int availableHeight =
                Math.max(
                        1,
                        contentBottom - contentTop
                );
        int innerWidth =
                Math.max(
                        1,
                        field_22789 - margin * 2
                );

        boolean showPreview = shouldShowPreview(
                innerWidth,
                availableHeight
        );

        if (showPreview && innerWidth < 680) {
            int previewHeight = stackedPreviewHeight(
                    availableHeight
            );
            int settingsY =
                    contentTop
                            + previewHeight
                            + 8;

            return new SettingsArea(
                    margin,
                    settingsY,
                    innerWidth,
                    Math.max(
                            1,
                            contentBottom - settingsY
                    )
            );
        }

        if (showPreview) {
            int previewWidth =
                    Math.max(
                            260,
                            Math.min(
                                    500,
                                    (int) Math.round(
                                            innerWidth * 0.43
                                    )
                            )
                    );
            int settingsX =
                    margin
                            + previewWidth
                            + 12;

            return new SettingsArea(
                    settingsX,
                    contentTop,
                    Math.max(
                            1,
                            field_22789 - margin - settingsX
                    ),
                    availableHeight
            );
        }

        return new SettingsArea(
                margin,
                contentTop,
                innerWidth,
                availableHeight
        );
    }

    private int settingsClipTop(
            int panelY,
            int panelHeight
    ) {
        int headerSpace;

        if (panelHeight >= 76) {
            headerSpace = 48;
        } else if (panelHeight >= 42) {
            headerSpace = 24;
        } else {
            headerSpace = 12;
        }

        return Math.min(
                panelY + panelHeight - 1,
                panelY + headerSpace
        );
    }

    private int settingsClipBottom(
            int panelY,
            int panelHeight
    ) {
        int top = settingsClipTop(
                panelY,
                panelHeight
        );
        int footerSpace;
        if (module instanceof WaypointModule && panelHeight >= 88) {
            footerSpace = 66;
        } else {
            footerSpace = panelHeight >= 58 ? 36 : 0;
        }

        return Math.max(
                top + 1,
                panelY
                        + panelHeight
                        - footerSpace
        );
    }

    private int settingsManageY(int panelY, int panelHeight) {
        if (!(module instanceof WaypointModule) || panelHeight < 88) return -1;
        return panelY + panelHeight - 60;
    }

    private int settingsResetY(
            int panelY,
            int panelHeight
    ) {
        if (panelHeight < 58) {
            return -1;
        }

        return panelY
                + panelHeight
                - 30;
    }

    private List<Setting<?>> visibleSettings() {
        List<Setting<?>> result =
                new ArrayList<>();

        for (
                Setting<?> setting
                : module.getSettings()
        ) {
            if (
                    setting
                            instanceof HudPositionSetting
            ) {
                continue;
            }

            if (
                    module.isSettingVisible(
                            setting
                    )
            ) {
                result.add(
                        setting
                );
            }
        }

        return result;
    }

    private void updateSlider(
            NumberSetting setting,
            double mouseX,
            int sliderX,
            int sliderWidth
    ) {
        double normalized =
                (
                        mouseX - sliderX
                )
                        /
                        Math.max(
                                1.0,
                                sliderWidth
                        );

        normalized =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                normalized
                        )
                );

        setting.set(
                setting.getMin()
                        +
                normalized
                        *
                (
                        setting.getMax()
                                - setting.getMin()
                )
        );
    }

    private String formatNumber(
            NumberSetting setting
    ) {
        if (setting instanceof PercentSetting) {
            return Math.round(
                    setting.get() * 100.0
            ) + "%";
        }

        String rawName = setting.getName();
        String name = rawName.toLowerCase();

        if (rawName.contains("%")) {
            return Math.round(setting.get()) + "%";
        }

        if (
                name.contains(
                        "opacity"
                )
                        ||
                name.contains(
                        "scale"
                )
        ) {
            return Math.round(
                    setting.get()
                            * 100.0
            )
                    + "%";
        }

        if (
                name.contains(
                        "rotation"
                )
        ) {
            return Math.round(
                    setting.get()
            )
                    + "°";
        }

        if (
                setting.getStep()
                        >= 1.0
        ) {
            return Integer.toString(
                    setting.get()
                            .intValue()
            );
        }

        return String.format(
                "%.2f",
                setting.get()
        );
    }

    private String keyLabel(
            int key
    ) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return "Unbound";
        }

        String named = switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_KP_ENTER -> "Numpad Enter";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps Lock";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            default -> null;
        };

        if (named != null) {
            return named;
        }

        String glfwName =
                GLFW.glfwGetKeyName(
                        key,
                        0
                );

        if (
                glfwName != null
                        &&
                !glfwName.isBlank()
        ) {
            return glfwName.toUpperCase();
        }

        if (
                key >= GLFW.GLFW_KEY_F1
                        &&
                key <= GLFW.GLFW_KEY_F25
        ) {
            return "F"
                    + (
                    key
                            - GLFW.GLFW_KEY_F1
                            + 1
            );
        }

        return "Key " + key;
    }

    private String prettyEnum(
            Object value
    ) {
        String raw =
                String.valueOf(
                        value
                )
                        .replace(
                                '_',
                                ' '
                        )
                        .toLowerCase();

        StringBuilder result =
                new StringBuilder();

        boolean uppercase =
                true;

        for (
                char c
                : raw.toCharArray()
        ) {
            result.append(
                    uppercase
                            ? Character
                            .toUpperCase(c)
                            : c
            );

            uppercase =
                    c == ' ';
        }

        return result.toString();
    }

    private void renderButton(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            String label,
            int mouseX,
            int mouseY
    ) {
        boolean hovered =
                inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        height
                );

        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                hovered
                        ? 0xFF382117
                        : CARD
        );

        outline(
                context,
                x,
                y,
                width,
                height,
                hovered
                        ? ORANGE
                        : BORDER
        );

        context.method_25300(
                field_22793,
                label,
                x + width / 2,
                y + 9,
                hovered
                        ? ORANGE
                        : TEXT
        );
    }

    private void renderToggle(
            class_332 context,
            int x,
            int y,
            boolean enabled
    ) {
        context.method_25294(
                x,
                y,
                x + 38,
                y + 20,
                enabled
                        ? ORANGE
                        : 0xFF45454B
        );

        int knobX =
                enabled
                        ? x + 21
                        : x + 3;

        context.method_25294(
                knobX,
                y + 3,
                knobX + 14,
                y + 17,
                0xFFF4F4F5
        );
    }

    private void outline(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
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

    private record SettingsArea(
            int x,
            int y,
            int width,
            int height
    ) {
    }

    private boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                &&
                mouseX <= x + width
                &&
                mouseY >= y
                &&
                mouseY <= y + height;
    }

    private void goBack() {
        ConfigManager.save();

        if (field_22787 != null) {
            field_22787.method_1507(
                    parent
            );
        }
    }

    @Override
    public void method_25419() {
        goBack();
    }

    @Override
    public boolean method_25421() {
        return false;
    }
}
