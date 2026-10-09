# Changelog

## Unreleased

## 1.0.0+mc26.3 — 7 October 2026

### Stow 1.0 — your build starts with a list

**Copy a materials list from a YouTube description, turn it into a project, find the supplies in your remembered storage, and keep building.** Stow 1.0 brings plain-text import and export together with a more compact Materials menu, Minecraft item icons, tabbed navigation, customizable HUDs and automatic tool replacement.

- **Paste, review, build:** import ordinary text without a schematic mod. Correct uncertain items, combine duplicates, choose how quantities affect existing goals and undo the whole import.
- **Share your plans:** copy all saved material targets as portable plain text with exact item IDs, including goals outside the current search or page.
- **See more, read less:** denser material rows, the familiar pixel priority star, short tooltips, helpful empty states and clearer storage cards.
- **Make the HUD yours:** choose alignment, position and size, see additional tracked materials, and read equipment durability as a simple countdown.
- **Keep moving:** refill building stacks, replace worn or broken main-hand tools, preserve vanilla crafting-result Shift-clicks and receive brief feedback above menu blur.

Client and companion are packaged as **1.0.0+mc26.3**, for Minecraft **26.3**, Fabric Loader **0.19.5+** and Java **25+**. The client requires Fabric API and Cloth Config; the companion requires Fabric API. Existing project data, chest selections and material priorities retain their saved format. The companion keeps the connected-log harvesting behavior from 0.5.13; the importer, menus and tool replacement belong to the client.

### Plain-text export and first-use guidance

- Added **Copy material list** as a paper icon beside Add and Import in the full Materials toolbar. It copies every saved target in the current project in project order, including hidden materials and goals outside the current search/page. Unsaved quantity edits are excluded; save the target first to export it. Copy is disabled for an empty project and reports the number of copied material types.
- Export uses a project-name heading and one `quantity namespace:item_id` line per material, for example `128 minecraft:oak_log`. Exact IDs work across language settings and avoid ambiguous translated names. The text can be pasted into Import and reviewed normally. It exports target quantities, not remaining requirements, inventory counts, chest locations, visibility or HUD priority. Import's existing character, line, target and project limits still apply.
- Empty Materials now points to **Import** or **+**, then choosing project chests. Empty Storage explains opening a chest, barrel or shulker box to remember its contents. Empty material-source views identify that no sources have been remembered and name the item to store/open. Searches with no results suggest another name or clearing the search; menus opened without a world explain joining one. Guidance uses a clear heading and short wrapped text in English and German without increasing row spacing.
- Enabling chest glow now shows **Glow on**, matching the existing **Glow off** feedback from inventory, Quick actions and settings. Both use the same short 1.1-second top-right notification and respect the Action notifications setting.
- The focused German UI pass found that Back and Undo both read “Zurück.” Undo now reads **Rückg.**, fitting the existing compact button and distinguishing it from navigation; its tooltip still names what will be restored.

### Materials importer

- Added **Import** to the Materials toolbar. Paste a whole YouTube description or another plain-text material list into a multiline editor, or use **Paste clipboard**, then select **Review list**. Project data changes only after confirming the review.
- Recognizes common quantity formats such as `32 Oak Planks`, `Oak Planks x32`, `Glass: 64`, `Glass (64)`, `2 stacks of Glass`, and `Glass: 2 stacks + 16`. Bullets and basic Markdown formatting are accepted. Comma and German period thousands separators are supported; quantities must be positive whole numbers.
- Resolves exact item names and registry IDs against installed items, accepting the active language and English vanilla names. Stack quantities use each item's real maximum stack size, including items that stack to 16 or 1.
- Review keeps the original source line visible. **Edit** lets players select an exact item from search results and correct its quantity; individual rows can be included or skipped. Unrecognized or ambiguous names, invalid quantities and ordinary description text are excluded from import until corrected. Substring matches never silently select an item variant.
- Duplicate included rows are combined by item ID. Choose **replace matching targets** (default) or **add quantities** for materials already in the active project. Review displays the resulting target before confirmation. Other project goals and their visibility preferences are preserved.
- The whole import is one **Undo** action. Quantity overflow and the 1,024-material project limit are checked before changing any goals, so a rejected batch cannot partially modify a project. Paste limits are 65,536 characters and 2,048 lines; each resulting material target is limited to 1,000,000 items.
- After import, Materials opens at the first imported goal with a success notification; the project button's tooltip retains the imported material count. The footer's Undo action restores the whole batch. Review separates item names from quantities and resulting targets to keep counts readable at small GUI sizes. Existing project files keep their saved-data format, chest selections and pinned-material choices.
- Added English and German labels, review guidance, errors and completion feedback.

