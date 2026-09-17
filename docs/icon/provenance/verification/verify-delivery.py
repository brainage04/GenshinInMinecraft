"""Verify the sole selected1024 native frame, retirement ledger and cleanup."""
import pathlib,json,hashlib,struct,subprocess
OUT=pathlib.Path(__file__).resolve().parent;ROOT=OUT.parent;ROUND3=OUT.parents[1]
manifest=json.loads((OUT/'manifest.json').read_text());assert len(manifest)==1
row=manifest[0];assert row['label']=='Dragonspine — Complementary native — heading -55° — FOV 60°'
meta_paths=list(OUT.glob('*-metadata.json'));assert len(meta_paths)==1 and len(list(OUT.glob('*.png')))==1
meta=json.loads(meta_paths[0].read_text());image=ROUND3/row['path'];raw=image.read_bytes()
assert raw==(ROUND3/meta['source']).read_bytes()
assert struct.unpack('>II',raw[16:24])==(1024,1024) and meta['size']==row['size']==[1024,1024]
assert hashlib.sha256(raw).hexdigest()==row['sha256']==meta['sha256']
assert meta['nativeF2'] and meta['uncropped'] and meta['variant']=='shader-native'
assert meta['worldTime']==4000 and not meta['doDaylightCycle'] and not meta['doWeatherCycle']
assert meta['camera']['yaw']==-55 and meta['camera']['fov']==meta['runtimeFov']==60 and meta['fovEffectScale']==1
reference_dir=OUT/'evidence/selected-1280-reference';old=json.loads((reference_dir/'06-shader-heading-minus55-fov60-metadata.json').read_text())
assert meta['camera']==old['camera']
assert hashlib.sha256((reference_dir/'06-shader-heading-minus55-fov60.png').read_bytes()).hexdigest()==old['sha256']
names=[r['file'].lower() for r in meta['modsPresent']]
assert any('iris-' in n for n in names) and any('sodium' in n for n in names) and not any('distanthorizons' in n for n in names)
framing=json.loads((OUT/'evidence/selected-framing-check.json').read_text());coverage=json.loads((OUT/'evidence/selected-native-coverage.json').read_text())
assert framing['pass'] and framing['file']==row['path'] and framing['sha256']==row['sha256']
assert len(framing['landmarks'])==6 and framing['maximumLandmarkAxisOffsetPixels']<=2 and abs(framing['fittedScale']-1)<=.005
assert coverage['pass'] and coverage['file']==row['path'] and not coverage['missingChunks'] and not coverage['missingHeightmaps']
assert coverage['statuses']=={'minecraft:full':5041} and coverage['heightmapColumns']==5041*256
assert not coverage['unprovenOrVisibleSourceEmptyColumns']
assert len(coverage['emptyColumnVisibility'])==len(coverage['sourceEmptyColumns'])
assert all(r['strictlyBehindView'] for r in coverage['emptyColumnVisibility'])
scan=json.loads((OUT/'evidence/image-artifact-check.json').read_text())['frames'];visual=json.loads((OUT/'evidence/artifact-review.json').read_text())['frames']
assert len(scan)==len(visual)==1 and scan[0]['file']==visual[0]['file']==row['path']
assert scan[0]['sha256']==visual[0]['sha256']==row['sha256']
assert scan[0]['squareNativeExpectedSize'] and scan[0]['expectedNativeSize']==[1024,1024] and scan[0]['sourceF2BytesIdentical']
assert scan[0]['flatBlackTiles']==0 and not scan[0]['connectedBlackHoleCandidates']
assert visual[0]['accepted'] and visual[0]['visuallyReviewedNativeFinal'] and not visual[0]['hudOrNametagsVisible'] and not visual[0]['visibleMissingChunkOrBlackWaterArtifact']
assert visual[0]['automaticScanPass']==scan[0]['automaticPass']
if not scan[0]['automaticPass']:assert visual[0]['manualResolution'] and visual[0]['reviewedSkyCandidates']==scan[0]['connectedSkyHoleCandidates']
retirements=json.loads((ROUND3/'removals.DragonspineOrtho.json').read_text());assert len(retirements)==14
originals={r['path']:r for r in json.loads((OUT/'evidence/history-15-frame-delivery/manifest.json').read_text())}
assert {r['path'] for r in retirements}==set(originals)-{row['path']}
for retired in retirements:
 assert retired['reason']=='GenshinInMinecraft: owner selected the shader heading -55 FOV 60 frame'
 original=originals[retired['path']];archived=ROUND3/'removed'/retired['path']
 assert not (ROUND3/retired['path']).exists() and hashlib.sha256(archived.read_bytes()).hexdigest()==original['sha256']
 assert archived.read_bytes()==(ROUND3/original['source']).read_bytes()
 archived_meta=archived.with_name(archived.stem+'-metadata.json');assert json.loads(archived_meta.read_text())['sha256']==original['sha256']
