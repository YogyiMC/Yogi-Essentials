package dev.yogi.yogiessentials.client.render;

import dev.yogi.yogiessentials.client.mixin.DrawContextAccessor;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.visual.ItemViewmodelModule;
import dev.yogi.yogiessentials.client.module.visual.LowFireModule;
import dev.yogi.yogiessentials.client.module.visual.LowShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SideShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SmallBlockModule;
import dev.yogi.yogiessentials.client.module.visual.SmallTotemModule;
import net.minecraft.class_10799;
import net.minecraft.class_11246;
import net.minecraft.class_11256;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_8030;
import net.minecraft.class_9334;




















public final class FirstPersonPreviewRenderer {

    private static final class_2960 BACKGROUND =
            class_2960.method_60655("yogiessentials", "textures/gui/preview_plains.png");



    







    private static final class_2960 PREVIEW_SHIELD_IDLE_MODEL =
            class_2960.method_60655("yogiessentials", "preview_shield_idle");

    private static final class_2960 PREVIEW_SHIELD_BLOCKING_MODEL =
            class_2960.method_60655("yogiessentials", "preview_shield_blocking");

    private static final int BACKGROUND_WIDTH = 736;
    private static final int BACKGROUND_HEIGHT = 414;

    private static final int BORDER = 0xFF34343D;
    private static final int CROSSHAIR = 0xEFFFFFFF;

    private FirstPersonPreviewRenderer() {
    }

    
    
    

    public static void render(
            class_332 context,
            Module module,
            int x,
            int y,
            int width,
            int height
    ) {
        int[] bg = getBackgroundRect(x, y, width, height);
        int bgX = bg[0];
        int bgY = bg[1];
        int bgWidth = bg[2];
        int bgHeight = bg[3];

        context.method_25294(x, y, x + width, y + height, 0xFF0B0B0F);

        renderBackground(context, bgX, bgY, bgWidth, bgHeight);

        
        class_1799 stack = previewStack(module);
        if (!stack.method_7960()) {
            submitViewmodel(context, module, stack, bgX, bgY, bgWidth, bgHeight);
        }

        
        
        context.method_44379(bgX, bgY, bgX + bgWidth, bgY + bgHeight);
        renderCrosshair(context, bgX + bgWidth / 2, bgY + bgHeight / 2);
        context.method_44380();

        
        if (module instanceof LowFireModule lowFire) {
            renderFireOverlay(context, lowFire, bgX, bgY, bgWidth, bgHeight);
        }

        outline(context, x, y, width, height, BORDER);
    }

    
    
    

    private static class_1799 previewStack(Module module) {
        if (module instanceof SmallBlockModule) {
            class_1799 shield =
                    new class_1799(class_1802.field_8255);

            shield.method_57379(
                    class_9334.field_54199,
                    PREVIEW_SHIELD_BLOCKING_MODEL
            );

            return shield;
        }

        if (module instanceof SideShieldModule
                || module instanceof LowShieldModule) {
            class_1799 shield =
                    new class_1799(class_1802.field_8255);

            shield.method_57379(
                    class_9334.field_54199,
                    PREVIEW_SHIELD_IDLE_MODEL
            );

            return shield;
        }

        if (module instanceof SmallTotemModule) {
            return new class_1799(class_1802.field_8288);
        }

        if (module instanceof ItemViewmodelModule viewmodels) {
            return ItemViewmodelModule.sampleStack(
                    viewmodels.getSelectedGroup().get()
            );
        }

        return class_1799.field_8037;
    }

    
    
    

    private static void submitViewmodel(
            class_332 context,
            Module module,
            class_1799 stack,
            int x,
            int y,
            int width,
            int height
    ) {
        class_310 client = class_310.method_1551();

        
        
        if (client.field_1724 == null) {
            return;
        }

        
        boolean offhandStyle =
                module instanceof SideShieldModule
                        || module instanceof LowShieldModule
                        || module instanceof SmallBlockModule
                        || module instanceof SmallTotemModule;

        boolean blocking = module instanceof SmallBlockModule;

        class_1268 hand = offhandStyle ? class_1268.field_5810 : class_1268.field_5808;

        
        class_1306 mainArm = client.field_1724.method_6068();
        class_1306 arm = hand == class_1268.field_5808 ? mainArm : mainArm.method_5928();

        int x1 = x;
        int y1 = y;
        int x2 = x + width;
        int y2 = y + height;

        class_8030 scissorArea = new class_8030(x1, y1, width, height);
        class_8030 bounds =
                class_11256.method_71535(x1, y1, x2, y2, scissorArea);

        class_11246 guiState =
                ((DrawContextAccessor) (Object) context).yogi$getGuiRenderState();

        guiState.method_70922(new YogiViewmodelGuiElementRenderState(
                stack.method_7972(),
                module,
                hand,
                arm,
                blocking,
                x1, y1, x2, y2,
                1.0F,
                scissorArea,
                bounds
        ));
    }

    
    
    

    private static int[] getBackgroundRect(int x, int y, int width, int height) {
        float sourceAspect = BACKGROUND_WIDTH / (float) BACKGROUND_HEIGHT;
        float targetAspect = width / (float) height;

        int drawWidth;
        int drawHeight;

        if (targetAspect > sourceAspect) {
            drawHeight = height;
            drawWidth = Math.round(height * sourceAspect);
        } else {
            drawWidth = width;
            drawHeight = Math.round(width / sourceAspect);
        }

        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;

        return new int[] { drawX, drawY, drawWidth, drawHeight };
    }

    private static void renderBackground(
            class_332 context,
            int x,
            int y,
            int width,
            int height
    ) {
        context.method_25302(
                class_10799.field_56883,
                BACKGROUND,
                x, y,
                0.0F, 0.0F,
                width, height,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT
        );
    }

    private static void renderCrosshair(class_332 context, int centerX, int centerY) {
        context.method_25294(centerX - 4, centerY, centerX - 1, centerY + 1, CROSSHAIR);
        context.method_25294(centerX + 2, centerY, centerX + 5, centerY + 1, CROSSHAIR);
        context.method_25294(centerX, centerY - 4, centerX + 1, centerY - 1, CROSSHAIR);
        context.method_25294(centerX, centerY + 2, centerX + 1, centerY + 5, CROSSHAIR);
    }

    private static void renderFireOverlay(
            class_332 context,
            LowFireModule module,
            int x,
            int y,
            int width,
            int height
    ) {
        if (!module.isEnabled() || !module.isFirstPersonLowFire()) {
            return;
        }

        int x1 = x;
        int y1 = y;
        int x2 = x + width;
        int y2 = y + height;

        class_8030 scissorArea = new class_8030(x1, y1, width, height);
        class_8030 bounds =
                class_11256.method_71535(x1, y1, x2, y2, scissorArea);

        class_11246 guiState =
                ((DrawContextAccessor) (Object) context).yogi$getGuiRenderState();

        guiState.method_70922(new YogiLowFireGuiElementRenderState(
                x1, y1, x2, y2,
                1.0F,
                scissorArea,
                bounds
        ));
    }

    private static void outline(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        context.method_25294(x, y, x + width, y + 1, color);
        context.method_25294(x, y + height - 1, x + width, y + height, color);
        context.method_25294(x, y, x + 1, y + height, color);
        context.method_25294(x + width - 1, y, x + width, y + height, color);
    }
}
