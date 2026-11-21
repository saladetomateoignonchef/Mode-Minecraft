package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.block.WeaponRackBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModeMinecraft.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<fr.modeminecraft.block.city.TrafficLightBlockEntity>> TRAFFIC_LIGHT =
            BLOCK_ENTITIES.register("traffic_light", () -> BlockEntityType.Builder.of(fr.modeminecraft.block.city.TrafficLightBlockEntity::new,
                    ModBlocks.TRAFFIC_LIGHT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<fr.modeminecraft.block.police.PoliceLockerBlockEntity>> POLICE_LOCKER =
            BLOCK_ENTITIES.register("police_locker", () -> BlockEntityType.Builder.of(fr.modeminecraft.block.police.PoliceLockerBlockEntity::new,
                    ModBlocks.POLICE_LOCKER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<fr.modeminecraft.block.police.JailDoorBlockEntity>> JAIL_DOOR =
            BLOCK_ENTITIES.register("jail_door", () -> BlockEntityType.Builder.of(fr.modeminecraft.block.police.JailDoorBlockEntity::new,
                    ModBlocks.SLIDING_JAIL_DOOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<fr.modeminecraft.block.city.AmbientSpeakerBlockEntity>> AMBIENT_SPEAKER =
            BLOCK_ENTITIES.register("ambient_speaker", () -> BlockEntityType.Builder.of(fr.modeminecraft.block.city.AmbientSpeakerBlockEntity::new,
                    ModBlocks.AMBIENT_SPEAKER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<fr.modeminecraft.block.city.WallClockBlockEntity>> WALL_CLOCK =
            BLOCK_ENTITIES.register("wall_clock", () -> BlockEntityType.Builder.of(fr.modeminecraft.block.city.WallClockBlockEntity::new,
                    ModBlocks.WALL_CLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeaponRackBlockEntity>> WEAPON_RACK =
            BLOCK_ENTITIES.register("weapon_rack", () -> BlockEntityType.Builder.of(WeaponRackBlockEntity::new,
                    ModBlocks.WEAPON_RACK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<fr.modeminecraft.block.TrophyRackBlockEntity>> TROPHY_RACK =
            BLOCK_ENTITIES.register("trophy_weapon_rack", () -> BlockEntityType.Builder.of(fr.modeminecraft.block.TrophyRackBlockEntity::new,
                    ModBlocks.TROPHY_WEAPON_RACK.get()).build(null));

    private ModBlockEntities() {}
}
