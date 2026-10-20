# Brief, working rules and prior evidence — 2026-10-20

Phase 1 of 3 (brief, baseline, research, design). No UI code, no commits or pushes on
lunapc, no publishing. All heavy work ran on the box in `/workspace/stow-redesign/`.

## Sources read

| Source | Location | How obtained |
| --- | --- | --- |
| Original brief | Attachment on lunapc (local user path omitted from the repository copy) | One `CopyToBox` from lunapc → `/workspace/stow-redesign/brief/pasted-text-1.txt` |
| `AGENTS.md` | repo root, `main` @ `5dd38ff` | `gh repo clone` on the box |
| Prior research/design/validation records | lunapc working tree (branch `codex/stow-overhaul`) | One `git status`, one directory listing, one `Compress-Archive` of `docs/ src/ dev-tools/ CHANGELOG.md README.md build.gradle` to `%TEMP%`, one `CopyToBox`. `MODLOG.md` was not read, copied or touched. |

Lunapc commands were run one at a time, spaced out, all light (no Gradle, no client, no clone).
The `%TEMP%\stow-wt-snapshot.zip` file (1.3 MB) was left on lunapc; it can be deleted any time.

## What the brief and AGENTS.md require (summary)

Brief: a full research → redesign → implement → test → measure overhaul. Shortlist 10–15
mods, examine ≥5 closely, run references in isolated instances where feasible, separate
observed/interpreted/unverified, map findings to adopt/adapt/investigate/reject, preserve data
and server authority, measure performance before optimizing, keep `CHANGELOG.md` Unreleased
current, keep a persistent plan in the repo, do not publish. "Do not stop after an audit or
mockups."

AGENTS.md: changelog discipline for every user-visible change; distinguish implementation from
validation; keep `docs/PROJECT-DESCRIPTION.md` and `docs/COMPANION-DESCRIPTION.md` canonical
and aligned with README and platform pages.

## Divergences between brief / AGENTS.md and this phase's request

| # | Brief / AGENTS.md | This request | Resolution in phase 1 |
| --- | --- | --- | --- |
| 1 | Implementation goal; "do not stop after an audit, mockups…" | Phase 1 only: brief, baseline, research, design; **no UI code** | Followed the request. Phase 1 is explicitly a stage, not the finish; implementation is phases 2–3. |
| 2 | Shortlist ~10–15, ≥5 close examinations | 15–20 shortlist, ≥8 deep dives | Did the larger number (20 entries, 10 deep dives); satisfies both. |
| 3 | Run representative mods in isolated test instances; launch Minecraft and inspect screens | No Minecraft clients or heavy work on lunapc; in-client testing deferred | Deferred. Deep dives used docs, source, issue trackers and official screenshots only. |
| 4 | Profile before optimizing; repeatable measurements | No runtime work on lunapc this phase | Performance baseline deferred; baseline is static only. |
| 5 | Keep `CHANGELOG.md` Unreleased current as changes happen (AGENTS.md too) | No implementation yet | No user-visible change was made, so no changelog entry. Phase 2 must start Unreleased entries with the first change. |
| 6 | Persistent plan kept **in the repository** | Records in a **box working copy**; do not commit on lunapc | Records lived in `/workspace/stow-redesign/docs/` during phase 1; moved into the repository under `docs/` in phase 2. |
| 7 | Canonical descriptions must stay aligned | Nothing published | No description changes; the design notes which sections will need updates. |
| 8 | Request says `MODLOG.md` is untracked | `git status` on lunapc shows ` M MODLOG.md` (tracked and modified on `codex/stow-overhaul`) | Left alone either way. Note it before any commit or branch switch so it is not lost. |
| 9 | — | Palette "mineral gray, cream, restrained gold" | Same palette as the rejected `DESIGN-SYSTEM.md`. Consistent with the request ("redesign structure and controls, not just colors"): the new design keeps the palette and changes structure. |

## Prior evidence on lunapc: what is verified and where the gaps are

| Record (lunapc, uncommitted) | Claim | Verification status today |
| --- | --- | --- |
| `docs/OVERHAUL.md` | "the exact 1.0.6 JAR … has no Cloth Config metadata or class references" | **Contradicted.** Released JAR (SHA-256 `51fdeb3b…a04108f5`) declares `cloth-config >=26.3.159` and `StowSettingsScreen.class` references `me/shedaniel` 22 times. |
| `docs/research/2026-10-20/system-rethink/CANDIDATES.md` | 20-mod adoption table | Method sound; counts refreshed today (`2026-10-20-02-platform-survey.md`). Some CF numbers there match a cfwidget mirror that lags the official page (Mouse Tweaks: mirror 483,116,369 vs official 483,135,665). |
| `…/STOW-BASELINE.md` | Query lost on Find→Plan→Find | Observed by the earlier session **on the rejected artifact** only. For released 1.0.6, confirmed today from source (`MemoryScreenBase.navigate()` passes `""`). |
| `…/REFERENCE-LAB.md` | 8 reference JARs ran; Shulker Box Tooltip 5.4.2 crashed before title | Observed by the earlier session with hashes. Not re-run. Screens were mostly empty roots (the file says so). The crash is relevant: Stow's own shulker preview hooks the same tooltip path. |
| `…/WORKFLOW-COMPARISON.md`, `SYSTEM-RETHINK.md` | AE2 / Sophisticated Backpacks doc notes; "no click-count improvement claimed" | Documentary, consistent with what I read. No task timings exist. |
| `docs/evidence/2026-10-20/` | Freshness tooltip before/after PNGs | Valid for that tooltip; "after" is the rejected branch. |
| `docs/DESIGN-SYSTEM.md`, `SYSTEM-ARCHITECTURE.md` | Rejected design and its replacement plan (Storage + Projects workspaces, item catalogue, sessions) | Background only. Some ideas recur in v2 because independent evidence supports them (item-first storage, kept context). v2 differs in structure: one Stow screen with a universal query that replaces the palette, master/detail panes, inline project switching, one HUD arranger, native settings. |

Gaps carried forward: no populated runtime walkthrough of 1.0.6; no 1.0.6 performance
baseline; no coexistence matrix; no keyboard-only or narration check; the reference lab
did not exercise populated workflows.
