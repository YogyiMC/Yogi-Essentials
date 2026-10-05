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
import net.minecraft.class_1802;
import net.minecraft.class_742;
import net.minecraft.class_804;
import org.joml.Vector3f;











public final class FirstPersonModelTransformOverride {

    private enum Baseline {
        NONE,
        GENERATED,
        BLOCK,
        SPYGLASS,
        SHIELD_IDLE,
        SHIELD_BLOCKING
    }

    private static final ThreadLocal<Baseline> ACTIVE =
            ThreadLocal.withInitial(() -> Baseline.NONE);

    
    private static final class_804 GENERATED = transform(
            0.0F, -90.0F, 25.0F,
            1.13F, 3.2F, 1.13F,
            0.68F, 0.68F, 0.68F
    );

    
    private static final class_804 BLOCK_RIGHT = transform(
            0.0F, 45.0F, 0.0F,
            0.0F, 0.0F, 0.0F,
            0.40F, 0.40F, 0.40F
    );

    private static final class_804 BLOCK_LEFT = transform(
            0.0F, 225.0F, 0.0F,
            0.0F, 0.0F, 0.0F,
            0.40F, 0.40F, 0.40F
    );

    
    private static final class_804 SHIELD_IDLE_RIGHT = transform(
            0.0F, 180.0F, 5.0F,
            -10.0F, 2.0F, -10.0F,
            1.25F, 1.25F, 1.25F
    );

    private static final class_804 SHIELD_IDLE_LEFT = transform(
            0.0F, 180.0F, 5.0F,
            10.0F, 0.0F, -10.0F,
            1.25F, 1.25F, 1.25F
    );

    
    private static final class_804 SHIELD_BLOCKING_RIGHT = transform(
            0.0F, 180.0F, -5.0F,
            -15.0F, 5.0F, -11.0F,
            1.25F, 1.25F, 1.25F
    );

    private static final class_804 SHIELD_BLOCKING_LEFT = transform(
            0.0F, 180.0F, -5.0F,
            5.0F, 5.0F, -11.0F,
            1.25F, 1.25F, 1.25F
    );

    private FirstPersonModelTransformOverride() {
    }

    
    public static void beginGameplay(
            class_742 player,
            class_1268 hand,
            class_1799 stack
    ) {
        ACTIVE.set(resolveGameplayBaseline(player, hand, stack));
    }

    



    public static void beginPreview(
            Module module,
            class_1268 hand,
            class_1799 stack
    ) {
        if (module instanceof LowShieldModule || module instanceof SideShieldModule) {
            ACTIVE.set(Baseline.SHIELD_IDLE);
            return;
        }
        if (module instanceof SmallBlockModule) {
            ACTIVE.set(Baseline.SHIELD_BLOCKING);
            return;
        }
        if (module instanceof SmallTotemModule) {
            ACTIVE.set(Baseline.GENERATED);
            return;
        }
        if (module instanceof ItemViewmodelModule) {
            ACTIVE.set(resolveViewmodelBaseline(stack));
            return;
        }
        ACTIVE.set(Baseline.NONE);
    }

    public static void end() {
        ACTIVE.remove();
    }

    public static void apply(
            class_804 resourcePackTransform,
            boolean leftHand,
            net.minecraft.class_4587.class_4665 entry
    ) {
        Baseline baseline = ACTIVE.get();
        class_804 replacement = switch (baseline) {
            case GENERATED -> GENERATED;
            case BLOCK -> leftHand ? BLOCK_LEFT : BLOCK_RIGHT;
            case SPYGLASS -> class_804.field_4284;
            case SHIELD_IDLE -> leftHand ? SHIELD_IDLE_LEFT : SHIELD_IDLE_RIGHT;
            case SHIELD_BLOCKING -> leftHand ? SHIELD_BLOCKING_LEFT : SHIELD_BLOCKING_RIGHT;
            case NONE -> resourcePackTransform;
        };

        
        
        replacement.method_23075(leftHand, entry);
    }

    private static Baseline resolveGameplayBaseline(
            class_742 player,
            class_1268 hand,
            class_1799 stack
    ) {
        if (stack == null || stack.method_7960() || YogiEssentialsClient.getModuleManager() == null) {
            return Baseline.NONE;
        }

        if (stack.method_31574(class_1802.field_8255)) {
            boolean blocking = player != null
                    && player.method_6115()
                    && player.method_6058() == hand;

            LowShieldModule low = module(LowShieldModule.class);
            SideShieldModule side = module(SideShieldModule.class);
            SmallBlockModule smallBlock = module(SmallBlockModule.class);

            boolean lowOwns = low != null && low.isEnabled();
            boolean sideOwns = !blocking && side != null && side.isEnabled();
            boolean blockOwns = blocking && smallBlock != null && smallBlock.isEnabled();

            if (!(lowOwns || sideOwns || blockOwns)) {
                return Baseline.NONE;
            }
            return blocking ? Baseline.SHIELD_BLOCKING : Baseline.SHIELD_IDLE;
        }

        if (stack.method_31574(class_1802.field_8288)) {
            SmallTotemModule totem = module(SmallTotemModule.class);
            return totem != null && totem.isEnabled()
                    ? Baseline.GENERATED
                    : Baseline.NONE;
        }

        ItemViewmodelModule viewmodels = module(ItemViewmodelModule.class);
        if (viewmodels == null || !viewmodels.isEnabled() || !viewmodels.shouldTransform(stack)) {
            return Baseline.NONE;
        }

        return resolveViewmodelBaseline(stack);
    }

    private static Baseline resolveViewmodelBaseline(class_1799 stack) {
        ItemViewmodelModule.ItemGroup group = ItemViewmodelModule.resolveGroup(stack);
        if (group == null) {
            return Baseline.NONE;
        }

        return switch (group) {
            case BUILDING_BLOCKS -> Baseline.BLOCK;
            case MISC_UTILITY -> stack.method_31574(class_1802.field_27070)
                    ? Baseline.SPYGLASS
                    : Baseline.GENERATED;
            case BUCKETS, FOOD, COBWEBS, ENDER_PEARLS, POTIONS -> Baseline.GENERATED;
        };
    }

    private static class_804 transform(
            float rx,
            float ry,
            float rz,
            float txPixels,
            float tyPixels,
            float tzPixels,
            float sx,
            float sy,
            float sz
    ) {
        return new class_804(
                new Vector3f(rx, ry, rz),
                new Vector3f(txPixels / 16.0F, tyPixels / 16.0F, tzPixels / 16.0F),
                new Vector3f(sx, sy, sz)
        );
    }

    private static <T extends Module> T module(Class<T> type) {
        return YogiEssentialsClient.getModuleManager().getModule(type);
    }
}
