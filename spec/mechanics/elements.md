# Elemental application, reactions, ICD, and energy

## Scope and source interpretation

Pinned reference: **[Genshin Impact Version 7.1][V]**. Main rules below cover ordinary Anemo/Pyro/Cryo/Electro/Hydro interactions for Traveler (Anemo), Amber, Kaeya, and Lisa with environmental Hydro. Crystallize is intentionally skipped. Conditional Lunar/ Stellar reactions are noted separately so the normal pair table is not misrepresented as universal in the pinned game.

`U` (also `GU`) means gauge units. `u` is an incoming source's **untaxed** gauge strength; `g` is the remaining **post-tax aura** on the target. Do not mix them. A source link in a table row covers every numeric cell in that row. KQM's living summaries contain tests from earlier patches; continuity into the pin is **[INFERENCE]** except where release-specific notes are cited. Unknowns and contradictory/historical findings are retained below rather than converted into exact test expectations.

## Aura application and natural decay

Ordinary Cryo, Electro, Hydro, and Pyro can be applied as auras. **Anemo and Geo cannot be applied as ordinary auras**, and therefore only act as triggers for their reactions [source][G]. Damage element alone does not guarantee application: elemental-application ICD and zero-gauge attacks matter.

A source applying a fresh aura has the **0.8× aura tax [source][G]**, independently confirmed by friendly-aura frame testing in the [KQM evidence vault][GE]. Reaction triggers do **not** get this aura tax when calculating gauge consumption. An elemental trigger that over-consumes an ordinary aura still triggers the reaction, clears the aura, and does not install its unused gauge as a fresh aura. Electro-Charged and self/innate auras have special rules [source][G].

For an isolated fresh aura from an ordinary source, all constants in the following formulas are from [KQM Decay Rate][G]:

\[
g_0=0.8u,\qquad T_{fresh}(u)=2.5u+7\ \mathrm{s},
\qquad D(u)=\frac{T_{fresh}(u)}{0.8u}\ \mathrm{s/U}.
\]

KQM also writes `D = 875/(4 × durability) + 25/8`, with **25 durability units per U [source][G]**. `D` in that notation is seconds **per gauge unit**, not the aura's total duration. The linear gauge loss rate is its reciprocal. Remaining duration is remaining gauge times `D` only while the same rate remains applicable.

| Incoming source (U) | Fresh aura after tax (U) | Isolated fresh duration (s) | Decay time per U (s/U) | Source |
| ---: | ---: | ---: | ---: | --- |
| 1 | 0.8 | 9.5 | 11.875 | [KQM][G] |
| 2 | 1.6 | 12.0 | 7.5 | [KQM][G] |
| 4 | 3.2 | 17.0 | 5.3125 | [KQM][G] |

Reapplications are **not simple gauge addition**. KQM says the first aura's decay rate normally stays until that aura is completely consumed/decayed, even when a later application changes the amount of gauge [source][G]. Its [consecutive-aura evidence][AR] says a weaker new gauge changes nothing while the old remaining gauge is larger, and a stronger new gauge sets the larger amount. In post-tax notation that is `g_new = max(g_remaining, 0.8u)` **[algebraic restatement of those sourced comparisons][AR]**, not `g += 0.8u`. The older evidence predates Pyro's refresh exception below; this max-amount rule does not remove that newer rate-update exception.

**Pyro exception:** since **Version 3.0 [source][G]**, a new Pyro application keeps the current decay rate if it does not change aura amount; if it changes aura amount, the rate updates to the trigger's decay rate. This exception remains relevant to the starter party and must not be replaced by a universal “first application locks forever” rule.

Innate/shield/self auras are not interchangeable with player-applied taxed auras. KQM says a stronger self-aura application that reacts can directly replace the existing aura; enemy shields and innate auras belong to that category [source][G]. Specific shield durability belongs to the enemy specs.

## Ordinary reaction consumption for every in-scope pair

For immediate ordinary reactions, consume `min(g, c × u)` of existing aura: `c` is a dimensionless gauge-consumption modifier, not a damage multiplier. Clamp remaining gauge at the empty state. Tax applies when becoming an aura, **not again to `c × u`** [source][G]. Frozen/EC have additional state described below.

