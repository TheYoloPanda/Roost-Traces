# Technology Stack

**Analysis Date:** 2026-07-02

## Languages

**Primary:**
- Java 21 - Mod source under `src/main/java/com/typ/roosttraces`; Gradle targets Java 21 via `java.toolchain.languageVersion = JavaLanguageVersion.of(21)` in `build.gradle`.

**Secondary:**
- Groovy Gradle DSL - Build configuration in `build.gradle`, plugin management in `settings.gradle`, and version properties in `gradle.properties`.
- JSON - Minecraft resource/data files in `src/main/resources`, including `src/main/resources/roosttraces.mixins.json`, `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`, block tags under `src/main/resources/data/roosttraces/tags/block`, and translations in `src/main/resources/assets/roosttraces/lang/en_us.json`.
- TOML template - NeoForge mod metadata template in `src/main/templates/META-INF/neoforge.mods.toml`; generated metadata is produced by the `generateModMetadata` Gradle task in `build.gradle`.

## Runtime

**Environment:**
- Minecraft 1.21.1 on NeoForge 21.1.233 using JavaFML - Version values live in `gradle.properties`; required loader and dependency ranges are declared in `src/main/templates/META-INF/neoforge.mods.toml`.
- Java 21 runtime - Required by Minecraft 1.21.1 and enforced by `build.gradle`.

**Package Manager:**
- Gradle Wrapper 9.2.1 - `gradle/wrapper/gradle-wrapper.properties` points at `https://services.gradle.org/distributions/gradle-9.2.1-bin.zip`.
- Lockfile: missing - No Gradle dependency lockfile is present; dependency versions are pinned through `gradle.properties` and `build.gradle`.

## Frameworks

**Core:**
- NeoForge ModDevGradle 2.0.141 - Configures NeoForge development, runs, metadata generation, and mod source binding in `build.gradle`.
- NeoForge 21.1.233 - Main mod registration uses `@Mod` in `src/main/java/com/typ/roosttraces/RoostTraces.java`; client-only config UI registration lives in `src/main/java/com/typ/roosttraces/RoostTracesClient.java`.
- SpongePowered Mixin 0.8+ compatibility - `src/main/resources/roosttraces.mixins.json` declares `JAVA_21` compatibility and loads `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`.
- Minecraft resource/datapack system - Node pool JSON is loaded from `roost_trace_node_pools` by `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`; built-in pool data is in `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`.

**Testing:**
- NeoForge GameTest run configuration - `build.gradle` defines `gameTestServer` and enables the `roosttraces` namespace for run configs; no `src/test`, `*Test.java`, or registered GameTest sources are present.
- Gradle `build` - CI and local verification run the standard Gradle build task from `.github/workflows/build.yml`.

**Build/Dev:**
- Gradle Java Library plugin - Declared in `build.gradle` for Java compilation and source packaging.
- Maven Publish plugin - Declared in `build.gradle`; publishes `mavenJava` to a local file repository at `repo/`, which is ignored by `.gitignore`.
- IntelliJ IDEA plugin - Declared in `build.gradle`; downloads dependency sources and Javadocs for IDE use.
- Foojay toolchain resolver convention 1.0.0 - Declared in `settings.gradle` for Java toolchain resolution.
- GitHub Actions - `.github/workflows/build.yml` runs checkout, Temurin JDK 21 setup, Gradle setup, and `./gradlew build` on push and pull request.

## Key Dependencies

**Critical:**
- Minecraft 1.21.1 - Runtime and API target configured in `gradle.properties`; mod dependency range emitted through `src/main/templates/META-INF/neoforge.mods.toml`.
- NeoForge 21.1.233 - Loader/runtime configured in `gradle.properties`; mod entry points and events are implemented in `src/main/java/com/typ/roosttraces/RoostTraces.java`.
- Ice and Fire CE 2.0-beta.17 - Required runtime dependency in `build.gradle` and `src/main/templates/META-INF/neoforge.mods.toml`; roost generation is hooked with `src/main/java/com/typ/roosttraces/mixin/DragonRoostPieceMixin.java`.
- Create ReAutomated 0.2.0 - Required runtime dependency in `build.gradle` and `src/main/templates/META-INF/neoforge.mods.toml`; node blocks are selected through tags such as `src/main/resources/data/roosttraces/tags/block/create_reautomated_nodes.json`.
- Create ReAutomated: Traces 0.2.1 - Required compile/runtime integration in `build.gradle`; public API calls are wrapped by `src/main/java/com/typ/roosttraces/compat/TraceCompat.java`.

