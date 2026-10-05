# stow

A quiet home for your items. A standalone client-side Fabric mod for Minecraft **26.3**.

## Install

1. Close Minecraft and remove the old Mouse Wheelie jar from your client `mods` folder.
2. Add `stow-0.5.10+mc26.3.jar`. Keep Fabric API installed and add [Cloth Config 26.3.159 for Fabric](https://modrinth.com/mod/cloth-config/version/fg2uyxOW), if it is not already installed.
3. Start Minecraft with Fabric Loader 0.19.5 or newer and Java 25.

The server does not need stow. Mod Menu is optional.

On first launch, stow copies existing Mouse Wheelie chest memories, projects, material goals, names, storage selections, pinned slots and drag-mode preference into `config/stow/`. Original files remain in place. This imports the custom revision's saved-data formats, not Mouse Wheelie's other configuration settings. Subsequent starts use stow's own data.

## Use

| Action | Control |
| --- | --- |
| Open the command palette | Ctrl + K; or the >_ inventory icon |
| Smart deposit into open storage | Click the hopper icon; deposits immediately |
| Configure keep amounts, shortcuts or HUD placement | Settings: Smart deposit, Inventory, Equipment HUD or Project totals |
| Equip the fastest suitable tool for a world block | Middle-click the block (Minecraft Pick Block binding) |
| Toggle automatic mining tools (off by default) | Alt + T; editable in the Shortcut editor |
| Fill a held bundle quickly | Hold it on the cursor; Shift + left-click or drag across items |
| Unpack a held bundle quickly | Hold it on the cursor; Shift + right-click or drag across empty slots |
| Unpack a bundle from its slot | Shift + left-drag from the bundle across empty or matching slots |
| Refill building stacks | Automatic at 8 blocks remaining; configure in Settings → Inventory |
| Configure the top-right project counters | Settings → Projects |
| Exclude carried items from material goals | Settings → Inventory → Include inventory items: Off |
| Prioritize a material in the in-game HUD | Star in Materials; or hover its floating inventory counter and press H |
| Disable/re-enable chest glow | Eye icon in inventory |
| Choose chests for the active project | Projects → Choose project chests |
| Sort hovered inventory or chest | Middle mouse button |
| Choose sort orders and drag mode | Sort icon above inventory; or Mod Menu settings |
| Sort by combined item quantity | Shift + middle-click |
| Sort by displayed name | Ctrl + middle-click |
| Change drag mode | Drag icon; its shape changes per mode |
| Move matching stacks | Shift + left drag |
| Move all crossed stacks in Both mode | Ctrl + Shift + left drag |
| Pin or unpin a player slot | Hover it and press P |
| Search current inventory | Ctrl + F; Escape clears search |
| Find previously opened storage | Chest icon |
| Track a material | Hover item and press N; or `/need 1000 cobble` |
| Edit floating material | Left-click its icon or count |
| Toggle nearest selected chest glow for a material | Right-click its icon or count; right-click again to stop |

Matching drag compares the entire item, including components such as custom names. Each gesture keeps its initial mode and source inventory. Pinned positions use a tiny padlock, 45% of the original icon size. Settings → Inventory → Pinned slot appearance can replace it with a highlighted slot. The separate Keep Shift-transfers out of pinned slots setting skips pinned empty/occupied destinations during Shift-click and Shift-drag. Manual cursor placement remains available. They are excluded from drag, sorting and Smart Deposit.

Quick bundle drag keeps the bundle in its original slot or on the cursor. Each crossed slot is used once, including during fast mouse movement. Filling takes only the remaining bundle capacity; unpacking moves one selected stack at a time. Pinned slots are skipped. Shift-clicking a placed bundle without dragging still transfers the whole bundle. Settings → Inventory → Quick bundle drag can disable these gestures.

Settings use Cloth Config with the same expanded category layout as BetterGrassify: search, reset buttons, Save and Cancel. Edits apply together when you save. Materials, storage, projects, the material picker and chest renaming share full-page native backgrounds, centered headers, matching footer controls and a section sidebar on wide screens and direct navigation tabs on small screens. Materials includes search and slim progress bars; Projects puts project selection before its name/chest editor, using two columns on wide screens. Storage cards keep coordinates, actions and quantity previews in separate areas. Project editing stays together near the top. Sidebar switching returns to the inventory without building a chain of utility screens. Floating counters remain background-free. Inventory buttons and counters reflow from the live container position when the recipe book changes its layout.

Sorting uses Mouse Wheelie's original stack combination and chained-permutation client algorithm, including named/component tie-breakers and bundle handling. Creative order is the default, based on the creative search catalogue. Quantity ranks each item by its **combined total**, not the size of an individual stack. Name uses the active language; Item ID uses numeric registry order; Combine only merges stacks without rearranging positions. Normal, Shift and Ctrl middle-click each have a configurable order (defaults: Creative, Quantity, Name). In containers, the hotbar can be separate or included; the player inventory screen keeps it separate. Pins, equipment and output slots are excluded. Restricted machine inputs are skipped when they cannot accept the whole permutation. Sorting uses ordinary client pickup packets; each batch finishes with an empty cursor and stops if the screen or contents change. Creative inventory keeps vanilla behavior.

Chest memory records contents only after you open a chest, barrel or shulker box. It does not remotely read unopened storage. Goals count **current inventory + included saved chest snapshots** by default. Turn off Settings → Inventory → Include inventory items to count only the active project's selected chests toward goals. The same `current / target` display, remaining amount and completion colors use that choice in all material views. Other players' changes become known when you reopen the chest. Reopening an empty chest updates the count. Projects have independent goals, HUD stars and storage selections, with up to 1,024 materials per project. Projects → Choose project chests opens the selection directly; selected chests sort first when opening the selection screen, and cards retain their positions while you change selections. The compact two-column selection shows at least four cards at the smallest supported GUI viewport. New projects start with no selected chests; existing selections are preserved. Floating counters have no background; scroll down to later goals. Totals are red below 40%, yellow from 40% until completion, and green at or above the target. Settings → Inventory → Remember inventory material page chooses between restoring each project's previous scroll position in the current playing session and restarting at the first item when reopening the inventory.

The right-aligned eye inventory button quickly hides/re-shows chest glow while retaining the selected chest. Clicking Glow in storage or right-clicking a material enables glow again, even if the eye was off. Right-click the same material counter again to stop its glow; the tooltip changes to “stop glow” while active. Stop glow clears the selection. Settings → Inventory also controls visibility.

Loaded storage uses **Minecraft's native spectral-arrow/entity outline effect**, in white, visible through walls. Chests and shulkers use their animated block-entity models; barrels use their block model. Both halves of a double chest are included. stow extracts the selected storage even when a wall hides its render section, and transfers only outline geometry into vanilla's existing pass. It does not submit a second chest skin, alter scene depth or add a fullscreen tint. The existing distance-scaled HUD name/distance label remains; remembered storage outside loaded chunks retains the projected box fallback. Labels stay anchored to the per-frame camera and shrink smoothly with distance, with an eight-pixel readability floor at GUI scales 1–4.

## Survival workspace

Press **Ctrl + K** (either Ctrl key) in the world, inventory or a stow menu to open the compact command palette. Search actions, item names, remembered storage contents or project names. Arrow keys select; Enter opens; Escape or Ctrl + K goes back. Scroll down one row at a time to reach more actions. `need 1000 cobble` opens an explicit material/quantity confirmation. Tools return directly to the original inventory or world. The launcher has a short contextual list; detailed shortcut, keep-amount and both HUD placement editors live in Settings. Shortcuts, keep amounts and chest glow are also directly searchable. Opening an action from inside another stow menu returns to the original inventory/world. Scrolling outside the palette does not move its selection. There is no duplicate Overview menu.

**Equipment Watch** lists every damageable item in its equipment menu, with remaining durability, percentages and bars ordered by urgency. The in-game HUD shows **hotbar gear and worn armor only**: backpack spares and offhand items stay out of the HUD. Healthy items are included by default; Equipment → Show all equipment can switch to warnings only. Repairing, unequipping or moving an item updates the HUD immediately. Equipment never appears beside the inventory material list. The default warning threshold is 15%; HUD icons align against the right edge, with durability counts to their left.

The **Equipment HUD** starts in the bottom right, raised above the hotbar. Settings → Equipment has visibility/size and a draggable placement preview with corner, scale, reset, Save and Back. Placement edits apply on Save. Both placement editors use fullscreen coordinates and the live rows, bounds and rendering code. Controls sit in the opposite corner. Choose Bottom right → Snap to screen edge for zero margins, then Save. Existing margins remain until changed. When no live gear is available, two example rows show the style. Back discards the draft.

**Project totals** appear separately in the **top right** by default, headed by the active project name. Each row shows an item icon and `current / target`, using the Include inventory items setting described above. Counts update as your inventory changes or you reopen storage. Settings → Projects controls visibility, corner, size adjustment (60–150%), a draggable placement preview, horizontal/vertical margins and row limit (**3–5**, default three). Stars prioritize goals first; other visible goals fill any remaining rows in their project order. A project with no stars still shows ordinary goals. Starred goals retain their priority order per project, independently of inventory visibility; unstar to free a priority slot. The Materials summary shows the priority count. Quantity drafts survive pagination and GUI resizing; resize retains typing focus/caret and incoming additions do not interrupt a target edit. Enter confirms a target. In the material picker, Enter chooses the first visible search result and highlights its amount; Enter again adds/saves it. In Projects, Enter renames the active project; Create new is an explicit button. Project visibility is independent of equipment HUD visibility, and the panel can be moved around a minimap or other HUD.

Building stock, equipment and project totals share the same default icon size, font size and six-pixel icon/text gap. Settings → Appearance → Shared HUD size adjusts all three together (100–200%, default 135%). Existing saved building count size becomes the shared size, without resetting settings. Equipment/project size adjustments remain independent (100% matches the block counter). Physical scaling is independent of Minecraft menu scale; oversized equipment lists shrink only to fit the screen. Both panels have no background and hide with F1 or while another screen is open. Inventory counters retain their GUI layout. No minimap, route or world waypoint system is added.

**Building stock** adds a background-free block icon and total beside the hotbar while holding a building block. It counts the selected stack plus exact matching stacks in main inventory and hotbar, including pinned reserves; storage snapshots, equipment and offhand are excluded. At narrow GUI widths it moves above the hotbar's right edge, avoiding the offhand slot. The counter disappears immediately when the selected hand becomes empty, including after the final block; a successful refill shows the new selected stack instead. The default display is 35% larger than before. Settings → Appearance → Shared HUD size adjusts it together with the other HUDs from 100–200%; placement stays inside the screen and moves above the hotbar when necessary. Settings → Inventory can hide this counter.

**Hand refill** tops up the same selected hotbar slot after successful main-hand block placement when its stack reaches 8 or fewer (configurable 1–16). It prefers backpack reserves, matches all item components, preserves named variants and pinned donor slots, and completes ordinary vanilla inventory clicks with an empty cursor. A pinned building slot can refill its existing type. Changing slots, opening a GUI, carrying an item on the cursor or an active sorting/deposit queue cancels the pending refill. Disable it in Settings → Inventory if another mod handles refill.

**Tool pick** uses Minecraft's Pick Block binding (middle-click by default) when aiming at a world block in survival. It considers vanilla tool rules, mining speed, Efficiency and held-item attributes, preferring correct drops when a block requires a suitable tool. It selects an existing hotbar tool or swaps a backpack tool into an unpinned hotbar position; the displaced stack remains in the backpack. Pinned backpack tools are not moved. Creative pick block, entities and inventory middle-click sorting retain vanilla/existing behavior. Settings → Inventory can disable tool picking.

**Auto tool** is off by default. Enable Settings → Inventory → Auto-switch mining tools, or press **Alt + T**. The shortcut can be changed in the Shortcut editor; its actionbar message confirms on/off and the choice is saved. While mining, it selects the fastest suitable tool already in your hotbar, including Efficiency and correct harvest tiers. Holding left-click across stone, dirt or logs switches tools as the target changes. It never retrieves backpack tools or rearranges slots. Hold Sneak to keep your chosen item temporarily. Menus, active item use, creative/adventure/spectator modes and unbreakable blocks do not trigger switching. Manual middle-click tool picking has its own setting. Search “auto tool” in the command palette to toggle it there too.

**Batch oak stripping and companion harvesting:** hold an axe, aim at an unstripped placed oak log and press **Alt + S**. With **stow companion** installed on the server, it discovers the connected oak-log group, strips all of it, then harvests it. Face, edge and corner connections include tall columns, long rows and dense piles beyond normal reach, with no fixed height or length cutoff. The starting log must be visible and reachable. Log axes are preserved while stripping. Other wood, oak wood blocks, leaves and separate groups are excluded.

Press the same shortcut again to cancel. The binding is editable in the Shortcut editor; search “strip” in the world command palette for the action. Settings → Inventory → Batch oak log stripping disables it. Sneak is never used as a start, modifier or cancellation control. The companion uses normal server axe uses and block breaks, respecting vanilla loot and Fabric interaction/break callbacks. It strips the full group before mining starts and skips logs already removed by Timber. Exact compatibility depends on the server's Timber mod.

The companion processes up to 512 work steps per server tick across active batches. Small groups can finish in one tick; larger ones take several ticks. It never loads chunks to expand a group and rejects discovery at unloaded boundaries. A configurable count limit defaults to 16,384 logs (`config/stow-companion.properties`, `maxLogs`, maximum 65,536). Survival reserves two durability points per log plus one spare before starting, conservatively ignoring Unbreaking savings; normal enchantment behavior still applies to the actual actions. Creative uses vanilla creative drops and durability. Protected/changed blocks and player/tool/context changes stop work. Cancelling preserves completed changes. Adventure and spectator are excluded.

**Without the companion**, the shortcut only strips up to 64 visible, nearby oak logs within normal interaction reach; it never mines them. Each ordinary client axe use waits for a server acknowledgement. Changed, blocked and out-of-reach logs are skipped; rejection or a missing acknowledgement stops the batch. Client scanning is bounded to eight blocks around the player while still respecting the actual interaction reach. Install the companion to harvest full connected groups. Installation and local-build changes are recorded in [CHANGELOG.md](../CHANGELOG.md).

**Smart Deposit** runs immediately from the hopper icon, palette or a configured shortcut; there is no confirmation screen. Hovering the hopper highlights eligible source slots and shows the quantity. **Settings → Deposit** saves whether to deposit matching item variants only (the default) and whether to keep the hotbar protected (the default). Turn matching-only off to fill empty storage with all unprotected items. Pins and keep amounts always stay protected. Matching stacks fill before empty slots; destination restrictions are respected. Machine, crafting, merchant and other menu types are excluded. Each queued source stack finishes with an empty cursor; closing/changing the menu or receiving unexpected contents stops pending moves. A short actionbar message reports the result or the reason nothing could move.

**Keep amounts** are minimums per item type, across all main-inventory and hotbar stacks, including pinned stacks and named variants. For example, keeping 64 cobblestone deposits only the extras that fit in eligible storage. Search an item, edit its minimum and Save; zero removes the rule. Changes are drafted together, including across pages; Back discards them. Keep amounts protect deposits, not ordinary manual transfers.

The **Shortcut editor** supports keyboard/mouse bindings and modifiers, searchable actions, individual reset, and unbinding with Delete while capturing. Save applies the draft; Back cancels it. Relevant Minecraft/mod key mappings are flagged in their input context; duplicate stow bindings are disabled until resolved. Inventory search consumes typing; Ctrl + K remains available in stow menus. Left/right modifier flags and Caps/Num Lock are normalized for keyboard and mouse shortcuts. Sort retains its Shift/Ctrl order variants. Other actions start unbound to avoid taking additional default keys.

## Build

Install JDK 25. Run `./gradlew build` on Linux/macOS or `gradlew.bat build` on Windows. The jar is written to `build/libs/`. The Gradle wrapper downloads Gradle 9.6.0; Fabric Loom 1.17 is used without mappings for unobfuscated Minecraft 26.3.

Client dependencies: Minecraft, Fabric Loader, Fabric API, Cloth Config. Mod Menu is compile-only and optional at runtime. The separate companion only needs Minecraft, Fabric Loader and Fabric API; it includes no client inventory, HUD or configuration-screen code. No Coat, Tweed, Amecs or scroll-transfer subsystem is bundled. Hand refill is a small standalone building helper.

The `tests/porttest/FeatureTest.java`, `CompanionTests.java`, `BuildAidTests.java`, `MaterialUpgradeTests.java`, `PolishTests.java` and `WorkspaceTests.java` client harnesses exercise gestures, persisted settings, legacy imports, sorting through vanilla menu transactions, search, snapshots, projects, counts, scrolling, tooltip rendering, GUI layouts and the locator projection and camera transforms. They are development fixtures and are not shipped in the release jar. `STOW_NATIVE_WORLD_TEST=1 python dev-tools/test-local.py` additionally creates a temporary integrated flat world and captures a chest outline through a solid wall.

Build reference: [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html).

## Credits

Storage and inventory utilities were extracted from our custom Mouse Wheelie port and adapted for stow. Required upstream attribution is retained in `NOTICE` and `LICENSE` (Apache 2.0). The new icon and branding are included in `src/main/resources/assets/stow/`.

### 0.5.5 interaction polish

Mouse-wheel scrolling moves one row at a time, including whole two-column rows in storage and the material picker. High-resolution wheel gestures accumulate correctly. A short 160ms cubic transition makes direction easy to follow; clipping and moving widget hitboxes keep rows from covering footer controls. Page arrows still move a full window. Range labels and thin scroll indicators show your position. The inventory tracker and quick actions also ease their one-row scrolling, and Cloth Config settings/tabs enable native smooth scrolling.

Inventory actions now use downloaded Material Design Icons rendered at 16 GUI pixels: a chest for Storage, a clipboard list for Materials, sort bars, lightning for Quick actions, a tray/down arrow for Deposit, and eye/eye-off for glow. Drag modes use stacked layers (matching), select-all (all), and a finger/drag arrow (both). The palette also distinguishes adding a goal (clipboard-plus), searching storage (magnifier), equipment (pickaxe), reserves (shield-check), projects (folders), shortcuts (keyboard), and settings (gear). Actual items retain Minecraft item models. X/remove, checkmark confirmation and page controls use centered icon textures. Materials has room between counts, progress and actions; Save target uses a checkmark and Enter still works. English/German help is shorter, hover titles bold, metadata values bright and secondary text subdued. The chest caption uses a white name and grey distance, sits near the spectral outline with a fixed screen-space gap, and preserves the existing bob-compensated/distance-scaled projection.

### Icon assets (0.5.6)

The actual SVG artwork is downloaded from [Pictogrammers Material Design Icons](https://pictogrammers.com/library/mdi/), pinned to upstream commit `9e04201d4557e729822fb57f62a316c3dea1d4a8`. The selected 24 SVGs and manifest are in `design/icons/mdi`; the Apache 2.0 attribution is in NOTICE and the upstream license is retained alongside the sources and runtime textures. `python dev-tools/render-icons.py` uses Inkscape and Pillow to export white 64×64 PNGs with alpha, rendered at centered 16px in native Minecraft buttons. Development tools are optional: the game needs no SVG library, network request or new dependency. Minecraft's texture loader performs filtering as GUI size changes. Item icons remain actual Minecraft item models.

### Vanilla crafting (0.5.8)

Crafting output slots use vanilla Shift-click in both the inventory and crafting table. stow does not replace crafting with a pickup queue or intercept its drag gesture. **Keep Shift-transfers out of pinned slots** still protects ordinary item transfers; crafting is exempt and may fill or merge into pinned slots according to vanilla rules.