| Existing aura | Incoming trigger | Reaction | Gauge consumption modifier / rule | Source |
| --- | --- | --- | --- | --- |
| Pyro | Anemo | Pyro Swirl | 0.5× trigger gauge | [KQM][G] |
| Cryo / Frozen | Anemo | Cryo Swirl | 0.5× trigger gauge | [KQM][G] |
| Electro | Anemo | Electro Swirl | 0.5× trigger gauge | [KQM][G] |
| Hydro | Anemo | Hydro Swirl | 0.5× trigger gauge | [KQM][G] |
| Anemo | Pyro / Cryo / Electro / Hydro | No ordinary reverse Swirl | Anemo aura does not exist; incoming eligible element may become aura | [KQM][G] |
| Cryo / Frozen | Pyro | Forward Melt | 2.0× trigger gauge | [KQM][G] |
| Pyro | Cryo | Reverse Melt | 0.5× trigger gauge | [KQM][G] |
| Pyro | Hydro | Forward Vaporize | 2.0× trigger gauge | [KQM][G] |
| Hydro | Pyro | Reverse Vaporize | 0.5× trigger gauge | [KQM][G] |
| Pyro | Electro | Overloaded | 1.0× trigger gauge | [KQM][G] |
| Electro | Pyro | Overloaded | 1.0× trigger gauge | [KQM][G] |
| Cryo / Frozen | Electro | Superconduct | 1.0× trigger gauge | [KQM][G] |
| Electro | Cryo | Superconduct | 1.0× trigger gauge | [KQM][G] |
| Hydro | Cryo | Frozen | 1.0× trigger gauge consumed from existing Hydro; generate separate Frozen gauge | [Original Freeze testing][FR] |
| Cryo | Hydro | Frozen | 1.0× trigger gauge consumed from existing Cryo; generate separate Frozen gauge | [Original Freeze testing][FR] |
| Hydro | Electro | Electro-Charged | Coexisting auras; each damaging tick removes 0.4 U from **each** aura | [KQM][G], [reaction mechanics][T] |
| Electro | Hydro | Electro-Charged | Coexisting auras; each damaging tick removes 0.4 U from **each** aura | [KQM][G], [reaction mechanics][T] |
| Same element | Same element | No new pair reaction | Aura refresh rules, not reaction consumption | [KQM][G] |

All distinct pairs among the in-scope elements are represented; reverse Anemo directions are impossible on an ordinary aura. Geo/Crystallize and Dendro reactions are outside this assigned pair table. Shatter is a secondary **blunt-attack-on-Frozen** interaction, not an additional element pair. Damage coefficients and RES elements are in [damage.md](damage.md).

Overloaded and Superconduct reaction AoE damage have **zero gauge [source][G]** and cannot apply fresh Pyro/Cryo from that reaction damage. EC chain damage likewise has **zero gauge [source][G]**. Swirl spread is different.

## Swirl spread and multiple auras

Swirl can spread the aura element to nearby targets, applying gauge or triggering further reactions; a spread application that becomes an aura is taxed [source][G]. Hydro dispersion applies Hydro but does not deal surrounding Hydro Swirl damage; the original target does take Hydro Swirl damage [source][T]. Swirl and a talent's elemental absorption are different effects.

Published gauge examples only; no general interpolation is implied:

| Remaining origin aura (U) | Anemo trigger (U) | Swirl-spread source (U) | New spread aura after tax (U) | Published spread decay time (s/U, approximate) | Source |
| ---: | ---: | ---: | ---: | ---: | --- |
| 0.8 | 1 | 2.2 | 1.76 | ~7.10227 | [KQM Swirl Application][G] |
| 0.8 | 2 | 1.95 | 1.56 | ~7.61218 | [KQM Swirl Application][G] |
| 1.6 | 2 | 3.45 | 2.76 | ~5.66123 | [KQM Swirl Application][G] |

The complete spread-gauge formula for arbitrary aura/trigger strengths is **unknown here**; these examples must not become special-case substitutes for that missing general rule.

A target can have coexisting Hydro/Electro from EC or Frozen with underlying Hydro/Cryo. Do not implement it with a single element enum. KQM's [double-Swirl testing][DS] says Anemo can Swirl both EC auras only when its gauge reduction exceeds the Electro gauge; otherwise Electro is swirled alone. On Frozen with underlying Hydro, the corresponding threshold is the Hydro gauge; below that threshold Hydro alone is swirled [source][DS]. These are special multiple-aura rules, not independent application of every pair-table row without ordering.

## Electro-Charged ticks

The trigger can persist as a taxed aura alongside the original element rather than disappearing like an ordinary reaction trigger [source][G]. Both gauges also undergo natural decay independently.

