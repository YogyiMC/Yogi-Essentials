package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.mixin.ClientPlayerInteractionManagerInvoker;
import dev.yogi.yogiessentials.client.module.optimizations.SpearOptimizerModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_12125;
import net.minecraft.class_1268;
import net.minecraft.class_1661;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1890;
import net.minecraft.class_1893;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2535;
import net.minecraft.class_2846;
import net.minecraft.class_2868;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_9334;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;





















public final class SpearLungeManager {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("YogiEssentials-SpearOptimizer");

    private static boolean comboWasDown;
    private static boolean externalLungeFixWarned;
    private static SpearOptimizerModule cached;
    private static int attemptSeq;

    private static final List<DiagSample> pending = new ArrayList<>();

    private SpearLungeManager() {
    }

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(SpearLungeManager::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(SpearLungeManager::onEndTick);
    }

    private static void onStartTick(class_310 client) {
        if (client.field_1724 == null
                || client.field_1687 == null
                || client.field_1761 == null
                || client.field_1755 != null) {
            comboWasDown = false;
            return;
        }

        SpearOptimizerModule module = module();
        if (module == null || !module.isEnabled()) {
            comboWasDown = false;
            return;
        }

        
        
        if (FabricLoader.getInstance().isModLoaded("lungefix")) {
            comboWasDown = false;
            if (!externalLungeFixWarned) {
                externalLungeFixWarned = true;
                LOGGER.warn(
                        "Standalone LungeFix is loaded. Disable/remove it before "
                                + "testing Yogi Essentials Spear Optimizer; the two "
                                + "implementations must not process the same input."
                );
            }
            return;
        }

        boolean attackDown = client.field_1690.field_1886.method_1434();
        int spearSlot = findPressedSpearSlot(client);
        boolean comboDown = attackDown && spearSlot != -1;

        if (!comboDown) {
            comboWasDown = false;
            return;
        }
        if (comboWasDown) {
            return;
        }
        comboWasDown = true;

        try {
            performManualBatchedLunge(client, module, spearSlot);
        } catch (Throwable failure) {
            
            if (module.diagnostics()) {
                LOGGER.warn("Spear Optimizer attempt failed safely", failure);
            }
        }
    }

    private static void performManualBatchedLunge(
            class_310 client,
            SpearOptimizerModule module,
            int spearSlot
    ) {
        class_746 player = client.field_1724;
        class_1661 inventory = player.method_31548();
        int oldSlot = inventory.method_67532();

        if (oldSlot < 0 || oldSlot >= 9
                || spearSlot < 0 || spearSlot >= 9
                || oldSlot == spearSlot) {
            return;
        }

        class_1799 oldStack = inventory.method_5438(oldSlot);
        class_1799 spearStack = inventory.method_5438(spearSlot);

        if (isVanillaSpear(oldStack) || !isVanillaSpear(spearStack)) {
            return;
        }
        if (!hasLunge(player, spearStack)) {
            
            return;
        }
        if (!spearStack.method_45435(client.field_1687.method_45162())) {
            return;
        }
        if (player.method_3144() || client.field_1761.method_2928()) {
            return;
        }

        class_12125 piercing =
                spearStack.method_58694(class_9334.field_63631);
        if (piercing == null) {
            return;
        }

        
        
        
        float preSwapCooldown = player.method_7261(0.0F);
        if (preSwapCooldown < 0.999F) {
            return;
        }

        class_634 networkHandler = client.method_1562();
        if (networkHandler == null) {
            return;
        }
        class_2535 connection = networkHandler.method_48296();
        if (connection == null || !connection.method_10758()) {
            return;
        }

        int id = ++attemptSeq;
        boolean diagnostics = module.diagnostics();
        class_243 beforeVelocity = player.method_18798();

        
        inventory.method_61496(spearSlot);

        
        
        
        ((ClientPlayerInteractionManagerInvoker) client.field_1761)
                .yogi$setLastSelectedSlot(spearSlot);

        
        
        
        
        
        connection.method_52906(
                new class_2868(spearSlot),
                null,
                false
        );
        connection.method_52906(
                new class_2846(
                        class_2846.class_2847.field_63165,
                        class_2338.field_10980,
                        class_2350.field_11033
                ),
                null,
                true
        );

        
        
        player.method_75124();
        player.method_75125();
        piercing.method_75243(player);
        player.method_6104(class_1268.field_5808);

        
        
        
        
        client.field_1690.field_1852[spearSlot].method_1436();
        client.field_1690.field_1886.method_1436();

        if (diagnostics) {
            LOGGER.info(
                    "[SpearOptimizer #{}] BATCHED_SLOT_STAB oldSlot={} spearSlot={} "
                            + "oldItem={} spearItem={} preCD={} preSpeed={} "
                            + "onGround={} tick={}",
                    id,
                    oldSlot,
                    spearSlot,
                    itemId(oldStack),
                    itemId(spearStack),
                    f(preSwapCooldown),
                    f(hSpeed(beforeVelocity)),
                    player.method_24828(),
                    client.field_1687.method_75260()
            );
            pending.add(new DiagSample(id, hSpeed(beforeVelocity)));
        }
    }

