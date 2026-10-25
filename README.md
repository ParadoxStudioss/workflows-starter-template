# Create: Freight & Logistics 0.1.2

Minecraft 1.21.1 / NeoForge 21.1.248 / official Create 6.0.10 / Java 21.

**Playable experimental build, not a completed production mod. Test in a new world with backups.**

## Working features
- Nine Create-styled, rotatable control machines with 3D geometric models, plus power-responsive railway crossings, barriers and yard lamps.
- Management GUIs opened by right-click with server-synchronized credits, contract states, order pages, marketplace purchases and depot registration.
- Server Rack has a real persistent five-slot component inventory (CPU, RAM, storage, networking and cooling), including component recovery when broken.
- Persistent companies, credits, freight contracts, depot position and checked item-based contract payouts.
- Freight contract payment consumes real items from the player's inventory; a completed contract cannot pay twice.
- Marketplace purchases check company balance and available inventory.
- Small isolated Create-track railway stops can generate in new plains, snowy plains and savanna areas.
- Operator `/freight railgen` command creates a 31-block Create track test segment.
- Original 16px item textures and redesigned multi-part block model geometry.

## What is NOT yet functioning
- World-spanning connected generated railway network, stations with schedules, bridges or tunnels
- NPC-operated trains, Blaze Burner conductors, automatic dispatch and delivered locomotive purchases
- 3x3 station chunk loading or cargo loading via Create interfaces
- Server hardware controlling actual automation throughput
- Factory production, depot automatic parking, dedicated warehouse inventory management
- Create Display Link pagination/data provider integration
- Train detection and automatic road-crossing protection (the barriers currently respond to redstone only)

## Test
1. Use Java 21, Minecraft 1.21.1, NeoForge 21.1.x and official Create 6.0.10.
2. Put the compiled JAR into your `mods` folder. Both client and server need it for multiplayer.
3. In a fresh creative test world place the new blocks and right-click the management computer or marketplace.
4. `/freight company create Paradox Freight`
5. Use marketplace buttons to purchase component parts and put them in the Server Rack slots.
6. Operator: `/freight issue minecraft:iron_ingot 32 1200`
7. Accept the order on the computer; hold 32 iron ingots in inventory, then click DELIVER.
8. Place a redstone lever beside a crossing barrier or yard lamp and check the state changes.
9. Operator: `/freight railgen` in an empty area to construct a real Create track test section.
10. Check fresh plains/savanna/snowy plains chunks for rare world-generated short railway stops.

## Commands
`/freight help`, `/freight company create <name>`, `/freight balance`,
`/freight contracts [page]`, `/freight accept <id>`, `/freight deliver`,
`/freight depot set`, `/freight shop`, `/freight shop buy <part>`.
Operators: `/freight issue <item> <count> <credits>`, `/freight railgen`.

## Build and quality
GitHub Actions compiles with Gradle 8.14 + Java 21 and checks that the JAR contains
GUI classes, model definitions and world-generation JSON. Source is isolated on
branch `create-freight-logistics-v0.1.2`. No live Minecraft client/server
integration test has yet been completed. This is not ready for valuable survival worlds.

## Compatibility
Only official Create is required as a gameplay mod. No third-party railway addons are needed.
