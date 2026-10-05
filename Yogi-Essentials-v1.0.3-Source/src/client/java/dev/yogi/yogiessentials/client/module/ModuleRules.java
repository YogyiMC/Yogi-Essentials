package dev.yogi.yogiessentials.client.module;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.module.visual.LowShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SideShieldModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleRules {
    public record Rule(List<Class<? extends Module>> conflictsWith,
                       List<Class<? extends Module>> requires,
                       List<Class<? extends Module>> recommendedWith,
                       List<Class<? extends Module>> automaticallyDisables) {
    }

    private static final Rule EMPTY = new Rule(List.of(), List.of(), List.of(), List.of());
    private static final Map<Class<? extends Module>, Rule> RULES = new HashMap<>();

    static {
        register(SideShieldModule.class, new Rule(List.of(LowShieldModule.class),
                List.of(), List.of(), List.of()));
        register(LowShieldModule.class, new Rule(List.of(SideShieldModule.class),
                List.of(), List.of(), List.of()));
    }

    private ModuleRules() {
    }

    public static void register(Class<? extends Module> type, Rule rule) {
        RULES.put(type, rule);
    }

    public static Rule forModule(Module module) {
        return RULES.getOrDefault(module.getClass(), EMPTY);
    }

    public static List<Module> activeConflicts(Module module) {
        ModuleManager manager = YogiEssentialsClient.getModuleManager();
        return forModule(module).conflictsWith().stream().map(type -> (Module) manager.getModule(type))
                .filter(other -> other != null && other.isEnabled()).toList();
    }

    public static boolean canEnable(Module module) {
        ModuleManager manager = YogiEssentialsClient.getModuleManager();
        return forModule(module).requires().stream()
                .map(manager::getModule).allMatch(other -> other != null && other.isEnabled());
    }

    public static void enable(Module module) {
        if (!canEnable(module)) {
            return;
        }
        ModuleManager manager = YogiEssentialsClient.getModuleManager();
        for (Module conflict : activeConflicts(module)) {
            conflict.setEnabled(false);
        }
        for (Class<? extends Module> type : forModule(module).automaticallyDisables()) {
            Module other = manager.getModule(type);
            if (other != null) {
                other.setEnabled(false);
            }
        }
        module.setEnabled(true);
        ConfigManager.save();
    }
}
