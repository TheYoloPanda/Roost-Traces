package com.typ.roosttraces.roost;

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
import net.minecraft.world.level.saveddata.SavedData;

public class RoostTraceSavedData extends SavedData {
    public static final String DATA_NAME = "roosttraces_roost_data";
    private static final long NO_RETRY_SCHEDULED = Long.MAX_VALUE;

    private final Map<String, PendingRoost> pending = new LinkedHashMap<>();
    private final Map<String, UnindexedPlacedRoost> indexPending = new LinkedHashMap<>();
    private final Map<String, PlacedRoost> placed = new LinkedHashMap<>();
    private long nextPendingRetryGameTime = NO_RETRY_SCHEDULED;
    private long nextIndexRetryGameTime = NO_RETRY_SCHEDULED;

    public static SavedData.Factory<RoostTraceSavedData> factory() {
        return new SavedData.Factory<>(RoostTraceSavedData::new, RoostTraceSavedData::load, null);
    }

    public static RoostTraceSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    public boolean addPending(PendingRoost roost) {
        if (placed.containsKey(roost.key()) || indexPending.containsKey(roost.key()) || pending.containsKey(roost.key())) {
            return false;
        }
        pending.put(roost.key(), roost);
        recomputeNextPendingRetryGameTime();
        setDirty();
        return true;
    }

    public void replacePending(PendingRoost roost) {
        if (placed.containsKey(roost.key()) || indexPending.containsKey(roost.key())) return;
        pending.put(roost.key(), roost);
        recomputeNextPendingRetryGameTime();
        setDirty();
    }

    public void removePending(String key) {
        if (pending.remove(key) != null) {
            recomputeNextPendingRetryGameTime();
            setDirty();
        }
    }

    public boolean isPlaced(String key) {
        return placed.containsKey(key);
    }

    public boolean hasDuePending(long gameTime) {
        return nextPendingRetryGameTime <= gameTime;
    }

    public boolean hasDueIndexPending(long gameTime) {
        return nextIndexRetryGameTime <= gameTime;
    }

    public void markPlaced(PlacedRoost roost) {
        boolean removedPending = pending.remove(roost.key()) != null;
        boolean removedIndexPending = indexPending.remove(roost.key()) != null;
        placed.put(roost.key(), roost);
        if (removedPending) recomputeNextPendingRetryGameTime();
        if (removedIndexPending) recomputeNextIndexRetryGameTime();
        setDirty();
    }

    public boolean markIndexPending(UnindexedPlacedRoost roost) {
        if (placed.containsKey(roost.key())) return false;
        boolean removedPending = pending.remove(roost.key()) != null;
        indexPending.put(roost.key(), roost);
        if (removedPending) recomputeNextPendingRetryGameTime();
        recomputeNextIndexRetryGameTime();
        setDirty();
        return true;
    }

    public void replaceIndexPending(UnindexedPlacedRoost roost) {
        if (placed.containsKey(roost.key())) return;
        indexPending.put(roost.key(), roost);
        recomputeNextIndexRetryGameTime();
        setDirty();
    }

    public void removeIndexPending(String key) {
        if (indexPending.remove(key) != null) {
            recomputeNextIndexRetryGameTime();
            setDirty();
        }
    }

    public Collection<PendingRoost> pendingSnapshot() {
        return List.copyOf(pending.values());
    }

    public Collection<UnindexedPlacedRoost> indexPendingSnapshot() {
        return List.copyOf(indexPending.values());
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

        ListTag indexPendingTag = new ListTag();
        for (UnindexedPlacedRoost roost : indexPending.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", roost.key());
            entry.putString("type", roost.type().id());
            entry.putLong("pivot", roost.pivotLong());
            entry.putLong("trace", roost.traceLong());
            entry.putLong("node", roost.nodeLong());
            entry.putString("node_id", roost.nodeId().toString());
            entry.putInt("attempts", roost.attempts());
            entry.putLong("next_attempt", roost.nextAttemptGameTime());
            indexPendingTag.add(entry);
        }
        tag.put("index_pending", indexPendingTag);

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

    static RoostTraceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RoostTraceSavedData data = new RoostTraceSavedData();
        ListTag pendingTag = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < pendingTag.size(); i++) {
            CompoundTag entry = pendingTag.getCompound(i);
            long pivot = entry.getLong("pivot");
            String key = RoostKeys.normalizeSavedKey(entry.getString("key"), pivot);
            if (key.isEmpty()) continue;
            data.pending.put(key, new PendingRoost(
                    key,
                    RoostType.fromId(entry.getString("type")),
                    pivot,
                    entry.getLong("chunk"),
                    entry.getLong("seed"),
                    entry.getInt("attempts"),
                    entry.getLong("next_attempt")));
        }

        ListTag indexPendingTag = tag.getList("index_pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < indexPendingTag.size(); i++) {
            CompoundTag entry = indexPendingTag.getCompound(i);
            long pivot = entry.getLong("pivot");
            String key = RoostKeys.normalizeSavedKey(entry.getString("key"), pivot);
            ResourceLocation nodeId = ResourceLocation.tryParse(entry.getString("node_id"));
            if (key.isEmpty() || nodeId == null) continue;
            data.pending.remove(key);
            data.indexPending.put(key, new UnindexedPlacedRoost(
                    key,
                    RoostType.fromId(entry.getString("type")),
                    pivot,
                    entry.getLong("trace"),
                    entry.getLong("node"),
                    nodeId,
                    entry.getInt("attempts"),
                    entry.getLong("next_attempt")));
        }

        ListTag placedTag = tag.getList("placed", Tag.TAG_COMPOUND);
        for (int i = 0; i < placedTag.size(); i++) {
            CompoundTag entry = placedTag.getCompound(i);
            long pivot = entry.getLong("pivot");
            String key = RoostKeys.normalizeSavedKey(entry.getString("key"), pivot);
            ResourceLocation nodeId = ResourceLocation.tryParse(entry.getString("node_id"));
            if (key.isEmpty() || nodeId == null) continue;
            data.pending.remove(key);
            data.indexPending.remove(key);
            data.placed.put(key, new PlacedRoost(
                    key,
                    RoostType.fromId(entry.getString("type")),
                    pivot,
                    entry.getLong("trace"),
                    entry.getLong("node"),
                    nodeId));
        }
        data.recomputeNextIndexRetryGameTime();
        data.recomputeNextPendingRetryGameTime();
        return data;
    }

    private void recomputeNextPendingRetryGameTime() {
        long next = NO_RETRY_SCHEDULED;
        for (PendingRoost roost : pending.values()) {
            next = Math.min(next, roost.nextAttemptGameTime());
        }
        nextPendingRetryGameTime = next;
    }

    private void recomputeNextIndexRetryGameTime() {
        long next = NO_RETRY_SCHEDULED;
        for (UnindexedPlacedRoost roost : indexPending.values()) {
            next = Math.min(next, roost.nextAttemptGameTime());
        }
        nextIndexRetryGameTime = next;
    }
}
