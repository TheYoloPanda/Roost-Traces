# Codebase Concerns

**Analysis Date:** 2026-07-02

## Tech Debt

**Legacy pending-placement path remains active in the runtime:**
- Issue: A server-tick migration path still processes `PendingRoost` entries even though new roost placement happens during structure generation.
- Files: `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`, `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`, `src/main/java/com/typ/roosttraces/roost/PendingRoost.java`
- Impact: Runtime behavior has two placement paths with different rollback and registration semantics, which makes future changes easy to apply to one path and miss in the other.
- Fix approach: Keep legacy migration isolated behind a clearly named migration service, add tests for pending-entry replay, and remove it only with an explicit save-compatibility decision.

**Unused and compatibility-only API surface is mixed into live config/code:**
- Issue: `existingPairRadius` is defined but unused, `RoostPendingRegistry.capture` is not referenced by current code, and `RoostCandidate.tracePos` plus `score` are stored but not consumed.
- Files: `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`, `src/main/java/com/typ/roosttraces/roost/RoostPendingRegistry.java`, `src/main/java/com/typ/roosttraces/placement/RoostCandidate.java`, `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`
- Impact: New contributors can mistake compatibility leftovers for active gameplay controls, and config users can tune values that do not affect new roost placement.
- Fix approach: Mark compatibility-only config entries in code, move unused migration helpers next to the migration path, and remove unused record fields or make the scanner actually use scoring.

**Reflection is used for Create ReAutomated node natural-state adaptation:**
- Issue: `TraceCompat.naturalNodeState` reflectively calls `unstable()` on node block classes and falls back to `defaultBlockState()`.
- Files: `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`
- Impact: Node state behavior depends on method naming in another mod, and failures degrade silently to a default state that may not match intended ore-node stability.
- Fix approach: Prefer a public Create ReAutomated or Create ReAutomated: Traces API for natural node states; keep reflection only as a compatibility fallback with explicit logging around fallback use.

## Known Bugs

**Clean dependency resolution fails for Create ReAutomated: Traces 0.2.1:**
- Symptoms: `.\gradlew.bat compileJava` fails before source compilation because `maven.modrinth:create-reautomated-traces:0.2.1` is not found in the configured repositories when the sibling local jar path is unavailable.
- Files: `build.gradle`, `gradle.properties`
- Trigger: Run `.\gradlew.bat compileJava` in a checkout that does not have `../CreateReAutomatedTraces/build/libs/createreautomatedtraces-0.2.1.jar`.
- Workaround: Build or provide the sibling `CreateReAutomatedTraces` jar, or replace the fallback dependency coordinate/repository with a resolvable artifact source.

**Worldgen placement can leave a node block without a Trace index record:**
- Symptoms: During worldgen, the node block is placed immediately, then index/SavedData registration is scheduled with `server.execute`; if `TraceCompat.recordNode` fails, the code logs and returns without rolling back the already placed node or creating a retry.
- Files: `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`, `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`, `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`
- Trigger: Trace index registration throws, returns false, or the API rejects the external node after `placeDuringWorldgen` has already modified the world.
- Workaround: Re-run placement through a manual repair path or regenerate the affected chunk; the codebase has no automated reconciliation path.

**Legacy migration completion is stored in JVM-global static state:**
- Symptoms: `legacyComplete` stops all future scheduler work once no pending entries are seen, and the flag is not scoped per server instance, world, dimension, or data version.
- Files: `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`, `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`
- Trigger: A server process marks migration complete, then pending entries appear later in the same JVM due to another world load, config changes, or delayed data availability.
- Workaround: Restart the JVM before loading a world that still contains pending entries.

## Security Considerations

**Datapack pool JSON is trusted during reload:**
- Risk: Malformed datapack JSON can drive unchecked `getAsBoolean()` and primitive conversion paths during resource reload.
- Files: `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`, `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`
- Current mitigation: The reload listener rejects non-object roots and ignores non-array selector lists.
- Recommendations: Type-check every parsed JSON value, catch per-file parse errors, log the resource id that failed, and keep the last valid pool config when one datapack file is invalid.

