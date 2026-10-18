# Development

## Build

Use JDK 25 and the included Gradle wrapper. Run `./gradlew build` on Linux/macOS or `.\gradlew.bat build` in Windows PowerShell. Import the repository root as a Gradle project in your IDE; `runClient` opens the development client.

The pinned versions are Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Cloth Config 26.3.159, optional Mod Menu 21.0.0, Loom 1.17.21 and Gradle 9.6.0. Minecraft 26.3 uses unobfuscated names, so there is no mappings dependency.

When changing the release version, update `build.gradle`, both Fabric metadata files, README and release notes together. `python3 dev-tools/check-project.py` checks metadata, translation keys, assets and the wrapper layout.

## Source layout

| Path | Purpose |
| --- | --- |
| `src/main/java/dev/stow/` | Mod configuration, client code, mixins and companion server code |
| `src/main/java/dev/stow/companion/` | Shared chest-sort protocol and companion sort validator; server implementation is excluded from the client JAR |
| `src/main/resources/` | Client metadata, English/German text and runtime assets |
| `server-resources/` | Companion metadata |
| `design/icons/mdi/` | Original third-party SVG icons and license |
| `dev-tools/` | Build preparation, source checks and icon conversion |
| `docs/FEATURES.md` | Detailed behavior and controls |
| `CHANGELOG.md` | Release changes |

The client JAR excludes companion server classes. The companion JAR contains only the shared protocol and companion server code. Local notes, publishing material and regression suites are ignored and excluded from the public repository and release packages.

Keep item names/components, pinned slots, cursor state and client/server synchronization intact when changing item transactions. Automatic mining uses hotbar selection only and defaults to off.

## Release validation

### Merchant refill follow-up — 19 October 2026

The user reports beta 2 stops after the first loaded payment stack. The follow-up replaces `MerchantMenu.tryMoveItems` between batches with shared in-place payment top-ups using normal inventory pickup/place/return operations. Vanilla's method first returns both payment slots to inventory and only refills once both are empty, which can fail with partial payments and a full inventory. The client fallback now detects slot progress rather than predicted merchant-use counts and compares current offer values rather than object identity. Matching components, input orientation, slot permissions, cursor and stock/space/batch bounds remain enforced.

Release client/companion version: `1.0.4+mc26.3`. Compilation only; no automated tests or Minecraft runtime were run, as explicitly requested by the user. The user will validate the reported trade/refill behavior. Publication does not establish runtime behavior or broad compatibility.

A completed build does not establish in-game compatibility. Before treating a release as ready, record manual client and companion checks here and resolve remaining compatibility items. For 1.0, game-specific checks remain for server damage applied to an item already in use and custom modded durability costs, as noted in `CHANGELOG.md`.

### 1.0.4 beta 2 — 18 October 2026

- The final beta 2 client and updated companion passed **130 checks** in a transformed Minecraft 26.3 client/server: 47 merchant checks (including nine companion validation checks), 13 attack, 14 real XP auto-mending and 56 tooltip/preview checks. Evidence: ignored `build/merchant-trade-probe/beta2-final-output.log` and `beta2-instant-result.txt`. A direct server transaction exhausts stock within the same server tick and preserves payment/output totals; normal client networking separately exercises all twelve merchant scenarios. Wrong menu/revision, invalid offer indices, an occupied cursor and invalid merchant ownership are rejected without changing payments, results or stock. Later clicks/Escape cannot undo an already completed instant request.
- A pre-companion paced build passed 121 checks (`beta-paced-output.log`). A first unpaced final-artifact run stopped early in one trading case; pacing follow-up batches four client ticks apart passed the same authoritative item-total cases. A menu revision does not necessarily advance for exactly predicted trades, so waiting only for a revision change was rejected during development.
- The new server endpoint uses only ordinary merchant operations and a fixed four-integer request. It limits work to 128 batches and one request per player per two server ticks. It never accepts client-proposed stacks or prices. Server settings preserve existing properties and default `instantVillagerTrading` to true. Only exact vanilla merchant-menu classes use the endpoint; custom classes use client fallback. No response after five seconds stops waiting without retrying uncertain work.
- Publication is a beta. Actual user modpack/server, physical controls, custom merchants, protection callbacks and XP-farm rules remain compatibility work; these isolated checks do not establish universal compatibility.
- With `instantVillagerTrading=false`, the exact beta 2 pair passed **121 checks** using the paced client path (`beta2-disabled-output.log`). All twelve trade scenarios still conserve authoritative server item totals; later clicks and Escape stop remaining batches. The companion retains fast sorting and harvesting while its trading channel is disabled.

