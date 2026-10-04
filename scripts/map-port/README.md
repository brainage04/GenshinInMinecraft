# Map-port utilities

These Python 3 standard-library tools operate on the purchased, **read-only** ZIP or a disposable copy. The utilities need no Gradle, loader mods or third-party NBT library, perform no server download and make no HoYoverse connection. The optional visual check uses the project's Fabric dev-client Gradle task. Findings and limitations are in [docs/map-port.md](../../docs/map-port.md).

## Inventory and reusable scan

Run from the repository root:

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/palette_scan.py inventory \
  'reference/map/Blocky Teyvat 5.1.0-001.zip' \
  --output run/map-port/spike/inventory.json

nice -n 19 ionice -c3 python3 scripts/map-port/palette_scan.py scan \
  'reference/map/Blocky Teyvat 5.1.0-001.zip' \
  --stride 32 --spawn-radius 2048 \
  --output run/map-port/spike/before.json
```

`scan` accepts either the instance ZIP (discovers the unique world under `saves/`) or a directory containing `level.dat`. It reads one region into memory at a time and parses chunks individually. It counts **section-palette entries by block ID**, not placed block voxels: two palette states with the same `Name` contribute two entries. Entity counts are instances, including passengers; block entities are instances in the inspected terrain chunks. Reports retain counts per region and chunk DataVersion distributions, not just global totals.

`--stride 1` scans all terrain and entity regions. The default scanner is full; the **port driver** defaults to the spike's sampled stride 32. Sampling orders terrain paths lexicographically, selects zero-based indices divisible by the stride, and additionally includes every overworld region whose 512×512 block footprint intersects the square `spawn x/z ± spawn-radius`. **All entity region files are scanned even when terrain is sampled.** The JSON `selected_regions` list is the exact logical manifest; a later `--manifest BEFORE.json` scan reads exactly those same regions, independent of changed spawn coordinates or new regions.

Minecraft 26.2 moves vanilla dimension files under `dimensions/minecraft/{overworld,the_nether,the_end}/`. The scanner normalizes region/POI/external-chunk paths to stable legacy identities (`region/...`, `DIM-1/...`, `DIM1/...`) and records logical-to-actual filenames in `relocated_regions`. Simultaneous old/new files for one logical path are ambiguous and fail. Custom dimension paths are unchanged. Inventory metadata reads 26.2's namespaced game-rule and world-border saved data as well as the source format.

A missing manifest region is accepted **only when the complete before-report proves zero chunk records, zero sections and empty ID/version counters**. It stays in the logical manifest with zero counts and is explicitly reported in `removed_empty_regions` in the after-report and diff. Missing populated regions, or missing regions without complete baseline evidence, remain fatal; the diff also fails if a still-present region loses chunk records. This is not a blanket ignore for absent entity files: even an entity chunk containing no instances is populated.

Region headers are inspected across the entire world to distinguish empty files from regions with chunk locations. POI regions and backup `.mca.*.backup` files contribute to inventory sizes but are not palette/entity scans. Gzip, zlib, uncompressed chunks and external `.mcc` chunks are supported. Unsupported compression (including optional LZ4), malformed NBT, unproven/nonempty missing comparison regions, or legacy numeric block IDs fail explicitly without writing a complete report. Never treat a partial/failed scan as evidence that IDs are absent.

## Extract without upgrading

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/port-map.py extract
```

The full world is copied to `run/map-port/blocky-teyvat-26.2/world`, including the map's own datapack, but **not** the enclosing launcher instance's mods/resource packs/shaders. It checks the exact uncompressed size plus 1 GiB of extraction reserve. Allow additional upgrade headroom. A work directory must be strictly below this repository's ignored `run/map-port/`. Existing output is refused unless `--force`; replacement requires the script's matching extraction marker, so it will not recursively delete an arbitrary directory. Partial or single-region extractions cannot be upgraded as full worlds.

The original spike exercised this smoke extraction; the subsequent completed full-world extraction/upgrade and its timings are recorded in [docs/map-port.md](../../docs/map-port.md):

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/port-map.py extract \
  --work-dir run/map-port/extractor-smoke --only region/r.6.-8.mca
