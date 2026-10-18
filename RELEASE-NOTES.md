# stow 1.0.4-beta.2+mc26.3 — 18 October 2026

Published as a **beta** while actual modpack/server and physical-input compatibility remain unverified. The updated companion adds instant bulk villager trading. Update both JARs for acceleration; older or absent companions retain paced client trading.

Final beta 2 validation: **130 isolated Minecraft checks** passed for the exact client and companion artifacts. Actual modpack/server and physical-input compatibility remain unverified.

### Instant trades with Stow companion

- **New behavior:** with the updated client and companion, Shift + left-click a villager output trades from the whole inventory in one server task. Vanilla merchant selection and quick-move logic determine payments, outputs, experience and stock. No client-supplied item stacks or prices are accepted.
- **Limits:** the server checks the open merchant menu, its revision, cursor and selected offer. It stops for exhausted stock, insufficient payment or result capacity, invalid menus and no progress, with a hard limit of 128 refill/quick-move batches per request. Requests are rate-limited to one per two server ticks. Work already completed instantly cannot be cancelled by a later click.
- **Compatibility:** older/absent companions and servers setting `instantVillagerTrading=false` in `config/stow-companion.properties` (restart required) use the paced client fallback. Requests that receive no reply time out after five seconds without replay. Custom merchant menus are excluded from server acceleration. Pinned payment/result positions follow vanilla merchant behavior. Existing data and companion settings are preserved; the new server option defaults to true.

### Compact hover descriptions

- **Problem:** long help text made Stow's hover panels bulky, especially the new attack settings. Shulker boxes also repeated their contents as text below the full preview grid.
- **New behavior:** English and German hover help for attack, previews, refill, sorting, tool replacement, log stripping and project completion now uses short summaries. Detailed behavior remains in the feature guide. When Stow's shulker grid is displayed, it replaces the duplicate written contents list; the name, custom lore and other item details remain visible.
- **Compatibility/settings:** text size and GUI scale are unchanged. Disabling shulker previews restores vanilla's written contents. Other containers and another mod's existing tooltip image retain their existing behavior. No new setting, companion update or data migration is needed.

Validation: the **1.0.4-test.2+mc26.3** development client builds successfully. Project checks pass with 414 bilingual keys. An isolated Minecraft client passed **56 tooltip checks**, including duplicate-list removal, retained title/lore, restoration of the written list when previews are disabled, ordinary container text, all box colors and existing preview protections. The rendered compact tooltip screenshot was inspected. Actual multiplayer modpack compatibility remains unverified.

### Auto-mending across the inventory

- **New control:** **Alt + M** toggles auto-mending; change or unbind it in the shortcut editor, or use **Toggle auto-mending** in the command palette. It starts off and stops when the eligible inventory cycle finishes.
- **Repair behavior:** damaged items with **Mending** are temporarily moved from the main inventory/hotbar into the offhand, most missing durability first. Collect normal XP to repair them. Each fully repaired item returns to its original position before the next is selected, and the original offhand item is restored. A damaged Mending item already in the offhand is repaired first. The selected main-hand slot stays available for farming; worn armor and the held main-hand item retain vanilla Mending behavior. No XP is generated and items without Mending are skipped.
- **Protection/interruption:** pinned donors and the selected hotbar slot are skipped. Names, enchantments and other components are preserved. Inventory operations and an occupied cursor delay swaps. Toggle off, opening menus, focus loss, pause, death or spectator mode stop activation; a borrowed item is returned once normal gameplay and a safe empty cursor allow it. Changing the borrowed item, its reserved position or selecting that hotbar position stops the cycle without overwriting the new state.
- **Disconnect limit:** disconnecting clears activation. If a connection closes before a return swap can be sent, the borrowed tool remains in the offhand and the previous offhand item remains in its reserved inventory position. No automatic cycle resumes on reconnect. No companion or saved-data migration is required.

### Shulker box previews

- **New behavior:** hover a shulker box to see a 9 × 3 grid of its 27 inventory slots in the tooltip, including empty positions, item icons, stack counts and item durability indicators. Undyed and all 16 dyed boxes are supported; empty boxes show an empty grid. The grid replaces the duplicate written contents list; names, lore and other item details remain available.
- **Setting:** **Settings → Inventory → Shulker box previews**, enabled by default. No Shift key or companion is required and existing settings files receive the default automatically.
- **Compatibility/limits:** previews read the contents component already present on the item; they do not open containers, fetch hidden server contents, move items or load nested containers. Only the first 27 positions are shown. Hidden tooltips/hidden container details remain hidden, and an existing tooltip image supplied by another mod takes priority. Contents and components are not changed.

