package fr.modeminecraft.item;

import fr.modeminecraft.entity.bot.BotRole;
import fr.modeminecraft.entity.bot.FalconSquads;
import fr.modeminecraft.entity.bot.Faction;
import fr.modeminecraft.entity.bot.HumanBotEntity;
import fr.modeminecraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Œuf d'apparition d'un bot de faction (rôle au hasard), du Chef des Troupes de Choc ou d'une escouade de Faucons. */
public class BotSpawnEggItem extends Item {
    private final Faction faction;
    private final BotRole role;

    public BotSpawnEggItem(Faction faction, BotRole role, Properties properties) {
        super(properties);
        this.faction = faction;
        this.role = role;
    }

    public Faction faction() {
        return faction;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (faction == Faction.FALCON && role == BotRole.COMMANDER) {
            FalconSquads.spawnSquad(level, pos);
        } else {
            HumanBotEntity bot = ModEntities.HUMAN_BOT.get().create(level);
            if (bot == null) {
                return InteractionResult.FAIL;
            }
            bot.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getRotation() + 180, 0);
            BotRole r = role != null ? role : defaultRole(level);
            bot.setup(faction, r, level.getRandom());
            bot.home = pos;
            bot.setPersistenceRequired();
            bot.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
            level.addFreshEntity(bot);
        }
        if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private BotRole defaultRole(ServerLevel level) {
        if (faction == Faction.CIVILIAN) {
            return BotRole.CIVILIAN;
        }
        if (faction == Faction.TRADER) {
            return BotRole.TRADER;
        }
        if (faction == Faction.FALCON) {
            BotRole[] r = {BotRole.SCOUT, BotRole.LANCER, BotRole.ASSASSIN, BotRole.BRUTE};
            return r[level.getRandom().nextInt(r.length)];
        }
        float x = level.getRandom().nextFloat();
        return x < 0.15f ? BotRole.SNIPER : x < 0.3f ? BotRole.OFFICER : BotRole.SOLDIER;
    }

    @Override
    public Component getName(net.minecraft.world.item.ItemStack stack) {
        return Component.translatable("item.modeminecraft.bot_egg", role == BotRole.CHIEF
                ? Component.translatable("entity.modeminecraft.bot.falcon.chief")
                : role == BotRole.COMMANDER ? Component.translatable("entity.modeminecraft.bot.falcon_squad")
                : Component.translatable("entity.modeminecraft.bot." + faction.id));
    }
}
