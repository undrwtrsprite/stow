from pathlib import Path
import subprocess,zipfile,json,os,shutil,tempfile
ROOT=Path(__file__).resolve().parents[2];BUILD=ROOT/'manual-build';TEST=ROOT/'stow-tests';TEST.mkdir(exist_ok=True)
GAME=Path(tempfile.mkdtemp(prefix='game-v03-',dir=TEST));(TEST/'latest-game.txt').write_text(str(GAME))
(GAME/'mods').mkdir();(GAME/'config').mkdir();(GAME/'assets').mkdir()
for path in [BUILD/'stow-dependencies/cloth-config-fabric-26.3.159.jar',ROOT/'stow/build/libs/stow-0.4.0+mc26.3.jar',BUILD/'probe/mods/fabric-api-0.161.0+26.3.jar',BUILD/'probe/mods/modmenu.jar']:shutil.copy2(path,GAME/'mods'/path.name)
shutil.copy2(BUILD/'probe/options.txt',GAME/'options.txt')
old=GAME/'config/mousewheelie-chest-memory';old.mkdir();(old/'import-fixture.json').write_text('{"version":4,"chests":[],"projects":[]}')
(GAME/'config/mousewheelie-pinned-slots.txt').write_text('35\n');(GAME/'config/mousewheelie.hjson').write_text('{ general: { "shift-drag-mode": "MATCHING_ONLY" } }')
cp=(ROOT/'stow/build/classpath.txt').read_text()+':'+str(ROOT/'stow/build/classes')
CLASSES=TEST/'classes-v03';shutil.rmtree(CLASSES,ignore_errors=True);CLASSES.mkdir()
subprocess.run([str(ROOT/'toolchain/jdk25/bin/javac'),'-proc:none','--release','25','-cp',cp,'-d',str(CLASSES),*map(str,(ROOT/'stow/tests/porttest').glob('*.java'))],check=True)
with zipfile.ZipFile(GAME/'mods/port-feature-test.jar','w') as z:
 for p in CLASSES.rglob('*.class'):z.write(p,p.relative_to(CLASSES).as_posix())
 z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'port-feature-test','version':'1','environment':'client','entrypoints':{'client':['porttest.FeatureTest']}}))
cp=(BUILD/'probe/classpath.txt').read_text().replace(str(BUILD/'probe/minecraft-client-26.3-probe.jar'),str(BUILD/'minecraft-client-26.3.jar'))
cmd=[str(ROOT/'toolchain/jdk25/bin/java'),'-Xmx2G','-Dporttest.screenshots=true','-Dmixin.debug.countInjections=true','-cp',cp,'net.fabricmc.loader.impl.launch.knot.KnotClient','--username','StowTest','--version','26.3','--gameDir',str(GAME),'--assetsDir',str(GAME/'assets'),'--assetIndex','26','--uuid','00000000000000000000000000000000','--accessToken','0','--userType','msa','--width','1280','--height','960']
env=os.environ.copy();env.update(SDL_VIDEODRIVER='offscreen',SDL_AUDIODRIVER='dummy',LIBGL_ALWAYS_SOFTWARE='1')
print('Test game:',GAME,flush=True)
with (GAME/'feature-test.log').open('w') as log:r=subprocess.run(cmd,cwd=GAME,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=120)
print('Client exit:',r.returncode);print((GAME/'feature-result.txt').read_text() if (GAME/'feature-result.txt').exists() else 'No test result');raise SystemExit(r.returncode)
