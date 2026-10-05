package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_310;
import net.minecraft.class_315;

public final class ChatSizeModule extends Module {
    private final NumberSetting scale = new VanillaChatScaleSetting();
    private Double previousVanillaScale;

    public ChatSizeModule() {
        super("Chat Size", "Change Minecraft's built-in Chat Scale setting.", Category.CHAT);
        addSetting(scale);
    }

    @Override
    protected void onEnable() {
        class_315 options = options();
        if (options == null) return;
        if (previousVanillaScale == null) {
            previousVanillaScale = options.method_42554().method_41753();
        }
        applyToVanilla(scale.get());
    }

    @Override
    protected void onDisable() {
        class_315 options = options();
        if (options != null && previousVanillaScale != null) {
            options.method_42554().method_41748(previousVanillaScale);
            options.method_1640();
        }
        previousVanillaScale = null;
    }

    @Override
    protected void onSettingsReset() {
        if (isEnabled()) {
            applyToVanilla(scale.get());
        }
    }

    public NumberSetting getScale() {
        return scale;
    }

    private static class_315 options() {
        class_310 client = class_310.method_1551();
        return client == null ? null : client.field_1690;
    }

    private static void applyToVanilla(double percent) {
        class_315 options = options();
        if (options == null) {
            return;
        }
        double vanillaScale = Math.max(0.0, Math.min(1.0, percent / 100.0));
        options.method_42554().method_41748(vanillaScale);
        options.method_1640();
    }

    private final class VanillaChatScaleSetting extends NumberSetting {
        private VanillaChatScaleSetting() {
            super("Chat Scale (%)", 100.0, 0.0, 100.0, 5.0);
        }

        @Override
        public void set(Double value) {
            super.set(value);
            if (ChatSizeModule.this.isEnabled()) {
                applyToVanilla(get());
            }
        }
    }
}
