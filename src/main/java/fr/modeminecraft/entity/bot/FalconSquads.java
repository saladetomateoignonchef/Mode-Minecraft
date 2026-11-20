package fr.modeminecraft.entity.bot;

import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La Troupe des Faucons Blancs : escouades nomades de 15 combattants (100 % corps à corps), hostiles à tous.
 * 1 Commandant + 1 Chef des Troupes de Choc + 5 éclaireurs + 4 lanciers + 2 assassins + 2 brutes.
 */
public final class FalconSquads {
    public static final List<BotRole> COMPOSITION = List.of(BotRole.COMMANDER, BotRole.CHIEF,
            BotRole.SCOUT, BotRole.SCOUT, BotRole.SCOUT, BotRole.SCOUT, BotRole.SCOUT,
            BotRole.LANCER, BotRole.LANCER, BotRole.LANCER, BotRole.LANCER,
            BotRole.ASSASSIN, BotRole.ASSASSIN, BotRole.BRUTE, BotRole.BRUTE);

    /** Équipement d'un Faucon Blanc selon son rôle (armures les plus lourdes du mod, aucune arme à feu). */
    static void equip(HumanBotEntity bot, BotRole role, RandomSource r) {
        String weapon = switch (role) {
            case COMMANDER -> r.nextBoolean() ? "claymore" : "war_hammer";
            case SCOUT -> r.nextBoolean() ? "katana" : "wakizashi";
            case LANCER -> r.nextBoolean() ? "lance" : "halberd";
            case ASSASSIN -> "karambit";
            case BRUTE -> r.nextBoolean() ? "mace" : "war_scythe";
            default -> null;                                     // le Chef porte son épée dans le dos
        };
        if (weapon != null) {
            bot.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HumanBotEntity.item(weapon)));
        }
        String helmet = switch (role) {
            case CHIEF, BRUTE -> "great_helm";
            case COMMANDER, LANCER -> "altyn";
            case SCOUT -> r.nextBoolean() ? "ops_core_fast" : null;
            default -> null;
        };
        if (helmet != null) {
            bot.setItemSlot(EquipmentSlot.HEAD, new ItemStack(HumanBotEntity.item(helmet)));
        }
        bot.setItemSlot(EquipmentSlot.CHEST, new ItemStack(HumanBotEntity.item(
                role == BotRole.SCOUT || role == BotRole.ASSASSIN ? "jpc_plate_carrier" : "fort_defender")));
        bot.setItemSlot(EquipmentSlot.LEGS, new ItemStack(HumanBotEntity.item(r.nextBoolean() ? "crye_g3_pants" : "ratnik_pants")));
        bot.setItemSlot(EquipmentSlot.FEET, new ItemStack(HumanBotEntity.item(r.nextBoolean() ? "salomon_forces_boots" : "lowa_zephyr_boots")));
        for (EquipmentSlot s : EquipmentSlot.values()) {
            bot.setDropChance(s, s == EquipmentSlot.MAINHAND ? 0.1f : 0.04f);
        }
    }

    /** Le chef d'escouade de ce Faucon (ou null s'il est tombé). */
    static HumanBotEntity leaderOf(HumanBotEntity bot) {
        for (HumanBotEntity o : bot.level().getEntitiesOfClass(HumanBotEntity.class, bot.getBoundingBox().inflate(96),
                o -> o.squadLeader && bot.squad.equals(o.squad) && o.isAlive())) {
            return o;
        }
        return null;
    }

    /** Fait apparaître une escouade complète autour de ce point (au sol). */
    public static List<HumanBotEntity> spawnSquad(ServerLevel level, BlockPos center) {
        UUID id = UUID.randomUUID();
        List<HumanBotEntity> out = new ArrayList<>();
        RandomSource r = level.getRandom();
        for (int i = 0; i < COMPOSITION.size(); i++) {
            BotRole role = COMPOSITION.get(i);
            double a = i * Math.PI * 2 / COMPOSITION.size();
            int x = center.getX() + (int) Math.round(Math.cos(a) * (i == 0 ? 0 : 4)), z = center.getZ() + (int) Math.round(Math.sin(a) * (i == 0 ? 0 : 4));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            HumanBotEntity bot = ModEntities.HUMAN_BOT.get().create(level);
            if (bot == null) {
                continue;
            }
            bot.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360, 0);
            bot.setup(Faction.FALCON, role, r);
            bot.squad = id;
            bot.squadLeader = role == BotRole.COMMANDER;
            bot.finalizeSpawn(level, level.getCurrentDifficultyAt(bot.blockPosition()), MobSpawnType.EVENT, null);
            level.addFreshEntity(bot);
            out.add(bot);
        }
        return out;
    }

    /**
     * Embuscade nocturne : l'escouade se poste en silence le long de l'itinéraire (route, passage d'un convoi),
     * chacun accroupi derrière un rocher, un arbre ou un mur, et attend que la proie arrive à portée.
     * @param approach direction d'où arrive la proie
     */
    public static List<HumanBotEntity> spawnAmbush(ServerLevel level, BlockPos center, net.minecraft.world.phys.Vec3 approach) {
        List<HumanBotEntity> squad = spawnSquad(level, center);
        net.minecraft.world.phys.Vec3 dir = approach.normalize();
        for (HumanBotEntity bot : squad) {
            bot.ambush = true;
            BlockPos hide = hidingSpot(level, bot.blockPosition(), dir);
            if (hide != null) {
                bot.moveTo(hide.getX() + 0.5, hide.getY(), hide.getZ() + 0.5, (float) (net.minecraft.util.Mth.atan2(-dir.x, dir.z) * net.minecraft.util.Mth.RAD_TO_DEG) + 180, 0);
            }
            bot.setStance(net.minecraft.world.entity.Pose.CROUCHING);
        }
        fr.modeminecraft.ModeMinecraft.LOGGER.info("Faucons Blancs : embuscade tendue en {}", center);
        return squad;
    }

    /** Un point au sol, près d'ici, avec un obstacle (rocher, tronc, feuillage, mur) entre lui et la proie. */
    static BlockPos hidingSpot(ServerLevel level, BlockPos near, net.minecraft.world.phys.Vec3 approach) {
        int sx = (int) Math.signum(Math.round(-approach.x * 2) / 2.0), sz = (int) Math.signum(Math.round(-approach.z * 2) / 2.0);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                int x = near.getX() + dx, z = near.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos p = new BlockPos(x, y, z);
                if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir() || !level.getFluidState(p.below()).isEmpty()) {
                    continue;
                }
                // l'obstacle doit être du côté d'où vient la proie
                BlockPos front = p.offset(sx, 0, sz);
                var fs = level.getBlockState(front);
                boolean cover = !fs.isAir() && (fs.isSolid() || fs.is(net.minecraft.tags.BlockTags.LEAVES) || fs.is(net.minecraft.tags.BlockTags.LOGS));
                if (!cover) {
                    continue;
                }
                double d = dx * dx + dz * dz;
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        return best;
    }

    /** Premier coup porté (ou reçu) : fini le silence, le cor sonne la charge et toute l'escouade attaque. */
    static void endAmbush(HumanBotEntity bot) {
        if (!bot.ambush || bot.squad == null) {
            return;
        }
        for (HumanBotEntity o : bot.level().getEntitiesOfClass(HumanBotEntity.class, bot.getBoundingBox().inflate(64),
                o -> bot.squad.equals(o.squad))) {
            o.ambush = false;
            o.setStance(net.minecraft.world.entity.Pose.STANDING);
            if (o.getTarget() == null && bot.getTarget() != null) {
                o.setTarget(bot.getTarget());
            }
        }
        BotEvents.horn(bot);
    }

    /** Butin : la Pourfendeuse de Dragons, les Cartes d'Accès Rouges et l'Insigne du Faucon Blanc. */
    static void dropLoot(HumanBotEntity bot, ServerLevel level) {
        if (bot.faction() != Faction.FALCON) {
            return;
        }
        RandomSource r = bot.getRandom();
        switch (bot.role()) {
            case CHIEF -> {
                bot.spawnAtLocation(new ItemStack(ModItems.DRAGONSLAYER.get()));
                bot.spawnAtLocation(new ItemStack(ModItems.RED_ACCESS_CARD.get()));
                bot.spawnAtLocation(new ItemStack(ModItems.FALCON_INSIGNIA.get()));
            }
            case COMMANDER -> {
                bot.spawnAtLocation(new ItemStack(ModItems.RED_ACCESS_CARD.get()));
                if (r.nextBoolean()) {
                    bot.spawnAtLocation(new ItemStack(ModItems.FALCON_INSIGNIA.get()));
                }
            }
            default -> {
                if (r.nextFloat() < 0.1f) {
                    bot.spawnAtLocation(new ItemStack(ModItems.FALCON_INSIGNIA.get()));
                }
            }
        }
    }

    private FalconSquads() {}
}
