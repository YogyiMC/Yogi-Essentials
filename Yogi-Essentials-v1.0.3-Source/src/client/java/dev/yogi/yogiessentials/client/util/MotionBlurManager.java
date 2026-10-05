package dev.yogi.yogiessentials.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.mixin.GameRendererAccessor;
import dev.yogi.yogiessentials.client.mixin.PostEffectPassAccessor;
import dev.yogi.yogiessentials.client.mixin.PostEffectProcessorAccessor;
import dev.yogi.yogiessentials.client.module.pvp.MotionBlurModule;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.class_276;
import net.minecraft.class_279;
import net.minecraft.class_283;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3264;
import net.minecraft.class_9920;
import net.minecraft.class_9960;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Yogi Essentials temporal motion blur.
 *
 * This is an independent Minecraft 1.21.11 implementation of temporal frame
 * blending, using the same broad technique documented by Natural Motion Blur.
 * It does not copy Natural Motion Blur source; Yogi Essentials keeps its own
 * post-effect implementation and lifecycle.
 */
public final class MotionBlurManager {
    private static final class_2960 EFFECT_ID =
            class_2960.method_60655("yogiessentials", "yogi_motion_blur");
    private static final String PARAM_BLOCK = "YogiBlurParams";

    private static class_279 effect;
    private static GpuBuffer paramsBuffer;
    private static boolean primed;
    private static boolean initialized;
    private static boolean loadFailureLogged;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static float lastWrittenWeight = Float.NaN;

    private MotionBlurManager() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ResourceManagerHelper.get(class_3264.field_14188)
                .registerReloadListener(new MotionBlurResourceReloader());
        WorldRenderEvents.END_MAIN.register(context -> render());
    }

    public static void render() {
        class_310 client = class_310.method_1551();
        MotionBlurModule module = getModule();

        if (module == null || !module.isEnabled()
                || client.field_1724 == null || client.field_1687 == null
                || (!module.getInMenus().get() && client.field_1755 != null)) {
            resetHistory();
            return;
        }

        float strength = module.getStrength().get().floatValue();
        if (strength <= 0.0F) {
            resetHistory();
            return;
        }

        class_279 processor = ensureEffect(client);
        if (processor == null || paramsBuffer == null) {
            return;
        }

        class_276 framebuffer = client.method_1522();
        if (framebuffer.field_1482 != lastWidth || framebuffer.field_1481 != lastHeight) {
            lastWidth = framebuffer.field_1482;
            lastHeight = framebuffer.field_1481;
            primed = false;
        }

        float previousWeight = primed
                ? perceptualHistoryWeight(strength)
                : 0.0F;
        writeBlendIfChanged(previousWeight);

        class_9920 pool = ((GameRendererAccessor) (Object) client.field_1773)
                .yogi$getPostEffectPool();
        processor.method_1258(framebuffer, pool);
        primed = true;
    }

    /**
     * Maps the UI percentage to temporal history retention.
     *
     * The old implementation treated the slider as a 60 FPS retention value
     * and then increased the per-frame history weight as FPS rose. At high FPS
     * that made even 10-20% feel extremely smeary. Motion Blur strength should
     * remain intuitive regardless of frame rate, so use a deliberately gentle
     * perceptual curve with no high-FPS amplification.
     *
     * Approximate anchors:
     *  10% -> 0.5% previous frame
     *  20% -> 2.4%
     *  50% -> 17.8%
     *  80% -> 50.2%
     *  99% -> 80.2%
     */
    private static float perceptualHistoryWeight(float strengthPercent) {
        float normalized = Math.max(0.0F, Math.min(0.99F, strengthPercent / 100.0F));
        return (float) (0.82 * Math.pow(normalized, 2.2));
    }

    public static void resetHistory() {
        lastWidth = -1;
        lastHeight = -1;
        lastWrittenWeight = Float.NaN;
        primed = false;
    }

    public static void invalidate() {
        effect = null;
        if (paramsBuffer != null && !paramsBuffer.isClosed()) {
            paramsBuffer.close();
        }
        paramsBuffer = null;
        loadFailureLogged = false;
        resetHistory();
    }

    private static MotionBlurModule getModule() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient.getModuleManager().getModule(MotionBlurModule.class);
    }

    private static class_279 ensureEffect(class_310 client) {
        if (paramsBuffer != null && paramsBuffer.isClosed()) {
            invalidate();
        }
        if (effect != null && paramsBuffer != null) {
            return effect;
        }

        try {
            effect = Objects.requireNonNull(
                    client.method_62887().method_62941(
                            EFFECT_ID,
                            Set.of(class_9960.field_53083)
                    )
            );
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Yogi Essentials motion blur parameters",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                    4
            );
            attachParameterBuffer(effect, paramsBuffer);
            loadFailureLogged = false;
            return effect;
        } catch (RuntimeException exception) {
            if (!loadFailureLogged) {
                System.err.println(
                        "[Yogi Essentials] Motion Blur post effect failed to load: "
                                + exception.getMessage()
                );
                loadFailureLogged = true;
            }
            return null;
        }
    }

    private static void attachParameterBuffer(
            class_279 processor,
            GpuBuffer replacement
    ) {
        for (class_283 pass
                : ((PostEffectProcessorAccessor) (Object) processor).yogi$getPasses()) {
            Map<String, GpuBuffer> buffers =
                    ((PostEffectPassAccessor) (Object) pass).yogi$getUniformBuffers();
            if (!buffers.containsKey(PARAM_BLOCK)) {
                continue;
            }
            GpuBuffer previous = buffers.put(PARAM_BLOCK, replacement);
            if (previous != null && previous != replacement) {
                previous.close();
            }
        }
    }

    private static void writeBlendIfChanged(float previousWeight) {
        if (!Float.isNaN(lastWrittenWeight)
                && Math.abs(previousWeight - lastWrittenWeight) < 0.0001F) {
            return;
        }
        try (GpuBuffer.MappedView view = RenderSystem.getDevice()
                .createCommandEncoder()
                .mapBuffer(paramsBuffer, false, true)) {
            Std140Builder.intoBuffer(view.data()).putFloat(previousWeight);
            lastWrittenWeight = previousWeight;
        }
    }
}
