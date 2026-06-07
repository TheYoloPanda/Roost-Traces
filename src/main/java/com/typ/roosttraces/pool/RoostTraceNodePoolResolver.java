package com.typ.roosttraces.pool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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

        Map<ResourceLocation, TraceNodeChoice> candidates = new LinkedHashMap<>();
        if (snapshot.inheritDefault() && RoostTracesConfig.INHERIT_DEFAULT_POOL.get()) {
            expandSelectors(snapshot.defaultSelectors(), candidates);
        }
        expandSelectors(snapshot.selectorsFor(type), candidates);
        if (candidates.isEmpty() && RoostTracesConfig.FALLBACK_TO_DEFAULT_POOL.get()) {
            expandSelectors(snapshot.defaultSelectors(), candidates);
        }

        return List.copyOf(candidates.values());
    }

    private static void expandSelectors(List<String> selectors, Map<ResourceLocation, TraceNodeChoice> output) {
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
                parseDirectSelector(selector).ifPresent(choice -> output.put(choice.nodeId(), choice));
            }
        }
    }

    private static Optional<TraceNodeChoice> parseDirectSelector(String selector) {
        SelectorParts parts = splitSelector(selector);
        ResourceLocation id = ResourceLocation.tryParse(parts.blockId());
        if (id == null) {
            RoostTraces.LOGGER.warn("Invalid roost trace node selector: {}", selector);
            return Optional.empty();
        }
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            RoostTraces.LOGGER.warn("Unknown roost trace node block id: {}", id);
            return Optional.empty();
        }

        Block block = BuiltInRegistries.BLOCK.get(id);
        BlockState state = TraceCompat.naturalNodeState(block);
        if (parts.properties() != null) {
            Optional<BlockState> parsed = applyProperties(selector, state, parts.properties());
            if (parsed.isEmpty()) return Optional.empty();
            state = parsed.get();
        }
        return Optional.of(new TraceNodeChoice(id, block, state));
    }

    private static SelectorParts splitSelector(String selector) {
        int stateStart = selector.indexOf('[');
        if (stateStart < 0) {
            if (selector.indexOf('{') >= 0) {
                RoostTraces.LOGGER.warn("Invalid roost trace node selector {}; use blockstate syntax like mod:block[stable=false]", selector);
            }
            return new SelectorParts(selector, null);
        }
        if (!selector.endsWith("]")) {
            RoostTraces.LOGGER.warn("Invalid roost trace node selector: {}", selector);
            return new SelectorParts(selector, null);
        }
        return new SelectorParts(
                selector.substring(0, stateStart),
                selector.substring(stateStart + 1, selector.length() - 1));
    }

    private static Optional<BlockState> applyProperties(String selector, BlockState state, String properties) {
        if (properties.isBlank()) return Optional.of(state);

        for (String rawPair : properties.split(",")) {
            String[] pair = rawPair.split("=", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                RoostTraces.LOGGER.warn("Invalid blockstate property in roost trace node selector: {}", selector);
                return Optional.empty();
            }

            Property<?> property = state.getBlock().getStateDefinition().getProperty(pair[0].trim());
            if (property == null) {
                RoostTraces.LOGGER.warn("Unknown blockstate property '{}' in roost trace node selector: {}", pair[0].trim(), selector);
                return Optional.empty();
            }

            Optional<BlockState> updated = setProperty(selector, state, property, pair[1].trim());
            if (updated.isEmpty()) return Optional.empty();
            state = updated.get();
        }
        return Optional.of(state);
    }

    private static <T extends Comparable<T>> Optional<BlockState> setProperty(
            String selector,
            BlockState state,
            Property<T> property,
            String rawValue) {
        Optional<T> value = property.getValue(rawValue);
        if (value.isEmpty()) {
            RoostTraces.LOGGER.warn("Invalid value '{}' for blockstate property '{}' in roost trace node selector: {}",
                    rawValue, property.getName(), selector);
            return Optional.empty();
        }
        return Optional.of(state.setValue(property, value.get()));
    }

    private static void addHolder(Holder<Block> holder, Map<ResourceLocation, TraceNodeChoice> output) {
        Block block = holder.value();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        output.putIfAbsent(id, new TraceNodeChoice(id, block, TraceCompat.naturalNodeState(block)));
    }

    private record SelectorParts(String blockId, String properties) {
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
