package dev.yogi.yogiessentials.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.smp.WaypointModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_12249;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_765;
import net.minecraft.class_7833;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public final class WaypointManager {

    public static final class Waypoint {
        public String id = UUID.randomUUID().toString();
        public String name = "Waypoint";
        public String dimension = "minecraft:overworld";
        public double x;
        public double y;
        public double z;
        public int color = 0xFFFF6A00;
        public boolean enabled = true;

        public Waypoint copy() {
            Waypoint copy = new Waypoint();
            copy.id = id;
            copy.name = name;
            copy.dimension = dimension;
            copy.x = x;
            copy.y = y;
            copy.z = z;
            copy.color = color;
            copy.enabled = enabled;
            return copy;
        }
    }

    private static final class Store {
        List<Waypoint> waypoints = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("yogiessentials")
            .resolve("waypoints.json");

    private static Store store = new Store();
    private static boolean initialized;

    private WaypointManager() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        load();
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(WaypointManager::render);
    }

    public static synchronized void load() {
        if (!Files.exists(PATH)) {
            store = new Store();
            return;
        }

        try (Reader reader = Files.newBufferedReader(PATH)) {
            Store read = GSON.fromJson(reader, Store.class);
            store = read == null ? new Store() : read;
            if (store.waypoints == null) store.waypoints = new ArrayList<>();
            sanitize();
        } catch (Exception ex) {
            System.err.println("[Yogi Essentials] Failed to load waypoints: " + ex.getMessage());
            store = new Store();
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(store, writer);
            }
        } catch (IOException ex) {
            System.err.println("[Yogi Essentials] Failed to save waypoints: " + ex.getMessage());
        }
    }

    private static void sanitize() {
        store.waypoints.removeIf(w -> w == null);
        for (Waypoint waypoint : store.waypoints) {
            if (waypoint.id == null || waypoint.id.isBlank()) waypoint.id = UUID.randomUUID().toString();
            if (waypoint.name == null || waypoint.name.isBlank()) waypoint.name = "Waypoint";
            if (waypoint.dimension == null || waypoint.dimension.isBlank()) waypoint.dimension = "minecraft:overworld";
            waypoint.color |= 0xFF000000;
        }
    }

    public static String currentDimension() {
        class_310 client = class_310.method_1551();
        if (client.field_1687 == null) return "minecraft:overworld";
        return client.field_1687.method_27983().method_29177().toString();
    }

    public static String prettyDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) return "Unknown";
        int colon = dimension.indexOf(':');
        String raw = colon >= 0 ? dimension.substring(colon + 1) : dimension;
        raw = raw.replace('_', ' ');
        StringBuilder out = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (!out.isEmpty()) out.append(' ');
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    public static synchronized List<Waypoint> getForDimension(String dimension) {
        return store.waypoints.stream()
                .filter(w -> dimension.equals(w.dimension))
                .sorted(Comparator.comparing(w -> w.name.toLowerCase(Locale.ROOT)))
                .map(Waypoint::copy)
                .toList();
    }

    public static synchronized List<String> knownDimensions() {
        List<String> result = new ArrayList<>();
        String current = currentDimension();
        result.add(current);
        for (Waypoint w : store.waypoints) {
            if (!result.contains(w.dimension)) result.add(w.dimension);
        }
        return result;
    }


    public static synchronized Waypoint addDeathWaypoint() {
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1687 == null) return null;

        long existingDeaths = store.waypoints.stream()
                .filter(w -> w != null && w.name != null && w.name.startsWith("Death"))
                .count();
        Waypoint waypoint = new Waypoint();
        waypoint.name = existingDeaths == 0 ? "Death" : "Death " + (existingDeaths + 1);
        waypoint.dimension = currentDimension();
        waypoint.x = client.field_1724.method_23317();
        waypoint.y = client.field_1724.method_23318();
        waypoint.z = client.field_1724.method_23321();
        waypoint.color = 0xFFFF3B30;
        store.waypoints.add(waypoint);
        save();
        return waypoint.copy();
    }

    public static synchronized Waypoint addCurrent(String name) {
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1687 == null) return null;
        Waypoint waypoint = new Waypoint();
        waypoint.name = name == null || name.isBlank() ? "Waypoint " + (getForDimension(currentDimension()).size() + 1) : name;
        waypoint.dimension = currentDimension();
        waypoint.x = client.field_1724.method_23317();
        waypoint.y = client.field_1724.method_23318();
        waypoint.z = client.field_1724.method_23321();
        store.waypoints.add(waypoint);
        save();
        return waypoint.copy();
    }

    public static synchronized void upsert(Waypoint updated) {
        if (updated == null) return;
        if (updated.id == null || updated.id.isBlank()) updated.id = UUID.randomUUID().toString();
        updated.name = updated.name == null || updated.name.isBlank() ? "Waypoint" : updated.name.trim();
        updated.dimension = updated.dimension == null || updated.dimension.isBlank() ? currentDimension() : updated.dimension;
        updated.color |= 0xFF000000;
        for (int i = 0; i < store.waypoints.size(); i++) {
            if (updated.id.equals(store.waypoints.get(i).id)) {
                store.waypoints.set(i, updated.copy());
                save();
                return;
            }
        }
        store.waypoints.add(updated.copy());
        save();
    }

    public static synchronized void remove(String id) {
        if (id == null) return;
        store.waypoints.removeIf(w -> id.equals(w.id));
        save();
    }

    public static synchronized void toggle(String id) {
        for (Waypoint waypoint : store.waypoints) {
            if (id != null && id.equals(waypoint.id)) {
                waypoint.enabled = !waypoint.enabled;
                save();
                return;
            }
        }
    }

    private static WaypointModule module() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        return YogiEssentialsClient.getModuleManager().getModule(WaypointModule.class);
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        class_310 client = class_310.method_1551();
        WaypointModule module = module();
        if (module == null || !module.isEnabled() || client.field_1687 == null || client.field_1724 == null) return;

        String dimension = currentDimension();
        List<Waypoint> waypoints = new ArrayList<>(getForDimension(dimension));

        
        
        
        if (isNetherDimension(dimension)
                && module.getShowOverworldInNether().get()) {
            
            
            synchronized (WaypointManager.class) {
                for (Waypoint source : store.waypoints) {
                    if (!source.enabled || !isOverworldDimension(source.dimension)) continue;
                    Waypoint projected = source.copy();
                    
                    
                    
                    
                    
                    projected.x = source.x / 8.0;
                    projected.y = source.y;
                    projected.z = source.z / 8.0;
                    
                    
                    waypoints.add(projected);
                }
            }
        }
        if (waypoints.isEmpty()) return;

        class_4184 camera = client.field_1773.method_19418();
        class_243 cameraPos = camera.method_71156();
        class_4587 matrices = context.matrices();
        class_4597 consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        double maxDistance = module.getMaxDistance().get();
        for (Waypoint waypoint : waypoints) {
            if (!waypoint.enabled) continue;
            double dx = waypoint.x - client.field_1724.method_23317();
            double dy = waypoint.y - client.field_1724.method_23318();
            double dz = waypoint.z - client.field_1724.method_23321();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > maxDistance) continue;

            renderMarker(context, waypoint, module, cameraPos);
            if (module.getShowLabels().get()) {
                renderLabel(client, matrices, consumers, camera, cameraPos, waypoint, distance, module);
            }
        }
    }

    private static void renderMarker(
            net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context,
            Waypoint waypoint,
            WaypointModule module,
            class_243 cameraPos
    ) {
        class_4587 matrices = context.matrices();
        class_4597 consumers = context.consumers();
        class_4588 lines = consumers.method_73477(class_12249.field_64042);
        class_4587.class_4665 entry = matrices.method_23760();

        double x = waypoint.x - cameraPos.field_1352;
        double y = waypoint.y - cameraPos.field_1351 + 0.05;
        double z = waypoint.z - cameraPos.field_1350;
        double radius = module.getMarkerSize().get();
        int color = waypoint.color | 0xFF000000;

        final int segments = 36;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2.0 * i / segments;
            double a2 = Math.PI * 2.0 * (i + 1) / segments;
            emitLine(lines, entry,
                    x + Math.cos(a1) * radius, y, z + Math.sin(a1) * radius,
                    x + Math.cos(a2) * radius, y, z + Math.sin(a2) * radius,
                    color, 2.0F);
        }

        emitLine(lines, entry, x - radius, y, z, x + radius, y, z, color, 2.0F);
        emitLine(lines, entry, x, y, z - radius, x, y, z + radius, color, 2.0F);

        if (module.getShowBeam().get()) {
            class_310 client = class_310.method_1551();
            double topY = client.field_1687 == null ? waypoint.y + 320.0 : client.field_1687.method_31600() + 1.0;
            double beamTop = topY - cameraPos.field_1351;
            emitLine(lines, entry, x, y, z, x, beamTop, z, color, 3.0F);
        }
    }

    private static void emitLine(
            class_4588 consumer,
            class_4587.class_4665 entry,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            int color,
            float width
    ) {
        Matrix4f matrix = entry.method_23761();
        consumer.method_22918(matrix, (float) x1, (float) y1, (float) z1)
                .method_39415(color)
                .method_60831(entry, 0.0F, 1.0F, 0.0F)
                .method_75298(width);
        consumer.method_22918(matrix, (float) x2, (float) y2, (float) z2)
                .method_39415(color)
                .method_60831(entry, 0.0F, 1.0F, 0.0F)
                .method_75298(width);
    }

    private static void renderLabel(
            class_310 client,
            class_4587 matrices,
            class_4597 consumers,
            class_4184 camera,
            class_243 cameraPos,
            Waypoint waypoint,
            double distance,
            WaypointModule module
    ) {
        String text = waypoint.name;
        if (module.getShowDistance().get()) {
            text += "  " + Math.round(distance) + "m";
        }

        double rx = waypoint.x - cameraPos.field_1352;
        double ry = waypoint.y - cameraPos.field_1351 + 1.35;
        double rz = waypoint.z - cameraPos.field_1350;

        matrices.method_22903();
        matrices.method_22904(rx, ry, rz);
        matrices.method_22907(class_7833.field_40716.rotationDegrees(-camera.method_19330()));
        matrices.method_22907(class_7833.field_40714.rotationDegrees(camera.method_19329()));
        
        
        
        
        
        final double referenceDistance = 16.0;
        double distanceCompensation = Math.max(1.0, distance / referenceDistance);
        double maxDistance = Math.max(referenceDistance + 1.0, module.getMaxDistance().get());
        double growthProgress = Math.log1p(Math.max(0.0, distance - referenceDistance))
                / Math.log1p(maxDistance - referenceDistance);
        growthProgress = Math.max(0.0, Math.min(1.0, growthProgress));
        double subtleGrowth = 1.0 + 0.15 * growthProgress;
        float labelScale = (float) (0.020 * distanceCompensation * subtleGrowth
                * module.getLabelSize().get());
        matrices.method_22905(-labelScale, -labelScale, labelScale);

        int textWidth = client.field_1772.method_1727(text);
        int background = module.getThroughWalls().get() ? 0x66000000 : 0x88000000;
        class_327.class_6415 layer = module.getThroughWalls().get()
                ? class_327.class_6415.field_33994
                : class_327.class_6415.field_33993;

        client.field_1772.method_27521(
                text,
                -textWidth / 2.0F,
                0.0F,
                waypoint.color | 0xFF000000,
                true,
                matrices.method_23760().method_23761(),
                consumers,
                layer,
                background,
                class_765.field_32767
        );
        matrices.method_22909();
    }
    private static boolean isNetherDimension(String dimension) {
        if (dimension == null) return false;
        String d = dimension.trim().toLowerCase(Locale.ROOT);
        return d.equals("minecraft:the_nether") || d.equals("the_nether") || d.equals("nether");
    }

    private static boolean isOverworldDimension(String dimension) {
        if (dimension == null) return false;
        String d = dimension.trim().toLowerCase(Locale.ROOT);
        return d.equals("minecraft:overworld") || d.equals("overworld");
    }

}
