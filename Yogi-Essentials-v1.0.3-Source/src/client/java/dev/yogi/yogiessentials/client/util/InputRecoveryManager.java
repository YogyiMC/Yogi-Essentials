package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.hud.HudManager;
import dev.yogi.yogiessentials.client.module.fixes.FocusRecoveryFixModule;
import dev.yogi.yogiessentials.client.module.fixes.StuckKeyFixModule;
import dev.yogi.yogiessentials.client.module.fixes.WorldUnloadCleanupModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_1041;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_437;









public final class InputRecoveryManager {
    private static final int RECOVERY_DELAY_TICKS = 2;

    private static boolean initialized;
    private static boolean wasFocused;
    private static class_437 lastScreen;
    private static Object lastWorld;
    private static Object lastNetworkHandler;

    private static int lastWindowWidth = -1;
    private static int lastWindowHeight = -1;
    private static boolean lastFullscreen;

    private static int keyResyncTicks = -1;
    private static int cursorRecoveryTicks = -1;

    private InputRecoveryManager() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        class_310 client = class_310.method_1551();
        if (client != null) {
            wasFocused = client.method_1569();
            lastScreen = client.field_1755;
            lastWorld = client.field_1687;
            lastNetworkHandler = client.method_1562();
            captureWindowState(client);
        }

        ClientTickEvents.END_CLIENT_TICK.register(InputRecoveryManager::tick);
    }

    private static void tick(class_310 client) {
        if (client == null || YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        StuckKeyFixModule stuck = YogiEssentialsClient.getModuleManager()
                .getModule(StuckKeyFixModule.class);
        FocusRecoveryFixModule focusRecovery = YogiEssentialsClient.getModuleManager()
                .getModule(FocusRecoveryFixModule.class);
        WorldUnloadCleanupModule cleanup = YogiEssentialsClient.getModuleManager()
                .getModule(WorldUnloadCleanupModule.class);

        boolean stuckEnabled = stuck != null && stuck.isEnabled();
        boolean focusRecoveryEnabled = focusRecovery != null && focusRecovery.isEnabled();
        boolean focused = client.method_1569();
        class_437 screen = client.field_1755;

        
        
        
        Object world = client.field_1687;
        Object networkHandler = client.method_1562();
        boolean worldChanged = lastWorld != world;
        boolean connectionChanged = lastNetworkHandler != networkHandler;

        if (worldChanged && cleanup != null && cleanup.isEnabled()) {
            
            
            
            
            boolean preserveConnectionPing =
                    !connectionChanged
                            && networkHandler != null;

            HudManager.clearTransientState(
                    preserveConnectionPing
            );
            ViewportCulling.clearCaches();
            dev.yogi.yogiessentials.client.module.performance.YogiTerrainScheduler.clear();
            FrameTelemetry.resetFrameClock();
            FpsCapController.reset();
        }

        
        
        
        if (wasFocused && !focused && stuckEnabled) {
            releaseAllKeybinds();
            keyResyncTicks = -1;
        }

        boolean screenOpened = lastScreen == null && screen != null;
        boolean screenClosed = lastScreen != null && screen == null;

        
        
        
        
        if (screenOpened && stuckEnabled) {
            releaseAllKeybinds();
        }

        boolean windowChanged = detectWindowStateChange(client);
        boolean regainedFocus = !wasFocused && focused;

        
        
        
        if (windowChanged && stuckEnabled) {
            releaseAllKeybinds();
        }

        if (focused && (regainedFocus || screenClosed || windowChanged)) {
            if (stuckEnabled) {
                keyResyncTicks = RECOVERY_DELAY_TICKS;
            }
            if (focusRecoveryEnabled) {
                cursorRecoveryTicks = RECOVERY_DELAY_TICKS;
            }
        }

        if (!focused || screen != null || client.field_1687 == null || client.field_1724 == null) {
            
            
            
            if (!focused) {
                cursorRecoveryTicks = -1;
            }
        } else {
            if (keyResyncTicks >= 0) {
                if (keyResyncTicks-- == 0) {
                    try {
                        class_304.method_1424();
                    } catch (Throwable ignored) {
                        
                    }
                    keyResyncTicks = -1;
                }
            }

            if (cursorRecoveryTicks >= 0) {
                if (cursorRecoveryTicks-- == 0) {
                    try {
                        if (!client.field_1729.method_1613()) {
                            client.field_1729.method_1612();
                        }
                    } catch (Throwable ignored) {
                        
                    }
                    cursorRecoveryTicks = -1;
                }
            }
        }

        lastWorld = world;
        lastNetworkHandler = networkHandler;
        lastScreen = screen;
        wasFocused = focused;
    }

    private static void releaseAllKeybinds() {
        try {
            class_304.method_1437();
        } catch (Throwable ignored) {
            
            
        }
    }

    private static boolean detectWindowStateChange(class_310 client) {
        class_1041 window = client.method_22683();
        if (window == null) {
            return false;
        }

        int currentWidth = window.method_4480();
        int currentHeight = window.method_4507();
        boolean currentFullscreen = window.method_4498();

        boolean changed = lastWindowWidth >= 0
                && (currentWidth != lastWindowWidth
                || currentHeight != lastWindowHeight
                || currentFullscreen != lastFullscreen);

        lastWindowWidth = currentWidth;
        lastWindowHeight = currentHeight;
        lastFullscreen = currentFullscreen;
        return changed;
    }

    private static void captureWindowState(class_310 client) {
        class_1041 window = client.method_22683();
        if (window == null) {
            return;
        }

        lastWindowWidth = window.method_4480();
        lastWindowHeight = window.method_4507();
        lastFullscreen = window.method_4498();
    }
}
