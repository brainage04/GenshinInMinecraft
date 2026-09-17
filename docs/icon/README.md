# GenshinInMinecraft icon

## What this is

`icon.png` is GenshinInMinecraft's project icon: a 1024x1024 PNG (2,628,251
bytes, RGBA), sha256
`da1ffab30092da890a8340bbe3ed86fadeb09b900bf66f544f1587aaa512b578`.

It shows the Dragonspine summit and the map's floating Skyfrost Nail above the
shader cloud layer, captured live inside the owner-supplied **Blocky Teyvat 5.1.0**
map with Complementary Reimagined enabled in-game.

## How it was made

**A real in-game capture**, not a Blender render and not generated pixel art. The
imagery is the owner's map geometry, lit and rendered by Iris + Sodium with the
Complementary Reimagined shader pack, photographed with Minecraft's own F2 key.

| | |
| --- | --- |
| Capture | Minecraft **1.21.10** client (offline-mode, single player) |
| Loader | Fabric Loader **0.18.4**, Fabric API `0.138.4+1.21.10` |
| Mods | Iris `1.9.7`, Sodium `0.7.3` (map-bundled jars, filenames and sha256 in `provenance/cleanup-report.json`); **Distant Horizons absent** |
| Shader pack | `ComplementaryReimagined_r5.7.1.zip` (sha256 `24a20634a7832d422d3cd5023be16829f26840e25c69404dd418306ea79f63f0`), profile **LOW** |
| Renderer settings | native render distance **32** chunks, `BORDER_FOG=true`, `fovEffectScale` 1.0 |
| Java / GL | OpenJDK 25.0.4.1, Mesa 26.1.8 **llvmpipe** software OpenGL (no GPU) |
| Display | private Xvfb `:211`, `1024x1024x24`; Minecraft window `1024x1024`; audio on an owned PipeWire null sink `round3_teyvat` |
| Screenshot | native Minecraft F2 at 1024x1024, F1 used to hide the HUD; **no crop, retouching, compositing or resampling** |

**World.** The owner's `Blocky Teyvat 5.1.0-001.zip` map archive
(3,948,634,100 bytes, sha256
`5acf999d8d35ca5f2c5b57e577c0398e58d253ce511a960b42cf12ee5921558b`) was read
read-only and extracted into an isolated runtime. The archive's world
(`Blocky Teyvat 5.1.0/saves/方块提瓦特2025 77/`) was installed as the single-player
world `Blocky Teyvat`; its `level.dat` carries `LevelName` 方块提瓦特,
`DataVersion` 4556 (1.21.10) and overworld seed **227290** in `WorldGenSettings`.
That seed does not determine the terrain: the map is hand-built (a WorldPainter
height datapack, `file/worldpainter.zip`, is enabled in `DataPacks`). The map's
bundled Fabric API / Iris / Sodium jars were installed from the archive's `mods/`
folder; no other bundled mod was installed, and the archive's only readme is
generic serverconfig text (kept as
`provenance/world/archive-serverconfig-readme.txt`). The map's own shipped
`options.txt` is kept as `provenance/world/archive-options.txt`.

**Camera** (first-person spectator, exactly as executed in game):

```
/tp @s 2272.722223 350.000000 -2533.959046 -55.000000 33.000000
```

x=2272.722223, y=350, z=-2533.959046, yaw **-55**, pitch **33**, nominal FOV
**60** (`fovEffectScale` 1.0; approximately 66 effective with flight FOV effects,
an empirical figure, not instrumented). The heading-plan entry this camera comes
from is entry 0 of `provenance/camera/shader-camera-plan.json`.

**Time and weather.** `/time set 4000`, `/weather clear`,
`/gamerule doDaylightCycle false`, `/gamerule doWeatherCycle false` — fixed
mid-morning light, clear sky, no cycle drift during the capture.

**The delivered frame is the native F2 file.** The screenshot landed in
`runtime/screenshots/2026-09-17_14.34.46.png`, was staged as
`06-shader-heading-minus55-fov60-1024.png`, and was finally renamed to the
canonical `06-shader-heading-minus55-fov60.png` with its sha256 unchanged. The
1024 recapture preserves the owner-selected 1280 composition: six spatially
separated landmarks match at offsets `0,0` with fitted scale 1.0
(`provenance/verification/selected-framing-check.json`).

