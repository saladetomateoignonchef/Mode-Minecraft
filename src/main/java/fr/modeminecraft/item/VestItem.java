package fr.modeminecraft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModItems;
import fr.modeminecraft.registry.ModSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Gilet pare-balles / plastron tactique 3D (cahier des charges "gilets"). Comme les casques, ce n'est pas un ArmorItem :
 * son vrai modèle 3D est dessiné sur le torse par VestLayer.
 *
 * Protection d'un tir au torse (pas à la tête ni aux membres) :
 * 1. la plaque balistique (avant ou arrière selon d'où vient la balle) absorbe une part des dégâts selon le calibre,
 *    et s'use d'autant ; à 0 elle se brise (bruit + message) ;
 * 2. le gilet réduit ensuite les dégâts restants selon le calibre (léger / fusil / gros calibre), puis s'use.
 */
public class VestItem extends Item implements Equipable {
    /** Capacité spéciale. */
    public enum Special { NONE, AVS, FORT_DEFENDER, EOD }

    /**
     * @param light  réduction des balles légères (pistolets, PM, plombs de fusil à pompe)
     * @param rifle  réduction des balles de fusils d'assaut et mitrailleuses (5,56 / 7,62...)
     * @param heavy  réduction des gros calibres (fusils de précision, .50 BMG...)
     */
    public record Stats(int tier, double armor, float shrapnel, float explosion, float light, float rifle, float heavy,
                        float melee, boolean plates, double speed, float reload, float ads, int durability, Special special,
                        Supplier<Item> repair) {}

    // ------------------------------------------------------------------ plaques balistiques
    public enum PlateType {
        STEEL("steel", 40, 1.0f, 0.30f, 0.10f, -0.03),        // arrête pistolets et PM, -30 % fusils, lourde
        CERAMIC("ceramic", 60, 1.0f, 1.0f, 0.50f, 0),          // arrête les fusils d'assaut, -50 % snipers, se fissure
        NETHERITE("netherite", 120, 1.0f, 1.0f, 0.90f, 0);     // encaisse même la .50 BMG

        public final String id;
        public final int durability;
        private final float[] absorb;
        public final double speed;

        PlateType(String id, int durability, float light, float rifle, float heavy, double speed) {
            this.id = id;
            this.durability = durability;
            this.absorb = new float[]{light, rifle, heavy};
            this.speed = speed;
        }

        public float absorb(int caliberClass) {
            return absorb[Math.max(0, Math.min(2, caliberClass))];
        }

        public static PlateType byId(String id) {
            for (PlateType t : values()) {
                if (t.id.equals(id)) {
                    return t;
                }
            }
            return null;
        }

        public Item item() {
            return switch (this) {
                case STEEL -> ModItems.STEEL_BALLISTIC_PLATE.get();
                case CERAMIC -> ModItems.CERAMIC_BALLISTIC_PLATE.get();
                case NETHERITE -> ModItems.NETHERITE_BALLISTIC_PLATE.get();
            };
        }
    }

    /** Plaque insérée dans une poche du gilet : type + points d'absorption restants. */
    public record Plate(String type, int durability) {
        public static final Codec<Plate> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Plate::type),
                Codec.INT.fieldOf("durability").forGetter(Plate::durability)).apply(i, Plate::new));
        public static final StreamCodec<ByteBuf, Plate> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Plate::type, ByteBufCodecs.VAR_INT, Plate::durability, Plate::new);

        public PlateType kind() {
            return PlateType.byId(type);
        }

        /** 0 = détruite, 1 = fissurée, 2 = intacte. */
        public int state() {
            PlateType k = kind();
            if (k == null || durability <= 0) {
                return 0;
            }
            return durability * 3 >= k.durability * 2 ? 2 : 1;
        }
    }

    private final Stats stats;

    public VestItem(Stats stats, Properties properties) {
        super(properties.stacksTo(1).durability(stats.durability()).attributes(attributes(stats)));
        this.stats = stats;
    }

    private static ItemAttributeModifiers attributes(Stats s) {
        var b = ItemAttributeModifiers.builder().add(Attributes.ARMOR,
                new AttributeModifier(ModeMinecraft.id("vest_armor"), s.armor(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST);
        if (s.speed() != 0) {
            b.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(ModeMinecraft.id("vest_speed"), s.speed(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.CHEST);
        }
        return b.build();
    }

    public Stats stats() {
        return stats;
    }

    public static VestItem worn(LivingEntity entity) {
        return fr.modeminecraft.item.tactical.TacticalSlots.vest(entity).getItem() instanceof VestItem v ? v : null;
    }

    public static Plate plate(ItemStack vest, boolean front) {
        return vest.get(front ? ModDataComponents.PLATE_FRONT.get() : ModDataComponents.PLATE_BACK.get());
    }

    public static void setPlate(ItemStack vest, boolean front, Plate plate) {
        var type = front ? ModDataComponents.PLATE_FRONT.get() : ModDataComponents.PLATE_BACK.get();
        if (plate == null) {
            vest.remove(type);
        } else {
            vest.set(type, plate);
        }
    }

    /** Multiplicateur du temps de rechargement des armes selon le gilet porté (6B3 : -10 %, JPC : -15 %). */
    public static float reloadMul(Player player) {
        VestItem v = worn(player);
        // + pantalon à poches cargo (M42)
        return 1 - (v == null ? 0 : v.stats.reload()) - GearEffects.reloadBonus(player);
    }

    /** Bonus de vitesse de visée (JPC : +15 %). */
    public static float adsMul(Player player) {
        VestItem v = worn(player);
        return v == null ? 1 : 1 + v.stats.ads();
    }

    /**
     * Tir au torse : plaque puis gilet. Renvoie les dégâts restants.
     * @param caliberClass 0 = léger (pistolets, PM, plombs), 1 = fusil, 2 = gros calibre
     * @param front la balle arrive par devant
     */
    public static float protect(ServerLevel level, LivingEntity target, float damage, int caliberClass, boolean front) {
        ItemStack vest = fr.modeminecraft.item.tactical.TacticalSlots.vest(target);
        if (!(vest.getItem() instanceof VestItem v) || damage <= 0) {
            return damage;
        }
        float before = damage;
        Plate plate = v.stats.plates() ? plate(vest, front) : null;
        if (plate != null && plate.kind() != null && plate.durability() > 0) {
            float absorbed = Math.min(damage * plate.kind().absorb(caliberClass), plate.durability());
            damage -= absorbed;
            int left = plate.durability() - Math.max(1, Math.round(absorbed));
            if (left <= 0) {
                setPlate(vest, front, null);
                level.playSound(null, target.getX(), target.getY() + 1.2, target.getZ(), ModSounds.PLATE_SHATTER.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
                if (target instanceof Player p) {
                    p.displayClientMessage(Component.translatable(front ? "message.modeminecraft.plate.broken_front"
                            : "message.modeminecraft.plate.broken_back").withStyle(ChatFormatting.RED), true);
                }
            } else {
                setPlate(vest, front, new Plate(plate.type(), left));
            }
        }
        float r = switch (caliberClass) {
            case 0 -> v.stats.light();
            case 1 -> v.stats.rifle();
            default -> v.stats.heavy();
        };
        damage *= 1 - r;
        float absorbed = before - damage;
        if (absorbed > 0) {
            vest.hurtAndBreak(Math.max(1, Math.round(absorbed / 3)), target, EquipmentSlot.CHEST);
            level.playSound(null, target.getX(), target.getY() + 1.1, target.getZ(), ModSounds.VEST_BULLET_HIT.get(), SoundSource.PLAYERS,
                    1.0f, 0.85f + level.random.nextFloat() * 0.2f);
        }
        return damage;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return ModSounds.VEST_EQUIP;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        var tactical = fr.modeminecraft.item.tactical.TacticalSlots.of(player);
        if (!chest.isEmpty() && !(chest.getItem() instanceof VestItem) && tactical.getStackInSlot(fr.modeminecraft.item.tactical.TacticalSlots.VEST).isEmpty()) {
            // un plastron est déjà porté : le gilet va dans la case « gilet », par-dessus
            if (!level.isClientSide) {
                tactical.setStackInSlot(fr.modeminecraft.item.tactical.TacticalSlots.VEST, stack.copyWithCount(1));
                stack.shrink(1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.VEST_EQUIP.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (chest.isEmpty()) {
            // Animation : les deux bras se rejoignent devant le torse et serrent le gilet (fermeture éclair, velcros)
            stack.set(ModDataComponents.VEST_EQUIP_TIME.get(), level.getGameTime());
            if (level.isClientSide) {
                fr.modeminecraft.client.VestClient.startEquip();
            }
        }
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(stats.repair().get());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 10 + stats.tier() * 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath();
        tooltip.add(Component.translatable("tooltip.modeminecraft.vest.era." + stats.tier()).withStyle(ChatFormatting.GOLD));
        if (stats.light() > 0 || stats.rifle() > 0 || stats.heavy() > 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.vest.bullets", pct(stats.light()), pct(stats.rifle()), pct(stats.heavy()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (stats.shrapnel() > 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.shrapnel", pct(stats.shrapnel())).withStyle(ChatFormatting.GRAY));
        }
        if (stats.explosion() > 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.helmet.explosion", pct(stats.explosion())).withStyle(ChatFormatting.GRAY));
        }
        if (stats.speed() != 0) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.vest.speed", pct((float) stats.speed())).withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.vest." + id).withStyle(ChatFormatting.DARK_AQUA));
        if (stats.plates()) {
            tooltip.add(Component.translatable("tooltip.modeminecraft.vest.plates").withStyle(ChatFormatting.YELLOW));
            for (boolean front : new boolean[]{true, false}) {
                Plate p = plate(stack, front);
                Component name = p == null || p.kind() == null ? Component.translatable("tooltip.modeminecraft.vest.no_plate")
                        : Component.translatable("item.modeminecraft." + p.kind().id + "_ballistic_plate").append(" " + p.durability() + "/" + p.kind().durability);
                tooltip.add(Component.translatable(front ? "tooltip.modeminecraft.vest.front" : "tooltip.modeminecraft.vest.back", name)
                        .withStyle(p == null ? ChatFormatting.DARK_GRAY : p.state() == 2 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
            }
        }
    }

    private static int pct(float f) {
        return Math.round(Math.abs(f) * 100);
    }
}
