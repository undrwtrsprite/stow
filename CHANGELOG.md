# Changelog

## Unreleased

### Area-selection camera controls

- Fixed the **Claim area → Fly and select corners** camera moving in the wrong direction. The view followed the camera's head rotation, which was never updated, while movement used a separate, mouse-driven yaw, so W drifted sideways/upward and horizontal mouse look did not turn the view; A/D strafing was also mirrored. The camera now uses one rotation for view, aiming and movement: W/S fly toward/away from where you are looking (including up/down pitch), A/D strafe left/right, Space/Shift move straight up/down and Ctrl still speeds up. The character keeps receiving neutral input and does not move. Range, loaded-chunk limits and controls are unchanged; no setting or data migration is needed.
- Corner selection is now a clear two-step flow. **Step 1 · Footprint:** aim at a block (up to 32 blocks away); left-click sets corner 1, right-click sets corner 2, clicking again redoes that corner and Backspace undoes the last one. The targeted block, both corners (green/blue) and a flat footprint outline with its width × length are drawn live; the footprint sits at the lower of the two clicked Y levels. Enter continues. **Step 2 · Height:** the scroll wheel or Up/Down (Page Up/Down) moves the top, Alt + wheel/arrows moves the bottom, Ctrl steps by five. It starts at the span of both clicked blocks and is clamped to the world height and 64 blocks of the character. The hint shows the live height, Y range, dimensions and block count, and turns red when the box is too large (over 262,144 blocks) or out of range. Left-click or Enter confirms and returns to the review screen; right-click or Backspace goes back to the footprint with the corners kept; Escape cancels at any step. The previous controls (left-click only, right-click reset, no height control) are replaced. A short click sounds on each accepted step and a low note on a rejected one.
- F1 (or the rebound Hide GUI key) now toggles the HUD while the selection camera is active, hiding the hint overlay and crosshair as in vanilla. Previously the key was swallowed by the camera's input capture. Selection outlines stay visible with the HUD hidden. The scroll wheel no longer changes the hotbar slot during selection.
- The selection hints are now a compact controls panel instead of plain text: a step header with two progress dots, key-cap buttons (dark rounded caps with the key name) and small mouse icons with the used button or wheel highlighted, each followed by its action. Only the current step's controls are shown, movement keys are grouped in one row, and a coloured status block shows the corners (green/blue, matching their outlines), footprint size, height range and block count (green when valid, red with the reason when too large or out of range). The panel sits bottom-left beside the hotbar when there is room, otherwise top-left, stays clear of the crosshair, follows the GUI scale and is hidden by F1. Key names come from your current key bindings for movement, jump, sneak, sprint and Hide GUI (falling back to the defaults when a binding is unbound or on a mouse button), and the camera now also moves with those bound keys instead of fixed W/A/S/D/Space/Shift/Ctrl.
- The claim request, its limits and the companion protocol are unchanged. New/changed hint text is translated in English and German. Compiled and checked only; in-game validation is pending.

## 1.0.5+mc26.3 — 20 October 2026

### Select and claim existing storage with a local camera

- Added **Storage → Claim area → Fly and select corners** for older unassigned containers, so replacing them is unnecessary. This assigns ownership explicitly; it cannot recover historic placers when no placement history exists. The detached local camera leaves the character with neutral input and normal server physics. W/A/S/D move, Space rises, Shift descends, Ctrl speeds up; left-click selects two opposite block corners, right-click resets, Enter returns to the review screen, and Escape returns without claiming. The camera is limited to loaded terrain within 64 blocks per axis of the character. It restores the original camera/input/perspective on exit, disconnect or world change and exits if the character moves more than four blocks.
- Review the inclusive selected box, then press **Claim unassigned storage in this box**. The updated companion scans at most **262,144 positions**, with both corners within 64 blocks per axis of the actual player. It visits only already-loaded chunks and never requests remote chunk loads or inventories. At most eight jobs are active, one per player, with a combined budget of 512 positions and eight containers per server tick; progress/label updates are bounded to 32 positions per packet. Start requests are limited to once per 20 ticks. No scan runs automatically or per rendered frame.
- Claims affect only completely unassigned supported chests/trapped chests, barrels and shulkers. Existing private/shared labels are always skipped, even for operators. Double chests are processed once and require both loaded, unassigned halves inside the selection. Locked/inaccessible containers and incomplete boundary doubles are skipped. Cancellation retains completed claims; moving more than eight blocks from the scan's starting position, changing dimension or disconnecting stops the job. Unloaded positions are skipped rather than queued for later.
- Ownership is saved with the actual server block entities through the companion's existing persistent tags and survives normal saves/restarts. Batched replies update known client snapshots and caches, without discovering unopened contents or changing project totals. Previously unknown containers still enter chest memory when opened. This action requires the updated client/companion; individual open-container claiming remains available. This is organization metadata, not historical attribution or inventory protection.

### Inventory and storage interface follow-ups

- Inventory toolbar/tracker geometry now follows the current inventory position immediately, including recipe-book shifts/animation and reopening with the book already open. Geometry changes no longer wait on the 50 ms button-data cache; deposit simulations and other expensive display calculations retain that cache.
- Stow text fields use a consistent blinking vertical caret at the end of text as well as within it, replacing the append underscore seen beside placeholders. Focused empty hints are drawn once with space for the caret; selection, scrolling, deletion, narration and IME remain vanilla. Vanilla/other-mod text fields keep their original cursor.
- Storage cards use denser spacing: normal rows are 58 GUI pixels instead of 78, selection rows 54 instead of 62, with up to three columns where at least 240/220 GUI pixels per card fit. Names, owner/coordinates, item previews, inclusion, glow, rename/forget and scrolling remain available. Visible preview item icons and formatted counts are cached on refresh. Pagination and hitboxes use the same new row heights; all 1000 remembered containers remain accessible.
- **Validation:** the exact test.6 client/companion pass 85 area/camera/UI checks, the client passes 770 established regressions and 21 effects/layout/no-companion checks (876 total). Real selection/input hooks, claim/cancel messages, serialization, old-chest Mine updates, cache retention, animation geometry, editing and compact pagination were exercised. Stable rendered camera/review/caret/list screenshots were inspected. Dedicated-server/full-modpack/protection-mod compatibility and full-pack frame times remain unverified; this is not a published release.

