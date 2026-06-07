package com.typ.roosttraces.pool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.typ.roosttraces.RoostTraces;
import com.typ.roosttraces.RoostTracesConfig;
import com.typ.roosttraces.compat.TraceCompat;
import com.typ.roosttraces.roost.RoostType;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class RoostTraceNodePoolResolver {
    private static final Gson GSON = new Gson();
    private static volatile NodeSelectorConfig config = NodeSelectorConfig.defaults();

    private RoostTraceNodePoolResolver() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    public static List<TraceNodeChoice> resolve(RoostType type) {
        NodeSelectorConfig snapshot = RoostTracesConfig.USE_DATAPACK_POOLS.get()
                ? config
                : NodeSelectorConfig.defaults();

        Map<ResourceLocation, Block> candidates = new LinkedHashMap<>();
        if (snapshot.inheritDefault() && RoostTracesConfig.INHERIT_DEFAULT_POOL.get()) {
            expandSelectors(snapshot.defaultSelectors(), candidates);
        }
        expandSelectors(snapshot.selectorsFor(type), candidates);
        if (candidates.isEmpty() && RoostTracesConfig.FALLBACK_TO_DEFAULT_POOL.get()) {
            expandSelectors(snapshot.defaultSelectors(), candidates);
        }

        List<TraceNodeChoice> resolved = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Block> entry : candidates.entrySet()) {
            ResourceLocation nodeId = entry.getKey();
            Block nodeBlock = entry.getValue();
            if (TraceCompat.isInfiniteNode(nodeBlock)) {
                if (RoostTracesConfig.DEBUG.get()) {
                    RoostTraces.LOGGER.warn("Skipping infinite roost trace node {}", nodeId);
                }
                continue;
            }
            Optional<Block> traceBlock = TraceCompat.traceBlockFor(nodeBlock);
            if (traceBlock.isEmpty()) {
                TraceCompat.warnMissingTraceData(nodeId);
                continue;
            }
            Block host = TraceCompat.hostBlockFor(nodeBlock);
            if (host == null) {
                RoostTraces.LOGGER.warn("No host block could be resolved for {}, skipping roost trace placement", nodeId);
                continue;
            }
            resolved.add(new TraceNodeChoice(nodeId, nodeBlock));
        }
        return List.copyOf(resolved);
    }

    private static void expandSelectors(List<String> selectors, Map<ResourceLocation, Block> output) {
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) continue;
            if (selector.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
                if (tagId == null) {
                    RoostTraces.LOGGER.warn("Invalid roost trace node tag selector: {}", selector);
                    continue;
                }
                TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), tagId);
                BuiltInRegistries.BLOCK.getTag(tag).ifPresent(named ->
                        named.forEach(holder -> addHolder(holder, output)));
            } else {
                ResourceLocation id = ResourceLocation.tryParse(selector);
                if (id == null) {
                    RoostTraces.LOGGER.warn("Invalid roost trace node selector: {}", selector);
                    continue;
                }
                if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                    RoostTraces.LOGGER.warn("Unknown roost trace node block id: {}", id);
                    continue;
                }
                output.putIfAbsent(id, BuiltInRegistries.BLOCK.get(id));
            }
        }
    }

    private static void addHolder(Holder<Block> holder, Map<ResourceLocation, Block> output) {
        Block block = holder.value();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        output.putIfAbsent(id, block);
    }

    private static final class Listener extends SimpleJsonResourceReloadListener {
        Listener() {
            super(GSON, "roost_trace_node_pools");
        }

        @Override
        protected void apply(
                @NotNull Map<ResourceLocation, JsonElement> resources,
                @NotNull ResourceManager manager,
                @NotNull ProfilerFiller profiler) {
            if (resources.isEmpty()) {
                config = NodeSelectorConfig.defaults();
                return;
            }

            NodeSelectorConfig parsed = NodeSelectorConfig.defaults();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    RoostTraces.LOGGER.warn("Ignoring roost node pool {} because it is not a JSON object", entry.getKey());
                    continue;
                }
                parsed = parse(entry.getValue().getAsJsonObject(), parsed);
            }
            config = parsed;
            RoostTraces.LOGGER.info("Loaded roost trace node pools: inheritDefault={}, default={}, fire={}, ice={}, lightning={}",
                    parsed.inheritDefault(),
                    parsed.defaultSelectors().size(),
                    parsed.fireSelectors().size(),
                    parsed.iceSelectors().size(),
                    parsed.lightningSelectors().size());
        }

        private static NodeSelectorConfig parse(JsonObject json, NodeSelectorConfig fallback) {
            boolean inherit = json.has("inherit_default")
                    ? json.get("inherit_default").getAsBoolean()
                    : fallback.inheritDefault();
            return new NodeSelectorConfig(
                    inherit,
                    list(json, "default", fallback.defaultSelectors()),
                    list(json, "fire", fallback.fireSelectors()),
                    list(json, "ice", fallback.iceSelectors()),
                    list(json, "lightning", fallback.lightningSelectors()));
        }

        private static List<String> list(JsonObject json, String name, List<String> fallback) {
            if (!json.has(name)) return fallback;
            JsonElement value = json.get(name);
            if (!value.isJsonArray()) return fallback;
            List<String> result = new ArrayList<>();
            value.getAsJsonArray().forEach(element -> {
                if (element.isJsonPrimitive()) {
                    result.add(element.getAsString().toLowerCase(Locale.ROOT));
                }
            });
            return List.copyOf(result);
        }
    }
}
