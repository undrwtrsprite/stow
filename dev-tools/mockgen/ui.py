import os
"""Tiny pixel-UI renderer for Stow v2 mocks. Coordinates are Minecraft GUI pixels; S = output scale.
No Mojang assets: item sprites are procedural; font is Monocraft (OFL-1.1)."""
from PIL import Image, ImageDraw, ImageFont
import random, math
S = 2
FONT = ImageFont.truetype(os.environ.get('MONOCRAFT_TTF', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'fonts', 'Monocraft-ttf', 'Monocraft.ttf')), 8*S)
FONT_S = ImageFont.truetype(os.environ.get('MONOCRAFT_TTF', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'fonts', 'Monocraft-ttf', 'Monocraft.ttf')), 7*S)
# palette (vanilla-plus: mineral gray, cream, restrained gold)
BG0=(28,29,31); PANEL=(46,48,51); PANEL2=(58,61,64); RAISE=(70,73,77); EDGE_L=(96,100,105); EDGE_D=(20,21,23)
CREAM=(239,230,207); MUTED=(168,163,154); DIM=(120,117,110); GOLD=(201,165,72); GOLD_D=(140,112,45)
SAGE=(143,181,138); OCHRE=(217,164,65); CLAY=(199,112,95); SLOT=(139,139,139); SLOT_D=(55,55,55); SLOT_L=(255,255,255)
BTN=(108,108,108); BTN_T=(160,160,160); BTN_D=(40,40,40)

