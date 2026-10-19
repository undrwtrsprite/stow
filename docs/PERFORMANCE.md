# Performance pass — Stow 1.0.5 (19–20 October 2026)

This document records the performance work behind **Stow and Stow Companion 1.0.5+mc26.3**. The pass was measured on the **1.0.5-test.3+mc26.3 client**, which combines the performance pass, Matching/Both Mouse Tweaks fix, shulker memory lifecycle/cards and equipment HUD order. It is not a claim of complete modpack compatibility. The test.3 changes were client-only and changed no companion code. **The Stow Companion did change in 1.0.5:** it adds storage owner labels (test.5) and Claim area (test.6), described in the follow-up sections below. The performance, Mouse Tweaks, shulker and HUD fixes still work without the companion.

## Coverage and decisions

| Area | Review and result |
| --- | --- |
| Inventory toolbar / deposit | Retains test.2's 50 ms display refresh, shared hover preview, unsupported-menu exclusion and fresh click-time plans. |
| Equipment/project HUD | Retains bounded display caching with immediate project/config invalidation, now including warning threshold. HUD-only collection skips backpack/offhand before copying; visible gear follows hotbar order then helmet-to-boots. Detailed equipment screens retain durability ordering. |
| Material/storage data | All included item totals and source ages share one revision-scoped index. Immutable goal/visibility/star/HUD views are reused and invalidated on edits, project selection, source updates, Forget and Undo. Nearest-source lookup avoids complete search/sort results and per-container stream allocations. |
| Searches / imports | One bounded active-query parse replaces regex-per-item work. Import aliases/names are normalized once per importer; numeric and markup patterns are shared. Original matching/order/ambiguity/quantity rules are checked. |
| Storage cards / tooltips | Visible storage previews sort on refresh rather than each frame. Colored shulker cards cache icons on refresh. The read-only 27-slot shulker tooltip remains bounded and retains independent snapshots; no tooltip speedup is claimed. |
| Tick helpers | Inactive sort/deposit/trade/refill/Mending/attack handlers return early. Idle bulk strip avoids a readiness call without a companion request. Unchanged observed tools reuse a snapshot; disabled refill stops observation. Active inventory safety checks remain. |
| Disk / update networking | Save requests coalesce per queued store, clean stores skip scheduling, and changes during a write can queue a follow-up. Version downloads remain off-thread with existing deadlines/retry periods. Startup world-memory loading remains synchronous and bounded by the existing 8 MiB read limit. |
| World rendering | Chest locator draws the selected storage target only (one or two blocks); it does not scan loaded blocks for every frame. Its rendering remains unchanged. Shulker reconciliation visits remembered positions on chunk-load events, not an every-tick world scan. |
| Client actions / companion | Sorting, deposit, trading, tool selection, stripping and companion request loops were reviewed for scope/bounds. Existing rate limits, batch caps, cursor completion, revision validation and harvest work budgets remain. Companion TPS and the user's multiplayer installation were not profiled. |

## Comparison with test.2

Both clients ran in separate copies of the same development world with Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Cloth Config 26.3.159 and Mouse Tweaks 2.31. Workloads used 1000 containers with 54 entries each, a 1024-goal project and the real item catalogue. Each timing/allocation value is the median of three warmed in-process batches. JDK 25 on Windows used four processors, a 2 GiB heap and the fixture's existing `-XX:TieredStopAtLevel=1` native-startup workaround. These flags are not embedded in the mod.

Allocations below are bytes allocated by the render thread during each batch, not retained heap, whole-process memory, a leak measurement, or FPS. Timings include fixture calls. Unchanged small workloads can vary; no improvement is claimed for inactive handlers or tooltip snapshots. This is not a live frame-time comparison of the user's pack.

| Workload | test.2 ms | test.3 ms | test.2 allocated MiB | test.3 allocated MiB |
| --- | ---: | ---: | ---: | ---: |
| 1024 cold material totals | 1014.021 | 4.443 | 31.337 | 3.695 |
| 200 source-age queries | 349.042 | 0.031 | 76.369 | 0.000 |
| 1000 goal/HUD-view queries | 360.274 | 0.202 | 41.733 | 0.031 |
| 5 storage searches | 200.047 | 47.686 | 220.901 | 10.610 |
| 20 importer searches | 151.732 | 4.959 | 111.218 | 1.038 |
| 10000 inventory matches | 13.951 | 5.137 | 12.512 | 3.815 |
| 1000 equipment HUD collections | 9.739 | 3.327 | 4.967 | 1.419 |
| 10000 tool observations | 2.831 | 2.223 | 0.534 | 0.000 |
| 10000 inactive-handler cycles | 1.094 | 1.172 | 0.000 | 0.000 |
| 1000 tooltip snapshots | 8.250 | 8.935 | 5.959 | 5.959 |
| 20 nearest-source lookups | 494.151 | 18.496 | 765.390 | 0.656 |

