package fr.modeminecraft.client.book;

import fr.modeminecraft.entity.MountedGunEntity;
import fr.modeminecraft.item.HelmetItem;
import fr.modeminecraft.item.MountedGunItem;
import fr.modeminecraft.item.VestItem;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.GunStats;
import fr.modeminecraft.item.melee.MeleeWeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/** Onglet "Fiche technique" : les vraies statistiques en jeu de l'objet (lues dans le code, jamais recopiées). */
public final class StatSheet {
    private static MutableComponent line(String key, Object... args) {
        return Component.translatable("book.modeminecraft.stat." + key, args);
    }

    private static String hp(float v) {
        String n = v == Math.round(v) ? String.valueOf(Math.round(v)) : String.format("%.1f", v);
        String hearts = String.format("%.1f", v / 2).replace(".0", "");
        return n + " PV (" + hearts + " ♥)";
    }

    private static int pct(double f) {
        return (int) Math.round(Math.abs(f) * 100);
    }

    public static List<Component> lines(ItemStack stack) {
        List<Component> out = new ArrayList<>();
        Item item = stack.getItem();
        if (item instanceof GunItem gun) {
            GunStats s = gun.stats();
            String caliber = BuiltInRegistries.ITEM.getKey(s.ammo().get()).getPath();
            out.add(line("category", Component.translatable("book.modeminecraft.category." + s.category().name().toLowerCase())));
            out.add(line("damage", (s.pellets() > 1 ? s.pellets() + " x " : "") + hp(s.damage())));
            out.add(line("headshot", s.category() == GunStats.Category.SNIPER ? "x2,5" : "x2"));
            float rpm = s.rpm() > 0 ? s.rpm() : 1200f / Math.max(1, s.fireDelay());
            out.add(line("rpm", Math.round(rpm)));
            out.add(line("caliber", s.ammo().get().getDescription()));
            out.add(line("magazine", s.magazineSize()));
            out.add(line("armor_pen", pct(Math.min(1, GunItem.armorPenetration(caliber) + (s.armorPiercing() ? 0.3f : 0)))));
            out.add(line("reload", String.format("%.1f", s.reloadTicks() / 20f)));
            if (s.walkSpeed() != 1) {
                int p = Math.round((s.walkSpeed() - 1) * 100);
                out.add(line("weight", (p > 0 ? "+" : "") + p));
            }
        } else if (item instanceof HelmetItem h) {
            HelmetItem.Stats s = h.stats();
            out.add(line("era", Component.translatable("tooltip.modeminecraft.helmet.era." + s.tier())));
            out.add(line("armor", s.armor()));
            out.add(line("headshot_reduction", pct(s.headshot())));
            if (s.module() == HelmetItem.Module.VISOR) {
                out.add(line("visor", pct(s.headshotModule())));
            }
            if (s.melee() > 0) out.add(line("melee", pct(s.melee())));
            if (s.shrapnel() > 0) out.add(line("shrapnel", pct(s.shrapnel())));
            if (s.explosion() > 0) out.add(line("explosion", pct(s.explosion())));
            if (s.speed() != 0) out.add(line("speed", pct(s.speed())));
            out.add(line("durability", s.durability()));
        } else if (item instanceof VestItem v) {
            VestItem.Stats s = v.stats();
            out.add(line("era", Component.translatable("tooltip.modeminecraft.vest.era." + s.tier())));
            out.add(line("armor", s.plates() ? s.armor() + " → " + (s.armor() + 2) : s.armor()));
            out.add(line("bullets", pct(s.light()), pct(s.rifle()), pct(s.heavy())));
            if (s.shrapnel() > 0) out.add(line("shrapnel", pct(s.shrapnel())));
            if (s.explosion() > 0) out.add(line("explosion", pct(s.explosion())));
            if (s.speed() != 0) out.add(line("speed", pct(s.speed())));
            if (s.reload() > 0) out.add(line("reload_bonus", pct(s.reload())));
            if (s.ads() > 0) out.add(line("ads_bonus", pct(s.ads())));
            out.add(line(s.plates() ? "plates_yes" : "plates_no"));
            out.add(line("durability", s.durability()));
        } else if (item instanceof MountedGunItem t) {
            MountedGunEntity.Kind k = t.kind();
            out.add(line("turret_ammo", k.ammo().getDescription(), k.perLoad));
            out.add(line("capacity", k.capacity));
            out.add(line("rpm", Math.round(k.rpm)));
            out.add(line("damage", k.projectile == MountedGunEntity.Projectile.GRENADE ? "22 PV (3,5 blocs)" : hp(k.damage)));
            out.add(line("yaw", k.yawArc >= 180 ? "360°" : "±" + k.yawArc + "°"));
            out.add(line("pitch", k.pitchDown, k.pitchUp));
            out.add(k.heatPerShot > 0 ? line("overheat", Math.round(1 / k.heatPerShot)) : line("no_overheat"));
        } else if (item instanceof MeleeWeaponItem m) {
            var s = m.stats();
            out.add(line("damage", hp(s.damage())));
            out.add(line("attack_speed", String.format("%.1f", s.speed())));
            out.add(line("reach", String.format("%.1f", s.reach())));
            if (s.backstab() > 1) out.add(line("backstab", String.format("%.1f", s.backstab())));
        }
        // Le reste : la description de l'objet (infobulle)
        var mc = Minecraft.getInstance();
        boolean fullSheet = item instanceof GunItem || item instanceof HelmetItem || item instanceof VestItem || item instanceof MountedGunItem;
        if (mc.level != null && !fullSheet) {
            List<Component> tip = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
            if (tip.size() > 1) {
                if (!out.isEmpty()) {
                    out.add(Component.empty());
                }
                for (int i = 1; i < tip.size(); i++) {
                    out.add(tip.get(i));
                }
            }
        }
        if (out.isEmpty()) {
            out.add(Component.translatable("book.modeminecraft.stat.none").withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    private StatSheet() {}
}
