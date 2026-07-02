<!-- refreshed: 2026-07-02 -->
# Architecture

**Analysis Date:** 2026-07-02

## System Overview

```text
+-------------------------------------------------------------+
|                    NeoForge Mod Lifecycle                    |
| `src/main/java/com/typ/roosttraces/RoostTraces.java`         |
+-------------------+-------------------+---------------------+
| Config Bootstrap  | Resource Reload   | Server Tick Retry   |
| `RoostTracesConfig.java` | `pool/`    | `placement/`        |
+---------+---------+---------+---------+----------+----------+
          |                   |                    |
          v                   v                    v
+-------------------------------------------------------------+
|               Ice and Fire Roost Generation Hook             |
| `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java` |
+-------------------------------------------------------------+
          |
          v
+-------------------------------------------------------------+
|                   Roost Trace Placement Core                 |
| `src/main/java/com/typ/roosttraces/placement/`               |
| scan candidate -> choose node -> place node -> record result |
+-------------------+-------------------+---------------------+
          |                   |                    |
          v                   v                    v
+-------------------+ +-------------------+ +------------------+
| Roost State       | | Node Pools        | | Trace API Compat |
| `roost/`          | | `pool/` + JSON    | | `compat/`        |
+-------------------+ +-------------------+ +------------------+
          |                   |                    |
          v                   v                    v
+-------------------------------------------------------------+
| SavedData, datapack resources, Create ReAutomated: Traces API |
| `RoostTraceSavedData.java`, `src/main/resources/data/`        |
+-------------------------------------------------------------+
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| Mod bootstrap | Registers common config, resource reload listener, and server tick scheduler on the correct NeoForge buses. | `src/main/java/com/typ/roosttraces/RoostTraces.java` |
| Client bootstrap | Registers the NeoForge config screen extension only on the client distribution. | `src/main/java/com/typ/roosttraces/RoostTracesClient.java` |
| Common config | Defines placement, node pool, replaceable block, retry, and debug settings; caches parsed replaceable block ids after config load. | `src/main/java/com/typ/roosttraces/RoostTracesConfig.java` |
| Ice and Fire hook | Injects after `DragonRoostPiece.postProcess`, resolves roost type, and invokes worldgen-time node placement. | `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java` |
| Roost domain | Represents roost type, deterministic keys/seeds, pending roosts, placed roosts, and saved data. | `src/main/java/com/typ/roosttraces/roost/` |
| Node pool resolver | Loads datapack selector JSON, expands block ids/tags into trace node choices, and applies fallback/default pool rules. | `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` |
| Placement scanner | Finds a valid roost floor candidate without force-loading chunks and caches sorted offset lists by radius. | `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java` |
| Placement executor | Chooses a node, suppresses generated surface trace output, places a node-only block, records Trace index state, and rolls back on failure. | `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java` |
| Pending retry scheduler | Processes existing pending roost entries on server tick with bounded per-tick and per-entry limits. | `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java` |
| Trace compatibility facade | Wraps Create ReAutomated: Traces public APIs and the Create ReAutomated natural node state helper. | `src/main/java/com/typ/roosttraces/compat/TraceCompat.java` |
| Datapack resources | Provides default node pool selectors and safe roost replacement tags. | `src/main/resources/data/roosttraces/` |

## Pattern Overview

**Overall:** Mixin-captured worldgen placement with datapack-configured selection and SavedData-backed bookkeeping.

**Key Characteristics:**
- Use the Ice and Fire structure generation lifecycle as the primary trigger through `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java:21`.
- Place the visible node during roost generation in `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:31`, then defer Trace index and SavedData writes to the server thread at `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:64`.
- Keep gameplay configuration data-driven with `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json` and block tags under `src/main/resources/data/roosttraces/tags/block/`.
- Avoid force-loading chunks by checking `LevelAccessor.hasChunk` inside `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java:103` and the scheduler chunk check at `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:55`.

## Layers

**Bootstrap Layer:**
- Purpose: Register config, reload listeners, mixins, and server event handlers through NeoForge/FML lifecycle points.
- Location: `src/main/java/com/typ/roosttraces/RoostTraces.java`, `src/main/java/com/typ/roosttraces/RoostTracesClient.java`, `src/main/templates/META-INF/neoforge.mods.toml`, `src/main/resources/roosttraces.mixins.json`
- Contains: `@Mod` classes, mod metadata template, mixin config, event bus registration.
- Depends on: NeoForge mod event bus, NeoForge game event bus, FML config APIs.
- Used by: Minecraft/NeoForge mod loader.

**Mixin Hook Layer:**
- Purpose: Enter the Ice and Fire roost generation flow at the end of roost piece placement.
- Location: `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`
- Contains: A single `@Mixin` targeting `com.iafenvoy.iceandfire.world.structure.DragonRoostStructure$DragonRoostPiece` and one `@Inject` handler.
- Depends on: Ice and Fire CE class names, Sponge Mixin, `RoostTypeResolver`, `RoostTraceNodePlacer`.
- Used by: Mixin runtime through `src/main/resources/roosttraces.mixins.json`.

**Roost Domain Layer:**
- Purpose: Represent roost identity and durable placement state independently from scanning and placement mechanics.
- Location: `src/main/java/com/typ/roosttraces/roost/`
- Contains: `RoostType`, `RoostTypeResolver`, `RoostKeys`, `PendingRoost`, `PlacedRoost`, `RoostTraceSavedData`, and `RoostPendingRegistry`.
- Depends on: Minecraft positions, dimensions, chunks, `SavedData`.
- Used by: `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java` and `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`.

**Node Pool Layer:**
- Purpose: Convert datapack selectors and block tags into deterministic candidate node choices.
- Location: `src/main/java/com/typ/roosttraces/pool/`
- Contains: `NodeSelectorConfig`, `TraceNodeChoice`, `RoostTraceTags`, `RoostTraceNodePoolResolver`.
- Depends on: Gson, Minecraft registries/tags, Create ReAutomated natural node states through `TraceCompat`.
- Used by: `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`.

**Placement Layer:**
- Purpose: Find safe roost material, clear one block above the node when allowed, place the selected node state, and register results.
- Location: `src/main/java/com/typ/roosttraces/placement/`
- Contains: `RoostCandidate`, `RoostCandidateScanner`, `RoostTraceNodePlacer`, `RoostTracePlacementScheduler`.
- Depends on: Config, node pools, roost state, Trace API facade, Minecraft `LevelAccessor`/`ServerLevel`/`WorldGenLevel`.
- Used by: Mixin hook and server tick event registration.

**Compatibility Layer:**
- Purpose: Isolate direct calls into Create ReAutomated: Traces and Create ReAutomated helper behavior.
- Location: `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`
- Contains: Public Trace API wrappers, worldgen exclusion calls, index record call, natural node state lookup.
- Depends on: `com.typ.traces.api.TraceApi`, `com.typ.traces.api.TraceWorldgenExclusions`, reflection for `unstable()`.
- Used by: `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` and `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`.

**Resource Layer:**
- Purpose: Provide built-in datapack defaults and client-facing translations.
- Location: `src/main/resources/`
- Contains: `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`, block tags under `src/main/resources/data/roosttraces/tags/block/`, lang file `src/main/resources/assets/roosttraces/lang/en_us.json`, and mixin config `src/main/resources/roosttraces.mixins.json`.
- Depends on: Minecraft datapack and resource reload systems.
- Used by: Node pool resolver, block tag checks, NeoForge config screen.

## Data Flow

### Primary Request Path

1. NeoForge constructs `RoostTraces`, registering config, reload listener, and scheduler (`src/main/java/com/typ/roosttraces/RoostTraces.java:14`, `src/main/java/com/typ/roosttraces/RoostTraces.java:20`, `src/main/java/com/typ/roosttraces/RoostTraces.java:23`, `src/main/java/com/typ/roosttraces/RoostTraces.java:24`).
2. Ice and Fire places a dragon roost piece, then the mixin runs after `postProcess` returns (`src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java:21`).
3. The mixin validates that the pivot is inside the chunk box, resolves the roost type from the piece class hierarchy, and calls worldgen placement (`src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java:31`, `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java:32`, `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java:33`).
4. `RoostTraceNodePlacer.placeDuringWorldgen` checks config, resolves a node pool, chooses a node by deterministic seed, scans for a candidate, suppresses generated trace output, and places the node-only block (`src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:31`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:42`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:47`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:51`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:52`).
5. The placer defers saved marker and Trace index registration with `ServerLevel.getServer().execute` (`src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:64`).
6. The server-thread callback records the node through `TraceCompat.recordNode` and marks the roost placed in `RoostTraceSavedData` (`src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:151`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:152`, `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java:58`).

