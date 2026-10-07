# GenshinInMinecraft

Recreating Genshin Impact gameplay in Minecraft 26.2 (Java 25), with shared code, mixins and resources in `common/` and thin Fabric/NeoForge adapters in `fabric/` and `neoforge/`. The long-term target includes traversal, parties, combat and elemental reactions, exploration, quests, progression and co-op on the purchased WanggMC Blocky Teyvat map.

This is a private-use project for the owner and friends, not a template or a released gameplay pack. Read [the durable project brief](docs/GENSHIN_MINECRAFT_BRIEF.md), [agent commands and invariants](AGENTS.md), and [the runnable task queue](todo.md).

# Playing the first draft

1. `./gradlew --no-daemon :fabric:runClient` (or `:neoforge:runClient`), create a **new, disposable** creative world with cheats on.
2. `/genshin arena camp` — builds a flat50×50 arena around you (destructive inside that box), turns on managed mode, puts you in adventure mode and places three **Lv8** hilichurls at the far side. This early-camp level is a documented adaptation, awaiting the owner's exact WL0 Starfell/Windrise observations.
3. You are a party of Traveler (Anemo), Amber, Kaeya and Lisa at level 20. **1–4** switch; **left click** normal attack (hold for charged / Amber's aimed shot); **E** skill (tap/hold); **Q** burst; **Sprint** (left Ctrl) dashes and sprints using stamina; **mouse** orbits the camera, WASD moves relative to it; **F5** toggles first person. Walk into solid walls to **climb** (W/S up/down, A/D sideways, Jump climb-jump, S+Jump away, Sneak drop); airborne **Jump** opens/closes the **glider**, with WASD steering.
4. `/genshin camp hilichurl [count] [level]` spawns more (default3, count1–12; defaultLv8, level1–20); e.g. `/genshin camp hilichurl 3 20` selects the original level20 encounter. `/genshin aura hydro 2` puts a Hydro aura on the enemy you look at, for Electro-Charged/Frozen/Vaporize. `/genshin managed off` returns the world to vanilla rules and controls.

The fixed starter weapons are **level20/20, R1 Harbinger of Dawn** (Traveler/Kaeya), **Slingshot** (Amber) and **Thrilling Tales of Dragon Slayers** (Lisa), with their secondary stats/passives. Harbinger grants CRIT Rate above90% HP; Slingshot boosts normal/charged instant arrows, not E/Q; switching away from Lisa buffs the incoming member's ATK for10s (20s proc cooldown). No artifacts yet. Sources and exact published inputs: [damage specification](spec/mechanics/damage.md#named-starter-loadout-2026-10-07).

Full controls, numbers and adaptations: [AGENTS.md](AGENTS.md#in-game-development-commands), [docs/decisions.md](docs/decisions.md), [spec/fidelity.md](spec/fidelity.md).

# Playing on Blocky Teyvat

`python3 scripts/map-port/port-map.py make-playtest` creates a **fresh disposable copy** of the completed26.2 map upgrade with an arrival outside Mondstadt, managed mode and the versioned overlay applied on first load. It never edits the purchased ZIP or upgrade evidence world. Open it with:

```sh
./gradlew --no-daemon --no-configuration-cache \
  --init-script run/map-port/mondstadt-playtest-world.init.gradle :fabric:runClient
```

Accept the disposable copy's experimental-world prompts. Right-click the original mod Statues/Waypoints to activate per-player; **M** or `/genshin teleport` shows unlocked destinations. Statues recover the party from a shared finite reserve; a full wipe's Revive returns to the nearest player-unlocked point at35% HP. Starfell/Windrise each have a Lv8 three-member camp with persistent12-hour death timers (documented adaptations, not a claimed quest recreation). On another disposable map copy, operators use `/genshin managed on` and `/genshin overlay apply`. **Never run `/genshin arena` on this map**; the overlay itself places only entities and edits no blocks.

Landmark coordinates/screenshots and exact behavior: [docs/map-landmarks.md](docs/map-landmarks.md). Fresh-copy safety checks, names/options and mandatory private-display launch for agents: [scripts/map-port/README.md](scripts/map-port/README.md#managed-mondstadt-playtest).

# Two-player co-op

Run the matching Fabric or NeoForge jar/dependencies on a dedicated server and both clients, enable `/genshin managed on` (or use a disposable arena/map overlay), and join the same world. The group is all connected managed-world players, across dimensions, maximum4. Two players each control2 of their own4 characters;3 players get2/1/1 (earliest connected player gets2),4 players get1 each. Overworld duplicates across players are allowed. FirstN slots are kept by default; outside combat, actions and traversal use `/genshin coop pick 1 3` (or `1,3`) to choose exactly your allocation. Leaving restores the full owned party without resetting dormant HP, energy or cooldowns.

The small top-left co-op list shows teammate names, active characters and HP; each real remote player uses their active-character model/nameplate. Both players attack the same enemies and can react on each other's auras; damage belongs to the trigger character. No direct player/puppet friendly fire or Thrilling Tales transfer occurs. Skill particles credit nearby living same-dimension partners using each own roster and Energy Recharge. Enemy HP scales×1.5 with2 players (×2/×2.5 with3/4); current overworld ATK is unchanged. Statues spend one shared reserve but unlocks stay per-player. A wipe/35% respawn affects only that player's allocated characters; teammates continue. Resonance is not implemented. Routing32-block range, live HP-percentage rescaling, join-order host and roster choices are explicit [adaptations](docs/decisions.md), not invented pinned-release measurements.

Repeatable private dedicated-server proof: `python3 scripts/coop-smoke/run.py` **after** the Gradle gate, never alongside it. It runs two separate headless development clients and saves their remote-model screenshots plus combat/reaction/independent-respawn assertions under ignored `run/coop-smoke/<timestamp>/`; requirements and isolation are in [AGENTS.md](AGENTS.md#dedicated-two-client-co-op-smoke). The driver is test-only, absent from release jars.

Verified2026-10-08: full gate passes with86 server tests per loader/mode plus the connected Fabric client suite; separate two-client smoke passes. Inspected proof: `run/coop-smoke/20261008-091603/screenshots/coop-host-sees-lisa.png`, `coop-guest-sees-kaeya.png` and `coop-guest-wiped.png`; exact run/assertions are recorded in [decisions](docs/decisions.md#2026-10-08---dedicated-server-co-op-allocation-and-combat-24).


# Development

`./gradlew --no-daemon build` produces two jars in `build/libs`: `genshininminecraft-<version>.jar` (Fabric, requires Fabric API, Cloth Config and **GeckoLib Fabric 26.2 5.5.5**) and `genshininminecraft-neoforge-<version>.jar` (NeoForge, requires Cloth Config and **GeckoLib NeoForge 26.2 5.5.6**). Install exactly one loader jar plus the matching external GeckoLib jar on clients **and dedicated servers**; do not install the common API jar or both loader builds. Development and production GameTests provision GeckoLib automatically.

- Use Java 25, configured by `java_version` in `gradle.properties`.
- `./gradlew --no-daemon :fabric:runClient` / `./gradlew --no-daemon :neoforge:runClient` launch development clients.
- `./gradlew --no-daemon :fabric:runServer` / `./gradlew --no-daemon :neoforge:runServer` launch dedicated development servers.
- Agents must use a private headless display, never the owner's desktop. See [AGENTS.md](AGENTS.md).
- Mod Menu is a Fabric development dependency for the Cloth Config screen, generated by AutoConfig from `ModConfig` and saved to `config/genshininminecraft.json`; NeoForge exposes the screen through the Mods list.
- The Blocky Teyvat map port is scripted in `scripts/map-port/` (see [docs/map-port.md](docs/map-port.md)); worlds stay in git-ignored `run/map-port/`.
- Managed players have original blocky Aether/Amber/Kaeya/Lisa bodies and character arms/weapons, animated locomotion/climbing/gliders, and server-synced normal/charged/aim/skill/burst motions timed to the kits. Combat overrides upper body while legs keep locomotion, with additive hurt recoil and a final fallen pose. Vanilla rendering returns managed-off. Art is recognisable placeholder-quality original geometry/motion, not imported Genshin/fan assets; all action sounds use vanilla events.
- Re-author checked-in geo/animation/PNG assets with `python3 tools/models/generate.py`; verify byte-for-byte reproduction with `python3 tools/models/generate.py --check`. Only Python 3's stdlib is needed for authoring; normal builds require neither Python nor Blockbench.

# Testing

The complete build and server GameTest gate is:

```shell
./gradlew --no-daemon build runAllGameTests
```

Run JUnit unit tests separately with:

```shell
./gradlew --no-daemon test
```

JUnit tests in `common/src/test` cover the plain-Java rules layer (damage, reactions, ICD, energy, stamina, party, each character kit, camera math and server-authoritative locomotion phase/time), plus generated asset contracts; Fabric unit tests round-trip the public visual payload. Shared server GameTests in `common/src/gametest` run on both loaders and cover managed-world protections, the arena and camp commands, every starter kit in-world, reactions, hilichurl AI, stamina/dash, party switching and lifecycle edge cases. The Fabric client GameTest joins a dedicated server and drives real keys and mouse input (camera, kits, HUD), verifies managed GeckoLib versus unmanaged vanilla extraction/body/empty-and-occupied first-person submissions, parses/bakes every original model/clip and tests absolute phase seeking/resource reload. It saves standing captures `genshin-character-{aether,amber,kaeya,lisa}.png`, each character's `-arms.png`, and `genshin-character-{dash,climb,glide}.png` under `fabric/build/run/clientGameTest/screenshots/`. Its task is:

```shell
./gradlew --no-daemon :fabric:runProductionClientGameTest
```

`runAllGameTests` already starts its own private Xvfb display for the client GameTest. On Ubuntu, headless clients need Xvfb and the OpenGL/windowing libraries used by the GitHub Actions workflow. Recording additionally needs ffmpeg and PipeWire tools:

```shell
sudo apt-get update
sudo apt-get install -y ffmpeg pipewire-bin xvfb mesa-utils libflite1 libgl1-mesa-dri libglx-mesa0 libxi6 libxrandr2 libxrender1 libxtst6 libxinerama1 libxcursor1 libxxf86vm1
```

To run a development client headlessly (never on the owner's session):

```shell
ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1 \
xvfb-run -a --server-args="-screen 0 1280x720x24 -nolisten tcp" \
./gradlew --no-daemon :fabric:runClient
```

The optional FabricModdingConventions recording task is `./gradlew --no-daemon :fabric:recordClientGameTest`. It requires a private display and recording/audio services; never use the owner's PipeWire session.

# Publishing

Public publishing/distribution is not authorized: first complete the permissions and legal review in the brief. The existing automation is documented for reference.

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md).
Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).

# Credits

Thank you to [nea89o](https://github.com/nea89o)
for developing the GitHub Actions [workflow](https://github.com/nea89o/Forge1.8.9Template/blob/master/.github/workflows/init.yml)
and [script](https://github.com/nea89o/Forge1.8.9Template/blob/master/make-my-own.sh)
from which I based my workflow and script off of.

See [CREDITS.md](CREDITS.md) for template, map/model reference and existing project-icon provenance.
