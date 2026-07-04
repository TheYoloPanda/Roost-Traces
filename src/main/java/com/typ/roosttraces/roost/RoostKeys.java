package com.typ.roosttraces.roost;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class RoostKeys {
    private RoostKeys() {}

    public static String key(ResourceKey<Level> dimension, BlockPos pivot) {
        return dimension.location() + ":" + pivot.asLong();
    }

    public static String normalizeSavedKey(String savedKey, long pivotLong) {
        if (savedKey == null || savedKey.isEmpty()) return savedKey;
        String exactSuffix = ":" + pivotLong;
        if (savedKey.endsWith(exactSuffix)) return savedKey;

        int lastColon = savedKey.lastIndexOf(':');
        int secondLastColon = lastColon <= 0 ? -1 : savedKey.lastIndexOf(':', lastColon - 1);
        if (secondLastColon > 0
                && isInteger(savedKey.substring(secondLastColon + 1, lastColon))
                && isInteger(savedKey.substring(lastColon + 1))) {
            return savedKey.substring(0, secondLastColon) + exactSuffix;
        }
        return savedKey;
    }

    public static long placementSeed(BlockPos pivot, ChunkPos chunkPos, RoostType type) {
        return mix64(pivot.asLong() ^ chunkPos.toLong() ^ ((long) type.ordinal() << 56));
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
