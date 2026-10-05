package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.HitRegistrationModule;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.minecraft.class_1294;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1531;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_8143;

/**
 * Immediate local hit feedback based on the core behavior of BetterHitreg.
 *
 * The manager never cancels or changes the outgoing vanilla attack, never
 * changes attack reach and never changes server-side damage. It only predicts
 * the feedback that the server normally sends later, then optionally suppresses
 * the duplicate remote hurt-animation packet when it arrives.
 *
 * The reference mod's "safe regs" gating is intentionally not implemented.
 */
public final class HitRegistrationManager {
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "Yogi Essentials Hit Feedback");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private static int lastTargetId = Integer.MIN_VALUE;
    private static long lastPredictionTime;

    private HitRegistrationManager() {
    }

    public static void onAttack(class_1657 attacker, class_1297 target) {
        HitRegistrationModule module = getModule();
        class_310 client = class_310.method_1551();

        if (module == null || !module.isEnabled()
                || attacker == null || attacker != client.field_1724
                || client.field_1687 == null
                || !(target instanceof class_1309 living)
                || target instanceof class_1531
                || !target.method_5805()
                || target.method_5655()) {
            return;
        }

        if (module.getPlayersOnly().get() && !(target instanceof class_1657)) {
            return;
        }


        final int targetId = target.method_5628();
        final float cooldown = attacker.method_7261(0.5F);
        final boolean sprintHit = cooldown > 0.9F && attacker.method_5624();
        final boolean criticalHit = cooldown > 0.9F
                && !sprintHit
                && attacker.field_6017 > 0.0F
                && !attacker.method_24828()
                && !attacker.method_6101()
                && !attacker.method_5799()
                && !attacker.method_5765()
                && !attacker.method_6059(class_1294.field_5919);

        lastTargetId = targetId;
        lastPredictionTime = System.currentTimeMillis();

        long delayMs = Math.round(module.getFeedbackDelay().get());
        Runnable feedback = () -> client.execute(() -> {
            class_1297 current = client.field_1687 == null ? null : client.field_1687.method_8469(targetId);
            if (!(current instanceof class_1309 currentLiving) || !current.method_5805()) {
                return;
            }
            playImmediateFeedback(module, attacker, currentLiving, cooldown, sprintHit, criticalHit);
        });

        if (delayMs <= 0L) {
            feedback.run();
        } else {
            SCHEDULER.schedule(feedback, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private static void playImmediateFeedback(
            HitRegistrationModule module,
            class_1657 attacker,
            class_1309 target,
            float cooldown,
            boolean sprintHit,
            boolean criticalHit
    ) {
        class_310 client = class_310.method_1551();
        if (client.field_1687 == null) {
            return;
        }

        if (module.getHurtAnimation().get()) {
            target.field_6254 = 10;
            target.field_6235 = 10;
            target.method_5879(attacker.method_36454());
        }

        if (module.getHitSounds().get()) {
            class_3414 attackSound = chooseAttackSound(cooldown, sprintHit, criticalHit);
            float volume = module.getSoundVolume().get().floatValue() / 100.0F;
            client.field_1687.method_8486(
                    target.method_23317(), target.method_23318(), target.method_23321(),
                    attackSound, class_3419.field_15248,
                    volume, 1.0F, false
            );

            if (target instanceof class_1657) {
                client.field_1687.method_8486(
                        target.method_23317(), target.method_23318(), target.method_23321(),
                        class_3417.field_15115, class_3419.field_15248,
                        volume, 1.0F, false
                );
            }
        }

        if (module.getParticles().get()) {
            if (criticalHit) {
                attacker.method_7277(target);
            }
            if (!criticalHit && cooldown > 0.9F) {
                attacker.method_7304(target);
            }
        }
    }

    private static class_3414 chooseAttackSound(
            float cooldown,
            boolean sprintHit,
            boolean criticalHit
    ) {
        if (cooldown <= 0.9F) {
            return class_3417.field_14625;
        }
        if (criticalHit) {
            return class_3417.field_15016;
        }
        if (sprintHit) {
            return class_3417.field_14999;
        }
        return class_3417.field_14840;
    }

    /**
     * Returns true only for the server hurt-animation packet that duplicates a
     * recent local prediction. The packet carries presentation only for the
     * remote target; actual health/damage authority remains server-side.
     */
    public static boolean shouldSuppressDamageFeedback(class_8143 packet) {
        HitRegistrationModule module = getModule();
        if (module == null || !module.isEnabled()
                || !module.getHurtAnimation().get()
                || !module.getSuppressDuplicateAnimation().get()
                || packet == null
                || packet.comp_1267() != lastTargetId) {
            return false;
        }

        long age = System.currentTimeMillis() - lastPredictionTime;
        long window = Math.round(module.getDuplicateWindow().get());
        return age >= 0L && age <= window;
    }

    public static void reset() {
        lastTargetId = Integer.MIN_VALUE;
        lastPredictionTime = 0L;
    }

    private static HitRegistrationModule getModule() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient.getModuleManager().getModule(HitRegistrationModule.class);
    }
}
