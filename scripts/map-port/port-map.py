#!/usr/bin/env python3
"""Extract a disposable map copy and upgrade it with the pinned official 26.2 jar."""

import argparse
import fcntl
import gzip
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import struct
import subprocess
import sys
import time
import zipfile

from palette_scan import NbtReader, WorldSource, compare, read_nbt, save_json, scan

REPO = Path(__file__).resolve().parents[2]
WORK_BASE = REPO / "run/map-port"
DEFAULT_ARCHIVE = REPO / "reference/map/Blocky Teyvat 5.1.0-001.zip"
DEFAULT_WORK = WORK_BASE / "blocky-teyvat-26.2"
VERSION = "26.2"
DATA_VERSION = 4903
# Cached Mojang version metadata, downloads.server.sha1; not a remapped Loom jar.
SERVER_SHA1 = "823e2250d24b3ddac457a60c92a6a941943fcd6a"
MARKER = ".map-port-extraction.json"

# Pinned to 26.2's GameRuleRegistryFix (including its inversions and combined fire
# rule). Preparation leaves vanilla legacy strings for Mojang's fixer; only the
# post-hoc repair uses this mapping to recover the original values.
RULE_RENAMES = {
    "allowEnteringNetherUsingPortals": "allow_entering_nether_using_portals",
    "announceAdvancements": "show_advancement_messages",
    "blockExplosionDropDecay": "block_explosion_drop_decay",
    "commandBlockOutput": "command_block_output",
    "commandBlocksEnabled": "command_blocks_work",
    "commandModificationBlockLimit": "max_block_modifications",
    "disableElytraMovementCheck": "elytra_movement_check",
    "disablePlayerMovementCheck": "player_movement_check",
    "disableRaids": "raids",
    "doDaylightCycle": "advance_time",
    "doEntityDrops": "entity_drops",
    "doImmediateRespawn": "immediate_respawn",
    "doInsomnia": "spawn_phantoms",
    "doLimitedCrafting": "limited_crafting",
    "doMobLoot": "mob_drops",
    "doMobSpawning": "spawn_mobs",
    "doPatrolSpawning": "spawn_patrols",
    "doTileDrops": "block_drops",
    "doTraderSpawning": "spawn_wandering_traders",
    "doVinesSpread": "spread_vines",
    "doWardenSpawning": "spawn_wardens",
    "doWeatherCycle": "advance_weather",
    "drowningDamage": "drowning_damage",
    "enderPearlsVanishOnDeath": "ender_pearls_vanish_on_death",
    "fallDamage": "fall_damage",
    "fireDamage": "fire_damage",
    "forgiveDeadPlayers": "forgive_dead_players",
    "freezeDamage": "freeze_damage",
    "globalSoundEvents": "global_sound_events",
    "keepInventory": "keep_inventory",
    "lavaSourceConversion": "lava_source_conversion",
    "locatorBar": "locator_bar",
    "logAdminCommands": "log_admin_commands",
    "maxCommandChainLength": "max_command_sequence_length",
    "maxCommandForkCount": "max_command_forks",
    "maxEntityCramming": "max_entity_cramming",
    "minecartMaxSpeed": "max_minecart_speed",
    "mobExplosionDropDecay": "mob_explosion_drop_decay",
    "mobGriefing": "mob_griefing",
    "naturalRegeneration": "natural_health_regeneration",
    "playersNetherPortalCreativeDelay": "players_nether_portal_creative_delay",
    "playersNetherPortalDefaultDelay": "players_nether_portal_default_delay",
    "playersSleepingPercentage": "players_sleeping_percentage",
    "projectilesCanBreakBlocks": "projectiles_can_break_blocks",
    "pvp": "pvp",
    "randomTickSpeed": "random_tick_speed",
    "reducedDebugInfo": "reduced_debug_info",
    "sendCommandFeedback": "send_command_feedback",
    "showDeathMessages": "show_death_messages",
    "snowAccumulationHeight": "max_snow_accumulation_height",
    "spawnMonsters": "spawn_monsters",
    "spawnRadius": "respawn_radius",
    "spawnerBlocksEnabled": "spawner_blocks_work",
    "spectatorsGenerateChunks": "spectators_generate_chunks",
    "tntExplodes": "tnt_explodes",
    "tntExplosionDropDecay": "tnt_explosion_drop_decay",
    "universalAnger": "universal_anger",
    "waterSourceConversion": "water_source_conversion",
}
INTEGER_RULES = {
    "commandModificationBlockLimit", "maxCommandChainLength", "maxCommandForkCount",
    "maxEntityCramming", "minecartMaxSpeed", "playersNetherPortalCreativeDelay",
    "playersNetherPortalDefaultDelay", "playersSleepingPercentage", "randomTickSpeed",
    "snowAccumulationHeight", "spawnRadius",
}
INVERTED_RULES = {"disableElytraMovementCheck", "disablePlayerMovementCheck", "disableRaids"}
LEGACY_VANILLA_RULES = set(RULE_RENAMES) | {"doFireTick", "allowFireTicksAwayFromPlayer"}


