package fr.modeminecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.BulletEntity;
import fr.modeminecraft.item.gun.AmmoType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Balles en vrai 3D (douille en laiton, ogive en cuivre ; plomb rond pour la chevrotine), orientées dans le sens du vol.
 * La balle part réellement de l'œil du tireur ; à l'écran, elle sort du canon (au centre quand on vise)
 * et rejoint sa vraie trajectoire en quelques blocs : elle file droit vers le réticule.
 */
public class BulletRenderer extends EntityRenderer<BulletEntity> {
    public static final ModelResourceLocation BULLET = ModelResourceLocation.standalone(ModeMinecraft.id("item/parts/bullet_3d"));
    public static final ModelResourceLocation PELLET = ModelResourceLocation.standalone(ModeMinecraft.id("item/parts/pellet_3d"));
    private static final ItemStack DUMMY = new ItemStack(Items.STICK);
    /** Distance (blocs) sur laquelle la balle visible rejoint sa vraie trajectoire. */
    private static final double CONVERGE = 6.0;

    /** balle -> {départ réel x, y, z, décalage du canon x, y, z} */
    private static final Map<Integer, double[]> START = new ConcurrentHashMap<>();

    public BulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0;
    }

    @Override
    public void render(BulletEntity bullet, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 pos = new Vec3(Mth.lerp(partial, bullet.xo, bullet.getX()), Mth.lerp(partial, bullet.yo, bullet.getY()),
                Mth.lerp(partial, bullet.zo, bullet.getZ()));
        double[] start = START.computeIfAbsent(bullet.getId(), k -> origin(bullet, pos));
        double travelled = pos.distanceTo(new Vec3(start[0], start[1], start[2]));
        if (bullet.getOwner() == mc.getCameraEntity() && travelled < 0.8) {
            return;                                                  // trop près de l'œil : invisible
        }
        double k = Math.max(0, 1 - travelled / CONVERGE);
        pose.pushPose();
        pose.translate(start[3] * k, start[4] * k, start[5] * k);
        Vec3 v = bullet.getDeltaMovement();
        float yRot = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
        float xRot = (float) (Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
        pose.mulPose(Axis.YP.rotationDegrees(yRot - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(xRot));
        boolean pellet = bullet.ammoType() == AmmoType.DRAGON_BREATH || bullet.ammoType() == AmmoType.FLECHETTE
                || bullet.ammoType() != AmmoType.SLUG && bullet.getOwner() instanceof net.minecraft.world.entity.LivingEntity o
                && o.getMainHandItem().getItem() instanceof fr.modeminecraft.item.gun.GunItem g
                && g.stats().category() == fr.modeminecraft.item.gun.GunStats.Category.SHOTGUN;
        float s = pellet ? 0.5f : 0.8f;
        pose.scale(s, s, s);
        int shown = bullet.ammoType() == AmmoType.TRACER ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : light;
        mc.getItemRenderer().render(DUMMY, ItemDisplayContext.NONE, false, pose, buffers, shown, OverlayTexture.NO_OVERLAY,
                mc.getModelManager().getModel(pellet ? PELLET : BULLET));
        pose.popPose();
        if (!bullet.isAlive() || bullet.tickCount > 200) {
            START.remove(bullet.getId());
        }
        super.render(bullet, yaw, partial, pose, buffers, light);
    }

    /** Point de départ réel et décalage vers la bouche du canon (vue à la 1re personne : selon la visée). */
    private static double[] origin(BulletEntity bullet, Vec3 now) {
        Vec3 spawn = now;
        Vec3 offset = Vec3.ZERO;
        if (bullet.getOwner() instanceof Player shooter && bullet.tickCount <= 2) {
            Minecraft mc = Minecraft.getInstance();
            Vec3 eye = shooter.getEyePosition();
            Vec3 look = shooter.getLookAngle();
            Vec3 right = fr.modeminecraft.item.gun.LeanHandler.right(shooter);
            Vec3 up = right.cross(look).normalize();
            Vec3 muzzle;
            if (shooter == mc.player && mc.options.getCameraType().isFirstPerson()) {
                float aim = GunPoseHelper.aimProgress(1.0f);
                muzzle = eye.add(look.scale(1.0)).add(right.scale(0.24 * (1 - aim))).add(up.scale(-0.2 * (1 - aim) - 0.05 * aim));
            } else {
                muzzle = eye.add(look.scale(1.1)).add(right.scale(0.24)).add(up.scale(-0.3));
            }
            // départ réel : l'œil (- 0,1), sur la ligne de tir
            spawn = shooter.getEyePosition().add(0, -0.1, 0);
            Vec3 alongTrue = spawn.add(look.scale(1.0));
            offset = muzzle.subtract(alongTrue);
        }
        return new double[]{spawn.x, spawn.y, spawn.z, offset.x, offset.y, offset.z};
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity bullet) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
