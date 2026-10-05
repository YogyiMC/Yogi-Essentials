package dev.yogi.yogiessentials.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.KeybindSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;
import dev.yogi.yogiessentials.client.setting.StringSetting;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;

public final class ConfigManager {

    private static final int CURRENT_SCHEMA_VERSION = 11;

    



    private static boolean hasSeenWelcome = false;
    private static boolean customTitleScreen = false;

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path CONFIG_DIR =
            FabricLoader
                    .getInstance()
                    .getConfigDir()
                    .resolve("yogiessentials");

    private static final Path CONFIG_PATH =
            CONFIG_DIR.resolve("config.json");

    private static final Path TEMP_PATH =
            CONFIG_DIR.resolve("config.json.tmp");

    private static final Path BACKUP_PATH =
            CONFIG_DIR.resolve("config.backup.json");

    private ConfigManager() {
    }

    
    public static boolean hasSeenWelcome() {
        return hasSeenWelcome;
    }

    
    public static boolean isCustomTitleScreenEnabled() {
        return customTitleScreen;
    }

    public static void setCustomTitleScreenEnabled(boolean enabled) {
        if (customTitleScreen == enabled) {
            return;
        }

        customTitleScreen = enabled;
        save();
    }

    public static void markWelcomeSeen() {
        if (hasSeenWelcome) {
            return;
        }

        hasSeenWelcome = true;
        save();
    }

    public static void load() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        if (!Files.exists(CONFIG_PATH)) {
            PerformanceOptimizer.importConfig(
                    new YogiConfig.OptimizerConfig()
            );
            save();
            System.out.println(
                    "[Yogi Essentials] Loaded config."
            );
            return;
        }

        YogiConfig config;

        try {
            config = readConfig(CONFIG_PATH);
        } catch (
                IOException
                        | JsonParseException exception
        ) {
            System.err.println(
                    "[Yogi Essentials] Failed to load config: "
                            + exception.getMessage()
            );

            if (restoreAndLoadBackup()) {
                return;
            }

            System.err.println(
                    "[Yogi Essentials] No valid config backup was available."
            );
            return;
        }

