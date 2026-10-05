## 0.5.11+mc26.3 — Oak stripping and companion harvesting

- Added editable **Alt + S** oak stripping. Hold an axe and aim at an unstripped oak log; press the shortcut again to cancel. Sneak is unchanged.
- Added the optional **stow companion** for Fabric servers. The same shortcut strips every log in the connected oak group, then harvests it, following tall columns, long rows and dense piles without a fixed height or length cutoff.
- The starting log must be visible and reachable. Groups must be loaded and permitted. The default configurable server limit is 16,384 logs.
- Small groups can finish in one server tick; larger groups are processed in batches. Normal axe durability, survival loot and creative behavior apply.
- The whole group is stripped before mining starts; logs already removed by Timber are skipped. Other wood, leaves, oak wood blocks and separate groups are excluded.
- Without the companion, the client only strips nearby visible oak logs and does not mine them.
- Added settings, command-palette support and English/German messages.
- Builds, metadata and automated headless server checks passed. The author confirmed the combined build works in-game.

**Players:** update to the stow client JAR; Fabric API and Cloth Config are required. **Server:** install the companion JAR with Fabric API to enable connected harvesting. For single-player, install both JARs on the client. Both target Minecraft 26.3, Fabric Loader 0.19.5+ and Java 25+.
