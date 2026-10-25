# Create: Freight & Logistics — functional foundation (0.1.0-alpha)

Minecraft 1.21.1 / NeoForge 21.1.x / official Create 6.0.10 / Java 21.

**Status:** Early functional foundation, NOT the finished railway simulation. No custom GUI, world generation, AI trains, cargo train loading, chunk-forcing, Create Display Link integration or autonomous train delivery yet. Do not install in a production world without backups.

## Working in-game
* Creative tab with 10 placeable industrial, railway and management blocks and 8 parts.
* Right-click management blocks for context-aware assistance.
* Redstone-powered crossing light model that changes state and emits light.
* Persistent companies, per-company credits and home-depot position in SavedData.
* Persistent freight contracts with pagination (1–10), accept and deliver flows.
* Delivery consumes **real** items from player inventory, credits payment exactly once.
* Shop supports purchasing hardware/vouchers with credits (prevents charging when inventory full).
* Admin-only contract issuance.
* Basic crafting recipes for core hardware.
* Mod metadata requires official Create. No other gameplay mod required.

## Test commands
`/freight help`
`/freight company create Paradox Freight`
`/freight balance`
`/freight depot set`
`/freight shop`
`/freight shop buy industrial_cpu`
`/freight contracts 1`
Admin: `/freight issue minecraft:iron_ingot 32 1200`
`/freight accept 1` and `/freight deliver` when carrying 32 iron ingots.

## Build
Run `gradle build` under JDK 21 with Gradle 8.14+ and internet to download NeoForge.
JAR appears under `build/libs/`.

## Roadmap (not implemented)
1. Registered train stations, Create schedules, animated level crossing barrier.
2. 3×3 on-demand station loading and cargo interfaces.
3. Generation of railway networks, stations, trains and era presets.
4. Train marketplace physical deliveries, passenger trains and Blaze conductors.
5. Factory automation, component-based server racks and full management GUI.
6. Native Create Display Link source with 1–10 page selector and relays.

This branch is isolated from the owner's existing project on main.
