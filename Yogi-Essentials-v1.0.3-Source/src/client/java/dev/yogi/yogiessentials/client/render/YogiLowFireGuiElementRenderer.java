package dev.yogi.yogiessentials.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_10366;
import net.minecraft.class_11239;
import net.minecraft.class_11246;
import net.minecraft.class_11279;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;

/**
 * Low Fire preview using the exact same direct renderer and runtime texture as
 * gameplay. It never asks DrawContext or the block atlas for a fire Sprite.
 */
public final class YogiLowFireGuiElementRenderer
        extends class_11239<YogiLowFireGuiElementRenderState> {

    private static final float NEAR_Z = 0.05F;
    private static final float FAR_Z = 100.0F;

    private final class_11279 projection =
            new class_11279(
                    "Yogi Essentials protected fire preview",
                    NEAR_Z,
                    FAR_Z
            );

    public YogiLowFireGuiElementRenderer(
            class_4597.class_4598 vertexConsumers
    ) {
        super(vertexConsumers);
    }

    @Override
    public void render(
            YogiLowFireGuiElementRenderState elementState,
            class_11246 guiState,
            int windowScaleFactor
    ) {
        RenderSystem.backupProjectionMatrix();
        try {
            super.method_70913(elementState, guiState, windowScaleFactor);
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }

    @Override
    public Class<YogiLowFireGuiElementRenderState> method_70903() {
        return YogiLowFireGuiElementRenderState.class;
    }

    @Override
    protected String method_70906() {
        return "yogi_protected_low_fire";
    }

    @Override
    protected float method_70907(int height, int windowScaleFactor) {
        return height / 2.0F;
    }

    @Override
    protected void render(
            YogiLowFireGuiElementRenderState state,
            class_4587 matrices
    ) {
        class_310 client = class_310.method_1551();

        int guiWidth = Math.max(1, state.comp_4124() - state.comp_4122());
        int guiHeight = Math.max(1, state.comp_4125() - state.comp_4123());
        int guiScale = Math.max(1, client.method_22683().method_4495());
        int framebufferWidth = Math.max(1, guiWidth * guiScale);
        int framebufferHeight = Math.max(1, guiHeight * guiScale);

        RenderSystem.setProjectionMatrix(
                projection.method_71095(framebufferWidth, framebufferHeight, 70.0F),
                class_10366.field_54953
        );

        matrices.method_34426();
        YogiDirectFireRenderer.renderFirstPerson(matrices, field_59933);
        field_59933.method_22993();
    }

    @Override
    public void close() {
        projection.close();
        super.close();
    }
}
