package dev.yogi.yogiessentials.client.setting;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    @Override
    public void set(Double value) {
        
        
        
        double safeValue =
                value != null && Double.isFinite(value)
                        ? value
                        : getDefaultValue();

        double clamped = Math.max(min, Math.min(max, safeValue));

        if (step > 0.0) {
            clamped = Math.round(clamped / step) * step;
        }

        
        clamped = Math.max(min, Math.min(max, clamped));
        super.set(clamped);
    }
}
