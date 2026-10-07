"""Standard-library regressions; synthetic worlds live only under run/map-port/."""

import gzip
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

import palette_scan as scanner

spec = importlib.util.spec_from_file_location("port_map", Path(__file__).with_name("port-map.py"))
port = importlib.util.module_from_spec(spec)
spec.loader.exec_module(port)


def named(tag, name, payload):
    return bytes([tag]) + port.nbt_string(name) + payload


def compound(*children):
    return b"".join(children) + b"\0"


def level(rules=None, version=4556):
    return named(10, "", compound(named(10, "Data", compound(
        named(3, "DataVersion", struct.pack(">i", version)),
        named(8, "LevelName", port.nbt_string("Synthetic")),
        named(10, "GameRules", port.rule_payload(rules or {}))))))


def entity_region():
    entity = port.rule_payload({"id": "minecraft:item_display"})
    root = named(10, "", compound(
        named(3, "DataVersion", struct.pack(">i", 4556)),
        named(9, "Entities", struct.pack(">Bi", 10, 1) + entity)))
    payload = zlib.compress(root)
    header = struct.pack(">I", (2 << 8) | 1) + bytes(8192 - 4)
    chunk = struct.pack(">I", len(payload) + 1) + b"\2" + payload
    return header + chunk + bytes(4096 - len(chunk))


