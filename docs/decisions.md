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


## 2026-10-04 — server-owned Anemo Traveler and managed combat controls

The first playable character is **Aether (Anemo), level 20/20, ascension 0, C0, talent level 1**. `TravelerAnemoKit.STARTER_LEVEL = 20` selects the published rounded `CharacterBaseStats` entry: HP 2342.39, character ATK 45.75 and DEF 147.01. The one-character party has Traveler as its active character. A fixed **23 base-ATK training sword** is a temporary equipment adaptation, independent of the held Minecraft item; total starter ATK is 68.75. Weapon progression, artifacts, passive/quest unlocks and character switching are not granted by this slice. Individual talent hits roll the sourced 5% CRIT Rate / 50% CRIT DMG with a server-owned seeded generator, not expected-average crit.

Each server owns one ordered `EventTimeline` shared by its players, preserving source frame ordering even when different players' hits land during one Minecraft tick. Players send only attack/skill press and release or burst press: no claimed damage, target, resource balance or timestamp. The server checks managed mode, alive/non-spectator status, animation recovery, cooldown and energy before starting an action. Pending casts cannot cross dimensions or survive removal of their owning session.

Normal hitmarks use the first Aether original trials **13/13/16/30/25 frames**; next-normal recovery uses the separately labeled TCL **23/32/40/49/81 frames**. The exact combo timeout is unsourced: choose a named **90-frame window after recovery**, not an exact Genshin claim. Holding attack converts after the sourced normal-to-charged **28/28/36/45** transition for normals 1–4; charged hits use the measured **10-frame first hit, then 11-frame offset**, and 55-frame recovery. It adds the normal preceding the charged attack, and skips the sourced 20-stamina cost until item 8.

Palm Vortex uses the sourced cutting schedule **21/30/51/60/81/90**, Initial/Max Cutting 12%/16.8%, Initial/Max Storm 176%/192%, and absorbed hits at 25% of their Anemo multiplier. Tap storm lands at source frame 32 with cooldown starting at frame 27 (5 seconds). Exact hold-stage thresholds and release hitmarks are unknown: the adaptation selects strong release at **30 held frames**, auto-release at **108 frames**, and storm **5 frames after hold release**, followed by 48 frames of recovery. Strong hold cooldown is 8 seconds from release; it grants a deterministic **3 particles** rather than inventing an engine probability from the empirical 3:4-particle 2:1 observations. Tap grants 2; only the final storm hitting an enemy grants energy, directly using the same-element on-field `Energy` table (3 each). Absorption locks Cryo/Frozen > Pyro > Hydro > Electro from target auras before Anemo consumption; the first cutting hit has no absorbed damage. Own/self aura sampling and the original frame-rate-dependent absorption distribution are not fabricated.

Gust Surge costs **60 energy**, starts its **15-second cooldown at cast**, travels for **6 seconds**, and has the original Aether **96-frame first tick**, followed by eight 30-frame-spaced ticks (last at frame 336). The Wiki's 2-second activation-relative first-tick claim remains separate. Burst Anemo/absorbed multipliers are **80.8%/24.8%**, gauges 1U/2U and separate standard ICD groups. Stats are immutable for this starter so burst snapshots naturally retain the cast stats. Cutting, burst and absorbed tags use the rules ICD; storms have no application ICD. Swirl and other returned ordinary reactions are separate `Damage` transformative instances, respecting reaction damage ICD; EC follow-up ticks retain the last relevant owner snapshot. Arbitrary Swirl-spread gauge remains unsourced and is not guessed.

Burst energy is reserved/drained immediately when the server accepts the cast rather than three animation frames later in the original Aether trials. This deliberate input/resource adaptation prevents duplicate intents from spending the same balance. Tornado pull/particle updates run at most once per reference-frame boundary, so repeated packets within a server tick cannot amplify movement.

Temporary geometry in Minecraft blocks: normal/charged horizontal sword radius **3**, total arc **120°**, vertical query ±2; Palm radius **4**, forward half-circle; tornado radius **3** with absorbed radius **1.5**, moving **2 blocks/second** along cast-facing. Small enemies mean bounding width ≤1.4 and height ≤2.2, pulled toward the tornado each server tick. These are named geometry/weight substitutions, not sourced Genshin distances or lift/poise rules. Vanilla sweep/cloud and element-coloured dust make hits and absorption visible; no external assets were added.

Every hit non-player living entity receives a temporary level-20 profile with **max vanilla health ×100 Genshin HP**, equal-level DEF from the sourced damage formula, **10% default RES**, its own aura and ICD state. Fractional Genshin HP is mirrored to vanilla health; zero uses a normal player-attributed damage source for death and loot credit. Profiles are discarded when the entity is removed. Incoming/environmental damage to players still uses vanilla processing and is imported as a proportional Genshin HP change, then mirrored back; this is not an incoming Genshin damage formula. Character state is in memory only: relog, respawn and managed-off reset energy/cooldown/combo state; persistence is not promised yet.

