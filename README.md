# Create: Freight & Logistics 0.1.3 — Integration Preview

Minecraft **1.21.1** / NeoForge **21.1.x** / official Create **6.0.10** / Java 21.

**Development build.** Back up worlds before testing; it is not yet the complete automatic railway economy discussed in our roadmap.

## Functional features added in 0.1.3

- Real Create API integration, compiled against Create 6.0.10-280 and bundled Ponder 1.0.82 API (no other gameplay mod required besides Create).
- **Depot auto return:** place a Create Train Station, name it, then register it with the Depot Controller or \`/freight depot set\`. Trains owned by your Minecraft player and with a Create conductor may receive a native Create schedule to return home after their other schedule completes and they become idle. Use \`/freight depot toggle\` to turn this feature off. This is automatic **return to the registered station**, not full siding allocation.
- **Temporary station loading:** approaching Create trains cause a 3x3 chunk area around their destination Create Train Station to tick. Global limit: four simultaneous stations. Tickets expire after 30 seconds without renewed approach; persisted tickets are invalidated on restart.
- **Warehouse Controller:** genuine 27-slot persistent physical item inventory; own GUI and inventory restoration when the block is removed.
- **Server Rack:** five component categories with persistent slot inventory; separate hardware screen.
- **Specialized screens:** company overview/orders, shop, depot, factory monitoring, signal information and display relay, with separate actions and layouts.
- **Create Display Links:** actual Create DisplaySource registered on management/industry blocks. Orders display on 1-10 selectable pages; rack reports hardware counts and warehouse reports storage totals.
- **Create Ponder:** seven in-game tutorials generated with real structure NBT under assets/freightlogistics/ponder. Hover supported blocks and hold W to view their scenes.
- **Appearance:** replaced nine generic cube models with detailed individual block geometry. Server rack features 45 separate voxel elements: drive trays, vents, rails, copper components and status lamps.

Retains 0.1.2 features: persistent companies, contract purchasing/payment and inventory validation, redstone crossings and lamps, experimental disconnected world-generated short railway stops, and \`/freight railgen\`.

## Critical limitations — not complete

- **World-wide connected procedural network generation is NOT built.** Existing worldgen places small isolated track stops in appropriate biomes, not a network with tunnels, bridges and connected stations.
- Train AI and **passenger services are NOT generated**. Create's conductor/schedule system must already be set up on an assembled train for depot return.
- Automatic warehouse-to-train cargo transfers, factory restock loops, NPC locomotive deliveries, network-wide dispatch and dedicated shunting tracks do NOT yet operate.
- The rack inventory is functional, but hardware throughput, cooling and power simulation aren't hooked into automation.
- Factory controller currently displays nearby warehouse totals; it is not an automatic crafting/production controller.
- Railway crossing models respond to redstone; **approaching trains do not yet trigger them**.
- A Display Relay currently reports the public order list, not company-private resources.
- Compile and resource checks passing do not guarantee that every client/runtime interaction works. An in-game integrated runtime test is still needed.

## First test

1. Use fresh Minecraft 1.21.1 NeoForge instance with Create 6.0.10 and this JAR on both client and server (if multiplayer).
2. Creative tab **Freight & Logistics** — test equipment appearances and all right-click GUIs.
3. \`/freight company create Paradox Freight\` then place Marketplace Terminal; buy a CPU and RAM, install inside Server Rack.
4. Place Warehouse Controller, insert items, exit/re-enter world and verify inventory persists.
5. Admin: \`/freight issue minecraft:iron_ingot 32 1200\`, accept via Freight Terminal and complete with 32 iron ingots.
6. Build an ordinary Create train with conductor, working track network and Create Train Station. Name the station. Stand within 12 blocks and click **Register nearby Create Station** on Depot Controller.
7. Enable automatic parking. Give your assembled Create train a normal schedule, let it complete and idle; watch whether it receives a Create schedule to the home station. This test requires the player's Create train ownership to match the company owner.
8. Place Create Display Link on a Management Computer or Warehouse Controller; connect to Display Board. Choose **Freight & Logistics Data** as source and change Page from 1 to 10.
9. Hover a Server Rack/Depot Controller/etc in inventory and hold W to access Ponder guides.
10. Admin: \`/freight railgen\` in a clear flat area. Explore new plains areas for rare generated track stops.

## GitHub

Source branch: \`create-freight-logistics-v0.1.3-integration\` in the owner's existing repository. The main branch is untouched. Builds are produced by GitHub Actions and verified for classes, models, JSONs and Ponder NBT assets.

## User-facing dependencies

Only Minecraft 1.21.1, NeoForge 21.1.x, official Create 6.0.10, Java 21 and this mod. There are no mandatory other Create addons.
