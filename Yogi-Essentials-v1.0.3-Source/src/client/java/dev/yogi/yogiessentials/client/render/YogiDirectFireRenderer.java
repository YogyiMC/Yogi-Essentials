package dev.yogi.yogiessentials.client.render;

import net.minecraft.class_12249;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_765;
import net.minecraft.class_7833;

/**
 * Resource-pack-independent Low Fire renderer.
 *
 * <p>Ground fire intentionally follows Minecraft's normal fire-floor/side
 * geometry while using the exact user-supplied low-fire artwork. The source
 * texture itself contains the low transparent area, so we do not squash it,
 * crop it, procedurally recolor it, or build a fake cluster/ring model.</p>
 */
public final class YogiDirectFireRenderer {

    private static final int FIRST_PERSON_COLOR = 0xE6FFFFFF;
    private static final int WORLD_COLOR = 0xFFFFFFFF;
    private static final float MODEL_HEIGHT = 1.4F; // vanilla template_fire_* = 22.4/16
    private static final float EDGE_EPSILON = 0.0008F;
    private static final float SOUL_VISIBLE_FRACTION = 2.0F / 16.0F;
    private static final float SOUL_VISIBLE_TOP_V = 14.0F / 16.0F;

    private YogiDirectFireRenderer() {
    }

    public static void renderFirstPerson(
            class_4587 matrices,
            class_4597 vertexConsumers
    ) {
        class_4588 consumer = vertexConsumers.method_73477(
                class_12249.method_75943(
                        YogiFireVisuals.runtimeTexture(YogiFireVisuals.FireKind.FIRST_PERSON)
                )
        );

        matrices.method_22903();
        LowFireTransforms.applyFirstPerson(matrices);

        for (int i = 0; i < 2; i++) {
            int side = i * 2 - 1;
            matrices.method_22903();
            matrices.method_46416(-side * 0.24F, -0.30F, 0.0F);
            matrices.method_22907(class_7833.field_40716.rotationDegrees(side * 10.0F));

            class_4587.class_4665 entry = matrices.method_23760();
            screenVertex(consumer, entry, -0.5F, -0.5F, -0.5F, 1.0F, 1.0F);
            screenVertex(consumer, entry,  0.5F, -0.5F, -0.5F, 0.0F, 1.0F);
            screenVertex(consumer, entry,  0.5F,  0.5F, -0.5F, 0.0F, 0.0F);
            screenVertex(consumer, entry, -0.5F,  0.5F, -0.5F, 1.0F, 0.0F);
            matrices.method_22909();
        }

        matrices.method_22909();
    }

    public static void renderGroundFire(
            class_4587 matrices,
            class_4597 vertexConsumers,
            double x,
            double y,
            double z,
            boolean soulFire
    ) {
        class_4588 consumer = vertexConsumers.method_73477(
                class_12249.method_76014(
                        YogiFireVisuals.runtimeTexture(
                                soulFire
                                        ? YogiFireVisuals.FireKind.SOUL_GROUND
                                        : YogiFireVisuals.FireKind.NORMAL_GROUND
                        )
                )
        );

        class_4587.class_4665 entry = matrices.method_23760();
        float ox = (float) x;
        float oy = (float) y;
        float oz = (float) z;

        if (soulFire) {
            emitRotatedXPlaneClipped(consumer, entry, ox, oy, oz, 0.55F, -22.5F);
            emitRotatedXPlaneClipped(consumer, entry, ox, oy, oz, 0.45F,  22.5F);
            emitRotatedZPlaneClipped(consumer, entry, ox, oy, oz, 0.55F, -22.5F);
            emitRotatedZPlaneClipped(consumer, entry, ox, oy, oz, 0.45F,  22.5F);

            emitEdgeZPlaneClipped(consumer, entry, ox, oy, oz, EDGE_EPSILON);
            emitEdgeZPlaneClipped(consumer, entry, ox, oy, oz, 1.0F - EDGE_EPSILON);
            emitEdgeXPlaneClipped(consumer, entry, ox, oy, oz, EDGE_EPSILON);
            emitEdgeXPlaneClipped(consumer, entry, ox, oy, oz, 1.0F - EDGE_EPSILON);
            return;
        }

        emitRotatedXPlane(consumer, entry, ox, oy, oz, 0.55F, -22.5F);
        emitRotatedXPlane(consumer, entry, ox, oy, oz, 0.45F,  22.5F);
        emitRotatedZPlane(consumer, entry, ox, oy, oz, 0.55F, -22.5F);
        emitRotatedZPlane(consumer, entry, ox, oy, oz, 0.45F,  22.5F);

        emitEdgeZPlane(consumer, entry, ox, oy, oz, EDGE_EPSILON);
        emitEdgeZPlane(consumer, entry, ox, oy, oz, 1.0F - EDGE_EPSILON);
        emitEdgeXPlane(consumer, entry, ox, oy, oz, EDGE_EPSILON);
        emitEdgeXPlane(consumer, entry, ox, oy, oz, 1.0F - EDGE_EPSILON);
    }

