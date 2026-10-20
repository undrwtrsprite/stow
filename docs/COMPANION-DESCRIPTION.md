# stow companion

**Optional server support for Stow's instant bulk villager trading, fast chest sorting, connected-log harvesting, storage owner labels and Claim area.** For Minecraft **26.3 on Fabric**.

Install the [Stow client](https://modrinth.com/mod/stow) for the inventory interface and controls. The companion adds server support for these features; it does not add its own client menus, HUDs, attack controls or Mending cycle.

## Companion 1.0.6

Companion 1.0.6 matches Stow 1.0.6 and has no functional changes. The 1.0.6 client updates (area-camera fix, two-step selection, controls panel, F1 and the What's new screen) are client-only and send the same Claim area requests, so companion 1.0.5 keeps working with them.

## New in companion 1.0.5

The companion was updated for Stow 1.0.5. It adds **storage owner labels** and **Claim area**. Both the client and companion need **1.0.5 or later** for them. Instant trades, fast chest sorting and connected-log harvesting work as before.

## Label who owns your storage

With the companion installed, newly placed chests, trapped chests, barrels and shulker boxes are labeled with the placing player's name and UUID. The label is saved with the container in its chunk, survives restarts and disappears when the container is removed. A shulker that is picked up and placed again is labeled with its new placer. Existing storage starts **Unclaimed**; opening it does not claim it.

In the Stow client, storage cards show a player, **Shared**, **Unclaimed** or **Mixed** owners, and an **Owner filter** narrows the list to Everyone, Mine, Shared, Unclaimed, Mixed or a single player. Players can open older storage and use **Rename → Storage owner** to claim it, mark it Shared or restore its player label. Only the labeling player or an operator can change an existing label. Double chests with different owners need authority over both halves.

Label changes require the real storage menu to stay open, normal access checks and a loaded container within eight blocks. Requests carry no item contents and never load remote chunks. The server handles at most 32 requests per player per 20 ticks. Ender chests, minecarts and modded containers are not labeled.

Labels are for organizing. They do not lock containers, grant access, change project counts or share remembered contents with other players.

## Claim older storage by area

**Storage → Claim area** lets a player fly a local free camera, select two block corners, review the box and claim its unclaimed storage in one step, without breaking and replacing it. The camera runs only on the client; the player's character stays put with normal server physics.

When the player confirms, the companion scans the box:

- Up to **262,144 positions**, with both corners within **64 blocks** per axis of the player.
- Only already-loaded chunks; it never loads remote chunks or reads remote inventories. Unloaded positions are skipped.
- In small batches: at most eight active jobs (one per player), a combined 512 positions and eight containers per server tick, and one start request per 20 ticks. Nothing scans automatically.
- Only fully unclaimed chests, trapped chests, barrels and shulker boxes. Existing owners and Shared labels are always skipped, even for operators. Double chests need both halves loaded, inside the box and unclaimed. Locked or inaccessible containers are skipped.

Cancelling keeps the claims already made. Moving more than eight blocks from the starting point, changing dimension or disconnecting stops the job. Claims are saved with the containers and survive restarts. They set a label only. They can't recover who originally placed a container and don't reveal unopened contents.

## Trade your whole inventory instantly

With Stow **1.0.4 or later** on both sides, **Shift + left-click a villager trade output** to trade every available payment stack in one server operation. The normal merchant selection and quick-move code handles both payment items, results, experience and stock. Partial payment stacks are topped up between batches. Trading stops when payment runs out, the offer sells out or the result cannot fit.

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

The companion does not require Cloth Config, Mod Menu or Mouse Wheelie. Replace older companion JARs instead of keeping several versions. Keep both JARs on matching versions (currently **1.0.6**); storage owner labels and Claim area need **1.0.5 or later** (1.0.4 or later for instant trades and the payment refill fix). Existing server settings are preserved, and the instant-trading option defaults to enabled. Players without the updated client or companion keep the other features and see their last saved labels, with label editing unavailable.

Stow's other inventory, storage-memory, compact storage list, material-planning, refill, shulker-preview, attack and auto-mending helpers belong to the client, as do the 1.0.5 performance, shulker, HUD and Mouse Tweaks fixes. They do not require this companion. This project does not change which servers permit automated actions. Compatibility with dedicated multiplayer setups, full modpacks and protection mods still needs testing.

## About the project

Stow was made for friends and a personal Minecraft server with substantial AI assistance. Much of the code, text, translations and branding was generated or edited with AI. It is a personal project and may have rough edges.

The related client includes sorting code adapted from Mouse Wheelie by Siphalor and contributors. Stow and its companion use the **Apache 2.0** license; original attribution is retained in the repository. Fabric and other dependencies are separate projects.

[Stow client](https://modrinth.com/mod/stow) · [Source and GitHub downloads](https://github.com/undrwtrsprite/stow) · [Changelog](https://github.com/undrwtrsprite/stow/blob/main/CHANGELOG.md) · [Report a bug](https://github.com/undrwtrsprite/stow/issues)

This is an independent project, not affiliated with Mojang or Microsoft. No Minecraft game files are included.