**Replacement rules can be widened by config/datapack input:**
- Risk: `roost_replaceable` tags and `replaceable.ids` directly decide which node-position blocks may be replaced; overly broad pack/config entries can make placement destructive inside structures.
- Files: `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`, `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`, `src/main/resources/data/roosttraces/tags/block/roost_replaceable.json`
- Current mitigation: The scanner rejects block entities at the node and clearance positions, protects several high-risk vanilla blocks in clearance checks, and ships a narrow default replaceable list.
- Recommendations: Document replacement tags as destructive permissions, keep defaults limited to Ice and Fire roost floor materials, and consider an allowlist-only validator for config ids.

## Performance Bottlenecks

**Candidate scanning cost scales sharply with radius and candidate limit:**
- Problem: A radius of 64 creates roughly 270k offset candidates before sorting/caching, and the configured max check limit allows up to 65,536 position checks per roost.
- Files: `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`, `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`
- Cause: `buildOffsets` eagerly builds and sorts all offsets for each configured radius, and each checked candidate may call chunk, block entity, block state, and clearance lookups.
- Improvement path: Keep defaults conservative, cap practical scan budgets, add metrics for skipped/placed roosts, and consider a roost-material-first scan when placement misses become common.

**SavedData serialization grows with every tracked roost:**
- Problem: `RoostTraceSavedData` stores all pending and placed roosts in in-memory maps and writes both collections into one saved-data blob.
- Files: `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`, `src/main/java/com/typ/roosttraces/roost/PlacedRoost.java`, `src/main/java/com/typ/roosttraces/roost/PendingRoost.java`
- Cause: There is no pruning, chunk partitioning, data versioning, or compaction for placed records.
- Improvement path: Add a schema version, track only fields needed for duplicate prevention and repair, and consider per-dimension or chunk-keyed storage if large-world saves become slow.

## Fragile Areas

**Ice and Fire structure hook depends on an internal mixin target and signature:**
- Files: `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`, `src/main/resources/roosttraces.mixins.json`, `src/main/templates/META-INF/neoforge.mods.toml`
- Why fragile: The mixin targets `com.iafenvoy.iceandfire.world.structure.DragonRoostStructure$DragonRoostPiece` and injects into `postProcess` with `remap = false` and `defaultRequire = 1`.
- Safe modification: Verify the target class and method against the Ice and Fire CE source for every dependency bump, and keep the injection narrowly after successful roost generation.
- Test coverage: No automated mixin-load or generated-roost GameTest coverage exists in `src`.

**Roost type detection depends on class-name substrings:**
- Files: `src/main/java/com/typ/roosttraces/roost/RoostTypeResolver.java`, `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`
- Why fragile: Fire, ice, and lightning roosts are identified by lowercasing implementation class names and checking for `firedragonroost`, `icedragonroost`, or `lightningdragonroost`.
- Safe modification: Prefer explicit type information from Ice and Fire CE when available; otherwise cover resolver behavior with tests using representative piece class names.
- Test coverage: No resolver tests or compatibility fixtures exist in `src`.

**Create ReAutomated: Traces integration is on the critical path for gameplay:**
- Files: `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`, `src/main/templates/META-INF/neoforge.mods.toml`
- Why fragile: Node placement, trace suppression, and Trace Finder visibility depend on external APIs from Create ReAutomated: Traces.
- Safe modification: Wrap each external API call in a narrow compatibility boundary, keep version requirements exact, and add verification worlds that confirm Trace Finder discovery after roost generation.
- Test coverage: No integration test verifies suppression, node-only index registration, or Trace Finder visibility.

## Scaling Limits

**Roost scan capacity is bounded by config rather than world context:**
- Current capacity: Defaults are `scanRadius=24` and `maxCandidateChecksPerRoost=4096`.
- Limit: Maximum config values allow `scanRadius=64` and `maxCandidateChecksPerRoost=65536`, which can become expensive during structure generation or legacy tick replay.
- Scaling path: Keep a hard internal budget independent of config, add debug counters for placement miss reasons, and avoid increasing radius without measuring worldgen time.

**Legacy pending replay is intentionally slow:**
- Current capacity: Default `maxPendingRoostsPerTick=1`, max 16, with up to 8 attempts and exponential retry delays.
- Limit: Worlds with many pending entries can take many server ticks or minutes to drain, especially if chunks are not loaded.
- Scaling path: Expose progress diagnostics, process only loaded chunks, and keep retry state in `RoostTraceSavedData` instead of global scheduler state.

