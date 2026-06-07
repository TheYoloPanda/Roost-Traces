package com.typ.roosttraces.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class TraceCompat {
    private static final String TRACE_BLOCK_DATA_MAP = "com.typ.traces.worldgen.TraceBlockDataMap";
    private static final String TRACE_INDEX = "com.typ.traces.index.TraceIndex";

    private static volatile Method traceBlockFor;
    private static volatile Method recordTrace;
    private static volatile Method forEachInRange;

    private TraceCompat() {}

    public record RecordedTrace(ResourceLocation nodeId, BlockPos pos) {
    }

    @SuppressWarnings("unchecked")
    public static Optional<Block> traceBlockFor(Block nodeBlock) {
        try {
            Method method = traceBlockFor;
            if (method == null) {
                method = Class.forName(TRACE_BLOCK_DATA_MAP).getMethod("traceBlockFor", Block.class);
                traceBlockFor = method;
            }
            Object value = method.invoke(null, nodeBlock);
            if (value instanceof Optional<?> optional) {
                return (Optional<Block>) optional;
            }
        } catch (ReflectiveOperationException e) {
            RoostTraces.LOGGER.warn("Unable to call TraceBlockDataMap.traceBlockFor; Create ReAutomated: Traces API not available", e);
        }
        return Optional.empty();
    }

    public static boolean recordTrace(ServerLevel level, BlockPos tracePos, ResourceLocation nodeId) {
        if (!RoostTracesConfig.REGISTER_IN_TRACE_INDEX.get()) return true;
        try {
            Method method = recordTrace;
            if (method == null) {
                method = Class.forName(TRACE_INDEX).getMethod("record", ServerLevel.class, BlockPos.class, ResourceLocation.class);
                recordTrace = method;
            }
            Object value = method.invoke(null, level, tracePos, nodeId);
            return value instanceof Boolean result ? result : true;
        } catch (ReflectiveOperationException e) {
            RoostTraces.LOGGER.warn("Unable to record roost trace at {} for {} in TraceIndex", tracePos, nodeId, e);
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    public static Optional<RecordedTrace> findRecordedTraceInRange(ServerLevel level, BlockPos pivot, int radiusBlocks, Set<ResourceLocation> nodeIds) {
        try {
            Method method = forEachInRange;
            if (method == null) {
                method = Class.forName(TRACE_INDEX).getMethod(
                        "forEachInRange",
                        ServerLevel.class,
                        ChunkPos.class,
                        int.class,
                        Predicate.class,
                        Consumer.class);
                forEachInRange = method;
            }

            AtomicReference<RecordedTrace> found = new AtomicReference<>();
            Predicate<ResourceLocation> filter = nodeIds::contains;
            Consumer consumer = record -> {
                if (found.get() != null) return;
                try {
                    Method posMethod = record.getClass().getMethod("pos");
                    Method nodeIdMethod = record.getClass().getMethod("nodeId");
                    Object posValue = posMethod.invoke(record);
                    Object nodeIdValue = nodeIdMethod.invoke(record);
                    if (posValue instanceof BlockPos pos && withinHorizontalRadius(pivot, pos, radiusBlocks)) {
                        if (nodeIdValue instanceof ResourceLocation nodeId) {
                            found.set(new RecordedTrace(nodeId, pos.immutable()));
                        }
                    }
                } catch (ReflectiveOperationException ignored) {
                    if (!nodeIds.isEmpty()) {
                        found.set(new RecordedTrace(nodeIds.iterator().next(), pivot.immutable()));
                    }
                }
            };
            int radiusChunks = Math.max(1, (radiusBlocks + 15) >> 4);
            method.invoke(null, level, new ChunkPos(pivot), radiusChunks, filter, consumer);
            return Optional.ofNullable(found.get());
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        }
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

    private static boolean withinHorizontalRadius(BlockPos pivot, BlockPos pos, int radius) {
        long dx = pos.getX() - pivot.getX();
        long dz = pos.getZ() - pivot.getZ();
        return dx * dx + dz * dz <= (long) radius * radius;
    }
}