| Mechanic | Value | Unit / qualification | Source |
| --- | ---: | --- | --- |
| Ordinary tick interval | 1.0 | s, beginning with initial reaction tick; hitlag can extend primary-target timing | [KQM][G], [KQM][T] |
| Gauge removed per damaging tick | 0.4 | U **from Hydro and from Electro separately** | [KQM][G] |
| Premature final-tick exclusion window | 0.5 | s since preceding tick; if an aura disappears within this window, no extra early tick | [KQM][G] |
| Secondary-target tick spacing | 60 | frames, not extended by hitlag | [KQM][T] |
| EC damage cooldown | ~0.5 | s per target, separate from elemental application ICD | [KQM][T] |

If a gauge naturally expires before the next scheduled tick and sufficiently long after the preceding tick, the final tick can occur early at expiry [source][G]. KQM's reaction page adds that gauge is consumed only when EC deals damage and only when both gauges exist; rejected damage still does not stop natural decay [source][T]. The other aura can survive when its partner is removed. EC ticks themselves do not count as new reaction triggers for generic on-reaction effects; reapplications can trigger such effects [source][T].

Ownership depends on relevant applications rather than simply element identity. KQM reports the last element applier's EM snapshot and ownership changes from secondary spread; zero-gauge Electro does not take ownership [source][T]. EC's patch-era bug and exact cooldown ordering are explicitly unresolved below.

## Frozen gauge and decay