### Inventory effects and material-list overlap

- The floating material list previously occupied the same right-hand inventory margin as active potion/status-effect cards. In the player inventory it now starts below the last visible vanilla effect card, accounting for the compressed spacing used above five effects, and can use the remaining screen height below the inventory. Its normal position returns when effects disappear. Recipe-book placement is respected through the inventory's current bounds. Scrolling, item counts and material controls remain available; at sizes with no room for one readable row, the existing Materials button remains available. Other containers keep their existing layout. No setting or data migration is needed. The final isolated client passes 21 effect-layout checks with server-synchronized one/seven effects, GUI scales 2/4, open/closed recipe book, wheel scrolling and removal while the inventory stays open; six screenshots were inspected. Full-modpack compatibility remains unverified.

### Companion storage labels and player filters

- Newly placed vanilla/trapped chests, barrels and shulker boxes receive the placing player's UUID/name when the updated companion is installed. Labels are stored with the server block entity, survive chunk saves and disappear with removed storage. Replacing or placing a carried shulker assigns the current placer; this identifies the placed container, not the provenance of its contents. Ender chests, minecarts and custom storage are excluded.
- Label drawing uses cached page strings/icons; the player filter catalogue is cached independently and never runs a contents search or distance sort. Label-only replies preserve the material-count/source-age index and goal views. There is no polling per frame or world-wide ownership scan.
- The storage and project storage-selection lists display player, Shared, Unclaimed or Mixed owners labels and provide a cycling Owner filter for everyone, mine, shared, unclaimed, mixed and individual players. The filter changes the visible list only; project quantities and the existing All/None controls retain their project-wide meaning. Container icons are cached with the labels during refresh.
- Open the storage, then use its list card's Rename → Storage owner controls to claim older unclaimed storage, mark it shared, or restore its player label. Shared storage retains its assigning player's UUID. Only that player or a game-master/operator can relabel it; changing labels requires the real supported storage menu to remain open, normal validity/access checks and an eight-block range. Mixed double-chest halves are labeled explicitly; edits require authority over both halves and update both.
- Labels are organization metadata, not inventory protection or remote discovery. Ownership requests never carry inventory contents or load remote chunks. At most 32 requests per player per 20 server ticks are handled; client pending requests are bounded to 32 for five seconds and cached replies to 256. Labels refresh on opening or viewing nearby remembered storage; remote saved labels reflect the last observation. Player names reflect the recorded name, while identity and filters use UUIDs.
- This feature requires the matching development companion and client. Older/absent companions retain normal client features and saved labels, with editing disabled. Existing storage starts Unclaimed; opening alone does not claim it. Optional client ownership metadata stays in version-4 memory files; no automatic world-wide scan or migration runs. The exact final pair passes 48 isolated ownership assertions, including actual placement/save-load and client/server label messages, non-owner rejection and all special list filters. The existing 770 client regression checks also pass. The cached catalogue accessor measured zero allocation over 100,000 reads in the isolated fixture; this is not a full-pack FPS claim. Screenshots and detailed limits are in development notes.

### Container icons throughout the storage list

- Ordinary and double chests now show a chest icon, and barrels show a barrel icon, alongside the existing dyed shulker icons in storage/search and project storage-selection cards. Previously only shulkers had a container icon. Headers reserve space consistently for the icon and controls; only shulkers receive the dye tint. Icons are cached on list refresh, with no new stack creation per rendered frame. Saved data and companion requirements for icons are unchanged. Normal and selection list screenshots with chests/barrels/colored shulkers were inspected.

### Shulkers in remembered storage

- A queued air/block update from before a local placement no longer discards that newer pending placement before its server confirmation. Mismatched candidates remain bounded by the existing five-second timeout and are discarded when their prediction sequence is acknowledged.

- **Reported problems:** picking up/moving shulkers left stale entries and duplicate material totals, and replacing a box on the ground did not update its remembered contents. Shulkers also looked like ordinary chests in the storage list.
- **Change:** server block updates remove remembered shulkers that are no longer present; loaded chunks reconcile already-stale positions. A successfully placed box can be remembered from its carried item contents after the server confirms the matching placed block, without reopening it. Reopening still refreshes contents from the actual menu. Placement candidates are bounded to 64 for five seconds and cleared across worlds/disconnects; rejected or unconfirmed placements are not remembered. Identical boxes at different confirmed positions are retained as separate boxes.
- **List appearance:** known shulkers have a matching box icon, colored accent and subtle card tint in storage/search/selection views; undyed and older unknown-color records use a neutral purple style. Coordinates, names, inclusion and glow controls remain available.
- **Data/limits:** optional dye metadata is added to existing version-4 memory files; older records load without migration and learn their color when reopened or placed. Contents changed remotely, replacements of the same color while unobserved, and unloaded locations still require fresh server/menu observations. No companion is required.
- **Validation:** the exact development client passes the integrated-server placement/removal/replacement/move fixture, independent identical boxes, single/section block updates, loaded-chunk reconciliation, stale-open-menu protection, rejected placement, all 17 dye/undyed icons and saved-color reload. Normal/selection list screenshots were inspected. The full user's multiplayer modpack/server remains unverified; see `docs/PERFORMANCE.md`.

### Equipment HUD order

