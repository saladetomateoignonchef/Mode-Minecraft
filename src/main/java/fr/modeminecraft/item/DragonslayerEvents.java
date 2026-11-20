package fr.modeminecraft.item;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La Pourfendeuse de Dragons entre les mains d'un joueur (ou du Chef) :
 * - perforation lourde : ignore 60 % de l'armure de la cible ;
 * - arrache 25 % de la durabilité maximale des plaques balistiques du gilet de la cible ;
 * - bruit de dégainage / rengainage quand on la sélectionne / range dans la barre d'action.
 * (Le bouclier brisé 5 s et le malus de -15 % de vitesse en main sont dans {@link DragonslayerItem}.)
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class DragonslayerEvents {
    public static final float ARMOR_IGNORED = 0.6f, PLATE_DAMAGE = 0.25f;
    private static final Map<UUID, Boolean> HOLDING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker
                || source.is(DamageTypeTags.IS_PROJECTILE) || !attacker.getMainHandItem().is(ModItems.DRAGONSLAYER.get())) {
            return;
        }
        float keep = 1 - ARMOR_IGNORED;
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * keep);
        LivingEntity target = event.getEntity();
        ItemStack vest = target.getItemBySlot(EquipmentSlot.CHEST);
        if (vest.getItem() instanceof VestItem v && v.stats().plates()) {
            for (boolean front : new boolean[]{true, false}) {
                VestItem.Plate p = VestItem.plate(vest, front);
                if (p == null || p.kind() == null || p.durability() <= 0) {
                    continue;
                }
                int left = p.durability() - Math.max(1, Math.round(p.kind().durability * PLATE_DAMAGE));
                VestItem.setPlate(vest, front, left <= 0 ? null : new VestItem.Plate(p.type(), left));
                if (left <= 0) {
                    target.level().playSound(null, target.getX(), target.getY() + 1.2, target.getZ(), ModSounds.PLATE_SHATTER.get(),
                            SoundSource.PLAYERS, 1.2f, 0.9f);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        boolean holding = p.getMainHandItem().is(ModItems.DRAGONSLAYER.get());
        Boolean before = HOLDING.put(p.getUUID(), holding);
        if (before != null && before != holding && !p.isSpectator()) {
            if (holding) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.DRAGONSLAYER_UNSHEATHE.get(), SoundSource.PLAYERS, 1.0f, 1.05f);
            } else if (hasInHotbar(p)) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.DRAGONSLAYER_SHEATHE.get(), SoundSource.PLAYERS, 0.9f, 1.05f);
            }
        }
    }

    private static boolean hasInHotbar(ServerPlayer p) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().items.get(i).is(ModItems.DRAGONSLAYER.get())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HOLDING.remove(event.getEntity().getUUID());
    }

    private DragonslayerEvents() {}
}
