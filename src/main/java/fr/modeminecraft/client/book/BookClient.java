package fr.modeminecraft.client.book;

import fr.modeminecraft.ModConfigs;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.client.ModKeyMappings;
import fr.modeminecraft.client.screen.ArmamentTableScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.WeakHashMap;

/**
 * Livre des recettes côté client :
 * - panneau latéral (façon Not Enough Items) à droite de l'inventaire, de l'établi, des fours, de la table d'armement
 *   et de la table de modification ; touche O pour l'afficher / le masquer ;
 * - R / U (ou clic gauche / droit) sur n'importe quel objet survolé : recette / utilisations ;
 * - manuel : les deux mains lèvent le livre et l'ouvrent avant d'afficher l'interface.
 */
@EventBusSubscriber(modid = ModeMinecraft.MODID, value = Dist.CLIENT)
public final class BookClient {
    private static final WeakHashMap<Screen, ItemPanel> PANELS = new WeakHashMap<>();
    private static final int OPEN_TICKS = 8;
    private static long openStart = -1000;
    private static ItemStack openStack = ItemStack.EMPTY;
    /** Avec JEI, son propre panneau occupe déjà la droite : le nôtre est masqué, sauf si on appuie sur O. */
    private static final boolean JEI = net.neoforged.fml.ModList.get().isLoaded("jei");
    private static boolean showWithJei;

    private static boolean panelWanted() {
        return JEI ? showWithJei : ModConfigs.RECIPE_PANEL.get();
    }

    private static boolean supported(Screen s) {
        return s instanceof InventoryScreen || s instanceof CraftingScreen || s instanceof AbstractFurnaceScreen<?>
                || s instanceof ArmamentTableScreen || s instanceof fr.modeminecraft.client.screen.ModificationTableScreen;
    }

    // ------------------------------------------------------------------ panneau latéral
    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        PANELS.remove(screen);
        if (!supported(screen) || !panelWanted() || !(screen instanceof AbstractContainerScreen<?> c)) {
            return;
        }
        int left = c.getGuiLeft() + c.getXSize() + 8;
        int w = Math.min(18 * 9 + 4, screen.width - left - 6);
        if (w < 40) {
            return;
        }
        ItemPanel panel = new ItemPanel(Minecraft.getInstance().font, left, 6, w, screen.height - 12,
                (stack, uses) -> RecipeViewScreen.open(screen, stack, uses));
        PANELS.put(screen, panel);
        event.addListener(panel.search);
    }

    /** Le panneau occupe la droite : les effets de potion de l'inventaire passent à gauche (version compacte). */
    @SubscribeEvent
    public static void onEffects(ScreenEvent.RenderInventoryMobEffects event) {
        if (PANELS.containsKey(event.getScreen()) && event.getScreen() instanceof AbstractContainerScreen<?> c) {
            int room = c.getGuiLeft();
            if (room >= 36) {
                event.setCompact(true);
                event.setHorizontalOffset(-(c.getXSize() + 2 + 33 + 2));
            } else {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        ItemPanel panel = PANELS.get(event.getScreen());
        if (panel == null) {
            return;
        }
        var g = event.getGuiGraphics();
        panel.render(g, Minecraft.getInstance().font, event.getMouseX(), event.getMouseY());
        ItemStack h = panel.hovered(event.getMouseX(), event.getMouseY());
        if (!h.isEmpty()) {
            g.renderTooltip(Minecraft.getInstance().font, h, event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        ItemPanel panel = PANELS.get(event.getScreen());
        if (panel != null && panel.click(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.getScreen().setFocused(panel.search.isFocused() ? panel.search : null);
            event.setCanceled(true); // sinon l'inventaire croit qu'on clique dehors (et lâche l'objet tenu)
        }
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        ItemPanel panel = PANELS.get(event.getScreen());
        if (panel != null && panel.scroll(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        ItemPanel panel = PANELS.get(screen);
        if (panel != null && panel.search.isFocused()) {
            // On tape dans la recherche : la touche E ne doit pas fermer l'inventaire
            if (event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
                panel.search.setFocused(false);
                screen.setFocused(null);
            } else {
                panel.search.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
            }
            event.setCanceled(true);
            return;
        }
        if (!supported(screen)) {
            return;
        }
        var key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        if (ModKeyMappings.ITEM_PANEL.isActiveAndMatches(key)) {
            if (JEI) {
                showWithJei = !showWithJei;
            } else {
                ModConfigs.RECIPE_PANEL.set(!ModConfigs.RECIPE_PANEL.get());
            }
            Minecraft.getInstance().setScreen(screen); // reconstruit l'écran avec ou sans le panneau
            event.setCanceled(true);
            return;
        }
        boolean recipe = ModKeyMappings.SHOW_RECIPE.isActiveAndMatches(key), uses = ModKeyMappings.SHOW_USES.isActiveAndMatches(key);
        if (!recipe && !uses) {
            return;
        }
        var mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos() * screen.width / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * screen.height / mc.getWindow().getScreenHeight();
        ItemStack hovered = panel != null ? panel.hovered(mx, my) : ItemStack.EMPTY;
        if (hovered.isEmpty() && screen instanceof AbstractContainerScreen<?> c && c.getSlotUnderMouse() != null) {
            hovered = c.getSlotUnderMouse().getItem();
        }
        if (!hovered.isEmpty()) {
            RecipeViewScreen.open(screen, hovered, uses);
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ ouverture du manuel
    public static void startOpening(ItemStack stack) {
        var mc = Minecraft.getInstance();
        if (mc.level != null) {
            openStart = mc.level.getGameTime();
            openStack = stack;
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && !openStack.isEmpty() && mc.level.getGameTime() - openStart >= OPEN_TICKS) {
            openStack = ItemStack.EMPTY;
            if (mc.screen == null) {
                mc.setScreen(new RecipeViewScreen(null, ItemStack.EMPTY, false));
            }
        }
    }

    /** Les deux mains lèvent le manuel devant les yeux et l'ouvrent (il pivote). */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || openStack.isEmpty() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        float t = Math.min(1, (mc.level.getGameTime() + event.getPartialTick() - openStart) / OPEN_TICKS);
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, -0.55f + 0.4f * t, -0.55f);
        pose.mulPose(Axis.XP.rotationDegrees(-60 + 50 * t));
        pose.mulPose(Axis.YP.rotationDegrees(180 * t));
        pose.scale(0.5f, 0.5f, 0.5f);
        mc.getItemRenderer().renderStatic(openStack, ItemDisplayContext.FIXED, event.getPackedLight(), OverlayTexture.NO_OVERLAY, pose,
                event.getMultiBufferSource(), mc.level, 0);
        pose.popPose();
        event.setCanceled(true);
    }

    private BookClient() {}
}
