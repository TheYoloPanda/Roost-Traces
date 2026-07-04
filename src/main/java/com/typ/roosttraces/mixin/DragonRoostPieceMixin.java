package com.typ.roosttraces.mixin;

import com.typ.roosttraces.placement.RoostTraceNodePlacer;
import com.typ.roosttraces.roost.RoostType;
import com.typ.roosttraces.roost.RoostTypeResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(targets = "com.iafenvoy.iceandfire.world.structure.DragonRoostStructure$DragonRoostPiece", remap = false)
public abstract class DragonRoostPieceMixin {
    @Inject(method = "postProcess", at = @At("RETURN"), remap = false)
    private void roosttraces$afterRoostGenerated(
            WorldGenLevel world,
            StructureManager structureAccessor,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            BlockPos pivot,
            CallbackInfo ci) {
        if (!chunkBox.isInside(pivot)) return;
        RoostType type = RoostTypeResolver.fromPiece(this);
        RoostTraceNodePlacer.placeDuringWorldgen(world, pivot, chunkPos, type);
    }
}
