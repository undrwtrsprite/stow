# Stow baseline — released 1.0.6+mc26.3 (static source analysis)

Recorded 2026-10-20 (Europe/Berlin). Phase 1 of 3 of the redesign. **Static analysis only.**
Runtime performance and in-client screenshots are **deferred** to a later phase: both
need Haidan's PC (lunapc), which lagged from earlier work, and will be done gently and
with his OK.

## Subject and provenance

| Item | Value | How established |
| --- | --- | --- |
| Source | `github.com/undrwtrsprite/stow` `main` @ `5dd38ff`, tag `v1.0.6+mc26.3` | Observed: cloned on the box with `gh` |
| Released client JAR | `stow-1.0.6+mc26.3.jar`, SHA-256 `51fdeb3b…a04108f5` (matches the release `SHA256SUMS.txt`) | Observed: downloaded from the GitHub release on the box |
| Companion JAR | `stow-companion-1.0.6+mc26.3.jar`, SHA-256 `9bd063e3…516d9ea468e` (from `SHA256SUMS.txt`) | Observed (checksum file only, JAR not unpacked) |
| Versions | Minecraft 26.3, Java ≥25, Fabric Loader ≥0.19.5, Fabric API ≥0.161.0+26.3, **Cloth Config ≥26.3.159 (required)**, Mod Menu 21.0.0 compile-only/optional, Loom 1.17.21; `breaks: mousewheelie` | Observed: `fabric.mod.json`, `build.gradle`, and the released JAR's `fabric.mod.json` |
| Lunapc working tree | Branch `codex/stow-overhaul` = `5dd38ff` + local commit `5188c88` + ~150 uncommitted changes (the rejected overhaul). **Not** the baseline. | Observed: one `git status`/`git log` on lunapc (read-only) |

Settings in the released 1.0.6 build are a **Cloth Config** screen (`ConfigBuilder`, 7 categories,
2 sub-categories) — observed in the source, in `fabric.mod.json`, and as 22 `me/shedaniel`
references inside the released `StowSettingsScreen.class`. This contradicts
`docs/OVERHAUL.md` on the lunapc branch, which says the 1.0.6 JAR "has no Cloth Config
metadata or class references". Treat that claim as **wrong** for the published JAR.

Labels used below: **Observed (source)** = read in the 1.0.6 source on the box; **Observed
(doc)** = stated in repo docs, not re-verified at runtime; **Inferred** = my interpretation;
**Unverified** = not checked.

## 1. Inventory of Stow-owned surfaces

### 1.1 Screens and menus (20)

