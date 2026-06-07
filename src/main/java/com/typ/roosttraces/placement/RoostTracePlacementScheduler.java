package com.typ.roosttraces.placement;

import java.util.Collection;

import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;
import com.typ.roosttraces.roost.PendingRoost;
import com.typ.roosttraces.roost.RoostTraceSavedData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class RoostTracePlacementScheduler {
    private static final int RETRY_DELAY_TICKS = 20 * 30;

    private RoostTracePlacementScheduler() {}

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!RoostTracesConfig.PLACEMENT_ENABLED.get()) return;

        MinecraftServer server = event.getServer();
        if (RoostTracesConfig.PLACE_AFTER_CHUNK_GENERATED.get()) {
            int remaining = RoostTracesConfig.MAX_PENDING_ROOSTS_PER_TICK.get();
            for (ServerLevel level : server.getAllLevels()) {
                if (remaining <= 0) break;
                remaining = processLevel(level, remaining);
            }
        }
    }

    private static int processLevel(ServerLevel level, int remainingBudget) {
        RoostTraceSavedData data = RoostTraceSavedData.get(level);
        Collection<PendingRoost> pending = data.pendingSnapshot();
        if (pending.isEmpty()) return remainingBudget;

        long gameTime = level.getGameTime();
        int remaining = remainingBudget;
        for (PendingRoost roost : pending) {
            if (remaining <= 0) break;
            if (roost.nextAttemptGameTime() > gameTime) continue;
            ChunkPos chunk = roost.chunkPos();
            if (!level.hasChunk(chunk.x, chunk.z)) continue;

            RoostTraceNodePlacer.PlacementResult result = RoostTraceNodePlacer.place(level, roost);
            if (result == RoostTraceNodePlacer.PlacementResult.PLACED
                    || result == RoostTraceNodePlacer.PlacementResult.ALREADY_EXISTS) {
                remaining--;
                continue;
            }

            long delay = RETRY_DELAY_TICKS * Math.min(8, roost.attempts() + 1L);
            data.replacePending(roost.withRetry(gameTime + delay));
            remaining--;
            if (RoostTracesConfig.DEBUG.get()) {
                RoostTraces.LOGGER.warn("Deferred roost trace placement for {} at {}: {}",
                        roost.type().id(), roost.pivot(), result);
            }
        }
        return remaining;
    }
}
