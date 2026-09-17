# Genshin-in-Minecraft: DeepSeek-only implementation and verification brief

Prepared: 14 September 2026.

This is a project specification, not a claim that the mod, test tools, local environment, or model routes have already been built, inspected, or verified. Instructions below are engineering policies. Public starting references are separated from owner-provided facts and decisions the implementing agent must verify locally.

**Exclusive model policy: DeepSeek V4.1 Flash at max reasoning effort for every AI role and every project-controlled model call. No other models and no lower-effort routes.**

## 1. Objective and scope

Create an original Minecraft Java client/server mod implementation that recreates Genshin Impact's observable gameplay as faithfully as practical on top of the owner's purchased WanggMC / wangg_mc Blocky Teyvat map. This is not merely a decorative map, a collection of themed items, or a vanilla combat reskin.

The long-term target is a playable, coherent recreation: traversal, camera and controls, character parties, combat and reactions, enemies, exploration, quests, progression, equipment, rewards, menus, persistence, and cooperative play. Preserve that full ambition in the backlog. Deliver it through independently verified slices rather than a single attempt to implement every character and region.

Prioritize, in order: protecting the owner's files; correct and evidenced behavior; an end-to-end playable experience; repeatable validation; maintainable implementation; content coverage; visual polish. Visual readability and usable controls belong to playability, not optional final polish.

Success is measured by reproducible behavior and player-visible results, not number of files, lines of code, features named in documentation, or an agent's assertion that the game works.

The owner also wants to evaluate what DeepSeek V4.1 Flash at max can accomplish across the whole development lifecycle with minimal human intervention. Preserve that experimental constraint when progress is difficult. Do not rescue the project with a different model or conceal a need for assistance. Evaluate the model together with the recorded OMP configuration and tools; do not present the result as a model-only benchmark or let evaluation paperwork displace implementation.

### Facts supplied by the owner

- The owner purchased the map identified by `https://ko-fi.com/s/5609329f72`.
- The owner has permission to publish social-media videos using the map with attribution to its creator.
- The requested runtime is Minecraft, with both client-side and server-side mod functionality.
- The sole authorized AI model is DeepSeek V4.1 Flash at max reasoning effort in OMP. This applies to the lead, all workers, planning, research, coding, review, visual interpretation, playtesting, summaries, and other model-backed project operations.
- The owner wants to see what this model can achieve without assistance from another model.
- The owner wants minimal routine manual intervention.

Do not reinterpret video permission as established permission to redistribute the world, bundle its assets, offer a public server, or use third-party Genshin assets. Treat those as separate release questions. This brief is not a legal clearance opinion.

### Public starting information, to check against actual inputs

Prior project notes report that the map's shop listing distinguishes Blocky Teyvat 5.1.0 on Minecraft 1.21.10, including Voxy support and Dornman Port, from 5.0.0 on Minecraft 1.20.1. It lists Mondstadt, Liyue, Inazuma, Sumeru, Fontaine, and Natlan and describes bundled Fabric modpacks/shaders. Treat these as previously recorded seller statements, not a fresh verification of the listing or the owner's downloaded package. Check the package itself and consult the listing as needed. [S1]

The project must inspect the purchased Java package before selecting Minecraft, Java, Fabric, rendering dependencies, or a world migration strategy.

## 2. Authorized defaults and decision policy

The exclusive model policy is an owner requirement, not a default that the agent may override. An incompatibility is a blocker to repair or report, never permission to substitute a model or reduce reasoning effort.

Use these technical defaults unless the owner provides different instructions or inspection shows an incompatibility:

- Minecraft Java Edition, Fabric, Java, a pinned Gradle wrapper, and a reproducible development environment. Prefer a project-local Nix development shell on NixOS; do not change the system configuration.
- Use the supplied map's compatible version, not automatically the newest Minecraft release. Choose one version initially. Do not undertake Bedrock or multi-loader support.
- Support an integrated server and a dedicated server. Start with a private local test server. Plan for a small cooperative group, initially up to four clients; do not turn the project into an MMO.
- Prefer faithful source-game party/co-op rules once researched. Any differing allocation of character slots, quest ownership, loot, or world progression is an explicit adaptation, not implicit parity.
- Freeze one publicly documented Genshin release as the reference baseline. Verify and record its version/date; do not chase a moving release or mix mechanics from different versions unknowingly. If current evidence is incomplete, mark the gaps rather than substituting an undocumented older baseline.
- Begin in a compact, verified playable part of Mondstadt. If absent or unusable, choose another supplied, suitable area and record the reason.
- Use original or demonstrably permitted visual/audio assets. Labeled placeholders may unblock mechanics, but do not count them as finished content.
- No real-money transactions, account integration, public hosting, or automatic publication. Character acquisition can be modeled later as local gameplay without a payment service.

