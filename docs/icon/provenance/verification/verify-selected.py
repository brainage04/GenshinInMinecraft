"""Compare native1024 framing with the selected1280 reference and inspect source coverage.
Usage: python3 verify-selected.py [METADATA_FILENAME]
Only the comparison reference is resampled in memory; delivered PNG bytes are never edited.
"""
import pathlib,json,hashlib,math,sys,importlib.util,zlib,collections,time
from PIL import Image,ImageFilter,ImageChops,ImageStat
OUT=pathlib.Path(__file__).resolve().parent;ROOT=OUT.parent;ROUND3=OUT.parents[1]
meta=json.loads((OUT/(sys.argv[1] if len(sys.argv)>1 else '06-shader-heading-minus55-fov60-metadata.json')).read_text())
reference_dir=OUT/'evidence/selected-1280-reference'
old=json.loads((reference_dir/'06-shader-heading-minus55-fov60-metadata.json').read_text())
reference_path=reference_dir/'06-shader-heading-minus55-fov60.png';target_path=ROUND3/meta['file']
assert hashlib.sha256(reference_path.read_bytes()).hexdigest()==old['sha256']
assert target_path.read_bytes()==(ROUND3/meta['source']).read_bytes()
assert hashlib.sha256(target_path.read_bytes()).hexdigest()==meta['sha256']
assert meta['size']==[1024,1024] and old['size']==[1280,1280]
assert meta['camera']==old['camera'] and meta['variant']=='shader-native'
assert meta['runtimeFov']==60 and meta['fovEffectScale']==old['fovEffectScale']==1
reference=Image.open(reference_path).convert('RGB').resize((1024,1024),Image.Resampling.LANCZOS)
target=Image.open(target_path).convert('RGB');assert target.size==(1024,1024)
reference_edges=reference.convert('L').filter(ImageFilter.GaussianBlur(.7)).filter(ImageFilter.FIND_EDGES)
target_edges=target.convert('L').filter(ImageFilter.GaussianBlur(.7)).filter(ImageFilter.FIND_EDGES)
landmarks=[('Nail upper pillar',512,185,48),('right ice arc',595,332,80),('floating stone',714,477,72),('left snow shelf',345,580,80),('lower right ridge',620,645,80),('lake west rim',787,920,80)]
matches=[]
for name,x,y,size in landmarks:
 half=size//2;box=(x-half,y-half,x+half,y+half);patch=reference_edges.crop(box)
 assert ImageStat.Stat(patch).var[0]>20,name
 candidates=[]
 for dy in range(-12,13):
  for dx in range(-12,13):
   shifted=target_edges.crop((box[0]+dx,box[1]+dy,box[2]+dx,box[3]+dy))
   error=ImageStat.Stat(ImageChops.difference(patch,shifted)).mean[0]
   candidates.append((error,dx*dx+dy*dy,dx,dy))
 error,_,dx,dy=min(candidates)
 matches.append({'landmark':name,'referenceCenterAt1024':[x,y],'matchCenter':[x+dx,y+dy],'offsetPixels':[dx,dy],'meanEdgeAbsoluteError':error})
