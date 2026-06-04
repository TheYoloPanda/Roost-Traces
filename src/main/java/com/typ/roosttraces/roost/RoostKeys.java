package com.typ.roosttraces.roost;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class RoostKeys {
    private static final int ROOST_CELL_SIZE = 64;

    private RoostKeys() {}

    public static String key(ResourceKey<Level> dimension, BlockPos pivot) {
        int cellX = Math.floorDiv(pivot.getX(), ROOST_CELL_SIZE);
        int cellZ = Math.floorDiv(pivot.getZ(), ROOST_CELL_SIZE);
        return dimension.location() + ":" + cellX + ":" + cellZ;
    }
}
