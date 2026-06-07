package com.typ.roosttraces;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class RoostTracesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final List<String> DEFAULT_REPLACEABLE_IDS = List.of(
            "iceandfire:crackled_dirt",
            "iceandfire:crackled_gravel",
            "iceandfire:crackled_cobblestone",
            "iceandfire:chared_dirt",
            "iceandfire:chared_gravel",
            "iceandfire:chared_cobblestone",
            "iceandfire:frozen_dirt",
            "iceandfire:frozen_gravel",
            "iceandfire:frozen_cobblestone");

    public static final ModConfigSpec.BooleanValue PLACEMENT_ENABLED;
    public static final ModConfigSpec.BooleanValue CAPTURE_DURING_WORLDGEN;
    public static final ModConfigSpec.BooleanValue PLACE_AFTER_CHUNK_GENERATED;
    public static final ModConfigSpec.BooleanValue ENABLE_BACKFILL;
    public static final ModConfigSpec.IntValue SCAN_RADIUS;
    public static final ModConfigSpec.IntValue EXISTING_PAIR_RADIUS;
    public static final ModConfigSpec.IntValue MAX_PENDING_ROOSTS_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_BACKFILL_CHUNKS_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_CANDIDATE_CHECKS_PER_ROOST;
    public static final ModConfigSpec.BooleanValue ALLOW_PILE_REPLACEMENT;
    public static final ModConfigSpec.BooleanValue REGISTER_IN_TRACE_INDEX;
    public static final ModConfigSpec.BooleanValue DEBUG;

    public static final ModConfigSpec.BooleanValue USE_DATAPACK_POOLS;
    public static final ModConfigSpec.BooleanValue INHERIT_DEFAULT_POOL;
    public static final ModConfigSpec.BooleanValue FALLBACK_TO_DEFAULT_POOL;
    public static final ModConfigSpec.BooleanValue ALLOW_STONE_FALLBACK_FOR_CUSTOM_NODES;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REPLACEABLE_IDS;
    public static final ModConfigSpec SPEC;

    private static volatile Set<ResourceLocation> replaceableIds = parseIds(DEFAULT_REPLACEABLE_IDS);

    static {
        BUILDER.push("placement");
        PLACEMENT_ENABLED = BUILDER.define("enabled", true);
        CAPTURE_DURING_WORLDGEN = BUILDER.define("captureDuringRoostWorldgen", true);
        PLACE_AFTER_CHUNK_GENERATED = BUILDER.define("placeAfterChunkGenerated", true);
        ENABLE_BACKFILL = BUILDER.define("enableBackfill", false);
        SCAN_RADIUS = BUILDER.defineInRange("scanRadius", 24, 4, 64);
        EXISTING_PAIR_RADIUS = BUILDER.defineInRange("existingPairRadius", 48, 8, 128);
        MAX_PENDING_ROOSTS_PER_TICK = BUILDER.defineInRange("maxPendingRoostsPerTick", 1, 1, 16);
        MAX_BACKFILL_CHUNKS_PER_TICK = BUILDER.defineInRange("maxBackfillChunksPerTick", 1, 1, 16);
        MAX_CANDIDATE_CHECKS_PER_ROOST = BUILDER.defineInRange("maxCandidateChecksPerRoost", 4096, 64, 65536);
        ALLOW_PILE_REPLACEMENT = BUILDER.define("allowPileReplacement", false);
        REGISTER_IN_TRACE_INDEX = BUILDER.define("registerInTraceIndex", true);
        DEBUG = BUILDER.define("debug", false);
        BUILDER.pop();

        BUILDER.push("nodes");
        USE_DATAPACK_POOLS = BUILDER.define("useDatapackPools", true);
        INHERIT_DEFAULT_POOL = BUILDER.define("inheritDefaultPool", true);
        FALLBACK_TO_DEFAULT_POOL = BUILDER.define("fallbackToDefaultPool", true);
        ALLOW_STONE_FALLBACK_FOR_CUSTOM_NODES = BUILDER.define("allowStoneFallbackForCustomNodes", false);
        BUILDER.pop();

        BUILDER.push("replaceable");
        REPLACEABLE_IDS = BUILDER.defineListAllowEmpty(
                "ids",
                DEFAULT_REPLACEABLE_IDS,
                () -> "",
                RoostTracesConfig::validBlockId);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private RoostTracesConfig() {}

    public static void onLoad(ModConfigEvent event) {
        rebuildCaches();
    }

    public static Set<ResourceLocation> replaceableIds() {
        return replaceableIds;
    }

    private static boolean validBlockId(Object value) {
        if (!(value instanceof String s)) return false;
        ResourceLocation id = ResourceLocation.tryParse(s);
        return id != null;
    }

    private static void rebuildCaches() {
        replaceableIds = parseIds(REPLACEABLE_IDS.get());
    }

    private static Set<ResourceLocation> parseIds(List<? extends String> rawIds) {
        Set<ResourceLocation> parsed = new HashSet<>();
        for (String raw : rawIds) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id != null) parsed.add(id);
        }
        return Set.copyOf(parsed);
    }
}