- Damageable equipment now appears in hotbar order (left to right), then armor order (helmet, chestplate, leggings, boots), instead of sorting the HUD by remaining durability. Existing low-durability filtering, colors, percentage preference and exclusion of backpack/offhand spares remain. The detailed Equipment Watch screen retains its durability ordering. Slot-order, warning/filter and immediate threshold-invalidation checks pass; a real-client HUD screenshot with server-synchronized equipment was inspected.

### Full client performance pass

- Storage totals and oldest-source ages now share one included-storage index per data revision, instead of rescanning every remembered item for each material. Goal/visibility/HUD lists reuse immutable views until a project or storage edit; edits, exclusions, forgetting, Undo and project switches invalidate them immediately. Nearest-source lookup filters candidates directly instead of building and sorting a complete storage-search result.
- Inventory/storage searches reuse a bounded parsed query; import search normalizes the catalogue once, with shared regex patterns for parsing. Case, whitespace, namespace filters, stored-item singular fallback and conservative import resolution retain their existing behavior. Visible storage previews are sorted on refresh rather than every frame.
- Equipment HUD collection skips backpack/offhand entries before copying stacks. Tool refill reuses an unchanged tool observation and stops tracking while disabled. Idle bulk stripping avoids its readiness check when no companion job is pending. Repeated save requests for the same store coalesce while queued, with follow-up requests still allowed during a write; unchanged stores skip save scheduling.
- Existing limits (1000 remembered containers, 1024 goals per project, 16 projects), controls, item components and saved-file formats are unchanged. These changes retain the earlier 50 ms display refresh and Mouse Tweaks drag fix. No companion update is required.
- **Validation:** `1.0.5-test.3+mc26.3` builds and passes metadata/artifact checks plus **770 isolated runtime assertions**. Eleven controlled workloads were compared with test.2 using CPU/allocation recordings. For a 1000-container/1024-goal fixture, cold totals fell from 1014 ms to 4.44 ms; 20 nearest-source lookups fell from 494 ms/765 MiB allocated to 18.50 ms/0.66 MiB. These are warmed batch measurements, not FPS claims. Full results, data/compatibility limits and visual evidence are recorded in `docs/PERFORMANCE.md`; the user's complete modpack/server frame times remain unmeasured.

### Reduce repeated inventory and HUD work

- **Reported problem:** lag during normal play and in inventory menus with the 1.0.5 test client. Inspection found that toolbar updates could simulate Smart Deposit several times per rendered frame, while the equipment/project HUD rebuilt rows at render frequency and each material row could rescan the same open chest.
- **Change:** toolbar/deposit previews and gameplay HUD rows reuse display calculations for up to 50 ms. Hover highlights reuse the existing preview; menus without Smart Deposit support skip its preview/reason calculations. Disabled equipment HUD rows are not built. Project changes and HUD content settings invalidate cached HUD rows immediately; screen initialization/resizing and the drag-mode button refresh the toolbar immediately. Repeated material queries share one chest snapshot per client tick; incoming slot updates and closing a chest still explicitly refresh memory.
- **Behavior/data:** counts and preview text may trail a change by up to one normal tick (50 ms). Drawing/animation remain per frame and clicks still calculate/revalidate actual transfers against current inventory. No data migration, changed limits or companion update is needed; the Mouse Tweaks drag fix is retained.
- **Validation:** the JDK 25 build and project checker pass for `1.0.5-test.2+mc26.3`. An isolated client passed 18 performance/freshness checks and all 74 Mouse Tweaks Shift-drag checks. Compared with test.1, 200 consecutive toolbar calls caused zero additional deposit simulations after initialization instead of 200; 200 HUD calls rebuilt rows once instead of 200 times; 200 same-tick material queries caused no additional chest scan instead of 200. These are controlled work-count comparisons, not a measured FPS improvement on the user's full modpack/server; that validation remains outstanding.

### Restore matching Shift-drag with Mouse Tweaks

- **Problem:** Mouse Tweaks' empty-cursor Shift-drag handler runs before Stow's screen handler and can transfer every crossed item, bypassing Matching filtering and Both mode's Shift/Ctrl+Shift distinction. It can also interfere with Stow's bundle gestures.
- **Change:** Mouse Tweaks now yields while a Stow matching, all-items or bundle drag is active. Shift + left-drag transfers matching item variants in Matching and Both; Ctrl + Shift + left-drag transfers all crossed items in Both. Stow retains its initial item/component matching, source inventory and pinned/result-slot protections. Mouse Tweaks' other gestures and wheel actions remain available. Closing the screen clears Stow's drag ownership.
- **Compatibility/data:** integration is optional and does not require Mouse Tweaks. No settings, saved data or companion update is required. Runtime validation is tracked in `docs/DEVELOPMENT.md`; this is an unreleased fix.
- **Validation:** development build and project checks pass. The released build reproduced the incorrect transfer with Mouse Tweaks 2.31; the fixed JAR passed 74 isolated runtime checks with it and 72 without it. These exercise Fabric's mouse callbacks, Matching/Both filtering, release/close cleanup and retained independent Mouse Tweaks dragging. Physical input and the full user's modpack/server remain unverified.

## 1.0.4+mc26.3 — 19 October 2026

### Refill villager payments between batches

- **Reported problem:** whole-inventory trading still stopped after the first loaded payment stack. The previous implementation reused vanilla offer selection to refill; that operation first tries to unload both payment slots and can stop with partial payments still present. The client fallback also relied on predicted offer-use counts and the identity of an offer object to decide whether to continue.
- **Change:** both the companion and client fallback now top up matching payments directly in the merchant's two input slots, using ordinary pickup/place/return operations. Leftover payment items stay in the menu, including costs that do not divide evenly into a stack and two-item payments. Reversed compatible payment slots are retained. Stack limits, item components, slot permissions and the empty-cursor requirement are preserved. Unsuitable input contents stop rather than being replaced.
- **Client continuation:** progress is determined from actual slot changes rather than the client-side offer-use counter. Equivalent replacement offer objects no longer stop the operation; changed prices, payment/result components, inventory or user input still stop it. Stock, result-space and 128-batch limits remain.
- **Distribution:** the stable client and companion builds include this refill change. Update both JARs to use the companion refill fix; older companions retain the paced client fallback. Existing data, settings and the trade protocol remain compatible.
- **Validation:** compilation only. At the user's explicit request, no automated tests or in-game checks were run for this change. The reported behavior remains awaiting user validation.

