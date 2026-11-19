package fr.modeminecraft.network;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.GunAttachments;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Menu rapide des accessoires (touche Y) : monter l'accessoire de l'emplacement d'inventaire "inventorySlot",
 * ou retirer celui de l'emplacement d'arme "removeSlot" (l'autre valeur vaut -1). Le serveur vérifie tout.
 */
public record QuickAttachPayload(int inventorySlot, int removeSlot) implements CustomPacketPayload {
    public static final Type<QuickAttachPayload> TYPE = new Type<>(ModeMinecraft.id("quick_attach"));
    public static final StreamCodec<ByteBuf, QuickAttachPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QuickAttachPayload::inventorySlot, ByteBufCodecs.VAR_INT, QuickAttachPayload::removeSlot,
            QuickAttachPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuickAttachPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> apply(context.player(), payload.inventorySlot, payload.removeSlot));
    }

    private static void apply(Player player, int inventorySlot, int removeSlot) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)
                || stack.getOrDefault(ModDataComponents.RELOAD_END.get(), 0L) > player.level().getGameTime()) {
            return;
        }
        GunAttachments attachments = GunItem.getAttachments(stack);
        Item removed = null;
        if (removeSlot >= 0 && removeSlot < AttachmentSlot.values().length) {
            AttachmentSlot slot = AttachmentSlot.values()[removeSlot];
            removed = attachments.get(slot).orElse(null);
            if (removed == null) {
                return;
            }
            attachments = attachments.with(slot, null);
        } else if (inventorySlot >= 0 && inventorySlot < player.getInventory().getContainerSize()) {
            ItemStack part = player.getInventory().getItem(inventorySlot);
            if (!(part.getItem() instanceof AttachmentItem a) || !gun.accepts(a)) {
                return;
            }
            removed = attachments.get(a.slot()).orElse(null);
            attachments = attachments.with(a.slot(), a);
            if (a.slot() == AttachmentSlot.MUZZLE) {
                stack.remove(ModDataComponents.MUZZLE_WEAR.get()); // silencieux neuf
            }
            part.shrink(1);
        } else {
            return;
        }
        GunItem.setAttachments(stack, attachments);
        if (removed != null) {
            ItemStack back = new ItemStack(removed);
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        }
        // Chargeur plus petit : les balles en trop reviennent dans l'inventaire
        int capacity = gun.resolve(stack).magazineSize();
        int ammo = GunItem.getAmmo(stack);
        if (ammo > capacity) {
            GunItem.setAmmo(stack, capacity);
            gun.giveBack(player, ammo - capacity, GunItem.getLoadedType(stack));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(),
                SoundSource.PLAYERS, 0.7f, 1.3f);
    }
}
