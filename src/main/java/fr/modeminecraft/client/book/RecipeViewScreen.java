package fr.modeminecraft.client.book;

import fr.modeminecraft.client.ModKeyMappings;
import fr.modeminecraft.client.screen.ArmamentTableScreen;
import fr.modeminecraft.client.screen.GuiDraw;
import fr.modeminecraft.recipe.ArmamentRecipe;
import fr.modeminecraft.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Livre des recettes / encyclopédie (façon Not Enough Items). Fenêtre centrale avec un onglet par station
 * (établi 3x3, établi d'armurier, fonderie / haut-fourneau, génération naturelle, fiche technique) et le panneau
 * d'objets à droite. Clic gauche / R = recette, clic droit / U = utilisations, Retour arrière = page précédente.
 * Ouvert depuis un établi, un four ou la table d'armement : le bouton [+] y place les ingrédients.
 */
public class RecipeViewScreen extends Screen {
    private static final int WW = 236, WH = 206;
    private record State(ItemStack stack, boolean uses, RecipeIndex.Station tab, int page) {}
    private record Shown(int x, int y, ItemStack stack) {}

    private final Screen parent;
    private final Deque<State> history = new ArrayDeque<>();
    private ItemStack stack;
    private boolean uses;
    private RecipeIndex.Station tab;
    private int page;
    private ItemPanel panel;
    private int wx, wy;
    private Button plus;
    private final List<Shown> shown = new ArrayList<>();
    private Set<Integer> missing = new HashSet<>();
    private String pendingQuery;

    public RecipeViewScreen(Screen parent, ItemStack stack, boolean uses) {
        super(Component.translatable("item.modeminecraft.military_field_manual"));
        this.parent = parent;
        this.stack = stack.copyWithCount(1);
        this.uses = uses;
    }

    public static void open(Screen parent, ItemStack stack, boolean uses) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new RecipeViewScreen(parent, stack, uses));
    }

    @Override
    protected void init() {
        int panelW = Math.max(0, Math.min(18 * 9 + 4, width - WW - 30));
        int total = WW + (panelW >= 40 ? panelW + 12 : 0);
        wx = (width - total) / 2;
        wy = (height - WH) / 2;
        if (panelW >= 40) {
            panel = new ItemPanel(font, wx + WW + 12, wy, panelW, WH, this::navigate);
            addRenderableWidget(panel.search);
            if (pendingQuery != null) {
                panel.setQuery(pendingQuery);
                pendingQuery = null;
            }
        }
        plus = addRenderableWidget(Button.builder(Component.literal("+"), b -> transfer())
                .bounds(wx + WW - 24, wy + WH - 24, 18, 18).build());
        if (tab == null || !tabs().contains(tab)) {
            tab = tabs().get(0);
        }
    }

    /** Choisit un onglet (s'il existe pour cet objet). */
    public void selectTab(RecipeIndex.Station station) {
        if (tabs().contains(station)) {
            tab = station;
            page = 0;
        }
    }

    // ------------------------------------------------------------------ navigation
    private void navigate(ItemStack s, boolean u) {
        if (s.isEmpty()) {
            return;
        }
        history.push(new State(stack, uses, tab, page));
        stack = s.copyWithCount(1);
        uses = u;
        page = 0;
        tab = tabs().get(0);
    }

    private void back() {
        if (!history.isEmpty()) {
            State st = history.pop();
            stack = st.stack();
            uses = st.uses();
            tab = st.tab();
            page = st.page();
        }
    }

    private List<RecipeIndex.Station> tabs() {
        List<RecipeIndex.Station> t = new ArrayList<>();
        if (!stack.isEmpty()) {
            for (RecipeIndex.Station s : new RecipeIndex.Station[]{RecipeIndex.Station.CRAFTING, RecipeIndex.Station.ARMAMENT, RecipeIndex.Station.COOKING}) {
                if (!recipes(s).isEmpty()) {
                    t.add(s);
                }
            }
            if (!uses && RecipeIndex.hasWorldGen(stack)) {
                t.add(RecipeIndex.Station.WORLDGEN);
            }
        }
        t.add(RecipeIndex.Station.STATS);
        return t;
    }

    private List<RecipeHolder<?>> recipes(RecipeIndex.Station s) {
        return uses ? RecipeIndex.using(stack, s) : RecipeIndex.producing(stack, s);
    }

    private RecipeHolder<?> current() {
        if (tab == null || tab == RecipeIndex.Station.STATS || tab == RecipeIndex.Station.WORLDGEN) {
            return null;
        }
        List<RecipeHolder<?>> list = recipes(tab);
        if (list.isEmpty()) {
            return null;
        }
        page = Math.floorMod(page, list.size());
        return list.get(page);
    }

    private static ItemStack tabIcon(RecipeIndex.Station s) {
        return switch (s) {
            case CRAFTING -> new ItemStack(Items.CRAFTING_TABLE);
            case ARMAMENT -> new ItemStack(ModItems.ARMAMENT_TABLE.get());
            case COOKING -> new ItemStack(Items.BLAST_FURNACE);
            case WORLDGEN -> new ItemStack(Items.IRON_PICKAXE);
            case STATS -> new ItemStack(ModItems.MILITARY_FIELD_MANUAL.get());
        };
    }

    // ------------------------------------------------------------------ rendu
    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        shown.clear();
        missing = new HashSet<>();
        super.render(g, mx, my, partialTick);
        GuiDraw.panel(g, wx, wy, WW, WH);
        if (panel != null) {
            panel.render(g, font, mx, my);
        }
        RecipeHolder<?> recipe = null;
        if (stack.isEmpty()) {
            renderHome(g, mx, my);
        } else {
            // En-tête : objet, recettes / utilisations
            g.renderItem(stack, wx + 8, wy + 7);
            g.drawString(font, font.plainSubstrByWidth(stack.getHoverName().getString(), WW - 40), wx + 28, wy + 6, 0xFF202020, false);
            drawToggle(g, mx, my, wx + 28, wy + 17, Component.translatable("gui.modeminecraft.book.recipes"), !uses);
            drawToggle(g, mx, my, wx + 108, wy + 17, Component.translatable("gui.modeminecraft.book.uses"), uses);
            // Onglets des stations
            List<RecipeIndex.Station> tabs = tabs();
            for (int i = 0; i < tabs.size(); i++) {
                int tx = wx + 8 + i * 26, ty = wy + 32;
                boolean active = tabs.get(i) == tab;
                g.fill(tx, ty, tx + 24, ty + 22, active ? 0xFFC6C6C6 : 0xFF8B8B8B);
                g.renderOutline(tx, ty, 24, 22, active ? 0xFF000000 : 0xFF555555);
                g.renderItem(tabIcon(tabs.get(i)), tx + 4, ty + 3);
                if (mx >= tx && mx < tx + 24 && my >= ty && my < ty + 22) {
                    g.renderTooltip(font, Component.translatable("gui.modeminecraft.book.tab." + tabs.get(i).name().toLowerCase()), mx, my);
                }
            }
            g.fill(wx + 6, wy + 54, wx + WW - 6, wy + 55, 0xFF555555);
            recipe = current();
            switch (tab) {
                case CRAFTING -> renderCrafting(g, recipe);
                case ARMAMENT -> renderArmament(g, recipe);
                case COOKING -> renderCooking(g, recipe);
                case WORLDGEN -> renderWorldGen(g);
                case STATS -> renderStats(g);
            }
            if (recipe != null) {
                int n = recipes(tab).size();
                if (n > 1) {
                    g.drawCenteredString(font, "◀  " + (page + 1) + " / " + n + "  ▶", wx + WW / 2, wy + WH - 18, 0xFF303030);
                }
            }
        }
        // [+] : seulement si la station correspond à l'écran d'où l'on vient
        boolean canPlace = recipe != null && placeTarget(recipe) != null;
        plus.visible = canPlace;
        plus.active = canPlace && missing.isEmpty();
        plus.setTooltip(Tooltip.create(Component.translatable(missing.isEmpty() ? "gui.modeminecraft.book.transfer"
                : "gui.modeminecraft.book.transfer_missing")));
        for (Shown s : shown) {
            if (!s.stack().isEmpty() && mx >= s.x() && mx < s.x() + 16 && my >= s.y() && my < s.y() + 16) {
                g.renderTooltip(font, s.stack(), mx, my);
                return;
            }
        }
        if (panel != null) {
            ItemStack h = panel.hovered(mx, my);
            if (!h.isEmpty()) {
                g.renderTooltip(font, h, mx, my);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partialTick) {
        renderTransparentBackground(g);
    }

    private void drawToggle(GuiGraphics g, int mx, int my, int x, int y, Component text, boolean on) {
        int w = 76;
        g.fill(x, y, x + w, y + 12, on ? 0xFF4A6A3A : (mx >= x && mx < x + w && my >= y && my < y + 12 ? 0xFF6A6A6A : 0xFF8B8B8B));
        g.drawCenteredString(font, text, x + w / 2, y + 2, 0xFFFFFFFF);
    }

    private void slot(GuiGraphics g, int x, int y, ItemStack s, boolean red) {
        GuiDraw.slot(g, x - 1, y - 1);
        if (red) {
            g.fill(x, y, x + 16, y + 16, 0x80FF2020);
        }
        if (!s.isEmpty()) {
            g.renderItem(s, x, y);
            g.renderItemDecorations(font, s, x, y);
        }
        shown.add(new Shown(x, y, s));
    }

    /** Un ingrédient change d'aspect chaque seconde quand il accepte plusieurs objets (tag). */
    private static ItemStack cycle(Ingredient ing) {
        ItemStack[] items = ing.getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        long t = System.currentTimeMillis() / 1000;
        return items[(int) (t % items.length)];
    }

    private void caption(GuiGraphics g, Component c) {
        g.drawString(font, c, wx + 10, wy + 60, 0xFF404040, false);
    }

    private void renderCrafting(GuiGraphics g, RecipeHolder<?> h) {
        if (!(h != null && h.value() instanceof CraftingRecipe r)) {
            return;
        }
        caption(g, Component.translatable("gui.modeminecraft.book.tab.crafting"));
        List<Ingredient> ings = r.getIngredients();
        int w = r instanceof ShapedRecipe s ? s.getWidth() : 3;
        List<Ingredient> flat = new ArrayList<>(ings);
        List<Integer> red = missingSlots(flat.stream().map(i -> 1).toList(), flat);
        int gx = wx + 30, gy = wy + 82;
        for (int i = 0; i < 9; i++) {
            int col = i % 3, row = i / 3;
            int idx = r instanceof ShapedRecipe ? (col < w ? row * w + col : -1) : i;
            Ingredient ing = idx >= 0 && idx < flat.size() ? flat.get(idx) : Ingredient.EMPTY;
            slot(g, gx + col * 18, gy + row * 18, cycle(ing), red.contains(idx));
        }
        g.drawString(font, "→", gx + 62, gy + 22, 0xFF404040, false);
        slot(g, gx + 82, gy + 18, RecipeIndex.result(r), false);
        if (r instanceof ShapedRecipe s && s.getWidth() <= 2 && s.getHeight() <= 2 || !(r instanceof ShapedRecipe) && ings.size() <= 4) {
            g.drawWordWrap(font, Component.translatable("gui.modeminecraft.book.fits_2x2"), wx + 10, gy + 60, WW - 20, 0xFF2A6A2A);
        }
    }

    private void renderArmament(GuiGraphics g, RecipeHolder<?> h) {
        if (!(h != null && h.value() instanceof ArmamentRecipe r)) {
            return;
        }
        caption(g, Component.translatable("gui.modeminecraft.book.tab.armament").append(" — ")
                .append(Component.translatable("gui.modeminecraft.category." + r.category().getSerializedName())));
        List<Ingredient> ings = RecipeIndex.ingredients(r);
        List<Integer> counts = r.ingredients().stream().map(si -> si.count()).toList();
        List<Integer> red = missingSlots(counts, ings);
        for (int i = 0; i < ings.size(); i++) {
            ItemStack s = cycle(ings.get(i)).copyWithCount(counts.get(i));
            slot(g, wx + 12 + (i % 5) * 20, wy + 78 + (i / 5) * 22, s, red.contains(i));
        }
        // Le modèle 3D de l'objet obtenu, en grand
        g.pose().pushPose();
        g.pose().translate(wx + 158, wy + 78, 0);
        g.pose().scale(3.5f, 3.5f, 1);
        g.renderItem(r.result(), 0, 0);
        g.pose().popPose();
        shown.add(new Shown(wx + 158, wy + 78, r.result()));
        if (r.result().getCount() > 1) {
            g.drawString(font, "x" + r.result().getCount(), wx + 205, wy + 130, 0xFF303030, false);
        }
    }

    private void renderCooking(GuiGraphics g, RecipeHolder<?> h) {
        if (!(h != null && h.value() instanceof AbstractCookingRecipe r)) {
            return;
        }
        String kind = r.getType() == RecipeType.BLASTING ? "blasting" : r.getType() == RecipeType.SMOKING ? "smoking"
                : r.getType() == RecipeType.CAMPFIRE_COOKING ? "campfire" : "smelting";
        caption(g, Component.translatable("gui.modeminecraft.book.cook." + kind));
        List<Integer> red = missingSlots(List.of(1), r.getIngredients());
        slot(g, wx + 50, wy + 90, cycle(r.getIngredients().get(0)), red.contains(0));
        g.renderItem(new ItemStack(Items.COAL), wx + 50, wy + 112);
        g.drawString(font, "→", wx + 84, wy + 94, 0xFF404040, false);
        slot(g, wx + 110, wy + 90, RecipeIndex.result(r), false);
        g.drawString(font, Component.translatable("gui.modeminecraft.book.cook_time", String.format("%.1f", r.getCookingTime() / 20f),
                String.format("%.1f", r.getExperience())), wx + 20, wy + 140, 0xFF404040, false);
    }

    private void renderWorldGen(GuiGraphics g) {
        boolean titanium = !RecipeIndex.normalize(stack.getHoverName().getString()).contains("rubis");
        int min = -64, max = titanium ? 16 : 32, best = titanium ? -32 : -16;
        caption(g, Component.translatable("gui.modeminecraft.book.tab.worldgen"));
        // Diagramme des couches : de Y = -64 (bas) à Y = 80 (haut)
        int x0 = wx + 16, y0 = wy + 72, h = 120, top = 80, bottom = -64;
        java.util.function.IntUnaryOperator yOf = yy -> y0 + (int) ((top - yy) / (float) (top - bottom) * h);
        g.fill(x0, y0, x0 + 26, yOf.applyAsInt(0), 0xFF7A7A7A);                  // pierre
        g.fill(x0, yOf.applyAsInt(0), x0 + 26, y0 + h, 0xFF46464D);              // ardoise des abîmes
        g.fill(x0, yOf.applyAsInt(max), x0 + 26, yOf.applyAsInt(min), titanium ? 0x90B4C0CC : 0x90E04A62);
        g.fill(x0 - 3, yOf.applyAsInt(best), x0 + 29, yOf.applyAsInt(best) + 1, 0xFFFFD040);
        for (int yy : new int[]{64, 16, 0, -32, -64}) {
            g.drawString(font, "Y " + yy, x0 + 32, yOf.applyAsInt(yy) - 4, yy == best ? 0xFFB08000 : 0xFF404040, false);
        }
        int tx = wx + 92, tw = WW - 100;
        int ty = wy + 74;
        ty = wrap(g, Component.translatable("gui.modeminecraft.book.wg.layers", min, max), tx, ty, tw, 0xFF303030);
        ty = wrap(g, Component.translatable("gui.modeminecraft.book.wg.best", best), tx, ty, tw, 0xFFB08000);
        ty = wrap(g, Component.translatable(titanium ? "gui.modeminecraft.book.wg.rarity_ti" : "gui.modeminecraft.book.wg.rarity_rubis"), tx, ty, tw, 0xFF303030);
        ty = wrap(g, Component.translatable("gui.modeminecraft.book.wg.tool"), tx, ty, tw, 0xFF303030);
        g.renderItem(new ItemStack(Items.IRON_PICKAXE), tx, ty);
        g.renderItem(new ItemStack(Items.DIAMOND_PICKAXE), tx + 20, ty);
        wrap(g, Component.translatable("gui.modeminecraft.book.wg.smelt"), tx, ty + 20, tw, 0xFF303030);
    }

    private int wrap(GuiGraphics g, Component c, int x, int y, int w, int color) {
        for (var seq : font.split(c, w)) {
            g.drawString(font, seq, x, y, color, false);
            y += 10;
        }
        return y + 3;
    }

    private void renderStats(GuiGraphics g) {
        caption(g, Component.translatable("gui.modeminecraft.book.tab.stats"));
        int y = wy + 74;
        for (Component c : StatSheet.lines(stack)) {
            for (var seq : font.split(c, WW - 74)) {
                if (y > wy + WH - 14) {
                    break;
                }
                g.drawString(font, seq, wx + 10, y, 0xFF303030, false);
                y += 10;
            }
        }
        g.pose().pushPose();
        g.pose().translate(wx + WW - 58, wy + 70, 0);
        g.pose().scale(3f, 3f, 1);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    private record Category(String key, String query, Item icon) {}

    private List<Category> categories() {
        return List.of(new Category("guns", "@gun", ModItems.M4.get()),
                new Category("melee", "@melee", ModItems.RUBIS_SWORD.get()),
                new Category("ammo", "@ammo", ModItems.AMMO_556.get()),
                new Category("attachments", "@accessoire", ModItems.SILENCER.get()),
                new Category("explosives", "@explosif", ModItems.FRAG_GRENADE.get()),
                new Category("turrets", "@tourelle", ModItems.MG42.get()),
                new Category("helmets", "@casque", ModItems.OPS_CORE_FAST.get()),
                new Category("vests", "@gilet", ModItems.JPC_PLATE_CARRIER.get()),
                new Category("plates", "@plaque", ModItems.CERAMIC_BALLISTIC_PLATE.get()),
                new Category("pants", "@pantalon", ModItems.CRYE_G3_PANTS.get()),
                new Category("boots", "@rangers", ModItems.M65_COMBAT_BOOTS.get()),
                new Category("materials", "@material", ModItems.TITANIUM_INGOT.get()));
    }

    private void renderHome(GuiGraphics g, int mx, int my) {
        int y = wy + 7;
        for (var seq : font.split(Component.translatable("item.modeminecraft.military_field_manual"), WW - 20)) {
            g.drawString(font, seq, wx + (WW - font.width(seq)) / 2, y, 0xFF2A3A20, false);
            y += 10;
        }
        y += 4;
        for (String k : new String[]{"home1", "home2", "home3"}) {
            for (var seq : font.split(Component.translatable("gui.modeminecraft.book." + k), WW - 20)) {
                g.drawString(font, seq, wx + 10, y, 0xFF404040, false);
                y += 10;
            }
        }
        List<Category> cats = categories();
        for (int i = 0; i < cats.size(); i++) {
            int bx = wx + 10 + (i % 2) * 110, by = wy + 88 + (i / 2) * 19;
            boolean hover = mx >= bx && mx < bx + 106 && my >= by && my < by + 18;
            g.fill(bx, by, bx + 106, by + 18, hover ? 0xFF6A7A5A : 0xFF8B8B8B);
            g.renderItem(new ItemStack(cats.get(i).icon()), bx + 1, by + 1);
            g.drawString(font, Component.translatable("gui.modeminecraft.book.cat." + cats.get(i).key()), bx + 20, by + 5, 0xFFFFFFFF, false);
        }
    }

    // ------------------------------------------------------------------ ingrédients manquants et transfert [+]
    /** Ingrédients que le joueur n'a pas (cases surlignées en rouge). */
    private List<Integer> missingSlots(List<Integer> counts, List<Ingredient> ings) {
        Map<Integer, Integer> stock = new HashMap<>();
        var inv = minecraft.player.getInventory();
        for (int s = 0; s < inv.getContainerSize(); s++) {
            stock.put(s, inv.getItem(s).getCount());
        }
        List<Integer> red = new ArrayList<>();
        for (int i = 0; i < ings.size(); i++) {
            Ingredient ing = ings.get(i);
            if (ing.isEmpty()) {
                continue;
            }
            int need = counts.get(Math.min(i, counts.size() - 1));
            for (int s = 0; s < inv.getContainerSize() && need > 0; s++) {
                if (ing.test(inv.getItem(s))) {
                    int take = Math.min(need, stock.get(s));
                    stock.put(s, stock.get(s) - take);
                    need -= take;
                }
            }
            if (need > 0) {
                red.add(i);
                missing.add(i);
            }
        }
        return red;
    }

    /** L'écran où l'on peut placer cette recette, ou null. */
    private Screen placeTarget(RecipeHolder<?> h) {
        if (parent == null) {
            return null;
        }
        var r = h.value();
        if (r instanceof ArmamentRecipe && parent instanceof ArmamentTableScreen) {
            return parent;
        }
        if (r instanceof CraftingRecipe c && (parent instanceof CraftingScreen || parent instanceof InventoryScreen && c.canCraftInDimensions(2, 2))) {
            return parent;
        }
        if (r instanceof AbstractCookingRecipe) {
            if (r.getType() == RecipeType.SMELTING && parent instanceof FurnaceScreen || r.getType() == RecipeType.BLASTING && parent instanceof BlastFurnaceScreen
                    || r.getType() == RecipeType.SMOKING && parent instanceof SmokerScreen) {
                return parent;
            }
        }
        return null;
    }

    private void transfer() {
        RecipeHolder<?> h = current();
        if (h == null || placeTarget(h) == null) {
            return;
        }
        minecraft.setScreen(parent);
        if (h.value() instanceof ArmamentRecipe r && parent instanceof ArmamentTableScreen table) {
            table.selectRecipe(r);
        } else if (parent instanceof AbstractContainerScreen<?> c && minecraft.gameMode != null) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new fr.modeminecraft.network.PlaceRecipePayload(c.getMenu().containerId, h.id(), Screen.hasShiftDown()));
        }
    }

    // ------------------------------------------------------------------ souris et clavier
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (panel != null && panel.click(mx, my, button)) {
            setFocused(panel.search.isFocused() ? panel.search : null);
            return true;
        }
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (stack.isEmpty()) {
            List<Category> cats = categories();
            for (int i = 0; i < cats.size(); i++) {
                int bx = wx + 10 + (i % 2) * 110, by = wy + 88 + (i / 2) * 19;
                if (mx >= bx && mx < bx + 106 && my >= by && my < by + 18 && panel != null) {
                    panel.setQuery(cats.get(i).query());
                    return true;
                }
            }
            return false;
        }
        if (my >= wy + 17 && my < wy + 29) {
            if (mx >= wx + 28 && mx < wx + 104) {
                uses = false;
                page = 0;
                tab = tabs().get(0);
                return true;
            }
            if (mx >= wx + 108 && mx < wx + 184) {
                uses = true;
                page = 0;
                tab = tabs().get(0);
                return true;
            }
        }
        List<RecipeIndex.Station> tabs = tabs();
        for (int i = 0; i < tabs.size(); i++) {
            int tx = wx + 8 + i * 26, ty = wy + 32;
            if (mx >= tx && mx < tx + 24 && my >= ty && my < ty + 22) {
                tab = tabs.get(i);
                page = 0;
                return true;
            }
        }
        if (my >= wy + WH - 20 && my < wy + WH - 8 && mx >= wx + 60 && mx < wx + WW - 60) {
            page += mx < wx + WW / 2 ? -1 : 1;
            return true;
        }
        for (Shown s : shown) {
            if (!s.stack().isEmpty() && mx >= s.x() && mx < s.x() + 16 && my >= s.y() && my < s.y() + 16) {
                navigate(s.stack(), button == 1);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (panel != null && panel.scroll(mx, my, dy)) {
            return true;
        }
        if (current() != null) {
            page += dy < 0 ? 1 : -1;
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    private ItemStack hoveredStack(double mx, double my) {
        for (Shown s : shown) {
            if (!s.stack().isEmpty() && mx >= s.x() && mx < s.x() + 16 && my >= s.y() && my < s.y() + 16) {
                return s.stack();
            }
        }
        return panel == null ? ItemStack.EMPTY : panel.hovered(mx, my);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        boolean typing = panel != null && panel.search.isFocused();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (typing) {
                panel.search.setFocused(false);
                setFocused(null);
                return true;
            }
            onClose();
            return true;
        }
        if (typing) {
            return panel.search.keyPressed(key, scan, modifiers);
        }
        var k = InputConstants.getKey(key, scan);
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            back();
            return true;
        }
        double mx = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
        double my = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
        if (ModKeyMappings.SHOW_RECIPE.isActiveAndMatches(k) || ModKeyMappings.SHOW_USES.isActiveAndMatches(k)) {
            ItemStack h = hoveredStack(mx, my);
            if (!h.isEmpty()) {
                navigate(h, ModKeyMappings.SHOW_USES.isActiveAndMatches(k));
            }
            return true;
        }
        if (minecraft.options.keyInventory.isActiveAndMatches(k)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
