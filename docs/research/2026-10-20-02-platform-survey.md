# Platform survey and shortlist — 2026-10-20

## Method

- **Modrinth:** official API `api.modrinth.com/v2`, read 2026-10-20 at 19:42 CEST
  (17:42 UTC). 18 discovery searches sorted by downloads (`inventory`, `sort`, `storage`,
  `chest`, `schematic`, `building`, `tooltip`, `preview`, `hud`, `config`, `minimap`,
  `recipe`, `search`, `waypoint`, `shulker`, `backpack`, `keybind`, `durability`) plus 16
  targeted name searches. Then `/projects` and `/project/{id}/version` for 38 candidates.
  Raw: `data/2026-10-20/modrinth-search-2026-10-20.json`,
  `data/2026-10-20/modrinth-projects-2026-10-20.json`; scripts alongside.
- **CurseForge:** the official API needs a key, and `curl` to curseforge.com returns 403.
  Two sources, labelled per row:
  1. **Official project page** read through WebFetch, 2026-10-20 ≈19:45–19:55 CEST (8 mods).
  2. **cfwidget mirror** (`api.cfwidget.com`, third-party cache of CurseForge data), read
     19:43 CEST. It **lags** (Mouse Tweaks: mirror 483,116,369 vs official 483,135,665)
     and its file lists are partial. Use those values as approximate only.
     Raw: `data/2026-10-20/curseforge-cfwidget-2026-10-20.json`.
- All counts are **all-time, whole-project, per platform**. Modrinth and CurseForge are
  kept in separate columns and never summed or ranked against each other. "Fabric 26.3"
  is the newest Modrinth version whose `game_versions` contains `26.3` and whose
  `loaders` contains `fabric`. Its own per-file download count is in the raw JSON.
  Neither platform gives a recent-window popularity figure in what I read, so "recent
  evidence" below means update dates and issue activity, not download velocity.
- **Issue trackers:** GitHub search API through `gh` (top issues by comment count, and
  issues created since 2026-01-01). Codeberg API for IPN. Raw JSON is in `data/2026-10-20/`.

## Shortlist (20 entries)

Selection: the leaders by adoption in each overlapping area (inventory gestures/sorting,
storage memory, storage search, planning, previews, contextual HUD, configuration and
keybinds, search, HUD/navigation), plus smaller or historical mods that solve a Stow problem
unusually well or overlap Stow directly. Chest Tracker counts as one entry with two rows
(the original is stuck on 1.21.4; the community port carries 26.3).

