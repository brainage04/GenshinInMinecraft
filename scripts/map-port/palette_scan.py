#!/usr/bin/env python3
"""Read-only world inventory and Anvil palette scan; Python standard library only."""

import argparse
from collections import Counter, defaultdict
import gzip
import json
from pathlib import Path, PurePosixPath
import re
import struct
import sys
import time
import resource
import zipfile
import zlib

REGION = re.compile(r"^(?:(.+)/)?(region|entities|poi)/r\.(-?\d+)\.(-?\d+)\.mca$")


class NbtReader:
    """Big-endian Java NBT, retaining numeric arrays as views instead of Python ints."""

    def __init__(self, data):
        self.data = memoryview(data)
        self.pos = 0

    def take(self, count):
        if count < 0 or self.pos + count > len(self.data):
            raise ValueError("truncated or invalid NBT length")
        result = self.data[self.pos:self.pos + count]
        self.pos += count
        return result

    def number(self, fmt):
        return struct.unpack(fmt, self.take(struct.calcsize(fmt)))[0]

    def string(self):
        # Java's modified UTF-8 represents NUL as C0 80 and supplementary code
        # points as surrogate pairs. Preserve names from Java DataInput exactly.
        raw = bytes(self.take(self.number(">H"))).replace(b"\xc0\x80", b"\x00")
        text = raw.decode("utf-8", errors="surrogatepass")
        return text.encode("utf-16", errors="surrogatepass").decode("utf-16")

    def payload(self, tag):
        formats = {1: ">b", 2: ">h", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}
        if tag in formats:
            return self.number(formats[tag])
        if tag in (7, 11, 12):
            count = self.number(">i")
            if count < 0:
                raise ValueError("negative NBT array length")
            return self.take(count * {7: 1, 11: 4, 12: 8}[tag])
        if tag == 8:
            return self.string()
        if tag == 9:
            child, count = self.number(">B"), self.number(">i")
            if count < 0 or (child == 0 and count):
                raise ValueError("invalid NBT list")
            return [self.payload(child) for _ in range(count)]
        if tag == 10:
            result = {}
            while True:
                child = self.number(">B")
                if child == 0:
                    return result
                name = self.string()
                result[name] = self.payload(child)
        raise ValueError(f"unknown NBT tag {tag}")

    def root(self):
        tag = self.number(">B")
        if tag != 10:
            raise ValueError("NBT root is not a compound")
        self.string()
        return self.payload(tag)


def read_nbt(data):
    return NbtReader(data).root()


def dimension_for(relative):
    parts = PurePosixPath(relative).parts
    if parts[0] == "DIM-1":
        return "minecraft:the_nether"
    if parts[0] == "DIM1":
        return "minecraft:the_end"
    if parts[0] == "dimensions" and len(parts) >= 4:
        return parts[1] + ":" + "/".join(parts[2:-2] if parts[-2] in ("region", "entities", "poi", "data") else parts[2:-1])
    return "minecraft:overworld"


