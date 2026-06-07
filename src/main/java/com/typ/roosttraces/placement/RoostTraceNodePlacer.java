package com.typ.roosttraces.placement;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;
import com.typ.roosttraces.compat.TraceCompat;
import com.typ.roosttraces.pool.RoostTraceNodePoolResolver;
import com.typ.roosttraces.pool.TraceNodeChoice;
import com.typ.roosttraces.roost.PendingRoost;
import com.typ.roosttraces.roost.PlacedRoost;
import com.typ.roosttraces.roost.RoostTraceSavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class RoostTraceNodePlacer {
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private RoostTraceNodePlacer() {}

    public static PlacementResult place(ServerLevel level, PendingRoost pending) {
        RoostTraceSavedData data = RoostTraceSavedData.get(level);
        if (data.isPlaced(pending.key())) return PlacementResult.ALREADY_EXISTS;

        List<TraceNodeChoice> pool = RoostTraceNodePoolResolver.resolve(pending.type());
        if (pool.isEmpty()) return PlacementResult.NO_NODE_POOL;

        Optional<ExistingPair> existingPair = findExistingPair(level, pending.pivot(), pool);
        if (existingPair.isPresent()) {
            ExistingPair pair = existingPair.get();
            if (!pair.indexed() && !TraceCompat.recordTrace(level, pair.tracePos(), pair.nodeId())) {
                return PlacementResult.TRACE_INDEX_FAILED;
            }
            data.markPlaced(new PlacedRoost(
                    pending.key(),
                    pending.type(),
                    pending.pivotLong(),
                    pair.tracePos().asLong(),
                    pair.nodePos().asLong(),
                    pair.nodeId()));
            return PlacementResult.ALREADY_EXISTS;
        }

        TraceNodeChoice choice = choose(pool, pending.placementSeed());
        Optional<RoostCandidate> candidate = RoostCandidateScanner.find(level, pending.pivot());
        if (candidate.isEmpty()) return PlacementResult.NO_CANDIDATE;

        RoostCandidate found = candidate.get();
        Optional<NodePlacement> placement = placeNodeOnly(level, found.nodePos(), choice);
        if (placement.isEmpty()) {
            return PlacementResult.PLACEMENT_FAILED;
        }

        if (!TraceCompat.recordTrace(level, found.tracePos(), choice.nodeId())) {
            placement.get().rollback(level);
            return PlacementResult.TRACE_INDEX_FAILED;
        }

        data.markPlaced(new PlacedRoost(
                pending.key(),
                pending.type(),
                pending.pivotLong(),
                found.tracePos().asLong(),
                found.nodePos().asLong(),
                choice.nodeId()));

        if (RoostTracesConfig.DEBUG.get()) {
            RoostTraces.LOGGER.info("Placed roost node-only trace for {} at {} after {} candidate checks",
                    choice.nodeId(),
                    found.nodePos(),
                    found.checkedCandidates());
        }
        return PlacementResult.PLACED;
    }

    private static Optional<NodePlacement> placeNodeOnly(ServerLevel level, BlockPos nodePos, TraceNodeChoice choice) {
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

    private static Optional<ExistingPair> findExistingPair(ServerLevel level, BlockPos pivot, List<TraceNodeChoice> pool) {
        int radius = RoostTracesConfig.EXISTING_PAIR_RADIUS.get();
        Set<ResourceLocation> nodeIds = new HashSet<>();
        for (TraceNodeChoice choice : pool) nodeIds.add(choice.nodeId());
        Optional<TraceCompat.RecordedTrace> recordedTrace = TraceCompat.findRecordedTraceInRange(level, pivot, radius, nodeIds);
        if (recordedTrace.isPresent()) {
            TraceCompat.RecordedTrace trace = recordedTrace.get();
            Block blockAtTrace = level.getBlockState(trace.pos()).getBlock();
            for (TraceNodeChoice choice : pool) {
                if (choice.nodeId().equals(trace.nodeId()) && choice.nodeBlock() == blockAtTrace) {
                    return Optional.of(new ExistingPair(trace.nodeId(), trace.pos(), trace.pos(), true));
                }
            }
        }

        int radiusSq = radius * radius;
        int minY = Math.max(level.getMinBuildHeight(), pivot.getY() - 12);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, pivot.getY() + 16);
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) continue;
                int x = pivot.getX() + dx;
                int z = pivot.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = minY; y <= maxY; y++) {
                    cur.set(x, y, z);
                    Block block = level.getBlockState(cur).getBlock();
                    for (TraceNodeChoice choice : pool) {
                        if (choice.nodeBlock() == block && RoostCandidateScanner.canClear(level, cur.above())) {
                            BlockPos found = cur.immutable();
                            return Optional.of(new ExistingPair(choice.nodeId(), found, found, false));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private record ExistingPair(ResourceLocation nodeId, BlockPos tracePos, BlockPos nodePos, boolean indexed) {
    }

    private record NodePlacement(
            BlockPos nodePos,
            BlockState previousNode,
            BlockPos abovePos,
            BlockState previousAbove,
            boolean clearedAbove) {

        void rollback(ServerLevel level) {
            level.setBlock(nodePos, previousNode, PLACE_FLAGS);
            if (clearedAbove) {
                level.setBlock(abovePos, previousAbove, PLACE_FLAGS);
            }
        }
    }

    public enum PlacementResult {
        PLACED,
        ALREADY_EXISTS,
        NO_NODE_POOL,
        NO_CANDIDATE,
        PLACEMENT_FAILED,
        TRACE_INDEX_FAILED
    }
}
