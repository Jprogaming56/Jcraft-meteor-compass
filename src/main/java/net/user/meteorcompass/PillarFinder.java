package net.user.meteorcompass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the nearest JCraft Additions Devil's Pillar (jcraft_additions:devils_pillar) that still
 * contains jcraft_additions:suspicious_corpse_part_sand (the Saint's corpse part sand).
 * Pillars are random_spread structures (spacing 32 chunks), so we walk the spread grid in rings.
 */
final class PillarFinder {
    private static final ResourceLocation SET_ID = new ResourceLocation("jcraft_additions", "devils_pillar");
    private static final ResourceLocation STRUCTURE_ID = new ResourceLocation("jcraft_additions", "devils_pillar");

    private PillarFinder() {}

    static Block target() {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jcraft_additions", "suspicious_corpse_part_sand"));
        return b == null ? Blocks.AIR : b;
    }

    @Nullable
    static BlockPos find(ServerLevel level, BlockPos origin) {
        Block target = target();
        if (target == Blocks.AIR) return null;

        Registry<StructureSet> sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        StructureSet set = sets.get(SET_ID);
        Structure structure = structures.get(STRUCTURE_ID);
        if (set == null || structure == null) return null;
        if (!(set.placement() instanceof RandomSpreadStructurePlacement rs)) return null;

        int spacing = rs.spacing();
        long seed = level.getSeed();
        int ocx = Math.floorDiv(origin.getX() >> 4, spacing);
        int ocz = Math.floorDiv(origin.getZ() >> 4, spacing);

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        int maxRing = CompassConfig.PILLAR_RINGS.get();
        for (int r = 0; r <= maxRing; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    ChunkPos cp = rs.getPotentialStructureChunk(seed, (ocx + dx) * spacing, (ocz + dz) * spacing);
                    ChunkAccess chunk = level.getChunk(cp.x, cp.z, ChunkStatus.STRUCTURE_STARTS);
                    StructureStart start = chunk.getStartForStructure(structure);
                    if (start == null || !start.isValid()) continue;

                    BlockPos found = MeteorFinder.nearestOreIn(level, start.getBoundingBox(), origin, target);
                    if (found != null) {
                        double d = found.distSqr(origin);
                        if (d < bestDist) {
                            bestDist = d;
                            best = found;
                        }
                    }
                }
            }
            // Anything in the next ring is at least r * spacing chunks away horizontally.
            if (best != null && Math.sqrt(bestDist) <= (double) r * spacing * 16.0) break;
        }
        return best;
    }
}
