package fr.modeminecraft.entity.bot;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Événements des factions :
 * - une faction prévenue (attaque, coup de feu entendu, coffre pillé) devient hostile et ses membres proches ripostent ;
 * - fusillade contre la police : une équipe du SWAT est déployée (4 agents, au plus toutes les 2 minutes) ;
 * - escouades nomades des Faucons Blancs qui sillonnent la carte autour des joueurs ;
 * - cri de ralliement des Faucons quand ils repèrent une cible.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class BotEvents {
    private static final Map<UUID, Long> SWAT_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> HORN_COOLDOWN = new HashMap<>();
    private static final List<Object[]> LATER = new ArrayList<>();

    /** Attributs de l'entité (bus du mod). */
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.HUMAN_BOT.get(), HumanBotEntity.createAttributes().build());
        event.put(ModEntities.PIGEON.get(), fr.modeminecraft.entity.city.PigeonEntity.createAttributes().build());
        event.put(ModEntities.DRONE.get(), fr.modeminecraft.entity.drone.DroneEntity.createAttributes().build());
    }

    /** Toute la faction (même groupe, 40 blocs) se retourne contre ce joueur. */
    public static void alertFaction(HumanBotEntity bot, Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        Faction f = bot.faction();
        for (HumanBotEntity o : bot.level().getEntitiesOfClass(HumanBotEntity.class, bot.getBoundingBox().inflate(40),
                o -> o.faction().group.equals(f.group) && o.faction().combatant())) {
            if (o.getTarget() == null) {
                o.setTarget(player);
            }
        }
        if (f.group.equals("police")) {
            callSwat((ServerLevel) bot.level(), player);
        }
    }

    /** Renforts du SWAT : 4 agents déployés à 24-32 blocs du joueur. */
    public static void callSwat(ServerLevel level, Player player) {
        long now = level.getGameTime();
        if (SWAT_COOLDOWN.getOrDefault(player.getUUID(), -10000L) > now) {
            return;
        }
        SWAT_COOLDOWN.put(player.getUUID(), now + 2400);
        var r = level.getRandom();
        double a = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < 4; i++) {
            int x = (int) (player.getX() + Math.cos(a) * 28) + r.nextInt(5) - 2;
            int z = (int) (player.getZ() + Math.sin(a) * 28) + r.nextInt(5) - 2;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            HumanBotEntity swat = ModEntities.HUMAN_BOT.get().create(level);
            if (swat == null) {
                continue;
            }
            swat.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            swat.setup(Faction.SWAT, i == 0 ? BotRole.OFFICER : i == 1 ? BotRole.DRONE_OPERATOR : BotRole.SOLDIER, r);
            swat.finalizeSpawn(level, level.getCurrentDifficultyAt(swat.blockPosition()), MobSpawnType.EVENT, null);
            swat.setTarget(player);
            level.addFreshEntity(swat);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.modeminecraft.bot.swat")
                .withStyle(net.minecraft.ChatFormatting.RED), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POLICE_FREEZE_WARNING.get(), SoundSource.HOSTILE, 2f, 0.8f);
    }

    /** Cible repérée : les Faucons sonnent la charge et toute l'escouade attaque. */
    static void onTargetAcquired(HumanBotEntity bot, LivingEntity target) {
        if (bot.faction() != Faction.FALCON || bot.squad == null) {
            return;
        }
        if (!bot.ambush) {
            horn(bot);                                    // en embuscade : silence jusqu'au premier coup
        }
        for (HumanBotEntity o : bot.level().getEntitiesOfClass(HumanBotEntity.class, bot.getBoundingBox().inflate(40),
                o -> bot.squad.equals(o.squad) && o.getTarget() == null)) {
            o.setTarget(target);
        }
    }

    static void horn(HumanBotEntity bot) {
        long now = bot.level().getGameTime();
        if (HORN_COOLDOWN.getOrDefault(bot.squad, -1000L) < now) {
            HORN_COOLDOWN.put(bot.squad, now + 600);
            bot.level().playSound(null, bot.getX(), bot.getY(), bot.getZ(), ModSounds.FALCON_HORN_CHARGE.get(), SoundSource.HOSTILE, 3f, 1f);
        }
    }

    /** Coup de feu : les factions qui l'entendent (40 blocs) réagissent, les civils fuient. */
    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (!event.getVanillaEvent().is(GameEvent.PROJECTILE_SHOOT) || !(event.getCause() instanceof Player p) || p.level().isClientSide
                || p.isCreative() || !(p.getMainHandItem().getItem() instanceof fr.modeminecraft.item.gun.GunItem)) {
            return;
        }
        Level level = p.level();
        long now = level.getGameTime();
        boolean silenced = ((fr.modeminecraft.item.gun.GunItem) p.getMainHandItem().getItem()).resolve(p.getMainHandItem()).silenced();
        CivilianLife.panic(level, p.position(), silenced ? 8 : 40, 240);           // les civils crient et courent se mettre à l'abri
        RadioCalls.gunshot((ServerLevel) level, p, silenced);                       // silencieux : 8 blocs
    }

    /** Explosion : panique des civils alentour. */
    @SubscribeEvent
    public static void onExplosion(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide) {
            CivilianLife.panic(event.getLevel(), event.getExplosion().center(), 32, 300);
        }
    }

    /** Fouiller les coffres d'une faction sous ses yeux la rend hostile (rebelles, cartels...). */
    @SubscribeEvent
    public static void onOpenContainer(PlayerInteractEvent.RightClickBlock event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.isCreative() || !(p.level().getBlockEntity(event.getPos()) instanceof BaseContainerBlockEntity)) {
            return;
        }
        long now = p.level().getGameTime();
        BlockPos pos = event.getPos();
        for (HumanBotEntity bot : p.level().getEntitiesOfClass(HumanBotEntity.class, p.getBoundingBox().inflate(24),
                b -> b.faction().combatant() && b.faction() != Faction.FALCON && b.home != null && b.home.distSqr(pos) < 64 * 64)) {
            if (RadioCalls.sees(bot, p)) {
                RadioCalls.witness(bot, p);                                  // pillage sous ses yeux
                break;
            }
        }
    }

    /** Escouades nomades : toutes les minutes, chance d'en voir arriver une près d'un joueur sans Faucons autour. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        ServerLevel overworld = server.overworld();
        long now = overworld.getGameTime();
        // actions différées
        for (Iterator<Object[]> it = LATER.iterator(); it.hasNext(); ) {
            Object[] t = it.next();
            if ((long) t[0] <= now) {
                it.remove();
                ((Runnable) t[1]).run();
            }
        }
        if (now % 1200 != 0 || !overworld.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        for (ServerPlayer p : overworld.players()) {
            if (!p.isSpectator() && overworld.getRandom().nextFloat() < CONVOY_CHANCE) {
                Convoys.spawn(overworld, p.position());
            }
            if (!p.isSpectator() && overworld.getRandom().nextFloat() < TIGR_CHANCE) {
                Convoys.spawnTigr(overworld, p.position());
            }
            if (!p.isSpectator() && overworld.getRandom().nextFloat() < ARMOR_CHANCE) {
                fr.modeminecraft.entity.military.ArmorPatrols.spawnPatrol(overworld, p.position());
            }
            if (!p.isSpectator() && overworld.getRandom().nextFloat() < BATTLE_CHANCE) {
                fr.modeminecraft.entity.military.ArmorPatrols.startBattle(overworld, p.position());
            }
            if (!p.isSpectator() && overworld.getRandom().nextFloat() < AIRDROP_CHANCE) {
                fr.modeminecraft.entity.AirdropEntity.call(overworld, p.position());
            }
            if (p.isSpectator() || overworld.getRandom().nextFloat() > FALCON_CHANCE) {
                continue;
            }
            boolean near = !overworld.getEntitiesOfClass(HumanBotEntity.class, p.getBoundingBox().inflate(160), b -> b.faction() == Faction.FALCON).isEmpty();
            if (near) {
                continue;
            }
            if (fr.modeminecraft.world.night.NightOps.isNight(overworld)) {
                // la nuit : embuscade silencieuse en travers de la route que suit le joueur
                Vec3 move = new Vec3(p.getDeltaMovement().x, 0, p.getDeltaMovement().z);
                Vec3 dir = move.lengthSqr() > 1.0E-4 ? move.normalize() : Vec3.directionFromRotation(0, p.getYRot());
                int x = (int) (p.getX() + dir.x * 55 - dir.z * 5), z = (int) (p.getZ() + dir.z * 55 + dir.x * 5);
                BlockPos c = new BlockPos(x, overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (overworld.isLoaded(c) && overworld.getFluidState(c.below()).isEmpty()) {
                    FalconSquads.spawnAmbush(overworld, c, dir.reverse());
                }
                continue;
            }
            double a = overworld.getRandom().nextDouble() * Math.PI * 2;
            int x = (int) (p.getX() + Math.cos(a) * 80), z = (int) (p.getZ() + Math.sin(a) * 80);
            BlockPos c = new BlockPos(x, overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!overworld.isLoaded(c) || !overworld.getFluidState(c.below()).isEmpty()) {
                continue;
            }
            FalconSquads.spawnSquad(overworld, c);
            ModeMinecraft.LOGGER.info("Faucons Blancs : escouade apparue en {}", c);
        }
    }

    /** Probabilité, par minute et par joueur, qu'une escouade de Faucons Blancs arrive (s'il n'y en a pas déjà). */
    public static float FALCON_CHANCE = 0.12f;
    /** Probabilité, par minute et par joueur, d'un largage aérien (environ un toutes les 25 minutes). */
    public static float AIRDROP_CHANCE = 0.04f;
    /** Probabilité, par minute et par joueur, qu'un convoi de patrouille passe dans les environs. */
    public static float CONVOY_CHANCE = 0.05f;
    /** Patrouille mobile en Tigr (par joueur et par minute). */
    public static float TIGR_CHANCE = 0.06f;
    /** Patrouille de blindés ennemis pilotés par l'IA, et grande bataille OTAN / URSS (rare). */
    public static float ARMOR_CHANCE = 0.03f, BATTLE_CHANCE = 0.006f;

    static void later(HumanBotEntity bot, int delay, Runnable r) {
        LATER.add(new Object[]{bot.level().getGameTime() + delay, r});
    }

    private BotEvents() {}
}