| # | Surface | Opened from | Notes (Observed (source) unless marked) |
| --- | --- | --- | --- |
| 1 | **Materials** (`MaterialsScreen`) | Inventory clipboard icon; palette; tab | Project button, search, + Material, Import, Copy list, Storage·N chests, rows (icon, `current / target`, target field + Save, star = HUD priority, eye = inventory tracker visibility, Sources, ×), Finish/Reopen, Undo, `<` `>` paging, Back. 42-px rows. |
| 2 | **Storage** (`ChestMemoryScreen`, browse mode) | Inventory chest icon; palette "Find X"; tab | Search, owner filter (cycle button), **Claim area**, container cards (1–3 columns, 58 px) with Glow/Hide, Rename, Forget; footer Back, Stop glow, Undo, paging. **No combined item total**, container-first. |
| 3 | **Project chests** (`ChestMemoryScreen`, selection mode) | Materials "Storage·N"; Projects "Choose project chests" | All / None, per-card +/x include toggle, Glow, Forget (X), Rename. |
| 4 | **Projects** (`ProjectsScreen`) | Materials project button; palette; tab | Project list buttons (click = switch), name field, Create new, Rename, Delete, Choose project chests, Undo, paging. |
| 5 | **Material picker** | + Material; `N` on hovered item; palette `need 1000 cobble` | Search (name or `ns:id`), item list, amount field, Add/Save (Enter twice). |
| 6 | **Import materials** | Materials → Import | Paste clipboard, text area (65,536 chars / 2,048 lines), Review list, row list with include/skip, mode (add/replace), Import, Undo. |
| 7 | **Review material row** | Import → Edit | Item search, quantity (`2 stacks + 16`), Use / Skip. |
| 8 | **Rename container** | Storage/selection card → Rename | Nickname field, Save, Reset, **Storage owner** button. |
| 9 | **Storage owner** (companion 1.0.5+) | Rename → Storage owner | Claim as mine / Mark as shared / Use player label; denial and "requires companion" texts. |
| 10 | **Claim area** (companion) | Storage → Claim area | Fly and select corners, Claim unassigned storage, Cancel scan, progress line. |
| 11 | **Area selection** (camera mode, `AreaSelection`) | Claim area → Fly | Free camera (WASD/Space/Shift, Ctrl faster), LC corner 1, RC corner 2, RC/Enter → height step, scroll/↑↓ height, Alt = 5 at a time, Backspace undo, Enter confirm, Esc back; on-screen key legend HUD (`stow.area.*`, 15 action labels). |
| 12 | **Quick actions** (`CommandPaletteScreen`) | Ctrl+K (world, inventory, Stow menus); inventory command-block icon | Static list: Track held, Materials, Storage, Projects, Deposit (in container), Settings. **Shortcuts, Keep amounts, Toggle glow, auto tool/strip/attack/mend appear only after typing.** Items, storage contents, projects and `need N item` are searchable. ↑↓ / Enter / Esc. |
| 13 | **Settings** (Cloth Config `ConfigBuilder`) | Palette → Settings; Mod Menu | Tabs: Appearance (5), Sorting (5 + Advanced), Inventory (10 + Building & tools), Automatic attack (3), Deposit (3), Equipment (4), Projects (5). Save/Cancel, search, per-entry reset (Cloth). Links to child editors below. |
| 14 | **Shortcut editor** | Settings → Inventory **or** Automatic attack → Shortcuts | 16 actions, search, capture (Esc cancel, Delete unbind), conflict line, Save/Back. |
| 15 | **Keep amounts** | Settings → Deposit → Keep amounts; palette (after typing) | Item search, per-item minimum, Save/Back. |
| 16a–c | **HUD layout editor** (`DockLayoutScreen`) ×3 targets | Settings → Equipment → *Move equipment HUD*; Settings → Projects → *Move project HUD*; Settings → Appearance → *Block count layout* | Drag, corner, alignment, size, snap to edge, reset, Save/Back. **One element per editor; three entry points in three categories.** |
| 17 | **Equipment watch** (`EquipmentScreen`) | Only via the EQUIPMENT shortcut, **unbound by default**; not in palette | Search, durability list. Effectively hidden. |
| 18 | `ToolListScreen` | **No entry point found** in the source | Inferred: dead code; confirm in phase 2. |
| 19 | **What's new** (`WhatsNewScreen`) | Automatically once after an update (setting); Settings → Appearance → What's new | Version, "How to use", Got it, Open full changelog. Bundled JSON for 1.0.5/1.0.6. |
| 20 | **Update notice** (`VersionChecker`) | Notification when a newer version exists | "Click to open its download page." |

### 1.2 Inventory/container overlays (Observed (source): `InventoryExtrasLayout`, `MixinAbstractContainerScreen`)

- Toolbar of **6 icon buttons + 1 search field** drawn 24 GUI px above the container
  (Drag mode, Storage, Materials, Quick actions, Deposit, search … Glow at the right edge).
  When there is no room above, it becomes a **vertical column right of the container**
  and the search field moves to the left margin.
- **Floating material tracker** (right of the container, up to 140 px wide): project
  material icons + counts; left-click edit, right-click glow/stop glow, `H` prioritize.
- **Slot markers** for pinned slots (padlock at 45% or highlight); deposit hover highlights
  eligible source slots; Shift-drag / bundle-drag gestures; **sort has no visible button**.
