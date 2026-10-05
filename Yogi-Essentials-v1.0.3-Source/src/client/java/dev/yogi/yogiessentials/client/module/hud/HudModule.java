package dev.yogi.yogiessentials.client.module.hud;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.HudPositionSetting;

public abstract class HudModule extends Module {

    private final HudPositionSetting x;
    private final HudPositionSetting y;

    protected HudModule(
            String name,
            String description,
            double defaultX,
            double defaultY
    ) {

        super(
                name,
                description,
                Category.HUD
        );

        x =
                new HudPositionSetting(
                        "HUD X",
                        defaultX
                );

        y =
                new HudPositionSetting(
                        "HUD Y",
                        defaultY
                );

        



        addSetting(x);
        addSetting(y);
    }

    public HudPositionSetting getXSetting() {
        return x;
    }

    public HudPositionSetting getYSetting() {
        return y;
    }

    public double getHudX() {
        return x.get();
    }

    public double getHudY() {
        return y.get();
    }

    public void setHudPosition(
            double x,
            double y
    ) {

        this.x.set(x);
        this.y.set(y);
    }
}