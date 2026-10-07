# Co-op: rosters, combat, recovery, rewards, and world permissions

Reference target: **Genshin Impact Version 7.1**, pinned in the [project brief](../../docs/GENSHIN_MINECRAFT_BRIEF.md#fixed-decisions). Read this together with [party.md](party.md): co-op does not give every player a full solo roster.

**Evidence status:** this is public-source research, not measurements in the pinned release. Sources are current Genshin community wikis, KQM, official HoYoverse support, and published community observations. No datamines were used. Historical descriptions are identified rather than silently substituted for the target. `unknown` means no sufficiently specific accessible evidence was found; `[INFERENCE]` is not an experimentally established rule. Source links in table rows apply to all numeric cells in that row.

## Roster allocation and control

The session's normal combat roster is shared in capacity, **not** in control or character ownership. Each player controls their own on-field avatar and switches only among their own allocated characters. Another player's character is never a selectable switch target. The normal allocations below are documented by the [Co-Op Mode wiki][coop] and an [original community co-op guide][old-guide].

| Players in the world (players) | Host's allocated roster (characters) | Each guest's allocated roster (characters) | Combined allocated roster (characters) | Simultaneously on-field (characters) | Source |
| --- | --- | --- | --- | --- | --- |
| 1 | Up to 4 | Not applicable | Up to 4 | 1 | [Party wiki][party-wiki] |
| 2 | 2 | 2 | 4 | 2 | [Co-Op Mode][coop]; [original guide][old-guide] |
| 3 | 2 | 1 | 4 | 3 | [Co-Op Mode][coop]; [original guide][old-guide] |
| 4 | 1 | 1 | 4 | 4 | [Co-Op Mode][coop]; [original guide][old-guide] |

- Switching between already allocated characters is allowed during combat. Replacing the allocated roster through Party Setup is not allowed in combat; this is distinct from switching. Ordinary switching restrictions remain those in [party.md](party.md). Sources: [Co-Op Mode][coop], [Error — Party Setup restrictions][errors].
- Character builds, HP, Energy, weapons, and talents belong to the controlling player's character, not to a pooled host character. **[INFERENCE]** This follows from each participant choosing their own characters and receiving individual rewards; these sources do not describe the exact network authority or serialization scheme. Sources: [Co-Op Mode][coop], [original guide][old-guide].
- An unused owned character outside the allocated co-op roster is not an extra combat slot. Changing roster allocation on joins/leaves is discussed below; do not assume the entire solo account roster stays active. Source: [Co-Op Mode][coop].

### Duplicate characters and Traveler

| Context | Rule | Source / qualification |
| --- | --- | --- |
| Overworld, different players | The same playable character can be used by different players simultaneously. This includes Traveler; do not globally reserve a character identity for the host. | [Co-Op Mode — Rules and Restrictions][coop] |
| Domain selection | The selected combined roster must have unique characters; ordinary duplicates across players are disallowed. | [Co-Op Mode — Co-Op Activities][coop] |
| Traveler in a Domain | Male Traveler and female Traveler are explicitly allowed together, despite normally being treated as the same character. This exception is not permission for arbitrary duplicate Travelers. | [Co-Op Mode — Co-Op Activities][coop] |
| Same-sex Travelers with different elements in a Domain | `unknown`: the accessible description confirms the male/female exception but does not explicitly test this combination. Do not infer that changing element bypasses identity restrictions. | [Co-Op Mode][coop] |
| Duplicate within a single player's owned roster | No ordinary duplicate-character slot mechanism is established by these sources; the cross-player overworld exception should not create a duplicate in the same player's roster. | [Party][party-wiki]; [Co-Op Mode][coop] |

## Energy particles and orbs

**Do not confuse particle generation ownership, particle collection, and direct/flat Energy restoration.** Ordinary particles grant Energy on receipt, not immediately when the generating skill hits. Each receiving character's element, on/off-field state, and own Energy Recharge matter. Flat Energy effects must follow their own recipient wording; they are not automatically replicated like particles. Sources: [KQM Energy][kqm-energy], [Energy wiki][energy].

### What is established, and what is not

| Question | Finding | Evidence |
| --- | --- | --- |
| Are particles intrinsically restricted to the generating character? | No. Ordinary particle collection supplies the recipient's party, and switching before receipt can change the on-field beneficiary. This establishes that generator identity alone cannot determine the recipient. | [KQM Energy][kqm-energy]; [Energy — Particles and Orbs][energy] |
| Are particles restricted to the generator's **player**, or delivered to nearby other players? | **Exact pinned-release cross-player routing: `unknown`.** The accessible wiki documents co-op conversion by each player's controllable roster, but does not explicitly distinguish replicated local particles from a shared pickup or an Energy broadcast. **[INFERENCE]** Nearby participants benefiting from one another's particle generation is consistent with ordinary co-op battery behavior; that inference is not an exact routing contract. | [Energy — Particles and Orbs][energy]; [original energy research linked by the wiki][energy-video] was CAPTCHA-blocked |
| How much does an eligible player's inactive character receive? | In a controllable roster of 2 characters, the inactive character receives 80% of its own hypothetical on-field gain, not 80% of the other character's final Energy gain. | [Energy][energy]; [original community research reproduced by Gamersky][energy-research]; [KQM Energy — Elemental vs Clear][kqm-energy] |
| Which characters count for the off-field conversion factor? | Characters that the individual player can control, not the combined session roster. This covers each player with a 2-character allocation; a player allocated only 1 character has no allocated inactive character. | [Energy][energy]; [Co-Op Mode][coop] |
| Exact co-op collection/credit radius (m), vertical bounds, loading-zone behavior | `unknown`. Do not assume session-wide Energy while participants explore separately. | No accessible original co-op measurement found. |
| Does every player need to collect a separate visible item? Can another player's collection consume yours? | `unknown`. Physical item duplication and Energy eligibility are separate questions. | No accessible original co-op measurement found. |
| Are fallen characters eligible to receive Energy? Does a wipe change eligibility? | `unknown` for the co-op distribution path. | The generic [Energy][energy] description does not settle this. |
| Co-op ICD grouping for particle generation and resonance particles | `unknown` as a universal rule. Generation cooldowns remain effect-specific; no universal cross-player ICD was established. | [KQM Energy][kqm-energy] describes effect-specific generation; not a universal co-op grouping. |

The conversion table below is an **eligible-recipient conversion table**, not proof of who receives a given player's particle. Values are Energy units per received item **before multiplying by that recipient's Energy Recharge**. "Same/different" compares the item element with the recipient's element. Sources: [Energy wiki][energy], [KQM Energy][kqm-energy].

| Recipient state | Matching particle (Energy/item) | Different-element particle (Energy/item) | Clear particle (Energy/item) | Matching orb (Energy/item) | Different-element orb (Energy/item) | Clear orb (Energy/item) | Source |
| --- | --- | --- | --- | --- | --- | --- | --- |
| On-field | 3 | 1 | 2 | 9 | 3 | 6 | [Energy][energy]; [KQM][kqm-energy] |
| Inactive, own controllable roster has 2 characters | 2.4 | 0.8 | 1.6 | 7.2 | 2.4 | 4.8 | [Energy][energy]; [original research reproduction][energy-research] |

Do not import the full-solo-roster inactive factor from [party.md](party.md) into every co-op player's Energy calculation. The combined roster used by resonance is not the per-player roster used by the inactive Energy factor. Sources: [Energy][energy], [Co-Op Mode][coop].

## Elemental resonance across players

- Resonance uses the **combined allocated party**, including characters belonging to other players, rather than requiring each player to meet the elemental requirement independently. The [Co-Op Mode wiki][coop] explicitly gives a cross-player Pyro example in which both players receive Fervent Flames. The normal complete roster has **4 characters**; unused characters in each player's account are not counted. Sources: [Co-Op Mode][coop], [Team Bonuses][resonance], [party.md](party.md).
- **[INFERENCE]** For the split allocations, off-field but allocated characters should remain part of that combined elemental tally. This follows from the allocated-party definition; a direct pinned-release test of the off-field co-op case was not found. Sources: [Co-Op Mode][coop], [Team Bonuses][resonance].
- Ordinary resonance magnitudes, requirements, and reaction trigger lists are in [party.md](party.md); do not create a different co-op percentage table.
- Resonance range between separated players, different map layers, and players inside versus outside a Domain is `unknown`. The [Team Bonuses wiki][resonance] itself explicitly requests verification of these co-op cases. A combined composition is not evidence for unlimited-distance buff delivery.

## Enemy HP and ATK scaling

Use multipliers relative to the **same enemy at the same level and encounter settings in solo**, not relative to a different World Level. The currently published co-op table gives the following normal scaling. It distinguishes world-session participant count from Domain participant count. Source: [Co-Op Mode — Co-Op Scaling][scaling].

| Players counted (players) | Enemy base HP multiplier (× solo) | Current published overworld base ATK multiplier (× solo) | Evidence |
| --- | --- | --- | --- |
| 1 | 1.00 | 1.00 | [Current wiki table][scaling] |
| 2 | 1.50 | 1.00 | [Current wiki table][scaling] |
| 3 | 2.00 | 1.00 | [Current wiki table][scaling] |
| 4 | 2.50 | 1.00 | [Current wiki table][scaling] |

- Overworld stats are described as depending on the number of players **in the session**, even if participants are in a Domain. Domain stats depend only on the players **in that Domain**. Do not substitute the number of nearby attackers or the number of characters for participant count. Source: [Co-Op Scaling][scaling].
- The wiki says the overworld ATK increase was removed in **Luna I**, but describes this as "seemingly" removed and marks the section incomplete. Its linked [later community observation][new-scaling-test] was not readable beyond a Reddit access challenge. The current table is the available target candidate, **not newly verified pinned-release data**. Source: [Co-Op Scaling][scaling].
- **Domain ATK multipliers after that change: `unknown`.** The removal statement specifically says overworld; do not silently extend the current overworld ATK column to Domains. Source: [Co-Op Scaling][scaling].
- Enemy shields, poise, elemental gauges, resistance, DEF, drops, and special encounter transformations do not gain an invented multiplier from this table. A universal co-op multiplier for each of those is `unknown`. The wiki says enemy variations can change with player count without specifying a universal variation-selection algorithm. Source: [Co-Op Mode][coop].
- Whether joining/leaving updates already spawned enemies immediately, preserves absolute HP or HP percentage, changes max HP only on respawn, or resets combat is `unknown`. The published player-count table does not answer those transition questions.
- Damage from Ley Line Disorders and Auras is described as not scaling with player count. This is not a statement that every enemy attack deals constant damage. Source: [Co-Op Scaling][scaling].

### Historical values — do not mix into the current table

The [older community scaling analysis][old-scaling-test], reproduced by the [wiki][scaling], gave these ATK values before the reported overworld change. The HP multipliers agree with the current table; the ATK difference is version-dependent, not a choice to average competing numbers.

| Players (players) | Historical base ATK multiplier (× solo) | Source |
| --- | --- | --- |
| 1 | 1.00 | [Original analysis][old-scaling-test]; [wiki's historical note][scaling] |
| 2 | 1.10 | [Original analysis][old-scaling-test]; [wiki's historical note][scaling] |
| 3 | 1.25 | [Original analysis][old-scaling-test]; [wiki's historical note][scaling] |
| 4 | 1.40 | [Original analysis][old-scaling-test]; [wiki's historical note][scaling] |

## Friendly fire and reaction ownership

### Friendly fire

**Requested recreation rule: ordinary direct attack friendly fire is disabled.** This must not become "all damage causally started by another player is impossible." Accessible public examples demonstrate indirect hazards, while a direct-hit friendly-fire exclusion measurement for the pinned release was not found (`unknown` evidence status for that exact exclusion).

- Players can ignite grass that then harms other players. An [original HoYoLAB community demonstration][friendly-hazard] describes this, and the [Grass wiki][grass] separately documents burning grass damaging a guest's locally respawned grass in co-op. This is environmental damage, not permission for a normal sword/projectile hit to damage an ally.
- Bloom, Hyperbloom, and Burgeon have player-damaging cases; Burning and electrified terrain also require their own hazard handling. Sources: [KQM Transformative Reactions][transformative], [Grass][grass]. Exact cross-player application, ownership, and exceptions for every such case in the pinned release are `unknown`; no global ally-immunity rule should be inferred.
- Applying a self/field aura to a teammate is also not direct friendly fire. For example Bennett's field imbues characters in its area with Pyro. Source: [Fantastic Voyage][bennett].

### Enemy auras are shared, but damage attribution is not pooled

An aura on an enemy is not made untouchable because another player's character applied it. Applying a compatible element can trigger the normal reaction. **[INFERENCE]** Extending the ordinary enemy-aura rules across player ownership is the normal co-op behavior; an isolated pinned-release cross-player attribution test was not found. Sources for the underlying rules: [KQM Amplifying Reactions][amplifying], [KQM Transformative Reactions][transformative].

| Reaction category / case | Damage and ownership rule to preserve | Evidence / limit |
| --- | --- | --- |
| Melt / Vaporize | The reaction amplifies the **triggering attack**, using the triggering attacker's damage calculation and EM/reaction bonuses. The player who first applied the aura does not acquire that hit's damage. | [KQM Amplifying Reactions][amplifying] |
| Ordinary single-trigger transformative reaction | Damage uses the reaction-triggering character's level, EM, and relevant reaction bonuses, not the original aura applier's stats or an average party EM. | [KQM Transformative Reactions][transformative] |
| Aggravate / Spread | The additive reaction bonus is attached to the triggering attack; it is not an independent hit credited to the creator of the Quicken aura. | [KQM Transformative Reactions — Aggravate/Spread][transformative] |
| Bloom into Hyperbloom / Burgeon | Creating a Dendro Core and converting that core are distinct reaction events. Do not assume the core creator must also own the converted reaction. Exact co-op core interaction and attribution edge cases: `unknown`. | [KQM Transformative Reactions][transformative] |
| Electro-Charged and ongoing Burning | Do not reduce continuing ticks to a permanent "first aura owner" or assume the latest damaging hit always owns every tick. KQM describes Electro-Charged ownership updates and Burning ownership behavior separately. Exact concurrent cross-player update/tie ordering: `unknown`. | [KQM Transformative Reactions — Electro-Charged / Burning][transformative] |
| Newer Lunar reactions | `unknown` for cross-player contributor accounting in this research. Their presence in the target-era sources is not evidence that a universal single-trigger ownership shortcut is correct. | [Lunar-Charged wiki][lunar] |

Off-field deployables retain an originating character/player for reaction-stat calculation; they are not reassigned to the host simply because another player is on-field. **[INFERENCE]** This follows from trigger-based reaction calculations, not a separately observed host-migration rule. Kill-credit systems and loot eligibility should not be inferred from reaction damage ownership. Sources: [KQM reaction rules][transformative], [individual co-op rewards][coop].

## Buffs, healing, shielding, and own-party boundaries

**"Party" is not a universal synonym for all connected players.** Sources explicitly distinguish transferable healing/shield effects from passives that stay within an owner's party. Sources: [Co-Op Mode — Co-Op/Restricted Activities][coop], [KQM Kirara — co-op shield exception][kirara].

| Effect | Other players affected? | Source / qualification |
| --- | --- | --- |
| Thrilling Tales of Dragon Slayers | **No automatic cross-player transfer.** It grants ATK to the new character taking the field when its holder switches. **[INFERENCE]** Since a player only switches their own allocated roster, the beneficiary is their own switched-in character, not an independently active guest. With no allocated switch target, that normal trigger is unavailable. | [KQM weapon description][ttds-kqm]; [weapon gameplay notes][ttds]; [Co-Op Mode][coop]. Direct pinned-release co-op test not found. |
| Bennett's Inspiration Field | Other players in the field can receive healing and the ATK buff when its conditions permit; field-based recipient rules must be respected. Overlapping Bennett fields' ATK bonuses do **not** stack: the first-applied bonus has priority, and only its applier refreshes that instance while it exists. | [Co-Op Mode][coop]; [Fantastic Voyage gameplay notes][bennett] |
| Barbara / Jean / Diona healing | Their documented teammate-healing abilities can affect co-op partners. This does not mean all variants heal every off-field character owned by every player. | [Co-Op Mode][coop] |
| Self-healing, such as Hu Tao's Burst | Not automatically given to other players. | [Co-Op Mode][coop] |
| Ordinary personal shield | Do not share it by default. Specific co-op shield grants are required. | [Co-Op Mode][coop]; [KQM Kirara][kirara] |
| Diona and Zhongli co-op shield grants | Their constellation-specific co-op shield effects can protect teammates; this is an explicit exception, not a property of all shields. | [Co-Op Mode][coop]; [KQM Diona guide][diona] |
| Razor sprint passive, Ningguang ore spotting passive, Tartaglia normal-attack talent passive | The wiki explicitly says these do **not** apply to the other player's party. | [Co-Op Mode — Restricted Activities][coop] |
| Food buffs | Apply to the consuming player's own characters, not other players' characters. Eating is not a session-wide buff broadcast. | [Item description explicitly excluding other players][food-buff]; [Food wiki][food] |
| Enemy debuffs / resistance reduction | Shared enemy state and party buffs are different mechanisms. Exact recipient/stacking scope follows the individual effect; no universal all-player buff rule is established here. | [KQM Party Mechanics evidence][party-evidence] |

Thrilling Tales' values are included only to make the own-party example unambiguous; character-specific talent scalings belong in their own specifications.

| Thrilling Tales refinement rank (rank) | ATK increase (%) | Buff duration (s) | Trigger cooldown (s) | Source |
| --- | --- | --- | --- | --- |
| 1 | 24 | 10 | 20 | [KQM weapon description][ttds-kqm]; [weapon wiki][ttds] |
| 2 | 30 | 10 | 20 | [KQM weapon description][ttds-kqm]; [weapon wiki][ttds] |
| 3 | 36 | 10 | 20 | [KQM weapon description][ttds-kqm]; [weapon wiki][ttds] |
| 4 | 42 | 10 | 20 | [KQM weapon description][ttds-kqm]; [weapon wiki][ttds] |
| 5 | 48 | 10 | 20 | [KQM weapon description][ttds-kqm]; [weapon wiki][ttds] |

## Statue recovery, character death, and local wipes

### Statue of The Seven

| Rule | Behavior / value | Source |
| --- | --- | --- |
| Co-op healing | Standing near a Statue can heal if Auto-Recover is enabled. Official support says co-op healing is disabled **if Auto-Recover is not enabled**, rather than unconditionally disabled in co-op. | [Official healing support][statue-support]; [Statue — Co-Op Mode][statue] |
| Restorative Power ownership | The host world's HP pool is shared by players in that world. A guest using it is not spending an independent full copy of the host reserve. | [Statue — Co-Op Mode][statue] |
| Guest's own unlock requirement | A player cannot heal from that specific Statue unless it is unlocked in their own world. | [Statue — Co-Op Mode][statue] |
| Nearby revival | Auto-Recover revives fallen party characters with 35% of maximum HP without consuming Restorative Power; subsequent healing consumes the pool. Co-op-specific revival range and exact eligible roster outside allocated characters: `unknown`. | [Statue — Auto-Recover][statue] |
| Healing delay | Staying near the base for 2 s is the wiki's documented ordinary healing condition. Exact co-op tick timing/range in the pinned release: `unknown`. | [Statue — Auto-Recover][statue] |
| Setting the threshold / manual interaction | **Source conflict:** the Co-Op Mode page lists Statue offerings/settings as host-only and permits Traveler resonance, whereas the Statue page says Statues cannot be interacted with in co-op. Official support establishes Auto-Recover healing, but does not resolve this entire interaction menu conflict. | [Co-Op Mode — Restricted Activities][coop]; [Statue — Co-Op Mode][statue]; [official support][statue-support] |
| Healing characters outside the allocated roster | `unknown` in co-op. The Statue page's ordinary inactive-character section is itself marked for verification; do not implement account-wide background recovery from this as established co-op behavior. | [Statue — Inactive Party Members][statue] |

No target-release global reserve maximum is supplied: the Statue page's published cap calculation is labeled for an earlier version and also includes Statues of the New Moon. Importing it as a pinned-release count would be unsupported. Source: [Statue's Blessing][statue].

### Fallen character versus player wipe

| State | Result | Evidence / reliability |
| --- | --- | --- |
| One allocated character falls while its owner still has another living allocated character | Ordinary replacement/switching remains within that owner's roster, not another player's character. | [Fallen Character][fallen], [party.md](party.md), [Co-Op Mode][coop]. Exact forced-replacement timing/order remains `unknown`. |
| An owner's entire allocated roster falls in an ongoing co-op Domain | Other living players can continue; the fallen player can spectate or leave the Domain. A local death is not an automatic whole-session failure. | [BWIKI Multiplayer — Domains][bwiki-coop] |
| Surviving teammates complete that Domain | Fallen players who stayed are revived in the Domain and can claim the Petrified Tree reward. Death does not by itself forfeit that reward. Exact returned HP: `unknown`. | [BWIKI Multiplayer — Domains][bwiki-coop] |
| Camera while spectating in a Domain | A fallen player can rotate the camera. This was added in Version 1.6; older complaints about a fixed death camera are historical. | [Co-Op Mode change history][coop-history] |
| Everyone in the challenge falls | Ordinary Domain failure applies. The exact co-op failure/retry transition, timer, and whether all players must confirm: `unknown`. | [Fallen Character — Game Over][fallen]; [BWIKI Multiplayer][bwiki-coop] |
| Entire local roster falls in the overworld | The generic game-over rule offers Revive and respawns the party at the nearest Teleport Waypoint with 35% HP. **[INFERENCE]** Applying this as a per-player co-op wipe does not wipe other players. A specific pinned-release overworld co-op respawn test, dormant-slot effects, and waypoint selection tie-breaker were not found. | [Fallen Character — Game Over][fallen]; co-op per-owner allocation in [Co-Op Mode][coop] |
| Recovery food | Recovery/revival foods act on the user's own characters. Exact ability to use revival food on an inactive allocated character during a co-op Domain, and restrictions after the last deployed character dies: `unknown`. Do not replace the documented Domain spectate/exit state with automatic food revival. | [Food][food]; [Error — revival item restrictions][errors]; [BWIKI Multiplayer][bwiki-coop] |
| Teammate revival talents/constellations | Effect-specific. An own-party revival is not automatically a revive for other players; the exact co-op recipient conditions of every revival talent are outside this system research and `unknown` here. | [Fallen Character][fallen] |

## Loot, chests, camps, and persistence ownership

| Object / activity | Host | Guests | Reward / persistence semantics | Source |
| --- | --- | --- | --- | --- |
| Overworld chest, including a chest unlocked by clearing a camp | Can open and collect | Cannot open | Defeating the guarding enemies together does not turn the chest into personal guest rewards. The chest belongs to the host world. | [Co-Op Mode — Restricted Activities][coop] |
| Common/elite enemy drops | Can collect | Can collect | Documented as collectible by all players, unlike host-only chest contents. Do not award only to the last hitter or chest owner. Exact eligibility radius and per-player drop-roll independence: `unknown`. | [Co-Op Mode — Collecting Resources][coop]; [BWIKI Multiplayer — Resources][bwiki-coop] |
| Boss, Ley Line Blossom, or Domain reward | Claims with own Resin if desired | Claims with own Resin if desired | Each participant chooses whether to spend their own Resin and receives individual rewards. Another player's claim does not spend yours. | [Co-Op Mode — Co-Op Activities][coop] |
| Boss reappearance after reward claims | Shared encounter reset | Shares same encounter | Official support says the boss reappears only after every player claims the Ley Line Blossom reward and the blossom disappears. Exact alternative reset behavior if somebody declines, leaves, or disconnects: `unknown`. | [Official boss respawn support][boss-support] |
| Ordinary ore | Can collect | Can collect when present | Collectible by all; another player's pickup does not make it universally exclusive. Cor Lapis is an explicit exception in the wiki's general ore statement. | [Co-Op Mode][coop]; [BWIKI Multiplayer][bwiki-coop] |
| Plants / time-gated gatherables | Can collect | Can collect | An open-world plant can be collected only once by a single person in the session. Guests can consume the host's available resources; asking permission is etiquette, not an enforced authorization gate. | [Co-Op Mode][coop] |
| Wood | Receives wood for own hits | Receives wood for own hits | The same tree can be used by different players, but wood only drops for the player hitting it. | [Co-Op Mode][coop] |
| Investigation points, Oculi, loose Sigils | Can interact/collect | Cannot interact/collect | Host-only progression objects; do not replicate their progression into guest saves. | [Co-Op Mode — Restricted Activities][coop] |
| Host-world exploration progress | Advances in host world | Can assist permitted activities | **[INFERENCE]** Clearing a guest-assisted camp or unlocking a host chest is not automatic clearing/opening in the guest's separate world. | [Co-Op Mode][coop]; [original guide's host-resource explanation][old-guide] |

The [Chest wiki][chests] additionally distinguishes camp chests locked by **respawning enemies** from chests unlocked by **one-time enemies or other mechanisms**: an unopened chest of the former kind can relock when its guards respawn, while the latter stays unlocked. Do not conflate "camp completed this visit," "chest unlocked," and "chest permanently opened." No camp/chest respawn timer is invented here.

## Host world versus guest permissions

| Operation | Rule | Source / conflict |
| --- | --- | --- |
| Joining policy | Host can reject requests, permit direct joins, or require approval. | [Co-Op Mode — Menu][coop]; [BWIKI Multiplayer — Permissions][bwiki-coop] |
| Admission capacity | Maximum 4 players in an ordinary world/session. | [Co-Op Mode][coop] |
| Character selection / switching | Each player changes their own permitted selection; guests do not take control of a host slot. Party Setup is restricted in combat. | [Co-Op Mode][coop]; [Error][errors] |
| Domain invitation / starting a challenge | Host initiates the co-op Domain challenge and participants ready their selected characters. The challenge's central start interaction is described as host-only. | [Original guide][old-guide]; [BWIKI Multiplayer — Domains][bwiki-coop] |
| Ley Line Outcrop initiation | Host initiates; guests can fight and independently claim eligible rewards. | [Co-Op Mode][coop] |
| Open-world mechanism Time Trial initiation | Host-only. | [Co-Op Mode][coop] |
| Elemental monuments | Wiki says host-only activation; do not generalize this into forbidding guests from all damageable puzzle objects. | [Co-Op Mode — Restricted Activities][coop] |
| Chests / Oculi / investigation / domain-door unlocking | Host-only interactions as documented above. | [Co-Op Mode][coop] |
| Changing world time | Host-only; a guest receives a time-adjustment error. | [Co-Op Mode][coop]; [Error][errors] |
| Cooking | Allowed in co-op. | [Co-Op Mode][coop] |
| Crafting / forging | Historical guide says these were unavailable; later change history explicitly adds co-op crafting/forging. Do not preserve the old blanket prohibition. Exact access to every crafting station/service in the pinned release: `unknown`. | [Original older guide][old-guide]; [Co-Op Mode change history][coop-history] |
| Most NPCs, shops, Katheryne, and some quest/cutscene interactions | Restricted even for the host while in co-op. Some World Quests can be assisted; no claim that all quests can or cannot be progressed. | [Co-Op Mode][coop]; [official co-op access support][coop-support] |
| Statue interaction / Traveler resonance | Conflicting descriptions; use the Statue section's conflict, not an unqualified permission boolean. | [Co-Op Mode][coop]; [Statue][statue] |
| Menu pause | The game is not paused by menus in co-op, even if no guest remains but the host is still in co-op mode. | [Co-Op Mode — Restricted Activities][coop] |
| Reputation bounty rewards | Guests may help, but only the host gains the relevant host-world reputation reward. | [Co-Op Mode][coop] |

Access is normally restricted by host versus guest World Level, with a published **World Level 8/9 mutual-entry exception introduced in Version 5.3**. This is another changed rule: an older unconditional "never join a higher World Level" description is incomplete. Adventure Rank **16** and the specified introductory Archon Quest are the documented normal unlock requirements. Sources: [Co-Op Mode][coop], [Co-Op change history][coop-history], [BWIKI Multiplayer][bwiki-coop]. These are reference-game admission rules, not an instruction to add account progression outside the project's chosen slice.

## Joining and leaving during combat

| Situation | Established behavior / unresolved detail | Source |
| --- | --- | --- |
| Joining an overworld host who is fighting ordinary enemies | `unknown` whether ordinary combat alone blocks admission in the pinned release. Neither the current rules summary nor official quest-block troubleshooting supplies an exact combat-state admission rule. Do not equate inability to enter Party Setup with inability to join. | [Co-Op Mode][coop]; [official support][coop-support]; [Error][errors] |
| Host in an ongoing Domain / Spiral Abyss challenge | BWIKI lists this as an inability-to-enter-world condition; domain matchmaking/entry readiness happens before the challenge. This is not evidence for mid-fight injection into an existing instance. | [BWIKI Multiplayer — Cannot Join][bwiki-coop]; [original Domain guide][old-guide] |
| Quest/activity with co-op blocked | Cannot enter co-op until the relevant blocking stage is completed; the game exposes the blocking quest. | [Official co-op access support][coop-support] |
| Fallen guest leaving an ongoing Domain | Explicitly allowed as an alternative to spectating. Other players can continue. | [BWIKI Multiplayer — Domains][bwiki-coop] |
| Living guest returning to own world mid-overworld-combat | `unknown`: exact permitted state and delay were not established. Ordinary ability to leave co-op is not a timing measurement. | [Co-Op Mode][coop] |
| Host kicking a participant or closing co-op mid-combat | `unknown`: permissions and timing during an active challenge were not established. | No sufficiently specific accessible evidence found. |
| Allocated slot restored after a guest leaves | Version 2.3 added filling the vacant slot with the corresponding character from the host's original pre-co-op composition. | [Co-Op Mode change history][coop-history] |
| Transition among split rosters on joins/leaves | The steady-state allocations are established above. Which existing character is removed, who gains a slot during every transition, HP/Energy preservation, cooldown/animation timing, and what happens if a removed character owns a deployable: `unknown`. | [Co-Op Mode][coop]; [change history][coop-history] gives only the specific host-slot restoration rule. |
| Disconnect/reconnect during battle | `unknown`: grace period, dormant avatar, host migration, enemy rescaling timing, and re-entry location are not supplied by these sources. | No sufficiently specific accessible evidence found. |

## Conflicts, unknowns, and least reliable items

- **Highest-risk missing detail: Energy routing between players.** Recipient conversion is well documented, but the exact cross-player particle-delivery mechanism, radius, duplicate pickup handling, and fallen-recipient eligibility are `unknown`. Do not turn the nearby-battery inference into a measured global Energy broadcast.
- **Enemy ATK version boundary:** the current wiki's overworld multiplier is the available target candidate; its claimed removal boundary is hedged, its scaling section incomplete, and the linked newer experiment inaccessible. Domain ATK after that change is `unknown`. The historical ATK table must not leak into current overworld values.
- **Statue menu conflict:** the general co-op page and Statue-specific page disagree on interaction permissions; official support only resolves the conditional Auto-Recover healing question. Shared host reserve is explicitly documented; co-op revival roster/range is less reliable.
- **Wipe precision:** Domain spectating, continuation, and successful-completion revival are directly documented by BWIKI. The overworld per-player application of the generic waypoint/HP rule is an inference, and return HP after a Domain completion is `unknown`.
- **Buff scope:** Thrilling Tales' own-player restriction is a consequence of its switch trigger and allocation rules, not a found pinned-release co-op test. Do not generalize it to field buffs; Bennett's cross-player non-stacking notes are more explicit.
- **Friendly fire:** direct attack friendly fire is the requested recreation rule; indirect player-started environmental hazards are documented. An isolated source measurement proving every direct attack exclusion in the pinned release was not found.
- **Mid-combat membership:** ordinary overworld join/leave gates, kicking, disconnects, slot transition timing, deployable persistence, and live enemy HP rescaling remain `unknown`.
- **Reaction edge cases:** ordinary trigger-based stat ownership is sourced. Concurrent co-op aura ordering, core attribution edge cases, newer Lunar contributor accounting, and host migration of effects remain `unknown`.
- **Cross-map resonance:** explicitly unresolved in the source wiki; no arbitrary radius or unlimited-range guarantee is provided.

No game measurements, tests, builds, or linters were run. This file is the complete research deliverable; the unknowns above are evidence gaps, not invented defaults.

## Sources

[coop]: https://genshin-impact.fandom.com/wiki/Co-Op_Mode
[scaling]: https://genshin-impact.fandom.com/wiki/Co-Op_Mode#Co-Op_Scaling
[coop-history]: https://genshin-impact.fandom.com/wiki/Co-Op_Mode/Change_History
[party-wiki]: https://genshin-impact.fandom.com/wiki/Party
[energy]: https://genshin-impact.fandom.com/wiki/Energy#Particles_and_Orbs
[kqm-energy]: https://library.keqingmains.com/combat-mechanics/energy
[energy-video]: https://www.bilibili.com/video/BV1my4y1r7VP
[energy-research]: https://www.gamersky.com/handbook/202106/1394486.shtml
[resonance]: https://genshin-impact.fandom.com/wiki/Team_Bonus#Elemental_Resonance
[old-scaling-test]: https://www.reddit.com/r/Genshin_Impact/comments/kmckgl/fyi_the_damage_and_hp_of_mobs_scales_in_coop_as/
[new-scaling-test]: https://www.reddit.com/r/Genshin_Impact/comments/1rcdus6/they_took_away_overworld_enemies_coop_atk/
[amplifying]: https://library.keqingmains.com/combat-mechanics/elemental-effects/amplifying-reactions
[transformative]: https://library.keqingmains.com/combat-mechanics/elemental-effects/transformative-reactions
[lunar]: https://genshin-impact.fandom.com/wiki/Lunar-Charged
[grass]: https://genshin-impact.fandom.com/wiki/Grass#Gameplay_Notes
[friendly-hazard]: https://www.hoyolab.com/article/389403
[ttds-kqm]: https://library.keqingmains.com/equipment/weapons/catalysts#thrilling-tales-of-dragon-slayers
[ttds]: https://genshin-impact.fandom.com/wiki/Thrilling_Tales_of_Dragon_Slayers
[bennett]: https://genshin-impact.fandom.com/wiki/Fantastic_Voyage#Gameplay_Notes
[kirara]: https://keqingmains.com/kirara/#Constellations
[diona]: https://keqingmains.com/q/diona-quickguide/
[party-evidence]: https://library.keqingmains.com/evidence/combat-mechanics/party-mechanics
[food]: https://genshin-impact.fandom.com/wiki/Food
[food-buff]: https://genshin-impact.fandom.com/wiki/Masala_Cheese_Balls
[statue]: https://genshin-impact.fandom.com/wiki/Statue_of_The_Seven#Statue's_Blessing
[statue-support]: https://support.hoyoverse.com/hc/en-us/articles/50333967158041-Why-can-t-I-heal-at-the-Statues-of-The-Seven
[fallen]: https://genshin-impact.fandom.com/wiki/Fallen_Character#Game_Over_Screen
[bwiki-coop]: https://wiki.biligame.com/ys/%E5%A4%9A%E4%BA%BA%E6%B8%B8%E6%88%8F
[chests]: https://genshin-impact.fandom.com/wiki/Chest#Additional_Information
[boss-support]: https://support.hoyoverse.com/hc/en-us/articles/52089419677849-Why-don-t-bosses-respawn-immediately-in-Co-Op-mode
[coop-support]: https://support.hoyoverse.com/hc/en-us/articles/50333905596953-Unable-to-Access-Co-op-Mode
[errors]: https://genshin-impact.fandom.com/wiki/Error#Overlay_Message_Errors
[old-guide]: https://www.hoyolab.com/article/321151