def migrated_rules(original):
    def boolean(name, default=None):
        value = original.get(name, default)
        if value not in ("true", "false"):
            raise ValueError(f"invalid original vanilla game rule {name}: {value!r}")
        return value == "true"

    result = {}
    for name, target in RULE_RENAMES.items():
        if name not in original:
            continue
        if name in INTEGER_RULES:
            value = int(original[name])
            minimum = 1 if name == "commandModificationBlockLimit" else 0
            maximum = 8 if name == "snowAccumulationHeight" else 2147483647
            value = max(minimum, min(maximum, value))
        else:
            value = boolean(name)
            if name in INVERTED_RULES:
                value = not value
        result["minecraft:" + target] = value
    if "doFireTick" in original or "allowFireTicksAwayFromPlayer" in original:
        result["minecraft:fire_spread_radius_around_player"] = (
            0 if not boolean("doFireTick", "true") else
            -1 if boolean("allowFireTicksAwayFromPlayer", "false") else 128)
    return result


def work_path(path):
    resolved = path.resolve()
    base = WORK_BASE.resolve()
    if not base.is_relative_to(REPO) or path.is_symlink() or resolved == base or not resolved.is_relative_to(base):
        raise ValueError("work directory must be strictly inside this repo's run/map-port/, not a symlink")
    return resolved


def extraction_marker(source, root, only=None):
    return {"schema": 1, "archive": str(source.path), "archive_bytes": source.path.stat().st_size,
            "world_prefix": source.prefix, "work_dir": str(root),
            "selection": only or "full", "state": "extracting"}


def extract(source, root, force=False, only=None):
    started = time.monotonic()
    expected = extraction_marker(source, root, only)
    names = [only] if only else sorted(source.entries)
    if only and (only not in source.entries or not re.fullmatch(r"region/r\.-?\d+\.-?\d+\.mca", only)):
        raise ValueError("--only must name one existing overworld region/r.X.Z.mca")
    # Validate every path before creating output; no ZipFile.extractall, symlinks,
    # traversal, or absolute member paths. The archive is always opened read-only.
    for name in names:
        relative = PurePosixPath(name)
        info = source.archive.getinfo(source.prefix + name)
        if relative.is_absolute() or ".." in relative.parts or "\\" in name or stat.S_ISLNK(info.external_attr >> 16):
            raise ValueError(f"unsafe archive member: {name}")
    root.parent.mkdir(parents=True, exist_ok=True)
    needed = sum(source.entries[name] for name in names)
    reserve = 64 * 1024**2 if only else 1024**3
    if shutil.disk_usage(root.parent).free < needed + reserve:
        raise ValueError(f"need at least {needed + reserve:,} free bytes for extraction (and extra space for upgrading)")
    if root.exists():
        if not force:
            raise ValueError(f"{root} exists; use --force to replace a script-owned extraction")
        marker_path = root / MARKER
        if not marker_path.is_file():
            raise ValueError("refusing --force without this script's extraction marker")
        old = json.loads(marker_path.read_text())
        if old.get("schema") != 1 or old.get("work_dir") != str(root) or old.get("archive") != str(source.path):
            raise ValueError("refusing --force: extraction marker does not match this work directory/archive")
        shutil.rmtree(root)
    root.mkdir()
    save_json(root / MARKER, expected)
    world = root / "world"
    world.mkdir()
    for name in names:
        destination = world / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        with source.archive.open(source.prefix + name, "r") as origin, destination.open("xb") as output:
            shutil.copyfileobj(origin, output, length=1024 * 1024)
    expected.update(state="extracted", extracted_bytes=needed, extracted_files=len(names),
                    extract_seconds=round(time.monotonic() - started, 3))
    save_json(root / MARKER, expected)
    return expected