## 1.0.4-beta.2+mc26.3 — 18 October 2026

Published as a **beta** while actual modpack/server and physical-input compatibility remain unverified. The updated companion adds instant bulk villager trading. Update both JARs for acceleration; older or absent companions retain paced client trading.

Final beta 2 validation: the exact client and companion artifacts passed **130 isolated Minecraft checks**, including instant single-task trading, rejected requests and authoritative inventory totals. The full build and metadata/artifact checks pass. Actual modpack/server and physical-input compatibility remain unverified. See `docs/DEVELOPMENT.md` for scope and evidence.

### Instant trades with Stow companion

- **New behavior:** with the updated client and companion, Shift + left-click a villager output trades from the whole inventory in one server task. Vanilla merchant selection and quick-move logic determine payments, outputs, experience and stock. No client-supplied item stacks or prices are accepted.
- **Limits:** the server checks the open merchant menu, its revision, cursor and selected offer. It stops for exhausted stock, insufficient payment or result capacity, invalid menus and no progress, with a hard limit of 128 refill/quick-move batches per request. Requests are rate-limited to one per two server ticks. Work already completed instantly cannot be cancelled by a later click.
- **Compatibility:** older/absent companions and servers setting `instantVillagerTrading=false` in `config/stow-companion.properties` (restart required) use the paced client fallback. Requests that receive no reply time out after five seconds without replay. Custom merchant menus are excluded from server acceleration. Pinned payment/result positions follow vanilla merchant behavior. Existing data and companion settings are preserved; the new server option defaults to true.

### Project pages and packaging

- Replaced the main client and companion descriptions with complete feature guides covering controls, requirements, quantity limits and compatibility. Canonical descriptions are saved under `docs/` and are the default for future website updates; release notes stay separate.
- Source JARs now include `LICENSE` and `NOTICE`, matching the attribution already included in both runtime JARs.
- The exact beta 2 pair also passed 121 isolated checks with instant trading disabled, exercising the paced fallback and cancellation. Modpack compatibility remains unverified.

### Compact hover descriptions

- **Problem:** long help text made Stow's hover panels bulky, especially the new attack settings. Shulker boxes also repeated their contents as text below the full preview grid.
- **New behavior:** English and German hover help for attack, previews, refill, sorting, tool replacement, log stripping and project completion now uses short summaries. Detailed behavior remains in the feature guide. When Stow's shulker grid is displayed, it replaces the duplicate written contents list; the name, custom lore and other item details remain visible.
- **Compatibility/settings:** text size and GUI scale are unchanged. Disabling shulker previews restores vanilla's written contents. Other containers and another mod's existing tooltip image retain their existing behavior. No new setting, companion update or data migration is needed.

Validation: the **1.0.4-test.2+mc26.3** development client builds successfully. Project checks pass with 414 bilingual keys. An isolated Minecraft client passed **56 tooltip checks**, including duplicate-list removal, retained title/lore, restoration of the written list when previews are disabled, ordinary container text, all box colors and existing preview protections. The rendered compact tooltip screenshot was inspected. Actual multiplayer modpack compatibility remains unverified.

### Auto-mending across the inventory

- **New control:** **Alt + M** toggles auto-mending; change or unbind it in the shortcut editor, or use **Toggle auto-mending** in the command palette. It starts off and stops when the eligible inventory cycle finishes.
- **Repair behavior:** damaged items with **Mending** are temporarily moved from the main inventory/hotbar into the offhand, most missing durability first. Collect normal XP to repair them. Each fully repaired item returns to its original position before the next is selected, and the original offhand item is restored. A damaged Mending item already in the offhand is repaired first. The selected main-hand slot stays available for farming; worn armor and the held main-hand item retain vanilla Mending behavior. No XP is generated and items without Mending are skipped.
- **Protection/interruption:** pinned donors and the selected hotbar slot are skipped. Names, enchantments and other components are preserved. Inventory operations and an occupied cursor delay swaps. Toggle off, opening menus, focus loss, pause, death or spectator mode stop activation; a borrowed item is returned once normal gameplay and a safe empty cursor allow it. Changing the borrowed item, its reserved position or selecting that hotbar position stops the cycle without overwriting the new state.
- **Disconnect limit:** disconnecting clears activation. If a connection closes before a return swap can be sent, the borrowed tool remains in the offhand and the previous offhand item remains in its reserved inventory position. No automatic cycle resumes on reconnect. No companion or saved-data migration is required.

### Shulker box previews

- **New behavior:** hover a shulker box to see a 9 × 3 grid of its 27 inventory slots in the tooltip, including empty positions, item icons, stack counts and item durability indicators. Undyed and all 16 dyed boxes are supported; empty boxes show an empty grid. The grid replaces the duplicate written contents list; names, lore and other item details remain available.
- **Setting:** **Settings → Inventory → Shulker box previews**, enabled by default. No Shift key or companion is required and existing settings files receive the default automatically.
- **Compatibility/limits:** previews read the contents component already present on the item; they do not open containers, fetch hidden server contents, move items or load nested containers. Only the first 27 positions are shown. Hidden tooltips/hidden container details remain hidden, and an existing tooltip image supplied by another mod takes priority. Contents and components are not changed.

### Hold Attack or click at an interval

