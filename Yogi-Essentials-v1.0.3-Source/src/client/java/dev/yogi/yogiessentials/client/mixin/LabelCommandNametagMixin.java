package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.visual.NametagCustomizerModule;
import dev.yogi.yogiessentials.client.util.NametagRenderContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.class_11661;
import net.minecraft.class_11689;
import net.minecraft.class_12075;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_4587;
import net.minecraft.class_5250;

/** Styles only Minecraft's native player-name command and removes its see-through copy. */
@Mixin(class_11689.class_12050.class)
public abstract class LabelCommandNametagMixin {

    @Shadow @Final List<class_11661.class_11672> normalLabels;
    @Shadow @Final List<class_11661.class_11672> seethroughLabels;

    @Unique private int yogiEssentials$normalStart;
    @Unique private int yogiEssentials$seethroughStart;

    @Inject(method = "add", at = @At("HEAD"))
    private void yogiEssentials$captureStart(
            class_4587 matrices,
            class_243 pos,
            int y,
            class_2561 label,
            boolean notSneaking,
            int light,
            double squaredDistanceToCamera,
            class_12075 cameraState,
            CallbackInfo ci
    ) {
        yogiEssentials$normalStart = normalLabels.size();
        yogiEssentials$seethroughStart = seethroughLabels.size();
    }

    @Inject(method = "add", at = @At("RETURN"))
    private void yogiEssentials$stylePlayerNametag(
            class_4587 matrices,
            class_243 pos,
            int y,
            class_2561 label,
            boolean notSneaking,
            int light,
            double squaredDistanceToCamera,
            class_12075 cameraState,
            CallbackInfo ci
    ) {
        if (!NametagRenderContext.isCurrentPlayerNametag(label)) return;
        if (YogiEssentialsClient.getModuleManager() == null) return;

        NametagCustomizerModule module =
                YogiEssentialsClient.getModuleManager().getModule(NametagCustomizerModule.class);
        if (module == null || !module.isEnabled() || module.getShowServerNametags().get()) return;

        yogiEssentials$styleRange(normalLabels, yogiEssentials$normalStart, module);
        if (seethroughLabels.size() > yogiEssentials$seethroughStart) {
            seethroughLabels.subList(yogiEssentials$seethroughStart, seethroughLabels.size()).clear();
        }
    }

    @Unique
    private static void yogiEssentials$styleRange(
            List<class_11661.class_11672> labels,
            int start,
            NametagCustomizerModule module
    ) {
        for (int i = Math.max(0, start); i < labels.size(); i++) {
            class_11661.class_11672 command = labels.get(i);
            if (!NametagRenderContext.isCurrentPlayerNametag(command.comp_4517())) continue;

            class_5250 text = command.comp_4517().method_27661();
            text.method_10862(text.method_10866()
                    .method_10982(module.getBold().get())
                    .method_10978(module.getItalic().get())
                    .method_30938(module.getUnderline().get()));
            if (module.getCustomTextColor().get()) {
                text.method_10862(text.method_10866().method_36139(module.getTextColor().getRgb()));
            }

            Matrix4f matrix = new Matrix4f(command.comp_4514());
            float scale = module.getScale().get().floatValue();
            if (Math.abs(scale - 1.0F) > 0.001F) {
                matrix.scale(scale, scale, scale);
            }

            int originalAlpha = (command.comp_4519() >>> 24) & 0xFF;
            int textColor = module.getCustomTextColor().get()
                    ? ((originalAlpha << 24) | module.getTextColor().getRgb())
                    : command.comp_4519();

            int background = module.getBackgroundArgb();
            if (originalAlpha < 255) {
                int configuredAlpha = (background >>> 24) & 0xFF;
                int fadedAlpha = configuredAlpha * originalAlpha / 255;
                background = (fadedAlpha << 24) | (background & 0x00FFFFFF);
            }

            labels.set(i, new class_11661.class_11672(
                    matrix,
                    command.comp_4515(),
                    command.comp_4516() + module.getVerticalOffset().get().floatValue(),
                    text,
                    command.comp_4518(),
                    textColor,
                    background,
                    command.comp_4521()
            ));
        }
    }
}
