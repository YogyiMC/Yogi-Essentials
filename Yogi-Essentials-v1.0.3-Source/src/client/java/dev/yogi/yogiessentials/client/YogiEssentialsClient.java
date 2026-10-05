package dev.yogi.yogiessentials.client;

import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.hud.HudManager;
import dev.yogi.yogiessentials.client.keybind.KeybindManager;
import dev.yogi.yogiessentials.client.module.ModuleManager;
import dev.yogi.yogiessentials.client.render.YogiViewmodelGuiElementRenderer;
import dev.yogi.yogiessentials.client.render.YogiLowFireGuiElementRenderer;

import dev.yogi.yogiessentials.client.module.visual.LowFireModule;
import dev.yogi.yogiessentials.client.module.visual.LowShieldModule;
import dev.yogi.yogiessentials.client.module.visual.ItemViewmodelModule;
import dev.yogi.yogiessentials.client.module.visual.ExtendedFovModule;
import dev.yogi.yogiessentials.client.module.visual.SideShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SmallBlockModule;
import dev.yogi.yogiessentials.client.module.visual.SmallTotemModule;
import dev.yogi.yogiessentials.client.module.visual.NametagCustomizerModule;

import dev.yogi.yogiessentials.client.module.hud.ArmorHudModule;
import dev.yogi.yogiessentials.client.module.hud.AttackCooldownHudModule;
import dev.yogi.yogiessentials.client.module.hud.CoordinatesHudModule;
import dev.yogi.yogiessentials.client.module.hud.FpsHudModule;
import dev.yogi.yogiessentials.client.module.hud.PingHudModule;
import dev.yogi.yogiessentials.client.module.hud.PotionEffectsHudModule;
import dev.yogi.yogiessentials.client.module.hud.ShieldStatusHudModule;
import dev.yogi.yogiessentials.client.module.hud.SaturationHudModule;
import dev.yogi.yogiessentials.client.module.hud.TotemHudModule;

import dev.yogi.yogiessentials.client.module.pvp.ArmorBreakWarningModule;
import dev.yogi.yogiessentials.client.module.pvp.CrosshairModule;
import dev.yogi.yogiessentials.client.module.pvp.EffectExpiryWarningModule;
import dev.yogi.yogiessentials.client.module.pvp.FoodWarningModule;
import dev.yogi.yogiessentials.client.module.pvp.LowHealthWarningModule;
import dev.yogi.yogiessentials.client.module.pvp.LowTotemWarningModule;
import dev.yogi.yogiessentials.client.module.pvp.NoDistortionModule;
import dev.yogi.yogiessentials.client.module.pvp.NoFovEffectsModule;
import dev.yogi.yogiessentials.client.module.pvp.NoHurtCameraModule;
import dev.yogi.yogiessentials.client.module.pvp.NoPotionParticlesModule;
import dev.yogi.yogiessentials.client.module.pvp.MotionBlurModule;
import dev.yogi.yogiessentials.client.module.pvp.HitRegistrationModule;
import dev.yogi.yogiessentials.client.module.pvp.ReachDisplayModule;
import dev.yogi.yogiessentials.client.module.pvp.ToggleSprintModule;
import dev.yogi.yogiessentials.client.module.pvp.CombatHitboxesModule;
import dev.yogi.yogiessentials.client.module.pvp.WarningQueueModule;

