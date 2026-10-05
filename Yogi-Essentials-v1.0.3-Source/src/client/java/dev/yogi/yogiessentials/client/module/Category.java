package dev.yogi.yogiessentials.client.module;

public enum Category {

    VISUAL("Visuals"),
    HUD("HUD"),
    PVP("PvP"),
    SMP("SMP"),
    CHAT("Chat"),
    FIXES("Fixes"),
    OPTIMIZATIONS("Optimizations"),
    PERFORMANCE("Performance");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}