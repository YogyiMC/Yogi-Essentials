package dev.yogi.yogiessentials.client.module.visual;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class LowShieldModule extends Module {

    private final NumberSetting height = new NumberSetting("Height", 0.35, 0.0, 1.0, 0.05);

    public LowShieldModule() {
        super("Low Shield", "Lowers the shield for better visibility.", Category.VISUAL);
        addSetting(height);
    }

    @Override
    protected void onEnable() {
        if (YogiEssentialsClient.getModuleManager() == null) return;
        SideShieldModule side = YogiEssentialsClient.getModuleManager().getModule(SideShieldModule.class);
        if (side != null && side.isEnabled()) side.setEnabled(false);
    }

    public NumberSetting getHeight() { return height; }
}
