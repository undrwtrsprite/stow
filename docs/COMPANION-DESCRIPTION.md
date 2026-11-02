# stow companion

**Faster sorting, instant villager trades and easy storage claiming for your server.** The companion is an optional server-side mod for **Minecraft 26.3 on Fabric** that works with [Stow](https://modrinth.com/mod/stow).

Stow does everything in your own client. The companion moves the heavy lifting onto the server, where it is done in a few safe, bounded steps: it applies a whole sort or trade in one go, harvests a whole tree when you strip one log, and lets players claim older storage in one selection.

It adds no menus, HUDs or controls of its own. Players keep using Stow as normal.

## Instant villager trades

With Stow 1.0.4 or later on the client, **Shift + left-click a villager's offer** trades your whole stock of that item in one server operation, not a long series of clicks. The server checks the menu, your cursor and the offer before anything changes, and it follows the villager's normal prices, stock and experience. Each gesture is capped at **128 batches**, and a finished trade can't be undone by cancelling later.

Custom trading menus and servers without the companion fall back to Stow's paced trades. To turn instant trading off, set `instantVillagerTrading=false` in `config/stow-companion.properties` and restart.

## Fast chest sorting

Sort vanilla chests, trapped and double chests, barrels, ender chests and shulker boxes in one validated step. The server checks the open storage, your permissions, stack limits and exact item totals before it changes anything. Your own inventory and cursor stay untouched, and other players viewing the same storage see the update normally.

Requests cover up to **54 storage slots**. Larger or unusual menus, and any storage where the contents changed mid-sort, fall back to Stow's normal click-by-click sorting. To turn it off, set `fastChestSorting=false` in `config/stow-companion.properties` and restart.

## Harvest whole trees with Alt + S

Hold an axe, aim at a log and press **Alt + S**. The companion strips the connected group of that log type and harvests it, including all vanilla log types and Nether stems. Tall columns, long rows and dense piles work too, up to a limit of **16,384 logs** by default. Your reach, the chunks that are loaded, permissions, durability and normal loot all still apply. Press **Alt + S** again to cancel; what's already done stays done.

Without the companion, Alt + S only strips nearby visible logs and doesn't harvest them.

## Label who owns your storage

With the companion, every chest, trapped chest, barrel and shulker box you place is labeled with your name. Labels survive restarts, and they disappear when the container is removed. Shulker boxes are labeled with whoever places them.

In Stow, your storage list shows **your** containers, **Shared** ones, the **Unclaimed** ones and the **Mixed** double chests, and you can filter by any of them. Use **Rename → Storage owner** to claim older storage, mark it Shared or restore a label. Only the player who set a label, or an operator, can change it. Labels organize your list. They don't lock containers, grant access, reveal unopened contents or show who placed old storage before labels existed.

Label changes need the storage menu open, normal access, and the container within eight blocks. No item contents are sent, and remote chunks never load.

## Claim older storage by area

**Storage → Claim area** lets a player fly a free camera over a region, pick two corners, check the box and claim every unclaimed storage container inside it in one step. The player's character stays where it is, and nothing is claimed until the player confirms.

- Up to **262,144 positions**, with the corners within **64 blocks** of the player on each axis.
- Only chunks that are already loaded are scanned. Unloaded positions are skipped, never loaded.
- The scan runs in small batches to keep the server responsive: at most eight jobs at once, and a set budget of positions and containers per server tick.
- Only unclaimed chests, trapped chests, barrels and shulker boxes are claimed. Existing owners and Shared labels are always kept, even for operators. Locked or inaccessible containers are skipped.

Cancelling keeps the claims already made. Walking more than eight blocks from where you started, changing dimension or disconnecting stops the scan. Claims are saved with the containers.

## Install and compatibility

- **Server:** Minecraft **26.3**, Java **25+**, Fabric Loader **0.19.5+**, Fabric API **0.161.0+26.3**, and the companion JAR in the server's `mods` folder.
- **Players:** the Stow client with Fabric API and Cloth Config **26.3.159+**. Mod Menu is optional. The companion itself doesn't need Cloth Config or Mod Menu.
- **Single-player:** put both the Stow client and the companion in your client's `mods` folder.
- **Versions:** use the same companion version (currently **1.0.6**) on the server. Storage labels and Claim area need companion **1.0.5 or later**; instant trades and the payment refill fix need **1.0.4 or later**.

Replace old companion JARs rather than keeping several. Existing server settings are kept, and instant trading is on by default. Players without the updated client or companion still play normally, and they can see labels that were already saved, but they can't edit them.

Stow's other features (sorting, storage memory, material planning, refill, shulker previews, attack and auto-mending) run in the client alone and don't need the companion.

**Still to test:** dedicated multiplayer servers, full modpacks and protection mods. Check Alt + S, Claim area and instant trades on your own setup before relying on them, especially with Timber-style or protection mods.

## About the project

Stow and its companion were made for my friends and our personal Minecraft server, with a lot of AI assistance. Much of the code, text, translations and branding was generated or edited with AI. I choose the features and test them, but it's a personal project and may have rough edges.

The client's inventory sorting is adapted from [Mouse Wheelie](https://github.com/Siphalor/mouse-wheelie) by Siphalor and contributors. Stow and the companion are released under the **Apache 2.0** license, with the original attribution kept in the source.

[Stow client](https://modrinth.com/mod/stow) · [Source and downloads](https://github.com/undrwtrsprite/stow) · [Changelog](https://github.com/undrwtrsprite/stow/blob/main/CHANGELOG.md) · [Report a bug](https://github.com/undrwtrsprite/stow/issues)

The companion is an independent project and is not affiliated with Mojang or Microsoft. No Minecraft game files are included.
