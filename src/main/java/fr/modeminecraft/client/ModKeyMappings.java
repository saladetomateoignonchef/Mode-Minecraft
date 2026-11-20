package fr.modeminecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Touches du mod (modifiables dans Options > Commandes). */
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.modeminecraft";

    public static final KeyMapping RELOAD = new KeyMapping("key.modeminecraft.reload",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    public static final KeyMapping FIRE_MODE = new KeyMapping("key.modeminecraft.fire_mode",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    public static final KeyMapping CHECK_CHAMBER = new KeyMapping("key.modeminecraft.check_chamber",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    public static final KeyMapping AMMO_TYPE = new KeyMapping("key.modeminecraft.ammo_type",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    public static final KeyMapping LASER = new KeyMapping("key.modeminecraft.laser",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_T, CATEGORY);

    /** Lampe tactique : L = allumer / éteindre, Maj + L = changer de mode. */
    public static final KeyMapping FLASHLIGHT = new KeyMapping("key.modeminecraft.flashlight",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, CATEGORY);

    public static final KeyMapping UNDERBARREL = new KeyMapping("key.modeminecraft.underbarrel",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    // GLFW_KEY_Q / GLFW_KEY_E = touches A / E sur un clavier AZERTY (Q / E en QWERTY).
    // Elles ne servent à se pencher qu'en visant : sinon jeter / inventaire marchent normalement.
    public static final KeyMapping LEAN_LEFT = new KeyMapping("key.modeminecraft.lean_left",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, CATEGORY);

    public static final KeyMapping LEAN_RIGHT = new KeyMapping("key.modeminecraft.lean_right",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, CATEGORY);

    public static final KeyMapping HOLD_BREATH = new KeyMapping("key.modeminecraft.hold_breath",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, CATEGORY);

    /** Menu rapide des accessoires (roue) : maintenir, pointer, relâcher. */
    public static final KeyMapping QUICK_ATTACH = new KeyMapping("key.modeminecraft.quick_attach",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY);

    /** Casque : visière blindée / jumelles de vision nocturne. */
    public static final KeyMapping HELMET_MODULE = new KeyMapping("key.modeminecraft.helmet_module",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);

    // Livre des recettes (dans les interfaces : pas de conflit avec R = recharger en jeu)
    public static final KeyMapping ITEM_PANEL = new KeyMapping("key.modeminecraft.item_panel",
            net.neoforged.neoforge.client.settings.KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping SHOW_RECIPE = new KeyMapping("key.modeminecraft.show_recipe",
            net.neoforged.neoforge.client.settings.KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping SHOW_USES = new KeyMapping("key.modeminecraft.show_uses",
            net.neoforged.neoforge.client.settings.KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CATEGORY);

    /** S'allonger / se relever (en plus du double appui sur "s'accroupir"). */
    public static final KeyMapping PRONE = new KeyMapping("key.modeminecraft.prone",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    public static final KeyMapping MELEE = new KeyMapping("key.modeminecraft.melee",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);

    private ModKeyMappings() {}
}
