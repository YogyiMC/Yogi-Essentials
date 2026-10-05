package dev.yogi.yogiessentials.client.keybind;

import dev.yogi.yogiessentials.client.gui.HudEditorScreen;
import dev.yogi.yogiessentials.client.gui.YogiEssentialsScreen;
import dev.yogi.yogiessentials.client.gui.WaypointScreen;
import dev.yogi.yogiessentials.client.util.UiSoundManager;
import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.smp.FullbrightModule;
import dev.yogi.yogiessentials.client.module.smp.WaypointModule;
import dev.yogi.yogiessentials.client.config.ConfigManager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_3675;
import org.lwjgl.glfw.GLFW;

public final class KeybindManager {

    private static final class_304.class_11900 CATEGORY =
            class_304.class_11900.method_74698(
                    class_2960.method_60655(
                            "yogiessentials",
                            "main"
                    )
            );

    private static class_304 openMenuKey;
    private static boolean fullbrightKeyDown;
    private static boolean manageWaypointsKeyDown;

    private KeybindManager() {
    }

    
    public static class_304 getOpenMenuKey() {
        return openMenuKey;
    }

    public static void initialize() {

        openMenuKey =
                KeyBindingHelper.registerKeyBinding(
                        new class_304(
                                "key.yogiessentials.open_menu",
                                class_3675.class_307.field_1668,
                                GLFW.GLFW_KEY_RIGHT_SHIFT,
                                CATEGORY
                        )
                );

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {

                    FullbrightModule fullbright = YogiEssentialsClient.getModuleManager() == null
                            ? null
                            : YogiEssentialsClient.getModuleManager().getModule(FullbrightModule.class);
                    if (fullbright != null && client.method_22683() != null) {
                        int key = fullbright.getToggleKey().get();
                        boolean down = key > 0
                                && GLFW.glfwGetKey(client.method_22683().method_4490(), key) == GLFW.GLFW_PRESS;
                        if (down && !fullbrightKeyDown && client.field_1755 == null) {
                            fullbright.toggle();
                            ConfigManager.save();
                        }
                        fullbrightKeyDown = down;
                    } else {
                        fullbrightKeyDown = false;
                    }

                    WaypointModule waypoints = YogiEssentialsClient.getModuleManager() == null
                            ? null
                            : YogiEssentialsClient.getModuleManager().getModule(WaypointModule.class);
                    if (waypoints != null && client.method_22683() != null) {
                        int key = waypoints.getManageWaypointsKey().get();
                        boolean down = key > 0
                                && GLFW.glfwGetKey(client.method_22683().method_4490(), key) == GLFW.GLFW_PRESS;

                        if (down && !manageWaypointsKeyDown && client.field_1755 == null) {
                            UiSoundManager.menuOpen();
                            client.method_1507(new WaypointScreen(null));
                        }

                        manageWaypointsKeyDown = down;
                    } else {
                        manageWaypointsKeyDown = false;
                    }

                    while (
                            openMenuKey.method_1436()
                    ) {

                        if (
                                client.field_1755
                                        instanceof HudEditorScreen
                                        ||
                                        client.field_1755
                                                instanceof YogiEssentialsScreen
                        ) {

                            UiSoundManager.menuClose();

                            client.method_1507(
                                    null
                            );

                            continue;
                        }

                        if (
                                client.field_1755
                                        == null
                        ) {

                            UiSoundManager.menuOpen();

                            client.method_1507(
                                    new HudEditorScreen()
                            );
                        }
                    }
                }
        );
    }
}