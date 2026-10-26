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