class MapPortTests(unittest.TestCase):
    def setUp(self):
        port.WORK_BASE.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix="regression-", dir=port.WORK_BASE)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.world = self.root / "world"
        self.world.mkdir()
        (self.world / "level.dat").write_bytes(gzip.compress(level()))

    def write_region(self, name, data):
        path = self.world / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        return path

    def scan(self, manifest=None):
        source = scanner.WorldSource(self.world)
        try:
            return scanner.scan(source, radius=0, manifest=manifest)
        finally:
            source.close()

    def baseline(self):
        before = self.scan()
        path = self.root / "before.json"
        scanner.save_json(path, before)
        return before, path

    def test_relocated_entities_keep_identity_and_counts(self):
        old = self.write_region("entities/r.0.0.mca", entity_region())
        before, manifest = self.baseline()
        new = self.world / "dimensions/minecraft/overworld/entities/r.0.0.mca"
        new.parent.mkdir(parents=True)
        old.rename(new)
        after = self.scan(manifest)
        diff = scanner.compare(before, after)
        self.assertEqual(after["totals"]["entities"], {"minecraft:item_display": 1})
        self.assertEqual(diff["removed_empty_regions"], [])
        self.assertEqual(diff["changed_regions"], {})
        self.assertEqual(diff["relocated_regions"], {
            "entities/r.0.0.mca": "dimensions/minecraft/overworld/entities/r.0.0.mca"})

    def test_removed_empty_region_is_reported(self):
        for contents in (b"", bytes(8192)):
            with self.subTest(bytes=len(contents)):
                path = self.write_region("entities/r.0.0.mca", contents)
                before, manifest = self.baseline()
                path.unlink()
                after = self.scan(manifest)
                self.assertEqual(after["selected_regions"], before["selected_regions"])
                self.assertEqual(scanner.compare(before, after)["removed_empty_regions"], ["entities/r.0.0.mca"])

    def test_missing_populated_region_fails(self):
        path = self.write_region("entities/r.0.0.mca", entity_region())
        _, manifest = self.baseline()
        path.unlink()
        with self.assertRaisesRegex(ValueError, "nonempty or unproven"):
            self.scan(manifest)

    def test_present_region_with_lost_chunks_fails_comparison(self):
        path = self.write_region("entities/r.0.0.mca", entity_region())
        before, manifest = self.baseline()
        path.write_bytes(bytes(8192))
        after = self.scan(manifest)
        with self.assertRaisesRegex(ValueError, "chunk records lost"):
            scanner.compare(before, after)

    def test_missing_unproven_region_fails(self):
        path = self.root / "before.json"
        scanner.save_json(path, {"selected_regions": ["entities/r.0.0.mca"], "regions": {}})
        with self.assertRaisesRegex(ValueError, "nonempty or unproven"):
            self.scan(path)

    def test_ambiguous_old_and_new_paths_fail(self):
        self.write_region("entities/r.0.0.mca", b"")
        self.write_region("dimensions/minecraft/overworld/entities/r.0.0.mca", b"")
        with self.assertRaisesRegex(ValueError, "ambiguous"):
            scanner.WorldSource(self.world)

    def test_vanilla_dimension_path_normalization(self):
        self.assertEqual(scanner.comparison_path("dimensions/minecraft/the_nether/region/r.1.2.mca"), "DIM-1/region/r.1.2.mca")
        self.assertEqual(scanner.comparison_path("dimensions/minecraft/the_end/entities/c.1.2.mcc"), "DIM1/entities/c.1.2.mcc")
        custom = "dimensions/example/custom/region/r.1.2.mca"
        self.assertEqual(scanner.comparison_path(custom), custom)
        self.assertEqual(scanner.dimension_for(
            "dimensions/minecraft/overworld/data/minecraft/world_border.dat"), "minecraft:overworld")
        self.assertEqual(scanner.dimension_for(
            "dimensions/example/custom/path/data/example/saved.dat"), "example:custom/path")

    def test_gamerule_migration_keeps_map_protection_and_inverts_checks(self):
        actual = port.migrated_rules({
            "doFireTick": "false", "allowFireTicksAwayFromPlayer": "false",
            "doMobSpawning": "false", "mobGriefing": "false",
            "doDaylightCycle": "false", "doWeatherCycle": "false",
            "randomTickSpeed": "0", "spawnRadius": "0", "keepInventory": "true",
            "disableRaids": "false", "disableElytraMovementCheck": "false",
            "disablePlayerMovementCheck": "true", "axiomDoBlockDrops": "true"})
        self.assertEqual(actual["minecraft:fire_spread_radius_around_player"], 0)
        self.assertEqual(actual["minecraft:random_tick_speed"], 0)
        self.assertFalse(actual["minecraft:spawn_mobs"])
        self.assertFalse(actual["minecraft:mob_griefing"])
        self.assertFalse(actual["minecraft:advance_time"])
        self.assertFalse(actual["minecraft:advance_weather"])
        self.assertTrue(actual["minecraft:keep_inventory"])
        self.assertTrue(actual["minecraft:raids"])
        self.assertTrue(actual["minecraft:elytra_movement_check"])
        self.assertFalse(actual["minecraft:player_movement_check"])
        self.assertNotIn("axiomDoBlockDrops", actual)
        for away, radius in (("false", 128), ("true", -1)):
            self.assertEqual(port.migrated_rules({"doFireTick": "true", "allowFireTicksAwayFromPlayer": away})[
                "minecraft:fire_spread_radius_around_player"], radius)

    def test_prepare_filters_unknown_rules_without_changing_vanilla(self):
        rules = {"randomTickSpeed": "0", "doFireTick": "false", "axiomDoBlockDrops": "true"}
        enabled = ["vanilla", "axiom", "fabric", "file/worldpainter.zip"]
        data = named(10, "", compound(named(10, "Data", compound(
            named(10, "GameRules", port.rule_payload(rules)),
            named(10, "DataPacks", compound(named(9, "Enabled",
                struct.pack(">Bi", 8, len(enabled)) + b"".join(port.nbt_string(n) for n in enabled))))))))
        import zipfile
        pack_path = self.world / "datapacks/worldpainter.zip"
        pack_path.parent.mkdir()
        height = {"min_y": -2032, "height": 4064, "logical_height": 4064}
        with zipfile.ZipFile(pack_path, "w") as pack:
            pack.writestr("pack.mcmeta", '{"pack":{"pack_format":9}}')
            pack.writestr("data/minecraft/dimension_type/overworld.json", json.dumps(height))
        (self.world / "level.dat").write_bytes(gzip.compress(data))
        report = port.prepare_vanilla(self.world, self.root,
            {"pack_version": {"data_major": 107, "data_minor": 1}}, height)
        patched = scanner.read_nbt(gzip.decompress((self.world / "level.dat").read_bytes()))["Data"]
        self.assertEqual(patched["GameRules"], {"randomTickSpeed": "0", "doFireTick": "false"})
        self.assertEqual(report["removed_unknown_gamerules"], ["axiomDoBlockDrops"])
        self.assertEqual(gzip.decompress((self.root / "original-level.dat").read_bytes()), data)

    def test_repair_backs_up_and_is_idempotent(self):
        original = {"randomTickSpeed": "0", "doFireTick": "false", "axiomDoBlockDrops": "true"}
        (self.root / "original-level.dat").write_bytes(gzip.compress(level(original)))
        (self.world / "level.dat").write_bytes(gzip.compress(level(version=4903)))
        (self.world / "session.lock").write_bytes(b"lock")
        scanner.save_json(self.root / port.MARKER, {"work_dir": str(self.root), "selection": "full"})
        path = self.world / "data/minecraft/game_rules.dat"
        path.parent.mkdir(parents=True)
        corrupt = gzip.compress(named(10, "", compound(
            named(10, "data", port.rule_payload({"minecraft:random_tick_speed": 3, "axiomDoBlockDrops": "true"})),
            named(3, "DataVersion", struct.pack(">i", 4772)))))
        path.write_bytes(corrupt)
        report = port.repair_gamerules(self.root)
        self.assertEqual((self.root / "pre-repair-game_rules.dat").read_bytes(), corrupt)
        self.assertEqual(report["after"], {"minecraft:random_tick_speed": 0, "minecraft:fire_spread_radius_around_player": 0})
        self.assertEqual(scanner.read_nbt(gzip.decompress(path.read_bytes()))["DataVersion"], 4903)
        self.assertEqual(port.repair_gamerules(self.root)["status"], "already_repaired")

    def test_make_playtest_is_fresh_managed_and_preserves_source_and_terrain(self):
        raw = named(10, "", compound(named(10, "Data", compound(
            named(3, "DataVersion", struct.pack(">i", 4903)),
            named(3, "GameType", struct.pack(">i", 0)),
            named(1, "allowCommands", b"\0"),
            named(10, "spawn", compound(
                named(11, "pos", struct.pack(">iiii", 3, 3458, 63, -4002)),
                named(5, "yaw", struct.pack(">f", 0))))))))
        original = gzip.compress(raw)
        (self.world / "level.dat").write_bytes(original)
        (self.world / "session.lock").write_bytes(b"lock")
        terrain = self.write_region("dimensions/minecraft/overworld/region/r.0.0.mca", entity_region())
        source_player = self.world / "players/data/copied-player.dat"
        source_player.parent.mkdir(parents=True)
        source_player.write_bytes(b"source player")
        destination = self.root / "fresh-world"
        report = port.make_playtest(self.world, destination)
        self.assertTrue(report["managed_on_first_load"])
        self.assertEqual(json.loads((destination / ".genshin-playtest.json").read_text()),
                         {"version": 1, "overlay": "mondstadt"})
        data = scanner.read_nbt(gzip.decompress((destination / "level.dat").read_bytes()))["Data"]
        self.assertEqual(data["GameType"], 2)
        self.assertTrue(data["allowCommands"])
        self.assertEqual(struct.unpack(">iii", data["spawn"]["pos"]), (2938, -206, -3635))
        self.assertEqual(data["spawn"]["yaw"], 90)
        self.assertEqual((destination / terrain.relative_to(self.world)).read_bytes(), terrain.read_bytes())
        self.assertEqual((self.world / "level.dat").read_bytes(), original)
        self.assertTrue(source_player.exists())
        self.assertFalse((destination / "players").exists())
        self.assertIn("maxHeapSize = '2G'", Path(report["init_script"]).read_text())
        self.assertIn("simulationDistance:5", destination.with_name(destination.name + "-client").joinpath("options.txt").read_text())
        with self.assertRaisesRegex(ValueError, "already exist"):
            port.make_playtest(self.world, destination)
        with self.assertRaisesRegex(ValueError, "strictly inside"):
            port.make_playtest(self.world, self.root.parent.parent / "outside")

    def test_modified_utf8_patch_preserves_unrelated_bytes(self):
        text = "方块\0\U0001f30d"
        raw = named(10, "", compound(named(8, "text", port.nbt_string(text)),
            named(10, "rules", port.rule_payload({"randomTickSpeed": "3"}))))
        patched = port.replace_payload(raw, ["rules"], 10, port.rule_payload({"randomTickSpeed": "0"}))
        self.assertEqual(scanner.read_nbt(patched)["text"], text)
        self.assertEqual(patched[:patched.index(b"rules")], raw[:raw.index(b"rules")])


if __name__ == "__main__":
    unittest.main()
