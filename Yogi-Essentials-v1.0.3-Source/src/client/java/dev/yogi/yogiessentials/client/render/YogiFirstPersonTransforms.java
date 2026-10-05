package dev.yogi.yogiessentials.client.render;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.visual.ItemViewmodelModule;
import dev.yogi.yogiessentials.client.module.visual.LowShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SideShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SmallBlockModule;
import dev.yogi.yogiessentials.client.module.visual.SmallTotemModule;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_7833;























public final class YogiFirstPersonTransforms {

    private YogiFirstPersonTransforms() {
    }

    
    
    

    public static final float SHIELD_BASE_X = 0.32F;
    public static final float SHIELD_BASE_Y_OFFHAND = -0.34F;
    public static final float SHIELD_BASE_Y_MAINHAND = -0.12F;
    public static final float SHIELD_BASE_Z = 0.10F;
    public static final float SHIELD_BASE_Y_ROTATION = 62.0F;
    public static final float SHIELD_BASE_Z_ROTATION = 18.0F;

    public static final float SMALL_BLOCK_BASE_Y_OFFHAND = -0.155F;
    public static final float SMALL_BLOCK_BASE_Y_MAINHAND = -0.265F;

    public static final float SMALL_BLOCK_MIN_SCALE = 0.40F;
    public static final float SMALL_BLOCK_MAX_SCALE = 1.20F;
    public static final float TOTEM_MIN_SCALE = 0.20F;
    public static final float TOTEM_MAX_SCALE = 1.20F;

    
    
    

    public static void applyShieldGameplay(
            class_742 player,
            class_1268 hand,
            class_4587 matrices
    ) {
        LowShieldModule lowShield = module(LowShieldModule.class);
        SideShieldModule sideShield = module(SideShieldModule.class);
        SmallBlockModule smallBlock = module(SmallBlockModule.class);

        boolean blocking =
                player.method_6115() && player.method_6058() == hand;

        if (lowShield != null && lowShield.isEnabled()) {
            applyLowShield(lowShield, matrices);
        }

        if (blocking && smallBlock != null && smallBlock.isEnabled()) {
            applySmallBlock(hand, smallBlock, matrices);
            return;
        }

        if (!blocking && sideShield != null && sideShield.isEnabled()) {
            applySideShield(hand, sideShield, matrices);
        }
    }

    public static void applyTotemGameplay(class_1268 hand, class_4587 matrices) {
        SmallTotemModule module = module(SmallTotemModule.class);
        if (module == null || !module.isEnabled()) {
            return;
        }
        applyTotem(hand, module, matrices);
    }

    public static boolean applyItemViewmodelGameplay(
            class_1268 hand,
            class_1799 item,
            class_4587 matrices
    ) {
        ItemViewmodelModule module = module(ItemViewmodelModule.class);

        if (module == null
                || !module.isEnabled()
                || item == null
                || item.method_7960()) {
            return false;
        }

        ItemViewmodelModule.ViewmodelSettings settings =
                module.getSettingsFor(item);

        if (settings == null || settings.isRegular()) {
            return false;
        }

        applyItemViewmodel(hand, settings, matrices);
        return true;
    }

    
    
    

    public static void applyLowShield(LowShieldModule module, class_4587 matrices) {
        float height = Math.max(0.0F, Math.min(1.0F, module.getHeight().get().floatValue()));
        float lowering = (1.0F - height) * 0.35F;
        matrices.method_46416(0.0F, -lowering, 0.0F);
    }

    public static void applySideShield(
            class_1268 hand,
            SideShieldModule module,
            class_4587 matrices
    ) {
        boolean offhand = hand == class_1268.field_5810;
        float direction = offhand ? -1.0F : 1.0F;

        float xAdjustment = module.getHorizontalOffset().get().floatValue();
        float yAdjustment = module.getVerticalOffset().get().floatValue();
        float rotationAdjustment = module.getRotation().get().floatValue();

        float baseY = offhand ? SHIELD_BASE_Y_OFFHAND : SHIELD_BASE_Y_MAINHAND;

        matrices.method_46416(
                direction * (SHIELD_BASE_X + xAdjustment),
                baseY + yAdjustment,
                SHIELD_BASE_Z
        );

        matrices.method_22907(class_7833.field_40716.rotationDegrees(
                direction * (SHIELD_BASE_Y_ROTATION + rotationAdjustment)));

        matrices.method_22907(class_7833.field_40718.rotationDegrees(
                direction * SHIELD_BASE_Z_ROTATION));
    }

    public static void applySmallBlock(
            class_1268 hand,
            SmallBlockModule module,
            class_4587 matrices
    ) {
        boolean offhand = hand == class_1268.field_5810;
        float direction = offhand ? -1.0F : 1.0F;

        float xAdjustment = module.getHorizontalOffset().get().floatValue();
        float yAdjustment = module.getVerticalOffset().get().floatValue();
        float scale = module.getScale().get().floatValue();

        float baseY = offhand
                ? SMALL_BLOCK_BASE_Y_OFFHAND
                : SMALL_BLOCK_BASE_Y_MAINHAND;

        matrices.method_46416(direction * xAdjustment, baseY + yAdjustment, 0.0F);

        scale = Math.max(SMALL_BLOCK_MIN_SCALE, Math.min(SMALL_BLOCK_MAX_SCALE, scale));
        matrices.method_22905(scale, scale, scale);
    }

    public static void applyTotem(
            class_1268 hand,
            SmallTotemModule module,
            class_4587 matrices
    ) {
        float scale = module.getScale().get().floatValue();
        float x = module.getHorizontalOffset().get().floatValue();
        float y = module.getVerticalOffset().get().floatValue();
        float z = module.getDepthOffset().get().floatValue();
        float rotation = module.getRotation().get().floatValue();

        float direction = hand == class_1268.field_5810 ? -1.0F : 1.0F;

        scale = Math.max(TOTEM_MIN_SCALE, Math.min(TOTEM_MAX_SCALE, scale));

        matrices.method_46416(direction * x, y, z);
        matrices.method_22907(class_7833.field_40718.rotationDegrees(direction * rotation));
        matrices.method_22905(scale, scale, scale);
    }

    public static void applyItemViewmodel(
            class_1268 hand,
            ItemViewmodelModule.ViewmodelSettings settings,
            class_4587 matrices
    ) {
        float handDirection = hand == class_1268.field_5810 ? -1.0F : 1.0F;

        float x = settings.getX().get().floatValue() * handDirection;
        float y = settings.getY().get().floatValue();
        float z = settings.getZ().get().floatValue();
        float scale = settings.getScale().get().floatValue();
        float rotation = settings.getRotation().get().floatValue() * handDirection;

        matrices.method_46416(x, y, z);
        matrices.method_22907(class_7833.field_40718.rotationDegrees(rotation));
        matrices.method_22905(scale, scale, scale);
    }

    
    
    

    









    public static void applyDeterministicBlockDelta(class_4587 matrices, class_1268 hand) {
        float side = hand == class_1268.field_5810 ? -1.0F : 1.0F;
        matrices.method_46416(side * -0.02F, 0.02F, 0.03F);
        matrices.method_22907(class_7833.field_40716.rotationDegrees(side * -10.0F));
        matrices.method_22907(class_7833.field_40714.rotationDegrees(-6.0F));
    }

    
    
    

    private static <T extends Module> T module(Class<T> clazz) {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient.getModuleManager().getModule(clazz);
    }
}
