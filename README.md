![stow icon](src/main/resources/assets/stow/icon.png)

# stow

**A quiet home for your items.** Stow brings inventory sorting, remembered storage, build planning and everyday tool helpers together for **Minecraft 26.3 on Fabric**.

Paste a materials list, find what you already own, gather the rest and keep track while you build. Sort chests, move stacks, refill blocks and rockets, preview shulkers, trade with villagers and repair your Mending tools without a collection of separate inventory utilities.

**The new trading, attack, shulker-preview and auto-mending features are available in 1.0.4 beta 2 and later.** They have been checked in an isolated Minecraft client/server; individual modpacks, server restrictions and protection mods still need compatibility testing.

## Keep your inventory in order

- **Five sort orders:** creative catalogue, name, quantity, item type and combine-only. Middle-click sorts; Shift and Ctrl select your alternate orders.
- **Move several stacks:** Shift-drag matching items, or use Ctrl + Shift-drag for every crossed item in Both mode. Result slots are excluded from drag gestures to prevent accidental crafting or trading.
- **Search and pin:** find inventory items and pin positions you want sorting, drag and deposit actions to leave alone. Ordinary Shift-transfers can also avoid pinned destinations.
- **Smart deposit:** move items into open storage immediately, with matching-only transfers, keep amounts and hotbar protection.
- **Quick bundles:** Shift-left-drag to fill a held bundle, Shift-right-drag to unpack it, or drag from a placed bundle. Capacity, item components and pinned positions are respected.
- **Shulker previews:** hover any undyed or colored shulker box for its 27-slot contents grid, including counts and durability indicators. The grid replaces the duplicate written item list; names and lore remain. Disable previews in Inventory settings if you prefer the normal list.

## Remember your storage

Stow remembers snapshots of chests, barrels and shulker boxes you open. Search those snapshots, rename storage, review contents and highlight the chests holding a material through walls. Right-click a floating material counter to show or stop its source glow.

Memory reflects the last time you opened a container. It cannot inspect unopened storage or see somebody else's changes until you reopen it. Forgetting a snapshot keeps its project selection; reopening refreshes its contents. Undo restores forgotten entries.

## Turn a materials list into a build plan

Open **Materials → Import** and paste ordinary text, including a YouTube materials list. Formats such as `128 Oak Logs`, `Glass x64` and `2 stacks of Stone` work without a schematic mod. Review the results, correct uncertain names, skip unrelated lines and choose whether to add quantities or replace matching targets. Duplicates combine and Undo reverses the import.

Create separate projects with their own material goals and counted chests. Totals can include your inventory, remembered storage or both. Newly discovered chests can join the active project automatically. Star the materials you want on the HUD, edit quantities and hide individual goals without deleting them.

**Finish project** becomes available when every goal is met. Counts keep updating as you use materials, with neutral colors and “N used” instead of planning warnings. **Reopen** restores normal planning colors. Both support Undo.

The paper icon copies every saved target as plain text with exact item IDs, ready to share or import into another project. Projects support **1,024 material types** and targets up to **1,000,000 items each**. Imports accept **65,536 characters and 2,048 lines**; unknown or ambiguous rows stay excluded until corrected.

## Keep building and look after your tools

- **Block and rocket refill:** top up the hand you used from matching inventory reserves, including offhand rockets and Elytra boosts. Choose a threshold from 0–16; 0 waits until the last item is used. Rocket flight and explosion data must match, and pinned reserves are skipped.
- **Block-count HUD:** see your remaining building stock beside the hotbar or place the counter elsewhere.
- **Tool selection:** middle-click a world block to choose a suitable hotbar tool. **Alt + T** toggles automatic mining-tool selection, which starts off. Hold Sneak to keep your own choice temporarily.
- **Tool replacement:** replace worn main-hand tools from inventory using **Keep 1 durability** or **After break**. Keep 1 preserves the old tool and blocks a damaging use without a suitable spare. Pinned spares, armor and offhand equipment are excluded from this replacement feature.
- **Equipment HUD:** watch hotbar-tool and worn-armor durability as remaining points or percentage only. The detailed equipment screen also shows backpack and offhand gear.

## Trade from your whole inventory

**Shift + left-click the villager's output** to keep trading the selected offer, refilling both payment inputs from your main inventory and hotbar between batches. It stops when materials, villager stock or inventory space run out. Another click, a keypress, scrolling or closing the menu stops further batches.

**With the updated Stow companion, these bulk trades complete instantly in one server operation.** Without it, Stow repeats normal transactions at a paced rate. Both paths stop at stock, payment and inventory-space limits, with at most 128 batches per gesture. The server controls all prices, experience and item matching. Instant work already completed cannot be cancelled afterward. Servers can disable acceleration with `instantVillagerTrading=false` in `config/stow-companion.properties` and a restart. Missing replies time out after five seconds without replay. Custom merchant menus use the client fallback. As with vanilla merchant selection and output transfers, payments and results can use pinned inventory positions. Other recipe outputs keep their normal Shift-click behavior.

## Repair Mending items while you collect XP

**Alt + M** toggles auto-mending. Damaged Mending items cycle from your inventory into the offhand, most missing durability first. Collect XP normally; Stow does not create XP or repair items without Mending. Each repaired item returns to its original position, and your original offhand item is restored.