px=sum(r['referenceCenterAt1024'][0] for r in matches)/len(matches);py=sum(r['referenceCenterAt1024'][1] for r in matches)/len(matches)
qx=sum(r['matchCenter'][0] for r in matches)/len(matches);qy=sum(r['matchCenter'][1] for r in matches)/len(matches)
scale=sum((r['referenceCenterAt1024'][0]-px)*(r['matchCenter'][0]-qx)+(r['referenceCenterAt1024'][1]-py)*(r['matchCenter'][1]-qy) for r in matches)/sum((r['referenceCenterAt1024'][0]-px)**2+(r['referenceCenterAt1024'][1]-py)**2 for r in matches)
max_shift=max(max(map(abs,r['offsetPixels'])) for r in matches)
framing={'method':'Six spatially separated static landmarks, edge-template matching within12 pixels of their expected1024 coordinates. The1280 reference is reduced in memory for comparison only; output remains native F2. Require every landmark within2 pixels and fitted scale within0.5 percent of unity.','referenceFile':str(reference_path.relative_to(ROUND3)),'referenceSha256':old['sha256'],'file':meta['file'],'sha256':meta['sha256'],'sameRecordedCamera':True,'nominalFovDegrees':60,'fovEffectScale':1,'effectiveFovDegreesApproximate':66,'effectiveFovBasis':'Earlier empirical spectator-flight FOV isolation, plus the current independent image-framing match. Effective FOV is not directly instrumented.','landmarks':matches,'fittedScale':scale,'fittedTranslationPixels':[qx-scale*px,qy-scale*py],'maximumLandmarkAxisOffsetPixels':max_shift,'pass':max_shift<=2 and abs(scale-1)<=.005}
(OUT/'evidence/selected-framing-check.json').write_text(json.dumps(framing,indent=2))
print('Framing offsets',[r['offsetPixels'] for r in matches],'scale',scale,'pass',framing['pass'])
assert framing['pass'],'Selected composition differs from the reference'
started=time.time();spec=importlib.util.spec_from_file_location('selected_anvil',ROUND3/'blender-g/anvil.py');anvil=importlib.util.module_from_spec(spec);sys.modules[spec.name]=anvil;spec.loader.exec_module(anvil)
camera=meta['camera'];center=[math.floor(camera['xyz'][0]/16),math.floor(camera['xyz'][2]/16)];radius=35
regions={};missing=[];bad_heightmaps=[];empty=[];statuses=collections.Counter();columns=0;minimum=10**9;maximum=-10**9
for cx in range(center[0]-radius,center[0]+radius+1):
 for cz in range(center[1]-radius,center[1]+radius+1):
  key=(cx//32,cz//32)
  if key not in regions:
   path=ROOT/f'runtime/saves/Blocky Teyvat/region/r.{key[0]}.{key[1]}.mca'
   if not path.exists():missing.append([cx,cz]);continue
   f=path.open('rb');regions[key]=(f,f.read(4096))
  f,header=regions[key];idx=(cz%32*32+cx%32)*4;offset=int.from_bytes(header[idx:idx+3],'big')*4096
  if not offset:missing.append([cx,cz]);continue
  f.seek(offset);length=int.from_bytes(f.read(4),'big');codec=f.read(1);assert codec==b'\x02'
  data=anvil.parse_nbt(zlib.decompress(f.read(length-1)));statuses[data.get('Status','absent')]+=1
  heights=data.get('Heightmaps',{}).get('WORLD_SURFACE')
  if not heights:bad_heightmaps.append([cx,cz]);continue
  for k in range(256):
   height=(((heights[k//5]&((1<<64)-1))>>((k%5)*12))&4095)-2033;columns+=1
   if height==-2033:empty.append([cx*16+k%16,cz*16+k//16]);continue
   minimum=min(minimum,height);maximum=max(maximum,height)
for f,header in regions.values():f.close()
yaw=math.radians(camera['yaw']);horizontal_rays_forward=abs(camera['pitch'])+framing['effectiveFovDegreesApproximate']/2<90
empty_visibility=[]
for x,z in empty:
 forwards=[-(x+dx-camera['xyz'][0])*math.sin(yaw)+(z+dz-camera['xyz'][2])*math.cos(yaw) for dx,dz in ((0,0),(1,0),(0,1),(1,1))]
 empty_visibility.append({'column':[x,z],'maximumHorizontalForward':max(forwards),'strictlyBehindView':horizontal_rays_forward and max(forwards)<0,'outsideConfigured32ChunkWindow':abs(x//16-center[0])>32 or abs(z//16-center[1])>32})
unproven_empty=[r['column'] for r in empty_visibility if not r['strictlyBehindView']]
coverage={'method':'Actual Anvil source coverage for the native32 profile, not DH coverage. Check the entire surrounding35-chunk-square client cache window (32 configured chunks plus3 padding), including areas outside the visible frustum. Read every chunk header, NBT status and all256 WORLD_SURFACE heights using the previously validated12-bit custom-dimension height encoding. Any zero-heightmap column must be strictly behind the complete horizontal view half-space: pitch plus half FOV is below90 degrees, so every visible ray points horizontally forward. Test all four column corners, rather than silently ignoring source-air columns. This proves stored source coverage; actual rendering is separately checked with full native F3 cache counts, reference-image comparison, image scanning and visual review. No unexposed render-queue counter is claimed. Complementary border fog intentionally limits the distant background.','file':meta['file'],'camera':camera,'configuredNativeRenderDistanceChunks':32,'checkedCacheRadiusChunks':radius,'centerChunk':center,'requiredChunks':(2*radius+1)**2,'statuses':dict(statuses),'heightmapColumns':columns,'missingChunks':missing,'missingHeightmaps':bad_heightmaps,'sourceEmptyColumns':empty,'emptyColumnVisibility':empty_visibility,'unprovenOrVisibleSourceEmptyColumns':unproven_empty,'horizontalRaysPointForward':horizontal_rays_forward,'minimumSurfaceY':minimum,'maximumSurfaceY':maximum,'elapsedSeconds':time.time()-started,'pass':not missing and not bad_heightmaps and not unproven_empty and sum(statuses.values())==(2*radius+1)**2 and set(statuses)=={'minecraft:full'}}
(OUT/'evidence/selected-native-coverage.json').write_text(json.dumps(coverage,indent=2))
print('Native source chunks',coverage['requiredChunks'],'columns',columns,'statuses',dict(statuses),'missing',len(missing),'empty',len(empty),'pass',coverage['pass'])
assert coverage['pass'],'Native source coverage incomplete'
