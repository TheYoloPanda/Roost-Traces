package com.typ.roosttraces.mixin;

import java.util.Optional;

import com.typ.roosttraces.compat.TraceCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

@Mixin(targets = "com.typ.traces.server.TraceFinderTickHandler", remap = false)
public abstract class TraceFinderTickHandlerMixin {
    @Inject(method = "resolveTracePos", at = @At("HEAD"), cancellable = true, remap = false)
    private static void roosttraces$resolveNodeOnlyTrace(
            ServerLevel level,
            BlockPos indexedPos,
            Block traceBlock,
            CallbackInfoReturnable<Optional<BlockPos>> cir) {
        if (traceBlock == null) return;

        Block indexedBlock = level.getBlockState(indexedPos).getBlock();
        Optional<Block> mappedTraceBlock = TraceCompat.traceBlockFor(indexedBlock);
        if (mappedTraceBlock.isPresent() && mappedTraceBlock.get() == traceBlock) {
            cir.setReturnValue(Optional.of(indexedPos));
        }
    }
}
