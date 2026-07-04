# Roost Traces

NeoForge 1.21.1 companion mod for Ice and Fire CE, Create ReAutomated, and Create ReAutomated: Traces.

Roost Traces places one compatible Create ReAutomated node directly in each generated surface dragon roost. It registers that node with Create ReAutomated: Traces as a node-only Trace Finder target, without adding a visible trace outcrop or platform.

## Requirements

- Ice and Fire CE
- Create ReAutomated
- Create ReAutomated: Traces 0.3.x
- Create 6 and its normal runtime dependencies

## Behavior

- Hooks Ice and Fire CE surface dragon roost generation.
- Places the node during worldgen when a safe roost-floor position is available.
- Suppresses the generic Traces surface outcrop for the roost node.
- Defers saved-data and Trace index writes to the server thread.
- Retries recoverable placement/index failures with a bounded per-tick budget.
- Never force-loads chunks.

## Node Pools

Roost node choices are datapack-driven:

```text
data/roosttraces/roost_trace_node_pools/default.json
```

Selectors may be direct block ids or block tags:

```json
{
  "inherit_default": false,
  "default": [],
  "fire": ["createreautomated:gold_node"],
  "ice": ["createreautomated:diamond_node"],
  "lightning": ["createreautomated:zinc_node"]
}
```

Use tags for larger pools:

```json
{
  "inherit_default": false,
  "default": [],
  "fire": ["#my_pack:roost/fire_nodes"],
  "ice": ["#my_pack:roost/ice_nodes"],
  "lightning": ["#my_pack:roost/lightning_nodes"]
}
```

Choices are deterministic per roost seed. Multiple pool files are applied in deterministic resource-id order.

## Config

Common config controls:

- placement enable/disable
- worldgen capture
- scan radius and candidate check budget
- bounded retry budget
- Trace index registration
- replaceable roost block ids
- datapack pool behavior

`existingPairRadius` remains readable for old config files but is no longer used by new placement.

Safe replaceable roost blocks can also be extended with:

```text
data/roosttraces/tags/block/roost_replaceable.json
```
