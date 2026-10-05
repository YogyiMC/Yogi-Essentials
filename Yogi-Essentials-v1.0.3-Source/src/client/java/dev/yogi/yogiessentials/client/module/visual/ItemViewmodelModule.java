package dev.yogi.yogiessentials.client.module.visual;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.class_1747;
import net.minecraft.class_1755;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1812;
import net.minecraft.class_9334;

public class ItemViewmodelModule extends Module {

    public enum ItemGroup {
        BUCKETS,
        FOOD,
        BUILDING_BLOCKS,
        COBWEBS,
        ENDER_PEARLS,
        POTIONS,
        MISC_UTILITY
    }


    public static final class ViewmodelSettings {

        private final NumberSetting scale;
        private final NumberSetting x;
        private final NumberSetting y;
        private final NumberSetting z;
        private final NumberSetting rotation;

        private ViewmodelSettings(
                String prefix
        ) {
            scale =
                    new NumberSetting(
                            prefix + " Scale",
                            1.0,
                            0.35,
                            1.65,
                            0.05
                    );

            x =
                    new NumberSetting(
                            prefix + " X",
                            0.0,
                            -0.50,
                            0.50,
                            0.01
                    );

            y =
                    new NumberSetting(
                            prefix + " Y",
                            0.0,
                            -0.50,
                            0.50,
                            0.01
                    );

            z =
                    new NumberSetting(
                            prefix + " Z",
                            0.0,
                            -0.50,
                            0.50,
                            0.01
                    );

            rotation =
                    new NumberSetting(
                            prefix + " Rotation",
                            0.0,
                            -90.0,
                            90.0,
                            1.0
                    );
        }

        public NumberSetting getScale() {
            return scale;
        }

        public NumberSetting getX() {
            return x;
        }

        public NumberSetting getY() {
            return y;
        }

        public NumberSetting getZ() {
            return z;
        }

        public NumberSetting getRotation() {
            return rotation;
        }

        





        public boolean isRegular() {
            return nearly(scale.get().doubleValue(), 1.0)
                    && nearly(x.get().doubleValue(), 0.0)
                    && nearly(y.get().doubleValue(), 0.0)
                    && nearly(z.get().doubleValue(), 0.0)
                    && nearly(rotation.get().doubleValue(), 0.0);
        }

        private static boolean nearly(double a, double b) {
            return Math.abs(a - b) < 0.000001;
        }

        private boolean owns(
                Setting<?> setting
        ) {
            return setting == scale
                    ||
                    setting == x
                    ||
                    setting == y
                    ||
                    setting == z
                    ||
                    setting == rotation;
        }
    }

    private final EnumSetting<ItemGroup> selectedGroup =
            new EnumSetting<>(
                    "Item Group",
                    ItemGroup.BUCKETS,
                    ItemGroup.class
            );

    private final Map<ItemGroup, ViewmodelSettings> profiles =
            new EnumMap<>(
                    ItemGroup.class
            );

    public ItemViewmodelModule() {
        super(
                "Item Viewmodels",
                "Adjust first-person item groups. Misc Utility only affects compasses, clocks, recovery compasses, and spyglasses.",
                Category.VISUAL
        );

        addSetting(
                selectedGroup
        );

        for (
                ItemGroup group
                : ItemGroup.values()
        ) {
            String prefix =
                    prettyName(
                            group
                    );

            ViewmodelSettings profile =
                    new ViewmodelSettings(
                            prefix
                    );

            profiles.put(
                    group,
                    profile
            );

            addSetting(
                    profile.getScale()
            );

            addSetting(
                    profile.getX()
            );

            addSetting(
                    profile.getY()
            );

            addSetting(
                    profile.getZ()
            );

            addSetting(
                    profile.getRotation()
            );
        }
    }