def checked_server(path):
    digest = hashlib.sha1()
    with path.open("rb") as jar:
        for block in iter(lambda: jar.read(1024 * 1024), b""):
            digest.update(block)
    if digest.hexdigest() != SERVER_SHA1:
        raise ValueError(f"server SHA-1 is {digest.hexdigest()}, expected Mojang 26.2 {SERVER_SHA1}; supply the official bundled server jar")
    with zipfile.ZipFile(path, "r") as bundle:
        entries = [line.split("\t") for line in bundle.read("META-INF/versions.list").decode().splitlines()]
        versions = [entry for entry in entries if entry[1] == VERSION]
        if len(versions) != 1:
            raise ValueError("official server bundle has no unique 26.2 inner jar")
        with zipfile.ZipFile(io.BytesIO(bundle.read("META-INF/versions/" + versions[0][2])), "r") as inner:
            version = json.loads(inner.read("version.json"))
            overworld = json.loads(inner.read("data/minecraft/dimension_type/overworld.json"))
    if version["id"] != VERSION or version["world_version"] != DATA_VERSION or version["java_version"] != 25:
        raise ValueError("server version metadata does not match pinned 26.2/4903/Java 25")
    return version, overworld


def replace_payload(data, path, expected_tag, encoded):
    """Patch one typed NBT payload, leaving all unrelated bytes intact."""
    reader = NbtReader(data)
    if reader.number(">B") != 10:
        raise ValueError("level.dat is not compound NBT")
    reader.string()

    def find(parts):
        while True:
            tag = reader.number(">B")
            if tag == 0:
                raise ValueError(f"missing NBT path {'/'.join(path)}")
            name = reader.string()
            if name != parts[0]:
                reader.payload(tag)
                continue
            if len(parts) > 1:
                if tag != 10:
                    raise ValueError(f"NBT parent {name} is not a compound")
                return find(parts[1:])
            if tag != expected_tag:
                raise ValueError(f"NBT field {'/'.join(path)} has tag {tag}, expected {expected_tag}")
            start = reader.pos
            reader.payload(tag)
            return data[:start] + encoded + data[reader.pos:]

    return find(path)


def nbt_string(value):
    # Java DataInput uses modified UTF-8, including surrogate pairs.
    units = value.encode("utf-16-be", errors="surrogatepass")
    raw = b"".join(chr(struct.unpack_from(">H", units, offset)[0]).encode("utf-8", errors="surrogatepass")
                   for offset in range(0, len(units), 2)).replace(b"\x00", b"\xc0\x80")
    return struct.pack(">H", len(raw)) + raw


def rule_payload(rules):
    encoded = bytearray()
    for name, value in sorted(rules.items()):
        if isinstance(value, bool):
            tag, payload = 1, struct.pack(">b", value)
        elif isinstance(value, int):
            tag, payload = 3, struct.pack(">i", value)
        elif isinstance(value, str):
            tag, payload = 8, nbt_string(value)
        else:
            raise ValueError(f"unsupported game rule value for {name}: {value!r}")
        encoded.extend(bytes([tag]) + nbt_string(name) + payload)
    encoded.append(0)
    return encoded


def replace_string_list(data, path, values):
    encoded = struct.pack(">Bi", 8, len(values)) + b"".join(nbt_string(value) for value in values)
    return replace_payload(data, path, 9, encoded)


