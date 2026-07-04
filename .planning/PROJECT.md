# Roost Traces

## What This Is

Roost Traces is a NeoForge 1.21.1 compatibility mod that augments Ice and Fire CE dragon roost generation by placing Create ReAutomated node-only Trace nodes inside generated roosts. It is for modpack authors and players using Ice and Fire CE, Create ReAutomated, and Create ReAutomated: Traces who want roosts to become predictable, configurable sources of traceable nodes.

The current milestone focuses on making the pack-facing configuration surface more robust. Datapack pool errors, tag mistakes, release-version confusion, and destructive replaceable-block settings should be easier to diagnose and safer to recover from without changing the intended roost gameplay.

## Core Value

Pack authors can customize roost trace behavior confidently without malformed datapack/config input breaking reloads, hiding important errors, or changing roost gameplay unpredictably.

## Requirements

### Validated

- [x] Dragon roost generation can place a node-only trace node through the Ice and Fire CE roost structure hook.
- [x] Node choices are data-driven through `roost_trace_node_pools` JSON and block tags.
- [x] Create ReAutomated: Traces API calls are isolated behind `TraceCompat`.
- [x] Roost placement and index state are persisted through `RoostTraceSavedData`.
- [x] Failed Trace index registration can be deferred through `index_pending` retry state.
- [x] Placement scanning is bounded and avoids loading chunks unnecessarily.
- [x] Common placement and node-pool behavior is configurable through NeoForge config.

### Active

- [ ] Make datapack node-pool parsing tolerant of malformed fields, files, and selectors.
- [ ] Preserve or restore a valid effective node-pool configuration when reload input is invalid where the lifecycle allows it.
- [ ] Log pack-author-facing warnings for clearly invalid configuration without spamming normal worlds.
- [ ] Keep detailed diagnostics debug-gated behind the existing debug configuration.
- [ ] Clarify destructive replaceable-block allowlists and pool fallback behavior for pack authors.
- [ ] Align release-facing version and dependency documentation with the actual Gradle and NeoForge metadata contract.
- [ ] Add essential tests for isolated parsing, fallback, or domain helpers where this can be done without mocking Minecraft runtime behavior.

### Out of Scope

- Changing roost reward balance, node rarity, scan radius defaults, or player-facing progression - this milestone is robustness-focused, not a gameplay rebalance.
- Adding broad post-generation scans or forced chunk loading - the mod should keep using bounded worldgen placement and lightweight retry paths.
- Rewriting the whole placement system or replacing the Ice and Fire CE mixin hook - only targeted pack-facing robustness work is in scope.
- Adding a repair/reconciliation command for orphaned nodes - useful future work, but larger than the first pack-robustness milestone.
- Removing legacy pending-placement compatibility state - saved-data compatibility needs a dedicated migration plan and tests.
- Adding verbose always-on placement diagnostics - detailed scanner/retry output should stay behind debug unless an error is terminal or clearly misconfigured.

## Context

This is an existing Java 21 NeoForge mod for Minecraft 1.21.1. The codebase is organized around a small set of explicit layers:

- `mixin/` captures Ice and Fire CE dragon roost generation.
- `placement/` owns candidate scanning, node placement, rollback, and retry scheduling.
- `pool/` owns datapack node-pool parsing and selector resolution.
- `roost/` owns roost identity, domain records, and SavedData persistence.
- `compat/` wraps Create ReAutomated: Traces API calls.

The current codebase map identifies several pack-facing risks:

- Malformed `roost_trace_node_pools` JSON can break reload too broadly instead of being rejected per file or field.
- Effective pool sizes, empty tags, fallback choices, and selector problems are not reported clearly enough for modpack debugging.
- Replaceable block tags/config are destructive allowlists and need careful validation and documentation.
- Release and dependency documentation has drifted from the actual `gradle.properties` and `neoforge.mods.toml` contract.
- There is no committed automated test suite, so low-level robust parsing or persistence changes should add focused tests where practical.

The desired first milestone is a balanced pack-author robustness pass. The behavior policy is tolerant: invalid datapack/config input should produce clear warnings, discard only the invalid resource or selector where possible, and preserve the last valid effective configuration when a reload failure would otherwise leave the mod in a worse state. Detailed diagnostics should be debug-gated; normal logs should avoid spam and reserve warnings for actionable pack issues.

## Constraints

- **Tech stack**: Java 21, Minecraft 1.21.1, NeoForge 21.1.233, Gradle, Sponge Mixin - stay within the existing mod architecture and lifecycle.
- **Compatibility**: Preserve existing saved-data keys, config keys, datapack formats, dependency ranges, and user-visible gameplay unless an explicit migration or release note is added.
- **Performance**: Do not add unbounded scans, forced chunk loads, heavy server-tick work, or noisy worldgen logging.
- **Lifecycle**: Keep worldgen block edits separate from server-thread SavedData and Trace index writes.
- **Integration boundary**: Keep Create ReAutomated: Traces calls in `TraceCompat`; keep Ice and Fire CE internal assumptions localized to the mixin/type resolver path.
- **Pack author control**: Keep node selection and replaceable materials data-driven through datapacks/tags/config rather than hardcoded Java behavior.
- **Diagnostics**: Use warnings for actionable invalid pack input; keep detailed reports behind the existing debug setting.
- **Verification**: Use `.\gradlew.bat compileJava` as the minimum code check and `.\gradlew.bat build` for resource, metadata, or packaging changes.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| First milestone targets pack robustness. | The user selected a balanced pack-facing pass over pure gameplay reliability or test-only work. | Pending |
| Use tolerant datapack/config error handling. | A malformed pack resource should not break all reload behavior when a narrower recovery is possible. | Pending |
| Keep diagnostics debug-gated by default. | Pack authors need detail, but normal worldgen and retry logs should remain quiet unless there is an actionable warning. | Pending |
| Include essential docs and tests. | Pack-facing behavior must be discoverable, and isolated helper changes should have low-cost verification. | Pending |
| Preserve existing gameplay behavior. | Robustness work should not rebalance roost rewards, node rarity, or placement feel without a separate gameplay decision. | Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `$gsd-transition`):
1. Requirements invalidated? Move to Out of Scope with reason.
2. Requirements validated? Move to Validated with phase reference.
3. New requirements emerged? Add to Active.
4. Decisions to log? Add to Key Decisions.
5. "What This Is" still accurate? Update if drifted.

**After each milestone** (via `$gsd-complete-milestone`):
1. Full review of all sections.
2. Core Value check: still the right priority?
3. Audit Out of Scope: reasons still valid?
4. Update Context with current state.

---
*Last updated: 2026-07-03 after initialization*
