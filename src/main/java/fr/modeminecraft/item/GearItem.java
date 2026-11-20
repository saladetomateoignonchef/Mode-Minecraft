package fr.modeminecraft.item;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
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
 * Pantalon militaire ou rangers 3D (cahier des charges "pantalons & rangers") : peu d'armure, beaucoup d'effets passifs.
 * Les effets fixes passent par les attributs de Minecraft (vitesse, vitesse accroupi, dégâts de chute, recul, montée
 * automatique, saut, nage) ; les effets qui dépendent de la situation (sprint, reptation, forêt, faim, silence,
 * détection, jambes protégées) sont dans GearEffects.
 */
public class GearItem extends Item implements Equipable {
    /** Effets d'une pièce (0 = aucun). Les pourcentages sont des fractions (0,15 = +15 %). */
    public static final class Stats {
        public int tier = 1, durability = 200;
        public double armor, speed, sprint, sneak, prone, fall, knockback, step, jump, swim, safeFall, biomeSpeed;
        public float food, reload, draw, detection, legBullets, legExplosion, groundBlast;
        public boolean lightBulletsOnly, stealth, noSlowdown, coldImmune;
        public Supplier<Item> repair = () -> net.minecraft.world.item.Items.LEATHER;

        public static Stats of(int tier, double armor, int durability) {
            Stats s = new Stats();
            s.tier = tier;
            s.armor = armor;
            s.durability = durability;
            return s;
        }

        public Stats speed(double v) { speed = v; return this; }
        public Stats sprint(double v) { sprint = v; return this; }
        public Stats sneak(double v) { sneak = v; return this; }
        public Stats prone(double v) { prone = v; return this; }
        public Stats food(float v) { food = v; return this; }
        public Stats fall(double v) { fall = v; return this; }
        public Stats knockback() { knockback = 1; return this; }
        public Stats step(double v) { step = v; return this; }
        public Stats jump(double v) { jump = v; return this; }
        public Stats swim(double v) { swim = v; return this; }
        public Stats safeFall(double v) { safeFall = v; return this; }
        public Stats biomeSpeed(double v) { biomeSpeed = v; return this; }
        public Stats reload(float v) { reload = v; return this; }
        public Stats draw(float v) { draw = v; return this; }
        public Stats detection(float v) { detection = v; return this; }
        public Stats legs(float bullets, float explosions, boolean lightOnly) { legBullets = bullets; legExplosion = explosions; lightBulletsOnly = lightOnly; return this; }
        public Stats groundBlast(float v) { groundBlast = v; return this; }
        public Stats stealth() { stealth = true; return this; }
        public Stats noSlowdown() { noSlowdown = true; return this; }
        public Stats coldImmune() { coldImmune = true; return this; }
        public Stats repair(Supplier<Item> r) { repair = r; return this; }
    }

    private final Stats stats;
    private final EquipmentSlot slot;

    public GearItem(EquipmentSlot slot, Stats stats, Properties properties) {
        super(properties.stacksTo(1).durability(stats.durability).attributes(attributes(slot, stats)));
        this.slot = slot;
        this.stats = stats;
    }

