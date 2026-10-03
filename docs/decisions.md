# Decisions

Dated choices and reasons. [The project brief](GENSHIN_MINECRAFT_BRIEF.md) is the durable specification; [todo.md](../todo.md) gives the runnable order. Record future changes here rather than silently changing a fixed choice.

## 2026-09-29 — fixed decisions from the brief

| Topic | Decision | Reason |
| --- | --- | --- |
| Minecraft | 26.2, Java 25 (see `gradle.properties`). | Fixed platform baseline for code, dependencies and tests. |
| Loaders | Fabric and NeoForge; shared code, mixins and resources in `common/`, with thin loader adapters. Every feature works on both. | One shared implementation with loader parity. |
| Map | Blocky Teyvat 5.1.0, shipped for 1.21.10, ported to 26.2 on a copy. | Use the purchased map while preserving its pristine source. |
| Genshin reference | Version 7.1, "A Rekviem for the Underworld", released 2026-09-23. Re-pin only as a deliberate decision. | Keep sourced mechanics and test expectations on one version. |
| First area | A compact part of Mondstadt that the map actually contains. | Prove one playable area end to end before expanding content. |
| Starter party | Traveler (Anemo), Amber, Kaeya and Lisa, one kit at a time. | A bounded starter-party slice that grows without losing playability. |
| Multiplayer | Integrated and dedicated servers, up to four players. Follow Genshin co-op rules; record deviations as adaptations. | Support the owner and friends without silently changing ownership or party rules. |
| Character visuals | Placeholders first; better models later, including privately used fan models such as the YiFang `.ysm` set. | Prioritize readable, playable kits before visual polish. |
| Payments | None. Character acquisition, if added, is local gameplay only. | No payment or external-account dependency. |

## 2026-10-04 — first playable draft scope and order

