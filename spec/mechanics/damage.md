# Damage and starter character base stats

## Reference version and evidence policy

Target: **Genshin Impact [Version 7.1, “A Rekviem for the Underworld”][V]**. This describes ordinary overworld combat for the starter party, not Genius Invokation TCG. Percentage variables in formulas are fractions, not percentage-point numbers. Tables preserve the precision published by their sources; additional hidden precision is **unknown**. A link in a table's Source column applies to every numeric cell in that row.

KQM's general mechanics pages are living summaries of community findings, often backed by older tests rather than tests explicitly repeated in the pinned release. Their ordinary-reaction values agree with the current wiki tables. Treat that continuity as **[INFERENCE]**, not proof of a new-version retest. Version-specific changes and exceptional reaction types are separated below; do not silently import historical multipliers.

## Outgoing talent damage

For an ordinary hit, use the following [KQM formula][D]:

\[
D=\left(\sum_i (S_iT_iM_i)+A\right)(1+B-R_D)\,C\,M_{DEF}\,M_{RES}\,M_{amp}.
\]

- `S_i`: scaling stat (total ATK, total DEF, Max HP, or EM).
- `T_i`: talent multiplier expressed as a fraction. **The talent percentage is already included here**; do not multiply it again through `M_i`.
- `M_i`: separate base-damage multiplier when explicitly supplied by a talent; otherwise the neutral multiplier in [KQM][D].
- `A`: additive base-damage bonus, when present; separate from flat ATK.
- `B`: sum of applicable elemental/Physical, attack-category, and common DMG bonuses. Only bonuses applicable to this hit are included.
- `R_D`: applicable damage reduction of the target, not DEF reduction or RES reduction.
- `C`: critical-hit multiplier, as below.
- `M_amp`: amplifying-reaction multiplier if this hit actually triggers Melt/Vaporize. Otherwise neutral. Element application and reaction eligibility are controlled by [elements.md](elements.md), not by the damage element alone.

Stat construction, including all constants, is sourced to [KQM Base Damage][D]:

\[
ATK=(ATK_{character}+ATK_{weapon})(1+ATK\%)+FlatATK
\]
\[
DEF=DEF_{character}(1+DEF\%)+FlatDEF,
\qquad HP=HP_{character}(1+HP\%)+FlatHP.
\]

Do not use naked character base ATK as total ATK; weapon base ATK belongs inside percentage ATK scaling. Talent-specific scalings belong in the individual character specifications.

### Crit

All constants and boundaries here are from [KQM Critical Hits][D]:

\[
C=\begin{cases}1+CD&\text{critical hit}\\1&\text{noncritical hit}\end{cases},
\qquad \mathbb{E}[C]=1+clamp(CR,0,1)CD.
\]

| Quantity | Value | Unit | Source |
| --- | ---: | --- | --- |
| Ordinary base CRIT Rate | 5 | % | [KQM][D] |
| Ordinary base CRIT DMG | 50 | % | [KQM][D] |
| Effective CRIT probability lower bound | 0 | % | [KQM][D] |
| Effective CRIT probability upper bound | 100 | % | [KQM][D] |

The expectation formula is for averages, not a replacement for individual critical-hit outcomes.

## Enemy DEF multiplier

For outgoing character talent damage, all constants and the reduction cap are from [KQM Enemy Defense][D]:

\[
M_{DEF}=\frac{L_c+100}{(L_c+100)+(L_e+100)(1-r_{DEF})(1-i_{DEF})}.
\]

`L_c` is the level of the character responsible for this hit, not necessarily the currently active character; `L_e` is enemy level. `r_DEF` is total enemy DEF reduction; `i_DEF` is the attack's applicable DEF ignore. Reduction and ignore are separate multiplicative terms, not one combined additive reduction. KQM hard-caps DEF reduction at **90% [source][D]**. Equal attacker/enemy levels without reduction or ignore produce **0.5× [algebraic consequence of source][D]**.

This is not the formula for incoming damage to a player character. Ordinary transformative reaction damage bypasses DEF entirely, including these reduction/ignore terms [source][T].

## Enemy RES multiplier

Let `r = base resistance − applicable resistance reduction`, using the **damage instance's element**, or Physical for Physical damage. All constants and branch inequalities are from [KQM Enemy Resistance][D].

