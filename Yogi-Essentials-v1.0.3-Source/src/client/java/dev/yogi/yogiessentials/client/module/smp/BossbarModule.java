package dev.yogi.yogiessentials.client.module.smp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;


public final class BossbarModule extends Module {
    private final BooleanSetting showBars = new BooleanSetting("Show Boss Bars", true);
    private final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.25, 2.5, 0.05);
    private final NumberSetting xOffset = new NumberSetting("X Offset", 0.0, -500.0, 500.0, 1.0);
    private final NumberSetting yOffset = new NumberSetting("Y Offset", 0.0, -200.0, 500.0, 1.0);

    public BossbarModule() {
        super("Bossbar", "Customize or hide the vanilla boss bar display on the client.", Category.SMP);
        addSetting(showBars);
        addSetting(scale);
        addSetting(xOffset);
        addSetting(yOffset);
    }

    public BooleanSetting getShowBars() { return showBars; }
    public NumberSetting getScale() { return scale; }
    public NumberSetting getXOffset() { return xOffset; }
    public NumberSetting getYOffset() { return yOffset; }
}
