package dev.yogi.yogiessentials.client.module;

import dev.yogi.yogiessentials.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;

    private final List<Setting<?>> settings =
            new ArrayList<>();

    private boolean enabled;

    protected Module(
            String name,
            String description,
            Category category
    ) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(
            boolean enabled
    ) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;

        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    protected void addSetting(
            Setting<?> setting
    ) {
        settings.add(setting);
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(
                settings
        );
    }

    public boolean isSettingVisible(
            Setting<?> setting
    ) {
        return true;
    }

    public void resetToDefaults() {
        for (
                Setting<?> setting
                : settings
        ) {
            setting.resetToDefault();
        }

        onSettingsReset();
    }

    protected void onSettingsReset() {
    }
}
