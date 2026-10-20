from ui import *
OUT = '/workspace/stow-redesign/docs/research/img/'
import os; os.makedirs(OUT, exist_ok=True)

def header(c, query='', hint='Find items, containers, projects or actions…', seg=0, compact=False, x0=None, w=None):
    x0 = 20 if x0 is None else x0; w = c.w - 40 if w is None else w
    c.text(c.w/2, 11, 'Stow', CREAM, anchor='mm')
    c.button(x0 + w - 20, 3, 20, 16, '', icon='gear')
    c.button(x0 + w - 42, 3, 20, 16, '', icon='help')
    c.text(x0, 11, 'Castle walls · 12 chests' if not compact else 'Castle walls', MUTED, small=True, anchor='lm')
    c.field(x0, 22, w, 18, query, hint, focused=True, icon='magnify')
    segs = [('Find', None, 'magnify'), ('Project', '8 missing', 'star'), ('Containers', '40', 'chest')]
    if compact: segs = [('Find', None, None), ('Project', '8', None), ('Boxes', '40', None)]
    sw = w // 3
    for i, (l, b, ic) in enumerate(segs):
        c.segment(x0 + i*sw, 44, sw - (0 if i < 2 else 0) - 1, 18, l, i == seg, b, ic)

def footer(c, hints, left=('Done',), undo=None, rng=None, x0=20, w=None):
    w = c.w - 40 if w is None else w; y = c.h - 24
    c.button(x0, y, 60, 18, left[0])
    bx = x0 + 64
    if undo: c.button(bx, y, 120, 18, undo, icon='undo'); bx += 124
    c.text(bx + 4, y + 9, hints, MUTED, small=True, anchor='lm', maxw=x0 + w - bx - (60 if rng else 4))
    if rng: c.text(x0 + w, y + 9, rng, DIM, small=True, anchor='rm')

def group(c, x, y, w, label):
    c.text(x + 4, y + 6, label.upper(), GOLD, small=True, anchor='lm'); c.rect(x + 4 + c.tw(label.upper(), True) + 4, y + 6, w - c.tw(label.upper(), True) - 14, 1, (70,72,75))
    return y + 12

