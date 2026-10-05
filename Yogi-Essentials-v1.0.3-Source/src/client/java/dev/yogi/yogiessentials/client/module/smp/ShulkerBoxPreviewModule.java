package dev.yogi.yogiessentials.client.module.smp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public final class ShulkerBoxPreviewModule extends Module {
    public enum ActivationMode {
        HOVER,
        SHIFT_HOVER
    }

    private final EnumSetting<ActivationMode> activationMode =
            new EnumSetting<>("Activation Mode", ActivationMode.HOVER, ActivationMode.class);
    private final NumberSetting previewScale =
            new NumberSetting("Preview Scale (%)", 100.0, 0.0, 100.0, 5.0);
    private final BooleanSetting showEmptySlots =
            new BooleanSetting("Show Empty Slots", true);

    public ShulkerBoxPreviewModule() {
        super("Shulker Box Previews",
                "Preview shulker contents in inventories. Choose normal hover or require Shift + hover.",
                Category.SMP);
        addSetting(activationMode);
        addSetting(previewScale);
        addSetting(showEmptySlots);
    }

    public EnumSetting<ActivationMode> getActivationMode() { return activationMode; }
    public NumberSetting getPreviewScale() { return previewScale; }
    public BooleanSetting getShowEmptySlots() { return showEmptySlots; }
}
