package fr.modeminecraft.nuclear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Bombes d'aviation : explosive AN-M65 / FAB, incendiaire AN-M76 (napalm), guidée (GBU / KAB / JDAM), nucléaire. */
public class BombItem extends Item {
    public enum Kind {
        HE(12), NAPALM(16), NUKE(1),
        /** Bombe guidée laser / GPS (GBU-27, GBU-12, KAB-500, JDAM) : vole vers le point visé, souffle de 16 blocs. */
        GUIDED(16);

        /** Nombre maximal dans la soute du B-29. */
        public final int bayMax;

        Kind(int bayMax) {
            this.bayMax = bayMax;
        }
    }

    public final Kind kind;

    public BombItem(Kind kind, Properties p) {
        super(p);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tooltip, TooltipFlag flag) {
        String k = kind.name().toLowerCase();
        tooltip.add(Component.translatable("tooltip.modeminecraft.bomb." + k).withStyle(kind == Kind.NUKE ? ChatFormatting.RED : ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.modeminecraft.bomb.bay", kind.bayMax).withStyle(ChatFormatting.GRAY));
        if (kind == Kind.NUKE) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.bomb.nuke_arm").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
