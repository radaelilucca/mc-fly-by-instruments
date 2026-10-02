# Aeronautics: Fly by Instruments

NeoForge 1.21.1 addon for Create Aeronautics. The current prototype includes a
thin Modular Table Base, Avionics Cable Network, Network Bridge, and Radar
Altimeter.
It has the same 3-pixel thickness as a Copycat Panel and mounts against the
clicked face, including on top of other blocks. Find it in the Aeronautics: Fly
by Instruments creative tab or use `/give @s aeronautics_fly_by_instruments:modular_table_base`.

Each table now has a 3×3 grid. Hold a Lamp Module and right-click the exposed
surface to install it in the highlighted cell. Green indicates an available
footprint; red indicates a collision or a footprint outside the grid. The 2×1
Lamp Module rotates between horizontal and vertical with the mouse wheel while
aiming at the table. Hold the Create Wrench and right-click an installed module
to retrieve it. Tables can be mounted on any sturdy face, including ceilings.
Installed modules persist when the world is saved and are returned when the
table is broken. Lamps still have placeholder appearance and no lighting or
alert behavior.

Connect Avionics Cables to build a data network and attach a Network Bridge to
query its topology from CC:Tweaked. The Radar Altimeter is an independent
peripheral that reports clearance from its lower face using multi-ray sampling.
Use `/give @s aeronautics_fly_by_instruments:avionics_cable`,
`/give @s aeronautics_fly_by_instruments:network_bridge`, and
`/give @s aeronautics_fly_by_instruments:radar_altimeter`.

Find both Lamp Modules in the same creative tab, or use
`/give @s aeronautics_fly_by_instruments:lamp_module`.
Use `/give @s aeronautics_fly_by_instruments:lamp_module_2x1` for the two-cell module.

Development and CC:Tweaked API documentation: [docs/README.md](docs/README.md).
Future instruments and alerts are defined in `PRD.md`.

Place a Terrain Scanner facing the direction you want to sample: the standard
version sweeps a fixed forward cone, while the Advanced version lets you adjust
its total opening from 30° to 180° in-game. Both report obstacle contacts to
CC:Tweaked and can connect to the Avionics Cable Network. Use
`/give @s aeronautics_fly_by_instruments:terrain_scanner` or
`/give @s aeronautics_fly_by_instruments:terrain_scanner_advanced`.

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