| Effective resistance `r` (fraction) | Equivalent range (%) | `M_RES` (dimensionless) | Source |
| --- | --- | --- | --- |
| `r < 0` | below 0% | `1 − r/2` | [KQM][D] |
| `0 ≤ r < 0.75` | 0% to below 75% | `1 − r` | [KQM][D] |
| `r ≥ 0.75` | 75% and above | `1/(4r + 1)` | [KQM][D] |

At the upper breakpoint the adjacent expressions coincide; use the published inclusive high-resistance branch. RES can be negative; do not clamp it to zero. Immunity and shield rules are separate from this scalar formula.

## Incoming enemy talent damage

For an ordinary unbuffed enemy attack, `D_in = enemyATK × attackMultiplier × A/(characterDEF + A) × M_RES`, where `A = 5 × enemyLevel + 500`. This uses the **player character's actual total DEF**, not the outgoing character/enemy level ratio. The Japanese [damage-calculation reference](https://wikiwiki.jp/genshinwiki/%E7%A0%82%E5%A0%B4/%E3%83%80%E3%83%A1%E3%83%BC%E3%82%B8%E8%A8%88%E7%AE%97%E5%BC%8F/%E5%8F%82%E7%85%A7%E3%82%B9%E3%83%86%E3%83%BC%E3%82%BF%E3%82%B9#g7b168eb), inspected 2026-10-04, explicitly derives a general attacker/defender formula with **5 and 500**. The indexed [English DEF reference](https://genshin-impact.fandom.com/wiki/DEF) reports the same form (direct page retrieval returned HTTP 403).

**Conflict:** that Japanese page's separate [incoming section](https://wikiwiki.jp/genshinwiki/%E7%A0%82%E5%A0%B4/%E3%83%80%E3%83%A1%E3%83%BC%E3%82%B8%E8%A8%88%E7%AE%97%E5%BC%8F/%E5%8F%82%E7%85%A7%E3%82%B9%E3%83%86%E3%83%BC%E3%82%BF%E3%82%B9#yfe0e4a2) instead prints **501**. Pinned-release exactness is unknown; `Damage.INCOMING_DEF_BASE = 500` deliberately selects the general derivation/English-reference baseline rather than hiding this discrepancy. The separate damage-reduction factor is neutral for this unshielded starter; shields and reduction buffs are not granted. The starter's Physical RES is an explicit `HilichurlProfile.STARTER_PLAYER_RESISTANCE = 0` adaptation until character RES is separately sourced.

The camp implements the published Fighter **100% Physical club hit** from [hilichurls.md](hilichurls.md). Its ATK curve is absent from the researched specification: `HilichurlProfile.ADAPTED_ATK = 120` is a named level-20 gameplay substitution, **not sourced enemy ATK**. No enemy crit, infusion, multi-hit combo, jump attack or loot/particle emission is inferred. Tests calculate the selected inputs without rounding intermediate damage, not exact live-game golden output.


## Amplifying reactions: Melt and Vaporize

All constants in this formula are from [KQM Amplifying Reaction][D]:

\[
M_{amp}=m_{reaction}\left(1+\frac{2.78\,EM}{1400+EM}+b_{reaction}\right).
\]

Use the triggering character's EM and applicable reaction bonus. `b_reaction` is an additive reaction-specific DMG bonus, not ordinary DMG Bonus. Melt and Vaporize amplify the eligible triggering hit; that hit retains its normal talent scaling, DMG bonuses, crit, DEF, and its own damage element's RES [source][D].

| Existing aura | Triggering element | Reaction direction | `m_reaction` (×) | Resistance used | Source |
| --- | --- | --- | ---: | --- | --- |
| Cryo / Frozen | Pyro | Forward Melt | 2.0 | Pyro | [KQM][D] |
| Pyro | Cryo | Reverse Melt | 1.5 | Cryo | [KQM][D] |
| Pyro | Hydro | Forward Vaporize | 2.0 | Hydro | [KQM][D] |
| Hydro | Pyro | Reverse Vaporize | 1.5 | Pyro | [KQM][D] |
| No amplifying reaction | — | — | 1.0 | Hit element / Physical | [KQM][D] |

