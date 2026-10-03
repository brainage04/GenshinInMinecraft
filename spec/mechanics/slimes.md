# Slimes: Pyro, Cryo, Electro, Hydro, and Anemo

## Scope and version qualification

Target: **[Version 7.1, “A Rekviem for the Underworld”](https://genshin.hoyoverse.com/m/en/news/detail/166383)**, as pinned in the project brief. This file covers small and large ordinary overworld variants of each named element. Mutant Electro Slimes are distinguished as a related large variant; domain-enhanced slimes are not silently substituted for overworld enemies.

**Evidence limitation:** the sources below are live wiki/KQM reference pages and older linked community tests, not a version-tagged enemy-data export for the pinned release. The [update-details transcript](https://traveler.gg/a-rekviem-for-the-underworld-version-7-1-update-details/) did not identify a named slime change in the material inspected. Absence of a named change does not prove unchanged behavior. All numerical values below are **published reference data; exact pinned-release applicability is unknown**. Exact game curve floats must not be invented from rounded tables. No live-game verification was performed.

## Base HP, DEF, and level curve

The [enemy scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) defines:

`normalMaxHP(L) = baseHP × hpLevelMultiplier(curveType, L)`.

Base HP is a curve parameter, not HP at the lowest level. Co-op, quests, and domains can apply separate multipliers ([small Pyro statistics](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Stats), [large Cryo statistics](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Stats)). Values for the chosen Mondstadt encounters in those special contexts are **unknown**.

| Scope | Published base HP parameter (HP) | Published curve | Ratio to basic Hilichurl (dimensionless) | Source |
| --- | ---: | --- | ---: | --- |
| Small Pyro, Cryo, Electro, Hydro, Anemo | 10.8672 | Type 1 | 0.8 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP), [KQM small](https://library.keqingmains.com/enemy-data/elementals/slime) |
| Large Pyro, Cryo, Electro, Hydro, Anemo | 27.168 | Type 1 | 2 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP), [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |

### Published low-level reference table

The [Japanese wiki mirror](https://wiki3.jp/genshin_impact/page/2093) publishes a rounded **Hilichurl HP baseline** by level. This is not the raw multiplier column of the modern English wiki. It explicitly warns that its decimals are rounded and that calculations can incur rounding errors. The following expressions combine that transcribed baseline with the [English wiki's slime ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP). They are **arithmetic-derived approximations, not measurements, interpolation, or exact game floats**. Expressions are retained rather than claiming extra decimal precision. The rows cover the requested [levels 1–20](https://wiki3.jp/genshin_impact/page/2093).

Both size columns apply to all elements in scope. Exact pinned HP for every row is **unknown**.

| Enemy level (level) | Published Hilichurl baseline (HP, rounded) | Small slime HP expression (HP, approximate) | Large slime HP expression (HP, approximate) | Source |
| ---: | ---: | --- | --- | --- |
| 1 | 72.917 | 0.8 × 72.917 | 2 × 72.917 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 2 | 92.628 | 0.8 × 92.628 | 2 × 92.628 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 3 | 114.394 | 0.8 × 114.394 | 2 × 114.394 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 4 | 138.215 | 0.8 × 138.215 | 2 × 138.215 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 5 | 164.093 | 0.8 × 164.093 | 2 × 164.093 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 6 | 192.050 | 0.8 × 192.050 | 2 × 192.050 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 7 | 222.075 | 0.8 × 222.075 | 2 × 222.075 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 8 | 237.820 | 0.8 × 237.820 | 2 × 237.820 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 9 | 261.734 | 0.8 × 261.734 | 2 × 261.734 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 10 | 286.604 | 0.8 × 286.604 | 2 × 286.604 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 11 | 326.883 | 0.8 × 326.883 | 2 × 326.883 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 12 | 368.759 | 0.8 × 368.759 | 2 × 368.759 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 13 | 412.257 | 0.8 × 412.257 | 2 × 412.257 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 14 | 460.521 | 0.8 × 460.521 | 2 × 460.521 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 15 | 510.583 | 0.8 × 510.583 | 2 × 510.583 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 16 | 562.470 | 0.8 × 562.470 | 2 × 562.470 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 17 | 624.878 | 0.8 × 624.878 | 2 × 624.878 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 18 | 679.928 | 0.8 × 679.928 | 2 × 679.928 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 19 | 736.002 | 0.8 × 736.002 | 2 × 736.002 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 20 | 885.200 | 0.8 × 885.200 | 2 × 885.200 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratios](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |

Independent displayed-integer samples:

| Size / source subtype | Level (level) | Wiki-displayed HP (HP, rounded) | Source |
| --- | ---: | ---: | --- |
| Small Pyro | 1 | 58 | [Wiki stats](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Stats) |
| Small Pyro | 10 | 229 | [Wiki stats](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Stats) |
| Small Pyro | 20 | 708 | [Wiki stats](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Stats) |
| Large Cryo | 1 | 146 | [Wiki stats](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Stats) |
| Large Cryo | 10 | 573 | [Wiki stats](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Stats) |
| Large Cryo | 20 | 1,770 | [Wiki stats](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Stats) |

A closed-form HP polynomial is **unknown**. Treat the curve as a level-indexed lookup. Do not smooth its steps, extrapolate missing rows, or use these displayed integers to reconstruct exact base parameters.

### DEF and ATK parameters

For ordinary covered enemies, **`DEF(L) = 5 × L + 500` DEF points** ([DEF wiki](https://genshin-impact.fandom.com/wiki/DEF#Enemy_Defense)). The [small Pyro](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Stats) and [large Cryo](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Stats) tables report **505 / 550 / 600 DEF points** at **level 1 / 10 / 20**. Size does not change this unmodified DEF rule.

Attack damage must use the attacker's ATK parameter and ATK curve, not the HP ratio. The [scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_ATK) lists these parameters; exact ATK curve floats for the pinned release are **unknown**.

| Variant | Published base ATK parameter (ATK points) | Published curve | Source |
| --- | ---: | --- | --- |
| Small Pyro, Cryo, Electro, Hydro | 7.536 | Type 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_ATK) |
| Small Anemo | 15.072 | Type 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_ATK) |
| Large Anemo, Hydro | 35.168 | Type 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_ATK) |
| Large Pyro, Cryo, Electro; Mutant Electro | 52.752 | Type 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_ATK) |

## Resistance, immunity, and poise

The [small](https://library.keqingmains.com/enemy-data/elementals/slime#resistance-table) and [large](https://library.keqingmains.com/enemy-data/elementals/large-slime#resistance-table) KQM tables state **immunity to the slime's own element**, and ordinary resistance to other damage types. Their generic table cells must be overridden by the own-element immunity prose. Immunity is not a finite RES percentage that can be reduced until damage becomes possible.

| Slime element, both sizes | Physical RES (%) | Pyro RES (%) | Hydro RES (%) | Electro RES (%) | Cryo RES (%) | Dendro RES (%) | Anemo RES (%) | Geo RES (%) | Source |
| --- | ---: | --- | --- | --- | --- | ---: | --- | ---: | --- |
| Pyro | 10 | Immune | 10 | 10 | 10 | 10 | 10 | 10 | [KQM small](https://library.keqingmains.com/enemy-data/elementals/slime), [large](https://library.keqingmains.com/enemy-data/elementals/large-slime), [Pyro wiki](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Stats) |
| Cryo | 10 | 10 | 10 | 10 | Immune | 10 | 10 | 10 | [KQM small](https://library.keqingmains.com/enemy-data/elementals/slime), [large](https://library.keqingmains.com/enemy-data/elementals/large-slime), [large Cryo wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Stats) |
| Electro | 10 | 10 | 10 | Immune | 10 | 10 | 10 | 10 | [KQM small](https://library.keqingmains.com/enemy-data/elementals/slime), [large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Hydro | 10 | 10 | Immune | 10 | 10 | 10 | 10 | 10 | [KQM small](https://library.keqingmains.com/enemy-data/elementals/slime), [large](https://library.keqingmains.com/enemy-data/elementals/large-slime), [Hydro wiki](https://genshin-impact.fandom.com/wiki/Hydro_Slime#Stats) |
| Anemo | 10 | 10 | 10 | 10 | 10 | 10 | Immune | 10 | [KQM small](https://library.keqingmains.com/enemy-data/elementals/slime), [large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |

Large Cryo armor is separate from body RES/HP; removing it does not remove Cryo immunity. The generic KQM own-element immunity rule also includes extinguished Pyro slimes; no immunity-loss exception was found.

| Element | Small poise/endurance (poise units) | Large poise/endurance (poise units) | Regeneration and attack-state hyperarmor |
| --- | --- | --- | --- |
| Pyro | unknown | unknown | unknown |
| Cryo | unknown | unknown | unknown |
| Electro | unknown | unknown | unknown |
| Hydro | unknown | unknown | unknown |
| Anemo | unknown | unknown | unknown |

KQM lists no conventional weakspot for either size. Anemo inflation vulnerability is a special instant-defeat state, not an assumed reduced poise bar ([small](https://library.keqingmains.com/enemy-data/elementals/slime#weakspot-details), [large](https://library.keqingmains.com/enemy-data/elementals/large-slime#weakspot-details)). The [poise evidence](https://library.keqingmains.com/evidence/combat-mechanics/poise) distinguishes tested mechanics from extrapolated wiki data. The original linked [NGA post](https://bbs.nga.cn/read.php?tid=24216449&rand=913) was inaccessible; no exact bar values were established.

## Innate aura behavior: not simply “all slimes always have an aura”

The [KQM gauge evidence](https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/elemental-gauge-theory#elemental-auragauge) says innate elemental enemies such as slimes have permanent auras, explicitly giving Cryo slimes as an example. Treat this as a qualitative native/self-aura behavior, **not infinite GU** and not a license to apply the same rule to Anemo or extinguished Pyro. The exact regeneration implementation is **unknown**.

| Slime | Native aura and state | Published native gauge (GU) | Source / limits |
| --- | --- | --- | --- |
| Small Pyro | Pyro persists indefinitely while ignited; can be extinguished, later reignites | unknown as a directly measured aura value; extinguishing thresholds below | [Pyro wiki abilities](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) |
| Large Pyro | Ignited/extinguished state; retreats to reignite; delayed explosion if defeated while ignited | unknown; do not copy small thresholds without evidence | [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Small/large Cryo | Permanent native Cryo per KQM; large armor is distinct | unknown | [KQM gauge evidence](https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/elemental-gauge-theory#elemental-auragauge), [large Cryo wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Notes) |
| Small/large Electro | Native elemental aura under the general KQM innate-enemy rule; exact depletion/restoration timing unknown | unknown | [KQM gauge evidence](https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/elemental-gauge-theory#elemental-auragauge); not a subtype gauge measurement |
| Small Hydro | Native Hydro continuously available for reactions | 1 GU | [Original Frozen-innate-aura testing](https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#frozen-formula-does-work-with-innate-auras), [evidence video](https://imgur.com/a/7QKodnl) |
| Large Hydro | Native Hydro continuously available for reactions | 2 GU | [Original Frozen-innate-aura testing](https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#frozen-formula-does-work-with-innate-auras), [evidence video](https://imgur.com/a/7QKodnl) |
| Small/large Anemo | No ordinary applyable Anemo aura follows from the normal aura system; own-element damage immunity still exists | not applicable to ordinary aura; enemy-specific intrinsic status gauge unknown | [KQM gauge theory](https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-gauge-theory#aura-application) states Anemo cannot be applied as an aura; no subtype self-aura exception established |

Hydro native-gauge tests were last tested in **Version 2.8** ([evidence](https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#frozen-formula-does-work-with-innate-auras)). They report effective native gauge for the Freeze calculation, not an attack's application strength. Exact pinned values are unknown. Do not assume every size/element shares this pair of gauge values.

Small Pyro extinguishing requirements, transcribed from the [wiki](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities):

| Applied trigger | Required application to extinguish (GU) | Source |
| --- | ---: | --- |
| Hydro | 1 | [Small Pyro abilities](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) |
| Cryo | 2 | [Small Pyro abilities](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) |
| Electro | 2 | [Small Pyro abilities](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) |
| Anemo | 4 | [Small Pyro abilities](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) |
| Geo | 4 | [Small Pyro abilities](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) |

The small Pyro wiki says any received Pyro damage immediately reignites it, regardless of magnitude/gauge; this describes state triggering despite damage immunity. Exact natural reignition delay is **unknown**. Incoming damage, elemental application, aura state, and shield/ignition durability must remain distinct.

Cryo caution: the [large Cryo wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Notes) says it is Frozen-immune, including after armor loss, can nevertheless be Wet, and may Swirl Hydro before Cryo while Wet. It also notes that rain may allow very brief Frozen on a shieldless slime. These are **an unresolved qualification in the same source**, not grounds to silently choose an absolute no-Frozen rule under all weather. Freeze behavior for small Cryo is **unknown from a retrieved subtype-specific source**.

Anemo inflation: any **actual damage** while inflated instantly defeats it; **Anemo damage remains immune**, so an immune hit is not an instant-kill trigger ([Anemo wiki](https://genshin-impact.fandom.com/wiki/Anemo_Slime#Abilities)). KQM describes this as its airborne attack vulnerability for both sizes ([small](https://library.keqingmains.com/enemy-data/elementals/slime), [large](https://library.keqingmains.com/enemy-data/elementals/large-slime)). Exact state entry/exit frames are unknown; do not treat all airborne movement as the vulnerability window.

## Attacks

All multipliers are percentages of enemy ATK. Unknown is not zero. Attack cooldowns, windups, active/recovery frames, radii, projectile speeds, knockback forces, and ICD groups are **unknown** unless separately documented. Attack damage element does not establish its GU or application cadence.

### Small variants

| Variant / attack | Damage multiplier (% ATK) | Damage element | Attack application (GU) / ICD | Behavior and source |
| --- | --- | --- | --- | --- |
| Pyro: bump/pounce, ignited | 100 | Pyro | unknown / unknown | Retreats to suitable separation then pounces; [wiki](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities), [KQM](https://library.keqingmains.com/enemy-data/elementals/slime) |
| Pyro: bump, extinguished | 100 | Physical | not applicable | Wiki explicitly permits this state-dependent damage; [wiki](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) |
| Pyro: final burst | 200 | Pyro | unknown / unknown | Only if defeated while ignited; delayed explosion; exact delay unknown; [wiki](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) |
| Cryo: pounce | unknown | unknown in inspected KQM attack description | unknown / unknown | Initially retreats, pounces, retreats again; [KQM](https://library.keqingmains.com/enemy-data/elementals/slime) |
| Electro: pounce | unknown | unknown in inspected KQM attack description | unknown / unknown | Initially retreats, pounces, retreats again; [KQM](https://library.keqingmains.com/enemy-data/elementals/slime) |
| Hydro: bump | 100 | Hydro | unknown / unknown | Pounce-style close hit; [wiki](https://genshin-impact.fandom.com/wiki/Hydro_Slime#Abilities) |
| Hydro: attacked response | no damage stated | Applies Wet | unknown / unknown | Applies Wet nearby when attacked; exact immune-hit trigger rules unknown; [wiki](https://genshin-impact.fandom.com/wiki/Hydro_Slime#Abilities) |
| Anemo: inflated launch/bump | 50 | Anemo | unknown / unknown | Moves away before briefly inflating and launching; close pursuit can prevent attack; [wiki](https://genshin-impact.fandom.com/wiki/Anemo_Slime#Abilities), [KQM](https://library.keqingmains.com/enemy-data/elementals/slime) |
| Anemo: final wind burst | no damage stated | Wind knockback, not a measured aura | unknown / unknown | Pushes player back before dropping loot; [wiki](https://genshin-impact.fandom.com/wiki/Anemo_Slime#Abilities) |

Small Hydro's attacked response applies Wet for **10 s** in a **1 m radius** ([Hydro wiki](https://genshin-impact.fandom.com/wiki/Hydro_Slime#Abilities)); no application GU was established. Hydro hops on water and cannot drown. Anemo inflates to float down and cannot take fall damage ([Anemo wiki](https://genshin-impact.fandom.com/wiki/Anemo_Slime#Abilities)).

### Large variants

| Variant / attack | Damage multiplier (% ATK) | Damage element | Application (GU) / ICD | Behavior and source |
| --- | --- | --- | --- | --- |
| Pyro: fireball sequence | unknown | Pyro-themed attack; exact event element not independently retrieved | unknown / unknown | 3 consecutive parabolic fireballs while ignited; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Pyro: jumping slam | unknown | unknown in inspected KQM description | unknown / unknown | Jumps high, lands at target after delay; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Pyro: final burst | unknown | Pyro | unknown / unknown | Delayed AoE explosion if killed while affected by Pyro; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Cryo: ice shards | 33.3 per shard, 3 shards | Cryo | unknown / unknown | Only while armor intact; value transcribed, not replaced with an invented exact fraction; [wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities) |
| Cryo: jumping slam | 100 | Cryo | unknown / unknown | Only while armor intact; enhanced variants may have reduced cooldown, amounts unknown; [wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities) |
| Cryo: frigid spray/mist | 10 per reported DoT instance; tick interval unknown | Cryo | unknown / unknown | Armorless retreat and mist; regenerates armor inside mist; [wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities) |
| Electro: bump | unknown | Electro | unknown / unknown | Bumps target; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Electro: jumping slam | unknown | unknown in inspected KQM description | unknown / unknown | High jump, delayed landing at target; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Electro: field | unknown | Electricity; exact event element not independently retrieved | unknown / unknown | Small electricity field around player; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Hydro: jumping slam | unknown | unknown in inspected KQM description | unknown / unknown | High jump, delayed landing at target; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Hydro: trapping bubble | unknown | unknown for damage event | unknown / unknown | High-arcing projectile immobilizes player or other mobs on hit; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Anemo: close inflate | unknown | Anemo | unknown / unknown | Brief inflation at melee range; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Anemo: airborne projectile barrage | unknown per projectile, 5 projectiles | Anemo | unknown / unknown | Wobble, jump, inflate, fire sequentially; inflated damage pops it; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |
| Anemo: deflation push | No damage according to KQM | Knockback | unknown / unknown | Pushes close player away while deflating; [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) |

KQM says large Anemo inflates approximately **1 s** after reaching jump maximum height ([source](https://library.keqingmains.com/enemy-data/elementals/large-slime)). This is descriptive timing, not frame data. Exact barrage cadence and airborne vulnerability boundaries remain unknown.

Mutant Electro Slime: yellow large variant; periodically shocks surroundings and causes arcs with nearby purple Electro Slimes. Arcs do not bounce between purple slimes alone ([KQM large Electro tab](https://library.keqingmains.com/enemy-data/elementals/large-slime)). Arc multiplier, range, interval, gauge, and shared ICD group are **unknown**. Do not make every purple Electro Slime independently generate the mutant's arc behavior.

Large Hydro bubble trapping is level-dependent in [original community tests](https://library.keqingmains.com/evidence/combat-mechanics/enemy-mechanics/enemy-interactions#some-enemy-cc-is-level-dependent): it does not trap a player character **more than 20 levels above** the enemy; **exactly 20 levels above** still traps. [Hydro bubble test](https://youtu.be/9X1Sjo7FN2w), [boundary test](https://youtu.be/RVpgOohc2ic). This was last tested in **Version 2.6** ([evidence](https://library.keqingmains.com/evidence/combat-mechanics/enemy-mechanics/enemy-interactions#some-enemy-cc-is-level-dependent)). Trap duration and damage on a failed-trap hit are **unknown**.

### Large Cryo armor

Armor protects against incoming damage, is weakest to Pyro, receives additional destruction from blunt attacks, and regenerates after the armorless mist/retreat sequence ([wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities), [KQM](https://library.keqingmains.com/enemy-data/elementals/large-slime)). Armor GU is not body HP or the native aura's strength.

| Armor property / trigger | Published value | Units | Source |
| --- | ---: | --- | --- |
| Cryo armor durability | 8 | Cryo GU | [Large Cryo abilities](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities) |
| Pyro required to break | 4 | applied GU | [Large Cryo abilities](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities) |
| Electro required to break | 8 | applied GU | [Large Cryo abilities](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities) |
| Anemo required to break | 16 | applied GU | [Large Cryo abilities](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities) |
| Geo required to break | 16 | applied GU | [Large Cryo abilities](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities) |
| Blunt/poise damage required to break | unknown | poise units | Increased effectiveness documented; exact amount not established |
| Regeneration delay/duration | unknown | s | No exact time established |

Hydro/Dendro interaction with armor durability, non-reaction damage contribution, and elemental application ICD on the armor are **unknown**. Large Cryo freezes water beneath itself and cannot drown ([wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Abilities)).

## Energy and material drops

| Size | HP threshold (% max HP) | Object count/type | Element | Source |
| --- | --- | --- | --- | --- |
| Small, all covered elements | 60% | 1 particle | Own element | [KQM small](https://library.keqingmains.com/enemy-data/elementals/slime#energy-drops), [small Pyro wiki](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Energy) |
| Small, all covered elements | Death | 1 particle | Own element | [KQM small](https://library.keqingmains.com/enemy-data/elementals/slime#energy-drops) |
| Large, all covered elements | 66% | 1 particle | Own element | [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime#energy-drops), [large Cryo wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Energy) |
| Large, all covered elements | 33% | 1 particle | Own element | [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime#energy-drops) |
| Large, all covered elements | Death | 1 orb | Own element | [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime#energy-drops) |

The thresholds are transcribed as **66% / 33%** ([KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime#energy-drops)), not changed to exact thirds. Their internal comparison precision and behavior if damage crosses multiple thresholds together are **unknown**. Energy objects must use the party energy system; do not equate an orb with a particle or a material item.

All covered slimes drop the Slime Condensate / Slime Secretions / Slime Concentrate material family ([small Pyro](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Drops), [Hydro](https://genshin-impact.fandom.com/wiki/Hydro_Slime#Drops), [large Cryo](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Drops)). Material rarity unlock thresholds, exact low-level probabilities, quantities, respawn timing, and quest-specific loot suppression are **unknown in inspected data**.

| Published sample | Enemy level (level) | Mora (currency units) | Character EXP (EXP points) | Source / limitation |
| --- | --- | --- | --- | --- |
| Small Pyro / Hydro | 90+ | 32–49 | 20 | [Pyro drops](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Drops), [Hydro drops](https://genshin-impact.fandom.com/wiki/Hydro_Slime#Drops); not an early-game loot expectation |
| Large Cryo | 90+ | 64–98 | 20 | [Large Cryo drops](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Drops); not an early-game loot expectation |

## Detection, aggro, and Starfell Valley context

| Property | Units | All covered sizes/elements |
| --- | --- | --- |
| Initial detection distance / field of view | m / degrees | unknown |
| Alert propagation to nearby enemies | m | unknown |
| Retreat target separation / melee range | m | unknown |
| Leash/reset distance and delay | m / s | unknown |
| Movement/projectile speed | m/s | unknown |
| Spawn levels and elemental roster in the chosen Starfell Valley camp | levels / enemy counts | unknown |

[KQM de-aggro testing](https://library.keqingmains.com/evidence/combat-mechanics/enemy-mechanics/enemy-interactions#de-aggro-distances) describes enemy-dependent ranges from **spawn**, considering spatial coordinates, not a universal player-distance constant. The linked [measurement sheet](https://docs.google.com/spreadsheets/d/1s_A_RhVCkLn07XMWDMNMylLiJqhDERxAETmksI7B2Jg/edit#gid=1378246326) could not be retrieved. No subtype-specific pinned measurements were established.

The documented introductory camp near Mondstadt is a Hilichurl encounter; the historical [Going Upon the Breeze walkthrough](https://www.powerpyx.com/genshin-impact-going-upon-the-breeze-walkthrough/) describes **3 camp enemies**, but does not document slime composition. See [hilichurls.md](hilichurls.md#starfell-valley--introductory-mondstadt-camp) for the camp evidence and ambiguity. Do not insert slimes into its roster on that evidence alone.

## Source disagreements, unknowns, and reliability

- **Pyro attack-state qualification:** [KQM large](https://library.keqingmains.com/enemy-data/elementals/large-slime) says large Pyro only attacks while ignited and compares it to the small version, whereas the [small Pyro wiki](https://genshin-impact.fandom.com/wiki/Pyro_Slime#Abilities) explicitly gives an extinguished Physical bump. The small wiki row is not evidence that large Pyro has the same fallback; large extinguished attack behavior remains **unknown/conflicting by implication**.
- **Cryo Freeze qualification:** the [large Cryo wiki](https://genshin-impact.fandom.com/wiki/Large_Cryo_Slime#Notes) asserts Frozen immunity but adds a possible brief shieldless-rain exception. Keep that unresolved; neither erase the exception nor make all Cryo slimes normally freezable.
- **Anemo pop qualification:** KQM's “any damage during airborne attacks” and the wiki's “damage while inflated” have different wording. The wiki explicitly retains Anemo immunity. Inflation-window timing is **unknown**, not all airborne frames or all hits.
- Generic own-element RES tables and permanent-aura prose are summaries with subtype/state exceptions, not contradictory measurements. Pyro ignition, Cryo armor, Anemo intrinsic immunity, and Hydro native gauge must not be flattened into one universal slime aura model.
- **Least reliable for exact tests:** rounded mirrored HP baselines; descriptive AI timings; unsourced generic attack prose; wiki values printed as rounded decimals, particularly Cryo shard multipliers; old Freeze-gauge and CC tests extrapolated to the pinned release.
- **Still unknown:** exact pinned raw HP/ATK curve values, most enemy attack multipliers/gauges/ICD groups and frame data, poise/endurance, detection/leash/attack ranges, Cryo armor blunt coefficient and regeneration time, large Pyro extinction GU, Electro/Cryo native GU, exact aura restoration algorithm, early-level loot, and camp elemental roster. Research included current wiki subtype stats, KQM pages and original Markdown tabs, original gauge/Freeze/CC/poise tests, the rounded mirror curve, and historical quest walkthroughs. Several subtype wiki/NGA pages and the de-aggro sheet were inaccessible; this is recorded as missing evidence, not as a fabricated mechanic.
