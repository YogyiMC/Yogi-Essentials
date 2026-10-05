package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.gui.WelcomeScreen;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;









public final class WelcomeManager {

    private static boolean pending;

    private WelcomeManager() {
    }

    public static void initialize() {
        
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> {
                    if (!ConfigManager.hasSeenWelcome()) {
                        pending = true;
                    }
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    if (ConfigManager.hasSeenWelcome()) {
                        pending = false;
                        return;
                    }

                    if (!pending) {
                        return;
                    }

                    
                    
                    
                    if (client.field_1724 != null
                            && client.field_1687 != null
                            && client.field_1755 == null) {
                        pending = false;
                        client.method_1507(new WelcomeScreen());
                    }
                }
        );
    }
}
