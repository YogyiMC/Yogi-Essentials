package dev.yogi.yogiessentials.client.module.performance;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompatibleModConfigTuner {
    private static final int MAX_CONFIG_BYTES = 262144;
    private static final Map<String, Boolean> SODIUM = Map.of(
            "performance.use_block_face_culling", true,
            "performance.use_fog_occlusion", true,
            "performance.use_entity_culling", true,
            "performance.animate_only_visible_textures", true
    );
    private static final Map<String, Boolean> CULLING = Map.of(
            "skipEntityCulling", false,
            "skipBlockEntityCulling", false,
            "blockEntityFrustumCulling", true
    );
    private static final Map<String, Boolean> MORE_CULLING = Map.of(
            "signTextCulling", true,
            "rainCulling", true,
            "useBlockStateCulling", true,
            "itemFrameMapCulling", true,
            "powderSnowCulling", true
    );
    private static final Map<String, Boolean> BAD_OPTIMIZATIONS = Map.of(
            "enable_lightmap_caching", true,
            "enable_debug_renderer_disable_if_not_needed", true,
            "enable_particle_manager_optimization", true,
            "enable_toast_optimizations", true,
            "enable_block_entity_renderer_caching", true,
            "enable_remove_redundant_fov_calculations", true,
            "enable_remove_tutorial_if_not_demo", true
    );
    private static final Map<String, Boolean> SODIUM_EXTRA = Map.of(
            "detail_settings.sky", false,
            "detail_settings.stars", false,
            "detail_settings.sun", false,
            "detail_settings.moon", false,
            "detail_settings.rain_snow", false
    );

    private CompatibleModConfigTuner() {
    }

    public static List<String> optimize(Map<String, String> backups, boolean maximum) {
        List<String> changes = new ArrayList<>();
        if (backups == null) return changes;
        if (installed("sodium")) {
            tuneJson("Sodium", "sodium-options.json", SODIUM, backups, changes);
        }
        if (installed("entityculling")) {
            tuneJson("EntityCulling", "entityculling.json", CULLING, backups, changes);
        }
        if (installed("moreculling")) {
            tuneText("MoreCulling", "moreculling.toml", MORE_CULLING, backups, changes);
        }
        if (installed("badoptimizations")) {
            tuneText("BadOptimizations", "badoptimizations.txt", BAD_OPTIMIZATIONS, backups, changes);
        }
        if (maximum && (installed("sodium-extra") || installed("sodium_extra"))) {
            tuneJson("Sodium Extra", "sodium-extra-options.json", SODIUM_EXTRA, backups, changes);
        }
        return changes;
    }

    /** Captures the current contents of every compatible config file Yogi Essentials may tune.
     * Used by Auto Optimizer so canceling a re-optimization restores the exact
     * already-optimized state rather than the older pre-optimization backup. */
    public static Map<String, String> captureCurrent() {
        Map<String, String> captured = new LinkedHashMap<>();
        captureFile("sodium-options.json", captured);
        captureFile("entityculling.json", captured);
        captureFile("moreculling.toml", captured);
        captureFile("badoptimizations.txt", captured);
        captureFile("sodium-extra-options.json", captured);
        return captured;
    }

    private static void captureFile(String file, Map<String, String> captured) {
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve(file);
            if (Files.isRegularFile(path) && Files.size(path) <= MAX_CONFIG_BYTES) {
                captured.put(file, Files.readString(path, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
        }
    }

    public static void restore(Map<String, String> backups) {
        if (backups == null) return;
        for (Map.Entry<String, String> entry : backups.entrySet()) {
            String file = entry.getKey();
            if (!file.equals("sodium-options.json") && !file.equals("entityculling.json") && !file.equals("moreculling.toml")
                    && !file.equals("badoptimizations.txt") && !file.equals("sodium-extra-options.json")) continue;
            try {
                Path path = FabricLoader.getInstance().getConfigDir().resolve(file);
                if (Files.isRegularFile(path) && Files.size(path) <= MAX_CONFIG_BYTES) {
                    write(path, entry.getValue());
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static boolean installed(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    private static void tuneJson(String mod, String file, Map<String, Boolean> targets,
                                 Map<String, String> backups, List<String> changes) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(file);
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_CONFIG_BYTES) return;
            String original = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(original);
            if (!parsed.isJsonObject()) return;
            JsonObject root = parsed.getAsJsonObject();
            List<String> applied = new ArrayList<>();
            for (Map.Entry<String, Boolean> target : targets.entrySet()) {
                String[] parts = target.getKey().split("\\.");
                JsonObject section = root;
                for (int i = 0; i < parts.length - 1; i++) {
                    JsonElement next = section.get(parts[i]);
                    if (next == null || !next.isJsonObject()) {
                        section = null;
                        break;
                    }
                    section = next.getAsJsonObject();
                }
                if (section == null) continue;
                String key = parts[parts.length - 1];
                JsonElement current = section.get(key);
                if (current == null || !current.isJsonPrimitive()
                        || !current.getAsJsonPrimitive().isBoolean()
                        || current.getAsBoolean() == target.getValue()) continue;
                section.addProperty(key, target.getValue());
                applied.add(target.getKey() + " → " + (target.getValue() ? "On" : "Off"));
            }
            if (applied.isEmpty()) return;
            write(path, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            backups.putIfAbsent(file, original);
            changes.add(mod + " (restart game): " + String.join(", ", applied));
        } catch (RuntimeException | IOException ignored) {
        }
    }

    private static void tuneText(String mod, String file, Map<String, Boolean> targets,
                                 Map<String, String> backups, List<String> changes) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(file);
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_CONFIG_BYTES) return;
            String original = Files.readString(path, StandardCharsets.UTF_8);
            String updated = original;
            List<String> applied = new ArrayList<>();
            for (Map.Entry<String, Boolean> target : targets.entrySet()) {
                Pattern pattern = Pattern.compile("(?m)^([ \\t]*" + Pattern.quote(target.getKey())
                        + "[ \\t]*[:=][ \\t]*)(true|false)(?=[ \\t]*(?:[#\\r\\n]|$))");
                Matcher matcher = pattern.matcher(updated);
                if (matcher.find() && Boolean.parseBoolean(matcher.group(2)) != target.getValue()) {
                    updated = matcher.replaceFirst("$1" + target.getValue());
                    applied.add(target.getKey() + " → " + (target.getValue() ? "On" : "Off"));
                }
            }
            if (applied.isEmpty()) return;
            write(path, updated);
            backups.putIfAbsent(file, original);
            changes.add(mod + " (restart game): " + String.join(", ", applied));
        } catch (RuntimeException | IOException ignored) {
        }
    }

    private static void write(Path path, String value) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), "yogi-optimizer-", ".tmp");
        try {
            Files.writeString(temporary, value, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