Java Flight Recorder CPU/allocation samples independently identified repeated storage-count scans, regex matching, importer normalization and source-count stream allocations. The changes target those paths. Raw sample counts are not compared as percentages because the optimized recording finishes sooner.

## Runtime and visual validation

The exact client JAR passed **770 isolated assertions**: 641 full-audit checks (including search/cache mutation matrices, save concurrency, shulker previews/lifecycle and HUD order), 18 display freshness/action checks, 74 Mouse Tweaks drag checks and 37 Finish/Reopen project checks. Build, metadata, 414 English/German keys, canonical README alignment and artifact checks also pass.

The integrated-server lifecycle fixture uses actual client `useItemOn`, server inventory synchronization, block placement/removal and normal client packets. It verifies known-item placement snapshots only after confirmation, red-to-blue replacement with changed title/counts, removing old positions/totals/glow, moving a box without stale duplicates, and retaining two genuinely identical boxes at separate positions. Additional fixtures exercise actual single/section packet handlers, a loaded-chunk callback, protection against an old open menu recreating a removed box, rejected placement, all 17 dye/undyed icons and color metadata persistence. Loaded-chunk reconciliation is explicitly invoked against a real loaded chunk; it is not a long-distance travel test.

One earlier lifecycle run timed out awaiting the first placement; diagnostic and subsequent exact-artifact runs completed. Placement confirmation also handles vanilla's reconciled prediction acknowledgements. No in-game compatibility claim is based on the failed attempt.

Normal and selection storage-list screenshots were inspected, including a spacing adjustment to keep coordinates clear of the new box icon. A separate real-client HUD screenshot uses server-synchronized hotbar and armor items: axe, low-durability pickaxe, rod, helmet, chestplate, leggings and boots remain in slot order with their warning colors; a backpack spare is excluded.

## Data and practical limits

- Existing limits remain: 1000 remembered containers, 1024 goals per project, 16 projects, existing quantity/packet/action bounds and 50 ms passive display refresh.
- Shulker color is optional metadata within the existing version-4 memory document. Older records load without migration, and prior clients can ignore the extra field. Reopening/known placement learns dye color. The companion's data/protocol requirements do not change.
- Removals/replacements must be observed through server block data, acknowledgements or loaded chunks. Identical-color replacement while unobserved and remote contents changes cannot be inferred; reopening refreshes authoritative menu contents. Pending placement snapshots are capped at 64 for five seconds and cleared across world changes/disconnects. Genuine identical boxes are never deduplicated by contents.
- User saves, configuration and installed mod files were not changed. No release or project-page publication was performed. Actual FPS/frame times, physical input and inventory/protection behavior in the complete user's modpack/server remain to be checked.

## Local evidence

Ignored `build/full-pass-baseline/` and `build/full-pass-fixed/` contain `measurements.csv`, `workloads.jfr`, logs and results. The final result is `full-pass-fixed/full-result.txt`; the delivery run log is `output-exact-artifact.log`. Local sources/preparation are `build/FullPassProbe.java`, `FullAuditChecks.java`, `ShulkerLifecycleProbe.java`, `SaveProbe*.java`, existing regression probes and `prepare-full-pass.py`. Fixture code is excluded from the client JAR.

Storage screenshots are under `build/full-pass-fixed/screenshots/`; the separate HUD screenshot/result are under `build/full-pass-hud/`. These paths are local development evidence and are not bundled into releases.

Delivery artifact: `build/libs/stow-1.0.5-test.3+mc26.3.jar`. SHA-256: `059982b3382cfe39ff16e2a392b4b1577282782c87044db238247618b0d8288e`.


## Follow-up: icons, player labels and inventory effects (test.5)

The **1.0.5-test.5+mc26.3 client/companion pair** retains the measured full-pass changes above. The client adds chest/double-chest/barrel icons alongside dyed shulkers, owner filters and an inventory material-list layout that reserves vanilla status-effect space. The matching companion adds placer/claim/shared labels. The companion therefore changed in 1.0.5: this optional ownership feature needs Stow Companion 1.0.5, while the earlier performance/shulker/HUD fixes still work without it.

