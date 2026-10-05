package dev.yogi.yogiessentials.client.module.visual;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class SmallBlockModule extends Module {

    private final NumberSetting horizontalOffset =
            new NumberSetting("X Adjustment", 0.0, -0.40, 0.40, 0.01);

    private final NumberSetting verticalOffset =
            new NumberSetting("Y Adjustment", -0.13, -0.50, 0.50, 0.01);

    private final NumberSetting scale =
            new NumberSetting("Scale", 0.92, 0.40, 1.20, 0.01);

    public SmallBlockModule() {
        super(
                "Small Block",
                "Lowers and reduces the shield while blocking.",
                Category.VISUAL
        );

        addSetting(horizontalOffset);
        addSetting(verticalOffset);
        addSetting(scale);
    }

    public NumberSetting getHorizontalOffset() {
        return horizontalOffset;
    }

    public NumberSetting getVerticalOffset() {
        return verticalOffset;
    }

    public NumberSetting getScale() {
        return scale;
    }
}