### Hold Attack or click at an interval

- **New controls:** **Alt + A** toggles automatic Attack while playing. Choose **Settings → Automatic attack → Attack mode → Hold Attack / Interval clicks**. Hold keeps the normal Attack binding pressed (continuous mining and one initial attack); Interval sends repeated normal Attack clicks. The shortcut can be changed or unbound in Stow's shortcut editor, and the command palette also includes **Toggle automatic attack**.
- **Timing:** the interval is editable from **50 to 60000 milliseconds**, default **500 ms**. Clicks run on game ticks, at most 20 per second, without catch-up bursts after delays. Vanilla miss delays, attack cooldowns, item-use handling and Stow's tool protection still apply; this does not make a held attack repeatedly hit entities.
- **Stopping/settings:** toggle again to stop. Opening a menu/chat, switching away from the game, pausing, dying, entering spectator mode, disconnecting or shutting down stops it and releases any held Attack binding. Mode and interval are saved; activation is never restored automatically after menus, reconnects or restarts. Existing settings files receive the defaults without migration. No companion is required.

### Trade from the whole inventory

- **Previous behavior:** Shift-clicking a villager output used only the payments already loaded into the trade inputs, usually one stack. The 1.0.2/1.0.3 output-slot fixes preserved that vanilla limit; they did not implement trading across the whole inventory.
- **New behavior/control:** **Shift + left-click the trade output** to keep trading the selected offer, refilling both payment inputs from the main inventory and hotbar between batches. Trading stops when materials run out, the offer sells out, the output cannot fit, or a transfer makes no progress. Other recipe outputs retain their existing vanilla behavior; crossing a merchant output during Shift-drag still does not trade.
- **Interruption and bounds:** another click, a keypress (including Escape), scrolling, closing/changing the menu or disconnecting stops further batches. Unexpected inventory, offer, price or item-component changes also stop the operation. Without companion acceleration, follow-up batches are spaced four client ticks apart and 128 batches run per gesture, bounding unusual modded offers that return their own payment or replenish stock.
- **Compatibility/data:** uses vanilla merchant refill and transfer packets; no companion update, setting or saved-data migration is required. As with manually selecting and Shift-clicking a vanilla offer, merchant refills can use pinned payment stacks and result transfers can fill pinned inventory slots. The server remains responsible for trade availability and item matching.
- **Related fix:** Shift-clicking an offer-list button now reaches the vanilla button instead of starting an empty Stow Shift-drag. Ordinary inventory Shift-drag modes are unchanged.

Validation: JDK 25 Gradle build and project checks passed, including 414 English/German translation keys. The **1.0.4-test.1+mc26.3** development client passed **117 checks** inside an isolated Minecraft 26.3 client/server: 38 trading checks, 13 attack checks, 14 auto-mending checks using actual XP repair, and 52 shulker-preview checks. Actual preview screenshots were inspected. Published 1.0.3 baseline checks separately reproduced the single-input-stack limit. These earlier checks used a development build; the user's actual multiplayer modpack/server and physical-input compatibility remain unverified. See `docs/DEVELOPMENT.md` for evidence, test-fixture scope and the native-JVM startup workaround.

# stow 1.0.3+mc26.3 — 18 October 2026

## Faster chest sorting with the companion

Large chest sorts previously moved items through a queue of client clicks. With the updated client and companion, vanilla chests, trapped/double chests, barrels, ender chests and shulker boxes sort in one validated server operation. Existing middle-click, Shift and Ctrl sort orders, localized name order and stack combining are preserved. The server verifies the open menu, unchanged contents, slot permissions, stack limits and exact item/component totals before moving anything. Player inventory and cursor stay untouched.

Acceleration is automatic. Update both JARs, or install both locally for single-player. Player-inventory sorts, custom/restricted menus, older or absent companions and requests above 54 storage slots or 24 KiB use the existing queue. Changed snapshots stop safely; missing acknowledgements time out after five seconds without replaying the request. Server owners can set `fastChestSorting=false` in `config/stow-companion.properties` and restart if a storage/protection mod requires individual click handling.

