# Traversal: climbing, gliding, falls, and scale

Reference target: **Genshin Impact Version 7.1**, as pinned in [the project brief](../../docs/GENSHIN_MINECRAFT_BRIEF.md#fixed-decisions). Scope is ordinary playable-character traversal, especially the starter party. Character skills, gadgets, regional mechanisms, and event-only movement are exceptions, not ordinary movement defaults.

**Read alongside [stamina.md](stamina.md).** That file owns stamina capacity/progression, ordinary action-cost rows, consumption modifiers, regeneration, drowning, the historical glider-descent estimate, and the published plunge fall-damage-reduction bands. They are referenced here rather than copied into a second specification. This file adds surface eligibility, state transitions, evidence-quality corrections, additional historical measurements/conflicts, water landings, and character/jump scale.

## Evidence and version boundary

No reproducible traversal measurement explicitly identifying the pinned release was found. Consequently, **no historical measurement below is adopted as a verified pinned-version constant**. Current wiki rules are candidate descriptions; historical tests are identified separately. `unknown` means a researched value is unavailable, not permission to invent a default or interpolate from another mechanic.

The accessible sources are public wiki text, KQM's original evidence entries and linked recordings, and community posts/screenshots. No game code, extracted models/assets, or datamined configuration is used. Some wiki pages required their public `action=raw` view. HoYoLAB's article pages returned a loading shell; their public forum article data made the original text readable. Reddit direct reads were blocked; height figures available only through search-indexed post text are explicitly distinguished from directly read evidence.

## Climbing

### Entry and climbable surfaces

Approaching a suitable wall and moving toward it starts climbing; it is not limited to ladders or a particular rock material. Community examples include buildings, trees, rock walls, and climbable Geo Constructs ([Yubelious!, climbing tutorial — controls](https://www.hoyolab.com/article/13611410), [readable public article data](https://bbs-api-os.hoyolab.com/community/post/wapi/getPostFull?gids=2&post_id=13611410)). The minimum-stamina entry gate is already in [stamina.md — ordinary action costs](stamina.md#regeneration-and-ordinary-action-costs), sourced to [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption).

The [Climbing wiki](https://genshin-impact.fandom.com/wiki/Climbing) describes climbable surfaces as those that do not require hanging upside-down or nearly upside-down. It explicitly identifies domain walls as an example of surfaces that can be non-climbable **regardless of their angle**. Thus, angle alone is not a complete eligibility test, and the documentation does not establish a universal material whitelist.

| Surface/geometry | Published behavior | Evidence and limitation |
| --- | --- | --- |
| Ordinary eligible walls, cliff faces, tree surfaces, building surfaces | Can be climbed by moving into the surface | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing); [original community examples](https://www.hoyolab.com/article/13611410). Not proof that every object using the same visible material is climbable. |
| Eligible Geo Constructs | Some can be climbed | [Community controls tutorial](https://www.hoyolab.com/article/13611410). Do not infer that all constructs are traversable. |
| Upside-down/nearly upside-down surface | Not climbable by ordinary climbing | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing). The quantitative overhang boundary is `unknown`. |
| Domain walls explicitly made non-climbable | Not climbable even if their slope would otherwise qualify | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing). This is not evidence that every surface in every domain is prohibited. |
| Roofs, eaves, and other overhangs | An overhang can stop further ascent or cause loss of wall attachment | [Original overhang-routing observations](https://www.hoyolab.com/article/13636558). Sloping roofs and their underside must not be treated as the same geometry. |
| Other excluded surfaces | Complete object/material exclusion list is `unknown` | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing) provides examples, not a complete blacklist. |

| Climbing parameter | Pinned-version value, with units | What the sources establish |
| --- | --- | --- |
| Ground movement → climbing slope threshold | `unknown` degrees; angle convention also `unknown` | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing) is qualitative. No measured minimum angle separating walking/sliding from wall attachment was found. |
| Maximum climbable overhang angle | `unknown` degrees | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing) says upside-down/nearly upside-down is excluded; it does not publish an angle. |
| Continuous upward climb speed | `unknown` m/s | [KQM climbing evidence](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#climbing), [Climbing](https://genshin-impact.fandom.com/wiki/Climbing); no calibrated measurement found. |
| Continuous sideways/downward climb speeds | `unknown` m/s | Same sources; no evidence that these equal upward speed. |
| Ordinary moving-climb drain | `unknown` stamina units/s | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption) explicitly gives an unknown rate; existing coverage is in [stamina.md](stamina.md#regeneration-and-ordinary-action-costs). Historical animation-based observations below do not supply a time rate. |
| Climb-jump vertical displacement | `unknown` m/jump | [Climbing — Climb Jump](https://genshin-impact.fandom.com/wiki/Climbing#Climb_Jump) describes direction and faster ascent, not calibrated displacement. |
| Climb-jump sideways displacement, duration, and velocity curve | `unknown` m/jump, s, and m/s | Same source; no reproducible absolute measurement found. |
| Ledge/mantle reach, clearance, duration, and stamina billing | `unknown` m, s, and stamina units | [Community mantle definition](https://www.hoyolab.com/article/13636558) establishes the top-out concept, not its collision geometry or timing. |

Do not turn the qualitative “nearly upside-down” rule into an exact angle, or use a Minecraft surface-normal cutoff as though it were a measured Genshin constant.

### Climb-jump direction and costs

A climb-jump can be performed while holding still or moving. With movement input, it follows the movement direction, except diagonal-down input produces a horizontal jump and pure-down input jumps off the surface ([Climbing — Climb Jump](https://genshin-impact.fandom.com/wiki/Climbing#Climb_Jump)). This is a wall traversal action, not the ordinary ground jump.

**New cost conflict:** the citation-needed value already noted in `stamina.md` disagrees with an older community depletion-count calculation. Preserve both rather than resolving the disagreement silently.

| Quantity | Published value with units | Source and reliability |
| --- | --- | --- |
| Wiki climb-jump cost | 25 stamina units/jump, **citation needed** | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption), [raw source showing the citation-needed marker](https://genshin-impact.fandom.com/wiki/Stamina?action=raw). Not an authoritative pinned-version constant. |
| Historical community climb-jump cost claim | 24 stamina units/jump | [是水寒啊, original TapTap stamina study — climbing](https://www.taptap.cn/moment/210923504076325385), visibly published in November 2021. Exact tested game version is `unknown`; older than the pin. |
| Observations used for that cost claim | 10 climb-jumps exhaust a 240-stamina pool | [Same original study](https://www.taptap.cn/moment/210923504076325385). This is a depletion-count observation, not a frame-by-frame bill for each jump. Minimum activation stamina and last-action partial spending were not established. |
| Historical ordinary-climb cost claim | 1 stamina unit per observed climbing action for the ordinary non-child models | [Same study](https://www.taptap.cn/moment/210923504076325385); the author counted 240 actions to depletion of a 240-stamina pool. What constitutes an action is animation-observed, not a published simulation interval. |
| Historical child-model observation | Approximately 270 observed climbing actions before depletion of the same pool | [Same study](https://www.taptap.cn/moment/210923504076325385). No exact per-action cost or per-second conversion is adopted. |
| Historical relative climb-jump displacement | Approximately the height of 6 ordinary climbing actions | [Same study](https://www.taptap.cn/moment/210923504076325385). A qualitative action-count comparison, **not** a distance in metres. |

The study includes [ordinary-climb animation evidence](https://img2-tc.tapimg.com/bbcode/images/757f72a690c4a7138d71d51745459d4b.gif/_tap_ugc.gif) and [climb-jump animation evidence](https://img2-tc.tapimg.com/bbcode/images/a4517b8d507ef7fead5ea69068230b51.gif/_tap_ugc.gif), but no reproducible timestamped cost ledger or distance calibration. **[INFERENCE]** A pool-to-depletion action count alone cannot distinguish an exact action cost when the final action may overspend the remainder. Therefore the observations do not independently disprove the wiki value. Pinned-version climb-jump cost remains `unknown`.

For stationary wall attachment, the same [historical study](https://www.taptap.cn/moment/210923504076325385) explicitly reports that stopping climbing stops consumption but **does not permit natural recovery while still attached**. Its pinned-version persistence and any exceptional recovery effect are `unknown`. This fills an evidence gap in `stamina.md`; it is not a replacement regeneration rule.

### Ledge mantle and leaving the wall

A mantle/top-out puts the character's feet on the surface above the climb; the [community tutorial](https://www.hoyolab.com/article/13636558) describes this as changing from pulling to pushing with the arms. A reachable ledge is a place to stand/rest, not a license to traverse an inverted underside. **The exact automatic mantle trigger is `unknown`**: a fixed reach radius, required headroom, allowable lip thickness, animation frames, final snap distance, and whether mantle completion can survive exhaustion all need evidence. The blanket claim “every lip automatically mantles” is **citation needed** and is not adopted.

| Exit/event | Behavior | Source / unresolved detail |
| --- | --- | --- |
| Voluntary drop | Releases wall attachment; PC's default drop binding is `X` | [Controls — PC and Controllers](https://genshin-impact.fandom.com/wiki/Controls#PC_and_Controllers), [readable raw controls table](https://genshin-impact.fandom.com/wiki/Controls?action=raw). Character-specific transition timing is `unknown`. |
| Pure-down movement plus climb-jump | Jumps away from the wall | [Climbing — Climb Jump](https://genshin-impact.fandom.com/wiki/Climbing#Climb_Jump). |
| Successful top-out/standing on a ledge | Leaves wall traversal for a standing surface | [Community ledge/mantle descriptions](https://www.hoyolab.com/article/13636558). Exact geometry/state boundary is `unknown`. |
| Continued ascent into an untraversable overhang | Can result in detachment/falling | [Original community observations](https://www.hoyolab.com/article/13636558); exact collision/angle condition is `unknown`. |
| Stamina reaches 0 stamina units while climbing | Character falls; the fall may cause HP loss | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing). Do not keep the exhausted character suspended on the wall. |
| Climbing-exhaustion free fall | A plunge attack cannot be initiated from this helpless fall state | [Plunging Attack — Plunging Height](https://genshin-impact.fandom.com/wiki/Plunging_Attack#Plunging_Height). This differs from glider exhaustion. |
| Combat while still attached | Ordinary combat abilities are unavailable | [Climbing](https://genshin-impact.fandom.com/wiki/Climbing). A voluntary release followed by an eligible airborne attack is a different state. |

The exhaustive set of climb-interruption events and exact recovery/reattachment timing after forced detachment are `unknown`. [KQM's wall bunny-hopping demonstration](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#climbing-without-climbing), with [original recording](https://youtu.be/n56JICDn1Eg), is movement outside ordinary climbing, not proof of a climbing speed or drain rate. Its historical tested release is [Version 1.4](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#climbing-without-climbing); pinned-version persistence is `unknown`.

## Gliding

### Unlocking and deployment

Ordinary gliding is unlocked when Amber gives the Wind Glider during **City of Freedom**. After unlocking, pressing the jump action while airborne opens it **only when high enough above the ground** ([Gliding](https://genshin-impact.fandom.com/wiki/Gliding), [readable raw article](https://genshin-impact.fandom.com/wiki/Gliding?action=raw)). Airborne status alone is not the full published eligibility rule. Jumping from a ledge/cliff and jumping within a Wind Current are documented launch situations ([Exploration — Player Mechanics](https://genshin-impact.fandom.com/wiki/Exploration#Player_Mechanics)).

| Glider parameter | Pinned-version value with units | Evidence / conclusion |
| --- | --- | --- |
| Minimum deployment clearance above ground | `unknown` m | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding) says “high enough,” without a measured threshold. The measurement origin and ground probe are also `unknown`. |
| Earliest airborne deployment after jump input | `unknown` s / frames | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding) specifies mid-air jump input, not an input delay or startup frame count. |
| Ordinary flat-ground jump → glider eligibility | `unknown` as a universal pinned-version rule | [KQM elevator evidence](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#elevator-affects-movement-speed) demonstrates that added upward momentum can produce sufficient height to deploy. It does not publish a universal clearance threshold. |
| Minimum stamina to deploy/redeploy; opening cost | `unknown` stamina units | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding), [Stamina](https://genshin-impact.fandom.com/wiki/Stamina); no separate measured activation gate/cost found. |
| Ordinary forward glider speed | `unknown` m/s | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding), [KQM movement evidence](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#movement-speed-and-gliding); no calibrated pinned-release measurement found. |
| Ordinary downward glider speed | `unknown` m/s | The older, assumption-calibrated descent estimate is already recorded in [stamina.md — movement speeds](stamina.md#movement-speeds-and-distance-calibration), with [the original community post](https://www.reddit.com/r/Genshin_Lore/comments/13zlgji/descent_speed_of_the_wind_glider_and_its/). It is not forward speed or pinned-version verification. |
| Ordinary drain per second | Use the sourced gliding row in [stamina.md](stamina.md#regeneration-and-ordinary-action-costs); pinned-version precision remains `unknown` | [Stamina — Consumption](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption); stronger provenance qualification below. |
| Horizontal acceleration, maximum turn rate, turn radius, and vertical deployment transient | `unknown` m/s², degrees/s, m, and m/s | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding); no measured steering/velocity curves found. |

**New provenance qualification for the existing drain value:** the [Gliding article's raw source](https://genshin-impact.fandom.com/wiki/Gliding?action=raw) calls the rate **“Approximated from player testing. Source wanted.”** The original [TapTap study](https://www.taptap.cn/moment/210923504076325385) provides a historical corroborating observation: an unmodified **240-stamina pool depleted in 80 s**, with [the author's gliding animation](https://img2-tc.tapimg.com/bbcode/images/2c89c7dc949c3ba246985867928cb2d7.gif/_tap_ugc.gif). This supports the already-tabulated approximate rate, but does not establish opening cost, tick quantization, idle-input behavior, or the pinned release. Its exact tested game version is `unknown`.

For elevated jump deployment, KQM supplies [the glider-capable elevator jump](https://imgur.com/a/YRVCmHX), [normal jump](https://imgur.com/a/mM9GuOg), and [diminished downward-elevator jump](https://imgur.com/a/TvnGQgD). The experiment was last tested in [Version 2.6](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#elevator-affects-movement-speed). This is evidence that launch momentum matters; it does not justify an arbitrary airborne timer or a height chosen from character proportions.

### Steering and speed modifiers

The character can change travel direction during a glide; a [community gliding guide](https://www.hoyolab.com/article/319630), explicitly for [Version 1.4](https://www.hoyolab.com/article/319630), describes direction changes and recommends straight routes rather than indecisive turning. The wiki also distinguishes movement-direction controls from camera rotation ([Controls](https://genshin-impact.fandom.com/wiki/Controls)). Exact camera-relative steering rules, residual drift with no movement input, reversal behavior, and whether turning changes stamina consumption in the pinned release remain `unknown`. Do not assume the camera direction itself is the commanded travel direction, or invent lift/stall physics.

Ordinary Movement SPD buffs are not ordinary glider-speed buffs. The statement already in [stamina.md](stamina.md#movement-speeds-and-distance-calibration) has **original tested evidence**: [KQM's experiment](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#movement-speed-and-gliding), [recording](https://youtu.be/p9445Lis2dE), and [reviewed discussion](https://tickets.deeznuts.moe/transcripts/movement-speed-and-gliding). It compares a buffed and unbuffed crossing and reports the same travel time; the discussion notes that the **visual model animation** can change without faster travel. Last tested release: [Version 4.4](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#movement-speed-and-gliding). Pinned-release retesting is unavailable.

Gliding-specific effects are a separate category: the wiki reports the **Red Feather Fan increases gliding speed by 30% for 30 s** ([Gliding](https://genshin-impact.fandom.com/wiki/Gliding), [raw source](https://genshin-impact.fandom.com/wiki/Gliding?action=raw)). That is not a measured base speed, and it does not establish how forward versus vertical components change. The gadget is outside the ordinary starter-party baseline.

### Wind Currents and rings

Wind Currents create an updraft. A character can jump in one to begin gliding, and an already-gliding character gains altitude toward the current's top ([Wind Current](https://genshin-impact.fandom.com/wiki/Wind_Current), [raw source](https://genshin-impact.fandom.com/wiki/Wind_Current?action=raw); [Gliding — Obstacles](https://genshin-impact.fandom.com/wiki/Gliding#Obstacles)).

| Mechanism/property | Published behavior/value with units | Source / remaining gap |
| --- | --- | --- |
| Gliding within a Wind Current | No stamina consumption: 0 stamina units/s | [Wind Current](https://genshin-impact.fandom.com/wiki/Wind_Current), [raw source](https://genshin-impact.fandom.com/wiki/Wind_Current?action=raw). |
| Stamina while sustained in a current | Can recover stamina while gliding | [Same source](https://genshin-impact.fandom.com/wiki/Wind_Current); no distinct current-specific rate/delay is published. General regeneration lives in [stamina.md](stamina.md#regeneration-and-ordinary-action-costs), but exact onset on entry is `unknown`. |
| Updraft ascent speed and vertical acceleration | `unknown` m/s and m/s² | [Wind Current](https://genshin-impact.fandom.com/wiki/Wind_Current). Current top/extent is an environmental property; no universal ascent constant found. |
| Current boundary, top holding behavior, exit transition | `unknown` m / s | Same source; do not infer a universal cylinder size or immediate velocity snap. |
| Anemo Ring | Brief directional speed boost while gliding through it; horizontal, vertical, or oblique | [Anemo Ring](https://genshin-impact.fandom.com/wiki/Anemo_Ring), [raw source](https://genshin-impact.fandom.com/wiki/Anemo_Ring?action=raw). The boost follows the ring-facing direction closest to existing movement, not necessarily horizontal forward. |
| Ring boost magnitude/duration | `unknown` m/s and s | [Anemo Ring](https://genshin-impact.fandom.com/wiki/Anemo_Ring) supplies direction but no calibrated ordinary boost. |

Event-specific gliding **Sprint** and **Ascend** commands are explicitly restricted to the Gliding Challenge in the [Gliding article](https://genshin-impact.fandom.com/wiki/Gliding?action=raw). Do not add those commands to ordinary gliding or reinterpret them as the player's universal steering capabilities.

### Closing the glider

| Event | Result | Source |
| --- | --- | --- |
| Jump action pressed again | Closes the glider and resumes falling | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding?action=raw). |
| Eligible normal-attack input | Cancels gliding into a plunge attack | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding?action=raw), [Plunging Attack](https://genshin-impact.fandom.com/wiki/Plunging_Attack). |
| Ground reached | Ends gliding | [Exploration — Player Mechanics](https://genshin-impact.fandom.com/wiki/Exploration#Player_Mechanics). Exact landing/auto-close clearance is `unknown` m. |
| Stamina exhaustion | Glider closes and character falls | [Exploration](https://genshin-impact.fandom.com/wiki/Exploration#Player_Mechanics). An eligible plunge remains possible after this exhaustion, unlike climbing exhaustion ([Plunging Attack](https://genshin-impact.fandom.com/wiki/Plunging_Attack#Plunging_Height)). |
| Incapacitation, e.g. Frozen; enemy interruption | Gliding can be cancelled into falling | [Gliding](https://genshin-impact.fandom.com/wiki/Gliding?action=raw), [Exploration](https://genshin-impact.fandom.com/wiki/Exploration#Player_Mechanics). Do not equate every damage instance with a forced close. |
| An explicitly mid-air-usable skill | Can close the glider; reopening is a separate action | [Gliding — Combat Talents](https://genshin-impact.fandom.com/wiki/Gliding?action=raw). Not all characters can cast skills mid-air, and helpless falls are excluded. |

Exact closure frames, repeated-open input gating, interrupted-fall recovery, and deployment velocity changes remain `unknown`. Cancelling high above ground is not fall-damage immunity; see the landing rules below.

## Player fall damage and landings

### Ordinary thresholds, formula, and lethality

Use [stamina.md — ordinary fall damage](stamina.md#ordinary-fall-damage-and-plunge-mitigation) for the already-sourced maximum-HP basis, bypassed defenses, maximum loss, and plunge-reduction bands. Directly readable supporting sources are [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG), [raw Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG?action=raw), and [HP Loss — public source](https://genshin-impact.fandom.com/wiki/HP/HP%20Loss?action=raw).

| Requested player-fall quantity | Pinned-version result with units | Evidence / constraint |
| --- | --- | --- |
| Highest no-damage fall height | `unknown` m | [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG) explicitly still requests exact-formula research. |
| Impact-speed threshold for first damage | `unknown` m/s | Same source; no measured player threshold found. |
| Ordinary damage curve | `unknown` % maximum HP as a function of height/impact motion | Same source says height and horizontal velocity affect damage, but gives no player formula. Do not invent a linear, quadratic, or piecewise curve. |
| Full-health lethal fall threshold | `unknown` m or m/s | Same source; no calibrated threshold. Current HP and impact motion must not be erased from the question. |
| Can ordinary fall damage kill? | **Yes** | [KQM's recorded deaths by player fall HP loss](https://library.keqingmains.com/evidence/general-mechanics/overworld#hanrocks), [original recording](https://youtu.be/7zwe-kSpQ7M), and [HP Loss](https://genshin-impact.fandom.com/wiki/HP/HP%20Loss?action=raw). There is no demonstrated universal survivable-height or automatic survival guarantee. |
| Rounding, accumulation/reset bookkeeping, and sloped-contact treatment | `unknown` HP units / frames | [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG) describes mitigations but does not expose their implementation. |

**Important exclusion:** the numeric impact-speed threshold and formula under [Fall DMG — Collision Damage](https://genshin-impact.fandom.com/wiki/Fall_DMG#Collision_Damage) apply to **enemies and certain objects**, not playable characters. KQM's [Fall Damage evidence page](https://library.keqingmains.com/evidence/combat-mechanics/damage/other/fall-damage) also tests damage to enemies. Neither supplies the missing player formula.

### Plunges and opening/cancelling the glider

- Starting a plunge can mitigate the landing. The relevant descent distance is measured from **plunge initiation**, not the start of a preceding free fall ([Fall DMG — Trivia](https://genshin-impact.fandom.com/wiki/Fall_DMG#Trivia)). The reduction table and unresolved band endpoints are already in [stamina.md](stamina.md#ordinary-fall-damage-and-plunge-mitigation); those percentages must not be silently converted into a new ordinary-fall formula.
- A plunge requires eligible airborne attack input; an ordinary flat-ground jump does not usually provide enough altitude ([Plunging Attack](https://genshin-impact.fandom.com/wiki/Plunging_Attack)). Glider exhaustion allows a subsequent plunge, whereas climbing exhaustion and airborne interruption can leave a helpless state from which a plunge cannot be initiated ([same source](https://genshin-impact.fandom.com/wiki/Plunging_Attack#Plunging_Height)).
- A plunge is **not a universal survival guarantee**. The [HP Loss table](https://genshin-impact.fandom.com/wiki/HP/HP%20Loss?action=raw) reports maximum-HP loss for damaging plunges, not a minimum remaining-HP floor. **[INFERENCE]** A sufficiently injured character can be killed by such a loss even when the plunge reduces an otherwise larger landing loss. No independently measured pinned-version survival floor was found.
- Opening the glider during an ordinary fall can reset the fall-damage risk; closing it again starts an unprotected descent from the subsequent airborne position ([Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG)). This is not permanent immunity for the whole journey. Exact reset frame, distance reference, retained horizontal speed, and minimum successful-open duration are `unknown`.
- **Exception to “opening always helps”:** [KQM's Spoutrock experiment](https://library.keqingmains.com/evidence/general-mechanics/overworld#hanrocks), last tested in [Version 2.6](https://library.keqingmains.com/evidence/general-mechanics/overworld#hanrocks), documents that gliding, using a mid-air skill, or starting a plunge removes that jump's special landing protection; a glide-cancelled Spoutrock jump can end in death ([original clips](https://youtu.be/7zwe-kSpQ7M)). Pinned-version persistence is `unknown`. The [current wiki](https://genshin-impact.fandom.com/wiki/Fall_DMG#Fall_Damage_Reduction) likewise qualifies the Spoutrock/Flighty Simulacrum protection as applying only without a plunge or glider opening. Do not generalize ordinary glider reset into preserving every special jump immunity.

Xiao and explicitly Nightsoul-aligned plunges have separate no-fall-damage rules ([Plunging Attack](https://genshin-impact.fandom.com/wiki/Plunging_Attack#Plunging_Height), [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG)). They are not starter-party defaults.

### Water, shallow landings, ice, and water walking

The [Fall DMG article](https://genshin-impact.fandom.com/wiki/Fall_DMG) specifically says falling into water whose level is **above the character's waist**, including wading, negates fall damage. This is more permissive than “must already be swimming,” and more restrictive than “any visible water is safe.” No numerical safe-water depth in metres is published.

| Landing surface/state | Published result | Source / remaining gap |
| --- | --- | --- |
| Water above the character's waist | Negates ordinary fall damage | [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG). Per-character waist depth in m is `unknown`. |
| Shallower water | No documented blanket immunity | Same source's depth qualification. Exact damage/contact rule is `unknown`; Wet status alone is not proof of a cushioned landing. |
| Frozen water / solid surface on water | Do not assume the water underneath cushions the solid-surface landing | [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG) compares water-walking impact with impact on ice. |
| Character/party water-walking talent active | Can take fall damage on the water surface as though it were solid | [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG). Each talent's applicability/activation is character-specific. |
| Safe water impact followed by exhausted surface swimming | Fall mitigation does not remove drowning risk | Water mitigation is from [Fall DMG](https://genshin-impact.fandom.com/wiki/Fall_DMG); ordinary drowning and Fontaine's separate mode are already covered in [stamina.md](stamina.md#drowning-and-region-distinction). |

A water-diving animation can trigger from a sufficiently high cliff jump, depending on character model. In diving-enabled Fontaine water it may initiate diving if sufficiently deep ([Jumping — Diving Animation](https://genshin-impact.fandom.com/wiki/Jumping#Diving_Animation)). Animation eligibility and water-impact immunity are not demonstrated to share a numerical threshold.

## Character height, jump height, and map scale

### Ordinary jump

| Jump quantity | Reported value with units | Source / reliability |
| --- | --- | --- |
| Ordinary unmodified jump apex | Approximately 1 m above take-off ground, measured to character feet | [Plunging Attack — Plunging Height](https://genshin-impact.fandom.com/wiki/Plunging_Attack#Plunging_Height). The wiki explicitly calls its height table approximate; original calibration and tested version are `unknown`. Not a verified per-model pinned-version constant. |
| Ordinary jump stamina cost | 0 stamina units | [Jumping](https://genshin-impact.fandom.com/wiki/Jumping). Do not apply the climb-jump cost to a ground jump. |
| Per-model jump apex, flight time, upward impulse, and gravity | `unknown` m, s, m/s, and m/s² for the pinned version | [Jumping](https://genshin-impact.fandom.com/wiki/Jumping), [KQM movement evidence](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics). No calibrated baseline curve found. |

Apex height means feet rise relative to the take-off surface, not head height above ground. Jump modifiers, moving platforms, dash-jumps, and wall jumps are not the unmodified baseline. [KQM's historical movement-speed/jump-height experiment](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#move-speed-increases-jump-height-and-double-anemo-allows-for-plunge-attacks), with [recording](https://youtu.be/OB6QP67zjNg), also means a buffed jump should not be used to calibrate the ordinary baseline; its last tested release is [Version 1.1](https://library.keqingmains.com/evidence/general-mechanics/movement-and-physics#move-speed-increases-jump-height-and-double-anemo-allows-for-plunge-attacks), not the pin.

### Directly readable community height measurement: useful ratios, assumed absolute scale

[WrexHavoc's Windward Manor height study](https://www.hoyolab.com/article/411488), with [original public article data](https://bbs-api-os.hoyolab.com/community/post/wapi/getPostFull?gids=2&post_id=411488), uses screenshots and the furnishing wall grid, not extracted character models. The author explicitly labels the heights speculative. The absolute calibration is **chosen**, not measured against the game's metre display: a **4.5 m ceiling** divided into **10 vertical grid cells**, each assumed **0.45 m** tall ([original study](https://www.hoyolab.com/article/411488)). Therefore this measures image/grid proportions but does not independently determine Genshin metres.

The table below preserves the author's sample/model categories and reported grid counts. Heights in metres are unit conversions of the source's centimetre results, not newly inferred character values.

| Source's model category | In-world sample used by author | Observed height (grid cells) | Author's estimated height (m) | Source |
| --- | --- | --- | --- | --- |
| Adult male | Kaeya | 3.9 | 1.755 | [Windward Manor study](https://www.hoyolab.com/article/411488) |
| Adult female | Beidou | 3.6 | 1.620 | [Same study](https://www.hoyolab.com/article/411488) |
| Young male | Chongyun | 3.4 | 1.530 | [Same study](https://www.hoyolab.com/article/411488) |
| Young female | Fischl | 3.3 | 1.485 | [Same study](https://www.hoyolab.com/article/411488) |
| Child female | Diona | 2.6 | 1.170 | [Same study](https://www.hoyolab.com/article/411488) |

These are **historical, assumption-dependent estimates**, not official heights or collision-box dimensions. The source does not provide directly measured starter-party heights for Traveler, Amber, or Lisa. Do not copy the sample height onto another character merely because they share a model category. The number of model categories described by an older study is not a pinned-version roster specification.

### Other community heights: search-index-only and corrected by their author

[TheMuteObserver's in-game height-estimate post](https://www.reddit.com/r/Genshin_Impact/comments/pnv56v/genshin_impact_in_game_height_estimates/) reports scaled screenshot comparisons in Blender. **Only search-indexed post text was accessible in this research; direct Reddit reads were blocked.** Its starting scale assumes Aether's fan-estimated height rather than independently measuring an in-game metre. The indexed text also says the author later found exaggerated medium-female/tall-male estimates and linked a correction; the corrected spreadsheet/video contents could not be retrieved here. The following are the **original, low-reliability estimates**, not an adopted or corrected chart.

| Character | Original barefoot estimate (m) | Original footwear-inclusive estimate (m) | Source / status |
| --- | --- | --- | --- |
| Aether | 1.64 | 1.67 | [Original indexed post](https://www.reddit.com/r/Genshin_Impact/comments/pnv56v/genshin_impact_in_game_height_estimates/); Aether's assumed barefoot value is the calibration input, not an independent result. |
| Lumine | 1.57 | 1.62 | [Same original indexed post](https://www.reddit.com/r/Genshin_Impact/comments/pnv56v/genshin_impact_in_game_height_estimates/); subsequent correction not independently available. |
| Amber | 1.60 | 1.63 | [Same original indexed post](https://www.reddit.com/r/Genshin_Impact/comments/pnv56v/genshin_impact_in_game_height_estimates/); subsequent correction not independently available. |
| Kaeya | 1.90 | 1.93 | [Same original indexed post](https://www.reddit.com/r/Genshin_Impact/comments/pnv56v/genshin_impact_in_game_height_estimates/); subsequent correction not independently available. |
| Lisa | `unknown` | `unknown` | No independently retrievable original measurement found; a generic adult-female estimate is not a Lisa measurement. |

**Disagreement is not resolved:** Kaeya's absolute estimate in this chart differs substantially from the Windward Manor sample. The starting calibrations and treatment of footwear/hair are different; no evidence here identifies the disagreement as an actual model-height change between releases. The older glider-descent experiment's separate Traveler-height assumption is already explained in [stamina.md](stamina.md#movement-speeds-and-distance-calibration). None of these gives an official or independently calibrated pinned-version Traveler height.

### Calibration conclusion

**Do not choose the map's block-to-metre scale from these absolute fan heights alone.** The brief explicitly requires [landmark calibration and local map measurement](../../docs/GENSHIN_MINECRAFT_BRIEF.md#8-map-as-a-content-overlay). The proposal **1 Minecraft block = 1 m** is a project scale choice, not a sourced Genshin conversion; its suitability remains `unknown` ([brief — traversal](../../docs/GENSHIN_MINECRAFT_BRIEF.md#4-traversal)).

An independent distance reference is necessary before character pixels or travel times become metre measurements. Assuming a character's height to derive glider speed, then using that glider speed to confirm character height, is circular. Visual model height, footwear-inclusive height, collision height, and feet displacement at jump apex are different quantities; no common conversion between them is established here.

## Version changes, conflicts, and remaining unknowns

- **Pinned-release verification:** exact numerical traversal constants are not independently confirmed for the [pinned version](../../docs/GENSHIN_MINECRAFT_BRIEF.md#fixed-decisions). No sourced change to the ordinary climb/glide speeds, slope threshold, ordinary jump height, or player fall curve was found. “No change found” is not proof that older tests remain valid.
- **Demonstrated presentation change:** the [HP Loss source](https://genshin-impact.fandom.com/wiki/HP/HP%20Loss?action=raw) says certain HP-loss cases began displaying numbers like Physical DMG/CRIT DMG in [Version “Luna I”](https://genshin-impact.fandom.com/wiki/Version_%22Luna_I%22), without becoming combat damage. It does not identify ordinary fall loss as a particular changed case. Do not infer a changed fall formula or shield interaction from a damage-like display.
- **New explicit conflict:** the citation-needed wiki climb-jump cost and the historical depletion-derived cost disagree. Both are preserved above; cause/change date is `unknown`.
- **Height conflict:** independent fan charts use incompatible assumed metre scales; the original indexed screenshot chart also has an author-reported correction that was not retrievable. No chart is silently selected.
- **Outstanding numeric unknowns:** climbing's walking/attachment and overhang angle boundaries; calibrated climb speeds and per-second drain; climb-jump metre displacement/timing and exact cost; mantle geometry/timing/billing; glider deployment height/timing/stamina gate; forward/descent velocities and steering/transients; current ascent/boundaries and ring impulse; the complete player fall formula and safe/damaging/lethal thresholds; per-model absolute height and ordinary jump curves for the pinned release.

### Least reliable values

| Evidence/value class | Why it is least reliable | Source |
| --- | --- | --- |
| Absolute Windward Manor heights | Chosen ceiling scale; image/grid proportions do not establish metres | [Original study](https://www.hoyolab.com/article/411488) |
| Original indexed screenshot height chart | Assumed Aether reference, inaccessible direct post, and author-reported later correction | [Original post](https://www.reddit.com/r/Genshin_Impact/comments/pnv56v/genshin_impact_in_game_height_estimates/) |
| Historical glider descent estimate already in `stamina.md` | Depends on assumed Traveler height; no pinned-version retest | [Original experiment](https://www.reddit.com/r/Genshin_Lore/comments/13zlgji/descent_speed_of_the_wind_glider_and_its/) |
| Climb-jump and animation-count climbing costs | Citation-needed wiki value versus depletion-count deduction; final partial spending/animation timing not measured | [Wiki](https://genshin-impact.fandom.com/wiki/Stamina#Stamina_Consumption), [original TapTap study](https://www.taptap.cn/moment/210923504076325385) |
| Approximate ordinary jump apex | No original calibration or per-model/version measurement supplied | [Plunging Attack height table](https://genshin-impact.fandom.com/wiki/Plunging_Attack#Plunging_Height) |
| Precise ordinary glider drain | Wiki itself calls it approximated; older timed study corroborates the rate but not fine-grained billing or the pin | [Raw Gliding source](https://genshin-impact.fandom.com/wiki/Gliding?action=raw), [original TapTap study](https://www.taptap.cn/moment/210923504076325385) |

This is documentation research only. No gameplay measurements, simulation checks, builds, tests, linters, or formatters were run. Before these become implementation constants, the main agent should source-review the historical and citation-needed rows and obtain pinned-release observations for the listed unknowns; no computational test can establish a missing game-mechanics measurement.
