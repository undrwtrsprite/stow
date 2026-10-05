# Modrinth and CurseForge upload package

Use these same two JARs on **both** sites. They are two distinct mods: keep the stow client on its existing project, and publish the companion as a separate **stow companion** project. This keeps each project's normal download and dependency list clear. Modrinth generally recommends one normal download per version; see its [additional files guidance](https://support.modrinth.com/en/articles/8793363-additional-files).

| Project | Upload file | Required dependencies | Summary |
| --- | --- | --- | --- |
| stow | `stow-0.5.11+mc26.3.jar` | Fabric API, Cloth Config | Inventory sorting, chest memory and material tracking, with optional oak harvesting through stow companion. |
| stow companion | `stow-companion-0.5.11+mc26.3.jar` | Fabric API | Optional server support for stripping and harvesting connected oak logs with stow's Alt+S shortcut. |

- Version number: **0.5.11+mc26.3**
- Minecraft version: **26.3**
- Loader: **Fabric** (not Forge or NeoForge)
- Release type: **Release**
- Java: **25+**; Fabric Loader: **0.19.5+**
- License: **Apache-2.0**
- Source: <https://github.com/undrwtrsprite/stow>
- Issues: <https://github.com/undrwtrsprite/stow/issues>
- Client project: client mod; inventory features do not require a server installation. Mod Menu is an optional dependency.
- Companion project: runs on the server; optional on the client for an integrated single-player server. stow is needed on clients that use its shortcut, not on the dedicated server.

For CurseForge, tag each file with its correct Minecraft version and Fabric loader. This lets the launcher select compatible files; see [CurseForge file fields](https://support.curseforge.com/support/solutions/articles/9000197242).

Paste [STOW-DESCRIPTION.md](STOW-DESCRIPTION.md) into the main project's description and [COMPANION-DESCRIPTION.md](COMPANION-DESCRIPTION.md) into the companion's description. Use [VERSION-CHANGELOG.md](VERSION-CHANGELOG.md) as the release changelog for both. Add your companion project link to the main description after creating it if desired; the GitHub download links work independently.

Upload the `.jar` files themselves. The source JAR, checksum text file and package ZIP are not the mod downloads. No site-specific rebuild is needed.