import dev.yogi.yogiessentials.client.module.fixes.CenteredCrosshairFixModule;
import dev.yogi.yogiessentials.client.module.fixes.FocusRecoveryFixModule;
import dev.yogi.yogiessentials.client.module.fixes.FpsRecoveryFixModule;
import dev.yogi.yogiessentials.client.module.fixes.StuckKeyFixModule;
import dev.yogi.yogiessentials.client.module.fixes.WorldUnloadCleanupModule;
import dev.yogi.yogiessentials.client.module.fixes.FrameTransitionFixModule;
import dev.yogi.yogiessentials.client.module.fixes.WorldLoadStutterFixModule;
import dev.yogi.yogiessentials.client.module.smp.StreamerPrivacyModule;
import dev.yogi.yogiessentials.client.module.smp.FullbrightModule;
import dev.yogi.yogiessentials.client.module.smp.FogCustomizerModule;
import dev.yogi.yogiessentials.client.module.smp.WaypointModule;
import dev.yogi.yogiessentials.client.module.smp.ZoomModule;
import dev.yogi.yogiessentials.client.module.smp.BossbarModule;
import dev.yogi.yogiessentials.client.module.smp.ShulkerBoxPreviewModule;
import dev.yogi.yogiessentials.client.module.chat.*;
import dev.yogi.yogiessentials.client.util.ChatEnhancementManager;
import dev.yogi.yogiessentials.client.util.InputRecoveryManager;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import dev.yogi.yogiessentials.client.util.MotionBlurManager;
import dev.yogi.yogiessentials.client.util.CrashAssistantManager;