retirement_check=json.loads((OUT/'evidence/retirement-check.json').read_text());assert len(retirement_check['earlierPoisAlreadyLogged'])==6
ledger_paths={r['path'] for r in json.loads((ROUND3/'removals.json').read_text())['removed']}
for path in retirement_check['earlierPoisAlreadyLogged']:assert path in ledger_paths and (ROUND3/'removed'/path).exists()
scripts=json.loads((OUT/'script-sha256.json').read_text())
assert set(scripts)=={p.name for pattern in ('*.py','*.sh','*.args') for p in OUT.glob(pattern)}
for name,digest in scripts.items():assert hashlib.sha256((OUT/name).read_bytes()).hexdigest()==digest
history=OUT/'evidence/history-15-frame-delivery';historical_scripts=json.loads((history/'script-sha256.json').read_text())
for name,digest in historical_scripts.items():assert hashlib.sha256((history/name).read_bytes()).hexdigest()==digest
cleanup=json.loads((OUT/'cleanup-report.json').read_text())
for r in cleanup['restoredFiles']:assert (ROOT/r['target']).read_bytes()==(ROOT/r['backup']).read_bytes()
mods=[{'file':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted((ROOT/'runtime/mods').glob('*.jar'))]
assert mods==cleanup['mods'] and len(mods)==3
assert all(not pathlib.Path(f'/proc/{pid}').exists() for pid in cleanup['stoppedClientPids'])
assert subprocess.run(['pgrep','-af','@'+str(OUT/'capture-client.args')],capture_output=True).returncode==1
assert subprocess.run(['pgrep','-af','Xvfb :211'],capture_output=True).returncode==1 and not pathlib.Path('/tmp/.X11-unix/X211').exists()
modules=subprocess.run(['pactl','list','short','modules'],capture_output=True,text=True,check=True).stdout
sinks=subprocess.run(['pactl','list','short','sinks'],capture_output=True,text=True,check=True).stdout
assert not any('sink_name=round3_teyvat' in line.split('\t')[2].split() for line in modules.splitlines() if len(line.split('\t'))>2)
assert not any(line.split('\t')[1]=='round3_teyvat' for line in sinks.splitlines())
assert hashlib.sha256((ROOT/'renders/07-dragonspine-skyfrost-nail.png').read_bytes()).hexdigest()==cleanup['originalSelectedPngSha256']
assert hashlib.sha256((ROOT/'runtime/shaderpacks/ComplementaryReimagined_r5.7.1.zip').read_bytes()).hexdigest()==cleanup['shaderZipSha256']
result={'manifestEntries':1,'nativeSize':[1024,1024],'sourceF2BytesIdentical':True,'sameCameraAsSelectedReference':True,'nominalFovDegrees':60,'fovEffectScale':1,'effectiveFovDegreesApproximate':66,'matchedLandmarks':6,'maximumLandmarkAxisOffsetPixels':framing['maximumLandmarkAxisOffsetPixels'],'fittedScale':framing['fittedScale'],'nativeFullChunks':5041,'sourceHeightmapColumns':coverage['heightmapColumns'],'sourceEmptyColumnsProvenBehindCamera':len(coverage['sourceEmptyColumns']),'missingSourceChunks':0,'unprovenVisibleSourceEmptyColumns':0,'automaticSkyWarningsPreserved':len(scan[0]['connectedSkyHoleCandidates']),'flatBlackTiles':0,'visuallyAccepted':True,'retiredAlternatives':14,'retiredOriginalHashesVerified':True,'earlierPoisAlreadyLogged':6,'agentRemovalLedger':'removals.DragonspineOrtho.json','sharedLedgerOwner':'Main','authorHashesVerified':len(scripts),'historicalAuthorHashesVerified':len(historical_scripts),'allStartedRuntimeResourcesStopped':True,'blockers':json.loads((OUT/'blockers.json').read_text())['blocking']}
assert not result['blockers']
(OUT/'evidence/delivery-check.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
