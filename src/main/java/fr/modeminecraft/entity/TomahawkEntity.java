package fr.modeminecraft.entity;

import fr.modeminecraft.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Tomahawk lancé : trajectoire en cloche, 12 dégâts, on le récupère au sol (ou sur la cible). */
public class TomahawkEntity extends AbstractArrow implements ItemSupplier {
    public TomahawkEntity(EntityType<? extends TomahawkEntity> type, Level level) {
        super(type, level);
    }

    public TomahawkEntity(Level level, LivingEntity thrower, ItemStack stack) {
        super(ModEntities.TOMAHAWK.get(), thrower, level, stack, null);
        setBaseDamage(12.0 / 1.6 / 2); // les flèches font dégâts x vitesse ; ~12 PV à l'impact
        this.pickup = thrower instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild
                ? Pickup.CREATIVE_ONLY : Pickup.ALLOWED;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(fr.modeminecraft.registry.ModItems.TOMAHAWK.get());
    }

    @Override
    public ItemStack getItem() {
        return getPickupItemStackOrigin();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.06;
    }
}