```

Choose a fresh `--work-dir` to repeat that command, or explicitly use `--force` to replace this script-owned smoke extraction. The source ZIP is opened only with `ZipFile(..., "r")`; member names are validated before copying and ZIP symlinks/traversal are rejected.

## Full official 26.2 upgrade — with builds stopped

Read and agree to the [Minecraft EULA](https://aka.ms/MinecraftEULA) before passing `--accept-eula`. Use Java 25 and the **official bundled server jar**, not Loom's extracted, remapped or patched jars. The locally cached bundled jar is already available; no download is needed. Its pinned Mojang SHA-1 is `823e2250d24b3ddac457a60c92a6a941943fcd6a`. The driver streams the checksum and verifies the inner jar's version metadata before extracting/upgrading.

Exact command for this checkout, reusing the completed pristine spike scan:

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/port-map.py upgrade \
  --server-jar "$HOME/.gradle/caches/fabric-loom/26.2/minecraft-server.jar" \
  --java /run/current-system/sw/bin/java \
  --before-report run/map-port/spike/before.json \
  --work-dir run/map-port/blocky-teyvat-26.2 \
  --accept-eula
```

If that ignored baseline is absent, omit `--before-report` to scan the pristine archive again with the same deterministic sample. Omit both `--before-report` and add `--stride 1` for full before/after scans instead. An untouched full extraction produced by `extract` is reused; an interrupted/already-upgraded copy is refused unless replaced with `--force`. Keep the original baseline JSON and all generated artifacts inside ignored storage.

The driver:

1. Extracts only the world and records bytes, file count and duration in `.map-port-extraction.json`.
2. Records/reuses the original scan as `before.json`.
3. **Preserves the WorldPainter custom height** (`min_y=-2032`, `height=logical_height=4064`). It replaces only the extracted copy's WorldPainter pack with the checked 26.2 jar's real overworld dimension schema plus compatible original dimension values and 26.2 pack metadata. It removes only the unavailable `fabric`, `axiom`, `fabric-convention-tags-v2`, and `ok_zoomer` entries from the copied `level.dat`'s enabled packs. Unknown enabled packs or changed map height abort. **Unknown mod gamerules are removed from the copied legacy `Data.GameRules`; all vanilla names/string values are preserved for Mojang's own data fixer**, which handles 26.2 renames, booleans/integers, inversions and the combined fire-spread rule. Originals are kept as `original-level.dat` and `original-worldpainter.zip` under the work directory; `preparation.json` records removed packs/rules and retained values. **Never use `--safeMode`: it would discard the height override.**
4. Runs this command **with the work directory as its current directory**:

   ```sh
   nice -n 19 ionice -c3 /run/current-system/sw/bin/java \
     -Xmx4G -XX:ActiveProcessorCount=1 -XX:+UseSerialGC \
     -jar "$HOME/.gradle/caches/fabric-loom/26.2/minecraft-server.jar" \
     --forceUpgrade --nogui
   ```

   The generated `server.properties` uses `level-name=world`, binds to loopback on port 25595, disables query/RCON, and uses view/simulation distance 2. After the server reports `Done (...)!`, the driver sends `stop` through stdin and waits for clean shutdown. The entire console is retained in `upgrade-server.log`; it does not leave a daemon running.
5. Immediately records clean server startup/upgrade/shutdown timing with state `upgraded-unverified`, then requires `level.dat` DataVersion 4903, scans exactly the logical baseline manifest into `after.json`, and writes per-region/global differences into `diff.json`. Success sets state `upgraded` and records scan durations. Missing/added IDs are **rename candidates**, not proof of renames or preserved geometry. New startup-generated regions outside the baseline are inventoried but not compared. If comparison fails after the server has finished, use the scan-only commands below; **do not repeat the force-upgrade**.

The upgrade JVM deliberately uses the requested 4 GiB heap; **do not run it alongside Gradle/GameTests on the resource-capped machine**. The investigation/scan/extraction commands are single-threaded, nice 19, idle IO and far below the 2 GiB investigation memory ceiling. The port driver also lowers its own priority for direct invocations.

## Compare independently

After a cleanly stopped upgrade of a copy, including recovery from a comparison-only driver failure (**the completed 6942-second upgrade does not need rerunning**):

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/palette_scan.py scan \
  run/map-port/blocky-teyvat-26.2/world \
  --manifest run/map-port/blocky-teyvat-26.2/before.json \
  --output run/map-port/blocky-teyvat-26.2/after.json

