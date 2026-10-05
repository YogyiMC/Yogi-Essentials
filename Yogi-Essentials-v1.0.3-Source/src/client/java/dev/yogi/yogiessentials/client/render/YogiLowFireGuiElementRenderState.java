package dev.yogi.yogiessentials.client.render;

import net.minecraft.class_11256;
import net.minecraft.class_8030;

/** Render state for the resource-pack-independent Low Fire preview. */
public record YogiLowFireGuiElementRenderState(
        int x1,
        int y1,
        int x2,
        int y2,
        float scale,
        class_8030 scissorArea,
        class_8030 bounds
) implements class_11256 {
}