    private static void emitRotatedXPlaneClipped(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float localZ,
            float degrees
    ) {
        float[][] p = {
                rotateX(0.0F, 0.0F, localZ, degrees),
                rotateX(1.0F, 0.0F, localZ, degrees),
                rotateX(1.0F, MODEL_HEIGHT, localZ, degrees),
                rotateX(0.0F, MODEL_HEIGHT, localZ, degrees)
        };
        emitSoulVisibleSlice(consumer, entry, ox, oy, oz, p);
    }

    private static void emitRotatedZPlaneClipped(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float localX,
            float degrees
    ) {
        float[][] p = {
                rotateZ(localX, 0.0F, 0.0F, degrees),
                rotateZ(localX, 0.0F, 1.0F, degrees),
                rotateZ(localX, MODEL_HEIGHT, 1.0F, degrees),
                rotateZ(localX, MODEL_HEIGHT, 0.0F, degrees)
        };
        emitSoulVisibleSlice(consumer, entry, ox, oy, oz, p);
    }

    private static void emitEdgeZPlaneClipped(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float localZ
    ) {
        float[][] p = {
                {0.0F, 0.0F, localZ},
                {1.0F, 0.0F, localZ},
                {1.0F, MODEL_HEIGHT, localZ},
                {0.0F, MODEL_HEIGHT, localZ}
        };
        emitSoulVisibleSlice(consumer, entry, ox, oy, oz, p);
    }

    private static void emitEdgeXPlaneClipped(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float localX
    ) {
        float[][] p = {
                {localX, 0.0F, 0.0F},
                {localX, 0.0F, 1.0F},
                {localX, MODEL_HEIGHT, 1.0F},
                {localX, MODEL_HEIGHT, 0.0F}
        };
        emitSoulVisibleSlice(consumer, entry, ox, oy, oz, p);
    }

    private static void emitRotatedXPlane(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float localZ,
            float degrees
    ) {
        float[][] p = {
                rotateX(0.0F, 0.0F, localZ, degrees),
                rotateX(1.0F, 0.0F, localZ, degrees),
                rotateX(1.0F, MODEL_HEIGHT, localZ, degrees),
                rotateX(0.0F, MODEL_HEIGHT, localZ, degrees)
        };
        emitDoubleSidedQuad(consumer, entry, ox, oy, oz, p);
    }

    private static void emitRotatedZPlane(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float localX,
            float degrees
    ) {
        float[][] p = {
                rotateZ(localX, 0.0F, 0.0F, degrees),
                rotateZ(localX, 0.0F, 1.0F, degrees),
                rotateZ(localX, MODEL_HEIGHT, 1.0F, degrees),
                rotateZ(localX, MODEL_HEIGHT, 0.0F, degrees)
        };
        emitDoubleSidedQuad(consumer, entry, ox, oy, oz, p);
    }

    private static void emitEdgeZPlane(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float localZ
    ) {
        float[][] p = {
                {0.0F, 0.0F, localZ},
                {1.0F, 0.0F, localZ},
                {1.0F, MODEL_HEIGHT, localZ},
                {0.0F, MODEL_HEIGHT, localZ}
        };
        emitDoubleSidedQuad(consumer, entry, ox, oy, oz, p);
    }

    private static void emitEdgeXPlane(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float localX
    ) {
        float[][] p = {
                {localX, 0.0F, 0.0F},
                {localX, 0.0F, 1.0F},
                {localX, MODEL_HEIGHT, 1.0F},
                {localX, MODEL_HEIGHT, 0.0F}
        };
        emitDoubleSidedQuad(consumer, entry, ox, oy, oz, p);
    }

