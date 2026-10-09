# Planned features

Ideas for future stow releases. Nothing here is implemented or scheduled yet, and the designs may change before any work starts.

## One-key armor swap

**Status:** Proposed

Press one key to swap a worn armor piece with a chosen alternative from your inventory. Common pairs:

- Chestplate ↔ elytra
- Helmet ↔ a second helmet, for example one with Respiration or Aqua Affinity

### Setup

Pick the alternative for an armor slot from stow's Equipment screen, or from the inventory. Each armor slot can have one alternative. Setup is open for discussion; see the questions below.

### Behavior

- Each press swaps the worn piece with its alternative, so pressing again swaps back.
- The alternative moves into the armor slot. The worn piece moves into the inventory slot the alternative came from, so nothing is dropped or lost.
- If the alternative is missing, nothing changes and the action bar shows a short message.
- Swapping works the same in survival and creative.

### Rules

- The swap is refused while the cursor holds an item, to keep cursor state intact.
- A worn piece with the Curse of Binding cannot be removed. The swap is refused and the action bar says why.
- Swaps use the normal container click path, so the server and client stay in sync. No new network messages are needed.
- Item names, components and durability are kept as they are.

### Default control

**Alt + A**, editable in the Shortcut editor. Alt + A is not used by stow's defaults. Vanilla and other mods' bindings should be checked in-game, since the Shortcut editor flags conflicts with Minecraft's own keys.

### Open questions

1. Setup: the Equipment screen, or a key on the hovered inventory item?
2. Scope: one armor piece per press, or the whole configured set in one press?
3. Location: inventory screen only, or also in the world?
4. Should the offhand (for example a totem) be part of the same swap or a separate feature?
