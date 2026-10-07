# Mondstadt-area landmarks and content overlay

Located on 2026-10-08 on `run/map-port/landmark-world`, a separate `cp -a --reflink=auto` copy of the already-upgraded `run/map-port/blocky-teyvat-26.2/world`. The purchased ZIP and upgrade evidence world were not edited. Minecraft's force upgrade did not translate blocks: the owner's 1.21.10 camera coordinates work directly in 26.2, including the negative Y coordinates/custom −2032…2032 build height.

## Landmark confirmation

The three owner screenshots under `~/overnight/feedback-2026-10-04/genshin/` were compared with these private 1280×720 software-rendered client views. Screenshots remain ignored under `run/`, not embedded/shipped with the mod. Different FOV/view distance and missing source shaders do not imply missing terrain.

| Landmark | Camera feet `(x,y,z)` / view | Confirmed geometry / screenshot |
| --- | --- | --- |
| Mondstadt upper city | `(2586.375, −138.93907, −3665.221)`, yaw91.2/pitch34.5 | Cathedral/central plaza and giant winged Barbatos sculpture: `run/map-port/landmarks/01-mondstadt-owner-view.png`. The sculpture is a city landmark, **not** a functional Statue of The Seven. |
| Starfell Valley / Starfell Lake | `(3255.539, −186.00420, −3887.155)`, yaw70.3/pitch8.6 | Lake with small grassy island, enclosing cliffs and approach woods: `run/map-port/landmarks/02-starfell-owner-view.png`. |
| Windrise | `(2859.338, −213.98666, −3318.664)`, yaw−87.4/pitch3.3 | Enormous oak to the east/southeast, open valley and river: `run/map-port/landmarks/03-windrise-owner-view.png`. |
| Starfell island travel build | `(3095, −211, −3838)`, yaw90/pitch20 | Island/build close view: `run/map-port/landmarks/04-starfell-island-anchor.png`. |
| Introductory-roster camp footprint | `(2891, −211, −3773)`, yaw180/pitch25 | Existing woodland camp/fire footprint, not a new block build: `run/map-port/landmarks/05-intro-camp-candidate.png`. |

Read-only region-palette/block-entity inspection found beacon bases at **Starfell `(3084,−218,−3838)`**, **Windrise `(3032,−237,−3195)`**, city `(2665,−209,−3684)`, mainland bridge approach `(2934,−204,−3639)`, Starfell northern rise `(3038,−178,−3993)` and eastern Windrise `(3298,−194,−3245)`. The island and oak travel sculptures also use original map **item-display entities** (jungle-plank item displays near `(3084.6875,−213.125,−3837.4375)` and `(3032.4375,−232.125,−3194.6875)`); a section-palette scan alone would miss that art. These decorative objects are preserved. Our entities sit beside them, not on top of or replacing their blocks/display sculptures.

Read-only exact landmark probes (block coordinates, not extra overlay entities): the upper plaza's paved approach has stone-brick floor at **`(2586,−200,−3665)`**; the giant sculpture includes andesite at **`(2530,−160,−3665)`**. The city-side bridge mouth has stone-brick floor at **`(2775,−223,−3665)`** (feet−222), distinct from the mainland Waypoint farther east. Windrise's actual lower trunk includes **oak-wood blocks at `(3030,−228,−3155)`**, behind the Statue forecourt to its southeast. The Starfell island beacon is the precise lake/island centre-of-interest anchor above, not the owner's distant camera.

## Version1 binding

[`common/src/main/resources/data/genshininminecraft/overlay/mondstadt.json`](../common/src/main/resources/data/genshininminecraft/overlay/mondstadt.json) is the authoritative feet/arrival table. IDs remain stable independently of names. Each member UUID derives from `genshininminecraft:overlay/<overlay-id>/<object-id>`; camp member IDs append `.0`, `.1`, `.2`. Y below is **feet**, normally one block above the inspected floor.

| Stable ID / purpose | Entity anchor `(x,y,z)` | Safe arrival `(x,y,z)` |
| --- | --- | --- |
| `starfell.statue` — island Statue | `(3087.5,−217,−3831.5)` | `(3095.5,−219,−3837.5)` |
| `windrise.statue` — oak-foot Statue | `(3027.5,−236,−3199.5)` | `(3018.5,−236,−3199.5)` |
| `mondstadt.city` — ordinary city Waypoint | `(2669.5,−212,−3683.5)` | `(2670.5,−212,−3683.5)` |
| `mondstadt.gate` — mainland gate/bridge approach Waypoint | `(2938.5,−206,−3638.5)` | `(2938.5,−206,−3634.5)` |
| `starfell.north` — northern rise Waypoint | `(3042.5,−179,−3992.5)` | `(3045.5,−179,−3992.5)` |
| `windrise.east` — coastal rise Waypoint | `(3296.5,−196,−3248.5)` | `(3296.5,−196,−3246.5)` |
| `starfell.intro-camp` — 2 basic + shooter-as-basic, Lv8 | `(2891.5,−229,−3786.5)` | Members have individually inspected feet; see JSON. |
| `windrise.camp` — 3 adapted basic, Lv8 | `(2950.5,−236,−3229.5)` | Members have individually inspected feet; see JSON. |
| World arrival / no-unlock wipe fallback | `(2938.5,−206,−3634.5)`, yaw90 | Mainland bridge approach, four blocks clear of the obelisk. Source world spawn remains `(3458,63,−4002)`. |

