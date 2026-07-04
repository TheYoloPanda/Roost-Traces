# Codebase Structure

**Analysis Date:** 2026-07-02

## Directory Layout

```text
RoostTraces/
+-- .github/                       # Repository automation and GitHub metadata
+-- .gradle/                       # Local Gradle cache/state, not source
+-- .idea/                         # Local IntelliJ project files
+-- .planning/                     # GSD planning and codebase map artifacts
|   +-- codebase/                   # Generated codebase reference docs
+-- build/                         # Gradle build outputs and generated metadata
+-- gradle/
|   +-- wrapper/                    # Gradle wrapper jar and properties
+-- run/                           # NeoForge development run directory
+-- src/
|   +-- main/
|       +-- java/
|       |   +-- com/typ/roosttraces/
|       |       +-- compat/         # Create ReAutomated: Traces facade
|       |       +-- mixin/          # Ice and Fire CE structure hook
|       |       +-- placement/      # Candidate scan, placement, retry scheduler
|       |       +-- pool/           # Datapack node pool parsing and tags
|       |       +-- roost/          # Roost domain records, keys, saved data
|       |       +-- RoostTraces.java
|       |       +-- RoostTracesClient.java
|       |       +-- RoostTracesConfig.java
|       +-- resources/
|       |   +-- assets/roosttraces/lang/
|       |   +-- data/roosttraces/roost_trace_node_pools/
|       |   +-- data/roosttraces/tags/block/
|       |   +-- roosttraces.mixins.json
|       +-- templates/META-INF/     # Expanded into generated mod metadata
+-- build.gradle                   # NeoForge ModDevGradle build
+-- gradle.properties              # Versions, mod metadata, Gradle settings
+-- settings.gradle                # Plugin management and Gradle convention plugin
+-- README.md                      # User/modpack documentation
+-- DESCRIPTION.md                 # Mod page description
+-- CHANGELOG.md                   # Release notes
```

## Directory Purposes

**Root:**
- Purpose: Project build, wrapper, documentation, and source roots.
- Contains: `build.gradle`, `settings.gradle`, `gradle.properties`, `README.md`, `DESCRIPTION.md`, `CHANGELOG.md`, `gradlew`, `gradlew.bat`.
- Key files: `build.gradle`, `gradle.properties`, `src/main/templates/META-INF/neoforge.mods.toml`.

**`src/main/java/com/typ/roosttraces`:**
- Purpose: Main mod package and root-level NeoForge entry/config classes.
- Contains: `RoostTraces.java`, `RoostTracesClient.java`, `RoostTracesConfig.java`, and feature subpackages.
- Key files: `src/main/java/com/typ/roosttraces/RoostTraces.java`, `src/main/java/com/typ/roosttraces/RoostTracesClient.java`, `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`.

**`src/main/java/com/typ/roosttraces/compat`:**
- Purpose: Keep external Create ReAutomated: Traces API calls behind one facade.
- Contains: Compatibility helpers and API wrappers.
- Key files: `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`.

**`src/main/java/com/typ/roosttraces/mixin`:**
- Purpose: Host Sponge Mixin hooks into external mods.
- Contains: Ice and Fire CE roost piece injection.
- Key files: `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`.

**`src/main/java/com/typ/roosttraces/placement`:**
- Purpose: Own all scan, placement, rollback, and pending retry mechanics.
- Contains: Candidate record, candidate scanner, node placer, server tick scheduler.
- Key files: `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`, `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`, `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`.

**`src/main/java/com/typ/roosttraces/pool`:**
- Purpose: Own node pool configuration, tags, and selector resolution.
- Contains: JSON-backed selector config, resolved node choice record, block tag keys, reload listener.
- Key files: `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`, `src/main/java/com/typ/roosttraces/pool/NodeSelectorConfig.java`, `src/main/java/com/typ/roosttraces/pool/RoostTraceTags.java`.

**`src/main/java/com/typ/roosttraces/roost`:**
- Purpose: Own domain state for dragon roost types, keys, pending entries, placed entries, and persisted storage.
- Contains: Records, enum, resolver, key/seed utility, SavedData implementation, pending capture helper.
- Key files: `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`, `src/main/java/com/typ/roosttraces/roost/RoostKeys.java`, `src/main/java/com/typ/roosttraces/roost/RoostTypeResolver.java`.