**Infrastructure:**
- Parchment mappings 2024.11.17 for Minecraft 1.21.1 - Configured in `gradle.properties` and consumed by the `neoForge.parchment` block in `build.gradle`.
- Create 6.0.10-280 slim - Development runtime dependency in `build.gradle`; supports Create ReAutomated runtime testing.
- Ponder 1.0.82 for Minecraft 1.21.1 - Development runtime dependency in `build.gradle`; supports Create runtime dependencies.
- Flywheel 1.0.6 - Development runtime dependency in `build.gradle`; supports Create rendering/runtime dependencies.
- Registrate MC1.21-1.3.0+67 - Development runtime dependency in `build.gradle`; supports Create ecosystem runtime dependencies.
- Uranus Modrinth Maven artifact `1YdU56pR` and Jupiter 2.3.7-1.21.1-neoforge - Development runtime dependencies in `build.gradle` for Ice and Fire CE.
- Gson - Used by `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java` through Minecraft's bundled dependency surface to parse datapack JSON.
- SLF4J and Mojang LogUtils - Logger setup lives in `src/main/java/com/typ/roosttraces/RoostTraces.java`.

## Configuration

**Environment:**
- Mod identity and dependency versions are centralized in `gradle.properties`: `mod_id=roosttraces`, `mod_version=0.1.3`, `minecraft_version=1.21.1`, and dependency version pins.
- NeoForge common config is declared in `src/main/java/com/typ/roosttraces/RoostTracesConfig.java` and registered by `src/main/java/com/typ/roosttraces/RoostTraces.java`; runtime config is a generated common TOML file in the Minecraft config directory, not a repository file.
- Datapack-driven node pools are loaded from `data/*/roost_trace_node_pools` by `src/main/java/com/typ/roosttraces/pool/RoostTraceNodePoolResolver.java`; the bundled default is `src/main/resources/data/roosttraces/roost_trace_node_pools/default.json`.
- Built-in block tags are in `src/main/resources/data/roosttraces/tags/block`; these define replaceable roost blocks and Create ReAutomated node selectors.
- No `.env` files are present at repository root; do not add runtime secrets for this mod unless a future integration explicitly requires them.

**Build:**
- `build.gradle` defines repositories, dependencies, NeoForge runs, resource generation, local publishing, UTF-8 compilation, and `-Xlint:deprecation`.
- `settings.gradle` configures Gradle plugin management and Foojay toolchain resolution.
- `gradle/wrapper/gradle-wrapper.properties` pins the Gradle wrapper distribution.
- `src/main/templates/META-INF/neoforge.mods.toml` is expanded into generated mod metadata by `generateModMetadata` in `build.gradle`.
- `src/main/resources/roosttraces.mixins.json` configures the Mixin entry loaded by NeoForge metadata.

## Platform Requirements

**Development:**
- Run through `gradlew.bat` on Windows or `./gradlew` on Unix-like systems from the repository root.
- Java 21 toolchain is required; CI uses Temurin JDK 21 in `.github/workflows/build.yml`.
- External Maven repositories in `build.gradle` must be reachable to resolve NeoForge, Modrinth, Create, Ponder, Flywheel, Registrate, Ice and Fire CE, Uranus, and Jupiter artifacts.
- The build can prefer a local Create ReAutomated: Traces jar at `../CreateReAutomatedTraces/build/libs/createreautomatedtraces-${createreautomatedtraces_version}.jar`; otherwise it resolves `maven.modrinth:create-reautomated-traces` from the configured Maven repositories.

**Production:**
- Deployment artifact is a NeoForge mod JAR for Minecraft 1.21.1 built by Gradle from `build.gradle`.
- Runtime requires NeoForge, Minecraft, Ice and Fire CE, Create ReAutomated, and Create ReAutomated: Traces as declared in `src/main/templates/META-INF/neoforge.mods.toml`.
- No server-side web service, database, or hosted backend is part of the production stack.

---

*Stack analysis: 2026-07-02*
