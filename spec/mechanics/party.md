# Party, switching, resonance, and co-op roster rules

Reference target: **Genshin Impact Version 7.1**, pinned in [the project brief](../../docs/GENSHIN_MINECRAFT_BRIEF.md#fixed-decisions). Ordinary overworld/combat parties are covered here; special event switching rules and quest trial-slot exceptions are not normal defaults.

**Evidence status:** values are sourced from current community wiki descriptions and KQM, not measured in the pinned release during this assignment. Several wiki pages were readable only through indexed search excerpts. Historical tests and known outdated summaries are explicitly separated; no undocumented version change is assumed. A missing exact value is `unknown`.

## Party capacity and switching

| Rule | Value / behavior | Source |
| --- | --- | --- |
| Normal solo party capacity | Up to 4 characters | [Party](https://genshin-impact.fandom.com/wiki/Party) |
| Normal solo active character | 1 character on-field at a time | [Party](https://genshin-impact.fandom.com/wiki/Party); remaining roster members are off-field |
| Normal character-switch cooldown | 1 s | [Party](https://genshin-impact.fandom.com/wiki/Party), [Cooldown — Character Switching Cooldown](https://genshin-impact.fandom.com/wiki/Cooldown#Character_Switching_Cooldown) |
| Switch cooldown category | Separate from the outgoing/incoming character's Skill and Burst cooldowns | [Cooldown](https://genshin-impact.fandom.com/wiki/Cooldown); do not treat changing character as recasting or resetting those talents |
| Action restrictions | Animation state can disallow or defer a switch; charged attacks and Burst recovery have character-specific first-switch frames | [KQM Frames — Normal/Charged Attacks](https://library.keqingmains.com/combat-mechanics/frames#normal-and-charged-attack), [Burst idle-frame evidence](https://library.keqingmains.com/evidence/combat-mechanics/frames#burst-idle-iframes) |
| Fallen selection | A fallen character cannot be switched in until revived | [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character) |

The switch cooldown is a rate limit on switching, **not** the input-to-character-appearance latency. [KQM Frames](https://library.keqingmains.com/combat-mechanics/frames#swap) gives approximate switch latency as **network latency + 90 ms**, based on [original testing](https://library.keqingmains.com/evidence/combat-mechanics/frames#ping-vs-swap-delay-interaction) and its [measurement sheet](https://docs.google.com/spreadsheets/d/17WKudZk8pcy0iqbsniKC1Wybr-HWj514Y65xKzpzau8/edit). The original entry was last tested in [Version 5.1](https://library.keqingmains.com/evidence/combat-mechanics/frames#ping-vs-swap-delay-interaction), not the pinned release. The entry explicitly corrects older **network latency + 50 ms** and **twice network latency** approximations from different documentation ([KQM evidence](https://library.keqingmains.com/evidence/combat-mechanics/frames#ping-vs-swap-delay-interaction)); these are not extra switch cooldowns. Exact pinned-release latency, timer-start boundary, and blocked-input buffering are `unknown`.

## State retained when changing active character

Switching is a change of active roster member, not a rebuild of each member's combat state.

| State | Persistence / transition rule | Evidence and limits |
| --- | --- | --- |
| Skill/Burst cooldowns | Keep each character's cooldown state and let it count down off-field; switching does not reset or freeze it | [KQM cooldown model](https://library.keqingmains.com/combat-mechanics/cooldowns), [KQM Kaeya rotation guide](https://keqingmains.com/q/kaeya-quickguide/). **[INFERENCE]** off-field countdown is supported by ordinary repeatable multi-character rotations; the readable cooldown page does not explicitly state this sentence, and no standalone pinned-version switch/countdown test was found. Exact pause/hitlag interactions are not inferred here. |
| Burst energy | Each character retains its own energy meter; being off-field does not prevent particle/orb energy gain | [KQM Energy](https://library.keqingmains.com/combat-mechanics/energy#off-field-vs-on-field); death is an explicit reset, covered below |
| HP and fallen state | Remain character-specific; changing active character is not a heal or revival | [Party](https://genshin-impact.fandom.com/wiki/Party), [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character); off-field healing/damage can independently change HP |
| Stamina | The same player-party pool remains; no refresh on switch | [Stamina](https://genshin-impact.fandom.com/wiki/Stamina); see [stamina.md](stamina.md) |
| Elemental resonance | Determined by party composition, not which member is on-field; retains effect even with fallen roster members | [Team Bonus — Elemental Resonance](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance) |
| Created effects / buffs | Ability-specific, not a blanket deletion or blanket persistence rule | [KQM On-Field and Off-Field](https://library.keqingmains.com/combat-mechanics/damage/other/on-field-and-off-field), [KQM Frames](https://library.keqingmains.com/combat-mechanics/frames#hitlag-extension). Deployables, character-bound buffs, and effects explicitly ending on field exit must retain their own rules. |

The general party layer must not reset character cooldowns, HP, energy, or player stamina merely because the active slot changes. It also must not manufacture universal rules for aura/status persistence, snapshotting, buff termination, or deployable ownership; exact behavior is kit/effect-specific. Generic player-aura transfer behavior on switching is `unknown` in this research.

## Off-field energy from particles and orbs

[KQM Energy](https://library.keqingmains.com/combat-mechanics/energy) distinguishes:

- **Particle/orb energy:** apply the receiving character's element relationship, on/off-field multiplier, and that character's own Energy Recharge.
- **Flat energy:** not multiplied by Energy Recharge; recipients are specified by the source effect, not automatically the whole party.
- **Normal/charged-attack hit energy:** only the on-field character receives this method's flat energy; off-field party members do not share it ([KQM Energy — Auto Attacking](https://library.keqingmains.com/combat-mechanics/energy#auto-attacking)).

Do not give an off-field member a fraction of the active character's final energy gain: it receives a fraction of **its own hypothetical on-field gain**. The particle element can match the off-field member and not the collector ([KQM Energy — Elemental vs Clear](https://library.keqingmains.com/combat-mechanics/energy#elemental-vs-clear)).

### Conversion table for a normal full solo party

Values are **energy units per collected item before the recipient's Energy Recharge**, for a [4-character party](https://library.keqingmains.com/combat-mechanics/energy#table-of-energy).

| Collected item relative to recipient | Recipient on-field (energy units/item) | Recipient off-field (energy units/item) | Source |
| --- | --- | --- | --- |
| Same-element particle | 3.0 | 1.8 | [KQM Energy table](https://library.keqingmains.com/combat-mechanics/energy#table-of-energy) |
| Different-element particle | 1.0 | 0.6 | [KQM Energy table](https://library.keqingmains.com/combat-mechanics/energy#table-of-energy) |
| Clear/non-elemental particle | 2.0 | 1.2 | [KQM Energy table](https://library.keqingmains.com/combat-mechanics/energy#table-of-energy) |
| Same-element orb | 9.0 | 5.4 | [KQM Energy table](https://library.keqingmains.com/combat-mechanics/energy#table-of-energy) |
| Different-element orb | 3.0 | 1.8 | [KQM Energy table](https://library.keqingmains.com/combat-mechanics/energy#table-of-energy) |
| Clear/non-elemental orb | 6.0 | 3.6 | [KQM Energy table](https://library.keqingmains.com/combat-mechanics/energy#table-of-energy) |

Each resulting value is multiplied by the **receiving character's ER multiplier**, not the generator's ER ([KQM Energy — Energy Recharge](https://library.keqingmains.com/combat-mechanics/energy#energy-recharge)). Being on-field matters at **collection**, not particle creation; switching to the intended recipient before collection is energy funneling ([KQM team-building guide](https://keqingmains.com/misc/team-building/)).

| Characters in that player's available party (count) | Off-field fraction of the recipient's on-field particle/orb energy (%) | Source |
| --- | --- | --- |
| 4 | 60 | [KQM Energy](https://library.keqingmains.com/combat-mechanics/energy#off-field-vs-on-field) |
| 3 | 70 | [KQM Energy](https://library.keqingmains.com/combat-mechanics/energy#off-field-vs-on-field) |
| 2 | 80 | [KQM Energy](https://library.keqingmains.com/combat-mechanics/energy#off-field-vs-on-field) |

In co-op, this count is the number of characters **that player controls**, not the combined world roster ([Energy — Particles and Orbs](https://genshin-impact.fandom.com/wiki/Energy#Particles_and_Orbs)). Thus the solo-full-party off-field multiplier must not be imposed on the host's smaller controllable co-op party. Exact co-op collection radius, cross-player particle routing, and dead-character particle eligibility are `unknown` here; these are not established by the solo conversion table.

## Elemental resonance: requested pairs

Normal resonance requires a **full 4-character party** and at least **2 characters of the relevant element** ([KQM resonance](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-resonance), [Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance)). It uses party membership rather than active-character count. Extra same-element members do not strengthen the same resonance; distinct qualifying resonances can coexist ([Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance)). A same-element pair alone in an incomplete party is not evidence of resonance activation.

| Resonance | Required composition | Effects with units/conditions | Source |
| --- | --- | --- | --- |
| Impetuous Winds | At least 2 Anemo in a full party | Stamina consumption **−15%**; Movement SPD **+10%**; Elemental Skill and Burst cooldowns **−5%** | [KQM resonance](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-resonance#list-of-elemental-resonances), [KQM cooldowns](https://library.keqingmains.com/combat-mechanics/cooldowns#list-of-cooldown-reduction-sources). Cooldown reduction affects talents, not the switch cooldown; gliding SPD is excluded by [KQM movement](https://library.keqingmains.com/general-mechanics/movement-and-physics#movement-speed). |
| Fervent Flames | At least 2 Pyro in a full party | Cryo affects party characters for **40% less time**; ATK **+25%** | [KQM resonance](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-resonance#list-of-elemental-resonances), [Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance) |
| Shattering Ice | At least 2 Cryo in a full party | Electro affects party characters for **40% less time**; **+15 percentage points CRIT Rate** against opponents Frozen or affected by Cryo | [KQM resonance](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-resonance#list-of-elemental-resonances), [Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance). Conditional enemy-aura check, not unconditional sheet CRIT Rate. |
| High Voltage | At least 2 Electro in a full party | Hydro affects party characters for **40% less time**; qualifying reactions have **100% chance** to generate **1 Electro particle**, with **5 s cooldown** | [Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance), [Electro](https://genshin-impact.fandom.com/wiki/Electro#Elemental_Resonance). Current qualifying list and stale-source conflict below. |

“Affected for less time” is a **status-duration reduction on the party's characters**, not that element's damage resistance or an enemy-aura duration modifier. Pyro resonance is an ATK-percentage bonus, not a generic damage multiplier ([Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance)).

### Electro resonance version history and stale-source conflict

The current wiki's trigger list is **Superconduct, Overloaded, Electro-Charged, Lunar-Charged, Quicken, Aggravate, and Hyperbloom** ([Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance), [Electro](https://genshin-impact.fandom.com/wiki/Electro#Elemental_Resonance)).

| Source/version state | Listed qualifying reactions | Consequence |
| --- | --- | --- |
| KQM resonance summary currently retrieved | Superconduct, Overloaded, Electro-Charged only | [KQM table](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-resonance#list-of-elemental-resonances) is incomplete compared with the current wiki; do not silently use it as the pinned trigger list. |
| Version 3.0 change | Added Quicken, Aggravate, Hyperbloom | [Team Bonus — Change History](https://genshin-impact.fandom.com/wiki/Team_Bonus#Change_History) |
| Version 5.8 change | Added Lunar-Charged | [Team Bonus — Change History](https://genshin-impact.fandom.com/wiki/Team_Bonus#Change_History) |
| Current wiki description | Includes all reactions listed above | [Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance); no pinned-version direct test found |

Do not add other reactions merely because they involve Electro: the retrieved current description does **not** list Stellar-Conduct. Whether any undocumented pinned-release interaction adds it is `unknown`; this research found no basis to include it.

[KQM](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-resonance#swirl-chain-reactions-can-trigger-electro-resonance) says Swirl-induced qualifying secondary reactions can also activate High Voltage, sharing the usual generation cooldown. [KQM](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-resonance#cryo-resonance-applies-crit-rate-for-cryo-before-reactions) says Cryo resonance checks the target's Cryo aura before the hit's reaction removes it. These interaction descriptions are not independently re-tested for the pinned version.

The requested pair table intentionally excludes other elemental resonances and separate Team Bonus mechanics. A complete all-element implementation must source those independently rather than interpret their omission here as “no effect.”

## Character death and forced replacement

| Condition | Party behavior | Source / exact unknowns |
| --- | --- | --- |
| Active character falls with another living controllable member | Death animation, then forced switch to a surviving member; fallen member remains unavailable until revived | [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character), [Invincibility Frame — HP Locking](https://genshin-impact.fandom.com/wiki/Invincibility_Frame#HP_Locking) explicitly identifies a forced switch due to death. Exact surviving-slot selection order and override of an existing switch cooldown are `unknown`. |
| During death animation | Manual character switching is unavailable | [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character); animation duration is `unknown` and must not be assumed instantaneous |
| Character dies | Its Burst energy resets to 0 energy units | [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character) |
| After death-forced switch | Wiki reports approximately 3 s of HP-locking invulnerability | [Invincibility Frame — HP Locking](https://genshin-impact.fandom.com/wiki/Invincibility_Frame#HP_Locking), with [linked forced-switch clip](https://static.wikia.nocookie.net/gensin-impact/images/8/8b/Forced_Switch_I-Frame.mp4/revision/latest?cb=20240128150307). Approximate community value, not a measured pinned-release boundary. |
| Entire solo party falls | No living switch target; ordinary Revive returns party to nearest Teleport Waypoint with 35% HP | [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character); special domains/events can have different restart handling |
| Fallen member remains in roster | Its presence does not remove ordinary elemental resonance | [Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance) |

A forced switch may only select another character **controlled by that player**. It must not hand over another co-op player's character. Detailed co-op all-controlled-members-dead respawn/spectator handling is `unknown` here; the solo-party-wipe rule must not be assumed identical.

Drowning is separate from ordinary combat death: party-wide HP/energy penalties and the safe-location return are documented in [stamina.md](stamina.md), sourced from [Fallen Character](https://genshin-impact.fandom.com/wiki/Fallen_Character).

## Co-op character limits

Co-op supports up to **4 players**, with a combined cap of **4 selected characters** across players, not a separate full solo roster for everyone ([Co-Op Mode — Rules during Co-Op Mode](https://genshin-impact.fandom.com/wiki/Co-Op_Mode#Rules_during_Co-Op_Mode)).

| Players present (count) | Host controlled characters (count) | Guest controlled characters (count per guest) | Combined roster cap (characters) | Source |
| --- | --- | --- | --- | --- |
| 1, normal solo reference | 4 | Not applicable | 4 | [Party](https://genshin-impact.fandom.com/wiki/Party) |
| 2 | 2 | 2 | 4 | [Co-Op Mode](https://genshin-impact.fandom.com/wiki/Co-Op_Mode#Rules_during_Co-Op_Mode) |
| 3 | 2 | 1 | 4 | [Co-Op Mode](https://genshin-impact.fandom.com/wiki/Co-Op_Mode#Rules_during_Co-Op_Mode) |
| 4 | 1 | 1 | 4 | [Co-Op Mode](https://genshin-impact.fandom.com/wiki/Co-Op_Mode#Rules_during_Co-Op_Mode) |

Each player operates its own active character simultaneously; switching, where available, is only within that player's allocated roster. Selected characters can be changed through Party Setup while not in combat ([Co-Op Mode](https://genshin-impact.fandom.com/wiki/Co-Op_Mode#Rules_during_Co-Op_Mode)). Players with only a single allocated character have no alternative slot for ordinary character switching.

Elemental resonance works in co-op using the combined party composition, and does not require every party member to be alive ([Team Bonus](https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance)). The aggregate roster used for resonance is **not** the per-player roster count used for off-field energy conversion ([Energy](https://genshin-impact.fandom.com/wiki/Energy#Particles_and_Orbs)). Exact roster migration when players join/leave, which slots get removed, and preservation of removed-character cooldown state across Party Setup edits are `unknown`; switching between already allocated members is not the same operation as editing the roster.

## Unknowns, conflicts, and least reliable values

- **Known stale-source conflict:** KQM's retrieved Electro-resonance list omits Dendro-related and Lunar-Charged additions documented by the current wiki. Historical changes are linked above, rather than silently mixing versions.
- **Least reliable numeric values:** the approximate switch-latency intercept and death-forced-switch invulnerability duration; both lack a direct pinned-release measurement. Switch latency is not switch cooldown.
- **Least directly evidenced behavior:** off-field Skill/Burst countdown is supported by the cooldown model and practical rotation documentation, but the exact standalone assertion lacks a retrieved primary switch/countdown test; the inference is marked in the persistence table.
- **Remaining unknowns:** switch cooldown start boundary and input buffering, forced-replacement slot order/cooldown interaction, exact death timing, generic aura transfer, dead-character energy eligibility, co-op collection routing/range and wipe handling, join/leave slot migration, and any undocumented newer Electro-resonance trigger.
- **More consistently documented values:** normal party capacity, co-op allocations, switch cooldown, ordinary particle/orb conversion table, and the requested resonance magnitudes. These remain sourced community descriptions, not newly verified pinned-release measurements.

No game measurements, builds, or tests were performed for this research.