## Refill firework rockets

**Refill blocks and rockets** now supports both hands, including rockets used for Elytra boosts. It waits for the server-confirmed rocket count decrease before moving reserves; unsuccessful uses never refill. The existing threshold is 0–16 (default 8), with 0 refilling after the final block or rocket. Rocket components, including flight duration and explosion data, must match. Backpack reserves are preferred and pinned donors are skipped. Opening a menu, changing the item type/components or the selected slot for a pending main-hand refill, a cursor item or an active sort/deposit cancels pending refill. This fix does not require the companion and keeps existing settings.

## Show armor durability as a percentage

Enabling **Show percentage only** now replaces the raw durability number in the equipment HUD with the percentage alone (for example, `4%`). Disabling it continues to show remaining durability alone (for example, `10`). The HUD layout preview follows the same setting. Low-durability colors and warning thresholds are unchanged, and the detailed Equipment Watch screen continues to show the percentage together with current and maximum durability. No saved-data migration is required.

## Finish and reopen a completed project

- **Materials → Finish project** unlocks when every material goal in the active project meets its target. Finishing keeps inventory and selected-chest totals live as you build, while counts and the project HUD change to a neutral color and each row says **N used** instead of showing planning warnings or **N missing**.
- **Reopen project** restores the normal planning colors. Finish and Reopen both support Undo. The finished state is stored per project; targets, chest selections, hidden goals and HUD priorities are preserved.
- **Settings → Project totals → Show finished project totals** is on by default. Turn it off to hide the active finished project's top-right HUD. Existing saved project files load as unfinished, with no reset to goals or chest selections.

## Prevent accidental repeat trades while Shift-dragging

- Stow's matching/all-item Shift-drag and bundle-drag gestures skip vanilla recipe and trade result slots. Dragging across a villager trade output no longer invokes its repeat-trade handler.
- Direct Shift-click on a result slot retains Minecraft's native behavior, including batch trading and crafting. Vanilla result transfers keep their menu rules, including how they can fill or merge into pinned slots.
- The drag safeguard adds no setting, control or item-count limit and does not require the companion.

For Minecraft **26.3**, Fabric Loader **0.19.5+** and Java **25**. The client requires Fabric API **0.161.0+26.3** and Cloth Config **26.3.159+**; Mod Menu **21.0.0** is optional. The companion requires Fabric API. Remove Mouse Wheelie before installing stow. Replace prior client and companion JARs rather than keeping multiple versions. Saved project and chest-memory data remains compatible. The companion adds fast chest sorting and connected-log harvesting; install both JARs locally for those features in single-player.