### Visual and interaction polish

- Removed Equipment Watch from the Quick actions menu and its search results to simplify navigation. The on-screen equipment HUD and Equipment HUD settings remain available. Existing optional Equipment shortcuts continue to work.
- Material priority in the full Materials list uses the same 7×7 pixel star as the floating inventory material markers, centered in its button. Selected stars are gold, unselected stars gray and unavailable stars dimmed. This replaces the Nether Star item model and the smooth star texture; the configured priority limit, tooltips and saved choices are unchanged.
- The full Materials menu uses 42-pixel rows instead of 68 and a tighter toolbar. The smallest supported 320×240 GUI shows two rows instead of one; a 480-pixel-high GUI shows seven instead of four. Names, total counts, target editing, Save and a progress bar remain visible. Source chests, inventory visibility, HUD priority and removal use compact icon buttons with tooltips. Missing counts and the inventory/chest breakdown appear inline when space allows; hovering a total always reveals the breakdown and snapshot age. Hover the current project button for the material and priority counts. Searching, scrolling, paging, Undo and quantity drafts remain available.
- Removed the inventory sort-order button and closed its toolbar gap. Middle-click sorting and its Shift/Ctrl order variants remain; configure sorting in Quick actions → Settings → Sorting, or through Mod Menu.
- Shortened English and German import guidance, refill/tool protection, pinned transfer, storage, project and material visibility tooltips. Important limits and behavior remain in the relevant settings or documentation. Import review's Edit tooltip previews at most 140 source characters; editing still retains the complete original line.
- The world project HUD now shows **+N more materials** when visible material types do not fit within the configured 3–5 rows. The count uses the actual shown goals and excludes deliberately hidden goals. It counts material types, not remaining blocks.
- Replaced generic action glyphs with actual Minecraft item models throughout inventory buttons and Quick actions: chest for Storage, writable book for Materials, map for Projects, iron pickaxe for Equipment, command block for Quick actions, hopper for Deposit, repeater for Settings, spyglass for Find, shield for Keep amounts, and lever for Shortcuts. Drag modes use paper, shulker box and bundle. Glow uses an Eye of Ender/ender pearl; material priority uses a pixel star. Basic confirm, remove, plus/minus and page controls use small pixel symbols inside vanilla buttons. Hover, selected states, tooltips and keyboard narration remain.
- Materials, Storage and Projects have centered top tabs at every screen size, replacing the wide-screen left sidebar. Shared menus use a dimmed world, Minecraft menu texture, centered titles, spaced cards and consistent footer controls. Quick actions stays centered as its filtered list changes. Material row controls fit the minimum supported 320×240 GUI viewport.
- Removed horizontal chest-card separators that ran through item previews. Names, coordinates, counts and actions have separate space. Item preview backgrounds are 24×25 GUI pixels, spaced 26 pixels apart; counts fit within 22 pixels and stay inside their background, including large abbreviated amounts.
- Import review and its item chooser use the existing 160 ms cubic list easing, moving hitboxes and clipping, with a review scroll indicator. The paste editor also eases wheel scrolling; cursor navigation and scrollbar dragging remain immediate.
- Settings use native category tabs instead of expanded global headers, removing the misplaced gray category lines. Building/tool options and advanced creative-order caching are expandable groups. HUD editors are the single place for position, alignment and individual size, avoiding duplicated settings that could overwrite a saved layout.
- Recipe and trade outputs preserve their own vanilla Shift-click behavior, including player and table crafting, stonecutting, smithing, anvil, loom, cartography, grindstone, furnace and merchant outputs. Result transfers bypass Stow's pickup queue and custom Shift-drag handling. **Keep Shift-transfers out of pinned slots** still applies to ordinary transfers; vanilla result transfers can fill or merge into pinned slots according to the menu's rules.

### HUD layout and action feedback

