import json, pathlib, shutil, zipfile, hashlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
SOURCE=pathlib.Path('/home/thomas/01_TM/Coding/Minecraft/GenshinInMinecraft/wangg_mc/Blocky Teyvat 5.1.0-001.zip')
PREFIX='Blocky Teyvat 5.1.0/saves/方块提瓦特2025 77/'
WORLD=ROOT/'runtime/saves/Blocky Teyvat'
MODS=['fabric-api-0.138.4+1.21.10.jar','iris-fabric-1.9.7+mc1.21.10.jar','[钠] sodium-fabric-0.7.3+mc1.21.10.jar']
with zipfile.ZipFile(SOURCE) as archive:
    entries=[x for x in archive.infolist() if x.filename.startswith(PREFIX) and not x.is_dir()]
    required=sum(x.file_size for x in entries)
    free=shutil.disk_usage(ROOT).free
    assert free>required+10*1024**3, (free,required)
    installed=[]
    for entry in entries:
        rel=pathlib.PurePosixPath(entry.filename[len(PREFIX):])
        assert not rel.is_absolute() and '..' not in rel.parts
        target=WORLD/rel
        target.parent.mkdir(parents=True,exist_ok=True)
        with archive.open(entry) as reader,target.open('wb') as writer:
            shutil.copyfileobj(reader,writer,1024*1024)
        assert target.stat().st_size==entry.file_size
        installed.append({'path':str(target.relative_to(ROOT)),'bytes':entry.file_size,'archive_crc32':f'{entry.CRC:08x}'})
    (ROOT/'runtime/mods').mkdir(exist_ok=True)
    for name in MODS:
        target=ROOT/'runtime/mods'/name
        target.write_bytes(archive.read('Blocky Teyvat 5.1.0/mods/'+name))
    (ROOT/'evidence/archive-serverconfig-readme.txt').write_bytes(archive.read(PREFIX+'serverconfig/readme.txt'))
    (ROOT/'evidence/archive-options.txt').write_bytes(archive.read('Blocky Teyvat 5.1.0/options.txt'))
report={'source':str(SOURCE),'source_bytes':SOURCE.stat().st_size,'world_bytes':required,'free_before_extraction':free,'free_after_extraction':shutil.disk_usage(ROOT).free,'entries':installed,'mods':MODS,'YiFang':'Not installed: separate player-character .ysm models, no instructions or resourcepack dependency in map; archive options resourcePacks=[fabric]. World uses vanilla blocks and its included worldpainter datapack.'}
(ROOT/'evidence/installation.json').write_text(json.dumps(report,indent=2,ensure_ascii=False))
print(json.dumps({k:v for k,v in report.items() if k!='entries'},indent=2,ensure_ascii=False))
