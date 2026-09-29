# Genshin in Minecraft — project brief

Updated 2026-09-29. This is the durable spec. `todo.md` holds open work; `docs/decisions.md` records choices made along the way.

## Goal

Recreate Genshin Impact's gameplay in Minecraft Java on top of the purchased WanggMC Blocky Teyvat map: traversal, camera and controls, parties, combat and elemental reactions, enemies, exploration, quests, progression, menus, persistence, and co-op for a small group. This is not a themed item pack or a vanilla-combat reskin. The full recreation stays the long-term goal; deliver it as playable slices that each work end to end.

Priorities, in order: protect the owner's files; correct behaviour; a playable experience; repeatable tests; maintainable code; content coverage; visual polish. Readable visuals and usable controls count as playability, not polish.

## Ground rules

- **Private use only.** The mod is for the owner and friends. Nothing is published, hosted publicly, or distributed until the owner has contacted wangg_mc, taken legal advice, and possibly contacted HoYoverse. Fan-made or placeholder assets are fine for private play; record where every non-original asset came from (`CREDITS.md`) so a later release can be audited.
- **Map files are read-only.** `reference/map/*.zip` is the pristine source and is git-ignored. Extract and upgrade only into disposable copies. Never commit map data, extracted worlds, or third-party packs.
- **No leaked code, no extracted game assets, no connection to HoYoverse services or accounts.** Mechanics come from public documentation, community research (KQM Theorycrafting Library, wikis), and the owner's own gameplay observations.

## Fixed decisions

| Topic | Decision |
| --- | --- |
| Minecraft | 26.2, Java 25 (see `gradle.properties`) |
| Loaders | Fabric and NeoForge. Shared code, mixins and resources in `common/`; loader modules stay thin adapters. Every feature must work on both. |
| Map | Blocky Teyvat 5.1.0 (shipped for 1.21.10), ported to 26.2 on a copy. |
| Genshin reference | Version 7.1 "A Rekviem for the Underworld" (released 2026-09-23). Re-pin only as a deliberate decision; never mix mechanics from different versions silently. |
| First area | A compact part of Mondstadt that the map actually contains. |
| Starter party | Traveler (Anemo), Amber, Kaeya, Lisa, implemented one kit at a time. |
| Multiplayer | Integrated and dedicated servers, up to 4 players in co-op. Follow Genshin's co-op rules; any deviation is a recorded adaptation. |
| Character visuals | Placeholders first. Better models later (fan models such as the YiFang `.ysm` set are allowed for private use). |
| Payments | None. Character acquisition, if added, is local gameplay only. |

## Models and tooling

- **Claude Opus 5.5 (medium)** does all lead, planning, implementation, integration, debugging and final review work.
- **GPT (openai-codex subscriptions, available until late October 2026)** handles lower-stakes work where an independent model adds value:
  - `gpt-researcher` agent: compiles sourced mechanics data (talent scalings, frame data, gauge/ICD tables, reaction multipliers, enemy stats) into `spec/mechanics/`. Every value carries a source; Opus spot-checks before a value becomes a test expectation.
  - `reviewer` agent: second-opinion review of diffs before merge. Advisory, not a gate.
  - Session advisor: comments on completed turns.
  - When the subscriptions lapse, delete the GPT entries from `.omp/config.yml` and `.omp/agents/`.

## Hard problems

These decide whether the project feels like Genshin. Each needs a small spike (prototype plus a test or screenshot) before the approach is locked in, and the chosen approach goes in `docs/decisions.md`.

### 1. Porting the map to 26.2

