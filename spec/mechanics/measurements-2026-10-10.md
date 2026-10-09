# Real-game measurement attempt — 2026-10-10

## Outcome and safety stop

**Blocked at the first in-game login attempt by a slider CAPTCHA. No overworld measurement was obtained.** The visible challenge said **“Slide to complete the puzzle.”** The assignment explicitly required stopping and closing the game on any CAPTCHA; the challenge was not solved, and no second login attempt was made.

This is an attempt/evidence record, **not a completed measurement dataset**. Adventure Rank, World Level, the deployed party, camp rosters/levels, traversal speeds/costs, and fall thresholds remain unmeasured. Startup success and a version label are not evidence for those mechanics. No match/mismatch conclusion or numerical correction is justified by this attempt.

The exact missing prerequisite is access to an authenticated game session without this challenge. Continuing past the challenge would conflict with the account-safety instructions; this record does not authorize bypassing it or another agent login retry.

## Method and observed setup

- Read `AGENTS.md`, the durable brief, `todo.md`, the traversal/camp adaptations in `spec/fidelity.md`, `spec/mechanics/{traversal,stamina,hilichurls,world-objects,datamine}.md`, `rules/Traversal.java`, and `docs/map-landmarks.md` before attempting gameplay.
- Used the supplied An Anime Game Launcher **3.19.8**, pointing at the existing **`/srv/share/games/Genshin Impact`** installation. No game-folder move, deletion, replacement, or extraction was performed.
- Launcher/game processes ran inside `systemd-run --user --scope -p MemoryMax=12G -p CPUQuota=300%`. Immediately before the game launch, `pgrep -a java` returned no Java processes, satisfying the supplied SoundRecording resource gate.
- Initially tried the specifically authorized KDE/Xwayland desktop. Input proved unreliable, so setup and the final game attempt used a private **Xvfb `:112`, 1280×720** X11 display. This was not an overworld measurement and did not require changing the game's graphics settings.
- The launcher installed its selected **Wine DWProton WOW64 11.0-11**, Wine prefix dependency, and **DXVK 3.1.1** through its own first-run workflow. The first-run completion screen reported that the basic components had been downloaded.
- The telemetry-hosts action could not update `/etc/hosts` (`Failed to update /etc/hosts file`). As expressly permitted in the assignment, the launcher's telemetry check was temporarily disabled to acknowledge that exception. It was restored to its original enabled state after shutdown. No host-level telemetry configuration was installed.
- The existing `config.ini` reported **`game_version=7.1.0`**. The launcher's General screen also displayed **7.1.0**, and the real game's blank login screen displayed **`OSRELWin7.1.0_R48379043_S48511369_D48533839`**. These observations establish the attempted client version, not successful account access.
- The game's pre-login renderer log identified **NVIDIA GeForce GTX 1060 / Direct3D 11.0, feature level 11.1**. The game reached a rendered login screen on the private display; a rendering failure was not the terminal blocker.
- Login credentials were read from the supplied mode-600 file and typed only into the game's fields using input passed through standard input, not command-line arguments. No website login was performed. No screenshot or recording of filled credential fields was taken.
- After submission, the only response capture was a **480×96 credential-free title-band crop**, beginning at `(400,176)` on the 1280×720 game screen. It excluded the known username/password field areas and showed the CAPTCHA instruction.
- Sent **Alt+F4** to quit the game, then closed the launcher's own title-bar control. The launcher returned to its **Launch** state before closure; its supervising process exited with **code 0**. The private Xvfb session ended with it. The task-created desktop input portal session was already absent when its closure was requested.

No gameplay, teleport, combat, party edit, quest progression, purchase, wish, currency/resin spending, inventory change, or character death occurred during this attempt. Credentials were not printed or included in this report.

## Raw observations and private evidence

All screenshot paths below are in the git-ignored `run/genshin-measure/` directory. They are private evidence, not shipped game assets.

| Observation | Raw reading | Evidence | Uncertainty / limitation |
| --- | --- | --- | --- |
| Installed client version | `game_version=7.1.0` | Existing `/srv/share/games/Genshin Impact/config.ini`; `run/genshin-measure/private-preferences-security.png` displays launcher Game version **7.1.0** | Direct file/UI reading; does not establish logged-in server access. |
| Real client version at login | `OSRELWin7.1.0_R48379043_S48511369_D48533839` | `run/genshin-measure/game-login-before-entry.png` | Direct visible label; this screenshot was taken while both credential fields were blank. |
| Launcher component selection | Wine DWProton **11.0-11**; DXVK **3.1.1** | `run/genshin-measure/private-components-ready.png` | Direct selected labels, not a compatibility guarantee. |
| First-run setup outcome | “Everything's done!” / “All the basic components were downloaded.” | `run/genshin-measure/private-dependencies-result.png` | Launcher-reported setup completion. |
| First manual login response | “Slide to complete the puzzle” | `run/genshin-measure/login-response-title-band.png` | Direct CAPTCHA observation; does not establish whether the supplied credentials were accepted. |
| Post-stop launcher state | **Launch** button visible after the game was closed | `run/genshin-measure/launcher-after-captcha-stop.png` | Shows returned launcher state. Subsequent supervising-process output reported launcher exit code 0. |

