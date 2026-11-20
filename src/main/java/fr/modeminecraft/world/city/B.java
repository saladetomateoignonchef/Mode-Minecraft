package fr.modeminecraft.world.city;

import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Outil de construction procédurale : coordonnées locales à l'origine du bâtiment (x, z) et au niveau de la rue (y = 0),
 * blocs posés uniquement dans le chunk en cours de génération (les bâtiments sur plusieurs chunks sont construits
 * morceau par morceau, avec le même hasard).
 */
public final class B {
    final WorldGenLevel level;
    final BoundingBox clip;
    final int ox, oy, oz, sea;
    final RandomSource r;
    /** Hasard séparé pour les bots et le butin (ne décale pas le hasard des bâtiments sur plusieurs chunks). */
    final RandomSource er = RandomSource.create();
    /** Position de ce chunk dans le bâtiment (en chunks) et taille du bâtiment (blocs). */
    int px, pz, w = 16, d = 16;

    B(WorldGenLevel level, BoundingBox clip, int ox, int oy, int oz, RandomSource r, int sea) {
        this.level = level;
        this.clip = clip;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.r = r;
        this.sea = sea;
    }

    void part(int px, int pz, int w, int d) {
        this.px = px;
        this.pz = pz;
        this.w = w;
        this.d = d;
    }

    BlockPos pos(int x, int y, int z) {
        return new BlockPos(ox + x, oy + y, oz + z);
    }

    boolean inside(int x, int y, int z) {
        return clip.isInside(ox + x, oy + y, oz + z);
    }

    void set(int x, int y, int z, BlockState s) {
        if (inside(x, y, z)) {
            level.setBlock(pos(x, y, z), s, 2);
        }
    }

    void set(int x, int y, int z, Block b) {
        set(x, y, z, b.defaultBlockState());
    }