These damage multipliers are **not** the gauge-consumption multipliers in [elements.md](elements.md).

## Ordinary transformative reactions

Formula and all constants: [KQM Transformative Reaction][D], corroborated by [KQM reaction mechanics][T].

\[
D_{transform}=m_{reaction}\,K(L_{trigger})
\left(1+\frac{16\,EM_{trigger}}{2000+EM_{trigger}}+b_{reaction}\right)
M_{RES,\ reaction\ element}.
\]

This is a **separate damage instance**, not a multiplier on the triggering talent hit. Ordinary transformative damage does not use ATK/talent scaling, ordinary DMG Bonus, CRIT Rate/DMG, or enemy DEF [source][T]. Only character level, EM, reaction-specific bonuses, and the reaction damage element's RES affect this baseline formula. Reaction damage can be suppressed by its own damage ICD even when gauge consumption occurs; see [elements.md](elements.md).

| Reaction | Multiplier `m_reaction` (× per damage instance/tick) | Damage / RES element | Source |
| --- | ---: | --- | --- |
| Swirl | 0.6 | Element swirled: Pyro, Cryo, Electro, or Hydro; **not Anemo** | [KQM][D] |
| Overloaded | 2.75 | Pyro, regardless of trigger direction | [KQM][D] |
| Superconduct | 1.5 | Cryo, regardless of trigger direction | [KQM][D] |
| Electro-Charged | 2.0 | Electro, regardless of trigger direction | [KQM][D], [wiki][L] |
| Shattered / Shatter | 3.0 | Physical | [KQM][D] |
| Frozen | No intrinsic damage | Not applicable | [KQM][T] |

KQM writes EC's aggregate multiplier as **`2 × ECTriggers` [source][D]**. That sums multiple ticks; it is not an extra tick-count factor to multiply into every tick. Different owners or stats between ticks require separate evaluations. KQM states EC snapshots EM from the last relevant element applier and excludes zero-gauge Electro hits from ownership changes [source][T]; complicated EC ownership is not safely reduced to “always the first Electro character.”

**No ordinary transformative crit:** the listed starter reactions cannot crit [source][T]. Nahida's constellation exception concerns other reactions, not permission for these starter reactions to use normal crit. Lunar and Stellar damage have their own crit-capable formulas and are not ordinary transformative damage; see the version section.

Hydro Swirl damages the original Hydro-afflicted target but its spreading Hydro does not deal Swirl damage to surrounding targets [source][T]. Superconduct also reduces **Physical RES by 40 percentage points for 12 s [source][T]**; its reaction damage itself uses Cryo RES, not the reduced Physical RES.

### Level-based base values

`K(L)` below is the character Level Multiplier, a base-damage quantity **before** reaction multiplier, EM, bonuses, or RES. Values are transcribed from the wiki's [Level Multiplier table][L], not interpolated. The other columns are the wiki's published **rounded** reaction base-damage figures; they are reference display values, **not exact golden-test inputs**. Compute exact expected base damage from `K(L) × m_reaction` using the published inputs above, without rounding intermediate stages.