### Datapack Reload Flow

1. Bootstrap registers `RoostTraceNodePoolResolver::onAddReloadListener` on the NeoForge event bus (`src/main/java/com/typ/roosttraces/RoostTraces.java:23`).
2. The resolver adds a `SimpleJsonResourceReloadListener` for the `roost_trace_node_pools` folder (`src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:37`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:161`).
3. Reload parses every JSON object into a `NodeSelectorConfig`, using the previous parsed config as fallback while iterating resources (`src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:167`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:182`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:193`).
4. Placement resolves selectors into `TraceNodeChoice` values through direct block ids and `#tag` selectors (`src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:41`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:58`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:76`).

### Existing Pending Roost Retry Flow

1. `RoostTracePlacementScheduler.onServerTick` exits when placement is disabled or the pending queue has completed (`src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:25`).
2. Each server tick scans server levels for `RoostTraceSavedData` with pending entries and applies `maxPendingRoostsPerTick` budget (`src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:35`, `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:37`).
3. Pending entries are processed only when their target chunk is already loaded (`src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:53`, `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:55`).
4. Failed non-terminal placements are rescheduled with increasing delay; terminal failures or entries over the attempt cap are removed (`src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:64`, `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:73`, `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:83`).

**State Management:**
- Durable roost state lives in `SavedData` named `roosttraces_roost_data` (`src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java:20`).
- Pending and placed roosts are stored in `LinkedHashMap` fields inside `RoostTraceSavedData` and serialized to `pending` and `placed` NBT lists (`src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java:22`, `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java:23`, `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java:69`, `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java:99`).
- Runtime config caches are held in `RoostTracesConfig.replaceableIds` (`src/main/java/com/typ/roosttraces/RoostTracesConfig.java:41`).
- Runtime datapack config is held in the volatile `RoostTraceNodePoolResolver.config` field (`src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:33`).
- Scanner offset lists are cached by scan radius in `RoostCandidateScanner.OFFSETS_BY_RADIUS` (`src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java:25`).

## Key Abstractions

**Roost Type:**
- Purpose: Encode fire, ice, lightning, and unknown roost variants for pool selection and saved data.
- Examples: `src/main/java/com/typ/roosttraces/roost/RoostType.java`, `src/main/java/com/typ/roosttraces/roost/RoostTypeResolver.java`
- Pattern: Enum plus resolver that inspects Ice and Fire piece class names.

**Roost Key and Seed:**
- Purpose: Create exact per-dimension roost identity and deterministic node selection.
- Examples: `src/main/java/com/typ/roosttraces/roost/RoostKeys.java:11`, `src/main/java/com/typ/roosttraces/roost/RoostKeys.java:30`
- Pattern: Static utility using dimension id, pivot position, chunk position, roost type, and `mix64`.

**Node Selector Config:**
- Purpose: Represent datapack-configured selector lists for default, fire, ice, and lightning roosts.
- Examples: `src/main/java/com/typ/roosttraces/pool/NodeSelectorConfig.java`, `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`
- Pattern: Immutable record plus defaults and `selectorsFor(RoostType)`.

**Trace Node Choice:**
- Purpose: Carry the selected node id, block, and exact block state to place.
- Examples: `src/main/java/com/typ/roosttraces/pool/TraceNodeChoice.java`
- Pattern: Immutable record resolved before placement.

**Roost Candidate:**
- Purpose: Carry candidate node/trace positions, check count, and score from scanner to placer.
- Examples: `src/main/java/com/typ/roosttraces/placement/RoostCandidate.java`, `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java:47`
- Pattern: Immutable record returned in `Optional`.

**Placement Result:**
- Purpose: Give placement callers a bounded result vocabulary for logging, retry, rollback, and terminal failure handling.
- Examples: `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:175`, `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:83`
- Pattern: Enum plus scheduler terminal classification.

**Trace Compatibility Facade:**
- Purpose: Centralize API calls into Create ReAutomated: Traces and natural node state lookup.
- Examples: `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`
- Pattern: Static facade with runtime exception handling around Trace index recording.

## Entry Points

**Common Mod Entry:**
- Location: `src/main/java/com/typ/roosttraces/RoostTraces.java`
- Triggers: FML creates the `@Mod(RoostTraces.MODID)` class.
- Responsibilities: Register config spec, config load callback, reload listener, and scheduler.

**Client Mod Entry:**
- Location: `src/main/java/com/typ/roosttraces/RoostTracesClient.java`
- Triggers: FML creates the `@Mod(value = RoostTraces.MODID, dist = Dist.CLIENT)` class on client.
- Responsibilities: Register NeoForge's built-in config screen factory.

**Mixin Entry:**
- Location: `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`
- Triggers: Ice and Fire CE dragon roost piece `postProcess` return.
- Responsibilities: Convert roost generation into node placement work.

**Reload Listener Entry:**
- Location: `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`
- Triggers: NeoForge `AddReloadListenerEvent`.
- Responsibilities: Load datapack node pool JSON into runtime selector config.

**Server Tick Entry:**
- Location: `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`
- Triggers: NeoForge `ServerTickEvent.Post`.
- Responsibilities: Process existing pending roost placements without force-loading chunks.

**Build Metadata Entry:**
- Location: `build.gradle`, `src/main/templates/META-INF/neoforge.mods.toml`
- Triggers: Gradle `generateModMetadata` process resources task.
- Responsibilities: Expand mod metadata placeholders and include generated metadata in main resources.

## Architectural Constraints

- **Threading:** Worldgen-time block placement happens in `WorldGenLevel` from `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:31`; SavedData and Trace index writes are deferred with `ServerLevel.getServer().execute` at `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:64`. Preserve this separation for new worldgen writes.
- **Global state:** Module-level singletons include `RoostTraces.LOGGER` in `src/main/java/com/typ/roosttraces/RoostTraces.java:16`, `RoostTracesConfig.replaceableIds` in `src/main/java/com/typ/roosttraces/RoostTracesConfig.java:41`, `RoostTraceNodePoolResolver.config` in `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:33`, `RoostCandidateScanner.OFFSETS_BY_RADIUS` in `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java:25`, and `RoostTracePlacementScheduler.legacyComplete` in `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:20`.
- **Circular imports:** No Java package-level circular dependency chain is detected from current imports. Flow is directional: `mixin/` and `placement/` depend on `roost/`, `pool/`, and `compat/`; `pool/` depends on `compat/` and `roost/`; `compat/` depends only on config/logger plus external Trace APIs.
- **Chunk loading:** Candidate scanning and pending retries check loaded chunks through `LevelAccessor.hasChunk`; do not add placement paths that call chunk-loading APIs from generation or tick handlers.
- **Mixin target stability:** `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java:19` targets an Ice and Fire CE internal class by string with `remap = false`; source changes in Ice and Fire CE can break the hook.
- **Saved data compatibility:** `RoostTraceSavedData.load` normalizes saved keys through `RoostKeys.normalizeSavedKey` (`src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java:105`, `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java:121`). Preserve existing NBT field names unless a migration path is implemented.

## Anti-Patterns

### Unbounded Post-Generation Scanning

**What happens:** A server tick hook can access every loaded level and pending roost entry through `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java:35`.
**Why it's wrong:** New placement work belongs in the roost generation hook; broad scans or force-loaded chunk work would create server tick cost and chunk loading side effects.
**Do this instead:** Add new normal placement behavior to `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:31` and keep the scheduler bounded to existing pending saved data.

### Direct Worldgen Thread Persistence

**What happens:** The worldgen placement path has access to `WorldGenLevel` and `ServerLevel`, but it only places the block immediately and defers persistence/API writes at `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:64`.
**Why it's wrong:** Writing SavedData or external indexes directly inside the structure post-process path risks threading and lifecycle issues.
**Do this instead:** Keep worldgen block changes in `WorldGenLevel` and schedule `RoostTraceSavedData`/`TraceCompat.recordNode` writes on the main server thread as in `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:151`.

### Hardcoded Node Rewards

**What happens:** Built-in defaults are expressed through tags and datapack JSON, not hardcoded block lists in placement (`src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`, `src/main/resources/data/roosttraces/tags/block/create_reautomated_nodes.json`).
**Why it's wrong:** Hardcoding roost reward nodes in Java bypasses datapack control and modpack configurability.
**Do this instead:** Add new selectors or tags under `src/main/resources/data/roosttraces/`, then expand parser behavior in `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` only when the JSON format changes.

### Bypassing the Trace API Facade

**What happens:** Placement and pool resolution call `TraceCompat` for Trace API integration and natural state lookup (`src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:51`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:87`).
**Why it's wrong:** Direct scattered calls to external APIs make version bumps and failure handling harder.
**Do this instead:** Route Create ReAutomated: Traces calls through `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`.

## Error Handling

**Strategy:** Use early exits, `Optional`, bounded `PlacementResult` values, rollback on partial placement failure, and warnings for external API or datapack issues.

**Patterns:**
- Config and unknown roost type failures return explicit `PlacementResult` values in `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:31`.
- Candidate scanning returns `Optional.empty()` instead of throwing when no safe position is found (`src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java:29`).
- Direct selector parsing logs invalid selectors and returns `Optional.empty()` (`src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java:76`).
- `TraceCompat.recordNode` catches `RuntimeException`, logs a warning, and reports failure (`src/main/java/com/typ/roosttraces/compat/TraceCompat.java:33`).
- Failed Trace index registration in the pending path rolls back the placed block before returning `TRACE_INDEX_FAILED` (`src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java:93`).

## Cross-Cutting Concerns

**Logging:** Use the shared SLF4J logger `RoostTraces.LOGGER` from `src/main/java/com/typ/roosttraces/RoostTraces.java:16`; debug placement logs are gated by `RoostTracesConfig.DEBUG`.
**Validation:** Config validation lives in `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`; datapack selector validation lives in `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`; candidate safety checks live in `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`.
**Authentication:** Not applicable; this is an offline Minecraft mod with no auth layer.
**Configuration:** Common config lives in NeoForge config spec `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`; datapack extension points live under `src/main/resources/data/roosttraces/`.
**External integration:** Create ReAutomated: Traces integration is centralized in `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`; Ice and Fire integration is centralized in `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`.

---

*Architecture analysis: 2026-07-02*
