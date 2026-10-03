# Amber — starter kit

## Scope and evidence

Target: [Version 7.1, “A Rekviem for the Underworld”](https://traveler.gg/a-rekviem-for-the-underworld-version-7-1-update-details/). Baseline is unconstellated, with ascension unlocks stated separately. No Amber kit change was found in that update mirror; this is not a version-specific retest of community frame data. The [official update endpoint](https://genshin.hoyoverse.com/m/en/news/detail/166383) returned only the site shell. Numeric tables cite their source per row or in the table caption; all percentages are of total ATK unless labeled otherwise. Gauge means application strength before aura tax, not an application on every damage hit. Physical hits have no elemental application; do not confuse them with elemental **0U** damage.

Sources: [Wiki character/talents][W], [KQM TCL][K], [Bunny notes/scalings][E], [Fiery Rain notes/scalings][Q], [original normal frame trials][F], [original aimed-shot frame tests][A].

Weapon: **bow**; element: **Pyro** ([KQM][K]).

## Sharpshooter

The normal string has [five single-arrow attacks][K]. Aimed shots have an uncharged Physical state and a fully charged Pyro state; no additional charge tier is documented. [KQM][K] reports damage **and gauge** dropping by 10% for each 0.05 s of flight after 0.7 s, capped at 90% total reduction. These are flight-time thresholds, not aim-charge thresholds. Aimed-shot stamina cost: [0 stamina — bows do not consume stamina while aiming/firing charged shots](https://genshin-impact.fandom.com/wiki/Charged_Attack#Bows); do not substitute sword stamina cost.

| Hit / state | Talent-level-one multiplier (% ATK) | Element | Gauge (U) | ICD tag/type | Source |
|---|---:|---|---|---|---|
| Normal first | 36.12 | Physical | not applicable | not applicable | [Wiki][W] |
| Normal second | 36.12 | Physical | not applicable | not applicable | [Wiki][W] |
| Normal third | 46.44 | Physical | not applicable | not applicable | [Wiki][W] |
| Normal fourth | 47.30 | Physical | not applicable | not applicable | [Wiki][W] |
| Normal fifth | 59.34 | Physical | not applicable | not applicable | [Wiki][W] |
| Uncharged aimed shot | 43.86 | Physical | not applicable | not applicable | [Wiki][W] |
| Fully charged aimed shot | 124 | Pyro | 2, subject to flight dropoff | Charged Attack; 1 s / 3 hits | [Wiki][W] |
| Plunge collision | 56.83 | Physical | not applicable | not applicable | [Wiki][W] |
| Low plunge impact | 113.63 | Physical | not applicable | not applicable | [Wiki][W] |
| High plunge impact | 141.93 | Physical | not applicable | not applicable | [Wiki][W] |

### Available normal talent table

Every cell below: [Wiki Sharpshooter attribute scaling][W]; units **% ATK**. Higher normal levels not displayed by this source are **unknown**, not extrapolated.

| Hit/state | L1 | L2 | L3 | L4 | L5 | L6 | L7 | L8 | L9 | L10 | L11 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| First / second | 36.12 | 39.06 | 42 | 46.2 | 49.14 | 52.5 | 57.12 | 61.74 | 66.36 | 71.4 | 76.44 |
| Third | 46.44 | 50.22 | 54 | 59.4 | 63.18 | 67.5 | 73.44 | 79.38 | 85.32 | 91.8 | 98.28 |
| Fourth | 47.3 | 51.15 | 55 | 60.5 | 64.35 | 68.75 | 74.8 | 80.85 | 86.9 | 93.5 | 100.1 |
| Fifth | 59.34 | 64.17 | 69 | 75.9 | 80.73 | 86.25 | 93.84 | 101.43 | 109.02 | 117.3 | 125.58 |
| Aimed | 43.86 | 47.43 | 51 | 56.1 | 59.67 | 63.75 | 69.36 | 74.97 | 80.58 | 86.7 | 92.82 |
| Fully charged aimed | 124 | 133.3 | 142.6 | 155 | 164.3 | 173.6 | 186 | 198.4 | 210.8 | 223.2 | 235.6 |
| Plunge collision | 56.83 | 61.45 | 66.08 | 72.69 | 77.31 | 82.6 | 89.87 | 97.14 | 104.41 | 112.34 | 120.27 |
| Low impact | 113.63 | 122.88 | 132.13 | 145.35 | 154.59 | 165.17 | 179.7 | 194.23 | 208.77 | 224.62 | 240.48 |
| High impact | 141.93 | 153.49 | 165.04 | 181.54 | 193.1 | 206.3 | 224.45 | 242.61 | 260.76 | 280.57 | 300.37 |

## Explosive Puppet — Baron Bunny

Tap throws Bunny; hold adjusts direction and throw distance, with longer holds throwing farther. Holding does **not** charge the explosion multiplier. Bunny continuously taunts and draws enemy attacks; explodes when its HP is exhausted or its lifetime expires. Its timer begins on landing, whereas skill cooldown begins on cast. Bunny can be displaced by gravity, collisions, attacks and vacuum fields, can be Frozen, and can activate pressure plates ([Wiki][W]). Taunt radius, threat priority, HP snapshot timing, defense/resistance inheritance, throw-distance curve and timer behavior during Freeze: **unknown**.

| Property | Baseline value | Source |
|---|---|---|
| Explosion damage at talent level one | 123.2% ATK, AoE Pyro | [Bunny][E] |
| Bunny HP at talent level one | 41.36% of Amber's Max HP | [Bunny][E] |
| Lifetime from landing | 8 s | [Bunny][E] |
| Skill cooldown | 15 s | [Bunny][E] |
| Explosion gauge / ICD | 2 U Pyro; No ICD | [Bunny][E] |
| Particles on explosion hitting an enemy | 4 Pyro particles, guaranteed on qualifying hit; no generation on placement | [Bunny][E] |

C2 manual detonation is **not baseline**: a fully charged shot to Bunny's foot detonates it with [200% additional damage][E]. Do not enable it merely because aimed shots are implemented.

## Fiery Rain

| Property | Value | Source |
|---|---|---|
| Per wave damage at talent level one | 28.08% ATK Pyro | [Fiery Rain][Q] |
| Listed total damage at talent level one | 505.44% ATK | [Fiery Rain][Q] |
| Waves / types | 18 waves; 5 main arrows and 13 sub-arrows | [Fiery Rain][Q] |
| Duration / cooldown / energy cost | 2 s / 12 s / 40 energy | [Fiery Rain][Q] |
| Gauge, all main and sub-arrow hits | 1 U Pyro | [Fiery Rain][Q] |
| Shared ICD group | Elemental Burst; 1 s / 3 hits | [Fiery Rain][Q] |
| Exact wave interval and timestamp list | unknown; do not divide duration by wave count | Inspected [Wiki][Q] and [KQM][K] |
| Particle generation | unknown in inspected sources | [Fiery Rain][Q] |

Half of the waves land in the inner half of the area and half in the outer half. The damage region can miss sufficiently elevated/flying targets; [Wiki][Q] reports the true range approximately 30% larger than its indicator. Listed total is not a guarantee that all waves connect with a particular target.

### Available skill and burst talent table

Every numeric cell is from [Bunny attribute scaling][E] or [Fiery Rain attribute scaling][Q]. Explosion/wave/total units are **% ATK**; HP is **% Amber Max HP**. Final displayed level is preserved as published (some values visibly rounded). Undisplayed level fifteen is **unknown**.

| Level | Bunny HP (%) | Explosion (%) | Rain wave (%) | Rain total (%) |
|---|---:|---:|---:|---:|
| 1 | 41.36 | 123.2 | 28.08 | 505.44 |
| 2 | 44.46 | 132.44 | 30.19 | 543.35 |
| 3 | 47.56 | 141.68 | 32.29 | 581.26 |
| 4 | 51.7 | 154 | 35.1 | 631.8 |
| 5 | 54.8 | 163.24 | 37.21 | 669.71 |
| 6 | 57.9 | 172.48 | 39.31 | 707.62 |
| 7 | 62.04 | 184.8 | 42.12 | 758.16 |
| 8 | 66.18 | 197.12 | 44.93 | 808.7 |
| 9 | 70.31 | 209.44 | 47.74 | 859.25 |
| 10 | 74.45 | 221.76 | 50.54 | 909.79 |
| 11 | 78.58 | 234.08 | 53.35 | 960.34 |
| 12 | 82.72 | 246.4 | 56.16 | 1010.88 |
| 13 | 87.89 | 261.8 | 59.67 | 1074.06 |
| 14 | 93.1 | 277 | 63.2 | 1137 |

## Frames and cancels

[KQM frame convention](https://library.keqingmains.com/combat-mechanics/frames): **frames at 60 fps**, relative to each action's animation start; release is not target impact (arrow travel is additional). Original tests have trial variation, not exact universal constants.

| Action | Release (frames; original trials) | Next normal (frames; original trials) | Source |
|---|---|---|---|
| First normal | 14 / 15 / 14 | 26 / 27 / 26 | [Original trials][F] |
| Second normal | 10 / 10 / 10 | 22 / 22 / 22 | [Original trials][F] |
| Third normal | 27 / 28 / 27 | 37 / 38 / 37 | [Original trials][F] |
| Fourth normal | 26 / 26 / 26 | 34 / 34 / 33 | [Original trials][F] |
| Fifth normal | 26 / 26 / 26 | 60 / 59 / 61 | [Original trials][F] |
| Uncharged aimed, hold normal button | 15 | 25 total action frames | [Aim tests][A] |
| Fully charged aimed, hold normal button | 86 | 96 total action frames | [Aim tests][A] |

The [original normal sheet][F] marks skill/burst/dash/jump/swap from each normal as `x`: may interrupt at any time; the release must already have happened to preserve the arrow. Aimed-to-dash/jump also uses `x`; aimed-to-normal/skill/burst/swap is listed at [96 frames][F] for the fully charged input. Normal-to-aim entries are `?` (**unknown**). Dedicated aim-mode full-charge threshold, hold-skill cancel frames, and plunge frames: **unknown**.

### Skill and burst original trials

All entries are **frames**, in original trial order; [tap sheet](https://docs.google.com/spreadsheets/d/1XDS-RP7G7-htZ4PP9A5SXPbg7Gy-Laksp4oXwObNgf8/gviz/tq?tqx=out:html&sheet=Skill%20%28Tap%29) and [burst sheet](https://docs.google.com/spreadsheets/d/1XDS-RP7G7-htZ4PP9A5SXPbg7Gy-Laksp4oXwObNgf8/gviz/tq?tqx=out:html&sheet=Burst) supply every numeric cell.

| Event / transition | Tap skill (frames) | Burst (frames) |
|---|---|---|
| Bunny landing / first burst elemental hit | 45 / 46 / 46 | 72 / 71 / 72 |
| Explosion hitmark counted from Bunny landing | 485 / 484 / 483 | not applicable |
| Cooldown start | 5 / 5 / 5 | 56 / 56 / 56 |
| Energy drained | not applicable | 59 / 59 / 59 |
| Next normal | 32 / 32 / 31 | 111 / 111 / 111 |
| Next skill | 33 / 33 / 33 | 111 / 111 / 111 |
| Next burst | 32 / 33 / 32 | not applicable |
| Dash | 9 / 8 / 8 | 57 / 57 / 57 |
| Jump | 8 / 7 / 8 | 58 / 57 / 58 |
| Swap | 23 / 22 / 23 | 61 / 60 / 60 |
| Walk | unknown | 62 / 62 / 62 |

The burst sheet additionally labels a [0.4 s `thinkInterval`](https://docs.google.com/spreadsheets/d/1XDS-RP7G7-htZ4PP9A5SXPbg7Gy-Laksp4oXwObNgf8/gviz/tq?tqx=out:html&sheet=Burst). This is **not established as the damage-wave interval** and cannot be substituted for the individual-wave schedule. Exact wave timestamps remain **unknown**. Bunny landing/explosion are spatially dependent measurements, not guaranteed global constants.

## Passives

| Unlock | Effect | Source |
|---|---|---|
| Utility, available without ascension | Gliding stamina consumption reduced by 20% for own party; does not stack with identical passive | [KQM][K] |
| First ascension — Every Arrow Finds Its Target | Fiery Rain CRIT Rate +10 percentage points and AoE widened by 30% | [Fiery Rain][Q] |
| Fourth ascension — Precise Shot | Weak-point aimed hit grants +15% ATK for 10 s | [Wiki][W] |

## Unknowns and reliability

Least reliable: version applicability of older frame trials, undisplayed high-level scalings, rounded final skill/burst table entries, and Bunny taunt/HP inheritance details. No conflicting exact kit value was found beyond displayed rounding differences between [KQM][K] and [Wiki][W]. No exact Fiery Rain wave timestamps found. Unknowns are intentionally not unit-test constants.

[W]: https://genshin-impact.fandom.com/wiki/Amber#Talents
[K]: https://library.keqingmains.com/characters/pyro/amber
[E]: https://genshin-impact.fandom.com/wiki/Explosive_Puppet#Gameplay_Notes
[Q]: https://genshin-impact.fandom.com/wiki/Fiery_Rain#Advanced_Properties
[F]: https://docs.google.com/spreadsheets/d/1XDS-RP7G7-htZ4PP9A5SXPbg7Gy-Laksp4oXwObNgf8/edit?usp=sharing
[A]: https://docs.google.com/spreadsheets/d/187T-SngEZUUordjY_K_tF_DdvHjQju9CoBJdp2eJOis/edit?usp=sharing