import dev.yogi.yogiessentials.client.module.performance.BorderlessFullscreenModule;
import dev.yogi.yogiessentials.client.module.performance.LowEntityDistanceModule;
import dev.yogi.yogiessentials.client.module.performance.MinimalParticlesModule;
import dev.yogi.yogiessentials.client.module.performance.MemoryStabilityModule;
import dev.yogi.yogiessentials.client.module.performance.NoAmbientOcclusionModule;
import dev.yogi.yogiessentials.client.module.performance.NoBiomeBlendModule;
import dev.yogi.yogiessentials.client.module.performance.NoChunkFadeModule;
import dev.yogi.yogiessentials.client.module.performance.NoCloudsModule;
import dev.yogi.yogiessentials.client.module.performance.NoEntityShadowsModule;
import dev.yogi.yogiessentials.client.module.performance.NoVignetteModule;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptionManager;
import dev.yogi.yogiessentials.client.module.performance.FramePacingModule;
import dev.yogi.yogiessentials.client.module.performance.ChunkUpdateStabilizerModule;
import dev.yogi.yogiessentials.client.module.performance.SimulationDistanceOptimizerModule;
import dev.yogi.yogiessentials.client.module.performance.ReducedMipmapModule;
import dev.yogi.yogiessentials.client.module.performance.AdaptiveEntityBudgetModule;
import dev.yogi.yogiessentials.client.module.performance.HighDistanceGuardModule;
import dev.yogi.yogiessentials.client.module.performance.MemoryPressureGuardModule;
import dev.yogi.yogiessentials.client.module.performance.ParticleBudgetModule;
import dev.yogi.yogiessentials.client.module.performance.EntityRenderDistanceModule;
import dev.yogi.yogiessentials.client.module.performance.DynamicBlockEntityOptimizerModule;
import dev.yogi.yogiessentials.client.module.performance.HudUpdateOptimizerModule;
import dev.yogi.yogiessentials.client.module.performance.WeatherRenderBudgetModule;
import dev.yogi.yogiessentials.client.module.performance.FastLeavesModule;
import dev.yogi.yogiessentials.client.module.performance.FastTransparencyModule;
import dev.yogi.yogiessentials.client.module.performance.MenuBlurOptimizerModule;
import dev.yogi.yogiessentials.client.module.performance.AnisotropicSamplingOptimizerModule;
import dev.yogi.yogiessentials.client.module.performance.TextureFilteringOptimizerModule;
import dev.yogi.yogiessentials.client.module.performance.ViewportCullingModule;
import dev.yogi.yogiessentials.client.module.performance.RenderSubmissionBudgetModule;
import dev.yogi.yogiessentials.client.module.performance.HighSpeedChunkGuardModule;
import dev.yogi.yogiessentials.client.module.performance.MicrostutterGuardModule;
import dev.yogi.yogiessentials.client.module.performance.AdaptiveChunkPacingModule;
import dev.yogi.yogiessentials.client.module.performance.RenderThreadPriorityGuardModule;
import dev.yogi.yogiessentials.client.module.performance.AsyncChatIndexModule;
import dev.yogi.yogiessentials.client.module.performance.CustomTerrainRendererModule;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class YogiEssentialsClient
        implements ClientModInitializer {

    public static final String MOD_ID =
            "yogiessentials";

    private static ModuleManager moduleManager;
    private static boolean wasPlayerAlive;

    @Override
    public void onInitializeClient() {

        moduleManager =
                new ModuleManager();

        CrashAssistantManager.disableAllRecovery();










        SpecialGuiElementRegistry.register(
                context ->
                        new YogiViewmodelGuiElementRenderer(
                                context.vertexConsumers()
                        )
        );

        SpecialGuiElementRegistry.register(
                context ->
                        new YogiLowFireGuiElementRenderer(
                                context.vertexConsumers()
                        )
        );

        
        moduleManager.register(new LowFireModule());
        moduleManager.register(new LowShieldModule());
        moduleManager.register(new SideShieldModule());
        moduleManager.register(new SmallBlockModule());
        moduleManager.register(new SmallTotemModule());
        moduleManager.register(new ItemViewmodelModule());
        moduleManager.register(new ExtendedFovModule());
        moduleManager.register(new NametagCustomizerModule());

        
        moduleManager.register(new FpsHudModule());
        moduleManager.register(new PingHudModule());
        moduleManager.register(new CoordinatesHudModule());
        moduleManager.register(new ArmorHudModule());
        moduleManager.register(new TotemHudModule());
        moduleManager.register(new ShieldStatusHudModule());
        moduleManager.register(new AttackCooldownHudModule());
        moduleManager.register(new PotionEffectsHudModule());
        moduleManager.register(new SaturationHudModule());

        
        moduleManager.register(new NoHurtCameraModule());
        moduleManager.register(new NoFovEffectsModule());
        moduleManager.register(new NoDistortionModule());
        moduleManager.register(new CrosshairModule());
        moduleManager.register(new ReachDisplayModule());
        moduleManager.register(new CombatHitboxesModule());
        moduleManager.register(new NoPotionParticlesModule());
        moduleManager.register(new MotionBlurModule());
        moduleManager.register(new HitRegistrationModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.pvp.CombatSoundMixerModule());
        moduleManager.register(new ToggleSprintModule());

        moduleManager.register(new ArmorBreakWarningModule());
        moduleManager.register(new LowTotemWarningModule());
        moduleManager.register(new LowHealthWarningModule());
        moduleManager.register(new EffectExpiryWarningModule());
        moduleManager.register(new FoodWarningModule());
        moduleManager.register(new WarningQueueModule());

        
        moduleManager.register(new StreamerPrivacyModule());
        moduleManager.register(new WaypointModule());
        moduleManager.register(new ZoomModule());
        moduleManager.register(new FullbrightModule());
        moduleManager.register(new FogCustomizerModule());
        moduleManager.register(new BossbarModule());
        moduleManager.register(new ShulkerBoxPreviewModule());

        moduleManager.register(new ChatPingsModule());
        moduleManager.register(new ChatSearchBarModule());
        moduleManager.register(new ChatBackgroundOpacityModule());
        moduleManager.register(new ChatSizeModule());
        moduleManager.register(new RepeatedMessageStackingModule());
        moduleManager.register(new ClickToCopyModule());
        moduleManager.register(new ChatHistoryLimitModule());
        moduleManager.register(new MessageHighlightingModule());

        
        moduleManager.register(new CenteredCrosshairFixModule());
        moduleManager.register(new StuckKeyFixModule());
        moduleManager.register(new FocusRecoveryFixModule());
        moduleManager.register(new FpsRecoveryFixModule());
        moduleManager.register(new WorldUnloadCleanupModule());
        moduleManager.register(new FrameTransitionFixModule());
        moduleManager.register(new WorldLoadStutterFixModule());

        
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.InputOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.MaceOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.PearlOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.CrystalOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.PotOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.GeneralPvpOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.ElytraOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.WindChargeOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.SpearOptimizerModule());
        moduleManager.register(new dev.yogi.yogiessentials.client.module.optimizations.AnchorOptimizerModule());

        
        moduleManager.register(new BorderlessFullscreenModule());
        moduleManager.register(new MemoryStabilityModule());
        moduleManager.register(new MinimalParticlesModule());
        moduleManager.register(new NoEntityShadowsModule());
        moduleManager.register(new NoCloudsModule());
        moduleManager.register(new LowEntityDistanceModule());
        moduleManager.register(new NoBiomeBlendModule());
        moduleManager.register(new NoAmbientOcclusionModule());
        moduleManager.register(new NoVignetteModule());
        moduleManager.register(new NoChunkFadeModule());
        moduleManager.register(new ChunkUpdateStabilizerModule());
        moduleManager.register(new SimulationDistanceOptimizerModule());
        moduleManager.register(new FramePacingModule());
        moduleManager.register(new ReducedMipmapModule());
        moduleManager.register(new AdaptiveEntityBudgetModule());
        moduleManager.register(new HighDistanceGuardModule());
        moduleManager.register(new MemoryPressureGuardModule());
        moduleManager.register(new ParticleBudgetModule());
        moduleManager.register(new EntityRenderDistanceModule());
        moduleManager.register(new DynamicBlockEntityOptimizerModule());
        moduleManager.register(new HudUpdateOptimizerModule());
        moduleManager.register(new WeatherRenderBudgetModule());
        moduleManager.register(new FastLeavesModule());
        moduleManager.register(new FastTransparencyModule());
        moduleManager.register(new MenuBlurOptimizerModule());
        moduleManager.register(new AnisotropicSamplingOptimizerModule());
        moduleManager.register(new TextureFilteringOptimizerModule());
        moduleManager.register(new ViewportCullingModule());
        moduleManager.register(new RenderSubmissionBudgetModule());
        moduleManager.register(new HighSpeedChunkGuardModule());
        moduleManager.register(new MicrostutterGuardModule());
        moduleManager.register(new AdaptiveChunkPacingModule());
        moduleManager.register(new RenderThreadPriorityGuardModule());
        moduleManager.register(new AsyncChatIndexModule());
        moduleManager.register(new CustomTerrainRendererModule());

        ConfigManager.load();
        CustomTerrainRendererModule terrainRenderer = moduleManager.getModule(CustomTerrainRendererModule.class);
        if (terrainRenderer != null) terrainRenderer.refreshBackendSelection();
        FrameTelemetry.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.field_1724 == null || client.field_1687 == null) {
                wasPlayerAlive = false;
            } else {
                boolean aliveNow = client.field_1724.method_6032() > 0.0F;
                if (wasPlayerAlive && !aliveNow) {
                    WaypointModule waypoints = moduleManager.getModule(WaypointModule.class);
                    if (waypoints != null && waypoints.isEnabled() && waypoints.getDeathWaypoints().get()) {
                        dev.yogi.yogiessentials.client.util.WaypointManager.addDeathWaypoint();
                    }
                }
                wasPlayerAlive = aliveNow;
            }
            BorderlessFullscreenModule borderlessFullscreen = moduleManager.getModule(BorderlessFullscreenModule.class);
            if (borderlessFullscreen != null) borderlessFullscreen.reconcileWindow();

            FramePacingModule pacing = moduleManager.getModule(FramePacingModule.class);
            if (pacing != null) pacing.tick(client);

            AdaptiveEntityBudgetModule entityBudget = moduleManager.getModule(AdaptiveEntityBudgetModule.class);
            if (entityBudget != null) entityBudget.tick(client);

            HighDistanceGuardModule highDistance = moduleManager.getModule(HighDistanceGuardModule.class);
            if (highDistance != null) highDistance.tick(client);

            MemoryPressureGuardModule memoryGuard = moduleManager.getModule(MemoryPressureGuardModule.class);
            if (memoryGuard != null) memoryGuard.tick(client);

            ParticleBudgetModule particleBudget = moduleManager.getModule(ParticleBudgetModule.class);
            if (particleBudget != null) particleBudget.beginTick(client);

            EntityRenderDistanceModule entityRenderDistance = moduleManager.getModule(EntityRenderDistanceModule.class);
            if (entityRenderDistance != null) entityRenderDistance.tick(client);

            DynamicBlockEntityOptimizerModule blockEntityOptimizer = moduleManager.getModule(DynamicBlockEntityOptimizerModule.class);
            if (blockEntityOptimizer != null) blockEntityOptimizer.tick(client);

            HudUpdateOptimizerModule hudUpdateOptimizer = moduleManager.getModule(HudUpdateOptimizerModule.class);
            if (hudUpdateOptimizer != null) hudUpdateOptimizer.tick(client);

            HighSpeedChunkGuardModule highSpeedChunkGuard = moduleManager.getModule(HighSpeedChunkGuardModule.class);
            if (highSpeedChunkGuard != null) highSpeedChunkGuard.tick(client);

            MicrostutterGuardModule microstutterGuard = moduleManager.getModule(MicrostutterGuardModule.class);
            if (microstutterGuard != null) microstutterGuard.tick(client);

            AdaptiveChunkPacingModule chunkPacing = moduleManager.getModule(AdaptiveChunkPacingModule.class);
            if (chunkPacing != null) chunkPacing.tick(client);

            RenderThreadPriorityGuardModule renderPriority = moduleManager.getModule(RenderThreadPriorityGuardModule.class);
            if (renderPriority != null) renderPriority.tick(client);

            FrameTransitionFixModule frameTransitionFix = moduleManager.getModule(FrameTransitionFixModule.class);
            if (frameTransitionFix != null) frameTransitionFix.tick(client);

            WorldLoadStutterFixModule worldLoadStutterFix = moduleManager.getModule(WorldLoadStutterFixModule.class);
            if (worldLoadStutterFix != null) worldLoadStutterFix.tick(client);
        });
        PerformanceOptionManager.initialize();
        MotionBlurManager.initialize();

        KeybindManager.initialize();
        dev.yogi.yogiessentials.client.util.ToggleSprintManager.initialize();
        HudManager.initialize();
        InputRecoveryManager.initialize();
        dev.yogi.yogiessentials.client.util.FpsRecoveryManager.initialize();
        dev.yogi.yogiessentials.client.util.WelcomeManager.initialize();
        dev.yogi.yogiessentials.client.util.OptimizationManager.initialize();
        dev.yogi.yogiessentials.client.util.CrystalPredictionManager.initialize();
        dev.yogi.yogiessentials.client.util.ProjectilePredictionManager.initialize();
        dev.yogi.yogiessentials.client.util.SpearLungeManager.initialize();
        dev.yogi.yogiessentials.client.util.AnchorPredictionManager.initialize();
        dev.yogi.yogiessentials.client.util.WaypointManager.initialize();
        dev.yogi.yogiessentials.client.render.YogiGroundFireRenderer.initialize();
        dev.yogi.yogiessentials.client.util.ZoomManager.initialize();
        ChatEnhancementManager.initialize();

        Runtime
                .getRuntime()
                .addShutdownHook(
                        new Thread(
                                ConfigManager::save,
                                "Yogi Essentials Config Save"
                        )
                );

        System.out.println(
                "[Yogi Essentials] Loaded "
                        + moduleManager
                        .getModules()
                        .size()
                        + " modules."
        );
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }
}
