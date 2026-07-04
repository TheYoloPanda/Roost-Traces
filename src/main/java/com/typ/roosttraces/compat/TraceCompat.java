package com.typ.roosttraces.compat;

import java.lang.reflect.Method;

import com.typ.traces.api.TraceApi;
import com.typ.traces.api.TraceWorldgenExclusions;
import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class TraceCompat {
    private TraceCompat() {}

    public static void suppressGeneratedTrace(WorldGenLevel level, BlockPos nodePos) {
        TraceWorldgenExclusions.suppressGeneratedTrace(level, nodePos);
    }

    public static void suppressGeneratedTrace(ServerLevel level, BlockPos nodePos) {
        TraceWorldgenExclusions.suppressGeneratedTrace(level, nodePos);
    }

    public static boolean recordNode(ServerLevel level, BlockPos nodePos, ResourceLocation nodeId) {
        if (!RoostTracesConfig.REGISTER_IN_TRACE_INDEX.get()) return true;
        try {
            if (TraceApi.isRecorded(level, nodePos) && !TraceApi.removeExternalRecord(level, nodePos)) {
                return false;
            }
            return TraceApi.recordExternalNode(level, nodePos, nodeId);
        } catch (RuntimeException e) {
            RoostTraces.LOGGER.warn("Unable to record roost node at {} for {} in TraceIndex", nodePos, nodeId, e);
            return false;
        }
    }

    public static boolean isNodeAt(ServerLevel level, BlockPos nodePos, ResourceLocation nodeId) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(nodePos).getBlock()).equals(nodeId);
    }

    public static BlockState naturalNodeState(Block nodeBlock) {
        try {
            Method method = nodeBlock.getClass().getMethod("unstable");
            Object value = method.invoke(nodeBlock);
            if (value instanceof BlockState state) return state;
        } catch (NoSuchMethodException ignored) {
            // Custom nodes may not expose Create ReAutomated's natural state helper.
        } catch (ReflectiveOperationException e) {
            RoostTraces.LOGGER.warn("Unable to resolve unstable state for {}, using default state",
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(nodeBlock));
        }
        return nodeBlock.defaultBlockState();
    }
}
