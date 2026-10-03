# Lisa — starter kit

## Scope and evidence

Target: [Version 7.1, “A Rekviem for the Underworld”](https://traveler.gg/a-rekviem-for-the-underworld-version-7-1-update-details/). Baseline is unconstellated, with ascension effects separate. No Lisa kit change was found in that update mirror. The [official endpoint](https://genshin.hoyoverse.com/m/en/news/detail/166383) returned only a site shell. Older community tests are not claimed to have been retested in this release. Multipliers are **% total ATK**; gauge is before aura tax. Damage and Conductive stack application are not the same as elemental application.

Weapon: **catalyst**; element: **Electro** ([KQM][K]).

## Lightning Touch

Normal string has [four single-hit Electro attacks][K], each hitscan with a small impact AoE ([guide][G]). Charged attack is an AoE Electro attack costing [50 stamina][W]. Its true hitbox is a sector, not the entire visual circle, and cannot hit above Lisa's altitude ([guide][G]). Her teleport depends on target position; [TCL][K] labels the recovery after the third attack, while the [original evidence page][T] titles the behavior “final AA”; preserve that naming ambiguity rather than assigning an additional hit.

| Hit | Level-one damage (% ATK) | Element | Gauge (U) | ICD group/type | Source |
|---|---:|---|---:|---|---|
| First normal | 39.6 | Electro | 1 | Lisa Electro DMG, shared normals + tap skill; 2.5 s / 3 hits | [Wiki][W] |
| Second normal | 35.92 | Electro | 1 | same group | [Wiki][W] |
| Third normal | 42.8 | Electro | 1 | same group | [Wiki][W] |
| Fourth normal | 54.96 | Electro | 1 | same group | [Wiki][W] |
| Charged | 177.12 | Electro | 1 | disputed: No ICD versus 0.5 s (below) | [Wiki][W], [TCL][K] |
| Plunge collision | 56.83 | Electro | 0 | No ICD; damage without applying Electro | [Wiki][W] |
| Low plunge impact | 113.63 | Electro | 1 | No ICD | [Wiki][W] |
| High plunge impact | 141.93 | Electro | 1 | No ICD | [Wiki][W] |

### Charged-attack ICD conflict

| Source | Claimed application ICD | Status |
|---|---|---|
| [TCL character attack table][K] | 0.5 s | No original evidence supporting this timer found in the inspected evidence page |
| [Wiki advanced properties][W] | No ICD | Disagrees with TCL table |
| [KQM current guide][G] | No ICD | Agrees with Wiki; not proof of a version change |

No documented change resolving this was found. Do not silently choose a unit-test expectation.

### Available normal talent table

Every numeric cell: [Wiki Lightning Touch attribute scaling][W]; units **% ATK**. Higher undisplayed normal levels are **unknown**, not extrapolated.

| Hit | L1 | L2 | L3 | L4 | L5 | L6 | L7 | L8 | L9 | L10 | L11 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| First | 39.6 | 42.57 | 45.54 | 49.5 | 52.47 | 55.44 | 59.4 | 63.36 | 67.32 | 71.28 | 75.4 |
| Second | 35.92 | 38.61 | 41.31 | 44.9 | 47.59 | 50.29 | 53.88 | 57.47 | 61.06 | 64.66 | 68.39 |
| Third | 42.8 | 46.01 | 49.22 | 53.5 | 56.71 | 59.92 | 64.2 | 68.48 | 72.76 | 77.04 | 81.49 |
| Fourth | 54.96 | 59.08 | 63.2 | 68.7 | 72.82 | 76.94 | 82.44 | 87.94 | 93.43 | 98.93 | 104.64 |
| Charged | 177.12 | 190.4 | 203.69 | 221.4 | 234.68 | 247.97 | 265.68 | 283.39 | 301.1 | 318.82 | 337.24 |
| Plunge collision | 56.83 | 61.45 | 66.08 | 72.69 | 77.31 | 82.6 | 89.87 | 97.14 | 104.41 | 112.34 | 120.27 |
| Low impact | 113.63 | 122.88 | 132.13 | 145.35 | 154.59 | 165.17 | 179.7 | 194.23 | 208.77 | 224.62 | 240.48 |
| High impact | 141.93 | 153.49 | 165.04 | 181.54 | 193.1 | 206.3 | 224.45 | 242.61 | 260.76 | 280.57 | 300.37 |

## Violet Arc and Conductive

Tap launches a homing lightning orb, dealing AoE Electro damage and applying Conductive. Conductive is a per-enemy stack mechanic, not a Cryo/Electro aura, and is applied even where the shared elemental application ICD prevents a new Electro application ([guide][G], [original tests][T]). Hold releases AoE lightning scaled according to each target's current stack count, then clears that target's Conductive status ([Wiki][W]).

| Property | Value | Source |
|---|---|---|
| Tap damage at talent level one | 80% ATK | [Wiki][W] |
| Hold damage at talent level one, no stack / one / two / three | 320% / 368% / 424% / 487.2% ATK | [Wiki][W] |
| Tap / hold cooldown | 1 s / 16 s, hold CD starts on release | [Wiki][W], [guide][G] |
| Tap projectile lifetime | 3 s | [Guide][G] |
| Hold charge threshold | approximately 1.9 s; approximately 2.2 s under Cryo aura | [Guide][G], [linked timing video](https://www.youtube.com/watch?v=VxyeR53lRfw) |
| Maximum hold time | approximately 4 s, auto-release afterward | [Wiki][W], [guide][G] |
| Hold radius | 10 m | [Guide][G] |
| Tap gauge and ICD tag | 1 U Electro; Lisa Electro DMG, shared with normals; 2.5 s / 3 hits | [Wiki][W], [TCL][K] |
| Hold gauge and ICD | 2 U Electro at every stack tier; No ICD | [Wiki][W] |
| Tap particles | 0 | [Wiki][W] |
| Hold particles | 5 Electro particles, guaranteed if an enemy is hit | [Wiki][W] |
| Conductive cap | 3 stacks per enemy | [Wiki][W] |
| Conductive duration | 15 s per individual stack; separate expiry timers, extendable by hitlag | [Wiki][W], [guide][G] |

Releasing during the initial cast phase produces the **tap**, not a weaker hold. Releasing after the charge threshold produces the same hold damage as waiting for auto-release; further holding does not improve its multiplier ([guide][G]). Hold hitbox is cylindrical, can clip terrain and line of sight, and has **unknown** finite vertical extent ([original tests][T]). Tap snapshots; hold damage is dynamic at release ([guide][G]).

### Multi-stacking behavior

Conductive must not be implemented as simply “increment the primary target once per cast.” [Original TCL tests][T] document overlapping stack-granting radii:

- Initial tap AoE grants [one stack][W] to affected targets, then affected targets produce a stack-spreading bounce radius ([original bounce video](https://youtu.be/10QqWDgefpU)). A tightly packed group can receive multiple stacks from a single tap ([group video](https://youtu.be/t8SdGCkJ1lM)). Exact bounce radius and repeat-recursion rule: **unknown**.
- Triggering Overloaded/Superconduct with tap grants [two stacks to the directly hit enemy and one to enemies only in the reaction area][T]. Nearby initial/bounce/reaction radii can further overlap. [Two Pyro-inflicted enemies](https://youtu.be/bLgOMM6GiP8) and [two Cryo-inflicted enemies](https://youtu.be/bUrYn08k5jE) each reach [three stacks][T] under the tested arrangement.
- Burning grass/Frozen water can participate in a chained radius, producing [three stacks on the direct target and two on nearby appropriately positioned targets][T]. Do not model this as reaction damage automatically applying Electro.
- If Lisa leaves the field before the tap projectile hits, it does **not** grant Conductive ([Wiki][W], [guide][G]).

These interaction tests are older linked community evidence; exact radii and overlapping events need fresh validation before geometric unit tests.

## Lightning Rose

Placement has a weak Electro-damage hit that does not apply Electro; the stationary rose then produces recurring targeted discharges. Discharges prioritize enemies over Dendro Cores, otherwise target selection is random within the field. Damage snapshots when the lantern forms ([guide][G]).

| Property | Value | Source |
|---|---|---|
| Placement damage, all talent levels | 10% ATK Electro | [Wiki][W], [guide][G] |
| Placement gauge / ICD | 0 U; No ICD, no Electro aura | [Wiki][W] |
| Discharge damage at talent level one | 36.56% ATK Electro | [Wiki][W] |
| Duration / cooldown / energy cost | 15 s / 20 s / 80 energy | [Wiki][W] |
| Discharge schedule | 29 arcs; 0.5 s interval | [Wiki][W], [guide][G] |
| First discharge timestamp | Original frame sheet reports 119 frames from animation start, approximately 2 s; only one trial, not an exact guaranteed timestamp | [Original burst tab](https://docs.google.com/spreadsheets/d/1KRt6I_c5qeUQVXcHv7ZOZKdqjn4Z_VN1OyVFvhDumOc/gviz/tq?tqx=out:html&sheet=Burst) |
| Gauge / ICD | 1 U Electro; Elemental Burst, 2.5 s / 3 hits | [Wiki][W] |
| Field / discharge-impact radius | 7 m / 1 m | [Guide][G] |
| Particle generation | unknown in inspected sources | [Wiki][W] |

An arc and an enemy connection are not identical: a discharge has an impact AoE and can damage multiple entities. Baseline does not have the extra simultaneous bolts from constellations. Grounded sturdy targets are not unconditionally launched by each discharge; the [guide][G] distinguishes stagger from knockback of airborne enemies. Placement hit has a thin plane-like hitbox with limited vertical reach, unlike subsequent discharges ([original summon-range evidence][T]).

### Available skill/burst talent table

Every numeric cell: [Wiki Violet Arc/Lightning Rose attribute scaling][W], units **% ATK**. Source omits high skill levels and final burst level: those remain **unknown**, not interpolated.

| Level | Tap | Hold no stack | Hold one stack | Hold two stacks | Hold three stacks | Rose discharge |
|---|---:|---:|---:|---:|---:|---:|
| 1 | 80 | 320 | 368 | 424 | 487.2 | 36.56 |
| 2 | 86 | 344 | 395.6 | 455.8 | 523.74 | 39.3 |
| 3 | 92 | 368 | 423.2 | 487.6 | 560.28 | 42.04 |
| 4 | 100 | 400 | 460 | 530 | 609 | 45.7 |
| 5 | 106 | 424 | 487.6 | 561.8 | 645.54 | 48.44 |
| 6 | 112 | 448 | 515.2 | 593.6 | 682.08 | 51.18 |
| 7 | 120 | 480 | 552 | 636 | 730.8 | 54.84 |
| 8 | 128 | 512 | 588.8 | 678.4 | 779.52 | 58.5 |
| 9 | 136 | 544 | 625.6 | 720.8 | 828.24 | 62.15 |
| 10 | 144 | 576 | 662.4 | 763.2 | 876.96 | 65.81 |
| 11 | 152 | 608 | 699.2 | 805.6 | 925.68 | 69.46 |
| 12 | 160 | 640 | 736 | 848 | 974.4 | 73.12 |
| 13 | 170 | 680 | 782 | 901 | 1035.3 | 77.69 |
| 14 | unknown | unknown | unknown | unknown | unknown | 82.3 |
| 15 | unknown | unknown | unknown | unknown | unknown | unknown |

The [guide][G] summarizes stack multipliers as 115%, 133%, 152% of no-stack damage. These are rounded summaries: the exact [Wiki][W] talent multipliers above should not be replaced by multiplying those rounded ratios.

## Frames and cancel windows

Units: **frames at [60 fps](https://library.keqingmains.com/combat-mechanics/frames)**, relative to action start. Original sheet has no hitlag for these attacks and trial variation. Original hitmark definition is two frames after the first yellow appearance. Do not treat the shortest observed trial as a guaranteed engine boundary.

| Action | Original hitmark trials (frames) | Original next normal trials (frames) | Original charged transition trials (frames) | Source |
|---|---|---|---|---|
| First normal | 26 / 25 / 26 | 30 / 29 / 30 | 31 / 29 / 29 | [Frame trials][F] |
| Second normal | 17 / 18 / 18 | 20 / 20 / 20 | 24 / 24 / 24 | [Frame trials][F] |
| Third normal | 17 / 17 / 18 | 34 / 30 / 35 | 40 / 39 / 40 | [Frame trials][F] |
| Fourth normal | 31 / 32 / 31 | 57 / 57 / 57 | cannot transition directly | [Frame trials][F] |
| Charged, including windup | 72 / 70 / 70 | 91 / 91 / 91 | 103 / 101 / 104 | [Frame trials][F] |

Normal-to-skill/burst/dash/jump/swap entries are `x`, meaning interruptible at any time; only post-hit interrupts preserve the hit ([original sheet][F]).

| Charged-to action | Original trials (frames, including windup) | Source |
|---|---|---|
| Tap skill | 93 / 94 / 95 | [Original sheet][F] |
| Hold skill | 93 / 93 / 93 | [Original sheet][F] |
| Burst | 92 / 93 / 93 | [Original sheet][F] |
| Dash | 70 / 70 / 69 | [Original sheet][F] |
| Jump | 69 / 70 / 70 | [Original sheet][F] |
| Swap | 90 / 89 / 89 | [Original sheet][F] |

Original sheet notes charged windup is skipped after normals other than the last, or after hold skill: approximately [14 frames][F] shorter, rather than universal frame constants. Plunge frames remain **unknown**. Charge threshold in seconds is not a measured input-cancel frame count.

### Skill and burst original trials

Every numeric cell: [tap tab](https://docs.google.com/spreadsheets/d/1KRt6I_c5qeUQVXcHv7ZOZKdqjn4Z_VN1OyVFvhDumOc/gviz/tq?tqx=out:html&sheet=Skill%20%28Tap%29), [hold tab](https://docs.google.com/spreadsheets/d/1KRt6I_c5qeUQVXcHv7ZOZKdqjn4Z_VN1OyVFvhDumOc/gviz/tq?tqx=out:html&sheet=Skill%20%28Hold%29), or [burst tab](https://docs.google.com/spreadsheets/d/1KRt6I_c5qeUQVXcHv7ZOZKdqjn4Z_VN1OyVFvhDumOc/gviz/tq?tqx=out:html&sheet=Burst). Units: **frames**, original trial order.

| Event / transition | Tap (frames) | Hold, early release (frames) | Burst (frames) |
|---|---|---|---|
| Hitmark / placement | 22 / 22 / 21 | 116 / 118 / 117 | 56 / 56 / 56 |
| Cooldown start | 17 / 17 / 16 | 113 / 115 / 114 | 53 / 53 / 52 |
| Energy drained | not applicable | not applicable | 63 / 63 / 61 |
| Next normal | 38 / 38 / 37 | 140 / 141 / 141 | 85 / 86 / 85 |
| Charged | 38 / 38 / 38 | 139 / 138 / 140 | 86 / 86 / 88 |
| Next burst | 39 / 40 / 39 | 137 / 138 / 138 | not applicable |
| Next tap | not applicable | not applicable | 86 / 87 / 87 |
| Next hold | not applicable | not applicable | 86 / 87 / 86 |
| Dash | 36 / 34 / 35 | 116 / 115 / 116 | 88 / 88 / 88 |
| Jump | 21 / 19 / 20 | 117 / 117 / 117 | 57 / 57 / 57 |
| Swap | 20 / 20 / 20 | 117 / 118 / 117 | 56 / 56 / 56 |

Burst placement is “eyeball'd” in the source; the first discharge is [119 frames, approximately 2 s](https://docs.google.com/spreadsheets/d/1KRt6I_c5qeUQVXcHv7ZOZKdqjn4Z_VN1OyVFvhDumOc/gviz/tq?tqx=out:html&sheet=Burst), with no repeated trials. Hold measurements represent the tested release timing, **not** a maximum-duration hold or a hard input threshold. Tap projectile travel makes impact target-position dependent.

## Passives and version changes

| Unlock | Effect | Source |
|---|---|---|
| First ascension — Induced Aftershock | Charged hits apply one Conductive stack; do not enable on unascended starter | [Wiki][W], [guide][G] |
| Fourth ascension — Static Electricity Field | Rose discharges reduce target DEF by 15% for 10 s, refreshing on subsequent discharge; placement does not apply debuff | [Wiki][W], [guide][G] |
| Utility — General Pharmaceutics | 20% chance to refund a crafting ingredient when making potions | [TCL][K] |

Historical change outside baseline: [TCL evidence][T] explicitly records that as of Version 2.1, Infinite Circuit no longer grants its special self-energy on tap. It now applies only to hold: [2 energy per enemy, maximum 10 energy][K], distinct from normal particles. Do not recreate pre-change tap behavior from old multistack discussions.

## Unknowns and reliability

Least reliable: charged-attack ICD (unresolved disagreement), multistack geometric radii, Cryo-delayed hold timing, the single approximate first Rose tick measurement, and applicability of old frame trials to the pinned version. Exact first Rose tick timing, high skill levels absent in the source, plunge frames, and exact vertical limits remain unknown. No verification/build/test commands were run.

[W]: https://genshin-impact.fandom.com/wiki/Lisa#Talents
[K]: https://library.keqingmains.com/characters/electro/lisa
[G]: https://keqingmains.com/lisa/
[T]: https://library.keqingmains.com/evidence/characters/electro/lisa
[F]: https://docs.google.com/spreadsheets/d/1KRt6I_c5qeUQVXcHv7ZOZKdqjn4Z_VN1OyVFvhDumOc/edit?usp=sharing
