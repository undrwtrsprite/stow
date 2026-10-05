# stow companion

**Strip and harvest connected oak logs with one shortcut.** The optional Fabric server companion for stow, built for Minecraft 26.3.

With stow on the player's client and the companion on the server, hold an axe, aim at an unstripped oak log and press **Alt + S**. The companion finds the connected group, strips every selected log, then harvests it.

- Follows tall columns, long rows and dense piles through touching faces, edges and corners.
- No fixed height or length cutoff from the starting log.
- Strips the whole group before the first break, helping Timber mods encounter stripped logs.
- Skips logs already harvested by another mod rather than dropping them twice.
- Uses normal server axe actions, block-break callbacks, durability and loot.
- Press Alt + S again to cancel. Sneak is not used by this action.

The starting log must be visible and within reach. Groups must be loaded and permitted. Survival reserves enough durability for stripping and mining, leaving one spare durability point; Unbreaking savings are conservatively not included in that estimate. Creative keeps normal creative behavior, including no block drops. Cancelled work keeps changes already completed.

Small groups can finish in one server tick. Larger groups take several ticks to keep the server responsive. The default maximum is **16,384 logs**, configurable from 1–65,536 with `maxLogs` in `config/stow-companion.properties`; restart the server after changing it. The companion does not load terrain to expand a group. Only unstripped oak logs connect the group; other wood, leaves, oak wood blocks and separate piles are excluded.

## Installation

**Server:** install the companion JAR and Fabric API in `mods`.

**Players using the shortcut:** install the separate stow client mod with Fabric API and Cloth Config.

**Single-player:** install both JARs on the client so the companion runs in the integrated server.

Requires Minecraft **26.3**, Java **25+**, Fabric Loader **0.19.5+** and Fabric API **0.161.0+26.3**. The companion itself does not require Cloth Config, Mod Menu or the client stow JAR on a dedicated server. Players without stow can still join; they simply do not have its shortcut. The companion adds no inventory UI or new blocks/items.

The combined build was tested in-game by the author. Automated server checks cover drops, durability, connected groups, cancellation, protection callbacks and a simulated Timber cascade. Compatibility with other Timber/protection mods should be checked in-game.

stow is a personal project made for friends with AI assistance. **Much of its code and documentation was generated or edited with AI.** Licensed under Apache 2.0; shared project attribution is retained in LICENSE and NOTICE. Independent of Mojang and Microsoft.

[Source and client downloads](https://github.com/undrwtrsprite/stow) · [Changelog](https://github.com/undrwtrsprite/stow/blob/main/CHANGELOG.md) · [Report a bug](https://github.com/undrwtrsprite/stow/issues)
