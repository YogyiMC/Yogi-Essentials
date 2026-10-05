package dev.yogi.yogiessentials.client.module.performance;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Chooses the terrain-renderer integration path for Yogi Essentials.
 * Renderer replacements keep ownership of terrain drawing while Yogi Essentials keeps its
 * scheduling, culling, workload budgeting, telemetry and compatible tuning.
 */
public final class TerrainBackendManager {
    public enum RequestedMode {
        AUTO("Auto"),
        YOGI_BACKEND("Yogi Essentials Backend"),
        COMPATIBILITY("Compatibility");

        private final String label;
        RequestedMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum EffectiveBackend {
        DISABLED("Disabled"),
        YOGI_OPENGL("Yogi Essentials OpenGL Backend"),
        SODIUM_YOGI("Sodium + Yogi Essentials Compatibility Layer"),
        NVIDIUM_YOGI("Nvidium + Yogi Essentials Compatibility Layer"),
        VULKAN_YOGI("VulkanMod + Yogi Essentials Compatibility Layer"),
        COMPATIBILITY("Renderer Compatibility Layer");

        private final String label;
        EffectiveBackend(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public record BackendStatus(
            EffectiveBackend backend,
            boolean sodium,
            boolean nvidium,
            boolean vulkanMod,
            boolean immediatelyFast,
            boolean moreCulling,
            boolean entityCulling,
            String reason
    ) {}

    private static volatile BackendStatus lastStatus = detect(RequestedMode.AUTO, false);

    private TerrainBackendManager() {}

    public static BackendStatus resolve(RequestedMode requested, boolean enabled) {
        lastStatus = detect(requested == null ? RequestedMode.AUTO : requested, enabled);
        return lastStatus;
    }

    public static BackendStatus status() {
        return lastStatus;
    }

    public static boolean ownsVanillaTerrain() {
        return lastStatus.backend() == EffectiveBackend.YOGI_OPENGL;
    }

    public static boolean externalRendererOwnsTerrain() {
        return lastStatus.backend() == EffectiveBackend.SODIUM_YOGI
                || lastStatus.backend() == EffectiveBackend.NVIDIUM_YOGI
                || lastStatus.backend() == EffectiveBackend.VULKAN_YOGI;
    }

    private static BackendStatus detect(RequestedMode requested, boolean enabled) {
        FabricLoader loader = FabricLoader.getInstance();
        boolean sodium = loader.isModLoaded("sodium");
        boolean nvidium = loader.isModLoaded("nvidium");
        boolean vulkan = loader.isModLoaded("vulkanmod") || loader.isModLoaded("vulkan-mod");
        boolean immediatelyFast = loader.isModLoaded("immediatelyfast");
        boolean moreCulling = loader.isModLoaded("moreculling");
        boolean entityCulling = loader.isModLoaded("entityculling");

        if (!enabled) {
            return new BackendStatus(EffectiveBackend.DISABLED, sodium, nvidium, vulkan,
                    immediatelyFast, moreCulling, entityCulling,
                    "Custom terrain renderer module is disabled.");
        }

        if (vulkan) {
            return new BackendStatus(EffectiveBackend.VULKAN_YOGI, sodium, nvidium, true,
                    immediatelyFast, moreCulling, entityCulling,
                    "VulkanMod owns terrain drawing; Yogi Essentials keeps compatible scheduling, culling and workload optimizations active.");
        }
        if (nvidium) {
            return new BackendStatus(EffectiveBackend.NVIDIUM_YOGI, sodium, true, false,
                    immediatelyFast, moreCulling, entityCulling,
                    "Nvidium owns Sodium's terrain backend; Yogi Essentials keeps its compatible optimization layer active.");
        }
        if (sodium) {
            return new BackendStatus(EffectiveBackend.SODIUM_YOGI, true, false, false,
                    immediatelyFast, moreCulling, entityCulling,
                    "Sodium owns terrain drawing; Yogi Essentials augments it instead of double-rendering terrain.");
        }

        if (requested == RequestedMode.COMPATIBILITY) {
            return new BackendStatus(EffectiveBackend.COMPATIBILITY, false, false, false,
                    immediatelyFast, moreCulling, entityCulling,
                    "Compatibility mode keeps vanilla terrain ownership while Yogi Essentials applies surrounding optimizations.");
        }

        return new BackendStatus(EffectiveBackend.YOGI_OPENGL, false, false, false,
                immediatelyFast, moreCulling, entityCulling,
                requested == RequestedMode.YOGI_BACKEND
                        ? "Yogi Essentials backend explicitly selected."
                        : "No conflicting terrain renderer detected; Yogi Essentials backend selected automatically.");
    }
}
