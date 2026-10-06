# Changelog

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
