package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.optimizations.CrystalOptimizerModule;
import dev.yogi.yogiessentials.client.module.pvp.CombatSoundContext;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1511;
import net.minecraft.class_310;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CrystalPredictionManager {

    private static final long PREDICTION_TIMEOUT_MS = 350L;
    private static final Map<Integer, Long> hiddenUntil = new HashMap<>();
    private static CrystalOptimizerModule cached;

    private CrystalPredictionManager() {
    }

    public static void initialize() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            try {
                if (world.method_8608() && entity instanceof class_1511 crystal) {
                    if (world instanceof net.minecraft.class_638 clientWorld) {
                        CombatSoundContext.onCrystalAttack(clientWorld, crystal);
                    }
                    CrystalOptimizerModule module = module();
                    if (module != null && module.shouldPredictRemoval()) {
                        crystal.method_5648(true);
                        hiddenUntil.put(
                                crystal.method_5628(),
                                System.currentTimeMillis() + PREDICTION_TIMEOUT_MS
                        );
                    }
                }
            } catch (Throwable ignored) {
            }
            return class_1269.field_5811;
        });

        ClientTickEvents.END_CLIENT_TICK.register(CrystalPredictionManager::tick);
    }

    private static void tick(class_310 client) {
        if (hiddenUntil.isEmpty()) {
            return;
        }

        CrystalOptimizerModule module = module();
        if (client == null || client.field_1687 == null || module == null || !module.shouldPredictRemoval()) {
            restoreAll(client);
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> iterator = hiddenUntil.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> entry = iterator.next();
            class_1297 entity = client.field_1687.method_8469(entry.getKey());

            if (entity == null || entity.method_31481()) {
                iterator.remove();
                continue;
            }

            if (now >= entry.getValue()) {
                try {
                    entity.method_5648(false);
                } catch (Throwable ignored) {
                }
                iterator.remove();
            }
        }
    }

    private static void restoreAll(class_310 client) {
        if (client != null && client.field_1687 != null) {
            for (Integer entityId : hiddenUntil.keySet()) {
                class_1297 entity = client.field_1687.method_8469(entityId);
                if (entity != null && !entity.method_31481()) {
                    try {
                        entity.method_5648(false);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        hiddenUntil.clear();
    }

    private static CrystalOptimizerModule module() {
        if (cached == null && YogiEssentialsClient.getModuleManager() != null) {
            cached = YogiEssentialsClient.getModuleManager()
                    .getModule(CrystalOptimizerModule.class);
        }
        return cached;
    }
}
