package com.typ.roosttraces.roost;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public record PendingRoost(
        String key,
        RoostType type,
        long pivotLong,
        long chunkLong,
        long placementSeed,
        int attempts,
        long nextAttemptGameTime) {

    public BlockPos pivot() {
        return BlockPos.of(pivotLong);
    }

    public ChunkPos chunkPos() {
        return new ChunkPos(chunkLong);
    }

    public PendingRoost withRetry(long nextAttemptGameTime) {
        return new PendingRoost(key, type, pivotLong, chunkLong, placementSeed, attempts + 1, nextAttemptGameTime);
    }

    public PendingRoost withNextAttempt(long nextAttemptGameTime) {
        return new PendingRoost(key, type, pivotLong, chunkLong, placementSeed, attempts, nextAttemptGameTime);
    }
}