- **New controls:** **Alt + A** toggles automatic Attack while playing. Choose **Settings → Automatic attack → Attack mode → Hold Attack / Interval clicks**. Hold keeps the normal Attack binding pressed (continuous mining and one initial attack); Interval sends repeated normal Attack clicks. The shortcut can be changed or unbound in Stow's shortcut editor, and the command palette also includes **Toggle automatic attack**.
- **Timing:** the interval is editable from **50 to 60000 milliseconds**, default **500 ms**. Clicks run on game ticks, at most 20 per second, without catch-up bursts after delays. Vanilla miss delays, attack cooldowns, item-use handling and Stow's tool protection still apply; this does not make a held attack repeatedly hit entities.
- **Stopping/settings:** toggle again to stop. Opening a menu/chat, switching away from the game, pausing, dying, entering spectator mode, disconnecting or shutting down stops it and releases any held Attack binding. Mode and interval are saved; activation is never restored automatically after menus, reconnects or restarts. Existing settings files receive the defaults without migration. No companion is required.

### Trade from the whole inventory

- **Previous behavior:** Shift-clicking a villager output used only the payments already loaded into the trade inputs, usually one stack. The 1.0.2/1.0.3 output-slot fixes preserved that vanilla limit; they did not implement trading across the whole inventory.
- **New behavior/control:** **Shift + left-click the trade output** to keep trading the selected offer, refilling both payment inputs from the main inventory and hotbar between batches. Trading stops when materials run out, the offer sells out, the output cannot fit, or a transfer makes no progress. Other recipe outputs retain their existing vanilla behavior; crossing a merchant output during Shift-drag still does not trade.
- **Interruption and bounds:** another click, a keypress (including Escape), scrolling, closing/changing the menu or disconnecting stops further batches. Unexpected inventory, offer, price or item-component changes also stop the operation. Without companion acceleration, follow-up batches are spaced four client ticks apart and 128 batches run per gesture, bounding unusual modded offers that return their own payment or replenish stock.
- **Compatibility/data:** uses vanilla merchant refill and transfer packets; no companion update, setting or saved-data migration is required. As with manually selecting and Shift-clicking a vanilla offer, merchant refills can use pinned payment stacks and result transfers can fill pinned inventory slots. The server remains responsible for trade availability and item matching.
- **Related fix:** Shift-clicking an offer-list button now reaches the vanilla button instead of starting an empty Stow Shift-drag. Ordinary inventory Shift-drag modes are unchanged.

Validation: JDK 25 Gradle build and project checks passed, including 414 English/German translation keys. The **1.0.4-test.1+mc26.3** development client passed **117 checks** inside an isolated Minecraft 26.3 client/server: 38 trading checks, 13 attack checks, 14 auto-mending checks using actual XP repair, and 52 shulker-preview checks. Actual preview screenshots were inspected. Published 1.0.3 baseline checks separately reproduced the single-input-stack limit. These earlier checks used a development build; the user's actual multiplayer modpack/server and physical-input compatibility remain unverified. See `docs/DEVELOPMENT.md` for evidence, test-fixture scope and the native-JVM startup workaround.

## 1.0.3+mc26.3 — 18 October 2026

### Refill firework rockets

- **Problem:** hand refill only watched main-hand block placement, so launching firework rockets never scheduled a refill. Vanilla consumes rockets on the server, after the local use call has returned.
- **New behavior:** **Refill blocks and rockets** now tops up the hand that used a block or rocket, including offhand rockets and Elytra boosts. Rockets wait for the server-confirmed count decrease, for up to five seconds, before moving any reserve. The existing 0–16 threshold applies; 0 refills after the final item is used. Creative mode and unsuccessful uses do not refill.
- **Matching/protection:** reserve rockets must have identical components, including flight duration and explosion data. Backpack reserves are preferred and pinned donor slots are skipped. Changing the item type/components, changing the selected slot for a pending main-hand refill, opening a menu, carrying a cursor item, or starting a sorting/deposit operation cancels the pending refill. No companion is required and existing refill preferences are retained.

### Faster chest sorting with the companion

- **Problem:** large chest sorts used many queued client clicks and took several ticks to finish.
- **New behavior:** with the updated companion on the server, sorting a vanilla chest, trapped chest, barrel, ender chest or shulker box sends one bounded request and applies the validated result in one server tick. Stack combining and the selected client sort order, including localized name order, are preserved. Pinned slots are excluded and the cursor and player inventory are untouched. Other viewers receive the usual container updates.
- **Controls/settings:** use the existing middle-click, Shift + middle-click and Ctrl + middle-click sort controls. Acceleration is automatic. Server owners can set `fastChestSorting=false` in `config/stow-companion.properties` and restart to retain the click queue.
- **Safety and limits:** the server checks the open menu, distance/validity, cursor, current contents, slot permissions, stack limits and exact item/component totals before changing any slot. Changed snapshots are rejected without moving items. Requests cover at most 54 storage slots and 24 KiB. Player-inventory sorts, custom/restricted menus, older or absent companions and oversized item data retain the existing client queue. A pending request times out after five seconds without replaying it.
- **Installation:** update both the client and companion for acceleration. Install both locally for single-player. Saved project and chest-memory data needs no migration.

### Show armor durability as a percentage

- **Problem:** enabling **Show percentage only** added the percentage beside the raw remaining durability, so the equipment HUD still displayed the durability number.
- **New behavior:** when enabled, the equipment HUD and its layout preview show only the percentage (for example, `4%`). When disabled, they show only remaining durability (for example, `10`). Low-durability colors and warning thresholds are unchanged. The detailed Equipment Watch screen continues to show percentage and durability counts together.
- **Compatibility:** no configuration or saved-data migration is needed.

