# Changelog

## 0.1.4 — Railway world-generation foundation
- Replaced scattered, disconnected legacy stop injection with a deterministic
  two-directional grid of genuine Create TrackShape rail blocks.
- Mainline sections line up at chunk borders without forcing neighboring chunks.
- Made a new-chunk-only deferred generation scheduler.
- Added stone-brick viaduct supports and enclosed tunnels with train clearance.
- Added actual Create Train Station blocks, platforms, canopies and station naming.
- Added NeoForge server config for railway spacing, elevation, and infrastructure.
- Added /freight railway status, locate and manual generation testing commands.
- Added fresh-world dedicated-server CI smoke-testing workflow.
- Kept mod's prior Create management/warehouse/depot/Ponder integrations.

### Still planned
Terrain-adaptive grades, switchable curved junctions, village-aware routing,
distributed locomotive services, full NPC company system, automatic freight
loading/unloading, and long-distance dispatch.
