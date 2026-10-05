package dev.yogi.yogiessentials.client.render;

import java.io.IOException;
import java.io.InputStream;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2960;
import net.minecraft.class_310;

/**
 * Protected Low Fire textures loaded directly from the Yogi Essentials jar.
 *
 * <p>The ground-fire textures stay exact/static to match the reference pack,
 * while the first-person overlay is animated at runtime so the burning overlay
 * still behaves like actual fire instead of a frozen image.</p>
 */
public final class YogiFireVisuals {

    public enum FireKind {
        FIRST_PERSON,
        NORMAL_GROUND,
        SOUL_GROUND
    }

    private static final class_2960 FIRST_PERSON_ID =
            class_2960.method_60655("yogiessentials", "runtime/alexin_low_fire_overlay");
    private static final class_2960 NORMAL_GROUND_ID =
            class_2960.method_60655("yogiessentials", "runtime/alexin_low_fire_ground");
    private static final class_2960 SOUL_GROUND_ID =
            class_2960.method_60655("yogiessentials", "runtime/alexin_low_soul_fire_ground");

    private static final String FIRST_PERSON_RESOURCE =
            "/assets/yogiessentials/textures/block/alexin_low_fire_overlay.png";
    private static final String NORMAL_GROUND_RESOURCE =
            "/assets/yogiessentials/textures/block/alexin_low_fire_ground.png";
    private static final String SOUL_GROUND_RESOURCE =
            "/assets/yogiessentials/textures/block/alexin_low_soul_fire_ground.png";

    private static final int FRAME_COUNT = 12;
    private static final int TICKS_PER_FRAME = 2;

    private static boolean registered;
    private static FireRuntime firstPersonRuntime;
    private static int animationTick;

    private YogiFireVisuals() {
    }

    public static class_2960 runtimeTexture() {
        return runtimeTexture(FireKind.FIRST_PERSON);
    }

    public static class_2960 runtimeTexture(FireKind kind) {
        ensureRegistered();
        return switch (kind) {
            case FIRST_PERSON -> FIRST_PERSON_ID;
            case NORMAL_GROUND -> NORMAL_GROUND_ID;
            case SOUL_GROUND -> SOUL_GROUND_ID;
        };
    }

    /**
     * Keep ground fire exact/static like the pack, but animate the first-person
     * overlay so it behaves like actual burning fire again.
     */
    public static void tickAnimation() {
        ensureRegistered();
        if (firstPersonRuntime == null) {
            return;
        }

        int frame = (animationTick++ / TICKS_PER_FRAME) % FRAME_COUNT;
        uploadFrame(firstPersonRuntime, frame);
    }

    public static synchronized void ensureRegistered() {
        if (registered) {
            return;
        }

        class_310 client = class_310.method_1551();
        if (client == null) {
            throw new IllegalStateException("Minecraft client is not available");
        }

        firstPersonRuntime = registerAnimatedOverlay(
                client,
                FIRST_PERSON_ID,
                FIRST_PERSON_RESOURCE,
                "Yogi animated Low Fire overlay"
        );
        registerStatic(client, NORMAL_GROUND_ID, NORMAL_GROUND_RESOURCE, "Yogi exact Low Fire ground");
        registerStatic(client, SOUL_GROUND_ID, SOUL_GROUND_RESOURCE, "Yogi exact Soul Low Fire ground");
        registered = true;
        animationTick = 0;
    }

    private static FireRuntime registerAnimatedOverlay(
            class_310 client,
            class_2960 id,
            String classpathResource,
            String label
    ) {
        try (InputStream stream = YogiFireVisuals.class.getResourceAsStream(classpathResource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing protected Low Fire texture: " + classpathResource);
            }

            class_1011 base = class_1011.method_4309(stream);
            int[][] frames = buildOverlayFrames(base);
            class_1011 image = new class_1011(class_1011.class_1012.field_4997, base.method_4307(), base.method_4323(), false);
            copyFrameToImage(frames[0], image);
            base.close();

            class_1043 texture = new class_1043(() -> label, image);
            client.method_1531().method_4616(id, texture);
            texture.method_4524();

            if (client.method_1531().method_4619(id) != texture) {
                texture.close();
                throw new IllegalStateException("Low Fire runtime texture was not retained: " + id);
            }

            return new FireRuntime(texture, frames);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load protected Low Fire texture: " + classpathResource, exception);
        }
    }

    private static void registerStatic(
            class_310 client,
            class_2960 id,
            String classpathResource,
            String label
    ) {
        try (InputStream stream = YogiFireVisuals.class.getResourceAsStream(classpathResource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing protected Low Fire texture: " + classpathResource);
            }

            class_1011 image = class_1011.method_4309(stream);
            class_1043 texture = new class_1043(() -> label, image);
            client.method_1531().method_4616(id, texture);
            texture.method_4524();

            if (client.method_1531().method_4619(id) != texture) {
                texture.close();
                throw new IllegalStateException("Low Fire runtime texture was not retained: " + id);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load protected Low Fire texture: " + classpathResource, exception);
        }
    }

    private static void uploadFrame(FireRuntime runtime, int frame) {
        if (runtime.currentFrame == frame) {
            return;
        }
        runtime.currentFrame = frame;

        class_1011 image = runtime.texture.method_4525();
        if (image == null) {
            return;
        }

        copyFrameToImage(runtime.frames[frame], image);
        runtime.texture.method_4524();
    }

    private static int[][] buildOverlayFrames(class_1011 base) {
        int width = base.method_4307();
        int height = base.method_4323();
        int[][] frames = new int[FRAME_COUNT][width * height];

        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            double phase = frame * (Math.PI * 2.0 / FRAME_COUNT);

            for (int y = 0; y < height; y++) {
                double normalizedY = y / (double) Math.max(1, height - 1);
                double amplitude = (1.2 - normalizedY) * 1.1;
                int sway = (int) Math.round(Math.sin(phase + y * 0.58) * amplitude);

                int vertical = 0;
                if (y < height * 0.82) {
                    double lick = Math.sin(phase * 1.65 + y * 0.72);
                    if (lick > 0.68) {
                        vertical = 1;
                    } else if (lick < -0.78) {
                        vertical = -1;
                    }
                }

                int sourceY = clamp(y + vertical, 0, height - 1);
                for (int x = 0; x < width; x++) {
                    int sourceX = x - sway;
                    int pixel = (sourceX < 0 || sourceX >= width) ? 0x00000000 : base.method_61940(sourceX, sourceY);
                    frames[frame][y * width + x] = pixel;
                }
            }
        }

        return frames;
    }

    private static void copyFrameToImage(int[] pixels, class_1011 image) {
        int width = image.method_4307();
        int height = image.method_4323();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.method_61941(x, y, pixels[y * width + x]);
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class FireRuntime {
        private final class_1043 texture;
        private final int[][] frames;
        private int currentFrame = -1;

        private FireRuntime(class_1043 texture, int[][] frames) {
            this.texture = texture;
            this.frames = frames;
        }
    }
}
