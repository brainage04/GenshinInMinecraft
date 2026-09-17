import pathlib,json,urllib.request,shutil,hashlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'runtime'
CACHE=pathlib.Path('/home/thomas/.gradle/caches')
info=json.loads((CACHE/'fabric-loom/1.21.10/mojang_minecraft_info.json').read_text())
profile=json.load(urllib.request.urlopen('https://meta.fabricmc.net/v2/versions/loader/1.21.10/0.18.4/profile/json'))
(ROOT/'evidence/fabric-profile.json').write_text(json.dumps(profile,indent=2))
(ROOT/'evidence/minecraft-profile.json').write_text(json.dumps(info,indent=2))
def fetch(url,target,sha1=None):
    target.parent.mkdir(parents=True,exist_ok=True)
    if not target.exists():
        with urllib.request.urlopen(url) as src,target.open('wb') as dst: shutil.copyfileobj(src,dst)
    if sha1: assert hashlib.sha1(target.read_bytes()).hexdigest()==sha1,target
    return target
classpath=[]
libs={x['name'].split(':')[0]+':'+x['name'].split(':')[1]:x for x in info['libraries']}
for x in profile['libraries']: libs[x['name'].split(':')[0]+':'+x['name'].split(':')[1]]=x
# Keep native classifier jars separately, since same artifact keys must not collapse classifiers.
libs=list(info['libraries'])
replaced={':'.join(x['name'].split(':')[:2]) for x in profile['libraries']}
libs=[x for x in libs if ':'.join(x['name'].split(':')[:2]) not in replaced]+profile['libraries']
for lib in libs:
    allowed=True
    if 'rules' in lib:
        allowed=False
        for rule in lib['rules']:
            if rule.get('os',{}).get('name','linux')=='linux': allowed=rule['action']=='allow'
    if not allowed: continue
    parts=lib['name'].split(':'); group,artifact,version=parts[:3]
    filename=artifact+'-'+version+('-'+parts[3] if len(parts)>3 else '')+'.jar'
    art=lib.get('downloads',{}).get('artifact',{})
    rel=art.get('path',group.replace('.','/')+'/'+artifact+'/'+version+'/'+filename)
    target=ROOT/'libraries'/rel
    cached=next((CACHE/'modules-2/files-2.1'/group/artifact/version).glob('*/'+filename),None)
    target.parent.mkdir(parents=True,exist_ok=True)
    if cached and not target.exists(): shutil.copy2(cached,target)
    fetch(art.get('url',lib.get('url','https://libraries.minecraft.net/')+rel),target,art.get('sha1',lib.get('sha1')))
    classpath.append(str(target))
client=ROOT/'libraries/minecraft-client-1.21.10.jar'
shutil.copy2(CACHE/'fabric-loom/1.21.10/minecraft-client.jar',client)
classpath.append(str(client))
index=fetch(info['assetIndex']['url'],ROOT/'assets/indexes/27.json',info['assetIndex']['sha1'])
objects=json.loads(index.read_text())['objects']; copied=downloaded=0
for obj in objects.values():
    h=obj['hash']; rel=h[:2]+'/'+h
    target=ROOT/'assets/objects'/rel
    cached=CACHE/'fabric-loom/assets/objects'/rel
    target.parent.mkdir(parents=True,exist_ok=True)
    if cached.exists():
        if not target.exists(): shutil.copy2(cached,target)
        copied+=1
    else:
        fetch('https://resources.download.minecraft.net/'+rel,target,h); downloaded+=1
args=['-XX:ActiveProcessorCount=2','-XX:ParallelGCThreads=2','-XX:ConcGCThreads=1','-Xmx5G','-Djava.awt.headless=true','-Dfabric.development=false','-cp',':'.join(classpath),profile['mainClass'],'--username','TeyvatCapture','--uuid','a881d270fc7b4456871133ffe094aafe','--version','1.21.10','--gameDir',str(RUNTIME),'--assetsDir',str(ROOT/'assets'),'--assetIndex','27','--accessToken','0','--width','1280','--height','960','--quickPlaySingleplayer','Blocky Teyvat']
(ROOT/'client.args').write_text('\n'.join(json.dumps(x) if ' ' in x else x for x in args)+'\n')
print(json.dumps({'libraries':len(classpath),'assets_copied':copied,'assets_downloaded':downloaded}))