- **Shulker preview tooltip** 9×3 grid on hover.

### 1.3 HUD elements

Equipment HUD (bottom right; hotbar gear + armor), Project totals (top right, 3–5 rows,
"+N more"), Building stock (beside hotbar), action notifications (top-right toast, 1.1–1.8 s),
chest glow (native entity outline) + distance label, area-selection legend, update notice.
Shared HUD size 100–200% plus per-element size; three separate placement editors.

### 1.4 Keybinds (Observed (source) `StowShortcuts.Action`, 16 actions)

| Default | Action | Default | Action |
| --- | --- | --- | --- |
| Ctrl+K | Quick actions | Middle mouse | Sort (Shift/Ctrl variants = alternate orders) |
| Ctrl+F | Inventory search | P | Pin slot (hovered) |
| N | Track hovered item | H | Prioritize material on HUD |
| Alt+T | Auto tool | Alt+S | Strip logs / companion harvest / cancel |
| Alt+A | Automatic attack | Alt+M | Auto-mending |
| unbound | Deposit, Keep amounts, Equipment, Projects, Cycle drag mode, Toggle HUD | | |

Also: vanilla Pick Block (middle-click on a block) = tool pick; Shift+LC villager output =
whole-inventory trade; Shift-drag / Ctrl+Shift-drag; bundle gestures; area-selection keys.
**Stow bindings are not registered as vanilla `KeyMapping`s** (no `KeyBindingHelper` /
`registerKeyBinding` in the source), so they do not appear in Minecraft's Controls screen
or in Controlling's search.

### 1.5 Saved data and config (must be preserved)

`config/stow/settings.json` (≈50 fields + `shortcuts` map; atomic write via `.tmp`),
per-world chest-memory/project stores (`ChestMemoryStore`, JSON, version-4 dye metadata,
undo history), Mouse Wheelie one-time import. Companion: `config/stow-companion.properties`
(`fastChestSorting`, `instantVillagerTrading`, `maxLogs`), server-side ownership data.

## 2. Workflow step counts (baseline)

Counting rules: **K** key/chord press, **C** click, **D** drag, **T** one typed text entry
(length not counted), **S** scroll/camera movement (variable). Start state is "in the world,
no screen open" unless the row says "in container". Counts are **derived from the source**
(button wiring and screen transitions), not timed in the client. "Disc." = how a player
discovers it without the README.

