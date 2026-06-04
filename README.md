# Roost Traces

Roost Traces is a companion mod for Minecraft 1.21.1 on NeoForge. Its purpose is to make Ice and Fire CE dragon roosts part of the Create ReAutomated ore node loop.

Normally, Create ReAutomated: Traces only marks ore nodes that already exist in world generation. Dragon roosts are special structures, so this mod gives each compatible roost its own ore node without relying on the normal trace feature. The result is a roost that can be discovered with the Trace Finder and mined through Create ReAutomated systems, while still preserving the shape and gameplay of the roost.

When a dragon roost is generated, the mod captures the roost position and later places one Create ReAutomated Node into the roost floor. The Node is placed flush with the ground, without a stone platform and without an ore trace block above it, so it does not create an obstacle inside the roost. The position is still registered with the Create ReAutomated: Traces index, so the Trace Finder can detect it.

The mod does not constantly scan the world and does not force-load chunks. The Ice and Fire mixin only captures lightweight roost data. The actual placement runs server-side on the main thread with a per-tick budget. Backfill for existing worlds is available, but disabled by default.

## Dependencies

Roost Traces requires Ice and Fire CE, Create ReAutomated, and Create ReAutomated: Traces.

In a normal modpack, all required mods must be present in the mods folder. The dependency entries in `mods.toml` only tell NeoForge that these mods are required; they do not download them automatically.

## Node Pools

Roost node pools are configured through datapacks. The main file is:

```text
data/roosttraces/roost_trace_node_pools/default.json
```

For example, this makes fire dragon roosts use copper nodes and ice dragon roosts use zinc nodes:

```json
{
  "inherit_default": false,
  "default": [
    "#roosttraces:create_reautomated_nodes"
  ],
  "fire": [
    "createreautomated:copper_node"
  ],
  "ice": [
    "createreautomated:zinc_node"
  ],
  "lightning": [
    "#roosttraces:create_reautomated_nodes"
  ]
}
```

Infinite nodes are ignored. Stock Create ReAutomated nodes already have the trace mappings needed by Create ReAutomated: Traces.

Custom nodes need a trace mapping in:

```text
data/createreautomatedtraces/data_maps/block/trace_block_for_node.json
```

If a custom node does not provide a base rock like `OreNodeBlock`, an override can be added here:

```text
data/roosttraces/data_maps/block/host_block_for_node.json
```

## Configuration

The common config controls placement, scan radius, duplicate detection, tick budget, and backfill. The defaults are conservative. Backfill stays off unless explicitly enabled.

Roost blocks that may be replaced can be configured through the common config or through this tag:

```text
data/roosttraces/tags/block/roost_replaceable.json
```
