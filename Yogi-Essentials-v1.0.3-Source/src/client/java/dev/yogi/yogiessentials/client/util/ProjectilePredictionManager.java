package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.optimizations.PearlOptimizerModule;
import dev.yogi.yogiessentials.client.module.optimizations.PotOptimizerModule;
import dev.yogi.yogiessentials.client.module.optimizations.WindChargeOptimizerModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1676;
import net.minecraft.class_1802;
import net.minecraft.class_243;
import net.minecraft.class_3730;
import net.minecraft.class_638;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
















public final class ProjectilePredictionManager {

    private static final int MAX_AGE_TICKS = 6;
    private static final int PER_TYPE_COOLDOWN_TICKS = 4;
    private static final double MATCH_RADIUS = 6.0;

    private static final List<Ghost> ghosts = new ArrayList<>();

    private static PearlOptimizerModule pearl;
    private static WindChargeOptimizerModule wind;
    private static PotOptimizerModule pot;

    private static int pearlCooldown;
    private static int windCooldown;
    private static int potCooldown;
    private static int ghostIdSeq = -2000;

    private ProjectilePredictionManager() {
    }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            try {
                if (world.method_8608() && player != null) {
                    tryPredict(world, player, hand);
                }
            } catch (Throwable ignored) {
                
            }
            return class_1269.field_5811; 
        });

        
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == null || entity.method_5628() < 0) {
                return; 
            }
            reconcile(entity);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.field_1687 == null) {
                clearAll();
                return;
            }
            if (pearlCooldown > 0) pearlCooldown--;
            if (windCooldown > 0) windCooldown--;
            if (potCooldown > 0) potCooldown--;
            expire();
        });
    }

    private static void tryPredict(net.minecraft.class_1937 world,
                                   net.minecraft.class_1657 player,
                                   net.minecraft.class_1268 hand) {
        var stack = player.method_5998(hand);
        if (stack == null || stack.method_7960()) {
            return;
        }

        class_1299<?> type;
        float pitchOffset;
        float speed;

        if (stack.method_31574(class_1802.field_8634) && pearl() != null && pearl().shouldPredict()) {
            if (pearlCooldown > 0) return;
            type = class_1299.field_6082;
            pitchOffset = 0.0F;
            speed = 1.5F;
            pearlCooldown = PER_TYPE_COOLDOWN_TICKS;
        } else if (stack.method_31574(class_1802.field_49098) && wind() != null && wind().shouldPredict()) {
            if (windCooldown > 0) return;
            type = class_1299.field_47243;
            pitchOffset = 0.0F;
            speed = 1.5F;
            windCooldown = PER_TYPE_COOLDOWN_TICKS;
        } else if ((stack.method_31574(class_1802.field_8436) || stack.method_31574(class_1802.field_8150))
                && pot() != null && pot().shouldPredict()) {
            if (potCooldown > 0) return;
            type = stack.method_31574(class_1802.field_8150)
                    ? class_1299.field_56255   
                    : class_1299.field_56254;     
            pitchOffset = -20.0F;
            speed = 0.5F;
            potCooldown = PER_TYPE_COOLDOWN_TICKS;
        } else {
            return;
        }

        spawnGhost(world, player, type, pitchOffset, speed);
    }

    private static void spawnGhost(net.minecraft.class_1937 world,
                                   net.minecraft.class_1657 player,
                                   class_1299<?> type, float pitchOffset, float speed) {
        class_1297 ghost = type.method_5883(world, class_3730.field_16471); 
        if (ghost == null) {
            return;
        }

        ghost.method_5814(player.method_23317(), player.method_23320() - 0.1, player.method_23321());

        if (ghost instanceof class_1676 projectile) {
            
            projectile.method_24919(player, player.method_36455() + pitchOffset,
                    player.method_36454(), 0.0F, speed, 0.0F);
        }

        ghost.method_5838(ghostIdSeq--);
        ghost.field_5960 = true;

        if (world instanceof class_638 clientWorld) {
            clientWorld.method_53875(ghost); 
            ghosts.add(new Ghost(ghost, type, new class_243(player.method_23317(), player.method_23320(), player.method_23321())));
        }
    }

    private static void reconcile(class_1297 real) {
        Iterator<Ghost> it = ghosts.iterator();
        while (it.hasNext()) {
            Ghost g = it.next();
            double dx = real.method_23317() - g.origin.field_1352;
            double dy = real.method_23318() - g.origin.field_1351;
            double dz = real.method_23321() - g.origin.field_1350;
            if (g.type == real.method_5864()
                    && (dx * dx + dy * dy + dz * dz) <= MATCH_RADIUS * MATCH_RADIUS) {
                safeDiscard(g.entity);
                it.remove();
                return;
            }
        }
    }

    private static void expire() {
        Iterator<Ghost> it = ghosts.iterator();
        while (it.hasNext()) {
            Ghost g = it.next();
            g.age++;
            if (g.age > MAX_AGE_TICKS || g.entity == null || g.entity.method_31481()) {
                safeDiscard(g.entity);
                it.remove();
            }
        }
    }

    private static void clearAll() {
        for (Ghost g : ghosts) {
            safeDiscard(g.entity);
        }
        ghosts.clear();
    }

    private static void safeDiscard(class_1297 e) {
        try {
            if (e != null && !e.method_31481()) {
                e.method_31472();
            }
        } catch (Throwable ignored) {
        }
    }

    private static PearlOptimizerModule pearl() {
        if (pearl == null && YogiEssentialsClient.getModuleManager() != null) {
            pearl = YogiEssentialsClient.getModuleManager().getModule(PearlOptimizerModule.class);
        }
        return pearl;
    }

    private static WindChargeOptimizerModule wind() {
        if (wind == null && YogiEssentialsClient.getModuleManager() != null) {
            wind = YogiEssentialsClient.getModuleManager().getModule(WindChargeOptimizerModule.class);
        }
        return wind;
    }

    private static PotOptimizerModule pot() {
        if (pot == null && YogiEssentialsClient.getModuleManager() != null) {
            pot = YogiEssentialsClient.getModuleManager().getModule(PotOptimizerModule.class);
        }
        return pot;
    }

    private static final class Ghost {
        final class_1297 entity;
        final class_1299<?> type;
        final class_243 origin;
        int age;

        Ghost(class_1297 entity, class_1299<?> type, class_243 origin) {
            this.entity = entity;
            this.type = type;
            this.origin = origin;
        }
    }
}