Validation: JDK 25 build and project checks passed. The final Minecraft 26.3 client passed 112 release checks and 37 Finish/Reopen checks; acceleration-disabled sorting passed 61 checks across all five sort orders. Block-refill regressions passed 8 checks and the dedicated companion server passed both GameTests. Runtime coverage includes villager gestures, percentage-only rendering, actual Elytra rocket consumption/refill in both hands, fast-sort networking and rejection of invalid item changes. The actual multiplayer modpack and its Timber/protection mods remain unverified; see `docs/DEVELOPMENT.md` for evidence and compatibility limits.

## 1.0.2+mc26.3 — 17 October 2026

### Prevent accidental trades while Shift-dragging

- **Problem:** Stow's multi-slot Shift-drag could cross a villager trade output after starting on an offer input. The output's quick-move handler can repeat that trade, so one drag could consume several offers. Bundle drag gestures also now treat recipe and trade result slots as protected targets.
- **New behavior:** Stow's matching/all-item Shift-drag and Shift-bundle drag skip vanilla result slots. A drag across a merchant output no longer triggers a trade. Direct Shift-click on an output remains Minecraft's native action and can still batch-trade or batch-craft.
- **Controls/settings:** no new control or setting. Existing Shift-drag modes continue working on ordinary inventory slots.
- **Compatibility:** recipe and trade output slots remain owned by their vanilla menus; vanilla output transfers may still fill or merge into pinned inventory slots under that menu's rules.

### Finish a project

- **Problem:** after a build used up its materials, every goal drifted back toward red even though the project was done.
- **New behavior:** once every material in the active project is at its target, **Materials → Finish project** (footer, beside Undo) becomes available. Before that it is disabled and its tooltip explains why. A finished project keeps counting your inventory and included chests as you build, but counts, progress bars and the project HUD use one neutral blue instead of red/yellow/green, and the per-row status reads "N used" instead of "N missing". The project button shows "(finished)".
- **Reopen:** the same button becomes **Reopen project** and restores the normal planning colors. Finish and Reopen can both be reversed with Undo. Targets, chest selections, hidden goals and HUD stars are untouched.
- **Setting:** Settings → Project totals → **Show finished project totals** (on by default). Turn it off to hide the top-right project HUD while the active project is finished.
- **Storage:** the finished flag is saved per project in the existing chest-memory file (additive field; older files load as not finished).

- **Eligibility fix:** changing projects or reopening a finished project now invalidates cached Finish availability immediately. Clicking Finish also checks the current material counts again, so an outdated enabled button cannot finish a project whose targets are no longer met.
- **Compact layout fix:** the Finish/Reopen button adapts to narrow menus, and the material range label has its own line above the footer. Pagination reserves that space so the controls and material rows do not overlap.

Validation (9 October 2026): JDK 25 build and metadata/translation checks pass. An isolated Minecraft 26.3 Fabric client passed 37 automated checks covering Finish/Reopen, Undo, project isolation, save/reload, older saved data, finished HUD visibility, stale eligibility and footer geometry at 320, 360, 480 and 720 GUI pixels. Existing importer/project checks (47), export checks (6) and the companion harvest GameTest also pass. A subsequent live singleplayer session verified the rendered menu at GUI scale 4, Finish/Reopen/Undo, neutral colors below target, HUD updates when placing a block and finished-state persistence after leaving/rejoining. Actual multiplayer modpack compatibility and remembered chest updates on that server remain unverified. See `docs/DEVELOPMENT.md` for the validation record.

Validation (17 October 2026): JDK 25 Gradle build produced the client, companion and source JARs. `dev-tools/check-project.py` passed metadata, bilingual translation, license, wrapper and client/companion separation checks. The Minecraft 26.3 Fabric development client loaded Stow 1.0.2 without a Mixin injection error; the test environment did not expose a controllable game window, so the villager gesture was not verified interactively. See `docs/DEVELOPMENT.md` for this limitation and the remaining multiplayer compatibility notes.

## 1.0.1+mc26.3 — 9 October 2026

### Hardening

- Update checks now reject responses larger than 8 MiB instead of reading them fully into memory. Normal GitHub, Modrinth and CurseForge metadata is far below this limit. An oversized response counts as an unavailable service, just like other failed checks.
- The Gradle wrapper verifies the downloaded Gradle distribution against a pinned SHA-256 checksum.
- The local development bootstrap verifies downloaded libraries against pinned hashes and requires a hash for every Fabric library.

No gameplay, control or settings changes.

## 1.0.0+mc26.3 — 7 October 2026

### Stow 1.0 — your build starts with a list

**Copy a materials list from a YouTube description, turn it into a project, find the supplies in your remembered storage, and keep building.** Stow 1.0 brings plain-text import and export together with a more compact Materials menu, Minecraft item icons, tabbed navigation, customizable HUDs and automatic tool replacement.

- **Paste, review, build:** import ordinary text without a schematic mod. Correct uncertain items, combine duplicates, choose how quantities affect existing goals and undo the whole import.
- **Share your plans:** copy all saved material targets as portable plain text with exact item IDs, including goals outside the current search or page.
- **See more, read less:** denser material rows, the familiar pixel priority star, short tooltips, helpful empty states and clearer storage cards.
- **Make the HUD yours:** choose alignment, position and size, see additional tracked materials, and read equipment durability as a simple countdown.
- **Keep moving:** refill building stacks, replace worn or broken main-hand tools, preserve vanilla crafting-result Shift-clicks and receive brief feedback above menu blur.

Client and companion are packaged as **1.0.0+mc26.3**, for Minecraft **26.3**, Fabric Loader **0.19.5+** and Java **25+**. The client requires Fabric API and Cloth Config; the companion requires Fabric API. Existing project data, chest selections and material priorities retain their saved format. The companion keeps the connected-log harvesting behavior from 0.5.13; the importer, menus and tool replacement belong to the client.

### Plain-text export and first-use guidance

