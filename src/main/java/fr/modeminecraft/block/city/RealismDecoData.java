package fr.modeminecraft.block.city;

/** GÉNÉRÉ par tools/generate_realism_decor.py : ne pas modifier à la main. Décors de réalisme (aéroport, hôpital, gare, métro). */
public final class RealismDecoData {
    /** Un décor : identifiant, lumière émise, sans collision, forme (pixels, face avant au nord). */
    public record Def(String id, int light, boolean passable, double x0, double y0, double z0, double x1, double y1, double z1) {}

    public static final Def[] ALL = {
        new Def("baggage_carousel", 0, false, 0, 0, 0, 16, 8, 16),
        new Def("check_in_desk", 4, false, 0, 0, 0, 16, 17, 16),
        new Def("xray_scanner", 4, false, 0, 0, 2, 16, 16, 14),
        new Def("baggage_cart", 0, false, 1, 0, 2, 15, 12, 14),
        new Def("flight_board", 8, false, 0, 2, 13, 16, 15, 16),
        new Def("boarding_gate_desk", 0, false, 0, 0, 4, 16, 15, 15),
        new Def("suitcase_pile", 0, false, 1, 0, 2, 15, 10, 14),
        new Def("runway_light", 15, false, 4, 0, 5, 12, 11, 11),
        new Def("hospital_stretcher", 0, false, 0, 0, 0, 16, 12, 16),
        new Def("iv_stand", 0, false, 3, 0, 3, 13, 24, 13),
        new Def("heart_monitor", 4, false, 1, 0, 3.4, 15, 17, 12),
        new Def("medical_cabinet", 0, false, 1, 0, 2.2, 15, 24, 15),
        new Def("wheelchair", 0, false, 1, 0, 1, 15, 22, 14),
        new Def("surgical_light", 15, false, 1, 6, 1, 15, 16, 15),
        new Def("hospital_curtain", 0, true, 0, 0, 6.5, 16, 24, 8.5),
        new Def("rail_signal", 7, false, 4, 0, 3.5, 12, 24, 10),
        new Def("ticket_machine", 6, false, 1, 0, 2.2, 15, 24, 15),
        new Def("metro_turnstile", 0, false, 0, 0, 2, 16, 21, 14),
        new Def("tunnel_light", 13, true, 0, 2, 13.2, 16, 8, 16),
        new Def("exit_sign", 10, true, 2, 4, 13.2, 14, 10, 16),
        new Def("vent_fan", 0, false, 0, 0, 10.4, 16, 16, 16),
        new Def("emergency_generator", 0, false, 0, 0, 2, 16, 22, 14),
        new Def("electrical_cabinet", 0, false, 1, 0, 3.0, 15, 24, 14)
    };

    private RealismDecoData() {}
}
