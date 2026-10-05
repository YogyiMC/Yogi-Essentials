package dev.yogi.yogiessentials.client.setting;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {

    private final E[] values;

    public EnumSetting(String name, E defaultValue, Class<E> enumClass) {
        super(name, defaultValue);
        this.values = enumClass.getEnumConstants();
    }

    public E[] getValues() {
        return values;
    }

    public void next() {
        int next = (get().ordinal() + 1) % values.length;
        set(values[next]);
    }

    public void previous() {
        int previous = (get().ordinal() - 1 + values.length) % values.length;
        set(values[previous]);
    }
}
