package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.Module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_310;

/**
 * Reconciles option-backed performance modules without hammering Minecraft's
 * SimpleOption callbacks on a fixed one-second loop.
 *
 * Modules already apply immediately when toggled. Runtime reconciliation is now
 * event-biased: once when an options/menu screen closes, plus a slow 30-second
 * safety check for third-party changes made outside screens. Memory Stability is
 * ticked independently because its cleanup delay is intentionally tick-based.
 */
public final class PerformanceOptionManager {

    private static boolean initialized;
    private static int fallbackTicker;
    private static Object lastScreen;

    private PerformanceOptionManager() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            reconcileMemory(client);

            Object currentScreen = client == null ? null : client.field_1755;
            boolean screenClosed = lastScreen != null && currentScreen == null;
            lastScreen = currentScreen;

            if (screenClosed || ++fallbackTicker >= 600) {
                fallbackTicker = 0;
                reconcileOptionModules();
            }
        });
    }

    public static void reconcileAll() {
        reconcileOptionModules();
        reconcileMemory(class_310.method_1551());
    }

    private static void reconcileOptionModules() {
        if (YogiEssentialsClient.getModuleManager() == null) return;
        for (Module module : YogiEssentialsClient.getModuleManager().getModules()) {
            if (module instanceof OptionBackedPerformanceModule performance) {
                performance.reconcileWithMinecraftOptions();
            }
        }
    }

    private static void reconcileMemory(class_310 client) {
        if (YogiEssentialsClient.getModuleManager() == null) return;
        MemoryStabilityModule memory = YogiEssentialsClient.getModuleManager()
                .getModule(MemoryStabilityModule.class);
        if (memory != null) memory.reconcileMemory(client);
    }
}
