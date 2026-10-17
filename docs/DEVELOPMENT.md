# Development

## Build

Use JDK 25 and the included Gradle wrapper. Run `./gradlew build` on Linux/macOS or `.\gradlew.bat build` in Windows PowerShell. Import the repository root as a Gradle project in your IDE; `runClient` opens the development client.

The pinned versions are Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Cloth Config 26.3.159, optional Mod Menu 21.0.0, Loom 1.17.21 and Gradle 9.6.0. Minecraft 26.3 uses unobfuscated names, so there is no mappings dependency.

When changing the release version, update `build.gradle`, both Fabric metadata files, README and release notes together. `python3 dev-tools/check-project.py` checks metadata, translation keys, assets and the wrapper layout.

## Source layout

| Path | Purpose |
| --- | --- |
| `src/main/java/dev/stow/` | Mod configuration, client code, mixins and companion server code |
| `src/main/resources/` | Client metadata, English/German text and runtime assets |
| `server-resources/` | Companion metadata |
| `design/icons/mdi/` | Original third-party SVG icons and license |
| `dev-tools/` | Build preparation, source checks and icon conversion |
| `docs/FEATURES.md` | Detailed behavior and controls |
| `CHANGELOG.md` | Release changes |

The client JAR excludes companion server classes. The companion JAR contains only the shared protocol and companion server code. Local notes, publishing material and regression suites are ignored and excluded from the public repository and release packages.

Keep item names/components, pinned slots, cursor state and client/server synchronization intact when changing item transactions. Automatic mining uses hotbar selection only and defaults to off.

## Release validation

A completed build does not establish in-game compatibility. Before treating a release as ready, record manual client and companion checks here and resolve remaining compatibility items. For 1.0, game-specific checks remain for server damage applied to an item already in use and custom modded durability costs, as noted in `CHANGELOG.md`.

### Finish/Reopen development checks — 9 October 2026

The unreleased client changes were built with JDK 25 as `1.0.2-test.1+mc26.3`, without changing the release metadata in source or publishing the artifacts. The companion implementation is unchanged.

- Full Gradle build and project metadata/English-German translation checks passed. Gradle has no standard test sources; the following results come from separate local regression probes.
- An isolated Minecraft 26.3 Fabric client passed 37 Finish/Reopen assertions: nonempty projects and target eligibility, neutral finished colors, Undo, project isolation, saving/reloading, migration from files without the finished field, preserved goals/selections/priorities, HUD visibility and setting persistence, project-switch and click-time eligibility, and footer widget geometry at 320/360/480/720 GUI pixels.
- Retained importer/project regression checks passed all 47 assertions; export checks passed all 6 assertions.
- A fresh Minecraft 26.3 Fabric GameTest server passed the companion harvest fixture (and vanilla smoke test). The fixture covers tall columns, long rows, dense piles, stripped drops, durability, all 12 vanilla log/stem types and axes, species isolation, Fabric use/break vetoes, insufficient durability, cancellation, changed tools, unreachable seeds, packet codecs, count caps, changed snapshots, creative behavior and a simulated Timber callback cascade without duplicate drops. This is not a test against the user's actual Timber mod/server installation.
- Local evidence is retained in ignored build output: `build/finish-project-probe/finish-result.txt`, `build/finish-import-checks.log`, `build/finish-export-checks.log` and `build/finish-harvest-probe/results.xml`. The local feature probe source is `build/FinishProjectProbe.java`.

Live gameplay/visual checks were subsequently performed by operating a separate Minecraft 26.3 client in a fresh survival world, with the built test client and normal dependencies (no feature probe mod). At GUI scale 4 in a 1280×960 game viewport, the Materials footer rendered without overlaps. A 64-stone target with 64 stones enabled Finish; finishing changed the counts and bar to neutral blue. Removing 32 stones through a local-world command updated the finished menu and inventory HUD to 32/64 and showed "32 used". Reopen restored warning colors and disabled Finish below target; Undo restored the finished state. Saving, leaving and rejoining preserved the finished state and HUD priority. Placing a stone through normal right-click then reduced the count to 31/64, showed "33 used" and retained the neutral blue HUD/menu. No additional issue was found in this live check. The isolated world/configuration and game logs are under ignored `build/finish-gameplay-probe/`; user saves were not used.

Remaining compatibility validation before declaring the release ready: project switching, remembered chest updates and reconnect behavior on the actual multiplayer modpack/server, including the user's Timber installation. Those were not exercised by the local singleplayer session; automated project-switch, storage-fixture and companion tests are recorded separately above.

### 1.0.2 release checks — 17 October 2026

- JDK 25 Gradle `build` passed for client, companion and source artifacts. The project checker passed 396 English/German translation-key pairs, metadata, licenses, wrapper layout and client/server artifact separation.
- Minecraft 26.3 with Fabric Loader 0.19.5 loaded Stow 1.0.2 in the isolated development run. The available game-control backend did not enumerate a controllable window, so the villager result-slot drag and direct Shift-click behavior were not verified interactively. Treat those as code-reviewed and compiled, not as live-game validated.
- The companion source is unchanged. The actual multiplayer modpack/server, remembered-chest reconnect behavior and Timber installation remain unverified; this build makes no compatibility claim for that configuration.

## GitHub Actions

The Build workflow checks project metadata and builds on Windows and Linux. It uploads client, companion and source JARs as build artifacts. It has read-only repository permissions.

## Version checks

`VersionChecker` runs network work on daemon threads and adds the chat notice on the client tick thread. It checks published stable client releases for the running Minecraft version. GitHub releases must include the matching client JAR. Modrinth versions must list the matching Minecraft version and Fabric. CurseForge uses CFWidget's public file metadata, filtered by release type, Fabric and Minecraft version.

The request URLs are fixed public endpoints. Download links are constructed for the three official project pages. No credentials or game data are sent. `lastNotifiedVersion` is saved only in the player's local settings so reconnecting or restarting does not repeat the same notice.

## Upstream build references

- [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html)
- [Fabric Loom documentation](https://docs.fabricmc.net/develop/loom/)
- [Gradle setup action](https://github.com/gradle/actions)
- [CFWidget API](https://cfwidget.com/)
