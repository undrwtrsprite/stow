# Stow feature guide

This guide explains how each part of Stow works and what its limits are. For a short overview, see the [README](../README.md). Release-by-release changes are in the [changelog](../CHANGELOG.md).

Stow is for **Minecraft 26.3 on Fabric**. The client works on its own. The optional [companion](COMPANION-DESCRIPTION.md) adds server-side speed-ups and storage labels.

## Contents

- [Installation and requirements](#installation-and-requirements)
- [Sorting and moving items](#sorting-and-moving-items)
- [Pins, search and Smart Deposit](#pins-search-and-smart-deposit)
- [Storage memory](#storage-memory)
- [Material planning](#material-planning)
- [Building helpers](#building-helpers)
- [Tools, equipment and combat helpers](#tools-equipment-and-combat-helpers)
- [Villager trading](#villager-trading)
- [Shulker box previews](#shulker-box-previews)
- [Command palette, settings and HUDs](#command-palette-settings-and-huds)
- [Companion features](#companion-features)
- [Shortcut reference](#shortcut-reference)
- [Limits at a glance](#limits-at-a-glance)
- [Building from source](#building-from-source)
- [Credits and licenses](#credits-and-licenses)

## Installation and requirements

1. Install **Java 25+**, **Fabric Loader 0.19.5+**, **Fabric API 0.161.0+26.3** and **Cloth Config 26.3.159+**.
2. Put the Stow JAR in your client's `mods` folder.
3. Optional: install **Mod Menu 21.0.0** to open Stow's settings from the mod list.
4. Remove **Mouse Wheelie** before starting. The two mods conflict.

The server doesn't need Stow for the client features. For companion features, see [Companion features](#companion-features).

**Importing from Mouse Wheelie:** on first launch, Stow copies existing Mouse Wheelie chest memories, projects, material goals, names, storage selections, pinned slots and drag-mode preference into `config/stow/`. The original files are left in place. Stow doesn't import Mouse Wheelie's other settings. After the first launch, Stow uses its own data.

## Sorting and moving items

- **Sort:** middle-click a hovered inventory or chest to sort it. Shift + middle-click sorts by combined quantity. Ctrl + middle-click sorts by displayed name.
- **Sort orders:** Creative catalogue (the default for normal clicks), Quantity, Name, Item ID and Combine only. Each click type (normal, Shift, Ctrl) has its own configurable order. The defaults are Creative, Quantity and Name.
- **Combined quantity:** Quantity ranks an item by its combined total across stacks, not the size of one stack.
- **Name order:** uses the active game language.
- **Item ID order:** uses the numeric registry order.
- **Combine only:** merges partial stacks without moving other items.
- **Hotbar in containers:** the hotbar can be included in container sorts or kept separate. The player inventory screen always keeps it separate.
- **Protected slots:** pinned slots, equipment and output slots are never sorted.
- **Restricted inputs:** machine inputs are skipped when they can't accept the whole arrangement.
- **How sorting runs:** Stow sends ordinary client pickup packets. Each batch ends with an empty cursor and stops if the screen or its contents change. Creative inventory keeps vanilla behavior.
- **Move several stacks:** Shift + left-drag moves every matching stack. Ctrl + Shift + left-drag moves every stack you cross in Both mode.
- **Matching compares whole items**, including components such as custom names.
- **Drag modes:** the drag icon shows the current mode, and each gesture keeps its starting mode and source inventory.
- **Quick bundles:** Shift + left-click or drag across items to fill a bundle you're holding. Shift + right-click or drag across empty slots to unpack it. Shift + left-drag from a placed bundle unpacks it into empty or matching slots. Filling uses only the bundle's remaining capacity. Unpacking moves one stack at a time. Each crossed slot is used once, even during fast mouse movement. Pinned slots are skipped. Shift-clicking a placed bundle without dragging still moves the whole bundle.
- **Quick bundle drag** can be disabled under **Settings → Inventory**.
- **Mouse Tweaks:** when Mouse Tweaks is installed, Stow's drags keep the Matching filter, and Mouse Tweaks' other gestures and wheel actions keep working.

## Pins, search and Smart Deposit

- **Pin a slot:** hover it and press **P**. Pinned slots appear with a small padlock (45% of the icon size). **Settings → Inventory → Pinned slot appearance** can replace the padlock with a highlighted slot.
- **What pins protect:** sorting, drags, Smart Deposit and material-picker auto-placement leave pinned slots alone. Manual cursor placement still works.
- **Keep Shift-transfers out of pinned slots:** a separate setting. When it is on, ordinary Shift-clicks and Shift-drags also skip pinned destinations.
- **Search your inventory:** Ctrl + F. Escape clears the search.
- **Find remembered storage:** the chest icon opens storage search.

**Smart Deposit** moves items into open storage immediately. There is no confirmation screen. Trigger it with the hopper icon, from the command palette or with a shortcut. Hovering the hopper highlights the source slots and shows the quantity.

- **Settings → Deposit** controls two options:
  - **Match variants only** (on by default). Turn it off to fill empty storage with all unprotected items.
  - **Keep the hotbar protected** (on by default).
- Matching stacks fill first, then empty slots. Destination restrictions are respected.
- Machine, crafting, merchant and other non-storage menus are excluded.
- Pinned slots and keep amounts are always protected.
- A short notification reports the result, or why nothing could move.

**Keep amounts** set a minimum for an item across your main inventory and hotbar, including pinned stacks and named variants. Keeping 64 cobblestone, for example, deposits only the extra cobblestone that fits in storage. Search an item, set its minimum and save. Setting it to zero removes the rule. Keep amounts protect deposits only, not ordinary manual moves.

## Storage memory

Stow remembers storage snapshots, so you can search and plan without reopening every chest.

- **When snapshots are taken:** when you open a chest, barrel or shulker box.
- **Locally placed shulkers:** Stow remembers their contents from the item you're holding once the server confirms the placement.
- **Server-confirmed removal:** when the server confirms a shulker was removed, its saved snapshot, totals and glow are cleared.
- **What Stow doesn't do:** it doesn't read unopened storage remotely. Other players' changes appear when you reopen the container.
- **Empty chests:** reopening an empty chest updates its count.

Goals count **your current inventory plus the included saved snapshots** by default. To count only the chests selected for the active project, turn off **Settings → Inventory → Include inventory items**. That choice applies everywhere totals appear.

**Forgetting a snapshot** removes its contents but keeps the chest's project selection and material goals. Reopening it refreshes its totals. **Undo** restores forgotten entries. Excluding a chest is a separate control.

**Storage list:** cards show a container icon, coordinates and quantity previews in separate areas. Shulker cards take their dye color. Undyed and older unknown-color entries use a neutral purple style until refreshed. The list uses up to three columns where space allows.

**Chest glow:** loaded storage gets Minecraft's native white spectral outline, so it shows through walls. Double chests are outlined as a whole. Remembered storage outside loaded chunks shows a projected box instead. The name and distance label follows the camera and shrinks with distance, with a minimum readable size.

- Click **Glow** in the storage list, or right-click a material counter, to show the nearest selected chest's glow. Right-click the same counter again to stop it.
- The eye button in the inventory hides and re-shows glow. The chosen chest is kept.

## Material planning

Planning covers projects, material goals and the HUD that tracks them.

### Importing a list

Open **Materials → Import**, paste a list and choose **Review list**.

- **Formats:** `32 Oak Planks`, `Glass x64`, `Glass (64)` and `2 stacks of Glass` all work. Stacks use the item's real stack size. English vanilla names also work when the game language is German.
- **Review:** the preview keeps your original lines. **Edit** opens item search and quantity correction. The include/skip control chooses which rows enter the project.
- **Excluded rows:** unknown names, ambiguous variants, prose and invalid quantities stay out until you fix them. Duplicate included rows are combined.
- **Apply:** choose **replace matching targets** or **add quantities**, then review the resulting totals and confirm.
- **Undo** reverses the whole import. Other goals in the project are unchanged.
- **Limits:** see [Limits at a glance](#limits-at-a-glance). No changes are applied if a paste exceeds a limit.

### Projects and finishing

- Each project has its own targets, HUD stars and selected chests.
- **Choose project chests:** Projects → Choose project chests. Selected chests sort to the top.
- **New projects** start with no chests selected. Existing selections are kept.
- **Auto-assign:** newly discovered and reopened chests join the active project by default. Turn this off in **Project totals** settings.
- **Finish project** (in the Materials footer) unlocks when every target is met. A finished project keeps counting as you use materials. Its colors go neutral, and rows say "N used" instead of "missing".
- **Reopen project** restores the normal colors. **Settings → Project totals → Show finished project totals** can hide a finished project's HUD.
- **Totals colors:** red below 40%, yellow from 40% until complete, green at or above the target.
- **Copy material list:** the paper icon copies the project's saved targets, including hidden goals, as plain text with exact item IDs, for example `128 minecraft:oak_log`. It doesn't include chest choices, visibility or HUD priorities. Save quantity edits first.

### HUD

- **Project totals** appear in the top-right corner by default, headed by the project name. Each row shows an icon and `current / target`.
- **Row limit:** 3 to 5 rows (default 3). Starred goals come first, in your chosen order. Other visible goals fill the remaining rows.
- **Hiding:** the HUD can be hidden with **F1**, and it isn't shown while another screen is open.
- **Star a material:** press **H** while hovering a floating counter, or star it in the Materials list.
- **Track a material:** hover an item and press **N**, or use `/need 1000 cobble` in the command palette. The command opens a confirmation for the material and quantity.

### Moving the HUD

Each HUD has an editor with left, center or right alignment, dragging, corner anchoring, size controls and reset.

- **Project totals:** size is adjustable from 60% to 150%.
- **Shared HUD size:** **Settings → Appearance → Shared HUD size** sets building stock, equipment and project totals together, from 100% to 200% (default 135%).
- **Floating counters** have no background. Scroll down to reach later goals.

## Building helpers

- **Block count:** shows your remaining building stock beside the hotbar while you hold a building block. It counts the selected stack plus matching stacks in your main inventory and hotbar, including pinned reserves. Storage snapshots, equipment and the offhand are not counted. It can be moved, or hidden in **Settings → Inventory**.
- **Refill blocks and rockets:** tops up the hand you used when its stack drops to 8 or fewer. The threshold can be set from 0 to 16. At 0, refill happens after the last item is used. Works in both hands, including Elytra boosts. Rockets must match in flight duration and explosion data. Pinned reserves are skipped. Disable it in **Settings → Inventory** if another mod handles refill.
- **Tool pick:** while you aim at a world block in survival, press Pick Block (middle-click by default) to choose the fastest suitable tool. It uses an existing hotbar tool, or swaps a backpack tool into an unpinned hotbar slot. The displaced item stays in your inventory. Pinned tools aren't moved. Settings → Inventory can turn tool pick off.
- **Auto tool (off by default):** press **Alt + T** or enable it in Settings. While mining, it switches to the fastest suitable tool already in your hotbar. It never fetches backpack tools or moves items. Hold Sneak to keep your current item for a moment. It doesn't switch in menus, during item use, in creative, adventure or spectator modes, or on unbreakable blocks.

## Tools, equipment and combat helpers

### Tool replacement

**Inventory → Building & tools** contains the replacement settings.

- **Keep 1 durability:** when your main-hand tool is nearly worn out, swaps in a usable spare of the same item. The old tool goes to the spare's slot. Without a spare, a damaging use is blocked. Costly actions reserve enough durability to avoid breaking the tool.
- **After break:** waits for the tool to break, then swaps in a spare.
- Replacements prefer matching components and skip pinned spares. They wait while menus or other inventory operations are open. Armor and offhand equipment are excluded.

### Equipment HUD

- Shows **hotbar tools and worn armor only**. Backpack spares and offhand items stay off the HUD.
- Shows remaining durability as points by default. Turn on **Show durability percentage** to show a percentage instead.
- Order: hotbar tools left to right, then armor from helmet to boots.
- Healthy items are shown by default. Switch to warnings only in settings. The default warning threshold is 15%.
- The detailed Equipment screen also shows backpack and offhand gear, sorted by durability, with percentage and current/maximum values.
- It starts in the bottom right, above the hotbar. Use its placement editor to move it.

### Auto-mending

- **Alt + M** toggles it. You can also choose **Toggle auto-mending** from the command palette.
- Damaged Mending items cycle from your main inventory and hotbar into the offhand, most damaged first. Collect XP normally. Stow never creates XP and never repairs items without Mending.
- Fully repaired items return to their original positions, and your original offhand item is restored.
- Your selected hotbar slot stays available for XP farming. Armor and your held main-hand item still receive normal Mending repairs.
- Pinned donor positions are skipped.
- Menus, focus loss, pause, death and spectator mode stop the cycle safely. The borrowed item returns once gameplay resumes.
- Manual changes to the borrowed item or its reserved slot stop the cycle without overwriting your changes.
- If a disconnect stops the return packet from being sent, the borrowed item stays in the offhand. Reconnecting doesn't restart the cycle.

### Automatic attack

- **Alt + A** toggles it. You can also choose **Toggle automatic attack** from the command palette.
- **Settings → Automatic attack** offers two modes:
  - **Hold Attack** (default): holds the normal Attack control, so mining continues. It sends one initial attack.
  - **Interval clicks**: clicks Attack repeatedly, including against entities.
- Interval: 50 to 60,000 ms (default 500 ms). Clicks follow game ticks, at most 20 per second. Delays don't cause catch-up bursts.
- Vanilla attack cooldowns, miss delays and item use still apply.
- Opening a menu or chat, losing focus, pausing, dying, entering spectator mode or disconnecting stops it and releases the held Attack control. It stays off when you return.

### Batch log stripping

- Hold an axe, aim at a placed unstripped log and press **Alt + S**.
- **Without the companion:** strips up to 64 visible logs of the same type, within reach and within eight blocks of you. It never breaks them. Each ordinary axe use waits for the server to confirm it. Blocked or out-of-reach logs are skipped.
- **With the companion:** strips and then harvests the whole connected group of that log type. Face, edge and corner connections are included, with no height or length cutoff. See [Companion features](#companion-features).
- Log axis orientation is preserved while stripping.
- Press the same shortcut again to cancel.
- **Settings → Inventory → Log stripping and companion harvesting** turns it off.
- Sneak is never used as a start, modifier or cancel key.

## Villager trading

**Shift + left-click a villager's trade output** to keep trading that offer with materials from your whole main inventory and hotbar.

- Stow refills both payment slots between trades until your materials, the villager's stock or your output space runs out.
- Any other click, a keypress (including Escape), scrolling, closing the menu or disconnecting cancels further trades.
- A change to inventory, offers, prices or item components also stops the batch.
- Each gesture is capped at **128 batches**. Follow-up batches are spaced four client ticks apart.
- Pinned payment slots follow vanilla merchant rules.
- **With the companion (1.0.4 or later on both sides):** the whole batch completes in one server operation instead. Limits still apply. The server checks the open menu, its revision, your empty cursor and the offer, and it sets all prices and items. Requests are accepted at most once every two server ticks. Completed instant trades can't be cancelled afterward. See [Companion features](#companion-features).

Other recipe outputs keep their vanilla Shift-click behavior. Those include crafting, the crafting table, stonecutter, smithing, loom, cartography table, grindstone and furnace. Stow's multi-slot Shift-drag and bundle-drag skip result slots, so a drag that crosses a trade output can't trigger repeated trades.

## Shulker box previews

Hover an undyed or colored shulker box to see its **27 slots in a 9 × 3 grid**, with item icons, counts and durability indicators. Empty slots keep their positions. The grid replaces the written item list. The name, lore and other item details remain.

- **Settings → Inventory → Shulker box previews** turns this on or off. It's on by default. Turning it off restores the written list.
- Previews use only the contents the server has already sent. Stow doesn't open boxes or look inside nested containers.
- If another mod already shows a tooltip image for the item, that mod's image takes priority.

## Command palette, settings and HUDs

- **Ctrl + K** (either Ctrl key) opens the command palette in the world, your inventory or any Stow menu.
  - Search actions, item names, remembered storage contents and project names.
  - Arrow keys select, Enter opens, and Escape or Ctrl + K goes back.
  - Shortcuts, keep amounts and chest glow are searchable too.
  - Opening an action from inside another Stow menu returns you to the inventory or world when you leave.
- **Settings** are split into tabs with search, reset buttons, and Save and Cancel. Edits apply together when you save.
  - **Inventory:** sorting, drags, pins, previews, refill, the block counter and tool options.
  - **Deposit:** Smart Deposit options and keep amounts.
  - **Equipment:** the Equipment HUD.
  - **Project totals:** the material HUD.
  - **Appearance:** Shared HUD size, the action notifications, the What's new screen and the Block count layout.
  - **Automatic attack:** the attack mode and interval.
- **Shortcut editor:** supports keyboard and mouse bindings with modifiers. Search actions, reset one, or press Delete while capturing to unbind it. Save applies changes. Back cancels them.
  - Conflicts with Minecraft or other mods are flagged.
  - Duplicate Stow bindings are disabled until you resolve them.
  - Shift and Ctrl are normalized for keyboard and mouse shortcuts.
  - Most new actions start unbound, so Stow doesn't take extra keys.
- **Action notifications** appear at the top-right edge, above open menus. They cover saves, undo, imports, storage and project changes, sorting, deposits and tool replacement. You can turn them off in **Appearance**.
- **Scrolling:** wheel scrolling moves one row at a time, with a short smooth transition. Keyboard editing and scrollbar dragging respond immediately.
- **Remember the material page:** **Settings → Inventory → Remember inventory material page** chooses between restoring each project's scroll position for the session or starting at the top when you reopen the inventory.

## Companion features

The companion is an optional server-side mod. Install it on the server for these features. For single-player, install it locally next to the client. The companion adds no menus or HUDs of its own.

| Feature | Needs |
| --- | --- |
| Fast chest sorting | Stow client and companion **1.0.3+** |
| Instant villager trades | Stow client and companion **1.0.4+** |
| Storage owner labels and Claim area | Stow client and companion **1.0.5+** |
| Connected-log harvesting (Alt + S) | Stow client and companion |

Use the same companion version on the server. The current version is **1.0.6**.

### Fast chest sorting

- Sorts vanilla chests (including trapped and double chests), barrels, ender chests and shulker boxes in one server operation. The visible click queue disappears.
- Middle-click and Shift/Ctrl variants keep their selected order and stack combining.
- The server checks the open menu, current contents, slot permissions, stack limits and exact item and component totals, then applies the arrangement in a single tick.
- Your inventory and cursor are not touched. Other viewers receive normal storage updates.
- If the contents changed, the request is rejected safely.
- Limit: **54 storage slots and 24 KiB** per request.
- Larger requests, custom or restricted menus, player-inventory sorts, and missing or older companions use the normal click queue.
- A request with no reply times out after five seconds and is not replayed.
- To turn it off, set `fastChestSorting=false` in `config/stow-companion.properties` and restart. Use this if a protection or storage mod needs to intercept each click.

### Instant villager trades

- The whole-inventory Shift-click trade runs as one server task using vanilla merchant operations.
- Payment, stock and output-space limits apply, with the 128-batch cap.
- The server validates the menu, revision, empty cursor and offer, and supplies all prices and items.
- Custom merchant menus, older or missing companions, and servers with `instantVillagerTrading=false` in `config/stow-companion.properties` use the paced client trades.
- A missing reply times out after five seconds without being replayed.

### Connected-log harvesting

- With the companion, Alt + S finds the full connected group of logs of the same type, strips the group, then harvests it.
- Includes all vanilla log types, including poplar, plus crimson and warped stems. Other wood types, bark-sided blocks, leaves and unrelated groups are not included.
- Tall columns, long rows and dense piles are supported, with no fixed height or length cutoff.
- The starting log must be visible and reachable. Discovery never loads chunks, and it stops at unloaded boundaries.
- The count limit is set by `maxLogs` in `config/stow-companion.properties`. The default is **16,384** logs, with a maximum of 65,536.
- Survival requires enough durability for the whole group before starting. The check assumes no Unbreaking savings, so it is conservative.
- Vanilla loot, Fabric interaction and break callbacks, permissions and protections all apply.
- Logs already removed by Timber-style mods are skipped. Compatibility with a server's Timber mod depends on that mod.
- Changing or protecting a block, a player or tool change, or entering an adventure or spectator mode stops the batch. Cancelling keeps completed changes.

### Storage owner labels

- Storage placed by a player with the companion is labeled with that player's name and UUID. This includes chests, trapped chests, barrels and shulker boxes. Labels are saved with the container and survive restarts.
- Storage that was already there starts as **Unclaimed**. Opening it does not claim it.
- In the storage list you can filter by Everyone, Mine, Shared, Unclaimed, Mixed (double chests with different owners) or a single player. Project totals are not affected.
- **Rename → Storage owner** claims unclaimed storage, marks your open storage as Shared or restores a player label.
- Only the labeling player or an operator can change an existing label. Double chests with different owners need authority over both halves.
- Label changes require the storage menu to be open, normal access and the container within eight blocks. The server handles at most 32 requests per player per 20 ticks. No item contents are sent, and remote chunks are never loaded.
- Labels are for organizing. They don't lock containers, grant access or reveal unopened contents. They don't identify who placed older storage.

### Claim area

- **Storage → Claim area → Fly and select corners** opens a local free camera. Your character stays put and normal server physics apply.
- **Controls:** the movement keys (default W/A/S/D), jump, sneak and sprint all fly the camera. Ctrl speeds it up. Enter moves on to the next step.
- **Step 1 (footprint):** aim at a block within 32 blocks. Left-click sets corner 1 and right-click sets corner 2. With corner 1 set, right-click moves straight to the height step, as long as the footprint is valid.
- **Step 2 (height):** the scroll wheel or Up/Down moves the top. Alt with the wheel or arrows moves the bottom. Ctrl steps by five. Left-click or Enter confirms, and Backspace goes back to the footprint.
- **Review:** check the box, then press **Claim unassigned storage in this box**. Nothing is claimed until you press that button.
- **Limits:** up to **262,144 positions**, with corners within **64 blocks** of you on each axis. Only loaded chunks are scanned. Unloaded positions are skipped.
- **Batching:** at most eight jobs are active, one per player, with a combined budget of 512 positions and eight containers per server tick.
- **Scope:** only unclaimed chests, trapped chests, barrels and shulker boxes are claimed. Existing owners and Shared labels are always kept, even for operators. Locked or inaccessible containers are skipped. Double chests need both halves loaded, inside the box and unclaimed.
- **Cancelling** keeps the claims already made. Moving more than eight blocks from where the scan started, changing dimension or disconnecting stops the scan.
- Claims are saved with the containers and survive restarts. They set a label only. They don't identify historical placers or reveal unopened contents.

## Shortcut reference

Default shortcuts. Each one can be changed in the Shortcut editor.

| Action | Default |
| --- | --- |
| Open the command palette | Ctrl + K |
| Sort the hovered inventory or chest | Middle-click |
| Sort by combined quantity | Shift + middle-click |
| Sort by displayed name | Ctrl + middle-click |
| Move matching stacks | Shift + left-drag |
| Move every crossed stack (Both mode) | Ctrl + Shift + left-drag |
| Pin or unpin a hovered slot | P |
| Search your inventory | Ctrl + F (Escape clears it) |
| Track a material | N (hover the item) |
| Prioritize a material on the HUD | H (hover the floating counter) |
| Smart Deposit | Hopper icon (no default key) |
| Pick the best tool for a world block | Middle-click (Pick Block binding) |
| Toggle auto tool | Alt + T |
| Toggle automatic attack | Alt + A |
| Toggle auto-mending | Alt + M |
| Strip or harvest connected logs | Alt + S |
| Trade the whole inventory | Shift + left-click the villager output |
| Fill a held bundle quickly | Shift + left-click or drag across items |
| Unpack a held bundle quickly | Shift + right-click or drag across empty slots |
| Toggle chest glow | Eye icon in the inventory |

## Limits at a glance

| Item | Limit |
| --- | --- |
| Material types per project | 1,024 |
| Target per material | 1,000,000 items |
| Import size | 65,536 characters and 2,048 lines |
| Refill threshold | 0–16 |
| Automatic attack interval | 50–60,000 ms (at most 20 clicks per second) |
| Villager trade batches per gesture | 128 |
| Fast chest sorting request | 54 storage slots and 24 KiB |
| Connected-log count (`maxLogs`) | Default 16,384; maximum 65,536 |
| Claim area | 262,144 positions; corners within 64 blocks per axis |
| Companion requests per player | 32 per 20 ticks (labels) |

## Building from source

Install **JDK 25**, then run `./gradlew build` on Linux or macOS, or `gradlew.bat build` on Windows. The JAR is written to `build/libs/`. The Gradle wrapper downloads its own Gradle version. The build uses Fabric Loom 1.17 with Minecraft 26.3's unobfuscated classes, so no mappings are needed.

- **Client dependencies:** Minecraft, Fabric Loader, Fabric API and Cloth Config. Mod Menu is compile-only and optional at runtime.
- **Companion dependencies:** Minecraft, Fabric Loader and Fabric API. It contains no client inventory, HUD or configuration code.
- Run `python3 dev-tools/check-project.py` to check metadata, translation keys and assets.

For reference: [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html).

## Credits and licenses

- **Mouse Wheelie** by Siphalor and contributors (Apache License 2.0). Stow's stack combination, chained-permutation client sorting, creative catalogue lookup and sort modes are adapted from Mouse Wheelie. Stow's storage and inventory utilities were extracted from a custom Mouse Wheelie port and modified for Stow. The attribution is in [NOTICE](../NOTICE) and [LICENSE](../LICENSE).
- **Material Design Icons** by Pictogrammers and contributors (Apache License 2.0). The 16-pixel inventory and menu icons are rendered from the selected SVGs in `design/icons/mdi/`.
- **Cloth Config** is a separate, required dependency used through its public API. Mod Menu is optional.
- Stow's icon and branding are in `src/main/resources/assets/stow/`.

Stow is an independent project and is not affiliated with Mojang or Microsoft. Minecraft and its assets are not included.
