# Create: Freight & Logistics 0.1.4 — Railway Generation Release

**Minecraft 1.21.1 / NeoForge 21.1.x / official Create 6.0.10 / Java 21**

## New in 0.1.4: a real generated railway grid

In **new Overworld chunks only**, the addon lays genuine Create tracks on a deterministic
orthogonal network. The coordinate rules line up exactly across chunk boundaries.

- **Default line spacing:** 24 chunks (384 blocks).
- **Default track altitude:** Y=80, chosen for reliable chunk-to-chunk joins.
- **Crossing tiles:** appropriate Create crossing-rail shape where two mainlines meet.
- **Tunnel segments:** natural rock cut with five-block-wide clearance and a stone-brick envelope.
- **Bridge segments:** continuous ballast under rails, stone-brick support piers in open air.
- **Passenger stops:** real Create Train Station blocks on horizontal lines, with stone-brick
  platforms, lamps, and a spruce/copper canopy. Stations are queued for naming after
  Create's scheduled track-graph updates.
- **Protected blocks:** the generator refuses to overwrite block entities, beds, signs,
  doors, vanilla rails, planks, or other protected blocks. It can leave a gap if the
  new terrain cannot be modified safely. Only fresh chunks auto-generate the layout.
- **Performance:** at most one railway-bearing chunk is completed per server tick
  by default, and a bounded queue prevents massive backlog during exploration.

### Navigation and diagnostics

- `/freight railway status` — configuration, tracks and generated chunks, and the
  number of Create railway graphs and nodes actually found on the server.
- `/freight railway locate` — nearest *planned* mainline station coordinates.
- `/freight railway generate` — operator-only: build this chunk's planned railway
  layout manually when standing inside a qualifying existing chunk.
- `/freight railgen` — legacy straight-track test segment.

You must explore **new** chunks for auto-generation. An existing 0.1.3 world will
not retroactively receive railways. For a complete network, start a NEW world.
If the server has older explored chunks between new chunks, there will be rail gaps.

### Configuration

NeoForge SERVER config `freightlogistics-server.toml` (usually under
`<world>/serverconfig/`). Settings:

- `enabled=true`
- `line_spacing_chunks=24` (12–128)
- `rail_y=80` (68–180)
- `generate_stations=true`
- `allow_tunnels=true`
- `allow_bridges=true`
- `max_chunks_per_tick=1` (1–4)

**Do not change line spacing or rail Y after you have generated any chunks**:
new rails would no longer connect to the old network!

### Known limitations

This is a functional deterministic grid generator, NOT yet the complete
terrain-following, organic railway system from the design plan.

- Lines follow fixed Y; they don't adapt their height to terrain or use gentle
  curves. Mountains have tunnels; valleys have elevated viaducts.
- Level crossings may be geometric crossings rather than automatically
  switchable track junctions. Complex branching junctions are not yet built.
- No automatic network-wide signal placement, village-targeted routing,
  random NPC passengers, passenger trains, new locomotives, or train timetables.
- The generator protects sensitive blocks and may skip an obstructed chunk,
  creating a gap that requires manual track repair.
- Station naming and Create track graph are asynchronous. Some stations may
  need neighboring chunks to be loaded before Create fully connects the graph.
- A GitHub CI compilation pass alone does not prove the client runs or that
  every generated station functions. Use a test world and report crash logs.

The earlier company management, depot auto-return, warehouse inventory,
server rack, Display Links, and Ponder functionality remain in the mod.
No other third-party Create addon is mandatory.

## Test procedure

1. Make a fresh 1.21.1 NeoForge world with Create 6.0.10 and this JAR.
2. Ensure `/freight railway status` says enabled and reports graph counts after exploring.
3. Use `/freight railway locate` to find a planned station, then explore that location
   in a newly generated area (operator can teleport for testing).
4. Inspect the rails, junctions, bridge supports, tunnels, platform clearance,
   Create Station GUI and station name.
5. Place a conductor-operated Create train and test schedules across several
   chunks in each direction.
6. Save/reload and verify rails, stations, network routing, and station names persist.
7. Test in plains, ocean, mountains and near villages; send errors or screenshots
   when the layout is blocked or visually poor.

Source: GitHub branch `create-freight-logistics-v0.1.4-generation`.
