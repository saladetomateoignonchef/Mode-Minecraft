package fr.modeminecraft.item.tactical;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.network.TacticalSyncPayload;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cases « gilet » et « sac à dos » :
 * - le gilet de la case « gilet » donne sa protection, ses plaques et ses effets comme s'il était dans la case du plastron ;
 * - les deux objets sont affichés sur le joueur pour tout le monde ;
 * - à la mort, ils tombent au sol (sauf règle keepInventory).
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class TacticalEvents {
    private record Applied(Holder<Attribute> attribute, ResourceLocation id) {}

    private static final Map<UUID, String> LAST = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Applied>> APPLIED = new ConcurrentHashMap<>();

    private static String key(ItemStack s) {
        return s.isEmpty() ? "-" : s.getItem() + "|" + s.getComponentsPatch().hashCode();
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 5 != 0) {
            return;
        }
        var slots = TacticalSlots.of(p);
        String key = key(slots.getStackInSlot(0)) + "#" + key(slots.getStackInSlot(1)) + "#" + TacticalSlots.vestInTacticalSlot(p);
        if (!key.equals(LAST.get(p.getUUID()))) {
            LAST.put(p.getUUID(), key);
            updateAttributes(p);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, payload(p));
        }
    }

    private static TacticalSyncPayload payload(Player p) {
        var slots = TacticalSlots.of(p);
        return new TacticalSyncPayload(p.getId(), slots.getStackInSlot(0).copy(), slots.getStackInSlot(1).copy());
    }

    /** Protection et poids du gilet porté dans la case « gilet » (modificateurs du plastron, recopiés). */
    private static void updateAttributes(ServerPlayer p) {
        List<Applied> old = APPLIED.remove(p.getUUID());
        if (old != null) {
            for (Applied a : old) {
                var inst = p.getAttribute(a.attribute());
                if (inst != null) {
                    inst.removeModifier(a.id());
                }
            }
        }
        if (!TacticalSlots.vestInTacticalSlot(p)) {
            return;
        }
        List<Applied> now = new ArrayList<>();
        TacticalSlots.of(p).getStackInSlot(TacticalSlots.VEST).forEachModifier(EquipmentSlot.CHEST, (attr, mod) -> {
            var inst = p.getAttribute(attr);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(mod.id().getNamespace(), mod.id().getPath() + "_tactical");
            if (inst != null && !inst.hasModifier(id)) {
                inst.addTransientModifier(new AttributeModifier(id, mod.amount(), mod.operation()));
                now.add(new Applied(attr, id));
            }
        });
        APPLIED.put(p.getUUID(), now);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, payload(target));
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        var slots = TacticalSlots.of(p);
        for (int i = 0; i < 2; i++) {
            ItemStack s = slots.getStackInSlot(i);
            if (!s.isEmpty()) {
                event.getDrops().add(new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), s.copy()));
                slots.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
        APPLIED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LAST.remove(event.getEntity().getUUID());
        APPLIED.remove(event.getEntity().getUUID());
    }

    private TacticalEvents() {}
}