### Earlier client development checks — 18 October 2026

- **Compact-tooltip follow-up:** built **1.0.4-test.2+mc26.3**, shortened English/German hover summaries and suppressed only the vanilla container-description component when Stow's own shulker grid is active. This leaves font/GUI scale, item titles, lore, other item components, ordinary containers and the preview-disabled written list intact. The transformed client passed **56 tooltip checks**; its actual rendered screenshot `build/merchant-trade-probe/screenshots/2026-10-18_22.13.16.png` was inspected. Evidence is `build/merchant-trade-probe/compact-tooltip-output.log`. The final build and 414-key project checker pass; the last rebuild only refined short translations after the runtime check. Earlier feature validation below belongs to test.1 and is not presented as a complete rerun for test.2.
- Built with JDK 25 as **1.0.4-test.1+mc26.3** using a local Gradle override; published 1.0.3 source metadata is unchanged. This is a development client, not a published release. The full Gradle build passes; the project checker passes 414 English/German translation pairs. The test JAR contains all four features and excludes companion server implementations. No companion code changed.
- The final combined client run passed **117 checks**: 38 trading, 13 attack, 14 auto-mending and 52 preview assertions. The result is `build/merchant-trade-probe/trade-result.txt`, the combined log is `combined-final-output.log` in that directory, and its rendered screenshot is `screenshots/2026-10-18_21.59.50.png`. The verified client JAR SHA-256 is `c9e2c2f0b0989fd5910a0aa1aacb2e16ded7fcabe9820bf026918a1f47077fc2`.
- The 1.0.3 baseline passed 18 assertions inside an isolated Minecraft 26.3 client/server and reproduced the reported limitation: three stacks of emeralds with a 32-emerald cost complete only two trades. Development checks complete six trades from all three stacks, in each Stow drag mode. Additional cases cover stock exhaustion, two payment items, insufficient second payment, full inventory, another click, Escape and Shift-clicking the offer list. Server item totals and the cursor are checked, rather than only recording calls to an output slot.
- Attack checks cover default-off activation, the Alt+A dispatcher, held-key release, a single initial Hold click, interval deadlines and absence of catch-up bursts, focus loss and opening a menu. The loaded vanilla key handler and normal networking deliver interval attacks to a server-side target. The input fixture controls window-focus state and the target/deadline in-process for determinism; it does not drive the user's mouse or keyboard. A first-click release/press cycle clears vanilla's menu suppression when activating from a menu/palette while retaining normal combat handling.
- Shulker preview checks cover all 27 positions, item counts, names, durability/components, independent snapshots, undyed/all 16 dyed boxes, empty boxes, the setting, hidden item/container tooltips, non-shulker items and the unchanged bundle tooltip. The Fabric client tooltip renderer is exercised. Screenshots from the actual client were inspected: the 9 × 3 grid, counts, damaged pickaxe indicator and normal tooltip text render within the tooltip bounds.
- Auto-mending passed 14 checks using actual server XP-orb pickup/repair logic: two damaged Mending tools repair in sequence and return to their original positions; names/enchantments, a pinned item, an item without Mending and the selected main hand are preserved. The original offhand stack returns with the same count. Toggle-off restoration, stopping for menus, deferred restoration after closing them and a manually replaced offhand are checked. The test waits for the final return packet before checking authoritative inventory positions.
- Local probe sources, results and screenshots are retained under ignored `build/merchant-trade-probe/` and `build/*Probe.java`; the test world was copied from an existing development fixture. User saves and launcher profiles were not used. These checks load the transformed production classes and normal client/server networking, without installing the companion.
- Several early test-client startups exited in `jvm.dll` with Windows `c0000005`, as also recorded during 1.0.3 work. The isolated fixture uses four processors and `-XX:TieredStopAtLevel=1` to complete its checks. This is a test-launch workaround; its native cause is not established and no JVM flag is embedded in the mod JAR.

