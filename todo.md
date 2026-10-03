# GenshinInMinecraft todo

## First playable draft (planned 2026-10-04)

Goal: a player can join a world (test arena now, the ported map once it exists), play as the Anemo Traveler against a hilichurl camp with a Genshin-style HUD, then switch to Amber, Kaeya and Lisa and trigger reactions. Every step keeps the game runnable on both loaders and passes `./gradlew --no-daemon build runAllGameTests`.

1. [x] Project records: `AGENTS.md` (verified commands, invariants), `docs/decisions.md` (seeded with the brief's fixed decisions and tonight's choices), `CREDITS.md`, `spec/fidelity.md`; remove the template example command/mixins and their tests.
2. [x] Mechanics research for the slice (`gpt-researcher`, 7.1): damage formula and elemental gauge/decay/ICD/reaction rules for Anemo, Pyro, Cryo, Electro, Hydro; Traveler (Anemo), Amber, Kaeya, Lisa kits at talent level 1; hilichurl/slime stats.
3. [x] Rules layer in `common/` (no Minecraft types): stats, damage formula, aura application and decay, ICD, reactions for the starter elements, 60 fps event timeline; JUnit golden cases from `spec/mechanics/`.
4. [x] Managed world: a per-world switch that disables hunger, natural regeneration, block breaking/placing and natural mob spawning, plus a `/genshin arena` command that builds a flat test arena; server GameTests on both loaders.
5. [x] Active character and Traveler (Anemo) kit: server-owned character state (HP, energy, cooldowns), normal/charged attack string, tap/hold elemental skill, burst, keybinds sent as intent, Genshin damage applied to entities; plain-Java JUnit, shared server GameTests on both loaders and Fabric client arena/key-conflict GameTest. State is intentionally memory-only (relog/respawn/managed-off reset resources); see decisions/fidelity for timing, geometry, equipment and direct-energy adaptations.
6. [x] Managed-world HUD: four-row-capable party list with HP and energy, active numeric HP, skill/burst cooldowns and ready state, tracked floating damage/reaction numbers and synced enemy aura icons; vanilla bars return outside managed worlds. Pure formatting/layout tests and Fabric client arena/input/state GameTest pass; screenshots are in `fabric/build/run/clientGameTest/screenshots/genshin-hud-{aura,skill,burst-ready,vanilla}.png` (see decisions).
7. [x] Hilichurl enemy: custom entity with placeholder model, Genshin HP/RES, simple aggro/leash/telegraphed melee, camp spawner command; GameTests.
8. [x] Stamina: sourced 100-unit starter pool, sprint/dash costs and drain, delayed regeneration/exhaustion lockout, server-owned movement-direction dash with six-tick i-frames, 20-stamina sword charge, synced gold/red HUD arc. Screenshot: `fabric/build/run/clientGameTest/screenshots/genshin-stamina-wheel.png` (see decisions/fidelity for adapted distance, threshold and tick rounding).
9. Party switching and the other three starter kits:
   - [ ] 9a. Party of four with switching (keys 1–4 in managed worlds, sourced switch cooldown, off-field cooldowns/energy), per-character kit split out of `CombatRuntime.Session`; Kaeya, Amber and Lisa present with normal attacks only.
   - [ ] 9b. Kaeya kit (Frostgale, Glacial Waltz) and Cryo reactions in-world (Superconduct, Melt, Frozen with Hydro if available).
   - [ ] 9c. Amber kit (aimed shot, Explosive Puppet, Fiery Rain) incl. a minimal aim mode.
   - [ ] 9d. Lisa kit (Violet Arc stacks/Conductive, Lightning Rose) and Overloaded/Electro-Charged in-world.
10. [ ] Map port spike: read the archive's world `DataVersion` and scan palettes for non-vanilla blocks without modifying the archive; script extract + `--forceUpgrade` into a git-ignored directory. (Inspection and scripts done in 480182a; the full upgrade run is pending.)
11. [ ] Camera spike: third-person orbit camera with camera-relative movement (own it vs. ShoulderSurfing), recorded in `docs/decisions.md`.

## Questions for the owner

1. **Controls in managed worlds.** Item 5 makes E = Elemental Skill, Q = Elemental Burst and left click = Genshin normal attack, and suppresses the vanilla inventory/drop/melee on those keys *only while in a managed world*. Options: (a) keep this (Genshin muscle memory; vanilla inventory unreachable on E in managed worlds); (b) default to non-conflicting keys (e.g. R/F) and leave vanilla untouched. Recommendation: (a), since the vanilla inventory is replaced by Genshin menus later anyway.
2. **Where is Mondstadt on the map?** The spike found spawn at (3458, 63, -4002) and populated regions x −29..29, z −17..24 (region coords). Options: (a) you give block coordinates for Mondstadt city / Starfell Valley / Windrise; (b) the agent screenshots candidate areas on a headless client and you pick. Recommendation: (a) — a few coordinates from your own play save a lot of guessing; then only those regions get upgraded first.
3. **Running the 26.2 force-upgrade.** `scripts/map-port/port-map.py upgrade --accept-eula …` writes `eula=true` for a vanilla 26.2 server on the extracted copy (same EULA the GameTests already accept). Options: (a) the agent runs it overnight in the isolated session (~6 GB copy, likely hours at nice 19); (b) you run it yourself. Recommendation: (a), once Mondstadt coordinates are known, or for the whole world if you prefer.

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [x] Low: the NeoForge mods list showed the template description (fixed 2026-10-04; the shipped icon was already the project icon).
