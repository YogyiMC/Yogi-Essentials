package dev.yogi.yogiessentials.client.module.fixes;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.util.FpsCapController;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import dev.yogi.yogiessentials.client.util.ViewportCulling;
import net.minecraft.class_310;

/**
 * Resets Yogi Essentials' frame clocks after focus/resize/world transitions so a
 * paused or resized frame cannot poison pacing/telemetry and trigger false recovery.
 */
public final class FrameTransitionFixModule extends Module {
    private final BooleanSetting focusChanges = new BooleanSetting("Reset After Focus Changes", true);
    private final BooleanSetting resizeChanges = new BooleanSetting("Reset After Resize / Fullscreen Changes", true);
    private final BooleanSetting worldChanges = new BooleanSetting("Reset After World Changes", true);

    private boolean initialized;
    private boolean lastFocused;
    private int lastWidth;
    private int lastHeight;
    private boolean lastFullscreen;
    private Object lastWorld;

    public FrameTransitionFixModule() {
        super(
                "Frame Transition Stability",
                "Prevents focus, fullscreen, resize, and world-transition pauses from contaminating FPS pacing and stutter detection.",
                Category.FIXES
        );
        addSetting(focusChanges);
        addSetting(resizeChanges);
        addSetting(worldChanges);
        setEnabled(true);
    }

    public void tick(class_310 client) {
        if (!isEnabled() || client == null || client.method_22683() == null) {
            initialized = false;
            return;
        }

        boolean focused = client.method_1569();
        int width = client.method_22683().method_4489();
        int height = client.method_22683().method_4506();
        boolean fullscreen = client.method_22683().method_4498();
        Object world = client.field_1687;

        if (!initialized) {
            initialized = true;
            lastFocused = focused;
            lastWidth = width;
            lastHeight = height;
            lastFullscreen = fullscreen;
            lastWorld = world;
            resetFrameState(false);
            return;
        }

        boolean focusChanged = focused != lastFocused;
        boolean resizeChanged = width != lastWidth || height != lastHeight || fullscreen != lastFullscreen;
        boolean worldChanged = world != lastWorld;

        if ((focusChanges.get() && focusChanged)
                || (resizeChanges.get() && resizeChanged)
                || (worldChanges.get() && worldChanged)) {
            resetFrameState(worldChanged);
        }

        lastFocused = focused;
        lastWidth = width;
        lastHeight = height;
        lastFullscreen = fullscreen;
        lastWorld = world;
    }

    private static void resetFrameState(boolean worldChanged) {
        FpsCapController.reset();
        FrameTelemetry.resetFrameClock();
        if (worldChanged) {
            ViewportCulling.clearCaches();
        }
    }

    @Override
    protected void onDisable() {
        initialized = false;
    }
}
