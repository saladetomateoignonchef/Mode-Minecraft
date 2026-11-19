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
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_T, CATEGORY);

    public static final KeyMapping UNDERBARREL = new KeyMapping("key.modeminecraft.underbarrel",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    private ModKeyMappings() {}
}
