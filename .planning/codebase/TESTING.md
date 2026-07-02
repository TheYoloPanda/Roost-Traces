# Testing Patterns

**Analysis Date:** 2026-07-02

## Test Framework

**Runner:**
- JUnit or another JVM unit-test framework is not detected in `build.gradle`.
- The Java plugin provides the standard Gradle `test` lifecycle task, but there are no repository test sources under `src/test/`.
- NeoForge GameTest support is configured through the `gameTestServer` run config in `build.gradle`.
- Config: `build.gradle`

**Assertion Library:**
- Not detected. There are no `testImplementation` dependencies, `@Test` methods, `@GameTest` methods, assertion imports, or test source files.

**Run Commands:**
```bash
.\gradlew.bat compileJava        # Compile Java sources and catch API/import/generic errors
.\gradlew.bat build              # Build the mod jar and process generated metadata/resources
.\gradlew.bat test               # Standard Gradle test task; no repository tests are present
.\gradlew.bat runGameTestServer  # Expected ModDevGradle task for the gameTestServer run config when GameTests are added
```

## Test File Organization

**Location:**
- No test source directory is present. `src/` contains only `src/main/`.
- No co-located test files are present under `src/main/java/com/typ/roosttraces/`.
- No generated test resources are present under `src/generated/`.

**Naming:**
- Not detected. There are no files matching `*Test.java`, `*Tests.java`, `*.test.*`, or `*.spec.*`.
- Recommended naming for new JVM unit tests: mirror the production class name with `Test`, such as `src/test/java/com/typ/roosttraces/roost/RoostKeysTest.java`.
- Recommended naming for future GameTests: use descriptive classes under a test namespace, such as `src/test/java/com/typ/roosttraces/gametest/RoostTracePlacementGameTests.java`.

**Structure:**
```text
src/
└── main/
    ├── java/com/typ/roosttraces/        # Production Java sources
    ├── resources/                       # Runtime assets, data, mixin config
    └── templates/META-INF/              # Expanded mod metadata template
```

## Test Structure

**Suite Organization:**
```java
// No existing suite pattern is present.
// Use direct, behavior-focused tests for pure helpers first.
class RoostKeysTest {
    @Test
    void normalizesLegacyCoordinateKey() {
        // Arrange stable key inputs.
        // Act through RoostKeys.normalizeSavedKey(...).
        // Assert the exact saved-data key string.
    }
}
```

**Patterns:**
- Use pure unit tests for deterministic helpers that do not require a Minecraft runtime: `RoostKeys.normalizeSavedKey`, `RoostKeys.placementSeed`, `RoostType.fromId`, and selector string parsing behavior in `RoostTraceNodePoolResolver`.
- Use NeoForge GameTests or controlled integration runs for block placement, saved data attached to `ServerLevel`, reload listeners, tag expansion, mixin-triggered worldgen placement, and Trace API registration.
- Treat `.\gradlew.bat compileJava` as the minimum verification gate because the repository has no executable test suite.
- Use `.\gradlew.bat build` when resource processing, metadata expansion, mixin configuration, or TOML template changes are involved.

## Mocking

**Framework:** Not detected

**Patterns:**
```java
// No Mockito, fake server, or custom test double pattern exists.
// Prefer pure tests for non-Minecraft helpers and GameTests for Minecraft runtime behavior.
```

**What to Mock:**
- Avoid mocking broad Minecraft world objects by default. `ServerLevel`, `WorldGenLevel`, `LevelAccessor`, registries, tags, block states, and saved data behavior are coupled to Minecraft runtime semantics.
- If unit tests are introduced, mock only narrow, stable collaborators at repository boundaries, such as a tiny wrapper around `TraceCompat`, after the wrapper exists in production code.

**What NOT to Mock:**
- Do not mock NeoForge lifecycle events when the behavior depends on real event registration. Use the run configs in `build.gradle` for client, server, data, and GameTest execution.
- Do not mock `BuiltInRegistries`, block tags, or `BlockState` property definitions for selector resolution unless the test is explicitly limited to string validation.
- Do not mock mixin injection into Ice and Fire classes. Validate `DragonRoostPieceMixin` behavior through integration/GameTest coverage or compile checks with the dependency present.

## Fixtures and Factories

**Test Data:**
```java
// No fixture/factory pattern exists.
// Existing production records are the natural shape for future fixture data:
PendingRoost pending = new PendingRoost(
        key,
        RoostType.FIRE,
        pivot.asLong(),
        chunkPos.toLong(),
        seed,
        0,
        0L);
```

**Location:**
- Static datapack/resource fixtures already live in `src/main/resources/data/roosttraces/`.
- Node-pool fixture shape: `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`.
- Block tag fixture shape: `src/main/resources/data/roosttraces/tags/block/create_reautomated_nodes.json`, `src/main/resources/data/roosttraces/tags/block/fire_roost_nodes.json`, `src/main/resources/data/roosttraces/tags/block/ice_roost_nodes.json`, and `src/main/resources/data/roosttraces/tags/block/lightning_roost_nodes.json`.
- New test-only datapack fixtures should go under `src/test/resources/` when a test source set is added, not under `src/main/resources/`.

## Coverage

**Requirements:** None enforced

**View Coverage:**
```bash
# No JaCoCo or coverage task is configured in build.gradle.
```

## Test Types

**Unit Tests:**
- Not implemented.
- Best fit: pure Java helpers in `src/main/java/com/typ/roosttraces/roost/RoostKeys.java`, `src/main/java/com/typ/roosttraces/roost/RoostType.java`, `src/main/java/com/typ/roosttraces/roost/RoostTypeResolver.java`, and parse helpers in `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`.
- Add a test dependency in `build.gradle` before adding files under `src/test/java/`.

**Integration Tests:**
- Not implemented.
- Best fit: `RoostTraceSavedData` persistence in `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`, placement rollback in `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`, candidate scanning in `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java`, and reload listener behavior in `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`.

**E2E Tests:**
- NeoForge GameTest support is configured but no GameTests are present.
- The `client`, `server`, `gameTestServer`, and `data` run configs in `build.gradle` are the available runtime verification surfaces.
- Manual gameplay verification still matters for mixin-triggered roost generation because `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java` depends on Ice and Fire CE structure generation.

## Common Patterns

**Async Testing:**
```java
// Production async/server-thread handoff pattern:
level.getServer().execute(() -> recordPlaced(level, placed));
```

- Future tests around worldgen handoff should verify the queued server-thread side effect rather than asserting immediate saved-data mutation.
- Relevant files: `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java` and `src/main/java/com/typ/roosttraces/roost/RoostPendingRegistry.java`.

**Error Testing:**
```java
// Production expected-failure pattern:
Optional<RoostCandidate> candidate = RoostCandidateScanner.find(level, pivot);
if (candidate.isEmpty()) return PlacementResult.NO_CANDIDATE;
```

- Future tests should assert result values or empty optionals for expected failures, not thrown exceptions.
- Cover invalid datapack selectors through `RoostTraceNodePoolResolver` behavior and expected logged-skip outcomes.
- Cover saved-data malformed-entry handling through `RoostTraceSavedData.load` behavior once package-private or fixture-access testing is added.

---

*Testing analysis: 2026-07-02*
