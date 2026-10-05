package dev.yogi.yogiessentials.client.module.visual;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class SideShieldModule extends Module {

    private final NumberSetting horizontalOffset = new NumberSetting("X Adjustment", 0.15, -0.50, 0.50, 0.01);
    private final NumberSetting verticalOffset = new NumberSetting("Y Adjustment", 0.12, -0.50, 0.50, 0.01);
    private final NumberSetting rotation = new NumberSetting("Rotation", 27.0, -45.0, 45.0, 1.0);

    public SideShieldModule() {
        super("Side Shield", "Moves the idle shield toward the side of the screen.", Category.VISUAL);
        addSetting(horizontalOffset);
        addSetting(verticalOffset);
        addSetting(rotation);
    }

    @Override
    protected void onEnable() {
        if (YogiEssentialsClient.getModuleManager() == null) return;
        LowShieldModule low = YogiEssentialsClient.getModuleManager().getModule(LowShieldModule.class);
        if (low != null && low.isEnabled()) low.setEnabled(false);
    }

    public NumberSetting getHorizontalOffset() { return horizontalOffset; }
    public NumberSetting getVerticalOffset() { return verticalOffset; }
    public NumberSetting getRotation() { return rotation; }
}
