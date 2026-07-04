package com.typ.roosttraces.placement;

import java.util.List;
import java.util.Optional;

import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;
import com.typ.roosttraces.compat.TraceCompat;
import com.typ.roosttraces.pool.RoostTraceNodePoolResolver;
import com.typ.roosttraces.pool.TraceNodeChoice;
import com.typ.roosttraces.roost.PendingRoost;
import com.typ.roosttraces.roost.PlacedRoost;
import com.typ.roosttraces.roost.RoostKeys;
import com.typ.roosttraces.roost.RoostTraceSavedData;
import com.typ.roosttraces.roost.RoostType;
import com.typ.roosttraces.roost.UnindexedPlacedRoost;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class RoostTraceNodePlacer {
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int INITIAL_PLACEMENT_RETRY_DELAY_TICKS = 20 * 30;
    private static final int INITIAL_INDEX_RETRY_DELAY_TICKS = 20 * 30;

    private RoostTraceNodePlacer() {}

    public static PlacementResult placeDuringWorldgen(WorldGenLevel world, BlockPos pivot, ChunkPos chunkPos, RoostType type) {
        if (!RoostTracesConfig.PLACEMENT_ENABLED.get() || !RoostTracesConfig.CAPTURE_DURING_WORLDGEN.get()) {
            return PlacementResult.DISABLED;
        }
        if (type == RoostType.UNKNOWN) {
            if (RoostTracesConfig.DEBUG.get()) {
                RoostTraces.LOGGER.warn("Skipping dragon roost at {} because its type could not be resolved", pivot);
            }
            return PlacementResult.UNKNOWN_ROOST_TYPE;
        }

        ServerLevel level = world.getLevel();
        long seed = RoostKeys.placementSeed(pivot, chunkPos, type);
        List<TraceNodeChoice> pool = RoostTraceNodePoolResolver.resolve(type);
        if (pool.isEmpty()) {
            queuePendingFallback(level, pivot, chunkPos, type, seed, PlacementResult.NO_NODE_POOL);
            return PlacementResult.NO_NODE_POOL;
        }

        TraceNodeChoice choice = choose(pool, seed);
        Optional<RoostCandidate> candidate = RoostCandidateScanner.find(world, pivot);
        if (candidate.isEmpty()) {
            queuePendingFallback(level, pivot, chunkPos, type, seed, PlacementResult.NO_CANDIDATE);
            return PlacementResult.NO_CANDIDATE;
        }

        RoostCandidate found = candidate.get();
        TraceCompat.suppressGeneratedTrace(world, found.nodePos());
        Optional<NodePlacement> placement = placeNodeOnly(world, found.nodePos(), choice);
        if (placement.isEmpty()) {
            queuePendingFallback(level, pivot, chunkPos, type, seed, PlacementResult.PLACEMENT_FAILED);
            return PlacementResult.PLACEMENT_FAILED;
        }

        String key = RoostKeys.key(level.dimension(), pivot);
        PlacedRoost placed = new PlacedRoost(
                key,
                type,
                pivot.asLong(),
                found.nodePos().asLong(),
                found.nodePos().asLong(),
                choice.nodeId());
        level.getServer().execute(() -> recordOrQueuePlaced(level, placed));

        if (RoostTracesConfig.DEBUG.get()) {
            RoostTraces.LOGGER.info("Placed roost node-only trace for {} at {} during worldgen after {} candidate checks",
                    choice.nodeId(),
                    found.nodePos(),
                    found.checkedCandidates());
        }
        return PlacementResult.PLACED;
    }

    private static void queuePendingFallback(
            ServerLevel level,
            BlockPos pivot,
            ChunkPos chunkPos,
            RoostType type,
            long seed,
            PlacementResult reason) {
        if (!RoostTracesConfig.PLACE_AFTER_CHUNK_GENERATED.get()) return;

        BlockPos savedPivot = pivot.immutable();
        level.getServer().execute(() -> {
            PendingRoost pending = pendingRoost(
                    level,
                    savedPivot,
                    chunkPos,
                    type,
                    seed,
                    level.getGameTime() + INITIAL_PLACEMENT_RETRY_DELAY_TICKS);
            if (RoostTraceSavedData.get(level).addPending(pending) && RoostTracesConfig.DEBUG.get()) {
                RoostTraces.LOGGER.warn("Queued roost trace placement retry for {} at {} after worldgen result {}",
                        type.id(), savedPivot, reason);
            }
        });
    }

    public static PlacementResult place(ServerLevel level, PendingRoost pending) {
        RoostTraceSavedData data = RoostTraceSavedData.get(level);
        if (data.isPlaced(pending.key())) return PlacementResult.ALREADY_EXISTS;

        List<TraceNodeChoice> pool = RoostTraceNodePoolResolver.resolve(pending.type());
        if (pool.isEmpty()) return PlacementResult.NO_NODE_POOL;

        TraceNodeChoice choice = choose(pool, pending.placementSeed());
        Optional<RoostCandidate> candidate = RoostCandidateScanner.find(level, pending.pivot());
        if (candidate.isEmpty()) return PlacementResult.NO_CANDIDATE;

        RoostCandidate found = candidate.get();
        TraceCompat.suppressGeneratedTrace(level, found.nodePos());
        Optional<NodePlacement> placement = placeNodeOnly(level, found.nodePos(), choice);
        if (placement.isEmpty()) {
            return PlacementResult.PLACEMENT_FAILED;
        }

        PlacedRoost placed = new PlacedRoost(
                pending.key(),
                pending.type(),
                pending.pivotLong(),
                found.nodePos().asLong(),
                found.nodePos().asLong(),
                choice.nodeId());
        if (!TraceCompat.recordNode(level, found.nodePos(), choice.nodeId())) {
            data.markIndexPending(toUnindexed(placed, 1, nextInitialIndexRetry(level)));
            return PlacementResult.INDEX_PENDING;
        }

        data.markPlaced(placed);

        if (RoostTracesConfig.DEBUG.get()) {
            RoostTraces.LOGGER.info("Placed roost node-only trace for {} at {} after {} candidate checks",
                    choice.nodeId(),
                    found.nodePos(),
                    found.checkedCandidates());
        }
        return PlacementResult.PLACED;
    }

    private static Optional<NodePlacement> placeNodeOnly(ServerLevel level, BlockPos nodePos, TraceNodeChoice choice) {
        return placeNodeOnly((LevelAccessor) level, nodePos, choice);
    }

    private static Optional<NodePlacement> placeNodeOnly(LevelAccessor level, BlockPos nodePos, TraceNodeChoice choice) {
        BlockState nodeState = choice.nodeState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState previousNode = level.getBlockState(nodePos);

        BlockPos aboveNode = nodePos.above();
        BlockState previousAbove = level.getBlockState(aboveNode);
        boolean clearedAbove = false;
        if (!previousAbove.isAir() && RoostCandidateScanner.canClear(level, aboveNode)) {
            level.setBlock(aboveNode, air, PLACE_FLAGS);
            clearedAbove = true;
        }

        level.setBlock(nodePos, nodeState, PLACE_FLAGS);
        NodePlacement placement = new NodePlacement(
                nodePos.immutable(),
                previousNode,
                aboveNode.immutable(),
                previousAbove,
                clearedAbove);
        if (!level.getBlockState(nodePos).is(choice.nodeBlock())) {
            placement.rollback(level);
            return Optional.empty();
        }
        return Optional.of(placement);
    }

    private static TraceNodeChoice choose(List<TraceNodeChoice> pool, long seed) {
        int index = Math.floorMod(seed, pool.size());
        return pool.get(index);
    }

    private static PendingRoost pendingRoost(
            ServerLevel level,
            BlockPos pivot,
            ChunkPos chunkPos,
            RoostType type,
            long seed,
            long nextAttemptGameTime) {
        return new PendingRoost(
                RoostKeys.key(level.dimension(), pivot),
                type,
                pivot.asLong(),
                chunkPos.toLong(),
                seed,
                0,
                nextAttemptGameTime);
    }

    public static IndexResult retryIndexRegistration(ServerLevel level, UnindexedPlacedRoost roost) {
        BlockPos nodePos = roost.node();
        if (!TraceCompat.isNodeAt(level, nodePos, roost.nodeId())) {
            return IndexResult.MISSING_NODE;
        }
        if (!TraceCompat.recordNode(level, nodePos, roost.nodeId())) {
            return IndexResult.RETRY;
        }
        RoostTraceSavedData.get(level).markPlaced(roost.asPlaced());
        return IndexResult.RECORDED;
    }

    private static void recordOrQueuePlaced(ServerLevel level, PlacedRoost roost) {
        if (!TraceCompat.recordNode(level, BlockPos.of(roost.nodeLong()), roost.nodeId())) {
            RoostTraceSavedData.get(level).markIndexPending(toUnindexed(roost, 1, nextInitialIndexRetry(level)));
            RoostTraces.LOGGER.warn("Placed roost node at {}, but could not register trace index entry for {}; queued retry",
                    BlockPos.of(roost.nodeLong()), roost.nodeId());
            return;
        }
        RoostTraceSavedData.get(level).markPlaced(roost);
    }

    private static UnindexedPlacedRoost toUnindexed(PlacedRoost roost, int attempts, long nextAttemptGameTime) {
        return new UnindexedPlacedRoost(
                roost.key(),
                roost.type(),
                roost.pivotLong(),
                roost.traceLong(),
                roost.nodeLong(),
                roost.nodeId(),
                attempts,
                nextAttemptGameTime);
    }

    private static long nextInitialIndexRetry(ServerLevel level) {
        return level.getGameTime() + INITIAL_INDEX_RETRY_DELAY_TICKS;
    }

    private record NodePlacement(
            BlockPos nodePos,
            BlockState previousNode,
            BlockPos abovePos,
            BlockState previousAbove,
            boolean clearedAbove) {

        void rollback(LevelAccessor level) {
            level.setBlock(nodePos, previousNode, PLACE_FLAGS);
            if (clearedAbove) {
                level.setBlock(abovePos, previousAbove, PLACE_FLAGS);
            }
        }
    }

    public enum PlacementResult {
        PLACED,
        ALREADY_EXISTS,
        INDEX_PENDING,
        DISABLED,
        UNKNOWN_ROOST_TYPE,
        NO_NODE_POOL,
        NO_CANDIDATE,
        PLACEMENT_FAILED
    }

    public enum IndexResult {
        RECORDED,
        MISSING_NODE,
        RETRY
    }
}
