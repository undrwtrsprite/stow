# Contributing

stow started as a mod for friends and was developed heavily with AI. Small, understandable changes are welcome. Please describe what you changed and how you checked it; if AI helped, say so plainly.

## Bugs

Use the bug-report form. Include your stow/Minecraft/Fabric versions, what you expected, what happened and steps to reproduce. For rendering or input problems, mention shaders and other inventory mods. Attach the relevant log or screenshot when useful.

## Changes

Build with Java 25 and the Gradle wrapper. Keep UI controls short, layouts usable at different GUI scales and English/German translation keys in sync. Preserve vanilla crafting and item counts/components. Add a focused regression case when changing item movement or tool selection; the existing harness is described in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

Keep upstream attribution and license files with adapted code and icons. Contributions are provided under the repository's Apache 2.0 license.
