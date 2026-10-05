package dev.yogi.yogiessentials.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> modulesByClass = new HashMap<>();

    public void register(Module module) {
        if (module == null) return;
        modules.add(module);
        modulesByClass.put(module.getClass(), module);
    }

    public void register(Module... modules) {
        if (modules == null) return;
        for (Module module : modules) register(module);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getModulesByCategory(Category category) {
        if (category == null) return List.of();
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) result.add(module);
        }
        return List.copyOf(result);
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        if (clazz == null) return null;

        Module direct = modulesByClass.get(clazz);
        if (direct != null) return clazz.cast(direct);

        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                modulesByClass.put(clazz.asSubclass(Module.class), module);
                return clazz.cast(module);
            }
        }
        return null;
    }
}