Remaining validation before a public release: the user's actual multiplayer modpack/server (including merchant modifications and inventory restrictions), physical input and attack settings UI on that installation, and its XP farm/protection rules. Do not infer that compatibility from local compilation or these isolated fixtures. Pending offhand restoration cannot be sent after an abrupt disconnect; the two items remain in their current swapped positions and activation is cleared.

### Finish/Reopen development checks — 9 October 2026

The unreleased client changes were built with JDK 25 as `1.0.2-test.1+mc26.3`, without changing the release metadata in source or publishing the artifacts. The companion implementation is unchanged.

- Full Gradle build and project metadata/English-German translation checks passed. Gradle has no standard test sources; the following results come from separate local regression probes.
- An isolated Minecraft 26.3 Fabric client passed 37 Finish/Reopen assertions: nonempty projects and target eligibility, neutral finished colors, Undo, project isolation, saving/reloading, migration from files without the finished field, preserved goals/selections/priorities, HUD visibility and setting persistence, project-switch and click-time eligibility, and footer widget geometry at 320/360/480/720 GUI pixels.
- Retained importer/project regression checks passed all 47 assertions; export checks passed all 6 assertions.
- A fresh Minecraft 26.3 Fabric GameTest server passed the companion harvest fixture (and vanilla smoke test). The fixture covers tall columns, long rows, dense piles, stripped drops, durability, all 12 vanilla log/stem types and axes, species isolation, Fabric use/break vetoes, insufficient durability, cancellation, changed tools, unreachable seeds, packet codecs, count caps, changed snapshots, creative behavior and a simulated Timber callback cascade without duplicate drops. This is not a test against the user's actual Timber mod/server installation.
- Local evidence is retained in ignored build output: `build/finish-project-probe/finish-result.txt`, `build/finish-import-checks.log`, `build/finish-export-checks.log` and `build/finish-harvest-probe/results.xml`. The local feature probe source is `build/FinishProjectProbe.java`.

Live gameplay/visual checks were subsequently performed by operating a separate Minecraft 26.3 client in a fresh survival world, with the built test client and normal dependencies (no feature probe mod). At GUI scale 4 in a 1280×960 game viewport, the Materials footer rendered without overlaps. A 64-stone target with 64 stones enabled Finish; finishing changed the counts and bar to neutral blue. Removing 32 stones through a local-world command updated the finished menu and inventory HUD to 32/64 and showed "32 used". Reopen restored warning colors and disabled Finish below target; Undo restored the finished state. Saving, leaving and rejoining preserved the finished state and HUD priority. Placing a stone through normal right-click then reduced the count to 31/64, showed "33 used" and retained the neutral blue HUD/menu. No additional issue was found in this live check. The isolated world/configuration and game logs are under ignored `build/finish-gameplay-probe/`; user saves were not used.

Remaining compatibility validation before declaring the release ready: project switching, remembered chest updates and reconnect behavior on the actual multiplayer modpack/server, including the user's Timber installation. Those were not exercised by the local singleplayer session; automated project-switch, storage-fixture and companion tests are recorded separately above.

### 1.0.2 release checks — 17 October 2026

- JDK 25 Gradle `build` passed for client, companion and source artifacts. The project checker passed 396 English/German translation-key pairs, metadata, licenses, wrapper layout and client/server artifact separation.
- Minecraft 26.3 with Fabric Loader 0.19.5 loaded Stow 1.0.2 in the isolated development run. The available game-control backend did not enumerate a controllable window, so the villager result-slot drag and direct Shift-click behavior were not verified interactively. Treat those as code-reviewed and compiled, not as live-game validated.
- After the percentage-only equipment HUD change, the JDK 25 Gradle build and project checker passed again. Code review confirms the HUD and placement preview show only the percentage when enabled; the detailed Equipment Watch screen still shows percentage and current/maximum durability. No controllable game window was available for a visual check of this setting.
- The companion source is unchanged. The actual multiplayer modpack/server, remembered-chest reconnect behavior and Timber installation remain unverified; this build makes no compatibility claim for that configuration.

