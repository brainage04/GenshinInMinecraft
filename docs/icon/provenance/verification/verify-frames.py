"""Conservative image anomaly scan + native F2 byte comparison; no retouching."""
import pathlib,json,hashlib,math,collections,sys
from PIL import Image,ImageStat
OUT=pathlib.Path(__file__).resolve().parent;ROUND3=OUT.parents[1]
reports=[]
meta_paths=[OUT/name for name in sys.argv[1:]] if len(sys.argv)>1 else sorted(OUT.glob('*-metadata.json'))
for meta_path in meta_paths:
 meta=json.loads(meta_path.read_text());path=ROUND3/meta['file'];source=ROUND3/meta['source']
 raw=path.read_bytes();image=Image.open(path).convert('RGB');w,h=image.size
 square_native=w==h and [w,h]==meta['size']
 flat_sky=set();flat_black=set();tiles=0
 for ty in range(0,h-15,16):
  for tx in range(0,w-15,16):
   stats=ImageStat.Stat(image.crop((tx,ty,tx+16,ty+16)));r,g,b=stats.mean;v=sum(stats.var);tiles+=1
   if max(stats.mean)<3 and v<2:flat_black.add((tx//16,ty//16))
   # Missing-chunk voids show smooth bright blue sky inside an all-ground
   # downward frustum. Actual shaded ocean is dark and textured, not bright sky.
   if v<3 and r>90 and b>r*1.08 and g>r*1.03:flat_sky.add((tx//16,ty//16))
 def components(points):
  points=set(points);out=[]
  while points:
   seed=points.pop();component=[seed];stack=[seed]
   while stack:
    x,y=stack.pop()
    for n in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]:
     if n in points:points.remove(n);component.append(n);stack.append(n)
   if len(component)>=4:
    out.append({'tiles':len(component),'pixelBounds':[min(p[0] for p in component)*16,min(p[1] for p in component)*16,(max(p[0] for p in component)+1)*16,(max(p[1] for p in component)+1)*16]})
  return out
 sky_regions=components(flat_sky);black_regions=components(flat_black)
 border_sky=[r for r in sky_regions if r['pixelBounds'][0]==0 or r['pixelBounds'][1]==0 or r['pixelBounds'][2]==w or r['pixelBounds'][3]==h]
 # Shader-native and low-angle DH frames include a legitimate sky horizon.
 # Boundary-connected background is expected, unlike the all-ground aerials.
 has_horizon=meta['variant'] in ('shader-native','dh-profile')
 unexpected_sky=[r for r in sky_regions if r not in border_sky] if has_horizon else sky_regions
 reports.append({'file':meta['file'],'profile':meta['variant'],'resolution':[w,h],'expectedNativeSize':meta['size'],'squareNativeExpectedSize':square_native,'sourceF2BytesIdentical':raw==source.read_bytes(),'sha256':hashlib.sha256(raw).hexdigest(),'scanned16x16Tiles':tiles,'flatBrightSkyTiles':len(flat_sky),'connectedSkyRegions':sky_regions,'expectedBoundaryHorizonRegions':border_sky if has_horizon else [],'connectedSkyHoleCandidates':unexpected_sky,'flatBlackTiles':len(flat_black),'connectedBlackHoleCandidates':black_regions,'automaticPass':square_native and raw==source.read_bytes() and not unexpected_sky and not black_regions})
result={'method':'Scan every16×16 tile for near-uniform bright-blue sky (sum channel variance<3; R>90, B>1.08R, G>1.03R) and near-black voids (all means<3, variance<2), then find4-connected regions>=4 tiles. Any such sky region is suspect in an all-ground aerial DH frustum. Low-angle DH and shorter shader-native views include real horizons: boundary-connected sky/fog is expected and recorded separately; enclosed regions remain suspect. Byte-compare each delivered image against its original native F2 and verify its recorded square native dimensions. Combine image scans with DH FULL-column coverage or native source-chunk coverage and relevant F3 diagnostics; visually inspect every frame, including horizon transitions. These heuristic scans cannot prove every possible rendering defect absent.','frames':reports,'allAutomaticPass':bool(reports) and all(r['automaticPass'] for r in reports)}
(OUT/'evidence/image-artifact-check.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result,indent=2))
