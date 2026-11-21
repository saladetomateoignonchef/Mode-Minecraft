package fr.modeminecraft.entity.bot;

import fr.modeminecraft.item.gun.AttachmentItem;
import fr.modeminecraft.item.gun.AttachmentSlot;
import fr.modeminecraft.item.gun.FlashlightHandler;
import fr.modeminecraft.item.gun.GunItem;
import fr.modeminecraft.item.gun.LightMode;
import fr.modeminecraft.registry.ModDataComponents;
import fr.modeminecraft.world.night.NightOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Les bots la nuit :
 * - les patrouilles (police, milices, cartels, soldats...) allument la lampe tactique montée sur leur arme :
 *   vrais faisceaux qui balaient les rues et vrai éclairage dynamique ;
 * - dans le noir, un bot repère deux fois moins loin une cible que sa lampe n'éclaire pas.
 */
public final class BotNight {
    /** Lampe montée selon la faction (null = pas de lampe). */
    static String lightFor(Faction f, RandomSource r) {
        return switch (f) {
            case POLICE, SHERIFF -> r.nextBoolean() ? "surefire_m600" : "streamlight_tlr1";
            case SWAT -> "surefire_m600";
            case GARRISON, ARCTIC -> r.nextBoolean() ? "an_peq15" : "surefire_m600";
            case SOVIET, MINE_SECURITY, CARTEL -> r.nextBoolean() ? "streamlight_tlr1" : "duct_tape_flashlight";
            case MILITIA, GANG, REFINERY, REBEL -> "duct_tape_flashlight";
            default -> null;
        };
    }

    static void equipLight(ItemStack gun, Faction faction, RandomSource r) {
        if (!(gun.getItem() instanceof GunItem g) || g.stats().pistol() || g.stats().category() == fr.modeminecraft.item.gun.GunStats.Category.LAUNCHER) {
            return;                                                // lampe montée sur fusils, PM, fusils à pompe et mitrailleuses
        }
        String id = lightFor(faction, r);
        if (id == null || GunItem.getAttachments(gun).get(AttachmentSlot.SIDE_RAIL).isPresent()) {
            return;
        }
        GunItem.setAttachments(gun, GunItem.getAttachments(gun).with(AttachmentSlot.SIDE_RAIL, HumanBotEntity.item(id)));
    }

    static void tick(HumanBotEntity bot) {
        if (!(bot.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack gun = bot.getMainHandItem();
        AttachmentItem.Light light = gun.getItem() instanceof GunItem ? GunItem.light(gun) : null;
        if (light == null) {
            if (bot.tickCount % 20 == 0) {
                FlashlightHandler.clear(bot);
            }
            return;
        }
        boolean on = GunItem.lightMode(gun) != LightMode.OFF;
        if ((bot.tickCount + bot.getId()) % 20 == 0) {
            boolean want = bot.isAlive() && bot.faction().combatant() && NightOps.isNight(level)
                    && level.getNearestPlayer(bot, 32) != null && !bot.isPassenger();
            if (want != on) {
                gun.set(ModDataComponents.LIGHT_MODE.get(), want ? LightMode.ON : LightMode.OFF);
                on = want;
                if (!want) {
                    FlashlightHandler.clear(bot);
                }
            }
        }
        if (on && (bot.tickCount + bot.getId()) % 4 == 0) {
            if (level.getNearestPlayer(bot, 32) != null) {
                FlashlightHandler.updateBot(bot, level, light);
            } else {
                FlashlightHandler.clear(bot);
            }
        }
    }

    /** Distance à laquelle ce bot repère cette cible (moitié dans le noir, sauf dans le faisceau de sa lampe). */
    static double sightRange(HumanBotEntity bot, LivingEntity target, double base) {
        if (!NightOps.isNight(bot.level()) || bot.ambush) {          // en embuscade, les yeux sont habitués au noir
            return base;
        }
        int bright = bot.level().getMaxLocalRawBrightness(target.blockPosition().above());
        if (bright >= 7) {
            return base;
        }
        ItemStack gun = bot.getMainHandItem();
        AttachmentItem.Light light = gun.getItem() instanceof GunItem ? GunItem.light(gun) : null;
        if (light != null && GunItem.lightMode(gun) != LightMode.OFF) {
            Vec3 to = target.getEyePosition().subtract(bot.getEyePosition());
            if (to.length() < light.range() && to.normalize().dot(bot.getLookAngle()) > Math.cos(Math.toRadians(light.cone() * 2 + 10))) {
                return base;
            }
        }
        return base * 0.5;
    }

    private BotNight() {}
}
