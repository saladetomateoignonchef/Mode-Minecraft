package fr.modeminecraft.entity;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.FalconSquads;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Largage aérien : une caisse qui descend sous parachute (traînée de fumée), puis se pose en coffre de butin rare
 * avec une fumée de signalisation. Les Faucons Blancs et la faction locale convergent vers elle et s'affrontent.
 */
public class AirdropEntity extends Entity {
    public AirdropEntity(EntityType<? extends AirdropEntity> type, Level level) {
        super(type, level);
    }

    public AirdropEntity(Level level, double x, double y, double z) {
        this(ModEntities.AIRDROP.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        double sway = Math.sin(tickCount * 0.05) * 0.02;
        setDeltaMovement(sway, isInWater() ? 0.02 : -0.18, Math.cos(tickCount * 0.04) * 0.02);
        move(MoverType.SELF, getDeltaMovement());
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() - 0.2, getZ(), 0, -0.02, 0);
            return;
        }
        if (onGround() || isInWater() || tickCount > 2400) {
            land((ServerLevel) level());
        }
    }

    private void land(ServerLevel level) {
        BlockPos p = blockPosition();
        if (!level.getBlockState(p).canBeReplaced()) {
            p = p.above();
        }
        level.setBlock(p, Blocks.CHEST.defaultBlockState(), 3);
        RandomizableContainer.setBlockEntityLootTable(level, random, p,
                ResourceKey.create(Registries.LOOT_TABLE, fr.modeminecraft.ModeMinecraft.id("chests/airdrop")));
        addMatchingAmmo(level, p);
        // fumée de signalisation (feu de camp sur une botte de foin : colonne de fumée très haute)
        BlockPos s = p.east(2);
        if (level.getBlockState(s).canBeReplaced() && !level.getBlockState(s.below()).canBeReplaced()) {
            level.setBlock(s.below(), Blocks.HAY_BLOCK.defaultBlockState(), 3);
            level.setBlock(s, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.SIGNAL_FIRE, true), 3);
        }
        level.playSound(null, p, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5f, 0.6f);
        battle(level, p);
        discard();
    }

    /**
     * Cohérence du butin : le coffre est tiré tout de suite, et chaque arme à feu tirée au sort reçoit dans le même
     * coffre les munitions exactes qui lui correspondent (même type de balle, quelques chargeurs).
     */
    private static void addMatchingAmmo(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
            return;
        }
        chest.unpackLootTable(null);
        var wanted = new java.util.LinkedHashMap<net.minecraft.world.item.Item, Integer>();
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).getItem() instanceof fr.modeminecraft.item.gun.GunItem gun) {
                var stats = gun.stats();
                int perMag = Math.max(1, stats.magazineSize());
                int count = stats.disposable() ? 0 : Math.min(64, Math.max(perMag * 2, 3));
                if (count > 0) {
                    wanted.merge(stats.ammo().get(), count, Math::max);
                }
            }
        }
        for (var entry : wanted.entrySet()) {
            int have = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (chest.getItem(i).is(entry.getKey())) {
                    have += chest.getItem(i).getCount();
                }
            }
            int missing = entry.getValue() - have;
            for (int i = 0; i < chest.getContainerSize() && missing > 0; i++) {
                var s = chest.getItem(i);
                if (s.isEmpty()) {
                    int n = Math.min(missing, entry.getKey().getDefaultMaxStackSize());
                    chest.setItem(i, new net.minecraft.world.item.ItemStack(entry.getKey(), n));
                    missing -= n;
                } else if (s.is(entry.getKey()) && s.getCount() < s.getMaxStackSize()
                        && fr.modeminecraft.item.AmmoItem.getType(s) == fr.modeminecraft.item.gun.AmmoType.STANDARD) {
                    int n = Math.min(missing, s.getMaxStackSize() - s.getCount());
                    s.grow(n);
                    missing -= n;
                }
            }
        }
        chest.setChanged();
    }

    /** Les Faucons Blancs et la faction locale se disputent la caisse. */
    private static void battle(ServerLevel level, BlockPos crate) {
        var r = level.getRandom();
        double a = r.nextDouble() * Math.PI * 2;
        int fx = crate.getX() + (int) (Math.cos(a) * 45), fz = crate.getZ() + (int) (Math.sin(a) * 45);
        BlockPos falcons = new BlockPos(fx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fx, fz), fz);
        if (level.isLoaded(falcons)) {
            for (HumanBotEntity f : FalconSquads.spawnSquad(level, falcons)) {
                if (f.squadLeader) {
                    f.march = crate;
                }
            }
        }
        Faction local = localFaction(level, crate);
        int lx = crate.getX() - (int) (Math.cos(a) * 35), lz = crate.getZ() - (int) (Math.sin(a) * 35);
        for (int i = 0; i < 5; i++) {
            int x = lx + r.nextInt(5) - 2, z = lz + r.nextInt(5) - 2;
            BlockPos q = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.isLoaded(q)) {
                continue;
            }
            HumanBotEntity bot = ModEntities.HUMAN_BOT.get().create(level);
            if (bot == null) {
                continue;
            }
            bot.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, 0, 0);
            bot.setup(local, i == 0 ? BotRole.OFFICER : BotRole.SOLDIER, r);
            bot.home = crate;
            bot.homeRadius = 6;
            bot.finalizeSpawn(level, level.getCurrentDifficultyAt(q), MobSpawnType.EVENT, null);
            level.addFreshEntity(bot);
        }
    }

    public static Faction localFaction(ServerLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return Faction.REFINERY;
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return Faction.CARTEL;
        }
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA)) {
            return Faction.GARRISON;
        }
        if (biome.is(net.neoforged.neoforge.common.Tags.Biomes.IS_SNOWY)) {
            return Faction.SOVIET;
        }
        if (biome.is(net.neoforged.neoforge.common.Tags.Biomes.IS_DESERT)) {
            return Faction.MILITIA;
        }
        return Faction.SWAT;
    }

    /** Fait apparaître un largage à 60-100 blocs du point donné et prévient les joueurs proches. */
    public static AirdropEntity call(ServerLevel level, Vec3 near) {
        var r = level.getRandom();
        double a = r.nextDouble() * Math.PI * 2, d = 60 + r.nextInt(40);
        int x = (int) (near.x + Math.cos(a) * d), z = (int) (near.z + Math.sin(a) * d);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        AirdropEntity drop = new AirdropEntity(level, x + 0.5, Math.min(level.getMaxBuildHeight() - 5, ground + 90), z + 0.5);
        level.addFreshEntity(drop);
        for (var p : level.players()) {
            if (p.distanceToSqr(x, ground, z) < 300 * 300) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.airdrop", x, z)
                        .withStyle(net.minecraft.ChatFormatting.GOLD), false);
            }
        }
        return drop;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }
}
