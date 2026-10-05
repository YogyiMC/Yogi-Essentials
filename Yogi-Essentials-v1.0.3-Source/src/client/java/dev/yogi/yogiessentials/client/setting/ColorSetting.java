package dev.yogi.yogiessentials.client.setting;

public class ColorSetting extends Setting<Integer> {

    public ColorSetting(
            String name,
            int defaultArgb
    ) {
        super(name, defaultArgb);
    }

    public int getArgb() {
        return get();
    }

    public int getRgb() {
        return get() & 0x00FFFFFF;
    }

    public void setRgb(
            int rgb
    ) {
        int alpha =
                (get() >>> 24) & 0xFF;

        if (alpha == 0) {
            alpha = 0xFF;
        }

        set(
                (alpha << 24)
                        |
                (rgb & 0x00FFFFFF)
        );
    }

    public void setArgb(
            int argb
    ) {
        set(argb);
    }

    public String getHexRgb() {
        return String.format(
                "#%06X",
                getRgb()
        );
    }
}
