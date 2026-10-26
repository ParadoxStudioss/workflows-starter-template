# Create: Freight & Logistics 0.1.5.1-alpha — Curving Mainlines & Larger Stations

**Minecraft 1.21.1 · NeoForge 21.1.x · Create 6.0.10 · Java 21**

> This is an alpha railway-generation build. Use a brand-new throwaway
> world; keep the existing 0.1.4 world intact as backup. No other Create addon
> besides the original Create is required.

## What changed since 0.1.5

Version 0.1.5 introduced terrain-following straight mainlines and real Create curves at junctions. Version 0.1.5.1-alpha retains that working foundation and adds native Create S-curves along the primary routes. It does not require neighboring chunks to be generated just to plan its alignment.

- **Winding regional mainlines:** Every 384-block default interval has two 64-block native Create S-curves, bending the track by a seeded 8–14 blocks and returning to the junction approach. Native track entities are linked in both directions after endpoints load. Smaller custom regions below 320 blocks retain straight track approaches.
- **Ground-level railway:** every section uses the sampled world terrain and a
  moderated, gradual track elevation. Creates use genuine ascending track
  shapes when the grade changes. No universal Y=80 railway.
- **Ballast and earthworks:** gravel below the rails, andesite shoulders,
  coarse-dirt verges and small embankments.
- **Bridge crossings only where appropriate:** over low ground and water,
  small masonry columns and safety edges, no forest of 40-block pillars.
- **Rock tunnels:** cut-and-cover clearance with enclosed stone-brick walls.
- **Larger passenger stations:** two 81-block graded platforms, lighting,
  roofed canopies with dark-oak/copper trim, benches, and a 33×13 spruce
  station building with an industrial freight reception. These use genuine Create Train Station blocks.
- **Curved junction turnouts:** where orthogonal lines cross, creates four
  actual Create BezierConnection paths linking rail endpoints to let
  conductors change direction; not just visual fake track.
- **More trackside variety:** maintenance cabins, glazed dispatcher towers,
  raised copper water tanks, railway signals and lamps. Wayside buildings are
  kept away from the bigger station platforms. Structures are deterministic.
- **Protection and performance:** the generator avoids replacing existing
  Create track blocks, keeps native ballast writes within their source chunk,
  and caches expensive terrain samples to reduce server-tick stalls.

## Commands

- `/freight railway locate` — coordinate of next planned station.
- `/freight railway status` — current generation counters & Create graphs.
- `/freight railway inspect <x> <z>` — terrain-predicted rail Y and block at it.
- `/freight railway verify <x> <z>` — verify two adjacent rail positions are in
  the SAME actual Create track graph.
- `/freight railway junction <x> <z>` — verify a hub's actual Bezier connections.
- `/freight railway curve <x> <z>` — verify a long-distance S-curve's native bidirectional Create links (e.g. x=128,z=8).
- `/freight railway generate` — operator manual generation of a qualifying
  chunk in the existing world.
- `/freight railgen` — legacy straight 31-track test.

## World generation & configuration

Only **new Overworld chunks** automatically receive the railway network.
The default corridor grid is 24 chunks (384 blocks); crossing hubs occur at
world coordinates x=8+384n, z=8+384m.

Configure `<world>/serverconfig/freightlogistics-server.toml`:

```toml
[railway_generation]
enabled = true
line_spacing_chunks = 24
generate_stations = true
allow_tunnels = true
allow_bridges = true
max_chunks_per_tick = 1
```

`rail_y` is a legacy compatibility option and does not determine
the new terrain-following mainline elevation.

Do not alter spacing after generating chunks, or new tracks will not match
their previously generated neighbors. Previously generated 0.1.4 chunks
are NOT modified; use a fresh world for testing.

## What it does NOT yet do

- Railway junctions are still arranged on a regular regional network; the new
  meandering trunk segments remove kilometer-long dead-straight sections, but
  full village-to-village routing, biome-aware route selection, and dynamic
  regional graph planning remain future features.
- Not yet a network of scheduled, autonomously generated NPC trains.
- Complex interlocking and automatic signaling are still planned.
- Terrain clearing can be blocked by existing protected structures.
- A compiled JAR alone is not proof of safe handling of every modded
  terrain/biome combination. Test in a clean Creative world.

## Testing

1. Create a new world with Create 6.0.10 and the 0.1.5.1-alpha JAR.
2. Locate rail lines using `/freight railway locate`, then use Creative
   flight to inspect the trackbed and grade.
3. Explore a full hub and inspect the visible curves at x=8,z=8.
4. Use `/freight railway verify 63 8` to test graph connectivity
   across chunks, if both chunks have generated and are loaded.
5. Use `/freight railway junction 8 8` near the main grid origin
   to check the Bezier turnouts.
6. Check a generated station near x=72,z=8 (coordinates depend
   on origin chunk and spacing). Inspect the 41-block platforms and hall.
7. Drive an assembled Create train with a conductor along grades and
   through multiple junctions. Report screenshots and latest.log errors.

The company management, depot automatic returns, server rack, warehouse
inventory, Ponder and Display Link features remain from 0.1.3.

Source branch: `create-freight-logistics-v0.1.5-terrain`.

## Native NBT station templates

When an NBT station schematic is supplied, its structure format and modded block palette can be inspected and adapted into a future build. Alpha 0.1.5.1 does not yet import external .nbt automatically. Vanilla Structure Block NBT and Create schematics can use different palette conventions; do not copy one into the other without validation. Real Create station blocks must be attached to a generated, graph-connected track after the structure is placed.
