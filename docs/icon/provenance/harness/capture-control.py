"""Private-display camera/F2 controller. Never crops or edits image pixels.
Usage: python3 capture-control.py camera INDEX [dh-textured|shader-native|dh-profile]
       python3 capture-control.py capture INDEX FILE_STEM [dh-textured|shader-native|dh-profile] [SIZE=1280]
Camera expects gameplay with HUD visible; capture expects F1-hidden HUD.
FOV is set through Minecraft Options before invoking camera; the file value is
verified against the selected camera plan at capture time. Use only DISPLAY :211.
"""
import pathlib,subprocess,os,time,json,sys,hashlib,shutil,struct
OUT=pathlib.Path(__file__).resolve().parent;ROOT=OUT.parent
ENV={**os.environ,'DISPLAY':':211'}
def xd(*args):
 return subprocess.run(['xdotool',*map(str,args)],env=ENV,capture_output=True,text=True,check=True).stdout
variant_index=3 if sys.argv[1]=='camera' else 4
variant=sys.argv[variant_index] if len(sys.argv)>variant_index and sys.argv[variant_index] in ('dh-textured','shader-native','dh-profile') else 'dh-textured'
plan={'dh-textured':'camera-plan.json','shader-native':'shader-camera-plan.json','dh-profile':'low-camera-plan.json'}[variant]
index=int(sys.argv[2]);camera=json.loads((OUT/plan).read_text())[index]
if sys.argv[1]=='camera':
 x,y,z=camera['xyz'];command=f'/tp @s {x:.6f} {y:.6f} {z:.6f} {camera["yaw"]:.6f} {camera["pitch"]:.6f}'
 xd('key','t');time.sleep(5);xd('type','--clearmodifiers','--delay',30,command);time.sleep(2);xd('key','Return');time.sleep(6)
 print(command)
 with (OUT/'evidence/camera-commands.jsonl').open('a') as f:f.write(json.dumps({'unixTime':time.time(),'cameraIndex':index,'command':command})+'\n')
elif sys.argv[1]=='capture':
 options=dict(l.split(':',1) for l in (ROOT/'runtime/options.txt').read_text().splitlines() if ':' in l)
 fov=70+40*float(options['fov'])
 if abs(fov-camera['fov'])>.01:raise RuntimeError(f'Expected FOV {camera["fov"]}, runtime options has {fov}')
 if variant=='dh-profile' and float(options['fovEffectScale'])!=0:raise RuntimeError('Low-profile projections require FOV effects OFF')
 screenshots=ROOT/'runtime/screenshots';before=set(screenshots.glob('*.png'));start=time.time();xd('key','F2')
 while time.time()-start<90:
  time.sleep(2);new=set(screenshots.glob('*.png'))-before
  if new:
   source=max(new,key=lambda p:p.stat().st_mtime)
   if source.stat().st_size>1000:
    size0=source.stat().st_size;time.sleep(3)
    if source.stat().st_size==size0:break
 else:raise RuntimeError('No completed native F2 screenshot appeared')
 png=source.read_bytes();width,height=struct.unpack('>II',png[16:24])
 expected_size=int(sys.argv[5]) if len(sys.argv)>5 else 1280
 if (width,height)!=(expected_size,expected_size):raise RuntimeError(f'Refusing unexpected native resolution: {width}x{height}, expected {expected_size} square')
 target=OUT/(sys.argv[3]+'.png');shutil.copyfile(source,target)
 proof={'index':index,'camera':camera,'source':str(source.relative_to(ROOT.parent)),'file':str(target.relative_to(ROOT.parent)),'size':[width,height],'sha256':hashlib.sha256(png).hexdigest(),'unixTime':time.time(),'nativeF2':True,'uncropped':True,'runtimeFov':fov,'cameraCommand':f'/tp @s {camera["xyz"][0]:.6f} {camera["xyz"][1]:.6f} {camera["xyz"][2]:.6f} {camera["yaw"]:.6f} {camera["pitch"]:.6f}'}
 proof.update({'variant':variant,'worldTime':4000,'weather':'clear, fixed','doDaylightCycle':False,'doWeatherCycle':False,'vanillaRenderDistanceChunks':32,'shaderPackInstalled':'ComplementaryReimagined_r5.7.1.zip','shaderPackEnabled':variant=='shader-native','distantHorizonsEnabled':variant!='shader-native'})
 proof['fovEffectScale']=float(options['fovEffectScale'])
 proof['modsPresent']=[{'file':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted((ROOT/'runtime/mods').glob('*.jar'))]
 names=[m['file'].lower() for m in proof['modsPresent']]
 assert any('distanthorizons' in n for n in names)==(variant!='shader-native')
 assert any('iris-' in n for n in names)==(variant=='shader-native')
 config_paths=[ROOT/'runtime/options.txt',ROOT/'runtime/config/DistantHorizons.toml',ROOT/'runtime/config/iris.properties',ROOT/'runtime/config/sodium-options.json',ROOT/'runtime/shaderpacks/ComplementaryReimagined_r5.7.1.zip.txt']
 proof['configurationSnapshots']={}
 for config in config_paths:
  if config.exists():
   snapshot=OUT/'evidence'/(sys.argv[3]+'-'+config.name);shutil.copyfile(config,snapshot)
   proof['configurationSnapshots'][config.name]={'file':str(snapshot.relative_to(ROOT.parent)),'sha256':hashlib.sha256(snapshot.read_bytes()).hexdigest()}
 (OUT/(sys.argv[3]+'-metadata.json')).write_text(json.dumps(proof,indent=2));print(json.dumps(proof,indent=2))
else:raise SystemExit('Expected camera or capture')
