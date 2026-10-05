package dev.yogi.yogiessentials.client.module.visual;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class SmallTotemModule extends Module {

    private final NumberSetting scale =
            new NumberSetting("Scale", 0.60, 0.20, 1.20, 0.01);

    private final NumberSetting horizontalOffset =
            new NumberSetting("X Adjustment", 0.0, -0.50, 0.50, 0.01);

    private final NumberSetting verticalOffset =
            new NumberSetting("Y Adjustment", 0.0, -0.50, 0.50, 0.01);

    private final NumberSetting depthOffset =
            new NumberSetting("Z Adjustment", 0.0, -0.50, 0.50, 0.01);

    private final NumberSetting rotation =
            new NumberSetting("Rotation", 0.0, -90.0, 90.0, 1.0);

    public SmallTotemModule() {
        super(
                "Small Totem",
                "Makes held totems smaller and fully positionable.",
                Category.VISUAL
        );

        addSetting(scale);
        addSetting(horizontalOffset);
        addSetting(verticalOffset);
        addSetting(depthOffset);
        addSetting(rotation);
    }

    public NumberSetting getScale() {
        return scale;
    }

    public NumberSetting getHorizontalOffset() {
        return horizontalOffset;
    }

    public NumberSetting getVerticalOffset() {
        return verticalOffset;
    }

    public NumberSetting getDepthOffset() {
        return depthOffset;
    }

    public NumberSetting getRotation() {
        return rotation;
    }
}
