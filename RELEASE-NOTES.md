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