**Source correction:** the functional Anemo Statues in this slice are **Starfell Lake** and **Windrise**, not the Barbatos plaza sculpture or a fabricated extra Statue outside Mondstadt's gate. The mainland gate marker is an ordinary Waypoint. See the [sourced naming/placement distinction](../spec/mechanics/world-objects.md#landmark-based-placement-guide). These are local landmark bindings, not a claimed global map scale/coordinate transform.

## Applying and playing

Ordinary worlds remain unaffected. An operator first enables `/genshin managed on`, then runs **`/genshin overlay apply`**. Application persists version1 and sets the overlay arrival as world spawn metadata; it places **only original interactable mod entities**, never blocks. On subsequent managed loads, entity-loaded anchor chunks reconcile missing markers; deterministic UUIDs prevent duplicates while chunk entity reads finish. Living camp members are never inferred dead from absence in an unloaded leash chunk. Killed members retain their persistent twelve-hour deadline across reapply/reload.

The fresh-copy command in [scripts/map-port/README.md](../scripts/map-port/README.md#managed-mondstadt-playtest) writes a tiny `.genshin-playtest.json` marker into the disposable copy. Its first server tick enables managed mode and applies the shipped overlay, then ordinary SavedData owns it. It does **not** silently install this map binding into every arbitrary managed arena.

Right-click an obelisk within the adapted4-block range to activate it **for that player**. Activated markers glow (world-shared presentation). **M** or `/genshin teleport` opens the current player's unlocked destination list; select a destination for free travel, including escape from combat. Merely approaching does not unlock ordinary Statues/Waypoints. Near a Statue, fallen deployed-party members revive at rounded35% without reserve expenditure; after two continuous seconds within the adapted6-block radius an unlocked Statue heals the four deployed slots toward the adapted100% target from the world's shared reserve. A full wipe's vanilla Revive action uses the nearest point unlocked by that player at the location of final death, with rounded35% HP; before any unlock the mainland arrival is the named fallback. Statue arrivals intentionally lie outside its six-block healing radius: travel itself is not full healing.

No camp chests/rewards/offering progression are added in item23. Existing purchased-map decoration is left untouched. Exact camp levels, Windrise roster and quest-camp respawn classification are unknown; Lv8/three basics and ordinary repeatable twelve-hour-from-death camps are explicitly documented adaptations in [decisions](decisions.md) and [fidelity](../spec/fidelity.md), not source-game placement/quest claims.

**First-load placement correction:** the live port's vanilla new-player heightmap selectedY−2031 despite the authored spawn metadata. The overlay therefore sends a player with no prior party save to the exact arrival on first entry, before combat/recovery; an empty persisted unlock list records entry without unlocking a point. Existing saved-party players keep their saved position. This is an actual arrival fix, not immunity or suppression of void/fall damage.

## Real-map overlay screenshot tour

The private1280×720 llvmpipe/Xvfb tour used a fresh `make-playtest` copy of the upgraded world, not the older map-port spike playtest. Original purchased decoration and every terrain block were left untouched.

| Required subject | Ignored screenshot / observed result |
| --- | --- |
| Starfell Statue | `run/map-port/overlay-tour/01-starfell-statue.png` — activated original mod obelisk beside the preserved island Statue/sculpture, with Traveler and the lake. Real right-click activation/list: `01-starfell-activated-list.png`; selecting its destination travelled to the authored island arrival. |
| Mondstadt city Waypoint | `run/map-port/overlay-tour/02-mondstadt-city-waypoint.png` — full-bright mod obelisk in the foreground, preserved decorative Waypoint behind it, and city buildings. `02-mondstadt-activated-list.png` shows both city and Starfell destinations unlocked for the tour player through real interactions. |
| Windrise camp | `run/map-port/overlay-tour/03-windrise-hilichurl-camp.png` — the three Lv8 club substitutes at the path-side camp, with the oak/river/cliffs behind. The centre member is partly occluded by untouched tall grass. `03-windrise-managed-camp.png` gives the wider oak view; `03-windrise-roster-status.png` shows all three live positions. |

The city/camp sightseeing views use Spectator, with managed mode **on in the required captures**. Managed mode was temporarily suspended to seed the camp camera's vanilla yaw/pitch, then re-enabled before capture; these are placement/rendering evidence, not combat-AI assertions. The server GameTests exercise the live gameplay separately. The playtest was returned to Adventure/managed mode and the mainland arrival, saved and quit normally; no client ran alongside the Gradle gate.

