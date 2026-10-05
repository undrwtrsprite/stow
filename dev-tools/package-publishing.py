"""Package the already-built client and companion with upload text and SHA-256 checksums."""
from pathlib import Path
import hashlib, json, shutil, zipfile

root=Path(__file__).resolve().parents[1]
version=json.loads((root/'src/main/resources/fabric.mod.json').read_text())['version']
out=root/'build/publishing'/version
out.mkdir(parents=True,exist_ok=True)
names=[f'stow-{version}.jar',f'stow-companion-{version}.jar']
for name in names:
 source=root/'build/libs'/name
 with zipfile.ZipFile(source) as jar:
  metadata=json.loads(jar.read('fabric.mod.json'))
  expected_id='stow_companion' if name.startswith('stow-companion-') else 'stow'
  if metadata['id']!=expected_id or metadata['version']!=version:
   raise SystemExit('Artifact identity/version mismatch: '+name)
 shutil.copy2(source,out/name)
descriptions=['STOW-DESCRIPTION.md','COMPANION-DESCRIPTION.md','UPLOAD-GUIDE.md','VERSION-CHANGELOG.md']
for name in descriptions:shutil.copy2(root/'docs/publishing'/name,out/name)
checksums=''.join(hashlib.sha256((out/name).read_bytes()).hexdigest()+'  '+name+'\n' for name in names)
(out/'SHA256SUMS.txt').write_text(checksums,encoding='utf-8')
archive=out/f'stow-upload-kit-{version}.zip'
with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED) as z:
 for name in names+descriptions+['SHA256SUMS.txt']:z.write(out/name,name)
with zipfile.ZipFile(archive) as z:
 if z.testzip() is not None:raise SystemExit('Invalid upload package')
print('Prepared',out)
print(checksums,end='')