Freeze is nondamaging. Let `a` be the **remaining existing Hydro or Cryo aura**, in post-tax U, immediately before the trigger; let `u` be the untaxed opposite-element trigger. The Frozen gauge, including its constants, is [KQM's published formula][FD]:

\[
F=2\min(a,u)\quad\mathrm{U}.
\]

Consume the original aura according to the pair table, and keep any remaining original aura underneath Frozen. Do not apply another aura tax to `F`. If the original aura was fresh and had no refresh/reaction in the interim, the [original duration testing][FD] gives:

\[
a=(0.8u_{origin})\left(1-\frac{\Delta t}{2.5u_{origin}+7}\right).
\]

That shortcut is not valid after arbitrary refreshes or reactions; use the actual current gauge instead. In ordinary initial Freeze without freeze resistance or accumulated repeat-Freeze acceleration, [KQM][FD] gives:

\[
T_F=2\sqrt{5F+4}-4\quad\mathrm{s}.
\]

This is not ordinary linear aura decay. The kinematic constants are [KQM Frozen][T]:

| Frozen-state quantity | Value | Unit | Source |
| --- | ---: | --- | --- |
| Minimum initial Frozen gauge loss rate | 0.4 | U/s | [KQM][T] |
| Increase of gauge-loss rate while Frozen | 0.1 | U/s² | [KQM][T] |
| Change of gauge-loss rate while unfrozen | −0.2 | U/s², toward the minimum rate | [KQM][T] |

For the initial, no-resistance case, `F(t) = F(0) − 0.4t − 0.05t²` follows algebraically from those sourced constants [source][T]; terminate Frozen when gauge reaches the empty state. The quadratic coefficient is a derived integration coefficient, not an interpolated datum.

KQM generalizes repeat-Freeze duration using a decay-time modifier `d` carrying previous frozen/unfrozen history [source][T]:

\[
d=\sum_i\max(0,t_{frozen,i}-2t_{unfrozen,i}),\qquad
T_F=\sqrt{20F+(d+4)^2}-d-4.
\]

The [original extension ticket][FE] contains inconsistent signs/notation and fitted values; see the conflict table. The kinematic state above is clearer than treating that ticket's raw textual formula as an exact executable oracle.

### Underlying auras and freeze resistance

Hydro/Cryo can coexist with Frozen and decay while hidden. Reapplying opposite elements against underlying aura can extend Frozen. Frozen behaves Cryo-like for Melt/Superconduct/Swirl, but does not replace the underlying gauges [source][G], [KQM Frozen][T].

- Non-blunt Pyro on Frozen with underlying Hydro ordinarily Melts Frozen rather than also Vaporizing the Hydro. With underlying Cryo, Melt can consume both Frozen and underlying Cryo [source][T].
- Blunt elemental hits can Shatter first and then react with an exposed underlying aura; elemental damage without an underlying aura is not automatically Melt/Vaporize [source][T].
- Shatter ordinarily removes **8 U of Frozen gauge [source][T]**; KQM documents partial/multiple-Shatter anomalies as apparently bug-like and attack-dependent. Exact pinned-patch persistence of those anomalies is **unknown**, not permission to assume every blunt hit always clears every Frozen target.
- The wiki adds a separate pre-Shatter step: blunt attacks consume Frozen gauge according to poise damage first, and Shatter triggers only if Frozen gauge remains afterward [source][FW]. KQM's simple Shatter summary does not describe this prerequisite. Its exact poise-to-gauge coefficient is **unknown** in the read page, so a blanket “every blunt contact triggers Shatter damage” is unsafe.

For a fresh Freeze with target freeze resistance `r_F`, the corrected [KQM evidence][FC] is:

\[
F=2\min(a,u),\qquad T_F=2\sqrt{5F(1-r_F)+4}-4\quad\mathrm{s}.
\]

**Freeze resistance changes duration, not reaction gauge.** The older ticket's reduction of the gauge itself was explicitly corrected. A full combined formula/state evolution for repeat-Freeze history plus nonzero freeze resistance is **unknown** in the read sources. Do not silently derive an untested combination or confuse freeze resistance with Cryo damage RES.

## Elemental application ICD and tags

Standard ICD is **3 hits OR 2.5 s [KQM][I]**, not both requirements together. The first eligible hit applies. Within a timer window, the next applying hit is separated by the hit-count rule. The timer is started/reset by timer-eligible application; a hit-count application **does not reset that timer**. When the next hit arrives at or beyond the timer reset interval, the hit counter resets and that hit can apply [source][I]. Failed applications still deal their talent damage; only aura/trigger application is prevented.

[KQM's advanced explanation][I] and the [wiki ICD dataset][ID] specify a finite standard gauge sequence rather than an unlimited modulo counter:

| ICD component | Published value | Units / meaning | Source |
| --- | --- | --- | --- |
| Standard reset interval | 2.5 | s | [KQM][I], [wiki][ID] |
| Standard applying indices before timer reset | 1, 4, 7, 10, 13, 16, 19, 22 | hit index in the window | [KQM][I], [wiki][ID] |
| Standard finite sequence length | 24 | hits; subsequent attenuation is zero until timer reset | [KQM][I], [wiki][ID] |
| Applying attenuation factor | 1 | dimensionless gauge multiplier | [KQM][I] |
| Blocked attenuation factor | 0 | dimensionless gauge multiplier | [KQM][I] |
| Amber-specific reset interval | 1.0 | s, not the standard interval | [Wiki][ID] |
| Amber-specific hit rule | 3 | hits | [Wiki][ID] |

A tag (“Attenuation Tag”) identifies which attacks share application state; the type (“Attenuation Group”) specifies the timer and gauge sequence. **Same character + same tag + same type** share ICD; otherwise they are independent [source][ID]. State is also target-specific and character-specific [source][I]. Do not create a party-wide Pyro timer, or one timer for an entire Skill just because it has one button. Attacks without a tag have no application ICD [source][ID]. “No ICD” is distinct from zero-gauge damage.

A starter example is Lisa's Normal Attacks and press Skill sharing the **Lisa Electro DMG** tag with standard type, while the burst discharge uses the **Elemental Burst** tag and the charged attack/hold Skill have no application ICD [source][LI]. Numeric gauge strengths and exact ability-specific groups belong in the individual character specs; do not impose standard ICD on all abilities.

### Reaction damage ICD is different

These limits concern ordinary reaction damage, not whether the triggering elemental talent applies gauge [source][T], with original testing in the [KQM evidence vault][TE].

| Reaction damage | Limit | Interval (s) | Scope / effect | Source |
| --- | --- | ---: | --- | --- |
| Overloaded | 1 damage instance | 0.5 | Per character/target; further reactions still consume gauge and stagger | [Original testing][OI] |
| Superconduct | 2 damage instances | 0.5 | Per character/target; further reactions still consume gauge and stagger | [Original testing][SI] |
| Swirl | 2 damage instances per element | 0.5 | Per character/target; different Swirl elements are separate | [KQM advanced ICD][I] |
| Shattered | 2 damage instances | 0.5 | Same target; KQM summary does not fully state owner scope | [KQM][T] |

The wiki additionally claims a **0.1 s same-target Superconduct repeat-trigger damage restriction regardless of source [source][SC]**, which is absent from KQM's reaction summary. This is an additional poorly corroborated restriction, not silently merged into a golden expectation.

## Energy particles and orbs

The following are **base energy units per collected item before the receiving character's ER multiplier**. “Same element” compares each recipient's element to the particle/orb element, not the element of whoever generated/collected it. Receiving a particle/orb gives party members energy, with off-field attenuation; do not divide its fixed amount among the party [source][E]. An orb supplies **3× a particle's energy [source][E]**. Clear/neutral items have no matching element.

| Recipient | Party size (characters) | Same-element particle (energy/item) | Different-element particle (energy/item) | Clear particle (energy/item) | Same-element orb (energy/item) | Different-element orb (energy/item) | Clear orb (energy/item) | Source for entire row |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| On-field | Any | 3.0 | 1.0 | 2.0 | 9.0 | 3.0 | 6.0 | [KQM energy table][E] |
| Off-field | 4 | 1.8 | 0.6 | 1.2 | 5.4 | 1.8 | 3.6 | [KQM energy table][E] |
| Off-field | 3 | 2.1 | 0.7 | 1.4 | 6.3 | 2.1 | 4.2 | [KQM energy table][E] |
| Off-field | 2 | 2.4 | 0.8 | 1.6 | 7.2 | 2.4 | 4.8 | [KQM energy table][E] |

| Energy rule | Value | Unit / qualification | Source |
| --- | ---: | --- | --- |
| Same-element gain relative to different-element item | 3.0 | × | [KQM][E] |
| Clear gain relative to different-element item | 2.0 | × | [KQM][E] |
| Off-field factor, full party | 0.6 | ×, party of 4 | [KQM][E] |
| Off-field factor, smaller party | 0.7 | ×, party of 3 | [KQM][E] |
| Off-field factor, smaller party | 0.8 | ×, party of 2 | [KQM][E] |

`Energy_received = item_base_energy × recipient_ER`, with ER as a multiplier [source][E]. ER affects incoming energy, not particle count/production. Flat energy from talents/weapons is not multiplied by ER or shared automatically like particles [source][E]. Skill-specific particle quantities belong to the character specs. Co-op ownership/range sharing is **unknown in this table**; this single-party table is not a verified co-op distance model.

## Changes relevant to the pinned era

- Ordinary reaction damage multipliers changed in the post-legacy period; current coefficients and sourced historical comparisons are in [damage.md](damage.md). Do not use old damage values to infer a gauge-consumption change: the read sources do not document such a change to these ordinary pairs.
- Pyro's decay refresh exception is already incorporated above [source][G].
- **Lunar-Charged:** [KQM Lunar guide][LU] documents a passive-gated Hydro/Electro conversion. It preserves coexistence and **0.4 U per aura per tick**, but ticks at **approximately 2 s instead of ordinary EC's approximately 1 s [source][LU]**. The thundercloud is multi-contributor and crit-capable, not a rename of ordinary EC. Guide roster: Ineffa, Flins, Columbina. No starter-party member is one of those enablers **[INFERENCE from roster]**. The guide is labeled *Luna VI*, so exact pinned-patch full roster is **unknown** here. Lunar-Bloom requires Dendro and Lunar-Crystallize requires Geo, outside the assigned pair scope.
- **Stellar-Conduct and Stellar Swirl:** [KQM Stellar guide][ST] documents Stellar Linchpin-gated Cryo/Electro and Anemo/Cryo conversions with the **same gauge-consumption pattern** as their ordinary counterparts. Stellar Swirl does not immediately spread Cryo; the vortex explosion applies **1 U Cryo after 3 s [source][ST]**, subject to its distinct vortex mechanics. Stellar-Conduct's triggering reaction does not directly deal damage, unlike ordinary Superconduct. The older guide lists Sandrone, Odette, and Cryo Traveler; this is **not a complete pinned-version roster**.
- The pinned notes introduce **Vesna** as a Stellar Swirl enabler (official text available through its [mirror][MIR], [official URL][OFF]) and explicitly fix the calculation sometimes excluding Cryo contributors [source][V]. This makes the older Stellar guide's enabler roster and pre-patch buggy contributor behavior unsuitable as pinned-version expectations. Neither the new enabler nor the older named enablers are in the starter party **[INFERENCE]**.

## Conflicts, unknowns, and least reliable values

| Topic | Source claims | Handling |
| --- | --- | --- |
| Strong aura duration | Older gauge-vault beginner guide says **4 U lasts 16.8 s**; current summary says formula **2.5u + 7 s**, giving **17 s** at that strength; seconds-per-U differ accordingly [old evidence][GE], [current summary][G] | Explicit historical disagreement. Current summary is used for the normal table; pinned-version direct retest is **unknown**. |
| Frozen repeat-fit constants | Extension ticket reports fitted **a = −1.87 ± 0.01, b = 4.09 ± 0.02**, reported data constants **a = −2, b = 4**, and elsewhere recovery magnitude **1.8–2.0**; its displayed subtraction/sign notation is inconsistent [source][FE]. Summary uses recovery **−0.2 U/s²** and **0.4 U/s** minimum [source][T] | Do not silently equate the fit and source-data values or copy the inconsistent formula. Repeat-Freeze exact fit/sign reconstruction is among the **least reliable** results. |
| Freeze resistance | Older ticket multiplies **Frozen gauge** by `(1 − resistance)` [old finding][FO]; correction keeps full `F = 2 min(a,u)` and applies resistance only in duration [corrected testing][FC] | Explicitly superseded finding, not two interchangeable models. |
| EC final tick and cooldown | Gauge summary gives **1 s** ticks and **0.5 s** expiry exclusion [source][G]; reaction summary gives approximate **0.5 s** damage cooldown with damage-only gauge consumption [source][T]; oldest vault says EC can only damage once per **1 s** before describing exceptions [source][TE] | Simple baseline tick rules are sourced; precise event-ordering at cooldown/expiry boundaries is **unknown** and among the **least reliable** values. |
| Electro-trigger EC residue | Reaction page still reports a **Version 2.5** bug: Electro does not persist after ticks even when Electro exceeds Hydro [source][T], conflicting with the baseline surviving-other-aura model [source][G] | Pinned-version persistence/fix status **unknown**. Do not faithfully reproduce an older bug by assumption. |
| Superconduct extra damage restriction | Wiki claims **0.1 s** global-to-source repeat-trigger restriction [source][SC]; KQM documents **2 damage instances / 0.5 s per character** but omits that additional restriction [source][SI] | Possibly complementary rather than contradictory, but the added restriction is **least reliable** without linked primary corroboration. |
| Swirl spread | Summary gives gauge examples, not a complete arbitrary-input spread formula [source][G] | **unknown**; avoid interpolation of spread examples. |
| Conditional reaction completeness | Lunar guide's version label and Stellar guide's roster predate pin-specific content/fixes [source][LU], [source][ST], [pinned notes][V] | Gating notes are usable; complete pinned rosters, hidden precision, and all conditional formulas are **unknown** here. |

Additional unknowns: exact combined repeat-Freeze/freeze-resistance state equation; generic partial-Shatter behavior in the pinned release; environmental water gauge ownership and co-op collection range. These do not negate the sourced ordinary pair/energy tables, but must not be disguised as exact test expectations.

No builds, tests, or in-game checks were run. Main reviewer should spot-check transcription and version labels, then author gauge/ICD expectations for trigger-versus-aura tax, directional Melt/Vaporize consumption, separate EC gauges, Frozen's quadratic decay, target/owner/tag isolation, the finite standard sequence, and every energy-table entry. Conflicting/unknown cases require independent reference evidence before becoming exact tests.

[G]: https://library.keqingmains.com/combat-mechanics/elemental-effects/elemental-gauge-theory
[GE]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/elemental-gauge-theory
[T]: https://library.keqingmains.com/combat-mechanics/elemental-effects/transformative-reactions
[TE]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions
[FR]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#freeze-aura-mechanics
[FD]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#duration-of-freeze-aura
[FE]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#freeze-extensions
[FO]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#freeze-resistance
[FC]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#freeze-resistance-correction
[DS]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#how-to-get-double-swirls
[OI]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#overload-reaction-icd
[SI]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/transformative-reactions#superconduct-mechanic-update
[SC]: https://genshin-impact.fandom.com/wiki/Superconduct
[I]: https://library.keqingmains.com/combat-mechanics/internal-cooldown
[ID]: https://genshin-impact.fandom.com/wiki/Internal_Cooldown/Data
[LI]: https://genshin-impact.fandom.com/wiki/Lisa
[E]: https://library.keqingmains.com/combat-mechanics/energy
[V]: https://genshin-impact.fandom.com/wiki/Version/7.1
[OFF]: https://genshin.hoyoverse.com/m/en/news/detail/166383
[MIR]: https://gamevika.com/en/genshin/event/a-rekviem-for-the-underworld-version-71-update-details-21946
[LU]: https://keqingmains.com/misc/lunar/
[ST]: https://keqingmains.com/misc/stellar/
[AR]: https://library.keqingmains.com/evidence/combat-mechanics/elemental-effects/elemental-gauge-theory#what-happens-when-you-apply-consecutive-auras-of-different-u-values
[FW]: https://genshin-impact.fandom.com/wiki/Frozen#Shatter
