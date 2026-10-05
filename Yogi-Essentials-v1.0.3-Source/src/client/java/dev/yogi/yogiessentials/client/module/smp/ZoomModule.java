package dev.yogi.yogiessentials.client.module.smp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.KeybindSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public final class ZoomModule extends Module {

    public enum ZoomType {
        HOLD,
        TOGGLE
    }

    private final EnumSetting<ZoomType> zoomType =
            new EnumSetting<>("Zoom Type", ZoomType.HOLD, ZoomType.class);

    private final KeybindSetting zoomKey =
            new KeybindSetting("Zoom Key", GLFW.GLFW_KEY_C);

    private final NumberSetting zoomFov =
            new NumberSetting("Zoom FOV", 30.0, 5.0, 70.0, 1.0);

    private final BooleanSetting scrollToZoom =
            new BooleanSetting("Scroll to Zoom", true);

    private final NumberSetting scrollStep =
            new NumberSetting("Scroll Zoom Step", 2.0, 0.5, 10.0, 0.5);

    private final NumberSetting minimumScrollFov =
            new NumberSetting("Minimum Scroll FOV", 1.0, 1.0, 30.0, 1.0);

    private final BooleanSetting smoothZoom =
            new BooleanSetting("Smooth Zoom", true);

    private final NumberSetting smoothSpeed =
            new NumberSetting("Smooth Speed", 0.35, 0.05, 1.0, 0.05);

    private final BooleanSetting reduceSensitivity =
            new BooleanSetting("Reduce Sensitivity While Zoomed", true);

    private final NumberSetting sensitivityStrength =
            new NumberSetting("Zoom Sensitivity Reduction (%)", 100, 0, 100, 5);

    public ZoomModule() {
        super(
                "Zoom",
                "Zoom with hold/toggle, smooth transitions, scroll adjustment, and optional sensitivity scaling as you zoom further.",
                Category.SMP
        );

        addSetting(zoomType);
        addSetting(zoomKey);
        addSetting(zoomFov);
        addSetting(scrollToZoom);
        addSetting(scrollStep);
        addSetting(minimumScrollFov);
        addSetting(smoothZoom);
        addSetting(smoothSpeed);
        addSetting(reduceSensitivity);
        addSetting(sensitivityStrength);
    }

    @Override
    protected void onDisable() {
        dev.yogi.yogiessentials.client.util.ZoomManager.reset();
    }

    @Override
    public boolean isSettingVisible(dev.yogi.yogiessentials.client.setting.Setting<?> setting) {
        if (setting == smoothSpeed) {
            return smoothZoom.get();
        }
        if (setting == scrollStep || setting == minimumScrollFov) {
            return scrollToZoom.get();
        }
        if (setting == sensitivityStrength) {
            return reduceSensitivity.get();
        }
        return true;
    }

    public EnumSetting<ZoomType> getZoomType() { return zoomType; }
    public KeybindSetting getZoomKey() { return zoomKey; }
    public NumberSetting getZoomFov() { return zoomFov; }
    public BooleanSetting getScrollToZoom() { return scrollToZoom; }
    public NumberSetting getScrollStep() { return scrollStep; }
    public NumberSetting getMinimumScrollFov() { return minimumScrollFov; }
    public BooleanSetting getSmoothZoom() { return smoothZoom; }
    public NumberSetting getSmoothSpeed() { return smoothSpeed; }
    public BooleanSetting getReduceSensitivity() { return reduceSensitivity; }
    public NumberSetting getSensitivityStrength() { return sensitivityStrength; }
}
