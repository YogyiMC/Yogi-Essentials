package dev.yogi.yogiessentials.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small optional bridge for VulkanMod's window-mode setting.
 *
 * Yogi Essentials intentionally has no compile-time dependency on VulkanMod.
 * When VulkanMod is present we set its runtime Config.windowMode to 0 (Windowed)
 * and persist the same value to vulkanmod_settings.json.  This prevents Vulkan's
 * own Fullscreen / Windowed Fullscreen controller from fighting Yogi's native
 * compositor-owned borderless window.
 */
public final class VulkanWindowModeBridge {
    private static final int VULKAN_WINDOWED = 0;
    private static boolean warned;

    private VulkanWindowModeBridge() {}

    public static boolean isVulkanLoaded() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("vulkanmod") || loader.isModLoaded("vulkan-mod");
    }

    /**
     * Best-effort live + persisted switch to VulkanMod's plain Windowed mode.
     * Returns true if either the live config or the config file was updated.
     */
    public static boolean forceWindowedMode() {
        if (!isVulkanLoaded()) return true;

        boolean live = forceLiveConfig();
        boolean file = forceConfigFile();
        if (!live && !file && !warned) {
            warned = true;
            System.err.println("[Yogi Essentials] Could not force VulkanMod Window Mode to Windowed; Yogi Essentials Borderless Fullscreen will still remain non-exclusive.");
        }
        return live || file;
    }

    private static boolean forceLiveConfig() {
        try {
            Class<?> initializer = Class.forName("net.vulkanmod.Initializer", false,
                    VulkanWindowModeBridge.class.getClassLoader());
            Field configField;
            try {
                configField = initializer.getField("CONFIG");
            } catch (NoSuchFieldException ignored) {
                configField = initializer.getDeclaredField("CONFIG");
                configField.setAccessible(true);
            }
            Object config = configField.get(null);
            if (config == null) return false;

            Field mode;
            try {
                mode = config.getClass().getField("windowMode");
            } catch (NoSuchFieldException ignored) {
                mode = config.getClass().getDeclaredField("windowMode");
                mode.setAccessible(true);
            }

            Class<?> type = mode.getType();
            if (type == int.class || type == Integer.class) {
                mode.set(config, VULKAN_WINDOWED);
            } else if (type.isEnum()) {
                Object windowed = null;
                for (Object constant : type.getEnumConstants()) {
                    if (constant.toString().equalsIgnoreCase("windowed")
                            || ((Enum<?>) constant).name().equalsIgnoreCase("windowed")) {
                        windowed = constant;
                        break;
                    }
                }
                if (windowed == null) return false;
                mode.set(config, windowed);
            } else {
                return false;
            }

            try {
                Method write;
                try {
                    write = config.getClass().getMethod("write");
                } catch (NoSuchMethodException ignored) {
                    write = config.getClass().getDeclaredMethod("write");
                    write.setAccessible(true);
                }
                write.invoke(config);
            } catch (ReflectiveOperationException ignored) {
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean forceConfigFile() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("vulkanmod_settings.json");
        try {
            if (!Files.exists(path)) return false;
            JsonObject root;
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            }

            JsonElement old = root.get("windowMode");
            if (old != null && old.isJsonPrimitive() && old.getAsInt() == VULKAN_WINDOWED) {
                return true;
            }

            root.addProperty("windowMode", VULKAN_WINDOWED);
            try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