class WorldSource:
    def __init__(self, path):
        self.path = Path(path).resolve()
        self.archive = None
        if self.path.is_file():
            self.archive = zipfile.ZipFile(self.path, "r")
            roots = [name[:-len("level.dat")] for name in self.archive.namelist()
                     if re.search(r"(?:^|/)saves/[^/]+/level\.dat$", name)]
            if len(roots) != 1:
                raise ValueError(f"expected one saves/ world, found {roots}")
            self.prefix = roots[0]
            self.entries = {item.filename[len(self.prefix):]: item.file_size
                            for item in self.archive.infolist()
                            if item.filename.startswith(self.prefix) and not item.is_dir()}
        else:
            if not (self.path / "level.dat").is_file():
                raise ValueError("world directory must contain level.dat")
            self.prefix = ""
            self.entries = {item.relative_to(self.path).as_posix(): item.stat().st_size
                            for item in self.path.rglob("*") if item.is_file()}

    def close(self):
        if self.archive:
            self.archive.close()

    def read(self, relative):
        if self.archive:
            return self.archive.read(self.prefix + relative)
        return (self.path / relative).read_bytes()

    def header(self, relative):
        if self.archive:
            with self.archive.open(self.prefix + relative, "r") as entry:
                return entry.read(4096)
        with (self.path / relative).open("rb") as entry:
            return entry.read(4096)

    def level(self):
        data = read_nbt(gzip.decompress(self.read("level.dat")))["Data"]
        spawn = {axis: data.get("Spawn" + axis.upper()) for axis in ("x", "y", "z")}
        # Newer formats may store spawn as a compound rather than legacy keys.
        if all(value is None for value in spawn.values()):
            saved = data.get("spawn", {})
            spawn = {key: value for key, value in saved.items() if key != "pos"}
            if "pos" in saved:
                spawn.update(zip(("x", "y", "z"), struct.unpack(">iii", saved["pos"])))
        border = {key: value for key, value in data.items() if key.startswith("Border")}
        if "data/world_border.dat" in self.entries:
            border = read_nbt(gzip.decompress(self.read("data/world_border.dat")))["data"]
        return {
            "DataVersion": data.get("DataVersion"), "LevelName": data.get("LevelName"),
            "Version": data.get("Version"), "spawn": spawn,
            "SpawnAngle": data.get("SpawnAngle"),
            "world_border": border,
            "GameRules": data.get("GameRules", data.get("game_rules")),
            "DataPacks": data.get("DataPacks"),
            "enabled_features": data.get("enabled_features"),
            "removed_features": data.get("removed_features"),
        }

    def inventory(self):
        dimensions = defaultdict(lambda: {"files": 0, "uncompressed_bytes": 0,
                                           "region_files": 0, "region_bytes": 0,
                                           "entity_region_files": 0, "entity_region_bytes": 0,
                                           "poi_region_files": 0, "poi_region_bytes": 0})
        coords = defaultdict(list)
        nonempty = defaultdict(list)
        populated = defaultdict(list)
        for name, size in self.entries.items():
            dimension = dimension_for(name)
            item = dimensions[dimension]
            item["files"] += 1
            item["uncompressed_bytes"] += size
            match = REGION.fullmatch(name)
            if match:
                kind = match[2]
                key = {"region": "region", "entities": "entity_region", "poi": "poi_region"}[kind]
                item[key + "_files"] += 1
                item[key + "_bytes"] += size
                if kind == "region":
                    coord = [int(match[3]), int(match[4])]
                    coords[dimension].append(coord)
                    if size:
                        nonempty[dimension].append(coord)
                        header = self.header(name)
                        if len(header) != 4096:
                            raise ValueError(f"truncated region header: {name}")
                        if any(header):
                            populated[dimension].append(coord)
        for dimension, item in dimensions.items():
            item["region_file_bounds"] = bounds(coords[dimension])
            item["nonzero_region_file_bounds"] = bounds(nonempty[dimension])
            item["populated_region_files"] = len(populated[dimension])
            item["populated_region_bounds"] = bounds(populated[dimension])
        return {"source": str(self.path), "world_prefix": self.prefix,
                "file_count": len(self.entries), "uncompressed_bytes": sum(self.entries.values()),
                "dimensions": dict(sorted(dimensions.items())), "level": self.level(),
                "voxy_bytes": sum(size for name, size in self.entries.items() if name.startswith("voxy/")),
                "region_backup_bytes": sum(size for name, size in self.entries.items()
                                           if "/r." in name and name.endswith(".backup"))}


def bounds(coords):
    if not coords:
        return None
    return {"min_x": min(x for x, _ in coords), "max_x": max(x for x, _ in coords),
            "min_z": min(z for _, z in coords), "max_z": max(z for _, z in coords)}


def iter_chunks(source, relative, region_data):
    if not region_data:
        return
    if len(region_data) < 8192:
        raise ValueError("nonempty region shorter than its header")
    match = REGION.fullmatch(relative)
    rx, rz = int(match[3]), int(match[4])
    for slot in range(1024):
        location = struct.unpack_from(">I", region_data, slot * 4)[0]
        if not location:
            continue
        offset, sectors = (location >> 8) * 4096, location & 255
        if offset < 8192 or sectors == 0 or offset + 5 > len(region_data):
            raise ValueError(f"invalid chunk location in slot {slot}")
        length = struct.unpack_from(">I", region_data, offset)[0]
        compression = region_data[offset + 4]
        if length < 1 or length > sectors * 4096 - 4 or offset + 4 + length > len(region_data):
            raise ValueError(f"invalid chunk length in slot {slot}")
        if compression & 128:
            external = str(PurePosixPath(relative).parent / f"c.{rx * 32 + slot % 32}.{rz * 32 + slot // 32}.mcc")
            payload = source.read(external)
        else:
            payload = region_data[offset + 5:offset + 4 + length]
        method = compression & 127
        if method == 1:
            payload = gzip.decompress(payload)
        elif method == 2:
            payload = zlib.decompress(payload)
        elif method != 3:
            raise ValueError(f"unsupported Anvil compression {method}; scan is incomplete (LZ4 is not supported)")
        yield slot, read_nbt(payload)


