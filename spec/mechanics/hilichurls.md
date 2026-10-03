# Hilichurls and Samachurls

## Scope and version qualification

Target: **[Version 7.1, “A Rekviem for the Underworld”](https://genshin.hoyoverse.com/m/en/news/detail/166383)**, as pinned in the project brief. Scope is ordinary overworld basic Hilichurl, Hilichurl Fighter, Wooden Shield Hilichurl Guard, uninfused Hilichurl Shooter, Pyro Hilichurl Grenadier, and a Samachurl family summary. Elemental Shooters are distinguished where relevant; shielded Mitachurls and other elite families are not aliases for these enemies.

**Evidence limitation:** the wiki and KQM pages below are live reference pages, not a version-tagged export of the pinned release. The [update-details transcript](https://traveler.gg/a-rekviem-for-the-underworld-version-7-1-update-details/) did not identify a named change to these families in the material inspected. That does not establish unchanged values. Every reported value below is **published reference data; exact applicability to the pinned release is unknown unless explicitly stated otherwise**. No historical value is promoted to a confirmed pinned-version test expectation. Exact binary HP curve values and version-stamped enemy configuration were not available in the inspected sources.

## HP, DEF, and level scaling

Do not confuse an enemy's base HP parameter with its HP at the lowest level. The [English wiki scaling reference](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) defines:

`normalMaxHP(L) = baseHP × hpLevelMultiplier(curveType, L)`.

Quest, domain, and co-op multipliers are separate. Their values for the intended camp are **unknown**. Do not apply a Spiral Abyss multiplier to the overworld camp. The source explicitly warns that HP/ATK can differ in those contexts ([Fighter statistics](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Stats)).

| Enemy | Published base HP parameter (HP) | Published HP curve | Ratio to basic Hilichurl (dimensionless) | Source |
| --- | ---: | --- | ---: | --- |
| Basic Hilichurl | 13.584 | Type 1 | 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| Hilichurl Fighter | 13.584 | Type 1 | 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP), [KQM Fighter](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-fighter) |
| Wooden Shield Hilichurl Guard | 13.584 | Type 1 | 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP), [KQM Guard](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/shield-hilichurl-guard) |
| Hilichurl Shooter, including elemental Shooter family | 10.8672 | Type 1 | 0.8 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| Pyro Hilichurl Grenadier | 13.584 | Type 1 | 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP), [KQM Grenadier](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-grenadier) |
| Samachurl family | 13.584 | Type 1 | 1 | [Scaling wiki](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |

The Cryo Grenadier is a separate subtype and is not covered by the Pyro Grenadier row.

### Low-level published curve, not exact game floats

The [Japanese wiki mirror's enemy scaling table](https://wiki3.jp/genshin_impact/page/2093) publishes a **Hilichurl HP baseline**, not the raw dimensionless multiplier. It explicitly says its table is rounded and the game's values contain decimals. The baseline column below is transcribed, not fitted or interpolated. The Shooter column retains the multiplication expression to avoid presenting derived extra digits as measured precision. It covers the requested [levels 1–20](https://wiki3.jp/genshin_impact/page/2093).

For basic, Fighter, Wooden Shield, Pyro Grenadier, and Samachurl, use the baseline column only as a **rounded published reference**. Shooter reference HP is the baseline multiplied by its [published ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP). Exact pinned HP for every row is **unknown**.

| Enemy level (level) | Published Type 1 Hilichurl baseline (HP, rounded) | Shooter reference expression (HP, approximate) | Source |
| ---: | ---: | --- | --- |
| 1 | 72.917 | 0.8 × 72.917 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 2 | 92.628 | 0.8 × 92.628 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 3 | 114.394 | 0.8 × 114.394 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 4 | 138.215 | 0.8 × 138.215 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 5 | 164.093 | 0.8 × 164.093 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 6 | 192.050 | 0.8 × 192.050 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 7 | 222.075 | 0.8 × 222.075 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 8 | 237.820 | 0.8 × 237.820 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 9 | 261.734 | 0.8 × 261.734 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 10 | 286.604 | 0.8 × 286.604 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 11 | 326.883 | 0.8 × 326.883 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 12 | 368.759 | 0.8 × 368.759 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 13 | 412.257 | 0.8 × 412.257 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 14 | 460.521 | 0.8 × 460.521 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 15 | 510.583 | 0.8 × 510.583 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 16 | 562.470 | 0.8 × 562.470 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 17 | 624.878 | 0.8 × 624.878 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 18 | 679.928 | 0.8 × 679.928 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 19 | 736.002 | 0.8 × 736.002 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |
| 20 | 885.200 | 0.8 × 885.200 | [Curve](https://wiki3.jp/genshin_impact/page/2093), [ratio](https://genshin-impact.fandom.com/wiki/Enemy/Level_Scaling#Base_HP) |

The modern [Fighter page](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Stats) reports integer HP of **73 / 287 / 885 HP** at **level 1 / 10 / 20**, respectively. These are displayed, rounded values, consistent at that precision with the baseline; they are not competing exact float measurements. A closed-form HP polynomial is **unknown**. Preserve a level-indexed curve rather than interpolating missing rows.

### DEF

For ordinary enemies in scope, the [DEF reference](https://genshin-impact.fandom.com/wiki/DEF#Enemy_Defense) gives **`DEF(L) = 5 × L + 500` DEF points**. For example, the [Fighter table](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Stats) reports **505 / 550 / 600 DEF points** at **level 1 / 10 / 20**. The older mirror instead writes the scaling as **`1 + 0.01 × L`**, multiplying a base DEF of **500 points** ([mirror](https://wiki3.jp/genshin_impact/page/2093)); these forms are algebraically equivalent, not a version difference.

## Resistance, weakspots, and endurance

| Family | Physical RES (%) | Pyro / Hydro / Electro / Cryo / Dendro / Anemo / Geo RES (%) | Weakspot | Source |
| --- | ---: | --- | --- | --- |
| Basic Hilichurl | 10 | 10 each | Head | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl) |
| Fighter | 10 | 10 each | Head | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-fighter), [wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Stats) |
| Wooden Shield Guard, body only | 10 | 10 each | Head; shield interception remains separate | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/shield-hilichurl-guard) |
| Shooter | 10 | 10 each | Head | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-shooter) |
| Pyro Grenadier | 10 | 10 each | Head | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-grenadier) |
| Samachurl | 10 | 50 to own element; 10 to every other listed element | Face | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |

