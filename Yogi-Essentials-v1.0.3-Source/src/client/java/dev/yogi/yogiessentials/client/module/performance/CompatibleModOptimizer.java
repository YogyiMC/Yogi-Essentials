package dev.yogi.yogiessentials.client.module.performance;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_310;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompatibleModOptimizer {
    private static final Map<String, String> SODIUM_PERFORMANCE_BOOLEANS = Map.of(
            "useBlockFaceCulling", "Block face culling",
            "useFogOcclusion", "Fog occlusion",
            "useEntityCulling", "Entity culling",
            "animateOnlyVisibleTextures", "Visible texture animation",
            "alwaysDeferChunkUpdates", "Deferred chunk updates"
    );

    private CompatibleModOptimizer() {
    }

    public static Map<String, Boolean> captureSodium() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        try {
            Object performance = sodiumPerformance();
            if (performance == null) return values;
            for (String name : SODIUM_PERFORMANCE_BOOLEANS.keySet()) {
                try {
                    Field field = performance.getClass().getField(name);
                    if (field.getType() == boolean.class) {
                        values.put(name, field.getBoolean(performance));
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            values.clear();
        }
        return values;
    }

    public static List<String> optimizeSodium() {
        Map<String, Boolean> previous = captureSodium();
        if (previous.isEmpty()) return List.of();

        List<String> changes = new ArrayList<>();
        Map<String, Boolean> enabled = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : previous.entrySet()) {
            enabled.put(entry.getKey(), true);
            if (!entry.getValue()) {
                changes.add("Sodium: " + SODIUM_PERFORMANCE_BOOLEANS.getOrDefault(entry.getKey(), entry.getKey()) + " -> On");
            }
        }
        if (changes.isEmpty()) return changes;
        return applySodium(enabled) ? changes : List.of();
    }

    public static void restoreSodium(Map<String, Boolean> values) {
        if (values == null || values.isEmpty()) return;
        applySodium(values);
    }

    private static boolean applySodium(Map<String, Boolean> values) {
        Map<String, Boolean> previous = captureSodium();
        if (previous.isEmpty()) return false;
        try {
            Object options = sodiumOptions();
            Object performance = options.getClass().getField("performance").get(options);
            Method save = options.getClass().getMethod("writeToDisk", options.getClass());
            for (Map.Entry<String, Boolean> entry : values.entrySet()) {
                try {
                    Field field = performance.getClass().getField(entry.getKey());
                    if (field.getType() == boolean.class) {
                        field.setBoolean(performance, entry.getValue());
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
            save.invoke(null, options);
            class_310 client = class_310.method_1551();
            if (client != null && client.field_1687 != null && client.field_1769 != null) {
                try {
                    client.field_1769.getClass().getMethod("reload").invoke(client.field_1769);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError error) {
            try {
                Object performance = sodiumPerformance();
                if (performance != null) {
                    for (Map.Entry<String, Boolean> entry : previous.entrySet()) {
                        try {
                            Field field = performance.getClass().getField(entry.getKey());
                            if (field.getType() == boolean.class) {
                                field.setBoolean(performance, entry.getValue());
                            }
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
            return false;
        }
    }

    private static Object sodiumPerformance() throws ReflectiveOperationException {
        Object options = sodiumOptions();
        return options == null ? null : options.getClass().getField("performance").get(options);
    }

    private static Object sodiumOptions() throws ReflectiveOperationException {
        if (!FabricLoader.getInstance().isModLoaded("sodium")) return null;
        Class<?> sodium = Class.forName("net.caffeinemc.mods.sodium.client.SodiumClientMod");
        return sodium.getMethod("options").invoke(null);
    }
}
