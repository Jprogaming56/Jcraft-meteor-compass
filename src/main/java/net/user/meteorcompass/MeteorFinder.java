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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the nearest JCraft meteorite that still contains jcraft:meteorite_iron_ore_block.
 * Meteorites are random_spread structures, so we walk the spread grid outward in rings and
 * read each cell's structure start.
 */
final class MeteorFinder {
    /** How many spread-grid rings to search (each ring is ~17 chunks wide). */
    private static final int MAX_RING = 8;

    private static final String[][] SOURCES = {
            {"meteorites_land", "meteorite_land"},
            {"meteorites_ocean", "meteorite_ocean"}
    };

    private MeteorFinder() {}

    static Block ore() {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jcraft", "meteorite_iron_ore_block"));
        return b == null ? Blocks.AIR : b;
    }

    private record Source(RandomSpreadStructurePlacement placement, Structure structure) {}

    @Nullable
    static BlockPos findNearestOre(ServerLevel level, BlockPos origin) {
        Block ore = ore();
        if (ore == Blocks.AIR) return null;

        Registry<StructureSet> sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        List<Source> sources = new ArrayList<>();
        int minSpacing = Integer.MAX_VALUE;
        for (String[] s : SOURCES) {
            StructureSet set = sets.get(new ResourceLocation("jcraft", s[0]));
            Structure structure = structures.get(new ResourceLocation("jcraft", s[1]));
            if (set == null || structure == null) continue;
            if (!(set.placement() instanceof RandomSpreadStructurePlacement rs)) continue;
            sources.add(new Source(rs, structure));
            minSpacing = Math.min(minSpacing, rs.spacing());
        }
        if (sources.isEmpty()) return null;

        long seed = level.getSeed();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int r = 0; r <= MAX_RING; r++) {
            for (Source src : sources) {
                int spacing = src.placement().spacing();
                int ocx = Math.floorDiv(origin.getX() >> 4, spacing);
                int ocz = Math.floorDiv(origin.getZ() >> 4, spacing);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        ChunkPos cp = src.placement().getPotentialStructureChunk(
                                seed, (ocx + dx) * spacing, (ocz + dz) * spacing);
                        ChunkAccess chunk = level.getChunk(cp.x, cp.z, ChunkStatus.STRUCTURE_STARTS);
                        StructureStart start = chunk.getStartForStructure(src.structure());
                        if (start == null || !start.isValid()) continue;

                        BlockPos found = nearestOreIn(level, start.getBoundingBox(), origin, ore);
                        if (found != null) {
                            double d = found.distSqr(origin);
                            if (d < bestDist) {
                                bestDist = d;
                                best = found;
                            }
                        }
                    }
                }
            }
            // Anything in the next ring is at least r * spacing chunks away horizontally.
            if (best != null) {
                double safe = (double) r * minSpacing * 16.0;
                if (Math.sqrt(bestDist) <= safe) break;
            }
        }
        return best;
    }

    @Nullable
    static BlockPos nearestOreIn(ServerLevel level, BoundingBox bb, BlockPos origin, Block ore) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(bb.minY(), level.getMinBuildHeight());
        int maxY = Math.min(bb.maxY(), level.getMaxBuildHeight() - 1);

        for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
            for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz); // generates the chunk fully if needed
                int x0 = Math.max(bb.minX(), cx << 4), x1 = Math.min(bb.maxX(), (cx << 4) + 15);
                int z0 = Math.max(bb.minZ(), cz << 4), z1 = Math.min(bb.maxZ(), (cz << 4) + 15);
                for (int y = minY; y <= maxY; y++) {
                    for (int x = x0; x <= x1; x++) {
                        for (int z = z0; z <= z1; z++) {
                            pos.set(x, y, z);
                            if (chunk.getBlockState(pos).is(ore)) {
                                double d = pos.distSqr(origin);
                                if (d < bestDist) {
                                    bestDist = d;
                                    best = pos.immutable();
                                }
                            }
                        }
                    }
                }
            }
        }
        return best;
    }
}
