from pathlib import Path
import subprocess,os,zipfile,shutil,json
root=Path(__file__).resolve().parents[2];p=root/'stow';out=p/'build/classes';shutil.rmtree(out,ignore_errors=True);out.mkdir(parents=True)
cp=[root/'manual-build/minecraft-client-26.3.jar']
for directory in ['manual-build/libs','manual-build/libs/fabric','manual-build/runtime-libs','manual-build/stow-dependencies','manual-build/probe/mods']:
 cp+=list((root/directory).glob('*.jar'))
cp=[v for v in cp if 'fabric-api-' not in v.name or '/libs/fabric/' in str(v)]
(p/'build/classpath.txt').write_text(os.pathsep.join(map(str,cp)))
sources=list((p/'src/main/java').rglob('*.java'));(p/'build/sources.txt').write_text('\n'.join(map(str,sources)))
subprocess.run([str(root/'toolchain/jdk25/bin/javac'),'-proc:none','--release','25','-cp',os.pathsep.join(map(str,cp)),'-d',str(out),'@'+str(p/'build/sources.txt')],check=True)
version=json.loads((p/'src/main/resources/fabric.mod.json').read_text())['version'];jar=p/f'build/libs/stow-{version}.jar';jar.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(jar,'w',compression=zipfile.ZIP_DEFLATED) as z:
 for folder in [out,p/'src/main/resources']:
  for f in folder.rglob('*'):
   if f.is_file():z.write(f,f.relative_to(folder).as_posix())
 for name in ['LICENSE','NOTICE']:z.write(p/name,name)
print('Built',jar,jar.stat().st_size,'bytes')
