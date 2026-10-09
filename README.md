<img src="src/main/resources/assets/stow/icon.png" alt="stow icon" width="80" height="80">

# stow

A quiet home for your items. Inventory sorting, chest memory and material tracking for **Minecraft 26.3 · Fabric**, with an optional server companion for log stripping and harvesting.

[Downloads](https://github.com/undrwtrsprite/stow/releases) · [Builds](https://github.com/undrwtrsprite/stow/actions/workflows/build.yml) · [Report a bug](https://github.com/undrwtrsprite/stow/issues) · [All features](docs/FEATURES.md) · [Changelog](CHANGELOG.md) · [Older release notes](RELEASE-NOTES.md) · [Version archive](docs/HISTORY.md)

## Stow 1.0: your build starts with a list

Copy a materials list from a YouTube description, paste it into **Materials → Import**, review the items, and start gathering. Export your saved goals as plain text to share a plan or reuse it in another project. No schematic mod is required.

This release also brings **more materials on screen**, Minecraft item icons, centered menu tabs, the familiar pixel priority star, helpful empty states, and smooth import-list scrolling. Set each HUD's alignment, position and size; see **+N more materials**, count equipment durability down, and replace worn main-hand tools before they break or just after. Brief action feedback stays readable above open menus.

[Download on Modrinth](https://modrinth.com/mod/stow) · [Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/stow) · [What's new in 1.0](CHANGELOG.md)

## Why it exists

I started stow for my friends and our Minecraft server. We wanted an inventory mod that worked the way we liked, so I used AI to build and change it around our ideas.

**Much of stow's code, UI and documentation was generated or edited with AI.** I choose the features and test it, but this is a personal project, not a professionally developed mod. It will have rough edges. The source is public so people can see how it works, report problems and improve it.

The sorting system is adapted from [Mouse Wheelie](https://github.com/Siphalor/mouse-wheelie) by Siphalor and contributors. Their work is credited in [NOTICE](NOTICE); this project retains the Apache 2.0 license.

## What it does

- **Sort and move items:** Mouse Wheelie sorting, five sort orders, matching/all-item drag modes, inventory search and pinned slots.
- **Remember storage:** search previously opened chests, barrels and shulker boxes; rename them and highlight sources through walls.
- **Plan builds:** project-specific material goals and selected chests, live inventory counts, progress colors and configurable floating HUD counters.
- **Deposit and refill:** immediate smart deposit with keep amounts, building stack refill and a remaining-block count beside the hotbar. Set “Refill at this many items” to **0** to refill after the held stack runs out.
- **Use tools:** middle-click a block to pick a suitable tool. Optional automatic hotbar tool switching while mining is **off by default**; toggle it with **Alt + T**.
- **Strip and harvest logs:** hold an axe, aim at a placed log and press **Alt + S**. All vanilla log types and Nether stems are supported. With the optional stow companion on the server, it strips and harvests the connected group of the same type through tall columns, long rows and dense piles. Without the companion it only strips nearby visible logs. Press again to cancel; Sneak is unchanged.
- **Move bundle contents:** Shift-drag to fill a held bundle or unpack entries quickly.
- **Keep track of equipment:** durability for hotbar tools and worn armor, with configurable HUD placement.
- **Make it yours:** editable shortcuts, a searchable command palette, HUD previews and Cloth Config settings.

Chest memory uses snapshots of storage you have opened. It cannot read unopened chests or know what someone else changed until you reopen them. Material goals include your inventory by default; settings can count selected storage only.

## Update notices

Starting with **0.5.12**, stow checks for newer stable Fabric releases for your Minecraft version on **GitHub, Modrinth and CurseForge**. A clickable in-game chat notice opens the release page. Each new version is announced once, and checks run in the background while you play. Successful checks repeat after 12 hours; unavailable services retry after an hour.

The check requests public release metadata from GitHub, Modrinth and [CFWidget](https://cfwidget.com/) for CurseForge. It sends no player, world, server or account data. Listings and files awaiting platform approval become detectable after approval; CFWidget can cache CurseForge data for up to an hour.

## Install

Requires Java **25**, Fabric Loader **0.19.5+**, Fabric API **0.161.0+26.3** and Cloth Config **26.3.159+** for Fabric. Mod Menu **21.0.0** is optional for opening settings.

Put `stow-1.0.0+mc26.3.jar` in your client's `mods` folder. **Remove Mouse Wheelie first**; the two mods are incompatible. Your server does not need stow.

To enable Alt + S stripping **and mining**, install `stow-companion-1.0.0+mc26.3.jar` plus Fabric API in the server's `mods` folder. The companion needs Java 25 and Fabric Loader 0.19.5+, but no Cloth Config or client stow installation. For single-player, install both JARs on the client. The starting log must be reachable, and the group must be loaded and permitted. There is no fixed group height or length limit; a configurable count limit defaults to 16,384 logs. See [the changelog](CHANGELOG.md) for durability, Timber behavior and other limits.

### Previous GitHub downloads — 0.5.13+mc26.3

| File | Install location | Required dependencies |
| --- | --- | --- |
| [stow client JAR](https://github.com/undrwtrsprite/stow/releases/download/v0.5.13%2Bmc26.3/stow-0.5.13%2Bmc26.3.jar) | Player's client | Fabric API, Cloth Config |
| [stow companion JAR](https://github.com/undrwtrsprite/stow/releases/download/v0.5.13%2Bmc26.3/stow-companion-0.5.13%2Bmc26.3.jar) | Server, or client for single-player | Fabric API |

These previous downloads are in the [0.5.13 release](https://github.com/undrwtrsprite/stow/releases/tag/v0.5.13%2Bmc26.3), with checksums and source. The companion is a separate mod; install the client JAR to use stow's inventory features.

The [version archive](docs/HISTORY.md) preserves the 18 earlier stow builds, from 0.1.0 through 0.5.10, with checksums and source-availability notes.

The source ZIP is for developers. CI-built JARs are also available under **Artifacts** on a successful [build run](https://github.com/undrwtrsprite/stow/actions/workflows/build.yml), or you can build it yourself. GitHub artifact downloads require signing in.

## Quick controls

| Action | Default control |
| --- | --- |
| Open command palette | Ctrl + K |
| Sort inventory / quantity / name | Middle-click / Shift + middle-click / Ctrl + middle-click |
| Move matching stacks | Shift + left-drag |
| Move all crossed stacks in Both mode | Ctrl + Shift + left-drag |
| Pin a slot | Hover and press P |
| Track a material | Hover and press N, or `/need 1000 cobble` |
| Prioritize a material on the HUD | Hover its floating counter and press H |
| Find / stop a material's chest glow | Right-click its floating counter |
| Toggle automatic mining tools | Alt + T; starts off |
| Strip logs / harvest with companion / cancel | Alt + S; hold an axe and aim at a log |
| Keep the current tool temporarily | Hold Sneak while mining |
| Fill / unpack a held bundle | Shift + left-drag / Shift + right-drag |

## Build from source

Install a **JDK 25** and check `java -version`. The Gradle wrapper downloads the pinned Gradle version and the project dependencies; you do not need a separate Gradle installation.

**Windows PowerShell**, from the repository folder:

```powershell
.\gradlew.bat build
```

**Linux / macOS:**

```sh
./gradlew build
```

The client, companion and source JARs appear in `build/libs/`. `./gradlew runClient` launches the development client; use `.\gradlew.bat runClient` on Windows.

[Development](docs/DEVELOPMENT.md) explains the project structure and dependency versions. [CONTRIBUTING.md](CONTRIBUTING.md) covers bug reports and changes.

## Credits and license

- **Mouse Wheelie:** original sorting code and earlier custom-port utilities, Siphalor and contributors, Apache 2.0.
- **Material Design Icons:** Pictogrammers and contributors; original selected SVGs, their license and rasterized PNGs are included.
- **Cloth Config, Fabric and Mod Menu:** separate dependencies, not bundled.
- **stow icon:** made with AI for this project.

stow is licensed under [Apache 2.0](LICENSE). See [NOTICE](NOTICE) for attribution. Minecraft and its assets are not included in this repository. This is an independent project, not affiliated with Mojang or Microsoft.
