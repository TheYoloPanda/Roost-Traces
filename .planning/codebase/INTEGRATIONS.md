# External Integrations

**Analysis Date:** 2026-07-02

## APIs & External Services

**Minecraft / NeoForge Runtime:**
- Minecraft 1.21.1 / NeoForge 21.1.233 - Core mod runtime and API surface.
  - SDK/Client: NeoForge ModDevGradle and Minecraft/NeoForge classes configured in `build.gradle` and `gradle.properties`.
  - Auth: None.
  - Implementation: Main common entry point in `src/main/java/com/typ/roosttraces/RoostTraces.java`; client-only config screen registration in `src/main/java/com/typ/roosttraces/RoostTracesClient.java`.

**Ice and Fire CE:**
- Ice and Fire CE 2.0-beta.17 - Source of dragon roost generation that Roost Traces augments.
  - SDK/Client: Runtime mod dependency `maven.modrinth:iceandfire-ce` in `build.gradle`.
  - Auth: None.
  - Implementation: `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java` targets `com.iafenvoy.iceandfire.world.structure.DragonRoostStructure$DragonRoostPiece` and injects after `postProcess`.

**Create ReAutomated:**
- Create ReAutomated 0.2.0 - Provides compatible node blocks placed inside roosts.
  - SDK/Client: Runtime mod dependency `maven.modrinth:create-reautomated` in `build.gradle`.
  - Auth: None.
  - Implementation: Node block selectors and tags are configured in `src/main/resources/data/roosttraces/tags/block/create_reautomated_nodes.json`, `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`, and resolved by `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`.

**Create ReAutomated: Traces:**
- Create ReAutomated: Traces 0.2.1 - Registers roost nodes with Trace Finder/index behavior and suppresses generic generated traces for roost nodes.
  - SDK/Client: `compileOnly` plus `localRuntime` dependency in `build.gradle`; uses `maven.modrinth:create-reautomated-traces` or a local jar from `../CreateReAutomatedTraces/build/libs`.
  - Auth: None.
  - Implementation: API calls to `com.typ.traces.api.TraceApi` and `com.typ.traces.api.TraceWorldgenExclusions` are wrapped in `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`; placement code calls that wrapper from `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`.

**Mixin Runtime:**
- SpongePowered Mixin - Applies the Ice and Fire CE roost hook.
  - SDK/Client: Mixin config declared in `src/main/resources/roosttraces.mixins.json` and referenced from `src/main/templates/META-INF/neoforge.mods.toml`.
  - Auth: None.

**Build Artifact Repositories:**
- Maven repositories - External artifact sources for Gradle dependency resolution.
  - SDK/Client: Gradle repository declarations in `build.gradle`.
  - Auth: None.
  - Endpoints: `https://api.modrinth.com/maven`, `https://maven.createmod.net`, `https://maven.ithundxr.dev/snapshots`, `https://maven.blamejared.com/`, `https://jitpack.io`, `https://maven.nucleoid.xyz/`, `https://modmaven.dev`, and `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/`.

## Data Storage

**Databases:**
- Minecraft SavedData, not an external database.
  - Connection: Local world save managed by Minecraft; no connection string or database server.
  - Client: `net.minecraft.world.level.saveddata.SavedData` implemented by `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`.
  - Data name: `roosttraces_roost_data` in `src/main/java/com/typ/roosttraces/roost/RoostTraceSavedData.java`.

**File Storage:**
- Local repository resources only.
  - Datapack defaults: `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json` and block tags under `src/main/resources/data/roosttraces/tags/block`.
  - Client language resources: `src/main/resources/assets/roosttraces/lang/en_us.json`.
  - Generated metadata/resources: `build/generated/sources/modMetadata` and `src/generated/resources` are configured by `build.gradle`; generated output is excluded/ignored where appropriate.
  - Local publishing output: `repo/` is configured by `build.gradle` and ignored by `.gitignore`.

**Caching:**
- No external cache service.
- In-memory caches are local JVM state: `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` stores the current node selector config, and `src/main/java/com/typ/roosttraces/placement/RoostCandidateScanner.java` caches scan offsets by radius.

## Authentication & Identity

**Auth Provider:**
- Not applicable.
  - Implementation: Not detected in `src/main/java`, `src/main/resources`, `build.gradle`, or `.github/workflows/build.yml`.

## Monitoring & Observability

**Error Tracking:**
- None.
  - Implementation: Not detected in `build.gradle` or `src/main/java`.

**Logs:**
- SLF4J through Mojang LogUtils and NeoForge logging.
  - Implementation: Logger defined in `src/main/java/com/typ/roosttraces/RoostTraces.java`.
  - Usage: Warnings and debug logging appear in `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`, `src/main/java/com/typ/roosttraces/placement/RoostTraceNodePlacer.java`, `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`, and `src/main/java/com/typ/roosttraces/roost/RoostPendingRegistry.java`.

## CI/CD & Deployment

**Hosting:**
- Not detected for application/runtime hosting.
- Artifact target is a local NeoForge mod JAR produced by Gradle from `build.gradle`.

**CI Pipeline:**
- GitHub Actions.
  - Workflow: `.github/workflows/build.yml`.
  - Triggers: push and pull request.
  - Steps: `actions/checkout@v4`, `actions/setup-java@v4` with Temurin JDK 21, `gradle/actions/setup-gradle@v4`, `chmod +x ./gradlew`, and `./gradlew build`.
  - Deployment: Not detected in `.github/workflows/build.yml`.

## Environment Configuration

**Required env vars:**
- Not detected.
  - Gradle and mod versions are configured through `gradle.properties`.
  - Runtime mod config is declared in `src/main/java/com/typ/roosttraces/RoostTracesConfig.java`.
  - No `.env` files are present at repository root.

**Secrets location:**
- Not applicable.
  - `.github/workflows/build.yml` does not reference GitHub secrets.
  - Repository-root `.env` files are not present.

## Webhooks & Callbacks

**Incoming:**
- None.
  - Implementation: Not detected in `src/main/java`.
  - Minecraft/NeoForge callbacks are local runtime events and mixin injections: `src/main/java/com/typ/roosttraces/RoostTraces.java`, `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`, `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`, and `src/main/java/com/typ/roosttraces/placement/RoostTracePlacementScheduler.java`.

**Outgoing:**
- None at application runtime.
  - Implementation: Not detected in `src/main/java`.
  - Network access is limited to build-time Gradle dependency downloads from Maven repositories declared in `build.gradle`.
  - `DESCRIPTION.md` contains a BisectHosting image/link for project presentation, but runtime code performs no outbound request.

---

*Integration audit: 2026-07-02*
