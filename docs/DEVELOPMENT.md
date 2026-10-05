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
