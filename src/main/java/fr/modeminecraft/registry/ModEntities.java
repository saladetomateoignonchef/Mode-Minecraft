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

    /** Carreau d'arbalète (récupérable). */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.BoltEntity>> BOLT = ENTITY_TYPES.register("bolt",
            () -> EntityType.Builder.<fr.modeminecraft.entity.BoltEntity>of(fr.modeminecraft.entity.BoltEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(5).build("bolt"));

    /** Boule de plasma. */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.PlasmaEntity>> PLASMA = ENTITY_TYPES.register("plasma",
            () -> EntityType.Builder.<fr.modeminecraft.entity.PlasmaEntity>of(fr.modeminecraft.entity.PlasmaEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2).build("plasma"));

    /** Mitrailleuse lourde posée au sol (M2 Browning, DShK). */
    public static final DeferredHolder<EntityType<?>, EntityType<fr.modeminecraft.entity.MountedGunEntity>> MOUNTED_GUN = ENTITY_TYPES.register("mounted_gun",
            () -> EntityType.Builder.<fr.modeminecraft.entity.MountedGunEntity>of(fr.modeminecraft.entity.MountedGunEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(10).updateInterval(3).build("mounted_gun"));

    private ModEntities() {}
}