| Character level (Lv.) | `K(L)` (base DMG units) | Swirl base (DMG, rounded) | Overloaded base (DMG, rounded) | Superconduct base (DMG, rounded) | EC per-tick base (DMG, rounded) | Shattered base (DMG, rounded) | Source for entire row |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 1 | 17.165605 | 10.30 | 47.21 | 25.75 | 34.33 | 51.50 | [Wiki][L] |
| 2 | 18.535048 | 11.12 | 50.97 | 27.80 | 37.07 | 55.61 | [Wiki][L] |
| 3 | 19.904854 | 11.94 | 54.74 | 29.86 | 39.81 | 59.71 | [Wiki][L] |
| 4 | 21.274903 | 12.76 | 58.51 | 31.91 | 42.55 | 63.82 | [Wiki][L] |
| 5 | 22.6454 | 13.59 | 62.27 | 33.97 | 45.29 | 67.94 | [Wiki][L] |
| 6 | 24.649613 | 14.79 | 67.79 | 36.97 | 49.30 | 73.95 | [Wiki][L] |
| 7 | 26.640643 | 15.98 | 73.26 | 39.96 | 53.28 | 79.92 | [Wiki][L] |
| 8 | 28.868587 | 17.32 | 79.39 | 43.30 | 57.74 | 86.61 | [Wiki][L] |
| 9 | 31.367679 | 18.82 | 86.26 | 47.05 | 62.74 | 94.10 | [Wiki][L] |
| 10 | 34.143343 | 20.49 | 93.89 | 51.22 | 68.29 | 102.43 | [Wiki][L] |
| 11 | 37.201 | 22.32 | 102.30 | 55.80 | 74.40 | 111.60 | [Wiki][L] |
| 12 | 40.66 | 24.40 | 111.82 | 60.99 | 81.32 | 121.98 | [Wiki][L] |
| 13 | 44.446668 | 26.67 | 122.23 | 66.67 | 88.89 | 133.34 | [Wiki][L] |
| 14 | 48.563519 | 29.14 | 133.55 | 72.85 | 97.13 | 145.69 | [Wiki][L] |
| 15 | 53.74848 | 32.25 | 147.81 | 80.62 | 107.50 | 161.25 | [Wiki][L] |
| 16 | 59.081897 | 35.45 | 162.48 | 88.62 | 118.16 | 177.25 | [Wiki][L] |
| 17 | 64.420047 | 38.65 | 177.16 | 96.63 | 128.84 | 193.26 | [Wiki][L] |
| 18 | 69.724455 | 41.83 | 191.74 | 104.59 | 139.45 | 209.17 | [Wiki][L] |
| 19 | 75.123137 | 45.07 | 206.59 | 112.68 | 150.25 | 225.37 | [Wiki][L] |
| 20 | 80.584775 | 48.35 | 221.61 | 120.88 | 161.17 | 241.75 | [Wiki][L] |
| 90 | 1446.853458 | 868.11 | 3978.85 | 2170.28 | 2893.71 | 4340.56 | [Wiki][L] |

Do not use the enemy/environment Level Multiplier for a player-triggered reaction. At **Lv. 90 the enemy/environment value is 1202.813736 [source][L]**, whereas the character value is in the table. The wiki's comparison graph is explicitly still labeled pre-buff and needs updating; its numeric table, not that graph, is the source here.

## Starter character base stats

Ungeared base stats; no weapon, artifacts, resonance, food, or quest-granted Traveler stat boosts. `Lv./cap` distinguishes the level before versus after ascension. Both forms of the level boundary are supplied so an implementation cannot silently choose the ascended form. The ascension column is the **additional ascension bonus**, not Kaeya's total ER. A source dash is rendered as no bonus, not an unreported positive value.

| Character | Lv./cap | Base HP (HP units) | Base ATK (ATK units) | Base DEF (DEF units) | Ascension stat and bonus | Source for entire row |
| --- | --- | ---: | ---: | ---: | --- | --- |
| Traveler (Anemo; either twin) | 1/20 | 911.79 | 17.81 | 57.23 | ATK%; no bonus | [Traveler wiki][MC] |
| Traveler (Anemo; either twin) | 20/20 | 2342.39 | 45.75 | 147.01 | ATK%; no bonus | [Traveler wiki][MC] |
| Traveler (Anemo; either twin) | 20/40 | 3023.55 | 59.05 | 189.76 | ATK%; no bonus | [Traveler wiki][MC] |
| Amber | 1/20 | 793.26 | 18.70 | 50.36 | ATK%; no bonus | [Amber wiki][AM] |
| Amber | 20/20 | 2037.88 | 48.04 | 129.37 | ATK%; no bonus | [Amber wiki][AM] |
| Amber | 20/40 | 2630.48 | 62.01 | 166.99 | ATK%; no bonus | [Amber wiki][AM] |
| Kaeya | 1/20 | 975.62 | 18.70 | 66.38 | Energy Recharge%; no additional bonus | [Kaeya wiki][KA] |
| Kaeya | 20/20 | 2506.36 | 48.04 | 170.53 | Energy Recharge%; no additional bonus | [Kaeya wiki][KA] |
| Kaeya | 20/40 | 3235.19 | 62.01 | 220.12 | Energy Recharge%; no additional bonus | [Kaeya wiki][KA] |
| Lisa | 1/20 | 802.38 | 19.41 | 48.07 | EM; no bonus | [Lisa wiki][LI] |
| Lisa | 20/20 | 2061.30 | 49.87 | 123.49 | EM; no bonus | [Lisa wiki][LI] |
| Lisa | 20/40 | 2660.72 | 64.37 | 159.40 | EM; no bonus | [Lisa wiki][LI] |