- Added **Copy material list** as a paper icon beside Add and Import in the full Materials toolbar. It copies every saved target in the current project in project order, including hidden materials and goals outside the current search/page. Unsaved quantity edits are excluded; save the target first to export it. Copy is disabled for an empty project and reports the number of copied material types.
- Export uses a project-name heading and one `quantity namespace:item_id` line per material, for example `128 minecraft:oak_log`. Exact IDs work across language settings and avoid ambiguous translated names. The text can be pasted into Import and reviewed normally. It exports target quantities, not remaining requirements, inventory counts, chest locations, visibility or HUD priority. Import's existing character, line, target and project limits still apply.
- Empty Materials now points to **Import** or **+**, then choosing project chests. Empty Storage explains opening a chest, barrel or shulker box to remember its contents. Empty material-source views identify that no sources have been remembered and name the item to store/open. Searches with no results suggest another name or clearing the search; menus opened without a world explain joining one. Guidance uses a clear heading and short wrapped text in English and German without increasing row spacing.
- Enabling chest glow now shows **Glow on**, matching the existing **Glow off** feedback from inventory, Quick actions and settings. Both use the same short 1.1-second top-right notification and respect the Action notifications setting.
- The focused German UI pass found that Back and Undo both read “Zurück.” Undo now reads **Rückg.**, fitting the existing compact button and distinguishing it from navigation; its tooltip still names what will be restored.

### Materials importer

- Added **Import** to the Materials toolbar. Paste a whole YouTube description or another plain-text material list into a multiline editor, or use **Paste clipboard**, then select **Review list**. Project data changes only after confirming the review.
- Recognizes common quantity formats such as `32 Oak Planks`, `Oak Planks x32`, `Glass: 64`, `Glass (64)`, `2 stacks of Glass`, and `Glass: 2 stacks + 16`. Bullets and basic Markdown formatting are accepted. Comma and German period thousands separators are supported; quantities must be positive whole numbers.
- Resolves exact item names and registry IDs against installed items, accepting the active language and English vanilla names. Stack quantities use each item's real maximum stack size, including items that stack to 16 or 1.
- Review keeps the original source line visible. **Edit** lets players select an exact item from search results and correct its quantity; individual rows can be included or skipped. Unrecognized or ambiguous names, invalid quantities and ordinary description text are excluded from import until corrected. Substring matches never silently select an item variant.
- Duplicate included rows are combined by item ID. Choose **replace matching targets** (default) or **add quantities** for materials already in the active project. Review displays the resulting target before confirmation. Other project goals and their visibility preferences are preserved.
- The whole import is one **Undo** action. Quantity overflow and the 1,024-material project limit are checked before changing any goals, so a rejected batch cannot partially modify a project. Paste limits are 65,536 characters and 2,048 lines; each resulting material target is limited to 1,000,000 items.
- After import, Materials opens at the first imported goal with a success notification; the project button's tooltip retains the imported material count. The footer's Undo action restores the whole batch. Review separates item names from quantities and resulting targets to keep counts readable at small GUI sizes. Existing project files keep their saved-data format, chest selections and pinned-material choices.
- Added English and German labels, review guidance, errors and completion feedback.

### Visual and interaction polish

- Removed Equipment Watch from the Quick actions menu and its search results to simplify navigation. The on-screen equipment HUD and Equipment HUD settings remain available. Existing optional Equipment shortcuts continue to work.
- Material priority in the full Materials list uses the same 7×7 pixel star as the floating inventory material markers, centered in its button. Selected stars are gold, unselected stars gray and unavailable stars dimmed. This replaces the Nether Star item model and the smooth star texture; the configured priority limit, tooltips and saved choices are unchanged.
- The full Materials menu uses 42-pixel rows instead of 68 and a tighter toolbar. The smallest supported 320×240 GUI shows two rows instead of one; a 480-pixel-high GUI shows seven instead of four. Names, total counts, target editing, Save and a progress bar remain visible. Source chests, inventory visibility, HUD priority and removal use compact icon buttons with tooltips. Missing counts and the inventory/chest breakdown appear inline when space allows; hovering a total always reveals the breakdown and snapshot age. Hover the current project button for the material and priority counts. Searching, scrolling, paging, Undo and quantity drafts remain available.
- Removed the inventory sort-order button and closed its toolbar gap. Middle-click sorting and its Shift/Ctrl order variants remain; configure sorting in Quick actions → Settings → Sorting, or through Mod Menu.
- Shortened English and German import guidance, refill/tool protection, pinned transfer, storage, project and material visibility tooltips. Important limits and behavior remain in the relevant settings or documentation. Import review's Edit tooltip previews at most 140 source characters; editing still retains the complete original line.
- The world project HUD now shows **+N more materials** when visible material types do not fit within the configured 3–5 rows. The count uses the actual shown goals and excludes deliberately hidden goals. It counts material types, not remaining blocks.
- Replaced generic action glyphs with actual Minecraft item models throughout inventory buttons and Quick actions: chest for Storage, writable book for Materials, map for Projects, iron pickaxe for Equipment, command block for Quick actions, hopper for Deposit, repeater for Settings, spyglass for Find, shield for Keep amounts, and lever for Shortcuts. Drag modes use paper, shulker box and bundle. Glow uses an Eye of Ender/ender pearl; material priority uses a pixel star. Basic confirm, remove, plus/minus and page controls use small pixel symbols inside vanilla buttons. Hover, selected states, tooltips and keyboard narration remain.
- Materials, Storage and Projects have centered top tabs at every screen size, replacing the wide-screen left sidebar. Shared menus use a dimmed world, Minecraft menu texture, centered titles, spaced cards and consistent footer controls. Quick actions stays centered as its filtered list changes. Material row controls fit the minimum supported 320×240 GUI viewport.
- Removed horizontal chest-card separators that ran through item previews. Names, coordinates, counts and actions have separate space. Item preview backgrounds are 24×25 GUI pixels, spaced 26 pixels apart; counts fit within 22 pixels and stay inside their background, including large abbreviated amounts.
- Import review and its item chooser use the existing 160 ms cubic list easing, moving hitboxes and clipping, with a review scroll indicator. The paste editor also eases wheel scrolling; cursor navigation and scrollbar dragging remain immediate.
- Settings use native category tabs instead of expanded global headers, removing the misplaced gray category lines. Building/tool options and advanced creative-order caching are expandable groups. HUD editors are the single place for position, alignment and individual size, avoiding duplicated settings that could overwrite a saved layout.
- Recipe and trade outputs preserve their own vanilla Shift-click behavior, including player and table crafting, stonecutting, smithing, anvil, loom, cartography, grindstone, furnace and merchant outputs. Result transfers bypass Stow's pickup queue and custom Shift-drag handling. **Keep Shift-transfers out of pinned slots** still applies to ordinary transfers; vanilla result transfers can fill or merge into pinned slots according to the menu's rules.

