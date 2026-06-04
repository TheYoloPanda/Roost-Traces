package com.typ.roosttraces.roost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public class RoostTraceSavedData extends SavedData {
    public static final String DATA_NAME = "roosttraces_roost_data";

    private final Map<String, PendingRoost> pending = new LinkedHashMap<>();
    private final Map<String, PlacedRoost> placed = new LinkedHashMap<>();

    public static SavedData.Factory<RoostTraceSavedData> factory() {
        return new SavedData.Factory<>(RoostTraceSavedData::new, RoostTraceSavedData::load, null);
    }

    public static RoostTraceSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    public boolean addPending(PendingRoost roost) {
        if (placed.containsKey(roost.key()) || pending.containsKey(roost.key())) return false;
        pending.put(roost.key(), roost);
        setDirty();
        return true;
    }

    public void replacePending(PendingRoost roost) {
        if (placed.containsKey(roost.key())) return;
        pending.put(roost.key(), roost);
        setDirty();
    }

    public void removePending(String key) {
        if (pending.remove(key) != null) setDirty();
    }

    public boolean isPlaced(String key) {
        return placed.containsKey(key);
    }

    public void markPlaced(PlacedRoost roost) {
        pending.remove(roost.key());
        placed.put(roost.key(), roost);
        setDirty();
    }

    public Collection<PendingRoost> pendingSnapshot() {
        return List.copyOf(pending.values());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag pendingTag = new ListTag();
        for (PendingRoost roost : pending.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", roost.key());
            entry.putString("type", roost.type().id());
            entry.putLong("pivot", roost.pivotLong());
            entry.putLong("chunk", roost.chunkLong());
            entry.putLong("seed", roost.placementSeed());
            entry.putInt("attempts", roost.attempts());
            entry.putLong("next_attempt", roost.nextAttemptGameTime());
            pendingTag.add(entry);
        }
        tag.put("pending", pendingTag);

        ListTag placedTag = new ListTag();
        for (PlacedRoost roost : placed.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", roost.key());
            entry.putString("type", roost.type().id());
            entry.putLong("pivot", roost.pivotLong());
            entry.putLong("trace", roost.traceLong());
            entry.putLong("node", roost.nodeLong());
            entry.putString("node_id", roost.nodeId().toString());
            placedTag.add(entry);
        }
        tag.put("placed", placedTag);
        return tag;
    }

    private static RoostTraceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RoostTraceSavedData data = new RoostTraceSavedData();
        ListTag pendingTag = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < pendingTag.size(); i++) {
            CompoundTag entry = pendingTag.getCompound(i);
            String key = entry.getString("key");
            if (key.isEmpty()) continue;
            data.pending.put(key, new PendingRoost(
                    key,
                    RoostType.fromId(entry.getString("type")),
                    entry.getLong("pivot"),
                    entry.getLong("chunk"),
                    entry.getLong("seed"),
                    entry.getInt("attempts"),
                    entry.getLong("next_attempt")));
        }

        ListTag placedTag = tag.getList("placed", Tag.TAG_COMPOUND);
        for (int i = 0; i < placedTag.size(); i++) {
            CompoundTag entry = placedTag.getCompound(i);
            String key = entry.getString("key");
            ResourceLocation nodeId = ResourceLocation.tryParse(entry.getString("node_id"));
            if (key.isEmpty() || nodeId == null) continue;
            data.placed.put(key, new PlacedRoost(
                    key,
                    RoostType.fromId(entry.getString("type")),
                    entry.getLong("pivot"),
                    entry.getLong("trace"),
                    entry.getLong("node"),
                    nodeId));
        }
        return data;
    }
}
