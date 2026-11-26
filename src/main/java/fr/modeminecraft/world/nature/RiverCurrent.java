package fr.modeminecraft.world.nature;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Courant des rivières : dans un biome de rivière, l'eau entraîne lentement les nageurs, les bateaux légers et les objets flottants vers l'aval.
 * Le sens est calculé de la même façon côté client et côté serveur (pas de bruit climatique, qui n'existe que sur le serveur) : l'axe est celui où
 * l'eau s'étend le plus loin (une rivière est plus longue que large ; un lac n'a pas de courant), le sens est une fonction lente de la position
 * le long de l'axe (il s'inverse environ tous les 600 blocs, avec un courant qui faiblit jusqu'à zéro au point d'inversion : source ou confluent).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class RiverCurrent {
    private static final int SCAN = 12;
    /** Vitesse cible du courant (blocs par tick) : nageurs, bateaux, objets. 0,05 bloc par tick = 1 bloc par seconde. */
    private static final double SWIMMER = 0.055, BOAT = 0.06, ITEM = 0.045;

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        double power;
        if (e instanceof Player p) {
            if (p.isSpectator() || p.getAbilities().flying || p.getVehicle() != null) {
                return;
            }
            power = SWIMMER;
        } else if (e instanceof Boat) {
            power = BOAT;
        } else if (e instanceof ItemEntity) {
            power = ITEM;
        } else if (e instanceof LivingEntity && !(e instanceof net.minecraft.world.entity.animal.WaterAnimal) && true) {
            power = SWIMMER;
        } else {
            return;
        }
        Level level = e.level();
        BlockPos pos = e.blockPosition();
        // un objet qui flotte à la surface n'est pas « dans l'eau » : on regarde aussi la case sous lui
        boolean wet = e.isInWater() || e instanceof ItemEntity && level.getFluidState(pos.below()).is(FluidTags.WATER);
        if (!wet) {
            return;
        }
        if (!level.getBiome(pos).is(BiomeTags.IS_RIVER)) {
            return;
        }
        Vec3 flow = flow(level, pos);
        if (flow == null || flow.length() < 1.0E-3) {
            return;
        }
        // la vitesse le long du courant est ramenée en douceur vers la vitesse cible (jamais freinée si l'on va déjà plus vite dans le sens du courant)
        double mag = flow.length();
        Vec3 u = flow.scale(1.0 / mag);
        Vec3 dm = e.getDeltaMovement();
        double along = dm.x * u.x + dm.z * u.z;
        double target = power * mag;
        if (along < target) {
            e.setDeltaMovement(dm.add(u.scale((target - along) * 0.08)));
        }
    }

    /** Vecteur d'écoulement unitaire (ou réduit près d'un point d'inversion) de la rivière en ce point, ou null (lac, pas de rivière). */
    static Vec3 flow(Level level, BlockPos pos) {
        int ex = extent(level, pos, Direction.EAST) + extent(level, pos, Direction.WEST);
        int ez = extent(level, pos, Direction.SOUTH) + extent(level, pos, Direction.NORTH);
        if (Math.min(ex, ez) > 9) {
            return null;                                                    // plan d'eau large : pas de courant
        }
        boolean alongX = ex >= ez;
        double along = alongX ? pos.getX() : pos.getZ();
        double across = alongX ? pos.getZ() : pos.getX();
        double s = Math.sin(along / 95.0 + across * 0.002 + (alongX ? 0.7 : 2.1));      // sens et force : lente fonction de la position
        double strength = Math.max(-1.0, Math.min(1.0, s * 2.5));
        return alongX ? new Vec3(strength, 0, 0) : new Vec3(0, 0, strength);
    }

    private static int extent(Level level, BlockPos pos, Direction dir) {
        BlockPos.MutableBlockPos m = pos.mutable();
        int n = 0;
        while (n < SCAN) {
            m.move(dir);
            if (!level.getFluidState(m).is(FluidTags.WATER)) {
                break;
            }
            n++;
        }
        return n;
    }

    private RiverCurrent() {}
}
