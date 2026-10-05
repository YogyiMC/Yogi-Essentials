package dev.yogi.yogiessentials.client.render;

import dev.yogi.yogiessentials.client.module.Module;
import net.minecraft.class_11256;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_8030;














public record YogiViewmodelGuiElementRenderState(
        class_1799 stack,
        Module module,
        class_1268 hand,
        class_1306 arm,
        boolean blocking,
        int x1,
        int y1,
        int x2,
        int y2,
        float scale,
        class_8030 scissorArea,
        class_8030 bounds
) implements class_11256 {
}