Make reversible engineering choices autonomously and record their rationale. Ask only when an unavailable input, authority boundary, or genuinely consequential product decision prevents progress. Batch non-blocking questions; keep working on independent tasks.

Never ask the owner to do work the agent can perform locally: dependency inspection, log reading, coordinate extraction, reproducible bug isolation, version checks, or test execution.

## 3. Initial discovery and protection

### Workspace and machine

Inspect the existing repository, instructions, build files, available tools, Java runtimes, operating system, GPU/display access, free memory, free storage, and already-running build/game processes. Preserve unrelated work. Do not assume a clean repository.

Use provided local paths first, then a bounded search inside the project and `private_inputs/`. Do not search the entire home directory for credentials or unrelated files. If map inputs are absent, record the required path and continue with a small synthetic test world.

Create ignore rules before importing private inputs or writing generated data. Keep map archives, extracted worlds, proprietary resource packs, account information, logs containing secrets, and private recordings out of source control and public CI artifacts.

### Purchased map

Inspect archive metadata before executing any included launcher or code. Record package names, checksums, README/license instructions, `level.dat`/DataVersion, dimensions, region bounds, datapacks, resource packs, mod manifests, custom block dependencies, and spawn information. Check for scripts, command blocks, or existing gameplay systems that may interact with the mod.

Preserve a pristine, read-only source. Run only on independent writable copies or correctly isolated copy-on-write snapshots. Never hard-link mutable world files to the source. Never open the original in a newer client, downgrade it, or silently rewrite it. Perform conversions only on disposable copies with a documented reason and comparison checks.

Inventory large worlds incrementally. Do not load all chunks, hash multiple full copies concurrently, or generate a full-resolution map without checking resource requirements. Preserve machine usability.

Separate required map/rendering dependencies from optional performance and shader additions. Establish a minimal working profile and then a creator-compatible visual profile. A crash with an optional renderer is not automatically a core gameplay bug, and a pass with it removed is not a compatibility pass.

### Required discovery outputs

Write a concise environment report, exact version/dependency lock, private map manifest, initial rights/provenance notes, and a run configuration. Clearly distinguish observed facts, seller claims, assumptions, and unresolved questions.

## 4. OMP and exclusive model routing

### Non-negotiable invariant

Use **DeepSeek V4.1 Flash with explicit max reasoning effort for every project-controlled generative-model invocation**. This includes the main session and any automatically triggered model work, not merely named coding workers.

| Role or operation | Required route |
| --- | --- |
| Lead, orchestrator, planner, architect, and integrator | DeepSeek V4.1 Flash / max |
| Researcher, scout, librarian, and requirements author | DeepSeek V4.1 Flash / max |
| Implementer, task worker, designer, and content author | DeepSeek V4.1 Flash / max |
| Reviewer, security reviewer, advisor, and fresh-context diagnostician | DeepSeek V4.1 Flash / max |
| Screenshot/video-frame interpretation and other visual analysis | DeepSeek V4.1 Flash / max |
| Player, test designer, fixture author, bug triager, and model-based evaluator | DeepSeek V4.1 Flash / max |
| Commit-message generation, summaries, context compaction, and automatic helper calls | DeepSeek V4.1 Flash / max, or deterministic non-model handling |

Do not use a cheaper helper, a stronger rescue model, another DeepSeek variant, a previous release, a thinking-disabled route, or a lower effort for an easy task. Do not invoke another model through a hosted coding tool, visual service, generated research report, asset-generation tool, or external reviewer. Search and read raw sources directly rather than delegating research synthesis to another generative model.

Ordinary software is allowed: search, page retrieval, compilers, debuggers, Gradle, tests, game engines, parsers, calculators, linters, deterministic image processing, scripted input replay, and non-model simulation controllers. This is a single-model development constraint, not a requirement to spend a model call on every computation, game frame, or keystroke. Any generative component the project elects to call must satisfy the model/effort invariant.

### Inspect first; do not invent configuration

Use the installed OMP version and its actual schemas as the authority. Public agent/model-discovery and prompt-keyword documentation are starting references, not a substitute for installed-version inspection. [S2-S3]

