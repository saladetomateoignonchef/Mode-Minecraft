package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.item.BossCardItem;
import fr.modeminecraft.registry.ModBlocks;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Construction des structures maritimes, chunk par chunk (même hasard dans tous les chunks). Repère local (u, v) :
 * plateforme et porte-avions : depuis le coin de l'emprise ; base navale : u le long de la côte (0 à 160), v de la
 * terre (v négatif, le quai) vers le large (le port), repère tourné selon la côte. y = 0 : niveau de la mer.
 */
public class NavalPiece extends StructurePiece {
    private final String kind;
    private final boolean soviet;
    private final int ox, oz, dir;
    private final long seed;
    private static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    public NavalPiece(BoundingBox box, String kind, boolean soviet, int ox, int oz, int dir, long seed) {
        super(ModStructures.NAVAL_PIECE.get(), 0, box);
        this.kind = kind;
        this.soviet = soviet;
        this.ox = ox;
        this.oz = oz;
        this.dir = dir;
        this.seed = seed;
    }

    public NavalPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.NAVAL_PIECE.get(), tag);
        kind = tag.getString("Kind");
        soviet = tag.getBoolean("Soviet");
        ox = tag.getInt("OX");
        oz = tag.getInt("OZ");
        dir = tag.getInt("Dir");
        seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("Kind", kind);
        tag.putBoolean("Soviet", soviet);
        tag.putInt("OX", ox);
        tag.putInt("OZ", oz);
        tag.putInt("Dir", dir);
        tag.putLong("Seed", seed);
    }

    /** Repère local vers le monde (x, z). */
    static int[] toWorld(String kind, int ox, int oz, int dir, int u, int v) {
        if (!kind.equals("naval_base")) {
            return new int[]{ox + u, oz + v};
        }
        int[] land = DIRS[dir];
        int ax = -land[1], az = land[0];                                   // le long de la côte
        int du = u - 80, dv = 70 - v;                                      // v = 70 : centre du port ; v décroissant vers la terre
        return new int[]{ox + ax * du + land[0] * dv, oz + az * du + land[1] * dv};
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        int sea = generator.getSeaLevel();
        B b = new B(level, chunkBox, ox, sea, oz, RandomSource.create(seed), sea);
        R r = new R(b);
        switch (kind) {
            case "oil_rig" -> oilRig(r);
            case "carrier" -> carrier(r);
            default -> navalBase(r);
        }
    }

    // ------------------------------------------------------------------ repère tourné
    final class R {
        final B b;

        R(B b) {
            this.b = b;
        }

        int[] d(int u, int v) {
            int[] w = toWorld(kind, ox, oz, dir, u, v);
            return new int[]{w[0] - ox, w[1] - oz};
        }

        boolean inside(int u, int y, int v) {
            int[] p = d(u, v);
            return b.inside(p[0], y, p[1]);
        }

        void set(int u, int y, int v, BlockState s) {
            int[] p = d(u, v);
            b.set(p[0], y, p[1], s);
        }

        void set(int u, int y, int v, Block s) {
            set(u, y, v, s.defaultBlockState());
        }

        BlockState get(int u, int y, int v) {
            int[] p = d(u, v);
            return b.get(p[0], y, p[1]);
        }

        void fill(int u0, int y0, int v0, int u1, int y1, int v1, BlockState s) {
            int[] a = d(u0, v0), c = d(u1, v1);
            b.fill(Math.min(a[0], c[0]), y0, Math.min(a[1], c[1]), Math.max(a[0], c[0]), y1, Math.max(a[1], c[1]), s);
        }

        void fill(int u0, int y0, int v0, int u1, int y1, int v1, Block s) {
            fill(u0, y0, v0, u1, y1, v1, s.defaultBlockState());
        }

        void walls(int u0, int y0, int v0, int u1, int y1, int v1, Block s) {
            fill(u0, y0, v0, u1, y1, v0, s);
            fill(u0, y0, v1, u1, y1, v1, s);
            fill(u0, y0, v0, u0, y1, v1, s);
            fill(u1, y0, v0, u1, y1, v1, s);
        }

        /** Pilier de `top` jusqu'au fond (à travers l'eau). */
        void pillar(int u, int v, int top, BlockState s) {
            if (!inside(u, top, v)) {
                return;
            }
            for (int y = top; y > -90; y--) {
                BlockState cur = get(u, y, v);
                if (y < top && !cur.isAir() && cur.getFluidState().isEmpty() && !cur.canBeReplaced()) {
                    break;
                }
                set(u, y, v, s);
            }
        }

        /** Terre-plein : terrain rasé au-dessus, comblé en dessous (jusqu'au fond), revêtement à y = top. */
        void flatten(int u0, int v0, int u1, int v1, int top, BlockState surface, BlockState under) {
            for (int u = u0; u <= u1; u++) {
                for (int v = v0; v <= v1; v++) {
                    if (!inside(u, top, v)) {
                        continue;
                    }
                    for (int y = top + 1; y <= top + 40; y++) {
                        if (!get(u, y, v).isAir()) {
                            set(u, y, v, Blocks.AIR.defaultBlockState());
                        }
                    }
                    set(u, top, v, surface);
                    for (int y = top - 1; y > -50; y--) {
                        BlockState cur = get(u, y, v);
                        if (!cur.isAir() && cur.getFluidState().isEmpty() && !cur.canBeReplaced() && !cur.is(net.minecraft.tags.BlockTags.LEAVES)
                                && !cur.is(net.minecraft.tags.BlockTags.LOGS)) {
                            break;
                        }
                        set(u, y, v, under);
                    }
                }
            }
        }

        BlockPos pos(int u, int y, int v) {
            int[] p = d(u, v);
            return b.pos(p[0], y, p[1]);
        }

        HumanBotEntity bot(int u, int y, int v, Faction f, BotRole role, int home) {
            int[] p = d(u, v);
            return b.bot(p[0], y, p[1], f, role, home);
        }

        void pair(int u, int y, int v, Faction f, int home) {
            int[] p = d(u, v);
            b.pair(p[0], y, p[1], f, home);
        }

        void turret(int u, int y, int v, MountedGunEntity.Kind k, float yaw, Faction f) {
            int[] p = d(u, v);
            b.turret(p[0], y, p[1], k, yaw, f);
        }

        void chest(int u, int y, int v, String loot) {
            int[] p = d(u, v);
            b.chest(p[0], y, p[1], Direction.NORTH, loot, false);
        }

        /** Lacet Minecraft d'un cap local (du, dv). */
        float yaw(int du, int dv) {
            int[] a = d(0, 0), c = d(du, dv);
            return (float) Math.toDegrees(Math.atan2(-(c[0] - a[0]), c[1] - a[1]));
        }

        /** Navire à flot (équipage de bots si crew > 0), port d'attache `home`. */
        MilVehicleEntity ship(int u, int v, MilType t, float yaw, Faction f, int crew, boolean moored, int hu, int hv) {
            if (!inside(u, 0, v)) {
                return null;
            }
            ServerLevel sl = b.level.getLevel();
            BlockPos p = pos(u, 0, v);
            MilVehicleEntity s = MilVehicleEntity.create(sl, t, p.getX() + 0.5, b.sea - 0.1 - t.draft(), p.getZ() + 0.5, yaw);
            s.fillAmmo(1f);
            s.setFuel(1f);
            s.moored = moored;
            s.navalHome = pos(hu, 0, hv);
            s.autoDefense = crew > 0;
            for (int i = 0; i < crew; i++) {
                HumanBotEntity bot = ModEntities.HUMAN_BOT.get().create(sl);
                if (bot == null) {
                    break;
                }
                bot.moveTo(p.getX() + 0.5, b.sea + 2, p.getZ() + 0.5, yaw, 0);
                bot.setup(f, i == 0 ? BotRole.OFFICER : BotRole.SOLDIER, b.er);
                bot.battle = true;
                bot.setPersistenceRequired();
                bot.finalizeSpawn(b.level, b.level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
                bot.startRiding(s, true);
            }
            b.level.addFreshEntityWithPassengers(s);
            return s;
        }

        /** Chasseur garé sur le pont d'envol (place d'appontage mémorisée). */
        void jet(int u, int y, int v, MilType t, float yaw, int lu, int lv) {
            if (!inside(u, y, v)) {
                return;
            }
            BlockPos p = pos(u, y, v);
            MilVehicleEntity j = MilVehicleEntity.create(b.level.getLevel(), t, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw);
            j.fillAmmo(1f);
            j.setFuel(1f);
            j.carrierSpot = p;
            j.carrierYaw = yaw;
            j.carrierLaunch = pos(lu, y, lv);
            b.level.addFreshEntity(j);
        }
    }

    private static BlockState st(Block b) {
        return b.defaultBlockState();
    }

    /** Cuve cylindrique verticale. */
    private static void tank(R r, int cu, int cv, int y0, int h, int rad, Block wall, Block top) {
        for (int u = -rad; u <= rad; u++) {
            for (int v = -rad; v <= rad; v++) {
                double d = Math.sqrt(u * u + v * v);
                if (d <= rad + 0.3) {
                    r.fill(cu + u, y0, cv + v, cu + u, y0 + h - 1, cv + v, d > rad - 0.7 ? wall : Blocks.AIR);
                    r.set(cu + u, y0 + h, cv + v, top);
                }
            }
        }
    }

    // ================================================================== plateforme pétrolière offshore (64 x 64)
    private void oilRig(R r) {
        Faction f = Faction.OFFSHORE;
        BlockState deck = st(Blocks.GRAY_CONCRETE), grate = st(Blocks.IRON_TRAPDOOR), rail = st(Blocks.IRON_BARS);
        BlockState leg = st(Blocks.IRON_BLOCK), band = st(Blocks.YELLOW_CONCRETE);
        // 4 piles en treillis jusqu'au fond, bande jaune à la ligne de flottaison, entretoises
        int[][] legs = {{4, 4}, {56, 4}, {4, 56}, {56, 56}};
        for (int[] l : legs) {
            for (int du = 0; du < 4; du++) {
                for (int dv = 0; dv < 4; dv++) {
                    r.pillar(l[0] + du, l[1] + dv, 11, leg);
                }
            }
            r.fill(l[0], -3, l[1], l[0] + 3, 3, l[1] + 3, band);
        }
        for (int y : new int[]{-12, -4, 6}) {
            r.fill(8, y, 5, 55, y, 6, rail);
            r.fill(8, y, 57, 55, y, 58, rail);
            r.fill(5, y, 8, 6, y, 55, rail);
            r.fill(57, y, 8, 58, y, 55, rail);
        }
        // quai inférieur (embarcations) côté sud, à 2 blocs au-dessus de l'eau
        r.fill(0, 1, 60, 63, 1, 70, deck);
        r.fill(0, 2, 70, 63, 2, 70, rail);
        for (int u = 2; u < 64; u += 10) {
            r.pillar(u, 70, 0, leg);
            r.set(u, 2, 70, Blocks.OAK_FENCE);                              // bittes d'amarrage
        }
        r.fill(31, 2, 61, 31, 12, 61, Blocks.SCAFFOLDING);                // échelle vers le pont principal
        // pont principal y = 12 : béton gris, caillebotis, ligne de sécurité jaune, garde-corps
        r.fill(0, 12, 0, 63, 12, 63, deck);
        for (int u = 10; u < 54; u += 6) {
            r.fill(u, 12, 10, u + 1, 12, 53, grate);
        }
        r.walls(1, 12, 1, 62, 12, 62, Blocks.YELLOW_CONCRETE);
        r.walls(0, 13, 0, 63, 13, 63, Blocks.IRON_BARS);
        r.set(31, 12, 61, Blocks.SCAFFOLDING);
        r.set(31, 13, 63, Blocks.AIR);
        // derrick : tour en treillis de 8 x 8 qui s'affine jusqu'à 45 blocs, tige de forage jusqu'à l'eau
        for (int y = 13; y <= 45; y++) {
            int h = Math.max(1, 4 - (y - 13) / 9);
            int c = 32;
            for (int[] k : new int[][]{{-h, -h}, {h, -h}, {-h, h}, {h, h}}) {
                r.set(c + k[0], y, c + k[1], Blocks.RED_CONCRETE);
            }
            if (y % 4 == 1) {
                r.walls(c - h, y, c - h, c + h, y, c + h, Blocks.IRON_BARS);
            }
        }
        r.fill(31, 46, 31, 33, 47, 33, Blocks.IRON_BLOCK);                  // moufle
        r.fill(32, 0, 32, 32, 45, 32, Blocks.CHAIN);
        // torchère : bras en porte-à-faux, flamme permanente
        r.fill(-16, 22, 3, 0, 22, 4, Blocks.IRON_BLOCK);
        r.fill(-16, 23, 3, 0, 23, 4, Blocks.IRON_BARS);
        r.fill(1, 13, 3, 2, 22, 4, Blocks.IRON_BLOCK);
        r.set(-17, 23, 3, Blocks.NETHERRACK);
        r.set(-17, 24, 3, Blocks.FIRE);
        r.set(-17, 23, 4, Blocks.CAMPFIRE);
        // cuves
        tank(r, 50, 12, 13, 8, 3, Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE);
        tank(r, 50, 22, 13, 8, 3, Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE);
        tank(r, 14, 14, 13, 6, 3, Blocks.ORANGE_CONCRETE, Blocks.GRAY_CONCRETE);
        // grues sur piédestal (2)
        for (int[] c : new int[][]{{58, 34, 1}, {8, 30, -1}}) {
            r.fill(c[0], 13, c[1], c[0] + 1, 24, c[1] + 1, Blocks.YELLOW_CONCRETE);
            for (int k = 0; k < 16; k++) {
                r.set(c[0] + c[2] * k, 25 + k / 3, c[1], Blocks.YELLOW_CONCRETE);
            }
            r.fill(c[0] + c[2] * 15, 14, c[1], c[0] + c[2] * 15, 29, c[1], Blocks.CHAIN);
        }
        // quartiers d'habitation (2 étages) surmontés de l'hélipad
        r.fill(6, 13, 40, 27, 20, 59, Blocks.WHITE_CONCRETE);
        r.fill(7, 13, 41, 26, 15, 58, Blocks.AIR);
        r.fill(7, 17, 41, 26, 19, 58, Blocks.AIR);
        r.fill(7, 16, 41, 26, 16, 58, Blocks.LIGHT_GRAY_CONCRETE);
        for (int v = 42; v <= 57; v += 3) {
            r.set(6, 14, v, Blocks.GLASS);
            r.set(6, 18, v, Blocks.GLASS);
            r.set(27, 14, v, Blocks.GLASS);
            r.set(27, 18, v, Blocks.GLASS);
        }
        r.fill(16, 13, 40, 17, 14, 40, Blocks.AIR);                        // porte
        r.fill(25, 13, 50, 25, 20, 50, Blocks.SCAFFOLDING);                // accès étage et hélipad
        r.set(25, 16, 50, Blocks.SCAFFOLDING);
        for (int u = 8; u <= 22; u += 3) {
            r.set(u, 17, 57, Blocks.RED_BED);
            r.set(u, 13, 57, Blocks.WHITE_BED);
        }
        r.chest(9, 13, 42, "offshore_supplies");
        r.chest(22, 17, 42, "military_crate");
        // hélipad (22 x 20) : H blanc, bordure jaune, balises
        r.fill(5, 21, 39, 28, 21, 60, Blocks.GREEN_CONCRETE);
        r.walls(5, 21, 39, 28, 21, 60, Blocks.YELLOW_CONCRETE);
        r.fill(12, 21, 44, 13, 21, 55, Blocks.WHITE_CONCRETE);
        r.fill(20, 21, 44, 21, 21, 55, Blocks.WHITE_CONCRETE);
        r.fill(14, 21, 49, 19, 21, 50, Blocks.WHITE_CONCRETE);
        r.set(25, 21, 50, Blocks.SCAFFOLDING);
        for (int[] c : new int[][]{{5, 39}, {28, 39}, {5, 60}, {28, 60}}) {
            r.set(c[0], 22, c[1], Blocks.REDSTONE_LAMP);
        }
        // salle de commandement (boss) et salle des serveurs
        r.fill(36, 13, 40, 50, 17, 52, Blocks.GRAY_CONCRETE);
        r.fill(37, 13, 41, 49, 16, 51, Blocks.AIR);
        r.fill(43, 13, 40, 44, 14, 40, Blocks.AIR);
        for (int u = 38; u <= 48; u += 2) {
            r.set(u, 15, 52, Blocks.GLASS);
        }
        r.set(40, 13, 49, Blocks.CARTOGRAPHY_TABLE);
        r.set(46, 13, 49, Blocks.LECTERN);
        r.fill(36, 13, 54, 50, 17, 60, Blocks.BLACK_CONCRETE);
        r.fill(37, 13, 55, 49, 16, 59, Blocks.AIR);
        for (int u = 38; u <= 48; u += 2) {
            r.fill(u, 13, 56, u, 15, 58, Blocks.POLISHED_BLACKSTONE);       // baies de serveurs
            r.set(u, 16, 57, Blocks.SEA_LANTERN);
        }
        r.fill(43, 13, 53, 44, 14, 54, Blocks.AIR);
        r.bot(42, 13, 46, f, BotRole.OFFICER, 6);
        r.bot(42, 13, 46, f, BotRole.BOSS, 6);                              // Blackwater-01
        int[] vault = r.d(43, 46);
        BossVaults.build(r.b, vault[0], 13, vault[1], 8, false, BossCardItem.VaultType.OFFSHORE_VAULT);
        // défense : M2 aux quatre coins, gardes, tireurs d'élite sur l'hélipad
        r.turret(3, 13, 3, MountedGunEntity.Kind.M2, 135, f);
        r.turret(60, 13, 3, MountedGunEntity.Kind.M2, -135, f);
        r.turret(3, 13, 60, MountedGunEntity.Kind.M2, 45, f);
        r.turret(60, 13, 60, MountedGunEntity.Kind.M2, -45, f);
        r.pair(20, 13, 20, f, 16);
        r.pair(44, 13, 28, f, 16);
        r.pair(30, 13, 50, f, 12);
        r.pair(20, 2, 64, f, 10);
        r.bot(10, 22, 45, f, BotRole.SNIPER, 4);
        r.bot(24, 22, 56, f, BotRole.SNIPER, 4);
        r.chest(60, 13, 30, "offshore_supplies");
        r.chest(2, 2, 62, "offshore_supplies");
        // embarcations au quai sud : semi-rigide en patrouille, intercepteur et drone explosif amarrés
        float east = r.yaw(1, 0);
        r.ship(14, 76, MilType.RHIB, east, f, 2, false, 32, 32);
        r.ship(34, 76, MilType.INTERCEPTOR_BOAT, east, f, 0, true, 32, 32);
        r.ship(52, 75, MilType.EXPLOSIVE_USV, east, f, 0, true, 32, 32);
    }

    // ================================================================== porte-avions au mouillage
    private void carrier(R r) {
        boolean kuz = soviet;
        int L = kuz ? 120 : 130, W = kuz ? 30 : 34, c = 17;
        Faction f = kuz ? Faction.SOVIET : Faction.GARRISON;
        BlockState hull = st(Blocks.GRAY_CONCRETE), red = st(Blocks.RED_CONCRETE), black = st(Blocks.BLACK_CONCRETE);
        BlockState deck = st(kuz ? Blocks.GRAY_CONCRETE : Blocks.CYAN_TERRACOTTA), air = st(Blocks.AIR);
        for (int x = 0; x < L; x++) {
            double bow = x > L - 28 ? Math.sqrt(Math.max(0.02, (L - x) / 28.0)) : 1;
            int dw = Math.max(1, (int) Math.round(W / 2.0 * Math.min(1, bow * 1.15)));     // pont d'envol (débordant)
            int hw = Math.max(1, (int) Math.round((W / 2.0 - 4) * bow));                    // coque
            if (ox + x < r.b.clip.minX() || ox + x > r.b.clip.maxX()) {
                continue;
            }
            // œuvres vives (rouge antifouling), flottaison noire, coque grise ; intérieur vidé de l'eau
            r.fill(x, -9, c - hw, x, -9, c + hw, red);
            r.fill(x, -8, c - hw, x, -2, c - hw, red);
            r.fill(x, -8, c + hw, x, -2, c + hw, red);
            r.fill(x, -1, c - hw, x, 0, c - hw, black);
            r.fill(x, -1, c + hw, x, 0, c + hw, black);
            r.fill(x, 1, c - hw, x, 3, c - hw, hull);
            r.fill(x, 1, c + hw, x, 3, c + hw, hull);
            r.fill(x, -8, c - hw + 1, x, 3, c + hw - 1, air);
            r.fill(x, 4, c - hw, x, 4, c + hw, hull);                       // pont du hangar
            r.fill(x, 4, c - dw, x, 11, c - dw, hull);
            r.fill(x, 4, c + dw, x, 11, c + dw, hull);
            r.fill(x, 5, c - dw + 1, x, 11, c + dw - 1, air);
            if (x == 0 || x == L - 1) {
                r.fill(x, 4, c - dw, x, 11, c + dw, hull);
            }
            r.fill(x, 4, c - dw, x, 4, c - hw, hull);
            r.fill(x, 4, c + hw, x, 4, c + dw, hull);
            int top = 12;
            if (kuz && x > L - 24) {
                // tremplin du Kouznetsov : un demi-bloc tous les 2 blocs (l'avion monte sans heurter de marche)
                int half = (x - (L - 24)) / 2;
                top = 12 + half / 2;
                r.fill(x, 12, c - dw, x, top, c + dw, hull);
                if (half % 2 == 1) {
                    r.fill(x, top + 1, c - dw, x, top + 1, c + dw, B.slab(Blocks.SMOOTH_STONE_SLAB, false));
                }
            }
            r.fill(x, top, c - dw, x, top, c + dw, deck);
            if (x % 6 < 3) {
                r.set(x, top, c, Blocks.WHITE_CONCRETE);                    // axe du pont
            }
        }
        // marquages : piste oblique, catapultes (Ford) ou silos Granit (Kouznetsov), brins d'arrêt
        for (int x = 4; x < L * 0.55; x++) {
            int z = c - 8 + (int) ((x - 4) * 0.14);
            r.set(x, 12, z, Blocks.YELLOW_CONCRETE);
        }
        for (int x = 8; x <= 26; x += 6) {
            r.fill(x, 12, c - 8, x, 12, c + 4, Blocks.BLACK_CONCRETE);
        }
        if (!kuz) {
            for (int z : new int[]{c - 5, c + 3}) {
                r.fill(L - 70, 12, z, L - 6, 12, z, Blocks.POLISHED_BLACKSTONE);   // catapultes EMALS
            }
        } else {
            for (int x = L - 40; x < L - 28; x += 2) {
                for (int z = c - 4; z <= c + 4; z += 4) {
                    r.set(x, 12, z, Blocks.IRON_TRAPDOOR.defaultBlockState()          // trappes des 12 P-700 Granit, à fleur du pont
                            .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
                }
            }
        }
        // îlot à tribord : passerelle vitrée, radars, mât
        int ix = (int) (L * (kuz ? 0.52 : 0.6)), il = kuz ? 20 : 15, iz = c + (W / 2) - 7;
        r.fill(ix, 13, iz, ix + il, 28, iz + 5, Blocks.LIGHT_GRAY_CONCRETE);
        r.fill(ix + 1, 13, iz + 1, ix + il - 1, 27, iz + 4, Blocks.AIR);
        for (int y = 15; y <= 27; y += 4) {
            r.fill(ix, y, iz, ix + il, y, iz, Blocks.GLASS);
            r.fill(ix, y, iz + 5, ix + il, y, iz + 5, Blocks.GLASS);
            r.fill(ix + 1, y - 2, iz + 1, ix + il - 1, y - 2, iz + 4, Blocks.LIGHT_GRAY_CONCRETE);
        }
        r.fill(ix + 2, 13, iz, ix + 3, 14, iz, Blocks.AIR);
        r.fill(ix + 4, 13, iz + 2, ix + 4, 27, iz + 2, Blocks.SCAFFOLDING);
        r.fill(ix + il / 2, 29, iz + 2, ix + il / 2, 38, iz + 2, Blocks.IRON_BARS);
        r.fill(ix + il / 2 - 2, 33, iz + 1, ix + il / 2 + 2, 33, iz + 3, Blocks.GRAY_CONCRETE);
        r.fill(ix + 1, 24, iz - 1, ix + 3, 26, iz - 1, Blocks.GRAY_CONCRETE);      // panneaux radar
        r.fill(ix + il - 3, 24, iz - 1, ix + il - 1, 26, iz - 1, Blocks.GRAY_CONCRETE);
        r.chest(ix + 2, 13, iz + 3, "military_base");
        // défense rapprochée (Phalanx / Kortik) aux quatre coins du pont
        float out = r.yaw(0, 1);
        r.turret(4, 13, c - 14, MountedGunEntity.Kind.FLAK, r.yaw(0, -1), f);
        r.turret(4, 13, c + 14, MountedGunEntity.Kind.FLAK, out, f);
        r.turret(L - 34, 13, c - 12, MountedGunEntity.Kind.FLAK, r.yaw(0, -1), f);
        r.turret(L - 34, 13, c + 12, MountedGunEntity.Kind.FLAK, out, f);
        // chasseurs sur le pont (côté bâbord), nez vers l'étrave
        float bowYaw = r.yaw(1, 0);
        int n = 4 + r.b.r.nextInt(3);
        MilType[] pool = kuz ? new MilType[]{MilType.MIG_29, MilType.SU_57} : new MilType[]{MilType.F_16C, MilType.F_22};
        // garés en épi le long du bord bâbord ; catapultes sur l'axe dégagé du pont
        for (int i = 0; i < n; i++) {
            int x = 10 + i * 14;
            int launchX = kuz ? L - 64 : L - 72, launchZ = kuz ? c + (i % 2 == 0 ? -3 : 3) : (i % 2 == 0 ? c - 5 : c + 3);
            r.jet(x, 13, c - (W / 2) + 5, pool[i % 2], bowYaw, launchX - (i / 2) * 2, launchZ);
        }
        // équipage de pont, escorte
        r.pair(30, 13, c, f, 30);
        r.pair(70, 13, c + 4, f, 30);
        r.bot(ix + 2, 17, iz + 2, f, BotRole.OFFICER, 4);
        r.bot(ix + 6, 21, iz + 2, f, BotRole.SOLDIER, 4);
        MilType esc1 = kuz ? MilType.SLAVA : MilType.TICONDEROGA, esc2 = kuz ? MilType.UDALOY : MilType.ARLEIGH_BURKE;
        r.ship(L / 2, c - 45, esc1, bowYaw, f, 5, false, L / 2, c);
        r.ship(L / 2 - 20, c + 45, esc2, bowYaw, f, 5, false, L / 2, c);
    }

    // ================================================================== base navale côtière
    private void navalBase(R r) {
        Faction f = soviet ? Faction.SOVIET : Faction.GARRISON;
        BlockState quay = st(Blocks.SMOOTH_STONE), fillS = st(Blocks.STONE), brick = st(Blocks.STONE_BRICKS);
        MountedGunEntity.Kind mg = soviet ? MountedGunEntity.Kind.DSHK : MountedGunEntity.Kind.M2;
        float sea = r.yaw(0, 1);
        // terre-plein du quai (le relief est rasé, les creux comblés)
        r.flatten(0, -60, 159, -1, 2, quay, fillS);
        r.fill(0, 2, -1, 159, 2, -1, Blocks.POLISHED_ANDESITE);
        for (int u = 4; u < 160; u += 8) {
            r.set(u, 3, -2, Blocks.STONE_BRICK_WALL);                       // bittes d'amarrage
        }
        // allées
        r.fill(0, 2, -30, 159, 2, -28, Blocks.GRAY_CONCRETE);
        // 3 jetées sur pilotis
        for (int u0 : new int[]{18, 58, 98}) {
            r.fill(u0, 2, 0, u0 + 6, 2, 80, quay);
            for (int v = 0; v <= 80; v += 6) {
                r.pillar(u0, v, 1, brick);
                r.pillar(u0 + 6, v, 1, brick);
            }
            for (int v = 4; v <= 80; v += 8) {
                r.set(u0, 3, v, Blocks.STONE_BRICK_WALL);
                r.set(u0 + 6, 3, v, Blocks.STONE_BRICK_WALL);
            }
            r.set(u0 + 3, 3, 80, Blocks.LANTERN);
            r.turret(u0 + 3, 3, 78, mg, sea, f);
        }
        // cale sèche couverte (50 x 22 x 16) ouverte sur le port
        int du0 = 128, du1 = 149, dv0 = -50, dv1 = 1;
        r.fill(du0, -12, dv0, du1, 2, dv1, brick);
        r.fill(du0 + 2, -10, dv0 + 2, du1 - 2, -1, dv1, Blocks.WATER);
        r.fill(du0 + 2, 0, dv0 + 2, du1 - 2, 2, dv1, Blocks.AIR);
        r.fill(du0, 3, dv0, du1, 16, dv0, Blocks.LIGHT_GRAY_CONCRETE);
        r.fill(du0, 3, dv0, du0, 16, dv1, Blocks.LIGHT_GRAY_CONCRETE);
        r.fill(du1, 3, dv0, du1, 16, dv1, Blocks.LIGHT_GRAY_CONCRETE);
        r.fill(du0, 3, dv0 + 1, du1 - 1, 15, dv1 - 1, Blocks.AIR);
        r.fill(du0, 3, dv0 + 1, du0, 15, dv1, Blocks.LIGHT_GRAY_CONCRETE);
        r.fill(du0, 17, dv0, du1, 17, dv1, Blocks.GRAY_CONCRETE);
        for (int v = dv0 + 4; v < dv1; v += 6) {
            r.fill(du0 + 3, 17, v, du1 - 3, 17, v + 1, Blocks.GLASS);       // verrières
        }
        r.fill(du0, 13, dv1, du1, 16, dv1, Blocks.LIGHT_GRAY_CONCRETE);    // linteau de la porte
        r.fill(du0 + 1, 3, dv0 + 1, du0 + 1, 3, dv1 - 1, Blocks.IRON_BARS);
        r.fill(du1 - 1, 3, dv0 + 1, du1 - 1, 3, dv1 - 1, Blocks.IRON_BARS);
        r.fill(du0 + 1, 2, dv0 + 1, du0 + 1, 2, dv1, quay);                // coursives
        r.fill(du1 - 1, 2, dv0 + 1, du1 - 1, 2, dv1, quay);
        for (int v = dv0 + 6; v < dv1; v += 10) {
            r.set(du0 + 1, 14, v, Blocks.SEA_LANTERN);
            r.set(du1 - 1, 14, v, Blocks.SEA_LANTERN);
        }
        r.ship((du0 + du1) / 2, -22, soviet ? MilType.BUYAN : MilType.VIRGINIA, sea, f, 3, true, 80, 70);
        // portiques de manutention
        for (int u0 : new int[]{32, 72}) {
            for (int[] k : new int[][]{{0, -14}, {0, -4}, {10, -14}, {10, -4}}) {
                r.fill(u0 + k[0], 3, k[1], u0 + k[0], 16, k[1], Blocks.YELLOW_CONCRETE);
            }
            r.fill(u0, 17, -14, u0 + 10, 17, -4, Blocks.YELLOW_CONCRETE);
            r.fill(u0 + 1, 18, -9, u0 + 9, 18, 12, Blocks.YELLOW_CONCRETE); // flèche au-dessus de l'eau
            r.fill(u0 + 5, 6, 10, u0 + 5, 17, 10, Blocks.CHAIN);
        }
        // dépôt de carburant (citernes + kérosène pour le ravitaillement des navires à quai)
        tank(r, 64, -44, 3, 9, 4, Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE);
        tank(r, 76, -44, 3, 9, 4, Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE);
        tank(r, 88, -44, 3, 9, 4, Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE);
        for (int u : new int[]{30, 70, 110}) {
            r.set(u, 3, -3, ModBlocks.KEROSENE_TANK.get());
            r.set(u + 2, 3, -3, ModBlocks.AVIATION_AMMO_CRATE.get());
        }
        // dépôt de torpilles et de munitions (bunker)
        r.fill(100, 3, -48, 118, 8, -36, brick);
        r.fill(101, 3, -47, 117, 7, -37, Blocks.AIR);
        r.fill(108, 3, -36, 110, 5, -36, Blocks.AIR);
        for (int u = 102; u <= 116; u += 2) {
            r.chest(u, 3, -46, "naval_depot");
        }
        r.set(103, 7, -40, Blocks.LANTERN);
        // QG et caserne
        r.fill(4, 3, -56, 26, 10, -38, soviet ? Blocks.RED_TERRACOTTA : Blocks.WHITE_CONCRETE);
        r.fill(5, 3, -55, 25, 9, -39, Blocks.AIR);
        r.fill(5, 6, -55, 25, 6, -39, Blocks.SPRUCE_PLANKS);
        r.fill(14, 3, -38, 16, 5, -38, Blocks.AIR);
        for (int u = 6; u <= 24; u += 3) {
            r.set(u, 4, -56, Blocks.GLASS);
            r.set(u, 8, -56, Blocks.GLASS);
            r.set(u, 7, -54, Blocks.RED_BED);
        }
        r.fill(24, 3, -40, 24, 6, -40, Blocks.SCAFFOLDING);
        r.chest(6, 3, -54, "military_base");
        r.chest(6, 7, -40, "military_crate");
        r.bot(15, 3, -46, f, BotRole.OFFICER, 10);
        // batteries côtières (lance-roquettes tournés vers le large) et défense aérienne
        int[] o = r.d(0, 0), s1 = r.d(0, 1);
        int outX = s1[0] - o[0], outZ = s1[1] - o[1];
        for (int u : new int[]{3, 155}) {
            int[] p = r.d(u, -6);
            r.b.rocketBattery(p[0], 3, p[1], outX, outZ);
        }
        if (soviet) {
            int[] p = r.d(140, -58);
            r.b.strategic(p[0], 3, p[1], MilType.S_400, sea);
        }
        r.turret(1, 3, -2, mg, sea, f);
        r.turret(158, 3, -2, mg, sea, f);
        // garde
        r.pair(40, 3, -20, f, 30);
        r.pair(90, 3, -25, f, 30);
        r.pair(120, 3, -10, f, 20);
        r.pair(60, 3, 40, f, 20);
        r.bot(10, 11, -47, f, BotRole.SNIPER, 4);
        // navires à quai, étrave vers le large
        if (soviet) {
            r.ship(41, 44, MilType.UDALOY, sea, f, 4, true, 80, 70);
            r.ship(81, 44, MilType.SLAVA, sea, f, 4, true, 80, 70);
        } else {
            r.ship(41, 44, MilType.ARLEIGH_BURKE, sea, f, 4, true, 80, 70);
            r.ship(81, 44, MilType.TICONDEROGA, sea, f, 4, true, 80, 70);
        }
        r.ship(118, 45, soviet ? MilType.BUYAN : MilType.ZUMWALT, sea, f, 4, true, 80, 70);
    }
}
