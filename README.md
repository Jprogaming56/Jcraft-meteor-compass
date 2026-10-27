# JCraft Meteorite Compass
Forge 1.20.1 addon for JCraft 0.18.x.

Craft: Compass in the middle, Stellar Iron Ingots on the four sides (top/bottom/left/right).
Right-click: finds the nearest meteorite that still contains Meteorite Iron Ore and points the needle at the nearest ore block.
If that ore is mined out, the compass retargets on its own (checked every 2 seconds).
Search radius: about 8 spawn-grid rings (~2,000 blocks). It generates chunks while searching, so a search can cause a short stall.
Build: push to GitHub (create .github/workflows/build.yml by hand if the uploader skipped it) and run the Actions build.