Before substantial project work or delegation, inspect the installed CLI/help, active tools, model catalog, provider routes, credential availability without exposing secrets, agent discovery, role aliases, supported reasoning controls, model-backed tools, fallback chains, retries, prewalk/first-write handoffs, advisors, automatic compaction/summarization, commit helpers, permissions, and concurrency limits. Determine whether the current session is planning-only and whether its own route already complies.

Audit every discovered role and agent, including inherited defaults and project/global overrides. Names such as `default`, `plan`, `task`, `designer`, `slow`, `smol`, `tiny`, `vision`, `advisor`, and `commit` are examples to look for, not guaranteed schema keys. Trace alias chains and per-agent overrides through to the actual provider model and effort. Do not assume workers inherit the parent or that an effort setting propagates to utility calls.

Preserve global settings and unrelated projects. Apply only supported project/session overrides. Do not invent tool arguments, treat an alias as a callable agent, or mistake prompt text for a runtime model change. If correcting routing requires a session restart, write the exact verified launch/configuration steps and checkpoint; do not pretend the running lead changed models. Perform only the minimal setup needed to establish a compliant run before substantive work.

### Max means the provider's actual max setting

Resolve an available authorized provider/model identifier for DeepSeek V4.1 Flash using the installed catalog and current primary documentation. Do not guess an API model ID from the display name or assume a generic alias identifies the requested release. Consult DeepSeek's model release and thinking-control documentation as starting references. [S4-S5]

Set reasoning effort explicitly to **max** using the supported OMP/provider mechanism. Check the request translation: a UI label is not evidence that the adapter transmitted the provider's max setting. Detect clipping, unsupported-value substitution, thinking-disabled calls, effort ceilings, and model/effort changes after context compaction, first-write handoffs, retries, or delegation. Do not use an automatic effort keyword as proof that max was selected.

When available, inspect sanitized request/launch metadata and provider responses for the requested model ID, provider route, explicit effort, and effective settings. Never log credentials or private reasoning content. A model's self-description does not verify its identity or effort. If only a provider alias is exposed, record exactly what is observable and the remaining identity limitation; do not claim to know undisclosed backend weights or the internal reasoning actually performed.

The minimum routing gate is a documented provider identity mapping and evidence that the configured request path preserves the explicit max setting. Where request metadata is not directly exposed, use supported adapter inspection and provider documentation, recording the verification limitation. Do not require impossible hidden-backend attestation, but do not label an unrecognized alias or demonstrably clipped effort as verified.

### Fail closed on substitutions

Disable automatic model fallbacks and effort reductions for this project wherever supported. A retry may retry the same authorized model/effort route with bounded backoff; it must not route around an outage or context limit by selecting a different model or lower effort. Disable optional model-backed helpers that cannot obey the policy, or replace them with deterministic scripts and explicit compliant calls.

If the route or max setting is unavailable, first attempt a bounded, supported local configuration/adapter repair when the running agent itself complies. Otherwise checkpoint and report the exact blocker and minimum owner action. Stop affected AI work rather than silently substituting. Deterministic operations already set up may still run when safe; a noncompliant lead must not keep developing the game under the label of workaround work.

If a noncompliant call occurs, record it immediately, mark the affected run/output as outside the DeepSeek-only evaluation, and stop the offending route. Preserve unrelated files. Do not silently accept a mixed-model result or retroactively claim compliant provenance. Inventory pre-existing project work and report new contributions from the verified run separately.

### Route evidence and worker preflight

Write `docs/runtime-routing.md` with the installed OMP version, configuration source/precedence, provider/model mapping, max-effort translation, fallbacks disabled, automatic operations audited, observation limits, and verified invocation methods. Smoke-test each distinct invocation path before relying on it: parent, task worker, reviewer/advisor, utility/compaction path when enabled, and the actual image-plus-action player path once available. Re-check after configuration or provider changes.

Maintain a sanitized model-call/run ledger where supported: role, session/task identifier, requested and observed model/effort, configuration fingerprint, usage when available, and compliance status. Do not claim calls are exhaustively audited when the harness exposes only partial metadata. No calls are to be made to alternative models for comparison or rescue.

### Delegation and context discipline

Start with two delegated workers at most; increase up to four only after measuring memory, API concurrency, and integration overhead. Keep one graphical test environment, which may contain multiple clients for a single coordinated test. One integrator owns shared schemas and merges. Use isolated worktrees or non-overlapping ownership, bounded worker budgets, and no uncontrolled tree of agents.

