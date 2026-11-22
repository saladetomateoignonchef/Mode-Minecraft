package fr.modeminecraft.client;

import fr.modeminecraft.entity.military.MilType;
import fr.modeminecraft.entity.military.MilVehicleEntity;
import fr.modeminecraft.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Côté client des navires : touche V = vue suivante (normale, sonar passif, sonar actif, périscope optique, périscope
 * thermique — périscope des sous-marins seulement, jusqu'à 18 blocs d'immersion) ; HUD naval (chadburn, nœuds,
 * immersion, voies d'eau, station d'armes) ; écran sonar circulaire.
 * Sonar passif : relèvements des bâtiments qui font du bruit (en route), sans distance. Sonar actif : un « ping »
 * toutes les 3 s, échos avec distance, sous-marins en plongée compris (mais il trahit sa propre position).
 */
public final class NavalClient {
    public static final int NORMAL = 0, SONAR_PASSIVE = 1, SONAR_ACTIVE = 2, PERISCOPE = 3, PERISCOPE_IR = 4;
    private static int view;
    private static long lastPing;
    private static final int R = 70;
    private static final double RANGE = 300;

    private NavalClient() {}

    public static int view() {
        return view;
    }

    public static void reset() {
        view = NORMAL;
    }

    /** Touche V : vue suivante. */
    public static void cycle(MilVehicleEntity v) {
        int n = v.type().submarine() ? 5 : 3;
        view = (view + 1) % n;
        if (view >= PERISCOPE && v.depth() > 18) {
            view = NORMAL;                                                   // trop profond pour sortir le périscope
        }
        var p = Minecraft.getInstance().player;
        if (p != null) {
            p.displayClientMessage(Component.translatable("hud.modeminecraft.naval.view." + view), true);
            p.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, 1.4f);
        }
    }

    public static boolean thermal() {
        return view == PERISCOPE_IR;
    }

    /** Caméra du périscope : tête optique 1,5 bloc au-dessus de la surface, à l'aplomb du kiosque. */
    public static Vec3 periscope(MilVehicleEntity v, float partial) {
        if (view < PERISCOPE || !v.type().submarine()) {
            return null;
        }
        double surf = v.waterSurface();
        if (Double.isNaN(surf) || v.depth() > 18) {
            return null;
        }
        Vec3 base = v.getPosition(partial);
        Vec3 sail = v.toWorld(MilVehicleEntity.dims(v.type())[0] * 0.15, 0, 0).subtract(v.position());
        return new Vec3(base.x + sail.x, surf + 1.5, base.z + sail.z);
    }

    /** Lignes du HUD naval (sous le panneau du véhicule). */
    public static int hud(GuiGraphics g, MilVehicleEntity v, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        var font = mc.font;
        MilType t = v.type();
        var info = v.info();
        float knots = Math.abs(v.speed()) / 0.0111f / 1.852f;
        float throttle = v.isControlledByLocalInstance() ? v.throttle() : info.getFloat("throttle");
        String order = throttle > 0.9f ? "full" : throttle > 0.55f ? "half" : throttle > 0.05f ? "slow" : throttle < -0.05f ? "astern" : "stop";
        g.drawString(font, Component.translatable("hud.modeminecraft.naval.telegraph", Component.translatable("hud.modeminecraft.naval.order." + order),
                Math.round(throttle * 100), Math.round(knots)), x, y, 0xFFB0E0FF);
        y += 11;
        if (t.submarine()) {
            int depth = (int) Math.round(v.depth());
            g.drawString(font, Component.translatable(v.is(MilVehicleEntity.SUBMERGED) ? "hud.modeminecraft.naval.depth" : "hud.modeminecraft.naval.surfaced",
                    depth, 50), x, y, depth > 40 ? 0xFFFF9040 : 0xFF80D0FF);
            g.fill(x + 150, y + 1, x + 152, y + 9, 0xFF203040);
            g.fill(x + 150, y + 1 + (int) (8 * Math.min(1, depth / 50f)), x + 152, y + 2 + (int) (8 * Math.min(1, depth / 50f)), 0xFF80D0FF);
            y += 11;
        }
        if (v.officer(mc.player) || info.getInt("station") >= 0 && !t.stations().isEmpty()) {
            g.drawString(font, Component.translatable("hud.modeminecraft.naval.station", AirClient.stationName(v)), x, y, 0xFFFFE080);
            y += 11;
        }
        float flood = info.getFloat("flood");
        if (flood > 0.01f) {
            g.drawString(font, Component.translatable("hud.modeminecraft.naval.flooding", Math.round(flood * 100)), x, y,
                    (v.tickCount / 8) % 2 == 0 ? 0xFFFF4040 : 0xFFB03030);
            y += 11;
        }
        if (t == MilType.EXPLOSIVE_USV) {
            g.drawString(font, Component.translatable(info.getBoolean("usv") ? "hud.modeminecraft.naval.usv_armed" : "hud.modeminecraft.naval.usv_safe"),
                    x, y, info.getBoolean("usv") ? 0xFFFF4040 : 0xFF90E090);
            y += 11;
        }
        g.drawString(font, Component.translatable("hud.modeminecraft.naval.keys" + (t.submarine() ? "_sub" : "")), x, y, 0xFF909090);
        y += 11;
        if (v.is(MilVehicleEntity.SINKING)) {
            g.drawCenteredString(font, Component.translatable("hud.modeminecraft.naval.sinking"), g.guiWidth() / 2, g.guiHeight() / 2 - 40, 0xFFFF3030);
        }
        return y;
    }

    /** Écran sonar (en bas à droite) et effets du périscope. */
    public static void overlay(GuiGraphics g, MilVehicleEntity v, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (view == PERISCOPE || view == PERISCOPE_IR) {
            int w = g.guiWidth(), h = g.guiHeight(), cx = w / 2, cy = h / 2, r = Math.min(w, h) / 2 - 10;
            // masque circulaire de l'oculaire, réticule gradué
            for (int yy = 0; yy < h; yy += 2) {
                int dy = yy - cy;
                int half = dy * dy < r * r ? (int) Math.sqrt(r * r - dy * dy) : 0;
                g.fill(0, yy, cx - half, yy + 2, 0xFF000000);
                g.fill(cx + half, yy, w, yy + 2, 0xFF000000);
            }
            g.fill(cx - r, cy, cx + r, cy + 1, 0xC0101010);
            g.fill(cx, cy - r, cx + 1, cy + r, 0xC0101010);
            for (int k = -5; k <= 5; k++) {
                g.fill(cx + k * r / 6, cy - 4, cx + k * r / 6 + 1, cy + 5, 0xC0101010);
            }
            int bearing = Math.round(Mth.wrapDegrees(mc.player.getYRot() + 180) + 180) % 360;
            g.drawCenteredString(mc.font, Component.translatable("hud.modeminecraft.naval.bearing", bearing), cx, cy + r - 20, 0xFFE0E0A0);
            if (view == PERISCOPE_IR) {
                g.fill(0, 0, w, h, 0x2840FF40);
            }
            return;
        }
        if (view != SONAR_PASSIVE && view != SONAR_ACTIVE) {
            return;
        }
        long now = mc.level.getGameTime();
        boolean active = view == SONAR_ACTIVE;
        if (active && now - lastPing >= 60) {
            lastPing = now;
            mc.player.playSound(ModSounds.SONAR_PING.get(), 1f, 1f);
        }
        int cx = g.guiWidth() - R - 12, cy = g.guiHeight() - R - 40;
        g.fill(cx - R - 4, cy - R - 4, cx + R + 4, cy + R + 16, 0xE0041018);
        for (int ring = 1; ring <= 3; ring++) {
            circle(g, cx, cy, R * ring / 3, 0xFF124050);
        }
        // l'avant du navire en haut de l'écran
        float yaw = Mth.lerp(partial, v.yRotO, v.getYRot());
        double rad = Math.toRadians(yaw);
        float pingAge = (now - lastPing + partial) / 60f;
        if (active) {
            circle(g, cx, cy, (int) (R * Math.min(1, pingAge * 1.5f)), 0xFF40E0FF);
        }
        for (MilVehicleEntity o : mc.level.getEntitiesOfClass(MilVehicleEntity.class, v.getBoundingBox().inflate(RANGE, 80, RANGE),
                o -> o != v && (o.type().naval() || !o.type().heli() && o.isInWater()))) {
            Vec3 to = o.position().subtract(v.position());
            double d = Math.hypot(to.x, to.z);
            if (d > RANGE) {
                continue;
            }
            // repère du navire : avant = haut, tribord = droite
            double fwd = to.x * -Math.sin(rad) + to.z * Math.cos(rad), right = to.x * -Math.cos(rad) + to.z * -Math.sin(rad);
            double k = R / RANGE;
            boolean noisy = Math.abs(o.speed()) > 0.08f || o.getControllingPassenger() != null && !o.type().submarine();
            if (active) {
                if (pingAge * 1.5f * RANGE < d) {
                    continue;                                                // l'écho n'est pas encore revenu
                }
                int bx = cx + (int) (right * k), by = cy - (int) (fwd * k);
                int col = o.type().submarine() ? 0xFFFF60FF : 0xFFFFE040;
                g.fill(bx - 2, by - 2, bx + 3, by + 3, col);
                g.drawString(mc.font, (int) d + "", bx + 4, by - 4, col);
            } else if (noisy && !(o.type() == MilType.ZUMWALT && d > 150)) {
                // passif : relèvement seulement (trait jusqu'au bord)
                double len = Math.hypot(fwd, right);
                for (int s = 8; s < R; s += 2) {
                    g.fill(cx + (int) (right / len * s), cy - (int) (fwd / len * s), cx + (int) (right / len * s) + 1, cy - (int) (fwd / len * s) + 1, 0xFF40FFB0);
                }
            }
        }
        g.fill(cx - 1, cy - 4, cx + 2, cy + 5, 0xFFFFFFFF);
        g.drawCenteredString(mc.font, Component.translatable(active ? "hud.modeminecraft.naval.sonar_active" : "hud.modeminecraft.naval.sonar_passive"),
                cx, cy + R + 4, active ? 0xFF40E0FF : 0xFF40FFB0);
    }

    private static void circle(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int i = 0; i < 360; i += 3) {
            double a = Math.toRadians(i);
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }
}
