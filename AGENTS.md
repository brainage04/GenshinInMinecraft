# Working on GenshinInMinecraft

Read [the durable brief](docs/GENSHIN_MINECRAFT_BRIEF.md) and [the task queue](todo.md) first. The brief is the long-term specification; the queue gives the next runnable slice. Record choices and their reasons in [docs/decisions.md](docs/decisions.md), mechanics sources in `spec/mechanics/`, adaptations in [spec/fidelity.md](spec/fidelity.md), and asset/data provenance in [CREDITS.md](CREDITS.md).

## Commands

Use Java 25 for Minecraft 26.2, as configured in `gradle.properties`. Run commands from the repository root. Verified 2026-10-04: `./gradlew --no-daemon build runAllGameTests` passes in about 4 minutes inside the isolated session; `runAllGameTests` runs the Fabric and NeoForge development server GameTests, the Fabric production server and client GameTests (Loom starts its own private Xvfb display for the client), and the NeoForge production server GameTests. The other rows are standard Loom/convention tasks and were not run individually.

| Purpose | Command |
| --- | --- |
| Build both loader jars, run unit tests and every GameTest (gate before each commit) | `./gradlew --no-daemon build runAllGameTests` |
| Build both loader jars | `./gradlew --no-daemon build` |
| All configured JUnit unit tests | `./gradlew --no-daemon test` |
| Fabric JUnit tests, including loader metadata and command registration | `./gradlew --no-daemon :fabric:test` |
| Fabric development client | `./gradlew --no-daemon :fabric:runClient` |
| NeoForge development client | `./gradlew --no-daemon :neoforge:runClient` |
| Fabric dedicated development server | `./gradlew --no-daemon :fabric:runServer` |
| NeoForge dedicated development server | `./gradlew --no-daemon :neoforge:runServer` |
| Both loaders' server GameTests | `./gradlew --no-daemon runAllGameTests` |
| NeoForge development server GameTests | `./gradlew --no-daemon :neoforge:runGameTest` |
| NeoForge release-jar server GameTests | `./gradlew --no-daemon :neoforge:runProductionServerGameTest` |
| Fabric release-jar client GameTest | `./gradlew --no-daemon :fabric:runProductionClientGameTest` (starts its own private Xvfb display) |

The root build places Fabric and NeoForge release jars in `build/libs`; install exactly one loader jar. Fabric requires Fabric API and Cloth Config; NeoForge requires Cloth Config. Loader dependencies and convention plugins are configured in the Gradle files; do not replace their conventions with a second build/test path.

### Private headless clients

Start agent sessions using `scripts/omp-isolated.sh`. Never use the owner's display, session D-Bus, clipboard, input devices or audio services. Keep all spawned clients and servers inside the isolated session's resource limits (12 GiB RAM and 3 CPUs); size JVM heaps accordingly.

**This machine has Xvfb, not sway or grim.** `runProductionClientGameTest` manages its own Xvfb display. For an interactive development client, wrap it in a private Xvfb display with software OpenGL:

```sh
ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1 \
xvfb-run -a --server-args="-screen 0 1280x720x24 -nolisten tcp" \
./gradlew --no-daemon :fabric:runClient
```

Xvfb needs the OpenGL/X11 runtime libraries listed in [README.md](README.md). Recording through `:fabric:recordClientGameTest` additionally needs ffmpeg and PipeWire tooling; never connect recording to the owner's audio session. The existing Fabric client GameTest joins a dedicated server and asserts that the client initializer, world and player are available.

## Module layout

- `common/src/main/java`: shared implementation. Plain-Java rules belong here and have **no Minecraft types**: stats, damage, elements, ICD, abilities, ordered event timelines, cooldowns, progression and save schemas. Keep them deterministic with seeded randomness and stable ordering.
- `common/src/main/resources`: shared resources, translations and mixin configurations. Empty mixin lists are intentional until a feature needs a mixin; both loader metadata files still reference these configurations.
- `common/src/gametest`: shared server GameTest bodies.
- `fabric/`: thin Fabric entrypoints and event wiring; `src/client` holds client entrypoints, `src/test` JUnit tests, and `src/gametest` server registrations plus the client GameTest.
- `neoforge/`: thin NeoForge entrypoints and event wiring; `src/gametest` registers the shared server tests and provides their test-instance JSON files.
- `spec/`: sourced mechanics research for the pinned Genshin version and the fidelity ledger.
- `docs/`: the durable brief, decisions, release documentation and existing icon provenance.
- `reference/map/*.zip`: pristine, git-ignored map archives. **Read-only.**

## Invariants

- Every feature works on **both Fabric and NeoForge**, including integrated and dedicated servers. Keep loader modules thin and shared logic in `common/`.
- The **server is authoritative** for damage, elemental state, cooldowns, energy, stamina, inventory, rewards, progression, quests and persistence. Clients send intent; prediction must not grant authority.
- Client-only Minecraft classes stay out of dedicated-server paths. Separate input, camera, rendering, HUD and screens from rules and server adapters.
- Mechanics are pinned to Genshin 7.1, as recorded in the brief. Re-pin only by a dated decision; never silently mix versions. Test expectations come from public sources and owner observations, not implementation output.
- Never weaken tests to obtain a pass. Accepted bugs need a reproduction and regression test. Keep server GameTests on both loaders and the Fabric client join/in-world test exercised.
- Do not modify `reference/map/*.zip`. Extract or upgrade only into disposable, git-ignored copies. **Never commit map data, extracted worlds or third-party packs.** Before the map port, develop in a generated flat test arena.
- Private use only: no public hosting, publishing or distribution before the permissions/legal review described in the brief. No leaked code, extracted game assets or connections to HoYoverse services/accounts. Purchased map/model references are not shipped.
- Environmental fire must never destroy map blocks. Record other intentional vanilla-behaviour changes and fidelity adaptations.
- Prefer the smallest concrete design; no generic engine or scripting language without a demonstrated need.
- Keep runtime worlds, screenshots, recordings and logs under git-ignored directories.
- **Never set or override git identity** in repository config, on the command line, or through `GIT_AUTHOR_*`/`GIT_COMMITTER_*`. Never hand an identity override to a delegated task. Before committing, `git var GIT_AUTHOR_IDENT` must show the globally configured `brainage04 <thomasmcmah21@gmail.com>`; stop on a fallback identity.
