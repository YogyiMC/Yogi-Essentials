package dev.yogi.yogiessentials.client.util;

import net.fabricmc.loader.api.FabricLoader;

/** Single source of truth for the installed Yogi Essentials version. */
public final class ModVersionUtil {
    private static final String MOD_ID = "yogiessentials";

    private ModVersionUtil() {}

    public static String version() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static String displayVersion() {
        return "v" + version();
    }
}
