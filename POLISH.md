# Polish review — stow 0.5.6

The design stays native to Minecraft: the BetterGrassify-style Cloth Config settings, standard button sprites, item models, quiet list cards and background-free HUDs. The pass focuses on clarity, predictable input and consistent proportions.

| Area | Judgment and result |
| --- | --- |
| In-world HUD size | The building count was more readable than the other panels. Stock, equipment and project totals now share its base size, font, vertical centering and six-pixel icon/text gap. Appearance has one shared size control; panel adjustments remain available. Saved sizes and margins are preserved. |
| Equipment visibility | Two settings could disagree and hide the panel unexpectedly. Settings now exposes one equipment HUD visibility switch, beside its all-equipment and warning options. |
| Settings at large GUI scale | Native inline labels could disappear beneath controls. Sliders, toggles and numeric fields now put wrapped labels above their native controls when space is tight. Category names are short enough for the narrow sidebar. Native Save, Cancel, Reset, validation and keyboard controls remain. |
| Equipment alignment | A status heading changed the row alignment. Alignment now belongs to the panel, so icons retain their right edge with or without a heading. |
| HUD placement | Equipment had a drag editor; projects had only numeric margins. Both now use the actual fullscreen screen coordinates and live row/layout/draw code. No canvas-origin offsets remain. Zero margins reach the true bottom/right edges; Snap to screen edge makes this explicit. Controls occupy the opposite corner; Save applies the draft, Back discards it. |
| Materials | Quantity drafts now survive changing pages and GUI size. Resizing keeps the active item, focus and caret. Incoming goals preserve an active edit. Switching projects clears drafts belonging to the previous project. Saving remains explicit. |
| Material picker | Mouse selection already focused the quantity. Enter now does the same from search; the next Enter saves the goal. This supports a fast search → amount → confirm flow. |
| Projects | Enter in the name editor previously created another project. It now renames the active project; creation has its own explicit button. Delete still has Undo. |
| List screens | Equipment, keep amounts and shortcuts use the same card treatment as materials and storage. Save sits beside Back; paging and page labels follow the same footer layout. |
| Palette | Keep the short contextual default list. Detailed shortcut/keep editors and glow can be found through search. Actions opened from nested stow menus return to the original inventory/world. Wheel input outside the palette is ignored. |
| Storage | Stable selection order, preview icons and separated coordinates/actions remain. Hidden highlights now show Glow rather than misleadingly offering Hide. Pagination matches the other lists, and dimension labels are translated. |
| Hover help | Material help combines the mouse/priority gestures into one short line. Snapshot age appears only once at least five minutes old. Obvious text-button glow help was removed; icon actions and ambiguous/truncated content retain useful labels. |
| Inventory UI | Actual downloaded Material Design Icons replace the handwritten glyphs; filled recognizable objects have distinct action semantics and drag-mode shapes. Tiny lock/highlight, right-aligned eye, floating counters and recipe-book-relative placement remain. Equipment stays out of the inventory overlay. |
| Inventory behavior | Keep the existing Mouse Wheelie-derived sort orders/gestures, immediate deposit, keep amounts, pins, refill and tool picking. The client transaction regression suite checks count/component preservation and empty cursors. |
| Locator | Keep the native spectral outline and existing distance/bob-compensated projection. Use a neutral white chest name, grey distance and a fixed screen-space gap rather than a mint caption floating 0.4 world blocks above it. No scene/depth/shader changes. |

## References and boundaries

- Inspected the supplied BetterGrassify 1.8.8 Fabric 26.3 jar, including its actual expanded/globalized Cloth Config builder. Public source: https://github.com/UltimatChamp/BetterGrassify .
- Retained the Mouse Wheelie-derived inventory behavior from the supplied jars and existing port. Upstream feature reference: https://github.com/Siphalor/mouse-wheelie .
- Checked the official Cloth Config screen-builder guidance: https://shedaniel.gitbook.io/cloth-config/using-cloth-config/creating-a-config-screen .
- The Windows PrismLauncher mods folder was not mounted in this workspace. This is a review of the available stow source and supplied references, not an inspection or compatibility claim for every mod in that folder.
- Client checks and screenshot inspection cover GUI scales 1–4 and multiple viewport sizes. See VALIDATION.md for exact results and the limits of the game fixtures.

## 0.5.5 spacing and motion

- Materials: 68px cards, metadata at +21, progress at +33–35, actions at +40. No overlap between text and bar. Centered checkmark saves targets; centered X removes entries. Shared geometric icons center X controls in all planner/keep menus.
- All custom paginated lists: wheel moves one row (a whole pair in grids), not a page. A 160ms cubic ease preserves directional continuity; fractional trackpad/wheel deltas accumulate, reverse cleanly, and cannot jump an unbounded number of rows. Page buttons still advance a window. Range labels and thin scroll indicators orient the user. Storage selection does not reorder cards.
- Animated widgets use stable original positions rather than accumulating translated coordinates; hitboxes follow the visual rows, and clipping prevents footer actions from receiving a row click. Settings use Cloth Config's native smooth list/tab scrolling.
- Labels and tooltips: shorten hints and blocked-state explanations; reserve bold for hover headings and brighter color for actual metadata numbers. Hide/Show and Sources explain their scope when hovered. Native controls and locale parity remain.
- The 0.5.5 hand-drawn icons used Lucide semantic references but did not communicate their actions well enough; 0.5.6 replaces them with actual downloaded Material Design Icons. Actual material/equipment item models remain Minecraft models.

## 0.5.6 icon replacement

Use concrete objects and familiar UI symbols, not homemade outline approximations. Storage is a chest, Materials is a clipboard list, Quick actions is a lightning bolt, Deposit is an arrow into a tray, glow is an eye/struck eye, and Settings is a gear. Track/add uses a different clipboard-plus from the materials list, and Find uses a magnifier rather than repeating the storage chest. Equipment uses a recognizable pickaxe. All 24 icons are fetched from the same official upstream, including standard X/check/page controls, and retain their actual SVG paths.

SVG sources, upstream commit, mapping and license are kept in `design/icons/mdi`. Runtime uses transparent 64px PNGs tinted by active/disabled/priority state and drawn in a centered 16px box. Filled paths remain visible at small physical sizes. Inventory buttons, palette rows and planner controls share these exact resources. No network or external graphics library runs in Minecraft.

Sources: https://pictogrammers.com/library/mdi/icon/treasure-chest/ , https://pictogrammers.com/library/mdi/icon/clipboard-list/ , https://pictogrammers.com/library/mdi/icon/tray-arrow-down/ , https://pictogrammers.com/docs/general/license/ .