        applyConfigSafely(
                config,
                false
        );
    }

    public static void save() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        YogiConfig config =
                new YogiConfig();

        config.schemaVersion =
                CURRENT_SCHEMA_VERSION;

        config.hasSeenWelcome = hasSeenWelcome;
        config.customTitleScreen = customTitleScreen;

        for (
                Module module
                : YogiEssentialsClient
                .getModuleManager()
                .getModules()
        ) {
            YogiConfig.ModuleConfig moduleConfig =
                    new YogiConfig.ModuleConfig();

            moduleConfig.enabled =
                    module.isEnabled();

            for (
                    Setting<?> setting
                    : module.getSettings()
            ) {
                if (setting instanceof KeybindSetting keybindSetting) {
                    moduleConfig.numbers.put(
                            setting.getName(),
                            keybindSetting.get().doubleValue()
                    );
                    continue;
                }

                if (setting instanceof NumberSetting numberSetting) {
                    moduleConfig.numbers.put(
                            setting.getName(),
                            numberSetting.get()
                    );
                    continue;
                }

                if (setting instanceof BooleanSetting booleanSetting) {
                    moduleConfig.booleans.put(
                            setting.getName(),
                            booleanSetting.get()
                    );
                    continue;
                }

                if (setting instanceof EnumSetting<?> enumSetting) {
                    moduleConfig.enums.put(
                            setting.getName(),
                            enumSetting
                                    .get()
                                    .name()
                    );
                    continue;
                }

                if (setting instanceof ColorSetting colorSetting) {
                    moduleConfig.colors.put(
                            setting.getName(),
                            colorSetting.getArgb()
                    );
                    continue;
                }

                if (setting instanceof StringSetting stringSetting) {
                    moduleConfig.strings.put(
                            setting.getName(),
                            stringSetting.get()
                    );
                }
            }

            config.modules.put(
                    module.getName(),
                    moduleConfig
            );
        }

        config.performanceOptimizer =
                PerformanceOptimizer.exportConfig();

        try {
            Files.createDirectories(
                    CONFIG_DIR
            );

            try (
                    Writer writer =
                            Files.newBufferedWriter(
                                    TEMP_PATH
                            )
            ) {
                GSON.toJson(
                        config,
                        writer
                );
            }

            if (Files.exists(CONFIG_PATH)) {
                Files.copy(
                        CONFIG_PATH,
                        BACKUP_PATH,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            moveTempIntoPlace();

        } catch (IOException exception) {
            System.err.println(
                    "[Yogi Essentials] Failed to save config: "
                            + exception.getMessage()
            );
        }
    }

    private static YogiConfig readConfig(
            Path path
    ) throws IOException, JsonParseException {
        try (
                Reader reader =
                        Files.newBufferedReader(
                                path
                        )
        ) {
            YogiConfig config =
                    GSON.fromJson(
                            reader,
                            YogiConfig.class
                    );

            if (config == null) {
                throw new JsonParseException(
                        "Config file is empty."
                );
            }

            normalize(config);
            migrate(config);

            return config;
        }
    }

    private static void applyConfigSafely(
            YogiConfig config,
            boolean restoredBackup
    ) {
        boolean migrated =
                config.schemaVersion
                        != CURRENT_SCHEMA_VERSION;

        config.schemaVersion =
                CURRENT_SCHEMA_VERSION;

        hasSeenWelcome = config.hasSeenWelcome;
        customTitleScreen = config.customTitleScreen;

        try {
            applyModuleState(config);

            PerformanceOptimizer.importConfig(
                    config.performanceOptimizer
            );

            System.out.println(
                    "[Yogi Essentials] Loaded config."
            );

            if (restoredBackup) {
                System.out.println(
                        "[Yogi Essentials] Restored config backup."
                );
            }

            if (migrated) {
                save();
            }

        } catch (RuntimeException exception) {
            
            
            
            System.err.println(
                    "[Yogi Essentials] Failed to apply config state: "
                            + exception.getMessage()
            );
        }
    }

    private static void applyModuleState(
            YogiConfig config
    ) {
        for (
                Module module
                : YogiEssentialsClient
                .getModuleManager()
                .getModules()
        ) {
            YogiConfig.ModuleConfig moduleConfig =
                    config.modules.get(
                            module.getName()
                    );

            if (moduleConfig == null) {
                continue;
            }

            try {
            normalize(moduleConfig);

            for (
                    Setting<?> setting
                    : module.getSettings()
            ) {
                if (setting instanceof KeybindSetting keybindSetting) {
                    Double value =
                            moduleConfig.numbers.get(
                                    setting.getName()
                            );

                    if (value != null) {
                        keybindSetting.set(value.intValue());
                    }
                    continue;
                }

                if (setting instanceof NumberSetting numberSetting) {
                    Double value =
                            moduleConfig.numbers.get(
                                    setting.getName()
                            );

                    if (value != null) {
                        numberSetting.set(value);
                    }
                    continue;
                }

                if (setting instanceof BooleanSetting booleanSetting) {
                    Boolean value =
                            moduleConfig.booleans.get(
                                    setting.getName()
                            );

                    if (value != null) {
                        booleanSetting.set(value);
                    }
                    continue;
                }

                if (setting instanceof EnumSetting<?> enumSetting) {
                    String value =
                            moduleConfig.enums.get(
                                    setting.getName()
                            );

                    if (value != null) {
                        applyEnumValue(
                                enumSetting,
                                value
                        );
                    }
                    continue;
                }

                if (setting instanceof ColorSetting colorSetting) {
                    Integer value =
                            moduleConfig.colors.get(
                                    setting.getName()
                            );

                    if (value != null) {
                        colorSetting.setArgb(value);
                    }
                    continue;
                }

                if (setting instanceof StringSetting stringSetting) {
                    String value = moduleConfig.strings.get(setting.getName());
                    if (value != null) {
                        stringSetting.set(value);
                    }
                }
            }

            
            
            
            module.setEnabled(moduleConfig.enabled);
            } catch (RuntimeException moduleFailure) {
                
                
                
                System.err.println(
                        "[Yogi Essentials] Skipped applying module '"
                                + module.getName() + "': "
                                + moduleFailure.getMessage()
                );
            }
        }
    }

    private static boolean restoreAndLoadBackup() {
        if (!Files.exists(BACKUP_PATH)) {
            return false;
        }

        try {
            YogiConfig backup =
                    readConfig(BACKUP_PATH);

            Files.copy(
                    BACKUP_PATH,
                    CONFIG_PATH,
                    StandardCopyOption.REPLACE_EXISTING
            );

            applyConfigSafely(
                    backup,
                    true
            );

            return true;

        } catch (
                IOException
                        | JsonParseException exception
        ) {
            System.err.println(
                    "[Yogi Essentials] Failed to load config backup: "
                            + exception.getMessage()
            );
            return false;
        }
    }

    private static void moveTempIntoPlace()
            throws IOException {
        try {
            Files.move(
                    TEMP_PATH,
                    CONFIG_PATH,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException atomicFailure) {
            Files.move(
                    TEMP_PATH,
                    CONFIG_PATH,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private static void normalize(
            YogiConfig config
    ) {
        if (config.modules == null) {
            config.modules =
                    new HashMap<>();
        }

        if (config.performanceOptimizer == null) {
            config.performanceOptimizer =
                    new YogiConfig.OptimizerConfig();
        }

        if (config.performanceOptimizer.lastChanges == null) {
            config.performanceOptimizer.lastChanges =
                    new ArrayList<>();
        }

        if (
                config.performanceOptimizer.snapshot != null
                        && config.performanceOptimizer
                        .snapshot
                        .performanceModules == null
        ) {
            config.performanceOptimizer
                    .snapshot
                    .performanceModules =
                    new HashMap<>();
        }
    }

    private static void normalize(
            YogiConfig.ModuleConfig config
    ) {
        if (config.numbers == null) {
            config.numbers =
                    new HashMap<>();
        }

        if (config.booleans == null) {
            config.booleans =
                    new HashMap<>();
        }

        if (config.enums == null) {
            config.enums =
                    new HashMap<>();
        }

        if (config.colors == null) {
            config.colors =
                    new HashMap<>();
        }

        if (config.strings == null) {
            config.strings =
                    new HashMap<>();
        }
    }

    private static void migrate(
            YogiConfig config
    ) {
        int sourceSchemaVersion =
                config.schemaVersion;

        migrateCrosshairColors(config);

        
        
        
        
        
        
        
        if (
                sourceSchemaVersion < 4
                        && isBatch3DevelopmentConfig(config)
        ) {
            restoreProtectedVisualDefaults(config);
        }

        
        
        
        if (sourceSchemaVersion < 5) {
            migrateSmallBlockDefault(config);
            migrateIndividualArmorStyle(config);
        }

        
        
        
        
        if (sourceSchemaVersion < 6) {
            migrateInvalidModuleNumbers(config);
        }

        if (sourceSchemaVersion < 9) {
            migrateV103StabilityDefaults(config);
        }

        if (sourceSchemaVersion < 10) {
            config.customTitleScreen = false;
        }

        if (sourceSchemaVersion < 11) {
            migrateOptimizerRuntimeStack(config);
        }
    }

    private static void migrateOptimizerRuntimeStack(YogiConfig config) {
        if (config.performanceOptimizer == null
                || config.performanceOptimizer.lastRunEpochMillis <= 0L
                || config.performanceOptimizer.snapshot == null
                || config.performanceOptimizer.snapshot.performanceModules == null) {
            return;
        }

        String[] runtimeControllers = {
                "Frame Pacing / Stability Optimizer",
                "Adaptive Entity Budget",
                "High Distance Guard",
                "Memory Pressure Guard",
                "Particle Spawn Budget",
                "Entity Render Distance",
                "Dynamic Block Entity Optimizer",
                "HUD Update Optimizer",
                "Viewport Culling",
                "Render Submission Budget",
                "High Speed Chunk Guard",
                "Microstutter Guard",
                "Adaptive Chunk Pacing",
                "Render Thread Priority Guard",
                "Async Chat Index",
                "Custom Terrain Renderer"
        };

        for (String name : runtimeControllers) {
            Boolean wasEnabledBeforeOptimizer =
                    config.performanceOptimizer.snapshot.performanceModules.get(name);
            YogiConfig.ModuleConfig current = config.modules.get(name);
            if (Boolean.FALSE.equals(wasEnabledBeforeOptimizer)
                    && current != null
                    && current.enabled) {
                current.enabled = false;
            }
        }
    }

    private static void migrateV103StabilityDefaults(
            YogiConfig config
    ) {
        YogiConfig.ModuleConfig fpsRecovery =
                config.modules.get("FPS Recovery Fix");
        if (fpsRecovery != null) {
            normalize(fpsRecovery);
            fpsRecovery.booleans.put("Refresh Borderless", false);
            fpsRecovery.booleans.put("Refocus Window", false);
            fpsRecovery.booleans.put("Aggressive Fullscreen Refresh", false);
        }

        YogiConfig.ModuleConfig borderless =
                config.modules.get("Borderless Fullscreen");
        if (borderless != null) {
            borderless.enabled = false;
        }

        YogiConfig.ModuleConfig armor =
                config.modules.get("Armor Durability");
        if (armor != null) {
            normalize(armor);
            if (Boolean.TRUE.equals(armor.booleans.get("Individual Item Positions"))) {
                String[] pieces = {
                        "Helmet", "Chestplate", "Leggings", "Boots", "Tool", "Offhand"
                };
                for (String piece : pieces) {
                    armor.numbers.put(piece + " X", 0.0);
                    armor.numbers.put(piece + " Y", 0.0);
                    armor.numbers.put(piece + " Scale", 1.0);
                }
            }
        }
    }

    private static void migrateInvalidModuleNumbers(
            YogiConfig config
    ) {
        for (YogiConfig.ModuleConfig moduleConfig : config.modules.values()) {
            if (moduleConfig == null) {
                continue;
            }

            normalize(moduleConfig);
            moduleConfig.numbers.entrySet().removeIf(entry -> {
                Double value = entry.getValue();
                return value == null || !Double.isFinite(value);
            });
        }
    }


    private static void migrateSmallBlockDefault(
            YogiConfig config
    ) {
        YogiConfig.ModuleConfig smallBlock =
                config.modules.get("Small Block");

        if (smallBlock == null) {
            return;
        }

        normalize(smallBlock);
        Double x = smallBlock.numbers.get("X Adjustment");
        Double y = smallBlock.numbers.get("Y Adjustment");
        Double scale = smallBlock.numbers.get("Scale");

        boolean oldDefault =
                x != null && Math.abs(x) < 0.000001
                        && y != null && Math.abs(y) < 0.000001
                        && scale != null && Math.abs(scale - 0.92) < 0.000001;

        if (oldDefault) {
            smallBlock.numbers.put("Y Adjustment", -0.13);
        }
    }

    private static void migrateIndividualArmorStyle(
            YogiConfig config
    ) {
        YogiConfig.ModuleConfig armor =
                config.modules.get("Armor Durability");

        if (armor == null) {
            return;
        }

        normalize(armor);
        if (!Boolean.TRUE.equals(armor.booleans.get("Individual Armor Positions"))) {
            return;
        }

        
        
        armor.booleans.put("Background", false);
        armor.booleans.put("Border", false);
        armor.booleans.put("Accent Line", false);
        armor.booleans.put("Show Label", false);
    }

    private static void migrateCrosshairColors(
            YogiConfig config
    ) {
        
        
        
        YogiConfig.ModuleConfig crosshair =
                config.modules.get(
                        "Custom Crosshair"
                );

        if (crosshair == null) {
            return;
        }

        normalize(crosshair);

        if (
                !crosshair.colors.containsKey(
                        "Crosshair Color"
                )
        ) {
            migrateRgbTriplet(
                    crosshair,
                    "Red",
                    "Green",
                    "Blue",
                    "Crosshair Color"
            );
        }

        if (
                !crosshair.colors.containsKey(
                        "Outline Color"
                )
        ) {
            migrateRgbTriplet(
                    crosshair,
                    "Outline Red",
                    "Outline Green",
                    "Outline Blue",
                    "Outline Color"
            );
        }
    }

    private static boolean isBatch3DevelopmentConfig(
            YogiConfig config
    ) {
        
        
        return config.modules.containsKey(
                "Input Optimizer"
        )
                && config.modules.containsKey(
                "Spear Optimizer"
        );
    }

    private static void restoreProtectedVisualDefaults(
            YogiConfig config
    ) {
        String[] protectedModules = {
                "Low Fire",
                "Low Shield",
                "Side Shield",
                "Small Block",
                "Small Totem",
                "Item Viewmodels"
        };

        for (String moduleName : protectedModules) {
            Module module =
                    YogiEssentialsClient
                            .getModuleManager()
                            .getModules()
                            .stream()
                            .filter(candidate ->
                                    candidate
                                            .getName()
                                            .equals(moduleName)
                            )
                            .findFirst()
                            .orElse(null);

            YogiConfig.ModuleConfig moduleConfig =
                    config.modules.get(moduleName);

            if (module == null || moduleConfig == null) {
                continue;
            }

            normalize(moduleConfig);

            
            
            moduleConfig.numbers.clear();
            moduleConfig.booleans.clear();
            moduleConfig.enums.clear();
            moduleConfig.colors.clear();
            moduleConfig.strings.clear();

            for (Setting<?> setting : module.getSettings()) {
                writeDefaultValue(
                        moduleConfig,
                        setting
                );
            }
        }

        System.out.println(
                "[Yogi Essentials] Repaired Batch 3 visual defaults "
                        + "without changing module enabled states."
        );
    }

    private static void writeDefaultValue(
            YogiConfig.ModuleConfig moduleConfig,
            Setting<?> setting
    ) {
        if (setting instanceof KeybindSetting) {
            moduleConfig.numbers.put(
                    setting.getName(),
                    ((Integer) setting.getDefaultValue()).doubleValue()
            );
            return;
        }

        if (setting instanceof NumberSetting) {
            moduleConfig.numbers.put(
                    setting.getName(),
                    (Double) setting.getDefaultValue()
            );
            return;
        }

        if (setting instanceof BooleanSetting) {
            moduleConfig.booleans.put(
                    setting.getName(),
                    (Boolean) setting.getDefaultValue()
            );
            return;
        }

        if (setting instanceof EnumSetting<?>) {
            Enum<?> defaultValue =
                    (Enum<?>) setting.getDefaultValue();

            moduleConfig.enums.put(
                    setting.getName(),
                    defaultValue.name()
            );
            return;
        }

        if (setting instanceof ColorSetting) {
            moduleConfig.colors.put(
                    setting.getName(),
                    (Integer) setting.getDefaultValue()
            );
            return;
        }

        if (setting instanceof StringSetting) {
            moduleConfig.strings.put(
                    setting.getName(),
                    (String) setting.getDefaultValue()
            );
        }
    }

    private static void migrateRgbTriplet(
            YogiConfig.ModuleConfig config,
            String redName,
            String greenName,
            String blueName,
            String targetName
    ) {
        Double red =
                config.numbers.get(redName);
        Double green =
                config.numbers.get(greenName);
        Double blue =
                config.numbers.get(blueName);

        if (
                red == null
                        || green == null
                        || blue == null
        ) {
            return;
        }

        int rgb =
                (clampByte(red.intValue()) << 16)
                        | (clampByte(green.intValue()) << 8)
                        | clampByte(blue.intValue());

        config.colors.put(
                targetName,
                0xFF000000 | rgb
        );
    }

    private static int clampByte(
            int value
    ) {
        return Math.max(
                0,
                Math.min(
                        255,
                        value
                )
        );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static void applyEnumValue(
            EnumSetting enumSetting,
            String value
    ) {
        for (
                Object raw
                : enumSetting.getValues()
        ) {
            Enum enumValue =
                    (Enum) raw;

            if (
                    enumValue
                            .name()
                            .equalsIgnoreCase(
                                    value
                            )
            ) {
                enumSetting.set(
                        enumValue
                );
                return;
            }
        }
    }
}
