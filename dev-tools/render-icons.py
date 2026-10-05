"""Render the pinned, unmodified MDI SVG paths into white GUI textures.

Requires Inkscape and Pillow for development only. Runtime is standard Minecraft
PNG loading; no additional mod, web request or SVG renderer is needed in game.
"""
from pathlib import Path
import json, subprocess, tempfile
from PIL import Image

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / 'design/icons/mdi'
TARGET = PROJECT / 'src/main/resources/assets/stow/textures/gui/icons'
TEXTURE_SIZE = 64

def main():
    spec = json.loads((SOURCE / 'icons.json').read_text())
    TARGET.mkdir(parents=True, exist_ok=True)
    names=set(spec['icons'].values())
    for old in TARGET.glob('*.png*'):
        if old.name.split('.png')[0] not in names:
            old.unlink()
    with tempfile.TemporaryDirectory(prefix='stow-icons-') as temp:
        for name in spec['icons'].values():
            rendered = Path(temp) / (name + '.png')
            subprocess.run(['inkscape', str(SOURCE / (name + '.svg')),
                '--export-type=png', '--export-background-opacity=0',
                '--export-width=' + str(TEXTURE_SIZE), '--export-height=' + str(TEXTURE_SIZE),
                '--export-filename=' + str(rendered)], check=True, capture_output=True)
            raw = Image.open(rendered).convert('RGBA')
            icon = Image.new('RGBA', raw.size, (255, 255, 255, 0))
            icon.putalpha(raw.getchannel('A'))
            assert icon.getchannel('A').getbbox(), name
            icon.save(TARGET / (name + '.png'))
            # Filtering matters when a 64px vector render is scaled to 16 GUI px.
            (TARGET / (name + '.png.mcmeta')).write_text('{"texture":{"blur":true,"clamp":true}}\n')
            print('Rendered', name)
    (TARGET / 'LICENSE').write_bytes((SOURCE / 'LICENSE').read_bytes())

if __name__ == '__main__':
    main()
