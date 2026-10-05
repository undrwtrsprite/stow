"""Run companion regressions on a headless Fabric GameTest server using Gradle's cached dependencies.

First run gradlew prepareHarvestTestClasspath build. Requires JAVA_HOME pointing to JDK 25.
Uses a fresh, disposable world under build; does not connect to a player's server.
"""
from pathlib import Path
import json, os, shutil, subprocess, tempfile, zipfile, xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
cp=(root/'build/harvest-test-classpath.txt').read_text().strip()
paths=[Path(p) for p in cp.split(os.pathsep)]
jdk=Path(os.environ['JAVA_HOME'])/'bin'
suffix='.exe' if os.name=='nt' else ''
version=json.loads((root/'server-resources/fabric.mod.json').read_text())['version']
game=Path(tempfile.mkdtemp(prefix='harvest-server-',dir=root/'build'))
(game/'mods').mkdir()
api=next(p for p in paths if p.name.startswith('fabric-api-'))
mc=next(p for p in paths if p.name.startswith('minecraft-'))
gametest=next(p for p in paths if p.name.startswith('fabric-gametest-api-v1-'))
for p in [api,gametest,root/f'build/libs/stow-companion-{version}.jar']:
 shutil.copy2(p,game/'mods'/p.name)
classes=game/'test-classes'
subprocess.run([str(jdk/('javac'+suffix)),'-proc:none','--release','25','-cp',cp,'-d',str(classes),
 str(root/'tests/porttest/HarvestGameTest.java')],check=True)
with zipfile.ZipFile(game/'mods/harvest-test.jar','w') as z:
 for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes).as_posix())
 z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'stow-harvest-test','version':'1',
  'environment':'*','entrypoints':{'fabric-gametest':['porttest.HarvestGameTest']}}))
# Exercise the packaged server artifact, not loose production classes or client stow.
runtime=[p for p in paths if p.suffix=='.jar' and not p.name.startswith(('fabric-api-','fabric-','cloth-','modmenu-','basic-math-'))]
runtime+=[p for p in paths if p.name.startswith('fabric-loader-')]
(game/'server.properties').write_text('online-mode=false\nlevel-type=minecraft:flat\nspawn-protection=0\n')
args=['-Xmx2G','--enable-native-access=ALL-UNNAMED','-Dfabric-api.gametest','-Dfabric-api.gametest.report-file='+str(game/'results.xml'),
 '-Dfabric.gameJarPath='+str(mc),'-cp',os.pathsep.join(map(str,runtime)),
 'net.fabricmc.loader.impl.launch.knot.KnotServer','nogui']
# An argument file avoids Windows command-line length limits.
argfile=game/'java-args.txt'
argfile.write_text('\n'.join('"'+v.replace('\\','/').replace('"','\\"')+'"' for v in args))
print('Test world:',game,flush=True)
with (game/'server.log').open('w') as log:
 result=subprocess.run([str(jdk/('java'+suffix)),'@'+str(argfile)],cwd=game,stdout=log,stderr=subprocess.STDOUT,timeout=240)
print((game/'server.log').read_text(errors='replace')[-14000:])
report=game/'results.xml'
if not report.exists():raise SystemExit('FAIL: server did not produce a test report')
print(report.read_text())
tree=ET.parse(report)
if not any(t.get('name')=='stow-harvest-test:harvest_game_test_harvest' for t in tree.iter('testcase')):
 raise SystemExit('FAIL: companion test was not discovered')
if any(True for t in tree.iter() if t.tag in ('failure','error')):
 raise SystemExit('FAIL: server regression failed')
raise SystemExit(result.returncode)
