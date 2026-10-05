"""Check public project metadata and assets without downloading dependencies."""
from pathlib import Path
import json,re,zipfile

ROOT=Path(__file__).resolve().parents[1]
RESOURCES=ROOT/'src/main/resources'

def require(condition,message):
    if not condition:
        raise SystemExit('FAIL: '+message)

meta=json.loads((RESOURCES/'fabric.mod.json').read_text(encoding='utf-8'))
gradle=(ROOT/'build.gradle').read_text(encoding='utf-8')
version=re.search(r"^version\s*=\s*'([^']+)'",gradle,re.M)
require(version is not None and version[1]==meta['version'],'Gradle and Fabric versions differ')
require(meta['environment']=='client','stow must remain client-only')
companion=json.loads((ROOT/'server-resources/fabric.mod.json').read_text(encoding='utf-8'))
require(companion['version']==meta['version'],'companion and client versions differ')
require(companion['id']=='stow_companion' and companion['environment']=='*','invalid companion mod identity/environment')
require(set(companion['depends'])=={'minecraft','fabricloader','fabric-api','java'},'companion must not depend on client UI libraries')
for dependency in companion['depends']:
    require(companion['depends'][dependency]==meta['depends'][dependency],'companion dependency differs: '+dependency)
for entrypoint in companion['entrypoints']['main']:
    require((ROOT/'src/main/java'/Path(entrypoint.replace('.','/')+'.java')).is_file(),'missing companion entrypoint')
require(meta['license']=='Apache-2.0','upstream license must be retained')
require((RESOURCES/meta['icon']).is_file(),'mod icon is missing')
require('net.fabricmc.fabric-loom' in gradle,'unobfuscated Minecraft requires the non-remapping Loom plugin')
require('JavaLanguageVersion.of(25)' in gradle,'Java 25 toolchain is missing')
for dependency,coordinate in [('minecraft','com.mojang:minecraft:'),('fabricloader','net.fabricmc:fabric-loader:'),('fabric-api','net.fabricmc.fabric-api:fabric-api:'),('cloth-config','me.shedaniel.cloth:cloth-config-fabric:')]:
    pinned=meta['depends'][dependency].removeprefix('>=')
    require(coordinate+pinned in gradle,dependency+' metadata does not match the pinned build dependency')
lang=RESOURCES/'assets/stow/lang'
english=json.loads((lang/'en_us.json').read_text(encoding='utf-8'))
german=json.loads((lang/'de_de.json').read_text(encoding='utf-8'))
require(english.keys()==german.keys(),'English/German translation keys differ')
for key in english:
    require(len(re.findall(r'%(?:\d+\$)?[sd]',english[key]))==len(re.findall(r'%(?:\d+\$)?[sd]',german[key])),key+' formatting placeholders differ')
for config in meta['mixins']:
    require((RESOURCES/config).is_file(),'mixin configuration is missing: '+config)
for relative in ['LICENSE','NOTICE','gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.properties','gradle/wrapper/gradle-wrapper.jar','design/icons/mdi/LICENSE','src/main/resources/assets/stow/textures/gui/icons/LICENSE']:
    require((ROOT/relative).is_file(),'required project file missing: '+relative)
with zipfile.ZipFile(ROOT/'gradle/wrapper/gradle-wrapper.jar') as jar:
    require(jar.testzip() is None and 'org/gradle/wrapper/GradleWrapperMain.class' in jar.namelist(),'invalid Gradle wrapper JAR')
require('gradle-9.6.0-bin.zip' in (ROOT/'gradle/wrapper/gradle-wrapper.properties').read_text(encoding='utf-8'),'unexpected Gradle distribution')
for entrypoint in meta['entrypoints'].values():
    for class_name in entrypoint:
        require((ROOT/'src/main/java'/Path(class_name.replace('.','/')+'.java')).is_file(),'missing entrypoint: '+class_name)
print('PASS: stow '+meta['version']+', '+str(len(english))+' bilingual translation keys, metadata, licenses and wrapper layout')
for name,is_server in [('stow',False),('stow-companion',True)]:
    path=ROOT/f'build/libs/{name}-{meta["version"]}.jar'
    if not path.exists():continue
    with zipfile.ZipFile(path) as jar:
        require(jar.testzip() is None,'invalid artifact: '+name)
        packaged=json.loads(jar.read('fabric.mod.json'))
        require(packaged==(companion if is_server else meta),'artifact metadata mismatch: '+name)
        files=jar.namelist()
        require('dev/stow/stripping/protocol/OakHarvestProtocol.class' in files,'missing shared protocol: '+name)
        if is_server:
            require('dev/stow/stripping/server/OakHarvestServer.class' in files,'missing companion server')
            require(all(not f.endswith('.class') or f.startswith('dev/stow/stripping/') for f in files),'client code bundled in companion')
        else:
            require(all(not f.startswith('dev/stow/stripping/server/') for f in files),'server implementation bundled in client')
print('PASS: companion metadata and available client/server artifact separation')
