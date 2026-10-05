# Development

## Standard build

Use Java 25 and the included wrapper. On Linux/macOS, run `chmod +x gradlew` once if the wrapper has no executable permission (for example, after a first commit made on Windows). Run `./gradlew build` on Linux/macOS or `.\gradlew.bat build` in Windows PowerShell. Import the repository root as a Gradle project in your IDE. `runClient` opens the development client.

Versions are pinned in `build.gradle` and `gradle/wrapper/gradle-wrapper.properties`: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Cloth Config 26.3.159, optional Mod Menu 21.0.0, Loom 1.17.21 and Gradle 9.6.0. Minecraft 26.3 uses unobfuscated names, so the plugin is `net.fabricmc.fabric-loom`; there is no mappings dependency.

When changing the release version, update `build.gradle`, `fabric.mod.json`, README and release notes together. Run `python3 dev-tools/check-project.py` to check metadata, translation keys, referenced assets and the wrapper layout.

## Source layout

| Path | Purpose |
| --- | --- |
| `src/main/java/dev/stow/` | Mod configuration, client code and mixins |
| `src/main/resources/` | Fabric metadata, English/German text and runtime assets |
| `design/icons/mdi/` | Original third-party SVG icons and license |
| `tests/porttest/` | Minecraft client regression harness and integrated-world tests |
| `dev-tools/` | Source checks, prepared-workspace builds, test launcher and icon conversion |
| `docs/FEATURES.md` | Detailed behavior and controls |
| `CHANGELOG.md` | Release changes and verification |

Keep names/components, pinned slots, cursor state and client/server synchronization intact when changing item transactions. Automatic mining uses hotbar selection only; its default and reset value are off. Manual middle-click tool picking can retrieve backpack tools independently.

## GitHub Actions

The **Build** workflow runs source checks and the Gradle build on Windows and Linux for pushes and pull requests. Each successful job uploads the mod and source JARs. It has read-only repository permissions and does not publish a release or upload anything to Modrinth.

The **Minecraft regression tests** workflow can be started manually under Actions. Choose behavior, automatic mining, oak stripping, crafting, bundles/glow or native outlines. It downloads dependencies from their upstream sources and launches an offscreen Fabric client on Linux. These tests are independent of Gradle's ordinary `test` task; a successful Gradle build alone does not mean the Minecraft behavior checks ran.

## Optional local regression harness

This is a **Linux-only** developer tool. Clone this repository into a directory named `stow`, because the prepared-workspace scripts expect that name. Python 3, internet access and the usual Linux OpenGL/OpenAL runtime libraries are needed. It downloads a Java 25 toolchain and verified upstream dependencies into sibling `toolchain/` and `manual-build/` directories. No Minecraft JARs or dependency JARs are committed.

From the repository root:

```sh
python3 dev-tools/bootstrap-local.py
python3 dev-tools/build-local.py
STOW_TEST_SCREENSHOTS=0 python3 dev-tools/test-local.py
STOW_AUTO_TOOL_WORLD_TEST=1 STOW_TEST_SCREENSHOTS=0 python3 dev-tools/test-local.py
```

Other world runs use `STOW_STRIP_WORLD_TEST=1`, `STOW_CRAFTING_WORLD_TEST=1`, `STOW_BUNDLE_WORLD_TEST=1` or `STOW_NATIVE_WORLD_TEST=1`. Use only one world flag per run. The stripping suite uses a real integrated server, including a test-only interaction rejection hook. Logs and result files are written into unique sibling `stow-tests/game-v03-*` directories. Without the screenshot flag, the behavior run also captures the UI.

The optional server companion is built separately as `stow-companion-VERSION.jar`; the client JAR excludes its server classes. Its shared packet codecs have no client imports. To run headless companion checks on Windows or Linux, set `JAVA_HOME` to JDK 25, run `gradlew prepareHarvestTestClasspath build`, then `python dev-tools/test-harvest-server.py`. This launches a disposable Fabric GameTest world under `build/harvest-server-*`, using only the packaged companion, Fabric API and a test fixture. It checks real axe use, vanilla drops and durability, tall/long/dense groups, Fabric veto callbacks, a simulated Timber cascade, cancellation and invalid starting conditions. Test fixtures never ship in either mod JAR. `ConnectedLogsTest.java` separately exercises 2,000-log rows, 384-log columns, caps and unloaded boundaries without a game launch. Gradle's ordinary `test` task does not run these suites.

`build-local.py` is a direct javac fallback for this harness, not the public release pipeline. Use the Gradle workflow for normal builds. The harness uses controlled item prototypes for menu-only cases and real vanilla item components for integrated-world cases. Remote multiplayer and live shader packs require separate manual checks.

## Upstream build references

- [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html)
- [Fabric Loom documentation](https://docs.fabricmc.net/develop/loom/)
- [Gradle setup action](https://github.com/gradle/actions)
