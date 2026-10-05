package dev.yogi.yogiessentials.client.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YogiConfig {

    public int schemaVersion = 11;

    




    public boolean hasSeenWelcome = false;

    public boolean customTitleScreen = false;

    public Map<String, ModuleConfig> modules =
            new HashMap<>();

    public OptimizerConfig performanceOptimizer =
            new OptimizerConfig();

    public static class ModuleConfig {

        public boolean enabled;

        public Map<String, Double> numbers =
                new HashMap<>();

        public Map<String, Boolean> booleans =
                new HashMap<>();

        public Map<String, String> enums =
                new HashMap<>();

        public Map<String, Integer> colors =
                new HashMap<>();

        public Map<String, String> strings =
                new HashMap<>();
    }

    public static class OptimizerConfig {
        public int targetFps = 0;
        public long lastRunEpochMillis;
        public boolean reminderDismissed;
        public List<String> lastChanges =
                new ArrayList<>();
        public MinecraftOptionsSnapshot snapshot;
    }

    public static class MinecraftOptionsSnapshot {
        



        public int snapshotVersion = 3;

        public int renderDistance;
        public int simulationDistance;
        public double entityDistanceScaling;
        public int maxFps;
        public String graphicsMode;
        public String cloudRenderMode;
        public boolean vignette;
        public boolean ambientOcclusion;
        public boolean vsync;
        public boolean entityShadows;
        public int mipmapLevels;
        public int biomeBlendRadius;
        public String particlesMode;
        public double chunkFade;

        
        public int weatherRadius;
        public boolean cutoutLeaves;
        public boolean improvedTransparency;
        public int menuBackgroundBlurriness;
        public int maxAnisotropy;
        public String textureFilteringMode;

        
        public String chunkBuilderMode;
        public String inactivityFpsLimit;

        public Map<String, Boolean> performanceModules =
                new HashMap<>();
        public Map<String, Boolean> sodiumPerformanceOptions = new HashMap<>();
        public Map<String, Boolean> performanceBooleanSettings = new HashMap<>();
        public Map<String, String> compatibleModConfigBackups = new HashMap<>();
    }
}