Follow the [first playable draft queue](../todo.md#first-playable-draft-planned-2026-10-04): project records and template cleanup; sourced starter mechanics; the plain-Java rules layer; a managed world and arena; server-owned Traveler state and kit; HUD; a hilichurl camp; stamina; then party switching and Amber, Kaeya and Lisa kits/reactions. The map-port and camera spikes follow in the queue.

The draft target is a player joining a world, playing the Anemo Traveler against a hilichurl camp with a Genshin-style HUD, and then switching among the starter party to trigger reactions. Each step stays runnable on both loaders. This is a delivery slice, not a reduction of the brief's full-recreation goal.

## 2026-10-04 — generated arena before the map port

Develop and test gameplay in a generated flat test arena until the map port is available. The planned `/genshin arena` command is tracked in `todo.md`; it is not part of the initial root-command replacement. This keeps mechanics work independent of the map upgrade and protects the purchased archive from iterative development writes. Port only disposable copies, and keep world data out of git.

## 2026-10-04 — remove template examples

Remove the template command, client command and empty injection classes rather than carrying them into gameplay code. `/genshin` is the real server command root and initially reports the mod version obtained from loader metadata. Keep the shared command-registration plumbing for later commands, with no client-only commands registered yet.

Replace the old command-registration tests with `/genshin` registration coverage, including one shared server GameTest registered on each loader. Preserve the Fabric loader metadata test and the client dedicated-server join/in-world checks. Keep both mixin JSON files and their loader references, with valid empty lists, so the existing cross-loader resource setup remains consistent.

## 2026-10-04 — saved-world managed mode and bounded developer arena

`/genshin managed on|off|status` and `/genshin arena` require operator level 2 (`Commands.LEVEL_GAMEMASTERS` in 26.2). Managed mode is one switch for the entire world save, including all dimensions, persisted as overworld saved data under `genshininminecraft:managed_world`. Enabling snapshots only the changed gamerules; repeated `on` preserves that snapshot, and `off` restores the exact original values even after a saved-data reload.

The identifiers below were read from the decompiled mapped Minecraft 26.2 `GameRules`, `FoodData`, `FireBlock`, `LavaFluid`, `ChunkMap`, `Creeper` and `ServerLevel` sources, not copied from older Minecraft gamerule names:

| Vanilla system | Managed behaviour and implementation |
| --- | --- |
| Hunger, exhaustion and starvation | Shared `FoodData.tick` mixin keeps food and saturation at 20, clears exhaustion and the hunger timer, and skips vanilla hunger processing. Hunger-driven healing is skipped as well. |
| Natural health regeneration | `minecraft:natural_health_regeneration = false`. |
| Natural mob spawning | `minecraft:spawn_mobs = false`; explicit custom entity spawning remains available for future encounters. Existing mobs are not removed. |
| Fire consumption/spread and lava ignition | `minecraft:fire_spread_radius_around_player = 0`. There is no `doFireTick` rule in 26.2: both fire ticks and lava ignition check `ServerLevel.canSpreadFireAround`; `ChunkMap` requires player distance strictly less than the radius, so zero disables them everywhere. Fire may remain visible and still deal vanilla entity damage; it never consumes map blocks. |
| Mob griefing and mob explosions | `minecraft:mob_griefing = false`. `Creeper` uses `ExplosionInteraction.MOB`; `ServerLevel` chooses `BlockInteraction.KEEP` for these explosions when griefing is disabled. Entity damage is not replaced by this slice. |
| Player block breaking | Fabric's `PlayerBlockBreakEvents.BEFORE` and NeoForge's `BreakBlockEvent` reject the shared server policy for survival/adventure players. NeoForge also requests the corrective block update. |
| Player block placement | Shared `BlockItem.place` mixin rejects survival/adventure placement without consuming the item. Shared bucket and ignition-item mixins also prevent fluid pickup/placement and flint-and-steel/fire-charge placement from bypassing the policy. |
| Farmland trampling by players | Shared `FarmlandBlock.turnToDirt` mixin rejects survival/adventure conversion while preserving vanilla fall damage. Mob trampling is already prevented by mob griefing. |
| Owner map editing | Creative players bypass breaking, placement and player-trampling protections. |
| Weather and day/night | Unchanged; no gamerules are modified for these systems. |

`/genshin arena` is deliberately destructive within its fixed volume: 48×48 walkable interior, a one-block polished-andesite border over a smooth-stone floor, five blocks of cleared height, and a total write volume of 50×50×6 = 15,000 positions. It is centred on the executing player's horizontal block coordinates; the floor is one block below their feet, clamped to build height. It enables managed mode, teleports that player to the exact centre and changes them to adventure mode. Block side effects are suppressed during construction so clearing containers/unstable blocks cannot cascade outside the volume. Use only generated/disposable development worlds, never the purchased map archives.

Vanilla damage/knockback, inventory/hotbar, durability and traversal are not replaced here; they remain assigned to later gameplay slices. TNT and player/block-sourced explosions are outside the mob-explosion protection.

## 2026-10-04 — plain-Java starter combat rules

The `common` rules package imports only `java.*`: `Frames`, `EventTimeline`, `Stats`, `Damage`, `Element`, `Reaction`, `AuraState`, `ApplicationIcd`, `ReactionDamageIcd`, `Energy` and `CharacterBaseStats`. No Minecraft/loader/Mojang classes cross this boundary. Time is a nonnegative `long` count of 60 fps reference frames; one 20 TPS server tick equals exactly three frames. The single-threaded server-owned timeline is a min-heap ordered by `(frame, insertionSequence)`. An adapter drains `advanceTo(Frames.atServerTick(tick))`; a hit at frame 23 retains that timestamp/order even though its world-side callback executes during tick 8. Cooldown ready times remain plain `long`s rather than a wrapper around one value.

Keep a separate `AuraState` per target and application ICD by attacker UUID, tag, target UUID and rule type. Elemental damage does not automatically imply gauge application: callers check ICD and pass the sourced untaxed gauge. Auras store post-tax gauge and independent decay rates; Frozen has separate nonlinear state. Reaction results include type, consumed gauge and trigger/aura elements. EC coexistence returns a next-tick frame; callers enqueue it on the timeline, call `tickElectroCharged(frame, damageAccepted)`, then schedule the next returned frame. Drain pending ticks before later hit/state mutations. The adapter owns the last relevant applier's EM/owner snapshot; zero-gauge hits do not transfer it, and EC ticks are not new generic reaction triggers.

Damage percentages are fractions, weapon ATK participates inside ATK%, and seeded crit takes a caller-owned `RandomGenerator`; expected crit is a separate mode. Ordinary transformative damage is a separate noncritical instance that bypasses DEF and ordinary damage bonuses and selects its own resistance element. K(L) contains only sourced character levels 1–20 and 90; missing levels are rejected, never interpolated. Starter base-stat constants preserve published rounding and use 20/20 before ascension, without progression boosts. Energy is the sourced single-party table, not a guessed co-op range model.

The fidelity ledger distinguishes ordinary implemented rules from unresolved-source adaptations: configurable approximate EC damage cooldown and integer-frame final-tick ordering; current-summary strong-aura duration; non-additive repeat-Frozen extension; fresh-only resistance history when resistance is nonzero; explicit caller-supplied pre-Shatter consumption and configurable Shatter owner scope. Do not turn those choices or rounded character stats into exact live-game golden data. Geo/Dendro, conditional Lunar/Stellar reactions, arbitrary-input Swirl-spread gauge, shields/innate/self auras, hitlag, collision/poise conversion and environmental/co-op ownership are outside this sourced starter layer.

Common JUnit runs on the same 5.10.0 Jupiter BOM used by the convention plugin's Fabric loader-JUnit dependency, without bootstrapping Fabric in plain rules tests. The root `build` explicitly depends on the common `test` task; golden/boundary tests cite the mechanics spec tables, preserve the finite standard ICD sequence, and keep conflicting values out of exact expectations.

