# Kaeya — starter kit

## Scope and evidence

Target: [Version 7.1, “A Rekviem for the Underworld”](https://traveler.gg/a-rekviem-for-the-underworld-version-7-1-update-details/). Baseline is unconstellated with ascension unlocks separate. No Kaeya kit change was found in that update mirror. The [official endpoint](https://genshin.hoyoverse.com/m/en/news/detail/166383) returned only a site shell. Community tests cited here are not claimed to have been rerun in the pinned version. Multipliers are **% total ATK**. Gauge is application strength before aura tax, and ICD gates application rather than damage.

Weapon: **sword**; element: **Cryo** ([KQM][K]). “Frostgale” in the assignment corresponds to the English talent **Frostgnaw** ([Wiki][W]); no distinct Frostgale attack is documented.

## Ceremonial Bladework

Normal string: [five attacks, each a single sword hit][K]. Charged attack: [two sword hits, costing 20 stamina][W]. All normal, charged and plunge hits are Physical without external infusion, and therefore have **no elemental application**. The Wiki's underlying sword gauge entries describe infused behavior, not baseline Cryo attacks: [normal/charged share Normal Attack ICD, 2.5 s / 3 hits, with 1 U potential application; plunge collision is 0 U, landing 1 U and No ICD][W].

Every number in the following scaling table is sourced to [Wiki Ceremonial Bladework attribute scaling][W], units **% ATK**. Charged entries retain the two separate multipliers. Higher normal levels absent from this source are **unknown**, not extrapolated.

| Hit | L1 | L2 | L3 | L4 | L5 | L6 | L7 | L8 | L9 | L10 | L11 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| First | 53.75 | 58.13 | 62.5 | 68.75 | 73.13 | 78.13 | 85 | 91.88 | 98.75 | 106.25 | 114.84 |
| Second | 51.69 | 55.89 | 60.1 | 66.11 | 70.32 | 75.13 | 81.74 | 88.35 | 94.96 | 102.17 | 110.43 |
| Third | 65.27 | 70.59 | 75.9 | 83.49 | 88.8 | 94.88 | 103.22 | 111.57 | 119.92 | 129.03 | 139.47 |
| Fourth | 70.86 | 76.63 | 82.4 | 90.64 | 96.41 | 103 | 112.06 | 121.13 | 130.19 | 140.08 | 151.41 |
| Fifth | 88.24 | 95.42 | 102.6 | 112.86 | 120.04 | 128.25 | 139.54 | 150.82 | 162.11 | 174.42 | 188.53 |
| Charged | 55.04 + 73.1 | 59.52 + 79.05 | 64 + 85 | 70.4 + 93.5 | 74.88 + 99.45 | 80 + 106.25 | 87.04 + 115.6 | 94.08 + 124.95 | 101.12 + 134.3 | 108.8 + 144.5 | 117.6 + 156.19 |
| Plunge collision | 63.93 | 69.14 | 74.34 | 81.77 | 86.98 | 92.92 | 101.1 | 109.28 | 117.46 | 126.38 | 135.3 |
| Low plunge | 127.84 | 138.24 | 148.65 | 163.51 | 173.92 | 185.81 | 202.16 | 218.51 | 234.86 | 252.7 | 270.54 |
| High plunge | 159.68 | 172.67 | 185.67 | 204.24 | 217.23 | 232.09 | 252.51 | 272.93 | 293.36 | 315.64 | 337.92 |

Fifth normal teleports behind a suitable nearby enemy, can retarget if the previous enemy dies, and cannot pass through obstacles or behind large/shielded Mitachurl targets ([Wiki][W]).

## Frostgnaw

Single forward Cryo blast; no distinct hold variant ([Wiki][E]).

| Property | Value | Source |
|---|---|---|
| Talent-level-one damage | 191.2% ATK Cryo | [Wiki][E] |
| Cooldown | 6 s | [Wiki][E] |
| Gauge / ICD vs enemy | 2 U Cryo; No ICD | [Wiki][E] |
| Gauge vs environmental water / ice duration | 4 U Cryo; 8 s | [Wiki][E] |
| Base particles when an enemy is hit | 2 or 3 Cryo particles in a 1:2 ratio (relative frequencies), approximately 2.67 mean | [Original KQM energy trials][T] |
| Observed trial mean | 2.677 particles over 59 casts | [Original trials][T] |
| Per-particle-count exact probability | unknown as a proven exact distribution; trial ratio is empirical, not a fractional particle drop | [Original trials][T] |
| Range / vertical reach | 8 m forward / 2.2 m high | [KQM guide][G] |

Original energy testing corrected the historical assumption of equal-frequency outcomes; do not treat an older [1:1 assumption][T] as the pinned expectation.

## Glacial Waltz

Summons [three icicles][W] revolving around and following the active character, including after swapping. Damage requires icicle contact, not merely presence in a fixed radial zone. Icicles shatter for a final hit when the burst ends ([KQM guide][G]); damage snapshots Kaeya's stats after casting but before energy drains. Activation knockback is a separate non-damaging event ([Wiki][W]).

| Property | Value | Source |
|---|---|---|
| Talent-level-one icicle damage | 77.6% ATK Cryo | [Wiki][W] |
| Duration / cooldown / energy cost | 8 s / 15 s / 60 energy | [Wiki][W] |
| Icicle gauge / shared application ICD | 1 U Cryo; Elemental Burst tag, 2.5 s / 3 hits | [Wiki][W] |
| Approximate revolution period | approximately 2 s; 4 revolutions in baseline burst | [KQM guide][G] |
| Typical total connections | approximately 15, geometry/movement dependent, not fixed | [Wiki][W] |
| Contact radius | approximately 2.5 m from active character | [KQM guide][G] |
| Particle generation | unknown in inspected sources | [Wiki][W] |
| Exact hit schedule and final-shatter frame | unknown | [Wiki][W], [KQM guide][G] |

### Unresolved damage-lock conflict

These are **damage locks**, not elemental application ICD. Preserve the disagreement; do not silently use a fixed burst tick timer.

| Source | Claimed per-icicle re-hit behavior | Units / context |
|---|---|---|
| [Original KQM TCL testing][T] | At least 75 frames, observed 75–100 frames, enemy-independent lock | frames at 60 fps; non-final constellation testing; original page also calls 75 frames “1.2 s” (rounded) |
| [KQM guide][G] | Separate 0.5 s cooldown per icicle, unable to affect any enemy while locked | seconds; no shared lock between icicles |
| [Wiki][W] | 0.5 s cooldown before the same icicle can damage the same target again | seconds; target-specific wording differs from KQM guide |

Original [Regisvine video](https://m.youtube.com/watch?v=QpaoDEwA1NE), [Ruin Guard video](https://m.youtube.com/watch?v=zdbfwWboedA), and [frame counts](https://docs.google.com/spreadsheets/d/14FzSNVsEFG6wg1oDt49b8vPr_zjmKzRJsAQ2VC71OIc/edit) are the strongest linked evidence for the longer lock, but were tested in [Version 2.4][T], not the pinned release. Whether a version change explains the discrepancy is **unknown**; no relevant documented change was found.

### Available skill/burst talent table

Every numeric cell from [Frostgnaw scaling][E] / [Glacial Waltz scaling][W], units **% ATK per hit**. Final displayed values preserve source rounding. Undisplayed level fifteen: **unknown**.

| Level | Frostgnaw (%) | Icicle (%) |
|---|---:|---:|
| 1 | 191.2 | 77.6 |
| 2 | 205.54 | 83.42 |
| 3 | 219.88 | 89.24 |
| 4 | 239 | 97 |
| 5 | 253.34 | 102.82 |
| 6 | 267.68 | 108.64 |
| 7 | 286.8 | 116.4 |
| 8 | 305.92 | 124.16 |
| 9 | 325.04 | 131.92 |
| 10 | 344.16 | 139.68 |
| 11 | 363.28 | 147.44 |
| 12 | 382.4 | 155.2 |
| 13 | 406.3 | 164.9 |
| 14 | 430 | 175 |

## Frame data

Frames at [60 fps](https://library.keqingmains.com/combat-mechanics/frames), relative to each action start. Original hitmarks use hitlag start for normals and visible hitmarks for charged hits. Original transitions are **without hitlag**; KQM summary transitions differ, so both are retained.

| Action | Hitmark (frames) | Original next normal trial frames | TCL next normal frames | Original charged transition trial frames | TCL charged transition frames | Sources |
|---|---:|---|---:|---|---:|---|
| First normal | 14 | 21 / 21 / 21 | 27 | 30 / 30 / 30 | 36 | [Original][F], [TCL][K] |
| Second normal | 9 | 20 / 21 / 22 | 27 | 25 / 24 / 25 | 31 | [Original][F], [TCL][K] |
| Third normal | 14 | 39 / 39 / 39 | 47 | 46 / 47 / 47 | 55 | [Original][F], [TCL][K] |
| Fourth normal | 23 | 38 / 38 / 37 | 46 | 46 / 46 / 46 | 54 | [Original][F], [TCL][K] |
| Fifth normal | 30 | 64 / 64 / 65 | 74 | not applicable | not applicable | [Original][F], [TCL][K] |

Original normal-to-skill/burst/dash/jump/swap is marked `x`: cancel at any time, but only a post-hit cancel preserves damage ([sheet][F]). Original charged first-hit mark is [16 frames][F]; second hit offset is [0 frames][F] relative to the sheet's previous-hit convention, **not** a hit at animation-start frame zero.

| Charged-to action | Original trials (frames, without hitlag) | Source |
|---|---|---|
| Normal | 54 / 55 / 55 | [Original][F] |
| Skill | 37 / 37 / 36 | [Original][F] |
| Burst | 35 / 37 / 36 | [Original][F] |
| Dash | 24 / 26 / 25 | [Original][F] |
| Jump | 24 / 24 / 24 | [Original][F] |
| Swap | 34 / 34 / 34 | [Original][F] |

### Skill and burst original trials

Every numeric cell: [skill tab](https://docs.google.com/spreadsheets/d/11f_FxKhXFpL4EHSfFHPPjhXJvVOTi-FP_R6cqaZ9dwo/gviz/tq?tqx=out:html&sheet=Skill) or [burst tab](https://docs.google.com/spreadsheets/d/11f_FxKhXFpL4EHSfFHPPjhXJvVOTi-FP_R6cqaZ9dwo/gviz/tq?tqx=out:html&sheet=Burst). Units: **frames**, original trial order, without hitlag.

| Event / transition | Frostgnaw (frames) | Glacial Waltz (frames) |
|---|---|---|
| Hitmark / first elemental application | 28 / 28 / 28 | 52 / 53 / 53 |
| Cooldown start | 25 / 25 / 25 | 48 / 48 / 48 |
| Energy drained | not applicable | 51 / 51 / 51 |
| Next normal | 53 / 52 / 53 | 77 / 77 / 78 |
| Next skill | not applicable | 78 / 77 / 78 |
| Next burst | 52 / 53 / 52 | not applicable |
| Dash | 25 / 26 / 25 | 61 / 62 / 62 |
| Jump | 26 / 26 / 26 | 61 / 61 / 62 |
| Swap | 49 / 49 / 49 | 76 / 77 / 77 |

Jump rows carry the original warning “formula doesn't work here”; retain their measured status rather than generalizing them. Plunge frames remain **unknown**. Do not use per-icicle damage-lock frames as cast-animation data.

## Passives and unlock boundaries

| Unlock | Effect | Source |
|---|---|---|
| Utility — Hidden Strength | Own party sprint stamina consumption −20%; identical effects do not stack | [KQM][K] |
| First ascension — Cold-Blooded Strike | Heal Kaeya for 15% ATK per enemy hit by Frostgnaw | [Wiki][W] |
| Fourth ascension — Glacial Heart | One extra Cryo particle per Frozen enemy hit, capped at two per cast; already-Frozen enemies qualify | [Original KQM evidence][T] |

[Original testing][T] says the extra particles do not work against Boss/Trounce enemies, but do work against Cryo Abyss Mage. Do not add this fourth-ascension particle generation to the starter baseline. Constellation duration extension/additional icicle are also not baseline.

## Unknowns and reliability

Least reliable is Glacial Waltz's damage-lock interval and scope (explicit conflict above), followed by old frame trials and the final rounded talent table entries. Exact particle probabilities remain unknown beyond the empirical ratio; unit tests should distinguish possible integer counts from a rounded mean. Higher undisplayed talents and exact icicle timing remain unknown. No verification/build/test commands were run.

[W]: https://genshin-impact.fandom.com/wiki/Kaeya#Talents
[K]: https://library.keqingmains.com/characters/cryo/kaeya
[E]: https://genshin-impact.fandom.com/wiki/Frostgnaw#Gameplay_Notes
[G]: https://keqingmains.com/kaeya/
[T]: https://library.keqingmains.com/evidence/characters/cryo/kaeya
[F]: https://docs.google.com/spreadsheets/d/11f_FxKhXFpL4EHSfFHPPjhXJvVOTi-FP_R6cqaZ9dwo/edit?usp=sharing