**`src/main/resources/assets/roosttraces`:**
- Purpose: Client assets and translations.
- Contains: English localization for NeoForge config UI.
- Key files: `src/main/resources/assets/roosttraces/lang/en_us.json`.

**`src/main/resources/data/roosttraces`:**
- Purpose: Built-in datapack resources for node pools and block tags.
- Contains: Default node selector JSON and block tags.
- Key files: `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`, `src/main/resources/data/roosttraces/tags/block/roost_replaceable.json`, `src/main/resources/data/roosttraces/tags/block/create_reautomated_nodes.json`.

**`src/main/templates/META-INF`:**
- Purpose: Template mod metadata expanded by Gradle.
- Contains: `neoforge.mods.toml` with `${...}` placeholders.
- Key files: `src/main/templates/META-INF/neoforge.mods.toml`.

**`.planning/codebase`:**
- Purpose: Generated GSD codebase reference documents for planning and execution agents.
- Contains: Architecture and structure documents for this focus.
- Key files: `.planning/codebase/ARCHITECTURE.md`, `.planning/codebase/STRUCTURE.md`.

## Key File Locations

**Entry Points:**
- `src/main/java/com/typ/roosttraces/RoostTraces.java`: Common `@Mod` entry point and event/config registration.
- `src/main/java/com/typ/roosttraces/RoostTracesClient.java`: Client-only `@Mod` entry point for config screen extension.
- `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`: Mixin injection into Ice and Fire roost generation.
- `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`: Resource reload listener entry for node pool JSON.
- `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`: Server tick event entry for pending retries.

**Configuration:**
- `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`: Common NeoForge config spec and parsed replaceable id cache.
- `gradle.properties`: Minecraft, NeoForge, dependency versions, mod id, mod name, license, group, and version.
- `build.gradle`: ModDevGradle runs, dependency repositories, local runtime dependencies, source resource dirs, and metadata generation.
- `src/main/templates/META-INF/neoforge.mods.toml`: Generated mod metadata template and required mod dependencies.
- `src/main/resources/roosttraces.mixins.json`: Mixin package and mixin class list.

**Core Logic:**
- `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`: Main placement orchestrator and rollback logic.
- `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`: Candidate search and safe replacement rules.
- `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`: Datapack selector parsing and node resolution.
- `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`: Pending/placed persistence.
- `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`: Create ReAutomated: Traces integration boundary.

**Resources:**
- `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`: Built-in default, fire, ice, and lightning selector lists.
- `src/main/resources/data/roosttraces/tags/block/create_reautomated_nodes.json`: Built-in selector tag delegating to `#createreautomated:ore_nodes`.
- `src/main/resources/data/roosttraces/tags/block/fire_roost_nodes.json`: Roost-specific fire selector extension point.
- `src/main/resources/data/roosttraces/tags/block/ice_roost_nodes.json`: Roost-specific ice selector extension point.
- `src/main/resources/data/roosttraces/tags/block/lightning_roost_nodes.json`: Roost-specific lightning selector extension point.
- `src/main/resources/data/roosttraces/tags/block/roost_replaceable.json`: Safe roost floor materials that may be replaced by a node.
- `src/main/resources/assets/roosttraces/lang/en_us.json`: Config screen translations.

**Testing:**
- `src/test`: Not detected.
- `build.gradle`: Defines a `gameTestServer` run configuration, but no GameTest source files are present.

**Documentation:**
- `README.md`: User and modpack-facing behavior/config documentation.
- `DESCRIPTION.md`: Mod page description.
- `CHANGELOG.md`: Versioned change summary.

## Naming Conventions

**Files:**
- Main mod classes use `RoostTraces*`: `src/main/java/com/typ/roosttraces/RoostTraces.java`, `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`, `src/main/java/com/typ/roosttraces/RoostTracesClient.java`.
- Feature classes use domain prefixes: `RoostTraceNodePlacer`, `RoostTraceNodePoolResolver`, `RoostTraceSavedData`.
- Domain records use noun names: `PendingRoost`, `PlacedRoost`, `TraceNodeChoice`, `RoostCandidate`.
- Mixin classes end in `Mixin`: `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`.
- Resource files use lowercase snake_case: `roost_replaceable.json`, `create_reautomated_nodes.json`, `roost_trace_node_pools/default.json`.

