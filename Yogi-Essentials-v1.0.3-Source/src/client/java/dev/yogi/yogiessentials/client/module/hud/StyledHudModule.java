package dev.yogi.yogiessentials.client.module.hud;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

public abstract class StyledHudModule extends HudModule {

    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final ColorSetting backgroundColor = new ColorSetting("Background Color", 0xFF17171C);
    private final NumberSetting backgroundOpacity = new NumberSetting("Background Opacity", 0.80, 0.0, 1.0, 0.05);
    private final BooleanSetting border = new BooleanSetting("Border", true);
    private final ColorSetting borderColor = new ColorSetting("Border Color", 0xFF333339);
    private final BooleanSetting accentLine = new BooleanSetting("Accent Line", true);
    private final ColorSetting accentColor = new ColorSetting("Accent Color", 0xFFFF6A00);
    private final BooleanSetting textShadow = new BooleanSetting("Drop Shadow", true);
    private final ColorSetting labelColor = new ColorSetting("Label Color", 0xFFFF6A00);
    private final ColorSetting valueColor = new ColorSetting("Value Color", 0xFFF4F4F5);
    private final NumberSetting scale = new NumberSetting("HUD Scale", 1.0, 0.25, 4.00, 0.01);
    private final NumberSetting padding = new NumberSetting("Padding", 7.0, 2.0, 16.0, 1.0);
    private final BooleanSetting showLabel = new BooleanSetting("Show Label", true);
    private final BooleanSetting showColon = new BooleanSetting("Show Colon", true);
    private final BooleanSetting showValue = new BooleanSetting("Show Value", true);
    private final BooleanSetting compactMode = new BooleanSetting("Compact Mode", false);
    private final NumberSetting labelOffsetX = new NumberSetting("Label X", 0.0, -320.0, 320.0, 1.0);
    private final NumberSetting labelOffsetY = new NumberSetting("Label Y", 0.0, -240.0, 240.0, 1.0);
    private final NumberSetting valueOffsetX = new NumberSetting("Value X", 0.0, -320.0, 320.0, 1.0);
    private final NumberSetting valueOffsetY = new NumberSetting("Value Y", 0.0, -240.0, 240.0, 1.0);

    




    private final NumberSetting editorWidth = new NumberSetting("Editor Width", 0.0, 0.0, 800.0, 1.0);
    private final NumberSetting editorHeight = new NumberSetting("Editor Height", 0.0, 0.0, 800.0, 1.0);
    private final NumberSetting editorOffsetX = new NumberSetting("Editor X Offset", 0.0, -800.0, 800.0, 1.0);
    private final NumberSetting editorOffsetY = new NumberSetting("Editor Y Offset", 0.0, -800.0, 800.0, 1.0);

    protected StyledHudModule(String name, String description, double defaultX, double defaultY) {
        super(name, description, defaultX, defaultY);

        addSetting(background);
        addSetting(backgroundColor);
        addSetting(backgroundOpacity);
        addSetting(border);
        addSetting(borderColor);
        addSetting(accentLine);
        addSetting(accentColor);
        addSetting(textShadow);
        addSetting(labelColor);
        addSetting(valueColor);
        addSetting(scale);
        addSetting(padding);
        addSetting(showLabel);
        addSetting(showColon);
        addSetting(showValue);
        addSetting(compactMode);
        addSetting(labelOffsetX);
        addSetting(labelOffsetY);
        addSetting(valueOffsetX);
        addSetting(valueOffsetY);
        addSetting(editorWidth);
        addSetting(editorHeight);
        addSetting(editorOffsetX);
        addSetting(editorOffsetY);
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        if (setting == editorWidth || setting == editorHeight || setting == editorOffsetX || setting == editorOffsetY) {
            return false;
        }

        if (setting == backgroundColor || setting == backgroundOpacity) {
            return background.get();
        }

        if (setting == borderColor) {
            return border.get();
        }

        if (setting == accentColor) {
            return accentLine.get();
        }

        if (setting == labelColor || setting == labelOffsetX || setting == labelOffsetY) {
            return showLabel.get();
        }

        if (setting == showColon) {
            return showLabel.get();
        }

        if (setting == valueColor || setting == valueOffsetX || setting == valueOffsetY) {
            return showValue.get();
        }

        return true;
    }

    public BooleanSetting getBackground() { return background; }
    public ColorSetting getBackgroundColor() { return backgroundColor; }
    public NumberSetting getBackgroundOpacity() { return backgroundOpacity; }
    public BooleanSetting getBorder() { return border; }
    public ColorSetting getBorderColor() { return borderColor; }
    public BooleanSetting getAccentLine() { return accentLine; }
    public ColorSetting getAccentColor() { return accentColor; }
    public BooleanSetting getTextShadow() { return textShadow; }
    public ColorSetting getLabelColor() { return labelColor; }
    public ColorSetting getValueColor() { return valueColor; }
    public NumberSetting getScale() { return scale; }
    public NumberSetting getPadding() { return padding; }
    public BooleanSetting getShowLabel() { return showLabel; }
    public BooleanSetting getShowColon() { return showColon; }
    public BooleanSetting getShowValue() { return showValue; }
    public BooleanSetting getCompactMode() { return compactMode; }
    public NumberSetting getLabelOffsetX() { return labelOffsetX; }
    public NumberSetting getLabelOffsetY() { return labelOffsetY; }
    public NumberSetting getValueOffsetX() { return valueOffsetX; }
    public NumberSetting getValueOffsetY() { return valueOffsetY; }
    public NumberSetting getEditorWidth() { return editorWidth; }
    public NumberSetting getEditorHeight() { return editorHeight; }
    public NumberSetting getEditorOffsetX() { return editorOffsetX; }
    public NumberSetting getEditorOffsetY() { return editorOffsetY; }

    public boolean hasCustomEditorWidth() { return editorWidth.get() >= 1.0; }
    public boolean hasCustomEditorHeight() { return editorHeight.get() >= 1.0; }
    public boolean hasCustomEditorSize() { return hasCustomEditorWidth() || hasCustomEditorHeight(); }

    public void setEditorSize(double width, double height) {
        editorWidth.set(Math.max(0.0, width));
        editorHeight.set(Math.max(0.0, height));
    }

    public void setEditorBox(double offsetX, double offsetY, double width, double height) {
        editorOffsetX.set(offsetX);
        editorOffsetY.set(offsetY);
        editorWidth.set(Math.max(0.0, width));
        editorHeight.set(Math.max(0.0, height));
    }

    public void resetEditorSize() {
        editorWidth.set(0.0);
        editorHeight.set(0.0);
        editorOffsetX.set(0.0);
        editorOffsetY.set(0.0);
    }
}