    private static void onEndTick(class_310 client) {
        if (pending.isEmpty() || client.field_1724 == null || client.field_1687 == null) {
            if (client.field_1724 == null || client.field_1687 == null) {
                pending.clear();
            }
            return;
        }

        class_746 player = client.field_1724;
        Iterator<DiagSample> iterator = pending.iterator();
        while (iterator.hasNext()) {
            DiagSample sample = iterator.next();
            class_243 velocity = player.method_18798();
            double speed = hSpeed(velocity);
            sample.maxHorizontalSpeed = Math.max(sample.maxHorizontalSpeed, speed);

            String label = switch (sample.stage) {
                case 0 -> "END_TICK";
                case 1 -> "TICK+1";
                case 2 -> "TICK+2";
                default -> "TICK+3";
            };

            LOGGER.info(
                    "[SpearOptimizer #{}] {} hSpeed={} vel=({}, {}, {}) onGround={}",
                    sample.id,
                    label,
                    f(speed),
                    f(velocity.field_1352),
                    f(velocity.field_1351),
                    f(velocity.field_1350),
                    player.method_24828()
            );

            sample.stage++;
            if (sample.stage > 3) {
                double increase = sample.maxHorizontalSpeed - sample.preHorizontalSpeed;
                LOGGER.info(
                        "[SpearOptimizer #{}] RESULT preSpeed={} maxPostSpeed={} increase={}",
                        sample.id,
                        f(sample.preHorizontalSpeed),
                        f(sample.maxHorizontalSpeed),
                        f(increase)
                );
                iterator.remove();
            }
        }
    }

    private static int findPressedSpearSlot(class_310 client) {
        for (int slot = 0; slot < 9; slot++) {
            if (!client.field_1690.field_1852[slot].method_1434()) {
                continue;
            }
            if (isVanillaSpear(client.field_1724.method_31548().method_5438(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean hasLunge(
            class_746 player,
            class_1799 stack
    ) {
        try {
            var enchantmentRegistry =
                    player
                            .method_56673()
                            .method_30530(class_7924.field_41265);

            var lunge =
                    enchantmentRegistry.method_46747(
                            class_1893.field_63420
                    );

            return class_1890.method_8225(
                    lunge,
                    stack
            ) > 0;
        } catch (Throwable ignored) {
            
            
            return false;
        }
    }

    private static boolean isVanillaSpear(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        class_1792 item = stack.method_7909();
        return item == class_1802.field_63384
                || item == class_1802.field_63385
                || item == class_1802.field_63386
                || item == class_1802.field_63387
                || item == class_1802.field_63388
                || item == class_1802.field_63389
                || item == class_1802.field_63390;
    }

    private static double hSpeed(class_243 velocity) {
        return Math.sqrt(
                velocity.field_1352 * velocity.field_1352
                        + velocity.field_1350 * velocity.field_1350
        );
    }

    private static String f(double value) {
        return String.format("%.5f", value);
    }

    private static String itemId(class_1799 stack) {
        try {
            return class_7923.field_41178.method_10221(stack.method_7909()).toString();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static SpearOptimizerModule module() {
        if (cached == null && YogiEssentialsClient.getModuleManager() != null) {
            cached = YogiEssentialsClient
                    .getModuleManager()
                    .getModule(SpearOptimizerModule.class);
        }
        return cached;
    }

    private static final class DiagSample {
        private final int id;
        private final double preHorizontalSpeed;
        private double maxHorizontalSpeed;
        private int stage;

        private DiagSample(int id, double preHorizontalSpeed) {
            this.id = id;
            this.preHorizontalSpeed = preHorizontalSpeed;
            this.maxHorizontalSpeed = preHorizontalSpeed;
        }
    }
}
