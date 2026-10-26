# Changelog

## 0.1.5.2-alpha — Mountain works, station reversing and depot return

- Added a second, opposite-facing Create station at every generated mainline passenger stop. Both are targeted to the same continuous track and intended for two-ended train schedules.
- Added `/freight depot return`: route owned trains home from a station, or queue a return for their next station stop if already moving. No unsafe instant reversing of moving trains.
- Added `/freight railway repair` for operator-safe, loaded-only corridor and tunnel repairs around the player. Old chunks are never silently rewritten.
- Added deep cutting walls, stone-lined tunnel approaches and curved-track mountain excavation with full usable clearance in processed chunks.
- Replaced the oversized coarse-dirt bridge foundations with stone-and-ballast decks, reinforced pillars and masonry parapets.
- Hardened cross-chunk track and terrain clearance to avoid implicit writes/loads outside the current chunk.
- Re-runs native Create Bezier junction attachment when existing hub chunks load.
- Preserved the network's existing 64-block meanders, regional connectivity, station halls, freight controllers and other 0.1.5.1-alpha systems.

**Important:** reversing a Create schedule at a station requires a train assembled with Train Controls and conductors facing both directions. The alternative is an uninterrupted forward-only circuit with appropriate station placements; this version does not invent a physical balloon loop at every endpoint. The full world cannot be connected beyond unloaded/unvisited chunks, and protected player builds can stop a corridor until repaired manually.

# Changelog

## 0.1.5.1-alpha — Winding mainlines and station expansion

- Added seeded regional 64-block S-curves using native, reciprocal Create Bezier connections.
- Mainline tangents and curve endpoints are deterministic across newly generated chunks.
- Added curved earthworks and ballast, with underpasses/short supports where suitable.
- Extended generated passenger platforms from 41 to 81 blocks and expanded the station hall to 33x13.
- Added glazed dispatcher signal towers and elevated copper water tanks to wayside generation.
- Moved maintenance huts out of the enlarged station footprint.
- Improved safety around pre-existing Create tracks and chunk-boundary placement.
- Cached final terrain envelope heights and removed redundant spline-height queries.
- Added `/freight railway curve <x> <z>` for native S-curve link checks and CI smoke coverage.
- Kept prior management, depots, equipment and Ponder integration from version 0.1.5.

# Changelog

## 0.1.5 — Ground-hugging terrain generation and curved station networks

- Replaced universal Y=80 fixed viaduct lines with chunk-independent
  worldgen-noise terrain profiles.
- Uses sloped Create tracks for gradual elevations and a local grade guard.
- Added gravel ballast, andesite shoulders and coarse-dirt track verges.
- Bridge piers now appear only where rails are elevated above local ground.
- Built 41-block-long graded, dual-sided stations with full roof,
  passenger platforms, lanterns, benches and a timber station hall.
- Added four real Create BezierConnection turnouts at every suitable hub.
- Generated maintenance huts and wayside railway signaling equipment.
- Added railway terrain inspection and junction diagnostics.
- Strengthened physical Create graph and full station CI tests.
- Kept existing economy, automation, device and Ponder integrations.

**Still planned:** village-aware railway routes, smooth meandering mainlines,
switchyard layouts with schedules and automatic freight services.
