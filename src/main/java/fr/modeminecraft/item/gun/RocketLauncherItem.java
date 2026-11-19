package fr.modeminecraft.item.gun;

import fr.modeminecraft.entity.RocketEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/** Lance-roquettes : même fonctionnement qu'une arme à feu, mais tire des roquettes explosives. */
public class RocketLauncherItem extends GunItem {
    public RocketLauncherItem(GunStats stats, Properties properties) {
        super(stats, properties);
    }

    @Override
    protected Projectile createProjectile(Level level, Player player) {
        return new RocketEntity(level, player);
    }
}