    @Override
    public boolean isSettingVisible(
            Setting<?> setting
    ) {
        if (
                setting
                        == selectedGroup
        ) {
            return true;
        }

        ViewmodelSettings selected =
                profiles.get(
                        selectedGroup.get()
                );

        return selected != null
                &&
                selected.owns(
                        setting
                );
    }

    public EnumSetting<ItemGroup> getSelectedGroup() {
        return selectedGroup;
    }

    public ViewmodelSettings getSelectedSettings() {
        return profiles.get(
                selectedGroup.get()
        );
    }

    public ViewmodelSettings getSettings(
            ItemGroup group
    ) {
        return profiles.get(
                group
        );
    }

    public ViewmodelSettings getSettingsFor(
            class_1799 stack
    ) {
        ItemGroup group =
                resolveGroup(
                        stack
                );

        return group == null
                ? null
                : profiles.get(
                        group
                );
    }

    
    public boolean shouldTransform(class_1799 stack) {
        ViewmodelSettings settings = getSettingsFor(stack);
        return settings != null && !settings.isRegular();
    }

    public static ItemGroup resolveGroup(
            class_1799 stack
    ) {
        if (
                stack == null
                        ||
                stack.method_7960()
        ) {
            return null;
        }

        class_1792 item =
                stack.method_7909();

        
        
        if (
                stack.method_31574(
                        class_1802.field_8255
                )
                        ||
                stack.method_31574(
                        class_1802.field_8288
                )
        ) {
            return null;
        }

        if (
                stack.method_31574(
                        class_1802.field_8786
                )
        ) {
            return ItemGroup.COBWEBS;
        }

        if (
                stack.method_31574(
                        class_1802.field_8634
                )
        ) {
            return ItemGroup.ENDER_PEARLS;
        }

        if (
                item
                        instanceof class_1812
        ) {
            return ItemGroup.POTIONS;
        }

        if (
                item
                        instanceof class_1755
                        ||
                stack.method_31574(
                        class_1802.field_8103
                )
                        ||
                stack.method_31574(
                        class_1802.field_27876
                )
        ) {
            return ItemGroup.BUCKETS;
        }

        if (
                stack.method_58694(
                        class_9334.field_50075
                )
                        != null
        ) {
            return ItemGroup.FOOD;
        }

        if (
                item
                        instanceof class_1747
        ) {
            return ItemGroup.BUILDING_BLOCKS;
        }

        if (
                stack.method_31574(
                        class_1802.field_8251
                )
                        ||
                stack.method_31574(
                        class_1802.field_8557
                )
                        ||
                stack.method_31574(
                        class_1802.field_38747
                )
                        ||
                stack.method_31574(
                        class_1802.field_27070
                )
        ) {
            return ItemGroup.MISC_UTILITY;
        }

        
        return null;
    }

    public static class_1799 sampleStack(
            ItemGroup group
    ) {
        return switch (group) {
            case BUCKETS ->
                    new class_1799(
                            class_1802.field_8705
                    );

            case FOOD ->
                    new class_1799(
                            class_1802.field_8463
                    );

            case BUILDING_BLOCKS ->
                    new class_1799(
                            class_1802.field_8281
                    );

            case COBWEBS ->
                    new class_1799(
                            class_1802.field_8786
                    );

            case ENDER_PEARLS ->
                    new class_1799(
                            class_1802.field_8634
                    );

            case POTIONS ->
                    new class_1799(
                            class_1802.field_8436
                    );

            case MISC_UTILITY ->
                    new class_1799(
                            class_1802.field_8251
                    );
        };
    }

    private static String prettyName(
            ItemGroup group
    ) {
        String raw =
                group.name()
                        .replace(
                                '_',
                                ' '
                        )
                        .toLowerCase();

        StringBuilder result =
                new StringBuilder();

        boolean upper =
                true;

        for (
                char character
                : raw.toCharArray()
        ) {
            result.append(
                    upper
                            ? Character.toUpperCase(
                            character
                    )
                            : character
            );

            upper =
                    character == ' ';
        }

        return result.toString();
    }
}
