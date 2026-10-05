package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

/**
 * Customizes Minecraft's built-in F3+B debug hitboxes without creating replacement boxes.
 */
public final class CombatHitboxesModule extends Module {
    private final BooleanSetting players = new BooleanSetting("Show Player Hitboxes", true);
    private final BooleanSetting hostileMobs = new BooleanSetting("Show Hostile Mob Hitboxes", true);
    private final BooleanSetting passiveMobs = new BooleanSetting("Show Passive Mob Hitboxes", true);
    private final BooleanSetting otherMobs = new BooleanSetting("Show Other Mob Hitboxes", true);
    private final BooleanSetting itemDrops = new BooleanSetting("Show Item Drop Hitboxes", true);
    private final BooleanSetting experienceOrbs = new BooleanSetting("Show Experience Orb Hitboxes", true);
    private final BooleanSetting projectiles = new BooleanSetting("Show Projectile / Throwable Hitboxes", true);
    private final BooleanSetting vehicles = new BooleanSetting("Show Vehicle Hitboxes", true);
    private final BooleanSetting otherEntities = new BooleanSetting("Show Other Entity Hitboxes", true);

    private final ColorSetting playerColor = new ColorSetting("Player Hitbox Color", 0xFFFFFFFF);
    private final ColorSetting otherEntityColor = new ColorSetting("Other Entity Hitbox Color", 0xFFFFFFFF);

    private final BooleanSetting showLookDirection = new BooleanSetting("Show Look Direction", false);
    private final BooleanSetting showEyeHeight = new BooleanSetting("Show Eye Height", false);
    private final BooleanSetting showServerPosition = new BooleanSetting("Show Server Position", false);
    private final BooleanSetting showExtraDirectionLines = new BooleanSetting("Show Extra Debug Lines", false);
    private final BooleanSetting showDebugCircles = new BooleanSetting("Show Debug Circles", false);
    private final BooleanSetting showDebugPoints = new BooleanSetting("Show Debug Points", false);

    public CombatHitboxesModule() {
        super(
                "Hitboxes",
                "Customizes Minecraft's built-in F3+B hitboxes. Choose exactly which entity groups and helper visuals are shown. This never changes collision or reach.",
                Category.PVP
        );
        addSetting(players);
        addSetting(hostileMobs);
        addSetting(passiveMobs);
        addSetting(otherMobs);
        addSetting(itemDrops);
        addSetting(experienceOrbs);
        addSetting(projectiles);
        addSetting(vehicles);
        addSetting(otherEntities);
        addSetting(playerColor);
        addSetting(otherEntityColor);
        addSetting(showLookDirection);
        addSetting(showEyeHeight);
        addSetting(showServerPosition);
        addSetting(showExtraDirectionLines);
        addSetting(showDebugCircles);
        addSetting(showDebugPoints);
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        return true;
    }

    public boolean showPlayers() { return players.get(); }
    public boolean showHostileMobs() { return hostileMobs.get(); }
    public boolean showPassiveMobs() { return passiveMobs.get(); }
    public boolean showOtherMobs() { return otherMobs.get(); }
    public boolean showItemDrops() { return itemDrops.get(); }
    public boolean showExperienceOrbs() { return experienceOrbs.get(); }
    public boolean showProjectiles() { return projectiles.get(); }
    public boolean showVehicles() { return vehicles.get(); }
    public boolean showOtherEntities() { return otherEntities.get(); }
    public int playerColor() { return playerColor.getArgb(); }
    public int otherEntityColor() { return otherEntityColor.getArgb(); }
    public boolean showLookDirection() { return showLookDirection.get(); }
    public boolean showEyeHeight() { return showEyeHeight.get(); }
    public boolean showServerPosition() { return showServerPosition.get(); }
    public boolean showExtraDirectionLines() { return showExtraDirectionLines.get(); }
    public boolean showDebugCircles() { return showDebugCircles.get(); }
    public boolean showDebugPoints() { return showDebugPoints.get(); }
}