Player-label work is event driven. Rendering reuses page strings/icons; the filter catalogue is an immutable cached metadata map, with no contents traversal or distance sorting. Metadata-only edits retain the material totals/source-age index and immutable goal views. Identical replies do not advance the revision. Ownership has no world scan, background polling or per-frame request; the server checks one/two loaded block entities per request. Client pending/reply caps are 32/five seconds and 256, with a server limit of 32 handled requests per player per 20 ticks. In the isolated server-thread cache probe, **100,000 cached catalogue reads took 2.4374 ms and allocated 0 thread bytes**. This measures that accessor only, not whole-feature or full-modpack FPS.

The exact final client passes the established **770 assertions** again (`full-pass-final/full-result.txt`, `output-final.log`), including actual shulker placement/replacement/movement, Mouse Tweaks drag and project checks. This run first reproduced a pre-placement-air-update race from the earlier intermittent timeout: a queued older air update discarded a newer placement candidate. The fix retains nonmatching candidates until prediction acknowledgement or the five-second expiry; the succeeding final lifecycle run is recorded rather than claiming the failed attempt passed.

The exact final pair passes **48 ownership checks** (`ownership-probe/output-delivery.log`, `result.txt`): real BlockItem placement for chest/barrel/undyed/red shulkers, chunk-tag save/load, two distinct non-operator players, own/shared/claim authority, closed/remote rejection, double-half mixed and shared behavior, UUID/name handling, operator override, client disk reload, cache identity/invalidation, retained counts/goals and identical-reply no-op. Actual client/server query/share/private messages update real remembered storage, and all four special list filters are exercised. Owner controls and normal/selection list screenshots were inspected. Fake players exercise server authorization, not simultaneous physical multiplayer clients.

The no-companion effect fixture exercises server-synchronized one/seven effects, changes while inventory stays open, recipe-book repositioning, GUI scales 2/4, screen-bounded list/footer placement, wheel scrolling and restoration after effects disappear. The final run passes **21 layout checks** (`effects-overlap-probe/result.txt`, `output-delivery.log`) and six screenshots were inspected, including the open recipe book, compact effects, visible narrow list and normal position after removal. One intermediate fixture expected a hidden inventory widget to refresh while the narrow recipe-book overlay bypassed vanilla inventory rendering; the final fixture returns to the inventory before checking restoration. Final result and screenshots are under `effects-overlap-probe/`. Narrow views omit a footer that cannot fit while retaining scrolling; if no readable row fits, the Materials control remains available. No settings or saved-data migration is needed for layout/icons.

Ownership is optional metadata within version-4 client memory and `stow_owner` server block-entity tags. Older/absent companions leave saved labels readable and editing unavailable. Unobserved owners are shown as Unknown owner; a confirmed old unassigned block is Unclaimed. Existing storage is not retroactively attributed or claimed on opening. Names are recorded names; UUIDs determine identity. Labels do not enforce access, share contents or alter project totals, and remote saved labels may be stale. Dedicated-server/protection-mod/full-pack FPS and frame-time testing remain outstanding. No user installation, live world or public release was changed.

Final development artifacts:

- `build/libs/stow-1.0.5-test.5+mc26.3.jar` — SHA-256 `6afddab7f61520e168e4855dcc83cbcd0fa1e003a69c56dd3daaf45796cbd874`.
- `build/libs/stow-companion-1.0.5-test.5+mc26.3.jar` — SHA-256 `7f2e29af0d2f1ec85e7f27a430d2d63fd1a1da094f5b202dae67eef80f5720f3`.


## Follow-up: camera-selected one-time claims and compact UI (test.6, 20 October 2026)

The development **1.0.5-test.6+mc26.3** pair adds an explicit legacy-storage area claim, retaining test.5's cached player labels and existing performance changes. No historical placement data is invented. A client-only detached camera lets the player select two block corners and return to a review screen; no claims occur until the review button is pressed. The companion stores resulting UUID/name labels using the same chunk-persistent container tags as ordinary ownership. Server restart persistence relies on normal world saving with the updated companion installed; removing/downgrading it and saving containers can discard unrecognized ownership metadata.

