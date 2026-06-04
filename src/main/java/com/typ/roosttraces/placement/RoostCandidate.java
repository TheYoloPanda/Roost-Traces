package com.typ.roosttraces.placement;

import net.minecraft.core.BlockPos;

public record RoostCandidate(BlockPos tracePos, BlockPos nodePos, int checkedCandidates, int score) {
}
