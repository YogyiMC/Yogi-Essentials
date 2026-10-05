package dev.yogi.yogiessentials.client.module.performance;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.config.YogiConfig;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.Setting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_12393;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_4063;
import net.minecraft.class_4066;
import net.minecraft.class_5365;
import net.minecraft.class_6597;
import net.minecraft.class_9927;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import dev.yogi.yogiessentials.client.util.FpsCapController;






public final class PerformanceOptimizer {

    public record HardwareInfo(
            int logicalProcessors,
            long physicalMemoryMb,
            long jvmMaxMemoryMb,
            String gpuVendor,
            String gpuRenderer
    ) {
        public String cpuName() {
            String name = System.getenv("PROCESSOR_IDENTIFIER");
            return name == null || name.isBlank() ? "Unavailable" : name;
        }
    }

    public record CompatibilityInfo(
            boolean sodium,
            boolean sodiumExtra,
            boolean reesesSodiumOptions,
            boolean lithium,
            boolean immediatelyFast,
            boolean moreCulling,
            boolean badOptimizations,
            boolean entityCulling,
            boolean dynamicFps,
            boolean ferriteCore,
            boolean c2me,
            boolean nvidium,
            boolean vulkanMod,
            boolean exordium,
            boolean appleSkin
    ) {
    }

    public record OptimizationResult(
            boolean success,
            String message,
            List<String> changes
    ) {
    }

    public record ImpactChange(
            String category,
            String setting,
            String currentValue,
            String optimizedValue,
            String impact,
            boolean yogiSetting
    ) {
    }

    public record OptimizationImpact(
            int minecraftSettingsChanging,
            int yogiSettingsChanging,
            List<ImpactChange> changes
    ) {
    }

    private static YogiConfig.OptimizerConfig persistedState =
            new YogiConfig.OptimizerConfig();

    /** Runtime-only proof that the user actually started Auto Optimizer this launch. */
    private static boolean autoOptimizerSessionActive;

    /** Exact state present when the current measurement run started. This is
     * separate from persistedState.snapshot so a canceled re-optimization can
     * return to the already-optimized state while preserving the original Undo
     * Optimization backup. */
    private static YogiConfig.MinecraftOptionsSnapshot measurementRollbackSnapshot;
    private static boolean reoptimizationInProgress;

    private PerformanceOptimizer() {
    }

    /**
     * Compatibility bridge for stale frame-limiter mixins. The real pacing logic
     * lives in FpsCapController so normal targets use Minecraft's native limiter
     * and only targets above Minecraft's 260-FPS sentinel use Yogi Essentials pacing.
     */
    @Deprecated
    public static void paceFrame() {
        FpsCapController.paceFrame();
    }

    public static void importConfig(
            YogiConfig.OptimizerConfig config
    ) {
        persistedState =
                config == null
                        ? new YogiConfig.OptimizerConfig()
                        : config;

        if (persistedState.lastChanges == null) {
            persistedState.lastChanges =
                    new ArrayList<>();
        }

        autoOptimizerSessionActive = false;
        measurementRollbackSnapshot = null;
        reoptimizationInProgress = false;
    }

    public static YogiConfig.OptimizerConfig exportConfig() {
        return persistedState;
    }

    public static boolean hasRestoreSnapshot() {
        return persistedState.snapshot != null;
    }

    





    public static boolean isOptimizationActive() {
        return persistedState.snapshot != null
                && persistedState.lastRunEpochMillis > 0L;
    }

    /** True while Auto Optimizer owns a pre-run snapshot but has not accepted a result yet. */
    public static boolean isMeasurementInProgress() {
        return autoOptimizerSessionActive && measurementRollbackSnapshot != null;
    }

    /** Runtime option-rewriting modules must stay passive during and after an Auto Optimizer run. */
    public static boolean optimizerOwnsRuntimeOptions() {
        return isOptimizationActive() || isMeasurementInProgress();
    }

    public static int activeTargetFps() {
        return isOptimizationActive() ? Math.max(0, persistedState.targetFps) : 0;
    }

    /** The user-selected cap applies immediately, even before an optimizer run. */
    public static int selectedTargetFps() {
        if (isMeasurementInProgress()) return 0;
        return Math.max(0, persistedState.targetFps);
    }

    public static List<String> applyFrameTarget(
            int target,
            FrameTelemetry.Sample baseline
    ) {
        class_310 client = class_310.method_1551();
        class_315 options = client.field_1690;
        List<String> changes = new ArrayList<>();
        int oldCap = options.method_42524().method_41753();
        boolean oldVsync = options.method_42433().method_41753();

        int normalizedTarget = Math.max(0, target);
        int newCap = normalizedTarget > 0 && normalizedTarget <= 260 ? normalizedTarget : 260;
        options.method_42524().method_41748(newCap);
        options.method_42433().method_41748(false);
        if (oldCap != newCap || oldVsync) {
            changes.add("Minecraft FPS limit " + (oldCap == 260 ? "Unlimited" : oldCap)
                    + " -> " + (newCap == 260 ? "Unlimited" : newCap)
                    + "; VSync " + (oldVsync ? "On" : "Off") + " -> Off");
        }
        if (normalizedTarget > 260) {
            changes.add("Yogi Essentials FPS cap: " + normalizedTarget + " FPS");
        } else if (normalizedTarget > 0) {
            changes.add("FPS cap: " + normalizedTarget + " FPS");
        } else {
            changes.add("FPS cap: Unlimited");
        }

        options.method_1640();
        FpsCapController.reset();
        return changes;
    }