The selected main-hand slot remains available for farming, and pinned donor positions are skipped. Worn armor and your main-hand item still receive normal Mending repairs. Toggle off to stop; menus, focus loss, pause, death and spectator mode also stop activation. A pending return waits until normal gameplay allows a safe swap. Manual changes stop the cycle without overwriting them. If a disconnect prevents the return packet, the borrowed tool and original offhand item remain in their current swapped positions; reconnecting does not restart the cycle.

## Hold Attack or click at an interval

**Alt + A** toggles automatic Attack. In **Settings → Automatic attack**, choose **Hold Attack** for continuous mining with an initial attack, or **Interval clicks** for repeated attacks. The interval is **50–60000 ms**, with a **500 ms** default. Clicks follow game ticks, at most 20 per second; normal combat cooldowns and item-use handling still apply.

Toggle again to stop. Opening a menu or chat, switching away from the game, pausing, dying, spectator mode or disconnecting stops it and releases held Attack. It stays off when you return.

## Optional companion: faster sorting and connected logs

The **Stow client works without a server-side Stow installation**. Add the separate [stow companion](https://modrinth.com/mod/stow-companion) for:

- **Fast chest sorting:** supported vanilla chests, trapped/double chests, barrels, ender chests and shulker boxes sort in one validated server operation. The server checks the current contents, permissions, stack limits and exact item/component totals; player inventory and cursor stay untouched. Custom menus, older companions and oversized requests keep the normal client queue. Requests are bounded to 54 storage slots and 24 KiB. Server owners can disable acceleration with `fastChestSorting=false` in `config/stow-companion.properties` and restart.
- **Connected-log harvesting:** hold an axe, aim at an unstripped log and press **Alt + S**. The companion strips and then harvests the connected group of the same type, including all vanilla logs and Nether stems. Tall columns, long rows and dense piles are supported; the configurable count limit defaults to **16,384 logs**. Reach, loaded chunks, permissions, durability and normal loot rules apply. Press again to cancel. Without the companion, Alt + S only strips nearby visible logs and does not harvest them.

For single-player, install both JARs locally to enable these companion features. Exact compatibility with individual Timber, storage or protection mods is not guaranteed.

## Make it yours

Open the command palette with **Ctrl + K**. Change shortcuts, sorting defaults and settings in-game. Drag and resize the material, equipment and block-count HUDs, choose their alignment and reset their placement. English and German translations are included, with compact hover help and brief action feedback.

Common controls: **P** pins a hovered slot, **N** tracks a material and **H** prioritizes a hovered floating material counter. New attack and mending toggles start off. All listed shortcuts can be edited or unbound.

## Install

For **Minecraft 26.3**:

1. Install **Java 25+**, **Fabric Loader 0.19.5+**, **Fabric API 0.161.0+26.3** and **Cloth Config 26.3.159+**.
2. Put the Stow **client** JAR in your client's `mods` folder. Replace older Stow JARs and **remove Mouse Wheelie**; the two mods are incompatible.
3. Optionally install **Mod Menu 21.0.0** for access to settings.
4. For companion features, put the separate companion JAR and Fabric API on the server. In single-player, put both Stow JARs in the client. The companion does not need Cloth Config or Mod Menu.

Existing Stow projects, chest memories, goals, selected chests and priorities are retained. Removing Stow stops its features; keep `config/stow/` if you want your saved plans and memories for a later reinstall. Client-only inventory helpers do not require the companion. Servers can impose their own restrictions on automated actions.

Stow checks public GitHub, Modrinth and CurseForge release listings for updates and shows a clickable notice. Those checks send no player, world, server or account data. Pending moderation can delay update notices.

## About Stow

I made Stow for my friends and our Minecraft server with AI assistance. Much of its code, UI, translations, documentation and branding was generated or edited with AI. I choose the features and test them, but this is a personal project and may have rough edges.

Inventory sorting is adapted from [Mouse Wheelie](https://github.com/Siphalor/mouse-wheelie) by Siphalor and contributors. Stow retains the **Apache 2.0** license and upstream attribution. Material Design Icons are credited in the source. Fabric, Cloth Config and Mod Menu are separate projects and are not bundled.

[Source and GitHub downloads](https://github.com/undrwtrsprite/stow) · [Detailed feature guide](https://github.com/undrwtrsprite/stow/blob/main/docs/FEATURES.md) · [Changelog](https://github.com/undrwtrsprite/stow/blob/main/CHANGELOG.md) · [Report a bug](https://github.com/undrwtrsprite/stow/issues)

This is an independent project, not affiliated with Mojang or Microsoft. Minecraft and its assets are not included.

## Build from source

Use JDK 25 and the included Gradle wrapper: `./gradlew build` on Linux/macOS or `.\gradlew.bat build` in Windows PowerShell. Client, companion and source JARs appear in `build/libs/`. `runClient` launches the development client.

[Development and validation](docs/DEVELOPMENT.md) · [Contributing](CONTRIBUTING.md) · [Historical versions](docs/HISTORY.md) · [License](LICENSE) · [Attribution](NOTICE)
