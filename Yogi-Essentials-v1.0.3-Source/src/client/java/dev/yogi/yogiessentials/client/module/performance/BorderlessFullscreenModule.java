package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.VulkanWindowModeBridge;
import net.minecraft.class_1041;
import net.minecraft.class_310;
import net.minecraft.class_313;
import net.minecraft.class_319;
import org.lwjgl.glfw.GLFW;

/**
 * Borderless Fullscreen implementation restored from the published v1.0.2 build.
 *
 * v1.0.2 is the known-good presentation path for Sodium/Vulkan/Discord capture:
 * first leave exclusive fullscreen through Minecraft, then turn the normal GLFW
 * window into an undecorated monitor-sized window.  Do not replace this with
 * fullscreen-flag/accessor forcing or applyFullscreenVideoMode interception.
 */
public final class BorderlessFullscreenModule extends Module {
    private final BooleanSetting captureSafeMode = new BooleanSetting("Discord Capture Safe Mode", true);
    private final NumberSetting captureSafeInset = new NumberSetting("Capture Safe Inset", 1.0, 0.0, 4.0, 1.0);
    private final BooleanSetting repairOnFocus = new BooleanSetting("Repair On Focus", true);
    private final BooleanSetting coverTaskbar = new BooleanSetting("Cover Taskbar", true);

    private boolean borderlessApplied;
    private boolean capturedWindowedState;
    private boolean pendingInitialApply;
    private int deferredTicks;
    private boolean lastFocused;
    private int focusRepairTicks = -1;

    private int previousX;
    private int previousY;
    private int previousWidth = 1280;
    private int previousHeight = 720;
    private boolean previousDecorated = true;
    private boolean previousResizable = true;
    private boolean previousFloating;
    private boolean previousAutoIconify = true;
    private boolean previousMaximized;

    private static boolean allowVanillaFullscreenToggle;

    public BorderlessFullscreenModule() {
        super(
                "Borderless Fullscreen",
                "Windowed borderless fullscreen with Discord capture-safe sizing, taskbar coverage, and focus repair. F11 toggles between borderless and your normal window.",
                Category.PERFORMANCE
        );
        addSetting(captureSafeMode);
        addSetting(captureSafeInset);
        addSetting(repairOnFocus);
        addSetting(coverTaskbar);
    }

    @Override
    protected void onEnable() {
        class_310 client = class_310.method_1551();
        lastFocused = client != null && client.method_1569();
        focusRepairTicks = -1;
        pendingInitialApply = true;
        deferredTicks = 2;

        VulkanWindowModeBridge.forceWindowedMode();
        reconcileWindow();
    }

    @Override
    protected void onDisable() {
        pendingInitialApply = false;
        deferredTicks = 0;
        focusRepairTicks = -1;
        restoreOriginalState();
    }

    @Override
    public boolean isSettingVisible(dev.yogi.yogiessentials.client.setting.Setting<?> setting) {
        if (setting == captureSafeInset) return captureSafeMode.get();
        return true;
    }

    /** Exact v1.0.2 runtime behavior. */
    public void reconcileWindow() {
        if (!isEnabled()) return;
        class_310 client = class_310.method_1551();
        if (client == null || client.method_22683() == null || !isSupportedPlatform()) return;

        class_1041 window = client.method_22683();
        boolean focused = client.method_1569();

        if (borderlessApplied && repairOnFocus.get()) {
            if (!lastFocused && focused) focusRepairTicks = 2;
            if (focused && focusRepairTicks >= 0) {
                if (focusRepairTicks-- == 0) {
                    repairBorderless(window);
                    focusRepairTicks = -1;
                }
            }
        } else {
            focusRepairTicks = -1;
        }

        if (borderlessApplied && focused != lastFocused) {
            updateTaskbarCover(window, focused);
        }
        lastFocused = focused;

        if (!pendingInitialApply) return;
        if (deferredTicks > 0) {
            deferredTicks--;
            return;
        }

        if (window.method_4498()) {
            runVanillaFullscreenToggle(window);
            deferredTicks = 2;
            return;
        }

        captureWindowedState(window);
        applyBorderless(window);
        pendingInitialApply = false;
    }

    /** Exact v1.0.2 F11 behavior. */
    public boolean handleFullscreenToggle(class_1041 window) {
        if (!isEnabled() || allowVanillaFullscreenToggle || !isSupportedPlatform()) return false;

        pendingInitialApply = false;
        deferredTicks = 0;
        focusRepairTicks = -1;

        if (borderlessApplied) {
            restoreWindowedState(window);
        } else if (window.method_4498()) {
            runVanillaFullscreenToggle(window);
            pendingInitialApply = true;
            deferredTicks = 2;
        } else {
            captureWindowedState(window);
            applyBorderless(window);
        }
        return true;
    }

    private void captureWindowedState(class_1041 window) {
        if (capturedWindowedState) return;
        long handle = window.method_4490();
        previousX = window.method_4499();
        previousY = window.method_4477();
        previousWidth = Math.max(320, window.method_4480());
        previousHeight = Math.max(240, window.method_4507());
        previousDecorated = attribute(handle, GLFW.GLFW_DECORATED, true);
        previousResizable = attribute(handle, GLFW.GLFW_RESIZABLE, true);
        previousFloating = attribute(handle, GLFW.GLFW_FLOATING, false);
        previousAutoIconify = attribute(handle, GLFW.GLFW_AUTO_ICONIFY, true);
        previousMaximized = attribute(handle, GLFW.GLFW_MAXIMIZED, false);
        capturedWindowedState = true;
    }

    private void applyBorderless(class_1041 window) {
        if (!applyBorderlessGeometry(window)) return;
        borderlessApplied = true;
    }

