package fr.modeminecraft.scenario;

import fr.modeminecraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Briefings radio du QG : message scénarisé à chaque étape (indicatif du QG de son camp), grésillement radio. */
public final class Briefings {
    private Briefings() {}

    public static void radio(ServerPlayer p, String key) {
        Side s = Scenario.side(p);
        if (s == null) {
            return;
        }
        Component hq = Component.translatable("briefing.modeminecraft.hq." + s.id).withStyle(s == Side.US ? ChatFormatting.BLUE : ChatFormatting.RED, ChatFormatting.BOLD);
        p.sendSystemMessage(Component.empty().append(hq).append(" ").append(Component.translatable("briefing.modeminecraft." + key)
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
        p.playNotifySound(ModSounds.RADIO_CALL_CUT.get(), SoundSource.PLAYERS, 0.6f, 1.1f);
    }
}
