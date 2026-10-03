# Blocky Teyvat 5.1.0 → Minecraft 26.2 map-port spike

Investigated 2026-10-04. This is the spike for [todo item 10](../todo.md) and [hard problem 1](GENSHIN_MINECRAFT_BRIEF.md#1-porting-the-map-to-262), not a completed server upgrade or landmark fidelity sign-off. **The purchased ZIP was only read; no full-world extraction, Minecraft server, upgrade, Gradle task, commit, or network download was run.** A single terrain region was extracted to ignored storage and compared byte-for-byte with the ZIP member.

## Source, versions and size

- Pristine source: `reference/map/Blocky Teyvat 5.1.0-001.zip`; compressed file size **3,948,634,100 bytes**.
- `nice -n 19 ionice -c3 unzip -l` listed the launcher instance in **0.16 s**: **3,497 entries** including directories, **3,408 regular files**, **6,727,000,159 uncompressed bytes** in the entire package.
- The unique world is `Blocky Teyvat 5.1.0/saves/方块提瓦特2025 77/`. Python's ZIP reader preserves the actual Unicode name (the terminal's `unzip -l` rendered those characters as `?`).
- World only: **3,176 regular files**, **6,122,749,281 uncompressed bytes** (6.123 decimal GB / about 5.70 GiB). This includes empty region files, backups, saved editor/mod metadata, and the Voxy cache; do not confuse it with live terrain size.
- `level.dat` is gzip-compressed Java NBT. `Data.DataVersion = 4556`; `Data.Version = {Name: "1.21.10", Id: 4556, Series: "main", Snapshot: 0}`; `LevelName = 方块提瓦特`.
- The already cached official 26.2 client jar's `version.json` reports **`world_version = 4903`**, Java **25**, data pack version **107.1**, resource pack version **88.0**. Source: `$HOME/.gradle/caches/fabric-loom/26.2/minecraft-client.jar:version.json`. The source map therefore needs a **4556 → 4903** data-fixer upgrade; no target jar download was needed.
- Cached Mojang metadata (`$HOME/.gradle/caches/fabric-loom/26.2/mojang_minecraft_info.json`, `downloads.server`) identifies the official bundled server as **60,894,273 bytes**, SHA-1 **`823e2250d24b3ddac457a60c92a6a941943fcd6a`**, [Mojang server object](https://piston-data.mojang.com/v1/objects/823e2250d24b3ddac457a60c92a6a941943fcd6a/server.jar). The port driver pins this checksum and validates the bundled version metadata before use. The cached bundle's `META-INF/versions.list` identifies `26.2/server-26.2.jar` as its inner jar.
- `df -h .` reported **116 GiB available** on the repository filesystem (0.06 s). Space was sufficient, but the assignment deliberately limited extraction to one region. The driver requires the exact world bytes plus extraction/upgrade reserve; budget more than the bare 6.12 GB for logs, libraries and region rewriting.

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

The driver ports **only the disposable copy**: starts from that official 26.2 overworld definition, retains every original field still present in the target schema (including all three height values), emits pack metadata for **107.1**, and removes only the four unavailable editor-provided enabled pack entries. It preserves original `level.dat` and the WorldPainter ZIP outside `world/` within the work directory, records preparation in JSON, and rejects unexpected enabled packs, pack contents or custom height instead of silently dropping them. Modern vanilla ambience/timelines come from the target jar; their visual fidelity has not yet been checked in-game.

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

The spike does not change gamerules. The future official data fixer owns their format migration. Axiom rules are leftover editor keys; runtime managed-world rules remain the mod's responsibility. Even though fire tick is already disabled, TNT/projectile editing and vanilla player damage are still enabled in this saved source.

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

## Extraction exercised; upgrade deliberately deferred

The exact smoke extraction command was:

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/port-map.py extract \
  --work-dir run/map-port/extractor-smoke --only region/r.6.-8.mca
```

It extracted **one file, 5,976,064 bytes**, in **0.036 s** of measured extraction time (**0.19 s** command wall time). The extracted bytes were **identical** to the ZIP member; SHA-256 **`bd27b4b28986e8aa5c2f385815367d860f15a9f5b24ad0d374cf7d2238b986ba`**. No `level.dat` or full world was extracted into that smoke directory.

The installed `/run/current-system/sw/bin/java -version` reports **OpenJDK 25.0.4.1**; this was a runtime-version query, not a Minecraft launch. Wait until the lead's builds/GameTests stop, read the [Minecraft EULA](https://aka.ms/MinecraftEULA), and, if accepted, run this **full-world upgrade with sampled before/after comparison** from the repository root:

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/port-map.py upgrade \
  --server-jar "$HOME/.gradle/caches/fabric-loom/26.2/minecraft-server.jar" \
  --java /run/current-system/sw/bin/java \
  --before-report run/map-port/spike/before.json \
  --work-dir run/map-port/blocky-teyvat-26.2 \
  --accept-eula
```

The command extracts/reuses an untouched full copy, prepares the custom-height vanilla-compatible datapack, runs `-Xmx4G -XX:ActiveProcessorCount=1 -XX:+UseSerialGC ... --forceUpgrade --nogui` in the work directory, binds only to loopback, sends `stop` after server readiness, and compares the exact baseline region manifest. Outputs include `upgrade-server.log`, `preparation.json`, `before.json`, `after.json`, `diff.json`, and timings in `.map-port-extraction.json`. Both scripts and detailed options are documented in [scripts/map-port/README.md](../scripts/map-port/README.md).

**Upgrade time, post-upgrade IDs, target chunk versions and landmark screenshots are not measured in this spike.** No claim of successful 26.2 loading or preserved geometry is made. The launch/preparation/diff path is implemented but was not exercised; the later server log, ID diff and screenshots are necessary evidence.

## Open questions and later sign-off

- Where is **Mondstadt** in Minecraft coordinates? Spawn is a useful starting point, not an identified Mondstadt landmark. Establish at least two known landmarks, local scale and orientation on the upgraded copy before placing content.
- Does the copied 107.1 WorldPainter height pack load cleanly under official 26.2 and preserve its Y extent? Review registry/datapack errors and compare screenshots; avoid any fallback that disables it.
- Are there non-vanilla block/block-entity IDs outside the deterministic terrain sample? Sampling cannot rule these out globally. Full entity-region coverage is separate from terrain coverage.
- Do the official data fixer and startup ticks change IDs, properties, block entities or geometry? Palette-entry equality is weaker than voxel/state equality; missing/added IDs are only rename candidates. Inspect per-region diffs and visual landmarks.
- How long does the 4 GiB, one-active-processor official upgrade take, and do **all scanned chunk DataVersions** advance to 4903? A new `level.dat` alone is insufficient evidence of a successful complete chunk upgrade.
- Can unused editor metadata and the 1.30 GB Voxy cache be omitted from the eventual dev-world copy? This spike preserves them rather than silently deleting purchased-world data. The core game must work without the launcher instance's LOD/shader/mod profile.