def row(c, x, y, w, icon, name, right='', sub='', sel=False, rc=MUTED, h=20, subc=DIM):
    if sel: c.rect(x, y, w, h, (66,62,52)); c.marker(x, y, h)
    icon_draw(c, icon, x + 5, y + (h-16)//2)
    if sub:
        c.text(x + 25, y + 6, name, CREAM, anchor='lm', maxw=w - 30 - c.tw(right, True))
        c.text(x + 25, y + 15, sub, subc, small=True, anchor='lm', maxw=w - 30)
    else:
        c.text(x + 25, y + h/2, name, CREAM, anchor='lm', maxw=w - 34 - c.tw(right, True))
    if right: c.text(x + w - 5, y + 6 if sub else y + h/2, right, rc, small=True, anchor='rm')
    return y + h

# ---------------- 1. Find, wide ----------------
def find_wide():
    c = Canvas(640, 360); c.backdrop(); header(c, 'stone')
    c.menu_panel(20, 66, 600, 266)
    L = 20; lw = 320; y = 70
    y = group(c, L, y, lw, 'Items · 4')
    y = row(c, L+2, y, lw-8, 'stone', 'Stone', '3,352 known', '40 sources · nearest 6 m · seen 2 d ago', sel=True, h=24)
    y = row(c, L+2, y, lw-8, 'stone_bricks', 'Stone Bricks', '1,120', '9 sources · 14 m · today', h=24)
    y = row(c, L+2, y, lw-8, 'cobblestone', 'Cobblestone', '6,804', '22 sources · 6 m · today', h=24)
    y = row(c, L+2, y, lw-8, 'deepslate_tiles', 'Deepslate Tiles', '0 stored', 'Missing for Castle walls: 512', h=24, subc=OCHRE)
    y = group(c, L, y + 2, lw, 'Containers · 2')
    y = row(c, L+2, y, lw-8, 'chest', 'Stone chest', '6 m', '"stone" in name · 1,728 Stone', h=24)
    y = row(c, L+2, y, lw-8, 'barrel', 'Stonecutter barrel', '11 m', '"stone" in name · 3 kinds', h=24)
    y = group(c, L, y + 2, lw, 'Actions · 1')
    row(c, L+2, y, lw-8, 'sort', 'Sort inventory by name', 'Alt+S')
    c.scrollbar(L + lw - 4, 82, 240, .7, 0)
    # detail
    D = 20 + lw + 6; dw = 600 - lw - 10
    c.rect(D - 3, 70, 1, 258, (70,72,75))
    icon_draw(c, 'stone', D + 4, 74, 32)
    c.text(D + 42, 80, 'Stone', CREAM); c.text(D + 42, 92, 'minecraft:stone', DIM, small=True)
    cols = [('Carried', '12'), ('Stored', '3,340'), ('Known total', '3,352')]
    for i, (k, v) in enumerate(cols):
        cx = D + 4 + i*90; c.text(cx, 116, k, MUTED, small=True); c.text(cx, 128, v, CREAM)
    c.rect(D + 4, 140, dw - 12, 22, (40,42,45)); c.outline(D + 4, 140, dw - 12, 22, (70,72,75))
    icon_draw(c, 'star', D + 8, 143)
    c.text(D + 28, 151, 'Castle walls: need 4,000 · missing 648', OCHRE, small=True, anchor='lm')
    c.text(D + 4, 172, 'SOURCES · NEAREST FIRST', GOLD, small=True)
    srcs = [('chest', 'Stone chest', '1,728 · 6 m · 2 d'), ('barrel', 'Quarry barrel', '960 · 18 m · today'), ('chest', 'x 120, y 64, z -33', '512 · 31 m · 5 d'), ('shulker', 'in Purple shulker', '140 · 31 m · 5 d')]
    yy = 182
    for ic, n, r in srcs:
        icon_draw(c, ic, D + 4, yy); c.text(D + 24, yy + 8, n, CREAM, anchor='lm', maxw=130); c.text(D + dw - 52, yy + 8, r, MUTED, small=True, anchor='rm')
        c.button(D + dw - 48, yy, 40, 16, 'Show', small=True); yy += 19
    c.text(D + 4, yy + 4, '+ 36 more · 12 not seen for 5+ days', DIM, small=True)
    by = 300
    c.button(D + 4, by, 116, 20, 'Show nearest (Enter)', 'primary')
    c.button(D + 124, by, 92, 20, 'Add to project')
    c.button(D + 220, by, 46, 20, 'Keep…')
    footer(c, '↑↓ select · Enter show & close · Shift+Enter show · Tab actions · Esc close', rng='1–7 of 7')
    return c.save(OUT + '2026-10-20-find-wide.png')

# ---------------- 2. Find, compact (pushed detail) ----------------
def find_compact():
    c = Canvas(360, 240); c.backdrop()
    # compact detail view
    c.text(c.w/2, 11, 'Stow', CREAM, anchor='mm'); c.button(318, 3, 20, 16, '', icon='gear')
    c.button(10, 3, 54, 16, '‹ Back', small=True)
    c.field(10, 22, 340, 18, 'stone', icon='magnify', focused=False)
    c.menu_panel(10, 44, 340, 170)
    icon_draw(c, 'stone', 16, 50, 16); c.text(36, 58, 'Stone', CREAM, anchor='lm'); c.text(344, 58, '3,352 known', MUTED, small=True, anchor='rm')
    c.text(16, 74, 'Carried 12 · Stored 3,340', MUTED, small=True)
    c.text(16, 86, 'Castle walls: missing 648', OCHRE, small=True)
    yy = 98
    for ic, n, r in [('chest', 'Stone chest', '1,728 · 6 m'), ('barrel', 'Quarry barrel', '960 · 18 m'), ('chest', 'x 120, y 64, z -33', '512 · 31 m')]:
        c.rect(14, yy, 332, 20, (40,42,45) if yy == 98 else PANEL)
        if yy == 98: c.marker(14, yy, 20)
        icon_draw(c, ic, 18, yy + 2); c.text(38, yy + 10, n, CREAM, anchor='lm', maxw=170); c.text(342, yy + 10, r, MUTED, small=True, anchor='rm'); yy += 21
    c.text(16, yy + 4, '+ 37 more sources', DIM, small=True)
    c.button(14, 188, 120, 20, 'Show nearest (Enter)', 'primary'); c.button(138, 188, 100, 20, 'Add to project'); c.button(242, 188, 104, 20, 'More…')
    c.text(10, 226, 'Backspace back · Enter show · Tab more', MUTED, small=True, anchor='lm')
    return c.save(OUT + '2026-10-20-find-compact.png')

# ---------------- 3. Find empty state ("Now") ----------------
def find_empty():
    c = Canvas(640, 360); c.backdrop(); header(c, '')
    c.menu_panel(20, 66, 600, 266)
    # left: project + recent
    L = 24
    c.text(L, 74, 'ACTIVE PROJECT', GOLD, small=True)
    c.rect(L, 84, 290, 46, (40,42,45)); c.outline(L, 84, 290, 46, (70,72,75))
    icon_draw(c, 'star', L + 4, 88); c.text(L + 24, 96, 'Castle walls', CREAM, anchor='lm'); c.text(L + 284, 96, '16 / 24 ready', SAGE, small=True, anchor='rm')
    c.bar(L + 4, 108, 280, 16/24, SAGE); c.text(L + 4, 120, 'Missing: Deepslate Tiles 512 · Stone 648 · +6', OCHRE, small=True, anchor='lm', maxw=280)
    c.text(L, 140, 'RECENT', GOLD, small=True)
    y = 150
    for ic, n, r in [('stone', 'Stone', 'found 2 min ago'), ('oak_log', 'Oak Log', 'tracked'), ('chest', 'Stone chest', 'renamed'), ('glass', 'Glass', 'added to Castle walls')]:
        y = row(c, L, y, 290, ic, n, r)
    c.text(L, y + 6, 'STALE STORAGE', GOLD, small=True)
    y = row(c, L, y + 16, 290, 'barrel', 'Quarry barrels (6)', 'not seen 9 d', rc=OCHRE)
    # right: quick toggles
    R = 330
    c.text(R, 74, 'QUICK TOGGLES', GOLD, small=True)
    tg = [('Chest glow', 'On', ''), ('Auto tool', 'On', 'Alt+T'), ('Automatic attack', 'Off', 'Alt+A'), ('Auto-mending', 'Off', 'Alt+M'), ('Drag: matching stacks', '>', '')]
    y = 84
    for n, v, k in tg:
        c.rect(R, y, 284, 20, (40,42,45)); c.text(R + 6, y + 10, n, CREAM, anchor='lm', maxw=180)
        if k: c.text(R + 230, y + 10, k, DIM, small=True, anchor='rm')
        c.button(R + 236, y + 2, 44, 16, v, small=True); y += 22
    c.text(R, y + 8, 'TRY', GOLD, small=True)
    c.text(R, y + 20, 'need 64 glass  → add to project', MUTED, small=True)
    c.text(R, y + 32, '>sort  → run an action', MUTED, small=True)
    c.text(R, y + 44, '#castle  → open a project', MUTED, small=True)
    c.text(R, y + 56, '@quarry  → find a container', MUTED, small=True)
    footer(c, 'Type to search · ↑ history · Tab to the lists · Ctrl+1/2/3 switch views')
    return c.save(OUT + '2026-10-20-find-empty.png')

# ---------------- 4. Project wide ----------------
def project_wide():
    c = Canvas(640, 360); c.backdrop(); header(c, '', 'Filter or add: "need 64 glass"…', seg=1)
    c.menu_panel(20, 66, 600, 266)
    L = 24
    c.button(L, 70, 130, 18, 'Castle walls ▼', align='l', icon='star')
    c.text(L + 136, 79, '16 / 24 ready · 8 missing', MUTED, small=True, anchor='lm')
    c.button(L + 250, 70, 110, 18, 'Storage: 12 chests ▼', small=True)
    c.button(520, 70, 46, 18, '+ Add', small=True); c.button(568, 70, 48, 18, 'Import', small=True)
    x = L
    for i, (l, n) in enumerate([('Missing', 8), ('All', 24), ('Done', 16)]): x += c.chip(x, 92, l, i == 0, n) + 4
    # table
    T = L; tw = 380; y = 110
    cols = [('Item', T + 25, 'l'), ('Need', T + 200, 'r'), ('Have', T + 250, 'r'), ('Missing', T + 305, 'r'), ('Src', T + 340, 'r'), ('HUD', T + 368, 'm')]
    for n, cx, a in cols: c.text(cx, y + 5, n, GOLD, small=True, anchor={'l':'lm','r':'rm','m':'mm'}[a])
    c.rect(T, y + 11, tw, 1, (70,72,75)); y += 14
    data = [('deepslate_tiles', 'Deepslate Tiles', 512, 0, 0, True), ('stone', 'Stone', 4000, 3352, 40, True), ('glass', 'Glass', 256, 96, 3, False), ('oak_log', 'Oak Log', 192, 150, 5, False),
            ('iron_ingot', 'Iron Ingot', 64, 40, 2, False), ('torch', 'Torch', 128, 120, 4, False), ('redstone', 'Redstone Dust', 32, 31, 1, False), ('spruce_planks', 'Spruce Planks', 640, 600, 6, False)]
    for i, (ic, n, need, have, src, hud) in enumerate(data):
        sel = i == 1; h = 21
        if sel: c.rect(T, y, tw, h, (66,62,52)); c.marker(T, y, h)
        icon_draw(c, ic, T + 5, y + 2); c.text(T + 25, y + 8, n, CREAM, anchor='lm', maxw=140)
        c.bar(T + 25, y + 16, 140, have/need, GOLD if have < need else SAGE)
        c.text(T + 200, y + 10, f'{need:,}', MUTED, small=True, anchor='rm'); c.text(T + 250, y + 10, f'{have:,}', CREAM, small=True, anchor='rm')
        c.text(T + 305, y + 10, f'{need-have:,}', OCHRE if have else CLAY, small=True, anchor='rm')
        c.text(T + 340, y + 10, str(src) if src else '—', MUTED, small=True, anchor='rm')
        icon_draw(c, 'star' if hud else 'star_off', T + 360, y + 2); y += h + 1
    # detail
    D = 412; dw = 204; c.rect(D - 4, 108, 1, 220, (70,72,75))
    icon_draw(c, 'stone', D, 112, 32); c.text(D + 38, 120, 'Stone', CREAM); c.text(D + 38, 132, 'Missing 648', OCHRE, small=True)
    lines = [('Need', '4,000 = 62 stacks + 32'), ('', '~ 2.3 shulker boxes'), ('Have', '3,352'), ('  carried', '12'), ('  in scope', '3,340 (40 chests)'), ('Outside scope', '220 · 2 chests')]
    yy = 152
    for k, v in lines:
        c.text(D, yy, k, MUTED, small=True); c.text(D + dw - 4, yy, v, CREAM if k.strip() else MUTED, small=True, anchor='ra'); yy += 11
    c.button(D, 224, 98, 18, 'Edit need', small=True); c.button(D + 102, 224, 98, 18, 'On HUD ★', 'primary', small=True)
    c.button(D, 246, 98, 18, 'Show sources', small=True); c.button(D + 102, 246, 98, 18, 'Remove', small=True)
    c.text(D, 274, 'TIP: open a chest and press', DIM, small=True); c.text(D, 285, '"Take missing" to fill up.', DIM, small=True)
    footer(c, 'Enter edit need · Space HUD · Del remove · Ctrl+I import', undo='Undo: Remove Glass', rng='8 of 24')
    return c.save(OUT + '2026-10-20-project-wide.png')

# ---------------- 5. Containers wide ----------------
def containers_wide():
    c = Canvas(640, 360); c.backdrop(); header(c, '', 'Filter containers by name, item or owner…', seg=2)
    c.menu_panel(20, 66, 600, 266)
    L = 24; x = L
    for i, (l, n) in enumerate([('All', 40), ('In project', 12), ('Named', 9), ('Stale', 6)]): x += c.chip(x, 70, l, i == 0, n) + 4
    c.text(300, 76, 'Sort: nearest ▼', MUTED, small=True, anchor='rm'); c.text(376, 76, 'In proj.', DIM, small=True, anchor='rm')
    y = 88
    rows = [('chest', 'Stone chest', 'x 102 y 64 z -40 · 6 m', 'today', True, True), ('barrel', 'Quarry barrel', 'x 88 y 12 z -51 · 18 m', 'today', True, False), ('chest', 'Chest', 'x 120 y 64 z -33 · 31 m', '5 d', True, False),
            ('shulker', 'Purple shulker', 'in Chest at 120 64 -33', '5 d', False, False), ('barrel', 'Barrel', 'x 60 y 70 z 10 · 54 m', '9 d', False, False), ('chest', 'Farm output', 'x 33 y 66 z 98 · 140 m', 'today', False, False), ('hopper', 'Hopper', 'x 34 y 65 z 98 · 141 m', '2 d', False, False)]
    for ic, n, sub, age, inc, sel in rows:
        h = 24
        if sel: c.rect(L, y, 356, h, (66,62,52)); c.marker(L, y, h)
        icon_draw(c, ic, L + 5, y + 4); c.text(L + 25, y + 7, n, CREAM, anchor='lm', maxw=200); c.text(L + 25, y + 17, sub, DIM, small=True, anchor='lm')
        c.text(L + 316, y + 7, age, OCHRE if age.endswith('d') and age != '2 d' else MUTED, small=True, anchor='rm')
        c.button(L + 322, y + 4, 30, 16, 'Yes' if inc else '-', 'focus' if inc else 'normal', small=True)
        y += h + 1
    D = 392; c.rect(D - 4, 70, 1, 258, (70,72,75))
    icon_draw(c, 'chest', D, 72, 32); c.text(D + 38, 78, 'Stone chest  ✎', CREAM); c.text(D + 38, 91, 'Owner: me · seen today 14:02', MUTED, small=True)
    c.text(D, 112, 'SNAPSHOT · 27 SLOTS · 3 KINDS', GOLD, small=True)
    items = ['stone']*20 + ['cobblestone']*4 + ['torch'] + [None, None]
    for i, it in enumerate(items):
        c.slot(D + (i % 9)*18 + 22, 122 + (i // 9)*18, it, 64 if it in ('stone', 'cobblestone') else (12 if it else None))
    c.text(D, 186, 'Stone 1,280 · Cobblestone 256 · Torch 12', MUTED, small=True, maxw=220)
    c.button(D, 200, 70, 18, 'Show', 'primary', small=True); c.button(D + 74, 200, 70, 18, 'Rename', small=True); c.button(D + 148, 200, 72, 18, 'Owner ▼', small=True)
    c.button(D, 222, 108, 18, 'In Castle walls: yes', small=True); c.button(D + 112, 222, 108, 18, 'Forget', small=True)
    c.text(D, 252, 'Counts are from when you last', DIM, small=True); c.text(D, 263, 'opened it. Re-open to refresh.', DIM, small=True)
    c.button(D, 300, 220, 20, 'Claim an area…', small=True)
    footer(c, 'Enter show · F2 rename · Space include · Del forget', rng='7 of 40')
    return c.save(OUT + '2026-10-20-containers-wide.png')

# ---------------- 6. Inventory strip + key legend (in a chest screen) ----------------
def inventory_strip():
    c = Canvas(560, 300); c.backdrop(4)
    gx, gy = 200, 70; gw, gh = 176, 168
    c.rect(gx, gy, gw, gh, (198,198,198)); c.rect(gx, gy, gw, 2, (255,255,255)); c.rect(gx, gy, 2, gh, (255,255,255)); c.rect(gx, gy+gh-2, gw, 2, (85,85,85)); c.rect(gx+gw-2, gy, 2, gh, (85,85,85))
    c.d.text(((gx+8)*S, (gy+5)*S), 'Stone chest', font=FONT, fill=(64,64,64))
    chest = ['stone']*12 + ['deepslate_tiles']*0 + ['cobblestone']*5 + ['glass']*3 + [None]*7
    for i in range(27):
        it = chest[i] if i < len(chest) else None
        c.slot(gx + 7 + (i % 9)*18, gy + 17 + (i // 9)*18, it, 64 if it else None, hl=(120,100,40) if it == 'glass' else None)
    c.d.text(((gx+8)*S, (gy+73)*S), 'Inventory', font=FONT, fill=(64,64,64))
    inv = ['oak_log', 'torch', None, 'iron_ingot', None, None, 'stone', None, None] * 3
    for i in range(27): c.slot(gx + 7 + (i % 9)*18, gy + 84 + (i // 9)*18, inv[i], [12, 30, None, 9][i % 4] if inv[i] else None)
    hb = ['diamond', 'stone', 'oak_planks', 'torch', None, 'glass', None, None, 'chest']
    for i in range(9): c.slot(gx + 7 + i*18, gy + 142, hb[i], 64 if hb[i] in ('stone', 'oak_planks') else None)
    c.rect(gx + 7, gy + 142, 18, 1, GOLD)  # pinned marker
    # strip above GUI
    sy = gy - 22
    c.button(gx, sy, 46, 18, 'Sort', icon='sort', align='l', small=True)
    c.button(gx + 48, sy, 54, 18, 'Deposit', small=True)
    c.field(gx + 104, sy, 52, 18, '', 'Search', icon=None)
    c.button(gx + 158, sy, 18, 18, '', icon='stow')
    # project strip (right side, collision-aware)
    px = gx + gw + 6
    c.menu_panel(px, gy, 134, 98)
    c.text(px + 4, gy + 8, 'CASTLE WALLS', GOLD, small=True, anchor='lm')
    for i, (ic, n, m) in enumerate([('glass', 'Glass', '160'), ('stone', 'Stone', '648'), ('deepslate_tiles', 'Deepslate', '512')]):
        icon_draw(c, ic, px + 4, gy + 16 + i*18); c.text(px + 22, gy + 24 + i*18, n, CREAM, small=True, anchor='lm'); c.text(px + 130, gy + 24 + i*18, m, OCHRE, small=True, anchor='rm')
    c.button(px + 4, gy + 72, 126, 20, 'Take missing (2)', 'primary', small=True)
    c.text(px + 4, gy + 106, 'Here: Glass, Stone', DIM, small=True)
    c.text(px + 4, gy + 116, 'Respects pins & keep', DIM, small=True)
    # sort hover tooltip
    c.tooltip(gx - 192, sy + 22, ['Sort this chest (by name)', ('Scroll: change order', MUTED), ('Right-click: more options', MUTED), ('Middle-click also sorts', MUTED), ('Shift/Ctrl+middle: other orders', MUTED)])
    # key legend
    ly = gy + gh + 6
    c.menu_panel(gx - 150, ly, gw + 300, 44)
    c.text(gx - 146, ly + 8, 'KEYS WHILE HOVERING A SLOT', GOLD, small=True, anchor='lm'); c.text(gx + gw + 146, ly + 8, 'shown 3 of 3 · ? toggles', DIM, small=True, anchor='rm')
    keys = [('P', 'pin'), ('N', 'track'), ('G', 'find in storage'), ('H', 'prioritize'), ('Mid', 'sort'), ('Ctrl+F', 'search')]
    kx = gx - 146
    for k, d in keys:
        kw = int(c.tw(k, True)) + 8
        c.rect(kx, ly + 20, kw, 13, RAISE); c.outline(kx, ly + 20, kw, 13, EDGE_L); c.text(kx + 4, ly + 27, k, CREAM, small=True, anchor='lm')
        c.text(kx + kw + 3, ly + 27, d, MUTED, small=True, anchor='lm'); kx += kw + 6 + c.tw(d, True) + 10
    c.text(gx - 146, ly + 39, 'Gold slots: missing for your project. Gold bar: pinned slot.', DIM, small=True, anchor='lm')
    return c.save(OUT + '2026-10-20-inventory-strip.png')

# ---------------- 7. Settings / Controls ----------------
def settings_controls():
    c = Canvas(640, 360); c.backdrop()
    c.text(320, 11, 'Stow settings', CREAM, anchor='mm')
    c.field(20, 22, 600, 18, '', 'Search all settings and keys…', focused=False, icon='magnify')
    c.menu_panel(20, 44, 600, 288)
    cats = [('Inventory & sorting', 0), ('Deposit & keep amounts', 0), ('Storage & finding', 0), ('Projects', 0), ('Tools & automation', 0), ('HUD & glow', 0), ('Controls', 1), ('Updates & help', 0)]
    y = 50
    for n, sel in cats:
        if sel: c.rect(22, y, 150, 20, (66,62,52)); c.marker(22, y, 20)
        c.text(30, y + 10, n, CREAM if sel else MUTED, anchor='lm', maxw=138); y += 21
    c.button(26, 230, 140, 18, 'Arrange HUD…', small=True)
    c.text(26, 262, 'Saved to', DIM, small=True); c.text(26, 272, 'config/stow/settings.json', DIM, small=True)
    R = 182; c.rect(R - 4, 48, 1, 280, (70,72,75))
    c.text(R, 54, 'ACTION', GOLD, small=True); c.text(R + 210, 54, 'WHERE', GOLD, small=True); c.text(R + 300, 54, 'KEY', GOLD, small=True)
    rows = [('Open Stow', 'Anywhere', 'Ctrl+K', None), ('Inventory search', 'Inventory', 'Ctrl+F', None), ('Find hovered in storage', 'Inventory', 'G', 'new'),
            ('Pin slot', 'Inventory', 'P', None), ('Track item', 'Inventory', 'N', None), ('Prioritize on HUD', 'Inventory', 'H', None), ('Sort inventory', 'Inventory', 'Middle', None),
            ('Smart deposit', 'Container', '[ not set ]', None), ('Toggle auto tool', 'In world', 'Alt+T', None), ('Equipment watch', 'In world', 'F (set by you)', 'conflict')]
    y = 64
    for i, (a, w, k, flag) in enumerate(rows):
        sel = i == 9
        if sel: c.rect(R - 2, y, 438, 20, (66,62,52)); c.marker(R - 2, y, 20)
        c.text(R + 2, y + 10, a, CREAM, anchor='lm', maxw=200); c.text(R + 210, y + 10, w, MUTED, small=True, anchor='lm')
        st = 'focus' if sel else 'normal'
        c.button(R + 296, y + 2, 92, 16, k, st, small=True)
        c.button(R + 392, y + 2, 20, 16, 'R', 'disabled' if not flag else 'normal', small=True)
        if flag == 'conflict': c.text(R + 432, y + 10, '! 1', CLAY, small=True, anchor='rm')
        if flag == 'new': c.text(R + 432, y + 10, 'new', GOLD, small=True, anchor='rm')
        y += 22
    c.rect(R, y + 2, 432, 30, (58,40,36)); c.outline(R, y + 2, 432, 30, CLAY)
    c.text(R + 6, y + 11, '! F is also vanilla "Swap Item With Offhand" (in world).', CREAM, small=True, anchor='lm', maxw=420)
    c.text(R + 6, y + 23, 'Both will run. Press a new key, or Backspace to clear.', MUTED, small=True, anchor='lm', maxw=420)
    footer(c, 'Enter rebind · Backspace clear · Ctrl+Z undo last change', left=('Done',), rng='10 of 18')
    return c.save(OUT + '2026-10-20-settings-controls.png')

# ---------------- 8. Arrange HUD ----------------
def arrange_hud():
    c = Canvas(640, 360)
    for y in range(0, 360, 2):
        t = y / 360; col = (int(120 + 60*t), int(160 + 40*t), int(220 - 20*t)) if y < 230 else (86, 120, 60)
        c.rect(0, y, 640, 2, col)
    for x in range(0, 640, 16): c.rect(x, 230 + (x // 16 % 3), 16, 4, (70, 100, 50))
    # hotbar
    for i in range(9): c.slot(320 - 81 + i*18, 336, ['diamond', 'stone', 'oak_planks', 'torch', None, 'glass', None, None, 'chest'][i], None)
    # elements
    def hud(x, y, w, h, title, lines, sel=False, warn=False):
        c.rect(x, y, w, h, (20,21,23)); 
        c.text(x + 4, y + 7, title, GOLD, small=True, anchor='lm')
        for i, (ic, n, v) in enumerate(lines):
            icon_draw(c, ic, x + 3, y + 13 + i*17); c.text(x + 21, y + 21 + i*17, n, CREAM, small=True, anchor='lm'); c.text(x + w - 4, y + 21 + i*17, v, OCHRE, small=True, anchor='rm')
        oc = CLAY if warn else (GOLD if sel else (200,200,200))
        for i in range(0, w, 4): c.rect(x + i, y - 1, 2, 1, oc); c.rect(x + i, y + h, 2, 1, oc)
        for i in range(0, h, 4): c.rect(x - 1, y + i, 1, 2, oc); c.rect(x + w, y + i, 1, 2, oc)
    hud(8, 8, 130, 64, 'PROJECT: CASTLE WALLS', [('stone', 'Stone', '648'), ('glass', 'Glass', '160'), ('deepslate_tiles', 'Deepslate', '512')], sel=True)
    hud(8, 76, 110, 30, 'BUILDING STOCK', [('stone', 'Stone', '3,352')])
    hud(250, 300, 150, 30, 'AUTOMATION (NEW)', [('hopper', 'Auto tool · Glow', '')], warn=True)
    c.rect(8, 112, 130, 1, GOLD); c.text(142, 112, 'snap: left edge', GOLD, small=True, anchor='lm')
    c.text(404, 304, '! overlaps hotbar', CLAY, small=True, anchor='lm')
    # side panel
    P = 452; c.menu_panel(P, 8, 180, 280)
    c.text(P + 90, 18, 'Arrange HUD', CREAM, anchor='mm')
    y = 30
    for n, on, sel in [('Project totals', True, True), ('Building stock', True, False), ('Automation (new)', True, False), ('Equipment HUD', False, False), ('Notifications', True, False)]:
        if sel: c.rect(P + 2, y, 176, 18, (66,62,52)); c.marker(P + 2, y, 18)
        c.text(P + 8, y + 9, n, CREAM if on else DIM, small=True, anchor='lm'); c.button(P + 140, y + 1, 34, 16, 'On' if on else 'Off', small=True); y += 19
    c.text(P + 6, y + 8, 'POSITION', GOLD, small=True)
    corners = ['TL', '↑', 'TR', '←', '·', '→', 'BL', '↓', 'BR']
    for i, k in enumerate(corners): c.button(P + 6 + (i % 3)*22, y + 16 + (i // 3)*18, 20, 16, k, 'focus' if i == 0 else 'normal', small=True)
    c.text(P + 76, y + 24, 'Arrows: nudge 1 px', MUTED, small=True); c.text(P + 76, y + 36, 'Shift+arrows: 10 px', MUTED, small=True); c.text(P + 76, y + 48, 'Drag with mouse', MUTED, small=True)
    y += 74
    c.text(P + 6, y, 'SIZE', GOLD, small=True); c.rect(P + 40, y + 2, 120, 4, (30,31,33)); c.rect(P + 40, y + 2, 60, 4, GOLD); c.rect(P + 98, y - 2, 4, 12, CREAM); c.text(P + 174, y + 4, '100%', MUTED, small=True, anchor='rm')
    c.text(P + 6, y + 16, 'Rows shown: 3 >', MUTED, small=True)
    c.button(P + 6, 252, 54, 18, 'Reset', small=True); c.button(P + 63, 252, 54, 18, 'Cancel', small=True); c.button(P + 120, 252, 54, 18, 'Save', 'primary', small=True)
    return c.save(OUT + '2026-10-20-arrange-hud.png')

if __name__ == '__main__':
    for f in [find_wide, find_compact, find_empty, project_wide, containers_wide, inventory_strip, settings_controls, arrange_hud]: print(f())