def repair_gamerules(root):
    marker = json.loads((root / MARKER).read_text())
    if marker.get("work_dir") != str(root) or marker.get("selection") != "full":
        raise ValueError("repair requires this script's marked full extraction")
    world = root / "world"
    if world.is_symlink() or any(item.is_symlink() for item in world.rglob("*")):
        raise ValueError("refusing to repair a copy containing symlinks")
    if read_nbt(gzip.decompress((world / "level.dat").read_bytes()))["Data"]["DataVersion"] != DATA_VERSION:
        raise ValueError("repair requires an already-upgraded 26.2 world")
    original = read_nbt(gzip.decompress((root / "original-level.dat").read_bytes()))["Data"]
    if original["DataVersion"] != 4556:
        raise ValueError("repair requires the preserved 1.21.10 original-level.dat")
    original_rules = original["GameRules"]
    after = migrated_rules(original_rules)
    path = world / "data/minecraft/game_rules.dat"
    backup = root / "pre-repair-game_rules.dat"
    # Minecraft's session lock is a POSIX record lock on Linux. Never edit a
    # live world's saved data; the repair is for cleanly stopped copies only.
    with (world / "session.lock").open("rb+") as lock:
        fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        raw = gzip.decompress(path.read_bytes())
        before = read_nbt(raw)["data"]
        if before == after:
            return {"status": "already_repaired", "game_rules": after, "backup": str(backup)}
        patched = replace_payload(raw, ["data"], 10, rule_payload(after))
        patched = replace_payload(patched, ["DataVersion"], 3, struct.pack(">i", DATA_VERSION))
        with backup.open("xb") as output:
            output.write(path.read_bytes())
        temporary = path.with_name(path.name + ".map-port-new")
        with temporary.open("xb") as output:
            output.write(gzip.compress(patched, mtime=0))
        os.replace(temporary, path)
    report = {"status": "repaired", "file": str(path), "backup": str(backup),
              "before": before, "after": after,
              "removed_unknown_source_rules": sorted(set(original_rules) - LEGACY_VANILLA_RULES)}
    save_json(root / "gamerule-repair.json", report)
    return report


def prepare_vanilla(world, root, version, default_dimension):
    source = WorldSource(world)
    try:
        level = source.level()
        enabled = level["DataPacks"]["Enabled"]
        original_rules = level["GameRules"]
    finally:
        source.close()
    # Only these unavailable, editor-provided packs are removed. Unknown enabled
    # packs are a hard error rather than silently losing map-specific behaviour.
    unavailable = {"fabric", "axiom", "fabric-convention-tags-v2", "ok_zoomer"}
    retained = [name for name in enabled if name not in unavailable]
    if set(retained) != {"vanilla", "file/worldpainter.zip"}:
        raise ValueError(f"unexpected enabled datapacks {retained}; inspect before porting")
    pack_path = world / "datapacks/worldpainter.zip"
    with zipfile.ZipFile(pack_path, "r") as pack:
        files = {name for name in pack.namelist() if not name.endswith("/")}
        if files != {"pack.mcmeta", "data/minecraft/dimension_type/overworld.json"}:
            raise ValueError(f"unexpected WorldPainter datapack files {sorted(files)}")
        original_dimension = json.loads(pack.read("data/minecraft/dimension_type/overworld.json"))
    # Use the target version's real schema (26.2 uses attributes/timelines, not
    # the old effects/bed_works fields), while preserving compatible map values.
    dimension = dict(default_dimension)
    dimension.update({key: value for key, value in original_dimension.items() if key in dimension})
    if (dimension["min_y"], dimension["height"], dimension["logical_height"]) != (-2032, 4064, 4064):
        raise ValueError("unexpected map height; this spike is pinned to Blocky Teyvat 5.1.0")
    pack_version = version["pack_version"]
    pack_format = [pack_version["data_major"], pack_version["data_minor"]]
    pack_meta = {"pack": {"description": "Blocky Teyvat WorldPainter height, ported to Minecraft 26.2 on a disposable copy",
                          "min_format": pack_format, "max_format": pack_format}}
    shutil.copyfile(pack_path, root / "original-worldpainter.zip")
    shutil.copyfile(world / "level.dat", root / "original-level.dat")
    with zipfile.ZipFile(pack_path, "w", compression=zipfile.ZIP_DEFLATED) as pack:
        pack.writestr("pack.mcmeta", json.dumps(pack_meta))
        pack.writestr("data/minecraft/dimension_type/overworld.json", json.dumps(dimension))
    original_level = gzip.decompress((world / "level.dat").read_bytes())
    patched = replace_string_list(original_level, ["Data", "DataPacks", "Enabled"], retained)
    vanilla_rules = {name: value for name, value in original_rules.items() if name in LEGACY_VANILLA_RULES}
    # Do not pre-rename: GameRuleRegistryFix correctly renames, types, inverts
    # and combines vanilla rules. Unknown leftovers invalidate the entire codec.
    patched = replace_payload(patched, ["Data", "GameRules"], 10, rule_payload(vanilla_rules))
    (world / "level.dat").write_bytes(gzip.compress(patched, mtime=0))
    return {"removed_unavailable_datapacks": [name for name in enabled if name in unavailable],
            "removed_unknown_gamerules": sorted(set(original_rules) - LEGACY_VANILLA_RULES),
            "retained_vanilla_gamerules": vanilla_rules,
            "retained_datapacks": retained, "dimension": dimension, "pack_metadata": pack_meta}


