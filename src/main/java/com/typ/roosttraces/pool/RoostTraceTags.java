package com.typ.roosttraces.pool;

import com.typ.roosttraces.RoostTraces;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class RoostTraceTags {
    public static final TagKey<Block> ROOST_REPLACEABLE = roostTag("roost_replaceable");
    public static final TagKey<Block> CREATE_REAUTOMATED_NODES = roostTag("create_reautomated_nodes");
    public static final TagKey<Block> FIRE_ROOST_NODES = roostTag("fire_roost_nodes");
    public static final TagKey<Block> ICE_ROOST_NODES = roostTag("ice_roost_nodes");
    public static final TagKey<Block> LIGHTNING_ROOST_NODES = roostTag("lightning_roost_nodes");

    public static final TagKey<Block> CREATE_REAUTOMATED_ORE_NODES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("createreautomated", "ore_nodes"));

    private RoostTraceTags() {}

    private static TagKey<Block> roostTag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(RoostTraces.MODID, path));
    }
}
