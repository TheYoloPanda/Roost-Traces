package com.typ.roosttraces.placement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.typ.roosttraces.RoostTracesConfig;
import com.typ.roosttraces.pool.RoostTraceTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class RoostCandidateScanner {
    private static final int MIN_VERTICAL_OFFSET = -8;
    private static final int MAX_VERTICAL_OFFSET = 12;
    private static final Map<Integer, List<Offset>> OFFSETS_BY_RADIUS = new ConcurrentHashMap<>();

    private RoostCandidateScanner() {}

    public static Optional<RoostCandidate> find(LevelAccessor level, BlockPos pivot) {
        int radius = RoostTracesConfig.SCAN_RADIUS.get();
        int maxChecks = RoostTracesConfig.MAX_CANDIDATE_CHECKS_PER_ROOST.get();
        List<Offset> offsets = OFFSETS_BY_RADIUS.computeIfAbsent(radius, RoostCandidateScanner::buildOffsets);
        BlockPos.MutableBlockPos nodePos = new BlockPos.MutableBlockPos();
        int checked = 0;

        for (Offset offset : offsets) {
            if (++checked > maxChecks) break;
            nodePos.set(pivot.getX() + offset.dx, pivot.getY() + offset.dy, pivot.getZ() + offset.dz);
            if (!isChunkLoaded(level, nodePos)) continue;
            if (!isValidNodePosition(level, nodePos)) continue;

            BlockPos aboveNode = nodePos.above();
            if (!isValidClearancePosition(level, aboveNode)) continue;

            return Optional.of(new RoostCandidate(nodePos.immutable(), checked));
        }

        return Optional.empty();
    }

    public static boolean canClear(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || isClearable(state) || isRoostReplaceable(state);
    }

    private static boolean isValidNodePosition(LevelAccessor level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null) return false;
        BlockState state = level.getBlockState(pos);
        return isRoostReplaceable(state);
    }

    private static boolean isValidClearancePosition(LevelAccessor level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null) return false;
        BlockState state = level.getBlockState(pos);
        if (state.getFluidState().isSource()) return false;
        return state.isAir() || isClearable(state) || isRoostReplaceable(state);
    }

    private static boolean isRoostReplaceable(BlockState state) {
        if (state.is(RoostTraceTags.ROOST_REPLACEABLE)) return true;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return RoostTracesConfig.replaceableIds().contains(id);
    }

    private static boolean isClearable(BlockState state) {
        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return false;
        Block block = state.getBlock();
        if (state.is(BlockTags.LOGS) || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) return false;
        if (block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK || block == Blocks.REPEATING_COMMAND_BLOCK) return false;
        if (block == Blocks.END_PORTAL || block == Blocks.END_PORTAL_FRAME || block == Blocks.NETHER_PORTAL) return false;
        if (state.canBeReplaced()) return true;
        return state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.GLOW_LICHEN);
    }

    private static boolean isChunkLoaded(LevelAccessor level, BlockPos pos) {
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static List<Offset> buildOffsets(int radius) {
        List<Offset> offsets = new ArrayList<>();
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) continue;
                for (int dy = MIN_VERTICAL_OFFSET; dy <= MAX_VERTICAL_OFFSET; dy++) {
                    offsets.add(new Offset(dx, dy, dz));
                }
            }
        }
        offsets.sort(Comparator.comparingInt(offset -> offset.dx * offset.dx * 4 + offset.dz * offset.dz * 4 + offset.dy * offset.dy));
        return List.copyOf(offsets);
    }

    private record Offset(int dx, int dy, int dz) {
    }
}
