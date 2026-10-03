# GenshinInMinecraft todo

## First playable draft (planned 2026-10-04)

Goal: a player can join a world (test arena now, the ported map once it exists), play as the Anemo Traveler against a hilichurl camp with a Genshin-style HUD, then switch to Amber, Kaeya and Lisa and trigger reactions. Every step keeps the game runnable on both loaders and passes `./gradlew --no-daemon build runAllGameTests`.

1. [x] Project records: `AGENTS.md` (verified commands, invariants), `docs/decisions.md` (seeded with the brief's fixed decisions and tonight's choices), `CREDITS.md`, `spec/fidelity.md`; remove the template example command/mixins and their tests.
2. [x] Mechanics research for the slice (`gpt-researcher`, 7.1): damage formula and elemental gauge/decay/ICD/reaction rules for Anemo, Pyro, Cryo, Electro, Hydro; Traveler (Anemo), Amber, Kaeya, Lisa kits at talent level 1; hilichurl/slime stats.
3. [x] Rules layer in `common/` (no Minecraft types): stats, damage formula, aura application and decay, ICD, reactions for the starter elements, 60 fps event timeline; JUnit golden cases from `spec/mechanics/`.
4. [x] Managed world: a per-world switch that disables hunger, natural regeneration, block breaking/placing and natural mob spawning, plus a `/genshin arena` command that builds a flat test arena; server GameTests on both loaders.
5. [ ] Active character and Traveler (Anemo) kit: server-owned character state (HP, energy, cooldowns), normal attack string, elemental skill, burst, keybinds sent as intent, Genshin damage applied to entities; GameTests.
6. [ ] HUD: party list with HP and energy, skill/burst cooldowns, damage numbers, element icons over enemies; client GameTest screenshot.
7. [ ] Hilichurl enemy: custom entity with placeholder model, Genshin HP/RES, simple aggro/leash/telegraphed melee, camp spawner command; GameTests.
8. [ ] Stamina: sprint, dash with i-frames, charged-attack cost, stamina wheel on the HUD.
9. [ ] Party switching with Amber, Kaeya and Lisa kits (one at a time), reactions between them in-world.
10. [ ] Map port spike: read the archive's world `DataVersion` and scan palettes for non-vanilla blocks without modifying the archive; script extract + `--forceUpgrade` into a git-ignored directory.
11. [ ] Camera spike: third-person orbit camera with camera-relative movement (own it vs. ShoulderSurfing), recorded in `docs/decisions.md`.

## Questions for the owner

(none yet)

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [x] Low: the NeoForge mods list showed the template description (fixed 2026-10-04; the shipped icon was already the project icon).
