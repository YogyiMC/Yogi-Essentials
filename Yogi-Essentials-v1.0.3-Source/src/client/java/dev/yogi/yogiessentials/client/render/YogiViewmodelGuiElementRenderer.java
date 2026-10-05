package dev.yogi.yogiessentials.client.render;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.yogi.yogiessentials.client.mixin.GameRendererAccessor;
import dev.yogi.yogiessentials.client.mixin.HeldItemRendererInvoker;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.visual.ItemViewmodelModule;
import dev.yogi.yogiessentials.client.module.visual.LowShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SideShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SmallBlockModule;
import dev.yogi.yogiessentials.client.module.visual.SmallTotemModule;
import net.minecraft.class_10366;
import net.minecraft.class_11239;
import net.minecraft.class_11246;
import net.minecraft.class_11279;
import net.minecraft.class_11540;
import net.minecraft.class_11659;
import net.minecraft.class_11684;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_759;
import net.minecraft.class_765;
import net.minecraft.class_811;




























public final class YogiViewmodelGuiElementRenderer
        extends class_11239<YogiViewmodelGuiElementRenderState> {

    private static final float NEAR_Z = 0.05F;
    private static final float FAR_Z = 100.0F;

    private final class_11279 perspectiveProjection =
            new class_11279(
                    "Yogi Essentials first-person preview",
                    NEAR_Z,
                    FAR_Z
            );

    public YogiViewmodelGuiElementRenderer(
            class_4597.class_4598 vertexConsumers
    ) {
        super(vertexConsumers);
    }

    










    @Override
    public void render(
            YogiViewmodelGuiElementRenderState elementState,
            class_11246 guiState,
            int windowScaleFactor
    ) {
        class_310 client =
                class_310.method_1551();

        RenderSystem.backupProjectionMatrix();

        try {
            










            super.method_70913(
                    elementState,
                    guiState,
                    windowScaleFactor
            );

        } finally {
            



            client.field_1773
                    .method_71114()
                    .method_71034(
                            class_308.class_11274.field_60027
                    );

            RenderSystem.restoreProjectionMatrix();
        }
    }

    @Override
    public Class<YogiViewmodelGuiElementRenderState> method_70903() {
        return YogiViewmodelGuiElementRenderState.class;
    }

    @Override
    protected String method_70906() {
        return "yogi_first_person_viewmodel";
    }

    @Override
    protected float method_70907(
            int height,
            int windowScaleFactor
    ) {
        




        return height / 2.0F;
    }

    @Override
    protected void render(
            YogiViewmodelGuiElementRenderState state,
            class_4587 matrices
    ) {
        class_310 client =
                class_310.method_1551();

        if (client.field_1724 == null) {
            return;
        }

        int guiWidth =
                Math.max(
                        1,
                        state.comp_4124() - state.comp_4122()
                );

        int guiHeight =
                Math.max(
                        1,
                        state.comp_4125() - state.comp_4123()
                );

        int guiScale =
                Math.max(
                        1,
                        client.method_22683().method_4495()
                );

        int framebufferWidth =
                Math.max(
                        1,
                        guiWidth * guiScale
                );

        int framebufferHeight =
                Math.max(
                        1,
                        guiHeight * guiScale
                );

        












        float fov = 70.0F;

        








        RenderSystem.setProjectionMatrix(
                perspectiveProjection.method_71095(
                        framebufferWidth,
                        framebufferHeight,
                        fov
                ),
                class_10366.field_54953
        );

        








        matrices.method_34426();

        class_1268 hand =
                state.hand();

        class_1306 arm =
                state.arm();

        class_759 heldItemRenderer =
                ((GameRendererAccessor) (Object) client.field_1773)
                        .yogi$getHeldItemRenderer();

        HeldItemRendererInvoker vanilla =
                (HeldItemRendererInvoker) (Object) heldItemRenderer;

        







        vanilla.yogi$applyEquipOffset(
                matrices,
                arm,
                0.0F
        );

        vanilla.yogi$applySwingOffset(
                matrices,
                arm,
                0.0F
        );

        







        applyYogiTransforms(
                matrices,
                state.module(),
                hand
        );

        class_11684 dispatcher =
                client.field_1773.method_72911();

        class_11659 queue =
                dispatcher.method_73003();

        














        class_811 displayContext =
                arm == class_1306.field_6183
                        ? class_811.field_4322
                        : class_811.field_4321;

        















        class_11540 lightingState =
                new class_11540();

        client.method_65386()
                .method_65597(
                        lightingState,
                        state.stack(),
                        displayContext,
                        client.field_1724
                );

        class_308.class_11274 lightingType =
                lightingState.method_65608()
                        ? class_308.class_11274.field_60027
                        : class_308.class_11274.field_60026;

        client.field_1773
                .method_71114()
                .method_71034(lightingType);

        FirstPersonModelTransformOverride.beginPreview(
                state.module(),
                hand,
                state.stack()
        );
        try {
            heldItemRenderer.method_3233(
                    client.field_1724,
                    state.stack(),
                    displayContext,
                    matrices,
                    queue,
                    class_765.field_32767
            );
        } finally {
            FirstPersonModelTransformOverride.end();
        }

        dispatcher.method_73002();
    }

    private static void applyYogiTransforms(
            class_4587 matrices,
            Module module,
            class_1268 hand
    ) {
        if (module instanceof LowShieldModule lowShield) {
            YogiFirstPersonTransforms.applyLowShield(
                    lowShield,
                    matrices
            );

        } else if (module instanceof SideShieldModule sideShield) {
            YogiFirstPersonTransforms.applySideShield(
                    hand,
                    sideShield,
                    matrices
            );

        } else if (module instanceof SmallBlockModule smallBlock) {
            YogiFirstPersonTransforms.applySmallBlock(
                    hand,
                    smallBlock,
                    matrices
            );

        } else if (module instanceof SmallTotemModule smallTotem) {
            YogiFirstPersonTransforms.applyTotem(
                    hand,
                    smallTotem,
                    matrices
            );

        } else if (module instanceof ItemViewmodelModule viewmodels) {
            ItemViewmodelModule.ViewmodelSettings settings =
                    viewmodels.getSelectedSettings();

            if (settings != null) {
                YogiFirstPersonTransforms.applyItemViewmodel(
                        hand,
                        settings,
                        matrices
                );
            }
        }
    }

    @Override
    public void close() {
        perspectiveProjection.close();
        super.close();
    }
}