Give each worker a compact task packet: goal, relevant requirements, allowed files, interfaces, acceptance tests, evidence sources, budget, and stop conditions. Supply additional context by retrieval rather than copying the whole project into every prompt. Every worker uses the same required model and max effort. Workers return changed paths, executed tests, evidence locations, failures, and uncertainties.

Reviewer, implementer, player, and evaluator may be different agents using the same model. Separate their contexts, duties, and privileges. Fresh contexts provide procedural separation, not statistically independent model judgment. Counter shared blind spots with external evidence, deterministic oracles, held-out cases, and mutation tests rather than consensus among identical-model agents.

## 5. Reverse-engineering method and evidence

Reverse-engineer observable behavior into a fresh implementation. Do not depend on bypassing anti-cheat/access controls, leaked code, game-account credentials, protected asset extraction, or emulating HoYoverse services. Use authorized normal-game observations, public documentation, and properly licensed materials.

Build a feature/dependency map first, then research the next playable slice in depth. Avoid an unbounded attempt to read every guide and video before implementing anything.

For each behavior record:

`requirement ID | game version | source URL/timestamp or local evidence | conditions | observed rule | confidence | conflicts/unknowns | implementation location | verification method | status`

Use official descriptions for intended behavior and controlled observations/original research for empirical details. The KQM Theorycrafting Library and its linked evidence are useful discovery sources, not infallible or necessarily current for every patch. Follow original evidence where available. [S8-S9]

Do not equate a wiki statement, video narration, animation frame, and measured gameplay event. Video evidence must identify timestamps and relevant setup: character/weapon/enemy levels, buffs, equipment, talents, frame rate when known, and other conditions affecting the result. Distinguish actual frames inspected from transcripts or summaries. Never claim to have watched inaccessible media.

Explicitly investigate mechanics that a superficial recreation would miss: damage-event ordering, elemental application and reaction rules, application cooldowns, aura persistence, energy generation, snapshot/dynamic behavior, shields, resistance/defense, stagger, targeting, animation/input windows, canceling, traversal stamina, and character-specific exceptions. This list is a research agenda, not an assertion of specific formulas.

Define independent golden cases from evidence before implementing each subsystem. Expected values must not be generated by invoking the same implementation under test. Maintain a small second-method calculation or reference trace for critical cases. Test that seeded mistakes are caught where practical.

Use statuses such as `unknown`, `specified`, `implemented`, `verified`, `adapted`, and `blocked`. Implemented is not verified. Approximate and intentionally changed behavior must stay visible in a fidelity ledger.

## 6. Architecture and simulation

Keep the design modest enough to maintain; do not build a generic engine, scripting language, or abstraction framework without a concrete need.

Prefer four logical boundaries, which need not be separate published artifacts:

1. A testable Java rules layer for stats, abilities, reaction resolution, cooldowns, progression, and save schemas.
2. Server adapters for entities, movement validation, authoritative state, rewards, interactions, and networking.
3. Client adapters for input, camera, animation, HUD, inventory/menus, effects, and prediction/interpolation where necessary.
4. Versioned content/map definitions plus development-only testing tools.

The server owns damage, elemental state, cooldowns, energy, stamina validity, inventory/rewards, progression, quest transitions, and persistence. Clients submit validatable intent, not authoritative outcomes. Keep client-only classes out of dedicated-server initialization.

Use data-driven definitions for characters, abilities, enemies, items, quests, encounter triggers, and rewards where that reduces duplication. Support explicit scripted exceptions for behaviors that do not fit a shared schema; do not force every kit into an inaccurate universal template.

Investigate Minecraft's tick scheduling against the timing precision required by the reference. Do not silently round every observed event to a coarse tick. Where needed, evaluate an ordered event timeline or bounded rules-layer substeps without making the entire Minecraft world run at a different rate. Document residual collision/input timing differences. All gameplay timers in controlled tests must follow the same simulation clock.

Use seeded randomness and stable event ordering in the rules layer. Record complete initial state, input/event order, and dependency versions for reproducible tests. Do not promise bit-identical replay of the entire Minecraft world unless demonstrated.

Separate source-game rules from deliberate Minecraft adaptations: block interactions, terrain protection, camera collision, world scale, multiplayer ownership, and encounter instancing. Avoid accidentally retaining conflicting vanilla combat, hunger, durability, or progression behavior. Measure input responsiveness and animation readability, not just numeric damage.

## 7. Map integration and content pipeline