Samachurl own-element RES is a **40 percentage-point bonus**, resulting in **50%**, rather than immunity ([KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls)). The family table's generic elemental cells must be overridden by this prose rule.

| Enemy | Poise/endurance bar (poise units) | Poise regeneration/delay (units/s; s) | Attack-state hyperarmor multiplier (dimensionless) |
| --- | --- | --- | --- |
| Basic Hilichurl | unknown | unknown | unknown |
| Fighter | unknown | unknown | unknown |
| Wooden Shield Guard | unknown | unknown | unknown |
| Shooter | unknown | unknown | unknown |
| Grenadier | unknown | unknown | unknown |
| Samachurl | unknown | unknown | unknown |

The [KQM poise evidence](https://library.keqingmains.com/evidence/combat-mechanics/poise) warns that theoretical wiki poise tables contain extrapolated values and actual gameplay may differ. The linked [original NGA investigation](https://bbs.nga.cn/read.php?tid=24216449&rand=913) was inaccessible. No measured bar values for these subtypes were established; do not use a guessed common-enemy bar. Shield durability is not body poise.

## Attacks and equipment behavior

Multipliers are percentages of that enemy's ATK before player-side defenses. Unknown multipliers are not zero. Frame timings, cooldowns, attack-selection weights, hitbox dimensions, and per-attack ICD groups are **unknown**, except the published Shooter timing below. Physical hits do not carry an elemental gauge; elemental attacks can have damage without an established application value.

| Enemy / attack | Damage multiplier (% enemy ATK) | Damage element | Application (GU) / ICD group | Published behavior and source |
| --- | --- | --- | --- | --- |
| Basic: claw/jump attack | unknown | unknown in inspected KQM description | unknown / not applicable if Physical | Unarmed close attack; [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl) |
| Basic: rock throw | unknown | unknown in inspected KQM description | unknown / not applicable if Physical | Picks up a rock and throws it; [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl) |
| Fighter: club hit | 100 | Physical | not applicable | Club swing; [wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Abilities) |
| Fighter: jump and hit | 200 | Physical | not applicable | Powerful charged/jumping club strike, knockback; [wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Abilities) |
| Fighter: consecutive club swings | 100, 100, 125 per hit | Physical | not applicable | Consecutive combo; [wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Abilities) |
| Fighter after club destruction: punch | 75 | Physical | not applicable | Weaponless fallback; [wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Abilities) |
| Fighter after club destruction: rock throw | 50 | Physical | not applicable | Weaponless ranged fallback; [wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Abilities) |
| Fighter: burning-club hits | unknown for altered state | Pyro infusion reported by KQM | unknown / unknown | Pyro can ignite its wooden club; [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-fighter) |
| Wooden Shield Guard: club swing | unknown | unknown for each weapon state | unknown / unknown | Swings club; switches to claws if club burns away; [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/shield-hilichurl-guard) |
| Wooden Shield Guard: block | not a damage multiplier | Shield equipment | unknown | Front interception; side/back hits can pass; cannot block after shield destruction; [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/shield-hilichurl-guard) |
| Shooter: bolt | unknown | Normal Shooter uninfused; exact element label unknown in inspected source | not applicable for uninfused bolt | Stationary aim/charge then shoot; [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-shooter) |
| Elemental Shooter: bolt | unknown | Pyro, Cryo, or Electro by subtype | 2 GU, ICD unknown | Tested application, not a claim for normal Shooter; [original evidence](https://library.keqingmains.com/evidence/combat-mechanics/enemy-mechanics/enemy-elemental-gauge#elemental-aura-application-and-gauge-values-of-enemies), [video](https://youtu.be/cUWXy_PNO_E) |
| Pyro Grenadier: slime bomb | unknown | Pyro | unknown / unknown | Digs, aims, throws; explodes on impact; player can react with held slime; [KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-grenadier) |

The Fighter's club is a **Wood object**, ignitable and burnable by Pyro ([wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Abilities)). KQM reports a burning Pyro-infused club; the inspected wiki attack descriptions label ordinary hits Physical. Neither establishes burning-state multipliers or gauges. Do not silently extend the ordinary damage rows to the burning state or confuse a Fighter with a Berserker.

The Guard's wooden shield and club are burnable separately; KQM also says shield damage can destroy it. Shield HP, elemental durability, block arc, recovery timing, and exact absorption rules are **unknown** ([KQM Guard](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/shield-hilichurl-guard)). No shield unit value was established.

| Timing / volley detail | Published value | Source / qualification |
| --- | --- | --- |
| Shooter pre-shot charge | 4 s | [KQM Shooter](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-shooter); descriptive timing, not frame-tested here; interruption can reset it |
| Special Shooter volley | 5 bolts | [KQM Shooter](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-shooter); not all overworld Shooters |
| Special volley spread | approximately 45 degrees | [Shooter family wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Shooter_(Enemy_Group)); special domains/events/commissions |
| Elemental Shooter gauge test date/version | last tested in Version 1.3 | [KQM original evidence](https://library.keqingmains.com/evidence/combat-mechanics/enemy-mechanics/enemy-elemental-gauge#elemental-aura-application-and-gauge-values-of-enemies); pinned-release validity unknown |

Grenadiers may fail to find a slime in unsuitable terrain, e.g. a Pyro slime underwater, and continue digging ([KQM](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-grenadier)). Exact search/throw timers and blast radius are **unknown**.

## Samachurl summary

All listed subtypes can briefly charge then strike with their staff at close range, with high knockback ([KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls)). Staff and spell multipliers, spell GU, shared ICD groups, healing amount, and timings are **unknown** in inspected sources. Spell themes do not prove application magnitude or staff damage element.

| Subtype | Spell/field behavior | Damage element where documented | Multiplier (% ATK), application (GU), ICD | Source |
| --- | --- | --- | --- | --- |
| Anemo | Summons 3 pursuing tornadoes; separate ground suction field | unknown attack-by-attack in inspected description | unknown | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |
| Hydro | Repeated delayed water attacks at target feet; rain field heals and applies Wet to allies | Hydro-themed; exact damage event values unknown | unknown | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |
| Dendro | Burnable vine barricades; ground field continuously applies Dendro to player | Dendro application documented | unknown | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |
| Electro | Lightning strike creates totem; electrical circles mirrored by active totems | Electro-themed; exact damage event values unknown | unknown | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |
| Cryo | Pillars under itself/allies, breakable with Pyro/blunt attacks; telegraphed repeated stalagmites | Cryo-themed; exact damage event values unknown | unknown | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |
| Geo | Pillars breakable with Geo/blunt attacks; repeated stalagmites; pillar pulses | Geo-themed; exact damage event values unknown | unknown | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |

Anemo suction and Dendro barriers affect players regardless of enemy/player level difference in [community testing](https://library.keqingmains.com/evidence/combat-mechanics/enemy-mechanics/enemy-interactions#some-enemy-cc-is-level-dependent); [Anemo video](https://youtu.be/tXuW6ISx5Sk), [Dendro video](https://youtu.be/pH4oe-BI3JE). This is older testing, not pinned-version confirmation.

## Aggro, detection, leash, and movement

| Property | Units | All covered subtypes |
| --- | --- | --- |
| Initial detection distance; field-of-view | m; degrees | unknown |
| Hearing / attacked-ally alert distance | m | unknown |
| Attack range and minimum separation | m | unknown |
| Leash/reset distance and delay | m; s | unknown |
| Walk/run speed | m/s | unknown |

Do not substitute vanilla Minecraft hostile-mob ranges. [KQM de-aggro evidence](https://library.keqingmains.com/evidence/combat-mechanics/enemy-mechanics/enemy-interactions#de-aggro-distances) measures a range **from spawn in spatial coordinates**, not a universal distance from the player. It links an [original measurement sheet](https://docs.google.com/spreadsheets/d/1s_A_RhVCkLn07XMWDMNMylLiJqhDERxAETmksI7B2Jg/edit#gid=1378246326); no pinned-version measurements for the requested subtypes were established.

## Energy and item drops

Energy objects are not loot materials and are not themselves an amount of recovered character energy.

| Enemy | HP threshold (% max HP) | Object count/type | Element | Source |
| --- | --- | --- | --- | --- |
| Basic, Fighter, Wooden Shield, Shooter, Pyro Grenadier | 60% | 1 particle | Clear/non-elemental | [Basic](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl), [Fighter](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Energy), [Guard](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/shield-hilichurl-guard), [Shooter](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-shooter), [Grenadier](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-grenadier) |
| Same enemies | Death | 1 particle | Clear/non-elemental | [Basic](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl), [Fighter](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Energy), [Guard](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/shield-hilichurl-guard), [Shooter](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-shooter), [Grenadier](https://library.keqingmains.com/enemy-data/hilichurls/hilichurls/hilichurl-grenadier) |
| Samachurl | 60% | 1 particle | Own element | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |
| Samachurl | Death | 1 orb | Own element | [KQM](https://library.keqingmains.com/enemy-data/hilichurls/samachurls) |

| Family | Material loot | Currency / EXP published sample | Exact low-level drop probabilities and quantities |
| --- | --- | --- | --- |
| Melee Hilichurls / Pyro Grenadier | Damaged, Stained, Ominous Mask family | Fighter: level 90+ gives 32–49 Mora and 20 Character EXP; [wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Fighter#Drops) | unknown |
| Shooter family | Firm, Sharp, Weathered Arrowheads; masks also possible | Level 90+: 32–49 Mora and 20 Character EXP; [wiki](https://genshin-impact.fandom.com/wiki/Hilichurl_Shooter_(Enemy_Group)#Drops) | unknown |
| Samachurl | Divining, Sealed, Forbidden Curse Scroll family; Damaged, Stained, Ominous Mask secondary drops | Level 90+: 32–49 Mora and 20 Character EXP; [wiki](https://genshin-impact.fandom.com/wiki/Samachurl#Drops) | unknown |

The [Samachurl wiki](https://genshin-impact.fandom.com/wiki/Samachurl#Drops) supplies a high-level sample, not a full low-level drop table. Enemy-level unlock thresholds for material rarities are **unknown in this research**; they are not inferred from rarity. Currency/EXP high-level samples do not establish early-camp drops. No respawn timer or quest enemy loot-suppression rule was established.

## Starfell Valley / introductory Mondstadt camp

The [Going Upon the Breeze walkthrough](https://www.powerpyx.com/genshin-impact-going-upon-the-breeze-walkthrough/) explicitly places this quest in **Starfell Valley**. It distinguishes an individual nearby Hilichurl after waypoint unlocking from the subsequent camp up the hill outside Mondstadt. It documents **3 enemies in the camp** ([same source and screenshot](https://www.powerpyx.com/wp-content/uploads/genshin-impact-going-upon-the-breeze-5.jpg)).

**Unknown:** exact camp subtype roster, levels, coordinates, equipment, spawn waves, respawn/persistence, chest behavior, and whether the recreation brief's chosen camp is this quest camp rather than another Starfell Valley camp. The count is from a historical walkthrough, not a pinned-release spawn table. Do not fabricate a Fighter/Shooter/Guard combination or merge the isolated Hilichurl into the camp count.

## Conflicts, unknowns, and confidence

- **No documented numeric contradiction established** for the ordinary-family HP ratios, DEF, or body RES. Generic KQM RES tables have explicit Samachurl own-element overrides; do not omit them.
- Displayed integer HP and the mirror's rounded decimal baseline are different presentation precision, not different measured base parameters. Exact raw curve floats, numeric rounding policy, and pinned-version applicability remain **unknown**.
- Burning Fighter state is under-specified: KQM describes Pyro infusion, while the wiki gives ordinary Physical multipliers. The burning-state multiplier/GU cannot be selected silently.
- Least reliable published data for exact expectations: mirror HP decimals (rounded, old mirror), the descriptive Shooter charge duration, family-wide equipment/AI prose, and historical camp count/applicability.
- Still **unknown**: measured poise/endurance, almost all attack frame data and ICD groups, most subtype multipliers and gauges, shield durability/arc, aggro/detection/attack ranges, exact low-level loot tables, and exact camp roster. Attempts included wiki subtype/stat pages, current KQM pages and original Markdown tabs, original gauge/CC/poise evidence, the mirror curve, and quest walkthroughs. Several wiki and NGA requests were inaccessible; absence of a retrieved number is not a zero-value mechanic.