def count_entities(entities, counter):
    for entity in entities:
        entity_id = entity.get("id")
        if entity_id:
            counter[entity_id] += 1
        count_entities(entity.get("Passengers", []), counter)


def scan_region(source, relative):
    result = {"chunks": 0, "sections": 0, "blocks": Counter(),
              "block_entities": Counter(), "entities": Counter(), "data_versions": Counter()}
    for _, root in iter_chunks(source, relative, source.read(relative)):
        result["chunks"] += 1
        result["data_versions"][str(root.get("DataVersion", "absent"))] += 1
        chunk = root.get("Level", root)
        for section in chunk.get("sections", chunk.get("Sections", [])):
            states = section.get("block_states", {})
            palette = states.get("palette", section.get("Palette", []))
            if palette:
                result["sections"] += 1
            elif "Blocks" in section:
                raise ValueError("legacy numeric block IDs need an upgrade before a palette scan")
            for state in palette:
                result["blocks"][state["Name"]] += 1
        for entity in chunk.get("block_entities", chunk.get("TileEntities", [])):
            if "id" in entity:
                result["block_entities"][entity["id"]] += 1
        count_entities(chunk.get("Entities", chunk.get("entities", [])), result["entities"])
    return result


def selected_regions(source, stride, radius, manifest=None, only=None):
    block_regions = sorted(name for name in source.entries if REGION.fullmatch(name) and REGION.fullmatch(name)[2] == "region")
    entity_regions = sorted(name for name in source.entries if REGION.fullmatch(name) and REGION.fullmatch(name)[2] == "entities")
    if manifest:
        names = json.loads(Path(manifest).read_text())["selected_regions"]
        missing = [name for name in names if name not in source.entries]
        if missing:
            raise ValueError(f"comparison regions missing: {missing}")
        return names
    if only:
        if only not in block_regions + entity_regions:
            raise ValueError(f"not a region in this world: {only}")
        return [only]
    if stride == 1:
        return sorted(block_regions + entity_regions)
    spawn = source.level()["spawn"]
    sx, sz = spawn.get("x"), spawn.get("z")
    if radius and (sx is None or sz is None):
        raise ValueError("spawn coordinates unavailable for sample selection")
    chosen = []
    for index, name in enumerate(block_regions):
        match = REGION.fullmatch(name)
        x, z = int(match[3]) * 512, int(match[4]) * 512
        near_spawn = (not match[1] and radius > 0 and
                      x <= sx + radius and x + 511 >= sx - radius and
                      z <= sz + radius and z + 511 >= sz - radius)
        if index % stride == 0 or near_spawn:
            chosen.append(name)
    # Entities are relatively small: inspect all of them even for a block sample.
    return sorted(chosen + entity_regions)