**Key conflict decision:** in managed gameplay the registered `Genshin` category's **Elemental Skill (E)** and **Elemental Burst (Q)** own their bound keys; the matching vanilla inventory/drop clicks are consumed before vanilla handling. Rebinding inventory/drop away from the skill/burst key leaves those vanilla functions available. Vanilla attack becomes normal/charged intent and cannot also perform melee or block breaking; both loaders reject vanilla player melee server-side even from an unmodified/misbehaving client. Outside managed worlds vanilla E/Q/melee behaviour is unchanged. Screens stop held actions rather than casting through menus.

The managed client's attack binding is kit-owned in creative mode too, as required by the managed-input cutover. The older server block-modification policy still permits creative edits, but normal mouse-based block breaking with this client requires `/genshin managed off`; switching off also restores vanilla E/Q and melee. Placement remains subject to the existing creative bypass.

The item-6 HUD replaces the temporary action-bar status line entirely. A small S2C character payload carries the managed flag, HP/max HP/energy floats and remaining-frame varints, sent only when that state changes (cooldowns therefore change each server tick while running), and refreshed across dimension changes. Dedicated-server paths never reference client input or rendering classes.

Queued EC callbacks additionally require that their captured target profile is still the current UUID-map entry. Managed-off clears that map without advancing the combat clock; a later managed-on must not replay EC damage belonging to discarded aura/owner state. A shared managed off/on GameTest covers this lifetime boundary on both loaders.

## 2026-10-04 — managed-world HUD and tracked combat feedback

Use one shared client renderer with thin **Fabric `HudElementRegistry`** and **NeoForge `RegisterGuiLayersEvent` / `RenderGuiLayerEvent.Pre`** adapters for 26.2. The right-hand party list has space for four rows; today's one-member synced state supplies active Anemo Traveler, with element-coloured name/accent, HP bar and burst-energy bar. Bottom-centre HP shows numbers, above the retained vanilla hotbar. Bottom-right original stepped-diamond E/Q icons use rebound key labels, cooldown text and a full-energy burst bar; Q receives a gold border/READY label when both energy and cooldown permit a cast. No copied game artwork or new external assets.

Managed mode suppresses vanilla hearts, hunger, armor, the contextual/XP bar and XP level. The contextual layer also owns the mount-jump display in 26.2, so that display is hidden with it. Crosshair, hotbar/inventory, air, mount health, boss bars, chat and other overlays remain vanilla. Outside managed mode the original layers run unchanged. F1 hides custom HUD and world indicators as well; no actionbar fallback.

Cooldowns round **up**, not to nearest: positive values cannot show zero/ready early. Below ten seconds show one decimal; at the rounded ten-second boundary and above show whole seconds (e.g. 594 frames → 9.9, 595–600 → 10, 601 → 11). HP/energy fractions clamp to [0,1]. Pure JUnit boundary tests cover these choices.

Each accepted damage instance sends target entity ID, float amount, element, optional reaction and the actual server crit roll only to players tracking the target. Transformative damage stays noncritical; non-damaging/ICD-suppressed reaction triggers can send zero amount solely to announce their name. Client billboard text is anchored at the hit position, rises 0.7 blocks and fades over 20 client ticks (~1 second), with larger crit numbers. Three 32-pixel billboard lanes separate simultaneous talent/Swirl/absorbed damage; reaction names start 0.9 blocks higher, separate from the aura indicator. Lane cycling has a pure regression test after initial screenshots exposed overlapping hits. Aura element bitsets are sent only when the visible set changes (including natural expiry), plus an initial refresh on tracking an existing Genshin target. Frozen uses the Cryo indicator; bracketed coloured element initials are original placeholders. No entity receives an indicator merely because it is a vanilla mob.

Both loaders extract immutable world-text snapshots before submitting via Minecraft 26.2's `SubmitNodeCollector`; render callbacks never read live entities. Visual state clears on managed-off, disconnect or dimension/world change and prunes removed/untracked aura entities. Fabric's dedicated-server client GameTest seeds a Pyro-aura test cow in the managed arena, presses E, checks synced cooldown/damage/party state, then grants burst energy on the server to capture readiness. It writes aura, skill-hit, burst-ready and unmanaged-vanilla screenshots under the client's run-directory `screenshots/`. The convention's dev-only recording widget is removed from this test so it cannot cover numeric HP/READY; recording and step logs are retained.

Verification: `./gradlew --no-daemon build runAllGameTests` passed after visual-layout fixes in **3m 7s**. All **16 required server GameTests** passed in each Fabric/NeoForge development and production run, the Fabric production client input/state/screenshot GameTest passed, and the three HUD formatting/layout unit tests had zero failures/errors. Inspected the generated `fabric/build/run/clientGameTest/screenshots/genshin-hud-aura.png`, `genshin-hud-skill.png`, `genshin-hud-burst-ready.png` and `genshin-hud-vanilla.png`: party/HP/icons/countdown and separated damage/Swirl/aura text are visible, READY has a full energy bar/gold outline, and unmanaged capture restores hearts/hunger with no custom overlays. Screenshots remain git-ignored run artifacts.