nice -n 19 ionice -c3 python3 scripts/map-port/palette_scan.py diff \
  run/map-port/blocky-teyvat-26.2/before.json \
  run/map-port/blocky-teyvat-26.2/after.json \
  --output run/map-port/blocky-teyvat-26.2/diff.json
```

Review changed/missing IDs, actual chunk DataVersions and the complete server log, then take nearby/landmark screenshots on a separate playtest copy in a private headless client. Count equality alone is not a visual fidelity check. Never commit the world, third-party pack, archive or generated reports.

## Repair gamerules on an already-upgraded copy

For a copy upgraded before unknown-rule filtering was added:

```sh
nice -n 19 ionice -c3 python3 scripts/map-port/port-map.py repair-gamerules \
  --work-dir run/map-port/blocky-teyvat-26.2
```

Stop every server/client using that world first. The command requires a script-marked full extraction, preserved **1.21.10 `original-level.dat`**, target DataVersion **4903**, no symlinks and an unlocked session file. It recovers **all original vanilla values**, applies the pinned 26.2 rule migration and writes typed NBT to `world/data/minecraft/game_rules.dat`. It keeps an exact, never-overwritten **`pre-repair-game_rules.dat`** backup outside the world, writes **`gamerule-repair.json`** (before/after/removed source keys), and is idempotent.

The map's four Axiom camelCase rules are invalid 26.2 resource locations. The initial server logged a saved-data parse error, but direct queries showed valid partial vanilla values still applied (including **random tick speed 0**). Do not infer default fallback from the error alone; inspect saved data and query the stopped-copy test server. Removing those editor keys avoids the error and makes preservation explicit. The two legacy fire-tick rules combine into **`minecraft:fire_spread_radius_around_player=0`**; there is no 26.2 `doFireTick` boolean.

## Private visual playtest

Never load the upgrade evidence copy directly in the dev client. Copy it first (choose a fresh destination; `cp` must not nest a world into an existing directory):

```sh
nice -n 19 ionice -c3 cp -a --reflink=auto \
  run/map-port/blocky-teyvat-26.2/world run/map-port/playtest-world
```

Launch only on **a private Xvfb display**, with `ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1` and client heap **at most 2 GiB**. The completed check used an ignored Gradle init script to set `:fabric:runClient.maxHeapSize='2G'`, `workingDir=run/map-port/playtest-client`, and arguments `--gameDir <absolute playtest-client path> --quickPlaySingleplayer <absolute playtest-world path> --offlineDeveloperMode --width 1280 --height 720`:

```sh
nice -n 19 ionice -c3 env ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1 LP_NUM_THREADS=2 \
  xvfb-run -a --server-args="-screen 0 1280x720x24 -nolisten tcp" \
  ./gradlew --no-daemon --no-configuration-cache \
  --init-script run/map-port/client.init.gradle :fabric:runClient
```

The custom-height world shows an experimental-settings confirmation; accept it **only on the independent playtest copy**. Use ordinary spectator/teleport controls to inspect spawn **(3458, 63, −4002)** and nearby terrain, with an elevated overview. **Do not enable managed mode or use `/genshin arena` on this map.** Minecraft's F2 screenshots work on the private display; keep selected evidence under `run/map-port/screenshots/`. Software rendering has finite view distance: visible fog/chunk boundaries alone are not evidence of missing purchased geometry.

The completed captures and their camera coordinates are listed in [docs/map-port.md](../../docs/map-port.md). The saved spawn is far above its local sandy terrain; look downward or descend in spectator mode before interpreting sky/fog as void. Read-only `/execute if block 3458 -2032 -4002 minecraft:air ...` and the corresponding Y **2031** query exercised the loaded custom-height endpoints. Save and quit the playtest world before stopping the client.

## Regression checks

The standard-library synthetic-world cases cover dimension relocation/ambiguity, safe empty-region removal, populated/unproven loss rejection (including a present file with lost chunk records), preparation filtering, rule migration/inversions, repair backup/idempotence and byte-preserving NBT patches. They do not read or modify the purchased ZIP:

```sh
nice -n 19 ionice -c3 python3 -m unittest discover \
  -s scripts/map-port -p 'test_*.py' -v
```
