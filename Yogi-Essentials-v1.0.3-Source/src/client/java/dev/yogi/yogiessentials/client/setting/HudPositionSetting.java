package dev.yogi.yogiessentials.client.setting;


public class HudPositionSetting extends NumberSetting {
    public HudPositionSetting(String name, double defaultValue) {
        super(name, defaultValue, 0.0, 1.0, 0.001);
    }
}