Treat the map as world geometry/art, not as proof that gameplay placements, navigation, puzzle states, interiors, or quest triggers exist.

Build a spatial inventory from actual world data. Associate stable content IDs with dimension, bounded region, coordinates, orientation, and map-version fingerprints. Record verified routes, traversable surfaces, blocked entrances, water/climb/glide opportunities, and encounter-safe areas.

Do not assume original-game coordinates map through a single global scale/rotation. Establish local landmark correspondences and measure registration error before placing content automatically. Keep low-confidence placements out of claimed-complete content.

Represent NPCs, chests, enemies, quests, puzzles, domains, collectibles, teleport points, and region triggers as a versioned gameplay overlay. Keep per-player and shared state separate according to specified ownership rules. Preserve the artwork by default; disable unwanted terrain destruction and uncontrolled vanilla spawning in managed areas.

Use incremental, idempotent placement/import operations. Track map updates by fingerprints and identify affected bindings; do not blindly re-run coordinate generation or invalidate player progress. Optional geometry changes belong in explicit reversible patches on working copies.

Do not bulk-populate the whole map with thousands of guessed entities. Verify a small area end to end, then expand in batches with reachability, duplicate-ID, collision, and trigger-validation checks. Load and simulate only relevant regions.

## 8. Automated testing before content expansion

The first engineering milestone is a controllable, observable game, not a large character roster.

Implement four complementary layers:

- Rules tests: deterministic examples, boundary cases, invariants, and independent reference comparisons.
- Server integration tests: actual game entities, lifecycle, save/load, packets, player ownership, reward idempotency, and adverse sequences.
- Scripted real-client tests: controls, camera, rendering, menus, resource packs, and client/server integration.
- Exploratory model-driven real-client sessions: ambiguity, recovery, discoverability, unexpected action sequences, and player-visible regressions.

Fabric documents server game tests and client game tests. Verify which APIs exist for the pinned version; current documentation is not evidence that every older release provides the same features. Use a thin compatible runner when necessary. [S6]

### Real-client control bridge

Build the smallest adequate development-only bridge. It should support bounded key/button holds, camera changes, ordinary UI interaction, bounded waiting, and timestamped screenshots. Player actions must go through normal gameplay paths, not directly set success state.

Provide structured player-visible observations when helpful: visible HUD values, active screen, normal inventory information, and recent visible messages. Keep these synchronized with the image by frame/tick identifiers. Do not provide hidden enemies, undiscovered objectives, internal quest solutions, arbitrary exact state, or omniscient map coordinates in a black-box player run.

Label observation modes explicitly. Screenshot-only visual evaluation means no parallel HUD transcription or semantic hinting. An image-plus-player-visible-data run is a separate assisted mode. Both may be useful, but do not attribute results from the latter solely to visual understanding. Any model-based image interpretation uses DeepSeek V4.1 Flash at max; deterministic image comparisons are permitted as such.

Separate capabilities mechanically:

- **Player:** normal controls and permitted observations only.
- **Diagnostic:** detailed server state, event traces, entity inspection, and reproduction tools; no claim of normal playability.
- **Fixture manager:** controlled setup, seeded state, reset, and snapshots outside the player policy.

Enforce separation through actual tool exposure/access control. A player agent must not have shell/source access, the fixture token, arbitrary teleportation, invulnerability, reward grants, or a hidden `completeQuest` call. Prompts alone are insufficient isolation.

Bind control services to loopback, authenticate sessions, use isolated test directories, and exclude administrative test hooks from release builds. Include a release check proving they are unavailable to normal remote clients.

### Timing modes

Provide a deterministic stepping mode that advances bounded simulated time after inputs and returns observations. This reduces dependence on the model's response latency. Pause/step all gameplay timers and coordinate all participating clients; handle networking/display maintenance separately so the environment does not deadlock.

Also run unpaused real-time tests. Passing a frozen/stepped environment does not establish real-time combat feel, synchronization, or resilience to network delay. Use bounded input sequences or a deterministic low-level controller, label assisted runs, and preserve some raw-input tests. Record API/reasoning latency separately from simulated gameplay time. Never lower effort to accelerate a playtest; adjust stepping, observation frequency, batching, or test duration instead. Controller-assisted runs must not be presented as evidence that the model itself achieved frame-level execution.

A protocol-only bot can supplement load and server testing but does not demonstrate that the modded client, custom screens, animations, or controls work. Two clients in one integrated setup are not a substitute for a dedicated-server connection test.

### Test evidence

