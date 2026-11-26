package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.entity.RocketEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ModeMinecraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>> BULLET = ENTITY_TYPES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("bullet"));

    public static final DeferredHolder<EntityType<?>, EntityType<RocketEntity>> ROCKET = ENTITY_TYPES.register("rocket",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("rocket"));

    public static final DeferredHolder<EntityType<?>, EntityType<RocketEntity>> GRENADE = ENTITY_TYPES.register("grenade",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("grenade"));

    /** Missile guidé (Stinger, Javelin). */
    public static final DeferredHolder<EntityType<?>, EntityType<RocketEntity>> MISSILE = ENTITY_TYPES.register("missile",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(10).updateInterval(1).build("missile"));

    /** Véhicules militaires (mise à jour 0.4) : un type d'entité par modèle (taille propre, /summon par nom). */
    public static final java.util.Map<fr.modeminecraft.entity.military.MilType, DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.military.MilVehicleEntity>>>
            MIL_VEHICLE_TYPES = new java.util.EnumMap<>(fr.modeminecraft.entity.military.MilType.class);

    static {
        for (var t : fr.modeminecraft.entity.military.MilType.values()) {
            float[] d = fr.modeminecraft.entity.military.MilVehicleEntity.dims(t);
            MIL_VEHICLE_TYPES.put(t, ENTITY_TYPES.register(t.id, () -> EntityType.Builder.<fr.modeminecraft.entity.military.MilVehicleEntity>of(
                    (et, level) -> new fr.modeminecraft.entity.military.MilVehicleEntity(et, level, t), MobCategory.MISC)
                    .sized(d[1], d[2]).clientTrackingRange(16).updateInterval(1).build(t.id)));
        }
    }

    /** Locomotive et wagons du train des villes du Mesa. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.train.MesaTrainCarEntity>> MESA_TRAIN_CAR = ENTITY_TYPES.register("mesa_train_car",
            () -> EntityType.Builder.<fr.modeminecraft.entity.train.MesaTrainCarEntity>of(fr.modeminecraft.entity.train.MesaTrainCarEntity::new, MobCategory.MISC)
                    .sized(2.8f, 3.6f).clientTrackingRange(16).updateInterval(1).fireImmune().build("mesa_train_car"));

    /** Obus, roquettes et missiles des véhicules. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.military.ShellEntity>> SHELL = ENTITY_TYPES.register("heavy_shell",
            () -> EntityType.Builder.<fr.modeminecraft.entity.military.ShellEntity>of(fr.modeminecraft.entity.military.ShellEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(32).updateInterval(1).build("heavy_shell"));

    /** Carreau d'arbalète (récupérable). */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.BoltEntity>> BOLT = ENTITY_TYPES.register("bolt",
            () -> EntityType.Builder.<fr.modeminecraft.entity.BoltEntity>of(fr.modeminecraft.entity.BoltEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(5).build("bolt"));

    /** Boule de plasma. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.PlasmaEntity>> PLASMA = ENTITY_TYPES.register("plasma",
            () -> EntityType.Builder.<fr.modeminecraft.entity.PlasmaEntity>of(fr.modeminecraft.entity.PlasmaEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2).build("plasma"));

    /** Mitrailleuse lourde posée au sol (M2 Browning, DShK). */
    /** Bombes d'aviation (B-29) et champignon atomique. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.nuclear.BombEntity>> BOMB = ENTITY_TYPES.register("aerial_bomb",
            () -> EntityType.Builder.<fr.modeminecraft.nuclear.BombEntity>of(fr.modeminecraft.nuclear.BombEntity::new, MobCategory.MISC)
                    .sized(0.7f, 0.7f).clientTrackingRange(16).updateInterval(1).build("aerial_bomb"));
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.nuclear.NukeCloudEntity>> NUKE_CLOUD = ENTITY_TYPES.register("nuke_cloud",
            () -> EntityType.Builder.<fr.modeminecraft.nuclear.NukeCloudEntity>of(fr.modeminecraft.nuclear.NukeCloudEntity::new, MobCategory.MISC)
                    .sized(4f, 4f).clientTrackingRange(32).updateInterval(40).fireImmune().build("nuke_cloud"));
    /** Lance-roquettes statiques : trépied Type 63 (4 x 107 mm) et batterie fixe de 12 x 122 mm. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.military.RocketLauncherEntity>> ROCKET_LAUNCHER = ENTITY_TYPES.register("rocket_launcher",
            () -> EntityType.Builder.<fr.modeminecraft.entity.military.RocketLauncherEntity>of(fr.modeminecraft.entity.military.RocketLauncherEntity::new, MobCategory.MISC)
                    .sized(1.4f, 1.1f).clientTrackingRange(10).updateInterval(3).build("rocket_launcher"));
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.MountedGunEntity>> MOUNTED_GUN = ENTITY_TYPES.register("mounted_gun",
            () -> EntityType.Builder.<fr.modeminecraft.entity.MountedGunEntity>of(fr.modeminecraft.entity.MountedGunEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(10).updateInterval(3).build("mounted_gun"));

    /** Tomahawk lancé. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.TomahawkEntity>> TOMAHAWK = ENTITY_TYPES.register("tomahawk",
            () -> EntityType.Builder.<fr.modeminecraft.entity.TomahawkEntity>of(fr.modeminecraft.entity.TomahawkEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(4).build("tomahawk"));

    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.ThrownGrenadeEntity>> THROWN_GRENADE = ENTITY_TYPES.register("thrown_grenade",
            () -> EntityType.Builder.<fr.modeminecraft.entity.ThrownGrenadeEntity>of(fr.modeminecraft.entity.ThrownGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(2).build("thrown_grenade"));
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.AreaCloudEntity>> AREA_CLOUD = ENTITY_TYPES.register("area_cloud",
            () -> EntityType.Builder.<fr.modeminecraft.entity.AreaCloudEntity>of(fr.modeminecraft.entity.AreaCloudEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(20).fireImmune().build("area_cloud"));
    /** Véhicules conduisibles (quad, voiture de police, jeep, camion blindé). */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.VehicleEntity>> VEHICLE = ENTITY_TYPES.register("vehicle",
            () -> EntityType.Builder.<fr.modeminecraft.entity.VehicleEntity>of(fr.modeminecraft.entity.VehicleEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(2f, 1.5f).clientTrackingRange(10).updateInterval(1).build("vehicle"));
    /** Drones de combat pilotables. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.drone.DroneEntity>> DRONE = ENTITY_TYPES.register("drone",
            () -> EntityType.Builder.<fr.modeminecraft.entity.drone.DroneEntity>of(fr.modeminecraft.entity.drone.DroneEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6f, 0.3f).clientTrackingRange(32).updateInterval(1).build("drone"));
    /** Geyser d'une bouche d'incendie détruite. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.GeyserEntity>> GEYSER = ENTITY_TYPES.register("geyser",
            () -> EntityType.Builder.<fr.modeminecraft.entity.GeyserEntity>of(fr.modeminecraft.entity.GeyserEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).build("geyser"));
    /** Largage aérien (caisse sous parachute). */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.AirdropEntity>> AIRDROP = ENTITY_TYPES.register("airdrop",
            () -> EntityType.Builder.<fr.modeminecraft.entity.AirdropEntity>of(fr.modeminecraft.entity.AirdropEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(1f, 1f).clientTrackingRange(16).updateInterval(2).build("airdrop"));
    /** Bots humains (PNJ des villes et des factions). */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.bot.HumanBotEntity>> HUMAN_BOT = ENTITY_TYPES.register("human_bot",
            () -> EntityType.Builder.<fr.modeminecraft.entity.bot.HumanBotEntity>of(fr.modeminecraft.entity.bot.HumanBotEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(7).build("human_bot"));
    /** Pigeons et mouettes des villes. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.city.PigeonEntity>> PIGEON = ENTITY_TYPES.register("pigeon",
            () -> EntityType.Builder.<fr.modeminecraft.entity.city.PigeonEntity>of(fr.modeminecraft.entity.city.PigeonEntity::new, net.minecraft.world.entity.MobCategory.AMBIENT)
                    .sized(0.4f, 0.45f).clientTrackingRange(4).build("pigeon"));
    /** Faune sauvage du réalisme naturel : aigle, mouette, scorpion, serpent, cerf (un type d'entité par espèce). */
    public static final java.util.Map<fr.modeminecraft.entity.wild.WildlifeEntity.Kind, DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.wild.WildlifeEntity>>> WILD =
            new java.util.EnumMap<>(fr.modeminecraft.entity.wild.WildlifeEntity.Kind.class);
    static {
        for (fr.modeminecraft.entity.wild.WildlifeEntity.Kind k : fr.modeminecraft.entity.wild.WildlifeEntity.Kind.values()) {
            WILD.put(k, ENTITY_TYPES.register(k.id(), () -> EntityType.Builder.<fr.modeminecraft.entity.wild.WildlifeEntity>of(
                    (t, l) -> new fr.modeminecraft.entity.wild.WildlifeEntity(t, l, k), net.minecraft.world.entity.MobCategory.CREATURE)
                    .sized(k.width, k.height).clientTrackingRange(k.flier() ? 10 : 8).build(k.id())));
        }
    }
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.PlacedExplosiveEntity>> PLACED_EXPLOSIVE = ENTITY_TYPES.register("placed_explosive",
            () -> EntityType.Builder.<fr.modeminecraft.entity.PlacedExplosiveEntity>of(fr.modeminecraft.entity.PlacedExplosiveEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.4f).clientTrackingRange(8).updateInterval(5).build("placed_explosive"));

    private ModEntities() {}
}
