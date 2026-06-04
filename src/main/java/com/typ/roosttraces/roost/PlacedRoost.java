package com.typ.roosttraces.roost;

import net.minecraft.resources.ResourceLocation;

public record PlacedRoost(
        String key,
        RoostType type,
        long pivotLong,
        long traceLong,
        long nodeLong,
        ResourceLocation nodeId) {
}
