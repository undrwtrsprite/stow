# stow release archive

This branch preserves all 18 original stow JARs available at import time, from 0.1.0+mc26.3 through 0.5.10+mc26.3. Each filename and embedded Fabric mod ID/version was checked, and SHA-256 checksums identify the unchanged binaries. Only files named stow are included; Mouse Wheelie binaries are not part of this archive.

Download individual builds from [GitHub Releases](https://github.com/undrwtrsprite/stow/releases), or use the files below. Releases are created at recovery time; their publication dates are not original development dates. All versions target Minecraft 26.3; use the dependencies recorded in each JAR. Install one stow version at a time.

## Source availability

- **0.4.0+mc26.3:** the original source ZIP is preserved in [sources](sources/stow-source-0.4.0+mc26.3.zip). Its 75 files are imported unchanged on [codex/recovered-source-0.4.0](https://github.com/undrwtrsprite/stow/tree/codex/recovered-source-0.4.0), tagged [v0.4.0+mc26.3](https://github.com/undrwtrsprite/stow/tree/v0.4.0%2Bmc26.3). The recovery commit records the import time. The original source still pins Loom 1.17.0; it is retained as received and has not been rebuilt or repaired during this import.
- **0.5.10+mc26.3:** existing [published source](https://github.com/undrwtrsprite/stow/tree/d23eb958b53521cb0cd13511d5a68a48ce6d93ef), tagged [v0.5.10+mc26.3](https://github.com/undrwtrsprite/stow/tree/v0.5.10%2Bmc26.3). That source includes the published Loom build configuration correction; the archived JAR is the user's original binary.
- **All other versions:** original source is currently unavailable. Tags beginning with **binary-v** point to this binary archive, not to source snapshots for those versions. GitHub's automatically generated source archives for those tags contain this archive tree.
- An earlier handoff mentioned a prepared 0.5.8 source bundle, but that package was not available in this checkout or Downloads. This import does not claim to have recovered it.

| Version | Original binary | Source availability |
| --- | --- | --- |
| 0.1.0+mc26.3 | [stow-0.1.0+mc26.3.jar](releases/stow-0.1.0+mc26.3.jar) | Unavailable; original JAR only |
| 0.2.0+mc26.3 | [stow-0.2.0+mc26.3.jar](releases/stow-0.2.0+mc26.3.jar) | Unavailable; original JAR only |
| 0.3.0+mc26.3 | [stow-0.3.0+mc26.3.jar](releases/stow-0.3.0+mc26.3.jar) | Unavailable; original JAR only |
| 0.3.1+mc26.3 | [stow-0.3.1+mc26.3.jar](releases/stow-0.3.1+mc26.3.jar) | Unavailable; original JAR only |
| 0.4.0+mc26.3 | [stow-0.4.0+mc26.3.jar](releases/stow-0.4.0+mc26.3.jar) | Original source ZIP and recovered source tag |
| 0.4.1+mc26.3 | [stow-0.4.1+mc26.3.jar](releases/stow-0.4.1+mc26.3.jar) | Unavailable; original JAR only |
| 0.4.2+mc26.3 | [stow-0.4.2+mc26.3.jar](releases/stow-0.4.2+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.0+mc26.3 | [stow-0.5.0+mc26.3.jar](releases/stow-0.5.0+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.1+mc26.3 | [stow-0.5.1+mc26.3.jar](releases/stow-0.5.1+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.2+mc26.3 | [stow-0.5.2+mc26.3.jar](releases/stow-0.5.2+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.3+mc26.3 | [stow-0.5.3+mc26.3.jar](releases/stow-0.5.3+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.4+mc26.3 | [stow-0.5.4+mc26.3.jar](releases/stow-0.5.4+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.5+mc26.3 | [stow-0.5.5+mc26.3.jar](releases/stow-0.5.5+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.6+mc26.3 | [stow-0.5.6+mc26.3.jar](releases/stow-0.5.6+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.7+mc26.3 | [stow-0.5.7+mc26.3.jar](releases/stow-0.5.7+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.8+mc26.3 | [stow-0.5.8+mc26.3.jar](releases/stow-0.5.8+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.9+mc26.3 | [stow-0.5.9+mc26.3.jar](releases/stow-0.5.9+mc26.3.jar) | Unavailable; original JAR only |
| 0.5.10+mc26.3 | [stow-0.5.10+mc26.3.jar](releases/stow-0.5.10+mc26.3.jar) | Published source at d23eb958b53521cb0cd13511d5a68a48ce6d93ef |

## Verification

[manifest.json](manifest.json) records mod versions, byte sizes, SHA-256 hashes, tags and source availability. [SHA256SUMS](SHA256SUMS) covers every JAR and the original source ZIP. Original binary contents are unchanged; no historical source was reconstructed from compiled classes.

Source ZIP SHA-256: bd2811a25428681a22bcad3fdadf428db3d3416ced0474338ae2b4f03a2c261b

## Origin and credits

stow began as a personal project for friends and their Minecraft server. Much of its code, UI and documentation was generated or edited with AI. The sorting system and earlier custom-port utilities derive from [Mouse Wheelie](https://github.com/Siphalor/mouse-wheelie), by Siphalor and contributors. Apache 2.0 licensing and attribution are preserved in the original files and this branch's [LICENSE](LICENSE) and [NOTICE](NOTICE). For current features, development and full credits, see [main](https://github.com/undrwtrsprite/stow).