def run_server(java, server_jar, root):
    version_check = subprocess.run([java, "-version"], capture_output=True, text=True, check=True)
    if not re.search(r'version "25(?:[.\"]|$)', version_check.stderr + version_check.stdout):
        raise ValueError("official Minecraft 26.2 requires Java 25; use --java /path/to/java25")
    (root / "eula.txt").write_text("# Accepted explicitly with --accept-eula\neula=true\n")
    (root / "server.properties").write_text(
        "level-name=world\nserver-ip=127.0.0.1\nserver-port=25595\n"
        "online-mode=true\nmax-players=1\nview-distance=2\nsimulation-distance=2\n"
        "enable-query=false\nenable-rcon=false\nspawn-protection=0\n")
    command = ["nice", "-n", "19", "ionice", "-c3", java, "-Xmx4G", "-XX:ActiveProcessorCount=1",
               "-XX:+UseSerialGC", "-jar", str(server_jar), "--forceUpgrade", "--nogui"]
    started = time.monotonic()
    print("Running:", " ".join(command), flush=True)
    with (root / "upgrade-server.log").open("w") as log:
        process = subprocess.Popen(command, cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True, bufsize=1)
        ready = False
        try:
            for line in process.stdout:
                log.write(line)
                log.flush()
                print(line, end="", flush=True)
                if not ready and re.search(r'Done \([^)]*\)!', line):
                    ready = True
                    process.stdin.write("stop\n")
                    process.stdin.flush()
            code = process.wait()
        finally:
            if process.poll() is None:
                try:
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                    process.wait(timeout=60)
                except (BrokenPipeError, subprocess.TimeoutExpired):
                    process.terminate()
                    process.wait(timeout=60)
        if code != 0 or not ready:
            raise ValueError(f"server did not reach clean startup/shutdown (exit {code}); inspect {root / 'upgrade-server.log'}")
    return {"command": command, "server_start_upgrade_stop_seconds": round(time.monotonic() - started, 3)}