### HUD layout and action feedback

- Added a short **Glow off** notification when disabling chest glow from the inventory, Quick actions or settings, stopping it in Storage, or toggling off an already highlighted chest/material. It uses the same top-right position and 1.1-second duration as other glow feedback and respects the Action notifications setting. Automatic cleanup on disconnect/world changes or forgetting a chest remains quiet so it does not replace those actions' feedback.
- Project material rows align right by default like the equipment HUD: icons share the right edge and each count sits to their left. Removed the artificial minimum content width that left short rows away from the edge.
- Equipment HUD numbers default to remaining durability only, for example `1361`, and count down as equipment wears. **Show durability percentage** optionally adds the percentage beside the remaining durability. The detailed Equipment Watch screen shows both. Low-durability colors and thresholds remain.
- Equipment, project totals and building stock each have **left / center / right** alignment in their live layout editor. Dragging, corner selection and snap-to-edge control position; Reset restores that element's defaults. Alignment applies to the icon/count group and headings.
- Added **Settings → Appearance → Block count layout** with independent 60–200% size and free positioning. **Beside hotbar** retains automatic hotbar/offhand placement; dragging or selecting a corner switches to **Free position**. Shared HUD size still scales all three elements, with individual sizes applied relative to it.
- Action notifications render at the top-right screen edge after open menus, above their blur, and during gameplay, with a 140 ms entrance slide. Glow feedback lasts 1.1 seconds, sorting completion 1.2 seconds and routine feedback 1.8 seconds; recognized blocked, failed or interrupted actions stay for 2.8 seconds. New actions replace the current notice immediately instead of building a queue. Repeated identical notices do not extend the timer. **Appearance → Action notifications** controls these independently of HUD visibility.
- Added feedback for import, targets, visibility and material pins, project creation/rename/switch/delete, chest inclusion/exclusion/rename/forget, all/none storage selection, Undo, saved layouts/settings/keep amounts/shortcuts, inventory pins, sorting and tool replacement. Existing deposit, chest-source, stripping/harvest and automatic-tool feedback uses the same notification system. Sorting distinguishes completion from an interrupted transaction.

### Tool replacement

- Added **Inventory → Building & tools → Replace worn or broken tools**, with **Keep 1 durability** (default) and **After break** timing.
- Keep 1 durability swaps the worn main-hand tool into the spare's slot before another damaging use. Without a suitable spare, Stow prevents that use and explains why. Higher-cost actions reserve enough durability to avoid breaking, so a tool may be saved with more than one point. Mining/attack costs use item components; fishing rods, carrot sticks and crossbows reserve their higher vanilla use costs.
- After break waits for the local player's actual main-hand break event, then fills the empty selected slot. Dropping, moving or placing an item does not count as a break; selecting another slot cancels the pending replacement.
- Replacements require the same item type, prefer matching names/components/enchantments, then backpack spares and higher durability. Pinned spares, armor/offhand slots and worn-out candidates are skipped. Armor and elytra held for equipping are excluded. The vanilla hotbar swap leaves the cursor empty and keeps the worn tool.
- Automation waits during menus, sorting or deposit, and does not run for dead, creative or spectator players. Mining-tool selection avoids reserved tools while Keep 1 durability is enabled. Normal block interactions are evaluated before protecting a tool's use-on-block action.
- This client helper replaces main-hand items. Worn armor and offhand items are excluded. Server damage to an item already in use and custom modded durability costs require game-specific compatibility checks.

### Chest memory and material refresh

- Added a Forget control to project chest-selection cards, including the material's source-chest view. These cards previously offered only inclusion, glow and Rename, so forgetting required switching to the general Storage view. The compact X button next to Glow forgets that chest's saved contents; its tooltip distinguishes this from excluding it from a project. Both storage views use the existing Forget notification and Undo. Card headings and item previews reserve space for the controls. Material goals and the remembered project storage selection are retained for rediscovery.
- Forgetting a chest removes its remembered contents without deleting project material goals or changing that location's project storage selection. Missing chests contribute zero to totals and the selected-chest count.
- Rediscovering a previously selected chest restores its contribution using fresh contents. Its selection survives saving and restarting.
- Newly discovered or forgotten-and-reopened chests are assigned to the **active project** immediately by default, including projects with an empty custom selection. **Assign discovered chests to current project** in Project totals can disable this. Refreshing a known excluded chest leaves it excluded; other projects retain their own selections. **None** clears storage selections, and with auto-assignment disabled they stay empty on rediscovery. Notifications distinguish remembering a chest from adding it to the project.
- Forgetting the currently open chest suppresses further snapshots from that menu until a chest is reopened or Forget is undone, preventing the forgotten snapshot from immediately reappearing during a material-count refresh. Undo Forget resumes live updates and preserves a newer rediscovered snapshot.
- Materials refreshes its rows and controls when chest memory changes, including forgetting, rediscovery and updated contents, while keeping drafted quantities.

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
