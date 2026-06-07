package com.typ.roosttraces.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.typ.roosttraces.RoostTraces;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class TraceHostDataMap {
    public record HostBlockEntry(Block hostBlock) {
        public static final Codec<HostBlockEntry> CODEC = RecordCodecBuilder.create(inst ->
                inst.group(
                        BuiltInRegistries.BLOCK.byNameCodec()
                                .fieldOf("host_block")
                                .forGetter(HostBlockEntry::hostBlock)
                ).apply(inst, HostBlockEntry::new));
    }

    public static final DataMapType<Block, HostBlockEntry> TYPE = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(RoostTraces.MODID, "host_block_for_node"),
            Registries.BLOCK,
            HostBlockEntry.CODEC
    ).synced(HostBlockEntry.CODEC, false).build();

    private TraceHostDataMap() {}

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(TYPE);
    }

    public static Block hostBlockFor(Block node) {
        HostBlockEntry entry = BuiltInRegistries.BLOCK.wrapAsHolder(node).getData(TYPE);
        return entry == null ? null : entry.hostBlock();
    }
}