- Added a short **Glow off** notification when disabling chest glow from the inventory, Quick actions or settings, stopping it in Storage, or toggling off an already highlighted chest/material. It uses the same top-right position and 1.1-second duration as other glow feedback and respects the Action notifications setting. Automatic cleanup on disconnect/world changes or forgetting a chest remains quiet so it does not replace those actions' feedback.
- Project material rows align right by default like the equipment HUD: icons share the right edge and each count sits to their left. Removed the artificial minimum content width that left short rows away from the edge.
- Equipment HUD numbers default to remaining durability only, for example `1361`, and count down as equipment wears. **Show durability percentage** optionally restores percentages. Low-durability colors and thresholds remain.
- Equipment, project totals and building stock each have **left / center / right** alignment in their live layout editor. Dragging, corner selection and snap-to-edge control position; Reset restores that element's defaults. Alignment applies to the icon/count group and headings.
- Added **Settings → Appearance → Block count layout** with independent 60–200% size and free positioning. **Beside hotbar** retains automatic hotbar/offhand placement; dragging or selecting a corner switches to **Free position**. Shared HUD size still scales all three elements, with individual sizes applied relative to it.
- Action notifications render at the top-right screen edge after open menus, above their blur, and during gameplay, with a 140 ms entrance slide. Glow feedback lasts 1.1 seconds, sorting completion 1.2 seconds and routine feedback 1.8 seconds; recognized blocked, failed or interrupted actions stay for 2.8 seconds. New actions replace the current notice immediately instead of building a queue. Repeated identical notices do not extend the timer. **Appearance → Action notifications** controls these independently of HUD visibility.
- Added feedback for import, targets, visibility and material pins, project creation/rename/switch/delete, chest inclusion/exclusion/rename/forget, all/none storage selection, Undo, saved layouts/settings/keep amounts/shortcuts, inventory pins, sorting and tool replacement. Existing deposit, chest-source, stripping/harvest and automatic-tool feedback uses the same notification system. Sorting distinguishes completion from an interrupted transaction.

### Tool replacement

- Added **Inventory → Building & tools → Replace worn or broken tools**, with **Keep 1 durability** (default) and **After break** timing.
- Keep 1 durability swaps the worn main-hand tool into the spare's slot before another damaging use. Without a suitable spare, Stow prevents that use and explains why. Higher-cost actions reserve enough durability to avoid breaking, so a tool may be saved with more than one point. Mining/attack costs use item components; fishing rods, carrot sticks and crossbows reserve their higher vanilla use costs.
- After break waits for the local player's actual main-hand break event, then fills the empty selected slot. Dropping, moving or placing an item does not count as a break; selecting another slot cancels the pending replacement.
- Replacements require the same item type, prefer matching names/components/enchantments, then backpack spares and higher durability. Pinned spares, armor/offhand slots and worn-out candidates are skipped. Armor and elytra held for equipping are excluded. The vanilla hotbar swap leaves the cursor empty and keeps the worn tool.
- Automation waits during menus, sorting or deposit, and does not run for dead, creative or spectator players. Mining-tool selection avoids reserved tools while Keep 1 durability is enabled. Normal block interactions are evaluated before protecting a tool's use-on-block action.
- This client helper replaces main-hand items. Worn armor and offhand items are excluded. Server damage to an item already in use and custom modded durability costs require game-specific compatibility checks.

### Chest memory and material refresh

- Added a Forget control to project chest-selection cards, including the material's source-chest view. These cards previously offered only inclusion, glow and Rename, so forgetting required switching to the general Storage view. The compact X button next to Glow forgets that chest's saved contents; its tooltip distinguishes this from excluding it from a project. Both storage views use the existing Forget notification and Undo. Card headings and item previews reserve space for the controls. Material goals and the remembered project storage selection are retained for rediscovery.
- Forgetting a chest removes its remembered contents without deleting project material goals or changing that location's project storage selection. Missing chests contribute zero to totals and the selected-chest count.
- Rediscovering a previously selected chest restores its contribution using fresh contents. Its selection survives saving and restarting.
- Newly discovered or forgotten-and-reopened chests are assigned to the **active project** immediately by default, including projects with an empty custom selection. **Assign discovered chests to current project** in Project totals can disable this. Refreshing a known excluded chest leaves it excluded; other projects retain their own selections. **None** clears storage selections, and with auto-assignment disabled they stay empty on rediscovery. Notifications distinguish remembering a chest from adding it to the project.
- Forgetting the currently open chest suppresses further snapshots from that menu until a chest is reopened or Forget is undone, preventing the forgotten snapshot from immediately reappearing during a material-count refresh. Undo Forget resumes live updates and preserves a newer rediscovered snapshot.
- Materials refreshes its rows and controls when chest memory changes, including forgetting, rediscovery and updated contents, while keeping drafted quantities.