class Canvas:
    def __init__(s, w, h, bg=BG0):
        s.w, s.h = w, h
        s.im = Image.new('RGB', (w*S, h*S), bg)
        s.d = ImageDraw.Draw(s.im)
    def rect(s, x, y, w, h, c):
        if w<=0 or h<=0: return
        s.d.rectangle([x*S, y*S, (x+w)*S-1, (y+h)*S-1], fill=c)
    def outline(s, x, y, w, h, c, t=1):
        s.rect(x,y,w,t,c); s.rect(x,y+h-t,w,t,c); s.rect(x,y,t,h,c); s.rect(x+w-t,y,t,h,c)
    def text(s, x, y, txt, c=CREAM, small=False, anchor='la', maxw=None):
        f = FONT_S if small else FONT
        if maxw is not None:
            while txt and f.getlength(txt) > maxw*S:
                txt = txt[:-2] + '…' if len(txt) > 2 else ''
        # pixel drop shadow like vanilla
        s.d.text((x*S+S, y*S+S), txt, font=f, fill=tuple(int(v*0.25) for v in c), anchor=anchor)
        s.d.text((x*S, y*S), txt, font=f, fill=c, anchor=anchor)
        return f.getlength(txt)/S
    def tw(s, txt, small=False): return (FONT_S if small else FONT).getlength(txt)/S
    # --- chrome ---
    def backdrop(s, seed=1):
        r = random.Random(seed)
        for y in range(0, s.h, 2):
            for x in range(0, s.w, 2):
                g = 30 + int(10*math.sin(x/37.0)+8*math.cos(y/23.0)) + r.randint(-3,3)
                s.rect(x, y, 2, 2, (g+4, g+3, g))
        s.rect(0,0,s.w,s.h,None) if False else None
    def menu_panel(s, x, y, w, h, seed=3):
        r = random.Random(seed)
        s.rect(x, y, w, h, PANEL)
        for i in range(w*h//18):
            px, py = x + r.randrange(w), y + r.randrange(h)
            s.rect(px, py, 1, 1, (40,42,45) if r.random()<.6 else (53,55,58))
        s.rect(x, y, w, 1, EDGE_D); s.rect(x, y+h-1, w, 1, EDGE_L)
    def button(s, x, y, w, h, label, state='normal', icon=None, align='c', small=False):
        base = {'normal':BTN, 'hover':(126,126,126), 'disabled':(66,66,66), 'primary':(118,104,70)}[state if state!='focus' else 'normal']
        s.rect(x, y, w, h, BTN_D)
        s.rect(x+1, y+1, w-2, h-2, base)
        s.rect(x+1, y+1, w-2, 1, BTN_T if state!='disabled' else (90,90,90))
        s.rect(x+1, y+h-2, w-2, 1, (80,80,80) if state!='primary' else GOLD_D)
        if state in ('focus','primary'): s.outline(x, y, w, h, GOLD)
        col = CREAM if state!='disabled' else DIM
        ix = x + 4
        if icon:
            icon_draw(s, icon, x+3, y+(h-16)//2 if h>=18 else y+(h-16)//2); ix = x + 22
        if align == 'c' and not icon:
            s.text(x + w/2, y + h/2 + 0.5, label, col, small=small, anchor='mm', maxw=w-6)
        else:
            s.text(ix, y + h/2 + 0.5, label, col, small=small, anchor='lm', maxw=w-(ix-x)-3)
    def field(s, x, y, w, h, value='', hint='', focused=False, icon=None):
        s.rect(x, y, w, h, GOLD if focused else (160,160,160))
        s.rect(x+1, y+1, w-2, h-2, (12,12,13))
        tx = x + 5
        if icon: icon_draw(s, icon, x+3, y+(h-16)//2); tx = x + 22
        if value:
            tw_ = s.text(tx, y + h/2 + .5, value, CREAM, anchor='lm', maxw=w-(tx-x)-6)
            if focused: s.rect(tx + tw_ + 1, y + 4, 1, h - 8, CREAM)
        else:
            s.text(tx, y + h/2 + .5, hint, DIM, anchor='lm', maxw=w-(tx-x)-6)
            if focused: s.rect(tx, y + 4, 1, h - 8, CREAM)
    def chip(s, x, y, label, selected=False, count=None, small=True):
        t = label + (f' {count}' if count is not None else '')
        w = int(s.tw(t, small)) + 10; h = 13
        s.rect(x, y, w, h, (86,74,44) if selected else PANEL2)
        s.outline(x, y, w, h, GOLD if selected else EDGE_L)
        s.text(x + 5, y + 7, t, CREAM if selected else MUTED, small=small, anchor='lm')
        return w
    def segment(s, x, y, w, h, label, selected=False, badge=None, icon=None):
        s.rect(x, y, w, h, PANEL2 if selected else (38,40,43))
        s.outline(x, y, w, h, EDGE_L if selected else (30,31,33))
        if selected: s.rect(x+1, y+h-3, w-2, 2, GOLD)
        tx = x + w/2
        lab = label + (f'  {badge}' if badge else '')
        if icon:
            tot = 18 + s.tw(lab)
            icon_draw(s, icon, int(x + (w-tot)/2), y + (h-16)//2 - 1)
            s.text(x + (w-tot)/2 + 18, y + h/2, lab, CREAM if selected else MUTED, anchor='lm', maxw=w-24)
        else:
            s.text(tx, y + h/2, lab, CREAM if selected else MUTED, anchor='mm', maxw=w-6)
    def slot(s, x, y, item=None, count=None, hl=None):
        s.rect(x, y, 18, 18, SLOT); s.rect(x, y, 17, 1, SLOT_D); s.rect(x, y, 1, 17, SLOT_D)
        s.rect(x+1, y+17, 17, 1, SLOT_L); s.rect(x+17, y+1, 1, 17, SLOT_L)
        if hl: s.rect(x+1, y+1, 16, 16, hl)
        if item: icon_draw(s, item, x+1, y+1)
        if count and count > 1:
            t = str(count); s.text(x + 18 - s.tw(t, True) - 0.5, y + 10, t, (255,255,255), small=True)
    def scrollbar(s, x, y, h, frac, pos):
        s.rect(x, y, 2, h, (56,60,63)); th = max(10, int(h*frac)); ty = y + int((h-th)*pos)
        s.rect(x, ty, 2, th, GOLD)
    def marker(s, x, y, h): s.rect(x, y, 2, h, GOLD)
    def bar(s, x, y, w, frac, c=GOLD):
        s.rect(x, y, w, 2, (30,31,33)); s.rect(x, y, max(1,int(w*min(1,frac))), 2, c)
    def tooltip(s, x, y, lines):
        w = int(max(s.tw(t if isinstance(t,str) else t[0], True) for t in lines)) + 8; h = 4 + 9*len(lines)
        s.rect(x, y, w, h, (16,0,16)); s.outline(x, y, w, h, (80,0,255)); s.outline(x+1,y+1,w-2,h-2,(40,0,127))
        for i, t in enumerate(lines):
            txt, col = (t, CREAM) if isinstance(t, str) else t
            s.text(x+4, y+3+9*i + 4, txt, col, small=True, anchor='lm')
    def save(s, path): s.im.save(path); return path

# ---------------- procedural item sprites (16x16) ----------------
def _noise(seed):
    r = random.Random(seed); return [[r.random() for _ in range(16)] for _ in range(16)]
def tex(kind):
    n = _noise(hash(kind) & 0xffff)
    def f(u, v):
        u, v = int(u) % 16, int(v) % 16; z = n[v][u]
        if kind == 'stone': g = 125 + int(z*25) - (20 if z < .12 else 0); return (g, g, g)
        if kind == 'cobble':
            g = 110 + int(z*40) - (35 if (u*7+v*3) % 11 == 0 or (u+v*5) % 13 == 0 else 0); return (g, g, g)
        if kind == 'bricks':
            mortar = v % 4 == 3 or (u + (8 if (v//4) % 2 else 0)) % 8 == 7
            g = 90 if mortar else 128 + int(z*18); return (g, g, g+2)
        if kind == 'planks':
            line = v % 4 == 3; b = 0.7 if line else 1.0
            return (int((160+z*20)*b), int((128+z*14)*b), int((80+z*10)*b))
        if kind == 'spruce':
            line = v % 4 == 3; b = 0.7 if line else 1.0
            return (int((104+z*16)*b), int((76+z*10)*b), int((46+z*8)*b))
        if kind == 'log_side': b = 0.75 if u % 3 == 0 else 1.0; return (int((100+z*20)*b), int((78+z*14)*b), int((46+z*8)*b))
        if kind == 'log_top':
            d = math.hypot(u-7.5, v-7.5); ring = int(d) % 3 == 0
            if d > 6.8: return (96, 74, 44)
            return (176 if ring else 190, 146 if ring else 158, 92 if ring else 104)
        if kind == 'glass':
            edge = u in (0, 15) or v in (0, 15); glint = (u - v) in (3, 4) and 3 < u < 10
            return (220, 240, 245) if edge or glint else None
        if kind == 'deepslate': g = 62 + int(z*18) - (14 if v % 4 == 0 or u % 4 == 0 else 0); return (g, g, g+6)
        if kind == 'chest_top':
            if u in (0, 15) or v in (0, 15): return (60, 40, 18)
            return (156, 110, 50) if v % 5 else (120, 82, 36)
        if kind == 'chest_side':
            if u in (0, 15) or v in (0, 15) or v == 6: return (60, 40, 18)
            if 6 <= u <= 9 and 5 <= v <= 9: return (200, 200, 200)
            return (156, 110, 50) if u % 5 else (130, 90, 40)
        if kind == 'barrel_side':
            if v in (2, 3, 12, 13): return (70, 70, 70)
            return (int(140+z*14), int(100+z*10), int(56+z*6)) if u % 4 else (100, 70, 38)
        if kind == 'barrel_top':
            if u in (0, 15) or v in (0, 15): return (70, 70, 70)
            return (120, 86, 46) if 5 <= u <= 10 and 5 <= v <= 10 else (150, 108, 58)
        if kind == 'shulker':
            if v in (7, 8): return (90, 60, 110)
            return (int(150+z*20), int(110+z*14), int(170+z*14))
        if kind == 'hopper': return (70, 70, 72) if (u in (0, 15) or v in (0, 15)) else (54, 54, 56)
        return (255, 0, 255)
    return f

def cube(top, left, right=None):
    right = right or left
    im = Image.new('RGBA', (16, 16), (0,0,0,0)); px = im.load()
    for py in range(16):
        for pxl in range(16):
            x, y = pxl + .5, py + .5
            u, v = x - 8 + 2*y, 2*y - (x - 8)
            c, sh = None, 1.0
            if 0 <= u < 16 and 0 <= v < 16 and y < 8.5: c = top(u, v); sh = 1.0
            elif x < 8:
                sx, t = x*2, 2*(y - 4 - x/2)
                if 0 <= t < 16: c = left(sx, t); sh = .78
            else:
                sx, t = (x-8)*2, 2*(y - 8 + (x-8)/2)
                if 0 <= t < 16: c = right(sx, t); sh = .6
            if c: px[pxl, py] = (int(c[0]*sh), int(c[1]*sh), int(c[2]*sh), 255)
    return im

def flat(kind):
    im = Image.new('RGBA', (16, 16), (0,0,0,0)); d = ImageDraw.Draw(im)
    if kind in ('iron_ingot', 'gold_ingot'):
        c1, c2 = ((230,230,230), (150,150,150)) if kind == 'iron_ingot' else ((250,220,90), (190,140,30))
        d.polygon([(2,10),(6,6),(14,6),(10,10)], fill=c1); d.polygon([(2,10),(10,10),(10,12),(2,12)], fill=c2); d.polygon([(10,10),(14,6),(14,8),(10,12)], fill=tuple(int(v*.8) for v in c2))
    elif kind == 'diamond':
        d.polygon([(8,2),(14,7),(8,14),(2,7)], fill=(90,230,220)); d.polygon([(8,2),(11,7),(8,14),(5,7)], fill=(170,250,245)); d.point((6,5), fill=(255,255,255))
    elif kind == 'redstone':
        for (x, y) in [(4,6),(7,4),(10,7),(6,9),(9,11),(12,10),(5,12),(8,7)]: d.rectangle([x,y,x+1,y+1], fill=(200,20,20))
    elif kind == 'torch':
        d.rectangle([7,6,8,14], fill=(120,86,40)); d.rectangle([6,3,9,6], fill=(255,210,80)); d.rectangle([7,2,8,3], fill=(255,250,200))
    elif kind == 'paper':
        d.rectangle([3,2,12,14], fill=(235,235,225)); [d.line([5,5+i*3,10,5+i*3], fill=(150,150,150)) for i in range(3)]
    elif kind == 'compass':
        d.ellipse([2,2,13,13], fill=(150,150,150)); d.ellipse([4,4,11,11], fill=(220,220,210)); d.line([8,8,10,4], fill=(200,30,30), width=2)
    elif kind == 'gear':
        d.ellipse([3,3,12,12], outline=(220,220,220), width=2); [d.rectangle([7+int(5*math.cos(a))-1,7+int(5*math.sin(a))-1,7+int(5*math.cos(a))+1,7+int(5*math.sin(a))+1], fill=(220,220,220)) for a in [i*math.pi/4 for i in range(8)]]
    elif kind == 'magnify':
        d.ellipse([2,2,10,10], outline=(230,230,230), width=2); d.line([9,9,14,14], fill=(230,230,230), width=2)
    elif kind == 'star':
        pts = [(8+6*math.cos(math.pi/2+i*math.pi/5*2*1)*(1 if i%2==0 else .45), 8-6*math.sin(math.pi/2+i*math.pi/5)*(1 if i%2==0 else .45)) for i in range(10)]
        pts = [(8+(6 if i%2==0 else 2.6)*math.cos(-math.pi/2+i*math.pi/5), 8+(6 if i%2==0 else 2.6)*math.sin(-math.pi/2+i*math.pi/5)) for i in range(10)]
        d.polygon(pts, fill=GOLD)
    elif kind == 'star_off':
        pts = [(8+(6 if i%2==0 else 2.6)*math.cos(-math.pi/2+i*math.pi/5), 8+(6 if i%2==0 else 2.6)*math.sin(-math.pi/2+i*math.pi/5)) for i in range(10)]
        d.polygon(pts, outline=(150,150,150))
    elif kind == 'stow':
        d.rectangle([2,5,13,13], fill=(156,110,50)); d.rectangle([2,3,13,6], fill=(186,140,70)); d.rectangle([6,7,9,9], fill=GOLD)
    elif kind == 'sort':
        for i, w in enumerate([10, 7, 4]): d.rectangle([3, 3+i*4, 3+w, 4+i*4], fill=(230,230,230))
    elif kind == 'help':
        d.ellipse([2,2,13,13], outline=(230,230,230), width=1); d.text((5,2), '?', fill=(230,230,230))
    elif kind == 'eye':
        d.ellipse([2,5,13,11], outline=(230,230,230)); d.ellipse([6,6,9,9], fill=(230,230,230))
    elif kind == 'undo':
        d.arc([3,4,13,13], 180, 360+90, fill=(230,230,230), width=2); d.polygon([(1,8),(5,8),(3,5)], fill=(230,230,230))
    return im

SPRITES = {}
def sprite(kind):
    if kind in SPRITES: return SPRITES[kind]
    cubes = {'stone':('stone',), 'cobblestone':('cobble',), 'stone_bricks':('bricks',), 'oak_planks':('planks',), 'spruce_planks':('spruce',),
             'oak_log':('log_top','log_side'), 'glass':('glass',), 'deepslate_tiles':('deepslate',), 'chest':('chest_top','chest_side'),
             'barrel':('barrel_top','barrel_side'), 'shulker':('shulker',), 'hopper':('hopper',)}
    if kind in cubes:
        k = cubes[kind]; im = cube(tex(k[0]), tex(k[-1]))
    else: im = flat(kind)
    SPRITES[kind] = im; return im

def icon_draw(c, kind, x, y, size=16):
    im = sprite(kind).resize((size*S, size*S), Image.NEAREST)
    c.im.paste(im, (int(x*S), int(y*S)), im)

# Minecraft default font advance widths (GUI px incl. 1px spacing), for EN/DE length checks
NARROW = {'i':2,'l':3,'!':2,'.':2,',':2,"'":2,':':2,';':2,'|':2,'t':4,'I':4,'[':4,']':4,' ':4,'f':5,'k':5,'<':5,'>':5,'(':5,')':5,'{':5,'}':5,'*':5,'"':5,'`':3,'@':7,'~':7}
def mcw(t): return sum(NARROW.get(ch, 6) for ch in t) - (1 if t else 0)
