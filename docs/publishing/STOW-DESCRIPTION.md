# stow

**A quiet home for your items.** Inventory sorting, chest memory and material tracking for Minecraft 26.3 on Fabric.

stow brings the inventory, storage and building helpers we wanted for our friends' server into one client mod. Its inventory features work without a server-side stow installation. An optional **stow companion** adds fast server-side oak stripping and harvesting.

## Keep your inventory in order

- Sort items with five sorting orders and matching/all-item drag gestures.
- Search your inventory, pin slots and protect the items you want to keep.
- Deposit items into storage immediately, with matching-only transfers and configurable keep amounts.
- Fill and unpack bundles with Shift-drag gestures.

## Remember your storage and plan your builds

- Search snapshots of chests, barrels and shulker boxes you have opened.
- Rename remembered storage, preview its contents and highlight item sources through walls.
- Create material goals and projects with their own selected chests.
- Track progress with live inventory counts and adjustable HUD counters.

Chest memory is based on your last visit. It cannot read unopened storage or see someone else's changes until you reopen it.

## Build and use tools

- Refill your selected building stack from matching reserves and see remaining block counts beside the hotbar.
- Middle-click a block to select a suitable tool.
- Optional automatic hotbar tool switching while mining is off by default. Toggle it with **Alt + T**.
- Show durability for hotbar tools and worn armor in an adjustable equipment HUD.

## Strip and harvest oak logs with Alt + S

Hold an axe, aim at an unstripped oak log and press **Alt + S**.

- **With stow companion on the server:** strip the whole connected oak-log group, then harvest it. Tall columns, long rows and dense piles are followed without a fixed height or length cutoff.
- **Without the companion:** only strip nearby, visible oak logs within normal reach; no mining occurs.

The companion strips every selected log before mining starts, helping Timber mods encounter stripped logs. Press Alt + S again to cancel. **Sneak is not used by this action.** The shortcut is editable.

The starting log must be visible and reachable. The group must be loaded and permitted, and the axe must have enough durability. A configurable server count limit defaults to 16,384 logs. Small groups can finish in one server tick; larger groups are processed in batches. Normal survival durability and loot apply, while creative retains normal creative behavior. Cancellation keeps changes already completed. Other wood species, leaves, oak wood blocks and separate groups are excluded.

## Controls and customization

Use **Ctrl + K** for the command palette, middle-click for inventory sorting, **P** to pin a hovered slot and **N** to track a material. Shortcuts, HUD positions and settings can be changed in-game. English and German translations are included.

## Installation

Install the **stow client JAR** in your client's `mods` folder.

- Minecraft **26.3**
- Java **25+** and Fabric Loader **0.19.5+**
- Fabric API **0.161.0+26.3** and Cloth Config **26.3.159+** required
- Mod Menu **21.0.0** optional

**Remove Mouse Wheelie before installing stow; the two mods are incompatible.**

For connected oak harvesting, install the separate **stow companion JAR** with Fabric API on the server. For single-player, install both JARs on the client. The companion does not need Cloth Config or Mod Menu.

## About the project

I made stow for my friends and our Minecraft server with AI assistance. **Much of its code, UI and documentation was generated or edited with AI.** I choose the features and test them, but this is a personal project and may have rough edges.

The sorting system is adapted from **Mouse Wheelie** by Siphalor and contributors. stow retains the Apache 2.0 license and upstream attribution. It is an independent project, not affiliated with Mojang or Microsoft.

[Source and companion downloads](https://github.com/undrwtrsprite/stow) · [Changelog](https://github.com/undrwtrsprite/stow/blob/main/CHANGELOG.md) · [Report a bug](https://github.com/undrwtrsprite/stow/issues)
