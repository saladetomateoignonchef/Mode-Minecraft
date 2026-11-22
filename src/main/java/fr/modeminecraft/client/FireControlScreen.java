package fr.modeminecraft.client;

import fr.modeminecraft.entity.military.MilAmmo;
import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.network.RadarPayload;
import fr.modeminecraft.network.StrategicFirePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Console de tir des complexes lance-missiles (clic droit à la place d'opérateur) :
 * - S-400 / S-500 : écran radar circulaire (contacts en temps réel), clic sur les contacts pour les verrouiller
 *   (4 / 2 au plus), ou clic n'importe où ailleurs sur l'écran pour poser un marqueur (X, Z, altitude facultative) ;
 *   choix du missile, LANCER ; clic droit sur le radar : efface le marqueur ; S-500 : mode Sentinelle automatique ;
 * - Iskander : coordonnées X / Z (ou marque du désignateur laser), choix de l'ogive, LANCER ; les dernières
 *   coordonnées saisies restent mémorisées dans le véhicule.
 */
public class FireControlScreen extends Screen {
    private static RadarPayload last;
    private final MilVehicleEntity v;
    private final List<Integer> selected = new ArrayList<>();
    private int ammo;
    private EditBox xBox, zBox, altBox;
    /** Marqueur libre posé sur le radar (décalage en blocs par rapport au véhicule), ou null. */
    private double[] marker;
    private Button ammoButton, fireButton, sentinelButton;
    private static final int R = 92;

    public FireControlScreen(MilVehicleEntity v) {
        super(Component.translatable("gui.modeminecraft.fire_control"));
        this.v = v;
    }

    /** Contacts reçus du serveur. */
    public static void radar(RadarPayload p) {
        last = p;
    }

    /** Saisie de coordonnées X / Z : Iskander et console de tir navale (officier d'armes). */
    private boolean iskander() {
        return v.type() == MilType.ISKANDER || v.type().naval();
    }

    private boolean naval() {
        return v.type().naval();
    }

    private List<MilAmmo> ammoList() {
        if (naval()) {
            int st = v.info().getInt("station");
            return st >= 0 && st < v.type().weapons.size() ? v.type().weapons.get(st).ammo() : List.of();
        }
        return v.type().weapons.isEmpty() ? List.of() : v.type().weapons.get(0).ammo();
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        int px = iskander() ? cx - 80 : cx + 20;
        int py = iskander() ? cy - 40 : cy - 70;
        if (iskander()) {
            xBox = addRenderableWidget(new EditBox(font, px + 20, py, 70, 18, Component.literal("X")));
            zBox = addRenderableWidget(new EditBox(font, px + 110, py, 70, 18, Component.literal("Z")));
            xBox.setFilter(s -> s.matches("-?\\d{0,7}"));
            zBox.setFilter(s -> s.matches("-?\\d{0,7}"));
            var info = v.info();
            if (info.contains("LastTargetX")) {
                xBox.setValue(Integer.toString(info.getInt("LastTargetX")));      // coordonnées mémorisées
                zBox.setValue(Integer.toString(info.getInt("LastTargetZ")));
            } else {
                xBox.setValue(Integer.toString((int) v.getX() + 300));
                zBox.setValue(Integer.toString((int) v.getZ()));
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.fire_control.designator"), b -> {
                Vec3 m = fr.modeminecraft.item.military.LaserDesignatorItem.mark(Minecraft.getInstance().player);
                if (m != null) {
                    xBox.setValue(Integer.toString((int) Math.floor(m.x)));
                    zBox.setValue(Integer.toString((int) Math.floor(m.z)));
                }
            }).bounds(px, py + 24, 180, 18).build());
            py += 48;
        }
        ammoButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (naval()) {
                PacketDistributor.sendToServer(new fr.modeminecraft.network.MilControlPayload(fr.modeminecraft.network.MilControlPayload.AMMO));
            } else {
                ammo = (ammo + 1) % Math.max(1, ammoList().size());
            }
        }).bounds(px, py, 180, 18).build());
        fireButton = addRenderableWidget(Button.builder(Component.translatable("gui.modeminecraft.fire_control.launch"), b -> fire())
                .bounds(px, py + 24, 180, 22).build());
        if (!iskander()) {
            altBox = addRenderableWidget(new EditBox(font, px + 110, py - 24, 70, 18, Component.literal("Alt")));
            altBox.setFilter(s -> s.matches("\\d{0,3}"));
            altBox.setHint(Component.translatable("gui.modeminecraft.fire_control.alt_auto"));
        }
        if (v.type() == MilType.S_500) {
            sentinelButton = addRenderableWidget(Button.builder(Component.empty(), b -> PacketDistributor.sendToServer(
                    new StrategicFirePayload(StrategicFirePayload.TOGGLE_SENTINEL, List.of(), 0, 0, 0))).bounds(px, py + 52, 180, 18).build());
        }
    }

    private void fire() {
        if (iskander()) {
            try {
                int x = Integer.parseInt(xBox.getValue()), z = Integer.parseInt(zBox.getValue());
                PacketDistributor.sendToServer(new StrategicFirePayload(StrategicFirePayload.FIRE_STRIKE, List.of(), ammo, x, z));
            } catch (NumberFormatException ignored) {
            }
        } else if (!selected.isEmpty()) {
            PacketDistributor.sendToServer(new StrategicFirePayload(StrategicFirePayload.FIRE_SAM, List.copyOf(selected), ammo, 0, 0));
            selected.clear();
        } else if (marker != null) {
            int alt = -1;
            try {
                alt = altBox.getValue().isEmpty() ? -1 : Integer.parseInt(altBox.getValue());
            } catch (NumberFormatException ignored) {
            }
            PacketDistributor.sendToServer(new StrategicFirePayload(StrategicFirePayload.FIRE_SAM_POINT, List.of(alt), ammo,
                    (int) Math.floor(v.getX() + marker[0]), (int) Math.floor(v.getZ() + marker[1])));
        }
    }

    @Override
    public void removed() {
        // Iskander : les coordonnées saisies restent mémorisées dans le véhicule (même sans tirer)
        if (iskander() && xBox != null) {
            try {
                PacketDistributor.sendToServer(new StrategicFirePayload(StrategicFirePayload.SAVE_TARGET, List.of(), 0,
                        Integer.parseInt(xBox.getValue()), Integer.parseInt(zBox.getValue())));
            } catch (NumberFormatException ignored) {
            }
        }
        super.removed();
    }

    private int maxTargets() {
        return v.type() == MilType.S_500 ? 2 : 4;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!iskander()) {
            int cx = width / 2 - 70, cy = height / 2;
            double scale = R / fr.modeminecraft.entity.military.StrategicSystems.range(v.type());
            if (Math.hypot(mx - cx, my - cy) <= R && button == 1) {
                marker = null;                                               // clic droit : efface le marqueur
                return true;
            }
            for (RadarPayload.Blip b : last == null ? List.<RadarPayload.Blip>of() : last.blips()) {
                double bx = cx + b.dx() * scale, by = cy + b.dz() * scale;
                if (Math.hypot(mx - bx, my - by) < 5) {
                    if (selected.contains(b.id())) {
                        selected.remove((Integer) b.id());
                    } else if (selected.size() < maxTargets()) {
                        selected.add(b.id());
                    }
                    return true;
                }
            }
            if (Math.hypot(mx - cx, my - cy) <= R && button == 0) {
                marker = new double[]{(mx - cx) / scale, (my - cy) / scale};   // point libre : coordonnées du monde
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        var info = v.info();
        List<MilAmmo> list = ammoList();
        if (naval()) {
            int st = info.getInt("station");
            int n = 0;
            for (MilAmmo a : list) {
                n += info.getInt(a.name());
            }
            ammoButton.setMessage(Component.translatable("gui.modeminecraft.fire_control.station", AirClient.stationName(v), n));
        } else if (!list.isEmpty()) {
            MilAmmo a = list.get(Math.floorMod(ammo, list.size()));
            ammoButton.setMessage(Component.translatable("gui.modeminecraft.fire_control.missile",
                    Component.translatable("item.modeminecraft." + BuiltInRegistries.ITEM.getKey(a.item()).getPath()), info.getInt(a.name())));
        }
        boolean deployed = v.is(MilVehicleEntity.DEPLOYED) || naval();
        fireButton.active = deployed && (iskander() || !selected.isEmpty() || marker != null);
        int top = 12;
        g.drawCenteredString(font, Component.translatable("entity.modeminecraft." + v.type().id), width / 2, top, 0xFFE8D890);
        g.drawCenteredString(font, Component.translatable(deployed ? "gui.modeminecraft.fire_control.deployed" : "gui.modeminecraft.fire_control.stowed"),
                width / 2, top + 12, deployed ? 0xFF60E060 : 0xFFE06060);
        if (iskander()) {
            int cx = width / 2, cy = height / 2;
            g.drawString(font, "X", cx - 76, cy - 36, 0xFFFFFFFF);
            g.drawString(font, "Z", cx + 16, cy - 36, 0xFFFFFFFF);
            try {
                double d = Math.hypot(Integer.parseInt(xBox.getValue()) - v.getX(), Integer.parseInt(zBox.getValue()) - v.getZ());
                if (naval()) {
                    g.drawCenteredString(font, Component.translatable("gui.modeminecraft.fire_control.naval_distance", (int) d),
                            cx, cy + 60, d > 1200 ? 0xFFE06060 : 0xFFD0D0D0);
                } else {
                    g.drawCenteredString(font, Component.translatable("gui.modeminecraft.fire_control.distance", (int) d, 70 / 20 + (int) (d / 7 / 20)),
                            cx, cy + 60, d > 1000 || d < 60 ? 0xFFE06060 : 0xFFD0D0D0);
                }
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if (sentinelButton != null) {
            boolean on = last != null && last.sentinel();
            sentinelButton.setMessage(Component.translatable(on ? "gui.modeminecraft.fire_control.sentinel_on" : "gui.modeminecraft.fire_control.sentinel_off"));
        }
        // écran radar
        int cx = width / 2 - 70, cy = height / 2;
        g.fill(cx - R - 4, cy - R - 4, cx + R + 4, cy + R + 4, 0xE0081008);
        for (int ring = 1; ring <= 3; ring++) {
            circle(g, cx, cy, R * ring / 3, 0xFF1E6A2A);
        }
        g.fill(cx - R, cy, cx + R, cy + 1, 0xFF1E6A2A);
        g.fill(cx, cy - R, cx + 1, cy + R, 0xFF1E6A2A);
        float sweep = (Minecraft.getInstance().level.getGameTime() + partial) * 0.08f;
        for (int k = 0; k < R; k++) {
            g.fill(cx + (int) (Math.cos(sweep) * k), cy + (int) (Math.sin(sweep) * k), cx + (int) (Math.cos(sweep) * k) + 1,
                    cy + (int) (Math.sin(sweep) * k) + 1, 0xFF40FF60);
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        double scale = R / fr.modeminecraft.entity.military.StrategicSystems.range(v.type());
        if (last != null) {
            for (RadarPayload.Blip b : last.blips()) {
                int bx = cx + (int) (b.dx() * scale), by = cy + (int) (b.dz() * scale);
                int col = switch (b.kind()) {
                    case 0 -> 0xFFFF4040;      // hélicoptère
                    case 1 -> 0xFFFFA030;      // drone
                    case 3 -> 0xFF60A0FF;      // avion
                    case 4 -> 0xFFFF40FF;      // missile balistique
                    default -> 0xFFFFFF40;     // missile / roquette
                };
                g.fill(bx - 2, by - 2, bx + 3, by + 3, col);
                if (selected.contains(b.id())) {
                    circle(g, bx, by, 6, 0xFFFFFFFF);
                }
                if (Math.hypot(mouseX - bx, mouseY - by) < 6) {
                    g.renderTooltip(font, Component.translatable("gui.modeminecraft.fire_control.blip." + b.kind(),
                            (int) Math.hypot(b.dx(), b.dz()), (int) b.alt()), mouseX, mouseY);
                }
            }
        }
        if (marker != null) {
            int mx = cx + (int) (marker[0] * scale), my = cy + (int) (marker[1] * scale);
            g.fill(mx - 5, my, mx + 6, my + 1, 0xFFFF3030);
            g.fill(mx, my - 5, mx + 1, my + 6, 0xFFFF3030);
            circle(g, mx, my, 4, 0xFFFF3030);
            g.drawString(font, Component.translatable("gui.modeminecraft.fire_control.marker", (int) Math.floor(v.getX() + marker[0]),
                    (int) Math.floor(v.getZ() + marker[1]), (int) Math.hypot(marker[0], marker[1])), cx - R, cy + R + 32, 0xFFFF6060);
        }
        if (altBox != null) {
            g.drawString(font, Component.translatable("gui.modeminecraft.fire_control.alt"), altBox.getX() - 70, altBox.getY() + 5, 0xFFD0D0D0);
        }
        g.drawString(font, Component.translatable("gui.modeminecraft.fire_control.locked", selected.size(), maxTargets()), cx - R, cy + R + 8, 0xFFD0D0D0);
        g.drawString(font, Component.translatable("gui.modeminecraft.fire_control.range", (int) fr.modeminecraft.entity.military.StrategicSystems.range(v.type())),
                cx - R, cy + R + 20, 0xFF60E060);
    }

    private static void circle(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int i = 0; i < 360; i += 3) {
            double a = Math.toRadians(i);
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (v.isRemoved() || Minecraft.getInstance().player == null || Minecraft.getInstance().player.getVehicle() != v) {
            onClose();
        }
        selected.removeIf(id -> last == null || last.blips().stream().noneMatch(b -> b.id() == id));
    }
}
