package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.ModuleRules;
import java.util.stream.Collectors;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

public final class ModuleConflictScreen extends class_437 {
    private final class_437 parent;
    private final Module module;

    public ModuleConflictScreen(class_437 parent, Module module) {
        super(class_2561.method_43470("Module conflict"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float deltaTicks) {
        int x = Math.max(4, (field_22789 - 360) / 2);
        int y = Math.max(4, (field_22790 - 150) / 2);
        int w = Math.min(360, field_22789 - 8);
        context.method_25294(0, 0, field_22789, field_22790, 0x9A000000);
        context.method_25294(x, y, x + w, y + 150, 0xF018181E);
        context.method_25294(x, y, x + w, y + 3, 0xFFFF6A00);
        context.method_25294(x, y + 149, x + w, y + 150, 0xFFFF6A00);
        context.method_25294(x, y, x + 1, y + 150, 0xFFFF6A00);
        context.method_25294(x + w - 1, y, x + w, y + 150, 0xFFFF6A00);
        String conflicts = ModuleRules.activeConflicts(module).stream()
                .map(Module::getName).collect(Collectors.joining(", "));
        context.method_25303(field_22793, "MODULE CONFLICT", x + 12, y + 14, 0xFFFF6A00);
        context.method_51433(field_22793, field_22793.method_27523(module.getName() + " conflicts with",
                w - 24), x + 12, y + 39, 0xFFF0F0F0, true);
        context.method_51433(field_22793, field_22793.method_27523(conflicts, w - 24),
                x + 12, y + 55, 0xFFF0F0F0, true);
        context.method_51433(field_22793, field_22793.method_27523(
                "Continue will disable the conflicting module.", w - 24),
                x + 12, y + 75, 0xFFAAAAAA, true);
        context.method_25294(x + 12, y + 110, x + 120, y + 136, 0xFFFF6A00);
        context.method_25294(x + w - 120, y + 110, x + w - 12, y + 136, 0xFF383840);
        context.method_25303(field_22793, "Continue", x + 35, y + 119, 0xFFFFFFFF);
        context.method_25303(field_22793, "Cancel", x + w - 86, y + 119, 0xFFFFFFFF);
    }

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubled);
        }
        int x = Math.max(4, (field_22789 - 360) / 2);
        int y = Math.max(4, (field_22790 - 150) / 2);
        int w = Math.min(360, field_22789 - 8);
        if (click.comp_4799() >= y + 110 && click.comp_4799() < y + 136) {
            if (click.comp_4798() >= x + 12 && click.comp_4798() < x + 120) {
                ModuleRules.enable(module);
                method_25419();
                return true;
            }
            if (click.comp_4798() >= x + w - 120 && click.comp_4798() < x + w - 12) {
                method_25419();
                return true;
            }
        }
        return super.method_25402(click, doubled);
    }

    @Override
    public void method_25419() {
        if (field_22787 != null) {
            field_22787.method_1507(parent);
        }
    }
}
