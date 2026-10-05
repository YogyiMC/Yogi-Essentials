package dev.yogi.yogiessentials.client.setting;

public class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String name, String defaultValue, int maxLength) {
        super(name, defaultValue == null ? "" : defaultValue);
        this.maxLength = Math.max(1, maxLength);
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public void set(String value) {
        String safe = value == null ? "" : value;
        if (safe.length() > maxLength) {
            safe = safe.substring(0, maxLength);
        }
        super.set(safe);
    }
}