    private static ItemAttributeModifiers attributes(EquipmentSlot slot, Stats s) {
        var b = ItemAttributeModifiers.builder();
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
        String p = slot == EquipmentSlot.LEGS ? "pants_" : "boots_";
        add(b, group, Attributes.ARMOR, p + "armor", s.armor, AttributeModifier.Operation.ADD_VALUE);
        add(b, group, Attributes.MOVEMENT_SPEED, p + "speed", s.speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(b, group, Attributes.SNEAKING_SPEED, p + "sneak", s.sneak, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(b, group, Attributes.FALL_DAMAGE_MULTIPLIER, p + "fall", -s.fall, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(b, group, Attributes.KNOCKBACK_RESISTANCE, p + "knockback", s.knockback, AttributeModifier.Operation.ADD_VALUE);
        add(b, group, Attributes.STEP_HEIGHT, p + "step", s.step, AttributeModifier.Operation.ADD_VALUE);
        add(b, group, Attributes.JUMP_STRENGTH, p + "jump", s.jump, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(b, group, Attributes.SAFE_FALL_DISTANCE, p + "safe_fall", s.safeFall, AttributeModifier.Operation.ADD_VALUE);
        add(b, group, net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED, p + "swim", s.swim, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (s.noSlowdown) {
            add(b, group, Attributes.MOVEMENT_EFFICIENCY, p + "grip", 1.0, AttributeModifier.Operation.ADD_VALUE);
        }
        return b.build();
    }

    private static void add(ItemAttributeModifiers.Builder b, EquipmentSlotGroup group, Holder<Attribute> attr, String id, double v,
                            AttributeModifier.Operation op) {
        if (v != 0) {
            b.add(attr, new AttributeModifier(ModeMinecraft.id(id), v, op), group);
        }
    }

    public Stats stats() {
        return stats;
    }

    public boolean isBoots() {
        return slot == EquipmentSlot.FEET;
    }

    public static GearItem worn(LivingEntity e, EquipmentSlot slot) {
        return e.getItemBySlot(slot).getItem() instanceof GearItem g ? g : null;
    }

    /** Jambes touchées par une balle : réduction du pantalon (SWAT : balles légères, Ratnik : toutes). */
    public static float protectLegs(ServerLevel level, LivingEntity target, float damage, int caliberClass) {
        ItemStack pants = target.getItemBySlot(EquipmentSlot.LEGS);
        if (!(pants.getItem() instanceof GearItem g) || g.stats.legBullets <= 0 || g.stats.lightBulletsOnly && caliberClass > 0) {
            return damage;
        }
        float absorbed = damage * g.stats.legBullets;
        pants.hurtAndBreak(Math.max(1, Math.round(absorbed / 3)), target, EquipmentSlot.LEGS);
        level.playSound(null, target.getX(), target.getY() + 0.5, target.getZ(), ModSounds.VEST_BULLET_HIT.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.2f);
        return damage - absorbed;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return slot;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return isBoots() ? ModSounds.BOOTS_LACE : ModSounds.GEAR_EQUIP_CLOTH;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getItemBySlot(slot).isEmpty()) {
            // Animation : les deux bras s'abaissent brièvement sur les côtés (on enfile / on lace)
            stack.set(ModDataComponents.GEAR_EQUIP_TIME.get(), level.getGameTime());
            if (level.isClientSide) {
                fr.modeminecraft.client.GearClient.startEquip();
            }
        }
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    /** Brodequins cloutés : on marche sur la neige poudreuse sans s'y enfoncer. */
    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return stats.noSlowdown && isBoots();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(stats.repair.get());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 10 + stats.tier * 2;
    }

    private static int pct(double f) {
        return (int) Math.round(Math.abs(f) * 100);
    }

    /** Les effets de la pièce, une ligne chacun (infobulle et fiche technique du livre). */
    public List<Component> effectLines() {
        List<Component> out = new java.util.ArrayList<>();
        Stats s = stats;
        if (s.speed != 0) out.add(Component.translatable("gear.modeminecraft.speed", pct(s.speed)));
        if (s.sprint != 0) out.add(Component.translatable("gear.modeminecraft.sprint", pct(s.sprint)));
        if (s.biomeSpeed != 0) out.add(Component.translatable("gear.modeminecraft.biome_speed", pct(s.biomeSpeed)));
        if (s.sneak != 0) out.add(Component.translatable("gear.modeminecraft.sneak", pct(s.sneak)));
        if (s.prone != 0) out.add(Component.translatable("gear.modeminecraft.prone", pct(s.prone)));
        if (s.food != 0) out.add(Component.translatable("gear.modeminecraft.food", pct(s.food)));
        if (s.fall != 0) out.add(Component.translatable(s.fall >= 1 ? "gear.modeminecraft.no_fall" : "gear.modeminecraft.fall", pct(s.fall)));
        if (s.safeFall != 0) out.add(Component.translatable("gear.modeminecraft.safe_fall", Math.round(s.safeFall + 3)));
        if (s.knockback != 0) out.add(Component.translatable("gear.modeminecraft.knockback"));
        if (s.step != 0) out.add(Component.translatable("gear.modeminecraft.step"));
        if (s.jump != 0) out.add(Component.translatable("gear.modeminecraft.jump"));
        if (s.swim != 0) out.add(Component.translatable("gear.modeminecraft.swim", pct(s.swim)));
        if (s.reload != 0) out.add(Component.translatable("gear.modeminecraft.reload", pct(s.reload)));
        if (s.draw != 0) out.add(Component.translatable("gear.modeminecraft.draw", pct(s.draw)));
        if (s.detection != 0) out.add(Component.translatable("gear.modeminecraft.detection", pct(s.detection)));
        if (s.stealth) out.add(Component.translatable("gear.modeminecraft.stealth"));
        if (s.legBullets != 0) out.add(Component.translatable(s.lightBulletsOnly ? "gear.modeminecraft.legs_light" : "gear.modeminecraft.legs_all", pct(s.legBullets)));
        if (s.legExplosion != 0) out.add(Component.translatable("gear.modeminecraft.legs_explosion", pct(s.legExplosion)));
        if (s.groundBlast != 0) out.add(Component.translatable("gear.modeminecraft.ground_blast", pct(s.groundBlast)));
        if (s.noSlowdown) out.add(Component.translatable("gear.modeminecraft.grip"));
        if (s.coldImmune) out.add(Component.translatable("gear.modeminecraft.cold"));
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.gear.era." + stats.tier).withStyle(ChatFormatting.GOLD));
        for (Component c : effectLines()) {
            tooltip.add(Component.literal("+ ").append(c).withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.modeminecraft.gear.set_bonus").withStyle(ChatFormatting.DARK_GRAY));
    }
}
