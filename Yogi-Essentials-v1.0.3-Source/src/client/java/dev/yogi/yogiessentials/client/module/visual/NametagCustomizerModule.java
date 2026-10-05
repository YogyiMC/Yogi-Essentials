package dev.yogi.yogiessentials.client.module.visual;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

/** Cosmetic-only client-style player nametags. */
public final class NametagCustomizerModule extends Module {
    public enum ApplyTo {
        EVERY_PLAYER,
        SELF_ONLY
    }

    private final BooleanSetting showNametag = new BooleanSetting("Show Nametag", true);
    private final BooleanSetting showServerNametags = new BooleanSetting("Show Server Nametags", false);
    private final EnumSetting<ApplyTo> applyTo = new EnumSetting<>("Apply Settings To", ApplyTo.EVERY_PLAYER, ApplyTo.class);
    private final BooleanSetting customTextColor = new BooleanSetting("Custom Text Color", false);
    private final ColorSetting textColor = new ColorSetting("Text Color", 0xFFFFFFFF);
    private final BooleanSetting bold = new BooleanSetting("Bold Text", false);
    private final BooleanSetting italic = new BooleanSetting("Italic Text", false);
    private final BooleanSetting underline = new BooleanSetting("Underline Text", false);
    private final NumberSetting scale = new NumberSetting("Nametag Scale", 1.0, 0.60, 1.75, 0.05);
    private final NumberSetting verticalOffset = new NumberSetting("Vertical Offset", 0.0, -12.0, 20.0, 1.0);
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final ColorSetting backgroundColor = new ColorSetting("Background Color", 0xFF000000);
    private final NumberSetting backgroundOpacity = new NumberSetting("Background Opacity", 25.0, 0.0, 100.0, 1.0);

    public NametagCustomizerModule() {
        super(
                "Nametag Customizer",
                "Renders client-side player nametags with your cosmetic style, even when a server hides its normal player nametag. Choose whether the style applies to every player or only yourself.",
                Category.VISUAL
        );
        addSetting(showNametag);
        addSetting(showServerNametags);
        addSetting(applyTo);
        addSetting(customTextColor);
        addSetting(textColor);
        addSetting(bold);
        addSetting(italic);
        addSetting(underline);
        addSetting(scale);
        addSetting(verticalOffset);
        addSetting(background);
        addSetting(backgroundColor);
        addSetting(backgroundOpacity);
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        if (setting != showNametag && setting != showServerNametags && setting != applyTo && !showNametag.get()) return false;
        if (setting == textColor) return customTextColor.get();
        if (setting == backgroundColor || setting == backgroundOpacity) return background.get();
        return true;
    }

    public BooleanSetting getShowNametag() { return showNametag; }
    public BooleanSetting getShowServerNametags() { return showServerNametags; }
    public EnumSetting<ApplyTo> getApplyTo() { return applyTo; }
    public BooleanSetting getCustomTextColor() { return customTextColor; }
    public ColorSetting getTextColor() { return textColor; }
    public BooleanSetting getBold() { return bold; }
    public BooleanSetting getItalic() { return italic; }
    public BooleanSetting getUnderline() { return underline; }
    public NumberSetting getScale() { return scale; }
    public NumberSetting getVerticalOffset() { return verticalOffset; }
    public BooleanSetting getBackground() { return background; }
    public ColorSetting getBackgroundColor() { return backgroundColor; }
    public NumberSetting getBackgroundOpacity() { return backgroundOpacity; }

    public boolean appliesToEntityId(int entityId, int localPlayerId) {
        return applyTo.get() == ApplyTo.EVERY_PLAYER || entityId == localPlayerId;
    }

    public int getBackgroundArgb() {
        if (!background.get()) return 0;
        int alpha = Math.max(0, Math.min(255, (int) Math.round(backgroundOpacity.get() * 2.55)));
        return (alpha << 24) | backgroundColor.getRgb();
    }
}
