package dev.yogi.yogiessentials.client.module.optimizations;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import net.minecraft.class_1792;
import net.minecraft.class_310;
















public abstract class OptimizationModule extends Module {

    protected OptimizationModule(String name, String description) {
        super(name, description, Category.OPTIMIZATIONS);
    }

    protected static boolean holding(class_310 client, class_1792 item) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        return client.field_1724.method_6047().method_31574(item)
                || client.field_1724.method_6079().method_31574(item);
    }
}
