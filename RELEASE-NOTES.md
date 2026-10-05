# stow 0.5.11 — Oak stripping and companion harvesting

- Hold an axe, aim at a placed oak log and press **Alt + S** to strip nearby visible oak logs within normal reach. Press it again to cancel; the shortcut can be changed in the editor.
- Sneak is untouched. The optional **stow companion** adds server-side stripping followed by harvesting through the same Alt + S shortcut.
- Companion discovery follows connected oak logs through tall columns, long rows and dense piles, with no fixed height or length limit. The starting log must be visible and reachable. The group must be loaded and permitted; a configurable count limit defaults to 16,384 logs.
- The whole group is stripped before mining starts. Normal axe actions, break callbacks, durability and loot apply; logs already harvested by Timber are skipped. Exact compatibility with each Timber mod requires an in-game check.
- Without the companion, batches only strip up to 64 nearby visible logs, one server-confirmed use at a time, preserving log orientation and normal axe durability.
- Changing tools, opening a menu or starting another interaction cancels. Rejected or unconfirmed uses stop the batch without retries; the last durability point is protected.
- Added an Inventory settings toggle and a searchable world command-palette action, with English and German text.

Replace the old client JAR with `stow-0.5.11+mc26.3.jar`. For stripping **and mining**, install `stow-companion-0.5.11+mc26.3.jar` with Fabric API on the server. Full installation instructions, verification and limits are in [CHANGELOG.md](CHANGELOG.md).

# stow 0.5.10 — Automatic mining tools

- Added automatic hotbar tool selection when starting or continuing to mine. Stone picks a pickaxe, dirt a shovel and logs an axe; vanilla tool components, Efficiency and harvest requirements determine the choice.
- **Off by default**, including when upgrading. Enable it in Settings → Inventory → Auto-switch mining tools.
- **Alt + T** toggles it immediately, saves the choice and confirms on/off above the hotbar. Change the binding in the Shortcut editor, or search “auto tool” in the command palette.
- Hold Sneak to keep your chosen tool temporarily. Automatic selection leaves backpack items and slot contents in place. The existing manual middle-click tool picker remains independent.
- Creative pick block, open menus and unbreakable blocks retain their behavior.

Replace the old stow jar with `stow-0.5.10+mc26.3.jar`. Dependencies are unchanged. No server-side mod is needed.

# stow 0.5.9 — Quick bundles and focused equipment HUD

- Hold a bundle on the cursor and Shift-left-click or drag across stacks to fill it quickly. Filling respects capacity and preserves names/components.
- Shift-right-click or drag a held bundle across empty slots to unpack one entry per slot. The bundle stays on the cursor.
- Shift-left-drag from a placed bundle across empty or matching slots to unpack it. Shift-click without dragging still moves the whole bundle.
- Quick bundle gestures skip pinned slots, keep up with fast mouse movement, and can be disabled in Settings → Inventory → Quick bundle drag.
- Equipment HUD now shows only damageable hotbar items and worn armor. Backpack/offhand gear remains available in the equipment menu.
- Right-click a floating inventory material counter to show its source chest glow; right-click the same counter again to stop it. Hover help updates to match.
- Vanilla bulk Shift-crafting remains unchanged, including its pinned-slot exception. Existing data/settings upgrade without a reset.

Replace the old stow jar with `stow-0.5.9+mc26.3.jar`. Fabric API and Cloth Config remain required; Mod Menu is optional (21.0.0 supports Minecraft 26.3). No server-side mod is needed.
