package com.typ.roosttraces.pool;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public record TraceNodeChoice(ResourceLocation nodeId, Block nodeBlock, Block traceBlock, Block hostBlock) {
}
