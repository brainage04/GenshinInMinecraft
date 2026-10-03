# Traveler (Anemo) — starter kit

## Scope and evidence

Target: [Version 7.1, “A Rekviem for the Underworld”](https://traveler.gg/a-rekviem-for-the-underworld-version-7-1-update-details/). Starter baseline is unconstellated, without ascension or story-quest unlocks. No change to these baseline talents was found in the update mirror; the [official endpoint](https://genshin.hoyoverse.com/m/en/news/detail/166383) returned only a site shell. Historical community tests below are not claimed to have been retested in the target release. The later **Foreign Windwrath** unlock already exists in this version and is documented separately, not silently added to a new Traveler.

Weapon: **sword**; element: **Anemo** ([KQM][K]). Aether and Lumine differ in charged scalings, animations, and frame data. All damage multipliers below are **% total ATK**, not base ATK. Gauge units are nominal application gauge before aura tax; an ICD-blocked damage hit still deals its elemental damage.

## Foreign Ironwind

String: [five single-hit normals and a two-hit charged attack costing 20 stamina][W]. Baseline sword damage is Physical, not Anemo; Slitting Wind is a separate ascension hit. The [Wiki][W] lists nominal elemental application properties for sword attacks when externally infused: normal and charged hits share `Normal Attack`, standard [2.5 s / 3-hit ICD](https://library.keqingmains.com/combat-mechanics/internal-cooldown). Do not apply elemental gauge from Physical hits.

| Hit | Talent-level-one multiplier (% ATK) | Baseline element | Baseline gauge (U) | Infused application / ICD | Source |
|---|---:|---|---|---|---|
| First normal | 44.5 | Physical | not applicable | 1 U, Normal Attack, standard | [Wiki][W] |
| Second normal | 43.4 | Physical | not applicable | same | [Wiki][W] |
| Third normal | 53.0 | Physical | not applicable | same | [Wiki][W] |
| Fourth normal | 58.3 | Physical | not applicable | same | [Wiki][W] |
| Fifth normal | 70.8 | Physical | not applicable | same | [Wiki][W] |
| Charged first hit, either sibling | 55.9 | Physical | not applicable | same | [Wiki][W] |
| Charged second hit, Aether | 60.7 | Physical | not applicable | same | [Wiki][W] |
| Charged second hit, Lumine | 72.2 | Physical | not applicable | same | [Wiki][W] |
| Plunge collision | 63.93 | Physical | not applicable | 0 U; no elemental application | [Wiki][W] |
| Low plunge impact | 127.84 | Physical | not applicable | 1 U if infused; No ICD | [Wiki][W] |
| High plunge impact | 159.68 | Physical | not applicable | 1 U if infused; No ICD | [Wiki][W] |

### Available normal talent table

Every numeric cell: [Wiki Foreign Ironwind attribute scaling][W], units **% ATK**. Values are source-displayed precision, not inferred hidden coefficients. Undisplayed higher normal talent levels are **unknown**. [KQM's Full Talent Values][K] charged row is Lumine's, whereas its attack summary uses Aether's charged scalings; this is an unlabeled sibling difference, not grounds for mixing their second hits.

| Hit | L1 | L2 | L3 | L4 | L5 | L6 | L7 | L8 | L9 | L10 | L11 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| First | 44.5 | 48.1 | 51.7 | 56.9 | 60.5 | 64.6 | 70.3 | 76.0 | 81.7 | 87.9 | 94.1 |
| Second | 43.4 | 47.0 | 50.5 | 55.6 | 59.1 | 63.1 | 68.7 | 74.2 | 79.8 | 85.9 | 91.9 |
| Third | 53.0 | 57.3 | 61.6 | 67.8 | 72.1 | 77.0 | 83.8 | 90.6 | 97.3 | 105 | 112 |
| Fourth | 58.3 | 63.1 | 67.8 | 74.6 | 79.3 | 84.8 | 92.2 | 99.7 | 107 | 115 | 123 |
| Fifth | 70.8 | 76.5 | 82.3 | 90.5 | 96.3 | 103 | 112 | 121 | 130 | 140 | 150 |
| Charged, Aether | 55.9 + 60.7 | 60.5 + 65.7 | 65.0 + 70.6 | 71.5 + 77.7 | 76.1 + 82.6 | 81.3 + 88.3 | 88.4 + 96.0 | 95.6 + 104 | 103 + 112 | 111 + 120 | 118 + 128 |
| Charged, Lumine | 55.9 + 72.2 | 60.5 + 78.1 | 65.0 + 84.0 | 71.5 + 92.4 | 76.1 + 98.3 | 81.3 + 105 | 88.4 + 114 | 95.6 + 123 | 103 + 133 | 111 + 143 | 118 + 153 |
| Plunge collision | 63.93 | 69.14 | 74.34 | 81.77 | 86.98 | 92.92 | 101.1 | 109.28 | 117.46 | 126.38 | 135.3 |
| Low impact | 127.84 | 138.24 | 148.65 | 163.51 | 173.92 | 185.81 | 202.16 | 218.51 | 234.86 | 252.7 | 270.54 |
| High impact | 159.68 | 172.67 | 185.67 | 204.24 | 217.23 | 232.09 | 252.51 | 272.93 | 293.36 | 315.64 | 337.92 |

## Palm Vortex

Press produces Initial Storm damage. Holding produces cutting hits, followed by a stronger storm on release; holding longer changes damage and area ([Wiki][W], [KQM guide][G]). Skill damage is dynamic rather than snapshot, including absorbed damage ([original test](https://youtu.be/XbZFc77QXQE), [TCL][T]).

| Damage component | Level-one multiplier (% ATK) | Element | Gauge (U) | ICD tag/type | Source |
|---|---:|---|---:|---|---|
| Initial Cutting, each tick | 12 | Anemo | 1 | Elemental Skill Anemo; 2.5 s / 3 hits | [Wiki][W] |
| Max Cutting, each tick | 16.8 | Anemo | 1 | same shared cutting group | [Wiki][W] |
| Initial Storm | 176 | Anemo | 1 | No ICD | [Wiki][W] |
| Max Storm | 192 | Anemo | 1 | No ICD | [Wiki][W] |
| Absorbed cutting | 25% of corresponding Anemo multiplier | chosen absorbed element | 1 | Elemental Skill Cryo / Electro / Hydro / Pyro, separate elemental tags; 2.5 s / 3 hits | [Wiki][W], [original ratio tests](https://docs.google.com/spreadsheets/d/1uTBPUMtR4bQ_T7QeQc6_JRXQvcYxz1muZXiiSEF-Ze8/edit#gid=0) |
| Absorbed storm | 25% of corresponding Anemo multiplier | chosen absorbed element | 1 | No ICD | same sources |

The absorbed multiplier is not Swirl damage: it scales from ATK, can crit independently, and receives the matching elemental damage bonuses ([guide][G], [original tests][T]). Absorption does not replace the Anemo hits.

### Charge stages, cooldown, particles

| Stage / property | Value | Source |
|---|---|---|
| Press | Initial Storm only; 5 s cooldown; 2 Anemo particles on enemy hit | [Wiki][W], [guide][G] |
| Early hold using only Initial damage | Initial cutting plus Initial Storm; same cooldown/area/particle count as press | [Guide][G] |
| Short strong hold tested by KQM | Two Initial Cutting hits plus Max Storm; 8 s cooldown, 3–4 particles | [Aether short-hold sheet][AS], [guide][G] |
| Full hold Anemo string | Two Initial Cutting, four Max Cutting, one Max Storm | [Wiki][W], [Aether full-hold sheet][AH] |
| Initial cutting timestamps | 0.35 s and 0.5 s from cast start | [Wiki][W] |
| Max cutting timestamps | 0.85 s, 1 s, 1.35 s, 1.5 s from cast start | [Wiki][W] |
| Strong hold particles | 3 or 4, sampled in a 2:1 ratio; rounded mean 3.33, not fractional particles | [Guide][G], [Wiki][W] |
| Particle condition | Final storm must hit an enemy; cutting hits alone do not generate particles | [Wiki][W], [guide][G] |
| Strong hold particle probabilities | Exact engine probabilities unknown; empirical 2:1 ratio is not a proven random-generator specification | [Guide][G] |
| Exact release-to-stage boundary / final auto-release time | unknown | Inspected skill/scaling sources and original sheets |

Do not implement damage as a continuous interpolation between listed charge multipliers. There is a source inconsistency in release wording: the [guide][G] says that releasing turns the last cutting tier into its matching Storm tier, yet the [short-hold original sheet][AS] specifies **two Initial Cutting plus Max Storm**, and [later absorption evidence][T] describes an earlier hold yielding **two Initial Cutting plus Initial Storm**. These imply a time-dependent stage transition that the prose alone does not locate exactly. Preserve observed strings; exact boundary is **unknown**. Dash/jump cancels can lose the storm and all particles if mistimed ([guide][G]).

### Absorption and timing

Absorption priority is **Cryo/Frozen > Pyro > Hydro > Electro**; it can sample enemy auras or Traveler's own aura and locks to the absorbed element for that use ([Wiki][W], [guide][G]). It is independent of which aura a hit Swirls.

Full-hold absorbed hit counts are **not guaranteed**. [Original footage](https://youtu.be/QC0ZXCX2CeA) shows [five or six absorbed hits while there are seven Anemo hits][T]. [Later original trials](https://docs.google.com/spreadsheets/d/1QBE2949PT7pUo4UCoUHLk03zKbRDNTjLUQhDdGFv_a0/edit?usp=sharing) identify a delayed aura check: checking before the second initial cutting hit yields an extra absorbed hit; checking on/after it misses that hit. At [very low frame rates, four absorbed hits were observed][T]. Approximate rates in that study varied with frame rate and sample, so no exact probability is promoted to a test constant. The first initial cutting hit does not carry absorbed damage in these tests.

The [older guide][G] calls absorption “no cooldown”; the [Wiki's advanced-properties table][W] assigns standard ICD to **absorbed cutting elemental application**. Absorption selection and elemental application are different operations. Whether that wording nevertheless reflects an old application claim is **unknown**; use the explicit application table as a sourced claim, not proof resolving the prose.

## Gust Surge

Creates a forward-moving tornado that draws eligible enemies/objects toward itself. Both Anemo and absorbed damage snapshot ([original tests](https://youtu.be/lnia3ynnn0Y), [TCL][T]). Absorbed damage has a smaller hit area than Anemo damage; [original stationary-Ruin-Guard test](https://youtu.be/JZfWZiAeMsc) records [nine Anemo hits but only one absorbed hit][T]. Do not promise full absorbed damage to every enemy inside the broader vacuum.

| Property / hit | Value | Source |
|---|---|---|
| Anemo damage, each tick at level one | 80.8% ATK; 1 U Anemo | [Wiki][W] |
| Anemo ICD | Elemental Burst; 2.5 s / 3 hits | [Wiki][W] |
| Additional absorbed damage, each tick at level one | 24.8% ATK; 2 U of absorbed Cryo / Pyro / Hydro / Electro | [Wiki][W], [original gauge test](https://youtu.be/yh4dH0WbA6A) |
| Absorbed ICD | Elemental Burst Cryo / Pyro / Hydro / Electro; separate from Anemo; 2.5 s / 3 hits | [Wiki][W] |
| Duration / cooldown / energy | 6 s / 15 s / 60 energy | [Wiki][W] |
| Damage schedule | Up to 9 ticks, every 0.5 s | [Wiki][W], [original burst sheet][AQ] |
| First tick — conflicting measurements | Wiki: 2 s after activation; original Aether sheet: 96 frames from animation start | [Wiki][W], [AQ] |
| Particle generation | unknown in inspected sources | [Wiki][W], [TCL][T] |
| Absorption priority | Cryo > Pyro > Hydro > Electro; locks once per use | [Wiki][W] |
| Tornado movement speed and exact Anemo/absorbed hit radii | unknown | [Wiki][W], [TCL][T] |

The first-tick discrepancy is not silently converted into a common timestamp: the [original sheet][AQ] counts animation start, whereas the Wiki says activation. A target's location also affects first contact. The [Wiki][W] says Struggle applies to enemy weight at most [100][W], but not enemies more than [20 levels above Traveler][W]. [Earlier original lift testing][T] only estimated roughly [25 levels](https://youtu.be/rBDMuzkVb54); that is a rough observation, not equal-precision confirmation of the Wiki cutoff. Exact lift threshold remains **conflicting/uncertain**.

### Available skill and burst talent table

Every numeric cell: [Wiki Palm Vortex/Gust Surge scaling][W], units **% ATK per instance**. Undisplayed final skill/burst talent level is **unknown**, not interpolated. Absorbed skill ratios use the independently tested formula above; the burst has its own explicit scaling and must not use that ratio.

| Level | Initial cutting | Max cutting | Initial storm | Max storm | Tornado | Absorbed burst |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 12 | 16.8 | 176 | 192 | 80.8 | 24.8 |
| 2 | 12.9 | 18.06 | 189.2 | 206.4 | 86.86 | 26.66 |
| 3 | 13.8 | 19.32 | 202.4 | 220.8 | 92.92 | 28.52 |
| 4 | 15 | 21 | 220 | 240 | 101 | 31 |
| 5 | 15.9 | 22.26 | 233.2 | 254.4 | 107.06 | 32.86 |
| 6 | 16.8 | 23.52 | 246.4 | 268.8 | 113.12 | 34.72 |
| 7 | 18 | 25.2 | 264 | 288 | 121.2 | 37.2 |
| 8 | 19.2 | 26.88 | 281.6 | 307.2 | 129.28 | 39.68 |
| 9 | 20.4 | 28.56 | 299.2 | 326.4 | 137.36 | 42.16 |
| 10 | 21.6 | 30.24 | 316.8 | 345.6 | 145.44 | 44.64 |
| 11 | 22.8 | 31.92 | 334.4 | 364.8 | 153.52 | 47.12 |
| 12 | 24 | 33.6 | 352 | 384 | 161.6 | 49.6 |
| 13 | 25.5 | 35.7 | 374 | 408 | 171.7 | 52.7 |
| 14 | 27 | 37.8 | 396 | 432 | 182 | 55.8 |
| 15 | unknown | unknown | unknown | unknown | unknown | unknown |

## Frame data and cancels

Units: **frames at [60 fps](https://library.keqingmains.com/combat-mechanics/frames)**, original trial order. Normal hitmarks measure hitlag start; charged second-hit offsets are relative to the previous hit, not the action start. Original transition sheets explicitly exclude hitlag. The [TCL summary][K] can give longer transitions, so original observations and summary values are kept separate rather than called exact equivalent measurements.

| Normal | Aether hitmark trials (frames) | Lumine hitmark trials (frames) | Aether next-normal trials (frames) | Lumine next-normal trials (frames) | Aether TCL next normal (frames) | Sources |
|---|---|---|---|---|---:|---|
| First | 13 / 13 / 13 | 16 / 15 / 15 | 19 / 16 / 16 | 26 / 22 / 25 | 23 | [Aether][AN], [Lumine][LN], [TCL][K] |
| Second | 13 / 13 / 13 | 10 / 10 / 10 | 27 / 26 / 26 | 21 / 20 / 22 | 32 | same |
| Third | 16 / 16 / 16 | 19 / 19 / 19 | 33 / 32 / 31 | 28 / 27 / 27 | 40 | same |
| Fourth | 30 / 30 / 29 | 23 / 23 / 23 | 39 / 39 / 40 | 37 / 38 / 38 | 49 | same |
| Fifth | 25 / 25 / 25 | 14 / 14 / 14 | 68 / 69 / 69 | 64 / 64 / 64 | 81 | same |

Every numeric cell below: [Aether original][AN] / [Lumine original][LN]. Units **frames**.

| Transition / charged event | Aether trials | Lumine trials |
|---|---|---|
| First normal to charged | 28 / 28 / 28 | 32 / 32 / 31 |
| Second normal to charged | 28 / 28 / 28 | 23 / 22 / 23 |
| Third normal to charged | 36 / 36 / 36 | 39 / 39 / 38 |
| Fourth normal to charged | 45 / 45 / 45 | 45 / 45 / 45 |
| Charged first hit | 9 / 10 / 10 | 14 / 14 / 14 |
| Charged second hit, relative offset | 11 / 11 / 11 | 11 / 11 / 11 |
| Charged to normal | 55 / 55 / 55 | 58 / 58 / 58 |
| Charged to skill | 37 / 37 / 37 | 34 / 34 / 35 |
| Charged to burst | 36 / 36 / 37 | 34 / 35 / 35 |
| Charged to dash | 22 / 24 / 22 | 24 / 25 / 25 |
| Charged to jump | 23 / 23 / 21 | 23 / 25 / 24 |
| Charged to swap | 44 / 44 / 44 | 21 / 21 / 21 |

Normal-to-skill/burst/dash/jump/swap entries are `x`: interruptible at any time, but only after the damage hit to preserve it ([AN], [LN]). Lumine charged swap uses the input timing because the usual skill-button grayout indicator is absent ([LN]).

### Palm Vortex and burst frames

Every numeric cell: [KQM TCL][K], [Aether short hold][AS], [Aether full hold][AH], [Aether burst][AQ], or [Lumine tap][LT]. Single-valued press summary is from TCL; other entries preserve measured trials. Units **frames from animation start**, except explicitly noted offsets.

| Event / transition | Aether press, TCL | Aether short strong hold | Aether full hold | Aether burst | Lumine press trials |
|---|---|---|---|---|---|
| Hitmark | 32 | unknown | unknown | first tick 96 / 96 / 96 | 32 / 32 / 32 |
| Cooldown start | 27 | 50 / 50 / 50 | 109 / 108 / 108 | 0 / 0 / 0 | 27 / 27 / 27 |
| Energy drained | not applicable | not applicable | not applicable | 3 / 3 / 3 | not applicable |
| Next normal | 74 | 98 / 98 / 97 | 157 / 156 / 156 | 111 / 110 / 109 | 61 / 60 / 61 |
| Next burst | 76 | 98 / 99 / 98 | 156 / 157 / 157 | not applicable | 62 / 62 / 62 |
| Next skill | not applicable | not applicable | not applicable | 109 / 109 / 109 | not applicable |
| Dash | 30 | 54 / 54 / 54 | 113 / 112 / 112 | 96 / 96 / 96 | 31 / 31 / 31 |
| Jump | 31 | 54 / 55 / 54 | 114 / 112 / 113 | 96 / 96 / 95 | 31 / 31 / 30 |
| Swap | 66 | 89 / 90 / 89 | 147 / 147 / 147 | 100 / 101 / 101 | 59 / 60 / 60 |

Every numeric cell in this additional Lumine table: [short-hold tab](https://docs.google.com/spreadsheets/d/1QCtYnC_qdrCYwN5qDJLYYGuvx81z9fTGNzPdZ8H4nrU/gviz/tq?tqx=out:html&sheet=Skill%20%28Short%20Hold%29), [full-hold tab](https://docs.google.com/spreadsheets/d/1QCtYnC_qdrCYwN5qDJLYYGuvx81z9fTGNzPdZ8H4nrU/gviz/tq?tqx=out:html&sheet=Skill%20%28Hold%29), or [burst tab](https://docs.google.com/spreadsheets/d/1QCtYnC_qdrCYwN5qDJLYYGuvx81z9fTGNzPdZ8H4nrU/gviz/tq?tqx=out:html&sheet=Burst). Units **frames**, original trial order.

| Event / transition | Lumine short strong hold | Lumine full hold | Lumine burst |
|---|---|---|---|
| Hitmark | unknown | unknown | first tick 91 / 94 / 94 |
| Cooldown start | 50 / 51 / 50 | 109 / 108 / 109 | 0 / 0 / 0 |
| Energy drained | not applicable | not applicable | 2 / 3 / 3 |
| Next normal | 84 / 83 / 82 | 143 / 143 / 140 | 105 / 104 / 105 |
| Next burst | 83 / 84 / 84 | 143 / 142 / 142 | not applicable |
| Next skill | not applicable | not applicable | 105 / 104 / 104 |
| Dash | 54 / 53 / 54 | 113 / 113 / 113 | 90 / 91 / 90 |
| Jump | 54 / 56 / 54 | 113 / 113 / 112 | 91 / 90 / 89 |
| Swap | 83 / 83 / 83 | 143 / 142 / 141 | 95 / 95 / 96 |

Full/short-hold skill hitmarks are `?` in the original sheets; the cutting schedules in seconds are separate data. Plunge frames and exact charge-stage/cancel engine thresholds remain **unknown**. Do not reuse Aether's animation timings for Lumine. [Deprecated frame claims in TCL][T] are explicitly superseded by the current measured tables, not new candidate constants.

## Passives, constellations, and progression boundaries

| Unlock | Effect | Source |
|---|---|---|
| First ascension — Slitting Wind | Final normal adds a 60% ATK Anemo blade, 1 U Anemo; treated as Normal Attack damage | [Wiki][W], [TCL][K] |
| Slitting Wind timing | Before Aether's fifth Physical hit, after Lumine's fifth Physical hit | [TCL][K] |
| Slitting Wind application ICD | No ICD; separate from infused sword string | [Slitting Wind advanced properties](https://genshin-impact.fandom.com/wiki/Slitting_Wind#Gameplay_Notes) |
| Fourth ascension — Second Wind | Palm Vortex kill heals 2% Max HP each second for 5 s; trigger cooldown 5 s | [Wiki][W], [Second Wind notes](https://genshin-impact.fandom.com/wiki/Second_Wind) |
| First constellation — Raging Vortex | Palm Vortex pulls objects/enemies within 5 m; not part of unconstellated baseline | [Wiki][W], [TCL][K] |
| Final constellation — Intertwined Winds | Gust Surge damage reduces Anemo RES by 20%; absorbed element RES also reduced by 20% | [Wiki][W] |

### Later quest unlock already present in target version

**Foreign Windwrath** was introduced in [Version Luna IV][FW], unlocked by completing *Song of the Welkin Moon: Act VIII — True Moon*. It is not an ascension passive and must remain gated away from a new starter.

| Property | Value | Source |
|---|---|---|
| Blade of the Dawn Breeze acquisition | Nearby party Pyro/Hydro/Cryo/Electro attacks hitting opponents grant one stack of that elemental type; one stack per type | [Wiki Traveler additional talent][W] |
| Reaction damage acquisition | Traveler can acquire stacks from reactions such as Swirl/Overloaded | [Foreign Windwrath notes][FW] |
| Enhanced charged trigger | At least two different elemental stacks; consumes all stacks | [Wiki][W] |
| Charged Attack: Whirlwind | Each charged hit becomes Anemo and has increased damage equal to 60% ATK | [Wiki][W] |
| Consumed elemental stack | Summons matching-element Blade Wind dealing 50% ATK, counted as charged damage | [Wiki][W] |
| Trigger cooldown | 15 s; cannot gain stacks during Whirlwind | [Wiki][W] |
| Whirlwind/Blade Wind gauge, ICD, duration and hit/cancel frames | unknown | Inspected unlock sources |
| Resonance-related Anemo stat unlock | +10 percentage points CRIT Rate after the specified True Moon quest step, conditional on having resonated with Anemo | [Wiki Traveler permanent stat notes][W] |

Other past quest stat additions also must not appear in starter ATK/HP: [Version 3.0's permanent +3 base ATK][T], and [Version 5.7 tested Skirk training +7 base ATK, +15 EM, +50 base HP][T]. These alter character stats, not talent multipliers. Their relevance is progression state, not a baseline talent balance change.

## Unknowns and reliability

Least reliable: exact Palm stage boundaries and absorbed aura-check distribution; the conflicting Gust Surge first-contact time and approximate enemy-lift cutoff; historical frame applicability to the target release; all additional-talent application properties. Source rounding of normal multipliers is not sufficient evidence for hidden exact coefficients. Missing higher normal levels/final skill-burst level, plunge frames, exact geometry and the explicitly listed gauge/frame unknowns remain **unknown**, never interpolated. No verification/build/test commands were run.

[W]: https://genshin-impact.fandom.com/wiki/Traveler#Talents
[K]: https://library.keqingmains.com/characters/anemo/traveler-anemo
[G]: https://keqingmains.com/anemo-traveler/
[T]: https://library.keqingmains.com/evidence/characters/anemo/traveler-anemo
[FW]: https://genshin-impact.fandom.com/wiki/Foreign_Windwrath
[AN]: https://docs.google.com/spreadsheets/d/186FpS4ckDENVY4U60xxgevJZj_vdyYWZroIa7P_yDr4/edit?usp=sharing
[LN]: https://docs.google.com/spreadsheets/d/1S7cJszsMoQF6ShCvA7hMjiXMuOCoDRwgW1rseP0uC8k/edit?usp=sharing
[AS]: https://docs.google.com/spreadsheets/d/1y_KoAchtrWwPDradqLvkKuPbLUonsP9G4aFuuxtxAgg/edit?gid=322315628
[AH]: https://docs.google.com/spreadsheets/d/1y_KoAchtrWwPDradqLvkKuPbLUonsP9G4aFuuxtxAgg/edit?gid=1296632535
[AQ]: https://docs.google.com/spreadsheets/d/1y_KoAchtrWwPDradqLvkKuPbLUonsP9G4aFuuxtxAgg/edit?gid=775340159
[LT]: https://docs.google.com/spreadsheets/d/1QCtYnC_qdrCYwN5qDJLYYGuvx81z9fTGNzPdZ8H4nrU/edit?usp=sharing