## 0.5.13+mc26.3 — 6 October 2026

- Alt + S now supports every vanilla log type, including poplar, plus crimson and warped stems, in both client stripping and companion harvesting. Batches select the same block type as the starting log and preserve each log’s orientation.
- “Refill at this many items” now accepts **0**, refilling after the last block in the held stack is placed. The setting keeps its value after restarting.

## 0.5.12+mc26.3 — 6 October 2026

- Added a clickable in-game chat notice when a newer stable stow release for the running Minecraft version is available on GitHub, Modrinth or CurseForge.
- Checks run in the background while playing. Each new version is announced once in local settings. Successful checks repeat after 12 hours; unavailable services retry after an hour.
- CurseForge releases are checked through CFWidget's public metadata feed. Pending moderation is handled quietly, and CFWidget data may be cached for up to an hour. The check sends no player, world, server or account data.
- Removed regression suites and their launcher/workflow from the public repository while preserving local copies. Personal notes, publishing files and tests are excluded from future source archives and release packages.
- Client and companion are packaged as 0.5.12+mc26.3. The companion behavior and dependencies are unchanged.

## 0.5.11+mc26.3 — 5 October 2026

### Added

- **Alt + S** strips placed oak logs while holding an axe and aiming at an unstripped oak log. The shortcut is editable; press it again to cancel. Sneak is not used, so the binding does not overlap a Timber mod's Sneak control.
- Optional **stow companion** for Fabric servers. With the companion installed, Alt + S strips **and then harvests** the connected oak-log group. It follows touching faces, edges and corners through tall columns, long rows and dense piles, with no fixed height, length or distance cutoff from the starting log. The starting log must be visible and within normal reach.
- The companion strips the entire discovered group before starting any mining. An existing Timber mod can therefore encounter stripped logs when mining starts. Logs already removed by another mod are skipped rather than dropped twice.
- Server processing batches work without waiting for a separate client acknowledgement for each log. Small groups can finish in one server tick; larger groups take multiple ticks to keep the server responsive.
- English and German messages for completion, cancellation and rejected requests.

### Installation

- Player: install `stow-0.5.11+mc26.3.jar` with Fabric API and Cloth Config.
- Server: install `stow-companion-0.5.11+mc26.3.jar` with Fabric API. The companion does not require Cloth Config, Mod Menu or the client stow JAR.
- For single-player testing, install both JARs on the client to enable the companion in the integrated server.
- Both artifacts target Minecraft **26.3**, Fabric Loader **0.19.5+** and Java **25+**.

### Limits and behavior

- Without the companion, Alt + S only strips up to 64 nearby, visible oak logs within normal reach. It does not mine them.
- Only unstripped **oak log** groups are selected. Other wood species, oak wood blocks, leaves and separate piles are excluded. Existing stripped logs do not connect separate groups.
- Survival and creative are supported. Survival uses normal axe durability and drops. Before stripping, the companion reserves enough durability for both stripping and mining, leaving one durability point; it conservatively does not credit Unbreaking savings. Creative uses normal creative behavior, including no block drops.
- Protected blocks, changed blocks, tool changes, death, dimension changes, container use and disconnects stop or reject work. Completed changes remain when a batch is cancelled; cancellation cannot undo logs already harvested.
- The companion never loads terrain to expand a group. A group touching unloaded terrain is rejected before changes begin. Load the area first for very long rows.
- The default server limit is **16,384 connected logs**, configurable from **1–65,536** in `config/stow-companion.properties` (`maxLogs`). This count limit protects the server; it is not a height or length limit.
- Compatibility with Timber and protection mods should be checked in-game. Mods that only react to client mining packets may behave differently from mods that use normal block-break callbacks.

The client and companion are released together.

### Verification

- Local Gradle build passed for both client and companion JARs; metadata, 292 English/German keys and artifact separation checks passed.
- Headless Fabric server checks passed for a combined 100-log row, 96-log column and dense pile, exact stripped-log drops, durability, cancellation, creative mode, changed blocks, count limits and Fabric interaction/break vetoes.
- A simulated Timber cascade verified that all logs were stripped before the first break and no duplicate drops were produced. The server's specific Timber mod was not available for this test.
- Separate discovery checks passed for a 2,000-log row, a 384-log column, diagonal connections, isolated groups, unloaded boundaries and incremental work budgets.
- Manual in-game testing confirmed the combined client and companion behavior before release.
