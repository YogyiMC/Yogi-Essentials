package dev.yogi.yogiessentials.client.util;

/**
 * Compatibility shim for pre-redesign checkouts.
 * Reach Display now uses Minecraft's native hitbox gizmo and the HUD crosshair
 * hook, so no separate world renderer is registered anymore.
 */
@Deprecated(forRemoval = false)
public final class ReachDisplayRenderer {
    private ReachDisplayRenderer() {}

    public static void initialize() {
    }
}
