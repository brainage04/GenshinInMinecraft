#!/usr/bin/env python3
"""Extract a disposable map copy and upgrade it with the pinned official 26.2 jar."""

import argparse
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

from palette_scan import NbtReader, WorldSource, compare, save_json, scan

REPO = Path(__file__).resolve().parents[2]
WORK_BASE = REPO / "run/map-port"
DEFAULT_ARCHIVE = REPO / "reference/map/Blocky Teyvat 5.1.0-001.zip"
DEFAULT_WORK = WORK_BASE / "blocky-teyvat-26.2"
VERSION = "26.2"
DATA_VERSION = 4903
# Cached Mojang version metadata, downloads.server.sha1; not a remapped Loom jar.
SERVER_SHA1 = "823e2250d24b3ddac457a60c92a6a941943fcd6a"
MARKER = ".map-port-extraction.json"


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


def replace_string_list(data, path, values):
    """Patch just one TAG_List payload, leaving every other original NBT byte intact."""
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
            if tag != 9:
                raise ValueError("datapack Enabled field is not TAG_List")
            start = reader.pos
            old = reader.payload(tag)
            if not all(isinstance(value, str) for value in old):
                raise ValueError("datapack Enabled list is not strings")
            encoded = bytearray(struct.pack(">Bi", 8, len(values)))
            for value in values:
                raw = value.encode("utf-8")
                encoded.extend(struct.pack(">H", len(raw)))
                encoded.extend(raw)
            return data[:start] + encoded + data[reader.pos:]

    return find(path)


def prepare_vanilla(world, root, version, default_dimension):
    source = WorldSource(world)
    try:
        enabled = source.level()["DataPacks"]["Enabled"]
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
    (world / "level.dat").write_bytes(gzip.compress(patched, mtime=0))
    return {"removed_unavailable_datapacks": [name for name in enabled if name in unavailable],
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
    args = parser.parse_args()
    if args.command == "upgrade" and (args.stride < 1 or args.spawn_radius < 0):
        parser.error("stride must be >= 1 and spawn radius >= 0")
    # All substantial work is single-threaded and low CPU/IO priority, including
    # direct script invocations that omitted the documented nice/ionice wrapper.
    os.nice(19)
    subprocess.run(["ionice", "-c3", "-p", str(os.getpid())], check=True)
    root = work_path(args.work_dir)
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