def upgrade(args, source, root):
    if not args.accept_eula:
        raise ValueError("read https://aka.ms/MinecraftEULA and pass --accept-eula only if you agree")
    server_jar = args.server_jar.resolve()
    version, overworld = checked_server(server_jar)
    # Reserve space for a rewritten region and server libraries; extraction has
    # its own exact uncompressed-size check. Do not run this alongside builds.
    if shutil.disk_usage(root.parent if root.parent.exists() else REPO).free < sum(source.entries.values()) + 2 * 1024**3:
        raise ValueError("insufficient free disk for a disposable extraction plus upgrade headroom")
    if root.exists() and not args.force:
        marker = json.loads((root / MARKER).read_text())
        expected = extraction_marker(source, root)
        if any(marker.get(key) != expected[key] for key in ("schema", "archive", "archive_bytes", "world_prefix", "work_dir", "selection")) or marker.get("state") != "extracted":
            raise ValueError("existing work directory is not a full untouched extraction; use a fresh path or --force")
    else:
        marker = extract(source, root, args.force)
    world = root / "world"
    if world.is_symlink() or any(item.is_symlink() for item in world.rglob("*")):
        raise ValueError("refusing to upgrade a copy containing symlinks")
    before_path = root / "before.json"
    if args.before_report:
        before = json.loads(args.before_report.read_text())
        if before["inventory"]["source"] != str(source.path) or before["inventory"]["world_prefix"] != source.prefix:
            raise ValueError("baseline report is not from this pristine archive/world")
        if before.get("schema") != 1 or not before["selected_regions"]:
            raise ValueError("baseline report is not a complete palette scan")
    else:
        before = scan(source, args.stride, args.spawn_radius)
    save_json(before_path, before)
    marker["state"] = "upgrading"
    save_json(root / MARKER, marker)
    preparation = prepare_vanilla(root / "world", root, version, overworld)
    save_json(root / "preparation.json", preparation)
    server_timing = run_server(args.java, server_jar, root)
    marker.update(state="upgraded-unverified", **server_timing)
    save_json(root / MARKER, marker)
    upgraded = WorldSource(root / "world")
    try:
        if upgraded.level()["DataVersion"] != DATA_VERSION:
            raise ValueError("upgraded level.dat DataVersion is not 4903")
        after = scan(upgraded, manifest=before_path)
    finally:
        upgraded.close()
    save_json(root / "after.json", after)
    changes = compare(before, after)
    save_json(root / "diff.json", changes)
    marker.update(state="upgraded", **server_timing,
                  before_scan_seconds=before["elapsed_seconds"], after_scan_seconds=after["elapsed_seconds"])
    save_json(root / MARKER, marker)
    print(json.dumps({"work_dir": str(root), "timings": marker, "ID_changes": changes["changes"]}, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    for command in ("extract", "upgrade"):
        sub = commands.add_parser(command)
        sub.add_argument("--archive", type=Path, default=DEFAULT_ARCHIVE)
        sub.add_argument("--work-dir", type=Path, default=DEFAULT_WORK)
        sub.add_argument("--force", action="store_true", help="replace only a marked extraction from this archive")
        if command == "extract":
            sub.add_argument("--only", help="extract one region/r.X.Z.mca (smoke test, never an upgradeable world)")
        else:
            sub.add_argument("--server-jar", type=Path, required=True, help="official bundled Mojang 26.2 jar; checksum is pinned")
            sub.add_argument("--java", default="java", help="Java 25 executable")
            sub.add_argument("--accept-eula", action="store_true")
            sub.add_argument("--before-report", type=Path, help="reuse a completed pristine archive scan")
            sub.add_argument("--stride", type=int, default=32, help="1 for full scan; default matches the spike sample")
            sub.add_argument("--spawn-radius", type=int, default=2048)
    repair = commands.add_parser("repair-gamerules", help="restore original vanilla rules in a stopped upgraded copy")
    repair.add_argument("--work-dir", type=Path, default=DEFAULT_WORK)
    args = parser.parse_args()
    if args.command == "upgrade" and (args.stride < 1 or args.spawn_radius < 0):
        parser.error("stride must be >= 1 and spawn radius >= 0")
    # All substantial work is single-threaded and low CPU/IO priority, including
    # direct script invocations that omitted the documented nice/ionice wrapper.
    os.nice(19)
    subprocess.run(["ionice", "-c3", "-p", str(os.getpid())], check=True)
    root = work_path(args.work_dir)
    if args.command == "repair-gamerules":
        print(json.dumps(repair_gamerules(root), ensure_ascii=False, indent=2))
        return
    source = WorldSource(args.archive)
    try:
        if not source.archive:
            raise ValueError("extraction source must be the read-only purchased ZIP")
        if args.command == "extract":
            print(json.dumps(extract(source, root, args.force, args.only), ensure_ascii=False, indent=2))
        else:
            upgrade(args, source, root)
    finally:
        source.close()


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, zipfile.BadZipFile, subprocess.SubprocessError, struct.error) as error:
        sys.exit(f"Map port failed: {error}")
