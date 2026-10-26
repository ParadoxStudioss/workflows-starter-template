# Create: Freight & Logistics 0.1.5 — Terrain & Stations

**Minecraft 1.21.1 · NeoForge 21.1.x · Create 6.0.10 · Java 21**

> This is an experimental railway-generation build. Use a brand-new throwaway
> world; keep the existing 0.1.4 world intact as backup. No other Create addon
> besides the original Create is required.

## What changed since 0.1.4

Version 0.1.4 built flat Y=80, monotonous rail bridges and an X-shaped grid.
0.1.5 now uses a terrain-aware route profile computed directly from Minecraft's
worldgen noise, WITHOUT loading neighboring chunks just to query height.

- **Ground-level railway:** every section uses the sampled world terrain and a
  moderated, gradual track elevation. Creates use genuine ascending track
  shapes when the grade changes. No universal Y=80 railway.
- **Ballast and earthworks:** gravel below the rails, andesite shoulders,
  coarse-dirt verges and small embankments.
- **Bridge crossings only where appropriate:** over low ground and water,
  small masonry columns and safety edges, no forest of 40-block pillars.
- **Rock tunnels:** cut-and-cover clearance with enclosed stone-brick walls.
- **Larger passenger stations:** two 41-block graded platforms, lighting,
  roofed canopies with dark-oak/copper trim, benches and a spruce station
  building. These use genuine Create Train Station blocks.
- **Curved junction turnouts:** where orthogonal lines cross, creates four
  actual Create BezierConnection paths linking rail endpoints to let
  conductors change direction; not just visual fake track.
- **More trackside variety:** maintenance cabins, glazed windows,
  trackside signaling posts and lamps. Structures are deterministic.
- **Protection:** rail/station decorations skip unrelated existing Create
  rails and sensitive tile blocks when possible.

## Commands

- `/freight railway locate` — coordinate of next planned station.
- `/freight railway status` — current generation counters & Create graphs.
- `/freight railway inspect <x> <z>` — terrain-predicted rail Y and block at it.
- `/freight railway verify <x> <z>` — verify two adjacent rail positions are in
  the SAME actual Create track graph.
- `/freight railway junction <x> <z>` — verify a hub's actual Bezier connections.
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

- Routes still broadly follow an orthogonal regional network rather than
  dynamically choosing village-to-village paths or following river valleys.
- Curves are junction turnouts; an organic winding network with
  long-distance flexible branch planning isn't implemented yet.
- Not yet a network of scheduled, autonomously generated NPC trains.
- Complex interlocking and automatic signaling are still planned.
- Terrain clearing can be blocked by existing protected structures.
- A compiled JAR alone is not proof of safe handling of every modded
  terrain/biome combination. Test in a clean Creative world.

## Testing

1. Create a new world with Create 6.0.10 and the 0.1.5 JAR.
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