**No traversal recording was captured.** FFmpeg with X11 capture support was made available for the intended recording workflow, but no frame counts, timed movement intervals, stamina-wheel readings, distance calibration, or HP-loss readings were obtained. None are reconstructed from launcher screenshots.

## Requested measurements, repository comparison, and uncertainty

Repository values below are existing implementation/reference inputs, **not new game observations**. Block-space adaptations are not silently relabelled as independently calibrated Genshin metres. `Not evaluated` means there is insufficient real-game evidence to call either match or mismatch.

| Requested measurement | Measured value / raw readings | Method reached and uncertainty | Existing repository value / comparison |
| --- | --- | --- | --- |
| Adventure Rank and actual World Level | **Not measured** | Paimon menu/Adventurer Handbook unavailable before authenticated overworld entry. No uncertainty interval can be estimated. | No account-context conclusion. |
| Current team's characters, character levels, and equipped weapons/levels | **Not measured** | No character or team screen reached. No team edits were made. | The repository's Lv20 starter loadout must not be treated as this account's loadout; **not evaluated**. |
| Starfell Lake / Starfell Valley camp: every hilichurl/slime type and `Lv. N` | **Not measured** | No waypoint teleport or camp approach occurred; no enemy screenshots exist. | Existing Lv8 adaptation / selected overlay roster **not evaluated**. |
| Windrise camp: every hilichurl/slime type and `Lv. N` | **Not measured** | No waypoint teleport or camp approach occurred; no enemy screenshots exist. | Existing Lv8 / three adapted basics **not evaluated**. |
| Mondstadt gate/bridge approach: every hilichurl/slime type and `Lv. N` | **Not measured** | No waypoint teleport or camp approach occurred; no enemy screenshots exist. | No independently sourced gate camp in the current overlay; **not evaluated**. |
| Continuous climb speed and stamina used | **Not measured** | No calibrated vertical wall, character-height image, frame interval, or wheel fraction obtained. | `ADAPTED_CLIMB_BLOCKS_PER_TICK=.08` → **1.6 blocks/s** at 20 TPS; moving drain **5.36 stamina/s**. These remain adaptations; **not evaluated**. |
| Climb-jump distance and stamina cost | **Not measured** | No recorded jump or before/after wheel reading obtained. | Speed **.24 blocks/tick**, duration **18 reference frames = .3 s**, nominal velocity contribution **1.44 blocks**; selected cost **25 stamina**. The nominal contribution is not an observed collision-resolved displacement; **not evaluated**. |
| Glide horizontal distance/speed, height loss/descent speed, and stamina drain | **Not measured** | No known-height launch, flight recording, horizontal ruler, or wheel interval obtained. | Forward **5 blocks/s**, descent **2.351 blocks/s**, drain **3 stamina/s** before ungranted utility passives; **not evaluated**. The historical descent estimate remains assumption-calibrated, not newly pin-verified. |
| Held sprint speed and stamina drain | **Not measured** | No measured route, frame interval, dash/held-sprint boundary, or modifier inventory obtained. | Held sprint uses vanilla-speed sprint; selected drain **18 stamina/s**, with separate **18-stamina dash/start**. No calibrated real-game sprint speed is established here; **not evaluated**. |
| Fall damage versus height, first-damage threshold, and lethal threshold | **Not measured** | No ledge series, height reference, maximum HP, before/after HP, or impact-horizontal-motion readings obtained. No fall was attempted and no character died. | Adapted zero-horizontal-motion curve is **0% at/below 5 blocks**, **50% at 15 blocks**, **100% at/above 25 blocks**, with the separate horizontal-equivalent-height term. This is not a real-game measurement; **not evaluated**. |

### World Level handling

**No WL0 conversion is performed or authorized by this record.** The main agent explicitly clarified that observations must be tied only to the account's actual World Level. As already documented in [datamine.md — Camp spawns and world level](datamine.md#camp-spawns-and-world-level), `WorldLevelExcelConfigData.monsterLevel` is **not a verified per-camp offset**, and the inspected export has **no WL0 row**. Even a future nonzero-WL camp observation would not, by itself, justify subtracting that table's values to infer WL0 spawns. This attempt obtained neither the account's World Level nor any enemy level.

## Derived values and repository changes

- **Derived game-mechanics values: none.** The unit conversions in the comparison table describe the already-existing repository constants only.
- **Repository numeric values that should change on this evidence: none.** In particular, this attempt does not support changing camp Lv8, climb drain 5.36/s, climb/jump speeds or distance, glide speeds/drain, sprint costs, the provisional block-to-metre convention, or safe/lethal fall thresholds.
- **Existing uncertainties remain unchanged:** actual low-world-level camp rosters/levels; absolute distance calibration; model/party-dependent traversal and stamina modifiers; continuous climbing drain; climb-jump displacement/billing; glide velocity; sprint velocity/billing; and the ordinary playable-character fall curve/thresholds.
- Only this requested measurement-attempt document was added to the repository. No implementation code, tests, existing mechanics specifications, fidelity entries, or map bindings were edited. No git commands, builds, tests, linters, or formatters were run.

The requested end-to-end measurement acceptance criteria were **not satisfied**, because the mandatory CAPTCHA stop prevented all account and overworld observations. Retain the current adaptation labels; do not promote them to verified 7.1 values on the strength of this startup attempt.
