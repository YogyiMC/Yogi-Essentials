package dev.yogi.yogiessentials.client.setting;






public class PercentSetting extends NumberSetting {

    public PercentSetting(
            String name,
            double defaultValue,
            double min,
            double max,
            double step
    ) {
        super(name, defaultValue, min, max, step);
    }
}
