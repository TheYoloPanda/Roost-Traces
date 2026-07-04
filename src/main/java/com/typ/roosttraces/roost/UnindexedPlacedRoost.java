package com.typ.roosttraces.roost;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record UnindexedPlacedRoost(
        String key,
        RoostType type,
        long pivotLong,
        long traceLong,
        long nodeLong,
        ResourceLocation nodeId,
        int attempts,
        long nextAttemptGameTime) {

    public BlockPos pivot() {
        return BlockPos.of(pivotLong);
    }

    public BlockPos node() {
        return BlockPos.of(nodeLong);
    }

    public UnindexedPlacedRoost withRetry(long nextAttemptGameTime) {
        return new UnindexedPlacedRoost(
                key,
                type,
                pivotLong,
                traceLong,
                nodeLong,
                nodeId,
                attempts + 1,
                nextAttemptGameTime);
    }

    public UnindexedPlacedRoost withNextAttempt(long nextAttemptGameTime) {
        return new UnindexedPlacedRoost(
                key,
                type,
                pivotLong,
                traceLong,
                nodeLong,
                nodeId,
                attempts,
                nextAttemptGameTime);
    }

    public PlacedRoost asPlaced() {
        return new PlacedRoost(key, type, pivotLong, traceLong, nodeLong, nodeId);
    }
}
