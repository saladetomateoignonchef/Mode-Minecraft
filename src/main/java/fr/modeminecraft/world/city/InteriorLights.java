package fr.modeminecraft.world.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayDeque;

/**
 * Éclairage garanti des intérieurs (hangars, maisons, bunkers, étages...) : après la construction d'un morceau de
 * bâtiment, tout endroit où l'on peut marcher sous un toit et qui reçoit moins de {@link #MIN_LIGHT} de lumière reçoit
 * un luminaire, jusqu'à ce que partout la lumière soit supérieure à 10 (plus aucune place pour les monstres vanilla) :
 * - plafond bas (2 blocs) : plafonnier encastré (néon de ville) ;
 * - plafond de 3 à 4 blocs : lanterne suspendue ;
 * - grande hauteur (hangars) : balise encastrée au sol.
 * La propagation de la lumière est simulée comme dans le jeu (la lumière perd 1 par bloc, les murs l'arrêtent) ;
 * les éclairages déjà présents (néons, lanternes, torches, lave...) sont comptés.
 */
final class InteriorLights {
    /** Lumière minimale exigée partout à l'intérieur (la consigne : supérieure à 10). */
    static final int MIN_LIGHT = 11;
    private static final int MAX_ROOF = 40;

    private InteriorLights() {}

