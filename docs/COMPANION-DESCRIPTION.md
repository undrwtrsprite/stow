# stow companion

**Optional server support for Stow's instant bulk villager trading, fast chest sorting and connected-log harvesting.** For Minecraft **26.3 on Fabric**.

Install the [Stow client](https://modrinth.com/mod/stow) for the inventory interface and controls. The stable companion adds three server features; it does not add its own client menus, HUDs, attack controls or Mending cycle.

## Player labels for storage — development client/companion only

The unreleased **1.0.5 test client and matching companion** record who places vanilla/trapped chests, barrels and shulker boxes. The player's UUID/name is saved with the placed container in its chunk; removal/replacement removes the old association, and a moved box is labeled with its new placer. Existing storage is **Unclaimed**, not automatically assigned to the first opener.

Stow's storage list displays and filters by player, Mine, Shared, Unclaimed and Mixed owners. Open storage, then choose **Rename → Storage owner** to claim older storage, mark your storage shared or restore its player label. Shared storage retains its assigning player's authority. Existing labels can be changed only by that player or an operator; mixed double chests require authority over both halves. Labels organize the list and do not lock containers, change project counts, grant access or share remembered contents with other players.

The matching **test.6 or later development pair** also offers **Storage → Claim area**: use a detached local camera to select two block corners, review the box, then assign its older unclaimed storage to yourself without replacing blocks. This cannot recover historic placers. Every existing owner/Shared label is skipped, including for operators; double chests require both unclaimed halves inside the selection. The companion saves claims with the containers in their chunks. It does not discover unopened contents. The explicit finite scan never loads remote chunks: at most 262,144 positions, corners within 64 blocks per axis, eight simultaneous jobs/one per player, and a combined 512-position/eight-container budget per server tick. Cancel retains completed claims; moving more than eight blocks from the starting point, switching dimension or disconnecting stops the job. Unloaded positions are skipped.

Individual label changes require the real supported storage menu to remain open, normal menu/access checks and a loaded container within eight blocks. Requests carry no item contents and never load remote chunks; the server handles at most 32 requests per player per 20 ticks. Labels refresh on opening or viewing nearby remembered storage; saved remote labels can be stale. Names reflect the last recorded name; UUIDs identify players. Ender chests, minecarts and custom containers are excluded. Clients without the matching companion keep normal features and last saved labels, with ownership editing unavailable. Full-modpack and dedicated multiplayer validation remain pending; this is not a published release.

## Trade your whole inventory instantly

With Stow **1.0.4 or later** on both sides, **Shift + left-click a villager trade output** to trade every available payment stack in one server operation. The normal merchant selection and quick-move code handles both payment items, results, experience and stock. The 1.0.4 release also tops up partial payment stacks between batches. Trading stops when payment runs out, the offer sells out or the result cannot fit.

The server checks the open merchant menu and its revision, empty cursor and valid offer. Requests contain no proposed prices or item stacks. Each request is limited to **128 refill/quick-move batches** and one request per two server ticks. Completed instant work cannot be undone by a later cancellation. Pinned payment and result slots follow vanilla merchant behavior.

Custom merchant menus, old/absent companions and servers disabling acceleration retain the client's paced trades. Set `instantVillagerTrading=false` in `config/stow-companion.properties` and restart to disable it. Missing replies time out after five seconds without retrying an uncertain transaction.

## Sort supported storage in one operation

With a compatible Stow client and companion, sort vanilla chests, trapped/double chests, barrels, ender chests and shulker boxes using the normal Stow controls. Stack combining and all five sort orders are retained.

The server validates the open menu, distance, unchanged contents, cursor, slot permissions, stack limits and exact item/component totals before applying the result. Player inventory and cursor stay untouched; other viewers receive normal updates.

Requests cover at most **54 storage slots and 24 KiB**. Player-inventory sorts, custom/restricted menus, older companions and oversized requests retain the client's normal click queue. Changed snapshots stop safely. A missing reply times out after five seconds without replaying the request.

Server owners whose storage/protection mods need every individual click can set `fastChestSorting=false` in `config/stow-companion.properties` and restart.

## Strip and harvest connected logs

Hold an axe, aim at an unstripped log and press **Alt + S** through the Stow client. The companion follows the connected group of the same log type, strips the selected group, then harvests it. All vanilla log types, including poplar, plus crimson and warped Nether stems are supported. Tall columns, long rows and dense piles have no fixed height or length cutoff.

The starting log must be reachable, and the group must be loaded and permitted. A configurable count limit defaults to **16,384 logs**. Normal survival durability, loot and server break/use callbacks apply. Other species, leaves, wood/hyphae and unrelated groups do not extend the selection. Already removed logs are skipped, including logs removed by a simulated Timber-style callback cascade. Exact compatibility with individual Timber and protection mods still needs testing on that installation.

Press **Alt + S** again to cancel; work already completed is retained. Changing tools or opening a menu also stops the action. Sneak is not used by the shortcut, and the binding can be changed in the client. Without the companion, Stow's Alt + S only strips nearby visible logs and does not harvest them.

## Install

- **Server:** Minecraft **26.3**, Java **25+**, Fabric Loader **0.19.5+**, Fabric API **0.161.0+26.3**, and the companion JAR in `mods`.
- **Players using the features:** install the Stow client with Fabric API and Cloth Config **26.3.159+**. Mod Menu **21.0.0** is optional.
- **Single-player:** install both the Stow client and companion locally.

The companion does not require Cloth Config, Mod Menu or Mouse Wheelie. Replace older companion JARs instead of keeping several versions. Update both JARs to 1.0.4 or later for instant trades and the payment refill fix. Existing server settings are preserved, and the instant-trading option defaults to enabled.

Stow's other inventory, storage-memory, material-planning, refill, shulker-preview, attack and auto-mending helpers belong to the client. They do not require this companion. This project does not change which servers permit automated actions.

## About the project

Stow was made for friends and a personal Minecraft server with substantial AI assistance. Much of the code, text, translations and branding was generated or edited with AI. It is a personal project and may have rough edges.

The related client includes sorting code adapted from Mouse Wheelie by Siphalor and contributors. Stow and its companion use the **Apache 2.0** license; original attribution is retained in the repository. Fabric and other dependencies are separate projects.

[Stow client](https://modrinth.com/mod/stow) · [Source and GitHub downloads](https://github.com/undrwtrsprite/stow) · [Changelog](https://github.com/undrwtrsprite/stow/blob/main/CHANGELOG.md) · [Report a bug](https://github.com/undrwtrsprite/stow/issues)

This is an independent project, not affiliated with Mojang or Microsoft. No Minecraft game files are included.
