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

### In-game development commands

- `/genshin` reports the loaded mod version.
- `/genshin managed on|off|status` requires operator level 2. The switch covers the entire saved world/all dimensions, persists in overworld saved data and restores previous gamerules when switched off.
- `/genshin arena` requires operator level 2 and a player source. **Destructive:** rewrites a fixed 50×50×6 volume around that player with a smooth-stone floor, a one-block border and cleared air; enables managed mode, teleports the player to the centre and sets adventure mode. Use generated/disposable development worlds only.
- `/genshin camp hilichurl [count] [level]` requires operator level 2 and a player source; spawns a command-only club camp at that player's position (default3 members, count1–12; defaultLv8, level1–20 from the sourced HP table). `/genshin arena camp` places threeLv8 hilichurls at the far side of the generated arena. Lv8 is a documented WL0 adaptation awaiting owner observations of exact Starfell/Windrise camps, not equal to party level. Hilichurls fight only in managed mode: survival/adventure aggro, raised-club half-second wind-up, chase and anchor leash/reset. Explicit spawns work with natural spawning disabled; `/summon genshininminecraft:hilichurl` usesLv8 and establishes its own anchor.
- `/genshin aura pyro|cryo|electro|hydro <gauge>` requires operator level2, a player source and managed mode. Applies the positive **untaxed** gauge to the aimed combat target within16 blocks, otherwise the nearest eligible enemy in that range. Players and all player-owned puppets are excluded. This debug source enables Hydro playtesting (including Electro-Charged/Frozen/Vaporize); the starter party has no Hydro character. Aura tax/reaction/coexistence rules still apply; the command is not talent damage or an application-ICD hit.
- Managed survival/adventure players cannot break/place blocks or trample farmland. Creative players bypass those editing protections. Hunger, natural regeneration, natural spawning, environmental fire spread and mob griefing are disabled; weather/day-night remain unchanged. See [the detailed decision](docs/decisions.md#2026-10-04--saved-world-managed-mode-and-bounded-developer-arena).
- In managed mode, rebindable **1–4** switch **Traveler (Anemo), Amber, Kaeya, Lisa**, consuming conflicting vanilla hotbar clicks only there. One-second switch cooldown/action-specific cancel windows; fallen members cannot be selected. HP/energy/cooldowns persist off-field; stamina is shared. Traveler has left-click normal/charged, **E** tap/hold Palm Vortex and **Q** Gust Surge (60 energy). Kaeya has five Physical normals,20-stamina charge, **E Frostgnaw** (191.2% Cryo/2U/6s cooldown), **Q Glacial Waltz** (60 energy/8s orbit/15s cooldown), following the active character after switching. Amber taps five Physical normals, holds attack to aim (zoom/bracket/slower movement), releases Physical or after86 reference frames124%/2U Pyro; bow aim costs0 stamina. **E Explosive Puppet** throws a scaled vanilla-rabbit Baron Bunny (8s landed lifetime/15s cooldown), taunting hilichurls/exploding on expiry or destruction; **Q Fiery Rain** costs40 energy (2s rain/12s cooldown). Lisa has four Electro normals/50-stamina charge, **E Violet Arc** tap homing80%/1U ball (1s cooldown/0 particles), hold after approximately1.9s (maximum4s)10-block lightning consuming up to3 Conductive marks for320/368/424/487.2% ATK (2U/16s release-relative cooldown/5 hit particles), and **Q Lightning Rose** (80 energy/29 half-second36.56% arcs/15s field/20s cooldown). Tap marks have independent15s expiries/purple pips; ascension0 charged attacks **do not** grant the locked A1 stack passive. Bunny/rain/Rose persist after switching; Rose stays at its cast location. Managed E/Q/attack bindings take priority over vanilla controls. Player-owned effects exclude all players/puppets and never place fire or destroy blocks.
- While Amber is aiming, **Jump or Sprint cancels the held shot**, restores normal movement speed and hides zoom/reticle; releasing attack afterward does not fire. Cancellation also works for stationary or exhausted Sprint, even when no dash can start.
- Managed mode starts a **4-block third-person orbit**: mouse rotates the camera independently; WASD follows camera yaw and the character turns smoothly toward movement. **F5 toggles rear orbit/first person** (no front view there); managed-off/disconnect restores the previous vanilla perspective, and vanilla camera controls/cycling elsewhere are unchanged. Attack/E/Q press soft-faces the nearest visible eligible enemy within the adapted6-block/120° camera-forward cone before intent, without rotating the orbit; no target preserves body facing. Amber hold aim temporarily uses a clipped2-block/.6-right shoulder view with body facing the aim ray and a parallax-corrected eye-ray bracket. Both loaders share client-only hooks; no ShoulderSurfing dependency. See the dated item11 decision and `genshin-camera-{orbit,collision,amber-shoulder}.png` under the Fabric client GameTest screenshots directory.
- In managed gameplay, **the vanilla Sprint binding** (default left Ctrl, rebindable under vanilla movement controls) dashes on press while moving, then sprints while held; releasing it stops sprint rather than leaving vanilla's sprint latch/double-tap active. No new Dash binding, right-mouse override or Shift/sneak remap. The server spends shared party stamina and sends the six-tick movement-direction dash burst; the client never grants stamina or i-frames. Players start with100 stamina, held sprint drains18/s, sword charged attacks cost20 and Lisa charge costs50. Empty stamina locks these actions until recovery exceeds the named15-unit adaptation. A gold arc right of the crosshair appears while stamina is below full or draining, and turns red during exhaustion.
- Managed traversal uses existing movement bindings: walk into a full solid vertical collision face to climb; **W/S** ascend/descend, **A/D** move sideways, **Jump** climb-jumps, **S+Jump** jumps away and **Sneak** drops. Reach a clear ledge to mantle. Airborne **Jump** with at least two blocks of ground clearance opens the glider; WASD steers camera-relative, Jump again (or Sneak) closes it. Exhaustion releases the wall/glider. The green wing-particle trail and HUD hint are original placeholders, not elytra. Managed falls affect active-character max HP, with forced party replacement on lethal landings; above-waist water negates them. All calibration values are in `rules/Traversal` and recorded as adaptations.
- Managed gameplay shows all four element-coloured party rows with HP/energy, active numeric HP and E/Q cooldowns. Other players see the active-character nameplate; switching emits coloured dust. Hilichurls display `Lv. N · Hilichurl` below aura icons and independent purple Conductive pips. Active-character HP mirrors vanilla health; falls force the next living slot, all-dead uses vanilla death. Vanilla hearts/hunger/armor/XP are hidden, hotbar/crosshair remain, F1 hides custom indicators. Resources reset on relog/respawn/managed-off. Hilichurl HP follows its spawn level (Lv8:237.820; Lv20:885.200); other vanilla mobs retain training profiles. The fixed level20/20 R1 loadout is Harbinger of Dawn (Traveler/Kaeya), Slingshot (Amber), Thrilling Tales (Lisa), including sourced secondary stats/passives; no artifacts yet.
- Party rows now dim and sweep/count down while switch cooldown or a finite action window blocks them; an indefinite hold/traversal shows **LOCK** and a fallen member shows a grey boxed **X**. Original radial E/Q sweeps accompany cooldown seconds; Q's ring/diamond fill with energy and glow only when ready. Actual server-rejected switch/E/Q presses briefly flash/show feedback; rejected normals only click (no invented normal cooldown HUD—item20 owns recovery animation). Dash/start remains the rechecked18 units, charged once with no held drain during the six-tick dash. See `spec/mechanics/ui.md`.
- For ordinary mouse-based creative block editing, first run `/genshin managed off`: the managed client's attack key is kit-owned even in creative. The existing server block policies still allow creative placement and direct creative edits.

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
- `common/src/main/resources`: shared resources, translations and mixin configurations. The server mixins protect managed hunger, block placement and farmland; both loader metadata files reference the shared configurations.
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
