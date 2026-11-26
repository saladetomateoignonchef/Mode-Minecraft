package fr.modeminecraft.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.world.city.CityStructure;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Commande de test (opérateurs) : {@code /mmvariant <variante|off> [demi-taille]} impose la variante de ville construite par
 * {@code /place structure modeminecraft:city_plains ...} (par exemple desert, base, soviet, cartel...), pour vérifier chaque ville
 * sans chercher le bon biome. {@code off} rétablit la génération normale.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID)
public final class DebugCommands {
    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mmvariant").requires(s -> s.hasPermission(2))
                .then(Commands.argument("variante", StringArgumentType.word())
                        .executes(c -> set(c.getSource(), StringArgumentType.getString(c, "variante"), 0))
                        .then(Commands.argument("demi_taille", IntegerArgumentType.integer(0, 40))
                                .executes(c -> set(c.getSource(), StringArgumentType.getString(c, "variante"), IntegerArgumentType.getInteger(c, "demi_taille"))))));
    }

    private static int set(net.minecraft.commands.CommandSourceStack src, String variant, int radius) {
        CityStructure.forceVariant = variant.equals("off") ? null : variant;
        CityStructure.forceRadius = radius;
        src.sendSuccess(() -> Component.literal("Variante de ville forcée : " + (CityStructure.forceVariant == null ? "aucune" : variant)), true);
        return 1;
    }

    private DebugCommands() {}
}