These decimals are **published rounded base stats**, not recovered exact internal floats. The wiki documents Traveler stat increases per resonated element after the *True Moon* quest and additional talents unlocked by that quest [source][MC]. They are progression-dependent; the baseline starter table does not include them. Exact boosted base-stat totals at these levels are **unknown** in this system specification; consult the character specification for progression unlocks.

## Named starter loadout (2026-10-07)

`StarterLoadout` selects **level20/20, ascension0, refinement1** weapons for the level20/20, C0, talent1 party. This is a transparent development loadout, not a claim that every early-game account owns these weapons. The held Minecraft item does not select gear. **No artifacts, food, resonance or character ascension passives** are granted.

| Character(s) | Weapon | Base ATK at20/20 | Secondary at20/20 | R1 passive and source |
| --- | --- | ---: | --- | --- |
| Traveler, Kaeya | Harbinger of Dawn | 94 | CRIT DMG18% | HP **above90%**: CRIT Rate+14 percentage points; inactive at/below90%. [Published weapon table/passive](https://paimon.moe/weapons/harbinger_of_dawn). |
| Amber | Slingshot | 86 | CRIT Rate12% | Normal/Charged Attack impact within0.3s after firing: DMG Bonus+36%; otherwise−10%. Not an ATK bonus and not an E/Q bonus. [Published weapon table/passive](https://paimon.moe/weapons/slingshot). |
| Lisa | Thrilling Tales of Dragon Slayers | 94 | HP13.5% | Switching away grants the incoming character ATK+24% for10s; one proc per20s. Applies to base character+weapon ATK, not flat ATK or a separate final-damage multiplier; Lisa receives no self buff. [Published weapon table/passive](https://paimon.moe/weapons/thrilling_tales_of_dragon_slayers). |

These are the unascended20 rows, **not** the113/105/113 ATK ascended20 rows. Published rounding and unconfirmed pinned-version continuity remain the same evidence limitations as the base-character tables.

Before conditional party buffs, total ATK is **139.75 / 134.04 / 142.04 / 143.87** (Traveler/Amber/Kaeya/Lisa). Lisa maxHP is `2061.30×1.135=2339.5755`. Full-HP Harbinger users have19% CR/68% CD; Amber17% CR/50% CD; Lisa5% CR/50% CD. Losing the Harbinger condition changes CR only, not ATK or CD. Live talent hits use individual seeded rolls; `1+CR×CD` remains an analysis-only expectation.

Slingshot flight is measured **from firing, not from attack/charge press**. The current Amber raycast lands at release, hence0 elapsed frames and the positive branch at every reachable distance; the rules also represent the late-impact−10% branch. This is an existing hitscan geometry adaptation, **not sourced projectile speed**. Visible projectile/travel work is item19. Thrilling Tales uses600-frame duration/1200-frame cooldown; a rejected switch grants nothing, and its buff belongs only to the incoming character (including while off-field). Forced replacements reuse the switch proc as an explicit adaptation; exact death-proc behavior has not been sourced.


## Version changes and conditional reactions

### Historical damage buffs: not alternative current values

| Reaction | Historical multiplier (×) | Current ordinary multiplier (×) | Change version | Source |
| --- | ---: | ---: | --- | --- |
| Overloaded | 2.0 | 2.75 | 5.2 | [Wiki change history][OH] |
| Superconduct | 0.5 | 1.5 | 5.2 | [Wiki change history][SH] |
| Electro-Charged | 1.2 | 2.0 | 5.2 | [Wiki change history][EH] |
| Shattered / Shatter | 1.5 | 3.0 | 5.2 | [Wiki Frozen change history][FH] |

Shattered was also renamed **Shatter in Version 3.0 [source][FH]**; the two names here refer to the same reaction, not different damage types.

### Lunar and Stellar are gated, not global replacements

[KQM Lunar guide][LU] documents Hydro/Electro → Lunar-Charged only when an enabling passive is present (Ineffa, Flins, or Columbina in the guide's roster). This damage can crit, is multi-contributor, and ignores DEF and ordinary DMG Bonus. Do not run it through the ordinary EC formula. The guide is labeled *Luna VI*, not explicitly the pinned patch: exact pinned-version full roster/coefficients are **unknown** here.

[KQM Stellar guide][ST], labeled **Version 7.0 [source][ST]**, documents conditional Cryo/Electro → Stellar-Conduct and Anemo-on-Cryo → Stellar Swirl, enabled by Stellar Linchpin characters. Stellar-Conduct's reaction itself is nondamaging; Stellar Swirl has an Anemo initial damage instance and a Cryo vortex explosion. These are crit-capable, DEF-ignoring damage categories, not ordinary Superconduct/Swirl. The guide's older roster omits the new enabler Vesna; the pinned [update notes][V] and [official notes][OFF] identify the new character, and the official text readable through its [mirror][MIR] says she enables Stellar Swirl.

Pinned update notes explicitly fix Stellar Swirl sometimes failing to include contributing Cryo characters in damage calculation [source][V]. This is relevant when recreating that conditional reaction: use the fixed contributor behavior, not a pre-patch bug. The starter party has none of the documented Lunar/ Stellar enablers, so its ordinary-reaction formulas remain applicable **[INFERENCE from the documented gating]**. Full conditional reaction implementations are not specified by this ordinary starter-damage file.

## Conflicts, unknowns, and reliability

- **Least reliable for exact golden tests: base-stat decimals.** Wiki stats are rounded to the shown precision. Exact internal values and game rounding order are **unknown**; do not claim binary-float equality against live-game output from these tables.
- **Level-table precision:** wiki supplies the detailed `K(L)` table; KQM only exposes a rounded high-level summary (**1446.85 character / 1202.81 enemy/environment [source][D]**), consistent with the detailed values after rounding. This is a precision difference, not a reason to replace the table with the shorter summary. Exact precision beyond the wiki's published digits is **unknown**.
- **Historical reaction constants:** old versus new multiplier values above are version changes, not unresolved simultaneous claims. They must not be mixed in pinned-version tests.
- **Least reliable timing/ownership:** EC ownership in multi-target, repeated-application, hitlag, and environmental interactions. The library includes older bugs and evolving testing; see the explicit conflicts in [elements.md](elements.md).
- **Version-specific conditional reactions:** KQM's Stellar guide predates the patch's contributor fix and new enabler; Lunar guide's version label also predates the pin. Gating and the patch correction are sourced, but complete pinned coefficients/rosters are **unknown** in this file.
- **No executable verification performed.** Main reviewer should spot-check transcription and source version labels, then derive damage golden cases for DEF/RES boundaries, crit/noncrit, amplifying directions, and separate transformative instances without intermediate rounding.

[D]: https://library.keqingmains.com/combat-mechanics/damage/damage-formula
[T]: https://library.keqingmains.com/combat-mechanics/elemental-effects/transformative-reactions
[L]: https://genshin-impact.fandom.com/wiki/Elemental_Reaction/Level_Scaling
[V]: https://genshin-impact.fandom.com/wiki/Version/7.1
[OFF]: https://genshin.hoyoverse.com/m/en/news/detail/166383
[MIR]: https://gamevika.com/en/genshin/event/a-rekviem-for-the-underworld-version-71-update-details-21946
[MC]: https://genshin-impact.fandom.com/wiki/Traveler#Ascensions_and_Stats
[AM]: https://genshin-impact.fandom.com/wiki/Amber#Ascensions_and_Stats
[KA]: https://genshin-impact.fandom.com/wiki/Kaeya#Ascensions_and_Stats
[LI]: https://genshin-impact.fandom.com/wiki/Lisa#Ascensions_and_Stats
[OH]: https://genshin-impact.fandom.com/wiki/Overloaded/Change_History
[SH]: https://genshin-impact.fandom.com/wiki/Superconduct#Change_History
[EH]: https://genshin-impact.fandom.com/wiki/Electro-Charged/Change_History
[FH]: https://genshin-impact.fandom.com/wiki/Frozen#Change_History
[LU]: https://keqingmains.com/misc/lunar/
[ST]: https://keqingmains.com/misc/stellar/
