package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

public class CrosshairModule extends Module {

    public enum Shape {
        DOT,
        CROSS,
        PLUS,
        T_SHAPE,
        X
    }

    private final EnumSetting<Shape> shape =
            new EnumSetting<>(
                    "Shape",
                    Shape.CROSS,
                    Shape.class
            );

    private final NumberSetting size =
            new NumberSetting(
                    "Size",
                    5.0,
                    1.0,
                    16.0,
                    1.0
            );

    private final NumberSetting thickness =
            new NumberSetting(
                    "Thickness",
                    1.0,
                    1.0,
                    5.0,
                    1.0
            );

    private final NumberSetting gap =
            new NumberSetting(
                    "Gap",
                    3.0,
                    0.0,
                    12.0,
                    1.0
            );

    private final BooleanSetting outline =
            new BooleanSetting(
                    "Outline",
                    true
            );

    private final NumberSetting outlineThickness =
            new NumberSetting(
                    "Outline Thickness",
                    1.0,
                    1.0,
                    3.0,
                    1.0
            );

    private final ColorSetting color =
            new ColorSetting(
                    "Crosshair Color",
                    0xFFFFFFFF
            );

    private final ColorSetting outlineColor =
            new ColorSetting(
                    "Outline Color",
                    0xFF000000
            );

    private final NumberSetting opacity =
            new NumberSetting(
                    "Opacity",
                    1.0,
                    0.10,
                    1.0,
                    0.05
            );


    private final BooleanSetting showInThirdPerson =
            new BooleanSetting(
                    "Show in Third Person",
                    false
            );

    private final BooleanSetting attackCooldownAnimation =
            new BooleanSetting(
                    "Attack Cooldown Animation",
                    false
            );

    private final NumberSetting cooldownExpansion =
            new NumberSetting(
                    "Cooldown Expansion",
                    3.0,
                    0.0,
                    10.0,
                    1.0
            );

    public CrosshairModule() {
        super(
                "Custom Crosshair",
                "A clean, pixel-perfect crosshair with live preview and exact color picking.",
                Category.PVP
        );

        addSetting(shape);
        addSetting(size);
        addSetting(thickness);
        addSetting(gap);
        addSetting(outline);
        addSetting(outlineThickness);
        addSetting(color);
        addSetting(outlineColor);
        addSetting(opacity);
        addSetting(showInThirdPerson);
        addSetting(attackCooldownAnimation);
        addSetting(cooldownExpansion);
    }

    @Override
    public boolean isSettingVisible(
            Setting<?> setting
    ) {
        Shape current =
                shape.get();

        if (
                setting == gap
        ) {
            return current == Shape.CROSS
                    ||
                    current == Shape.T_SHAPE
                    ||
                    current == Shape.X;
        }

        if (
                setting == thickness
        ) {
            return current != Shape.DOT;
        }

        if (
                setting == outlineThickness
                        ||
                setting == outlineColor
        ) {
            return outline.get();
        }

        if (
                setting == cooldownExpansion
        ) {
            return attackCooldownAnimation.get();
        }

        return true;
    }

    public EnumSetting<Shape> getShape() {
        return shape;
    }

    public NumberSetting getSize() {
        return size;
    }

    public NumberSetting getThickness() {
        return thickness;
    }

    public NumberSetting getGap() {
        return gap;
    }

    public BooleanSetting getOutline() {
        return outline;
    }

    public NumberSetting getOutlineThickness() {
        return outlineThickness;
    }

    public ColorSetting getColor() {
        return color;
    }

    public ColorSetting getOutlineColor() {
        return outlineColor;
    }

    public NumberSetting getOpacity() {
        return opacity;
    }


    public BooleanSetting getShowInThirdPerson() {
        return showInThirdPerson;
    }

    public BooleanSetting getAttackCooldownAnimation() {
        return attackCooldownAnimation;
    }

    public NumberSetting getCooldownExpansion() {
        return cooldownExpansion;
    }
}
