package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.optimizations.ElytraOptimizerModule;
import dev.yogi.yogiessentials.client.module.optimizations.GeneralPvpOptimizerModule;
import dev.yogi.yogiessentials.client.module.optimizations.InputOptimizerModule;
import dev.yogi.yogiessentials.client.module.optimizations.MaceOptimizerModule;
import dev.yogi.yogiessentials.client.module.pvp.ToggleSprintModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_1304;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_3966;
import net.minecraft.class_746;













public final class OptimizationManager {

    private static InputOptimizerModule input;
    private static MaceOptimizerModule mace;
    private static GeneralPvpOptimizerModule gpvp;
    private static ElytraOptimizerModule elytra;

    private OptimizationManager() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(OptimizationManager::tick);
    }

    private static void tick(class_310 client) {
        if (client == null || client.field_1724 == null
                || client.field_1687 == null || client.field_1690 == null) {
            return;
        }
        if (YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        class_746 player = client.field_1724;

        
        if (elytra() != null && elytra().shouldPredictStop() && player.method_6128()
                && !hasWorkingElytra(player)) {
            try {
                player.method_66281(); 
            } catch (Throwable ignored) {
            }
        }

        
        // Toggle Sprint is authoritative when enabled. Its manager runs every
        ToggleSprintModule toggleSprint = YogiEssentialsClient.getModuleManager().getModule(ToggleSprintModule.class);
        if (toggleSprint != null && toggleSprint.isEnabled()) {
            return;
        }

        boolean wantSprint = false;

        if (input() != null) {
            if (input().autoSprintRecovery() && canSprintIntent(client, false)) {
                wantSprint = true;
            }
            if (input().keepSprintThroughUse() && canSprintIntent(client, true)) {
                wantSprint = true;
            }
        }
        if (!wantSprint && mace() != null && mace().shouldKeepSprint()
                && holdingMace(player) && canSprintIntent(client, true)) {
            wantSprint = true;
        }
        if (!wantSprint && gpvp() != null && gpvp().shouldKeepCombatSprint()
                && aimingAtPlayer(client) && canSprintIntent(client, false)) {
            wantSprint = true;
        }

        if (wantSprint && !player.method_5624()) {
            player.method_5728(true);
        }
    }

    



    private static boolean canSprintIntent(class_310 client, boolean allowItemUse) {
        class_746 player = client.field_1724;
        if (player == null || client.field_1690 == null) {
            return false;
        }
        if (player.method_5624()) {
            return false;
        }
        if (player.method_5715() || player.field_5976) {
            return false;
        }
        if (client.field_1690.field_1886.method_1434()) {
            return false;
        }
        if (!allowItemUse && player.method_6115()) {
            return false;
        }
        if (!client.field_1690.field_1867.method_1434()
                || !client.field_1690.field_1894.method_1434()) {
            return false;
        }
        return player.method_7344().method_7586() > 6;
    }

    private static boolean hasWorkingElytra(class_1657 player) {
        try {
            class_1799 chest = player.method_6118(class_1304.field_6174); 
            return chest != null && chest.method_31574(class_1802.field_8833)
                    && (chest.method_7936() - chest.method_7919() > 1);
        } catch (Throwable ignored) {
            
            return true;
        }
    }

    private static boolean holdingMace(class_1657 player) {
        return player.method_6047().method_31574(class_1802.field_49814)
                || player.method_6079().method_31574(class_1802.field_49814);
    }

    private static boolean aimingAtPlayer(class_310 client) {
        return client.field_1765 instanceof class_3966 hit
                && hit.method_17782() instanceof class_1657;
    }

    private static InputOptimizerModule input() {
        if (input == null && YogiEssentialsClient.getModuleManager() != null) {
            input = YogiEssentialsClient.getModuleManager().getModule(InputOptimizerModule.class);
        }
        return input;
    }

    private static MaceOptimizerModule mace() {
        if (mace == null && YogiEssentialsClient.getModuleManager() != null) {
            mace = YogiEssentialsClient.getModuleManager().getModule(MaceOptimizerModule.class);
        }
        return mace;
    }

    private static GeneralPvpOptimizerModule gpvp() {
        if (gpvp == null && YogiEssentialsClient.getModuleManager() != null) {
            gpvp = YogiEssentialsClient.getModuleManager().getModule(GeneralPvpOptimizerModule.class);
        }
        return gpvp;
    }

    private static ElytraOptimizerModule elytra() {
        if (elytra == null && YogiEssentialsClient.getModuleManager() != null) {
            elytra = YogiEssentialsClient.getModuleManager().getModule(ElytraOptimizerModule.class);
        }
        return elytra;
    }
}