**Directories:**
- Java packages are lower-case by layer under `src/main/java/com/typ/roosttraces/`: `compat`, `mixin`, `placement`, `pool`, `roost`.
- Datapack resources follow Minecraft namespace layout under `src/main/resources/data/roosttraces/`.
- Client assets follow Minecraft namespace layout under `src/main/resources/assets/roosttraces/`.
- Generated/planning docs use uppercase filenames under `.planning/codebase/`.

## Where to Add New Code

**New Roost Generation Hook:**
- Primary code: `src/main/java/com/typ/roosttraces/mixin/`
- Mixin registration: `src/main/resources/roosttraces.mixins.json`
- Supporting domain logic: `src/main/java/com/typ/roosttraces/roost/`
- Use this only when integrating another external structure or changing the generation entry point.

**New Placement Rule:**
- Primary code: `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`
- Placement behavior: `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`
- Config switch or numeric limit: `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`
- Keep candidate checks bounded and avoid chunk-loading calls.

**New Node Selector Feature:**
- Parser/resolution code: `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`
- Config record shape: `src/main/java/com/typ/roosttraces/pool/NodeSelectorConfig.java`
- Built-in datapack defaults: `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`
- Tags: `src/main/resources/data/roosttraces/tags/block/`

**New Persisted Roost Field:**
- Storage model: `src/main/java/com/typ/roosttraces/roost/PendingRoost.java` or `src/main/java/com/typ/roosttraces/roost/PlacedRoost.java`
- NBT serialization and load migration: `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`
- Key compatibility helpers: `src/main/java/com/typ/roosttraces/roost/RoostKeys.java`
- Preserve existing NBT keys unless an explicit migration is implemented.

**New External Create ReAutomated: Traces Call:**
- Implementation: `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`
- Call sites: Use the facade from `src/main/java/com/typ/roosttraces/placement/` or `src/main/java/com/typ/roosttraces/pool/`.
- Do not scatter direct `com.typ.traces.api` calls across placement or roost packages.

**New Config Option:**
- Config spec: `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`
- Client text: `src/main/resources/assets/roosttraces/lang/en_us.json`
- Documentation: `README.md`
- Keep gameplay-impacting defaults documented and stable for modpacks.

**New Block Tag:**
- Java tag key: `src/main/java/com/typ/roosttraces/pool/RoostTraceTags.java`
- Built-in data: `src/main/resources/data/roosttraces/tags/block/`
- Consumer: `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java` or `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`

**New Tests:**
- Primary location: `src/test/java/com/typ/roosttraces/`
- GameTest location: add an explicit source/package convention before writing tests; `build.gradle` has a `gameTestServer` run config but the repo currently has no test source tree.

**Utilities:**
- Shared helper methods that are roost-identity specific belong in `src/main/java/com/typ/roosttraces/roost/RoostKeys.java`.
- Placement-only helper methods belong in `src/main/java/com/typ/roosttraces/placement/`.
- External API wrappers belong in `src/main/java/com/typ/roosttraces/compat/`.
- Avoid adding broad manager classes; keep helpers near the layer that owns the behavior.

## Special Directories

**`.planning/codebase`:**
- Purpose: Generated architecture and codebase reference docs for GSD planning/execution.
- Generated: Yes.
- Committed: Intended to be committed by the orchestrator.

**`build`:**
- Purpose: Gradle build outputs, generated metadata, compiled classes, packaged jars.
- Generated: Yes.
- Committed: No.

**`.gradle`:**
- Purpose: Gradle local cache and task state.
- Generated: Yes.
- Committed: No.

**`run`:**
- Purpose: NeoForge client/server development run directory and generated game files.
- Generated: Yes.
- Committed: No.

**`.idea`:**
- Purpose: IntelliJ IDEA local project metadata.
- Generated: Yes.
- Committed: Usually no unless project policy says otherwise.

**`gradle/wrapper`:**
- Purpose: Gradle wrapper distribution metadata and wrapper jar.
- Generated: No.
- Committed: Yes.

**`src/main/templates`:**
- Purpose: Source template for mod metadata processed by Gradle.
- Generated: No.
- Committed: Yes.

**`src/main/resources/data/roosttraces/data_maps`:**
- Purpose: Reserved/empty datapack data map subtree.
- Generated: No.
- Committed: Only if files are added.

---

*Structure analysis: 2026-07-02*
