# Flight Safety Instruments

NeoForge 1.21.1 addon for Create Aeronautics. The first playable prototype is a
thin Modular Table Base, using the vanilla iron block texture as a placeholder.
It has the same 3-pixel thickness as a Copycat Panel and mounts against the
clicked face, including on top of other blocks. Find it in the Functional
Blocks creative tab or use `/give @s flightsafetyinstruments:modular_table_base`.

Each table now has a 3×3 grid. Hold a Lamp Module and right-click the exposed
surface to install it in the highlighted cell. Green indicates an available
cell; red indicates an occupied cell. Sneak-right-click with both hands empty
to retrieve a module. Installed modules persist when the world is saved and
are returned when the table is broken. The lamp currently has placeholder
appearance and no lighting or alert behavior.

Find the Lamp Module in the same creative tab, or use
`/give @s flightsafetyinstruments:lamp_module`.

Development and CC:Tweaked API documentation: [docs/README.md](docs/README.md).
Future instruments and alerts are defined in `PRD.md`.

## Development baseline

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.236
- Create 6.0.10+
- Create Aeronautics bundled 1.3+
- Sable 2.0.5
- CC:Tweaked 1.120.0+

The Gradle properties default to the Create, Create Aeronautics, and Sable JARs
used by the sibling `sable-horses` workspace. The CC:Tweaked 1.120.0 JAR is
staged at `libs/cc-tweaked-1.21.1-neoforge-1.120.0.jar` and can be overridden
with `-Pcomputercraft_jar=<path>` if needed.

Aeronautics and Sable are supplied to Gradle as local development dependencies
and are not bundled into the addon JAR. Their nested JARs are extracted to
`build/local-mod-dependencies/` for compile-time symbols only.

## Development commands

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

VS Code tasks for both commands are in `.vscode/tasks.json`.