    public static void setSelectedFrameTarget(int target) {
        persistedState.targetFps = Math.max(0, target);
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1690 != null) {
            int normalizedTarget = Math.max(0, target);
            int vanillaCap = normalizedTarget > 0 && normalizedTarget <= 260 ? normalizedTarget : 260;
            client.field_1690.method_42524().method_41748(vanillaCap);
            client.field_1690.method_42433().method_41748(false);
            client.field_1690.method_1640();
        }
        FpsCapController.reset();
        ConfigManager.save();
    }

    public static List<String> enableAllPerformanceModules() {
        return enablePerformanceModules();
    }

    /**
     * Auto Optimizer enables every module in the Performance category. Runtime
     * controllers that can rewrite visible options already honor
     * optimizerOwnsRuntimeOptions(), so they stay enabled for their intended hot
     * path optimizations without fighting the measured optimizer result.
     */
    public static List<String> enablePerformanceModules() {
        List<String> changes = new ArrayList<>();
        if (YogiEssentialsClient.getModuleManager() == null) return changes;

        for (Module module : YogiEssentialsClient.getModuleManager().getModulesByCategory(Category.PERFORMANCE)) {
            if (module == null) continue;
            if (!module.isEnabled()) {
                module.setEnabled(true);
                changes.add("Enabled Performance module: " + module.getName());
            }
        }

        RenderThreadPriorityGuardModule priorityGuard =
                YogiEssentialsClient.getModuleManager().getModule(RenderThreadPriorityGuardModule.class);
        if (priorityGuard != null && priorityGuard.prepareForAutoOptimizer()) {
            changes.add("Render Thread Priority Guard set to normal priority for stable frametimes");
        }

        PerformanceOptionManager.reconcileAll();

        for (Module module : YogiEssentialsClient.getModuleManager().getModulesByCategory(Category.PERFORMANCE)) {
            if (module != null && !module.isEnabled()) {
                module.setEnabled(true);
                if (module.isEnabled()) {
                    changes.add("Re-enabled Performance module: " + module.getName());
                } else {
                    System.err.println("[Yogi Essentials] Auto Optimizer could not keep Performance module enabled: " + module.getName());
                }
            }
        }

        CustomTerrainRendererModule terrainRenderer =
                YogiEssentialsClient.getModuleManager().getModule(CustomTerrainRendererModule.class);
        if (terrainRenderer != null && terrainRenderer.isEnabled()) {
            terrainRenderer.refreshBackendSelection();
            TerrainBackendManager.BackendStatus backend = terrainRenderer.getBackendStatus();
            changes.add("Terrain Backend: " + backend.backend());
            if (backend.vulkanMod()) {
                changes.add("VulkanMod retains terrain drawing ownership while all Yogi Essentials Performance modules remain enabled");
            }
        }
        return changes;
    }

    /** Restores only the optimizer-managed performance module/compatibility stack
     * to the pre-run snapshot while preserving Minecraft options already accepted
     * by measured candidate stages. Used when final validation detects regression. */
    public static void restorePerformanceStackSnapshot() {
        YogiConfig.MinecraftOptionsSnapshot snapshot = persistedState.snapshot;
        if (snapshot == null) return;
        restorePerformanceModuleStates(snapshot.performanceModules);
        restorePerformanceBooleanSettings(snapshot.performanceBooleanSettings);
        CompatibleModConfigTuner.restore(snapshot.compatibleModConfigBackups);
        CompatibleModOptimizer.restoreSodium(snapshot.sodiumPerformanceOptions);
        PerformanceOptionManager.reconcileAll();
        ConfigManager.save();
    }

    /**
     * Restores measured vanilla/third-party tuning to the exact pre-run snapshot
     * while deliberately keeping the Auto Optimizer's safe Performance stack enabled.
     * This is used when final validation finds slower tuning without re-enabling
     * every adaptive/high-frequency controller.
     */
    public static void restoreMeasuredSettingsKeepingPerformanceModules() {
        class_310 client = class_310.method_1551();
        YogiConfig.MinecraftOptionsSnapshot snapshot = persistedState.snapshot;
        if (client == null || client.field_1690 == null || snapshot == null) return;

        class_315 options = client.field_1690;
        CompatibleModConfigTuner.restore(snapshot.compatibleModConfigBackups);
        CompatibleModOptimizer.restoreSodium(snapshot.sodiumPerformanceOptions);

        class_5365 graphicsMode = enumValueOrDefault(
                class_5365.class, snapshot.graphicsMode, options.method_75329().method_41753());
        options.method_75317(graphicsMode);
        options.method_42503().method_41748(snapshot.renderDistance);
        options.method_42510().method_41748(snapshot.simulationDistance);
        options.method_42517().method_41748(snapshot.entityDistanceScaling);
        options.method_42524().method_41748(snapshot.maxFps);
        options.method_42528().method_41748(enumValueOrDefault(
                class_4063.class, snapshot.cloudRenderMode, options.method_42528().method_41753()));
        options.method_75335().method_41748(snapshot.vignette);
        options.method_41792().method_41748(snapshot.ambientOcclusion);
        options.method_42433().method_41748(snapshot.vsync);
        options.method_42435().method_41748(snapshot.entityShadows);
        options.method_42563().method_41748(snapshot.mipmapLevels);
        options.method_41805().method_41748(snapshot.biomeBlendRadius);
        options.method_42475().method_41748(enumValueOrDefault(
                class_4066.class, snapshot.particlesMode, options.method_42475().method_41753()));
        options.method_76253().method_41748(snapshot.chunkFade);

        if (snapshot.snapshotVersion >= 2) {
            options.method_75333().method_41748(snapshot.weatherRadius);
            options.method_75334().method_41748(snapshot.cutoutLeaves);
            options.method_75337().method_41748(snapshot.improvedTransparency);
            options.method_57702().method_41748(snapshot.menuBackgroundBlurriness);
            options.method_76247().method_41748(snapshot.maxAnisotropy);
            options.method_76747().method_41748(enumValueOrDefault(
                    class_12393.class, snapshot.textureFilteringMode, options.method_76747().method_41753()));
        }

        if (snapshot.snapshotVersion >= 3) {
            options.method_41798().method_41748(enumValueOrDefault(
                    class_6597.class, snapshot.chunkBuilderMode, options.method_41798().method_41753()));
            options.method_61970().method_41748(enumValueOrDefault(
                    class_9927.class, snapshot.inactivityFpsLimit, options.method_61970().method_41753()));
        }

        enableAllPerformanceModules();
        PerformanceOptionManager.reconcileAll();
        options.method_1640();
        ConfigManager.save();
    }

    public static List<String> optimizeCompatibleMods(boolean maximum) {
        if (persistedState.snapshot == null) return List.of();
        YogiConfig.MinecraftOptionsSnapshot snapshot = persistedState.snapshot;
        if (snapshot.compatibleModConfigBackups == null) {
            snapshot.compatibleModConfigBackups = new LinkedHashMap<>();
        }
        List<String> changes = CompatibleModConfigTuner.optimize(
                snapshot.compatibleModConfigBackups, maximum);
        if (!changes.isEmpty()) ConfigManager.save();
        return changes;
    }

    public static List<String> getLastChanges() {
        return List.copyOf(
                persistedState.lastChanges == null
                        ? List.of()
                        : persistedState.lastChanges
        );
    }

    public static boolean shouldShowOptimizationReminder() {
        return persistedState.lastRunEpochMillis == 0L
                && !persistedState.reminderDismissed;
    }

    public static void dismissOptimizationReminder() {
        persistedState.reminderDismissed = true;
        ConfigManager.save();
    }

    public static boolean beginMeasuredOptimization() {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1690 == null || autoOptimizerSessionActive) {
            return false;
        }

        reoptimizationInProgress = isOptimizationActive();
        measurementRollbackSnapshot = captureSnapshot(client.field_1690);
        measurementRollbackSnapshot.compatibleModConfigBackups = CompatibleModConfigTuner.captureCurrent();

        if (!reoptimizationInProgress) {
            persistedState.snapshot = measurementRollbackSnapshot;
            persistedState.lastRunEpochMillis = 0L;
        }
        autoOptimizerSessionActive = true;

        client.field_1690.method_42524().method_41748(260);
        client.field_1690.method_42433().method_41748(false);
        FpsCapController.reset();

        ConfigManager.save();
        return true;
    }

    public static OptimizationResult completeMeasuredOptimization(List<String> changes) {
        autoOptimizerSessionActive = false;
        measurementRollbackSnapshot = null;
        reoptimizationInProgress = false;
        persistedState.lastRunEpochMillis = System.currentTimeMillis();
        persistedState.reminderDismissed = true;
        persistedState.lastChanges = new ArrayList<>(changes);
        ConfigManager.save();
        return new OptimizationResult(true, "Measured optimization complete.", List.copyOf(changes));
    }

    /** Cancels only the current measurement run. If this was a re-optimization,
     * restore the exact already-optimized state and preserve the existing Undo
     * Optimization backup/status. On a first run this behaves like the normal
     * restore path and returns to the user's pre-optimizer settings. */
    public static OptimizationResult cancelMeasuredOptimization() {
        if (!autoOptimizerSessionActive || measurementRollbackSnapshot == null) {
            return new OptimizationResult(false, "No optimizer measurement is active.", List.of());
        }

        if (!reoptimizationInProgress) {
            measurementRollbackSnapshot = null;
            reoptimizationInProgress = false;
            return restorePreviousSettings();
        }

        YogiConfig.MinecraftOptionsSnapshot originalUndoSnapshot = persistedState.snapshot;
        long originalLastRun = persistedState.lastRunEpochMillis;
        boolean originalReminder = persistedState.reminderDismissed;
        List<String> originalChanges = new ArrayList<>(persistedState.lastChanges == null ? List.of() : persistedState.lastChanges);
        int originalTarget = persistedState.targetFps;

        persistedState.snapshot = measurementRollbackSnapshot;
        OptimizationResult restored = restorePreviousSettings();

        persistedState.snapshot = originalUndoSnapshot;
        persistedState.lastRunEpochMillis = originalLastRun;
        persistedState.reminderDismissed = originalReminder;
        persistedState.lastChanges = originalChanges;
        persistedState.targetFps = originalTarget;
        autoOptimizerSessionActive = false;
        measurementRollbackSnapshot = null;
        reoptimizationInProgress = false;
        ConfigManager.save();

        return new OptimizationResult(restored.success(),
                restored.success() ? "Re-optimization canceled; previous optimized settings kept." : restored.message(),
                originalChanges);
    }

    public static HardwareInfo detectHardware() {
        int logicalProcessors =
                Math.max(
                        1,
                        Runtime.getRuntime().availableProcessors()
                );

        long jvmMaxMb =
                bytesToMb(
                        Runtime.getRuntime().maxMemory()
                );

        long physicalMemoryMb =
                detectPhysicalMemoryMb();

        String gpuVendor = "Unavailable";
        String gpuRenderer = "Unavailable";

        try {
            GpuDevice device =
                    RenderSystem.tryGetDevice();

            if (device != null) {
                gpuVendor = safeText(device.getVendor());
                gpuRenderer = safeText(device.getRenderer());
            }
        } catch (Throwable ignored) {
            
            
        }

        return new HardwareInfo(
                logicalProcessors,
                physicalMemoryMb,
                jvmMaxMb,
                gpuVendor,
                gpuRenderer
        );
    }

    public static CompatibilityInfo detectCompatibility() {
        FabricLoader loader =
                FabricLoader.getInstance();

        return new CompatibilityInfo(
                loaded(loader, "sodium"),
                loadedAny(
                        loader,
                        "sodium-extra",
                        "sodium_extra"
                ),
                loadedAny(
                        loader,
                        "reeses-sodium-options",
                        "reeses_sodium_options"
                ),
                loaded(loader, "lithium"),
                loaded(loader, "immediatelyfast"),
                loaded(loader, "moreculling"),
                loaded(loader, "badoptimizations"),
                loaded(loader, "entityculling"),
                loadedAny(
                        loader,
                        "dynamic_fps",
                        "dynamicfps"
                ),
                loaded(loader, "ferritecore"),
                loaded(loader, "c2me"),
                loaded(loader, "nvidium"),
                loadedAny(
                        loader,
                        "vulkanmod",
                        "vulkan-mod"
                ),
                loaded(loader, "exordium"),
                loaded(loader, "appleskin")
        );
    }

    




    public static OptimizationImpact previewAutoOptimizerImpact() {
        class_310 client = class_310.method_1551();

        if (client == null || client.field_1690 == null) {
            return new OptimizationImpact(0, 0, List.of());
        }

        class_315 options = client.field_1690;
        HardwareInfo hardware = detectHardware();
        CompatibilityInfo compatibility = detectCompatibility();
        List<ImpactChange> changes = new ArrayList<>();

        class_5365 oldGraphics = options.method_75329().method_41753();
        addImpact(changes, "Rendering", "Graphics Mode",
                prettyEnum(oldGraphics), "Fast",
                "Benchmarks Minecraft's lower-overhead graphics preset and keeps it only when measured performance improves.",
                oldGraphics == class_5365.field_25427, false);

        int oldRender = options.method_42503().method_41753();
        int selectedTargetForRender = Math.max(0, persistedState.targetFps);
        int targetRender = Math.min(oldRender, selectedTargetForRender >= 180 ? 10 : 12);
        addImpact(changes, "Rendering", "Render Distance",
                Integer.toString(oldRender), Integer.toString(targetRender),
                "Measures a lower render distance candidate and only keeps it when it produces a reliable gain.",
                oldRender == targetRender, false);

        int oldSimulation = options.method_42510().method_41753();
        int simulationCap = oldRender >= 24 ? 6 : 8;
        int targetSimulation = Math.min(oldSimulation, Math.min(simulationCap, targetRender));
        addImpact(changes, "CPU/Chunks", "Simulation Distance",
                Integer.toString(oldSimulation), Integer.toString(targetSimulation),
                "Reduces simulation work around the player.",
                oldSimulation == targetSimulation, false);

        int oldMipmaps = options.method_42563().method_41753();
        int targetMipmaps = Math.min(oldMipmaps, 2);
        addImpact(changes, "Visual Quality", "Mipmap Levels",
                Integer.toString(oldMipmaps), Integer.toString(targetMipmaps),
                "Reduces mipmap cost without increasing the current level.",
                oldMipmaps == targetMipmaps, false);

        boolean oldVsync = options.method_42433().method_41753();
        addImpact(changes, "FPS", "VSync", onOff(oldVsync), "Off",
                "Avoids synchronization stalls and lets the selected FPS target control the cap.",
                !oldVsync, false);

        int oldFps = options.method_42524().method_41753();
        int selectedTarget = Math.max(0, persistedState.targetFps);
        int targetCap = selectedTarget > 0 && selectedTarget < 260 ? selectedTarget : 260;
        String targetCapText = targetCap == 260 ? "Unlimited" : Integer.toString(targetCap);
        addImpact(changes, "FPS", "FPS Limit",
                oldFps == 260 ? "Unlimited" : Integer.toString(oldFps), targetCapText,
                selectedTarget > 0
                        ? "Uses Minecraft's native limiter for the selected target without a second custom frame limiter."
                        : "Leaves the renderer uncapped while adaptive safeguards protect frame-time stability.",
                oldFps == targetCap, false);

        class_4066 oldParticles = options.method_42475().method_41753();
        addImpact(changes, "Rendering", "Particles",
                prettyEnum(oldParticles), "Minimal",
                "Cuts particle overdraw in busy scenes.",
                oldParticles == class_4066.field_18199, false);

        class_4063 oldClouds = options.method_42528().method_41753();
        addImpact(changes, "Rendering", "Clouds",
                prettyEnum(oldClouds), "Off",
                "Removes cloud rendering work.",
                oldClouds == class_4063.field_18162, false);

        double oldEntityDistance = options.method_42517().method_41753();
        double targetEntityDistance = Math.min(oldEntityDistance, 0.65);
        addImpact(changes, "Rendering", "Entity Distance",
                formatMultiplier(oldEntityDistance), formatMultiplier(targetEntityDistance),
                "Reduces entity render range only when it can lower render cost.",
                nearlyEqual(oldEntityDistance, targetEntityDistance), false);

        int oldBiomeBlend = options.method_41805().method_41753();
        addImpact(changes, "CPU/Chunks", "Biome Blend",
                Integer.toString(oldBiomeBlend), "0",
                "Avoids extra color blending across chunk borders.",
                oldBiomeBlend == 0, false);

        boolean oldEntityShadows = options.method_42435().method_41753();
        addImpact(changes, "Rendering", "Entity Shadows",
                onOff(oldEntityShadows), "Off",
                "Removes per-entity shadow rendering.",
                !oldEntityShadows, false);

        boolean oldAo = options.method_41792().method_41753();
        addImpact(changes, "Rendering", "Ambient Occlusion",
                onOff(oldAo), "Off",
                "Reduces smooth-lighting work.",
                !oldAo, false);

        boolean oldVignette = options.method_75335().method_41753();
        addImpact(changes, "Visual Quality", "Vignette",
                onOff(oldVignette), "Off",
                "Removes a cosmetic screen pass.",
                !oldVignette, false);

        double oldChunkFade = options.method_76253().method_41753();
        addImpact(changes, "CPU/Chunks", "Chunk Fade",
                formatMultiplier(oldChunkFade), formatMultiplier(0.0),
                "Avoids chunk fade transition work.",
                nearlyEqual(oldChunkFade, 0.0), false);

        int oldWeatherRadius = options.method_75333().method_41753();
        int targetWeatherRadius = 0;
        addImpact(changes, "Rendering", "Weather Effect Radius",
                Integer.toString(oldWeatherRadius), Integer.toString(targetWeatherRadius),
                "Limits expensive rain and snow effects without increasing the current radius.",
                oldWeatherRadius == targetWeatherRadius, false);

        boolean oldLeaves = options.method_75334().method_41753();
        addImpact(changes, "Visual Quality", "See-Through Leaves",
                onOff(oldLeaves), "Off",
                "Uses the cheaper leaf rendering path.",
                !oldLeaves, false);

        boolean oldTransparency = options.method_75337().method_41753();
        addImpact(changes, "Visual Quality", "Improved Transparency",
                onOff(oldTransparency), "Off",
                "Avoids the more expensive transparency path.",
                !oldTransparency, false);

        int oldBlur = options.method_57702().method_41753();
        addImpact(changes, "Visual Quality", "Menu Background Blur",
                Integer.toString(oldBlur), "0",
                "Removes menu blur rendering overhead.",
                oldBlur == 0, false);

        int oldAnisotropy = options.method_76247().method_41753();
        addImpact(changes, "Visual Quality", "Max Anisotropy",
                Integer.toString(oldAnisotropy), "1",
                "Reduces extra texture-sampling cost.",
                oldAnisotropy == 1, false);

        class_12393 oldFiltering = options.method_76747().method_41753();
        addImpact(changes, "Visual Quality", "Texture Filtering",
                prettyEnum(oldFiltering), "None",
                "Uses the least expensive extra texture-filtering mode.",
                oldFiltering == class_12393.field_64663, false);

        class_6597 oldChunkBuilder = options.method_41798().method_41753();
        addImpact(changes, "CPU/Chunks", "Prioritize Chunk Updates",
                prettyEnum(oldChunkBuilder), "None",
                "Avoids prioritization spikes while chunks rebuild.",
                oldChunkBuilder == class_6597.field_34788, false);

        class_9927 oldInactive = options.method_61970().method_41753();
        addImpact(changes, "FPS", "Inactive FPS Limit",
                prettyEnum(oldInactive), "Minimized",
                "Reduces work only while Minecraft is genuinely inactive.",
                oldInactive == class_9927.field_52743, false);

        addCoreModuleImpact(changes, MemoryStabilityModule.class, "Memory",
                "Cleans Yogi Essentials-owned transient state safely between worlds.");
        addCoreModuleImpact(changes, MinimalParticlesModule.class, "Rendering",
                "Keeps Yogi Essentials' particle optimization synchronized with Minecraft.");
        addCoreModuleImpact(changes, NoEntityShadowsModule.class, "Rendering",
                "Keeps entity shadows disabled through Yogi Essentials' reconciler.");
        addCoreModuleImpact(changes, NoCloudsModule.class, "Rendering",
                "Keeps cloud rendering disabled through Yogi Essentials' reconciler.");
        addCoreModuleImpact(changes, NoBiomeBlendModule.class, "CPU/Chunks",
                "Keeps biome blending disabled through Yogi Essentials' reconciler.");
        addCoreModuleImpact(changes, NoAmbientOcclusionModule.class, "Rendering",
                "Keeps ambient occlusion disabled through Yogi Essentials' reconciler.");
        addCoreModuleImpact(changes, NoVignetteModule.class, "Visual Quality",
                "Keeps the vignette disabled through Yogi Essentials' reconciler.");
        addCoreModuleImpact(changes, NoChunkFadeModule.class, "CPU/Chunks",
                "Keeps chunk fade disabled through Yogi Essentials' reconciler.");
        addCoreModuleImpact(changes, ChunkUpdateStabilizerModule.class, "CPU/Chunks",
                "Uses Minecraft's even chunk rebuild policy to reduce update bursts.");
        addCoreModuleImpact(changes, WeatherRenderBudgetModule.class, "Rendering",
                "Cuts rain/snow rendering radius to reduce overdraw.");
        addCoreModuleImpact(changes, FastLeavesModule.class, "Rendering",
                "Uses the cheaper leaf rendering path in foliage-heavy areas.");
        addCoreModuleImpact(changes, FastTransparencyModule.class, "Rendering",
                "Avoids the expensive improved-transparency path.");
        addCoreModuleImpact(changes, AnisotropicSamplingOptimizerModule.class, "Rendering",
                "Reduces optional anisotropic texture sampling overhead.");
        addCoreModuleImpact(changes, TextureFilteringOptimizerModule.class, "Rendering",
                "Uses the lowest-overhead optional texture filtering mode.");
        addCoreModuleImpact(changes, MenuBlurOptimizerModule.class, "Rendering",
                "Avoids unnecessary menu framebuffer blur passes.");
        addCoreModuleImpact(changes, SimulationDistanceOptimizerModule.class, "CPU/Chunks",
                "Separates visible render distance from the more expensive simulation radius.");
        addCoreModuleImpact(changes, ReducedMipmapModule.class, "Memory",
                "Cuts texture upload and sampling pressure by capping mipmap work.");

        int minecraftCount = 0;
        int yogiCount = 0;

        for (ImpactChange change : changes) {
            if (change.yogiSetting()) {
                yogiCount++;
            } else {
                minecraftCount++;
            }
        }

        return new OptimizationImpact(
                minecraftCount,
                yogiCount,
                List.copyOf(changes)
        );
    }

    public static OptimizationResult restorePreviousSettings() {
        class_310 client =
                class_310.method_1551();

        if (
                client == null
                        || client.field_1690 == null
        ) {
            return new OptimizationResult(
                    false,
                    "Minecraft settings are not ready yet.",
                    List.of()
            );
        }

        YogiConfig.MinecraftOptionsSnapshot snapshot =
                persistedState.snapshot;

        if (snapshot == null) {
            return new OptimizationResult(
                    false,
                    "No optimizer backup is available.",
                    List.of()
            );
        }

        class_315 options =
                client.field_1690;

        restorePerformanceModuleStates(
                snapshot.performanceModules
        );
        restorePerformanceBooleanSettings(snapshot.performanceBooleanSettings);
        CompatibleModConfigTuner.restore(snapshot.compatibleModConfigBackups);
        CompatibleModOptimizer.restoreSodium(snapshot.sodiumPerformanceOptions);

        PerformanceOptionManager.reconcileAll();

        class_5365 graphicsMode =
                enumValueOrDefault(
                        class_5365.class,
                        snapshot.graphicsMode,
                        options
                                .method_75329()
                                .method_41753()
                );

        options.method_75317(
                graphicsMode
        );

        options
                .method_42503()
                .method_41748(snapshot.renderDistance);
        options
                .method_42510()
                .method_41748(snapshot.simulationDistance);
        options
                .method_42517()
                .method_41748(snapshot.entityDistanceScaling);
        options
                .method_42524()
                .method_41748(snapshot.maxFps);
        options
                .method_42528()
                .method_41748(
                        enumValueOrDefault(
                                class_4063.class,
                                snapshot.cloudRenderMode,
                                options
                                        .method_42528()
                                        .method_41753()
                        )
                );
        options
                .method_75335()
                .method_41748(snapshot.vignette);
        options
                .method_41792()
                .method_41748(snapshot.ambientOcclusion);
        options
                .method_42433()
                .method_41748(snapshot.vsync);
        options
                .method_42435()
                .method_41748(snapshot.entityShadows);
        options
                .method_42563()
                .method_41748(snapshot.mipmapLevels);
        options
                .method_41805()
                .method_41748(snapshot.biomeBlendRadius);
        options
                .method_42475()
                .method_41748(
                        enumValueOrDefault(
                                class_4066.class,
                                snapshot.particlesMode,
                                options
                                        .method_42475()
                                        .method_41753()
                        )
                );
        options
                .method_76253()
                .method_41748(snapshot.chunkFade);

        if (snapshot.snapshotVersion >= 2) {
            options
                    .method_75333()
                    .method_41748(snapshot.weatherRadius);
            options
                    .method_75334()
                    .method_41748(snapshot.cutoutLeaves);
            options
                    .method_75337()
                    .method_41748(snapshot.improvedTransparency);
            options
                    .method_57702()
                    .method_41748(snapshot.menuBackgroundBlurriness);
            options
                    .method_76247()
                    .method_41748(snapshot.maxAnisotropy);
            options
                    .method_76747()
                    .method_41748(
                            enumValueOrDefault(
                                    class_12393.class,
                                    snapshot.textureFilteringMode,
                                    options
                                            .method_76747()
                                            .method_41753()
                            )
                    );
        }

        if (snapshot.snapshotVersion >= 3) {
            options
                    .method_41798()
                    .method_41748(
                            enumValueOrDefault(
                                    class_6597.class,
                                    snapshot.chunkBuilderMode,
                                    options
                                            .method_41798()
                                            .method_41753()
                            )
                    );
            options
                    .method_61970()
                    .method_41748(
                            enumValueOrDefault(
                                    class_9927.class,
                                    snapshot.inactivityFpsLimit,
                                    options
                                            .method_61970()
                                            .method_41753()
                            )
                    );
        }

        
        
        
        
        PerformanceOptionManager.reconcileAll();
        options.method_1640();

        persistedState.snapshot = null;
        autoOptimizerSessionActive = false;
        persistedState.lastChanges =
                new ArrayList<>();

        ConfigManager.save();

        return new OptimizationResult(
                true,
                "Previous Minecraft and Yogi Essentials Performance settings restored.",
                List.of("Restored optimizer backup.")
        );
    }

    private static YogiConfig.MinecraftOptionsSnapshot captureSnapshot(
            class_315 options
    ) {
        YogiConfig.MinecraftOptionsSnapshot snapshot =
                new YogiConfig.MinecraftOptionsSnapshot();

        snapshot.renderDistance =
                options
                        .method_42503()
                        .method_41753();
        snapshot.simulationDistance =
                options
                        .method_42510()
                        .method_41753();
        snapshot.entityDistanceScaling =
                options
                        .method_42517()
                        .method_41753();
        snapshot.maxFps =
                options
                        .method_42524()
                        .method_41753();
        snapshot.graphicsMode =
                options
                        .method_75329()
                        .method_41753()
                        .name();
        snapshot.cloudRenderMode =
                options
                        .method_42528()
                        .method_41753()
                        .name();
        snapshot.vignette =
                options
                        .method_75335()
                        .method_41753();
        snapshot.ambientOcclusion =
                options
                        .method_41792()
                        .method_41753();
        snapshot.vsync =
                options
                        .method_42433()
                        .method_41753();
        snapshot.entityShadows =
                options
                        .method_42435()
                        .method_41753();
        snapshot.mipmapLevels =
                options
                        .method_42563()
                        .method_41753();
        snapshot.biomeBlendRadius =
                options
                        .method_41805()
                        .method_41753();
        snapshot.particlesMode =
                options
                        .method_42475()
                        .method_41753()
                        .name();
        snapshot.chunkFade =
                options
                        .method_76253()
                        .method_41753();
        snapshot.snapshotVersion = 3;
        snapshot.weatherRadius =
                options
                        .method_75333()
                        .method_41753();
        snapshot.cutoutLeaves =
                options
                        .method_75334()
                        .method_41753();
        snapshot.improvedTransparency =
                options
                        .method_75337()
                        .method_41753();
        snapshot.menuBackgroundBlurriness =
                options
                        .method_57702()
                        .method_41753();
        snapshot.maxAnisotropy =
                options
                        .method_76247()
                        .method_41753();
        snapshot.textureFilteringMode =
                options
                        .method_76747()
                        .method_41753()
                        .name();
        snapshot.chunkBuilderMode =
                options
                        .method_41798()
                        .method_41753()
                        .name();
        snapshot.inactivityFpsLimit =
                options
                        .method_61970()
                        .method_41753()
                        .name();
        snapshot.performanceModules =
                currentPerformanceModuleStates();
        snapshot.performanceBooleanSettings = currentPerformanceBooleanSettings();
        snapshot.sodiumPerformanceOptions = CompatibleModOptimizer.captureSodium();
        snapshot.compatibleModConfigBackups = CompatibleModConfigTuner.captureCurrent();

        return snapshot;
    }

    private static int recommendedRenderDistance(
            HardwareInfo hardware,
            CompatibilityInfo compatibility
    ) {
        long memory =
                hardware.physicalMemoryMb() > 0
                        ? hardware.physicalMemoryMb()
                        : hardware.jvmMaxMemoryMb();

        int threads = hardware.logicalProcessors();
        int base;

        if (threads <= 4 || memory < 6_000) {
            base = 6;
        } else if (threads <= 8 || memory < 10_000) {
            base = 8;
        } else if (threads <= 12 || memory < 16_000) {
            base = 10;
        } else {
            base = 12;
        }

        
        
        
        if (compatibility.sodium() && threads >= 8 && memory >= 10_000) {
            base += 4;
        }

        
        
        
        if (compatibility.nvidium() && compatibility.sodium()
                && threads >= 12 && memory >= 16_000) {
            base += 6;
        }

        if (compatibility.vulkanMod() && threads >= 8 && memory >= 10_000) {
            base += 2;
        }

        return Math.min(24, base);
    }

    private static int recommendedSimulationDistance(
            HardwareInfo hardware,
            CompatibilityInfo compatibility
    ) {
        int target;

        if (hardware.logicalProcessors() <= 4) {
            target = 5;
        } else if (hardware.logicalProcessors() <= 8) {
            target = 6;
        } else if (hardware.logicalProcessors() <= 12) {
            target = 7;
        } else {
            target = 8;
        }

        
        
        
        if (compatibility.lithium() && compatibility.c2me()
                && hardware.logicalProcessors() >= 12) {
            target = Math.min(10, target + 1);
        }

        return target;
    }

    private static double recommendedEntityDistance(
            HardwareInfo hardware,
            CompatibilityInfo compatibility
    ) {
        long memory =
                hardware.physicalMemoryMb() > 0
                        ? hardware.physicalMemoryMb()
                        : hardware.jvmMaxMemoryMb();

        double target;

        if (hardware.logicalProcessors() <= 4 || memory < 6_000) {
            target = 0.35;
        } else if (hardware.logicalProcessors() <= 8 || memory < 10_000) {
            target = 0.45;
        } else {
            target = 0.50;
        }

        if (compatibility.sodium() && hardware.logicalProcessors() >= 8) {
            target = Math.min(0.60, target + 0.05);
        }

        if (compatibility.entityCulling() || compatibility.moreCulling()) {
            target = Math.min(0.65, target + 0.05);
        }

        return target;
    }

    private static int recommendedWeatherRadius(
            HardwareInfo hardware
    ) {
        if (hardware.logicalProcessors() <= 4) {
            return 8;
        }

        if (hardware.logicalProcessors() <= 8) {
            return 12;
        }

        return 16;
    }

    private static void appendEnvironmentRecommendations(
            List<String> changes,
            HardwareInfo hardware,
            CompatibilityInfo compatibility
    ) {
        if (hardware.jvmMaxMemoryMb() > 0 && hardware.jvmMaxMemoryMb() < 3_000) {
            changes.add(
                    "Recommendation: JVM memory is under 3 GB; launcher allocation was not changed automatically."
            );
        }

        if (!compatibility.sodium()) {
            changes.add(
                    "Compatibility note: Sodium was not detected; Yogi Essentials used vanilla-safe tuning only."
            );
        }

        if (!compatibility.lithium()) {
            changes.add(
                    "Compatibility note: Lithium was not detected; no external mod config was changed."
            );
        }

        if (compatibility.badOptimizations()) {
            changes.add(
                    "Compatibility note: BadOptimizations detected; Yogi Essentials leaves its internal tweaks managed by that mod."
            );
        }

        if (compatibility.entityCulling() || compatibility.moreCulling()) {
            changes.add(
                    "Compatibility note: culling mod detected; Yogi Essentials avoids rewriting third-party culling settings."
            );
        }

        if (compatibility.dynamicFps()) {
            changes.add(
                    "Compatibility note: Dynamic FPS detected; Yogi Essentials only limits FPS when minimized and does not override its config."
            );
        }

        if (compatibility.c2me()) {
            changes.add(
                    "Compatibility note: C2ME detected; Yogi Essentials preserves its chunk/CPU optimizations and avoids overriding its config."
            );
        }

        if (compatibility.nvidium()) {
            changes.add(
                    "Compatibility note: Nvidium detected; existing high render distance is allowed more headroom on supported NVIDIA hardware."
            );
        }

        if (compatibility.vulkanMod()) {
            changes.add(
                    "Compatibility note: VulkanMod detected; Yogi Essentials keeps VulkanMod in control of terrain/render pipelines and limits Auto Optimizer to renderer-agnostic Minecraft/Yogi settings."
            );
            if (compatibility.immediatelyFast()) {
                changes.add(
                        "Compatibility warning: VulkanMod and ImmediatelyFast are installed together. Yogi Essentials will not rewrite either renderer because this combination can conflict during shader/pipeline compilation."
                );
            }
        }

        if (compatibility.exordium()) {
            changes.add(
                    "Compatibility note: Exordium detected; Yogi Essentials keeps Exordium's GUI throttling intact and avoids rewriting its config."
            );
            if (compatibility.vulkanMod()) {
                changes.add(
                        "Compatibility warning: Exordium reports VulkanMod as incompatible; Yogi Essentials leaves both configs untouched rather than forcing a conflicting change."
                );
            }
        }

        if (!compatibility.ferriteCore()) {
            changes.add(
                    "Memory note: FerriteCore was not detected; Yogi Essentials Memory Stability only cleans Yogi Essentials session state and does not replace FerriteCore's engine memory reductions."
            );
        }
    }

    private static Map<String, Boolean> currentPerformanceModuleStates() {
        Map<String, Boolean> states =
                new LinkedHashMap<>();

        if (YogiEssentialsClient.getModuleManager() == null) {
            return states;
        }

        for (
                Module module
                : YogiEssentialsClient
                .getModuleManager()
                .getModulesByCategory(
                        Category.PERFORMANCE
                )
        ) {
            states.put(
                    module.getName(),
                    module.isEnabled()
            );
        }

        return states;
    }

    private static Map<String, Boolean> currentPerformanceBooleanSettings() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        if (YogiEssentialsClient.getModuleManager() == null) return values;
        for (Module module : YogiEssentialsClient.getModuleManager().getModulesByCategory(Category.PERFORMANCE)) {
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting toggle) {
                    values.put(module.getName() + "::" + setting.getName(), toggle.get());
                }
            }
        }
        return values;
    }

    private static void restorePerformanceBooleanSettings(Map<String, Boolean> previous) {
        if (previous == null || YogiEssentialsClient.getModuleManager() == null) return;
        for (Module module : YogiEssentialsClient.getModuleManager().getModulesByCategory(Category.PERFORMANCE)) {
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting toggle) {
                    Boolean value = previous.get(module.getName() + "::" + setting.getName());
                    if (value != null) toggle.set(value);
                }
            }
        }
    }

    private static void setCorePerformanceModulesEnabled(
            boolean enabled
    ) {
        setEnabled(
                MemoryStabilityModule.class,
                enabled
        );
        setEnabled(
                MinimalParticlesModule.class,
                enabled
        );
        setEnabled(
                NoEntityShadowsModule.class,
                enabled
        );
        setEnabled(
                NoCloudsModule.class,
                enabled
        );
        setEnabled(
                LowEntityDistanceModule.class,
                enabled
        );
        setEnabled(
                NoBiomeBlendModule.class,
                enabled
        );
        setEnabled(
                NoAmbientOcclusionModule.class,
                enabled
        );
        setEnabled(
                NoVignetteModule.class,
                enabled
        );
        setEnabled(
                NoChunkFadeModule.class,
                enabled
        );
        setEnabled(
                ChunkUpdateStabilizerModule.class,
                enabled
        );
        setEnabled(WeatherRenderBudgetModule.class, enabled);
        setEnabled(FastLeavesModule.class, enabled);
        setEnabled(FastTransparencyModule.class, enabled);
        setEnabled(AnisotropicSamplingOptimizerModule.class, enabled);
        setEnabled(TextureFilteringOptimizerModule.class, enabled);
        setEnabled(MenuBlurOptimizerModule.class, enabled);
        setEnabled(ParticleBudgetModule.class, enabled);
        setEnabled(EntityRenderDistanceModule.class, enabled);
        setEnabled(DynamicBlockEntityOptimizerModule.class, enabled);
        setEnabled(HudUpdateOptimizerModule.class, enabled);
        setEnabled(
                SimulationDistanceOptimizerModule.class,
                enabled
        );
    }

    private static <T extends Module> void setEnabled(
            Class<T> moduleClass,
            boolean enabled
    ) {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        T module =
                YogiEssentialsClient
                        .getModuleManager()
                        .getModule(moduleClass);

        if (module != null) {
            module.setEnabled(enabled);
        }
    }

    private static void restorePerformanceModuleStates(
            Map<String, Boolean> states
    ) {
        if (
                states == null
                        || YogiEssentialsClient.getModuleManager() == null
        ) {
            return;
        }

        for (
                Module module
                : YogiEssentialsClient
                .getModuleManager()
                .getModulesByCategory(
                        Category.PERFORMANCE
                )
        ) {
            Boolean enabled =
                    states.get(
                            module.getName()
                    );

            if (enabled != null) {
                module.setEnabled(enabled);
            }
        }
    }

    private static void appendModuleChanges(
            List<String> changes,
            Map<String, Boolean> before,
            Map<String, Boolean> after
    ) {
        List<String> enabled =
                new ArrayList<>();

        for (
                Map.Entry<String, Boolean> entry
                : after.entrySet()
        ) {
            boolean wasEnabled =
                    Boolean.TRUE.equals(
                            before.get(
                                    entry.getKey()
                            )
                    );

            if (
                    entry.getValue()
                            && !wasEnabled
            ) {
                enabled.add(
                        entry.getKey()
                );
            }
        }

        if (!enabled.isEmpty()) {
            changes.add(
                    "Yogi Essentials Performance Modules: enabled "
                            + String.join(
                            ", ",
                            enabled
                    )
            );
        }
    }

    private static void addImpact(
            List<ImpactChange> changes,
            String category,
            String setting,
            String currentValue,
            String optimizedValue,
            String impact,
            boolean alreadyMatches,
            boolean yogiSetting
    ) {
        if (alreadyMatches) {
            return;
        }

        changes.add(new ImpactChange(
                category,
                setting,
                currentValue,
                optimizedValue,
                impact,
                yogiSetting
        ));
    }

    private static <T extends Module> void addCoreModuleImpact(
            List<ImpactChange> changes,
            Class<T> moduleClass,
            String category,
            String impact
    ) {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        T module = YogiEssentialsClient
                .getModuleManager()
                .getModule(moduleClass);

        if (module == null || module.isEnabled()) {
            return;
        }

        changes.add(new ImpactChange(
                category,
                module.getName(),
                "Off",
                "On",
                impact,
                true
        ));
    }

    private static boolean nearlyEqual(
            double a,
            double b
    ) {
        return Math.abs(a - b) < 0.0001;
    }

    private static void setAndRecord(
            List<String> changes,
            String label,
            int before,
            int after,
            Runnable setter
    ) {
        if (before == after) {
            return;
        }

        setter.run();

        changes.add(
                label
                        + ": "
                        + before
                        + " → "
                        + after
        );
    }

    private static void recordChange(
            List<String> changes,
            String label,
            String before,
            String after
    ) {
        if (before.equals(after)) {
            return;
        }

        changes.add(
                label
                        + ": "
                        + before
                        + " → "
                        + after
        );
    }

    private static String onOff(
            boolean value
    ) {
        return value
                ? "On"
                : "Off";
    }

    private static String formatMultiplier(
            double value
    ) {
        return String.format(
                Locale.ROOT,
                "%.2f",
                value
        );
    }

    private static boolean loaded(
            FabricLoader loader,
            String modId
    ) {
        return loader.isModLoaded(modId);
    }

    private static boolean loadedAny(
            FabricLoader loader,
            String... modIds
    ) {
        for (String modId : modIds) {
            if (loader.isModLoaded(modId)) {
                return true;
            }
        }

        return false;
    }

    private static long detectPhysicalMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean bean =
                    ManagementFactory.getOperatingSystemMXBean();

            if (
                    bean
                            instanceof com.sun.management.OperatingSystemMXBean sunBean
            ) {
                return bytesToMb(
                        sunBean.getTotalMemorySize()
                );
            }
        } catch (Throwable ignored) {
        }

        return -1L;
    }

    private static long bytesToMb(
            long bytes
    ) {
        if (bytes <= 0L) {
            return -1L;
        }

        return Math.max(
                1L,
                bytes / (1024L * 1024L)
        );
    }

    private static String safeText(
            String value
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return "Unavailable";
        }

        return value.trim();
    }

    private static String prettyEnum(
            Enum<?> value
    ) {
        String raw =
                value
                        .name()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return Character.toUpperCase(
                raw.charAt(0)
        )
                + raw.substring(1);
    }

    private static <E extends Enum<E>> E enumValueOrDefault(
            Class<E> enumClass,
            String name,
            E fallback
    ) {
        if (name == null) {
            return fallback;
        }

        try {
            return Enum.valueOf(
                    enumClass,
                    name
            );
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
