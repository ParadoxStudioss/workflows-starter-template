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