Every run records build commit, dependency/content/map fingerprints, fixture/seed, player permissions, model/provider/effort, routing compliance, observation and timing modes, controller assistance, inputs, screenshots/clips when useful, server/client logs, and evaluator verdict. Include all failures and retries, not only the successful final trace. Label failures as product, harness, infrastructure, or agent-policy failures before choosing a repair.

Create a one-command reproduction for each accepted bug. A fix must pass the original reproduction, a durable regression, and relevant neighboring tests. Never weaken an assertion, remove a failing test, or fabricate expected behavior just to obtain green results.

## 9. DeepSeek-only qualification and capability evaluation

The model is fixed: DeepSeek V4.1 Flash at max. Qualification diagnoses the actual environment and capabilities; it is not a model-selection contest. A failure calls for harness repair, a clearer task, additional evidence, a bounded same-model retry, or an honest blocker—not a replacement model.

### Prove the visual/action path

Once a minimal client environment exists, verify that a fresh DeepSeek player session receives a known screenshot, identifies a visible change, issues a valid ordinary action, receives a new synchronized image, and continues through multiple tool turns. Check that images are not stripped, stale, mislabeled, or silently replaced with another model's description. Verify max through the same provider/adapter path used in actual playtests.

Do not infer successful visual integration from a successful text-only tool call. If the image path fails, diagnose transport/adapter support and use deterministic client tests while repairing it. A text-only or structured-state-only player run may be labeled as such, but it does not count as a visual-playtesting pass. If a compliant visual route cannot be established, record the limitation and continue only unaffected work.

### Small repeated gameplay suite

Use three short scenarios with two repeats each initially, equal starting states, and bounded input/model-call budgets. Choose scenarios available in the current build; unavailable features are not a model failure. Grow the suite as the playable slice matures to include visible-target navigation, menus, character selection, visible HUD changes, attacks, obstruction/death recovery, and an intentionally seeded visible defect.

Separate screenshot-only, image-plus-player-visible-data, scripted-controller-assisted, stepped, and unpaused real-time results. These conditions need not form a large full-factorial study; keep a small useful baseline and report which conditions were actually tested. Preserve a few held-out states/routes for acceptance rather than repeatedly tuning on the same demonstrations.

Score predefined outcomes through deterministic server/test oracles where possible, outside the player's accessible tools. For genuinely qualitative judgments, use a separate fresh DeepSeek V4.1 Flash/max evaluator with the rubric, reference evidence, and recorded observations, not merely the player's success claim. The player cannot mark its own task complete or relax the evaluator. The evaluator's use of diagnostic evidence must not leak hidden information back into the current blind player run.

Log task completion, valid/invalid actions, false success claims, unnecessary resets, recoveries, actionable bug reports, deterministic confirmation, tool failures, latency, and metered usage when available. Retain failed trials and distinguish timeout, transport failure, environment defect, and decision failure. Small repeated samples are diagnostic, not a statistically definitive assessment of the model's general ability.

### Evaluate development, not just gameplay

Maintain a concise `docs/deepseek-evaluation.md` drawn primarily from existing task/test logs. Record milestone gates passed, verified requirements, rejected/rolled-back changes, first acceptance-test outcomes, correction cycles, review findings confirmed by regressions, defects reaching integration, and human interventions. Distinguish ordinary owner setup/product decisions from technical rescue, hints, manual patches, or manually completed gameplay tasks.

Record the initial repository baseline and attribute only subsequent verified work to the compliant run. Capture meaningful harness/configuration changes and whether performance improved after them. Report tokens/cost only when measured or clearly calculated from documented inputs; do not invent missing usage. Shared-model agreement is not validation. Ground claims in behavior, independent source evidence, tests, and recorded limitations.

Do not consume the project budget producing elaborate benchmark infrastructure. The working game and reproducible gates are the principal evidence. Preserve failed approaches honestly without allowing them to consume unbounded retries.

### Reuse deterministic evidence

Save successful input traces as cheap regressions and create minimal reproductions from failures. Do not make a model call for every rendered frame or predictable scripted action. Use DeepSeek where interpretation, exploration, adaptation, design, or diagnosis adds value. Deterministic tests remain valuable evidence of the implemented product, but scripted execution alone is not evidence of autonomous model play.

## 10. Delivery milestones and gates

### M0: runnable foundation and trustworthy testing

