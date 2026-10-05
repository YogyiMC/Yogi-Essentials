package dev.yogi.yogiessentials.client.module.smp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.KeybindSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

import org.lwjgl.glfw.GLFW;






public final class WaypointModule extends Module {

    private final KeybindSetting manageWaypointsKey =
            new KeybindSetting("Manage Waypoints Keybind", GLFW.GLFW_KEY_UNKNOWN);

    private final BooleanSetting deathWaypoints =
            new BooleanSetting("Death Waypoints", true);

    private final BooleanSetting showLabels =
            new BooleanSetting("Show Labels", true);

    private final BooleanSetting showDistance =
            new BooleanSetting("Show Distance", true);

    private final BooleanSetting showBeam =
            new BooleanSetting("Vertical Beam", true);

    private final BooleanSetting throughWalls =
            new BooleanSetting("Through Walls", true);

    private final BooleanSetting showOverworldInNether =
            new BooleanSetting("Show Overworld Waypoints In Nether", false);

    private final NumberSetting markerSize =
            new NumberSetting("Marker Size", 0.75, 0.25, 3.00, 0.05);

    private final NumberSetting labelSize =
            new NumberSetting("Label Size", 1.55, 0.75, 3.00, 0.05);

    private final NumberSetting maxDistance =
            new NumberSetting("Max Render Distance", 2048.0, 32.0, 10000.0, 16.0);

    public WaypointModule() {
        super(
                "Waypoints",
                "Create and manage client-side waypoints. Waypoints are kept separately for every dimension and can be renamed, recolored, moved, enabled, or deleted.",
                Category.SMP
        );

        addSetting(manageWaypointsKey);
        addSetting(deathWaypoints);
        addSetting(showLabels);
        addSetting(showDistance);
        addSetting(showBeam);
        addSetting(throughWalls);
        addSetting(showOverworldInNether);
        addSetting(markerSize);
        addSetting(labelSize);
        addSetting(maxDistance);
    }

    public KeybindSetting getManageWaypointsKey() { return manageWaypointsKey; }
    public BooleanSetting getDeathWaypoints() { return deathWaypoints; }
    public BooleanSetting getShowLabels() { return showLabels; }
    public BooleanSetting getShowDistance() { return showDistance; }
    public BooleanSetting getShowBeam() { return showBeam; }
    public BooleanSetting getThroughWalls() { return throughWalls; }
    public BooleanSetting getShowOverworldInNether() { return showOverworldInNether; }
    public NumberSetting getMarkerSize() { return markerSize; }
    public NumberSetting getLabelSize() { return labelSize; }
    public NumberSetting getMaxDistance() { return maxDistance; }
}
