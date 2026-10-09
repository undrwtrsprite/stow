"""Recreate the pinned local Java/Minecraft/Fabric build and test dependencies."""
from pathlib import Path
import concurrent.futures as futures,json,urllib.request,hashlib,zipfile,tarfile,os
ROOT=Path(__file__).resolve().parents[2]; BUILD=ROOT/'manual-build'
for d in ['libs','libs/fabric','runtime-libs','stow-dependencies','probe/mods']: (BUILD/d).mkdir(parents=True,exist_ok=True)
def request(url):
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'stow-development/0.5.10'}),timeout=90) as response:return response.read()
def get(url,path,sha1=None,sha256=None):
 path=Path(path)
 if path.exists() and (not sha1 or hashlib.sha1(path.read_bytes()).hexdigest()==sha1) and (not sha256 or hashlib.sha256(path.read_bytes()).hexdigest()==sha256):return path
 data=request(url)
 if sha1 and hashlib.sha1(data).hexdigest()!=sha1:raise RuntimeError('SHA1 mismatch: '+path.name)
 if sha256 and hashlib.sha256(data).hexdigest()!=sha256:raise RuntimeError('SHA256 mismatch: '+path.name)
 path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data);return path
BUILD.mkdir(parents=True,exist_ok=True)
if not (BUILD/'minecraft-26.3.json').exists():
 manifest=json.loads(request('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
 metadata=next(v for v in manifest['versions'] if v['id']=='26.3')
 get(metadata['url'],BUILD/'minecraft-26.3.json',metadata['sha1'])
if not (BUILD/'jdk25-assets.json').exists():
 (BUILD/'jdk25-assets.json').write_bytes(request('https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=x64&image_type=jdk&os=linux'))
mc=json.loads((BUILD/'minecraft-26.3.json').read_text())
jdk=json.loads((BUILD/'jdk25-assets.json').read_text())[0]['binary']['package']
fabric=json.loads(request('https://meta.fabricmc.net/v2/versions/loader/26.3/0.19.5/profile/json'))
# The profile has no hash for the loader; this value matches Fabric's published SHA-1 sidecar for the same jar.
PINNED_FABRIC_SHA256={'fabric-loader-0.19.5.jar':'93044e4dd46de5d8136701292f05e868da096d2c9fddb4793e4fdbcc63efc695'}
(BUILD/'fabric-loader-profile.json').write_text(json.dumps(fabric))
def mod(project,version,path):
 versions=json.loads(request('https://api.modrinth.com/v2/project/'+project+'/version'))
 v=next(v for v in versions if v['version_number']==version and ('fabric' in v['loaders']))
 file=next(f for f in v['files'] if f['primary']);return get(file['url'],path,sha1=file['hashes']['sha1'])
jobs=[('https://repo.maven.apache.org/maven2/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar',BUILD/'libs/annotations-26.0.2.jar','c7ce3cdeda3d18909368dfe5977332dfad326c6d',None),(jdk['link'],ROOT/'toolchain/jdk25.tar.gz',None,jdk['checksum'])]
v=mc['downloads']['client'];jobs.append((v['url'],BUILD/'minecraft-client-26.3.jar',v['sha1'],None))
for lib in mc['libraries']:
 if 'rules' in lib and not any(r.get('action')=='allow' and r.get('os',{}).get('name','linux')=='linux' for r in lib['rules']):continue
 artifact=lib.get('downloads',{}).get('artifact')
 if artifact:jobs.append((artifact['url'],BUILD/'libs'/Path(artifact['path']).name,artifact.get('sha1'),None))
for lib in fabric['libraries']:
 name=lib['name'].split(':');filename=name[1]+'-'+name[2]+'.jar';path=name[0].replace('.','/')+'/'+name[1]+'/'+name[2]+'/'+filename
 # Six of seven libraries carry a SHA-256 in the profile; the loader uses the pinned value above.
 # A library with neither fails with KeyError instead of downloading unverified.
 jobs.append((lib['url']+path,BUILD/'runtime-libs'/filename,None,lib.get('sha256') or PINNED_FABRIC_SHA256[filename]))
with futures.ThreadPoolExecutor(max_workers=12) as pool:
 tasks=[pool.submit(get,*job) for job in jobs]
 tasks += [pool.submit(mod,'fabric-api','0.161.0+26.3',BUILD/'probe/mods/fabric-api-0.161.0+26.3.jar'),pool.submit(mod,'cloth-config','26.3.159+fabric',BUILD/'stow-dependencies/cloth-config-fabric-26.3.159.jar'),pool.submit(mod,'modmenu','21.0.0',BUILD/'probe/mods/modmenu.jar')]
 for i,t in enumerate(tasks):t.result()
print('Downloaded pinned dependencies',flush=True)
if not (ROOT/'toolchain/jdk25').exists():
 with tarfile.open(ROOT/'toolchain/jdk25.tar.gz') as archive:
  archive.extractall(ROOT/'toolchain',filter='data');name=archive.getnames()[0].split('/')[0]
 (ROOT/'toolchain'/name).rename(ROOT/'toolchain/jdk25')
with zipfile.ZipFile(BUILD/'probe/mods/fabric-api-0.161.0+26.3.jar') as archive:
 for name in archive.namelist():
  if name.startswith('META-INF/jars/') and name.endswith('.jar'):(BUILD/'libs/fabric'/Path(name).name).write_bytes(archive.read(name))
with zipfile.ZipFile(BUILD/'runtime-libs/fabric-loader-0.19.5.jar') as archive:
 for name in archive.namelist():
  if name.startswith('META-INF/jars/') and name.endswith('.jar'):(BUILD/'runtime-libs'/Path(name).name).write_bytes(archive.read(name))
cp=[BUILD/'minecraft-client-26.3.jar',*sorted((BUILD/'libs').glob('*.jar')),*sorted((BUILD/'runtime-libs').glob('*.jar'))]
(BUILD/'probe/classpath.txt').write_text(os.pathsep.join(map(str,cp)))
(BUILD/'probe/options.txt').write_text('guiScale:2\nrenderDistance:2\nsimulationDistance:2\nmaxFps:30\nfov:0.0\nfullscreen:false\n')
print('Java 25 build/test runtime ready',flush=True)
