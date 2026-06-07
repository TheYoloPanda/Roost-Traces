package com.typ.roosttraces.placement;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;
import com.typ.roosttraces.pool.RoostTraceTags;
import com.typ.roosttraces.roost.PendingRoost;
import com.typ.roosttraces.roost.RoostKeys;
import com.typ.roosttraces.roost.RoostTraceSavedData;
import com.typ.roosttraces.roost.RoostType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

public final class RoostBackfillScanner {
    private static final int MIN_DETECTOR_BLOCKS = 24;
    private static final int SURFACE_SCAN_BELOW = 12;
    private static final int SURFACE_SCAN_ABOVE = 4;

    private static final Queue<QueuedChunk> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<String> QUEUED = ConcurrentHashMap.newKeySet();

    private RoostBackfillScanner() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!RoostTracesConfig.ENABLE_BACKFILL.get()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ChunkAccess chunk = event.getChunk();
        ChunkPos pos = chunk.getPos();
        String key = level.dimension().location() + ":" + pos.toLong();
        if (QUEUED.add(key)) {
            QUEUE.add(new QueuedChunk(level.dimension(), pos.toLong(), key));
        }
    }

    public static void process(MinecraftServer server) {
        if (!RoostTracesConfig.ENABLE_BACKFILL.get()) return;
        int budget = RoostTracesConfig.MAX_BACKFILL_CHUNKS_PER_TICK.get();
        while (budget-- > 0) {
            QueuedChunk queued = QUEUE.poll();
            if (queued == null) return;
            QUEUED.remove(queued.queueKey());

            ServerLevel level = server.getLevel(queued.dimension());
            if (level == null) continue;
            ChunkPos chunk = new ChunkPos(queued.chunkLong());
            if (!level.hasChunk(chunk.x, chunk.z)) continue;
            scanChunk(level, chunk);
        }
    }

    private static void scanChunk(ServerLevel level, ChunkPos chunk) {
        ScanStats stats = scanDetectorBlocks(level, chunk);
        if (stats.count < MIN_DETECTOR_BLOCKS) return;

        BlockPos pivot = new BlockPos(
                (int) (stats.sumX / stats.count),
                (int) (stats.sumY / stats.count),
                (int) (stats.sumZ / stats.count));
        RoostType type = stats.bestType();
        if (type == RoostType.UNKNOWN) return;

        String roostKey = RoostKeys.key(level.dimension(), pivot);
        RoostTraceSavedData data = RoostTraceSavedData.get(level);
        if (data.isPlaced(roostKey)) return;

        long seed = mix64(pivot.asLong() ^ chunk.toLong() ^ ((long) type.ordinal() << 56));
        PendingRoost pending = new PendingRoost(roostKey, type, pivot.asLong(), chunk.toLong(), seed, 0, level.getGameTime());
        if (data.addPending(pending) && RoostTracesConfig.DEBUG.get()) {
            RoostTraces.LOGGER.info("Backfill queued {} dragon roost candidate at {} in {}", type.id(), pivot, level.dimension().location());
        }
    }

    private static ScanStats scanDetectorBlocks(ServerLevel level, ChunkPos chunk) {
        ScanStats stats = new ScanStats();
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int fromY = Math.max(minY, surface - SURFACE_SCAN_BELOW);
                int toY = Math.min(maxY, surface + SURFACE_SCAN_ABOVE);
                for (int y = fromY; y <= toY; y++) {
                    cur.set(x, y, z);
                    if (!level.getBlockState(cur).is(RoostTraceTags.ROOST_DETECTOR)) continue;
                    stats.accept(cur, BuiltInRegistries.BLOCK.getKey(level.getBlockState(cur).getBlock()));
                }
            }
        }
        return stats;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private record QueuedChunk(ResourceKey<Level> dimension, long chunkLong, String queueKey) {
    }

    private static final class ScanStats {
        int count;
        int fire;
        int ice;
        int lightning;
        long sumX;
        long sumY;
        long sumZ;

        void accept(BlockPos pos, ResourceLocation blockId) {
            count++;
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
            String path = blockId.getPath();
            if (path.contains("chared") || path.contains("charred")) fire++;
            else if (path.contains("frozen")) ice++;
            else if (path.contains("crackled")) lightning++;
        }

        RoostType bestType() {
            if (fire >= ice && fire >= lightning && fire > 0) return RoostType.FIRE;
            if (ice >= fire && ice >= lightning && ice > 0) return RoostType.ICE;
            if (lightning > 0) return RoostType.LIGHTNING;
            return RoostType.UNKNOWN;
        }
    }
}
