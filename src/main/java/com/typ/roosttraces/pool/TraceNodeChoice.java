package com.typ.roosttraces.pool;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record TraceNodeChoice(ResourceLocation nodeId, Block nodeBlock, BlockState nodeState) {
}
