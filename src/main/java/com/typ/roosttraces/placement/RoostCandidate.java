package com.typ.roosttraces.placement;

import net.minecraft.core.BlockPos;

public record RoostCandidate(BlockPos nodePos, int checkedCandidates) {
}