    BlockState get(int x, int y, int z) {
        return inside(x, y, z) ? level.getBlockState(pos(x, y, z)) : Blocks.AIR.defaultBlockState();
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        int ax = Math.max(Math.min(x0, x1), clip.minX() - ox), bx = Math.min(Math.max(x0, x1), clip.maxX() - ox);
        int az = Math.max(Math.min(z0, z1), clip.minZ() - oz), bz = Math.min(Math.max(z0, z1), clip.maxZ() - oz);
        for (int x = ax; x <= bx; x++) {
            for (int z = az; z <= bz; z++) {
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                    set(x, y, z, s);
                }
            }
        }
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        fill(x0, y0, z0, x1, y1, z1, b.defaultBlockState());
    }

    /** Murs (4 côtés) d'un pavé. */
    void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        fill(x0, y0, z0, x1, y1, z0, s);
        fill(x0, y0, z1, x1, y1, z1, s);
        fill(x0, y0, z0, x0, y1, z1, s);
        fill(x1, y0, z0, x1, y1, z1, s);
    }

    void walls(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        walls(x0, y0, z0, x1, y1, z1, b.defaultBlockState());
    }

    /**
     * Prépare le sol de la parcelle : fondations jusqu'au terrain (vallées comblées), relief dégagé au-dessus
     * (jusqu'à `clear` blocs), puis le revêtement en y = -1.
     */
    void ground(int x0, int z0, int x1, int z1, BlockState surface, BlockState under, int clear) {
        int ax = Math.max(x0, clip.minX() - ox), bx = Math.min(x1, clip.maxX() - ox);
        int az = Math.max(z0, clip.minZ() - oz), bz = Math.min(z1, clip.maxZ() - oz);
        for (int x = ax; x <= bx; x++) {
            for (int z = az; z <= bz; z++) {
                for (int y = 0; y <= clear; y++) {
                    BlockPos p = pos(x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                set(x, -1, z, surface);
                for (int y = -2; y > -24; y--) {
                    BlockState s = level.getBlockState(pos(x, y, z));
                    if (!s.isAir() && !s.canBeReplaced() && s.getFluidState().isEmpty() && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS)) {
                        break;
                    }
                    set(x, y, z, under);
                }
            }
        }
    }

    // ------------------------------------------------------------------ blocs orientés
    static BlockState stairs(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    static BlockState stairsTop(Block b, Direction facing) {
        return stairs(b, facing).setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
    }

    static BlockState slab(Block b, boolean top) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    static BlockState facing(Block b, Direction dir) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return s.setValue(HorizontalDirectionalBlock.FACING, dir);
        }
        if (s.hasProperty(BlockStateProperties.FACING)) {
            return s.setValue(BlockStateProperties.FACING, dir);
        }
        return s;
    }

    static BlockState lit(Block b) {
        BlockState s = b.defaultBlockState();
        return s.hasProperty(BlockStateProperties.LIT) ? s.setValue(BlockStateProperties.LIT, true) : s;
    }

    /** Volets / vitres connectés : pose simple (le jeu recalcule les connexions au chargement). */
    void pane(int x, int y, int z, Block b) {
        set(x, y, z, b.defaultBlockState());
    }

    // ------------------------------------------------------------------ coffres, bots, tourelles
    /** Coffre (ou tonneau) rempli par une table de butin du mod (loot_table/chests/<nom>.json). */
    void chest(int x, int y, int z, Direction facing, String loot, boolean barrel) {
        if (!inside(x, y, z)) {
            return;
        }
        BlockState s = barrel ? Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP)
                : Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        set(x, y, z, s);
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, fr.modeminecraft.ModeMinecraft.id("chests/" + loot));
        RandomizableContainer.setBlockEntityLootTable(level, er, pos(x, y, z), key);
    }

    /** Bot de faction (posé seulement par le chunk qui contient sa position). */
    HumanBotEntity bot(int x, int y, int z, Faction f, BotRole role, int homeRadius) {
        if (!inside(x, y, z)) {
            return null;
        }
        HumanBotEntity bot = ModEntities.HUMAN_BOT.get().create(level.getLevel());
        if (bot == null) {
            return null;
        }
        BlockPos p = pos(x, y, z);
        bot.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, er.nextFloat() * 360, 0);
        bot.setup(f, role, er);
        bot.home = p;
        bot.homeRadius = homeRadius;
        bot.setPersistenceRequired();
        bot.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(bot);
        return bot;
    }

    /** Patrouille en binôme : le second suit le premier. */
    void pair(int x, int y, int z, Faction f, int homeRadius) {
        HumanBotEntity a = bot(x, y, z, f, r.nextFloat() < 0.3f ? BotRole.OFFICER : BotRole.SOLDIER, homeRadius);
        HumanBotEntity b = bot(x + 1, y, z, f, BotRole.SOLDIER, homeRadius);
        if (a != null && b != null) {
            b.partner = a.getUUID();
        }
    }

    /** Tourelle fixe déjà déployée, avec son servant. */
    void turret(int x, int y, int z, MountedGunEntity.Kind kind, float yaw, Faction f) {
        if (!inside(x, y, z)) {
            return;
        }
        BlockPos p = pos(x, y, z);
        MountedGunEntity t = new MountedGunEntity(level.getLevel(), kind, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, kind.capacity);
        t.deployNow();
        HumanBotEntity g = ModEntities.HUMAN_BOT.get().create(level.getLevel());
        if (g != null) {
            g.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0);
            g.setup(f, BotRole.GUNNER, er);
            g.setPersistenceRequired();
            g.home = p;
            g.startRiding(t, true);
        }
        level.addFreshEntityWithPassengers(t);
    }

    /** Véhicule garé (posé seulement par le chunk qui contient sa position). */
    void vehicle(int x, int y, int z, fr.modeminecraft.entity.VehicleEntity.Type type, float yaw) {
        if (!inside(x, y, z)) {
            return;
        }
        BlockPos p = pos(x, y, z);
        level.addFreshEntity(new fr.modeminecraft.entity.VehicleEntity(level.getLevel(), type, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw));
    }

    int nextInt(int n) {
        return r.nextInt(n);
    }

    boolean chance(float p) {
        return r.nextFloat() < p;
    }
}
