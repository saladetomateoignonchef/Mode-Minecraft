package fr.modeminecraft.scenario;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Gadgets du scénario : brouilleur portatif (pile CR123A), talkie-walkie de commandement (Lieutenant), mallette de
 * frappe stratégique (Général), serre-flex (capture d'un ennemi blessé ou pris à revers).
 */
public class GadgetItem extends Item {
    public enum Kind { JAMMER, RADIO, BRIEFCASE, ZIP_TIES }

    private final Kind kind;

    public GadgetItem(Kind kind, Properties p) {
        super(p);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            switch (kind) {
                case JAMMER -> Gadgets.jam(sp);
                case RADIO -> PacketDistributor.sendToPlayer(sp, new Gadgets.Open("radio"));
                case BRIEFCASE -> PacketDistributor.sendToPlayer(sp, new Gadgets.Open("briefcase"));
                case ZIP_TIES -> {
                    return InteractionResultHolder.pass(s);
                }
            }
        }
        return kind == Kind.ZIP_TIES ? InteractionResultHolder.pass(s) : InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind == Kind.ZIP_TIES && target instanceof fr.modeminecraft.entity.bot.HumanBotEntity bot) {
            if (player instanceof ServerPlayer sp) {
                Prisoners.capture(sp, bot, stack);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.gadget." + kind.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
    }
}