Validation: the JDK 25 build and project checks passed. The final Minecraft 26.3 client passed 112 release checks plus 37 Finish/Reopen checks; the acceleration-disabled queue passed 61 checks across all five sort orders. Eight block-refill regressions and both dedicated-server GameTests also passed. These include actual Elytra rocket use/refill in both hands, rendered HUD checks, villager gestures and client/server sorting. The actual multiplayer modpack and its Timber/protection mods remain unverified. Detailed evidence and compatibility limits are recorded in [the development notes](https://github.com/undrwtrsprite/stow/blob/v1.0.3%2Bmc26.3/docs/DEVELOPMENT.md).

# stow 0.5.13 — All log types and empty-stack refill

- **Alt + S** now strips every vanilla log type, including poplar, plus crimson and warped Nether stems. The server companion strips and then harvests connected logs of the same type as the starting log. Each log keeps its orientation while stripping.
- **Refill at this many items** now accepts **0**. Set it to 0 in Settings → Inventory to refill the same hotbar slot only after placing the last block in the held stack. The value is preserved after restarting.
- Updated English and German settings, tooltips and messages.

## Installation

For Minecraft **26.3**, Fabric Loader **0.19.5+** and Java **25**:

- Client: `stow-0.5.13+mc26.3.jar` — requires Fabric API **0.161.0+26.3** and Cloth Config **26.3.159+**. Mod Menu **21.0.0** is optional. Remove Mouse Wheelie before installing stow.
- Optional server companion: `stow-companion-0.5.13+mc26.3.jar` — requires Fabric API. Update the companion on the server to enable other log types for harvesting. For single-player, install both JARs locally.

Replace your previous JARs rather than keeping two versions installed.

## Checks

Both release builds passed. A headless Fabric server verified all 12 vanilla log/stem types, all orientations, correct drops and durability, adjacent-species isolation, cancellation, protection callbacks and Timber cascade behavior. Focused refill checks verified zero-threshold scheduling, matching reserve transfers and saved settings.

# stow 0.5.12 — In-game update notices

- Added a clickable in-game chat notice when a newer stable stow release for the running Minecraft version is available on GitHub, Modrinth or CurseForge.
- Checks run in the background while playing. Each new version is announced once in local settings. Successful checks repeat after 12 hours; unavailable services retry after an hour.
- CurseForge releases are checked through CFWidget's public metadata feed. Pending moderation is handled quietly, and CFWidget data may be cached for up to an hour. The check sends no player, world, server or account data.
- Removed regression suites and their launcher/workflow from the public repository while preserving local copies. Personal notes, publishing files and tests are excluded from future source archives and release packages.
- Client and companion are packaged as 0.5.12+mc26.3. The companion behavior and dependencies are unchanged.

# stow 0.5.11 — Oak stripping and companion harvesting

- Hold an axe, aim at a placed oak log and press **Alt + S** to strip nearby visible oak logs within normal reach. Press it again to cancel; the shortcut can be changed in the editor.
- Sneak is untouched. The optional **stow companion** adds server-side stripping followed by harvesting through the same Alt + S shortcut.
- Companion discovery follows connected oak logs through tall columns, long rows and dense piles, with no fixed height or length limit. The starting log must be visible and reachable. The group must be loaded and permitted; a configurable count limit defaults to 16,384 logs.
- The whole group is stripped before mining starts. Normal axe actions, break callbacks, durability and loot apply; logs already harvested by Timber are skipped. Exact compatibility with each Timber mod requires an in-game check.
- Without the companion, batches only strip up to 64 nearby visible logs, one server-confirmed use at a time, preserving log orientation and normal axe durability.
- Changing tools, opening a menu or starting another interaction cancels. Rejected or unconfirmed uses stop the batch without retries; the last durability point is protected.
- Added an Inventory settings toggle and a searchable world command-palette action, with English and German text.

Replace the old client JAR with `stow-0.5.11+mc26.3.jar`. For stripping **and mining**, install `stow-companion-0.5.11+mc26.3.jar` with Fabric API on the server. Full installation instructions, verification and limits are in [CHANGELOG.md](CHANGELOG.md).

# stow 0.5.10 — Automatic mining tools

- Added automatic hotbar tool selection when starting or continuing to mine. Stone picks a pickaxe, dirt a shovel and logs an axe; vanilla tool components, Efficiency and harvest requirements determine the choice.
- **Off by default**, including when upgrading. Enable it in Settings → Inventory → Auto-switch mining tools.
- **Alt + T** toggles it immediately, saves the choice and confirms on/off above the hotbar. Change the binding in the Shortcut editor, or search “auto tool” in the command palette.
- Hold Sneak to keep your chosen tool temporarily. Automatic selection leaves backpack items and slot contents in place. The existing manual middle-click tool picker remains independent.
- Creative pick block, open menus and unbreakable blocks retain their behavior.

Replace the old stow jar with `stow-0.5.10+mc26.3.jar`. Dependencies are unchanged. No server-side mod is needed.

# stow 0.5.9 — Quick bundles and focused equipment HUD

- Hold a bundle on the cursor and Shift-left-click or drag across stacks to fill it quickly. Filling respects capacity and preserves names/components.
- Shift-right-click or drag a held bundle across empty slots to unpack one entry per slot. The bundle stays on the cursor.
- Shift-left-drag from a placed bundle across empty or matching slots to unpack it. Shift-click without dragging still moves the whole bundle.
- Quick bundle gestures skip pinned slots, keep up with fast mouse movement, and can be disabled in Settings → Inventory → Quick bundle drag.
- Equipment HUD now shows only damageable hotbar items and worn armor. Backpack/offhand gear remains available in the equipment menu.
- Right-click a floating inventory material counter to show its source chest glow; right-click the same counter again to stop it. Hover help updates to match.
- Vanilla bulk Shift-crafting remains unchanged, including its pinned-slot exception. Existing data/settings upgrade without a reset.

Replace the old stow jar with `stow-0.5.9+mc26.3.jar`. Fabric API and Cloth Config remain required; Mod Menu is optional (21.0.0 supports Minecraft 26.3). No server-side mod is needed.