The 5.1.0 package is a full 1.21.10 launcher instance with one world under `saves/`. Its mods are editors, renderers and utilities (WorldEdit, Axiom, Sodium, Iris, Voxy, ShoulderSurfing, Xaero's map, C2ME, Flashback); none appear to add blocks, so the world is expected to be vanilla blocks. Confirm by reading `level.dat` `DataVersion` and scanning chunk palettes for non-`minecraft:` block IDs.

Approach: extract the world into a working directory, run a 26.2 dedicated server with `--forceUpgrade` on a copy, then compare sampled regions before and after (block-palette counts per region, spot screenshots at landmarks). Record world size, upgrade time and any renamed or missing blocks. The upgraded copy becomes the dev world; the upgrade must be a repeatable script, not a one-off.

The map ships Voxy (LOD rendering) and shader packs. The core game must work without them; a separate visual profile adds Sodium/Iris or equivalents where 26.2 versions exist.

### 2. Character models, animation and party switching

Genshin party switching swaps the visible character, stats and kit instantly while the player keeps one position. In Minecraft the player remains one entity; the "active character" is server state that drives the renderer, hitbox, stats and abilities.

Open questions for the spike:
- Rendering path on 26.2: GeckoLib (check 26.2 availability on both loaders), a hand-written model renderer, or a player-model replacement.
- Animation: attack strings, skills and bursts need readable wind-up and recovery, because combat timing is judged by eye.
- Placeholder standard: a distinct blocky model or tinted player model per character, with an element-coloured nameplate, good enough to tell the four apart at a glance.

### 3. Camera and controls

Genshin uses a third-person orbit camera with camera-relative movement: WASD moves relative to the camera and the character turns to face its movement, independent of where the camera looks. Minecraft ties body facing to the camera. This needs a client-side decoupled camera plus server-validated movement intent.

Bindings to cover: normal attack (tap/hold for charged), elemental skill (tap/hold), burst, party slots 1–4, sprint/dash, jump, glide, climb, interact, map/menus. Decide whether to depend on ShoulderSurfing (it ships with the map pack) or own the camera; owning it is likely, since targeting and aiming must integrate with it. Aim mode is needed for Amber's bow.

### 4. Traversal

Stamina drives sprinting, dashing (with i-frames), climbing, gliding, swimming and charged attacks. Climbing must work on the map's blocky terrain; gliding needs a wind-glider state with Genshin's descent rate; falls use Genshin's fall damage rules, not vanilla's. Distances and speeds must be calibrated to the map's scale; measure it first (see 8).

### 5. Combat timing and hit detection

Genshin frame data is expressed at 60 fps; Minecraft ticks at 20 TPS (50 ms). The rules layer keeps its own ordered event timeline with sub-tick timestamps, so events such as "hit at frame 23" are not all rounded to the nearest tick. Document every residual difference.

Also needed:
- Attack shapes (arcs, circles, projectiles), not only entity bounding boxes.
- Hit-lag, poise and interruption, stagger, and dash i-frames.
- Server-authoritative hits, with client prediction only where latency would make combat feel wrong.

### 6. Elemental system and damage

Aura application with gauge units and decay, internal cooldown (ICD) groups, every reaction relevant to the starter elements (Anemo, Pyro, Cryo, Electro, Hydro from the environment), the damage formula (DEF, RES, crit, bonuses), energy and particles, and shields. Environmental interactions matter too: burning grass, water puddles, electro-charged water. Environmental fire must never destroy the map's blocks.

### 7. Enemies

Mondstadt starter enemies (hilichurls, slimes, samachurls, abyss mages to begin with) need custom AI: camps, aggro and leash ranges, attack telegraphs, and Genshin's HP/RES/poise values. Vanilla mob spawning is off in managed areas.

### 8. Map as a content overlay

The map is geometry and art only. It contains no quests, triggers, chests or navigation data.
- Place NPCs, chests, enemy camps, Waypoints, Statues of The Seven, puzzles and quest triggers as a versioned overlay with stable IDs bound to coordinates.
- Measure the map's scale and orientation against Genshin landmarks locally; don't assume one global transform.
- Verify one small area end to end before placing content elsewhere.

### 9. Vanilla interference

Hunger, natural regeneration, vanilla damage and knockback, block breaking and placing, the vanilla inventory and hotbar, item durability, and possibly day/night all conflict with Genshin rules. Disable or replace each one inside the managed world, and list every decision in `docs/decisions.md`.

### 10. UI

HUD: party list with HP and burst energy, skill and burst cooldowns, stamina wheel, damage numbers, and element icons over enemies. Menus: character screen (stats, talents, weapon, artifacts), inventory, quests, and map. All client-side screens, fed by synced server state.

### 11. Co-op

Genshin co-op gives each player their own characters, with a shared 4-character cap across players. The host's world state is shared; chest and quest ownership has per-player rules. Timing-sensitive combat must stay fair under real network latency, so test on a dedicated server with separate clients, not only on an integrated server.

## Architecture

1. **Rules layer** (plain Java in `common/`, no Minecraft types): stats, abilities, event timeline, elements and reactions, damage, cooldowns, progression, save schemas. Deterministic (seeded randomness, stable ordering) and unit-testable.
2. **Server adapters**: entities, movement validation, authoritative state, rewards, interactions, networking, persistence.
3. **Client adapters**: input, camera, rendering and animation, HUD and menus, effects, prediction/interpolation where needed. Client-only classes stay out of dedicated-server code paths.
4. **Data**: characters, abilities, enemies, items, quests, encounters and map bindings as versioned data files. Character kits that don't fit the shared schema get explicit code rather than an inaccurate generic template.

The server owns damage, elemental state, cooldowns, energy, stamina, inventory, rewards, progression, quest state and persistence. Clients send intent.

Keep the design as small as the features require; no generic engine or scripting language without a concrete need.

## Mechanics research

- `spec/mechanics/` holds the research for the pinned version: one file per character, enemy family, or system, with a source link for every value and notes on conflicts or unknowns.
- Research the next slice in depth, not the whole game up front.
- Test expectations come from these sources, never from running the implementation.
- Track anything approximated or deliberately changed for Minecraft in `spec/fidelity.md` (behaviour, status: `specified` / `implemented` / `verified` / `adapted`, and the reason).

## Testing

- **Rules tests** (JUnit): golden cases from sourced values, boundaries, reaction/ICD edge cases.
- **Server GameTests** (both loaders): entities, abilities in-world, save/load, rewards granted once, multiplayer ownership.
- **Client GameTests** (Fabric, headless via Xvfb): controls, camera, HUD, screens, joining a dedicated server.
- **Visual checks**: a dev-only, loopback-bound control bridge that can send inputs to a running client and capture screenshots, so behaviour can be checked by eye as well as by assertions. Never included in release builds.
- **Bug fixes**: every accepted bug gets a reproduction and a regression test. Tests are never weakened to get a pass.

## Milestones

- **M0 — foundation.** Map port script and upgraded dev world; client and dedicated server launch on both loaders; control bridge takes a screenshot, sends input, and observes a server-side state change; world reset works; two clients join one dedicated server.
- **M1 — first playable slice.** In a compact Mondstadt area:
  - camera and controls, traversal with stamina;
  - one character's full kit, then party switching and the other three;
  - elemental reactions for those kits and one enemy camp;
  - one short quest with a reward, menus for character and inventory;
  - save/reload and two-player co-op.

  Each step keeps the game runnable.
- **M2 — system completeness.** Weapons, artifacts, levelling and talents, death and revival, more enemy types, puzzles, domains, exploration rewards.
- **M3 — content expansion.** More characters, enemies, quests and regions through the data pipeline. Report coverage per category, not as a single percentage.
- **M4 — hardening and presentation.** Multi-client stress, reconnects, save migration, performance profiling on this machine, visual profile with shaders and LOD, install instructions for friends.

## Project records

Keep these small and current:
- `AGENTS.md`: commands and invariants.
- `todo.md`: the task queue and next runnable task.
- `docs/decisions.md`: dated decisions with reasons.
- `spec/`: mechanics research and fidelity ledger.
- `CREDITS.md`: asset and data provenance.

Local run artifacts (screenshots, logs, world copies) go under a git-ignored directory.

## References

- Map listing: https://ko-fi.com/s/5609329f72 (creator: https://ko-fi.com/wanggmc/shop)
- KQM Theorycrafting Library: https://library.keqingmains.com/
- Genshin version history: https://genshin-impact.fandom.com/wiki/Version
- Fabric automated testing: https://docs.fabricmc.net/develop/automatic-testing
- Minecraft usage guidelines: https://www.minecraft.net/en-us/usage-guidelines
