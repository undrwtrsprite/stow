# stow

A quiet home for your items. A standalone client-side Fabric mod for Minecraft **26.3**.

## Install

1. Close Minecraft and remove the old Mouse Wheelie jar from your client `mods` folder.
2. Add `stow-0.4.0+mc26.3.jar`. Keep Fabric API installed and add [Cloth Config 26.3.159 for Fabric](https://modrinth.com/mod/cloth-config/version/fg2uyxOW), if it is not already installed.
3. Start Minecraft with Fabric Loader 0.19.5 or newer and Java 25.

The server does not need stow. Mod Menu is optional.

On first launch, stow copies existing Mouse Wheelie chest memories, projects, material goals, names, storage selections, pinned slots and drag-mode preference into `config/stow/`. Original files remain in place. This imports the custom revision's saved-data formats, not Mouse Wheelie's other configuration settings. Subsequent starts use stow's own data.

## Use

| Action | Control |
| --- | --- |
| Open the command palette | Ctrl + K; or the >_ inventory icon |
| Smart deposit into open storage | Hopper icon; preview by hovering it |
| Configure keep amounts, shortcuts or HUD placement | Search the command palette |
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
| Highlight nearest selected chest with material | Right-click its icon or count |

Matching drag compares the entire item, including components such as custom names. Each gesture keeps its initial mode and source inventory. Pinned positions use a shaped padlock with a dark outline and keyhole. They are excluded from drag, sorting and Smart Deposit.

Settings use Cloth Config with the same expanded category layout as BetterGrassify: search, reset buttons, Save and Cancel. Edits apply together when you save. Materials, storage, projects, the material picker and chest renaming share full-page native backgrounds, centered headers, matching footer controls and a section sidebar on wide screens. Project editing stays together near the top. Sidebar switching returns to the inventory without building a chain of utility screens. Floating counters remain background-free.

Sorting uses Mouse Wheelie's original stack combination and chained-permutation client algorithm, including named/component tie-breakers and bundle handling. Creative order is the default, based on the creative search catalogue. Quantity ranks each item by its **combined total**, not the size of an individual stack. Name uses the active language; Item ID uses numeric registry order; Combine only merges stacks without rearranging positions. Normal, Shift and Ctrl middle-click each have a configurable order (defaults: Creative, Quantity, Name). In containers, the hotbar can be separate or included; the player inventory screen keeps it separate. Pins, equipment and output slots are excluded. Restricted machine inputs are skipped when they cannot accept the whole permutation. Sorting uses ordinary client pickup packets; each batch finishes with an empty cursor and stops if the screen or contents change. Creative inventory keeps vanilla behavior.

Chest memory records contents only after you open a chest, barrel or shulker box. It does not remotely read unopened storage. Goals count **current inventory + included saved chest snapshots**, so other players' changes become known when you reopen the chest. Reopening an empty chest updates the count. Projects have independent goals and storage selections. Floating counters have no background; scroll down to later goals.

The through-wall locator projects the remembered chest box and label onto the HUD after world rendering. Walls do not occlude this overlay. It neither clears nor integrates scene depth, avoiding the earlier full-screen shader haze. The outline uses the world’s per-frame camera transforms, including view bob, damage tilt and portal effects, to stay anchored while moving. Labels shrink smoothly with camera distance while keeping a readable minimum at GUI scales 1–4. The compact label shows the chest name and rounded player-to-chest-center distance, calculated from the interpolated player position rather than whole block coordinates. Near-plane crossings and off-screen edges are clipped, and targets behind the camera are hidden.

## Survival workspace

Press **Ctrl + K** in the world or inventory to open the command palette. Search actions, remembered storage contents or project names. Arrow keys select; Enter opens. `need 1000 cobble` opens an explicit material/quantity confirmation. Tools return to the palette or original inventory.

**Equipment Watch** monitors the selected hand and worn/offhand equipment, with low-durability warnings in the HUD and beside the inventory. Spare tools in the backpack do not warn. The default threshold is 15%; the equipment screen shows exact remaining durability, percentages and bars, ordered by urgency. Repairing an item or changing the selected hand updates it immediately. Click an inventory warning to open the details.

The **Survival dock** shows equipment warnings and the first visible material goals, using the same current inventory + selected remembered chest totals as the inventory counters. Extra goals have a small overflow indicator. It has no background and adds no minimap, route, world waypoint or marker system. `HUD layout` opens a draggable preview with corner, scale, reset, Save and Back controls. Placement edits are staged until Save. The default is bottom right, raised above the hotbar; the dock hides with F1 and while another screen is open. Settings control visibility, material rows and durability threshold.

**Overview** separates carried supplies from owned totals: ready to build, supplies to collect from storage, or materials still missing. It also reports free inventory slots and equipment warnings. This extends existing projects; it does not remotely inspect unopened chests.

**Smart Deposit** only runs when explicitly clicked, into an open ordinary chest/barrel/ender chest or shulker menu. It moves item variants already present in that storage, including exact names/components, fills matching stacks before empty slots, and obeys destination restrictions. Pins and, by default, the hotbar are protected. Hovering the hopper icon highlights affected source slots and previews the quantity. Machine, crafting, merchant and other menu types are excluded. Each queued source stack finishes with an empty cursor; closing/changing the menu or receiving unexpected contents stops pending moves. A short dock notification reports the result.

**Keep amounts** are minimums per item type, across all main-inventory and hotbar stacks, including pinned stacks and named variants. For example, keeping 64 cobblestone deposits only the extras that fit in eligible storage. Search an item, edit its minimum and Save; zero removes the rule. Changes are drafted together, including across pages; Back discards them. Keep amounts protect deposits, not ordinary manual transfers.

The **Shortcut editor** supports keyboard/mouse bindings and modifiers, searchable actions, individual reset, and unbinding with Delete while capturing. Save applies the draft; Back cancels it. Existing Minecraft/mod key mappings are flagged by physical key; duplicate stow bindings are disabled until resolved. Inventory search consumes typing, and stow menus leave text fields alone. Sort retains its Shift/Ctrl order variants. Other actions start unbound to avoid taking additional default keys.

## Build

Install JDK 25. Run `./gradlew build` on Linux/macOS or `gradlew.bat build` on Windows. The jar is written to `build/libs/`. The Gradle wrapper downloads Gradle 9.6.0; Fabric Loom 1.17 is used without mappings for unobfuscated Minecraft 26.3.

Dependencies: Minecraft, Fabric Loader, Fabric API, Cloth Config. Mod Menu is compile-only and optional at runtime. No Coat, Tweed, Amecs, server handshake, auto-refill or scroll-transfer subsystem is bundled.

The `tests/porttest/FeatureTest.java` and `CompanionTests.java` client harnesses exercises gestures, persisted settings, legacy imports, sorting through vanilla menu transactions, search, snapshots, projects, counts, scrolling, tooltip rendering, GUI layouts and the locator projection and camera transforms. It is a development fixture and is not shipped in the release jar.

Build reference: [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html).

## Credits

Storage and inventory utilities were extracted from our custom Mouse Wheelie port and adapted for stow. Required upstream attribution is retained in `NOTICE` and `LICENSE` (Apache 2.0). The new icon and branding are included in `src/main/resources/assets/stow/`.
