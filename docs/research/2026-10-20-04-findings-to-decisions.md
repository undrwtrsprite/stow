# Findings → decisions for Stow v2 — 2026-10-20

Each row links a finding (baseline `B…`/§ or a deep dive `DD n` in
`2026-10-20-03-deep-dives.md`) to a decision for the redesign. **Adopt** = take the pattern
largely as is; **Adapt** = take the idea, change the form; **Investigate** = needs a code
spike or a test in phase 2; **Reject** = out of scope or duplicates another mod.
Labels as before: Observed / Inferred / Unverified. Nothing here has been tested in a client.

| # | Finding | Source | Decision | v2 consequence |
| --- | --- | --- | --- | --- |
| F1 | Search, filter, page and selection are lost when switching Storage/Materials tabs (`navigate()` passes `""`) | Baseline §2 finding 1 (Observed, source) | **Adopt** shared session state | One Stow screen; query, selection and scroll kept per segment for the session (Tom's "Keep search", DD 9). |
| F2 | Storage is container-first; no per-item total, no nearest-first ordering | Baseline finding 2 (Observed) | **Adopt** item-first results | Find lists items with known total, source count, nearest distance and freshness (Chest Tracker grid, DD 1). |
| F3 | Locating needs Glow then two Esc presses | Baseline finding 3 (Observed) | **Adapt** | Enter = "Show nearest" closes the screens and starts the glow; Shift+Enter shows without closing. |
| F4 | Hover + key lookup is the shortest find path in the genre (Chest Tracker `Y`, JEI hover keys) | DD 1, DD 11 (Observed, docs/lang) | **Adapt** | Hover + `G` = find in storage. `G` chosen to avoid vanilla `F` (swap offhand) and Chest Tracker's `Y`. **Decision needed:** Stow's policy says new actions start unbound. |
| F5 | Projects is a separate screen just to switch/rename | Baseline finding 4 | **Adopt** inline switcher | Project segment has a drop-down switcher with Create / Rename / Finish / Reopen / Delete. |
| F6 | Litematica's Material List table (Total / Missing / Available) and stack + shulker breakdown | DD 4 (Observed, screenshot + source) | **Adopt** columns, **Reject** schematic features | Project table: Item / Need / Have / Missing / Sources / HUD; breakdown in the detail pane and tooltip. |
| F7 | Material HUD: one key marks containers holding needed blocks; chest slots glow | DD 5 (Observed, description) | **Adapt** for player projects only | N4: in an open container, slots holding missing project items get a gold fill + marker; N3 "Take missing". |
| F8 | IPN: visible Sort button; tooltip teaches modifiers; scroll over button changes order | DD 3 (Observed, lang) | **Adopt** | Inventory strip Sort button: click sorts, scroll changes order, tooltip names middle-click and Shift/Ctrl orders, right-click opens options. |
| F9 | Sort, pin, track, prioritize, whole trade, drag modifiers have no visible affordance | Baseline finding 6 | **Adopt** key legend | "?" legend in container screens; shown automatically for the first 3 opens, then on demand. |
| F10 | Settings split between Cloth Config and 5 custom editors; HUD placement in 3 editors / 3 categories | Baseline finding 5 (Observed, JAR + source) | **Adapt** | Native settings screen in Stow's menu style, task categories, global search; one Arrange HUD editor. Cloth removal = **Investigate** (dependency change, Mod Menu hook). |
| F11 | Stow keys are not vanilla `KeyMapping`s, so Controls/Controlling can't see them | Baseline §1.4, DD 8 | **Investigate** | Spike: register mappings with category + context text while keeping chord/conflict logic. Until then Stow's Controls page shows a "Where" column and conflicts with vanilla keys (JEI context strings, DD 8). |
| F12 | Players look for config in Mod Menu; config button sometimes missing | DD 8 (Observed, issues) | **Adopt** | Keep the Mod Menu entry (optional dep) pointing at the native settings. |
| F13 | Tom's: auto-focus search on open; performance cost labelled at the option | DD 9 (Observed, lang) | **Adopt** | Query bar focused on open; any option with a cost is labelled "(may cost FPS)". |
| F14 | Genre failure: doubled quantities with double chests / other storage mods | DD 9 (Observed, issues) | **Investigate** (test) | Phase 3 fixture: double chests, same position re-seen, shulkers inside chests; totals must not double. |
| F15 | Jade: say plainly what needs the server; narration key; oversized payload silently dropped (#824) | DD 6 (Observed) | **Adopt** | "Needs Stow Companion" badge on companion-only actions; failures visible in the action toast, never silent. |
| F16 | Shulker Box Tooltip: two densities; modifier-lock defaults disliked (#261) | DD 7 (Observed) | **Keep** current Stow rule | Keep plain-hover 9×3 preview, defer to another mod's tooltip; no lock modifiers by default. |
| F17 | Mouse Tweaks owns drag gestures for many players | DD 2 (Observed) | **Investigate** (coexistence) | Gesture ownership stated in Settings → Inventory; test deferred. |
| F18 | WorldEdit: two clicks for a footprint, one command for full height, size readout | DD 10 (Observed, docs) | **Adapt** | Area claim: start in the camera, `V` (proposed) = full allowed height, live size. |
| F19 | JEI search history (previous/next) | DD 11 (Observed, lang) | **Adopt** | ↑ in an empty query recalls recent queries (N6). |
| F20 | Snapshot age, Undo, action toasts, import review, conservation, complete DE | Baseline finding 9 | **Keep** | Freshness on every source row; named Undo in the footer; import stays reviewable. |
| F21 | Chest Tracker: data persistence complaints, in-world labels in F1 | DD 1 (Observed, issues) | **Avoid** | No new in-world labels; glow/label hidden with F1 (verify in phase 3). Data format untouched. |
| F22 | Quick-stack to nearby chests, remote withdrawal, schematic verification, minimap, recipes | Sorted/IPN, AE2/Tom's, Litematica, Xaero, JEI | **Reject** | Not Stow's job, or needs server-side remote access Stow does not have. |
