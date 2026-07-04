# Coding Conventions

**Analysis Date:** 2026-07-02

## Naming Patterns

**Files:**
- Use one PascalCase Java type per file under `src/main/java/com/typ/roosttraces/`: examples include `src/main/java/com/typ/roosttraces/RoostTraces.java`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`, and `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`.
- Keep package directories lowercase and domain-based: `src/main/java/com/typ/roosttraces/placement/`, `src/main/java/com/typ/roosttraces/pool/`, `src/main/java/com/typ/roosttraces/roost/`, `src/main/java/com/typ/roosttraces/compat/`, and `src/main/java/com/typ/roosttraces/mixin/`.
- Resource file names are lowercase with underscores where Minecraft expects registry-style paths: `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`, `src/main/resources/data/roosttraces/tags/block/roost_replaceable.json`, and `src/main/resources/roosttraces.mixins.json`.

**Functions:**
- Use lowerCamelCase for methods: `placeDuringWorldgen`, `recordPlaced`, `pendingSnapshot`, `normalizeSavedKey`, `expandSelectors`, and `applyProperties`.
- Use short accessor names on records when they represent domain values: `PendingRoost.pivot()`, `PendingRoost.chunkPos()`, `RoostType.id()`, and `TraceNodeChoice.nodeState()`.
- Prefix mixin injector methods with the mod id and `$`: `roosttraces$afterRoostGenerated` in `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`.

**Variables:**
- Use lowerCamelCase for locals and parameters: `nodePos`, `chunkPos`, `pending`, `remainingBudget`, `rawValue`, and `replaceableIds`.
- Use uppercase snake case for constants and public config spec values: `MODID`, `LOGGER`, `DATA_NAME`, `PLACE_FLAGS`, `RETRY_DELAY_TICKS`, `MAX_LEGACY_ATTEMPTS`, `SCAN_RADIUS`, and `REGISTER_IN_TRACE_INDEX`.
- For immutable snapshots from volatile or saved state, name the local after its role: `snapshot` in `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` and `pending` in `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`.

**Types:**
- Use PascalCase nouns for classes, records, and enums: `RoostTraceSavedData`, `RoostCandidateScanner`, `NodeSelectorConfig`, `PendingRoost`, `PlacedRoost`, and `RoostType`.
- Use Java records for immutable domain transfer data: `src/main/java/com/typ/roosttraces/placement/RoostCandidate.java`, `src/main/java/com/typ/roosttraces/roost/PendingRoost.java`, `src/main/java/com/typ/roosttraces/roost/PlacedRoost.java`, `src/main/java/com/typ/roosttraces/pool/TraceNodeChoice.java`, and `src/main/java/com/typ/roosttraces/pool/NodeSelectorConfig.java`.
- Use enums for closed result and domain sets: `RoostType` in `src/main/java/com/typ/roosttraces/roost/RoostType.java` and `PlacementResult` inside `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`.

## Code Style

**Formatting:**
- No repository formatter configuration is detected. There is no `.editorconfig`, Spotless config, Checkstyle config, PMD config, or formatter task in `build.gradle`.
- Use 4-space indentation for Java and Gradle files, as in `src/main/java/com/typ/roosttraces/RoostTracesConfig.java` and `build.gradle`.
- Use K&R brace placement for Java classes, methods, loops, and conditionals. Opening braces stay on the declaration line.
- Prefer compact guard clauses for simple validation and lifecycle exits:

```java
if (!RoostTracesConfig.PLACEMENT_ENABLED.get() || !RoostTracesConfig.CAPTURE_DURING_WORLDGEN.get()) return;
if (candidate.isEmpty()) return PlacementResult.NO_CANDIDATE;
```

- Use multi-line record declarations and constructor calls when the argument list carries domain meaning, as in `src/main/java/com/typ/roosttraces/roost/PendingRoost.java` and `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`.
- Use 2-space JSON indentation for data and assets under `src/main/resources/`, matching `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json` and `src/main/resources/assets/roosttraces/lang/en_us.json`.

**Linting:**
- Static lint tooling is not detected.
- Java compilation is configured with UTF-8 encoding and `-Xlint:deprecation` in `build.gradle`.
- Treat `.\gradlew.bat compileJava` as the main mechanical style and API-safety check once the Gradle wrapper distribution is available.

## Import Organization

**Order:**
1. `java.*` imports first, when present: examples include `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` and `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`.
2. Project and external library imports next, separated by a blank line: examples include `com.typ.roosttraces.*`, `com.typ.traces.api.*`, `com.google.gson.*`, and `org.spongepowered.asm.mixin.*`.
3. Minecraft and NeoForge imports last: examples include `net.minecraft.*` and `net.neoforged.*`.

**Path Aliases:**
- Not applicable. Java package imports use fully qualified package names rooted at `com.typ.roosttraces`, `net.minecraft`, `net.neoforged`, and dependency packages.
- Do not use wildcard imports. All source files under `src/main/java/com/typ/roosttraces/` use explicit imports.

## Error Handling

**Patterns:**
- Use result enums for expected placement outcomes instead of exceptions. `RoostTraceNodePlacer.PlacementResult` represents `PLACED`, `DISABLED`, `NO_CANDIDATE`, `TRACE_INDEX_FAILED`, and related outcomes in `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`.
- Use `Optional<T>` for nullable lookup and parse results. Examples: `RoostCandidateScanner.find`, `RoostTraceNodePoolResolver.parseDirectSelector`, `RoostTraceNodePoolResolver.applyProperties`, and `TraceCompat.traceBlockFor`.
- Catch exceptions only around unstable external boundaries. `src/main/java/com/typ/roosttraces/compat/TraceCompat.java` catches `RuntimeException` from `TraceApi.recordExternalNode` and reflection exceptions from Create ReAutomated node state lookup.
- Recover from datapack parse problems by logging and skipping invalid entries. `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` logs invalid selectors, unknown block ids, invalid blockstate properties, and non-object JSON resources.
- Keep saved-data loading tolerant of malformed entries. `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java` skips empty keys and invalid `ResourceLocation` values while loading `pending` and `placed` lists.
- Avoid returning `null` from domain helpers. Use `Optional.empty()`, `RoostType.UNKNOWN`, empty immutable collections, or a result enum.

## Logging

**Framework:** SLF4J through `com.mojang.logging.LogUtils`

**Patterns:**
- Use the shared logger `RoostTraces.LOGGER` from `src/main/java/com/typ/roosttraces/RoostTraces.java`.
- Use `warn` for recoverable external failures, dropped pending placements, invalid datapack selectors, and Trace index registration failures.
- Use `info` for successful load or placement summaries only when useful. Several placement/capture logs are gated by `RoostTracesConfig.DEBUG` in `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java` and `src/main/java/com/typ/roosttraces/roost/RoostPendingRegistry.java`.
- Keep high-frequency tick and worldgen logging behind `RoostTracesConfig.DEBUG` to avoid server log spam.

## Comments

**When to Comment:**
- Prefer self-explanatory method and type names over comments in Java source.
- Add comments only for non-obvious external behavior or build lifecycle details. Most comments are in `build.gradle`, where they explain NeoForge run configuration, generated resources, metadata expansion, and Gradle wrapper behavior.
- Use English comments for source and build files.

**JSDoc/TSDoc:**
- Not applicable. This repository is Java/Groovy/JSON/TOML, not TypeScript.
- JavaDoc is not used in the source files under `src/main/java/com/typ/roosttraces/`. Keep new comments short unless documenting public API consumed by datapacks or other mods.

## Function Design

**Size:** Use small, single-purpose helpers around lifecycle and domain boundaries.
- Good examples: `RoostKeys.key`, `RoostKeys.placementSeed`, `RoostCandidateScanner.isChunkLoaded`, `RoostCandidateScanner.score`, and `RoostTraceNodePoolResolver.setProperty`.
- Larger files such as `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` should keep parsing, selector expansion, listener registration, and property conversion in separate private methods.

**Parameters:** Pass explicit Minecraft context and domain records.
- Worldgen code passes `WorldGenLevel`, `BlockPos`, `ChunkPos`, and `RoostType` explicitly in `RoostTraceNodePlacer.placeDuringWorldgen`.
- Deferred placement passes a `ServerLevel` plus `PendingRoost` in `RoostTraceNodePlacer.place`.
- Saved state APIs accept stable keys and immutable domain records in `RoostTraceSavedData`.

**Return Values:** Prefer explicit outcomes and immutable snapshots.
- Use `PlacementResult` for placement execution.
- Use `Optional<RoostCandidate>` and `Optional<NodePlacement>` for discoverable absence.
- Return immutable collections with `List.copyOf` and `Set.copyOf`, as in `RoostCandidateScanner.buildOffsets`, `RoostTraceNodePoolResolver.resolve`, `RoostTraceNodePoolResolver.Listener.list`, and `RoostTracesConfig.parseIds`.

## Module Design

**Exports:** Public static entry points are grouped by package responsibility.
- Mod bootstrap: `src/main/java/com/typ/roosttraces/RoostTraces.java` and `src/main/java/com/typ/roosttraces/RoostTracesClient.java`.
- Configuration: `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`.
- External compatibility facade: `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`.
- Placement orchestration: `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`, `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`, and `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`.
- Datapack node pools and tags: `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`, `src/main/java/com/typ/roosttraces/pool/RoostTraceTags.java`, and `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`.
- Roost persistence and domain data: `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`, `src/main/java/com/typ/roosttraces/roost/PendingRoost.java`, and `src/main/java/com/typ/roosttraces/roost/PlacedRoost.java`.

**Barrel Files:** Not used.
- Java packages do not expose package-level barrel classes.
- Keep new helpers in the package that owns the behavior instead of adding broad manager or facade classes.
- Use `final` utility classes with private constructors for stateless helpers: `RoostKeys`, `RoostPendingRegistry`, `RoostCandidateScanner`, `RoostTraceNodePlacer`, `RoostTraceTags`, and `TraceCompat`.
- Use nested private records/classes only when the type is local to one implementation, as with `RoostCandidateScanner.Offset`, `RoostTraceNodePlacer.NodePlacement`, `RoostTraceNodePoolResolver.SelectorParts`, and `RoostTraceNodePoolResolver.Listener`.

---

*Convention analysis: 2026-07-02*