| Area | Mod | Modrinth all-time | CurseForge all-time | Fabric 26.3 on Modrinth (exact) | 26.3 loaders (Modrinth) | Modrinth updated | License |
| --- | --- | ---: | ---: | --- | --- | --- | --- |
| Inventory | [Mouse Tweaks](https://modrinth.com/mod/mouse-tweaks) | 63,931,786 | 483,135,665 (official page) | `26.3-2.31-fabric` (2026-09-15, release) | fabric, forge, neoforge | 2026-09-20 | BSD-3-Clause |
| Inventory | [Inventory Profiles Next](https://modrinth.com/mod/inventory-profiles-next) | 42,280,795 | 38,092,867 (official page) | `fabric-26.3-2.3.8` (2026-09-27, release) | fabric, forge, neoforge | 2026-09-27 | AGPL-3.0-or-later |
| Inventory | [Inventory Essentials](https://modrinth.com/mod/inventory-essentials) | 2,283,717 | 51,843,581 (cfwidget mirror) | `26.3.0.5+fabric-26.3` (2026-10-12, release) | fabric, neoforge | 2026-10-12 | LicenseRef-All-Rights-Reserved |
| Inventory | [Item Scroller](https://modrinth.com/mod/item-scroller) | 3,136,251 | 6,210,278 (cfwidget mirror) | `0.33.1` (2026-09-27, release) | fabric | 2026-09-27 | LGPL-3.0-only |
| Inventory | [Mouse Wheelie (historical; Stow ancestor)](https://modrinth.com/mod/mouse-wheelie) | 2,436,871 | 9,877,326 (cfwidget mirror) | none; nearest Fabric `1.16.3+mc26.2` for 26.2 | — | 2026-06-18 | Apache-2.0 |
| Inventory | [Sorted (small)](https://modrinth.com/mod/sorted) | 20,134 | no CF project found (cfwidget 404) | `0.1.29+26.3` (2026-10-20, release) | fabric, neoforge | 2026-10-20 | LicenseRef-All-Rights-Reserved |
| Storage memory | [Chest Tracker (original)](https://modrinth.com/mod/chest-tracker) | 997,753 | 1,152,554 (official page) | none; nearest Fabric `2.6.7+1.20.1` for 1.20, 1.20.1 | — | 2025-02-04 | LGPL-3.0-only |
| Storage memory | [Chest Tracker port (26.3)](https://modrinth.com/mod/chest-tracker-port) | 617,458 | not found on CF (not checked further) | `2.8.4+26.3` (2026-10-01, release) | fabric, quilt | 2026-10-01 | LGPL-3.0-only |
| Storage memory | [Storage Logger / Storage Tracker (small)](https://modrinth.com/mod/storage-tracker) | 3,438 | not found on CF (not checked further) | `1.2.1+26.3` (2026-10-02, release) | fabric | 2026-10-02 | MIT |
| Storage | [Tom's Simple Storage](https://modrinth.com/mod/toms-storage) | 21,084,014 | 55,355,525 (official page) | `26.3-2.12.2-fabric` (2026-10-15, release) | fabric, neoforge | 2026-10-15 | MIT |
| Storage | [Applied Energistics 2](https://modrinth.com/mod/ae2) | 6,352,324 | 229,283,000 (cfwidget mirror) | none; nearest Fabric `15.4.11` for 1.20.1 | neoforge | 2026-10-03 | LicenseRef-Multiple |
| Planning | [Litematica](https://modrinth.com/mod/litematica) | 27,903,433 | 14,546,745 (official page) | `0.29.1` (2026-09-27, release) | fabric | 2026-09-27 | LGPL-3.0-only |
| Planning | [Litematica Material HUD (small)](https://modrinth.com/mod/material-hud) | 15,471 | unavailable (cfwidget 404) | `1.0.17+26.3` (2026-10-20, release) | fabric | 2026-10-20 | GPL-3.0-or-later |
| Area selection | [WorldEdit](https://modrinth.com/mod/worldedit) | 11,797,935 | 66,381,150 (cfwidget mirror) | `7.4.6` (2026-10-15, release) | bukkit, fabric, folia, neoforge, paper, spigot | 2026-10-15 | GPL-3.0-only |
| Preview | [Shulker Box Tooltip](https://modrinth.com/mod/shulkerboxtooltip) | 40,853,103 | 20,554,218 (official page) | `5.4.2+26.3-fabric` (2026-09-16, release) | fabric, neoforge | 2026-09-16 | MIT |
| Preview/HUD | [Jade](https://modrinth.com/mod/jade) | 73,354,414 | 279,090,584 (official page) | `26.3.5+fabric` (2026-10-02, release) | fabric, neoforge, quilt | 2026-10-13 | CC-BY-NC-SA-4.0 |
| Config | [Mod Menu](https://modrinth.com/mod/modmenu) | 159,196,740 | 83,442,347 (cfwidget mirror) | `21.0.0` (2026-09-23, release) | fabric, quilt | 2026-10-12 | MIT |
| Config/keys | [Controlling](https://modrinth.com/mod/controlling) | 42,154,344 | 398,543,190 (cfwidget mirror) | `26.3.4` (2026-10-09, release) | fabric, neoforge | 2026-10-09 | MIT |
| Search | [JEI](https://modrinth.com/mod/jei) | 85,047,365 | 637,918,312 (cfwidget mirror) | `31.16.0.87` (2026-10-20, beta) | fabric, neoforge | 2026-10-20 | MIT |
| HUD/navigation | [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) | 119,905,797 | 255,759,407 (official page) | `fabric-26.3-26.6.1` (2026-10-20, release) | fabric, forge, neoforge, quilt | 2026-10-20 | LicenseRef-All-Rights-Reserved |
| HUD | [InventoryHUD+](https://modrinth.com/mod/inventoryhudplus) | 11,005,186 | 88,859,088 (cfwidget mirror) | `3.4.36` (2026-09-20, release) | fabric, forge, neoforge | 2026-09-20 | LicenseRef-All-Rights-Reserved |

### Why each is on the list (one line each), with recent evidence

| Mod | Why it is relevant to Stow | Recent evidence (Observed unless marked) |
| --- | --- | --- |
| Mouse Tweaks | Inventory-gesture leader; Stow already integrates with it (`MixinMouseTweaks`). | 2.31 for Fabric 26.3 on 2026-09-15; recent issues include "[26.3 Fabric] Right clicking bug" (#220, open) and "Ghost blocks" (#219, open). |
| Inventory Profiles Next | Biggest client sort/lock/refill/profiles mod; closest feature overlap with Stow's inventory side. | 2.3.8 on 2026-09-27 for 26.1.2–26.3 on all loaders. Codeberg has 53 open issues; the GitHub mirror is archived (2025-06-17). |
| Inventory Essentials | Large CF adoption for transfer shortcuts; a minimal-UI model. | 26.3.0.5 on 2026-10-12. Source is All Rights Reserved: behaviour only, no code. |
| Item Scroller | Power-user transfer gestures (masa family); shows the cost of hidden gestures. | 0.33.1 on 2026-09-27 (Fabric only). |
| Mouse Wheelie | Stow's sorting code descends from it (Apache-2.0, NOTICE). Shows what users of the ancestor expect. | **No 26.3 build**; nearest is `1.16.3+mc26.2`. Stow declares `breaks: mousewheelie`. |
| Sorted (small) | Very recent, focused: Quick Stack to nearby chests, keep lists, find stored items, with clear button UX. | Updated today (0.1.29+26.3). About 20k Modrinth downloads; no CF project found. |
| Chest Tracker (+ Where Is It) | **Closest competitor to Stow's chest memory**: item-first remembered storage, search, in-world highlight. | Original last updated 2025-02-04 (1.21.4); port 2.8.4+26.3 on 2026-10-01. Port has 8 open issues; top ones are about data not saving after relog (#22, #25, #16). |
| Storage Logger / Storage Tracker (small) | Opt-in tracked containers, per-item totals, areas; tested in the earlier lab. | 1.2.1+26.3 on 2026-10-02; about 3.4k downloads. Slug is `storage-tracker`. |
| Tom's Simple Storage | Most adopted "terminal" search with options to keep and auto-focus the query. | 26.3-2.12.2 Fabric on 2026-10-15. 238 open issues; a recent one is "Missing ingredients not marked in Crafting Terminal (Fabric 26.3)" (#806). CF page covers Forge/NeoForge only (Fabric is a separate CF project). |
| Applied Energistics 2 | Reference item-first terminal: sort/view/height controls, Shift freezes positions. | NeoForge only on 26.3. Last Fabric build is for 1.20.1. Docs only. |
| Litematica | Planning leader: Material List with Total/Missing/Available, HUD shopping list. | Modrinth 0.29.1 for 26.3 (sakura-ryoko port, 2026-09-27). The CF page's newest file is 26.2 beta 0.28.3, and the CF page carries a **security notice** for 1.21+ builds. Recent issue: "Searching has gotten really slow" (#406). |
| Litematica Material HUD (small) | Directly overlaps Stow: material list plus remembered chests that hold needed blocks, highlighted. | 1.0.17+26.3 released today. About 15k downloads. |
| WorldEdit | Canonical area selection (wand LC/RC, `//expand`, selection modes, CUI). | 7.4.6 for 26.3 Fabric on 2026-10-15. Recent: "26.3 Command Access does not allow non-hosts" (#3033). |
| Shulker Box Tooltip | Preview leader. Same feature as Stow's preview. Earlier lab: the 5.4.2 jar crashed. | 5.4.2+26.3 on 2026-09-16. Recent: "Holding CTRL to lock tooltips should be disabled by default" (#261), "Game freezes … Ender Chest with server integration" (#264, open). |
| Jade | Contextual HUD with narration, toggle and config keys; client works alone, server adds data. | 26.3.5 on 2026-10-02. Recent: "Container item list is silently dropped when server data exceeds 16 KB" (#824, open). |
| Mod Menu | Where Fabric players look for configuration (searchable list → config button). | 21.0.0 for 26.3 on 2026-09-23. 116 open issues; recent i18n regressions (#1100, #1101). |
| Controlling | Keybind search and conflict display in vanilla Controls. Stow bindings are invisible to it. | 26.3.4 on 2026-10-09. Recent: "Do not show key bind duplicates from debug menu" (#237, open). |
| JEI | Search, history and bookmark conventions players already know; context-labelled keybinds. | 31.16.0.87 **beta** for Fabric 26.3, today. Recent crash reports on inventory open (#4565, #4568). |
| Xaero's Minimap | Most adopted HUD/navigation: free HUD positioning, waypoints, key bindings in vanilla Controls. | fabric-26.3-26.6.1 today. All Rights Reserved; docs only. |
| InventoryHUD+ | HUD placement/customization for inventory and armor data, overlapping Stow's Equipment HUD. | 3.4.36 on 2026-09-20. All Rights Reserved; listed only. Not deep-dived. |

### Considered and not shortlisted

Client Sort, Inventory Management, Inventory Sorting (Kyrptonaught): sorting is covered by
IPN, Mouse Wheelie and Sorted. Storage Drawers, Sophisticated Backpacks/Storage: physical
storage blocks, not memory or search, and Sophisticated is NeoForge-only on 26.3 per
Modrinth. REI and EMI: JEI covers search conventions, and EMI has no 26.3 build on
Modrinth. YACL and Cloth Config: libraries; Stow's own settings decision is in the design.
MiniHUD, BetterF3, Uku's Armor HUD, AppleSkin: HUD references weaker than Jade or Xaero
for Stow's problems. Quark: no 26.3 build. Searchables: library used by Chest Tracker.
