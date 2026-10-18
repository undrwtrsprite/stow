# Stow 1.0.4 for Minecraft 26.3

Stow 1.0.4 combines inventory organization, storage memory, build planning and tool helpers with whole-inventory villager trading, optional instant trades with the server companion, automatic attack, shulker previews and inventory-wide Mending repair.

## Highlights

- **Trade from the whole inventory:** Shift + left-click a villager output to continue the selected offer, drawing payments from the inventory and hotbar. The refill follow-up tops up both payment slots in place, including partial stacks and two-item costs. Trading stops at missing payment, sold-out stock, full result space, changed offer/input or no progress. Client batches are paced; with the matching companion they complete in one bounded server operation.
- **Instant companion trades:** update both client and server companion. The server validates the open vanilla merchant menu and performs the trades with normal merchant rules. Each request is capped at 128 batches and rate-limited. Older companions use paced client trading. Custom merchant menus use the client path. Completed instant trades cannot be cancelled afterward.
- **Automatic attack:** Alt + A toggles Hold Attack or Interval clicks. Interval is configurable from 50 to 60000 ms (500 ms default). Activation stops on menus, focus loss, pause, death, spectator mode or disconnect.
- **Shulker previews:** view a 9 × 3 grid of the box's 27 stored positions in the tooltip. The grid uses item data already on the box and does not reveal server-hidden contents.
- **Auto-mending:** Alt + M cycles damaged Mending items through the offhand as normal XP repairs them, returning each item and restoring the prior offhand when safe.
- **Compact tooltips:** shorter English and German setting summaries; shulker previews replace the duplicate written contents list while retaining the name and lore.
- Existing features include five inventory sort orders, Shift-drag transfers, pinned slots, bundles, remembered storage, material projects, block/rocket refill, tool selection/replacement, equipment HUDs, fast companion chest sorting and connected-log harvesting.

## Install

For Minecraft 26.3, use Java 25+, Fabric Loader 0.19.5+, Fabric API 0.161.0+26.3 and Cloth Config 26.3.159+ on the client. Mod Menu 21.0.0 is optional. Replace previous Stow client and companion JARs; remove Mouse Wheelie, which conflicts with Stow. For companion features in single-player, install both JARs locally. The companion requires Fabric API and runs on the server.

Existing Stow projects, settings and companion properties are retained. The new instant-trading companion option defaults to enabled; set `instantVillagerTrading=false` in `config/stow-companion.properties` and restart to disable acceleration.

## Validation

The 1.0.4 villager payment-refill follow-up was compiled. At the user's request, no automated tests or in-game checks were run for this follow-up; its reported behavior awaits user validation. Earlier 1.0.4 feature checks are documented in the changelog and development notes. Individual modpack, server restriction, merchant and protection-mod compatibility is not claimed.