    private void repairBorderless(class_1041 window) {
        if (!borderlessApplied) return;
        applyBorderlessGeometry(window);
    }

    private boolean applyBorderlessGeometry(class_1041 window) {
        class_313 monitor = window.method_20831();
        if (monitor == null) return false;
        class_319 mode = monitor.method_1617();
        if (mode == null) return false;

        long handle = window.method_4490();
        int inset = captureSafeMode.get() && isWindows()
                ? Math.max(0, (int) Math.round(captureSafeInset.get()))
                : 0;
        int x = monitor.method_1616();
        int y = monitor.method_1618();
        int width = Math.max(320, mode.method_1668() - inset);
        int height = Math.max(240, mode.method_1669());

        GLFW.glfwRestoreWindow(handle);
        if (GLFW.glfwGetWindowMonitor(handle) != 0L) {
            GLFW.glfwSetWindowMonitor(handle, 0L, x, y, width, height, GLFW.GLFW_DONT_CARE);
        }
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_AUTO_ICONIFY, GLFW.GLFW_FALSE);

        boolean focused = class_310.method_1551() != null && class_310.method_1551().method_1569();
        updateTaskbarCover(window, focused);
        GLFW.glfwSetWindowPos(handle, x, y);
        GLFW.glfwSetWindowSize(handle, width, height);

        if (focused) {
            try {
                GLFW.glfwFocusWindow(handle);
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    private void updateTaskbarCover(class_1041 window, boolean focused) {
        if (window == null) return;
        boolean floating = coverTaskbar.get() && isWindows() && focused;
        try {
            GLFW.glfwSetWindowAttrib(
                    window.method_4490(),
                    GLFW.GLFW_FLOATING,
                    floating ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE
            );
        } catch (Throwable ignored) {
        }
    }

    private void restoreWindowedState(class_1041 window) {
        if (!capturedWindowedState) {
            borderlessApplied = false;
            return;
        }
        long handle = window.method_4490();
        GLFW.glfwRestoreWindow(handle);
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, previousDecorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_RESIZABLE, previousResizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FLOATING, previousFloating ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_AUTO_ICONIFY, previousAutoIconify ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowPos(handle, previousX, previousY);
        GLFW.glfwSetWindowSize(handle, Math.max(320, previousWidth), Math.max(240, previousHeight));
        if (previousMaximized) GLFW.glfwMaximizeWindow(handle);
        borderlessApplied = false;
    }

    private void restoreOriginalState() {
        class_310 client = class_310.method_1551();
        if (client == null || client.method_22683() == null) {
            borderlessApplied = false;
            capturedWindowedState = false;
            return;
        }
        class_1041 window = client.method_22683();
        if (borderlessApplied) restoreWindowedState(window);
        borderlessApplied = false;
        capturedWindowedState = false;
    }

    /**
     * Compatibility entry point used by the Vulkan notice button.
     * Keeps the published v1.0.2 transition semantics: if Minecraft is in real
     * fullscreen, leave it through vanilla first and defer borderless application;
     * otherwise apply the same v1.0.2 windowed-borderless geometry immediately.
     */
    public boolean applyNow() {
        if (!isEnabled() || !isSupportedPlatform()) return false;

        class_310 client = class_310.method_1551();
        if (client == null || client.method_22683() == null) return false;

        VulkanWindowModeBridge.forceWindowedMode();
        class_1041 window = client.method_22683();
        focusRepairTicks = -1;

        if (window.method_4498()) {
            runVanillaFullscreenToggle(window);
            pendingInitialApply = true;
            deferredTicks = 2;
            return true;
        }

        pendingInitialApply = false;
        deferredTicks = 0;
        captureWindowedState(window);
        if (!applyBorderlessGeometry(window)) return false;
        borderlessApplied = true;
        return true;
    }

    public boolean refreshPresentation() {
        if (!isEnabled() || !borderlessApplied) return false;
        class_310 client = class_310.method_1551();
        if (client == null || client.method_22683() == null) return false;
        return applyBorderlessGeometry(client.method_22683());
    }

    public boolean isApplied() {
        return borderlessApplied;
    }

    public BooleanSetting getCaptureSafeMode() {
        return captureSafeMode;
    }

    public NumberSetting getCaptureSafeInset() {
        return captureSafeInset;
    }

    public BooleanSetting getRepairOnFocus() {
        return repairOnFocus;
    }

    public BooleanSetting getCoverTaskbar() {
        return coverTaskbar;
    }

    public static boolean shouldInterceptVanillaFullscreenToggle() {
        if (allowVanillaFullscreenToggle || YogiEssentialsClient.getModuleManager() == null) return false;
        BorderlessFullscreenModule module = YogiEssentialsClient.getModuleManager().getModule(BorderlessFullscreenModule.class);
        return module != null && module.isEnabled();
    }

    public static boolean interceptVanillaFullscreenToggle(class_1041 window) {
        if (allowVanillaFullscreenToggle || YogiEssentialsClient.getModuleManager() == null) return false;
        BorderlessFullscreenModule module = YogiEssentialsClient.getModuleManager().getModule(BorderlessFullscreenModule.class);
        return module != null && module.handleFullscreenToggle(window);
    }

    private static void runVanillaFullscreenToggle(class_1041 window) {
        allowVanillaFullscreenToggle = true;
        try {
            window.method_4500();
        } finally {
            allowVanillaFullscreenToggle = false;
        }
    }

    private static boolean attribute(long handle, int attribute, boolean fallback) {
        try {
            return GLFW.glfwGetWindowAttrib(handle, attribute) == GLFW.GLFW_TRUE;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static boolean isSupportedPlatform() {
        return !System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}