## Provenance files

| File | What it is |
| --- | --- |
| `camera/06-shader-heading-minus55-fov60-metadata.json` | the delivered frame's record: camera, FOV, time, weather, world, shader pack, mod list with sha256, config snapshots, runtime metrics |
| `camera/shader-camera-plan.json` | the shader-camera study; **entry 0 is the delivered camera** (the other four headings are not this icon) |
| `harness/capture-launch.sh`, `harness/capture-client.args` | exactly how the 1024 client was launched (`--width 1024 --height 1024`, version 1.21.10, game dir, single-player world `Blocky Teyvat`) |
| `harness/capture-control.py` | the capture driver: teleport/HUD/F2 control and the `capture 0 ... shader-native 1024` invocation |
| `config/*-1024-options.txt`, `*-1024-iris.properties`, `*-1024-sodium-options.json`, `*-1024-ComplementaryReimagined_r5.7.1.zip.txt`, `config/sodium-mixins.properties` | the live client configuration snapshotted during the capture (render distance 32, FOV, shader pack selection and the pack's LOW settings) |
| `world/install-world.py`, `world/provision-client.py`, `world/reproduce.json` | how the map was installed from the owner's archive and how the client runtime was provisioned, with the commands used |
| `world/source-archive.json` | the map archive's identity (path, size, sha256) and the read-only handling rule |
| `world/archive-options.txt`, `world/archive-serverconfig-readme.txt`, `world/runtime-warnings.json` | the map's own shipped settings/readme, and the non-blocking load warnings (missing optional data packs, offline token, narrator, X11 cursor) |
| `verification/verify-selected.py`, `verify-frames.py`, `verify-delivery.py`, `build-manifest.py` | the checks that produced the verification records: framing match, image artefact scan, native chunk coverage, delivery |
| `verification/selected-framing-check.json` | six-landmark framing match against the 1280 reference (offsets 0,0; scale 1.0) |
| `verification/selected-native-coverage.json`, `...-initial-diagnostic.json` | native source-chunk coverage: 5041 FULL chunks, 1,290,496 heightmap columns, 0 missing; the 361 empty columns are proven to lie behind the camera (the earlier conservative diagnostic is preserved as-is) |
| `verification/image-artifact-check.json`, `artifact-review.json` | the 16x16 tile scan (0 flat-black tiles, one preserved upper-sky warning) and its visual adjudication |
| `verification/delivery-check.json` | the final delivery summary (1 manifest entry, native size, framing, coverage, warnings) |
| `verification/shader-proof.txt` | client log lines proving the shader pack loaded (profile LOW) on the private display/audio sink |
| `manifest.json`, `reproduce.json`, `blockers.json`, `cleanup-report.json`, `script-sha256.json` | the capture agent's delivery manifest, reproduction record, constraints/limitations, teardown report, and sha256 of every author script |
| `verification.json` | sha256 of `icon.png` and of every copied file, plus the capture summary — all copies are byte-identical to their round3 sources |

## How to regenerate

Working directory: the capture workspace
(`.../.local-icon-variants/round3/captures-teyvat/`), which no longer exists; the
scripts contain absolute paths to it, so recreate that layout or update the paths.

1. Install the world from the owner's archive (read-only source) and provision the
   client runtime:

   ```
   systemd-run --user --scope -p CPUWeight=20 -p AllowedCPUs=18,19 taskset -c 18,19 python3 tools/install-world.py
   systemd-run --user --scope -p CPUWeight=20 -p AllowedCPUs=18,19 taskset -c 18,19 python3 tools/provision-client.py
   ```

2. Start the private display and audio sink:

   ```
   systemd-run --user --scope --unit=round3-dragonspine-display -p CPUWeight=20 -p AllowedCPUs=18,19 \
     taskset -c 18,19 Xvfb :211 -screen 0 1024x1024x24 -nolisten tcp
   pactl load-module module-null-sink sink_name=round3_teyvat
   ```

3. Launch the client (Iris + Sodium, Complementary Reimagined LOW, native render
   distance 32):

   ```
   systemd-run --user --scope --unit=round3-dragonspine -p CPUWeight=20 -p AllowedCPUs=18,19 \
     taskset -c 18,19 sh renders-dragonspine/capture-launch.sh
   ```

4. In game, set the world state and the camera exactly as recorded:

   ```
   /gamemode spectator
   /time set 4000
   /weather clear
   /gamerule doDaylightCycle false
   /gamerule doWeatherCycle false
   /tp @s 2272.722223 350.000000 -2533.959046 -55.000000 33.000000
   ```

5. Hide the HUD (F1) and take the native screenshot via the capture driver:

   ```
   python3 renders-dragonspine/capture-control.py capture 0 06-shader-heading-minus55-fov60-1024 shader-native 1024
   ```

6. Verify the frame (Pillow is needed):

   ```
   export PYTHONPATH=/nix/store/4v9j9wbzyhrlx9980ygbr812313mazy0-python3.13-pillow-12.3.0/lib/python3.13/site-packages
   systemd-run --user --scope -p CPUWeight=20 -p AllowedCPUs=18,19 python3 renders-dragonspine/verify-selected.py 06-shader-heading-minus55-fov60-1024-metadata.json
   systemd-run --user --scope -p CPUWeight=20 -p AllowedCPUs=18,19 python3 renders-dragonspine/verify-frames.py 06-shader-heading-minus55-fov60-1024-metadata.json
   python3 renders-dragonspine/build-manifest.py
   python3 renders-dragonspine/verify-delivery.py
   ```

   The 1024 PNG is the icon as delivered (sha256
   `da1ffab30092da890a8340bbe3ed86fadeb09b900bf66f544f1587aaa512b578`); it needs
   no crop or post-processing step.

## Notes

- **The mod itself does not exist yet.** `docs/icon/` sits in the
  `GenshinInMinecraft/` workspace directory next to `GENSHIN_MINECRAFT_BRIEF.md`
  and the `wangg_mc/` map archive, because that directory is the project's home
  for this icon pass; the nested, still-empty `GenshinInMinecraft/` sub-folder is
  where the mod project is expected to be created. Move `docs/icon/` with the
  mod when it is created.
- This directory is not a git repository, so nothing was committed; the new files
  were simply left in place.
- FOV 60 is the **nominal slider value** with flight FOV effects enabled
  (`fovEffectScale` 1.0), approximately **66 effective**. That setting was kept
  deliberately, because turning effects off changes the owner-selected
  composition.
- This is intentionally a **summit/Nail detail** with a fog-limited background at
  native render distance 32 — it is not whole-mountain long-range Distant Horizons
  coverage, and DH was absent for this frame. The central vertical beacon in the
  image is legitimate map content.
- The image scan's `automaticPass` is `false` for exactly one reason: a 21-tile
  upper-sky region at `[528,160,784,192]` that the gradient threshold splits out.
  `verification/artifact-review.json` records the visual adjudication (ordinary
  upper sky/fog, not a missing-chunk hole); there are zero flat-black tiles.
- The capture runtime was restored after the shoot (the four original settings
  files, the three mods and the debug overlay), and the world was saved before
  quitting; see `provenance/cleanup-report.json`.
- **Two inputs are too large to ship here and are described instead of copied:**
  the owner's 3.7 GB map archive `Blocky Teyvat 5.1.0-001.zip` (path and sha256 in
  `provenance/world/source-archive.json`; `install-world.py` extracts the world
  from it) and the ~6 GB Minecraft runtime + extracted world that
  `provision-client.py` rebuilds from Mojang/Fabric downloads. Everything else
  needed to reproduce this frame is in `provenance/`.
- Deliberately **not** copied: the 5.9 GB Minecraft runtime, `libraries/` and
  `assets/`, the LOD caches (`DistantHorizons.sqlite*`), the extracted 6 GB
  Blocky Teyvat world, the Distant Horizons mod jars and DH evidence, the other
  headings' frames and their metadata (only entry 0 of the camera plan is this
  icon), the DH camera plan and the low-profile/DH planning and verification
  scripts, the 1280 reference PNG and other evidence screenshots (the contract
  keeps evidence screenshots out; the reference's sha256 is recorded in the
  framing check), the retired alternatives' record
  (`retirement-check.json`, whose candidates live under `round3/removed/`), and
  the runtime logs.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
