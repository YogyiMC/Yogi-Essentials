package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.render.LowGroundFireBlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1087;
import net.minecraft.class_2246;
import net.minecraft.class_2680;
import net.minecraft.class_4770;
import net.minecraft.class_773;

/**
 * Keeps normal pack/vanilla fire models untouched when Small Ground Fire is
 * disabled. When enabled, the wrapper emits no terrain-model parts because the
 * replacement is drawn by YogiGroundFireRenderer outside the baked-model path.
 */
@Mixin(class_773.class)
public abstract class BlockModelsLowFireMixin {

    @Shadow
    private Map<class_2680, class_1087> models;

    @Inject(method = "setModels", at = @At("TAIL"))
    private void yogiessentials$wrapFireModels(
            Map<class_2680, class_1087> models,
            CallbackInfo ci
    ) {
        Map<class_2680, class_1087> wrapped = new HashMap<>(this.models.size());
        this.models.forEach((state, model) -> {
            if ((state.method_26204() instanceof class_4770
                    || state.method_27852(class_2246.field_10036)
                    || state.method_27852(class_2246.field_22089))
                    && !(model instanceof LowGroundFireBlockStateModel)) {
                wrapped.put(state, new LowGroundFireBlockStateModel(model));
            } else {
                wrapped.put(state, model);
            }
        });
        this.models = wrapped;
    }
}
