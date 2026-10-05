package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.module.performance.YogiTerrainScheduler;
import net.minecraft.class_4184;
import net.minecraft.class_761;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_761.class)
public abstract class WorldRendererTerrainBackendMixin {
    @Shadow
    public abstract void scheduleChunkRender(int x, int y, int z, boolean important);

    @Inject(method = "scheduleChunkRender(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$queueChunkRebuild(int x, int y, int z, boolean important, CallbackInfo ci) {
        if (YogiTerrainScheduler.shouldIntercept() && !YogiTerrainScheduler.isFlushing()) {
            YogiTerrainScheduler.enqueue(x, y, z, important);
            ci.cancel();
        }
    }

    @Inject(method = "updateChunks", at = @At("HEAD"))
    private void yogiessentials$flushTerrainWork(class_4184 camera, CallbackInfo ci) {
        if (YogiTerrainScheduler.shouldIntercept()) {
            YogiTerrainScheduler.flush(this::scheduleChunkRender);
        }
    }

    @Inject(method = "updateChunks", at = @At("TAIL"))
    private void yogiessentials$flushLateTerrainWork(class_4184 camera, CallbackInfo ci) {
        if (YogiTerrainScheduler.shouldIntercept()) {
            YogiTerrainScheduler.flush(this::scheduleChunkRender);
        }
    }

    @Inject(method = "reload", at = @At("HEAD"))
    private void yogiessentials$clearTerrainQueueOnReload(CallbackInfo ci) {
        YogiTerrainScheduler.clear();
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void yogiessentials$clearTerrainQueue(CallbackInfo ci) {
        YogiTerrainScheduler.clear();
    }
}