### 1.0.3 validation — 18 October 2026

- Reviewed the villager result-slot guards, percentage-only HUD paths, companion request bounds/conservation checks, cursor handling and main/offhand refill state. Repaired a truncated GitHub source upload from release preparation and verified all 190 tracked files against the local source, allowing only line-ending differences. Subsequent changes use normal Git commits and pushes.
- The JDK 25 Gradle build and project checker pass for the 1.0.3 client, companion and sources, including 396 English/German translation keys and the new shared chest-sort protocol/server-code separation.
- The final isolated Minecraft 26.3 client with Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and Cloth Config 26.3.159 passed **112 release checks** plus the existing **37 Finish/Reopen checks**. These ran inside the loaded, transformed game classes in a copied test world, using programmatic input events and normal networking; user saves were not used.
- Runtime coverage includes matching/all/Both Shift-drag modes, Ctrl+Shift, ordinary merchant inputs, protected outputs, direct vanilla result Shift-click and carried-bundle drag. Live HUD rows and empty placement previews show percentage alone or remaining durability alone, with unchanged colors and filtering. Actual rendered screenshots were inspected.
- Companion checks cover single/double chests, barrels, ender chests and shulker boxes; combining; exact named-item components; rejected stale snapshots, forged/new/lost items, overstacking, duplicate/negative/player-inventory slots, menu/state mismatch and occupied cursor; 54-slot and 24 KiB decoding bounds; and oversized-data queue fallback. All five sort orders passed normal client-to-server requests against a 54-slot chest, with server/client contents agreeing and player inventory/cursor preserved.
- Rocket checks used actual Elytra boosts in both hands at thresholds 0 and 8. Server inventory updates confirmed consumption and refill, including empty-hand replacement; differing flight durations and pinned donors stayed untouched. A missing consumption acknowledgement expired without moving reserves. The existing block-refill regression also passed **8 checks**, including threshold persistence/bounds, final-block refill, unchanged stacks, exact totals and an empty cursor.
- A dedicated Fabric server with only the new companion and normal server dependencies passed both existing GameTests (vanilla smoke test and the extensive harvest fixture). This verifies dedicated-server loading and retention of the harvest behavior; it does not establish compatibility with the user's actual Timber installation.
- A separate client/server run with acceleration disabled passed **61 checks**, including all five sort orders through the normal click queue, exact client/server contents and preserved player inventory/cursor. Evidence is in ignored `build/release-review-probe/`, `build/release-fallback-probe/`, `build/release-dedicated-probe/results.xml` and `build/refill-zero-1.0.3.log`; local probe sources are retained under `build/` and are excluded from release artifacts.
- Two earlier test-client startups failed inside `jvm.dll` with native Windows exceptions before the checks ran (one `c0000005`, one `c0000409`). No Java or Mixin failure was reported. The final full run completed with four resource-loading workers; the native-startup cause has not been established.

Actual multiplayer modpack/server behavior, remembered-chest reconnect on that server and its Timber/protection mods remain unverified. Custom/restricted inventories deliberately use the client queue; servers with mods that need to intercept every vanilla click can disable fast sorting. These are explicit compatibility limits, not claims inferred from compilation or local fixtures.

## GitHub Actions

The Build workflow checks project metadata and builds on Windows and Linux. It uploads client, companion and source JARs as build artifacts. It has read-only repository permissions.

## Version checks

`VersionChecker` runs network work on daemon threads and adds the chat notice on the client tick thread. It checks published stable client releases for the running Minecraft version. GitHub releases must include the matching client JAR. Modrinth versions must list the matching Minecraft version and Fabric. CurseForge uses CFWidget's public file metadata, filtered by release type, Fabric and Minecraft version.

The request URLs are fixed public endpoints. Download links are constructed for the three official project pages. No credentials or game data are sent. `lastNotifiedVersion` is saved only in the player's local settings so reconnecting or restarting does not repeat the same notice.

## Upstream build references

- [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html)
- [Fabric Loom documentation](https://docs.fabricmc.net/develop/loom/)
- [Gradle setup action](https://github.com/gradle/actions)
- [CFWidget API](https://cfwidget.com/)
