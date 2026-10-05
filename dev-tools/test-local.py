from pathlib import Path
import subprocess,zipfile,json,os,shutil,tempfile
ROOT=Path(__file__).resolve().parents[2];BUILD=ROOT/'manual-build';TEST=ROOT/'stow-tests';TEST.mkdir(exist_ok=True)
GAME=Path(tempfile.mkdtemp(prefix='game-v03-',dir=TEST));(TEST/'latest-game.txt').write_text(str(GAME))
(GAME/'mods').mkdir();(GAME/'config').mkdir();(GAME/'assets').mkdir()
release=Path(os.environ['STOW_TEST_JAR']) if os.environ.get('STOW_TEST_JAR') else ROOT/('stow/build/libs/stow-'+json.loads((ROOT/'stow/src/main/resources/fabric.mod.json').read_text())['version']+'.jar')
for path in [BUILD/'stow-dependencies/cloth-config-fabric-26.3.159.jar',release,BUILD/'probe/mods/fabric-api-0.161.0+26.3.jar',BUILD/'probe/mods/modmenu.jar']:shutil.copy2(path,GAME/'mods'/path.name)
shutil.copy2(BUILD/'probe/options.txt',GAME/'options.txt')
old=GAME/'config/mousewheelie-chest-memory';old.mkdir();(old/'import-fixture.json').write_text('{"version":4,"chests":[],"projects":[]}')
(GAME/'config/mousewheelie-pinned-slots.txt').write_text('35\n');(GAME/'config/mousewheelie.hjson').write_text('{ general: { "shift-drag-mode": "MATCHING_ONLY" } }')
cp=(ROOT/'stow/build/classpath.txt').read_text()+':'+str(ROOT/'stow/build/classes')
CLASSES=GAME/'test-classes';CLASSES.mkdir()
client_sources=[p for p in (ROOT/'stow/tests/porttest').glob('*.java') if p.name!='HarvestGameTest.java']
subprocess.run([str(ROOT/'toolchain/jdk25/bin/javac'),'-proc:none','--release','25','-cp',cp,'-d',str(CLASSES),*map(str,client_sources)],check=True)
with zipfile.ZipFile(GAME/'mods/port-feature-test.jar','w') as z:
 for p in CLASSES.rglob('*.class'):z.write(p,p.relative_to(CLASSES).as_posix())
 entry='porttest.StripWorldTest' if os.environ.get('STOW_STRIP_WORLD_TEST')=='1' else 'porttest.AutoToolWorldTest' if os.environ.get('STOW_AUTO_TOOL_WORLD_TEST')=='1' else 'porttest.BundleWorldTest' if os.environ.get('STOW_BUNDLE_WORLD_TEST')=='1' else 'porttest.CraftingWorldTest' if os.environ.get('STOW_CRAFTING_WORLD_TEST')=='1' else 'porttest.NativeWorldTest' if os.environ.get('STOW_NATIVE_WORLD_TEST')=='1' else 'porttest.FeatureTest'
 if entry=='porttest.StripWorldTest':
  z.writestr('strip-world-test.mixins.json',json.dumps({'required':True,'package':'porttest','compatibilityLevel':'JAVA_25','mixins':['StripServerDenyMixin'],'injectors':{'defaultRequire':1}}))
 z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'port-feature-test','version':'1','environment':'client','entrypoints':{'client':[entry]},'mixins':['strip-world-test.mixins.json'] if entry=='porttest.StripWorldTest' else []}))
cp=(BUILD/'probe/classpath.txt').read_text().replace(str(BUILD/'probe/minecraft-client-26.3-probe.jar'),str(BUILD/'minecraft-client-26.3.jar'))
cmd=[str(ROOT/'toolchain/jdk25/bin/java'),'-Xmx2G','-Dporttest.screenshots='+('false' if os.environ.get('STOW_TEST_SCREENSHOTS')=='0' else 'true'),'-Dmixin.debug.countInjections=true','-cp',cp,'net.fabricmc.loader.impl.launch.knot.KnotClient','--username','StowTest','--version','26.3','--gameDir',str(GAME),'--assetsDir',str(GAME/'assets'),'--assetIndex','26','--uuid','00000000000000000000000000000000','--accessToken','0','--userType','msa','--width','1280','--height','960']
env=os.environ.copy();env.update(SDL_VIDEODRIVER='offscreen',SDL_AUDIODRIVER='dummy',LIBGL_ALWAYS_SOFTWARE='1')
print('Test game:',GAME,flush=True)
with (GAME/'feature-test.log').open('w') as log:r=subprocess.run(cmd,cwd=GAME,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=240 if entry!='porttest.FeatureTest' else 120)
result=GAME/('strip-world-result.txt' if entry=='porttest.StripWorldTest' else 'auto-tool-world-result.txt' if entry=='porttest.AutoToolWorldTest' else 'bundle-world-result.txt' if entry=='porttest.BundleWorldTest' else 'crafting-world-result.txt' if entry=='porttest.CraftingWorldTest' else 'native-world-result.txt' if entry=='porttest.NativeWorldTest' else 'feature-result.txt');print('Client exit:',r.returncode);print(result.read_text() if result.exists() else 'No test result');raise SystemExit(r.returncode)
