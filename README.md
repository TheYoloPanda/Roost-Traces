# Roost Traces

Roost Traces is a NeoForge companion mod for Minecraft 1.21.1. It connects Ice and Fire CE dragon roosts to the Create ReAutomated node loop by placing one compatible Create ReAutomated Node inside each generated surface roost and registering that position with Create ReAutomated: Traces.

The node is placed directly into the roost floor, without adding a visible trace outcrop or platform. This keeps the roost shape mostly unchanged while still allowing the Trace Finder to discover the node and Create ReAutomated machinery to extract from it. Placement is deferred to the main server thread, uses a per-tick budget, and does not force-load chunks. Existing worlds can be scanned through the optional backfill feature, which is disabled by default.

## Dependencies

Roost Traces requires Ice and Fire CE, Create ReAutomated, and Create ReAutomated: Traces. Uranus and Jupiter are dependencies of Ice and Fire CE itself; they are needed in a working modpack, but Roost Traces does not declare them as direct public dependencies.

The dependencies declared in `mods.toml` only tell NeoForge what must already be present. They do not download missing mods for a pack.

## Roost Node Pools

The nodes that may appear inside dragon roosts are controlled with datapack JSON. The main file is:

```text
data/roosttraces/roost_trace_node_pools/default.json
```

The file has four selector lists: `default`, `fire`, `ice`, and `lightning`. Selectors can be direct block ids, such as `createreautomated:gold_node`, or block tags, such as `#createreautomated:ore_nodes`. A selector list is treated as a whitelist. If a node is not selected by any pool used for that roost type, it will not be chosen.

The `inherit_default` field decides whether fire, ice, and lightning pools also include the `default` pool. Set it to `false` when each roost type should have its own exact list.

This example makes fire roosts generate only gold nodes, ice roosts only diamond nodes, and lightning roosts only zinc nodes:

```json
{
  "inherit_default": false,
  "default": [],
  "fire": [
    "createreautomated:gold_node"
  ],
  "ice": [
    "createreautomated:diamond_node"
  ],
  "lightning": [
    "createreautomated:zinc_node"
  ]
}
```

If a pool contains more than one valid node, Roost Traces chooses one deterministically from the roost seed. The same roost should keep the same chosen node across reloads.

This example allows several nodes in fire roosts but completely excludes copper and zinc by simply not listing them:

```json
{
  "inherit_default": false,
  "default": [],
  "fire": [
    "createreautomated:gold_node",
    "createreautomated:diamond_node",
    "createreautomated:nether_gold_node"
  ],
  "ice": [
    "createreautomated:diamond_node",
    "createreautomated:deepslate_diamond_node"
  ],
  "lightning": [
    "createreautomated:zinc_node",
    "createreautomated:deepslate_zinc_node"
  ]
}
```

You can also keep the JSON compact by defining your own tags and using those tags as selectors:

```json
{
  "inherit_default": false,
  "default": [],
  "fire": [
    "#my_pack:roost/fire_nodes"
  ],
  "ice": [
    "#my_pack:roost/ice_nodes"
  ],
  "lightning": [
    "#my_pack:roost/lightning_nodes"
  ]
}
```

A matching tag file could look like this:

```text
data/my_pack/tags/block/roost/fire_nodes.json
```

```json
{
  "replace": false,
  "values": [
    "createreautomated:gold_node",
    "createreautomated:nether_gold_node"
  ]
}
```

By default, Roost Traces includes `#roosttraces:create_reautomated_nodes`, which points to `#createreautomated:ore_nodes`, and `#roosttraces:custom_nodes`, which is empty for pack authors to fill. Infinite nodes are ignored automatically even if they are selected.

## Custom Nodes

Custom nodes can be used, but they must be valid from the point of view of Create ReAutomated: Traces. Each custom node needs a trace mapping in:

```text
data/createreautomatedtraces/data_maps/block/trace_block_for_node.json
```

For example:

```json
{
  "values": {
    "my_pack:ruby_node": {
      "trace_block": "my_pack:ruby_ore"
    }
  }
}
```

Roost Traces also needs to know which host block should be restored or treated as the natural base around a custom node. Stock Create ReAutomated nodes expose this through their node block, but custom nodes can provide an override here:

```text
data/roosttraces/data_maps/block/host_block_for_node.json
```

```json
{
  "values": {
    "my_pack:ruby_node": {
      "host_block": "minecraft:stone"
    }
  }
}
```

Once the node has a trace mapping and a host block, add it to a roost pool directly or through `data/roosttraces/tags/block/custom_nodes.json`.

## Placement Config

The common config controls whether roosts are captured during world generation, whether deferred placement runs after chunks generate, the scan radius, duplicate detection radius, per-tick placement budget, and optional backfill for existing worlds.

Backfill is intended for worlds that already contain Ice and Fire CE roosts generated before Roost Traces was installed. It scans loaded chunks for roost-like block patterns and queues matching candidates. It stays disabled by default because it is extra work and can only infer old roosts from existing blocks.

The node position scanner only replaces blocks considered safe roost material. The default replaceable list covers Ice and Fire CE crackled, chared, and frozen dirt, gravel, and cobblestone. You can extend that list through the common config or with:

```text
data/roosttraces/tags/block/roost_replaceable.json
```

Treasure pile blocks are not replaced by default. If you deliberately want the scanner to allow Ice and Fire CE pile blocks as valid placement or clearance material, enable `allowPileReplacement` in the common config.
