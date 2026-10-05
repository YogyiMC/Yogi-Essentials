package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.smp.ZoomModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_310;
import org.lwjgl.glfw.GLFW;

public final class ZoomManager {

    private static boolean initialized;
    private static boolean keyWasDown;
    private static boolean toggled;
    private static boolean active;
    private static boolean wasActive;
    private static float previousFactor;
    private static float factor;
    private static float desiredZoomFov = Float.NaN;
    private static float currentZoomFov = Float.NaN;
    private static float previousZoomFov = Float.NaN;

    private ZoomManager() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(ZoomManager::tick);
    }

    private static ZoomModule module() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        return YogiEssentialsClient.getModuleManager().getModule(ZoomModule.class);
    }

    private static void tick(class_310 client) {
        previousFactor = factor;
        previousZoomFov = currentZoomFov;

        ZoomModule module = module();
        if (module == null || !module.isEnabled() || client == null || client.method_22683() == null) {
            reset();
            return;
        }

        int key = module.getZoomKey().get();
        boolean keyDown = key >= 0
                && client.field_1755 == null
                && GLFW.glfwGetKey(client.method_22683().method_4490(), key) == GLFW.GLFW_PRESS;

        if (module.getZoomType().get() == ZoomModule.ZoomType.TOGGLE) {
            if (keyDown && !keyWasDown) {
                toggled = !toggled;
            }
            active = toggled;
        } else {
            toggled = false;
            active = keyDown;
        }

        keyWasDown = keyDown;

        
        
        
        if (active && !wasActive) {
            desiredZoomFov = module.getZoomFov().get().floatValue();
            currentZoomFov = desiredZoomFov;
            previousZoomFov = desiredZoomFov;
        }
        wasActive = active;

        if (Float.isNaN(desiredZoomFov)) {
            desiredZoomFov = module.getZoomFov().get().floatValue();
        }
        if (Float.isNaN(currentZoomFov)) {
            currentZoomFov = desiredZoomFov;
        }
        if (Float.isNaN(previousZoomFov)) {
            previousZoomFov = currentZoomFov;
        }

        float target = active ? 1.0F : 0.0F;
        if (!module.getSmoothZoom().get()) {
            factor = target;
            previousFactor = target;
            currentZoomFov = desiredZoomFov;
            previousZoomFov = desiredZoomFov;
            return;
        }

        float speed = module.getSmoothSpeed().get().floatValue();
        float step = Math.max(0.02F, Math.min(1.0F, speed));
        factor += (target - factor) * step;
        currentZoomFov += (desiredZoomFov - currentZoomFov) * step;

        if (Math.abs(target - factor) < 0.002F) {
            factor = target;
        }
        if (Math.abs(desiredZoomFov - currentZoomFov) < 0.01F) {
            currentZoomFov = desiredZoomFov;
        }
    }

    




    public static boolean handleScroll(double verticalAmount) {
        ZoomModule module = module();
        class_310 client = class_310.method_1551();

        if (module == null
                || !module.isEnabled()
                || !module.getScrollToZoom().get()
                || !active
                || client == null
                || client.field_1755 != null
                || Math.abs(verticalAmount) < 0.0001) {
            return false;
        }

        float configuredFov = module.getZoomFov().get().floatValue();
        float minimum = Math.min(
                configuredFov,
                module.getMinimumScrollFov().get().floatValue()
        );
        float step = module.getScrollStep().get().floatValue();

        if (Float.isNaN(desiredZoomFov)) {
            desiredZoomFov = configuredFov;
        }

        
        desiredZoomFov -= (float) verticalAmount * step;
        desiredZoomFov = Math.max(minimum, Math.min(configuredFov, desiredZoomFov));

        if (!module.getSmoothZoom().get()) {
            currentZoomFov = desiredZoomFov;
            previousZoomFov = desiredZoomFov;
        }

        return true;
    }

    public static float applyZoom(float baseFov, float tickProgress) {
        ZoomModule module = module();
        if (module == null || !module.isEnabled()) return baseFov;

        float t = Math.max(0.0F, Math.min(1.0F, tickProgress));
        float renderedFactor = previousFactor + (factor - previousFactor) * t;
        if (renderedFactor <= 0.0001F) return baseFov;

        float targetFov;
        if (Float.isNaN(currentZoomFov)) {
            targetFov = module.getZoomFov().get().floatValue();
        } else if (Float.isNaN(previousZoomFov)) {
            targetFov = currentZoomFov;
        } else {
            targetFov = previousZoomFov + (currentZoomFov - previousZoomFov) * t;
        }

        return baseFov + (targetFov - baseFov) * renderedFactor;
    }

    public static boolean isZoomed() {
        return active || factor > 0.001F || previousFactor > 0.001F;
    }

    public static double scaleLookDelta(double delta) {
        ZoomModule zoom = module();
        class_310 client = class_310.method_1551();
        if (zoom == null || !zoom.isEnabled() || !zoom.getReduceSensitivity().get()
                || client == null || client.field_1755 != null || !isZoomed()) return delta;
        double baseFov = client.field_1690.method_41808().method_41753();
        if (baseFov <= 0.0) return delta;
        double zoomFov = Float.isNaN(currentZoomFov)
                ? zoom.getZoomFov().get() : currentZoomFov;
        double currentFov = baseFov + (zoomFov - baseFov) * factor;
        double ratio = Math.tan(Math.toRadians(Math.max(1.0, currentFov)) / 2.0)
                / Math.tan(Math.toRadians(baseFov) / 2.0);
        double strength = zoom.getSensitivityStrength().get() / 100.0;
        return delta * (1.0 - strength * (1.0 - Math.min(1.0, ratio)));
    }

    public static void reset() {
        keyWasDown = false;
        toggled = false;
        active = false;
        wasActive = false;
        previousFactor = 0.0F;
        factor = 0.0F;
        desiredZoomFov = Float.NaN;
        currentZoomFov = Float.NaN;
        previousZoomFov = Float.NaN;
    }
}
