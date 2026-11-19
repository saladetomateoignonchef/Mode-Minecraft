package fr.modeminecraft.client;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.entity.AreaCloudEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/** Fumigène : les pseudos des joueurs dans la fumée ne s'affichent pas. */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class SmokeNameTags {
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        var e = event.getEntity();
        for (AreaCloudEntity c : e.level().getEntitiesOfClass(AreaCloudEntity.class, new AABB(e.blockPosition()).inflate(8))) {
            if (c.cloudType() == AreaCloudEntity.Type.SMOKE && c.contains(e.getEyePosition())) {
                event.setCanRender(TriState.FALSE);
                return;
            }
        }
    }

    private SmokeNameTags() {}
}