    private static float[] rotateX(float x, float y, float z, float degrees) {
        double radians = Math.toRadians(degrees);
        float dy = y - 0.5F;
        float dz = z - 0.5F;
        float ry = (float) (dy * Math.cos(radians) - dz * Math.sin(radians)) + 0.5F;
        float rz = (float) (dy * Math.sin(radians) + dz * Math.cos(radians)) + 0.5F;
        return new float[] {x, ry, rz};
    }

    private static float[] rotateZ(float x, float y, float z, float degrees) {
        double radians = Math.toRadians(degrees);
        float dx = x - 0.5F;
        float dy = y - 0.5F;
        float rx = (float) (dx * Math.cos(radians) - dy * Math.sin(radians)) + 0.5F;
        float ry = (float) (dx * Math.sin(radians) + dy * Math.cos(radians)) + 0.5F;
        return new float[] {rx, ry, z};
    }

    private static void emitDoubleSidedQuad(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float[][] p
    ) {
        worldVertex(consumer, entry, ox + p[0][0], oy + p[0][1], oz + p[0][2], 0.0F, 1.0F);
        worldVertex(consumer, entry, ox + p[1][0], oy + p[1][1], oz + p[1][2], 1.0F, 1.0F);
        worldVertex(consumer, entry, ox + p[2][0], oy + p[2][1], oz + p[2][2], 1.0F, 0.0F);
        worldVertex(consumer, entry, ox + p[3][0], oy + p[3][1], oz + p[3][2], 0.0F, 0.0F);

        worldVertex(consumer, entry, ox + p[1][0], oy + p[1][1], oz + p[1][2], 0.0F, 1.0F);
        worldVertex(consumer, entry, ox + p[0][0], oy + p[0][1], oz + p[0][2], 1.0F, 1.0F);
        worldVertex(consumer, entry, ox + p[3][0], oy + p[3][1], oz + p[3][2], 1.0F, 0.0F);
        worldVertex(consumer, entry, ox + p[2][0], oy + p[2][1], oz + p[2][2], 0.0F, 0.0F);
    }

    private static void emitSoulVisibleSlice(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float ox,
            float oy,
            float oz,
            float[][] p
    ) {
        float[][] q = {
                p[0],
                p[1],
                lerpPoint(p[1], p[2], SOUL_VISIBLE_FRACTION),
                lerpPoint(p[0], p[3], SOUL_VISIBLE_FRACTION)
        };

        worldVertex(consumer, entry, ox + q[0][0], oy + q[0][1], oz + q[0][2], 0.0F, 1.0F);
        worldVertex(consumer, entry, ox + q[1][0], oy + q[1][1], oz + q[1][2], 1.0F, 1.0F);
        worldVertex(consumer, entry, ox + q[2][0], oy + q[2][1], oz + q[2][2], 1.0F, SOUL_VISIBLE_TOP_V);
        worldVertex(consumer, entry, ox + q[3][0], oy + q[3][1], oz + q[3][2], 0.0F, SOUL_VISIBLE_TOP_V);

        worldVertex(consumer, entry, ox + q[1][0], oy + q[1][1], oz + q[1][2], 0.0F, 1.0F);
        worldVertex(consumer, entry, ox + q[0][0], oy + q[0][1], oz + q[0][2], 1.0F, 1.0F);
        worldVertex(consumer, entry, ox + q[3][0], oy + q[3][1], oz + q[3][2], 1.0F, SOUL_VISIBLE_TOP_V);
        worldVertex(consumer, entry, ox + q[2][0], oy + q[2][1], oz + q[2][2], 0.0F, SOUL_VISIBLE_TOP_V);
    }

    private static float[] lerpPoint(float[] from, float[] to, float t) {
        return new float[] {
                from[0] + (to[0] - from[0]) * t,
                from[1] + (to[1] - from[1]) * t,
                from[2] + (to[2] - from[2]) * t
        };
    }

    private static void screenVertex(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float x,
            float y,
            float z,
            float u,
            float v
    ) {
        consumer.method_56824(entry, x, y, z)
                .method_22913(u, v)
                .method_39415(FIRST_PERSON_COLOR);
    }

    private static void worldVertex(
            class_4588 consumer,
            class_4587.class_4665 entry,
            float x,
            float y,
            float z,
            float u,
            float v
    ) {
        consumer.method_56824(entry, x, y, z)
                .method_39415(WORLD_COLOR)
                .method_22913(u, v)
                .method_22922(class_4608.field_21444)
                .method_60803(class_765.field_32767)
                .method_60831(entry, 0.0F, 1.0F, 0.0F);
    }
}