    static void apply(B b, int minY, int maxY) {
        WorldGenLevel level = b.level;
        BoundingBox clip = b.clip;
        minY = Math.max(minY, level.getMinBuildHeight() + 1);
        maxY = Math.min(maxY, level.getMaxBuildHeight() - 2);
        int x0 = clip.minX(), z0 = clip.minZ();
        int w = clip.maxX() - x0 + 1, d = clip.maxZ() - z0 + 1, h = maxY - minY + 1;
        if (w <= 0 || d <= 0 || h <= 3) {
            return;
        }
        boolean[] lit = new boolean[w * d * h];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        // 1) lumière des sources déjà posées
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                for (int y = 0; y < h; y++) {
                    BlockState s = level.getBlockState(m.set(x0 + x, minY + y, z0 + z));
                    int em = s.getLightEmission();
                    if (em < MIN_LIGHT && fr.modeminecraft.world.night.NightOps.isCityLight(s) && indoorCityLight(level, m, x0 + x, minY + y, z0 + z, maxY)) {
                        em = 15;                                       // néon d'intérieur : allumé jour et nuit
                    }
                    if (em >= MIN_LIGHT) {
                        spread(level, lit, x0, minY, z0, w, d, h, x, y, z, em);
                    }
                }
            }
        }
        // 2) endroits praticables, couverts, pas assez éclairés
        for (int y = 1; y < h - 3; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    if (lit[(x * d + z) * h + y]) {
                        continue;
                    }
                    int wx = x0 + x, wy = minY + y, wz = z0 + z;
                    if (!standable(level, m, wx, wy, wz)) {
                        continue;
                    }
                    int roof = roofY(level, m, wx, wy, wz, maxY);
                    if (roof < 0) {
                        continue;                                      // plein air : rues et cours (lampadaires)
                    }
                    int gap = roof - wy;
                    int py, kind;                                       // position et type du luminaire
                    if (gap >= 3 && gap <= 4 && level.getBlockState(m.set(wx, roof - 1, wz)).isAir()) {
                        py = roof - 1;
                        kind = 0;                                       // lanterne suspendue
                    } else if (gap <= 2 && plain(level, m, wx, roof, wz)) {
                        py = roof;
                        kind = 1;                                       // plafonnier encastré
                    } else if (plain(level, m, wx, wy - 1, wz)) {
                        py = wy - 1;
                        kind = 1;                                       // balise encastrée au sol
                    } else {
                        continue;
                    }
                    BlockState light = kind == 0
                            ? Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true)
                            : Blocks.SEA_LANTERN.defaultBlockState();
                    level.setBlock(m.set(wx, py, wz), light, 2);       // luminaire fixe (ne s'éteint ni le jour ni en cas de panne)
                    spread(level, lit, x0, minY, z0, w, d, h, x, py - minY, z, 15);
                    lit[(x * d + z) * h + y] = true;
                }
            }
        }
    }

    /** Éclairage de ville situé à plus de 3 blocs sous le point le plus haut de sa colonne : il reste allumé le jour. */
    private static boolean indoorCityLight(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z, int maxY) {
        int top = y;
        for (int yy = y + 1; yy <= maxY; yy++) {
            BlockState s = level.getBlockState(m.set(x, yy, z));
            if (!s.isAir() && !s.is(BlockTags.LEAVES)) {
                top = yy;
            }
        }
        return top - y > 3;
    }

    /** Diffuse la lumière dans l'air autour de la source (perte de 1 par bloc, arrêtée par les blocs opaques). */
    private static void spread(WorldGenLevel level, boolean[] lit, int x0, int minY, int z0, int w, int d, int h, int sx, int sy, int sz, int power) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sy, sz, power});
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        java.util.HashSet<Integer> seen = new java.util.HashSet<>();
        seen.add((sx * d + sz) * h + sy);
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            if (c[3] >= MIN_LIGHT && c[1] >= 0 && c[1] < h) {
                lit[(c[0] * d + c[2]) * h + c[1]] = true;
            }
            if (c[3] - 1 < MIN_LIGHT) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                int nx = c[0] + dir.getStepX(), ny = c[1] + dir.getStepY(), nz = c[2] + dir.getStepZ();
                if (nx < 0 || nz < 0 || ny < 0 || nx >= w || nz >= d || ny >= h || !seen.add((nx * d + nz) * h + ny)) {
                    continue;
                }
                BlockState s = level.getBlockState(m.set(x0 + nx, minY + ny, z0 + nz));
                if (s.canOcclude() && s.isSolidRender(level, m)) {
                    continue;
                }
                queue.add(new int[]{nx, ny, nz, c[3] - 1});
            }
        }
    }

    /** Un joueur peut se tenir ici : deux blocs libres au-dessus d'un sol plein (pas dans un liquide). */
    private static boolean standable(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z) {
        BlockState feet = level.getBlockState(m.set(x, y, z));
        if (!feet.getCollisionShape(level, m).isEmpty() || !feet.getFluidState().isEmpty()) {
            return false;
        }
        BlockState head = level.getBlockState(m.set(x, y + 1, z));
        if (!head.getCollisionShape(level, m).isEmpty() || !head.getFluidState().isEmpty()) {
            return false;
        }
        BlockState floor = level.getBlockState(m.set(x, y - 1, z));
        return floor.isFaceSturdy(level, m, Direction.UP) && floor.getFluidState().isEmpty();
    }

    /** Altitude du toit au-dessus de ce point (premier bloc plein, hors feuillage), ou -1 s'il n'y en a pas : plein air. */
    private static int roofY(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z, int maxY) {
        for (int yy = y + 2; yy <= Math.min(maxY, y + MAX_ROOF); yy++) {
            BlockState s = level.getBlockState(m.set(x, yy, z));
            if (s.is(BlockTags.LEAVES)) {
                continue;
            }
            if (s.isFaceSturdy(level, m, Direction.DOWN)) {
                return yy;
            }
        }
        return -1;
    }

    /** Bloc plein ordinaire que l'on peut remplacer par un luminaire (ni coffre, ni porte, ni objet technique). */
    private static boolean plain(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z) {
        BlockState s = level.getBlockState(m.set(x, y, z));
        return !s.hasBlockEntity() && s.isSolidRender(level, m) && s.getDestroySpeed(level, m) >= 0 && !s.is(BlockTags.DOORS)
                && s.getLightEmission() == 0 && !(s.getBlock() instanceof net.minecraft.world.level.block.BedBlock);
    }
}