**Placed-roost records are unbounded:**
- Current capacity: All placed roosts remain in `RoostTraceSavedData.placed`.
- Limit: Very large worlds can accumulate a large saved-data file and larger save-time serialization work.
- Scaling path: Store compact duplicate-prevention keys, version the schema, and add a migration path before changing persisted fields.

## Dependencies at Risk

**Create ReAutomated: Traces artifact and API:**
- Risk: The source imports `com.typ.traces.api.TraceApi` and `TraceWorldgenExclusions`, while Gradle fallback dependency resolution for `create-reautomated-traces:0.2.1` currently fails without a sibling jar.
- Impact: Clean builds and CI builds can fail before source compilation; gameplay breaks if the API changes semantics for node-only records or suppression.
- Migration plan: Fix the published dependency coordinate/repository, keep the sibling jar fallback only as a local-development convenience, and pin compatibility tests to the declared `[0.2.1,0.3.0)` range.

**Ice and Fire CE internal worldgen implementation:**
- Risk: The mixin targets Ice and Fire CE internals rather than a stable public event or API.
- Impact: Ice and Fire CE class renames, method signature changes, or roost-generation refactors can prevent the mod from loading or silently stop placement.
- Migration plan: Check `C:\Users\TYP\Documents\repos` or the official Ice and Fire CE source before each dependency bump, then update the mixin target with a compile/run verification pass.

**Gradle configuration cache and future Gradle compatibility:**
- Risk: `gradle.properties` enables `org.gradle.configuration-cache=true`; the current failed compile also reports deprecated Gradle features that are incompatible with Gradle 10.
- Impact: Build failures can present as configuration-cache serialization problems, and future Gradle updates may break the build script or ModDevGradle usage.
- Migration plan: Keep Gradle wrapper updates explicit, run builds with `--warning-mode all` during dependency upgrades, and disable configuration cache temporarily when diagnosing dependency resolution.

## Missing Critical Features

**No automated test suite:**
- Problem: The repo defines a GameTest run configuration but has no Java tests, GameTests, or fixture-based parser tests under `src`.
- Blocks: Safe refactors of placement, SavedData migration, mixin compatibility, and datapack pool parsing.

**No startup/reload validation that node pools resolve to usable blocks:**
- Problem: Built-in fire, ice, and lightning node tags are empty, default selection depends on `#createreautomated:ore_nodes`, and missing/empty tags produce empty pools without a hard failure.
- Blocks: Pack authors can ship a configuration that makes roost placement no-op without an obvious validation report.

**No repair/reconciliation command for placed nodes missing Trace index records:**
- Problem: If the worldgen registration task fails after node placement, the code has no command, scan, or migration to find roost nodes and register them later.
- Blocks: Players can end up with visible/usable node blocks that the Trace Finder and saved roost marker do not know about.

## Test Coverage Gaps

**Worldgen and mixin behavior:**
- What's not tested: Ice and Fire CE roost generation hook, pivot/chunk-box filtering, block placement during worldgen, trace suppression, and deferred index registration.
- Files: `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`, `src/main/resources/roosttraces.mixins.json`
- Risk: Dependency updates can break placement or mod loading without a targeted failure.
- Priority: High

**SavedData schema and legacy pending replay:**
- What's not tested: `pending` and `placed` serialization, key normalization, retry attempts, terminal result handling, and global completion behavior.
- Files: `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`, `src/main/java/com/typ/roosttraces/roost/RoostKeys.java`, `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`
- Risk: Save compatibility regressions can drop pending entries, duplicate placements, or leave migration stuck.
- Priority: High

**Datapack selector parsing:**
- What's not tested: Direct block selectors, blockstate property parsing, tag expansion, invalid JSON handling, missing tags, fallback-to-default behavior, and type-specific pool inheritance.
- Files: `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`, `src/main/java/com/typ/roosttraces/pool/NodeSelectorConfig.java`, `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`
- Risk: Pack configuration mistakes can silently change roost rewards or stop placement.
- Priority: Medium

**Build dependency availability:**
- What's not tested: Clean-checkout dependency resolution for the published Create ReAutomated: Traces artifact.
- Files: `build.gradle`, `gradle.properties`, `.github/workflows/build.yml`
- Risk: CI and contributors without the sibling jar cannot compile.
- Priority: High

---

*Concerns audit: 2026-07-02*
