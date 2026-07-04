package com.typ.roosttraces.placement;

import java.util.Collection;

import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;
import com.typ.roosttraces.roost.PendingRoost;
import com.typ.roosttraces.roost.RoostTraceSavedData;
import com.typ.roosttraces.roost.UnindexedPlacedRoost;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class RoostTracePlacementScheduler {
    private static final int RETRY_DELAY_TICKS = 20 * 30;
    private static final int MAX_PLACEMENT_ATTEMPTS = 8;

    private RoostTracePlacementScheduler() {}

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!RoostTracesConfig.PLACEMENT_ENABLED.get()) return;

        MinecraftServer server = event.getServer();
        int remaining = RoostTracesConfig.MAX_PENDING_ROOSTS_PER_TICK.get();
        boolean processPlacementRetries = RoostTracesConfig.PLACE_AFTER_CHUNK_GENERATED.get();
        for (ServerLevel level : server.getAllLevels()) {
            RoostTraceSavedData data = RoostTraceSavedData.get(level);
            long gameTime = level.getGameTime();
            boolean hasDueIndexPending = data.hasDueIndexPending(gameTime);
            boolean hasDuePlacementPending = processPlacementRetries && data.hasDuePending(gameTime);
            if (!hasDueIndexPending && !hasDuePlacementPending) continue;
            if (remaining <= 0) break;
            if (hasDueIndexPending) {
                remaining = processIndexPending(level, data, remaining, gameTime);
            }
            if (remaining <= 0 || !processPlacementRetries) continue;
            remaining = processPlacementPending(level, data, remaining, gameTime);
        }
    }

    private static int processIndexPending(ServerLevel level, RoostTraceSavedData data, int remainingBudget, long gameTime) {
        Collection<UnindexedPlacedRoost> pending = data.indexPendingSnapshot();
        if (pending.isEmpty()) return remainingBudget;

        int remaining = remainingBudget;
        for (UnindexedPlacedRoost roost : pending) {
            if (remaining <= 0) break;
            if (roost.nextAttemptGameTime() > gameTime) continue;
            BlockPos nodePos = roost.node();
            if (!level.hasChunk(nodePos.getX() >> 4, nodePos.getZ() >> 4)) {
                data.replaceIndexPending(roost.withNextAttempt(gameTime + RETRY_DELAY_TICKS));
                remaining--;
                continue;
            }

            RoostTraceNodePlacer.IndexResult result = RoostTraceNodePlacer.retryIndexRegistration(level, roost);
            if (result == RoostTraceNodePlacer.IndexResult.RECORDED) {
                remaining--;
                continue;
            }

            if (result == RoostTraceNodePlacer.IndexResult.MISSING_NODE) {
                data.removeIndexPending(roost.key());
                RoostTraces.LOGGER.warn("Dropped roost trace index retry for {} at {} because the node block is no longer present",
                        roost.nodeId(), nodePos);
                remaining--;
                continue;
            }

            long delay = RETRY_DELAY_TICKS * Math.min(8, roost.attempts() + 1L);
            data.replaceIndexPending(roost.withRetry(gameTime + delay));
            remaining--;
            if (RoostTracesConfig.DEBUG.get()) {
                RoostTraces.LOGGER.warn("Deferred roost trace index retry for {} at {}",
                        roost.nodeId(), nodePos);
            }
        }
        return remaining;
    }

    private static int processPlacementPending(ServerLevel level, RoostTraceSavedData data, int remainingBudget, long gameTime) {
        Collection<PendingRoost> pending = data.pendingSnapshot();
        if (pending.isEmpty()) return remainingBudget;

        int remaining = remainingBudget;
        for (PendingRoost roost : pending) {
            if (remaining <= 0) break;
            if (roost.nextAttemptGameTime() > gameTime) continue;
            ChunkPos chunk = roost.chunkPos();
            if (!level.hasChunk(chunk.x, chunk.z)) {
                data.replacePending(roost.withNextAttempt(gameTime + RETRY_DELAY_TICKS));
                remaining--;
                continue;
            }

            RoostTraceNodePlacer.PlacementResult result = RoostTraceNodePlacer.place(level, roost);
            if (result == RoostTraceNodePlacer.PlacementResult.PLACED
                    || result == RoostTraceNodePlacer.PlacementResult.ALREADY_EXISTS
                    || result == RoostTraceNodePlacer.PlacementResult.INDEX_PENDING) {
                remaining--;
                continue;
            }

            if (isTerminal(result) || roost.attempts() + 1 >= MAX_PLACEMENT_ATTEMPTS) {
                data.removePending(roost.key());
                RoostTraces.LOGGER.warn("Dropped pending roost trace placement for {} at {} after {} attempt(s): {}",
                        roost.type().id(), roost.pivot(), roost.attempts() + 1, result);
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

    private static boolean isTerminal(RoostTraceNodePlacer.PlacementResult result) {
        return result == RoostTraceNodePlacer.PlacementResult.UNKNOWN_ROOST_TYPE
                || result == RoostTraceNodePlacer.PlacementResult.DISABLED;
    }
}
