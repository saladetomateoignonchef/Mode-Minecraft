package fr.modeminecraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Carte d'accès nominative d'un boss de ville : ouvre la porte blindée de SA chambre forte. */
public class BossCardItem extends Item {
    /** Les 6 chambres fortes (tag VaultType du cahier des charges). */
    public enum VaultType {
        SWAT_HQ("swat_hq"), DESERT_PALACE("desert_palace"), FOREST_BUNKER("forest_bunker"), SOVIET_HQ("soviet_hq"),
        CARTEL_VILLA("cartel_villa"), BADLANDS_REFINERY("badlands_refinery");

        public final String id;

        VaultType(String id) {
            this.id = id;
        }
    }

    public final VaultType vault;

    public BossCardItem(VaultType vault, Properties p) {
        super(p.stacksTo(1));
        this.vault = vault;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.boss_card." + vault.id).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.modeminecraft.boss_card").withStyle(ChatFormatting.GRAY));
    }
}