The scan is finite and event driven: at most 262,144 positions, each corner within 64 blocks per axis, eight active jobs/one per player, and a combined 512 positions/eight containers per server tick. Jobs skip unloaded chunks via `getChunkNow`, and never inspect inventory contents or request chunk loads. They skip every existing owner/shared label even for operators, locked/inaccessible containers and double chests with a half outside the box. Cancellation retains completed claims. Progress/label batches contain at most 32 positions; the client only patches already-known snapshots, retains material totals/goal caches and schedules normal asynchronous saving. No background ownership scan or per-frame networking was introduced.

The detached camera uses a negative local-only entity ID and is not added to the world. Original camera, perspective and input are restored on normal exit; player/world changes stop selection. Normal player physics remain active, while input is neutralized and selection keys/mouse actions are consumed. Movement is limited to loaded terrain and 64 blocks per axis. The player's actual position is the server's basis for validation; moving more than eight blocks during the scan stops the job. No server teleport or flight mode is granted.

The inventory-button follow-up compares geometry independently of cached display data, so recipe-book shifts and animated inventory positions update immediately without rerunning deposit simulations. Stow-only EditBoxes replace vanilla append underscores with the native vertical insert-caret renderer, retaining vanilla blink timing, selection and IME. Focused empty hints are drawn once with a caret gap. Normal/selection storage rows shrink to 58/54 GUI pixels and up to three columns; their preview item stacks and formatted count labels are cached on refresh. Pagination, scrolling and buttons share those dimensions. At 1280×960 / GUI scale 2, the fixture shows ten cards per page (previously six normal/eight selection), with all 1000 remembered entries still reachable.

The exact delivery client passed the established **770 regression assertions** (`full-pass-area-final/full-result.txt`, `output-delivery.log`). Initial runs reproduced the intermittent placement timeout; diagnostic inspection showed the fixture sometimes clicked its newly constructed support before the support's server packet reached the client, making the synthetic hit choose the wrong placement cell. The fixture now waits for all three support blocks to be observed before clicking them. No new production placement behavior was needed for that fixture correction. A diagnostic-only mixin failed one startup due to a non-private helper, was corrected for diagnosis, and was removed from the final regression/delivery artifacts.

The integrated `area-claim-probe` exercises bounded scan slices/replies, overflow/size/distance rejection, old chest/barrel/shulker/double claiming, foreign/shared preservation, boundary-double exclusion and chunk serialization; real camera keyboard/mouse corner selection, neutral player position, original input/perspective restoration, real claim/cancel messages, immediate Mine updates without replacing a chest, and unchanged material caches. It also exercises twelve animated geometry steps while the data-cache deadline is frozen, recipe-book reopening alignment, denser pagination and native text insertion/deletion. The final run passes **85 area/camera/UI assertions** (`area-claim-probe/output-visual-final.log`, `result.txt`). **21 further effects/layout/no-companion checks** pass on the same final client (`effects-overlap-area-final/output-delivery.log`, `result.txt`), for **876 total isolated assertions** including the established suite. Camera, review coordinates/actions, an actual focused vertical caret, compact normal/selection cards and narrow claim screen were inspected. Capture waits were added for the review/caret so screenshots show the updated frame rather than the preceding screen. Build, 454 bilingual keys, canonical README alignment, fixture exclusion and client/server packaging checks pass. Earlier failed camera initialization lacked a required local ID (fixed); a test corner aimed at air correctly hit the floor (the fixture now uses a solid marker).

Dedicated-server/full-modpack/protection-mod free-camera compatibility, physical-input usability across all bindings/languages and full-pack frame times remain unverified. No live server/user world or installed mod was changed. The test.6 artifacts listed below are development builds; the 1.0.5 release artifacts are listed in the next section.

Final development artifacts:

- `build/libs/stow-1.0.5-test.6+mc26.3.jar` — SHA-256 `0c7015f0d860c55f38a922493c2dc2d48f0a8fb8c561385fc54dddf07ec627dc`.
- `build/libs/stow-companion-1.0.5-test.6+mc26.3.jar` — SHA-256 `0e5ac5d2ef1d79fa4788cc7729fbb934cee721157c338ffe2c50f79e235b3f48`.

## Release artifacts (1.0.5+mc26.3)

The release JARs are separate builds from the test artifacts above; the measurements and checks in this document were recorded on the listed test artifacts.

- `stow-1.0.5+mc26.3.jar` — SHA-256 `234cf0f9da07acd4b85da4b6615acc97a1ccd8def01287fe12a001be5f9f42ae`.
- `stow-companion-1.0.5+mc26.3.jar` — SHA-256 `d9fe36155fcfba5944a9316109a9afa62b7181b400c1f040af7dbdc8ffde373c`.
