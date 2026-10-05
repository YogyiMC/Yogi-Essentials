package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.HudPositionSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

public abstract class WarningModule extends Module {

    public enum Placement {
        ACTION_BAR,
        BOSS_BAR,
        CUSTOM
    }

    public enum WarningSound {
        BELL,
        CHIME,
        LEVEL_UP,
        EXPERIENCE,
        ANVIL,
        CLICK
    }

    private final BooleanSetting screenWarning =
            new BooleanSetting(
                    "Screen Warning",
                    true
            );

    private final EnumSetting<Placement> placement =
            new EnumSetting<>(
                    "Placement",
                    Placement.BOSS_BAR,
                    Placement.class
            );

    private final BooleanSetting sound =
            new BooleanSetting(
                    "Warning Sound",
                    true
            );

    private final EnumSetting<WarningSound> warningSound =
            new EnumSetting<>(
                    "Sound Type",
                    WarningSound.BELL,
                    WarningSound.class
            );

    private final NumberSetting soundVolume =
            new NumberSetting(
                    "Sound Volume",
                    1.50,
                    0.10,
                    2.00,
                    0.05
            );

    private final BooleanSetting background =
            new BooleanSetting(
                    "Background",
                    true
            );

    private final ColorSetting backgroundColor =
            new ColorSetting(
                    "Background Color",
                    0xFF131317
            );

    private final NumberSetting backgroundOpacity =
            new NumberSetting(
                    "Background Opacity",
                    0.82,
                    0.0,
                    1.0,
                    0.05
            );

    private final BooleanSetting textShadow =
            new BooleanSetting(
                    "Text Shadow",
                    true
            );

    private final ColorSetting textColor =
            new ColorSetting(
                    "Text Color",
                    0xFFFFB060
            );

    private final ColorSetting accentColor =
            new ColorSetting(
                    "Accent Color",
                    0xFFFF6A00
            );

    private final NumberSetting scale =
            new NumberSetting(
                    "Warning Scale",
                    1.0,
                    0.60,
                    1.60,
                    0.05
            );

    private final HudPositionSetting customX;
    private final HudPositionSetting customY;

    protected WarningModule(
            String name,
            String description,
            double defaultCustomY
    ) {
        super(
                name,
                description,
                Category.PVP
        );

        customX =
                new HudPositionSetting(
                        "Custom X",
                        0.50
                );

        customY =
                new HudPositionSetting(
                        "Custom Y",
                        defaultCustomY
                );

        addSetting(screenWarning);
        addSetting(placement);
        addSetting(sound);
        addSetting(warningSound);
        addSetting(soundVolume);
        addSetting(background);
        addSetting(backgroundColor);
        addSetting(backgroundOpacity);
        addSetting(textShadow);
        addSetting(textColor);
        addSetting(accentColor);
        addSetting(scale);
        addSetting(customX);
        addSetting(customY);
    }

    @Override
    public boolean isSettingVisible(
            Setting<?> setting
    ) {
        if (setting == screenWarning) {
            return true;
        }

        if (setting == sound) {
            return true;
        }

        if (
                setting == warningSound
                        || setting == soundVolume
        ) {
            return sound.get();
        }

        if (
                setting == placement
                        ||
                setting == background
                        ||
                setting == textShadow
                        ||
                setting == textColor
                        ||
                setting == accentColor
                        ||
                setting == scale
        ) {
            return screenWarning.get();
        }

        if (
                setting == backgroundColor
                        ||
                setting == backgroundOpacity
        ) {
            return screenWarning.get()
                    && background.get();
        }

        
        
        if (
                setting == customX
                        ||
                setting == customY
        ) {
            return false;
        }

        return true;
    }

    public BooleanSetting getScreenWarning() {
        return screenWarning;
    }

    public EnumSetting<Placement> getPlacement() {
        return placement;
    }

    public BooleanSetting getSound() {
        return sound;
    }

    public EnumSetting<WarningSound> getWarningSound() {
        return warningSound;
    }

    public NumberSetting getSoundVolume() {
        return soundVolume;
    }

    public BooleanSetting getBackground() {
        return background;
    }

    public ColorSetting getBackgroundColor() {
        return backgroundColor;
    }

    public NumberSetting getBackgroundOpacity() {
        return backgroundOpacity;
    }

    public BooleanSetting getTextShadow() {
        return textShadow;
    }

    public ColorSetting getTextColor() {
        return textColor;
    }

    public ColorSetting getAccentColor() {
        return accentColor;
    }

    public NumberSetting getScale() {
        return scale;
    }

    public double getCustomX() {
        return customX.get();
    }

    public double getCustomY() {
        return customY.get();
    }

    public void setCustomPosition(
            double x,
            double y
    ) {
        customX.set(x);
        customY.set(y);
    }
}