Pass the exclusive DeepSeek/max routing preflight; inspect inputs; lock the build; boot a synthetic world and a safe copy of the purchased map when available; launch a client and a dedicated server; capture a real screenshot; issue normal input; observe a server-authoritative state change; reset and reproduce it. Demonstrate two clients joining the dedicated server, or record the exact external access/resource blocker without pretending multiplayer was tested.

Deliver working launch/test/reset commands and evidence, not only an architecture document. Include an end-to-end compliant DeepSeek/max screenshot-action-observation trace, or explicitly mark that portion of M0 blocked; a scripted trace does not replace it. Keep optional visual dependencies in a separately tested profile.

### M1: first representative playable slice

Use a compact confirmed area, a starter party chosen to exercise distinct behaviors, a small enemy set, and one short quest/encounter loop. Anemo Traveler, Amber, Kaeya, and Lisa are a suggested initial selection, subject to evidence and implementation sequencing—not a demand to implement four unfinished kits simultaneously.

Implement incrementally: movement/camera and one complete action loop; then party switching and contrasting attacks; then specified elemental interactions; then exploration/quest/reward/persistence. Keep the game runnable between changes.

Gate on an ordinary player being able to:

- Start, understand the immediate objective, travel using the relevant movement abilities, and interact with a real trigger.
- Fight using the implemented party abilities and visibly produce independently verified combat outcomes.
- Finish the objective, receive the specified reward once, open relevant menus, and inspect progression.
- Quit/rejoin and restart the server without losing or duplicating progress.
- Repeat relevant cases with a second player, including overlapping interactions and disconnect/reconnect.

Golden rules tests, real-client observations, and dedicated-server tests must support the gate. Do not call this slice a complete Genshin recreation.

### M2: system completeness

Expand the rules engine, movement cases, enemy behaviors, equipment, character progression, quest state machines, puzzles, exploration systems, death/revival, and party/co-op ownership. Add representative content only when needed to validate each new subsystem.

### M3: controlled content expansion

Grow character, enemy, quest, domain, item, and region coverage through the validated data pipeline. Research exceptions just in time. Report coverage by category and verification status; avoid a misleading single percentage for the entire game.

### M4: hardening and presentation

Test realistic multi-client play, reconnects, stale/duplicate requests, save migration, long sessions, map-edge cases, malformed content, and unexpected action order. Profile server tick time, client frame time, memory, chunk activity, and network traffic on recorded hardware/settings. Set realistic numeric budgets from the measured baseline rather than inventing guarantees.

Re-test with the intended visual stack and without development privileges. Package original mod artifacts separately from private map inputs. Produce install instructions and attribution. Keep public distribution, hosting, monetization, and third-party rights decisions behind explicit approval.

## 11. Persistent state, supervision, and bounded autonomy

Maintain a small durable project record:

- `AGENTS.md`: concise invariants, verified commands, boundaries, and links to deeper documents.
- `docs/environment.md`, `docs/runtime-routing.md`, `docs/architecture.md`, `docs/decisions.md`, and `docs/deepseek-evaluation.md`.
- `spec/`: evidence-linked behavior requirements and a fidelity ledger.
- `project-state/`: dependency-aware task queue, current milestone, blockers, and the next runnable task.
- `tests/`: independent fixtures and regression definitions.
- `artifacts/`: ignored local run evidence, with sanitized summaries committed only when appropriate.
- `CREDITS.md` and a dependency/asset provenance register.

Adapt paths to an existing repository instead of duplicating its organization. Keep raw research separate from short decision-ready specifications. Do not fill every agent context with all collected material.

Each work cycle is: select a ready bounded task; establish acceptance criteria and evidence; implement; run relevant tests; obtain fresh-context review when warranted; merge verified changes; update state; continue. All AI steps use DeepSeek V4.1 Flash at max. Reconcile persisted state with the actual repository/tests and revalidate routing at restart instead of trusting a stale summary.

Context compaction must use the same model at max or a non-model mechanism. Keep durable specifications and evidence outside the conversation so resumption does not depend on a hidden summary. If OMP cannot make its automatic compaction path compliant, disable it and use explicit compliant checkpointing/fresh sessions through supported interfaces. Do not wait until the context is exhausted to discover a forced incompatible helper.

Use finite task/model-call/time/resource budgets in the local run configuration. Honor existing owner/provider limits; do not enable new billed services, buy credits, or provision machines. Until an owner-specified expanded playtest budget exists, cap the initial exploratory batch at 100 model calls in total across its players, model-based evaluators, and qualification. This is a playtest-batch safety limit, not a budget for completing the entire project or a cost estimate. Keep implementation/review budgets separately visible within the authorized run limit. Report unavailable usage figures as unknown. Deterministic local regressions need no model-call allocation.

