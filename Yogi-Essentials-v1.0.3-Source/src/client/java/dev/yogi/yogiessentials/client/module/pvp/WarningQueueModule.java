package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.SectionSetting;

public final class WarningQueueModule extends Module {
    private final NumberSetting maxVisible = new NumberSetting("Visible Warning Cards", 1.0, 1.0, 5.0, 1.0);
    private final BooleanSetting rotate = new BooleanSetting("Rotate Hidden Cards", true);
    private final NumberSetting rotateSeconds = new NumberSetting("Rotate Every (Seconds)", 1.5, 0.25, 10.0, 0.25);
    private final NumberSetting spacing = new NumberSetting("Card Spacing (Pixels)", 5.0, 0.0, 30.0, 1.0);
    private final BooleanSetting staggerSounds = new BooleanSetting("Space Alert Sounds", true);
    private final NumberSetting soundGapSeconds = new NumberSetting("Sound Gap (Seconds)", 0.65, 0.1, 5.0, 0.05);

    public WarningQueueModule() {
        super(
                "Warning Queue",
                "Shows urgent alerts first, rotates extra cards, and spaces sounds. It does not create alerts.",
                Category.PVP
        );
        addSetting(new SectionSetting("Cards from enabled Armor, Totem, Health, Effect, and Food alerts"));
        addSetting(maxVisible);
        addSetting(rotate);
        addSetting(rotateSeconds);
        addSetting(spacing);
        addSetting(new SectionSetting("Sounds play when alerts first appear"));
        addSetting(staggerSounds);
        addSetting(soundGapSeconds);
        setEnabled(true);
    }

    public NumberSetting getMaxVisible() {
        return maxVisible;
    }

    public NumberSetting getRotateSeconds() {
        return rotateSeconds;
    }

    public BooleanSetting getRotate() { return rotate; }

    public BooleanSetting getStaggerSounds() { return staggerSounds; }

    public NumberSetting getSpacing() {
        return spacing;
    }

    public NumberSetting getSoundGapSeconds() {
        return soundGapSeconds;
    }
}
