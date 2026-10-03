# Decisions

Dated choices and reasons. [The project brief](GENSHIN_MINECRAFT_BRIEF.md) is the durable specification; [todo.md](../todo.md) gives the runnable order. Record future changes here rather than silently changing a fixed choice.

## 2026-09-29 — fixed decisions from the brief

| Topic | Decision | Reason |
| --- | --- | --- |
| Minecraft | 26.2, Java 25 (see `gradle.properties`). | Fixed platform baseline for code, dependencies and tests. |
| Loaders | Fabric and NeoForge; shared code, mixins and resources in `common/`, with thin loader adapters. Every feature works on both. | One shared implementation with loader parity. |
| Map | Blocky Teyvat 5.1.0, shipped for 1.21.10, ported to 26.2 on a copy. | Use the purchased map while preserving its pristine source. |
| Genshin reference | Version 7.1, "A Rekviem for the Underworld", released 2026-09-23. Re-pin only as a deliberate decision. | Keep sourced mechanics and test expectations on one version. |
| First area | A compact part of Mondstadt that the map actually contains. | Prove one playable area end to end before expanding content. |
| Starter party | Traveler (Anemo), Amber, Kaeya and Lisa, one kit at a time. | A bounded starter-party slice that grows without losing playability. |
| Multiplayer | Integrated and dedicated servers, up to four players. Follow Genshin co-op rules; record deviations as adaptations. | Support the owner and friends without silently changing ownership or party rules. |
| Character visuals | Placeholders first; better models later, including privately used fan models such as the YiFang `.ysm` set. | Prioritize readable, playable kits before visual polish. |
| Payments | None. Character acquisition, if added, is local gameplay only. | No payment or external-account dependency. |

## 2026-10-04 — first playable draft scope and order

Follow the [first playable draft queue](../todo.md#first-playable-draft-planned-2026-10-04): project records and template cleanup; sourced starter mechanics; the plain-Java rules layer; a managed world and arena; server-owned Traveler state and kit; HUD; a hilichurl camp; stamina; then party switching and Amber, Kaeya and Lisa kits/reactions. The map-port and camera spikes follow in the queue.

The draft target is a player joining a world, playing the Anemo Traveler against a hilichurl camp with a Genshin-style HUD, and then switching among the starter party to trigger reactions. Each step stays runnable on both loaders. This is a delivery slice, not a reduction of the brief's full-recreation goal.

## 2026-10-04 — generated arena before the map port

Develop and test gameplay in a generated flat test arena until the map port is available. The planned `/genshin arena` command is tracked in `todo.md`; it is not part of the initial root-command replacement. This keeps mechanics work independent of the map upgrade and protects the purchased archive from iterative development writes. Port only disposable copies, and keep world data out of git.

## 2026-10-04 — remove template examples

Remove the template command, client command and empty injection classes rather than carrying them into gameplay code. `/genshin` is the real server command root and initially reports the mod version obtained from loader metadata. Keep the shared command-registration plumbing for later commands, with no client-only commands registered yet.

Replace the old command-registration tests with `/genshin` registration coverage, including one shared server GameTest registered on each loader. Preserve the Fabric loader metadata test and the client dedicated-server join/in-world checks. Keep both mixin JSON files and their loader references, with valid empty lists, so the existing cross-loader resource setup remains consistent.
