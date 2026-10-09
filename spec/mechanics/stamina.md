# Stamina, traversal, and fall damage

Reference target: **Genshin Impact Version 7.1**, as pinned in [the project brief](../../docs/GENSHIN_MINECRAFT_BRIEF.md#fixed-decisions). Research concerns ordinary playable-character movement, not character-specific traversal skills, alternate sprints, or underwater Aquatic Stamina unless explicitly distinguished.

**Evidence status (2026-10-09):** [datamine.md](datamine.md#traversalstamina-constants-found) now pins7.1 numeric constants confirming100base/140extra,18dash/start,1.5s recovery wait,5climb-entry,25climb-jump and3glide cost. No number changes follow. Wiki/KQM remain behaviour/rate sources; held-sprint18/s,regen25/s, exact speeds/quantization/billing boundaries are not independently recovered. Older experiments remain historical evidence, not pinned measurements.

## Capacity and Statue progression

**Correction to the assignment's “base 240”:** the wiki distinguishes a new account's **100 stamina base** from the **240 stamina maximum after progression** ([Stamina — Maximum Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina)). Giving an unprogressed Minecraft account the fully upgraded maximum would be a project adaptation, not the researched Genshin starting value.

Stamina belongs to the player's party, not individual characters. Switching does not refill it or exchange it for a different character's pool ([Stamina](https://genshin-impact.fandom.com/wiki/Stamina)).

| Capacity property | Value and unit | Source / qualification |
| --- | --- | --- |
| New-account base | 100 stamina units | [Stamina — Maximum Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| Progressed hard maximum | 240 stamina units | [Stamina — Maximum Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| Maximum contribution from a nation's completed Statue progression | +70 stamina units | [Stamina — Maximum Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina); regional progression, not a separate bonus from every physical statue |

Offering the nation's Oculi upgrades its shared Statue progression; do not give independent stamina bonuses for each statue location ([Statue of The Seven](https://genshin-impact.fandom.com/wiki/Statue_of_The_Seven)). The following is the wiki's table, not an interpolated progression:

| Regional Statue level (dimensionless) | Cumulative maximum-stamina bonus (stamina units) | Source |
| --- | --- | --- |
| 1 | +0 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 2 | +7 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 3 | +14 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 4 | +22 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 5 | +30 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 6 | +38 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 7 | +46 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 8 | +54 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 9 | +62 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |
| 10 | +70 | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Maximum_Stamina) |

**Version change:** before [Version 5.0](https://genshin-impact.fandom.com/wiki/Stamina/Change_History), stamina progression came only from Mondstadt and Liyue. That update allowed other nations' Statue upgrades to contribute, retroactively, without raising the [240-unit cap](https://genshin-impact.fandom.com/wiki/Stamina/Change_History). Do not retain the obsolete “only Anemo/Geo statues increase stamina” rule for the pinned version. Exact Oculi offering counts are nation-specific and outside this stamina table.

## Regeneration and ordinary action costs

These are unmodified costs, before talent, resonance, constellation, and food reductions.

| Action/property | Cost/rate/delay with units | Source / limitations |
| --- | --- | --- |
| Natural regeneration | 25 stamina units/s | [Stamina introduction](https://genshin-impact.fandom.com/wiki/Stamina); conflicts with an older KQM calculation below |
| Delay before natural regeneration | 1.5 s after stopping stamina-using actions | [Stamina introduction](https://genshin-impact.fandom.com/wiki/Stamina); not a universal assertion that recovery is permitted while clinging to a wall or floating in water |
| Ordinary dash / sprint start | 18 stamina units per dash | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption), [Sprinting](https://genshin-impact.fandom.com/wiki/Sprinting); separate from held-sprint drain |
| Held ordinary sprint | 18 stamina units/s | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption); also used in [KQM's original Lumenstone test](https://library.keqingmains.com/evidence/general-mechanics/overworld#blooming-light-stamina-regeneration), with [recorded evidence](https://youtu.be/t4rjtB6m15U) |
| Normal climbing movement | `unknown` stamina units/s | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption) explicitly lists the rate as unknown |
| Minimum stamina to start climbing | 5 stamina units | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption); eligibility threshold, **not** a continuous drain rate |
| Climb-jump | 25 stamina units per jump | Pinned7.1 `CLIMB_JUMP_COST_STAMINA`; [file/commit citation](datamine.md#traversalstamina-constants-found). Supersedes wiki citation-needed/historical24 numeric uncertainty. Formula tuple semantics remain unverified. |
| Ordinary gliding | 3 stamina units/s | Pinned7.1 `FLY_COST_STAMINA=3`; [file/commit citation](datamine.md#traversalstamina-constants-found). Public “while moving” wording does not prove idle-input descent is free. |
| Normal surface swimming | 4 stamina units per movement animation | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption); explicitly animation-based, **not** a fixed per-second cost |
| Surface swim dash, initial | 2 stamina units | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption) |
| Surface swim dash, held movement | 10.2 stamina units/s | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption) |
| Plunging attack | 0 stamina units | [Plunging Attack](https://genshin-impact.fandom.com/wiki/Plunging_Attack); possible after glider exhaustion, but not after climbing-exhaustion free fall |

The precise point at which dash-start cost transitions into held-sprint drain is `unknown`; do not assume the entire dash animation incurs both costs. Stationary climbing/swimming recovery eligibility, partial-cost handling, and drain quantization in the pinned version are `unknown`. Normal swimming cannot be converted into a per-second rate without the relevant character's animation timing.

[KQM's original swimming test](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#optimal-swimming), with [straight-line](https://youtu.be/Nf_TkhpBSR8) and [turning](https://youtu.be/rqcAJtCP0x0) clips, reports extra stamina drain when turning. Its version-specific persistence and the exact extra cost are `unknown`; a constant normal-swim drain alone is not demonstrated fidelity.

### Consumption modifiers relevant to the starter party

| Modifier | Effect | Source |
| --- | --- | --- |
| Kaeya — Hidden Strength | Sprinting stamina consumption −20%; does not stack with passives providing the same effect | [Stamina — Utility Passives](https://genshin-impact.fandom.com/wiki/Stamina#Utility_Passives) |
| Amber — Gliding Champion | Gliding stamina consumption −20%; does not stack with passives providing the same effect | [Stamina — Utility Passives](https://genshin-impact.fandom.com/wiki/Stamina#Utility_Passives) |
| Anemo resonance | Stamina consumption −15% | [KQM resonance](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-resonance#list-of-elemental-resonances); party eligibility in [party.md](party.md) |

Different applicable sources of stamina consumption reduction stack additively according to [Stamina — Reduction Calculation](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption_Reduction_Calculation). Exact category applicability to all exceptional charged attacks is character-specific, not inferred from weapon type.

## Dash invulnerability and exhaustion

Dash invulnerability is **hitbox-removal protection against most attacks**, not unconditional HP-loss immunity. Continuous sprinting must not inherit permanent dash invulnerability ([Invincibility Frame — Hitbox Removal](https://genshin-impact.fandom.com/wiki/Invincibility_Frame#Hitbox_Removal)).

| Evidence | Startup from input | Active protection | Status |
| --- | --- | --- | --- |
| Current community wiki | 40 ms | 300 ms, equivalent to **18 frames at 60 frames/s** | [Invincibility Frame](https://genshin-impact.fandom.com/wiki/Invincibility_Frame#Hitbox_Removal); frame duration is an explicit time-unit conversion, not a new measurement. Exact integer-frame startup is `unknown`; do not round the time value silently. |
| Original KQM preliminary test | Approximately 3 frames at 60 frames/s | Approximately 20 active frames, ±1 frame | [KQM evidence](https://library.keqingmains.com/evidence/combat-mechanics/frames#preliminary-analysis-of-how-invincibility-frames-work), [startup footage](https://drive.google.com/file/d/1gMvqIfks6nwA76bhZvUPEkT8SGmjKR31/view), [active-time footage](https://drive.google.com/file/d/1gUd1kZgV4A6s_4ObSGx8XOYmZN8weAar/view). Historical [Version 1.0](https://library.keqingmains.com/evidence/combat-mechanics/frames#preliminary-analysis-of-how-invincibility-frames-work), not pinned-release verification. |

The original test observed a failed dodge at [2 frames and a success at 4 frames](https://library.keqingmains.com/evidence/combat-mechanics/frames#preliminary-analysis-of-how-invincibility-frames-work) from input, and [success at 24 / failure at 26 frames](https://library.keqingmains.com/evidence/combat-mechanics/frames#preliminary-analysis-of-how-invincibility-frames-work). Those observations and the author's approximate conclusion are preserved as published, not converted into an invented exact inclusive frame interval. **Conflict unresolved:** no evidence establishes whether the difference is a game change or methodology/precision.

| Exhaustion/eligibility case | Behavior or unresolved parameter | Source |
| --- | --- | --- |
| Empty stamina during ordinary sprint | Stamina-dependent sprint/dash cannot continue normally; exact run-transition timing and restart gate are `unknown` | [Sprinting](https://genshin-impact.fandom.com/wiki/Sprinting), [Stamina](https://genshin-impact.fandom.com/wiki/Stamina) |
| Dash restart threshold | Pinned-version threshold is `unknown`; historical KQM math says **cannot dash at or below 15 stamina units**, even with cost reductions | [KQM movement comparison](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#movement-techniques-and-player-model-comparisons); not evidence that the current threshold equals current dash cost |
| Climbing exhaustion | Character falls; subsequent fall damage depends on the fall | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing) |
| Gliding exhaustion | Glider closes; character falls and can take fall damage | [Exploration](https://genshin-impact.fandom.com/wiki/Exploration), [Plunging Attack](https://genshin-impact.fandom.com/wiki/Plunging_Attack) |
| Ordinary surface-swimming exhaustion | Drowning and respawn, not switching to a fresh stamina pool | [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character), [Stamina](https://genshin-impact.fandom.com/wiki/Stamina) |

A separate consecutive-dash gate must not be confused with exhaustion. [KQM's original test](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#consecutive-dash-cooldown-trigger-is-08s), with [video](https://youtu.be/dKiHbJZZA2E), finds a **0.8 s interval** for consecutive dashes to trigger the dash cooldown, and says hitlag extends that interval ([hitlag clip](https://youtu.be/tmIyJPkDFKk)). It was last tested in [Version 3.3](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#consecutive-dash-cooldown-trigger-is-08s); pinned-version trigger boundary and cooldown duration are `unknown`. The interval is not the cooldown duration.

### Drowning and region distinction

The [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character) description says drowning returns the active character to the location where stamina was last full, removes **10% of each party member's maximum HP**, and resets **all party members' energy to zero**. A character reduced to zero HP by this penalty remains fallen. This is party-wide resource loss, not automatically a full-HP death for only the swimmer. Exact safe-position selection and co-op penalty recipients are `unknown`.

Since [Version 4.0](https://genshin-impact.fandom.com/wiki/Swimming), Fontaine's diving-enabled water uses **Aquatic Stamina** instead of the ordinary pool, and depleting it does not cause drowning ([Fontaine](https://genshin-impact.fandom.com/wiki/Fontaine), [Swimming](https://genshin-impact.fandom.com/wiki/Swimming)). Do not apply Mondstadt swimming/drowning rules to that mode. [KQM's original test](https://library.keqingmains.com/evidence/general-mechanics/overworld#swimming-stamina-passives-dont-work-in-fontaine-water), with [clip](https://youtu.be/ZRDJzADdLZ0), also finds ordinary swim-cost passives do not apply there.

## Movement speeds and distance calibration

Genshin metres are not Minecraft blocks. No scale conversion is selected in this research. KQM's [movement comparison](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#movement-techniques-and-player-model-comparisons) establishes character-model and technique differences; its arbitrary distance units do **not** justify universal m/s constants.

| Movement quantity | Pinned-version value (m/s) | Evidence / reason |
| --- | --- | --- |
| Slow walking | `unknown` | [Movement SPD](https://genshin-impact.fandom.com/wiki/Movement_SPD), [KQM movement evidence](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics); no reproducible calibrated pinned-version value found |
| Ordinary running | `unknown` | Same sources; distinguish running from slow walking and from sprint input |
| Held sprint | `unknown` | Same sources; character-model-specific, not a universal constant |
| Initial dash speed / velocity curve | `unknown` | [KQM cliff-dash evidence](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#cliff-dash-displacement) shows velocity depends on dash phase and model |
| Glider forward speed | `unknown` | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding), [KQM movement summary](https://library.keqingmains.com/general-mechanics/movement-and-physics#movement-speed); no calibrated pinned-version measurement found |
| Glider downward descent speed | `unknown` | Historical estimate below is not pinned-version evidence |

A [community glider experiment](https://www.reddit.com/r/Genshin_Lore/comments/13zlgji) reports **2.351 m/s downward**, averaging measured estimates of **2.397, 2.317, 2.350, and 2.340 m/s**. Its calibration assumes the male Traveler is **1.625 m tall** ([original post](https://www.reddit.com/r/Genshin_Lore/comments/13zlgji)). The author explicitly calls for further experimentation. This is an older, assumption-dependent **descent estimate**, not forward speed or a confirmed pinned-release constant; the original post was available through indexed excerpts, while direct Reddit reads were blocked.

Ordinary Movement SPD buffs do not affect gliding speed according to [KQM](https://library.keqingmains.com/general-mechanics/movement-and-physics#movement-speed). Do not apply Anemo resonance's ground-speed bonus automatically to the glider.

## Charged attacks by weapon type

Weapon rows describe typical ordinary kits, not a universal override of every character's talent.

| Weapon/action | Unmodified stamina cost | Source / exceptions |
| --- | --- | --- |
| Sword charged attack | Usually 20 stamina units per attack | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption); this covers ordinary Traveler/Kaeya attacks. Keqing is listed separately at **25 stamina units** by the same source. |
| Bow aimed/charged shot | 0 stamina units | [Charged Attack](https://genshin-impact.fandom.com/wiki/Charged_Attack); aiming/charging on the ground does not consume stamina. Exceptional airborne aiming states are not this rule. |
| Catalyst charged attack | Usually 50 stamina units per attack | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption); ordinary Lisa baseline. Heizou is listed at **25 stamina units** by the same source; character-specific resource/passive exceptions exist. |
| Claymore sustained spin/slash charged attack | Usually 40 stamina units/s | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption), [Charged Attack](https://genshin-impact.fandom.com/wiki/Charged_Attack); drain is sustained, not a lump sum per spin. Exact startup/end billing is `unknown`. |
| Polearm charged attack | Usually 25 stamina units per attack | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption) |

Insufficient-stamina activation behavior, the charge phase at which each attack consumes its cost, and whether any partial spend is permitted are `unknown` at this system level. Those must not be inferred from the weapon table.

## Ordinary fall damage and plunge mitigation

Player fall damage is based on **maximum HP**, not a vanilla fixed-health-per-block rule. The wiki describes dependence on fall height **and horizontal impact velocity**; the exact player formula still needs research ([Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG)). [HP Loss](https://genshin-impact.fandom.com/wiki/HP/HP_Loss) describes ordinary fall loss up to **100% maximum HP** and plunge loss of **10%–40% maximum HP** when damage occurs. Fall HP loss bypasses shields, DEF, RES, and damage reduction according to [HP Loss](https://genshin-impact.fandom.com/wiki/HP/HP_Loss).

| Ordinary-fall parameter | Value | Source / conclusion |
| --- | --- | --- |
| No-damage height threshold (m) | `unknown` | [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG); no exact researched threshold found |
| HP-loss curve by height (% max HP) | `unknown` | [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG); do not invent a linear height curve |
| Guaranteed lethal height (m) | `unknown` | Same source; horizontal velocity and current HP prevent an unsupported universal height assertion |
| Maximum ordinary fall HP loss | Up to 100% max HP per landing | [HP Loss](https://genshin-impact.fandom.com/wiki/HP/HP_Loss) |

The wiki publishes the following **fall-damage reduction**, not an ordinary-fall damage formula:

| Plunge distance from attack initiation (m) | Published fall-damage reduction (%) | Source |
| --- | --- | --- |
| 0–40 | 100 | [Fall DMG — Fall Damage Reduction](https://genshin-impact.fandom.com/wiki/Fall_DMG) |
| 40–60 | 90 | [Fall DMG — Fall Damage Reduction](https://genshin-impact.fandom.com/wiki/Fall_DMG) |
| 60–100 | 80 | [Fall DMG — Fall Damage Reduction](https://genshin-impact.fandom.com/wiki/Fall_DMG) |
| Above 100 | 60 | [Fall DMG — Fall Damage Reduction](https://genshin-impact.fandom.com/wiki/Fall_DMG) |

Measure from **where the plunge starts**, not the beginning of an earlier fall ([Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG)). Exact inclusivity at the overlapping band endpoints is `unknown`. The table's reduction percentages must not silently be reinterpreted as direct percentage-max-HP losses at each boundary; the separate [HP Loss](https://genshin-impact.fandom.com/wiki/HP/HP_Loss) table gives a general plunge HP-loss range but does not supply the missing player formula. Xiao's plunging attacks negate fall damage regardless of height ([Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG)); this exception does not apply to ordinary characters.

The enemy/object collision-damage formula on [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG) is a different mechanic and must **not** be used as a player fall-damage formula. A plunge may be initiated without stamina after glider depletion, but not after climbing exhaustion or airborne interruption ([Plunging Attack](https://genshin-impact.fandom.com/wiki/Plunging_Attack)).

## Explicit historical conflicts and reliability

| Quantity | Current wiki description | Historical KQM movement calculation | Disposition |
| --- | --- | --- | --- |
| Dash cost | 18 stamina units/dash ([Stamina](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption)) | 15 stamina units/dash ([original KQM entry](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#movement-techniques-and-player-model-comparisons)) | Unresolved disagreement; no proven change date |
| Regeneration | 25 stamina units/s ([Stamina](https://genshin-impact.fandom.com/wiki/Stamina)) | 30 stamina units/s ([original KQM entry](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#movement-techniques-and-player-model-comparisons)) | Unresolved disagreement; old calculation is not a pinned-version default |
| Regeneration delay | 1.5 s ([Stamina](https://genshin-impact.fandom.com/wiki/Stamina)) | 1 s ([original KQM entry](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#movement-techniques-and-player-model-comparisons)) | Unresolved disagreement |
| Dash active invulnerability | 300 ms / 18 frames at 60 frames/s ([wiki](https://genshin-impact.fandom.com/wiki/Invincibility_Frame#Hitbox_Removal)) | Approximately 20 frames ±1 at 60 frames/s ([original KQM test](https://library.keqingmains.com/evidence/combat-mechanics/frames#preliminary-analysis-of-how-invincibility-frames-work)) | Unresolved disagreement; startup differs as well |

The **least reliable reported values** are the citation-needed climb-jump cost, the assumption-calibrated historical descent speed, and exact dash protection/restart boundaries. The ordinary fall curve, calibrated ground/glider-forward speeds, and climbing drain remain `unknown`, not approximations. The historical dash/regen calculation is internally unsuitable for choosing modern constants without new evidence. No gameplay measurements, builds, or tests were performed for this research.

## 2026-10-07 recheck: dash cost versus implementation billing

Fresh public [Sprinting](https://genshin-impact.fandom.com/wiki/Sprinting) and [Stamina consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption) retrievals still give **18 units per dash** and **18 units/s held sprint**. Retain these selected values; the historical15-unit discrepancy above is not resolved by subjective feel. The [HUD audit](ui.md#stamina-gauge-and-dash-recheck) traces the runtime's single start charge and no continuous drain through its18-frame dash, with shared server boundary assertions. No billing defect was found or stamina formula/cost changed. A new100-unit pool versus progressed240 and ungranted utility passives remain known distinctions, not proven explanations of this owner's observation. The game's exact dash-to-held-sprint billing boundary remains unknown.

