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
    /** Poussée par tick : nageurs, bateaux, objets. À l'équilibre (frottement de l'eau) : environ 1 à 1,5 bloc par seconde. */
    private static final double SWIMMER = 0.012, BOAT = 0.010, ITEM = 0.016;

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
        } else if (e instanceof LivingEntity && !(e instanceof net.minecraft.world.entity.animal.WaterAnimal) && e.tickCount % 2 == 0) {
            power = SWIMMER * 2;
        } else {
            return;
        }
        if (!e.isInWater()) {
            return;
        }
        Level level = e.level();
        BlockPos pos = e.blockPosition();
        if (!level.getBiome(pos).is(BiomeTags.IS_RIVER)) {
            return;
        }
        Vec3 flow = flow(level, pos);
        if (flow != null) {
            e.setDeltaMovement(e.getDeltaMovement().add(flow.scale(power)));
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
