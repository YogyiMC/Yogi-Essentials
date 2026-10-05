package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.fixes.FpsRecoveryFixModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_1041;
import net.minecraft.class_310;
import org.lwjgl.glfw.GLFW;










public final class FpsRecoveryManager {

    private static boolean initialized;
    private static double learnedBaselineFps;
    private static long lowFpsSinceMillis = -1L;
    private static long lastRecoveryMillis;

    private static Object lastWorld;
    private static long worldWarmupUntilMillis;

    private FpsRecoveryManager() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(FpsRecoveryManager::tick);
    }

    public static void reset() {
        learnedBaselineFps = 0.0;
        lowFpsSinceMillis = -1L;
        lastRecoveryMillis = 0L;
        lastWorld = null;
        worldWarmupUntilMillis = 0L;
    }

    private static void tick(class_310 client) {
        if (client == null) {
            return;
        }

        if (YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        FpsRecoveryFixModule module = YogiEssentialsClient.getModuleManager()
                .getModule(FpsRecoveryFixModule.class);

        if (module == null || !module.isEnabled() || !module.getAutoRecovery().get()) {
            lowFpsSinceMillis = -1L;
            return;
        }

        Object world = client.field_1687;
        long now = System.currentTimeMillis();
        if (world != lastWorld) {
            lastWorld = world;
            learnedBaselineFps = 0.0;
            lowFpsSinceMillis = -1L;
            worldWarmupUntilMillis = now + 5_000L;
        }

        
        
        
        
        if (client.field_1687 == null
                || client.field_1724 == null
                || client.field_1755 != null
                || !client.method_1569()
                || now < worldWarmupUntilMillis) {
            lowFpsSinceMillis = -1L;
            return;
        }

        int fps = Math.max(1, client.method_47599());
        updateBaseline(fps);

        double minimumHealthy = module.getMinimumHealthyFps().get();
        if (learnedBaselineFps < minimumHealthy) {
            lowFpsSinceMillis = -1L;
            return;
        }

        double triggerFps = learnedBaselineFps
                * (module.getDropThresholdPercent().get() / 100.0);

        
        
        boolean abnormallyLow = fps <= Math.max(30.0, triggerFps);

        if (!abnormallyLow) {
            lowFpsSinceMillis = -1L;
            return;
        }

        if (lowFpsSinceMillis < 0L) {
            lowFpsSinceMillis = now;
            return;
        }

        long detectionMillis = Math.round(module.getDetectionSeconds().get() * 1000.0);
        long cooldownMillis = Math.round(module.getCooldownSeconds().get() * 1000.0);

        if (now - lowFpsSinceMillis < detectionMillis
                || now - lastRecoveryMillis < cooldownMillis) {
            return;
        }

        lowFpsSinceMillis = -1L;
        lastRecoveryMillis = now;
        performRecovery(client, module, fps, learnedBaselineFps);
    }

    private static void updateBaseline(int fps) {
        if (learnedBaselineFps <= 0.0) {
            learnedBaselineFps = fps;
            return;
        }

        if (fps >= learnedBaselineFps) {
            
            learnedBaselineFps += (fps - learnedBaselineFps) * 0.12;
        } else {
            
            
            learnedBaselineFps += (fps - learnedBaselineFps) * 0.0015;
        }
    }

    private static void performRecovery(
            class_310 client,
            FpsRecoveryFixModule module,
            int fps,
            double baseline
    ) {
        class_1041 window = client.method_22683();
        if (window == null) {
            return;
        }

        FpsCapController.reset();
        FrameTelemetry.resetFrameClock();

        boolean repairedBorderless = false;

        if (module.getRefreshSwapInterval().get()
                && client.field_1690.method_42433().method_41753()
                && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("vulkanmod")) {
            try {
                GLFW.glfwSwapInterval(1);
            } catch (Throwable ignored) {
            }
        }


        System.out.println(
                "[Yogi Essentials] FPS Recovery triggered: current="
                        + fps
                        + " learnedBaseline="
                        + Math.round(baseline)
                        + " borderlessRefresh="
                        + repairedBorderless
        );
    }


}