| ID | Job | Baseline sequence | Steps | Disc. |
| --- | --- | --- | --- | --- |
| B1 | Sort an open container | middle-click over it | 1 | Hidden (no button; settings hint only) |
| B2 | Sort player inventory | E, middle-click | 2 | Hidden |
| B3 | Smart deposit (in container) | hopper icon | 1 | Visible icon |
| B4 | Pin a slot (in inventory) | hover + P | 1 | Hidden (shortcut editor only) |
| B5 | Search current inventory | Ctrl+F, T | 1 + T | Visible field |
| B6 | **Find an item in remembered storage and see it in the world** | E, Storage icon, T, Glow on the right card (scan cards; no combined total), Esc, Esc | **5 + T** | Visible but container-first |
| B6p | same via palette | Ctrl+K, T, ↓×0–3 to "Find X in storage", Enter, Glow, Esc | 4–7 + T | Palette hint |
| B7 | Track a hovered item as a goal (in inventory) | hover + N, [amount T], Enter | 2 (+T) | Tooltip hint |
| B8 | Add a material by name | E, Materials, + Material, T, Enter, T, Enter | 5 + 2T | Visible |
| B9 | Import a pasted list | E, Materials, Import, Paste clipboard, Review list, [mode 0–1], Import; **+ per row needing review:** Edit, T, choose item, Use | 6–7 + rows × (3 + T) | Visible |
| B10 | Change a material target | E, Materials, target field, T, Enter | 4 + T | Visible |
| B11 | Prioritize material on project HUD | E, Materials, star (or E, hover counter + H) | 3 (2) | Visible star / hidden H |
| B12 | Switch active project | E, Materials, project button, project, Back | 5 | Visible |
| B12p | same via palette | Ctrl+K, T, Enter | 2 + T | Palette |
| B13 | Create a project and choose its chests | E, Materials, project button, T, Create new, Choose project chests, All (or n toggles), Back, Back | 8 + T (or 7 + n + T) | Visible |
| B14 | Choose chests for current project | E, Materials, Storage·N, n toggles (or All), Back | 4 + n (5) | Visible |
| B15 | Finish a project | E, Materials, Finish | 3 | Locked until all targets met |
| B16 | Undo the last storage/project change | E, Materials or Storage, Undo | 3 | Visible footer |
| B17 | Glow nearest source of a tracked material | E, right-click its floating counter, Esc | 3 | Tooltip hint |
| B18 | Rename a container | E, Storage, [T], Rename, T, Save name | 5 + T (+T) | Visible |
| B19 | Claim ownership label (companion) | E, Storage, Rename, Storage owner, Claim as mine | 5 | Buried under Rename |
| B20 | Claim an area (companion) | E, Storage, Claim area, Fly and select, S, LC, RC, RC/Enter, S, Enter, Claim | 10 + 2S | Visible button; legend HUD |
| B21 | Change one setting (e.g. refill threshold) | E, Quick actions, Settings, category tab, [expand sub-category], [S], change, Save | 6–7 + S | Palette only (or Mod Menu: Esc, Mods, T, select, config, tab, change, Save = 7 + T) |
| B22 | Rebind a Stow shortcut | E, Quick actions, Settings, Inventory tab, Shortcuts, [T], action, press chord, Save, close Settings | 9 (+T) | Two entry links in two categories |
| B23 | Move one HUD element | E, Quick actions, Settings, category, Move … HUD, D, Save, close Settings | 7 + D | Three editors in three categories |
| B23a | Arrange all three HUD elements | as B23 but three editors from three categories | ≈ 14 + 3D | — |
| B24 | Set a keep amount | E, Quick actions, Settings, Deposit, Keep amounts, T, T, Save, close Settings | 7 + 2T (palette after typing: 3 + 3T) | Buried |
| B25 | Toggle auto tool / attack / mending / strip | one chord (Alt+T/A/M/S) | 1 | Palette lists them **only after typing** |
| B26 | Change drag mode (in inventory) | drag icon (cycles 3 modes) | 1–2 | Visible icon |
| B27 | Toggle chest glow | eye icon in inventory | 1 (+E) | Visible icon |
| B28 | Reopen What's new | E, Quick actions, Settings, link (Appearance is first tab) | 4 | Buried |
| B29 | Open Equipment watch details | bind a shortcut first (B22), then press it | 9 + 1 | Hidden |
| B30 | Preview shulker contents | hover | 0 | Automatic |
| B31 | Whole-inventory villager trade | Shift+LC output | 1 | Hidden |

### Cross-cutting findings (all Observed (source) unless marked)

1. **Context loss across tabs.** `MemoryScreenBase.navigate()` builds a new screen per tab
   and passes `""` as the Storage query; search, owner filter, page and selection are
   discarded. Returning to a task costs a re-type (T) plus re-scroll.
2. **Container-first storage.** Storage lists containers; it has no aggregated per-item
   total and no "nearest source" ordering. Finding *where most of X is* means scanning
   cards (Inferred cost grows with container count).
3. **Locating needs two Esc presses.** Glow selects a container but stays in the screen
   (`refresh(false)`); you must close Storage and then the inventory to see it.
4. **Projects is a separate screen just to switch or rename.** It costs 2–3 extra steps
   per switch; the palette is faster but only if you know the name.
