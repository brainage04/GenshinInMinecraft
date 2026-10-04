# Blocky Teyvat 5.1.0 → Minecraft 26.2 map-port spike

Investigated and upgraded 2026-10-04 for [todo item 10](../todo.md) and [hard problem 1](GENSHIN_MINECRAFT_BRIEF.md#1-porting-the-map-to-262). The purchased ZIP remained read-only. The full disposable copy was extracted and upgraded with the official 26.2 server; the after-scan and private Fabric client checks below use that completed upgrade, **not a second force-upgrade**. Deterministic terrain sampling and nearby screenshots are not full-world voxel equivalence or landmark fidelity sign-off.

## Source, versions and size

- Pristine source: `reference/map/Blocky Teyvat 5.1.0-001.zip`; compressed file size **3,948,634,100 bytes**.
- `nice -n 19 ionice -c3 unzip -l` listed the launcher instance in **0.16 s**: **3,497 entries** including directories, **3,408 regular files**, **6,727,000,159 uncompressed bytes** in the entire package.
- The unique world is `Blocky Teyvat 5.1.0/saves/方块提瓦特2025 77/`. Python's ZIP reader preserves the actual Unicode name (the terminal's `unzip -l` rendered those characters as `?`).
- World only: **3,176 regular files**, **6,122,749,281 uncompressed bytes** (6.123 decimal GB / about 5.70 GiB). This includes empty region files, backups, saved editor/mod metadata, and the Voxy cache; do not confuse it with live terrain size.
- `level.dat` is gzip-compressed Java NBT. `Data.DataVersion = 4556`; `Data.Version = {Name: "1.21.10", Id: 4556, Series: "main", Snapshot: 0}`; `LevelName = 方块提瓦特`.
- The already cached official 26.2 client jar's `version.json` reports **`world_version = 4903`**, Java **25**, data pack version **107.1**, resource pack version **88.0**. Source: `$HOME/.gradle/caches/fabric-loom/26.2/minecraft-client.jar:version.json`. The source map therefore needs a **4556 → 4903** data-fixer upgrade; no target jar download was needed.
- Cached Mojang metadata (`$HOME/.gradle/caches/fabric-loom/26.2/mojang_minecraft_info.json`, `downloads.server`) identifies the official bundled server as **60,894,273 bytes**, SHA-1 **`823e2250d24b3ddac457a60c92a6a941943fcd6a`**, [Mojang server object](https://piston-data.mojang.com/v1/objects/823e2250d24b3ddac457a60c92a6a941943fcd6a/server.jar). The port driver pins this checksum and validates the bundled version metadata before use. The cached bundle's `META-INF/versions.list` identifies `26.2/server-26.2.jar` as its inner jar.
- Initial `df -h .` reported **116 GiB available** on the repository filesystem (0.06 s). Space was sufficient for the full extraction, upgrade and independent playtest copy. The driver requires the exact world bytes plus extraction/upgrade reserve; budget more than the bare 6.12 GB for logs, libraries and region rewriting.

Dimension inventory counts **regular files**; overworld also includes shared world metadata, backups and Voxy files. “Terrain” means exact `region/r.X.Z.mca` names, not entities/POI/backups.

| Dimension/directory | Files | Total uncompressed bytes | Terrain regions / bytes | Entity regions / bytes | POI regions / bytes |
| --- | ---: | ---: | ---: | ---: | ---: |
| `minecraft:overworld` / world root | 3,146 | 6,122,747,258 | 1,191 / 4,434,755,584 | 856 / 327,962,624 | 882 / 14,106,624 |
| `minecraft:the_nether` / `DIM-1` | 3 | 198 | 0 / 0 | 0 / 0 | 0 / 0 |
| `minecraft:the_end` / `DIM1` | 3 | 198 | 0 / 0 | 0 / 0 | 0 / 0 |
| `immersive_portals:alternate1` | 4 | 271 | 0 / 0 | 0 / 0 | 0 / 0 |
| `immersive_portals:alternate2` | 4 | 271 | 0 / 0 | 0 / 0 | 0 / 0 |
| `immersive_portals:alternate3` | 4 | 271 | 0 / 0 | 0 / 0 | 0 / 0 |
| `immersive_portals:alternate4` | 4 | 271 | 0 / 0 | 0 / 0 | 0 / 0 |
| `immersive_portals:alternate5` | 4 | 271 | 0 / 0 | 0 / 0 | 0 / 0 |
| `pswg:tatooine` | 4 | 272 | 0 / 0 | 0 / 0 | 0 / 0 |

The non-overworld directories contain only old `data/*.dat` metadata, **no terrain/entity regions**. `WorldGenSettings.dimensions` registers only `minecraft:overworld`; their folder names are not evidence of playable custom dimensions. Mod-specific saved data also survives in the root (`imm_ptl_dim_reg.dat`, `global_portal.dat`, `mtr_train_data.dat`, `geckolib_ids.dat`, Fabric registry files, etc.). This is evidence of the world's editing history, not by itself evidence of modded blocks.

Other significant bytes already included above:

- Voxy cache: **1,302,612,012 bytes**.
- Region backups (`.backup`): **42,407,034 bytes**; not live region inputs.
- Of 1,191 terrain files, **954 have nonzero file length** and **953 have populated chunk-location tables**; one nonzero file has an empty location table. Empty files are retained by extraction.

## Spawn, extent and border

- Saved spawn: **overworld `(3458, 63, -4002)`**, yaw **0°**, pitch **0°**. It is stored in `Data.spawn.pos` (an NBT int array), **not** legacy `SpawnX/Y/Z` keys. Chunk `(216, -251)`, region **`r.6.-8.mca`**.
- All present terrain **filenames**, including zero-byte files: region X **−30…30**, Z **−18…24**. The enclosing region-footprint block envelope is X **−15,360…15,871**, Z **−9,216…12,799**. These extrema do not imply every intervening region or block is generated.
- All **953 actually populated terrain region headers**, and the nonzero-length terrain files, have region X **−29…29**, Z **−17…24**; enclosing block envelope X **−14,848…15,359**, Z **−8,704…12,799**. Every terrain header was inspected, so these populated-region bounds are not limited to the palette sample.
- Legacy `Border*` fields are absent from `level.dat`. The saved border is instead in gzip NBT **`data/world_border.dat`**, DataVersion **4556**: centre **(0, 0)**, diameter/current size and lerp target **60,000,000**, lerp time **0**, safe zone **5**, damage per block **0.20000000298023224**, warning distance **5 blocks**, warning time **15 seconds**. It does not tightly bound the built map.

## Custom-height datapack: required preservation

The world is not a default-height overworld, even if its blocks are vanilla:

- Enabled datapacks: **`vanilla`, `fabric`, `axiom`, `fabric-convention-tags-v2`, `ok_zoomer`, `file/worldpainter.zip`**.
- Disabled: **`minecart_improvements`, `redstone_experiments`, `trade_rebalance`**.
- `enabled_features` and `removed_features` fields are **absent**, rather than observed empty lists.
- The source world's `datapacks/worldpainter.zip` is only **623 bytes**, contains a `pack.mcmeta` declaring **pack_format 9**, and overrides `data/minecraft/dimension_type/overworld.json` with **`min_y=-2032`, `height=4064`, `logical_height=4064`**. That is **254 sections per full-height chunk** (Y −2032…2031), explaining the large air-palette totals and scan cost.
- Generator: `minecraft:flat`, one `minecraft:air` layer, `minecraft:the_void` biome, features/lakes disabled; seed **227290**. A startup-generated chunk outside existing terrain should not be mistaken for purchased map geometry.

**Do not use vanilla `--safeMode` or remove the WorldPainter pack.** [INFERENCE] Loading/upgrading with the default 384-block dimension could drop or ignore out-of-range sections and destroy map geometry. The cached 26.2 server's actual default overworld JSON uses `attributes`, `default_clock`, and `timelines` instead of the old `effects`/`bed_works`/`respawn_anchor_works` representation. Reusing the old pack blindly is not a safe cutover.

The driver ports **only the disposable copy**: starts from that official 26.2 overworld definition, retains every original field still present in the target schema (including all three height values), emits pack metadata for **107.1**, and removes only the four unavailable editor-provided enabled pack entries. It preserves original `level.dat` and the WorldPainter ZIP outside `world/` within the work directory, records preparation in JSON, and rejects unexpected enabled packs, pack contents or custom height instead of silently dropping them. Modern vanilla ambience/timelines come from the target jar.

## Saved game rules

The gzip NBT metadata scan records all **63** string-valued `Data.GameRules` in `run/map-port/spike/before.json` (and the initial `inventory.json`). Map-protection-relevant source values:

| Rule | Saved value |
| --- | --- |
| `doFireTick`, `allowFireTicksAwayFromPlayer` | `false` |
| `doMobSpawning`, `mobGriefing` | `false` |
| `randomTickSpeed`, `spawnRadius` | `0` |
| `doWeatherCycle`, `doDaylightCycle` | `false` |
| `doTileDrops` | `false` |
| `keepInventory`, `doImmediateRespawn` | `true` |
| `naturalRegeneration`, `pvp` | `true` |
| `fallDamage`, `fireDamage`, `freezeDamage`, `drowningDamage` | `true` |
| `tntExplodes`, `projectilesCanBreakBlocks`, `commandBlocksEnabled` | `true` |
| `doTraderSpawning`, `doPatrolSpawning`, `doWardenSpawning`, `doInsomnia`, `spawnMonsters` | `true` |
| `spectatorsGenerateChunks` | `true` |
| `axiomPlayerInvulnerability` | `false` |
| `axiomDoBlockGravity`, `axiomDoTrampleFarmland`, `axiomDoBlockDrops` | `true` |

### 26.2 migration and saved-data repair

The original **63** entries comprise **59 vanilla legacy rules and four Axiom editor rules**. Inspection of the cached 26.2 `GameRuleRegistryFix` bytecode (`minecraft-common.jar`, via `javap -c -p`; registered by `DataFixers` at schema **4658**) confirms that Mojang's fixer renames vanilla keys, converts string booleans/integers to typed NBT, inverts `disableRaids`/movement-check flags, and combines `doFireTick` plus `allowFireTicksAwayFromPlayer` into integer `minecraft:fire_spread_radius_around_player` (**0 disables fire spread; 128 is player-local; −1 is global**). No pre-renaming is needed. The upgraded rules now live in **`world/data/minecraft/game_rules.dat`**, not `level.dat`.

The first upgrade retained the four unknown camelCase Axiom keys. Its saved-data codec logged `Failed to parse saved data for 'SavedDataType[minecraft:game_rules]'` because those are invalid resource locations. **Direct vanilla-server queries on the independent playtest copy disproved the suspected reset to defaults:** the valid partial vanilla rules were still applied. Before repair, the saved file contained **58 correctly migrated vanilla entries plus four invalid Axiom strings**, DataVersion **4772**.

| 26.2 rule | Queried before repair | Repaired value |
| --- | --- | --- |
| `minecraft:random_tick_speed` | `0` | `0` |
| `minecraft:fire_spread_radius_around_player` | `0` | `0` |
| `minecraft:spawn_mobs`, `minecraft:mob_griefing` | `false` | `false` |
| `minecraft:advance_time`, `minecraft:advance_weather` | `false` | `false` |
| `minecraft:block_drops` | `false` | `false` |
| `minecraft:keep_inventory`, `minecraft:immediate_respawn` | `true` | `true` |
| `minecraft:respawn_radius` | `0` | `0` |

`port-map.py repair-gamerules --work-dir run/map-port/blocky-teyvat-26.2` restores **all original vanilla values** from the preserved `original-level.dat`, writes **58 typed modern entries / DataVersion 4903**, removes the four Axiom keys, and keeps an exact backup at **`run/map-port/blocky-teyvat-26.2/pre-repair-game_rules.dat`**. The full before/after rule inventory is in **`gamerule-repair.json`**; the pre-repair runtime answers/error are in **`run/map-port/query-server/logs/`**. It refuses live-world session locks and symlinks; repeat repair is idempotent and does not overwrite the backup.

The repaired file also loaded cleanly in the private Fabric integrated server: **no gamerule parse error** recurred, managed status was **off**, and all ten protection-rule queries reproduced the repaired values above. Those answers are retained in **`run/map-port/playtest-client-launch.log`**.

For future upgrades, `prepare_vanilla` filters unknown mod rule keys from **only the copied `Data.GameRules`**, preserves vanilla strings unchanged, and lets the official fixer own their migration. The preserved original metadata is not sanitized. TNT/projectile editing and vanilla player damage remain enabled as in the source; **managed mode was not used** for these checks.

## Palette/entity scan: measured deterministic sample

**No non-`minecraft:` block IDs or block-entity type IDs were found in the sampled terrain. No non-`minecraft:` entity type IDs were found across all entity regions.** This confirms vanilla namespaces within the stated coverage, not the unscanned terrain or compatibility of every ID/property with the target version.

The one-region benchmark (`region/r.6.-8.mca`) read **1,024 chunks**, **260,096 section palettes**, all chunk DataVersion **4556**, in **10.256 s** (10.41 s command wall time). [INFERENCE] Applying that rate to 954 nonzero terrain files gives approximately **163 minutes** before allowing for region variation, comfortably above the assignment's 45-minute full-scan threshold. A deterministic sample was therefore used instead of a full palette scan.

The completed command was:

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/palette_scan.py scan \
  'reference/map/Blocky Teyvat 5.1.0-001.zip' \
  --stride 32 --spawn-radius 2048 \
  --output run/map-port/spike/before.json
```

Exact selection:

- Sort all **1,191** terrain relative paths lexicographically; select indices **0, 32, 64, …** (zero-based; string ordering, not numeric region ordering).
- Add every overworld terrain region intersecting the spawn-centred **±2,048-block square**: region X **2…10**, Z **−12…−4**, all **81** of those files exist. The square, rather than a Euclidean circle, is intentional.
- Union: **115 terrain files**, **103 populated/nonzero files**, **98,884 terrain chunks**, **25,116,536 section palettes**, **422 distinct block IDs**. This is approximately **9.66% of terrain filenames / 10.80% of nonzero terrain files**, not a uniform voxel sample.
- Include **all 856 entity region files** (590 nonzero-length, 53 with actual entity chunks): **386 entity chunks**, **15,251 entity instances** across **14 vanilla type IDs**. Entity-file coverage is full even though terrain/block-entity coverage is sampled.
- Sampled block entities: **11,099 instances**, **20 vanilla type IDs**.
- Total region bytes streamed out of the ZIP: **1,036,505,088 uncompressed bytes** (terrain plus entities), **971 files**. Region bytes were held one at a time; no bulk world extraction was used.
- **960.670 s** measured scan duration (**16 min 0.670 s**, **960.86 s** command wall time). Peak process RSS **605,060 KiB / 590.88 MiB**, well below 2 GiB. Commands used nice **19**, idle IO class **3**, and one Python process.
- The complete manifest, per-region ID counts, metadata and timings are retained in ignored **`run/map-port/spike/before.json`**. After-upgrade scans use that exact manifest rather than resampling.

Top block counts are **section-palette entry counts by `Name`**, **not placed block voxels**. Different states of the same ID can contribute multiple entries in one section. Air dominates because every sampled chunk includes all 254 height sections.

| Block ID | Palette entries |
| --- | ---: |
| `minecraft:air` | 25,063,479 |
| `minecraft:water` | 165,578 |
| `minecraft:dirt` | 121,919 |
| `minecraft:grass_block` | 111,219 |
| `minecraft:stone` | 88,476 |
| `minecraft:short_grass` | 54,125 |
| `minecraft:tuff` | 52,467 |
| `minecraft:cobblestone` | 46,307 |
| `minecraft:spruce_leaves` | 45,330 |
| `minecraft:andesite` | 36,433 |
| `minecraft:fern` | 36,136 |
| `minecraft:large_fern` | 35,815 |
| `minecraft:tall_grass` | 35,755 |
| `minecraft:oak_leaves` | 35,544 |
| `minecraft:lily_of_the_valley` | 31,029 |
| `minecraft:snow` | 26,025 |
| `minecraft:deepslate` | 15,851 |
| `minecraft:dandelion` | 12,823 |
| `minecraft:rose_bush` | 12,576 |
| `minecraft:peony` | 12,159 |

Largest sampled block-entity counts: `minecraft:brushable_block` **8,901**, `barrel` **713**, `lectern` **622**, `banner` **319**, `campfire` **240** (all IDs use the `minecraft:` namespace). Full entity scan is dominated by `minecraft:item_display` **10,907**, `minecraft:falling_block` **3,777**, and `minecraft:tropical_fish` **510**; remaining vanilla types are armor stands, axolotls, block displays, cats, chest minecarts, end crystals, items, item frames, mangrove chest boats, rabbits and snowballs. Those display/falling-block entities may be part of the map's art; compare their post-upgrade counts as well as terrain.

**Chunk versions are mixed despite level.dat being 4556:** sampled terrain chunks are **78,119 × 4556** and **20,765 × 3465**; entity chunks are **338 × 4556** and **48 × 0**. DataVersion **0** is the observed saved tag, not an absent-field default. The force-upgrade step must process older chunks too; do not judge completeness from `level.dat` alone.

## Extraction and full official upgrade

The exact smoke extraction command was:

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/port-map.py extract \
  --work-dir run/map-port/extractor-smoke --only region/r.6.-8.mca
```

It extracted **one file, 5,976,064 bytes**, in **0.036 s** of measured extraction time (**0.19 s** command wall time). The extracted bytes were **identical** to the ZIP member; SHA-256 **`bd27b4b28986e8aa5c2f385815367d860f15a9f5b24ad0d374cf7d2238b986ba`**. No `level.dat` or full world was extracted into that smoke directory.

The installed `/run/current-system/sw/bin/java -version` reports **OpenJDK 25.0.4.1**. With builds stopped and the [Minecraft EULA](https://aka.ms/MinecraftEULA) explicitly accepted, the completed full-world upgrade used:

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/port-map.py upgrade \
  --server-jar "$HOME/.gradle/caches/fabric-loom/26.2/minecraft-server.jar" \
  --java /run/current-system/sw/bin/java \
  --before-report run/map-port/spike/before.json \
  --work-dir run/map-port/blocky-teyvat-26.2 \
  --accept-eula
```

The command extracts/reuses an untouched full copy, prepares the custom-height vanilla-compatible datapack, runs `-Xmx4G -XX:ActiveProcessorCount=1 -XX:+UseSerialGC ... --forceUpgrade --nogui` in the work directory, binds only to loopback, sends `stop` after server readiness, and compares the exact baseline region manifest. Outputs include `upgrade-server.log`, `preparation.json`, `before.json`, `after.json`, `diff.json`, and timings in `.map-port-extraction.json`. Both scripts and detailed options are documented in [scripts/map-port/README.md](../scripts/map-port/README.md).

Full extraction took **99 s wall time** (the driver's extraction-only marker records **88.204 s** for **3,176 files / 6,122,749,281 bytes**). The server reported **6942 s** for force-upgrade/optimization (**1 h 55 min 42 s**), processing **852,528 chunk records**. It reached `Done (0.262s)!`, received `stop`, and saved all dimensions cleanly. Logs: **`run/map-port/upgrade-driver.log`** and **`run/map-port/blocky-teyvat-26.2/upgrade-server.log`**.

The original driver then failed its manifest lookup because **26.2 moved**, rather than deleted, the overworld region directories: `region/`, `entities/` and `poi/` now live under **`dimensions/minecraft/overworld/`**. The cached `DataFixers` registers `DimensionStorageFileFix` at **4772**. **All 971 baseline paths exist at their new locations**, including **all 856 entity regions**. Of those entity files, **803 had zero chunk records** in the source (266 zero-byte files and 537 nonzero files with empty location tables); the other **53** held all **386 entity chunks / 15,251 instances**. No missing entity region was explained by deletion of an empty file: none was deleted.

The scanner now uses stable pre-upgrade logical region paths and records their actual relocated paths. A genuinely absent region is permitted **only if the complete baseline scan proves zero chunk records, zero sections and empty counters**; it is explicitly listed in `removed_empty_regions` in both scan and diff reports. A missing populated or unproven region still fails before producing a complete after-report, and the diff rejects decreased chunk-record counts even when a region file is still present. Ambiguous simultaneous old/new paths also fail. The port driver now persists successful server timing/state before scanning, so comparison errors do not hide completion of a future upgrade.

## Completed after-scan and comparison

The README's **scan-only** `palette_scan.py scan ... --manifest before.json` and `palette_scan.py diff before.json after.json` commands completed on the existing upgraded copy. The after-scan took **1009.394 s** (**16 min 49.394 s**; **1009.72 s command wall time**), peak RSS **620,960 KiB / 606.41 MiB**. Reports are **`run/map-port/blocky-teyvat-26.2/{before,after,diff}.json`**; the extraction marker now records the recovered comparison and completed state.

| Coverage/count | Before | After |
| --- | ---: | ---: |
| Logical selected region files | 971 (115 terrain + 856 entities) | 971, all relocated; 0 removed |
| Terrain chunk records | 98,884 | 98,884 |
| Entity chunk records | 386 | 386 |
| Section palettes | 25,116,536 | 25,116,536 |
| Block palette entries (all IDs) | 26,214,603 | 26,214,603 |
| Distinct block IDs | 422 | 421 |
| Sampled block-entity instances / IDs | 11,099 / 20 | 11,099 / 20 |
| Full entity instances / IDs | 15,251 / 14 | 15,251 / 14 |

**Every inspected chunk is now DataVersion 4903:** **98,884 terrain + 386 entity = 99,270 records**. `level.dat` is also **4903**. No non-`minecraft:` IDs were found in any inspected block palette, block entity or entity. There were **no block-entity/entity missing or added IDs and no count changes**, including all **10,907 item displays** and **3,777 falling-block entities**.

The **only block ID/count changes** were:

| Block ID | Before palette entries | After | Delta |
| --- | ---: | ---: | ---: |
| `minecraft:grass` | 63 | 0 | −63 |
| `minecraft:short_grass` | 54,125 | 54,188 | +63 |

`short_grass` already existed in the source, so the diff has **one missing ID and no added IDs**, not an added `short_grass` ID. This is the official **`grass` → `short_grass` rename**: the cached 26.2 `DataFixers` bytecode explicitly registers that `BlockRenameFix` at schema **3692**, needed by the source's older 3465 chunks. The changes are confined to **`region/r.27.-13.mca` (60 entries)** and **`region/r.9.3.mca` (3 entries)**. All other per-region block/entity counters, chunk counts and section counts match. This demonstrates ID/count preservation in the stated coverage; it is **not** a voxel/property/geometry equivalence proof.


## Private Fabric visual check

The upgraded evidence copy was **not loaded by the client**. `cp -a --reflink=auto` made the separate **`run/map-port/playtest-world`** (54.82 s); the repaired gamerule file was applied to that copy after the pre-repair rule queries. Fabric's dev client then quick-played the absolute copy path using the ignored **`run/map-port/client.init.gradle`**, on a **private `xvfb-run -a` display**, with **`ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1 LP_NUM_THREADS=2`**. The actual client process used **`-Xmx2G -XX:ActiveProcessorCount=2 -XX:+UseSerialGC`**, Java 25; renderer **llvmpipe / OpenGL 4.6 / Mesa 26.1.8**. No owner display/audio/input session was used. The experimental custom-world confirmation was accepted only on this disposable playtest copy.

Only non-destructive spectator/teleport, status and read-only gamerule/block queries were used. **Managed mode remained off; `/genshin arena` was never run.** The client read air successfully at **Y −2032 and 2031** in the loaded spawn column. The inspected spawn chunk retained all **254 sections, section Y −127…126**, agreeing with **min Y −2032 / exclusive max Y 2032**. The lower screenshots actually render terrain far below vanilla's −64 floor.

Four Minecraft F2 captures were inspected and copied under **`run/map-port/screenshots/`**:

| Screenshot | Camera (XYZ; yaw/pitch) | Observed content |
| --- | --- | --- |
| `01-saved-spawn-aerial.png` | `(3458, 63, −4002; 0°/70°)` | Saved-spawn aerial view: layered grass plateaus, trees, sheer stone cliffs, paths and a small water feature; lower/distant terrain fades into fog. |
| `02-spawn-valley-below-default-height.png` | `(3458, −180, −4002; 0°/12°)` | Below the spawn, inside the custom-height range: textured cliffs, grass/trees, shoreline, sand beach, water and a waterfall. The near valley floor is present, not void. |
| `03-nearby-shoreline.png` | `(3500, −215, −3880; 150°/5°)` | Nearby low shoreline: cliff faces, continuous water, sandy cove, grass and trees, all below the vanilla minimum height. |
| `04-nearby-elevated-overview.png` | `(3500, 20, −3800; 180°/55°)` | Flew upward from the shoreline: terraced grass ridge, surrounding plateaus, trees, winding path and water channel. |

**No obvious missing textures, missing foreground blocks or void holes were seen in those loaded areas.** The saved spawn is well above its local sandy section (the spawn chunk's non-air palette is at section Y **−15**, block Y **−240…−225**), so looking horizontally from Y 63 initially showed mostly sky/fog; lowering the camera reveals intact shore/valley terrain. This is not evidence of lost chunks. Distant sky/fog in the aerial captures is finite vanilla view distance, not a full-map completeness claim. These are nearby-area screenshots, **not identified Mondstadt landmarks**; there was no source-side in-game screenshot comparison.

Client launch/render/command evidence: **`run/map-port/playtest-client-launch.log`** and **`run/map-port/playtest-client/logs/`**. The playtest world was saved and left before closing the client. The launcher pack's Voxy cache, shaders and editor mods were not used for rendering.

Returning to the menu logged X11 `Standard cursor shape unavailable`. After the integrated server had logged **all chunks/all dimensions saved**, the client JVM was deliberately stopped with SIGTERM; Gradle therefore reports `:fabric:runClient FAILED` / exit **143**. This was a successful launch/render/inspection, **not a passing Gradle build or GameTest gate**.

## Open questions and later sign-off

- Where is **Mondstadt** in Minecraft coordinates? Spawn is a useful starting point, not an identified Mondstadt landmark. Establish at least two known landmarks, local scale and orientation on the upgraded copy before placing content.
- The copied 107.1 WorldPainter height pack loaded under the official server and Fabric dev client without a datapack/registry fallback. The runtime boundary reads and nearby screenshots above cover its custom Y extent; full-world visual fidelity remains a separate sign-off.
- Are there non-vanilla block/block-entity IDs outside the deterministic terrain sample? Sampling cannot rule these out globally. Full entity-region coverage is separate from terrain coverage.
- Block IDs/counts changed only for the confirmed grass rename in the sampled comparison; states/properties and placed voxels were not compared. Identify actual landmarks before asserting visual fidelity to Genshin.
- Can unused editor metadata and the 1.30 GB Voxy cache be omitted from the eventual dev-world copy? This spike preserves them rather than silently deleting purchased-world data. The core game must work without the launcher instance's LOD/shader/mod profile.
- `02-spawn-valley-below-default-height.png` shows a large **dark vertical shadow** down the left cliff and onto the water that the blocks don't explain. Likely stale or recomputed sky light in the 4064-block-tall world after the upgrade (lead's reading, not verified). Resolved 2026-10-04: the owner reviewed the screenshot and judged the lighting fine; no relighting step.
