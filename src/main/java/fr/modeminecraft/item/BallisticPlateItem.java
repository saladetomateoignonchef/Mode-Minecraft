package fr.modeminecraft.item;

import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Plaque balistique insérable. Clic droit en la tenant (avec un porte-plaques sur soi) : le bras gauche l'enfonce dans
 * la poche avant, puis arrière ; si les deux poches sont pleines, elle remplace la plus abîmée (rendue dans l'inventaire).
 * Sa barre de durabilité = ses points d'absorption restants.
 */
public class BallisticPlateItem extends Item {
    private final VestItem.PlateType type;

    public BallisticPlateItem(VestItem.PlateType type, Properties properties) {
        super(properties.stacksTo(1).durability(type.durability));
        this.type = type;
    }

    public VestItem.PlateType type() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack plate = player.getItemInHand(hand);
        ItemStack vest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(vest.getItem() instanceof VestItem v) || !v.stats().plates()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.modeminecraft.plate.no_carrier").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(plate);
        }
        if (!level.isClientSide) {
            VestItem.Plate front = VestItem.plate(vest, true), back = VestItem.plate(vest, false);
            boolean toFront;
            if (front == null) {
                toFront = true;
            } else if (back == null) {
                toFront = false;
            } else {
                toFront = front.durability() * back.kind().durability <= back.durability() * front.kind().durability;
            }
            VestItem.Plate old = VestItem.plate(vest, toFront);
            VestItem.setPlate(vest, toFront, new VestItem.Plate(type.id, type.durability - plate.getDamageValue()));
            vest.set(ModDataComponents.PLATE_INSERT_TIME.get(), level.getGameTime());
            if (!player.getAbilities().instabuild) {
                plate.shrink(1);
            }
            if (old != null && old.kind() != null && old.durability() > 0) {
                ItemStack back2 = new ItemStack(old.kind().item());
                back2.setDamageValue(old.kind().durability - old.durability());
                if (!player.getInventory().add(back2)) {
                    player.drop(back2, false);
                }
            }
            level.playSound(null, player.getX(), player.getY() + 1.1, player.getZ(), ModSounds.PLATE_INSERT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(Component.translatable(toFront ? "message.modeminecraft.plate.inserted_front"
                    : "message.modeminecraft.plate.inserted_back").withStyle(ChatFormatting.GREEN), true);
        } else {
            fr.modeminecraft.client.VestClient.startPlateInsert(plate.copy());
        }
        return InteractionResultHolder.sidedSuccess(plate, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.plate." + type.id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.plate.points", type.durability - stack.getDamageValue(), type.durability)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.modeminecraft.plate.use").withStyle(ChatFormatting.YELLOW));
    }
}