5. **Settings are split across systems.** A Cloth Config screen (visually different from
   Stow's own menus) plus 5 custom child editors linked from inside categories. HUD
   placement is split over 3 editors in 3 categories; Shortcuts are linked twice.
6. **Hidden essentials.** Sort (middle-click), pin (P), track (N), prioritize (H), whole
   trade (Shift+LC), bundle gestures and drag modifiers have no visible affordance in the
   inventory. The palette hides Shortcuts, Keep amounts, Glow and automation toggles until
   you type. Equipment watch has no default entry point. `ToolListScreen` has none at all.
7. **Bindings are outside vanilla Controls.** Conflicts are checked inside Stow's editor
   only; Minecraft's Controls screen and Controlling cannot see Stow actions.
8. **Inventory toolbar density.** 6 icons + search above the container (or a right-side
   column). Coexistence with other mods' container buttons (IPN, Chest Tracker, recipe
   viewers) is **Unverified**.
9. **Strengths to keep.** Visible snapshot age ("Last seen …"), explicit Undo on
   destructive actions, top-right action feedback with result counts, import review with
   per-row correction, exact item/component conservation and server validation for
   companion operations, complete EN/DE coverage (534/534 keys).

### EN/DE text length (Observed: lang files)

534 keys in both `en_us.json` and `de_de.json`, none missing. Median DE/EN character ratio
is 1.14; worst short labels are ×1.9–2.5 (`stow.area.act.undo`, `stow.action.auto_tool`,
`stow.common.save`, `stow.need.hide-item`/`show-item`, `stow.sort.scope.2`). Short labels
on fixed-width buttons are the overflow risk, not long help text. Design handling is in
`docs/design/redesign-v2.md` §8.

## 3. Deferred (not done in phase 1)

| Deferred item | Why | Planned |
| --- | --- | --- |
| Runtime performance: frame-time distributions, search latency, allocation/GC, tick/packet cost, large-dataset scenarios | Needs a Minecraft client on lunapc; Haidan asked us to slow down after lag | Later phase, short gentle sessions with his OK, before/after on the same setup |
| In-client screenshots of every Stow screen (EN/DE, compact/wide, GUI scales) | Same | Later phase; one session, few captures |
| Timed task runs and physical-input/keyboard-focus checks of the step counts above | Same | Later phase; current counts are source-derived |
| Coexistence with IPN, Mouse Tweaks, Chest Tracker, JEI/REI, Shulker Box Tooltip etc. | Same; also needs isolated profiles | Later phase |
| Confirm `ToolListScreen` is unreachable | Low risk; static search already finds no caller | Phase 2 code read |

## 4. Prior evidence (lunapc working tree, read-only snapshot copied to the box)

| Record | What it shows | Status for this phase |
| --- | --- | --- |
| `docs/research/2026-10-20/system-rethink/STOW-BASELINE.md` | 40-snapshot fixture; callback-level probe showed query lost on Find→Plan→Find | **Valid only for the rejected overhaul artifact** (stated in the file). For 1.0.6 the same loss is confirmed here from source only. |
| `docs/research/2026-10-20/REFERENCE-LAB.md` | 8 reference JARs run in a disposable profile; Shulker Box Tooltip 5.4.2 crashed before title (mixin target missing) | Observed by the earlier session (hashes recorded); not re-run. Mostly empty/root screens, as the file itself admits. |
| `docs/evidence/2026-10-20/README.md` + 2 PNGs | Freshness tooltip before/after on a 24-container fixture | Valid for that tooltip only; the "after" is the rejected branch. |
| `docs/OVERHAUL.md`, `OVERHAUL-RESEARCH.md`, `DESIGN-SYSTEM.md`, `SYSTEM-RETHINK.md`, `SYSTEM-ARCHITECTURE.md` | Plans, research and claims for the rejected design | Background only. Cloth Config claim is contradicted (above). Platform counts in `CANDIDATES.md` were refreshed today (see research records). |
| `CHANGELOG.md` / docs on `main` | Feature descriptions used for the inventory above | Observed (doc); FEATURES.md still describes Cloth Config, consistent with the JAR. |

Gaps: no populated runtime walkthrough of 1.0.6 exists; no measured performance baseline
for 1.0.6 on the current setup; no coexistence matrix; no keyboard-only/narration check.