Respond to cost, rate-limit, context, or machine pressure by shrinking tasks, retrieving less irrelevant context, lowering concurrency, batching predictable actions, caching results, or checkpointing. Never respond by changing the model, lowering effort, dropping required validation, or claiming that max was used when it was clipped. Use supported provider request limits sufficient for the selected reasoning mode; report incompatibilities rather than silently relabeling a constrained request.

Limit retries. After three materially different unsuccessful repair attempts, save the smallest reproduction, attempted fixes, test results, relevant source evidence, and uncertainty. Request one bounded fresh-context DeepSeek V4.1 Flash/max diagnosis, preferably using a different decomposition or second-method check. Escalation means better evidence or a different approach, never a different model or effort. If still blocked, record the blocker and continue independent ready work; ask the owner only for genuinely unavailable input or authority. Do not loop indefinitely, broaden scope, or repeatedly reset the world to hide a failure.

If OMP can execute a resumable local runner with checkpoints and stop conditions, implement and verify it using supported interfaces. Do not assume a prompt alone provides process supervision, automatic restart, or work after the session ends. Do not install an autostart service or infinite self-reprompting loop. Stop safely at authorization/resource limits while leaving other independent tasks ready to resume.

Provide brief updates at meaningful checkpoints: verified behavior, evidence location, blockers, and next task. Report routing violations immediately and summarize actual human intervention and capability limits at milestones. Do not seek approval for every implementation step. Never claim an unexecuted test passed, an unobserved feature is playable, or an unaudited run is proven single-model.

## 12. Immediate execution order

1. Verify the current lead and all planned model-backed invocation paths use DeepSeek V4.1 Flash/max; fix project/session routing and disable substitutions before substantive work. Preserve the workspace and global configuration.
2. Inspect the map/environment, capture the repository baseline, and write compact version, scope, routing, run-budget, and task-dependency records.
3. Build and prove the smallest real-client control/test loop while a compliant worker researches the first movement/combat slice. Use synthetic inputs where purchased inputs are absent.
4. Run DeepSeek's initial visual/action qualification only when an actual usable environment exists; log observation mode, routing evidence, and failures without substituting models.
5. Complete M0, then implement and verify M1 through small integrated changes, fresh-context same-model reviews, deterministic checks, and real-client evidence.
6. Continue ready tasks within the active run's budget. Checkpoint safely at hard limits. Do not stop merely because a plan was produced, and do not claim the full recreation is complete because one slice passes.

## Source register

These are starting references carried forward for the implementing agent to inspect as needed; this rewrite does not claim they were freshly accessed or that their contents match the installed environment. Local packages, installed schemas, and current primary documentation take precedence where they differ. Sources support factual investigation, not claims that the proposed architecture or requested model integration already works.

- **S1 — WanggMC map listing:** `https://ko-fi.com/s/5609329f72`
- **S2 — OMP task-agent discovery and model selection:** `https://github.com/can1357/oh-my-pi/blob/main/docs/task-agent-discovery.md`
- **S3 — OMP prompt keywords:** `https://github.com/can1357/oh-my-pi/blob/main/docs/magic-keywords.md`
- **S4 — DeepSeek V4.1 Flash release reference:** `https://api-docs.deepseek.com/news/news260910/`
- **S5 — DeepSeek thinking controls:** `https://api-docs.deepseek.com/guides/thinking_mode/`
- **S6 — Fabric automated testing:** `https://docs.fabricmc.net/develop/automatic-testing`
- **S7 — OMP Agent Hub and runtime metadata:** `https://github.com/can1357/oh-my-pi/blob/main/docs/agent-hub.md`
- **S8 — KQM Theorycrafting Library:** `https://library.keqingmains.com/`
- **S9 — Example detailed mechanic/evidence entry, internal cooldown:** `https://library.keqingmains.com/combat-mechanics/internal-cooldown`
- **S10 — Minecraft usage guidelines:** `https://www.minecraft.net/en-us/usage-guidelines`

Creator profiles supplied by the owner: `https://www.instagram.com/wangg_mc/`, `https://www.patreon.com/cw/wanggmc`, and `https://ko-fi.com/wanggmc/shop`. No purchased map package or local OMP installation was available for inspection when this specification was written. Availability, permissions, and runtime compatibility must be checked rather than inferred from these links.
