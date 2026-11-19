package fr.modeminecraft.entity;

import fr.modeminecraft.item.AmmoItem;
import fr.modeminecraft.item.gun.AmmoType;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModEntities;
import fr.modeminecraft.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Carreau de l'arbalète tactique : silencieux, rapide, récupérable (on le ramasse au sol ou sur la cible).
 * Carreaux explosifs / incendiaires selon le type de munition.
 */
public class BoltEntity extends AbstractArrow {
    private AmmoType type = AmmoType.STANDARD;

    public BoltEntity(EntityType<? extends BoltEntity> type, Level level) {
        super(type, level);
    }

    public BoltEntity(Level level, LivingEntity shooter, double damage, AmmoType type) {
        super(ModEntities.BOLT.get(), shooter, level, pickup(type), null);
        setBaseDamage(damage);
        this.type = type;
        this.pickup = type == AmmoType.EXPLOSIVE ? Pickup.DISALLOWED : Pickup.ALLOWED;
    }

    private static ItemStack pickup(AmmoType type) {
        ItemStack s = new ItemStack(ModItems.CROSSBOW_BOLT.get());
        if (type != AmmoType.STANDARD) {
            s.set(ModDataComponents.AMMO_TYPE.get(), type);
        }
        return s;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.CROSSBOW_BOLT.get());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        effect();
        if (type == AmmoType.INCENDIARY) {
            result.getEntity().igniteForSeconds(5);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        effect();
    }

    private void effect() {
        if (!level().isClientSide && type == AmmoType.EXPLOSIVE) {
            level().explode(this, getX(), getY(), getZ(), 1.8f, Level.ExplosionInteraction.NONE);
            discard();
        }
    }
}
