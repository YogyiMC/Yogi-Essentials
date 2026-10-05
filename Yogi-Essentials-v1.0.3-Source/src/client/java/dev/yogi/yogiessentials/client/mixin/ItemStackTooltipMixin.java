package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.smp.ShulkerBoxPreviewModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.class_10712;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1836;
import net.minecraft.class_2480;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_465;
import net.minecraft.class_5632;
import net.minecraft.class_9331;
import net.minecraft.class_9334;

@Mixin(class_1799.class)
public abstract class ItemStackTooltipMixin {
    @Inject(method = "getTooltipData", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$hideVanillaShulkerTooltipData(
            CallbackInfoReturnable<Optional<class_5632>> cir
    ) {
        if (yogiessentials$shouldSuppressVanillaShulkerPreview()) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "appendComponentTooltip", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$hideVanillaShulkerContainerLines(
            class_9331<?> componentType,
            class_1792.class_9635 context,
            class_10712 displayComponent,
            Consumer<class_2561> textConsumer,
            class_1836 type,
            CallbackInfo ci
    ) {
        if (componentType == class_9334.field_49622
                && yogiessentials$shouldSuppressVanillaShulkerPreview()) {
            ci.cancel();
        }
    }

    private boolean yogiessentials$shouldSuppressVanillaShulkerPreview() {
        class_310 client = class_310.method_1551();
        if (client == null || !(client.field_1755 instanceof class_465<?>)) {
            return false;
        }
        if (YogiEssentialsClient.getModuleManager() == null) {
            return false;
        }

        ShulkerBoxPreviewModule module =
                YogiEssentialsClient.getModuleManager().getModule(ShulkerBoxPreviewModule.class);
        if (module == null || !module.isEnabled()) {
            return false;
        }

        class_1799 self = (class_1799) (Object) this;
        return self.method_7909() instanceof class_1747 blockItem
                && blockItem.method_7711() instanceof class_2480;
    }
}
