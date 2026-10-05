package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.module.pvp.ToggleSprintModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_310;
import org.lwjgl.glfw.GLFW;






public final class ToggleSprintManager {

    private static boolean initialized;
    private static boolean toggleKeyDown;
    private static boolean forcingSprint;

    private ToggleSprintManager() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ToggleSprintModule module = getModule();
        if (module != null) {
            module.prepareForSession();
        }

        ClientTickEvents.END_CLIENT_TICK.register(
                ToggleSprintManager::onEndClientTick
        );
    }

    private static void onEndClientTick(class_310 client) {
        ToggleSprintModule module = getModule();

        if (module == null || client == null || client.method_22683() == null) {
            toggleKeyDown = false;
            releaseForcedSprint();
            return;
        }

        handleToggleKey(client, module);

        boolean canApply =
                client.field_1724 != null
                        && client.field_1687 != null
                        && client.field_1755 == null;

        if (!canApply || !module.isEnabled()) {
            releaseForcedSprint();
            return;
        }

        boolean shouldSprint = module.shouldForceSprint();
        client.field_1690.field_1867.method_23481(shouldSprint);
        forcingSprint = shouldSprint;

        if (shouldSprint) {
            if (canSprintNow(client) && !client.field_1724.method_5624()) {
                client.field_1724.method_5728(true);
            }
        } else if (client.field_1724.method_5624()) {
            client.field_1724.method_5728(false);
        }
    }

    private static void handleToggleKey(
            class_310 client,
            ToggleSprintModule module
    ) {
        int key = module.getToggleKey().get();

        boolean down =
                key > GLFW.GLFW_KEY_UNKNOWN
                        && GLFW.glfwGetKey(
                                client.method_22683().method_4490(),
                                key
                        ) == GLFW.GLFW_PRESS;

        if (
                down
                        && !toggleKeyDown
                        && client.field_1755 == null
                        && module.isEnabled()
        ) {
            if (module.toggleSprintState()) {
                ConfigManager.save();
            }
        }

        toggleKeyDown = down;
    }

    





    private static boolean canSprintNow(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1690 == null) {
            return false;
        }
        if (!client.field_1690.field_1894.method_1434()) {
            return false;
        }
        if (client.field_1724.method_5715() || client.field_1724.field_5976) {
            return false;
        }
        return client.field_1724.method_7344().method_7586() > 6;
    }

    public static void releaseForcedSprint() {
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1690 != null) {
            client.field_1690.field_1867.method_23481(false);
            if (client.field_1724 != null && client.field_1724.method_5624()) {
                client.field_1724.method_5728(false);
            }

        }

        forcingSprint = false;
    }

    private static ToggleSprintModule getModule() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }

        return YogiEssentialsClient
                .getModuleManager()
                .getModule(ToggleSprintModule.class);
    }
}
