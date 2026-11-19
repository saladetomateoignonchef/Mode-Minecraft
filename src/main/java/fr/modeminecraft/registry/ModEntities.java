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

    private ModEntities() {}
}
