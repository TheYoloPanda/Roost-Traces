package com.typ.roosttraces.roost;

import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;

public final class RoostPendingRegistry {
    private RoostPendingRegistry() {}

    public static void capture(WorldGenLevel world, BlockPos pivot, ChunkPos chunkPos, RoostType type) {
        if (!RoostTracesConfig.PLACEMENT_ENABLED.get() || !RoostTracesConfig.CAPTURE_DURING_WORLDGEN.get()) return;
        if (type == RoostType.UNKNOWN) {
            if (RoostTracesConfig.DEBUG.get()) {
                RoostTraces.LOGGER.warn("Skipping dragon roost at {} because its type could not be resolved", pivot);
            }
            return;
        }

        ServerLevel level = world.getLevel();
        String key = RoostKeys.key(level.dimension(), pivot);
        long seed = mix64(pivot.asLong() ^ chunkPos.toLong() ^ ((long) type.ordinal() << 56));
        PendingRoost pending = new PendingRoost(key, type, pivot.asLong(), chunkPos.toLong(), seed, 0, 0L);

        level.getServer().execute(() -> {
            RoostTraceSavedData data = RoostTraceSavedData.get(level);
            if (data.addPending(pending)) {
                if (RoostTracesConfig.DEBUG.get()) {
                    RoostTraces.LOGGER.info("Captured {} dragon roost at {} in {}", type.id(), pivot, level.dimension().location());
                }
            }
        });
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