def scan(source, stride=1, radius=2048, manifest=None, only=None):
    started = time.monotonic()
    names = selected_regions(source, stride, radius, manifest, only)
    totals = {"blocks": Counter(), "block_entities": Counter(), "entities": Counter(), "data_versions": Counter()}
    regions, populated = {}, defaultdict(list)
    chunks, sections = 0, 0
    for index, name in enumerate(names):
        result = scan_region(source, name)
        for key in totals:
            totals[key].update(result[key])
        chunks += result["chunks"]
        sections += result["sections"]
        regions[name] = result
        match = REGION.fullmatch(name)
        if match[2] == "region" and result["chunks"]:
            populated[dimension_for(name)].append([int(match[3]), int(match[4])])
        if (index + 1) % 25 == 0:
            print(f"Scanned {index + 1}/{len(names)} regions in {time.monotonic() - started:.1f}s", file=sys.stderr, flush=True)
    return {"schema": 1, "inventory": source.inventory(),
            "count_semantics": "blocks count section-palette entries by Name, not placed voxels; entities count instances including passengers",
            "selection": {"stride": stride, "spawn_radius_blocks": radius,
                          "manifest": str(manifest) if manifest else None, "only": only,
                          "ordering": "lexicographic relative paths, zero-based index divisible by stride; include overworld regions intersecting spawn +/- radius square; all entity regions"},
            "selected_regions": names, "regions": regions, "totals": totals,
            "non_vanilla": {key: {name: count for name, count in totals[key].items() if not name.startswith("minecraft:")}
                            for key in ("blocks", "block_entities", "entities")},
            "chunks_scanned": chunks, "sections_with_palettes": sections,
            "populated_region_bounds_in_scan": {dim: bounds(coords) for dim, coords in sorted(populated.items())},
            "elapsed_seconds": round(time.monotonic() - started, 3),
            "peak_rss_kib": resource.getrusage(resource.RUSAGE_SELF).ru_maxrss}


def compare(before, after):
    if before["selected_regions"] != after["selected_regions"]:
        raise ValueError("reports cover different regions; use --manifest BEFORE.json for the after scan")
    changes = {}
    for kind in ("blocks", "block_entities", "entities"):
        old, new = before["totals"][kind], after["totals"][kind]
        changes[kind] = {
            "missing_ids": sorted(set(old) - set(new)), "added_ids": sorted(set(new) - set(old)),
            "count_changes": {name: {"before": old.get(name, 0), "after": new.get(name, 0),
                                      "delta": new.get(name, 0) - old.get(name, 0)}
                              for name in sorted(set(old) | set(new)) if old.get(name, 0) != new.get(name, 0)},
        }
    region_changes = {}
    for name in before["selected_regions"]:
        old, new = before["regions"][name], after["regions"][name]
        if any(old[key] != new[key] for key in ("chunks", "sections", "blocks", "block_entities", "entities")):
            region_changes[name] = {"before": old, "after": new}
    return {"before_DataVersion": before["inventory"]["level"]["DataVersion"],
            "after_DataVersion": after["inventory"]["level"]["DataVersion"],
            "changes": changes, "changed_regions": region_changes,
            "interpretation": "Missing/added IDs are rename candidates, not proof of a rename. Palette entry count changes do not prove lost placed blocks; inspect changed regions and screenshots."}


def save_json(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    inventory = commands.add_parser("inventory", help="read zip/directory sizes and level.dat only")
    inventory.add_argument("source", type=Path)
    inventory.add_argument("--output", type=Path, required=True)
    scanner = commands.add_parser("scan", help="scan section palette entries and entity IDs")
    scanner.add_argument("source", type=Path)
    scanner.add_argument("--output", type=Path, required=True)
    scanner.add_argument("--stride", type=int, default=1)
    scanner.add_argument("--spawn-radius", type=int, default=2048)
    scanner.add_argument("--manifest", type=Path)
    scanner.add_argument("--only", help="scan one relative region path for timing")
    differ = commands.add_parser("diff")
    differ.add_argument("before", type=Path)
    differ.add_argument("after", type=Path)
    differ.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "diff":
        result = compare(json.loads(args.before.read_text()), json.loads(args.after.read_text()))
    else:
        if args.command == "scan" and (args.stride < 1 or args.spawn_radius < 0):
            parser.error("stride must be >= 1 and spawn radius >= 0")
        source = WorldSource(args.source)
        try:
            result = source.inventory() if args.command == "inventory" else scan(source, args.stride, args.spawn_radius, args.manifest, args.only)
        finally:
            source.close()
    save_json(args.output, result)
    if args.command == "scan":
        print(json.dumps({"elapsed_seconds": result["elapsed_seconds"],
                          "regions": len(result["selected_regions"]), "chunks": result["chunks_scanned"],
                          "non_vanilla": result["non_vanilla"],
                          "top_palette_entries": result["totals"]["blocks"].most_common(20)}, indent=2))
    else:
        print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, zipfile.BadZipFile, zlib.error, struct.error) as error:
        sys.exit(f"Map scan failed (no complete report written): {error}")
