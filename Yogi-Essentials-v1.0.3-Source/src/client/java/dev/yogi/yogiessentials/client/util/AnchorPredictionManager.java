package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.optimizations.AnchorOptimizerModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.class_12206;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_4969;
import net.minecraft.class_5321;
import net.minecraft.class_638;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;









public final class AnchorPredictionManager {

    



    private static final long PREDICTION_TIMEOUT_TICKS = 30L;

    private static final Map<class_2338, Prediction> PREDICTIONS =
            new ConcurrentHashMap<>();

    private static AnchorOptimizerModule cachedModule;
    private static class_638 lastWorld;
    private static class_5321<class_1937> lastDimension;
    private static long clientTick;

    private AnchorPredictionManager() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                tick(client);
            } catch (Throwable ignored) {
                
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            
            PREDICTIONS.clear();
            lastWorld = null;
            lastDimension = null;
        });
    }

    




    public static void onManualAnchorUse(
            class_2680 state,
            class_1937 world,
            class_2338 pos,
            class_1657 player
    ) {
        try {
            if (world == null
                    || !world.method_8608()
                    || player == null
                    || player.method_7325()
                    || state == null
                    || pos == null
                    || !state.method_27852(class_2246.field_23152)) {
                return;
            }

            AnchorOptimizerModule module = module();
            if (module == null || !module.shouldPredictVisualRemoval()) {
                return;
            }

            int charges = state.method_11654(class_4969.field_23153);
            if (charges <= 0) {
                return;
            }

            




            boolean anchorWorks = world
                    .method_75728()
                    .method_75697(
                            class_12206.field_63757,
                            pos
                    );
            if (anchorWorks) {
                return;
            }

            predict(pos);
        } catch (Throwable ignored) {
            
        }
    }

    


    public static boolean shouldHideRender(class_2338 pos) {
        return pos != null && PREDICTIONS.containsKey(pos);
    }

    


    public static void onServerBlockUpdate(class_2338 pos) {
        if (pos == null) {
            return;
        }

        class_2338 immutable = pos.method_10062();
        if (PREDICTIONS.remove(immutable) != null) {
            scheduleRerender(immutable);
        }
    }

    


    public static void clearPredictions() {
        if (PREDICTIONS.isEmpty()) {
            return;
        }

        class_2338[] positions = PREDICTIONS.keySet().toArray(class_2338[]::new);
        PREDICTIONS.clear();
        for (class_2338 pos : positions) {
            scheduleRerender(pos);
        }
    }

    private static void predict(class_2338 pos) {
        class_2338 immutable = pos.method_10062();
        PREDICTIONS.put(immutable, new Prediction(clientTick));
        scheduleRerender(immutable);
    }

    private static void tick(class_310 client) {
        clientTick++;

        class_638 world = client == null ? null : client.field_1687;
        class_5321<class_1937> dimension = world == null ? null : world.method_27983();

        
        if (world != lastWorld || !Objects.equals(dimension, lastDimension)) {
            PREDICTIONS.clear();
            lastWorld = world;
            lastDimension = dimension;
        }

        if (world == null || PREDICTIONS.isEmpty()) {
            return;
        }

        for (Map.Entry<class_2338, Prediction> entry : PREDICTIONS.entrySet()) {
            class_2338 pos = entry.getKey();
            Prediction prediction = entry.getValue();

            boolean timedOut = clientTick - prediction.createdAtTick()
                    >= PREDICTION_TIMEOUT_TICKS;
            boolean noLongerAnchor = !world.method_8320(pos)
                    .method_27852(class_2246.field_23152);

            if ((timedOut || noLongerAnchor)
                    && PREDICTIONS.remove(pos, prediction)) {
                scheduleRerender(pos);
            }
        }
    }

    private static void scheduleRerender(class_2338 pos) {
        try {
            class_310 client = class_310.method_1551();
            if (client == null || client.field_1687 == null || client.field_1769 == null) {
                return;
            }

            client.field_1769.method_18146(
                    pos.method_10263(), pos.method_10264(), pos.method_10260(),
                    pos.method_10263(), pos.method_10264(), pos.method_10260()
            );
        } catch (Throwable ignored) {
            
        }
    }

    private static AnchorOptimizerModule module() {
        if (cachedModule == null && YogiEssentialsClient.getModuleManager() != null) {
            cachedModule = YogiEssentialsClient.getModuleManager()
                    .getModule(AnchorOptimizerModule.class);
        }
        return cachedModule;
    }

    private record Prediction(long createdAtTick) {
    }
}
