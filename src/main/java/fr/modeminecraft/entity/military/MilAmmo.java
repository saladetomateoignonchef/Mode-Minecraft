package fr.modeminecraft.entity.military;

import fr.modeminecraft.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Munitions des véhicules militaires. Chaque véhicule a ses soutes (nombre de coups par type) : on les remplit en
 * faisant clic droit sur le véhicule avec l'objet de munition (une caisse de mitrailleuse = `perItem` cartouches).
 */
public enum MilAmmo {
    //            objet                                  par objet  type de projectile
    MG_762(() -> ModItems.AMMO_BELT_762R.get(), 100, Shell.MG_762),
    MG_127(() -> ModItems.AMMO_BOX_50.get(), 100, Shell.MG_127),
    MG_145(() -> ModItems.AMMO_BOX_145.get(), 80, Shell.MG_145),
    MINIGUN(() -> ModItems.AMMO_BELT_762R.get(), 250, Shell.MG_762),
    AP_I(() -> ModItems.AP_I_BELT.get(), 50, Shell.AP_I),
    HE_I(() -> ModItems.HE_I_BELT.get(), 50, Shell.HE_I),
    GRENADE_40(() -> ModItems.AMMO_BOX_40MM.get(), 32, Shell.GRENADE_40),
    APFSDS(() -> ModItems.APFSDS_SHELL.get(), 1, Shell.APFSDS),
    HE(() -> ModItems.HE_SHELL.get(), 1, Shell.HE),
    HEAT(() -> ModItems.HEAT_SHELL.get(), 1, Shell.HEAT),
    REFLEKS(() -> ModItems.CANNON_ATGM_MISSILE.get(), 1, Shell.REFLEKS),
    ROCKET_122(() -> ModItems.GRAD_ROCKET.get(), 1, Shell.ROCKET_122),
    ROCKET_107(() -> ModItems.ROCKET_107.get(), 1, Shell.ROCKET_107),
    ROCKET_227(() -> ModItems.MLRS_ROCKET.get(), 1, Shell.ROCKET_227),
    ATACMS(() -> ModItems.ATACMS_MISSILE.get(), 1, Shell.ATACMS),
    S8(() -> ModItems.S8_ROCKET.get(), 1, Shell.S8),
    HYDRA(() -> ModItems.HYDRA_ROCKET.get(), 1, Shell.HYDRA),
    ATGM(() -> ModItems.ATGM_MISSILE.get(), 1, Shell.ATGM),
    SAM(() -> ModItems.STINGER_MISSILE.get(), 1, Shell.SAM),
    // complexes lance-missiles (S-400, S-500, Iskander)
    S400_48N6(() -> ModItems.MISSILE_48N6E3.get(), 1, Shell.SAM_LONG),
    S400_9M96(() -> ModItems.MISSILE_9M96E2.get(), 1, Shell.SAM_AGILE),
    S500_77N6(() -> ModItems.MISSILE_77N6N.get(), 1, Shell.ABM),
    S500_40N6(() -> ModItems.MISSILE_40N6M.get(), 1, Shell.SAM_EXTREME),
    ISK_HE(() -> ModItems.ISKANDER_HE.get(), 1, Shell.BALLISTIC),
    ISK_CLUSTER(() -> ModItems.ISKANDER_CLUSTER.get(), 1, Shell.BALLISTIC),
    ISK_EMP(() -> ModItems.ISKANDER_EMP.get(), 1, Shell.BALLISTIC),
    // aviation
    AAM_IR(() -> ModItems.AAM_IR_MISSILE.get(), 1, Shell.AAM_IR),
    AAM_RADAR(() -> ModItems.AAM_RADAR_MISSILE.get(), 1, Shell.AAM_RADAR),
    AGM(() -> ModItems.AGM_MISSILE.get(), 1, Shell.AGM),
    ARM(() -> ModItems.ARM_MISSILE.get(), 1, Shell.ARM),
    CRUISE(() -> ModItems.CRUISE_MISSILE.get(), 1, Shell.CRUISE),
    // marine
    NAVAL_SHELL(() -> ModItems.NAVAL_SHELL.get(), 4, Shell.NAVAL_HE),
    CIWS_ROUNDS(() -> ModItems.CIWS_BELT.get(), 500, Shell.MG_145),
    ASHM(() -> ModItems.ASHM_MISSILE.get(), 1, Shell.ASHM),
    HYPERSONIC(() -> ModItems.HYPERSONIC_MISSILE.get(), 1, Shell.HYPERSONIC),
    NAVAL_SAM(() -> ModItems.NAVAL_SAM_MISSILE.get(), 1, Shell.SAM_LONG),
    TORPEDO(() -> ModItems.TORPEDO.get(), 1, Shell.TORPEDO),
    ASW_ROCKET(() -> ModItems.ASW_ROCKET.get(), 4, Shell.ASW);

    private final Supplier<Item> item;
    public final int perItem;
    public final Shell shell;

    MilAmmo(Supplier<Item> item, int perItem, Shell shell) {
        this.item = item;
        this.perItem = perItem;
        this.shell = shell;
    }

    public Item item() {
        return item.get();
    }

    /** Les projectiles : vitesse (blocs / tick), gravité, dégâts directs, rayon d'explosion, perforation (0-1). */
    public enum Shell {
        MG_762(6.0f, 0.004f, 9, 0, 0.05f, false),
        MG_127(6.0f, 0.004f, 16, 0, 0.15f, false),
        MG_145(6.5f, 0.004f, 20, 0, 0.25f, false),
        AP_I(5.0f, 0.006f, 26, 0, 0.45f, false),
        HE_I(4.6f, 0.008f, 14, 1.5f, 0.15f, false),
        GRENADE_40(2.2f, 0.035f, 8, 2.5f, 0.1f, false),
        APFSDS(9.0f, 0.002f, 110, 0.8f, 1.0f, false),
        HE(5.5f, 0.012f, 40, 7.0f, 0.3f, false),
        HEAT(6.5f, 0.008f, 80, 3.0f, 0.8f, false),
        REFLEKS(3.2f, 0.0f, 85, 3.0f, 0.85f, true),
        ROCKET_122(3.0f, 0.02f, 30, 4.5f, 0.3f, false),
        /** Roquette de 107 mm (lance-roquettes Type 63) : plus courte et plus légère que la Grad. */
        ROCKET_107(2.4f, 0.02f, 22, 3.5f, 0.25f, false),
        ROCKET_227(3.6f, 0.015f, 40, 5.5f, 0.35f, true),
        ATACMS(4.0f, 0.012f, 60, 9.0f, 0.5f, true),
        S8(3.8f, 0.012f, 25, 3.5f, 0.35f, false),
        HYDRA(3.8f, 0.012f, 25, 3.5f, 0.35f, false),
        ATGM(2.4f, 0.0f, 80, 3.0f, 0.9f, true),
        SAM(3.2f, 0.0f, 40, 3.0f, 0.5f, true),
        SAM_LONG(4.5f, 0.0f, 80, 4.0f, 0.5f, true),
        SAM_AGILE(5.0f, 0.0f, 50, 3.0f, 0.5f, true),
        ABM(7.0f, 0.0f, 120, 2.0f, 0.5f, true),
        SAM_EXTREME(5.5f, 0.0f, 80, 4.0f, 0.5f, true),
        /** Missile balistique (vol simulé par Strikes, l'entité n'est que visuelle). */
        BALLISTIC(4.0f, 0.0f, 0, 0, 0, true),
        /** Sous-munition de l'ogive à dispersion de l'Iskander. */
        SUBMUNITION(1.2f, 0.06f, 20, 3.5f, 0.4f, false),
        /** Missiles d'avion : air-air infrarouge / radar, air-sol, anti-radar, croisière. */
        AAM_IR(5.5f, 0.0f, 70, 3.5f, 0.5f, true),
        AAM_RADAR(6.0f, 0.0f, 80, 4.0f, 0.5f, true),
        AGM(4.2f, 0.0f, 120, 4.5f, 0.95f, true),
        ARM(5.0f, 0.0f, 110, 4.5f, 0.8f, true),
        CRUISE(3.6f, 0.0f, 160, 9.0f, 0.7f, true),
        /** Marine : obus de 100 à 130 mm, antinavire rasant, hypersonique (Zircon, CPS), torpille lourde, roquette ASM. */
        NAVAL_HE(6.0f, 0.03f, 60, 5.0f, 0.4f, false),
        ASHM(4.0f, 0.0f, 110, 5.5f, 0.8f, true),
        HYPERSONIC(9.0f, 0.0f, 150, 6.0f, 1.0f, true),
        TORPEDO(1.3f, 0.0f, 95, 5.0f, 0.9f, true),
        ASW(2.6f, 0.03f, 70, 4.5f, 0.5f, false);

        public final float speed, gravity, damage, blast, pierce;
        public final boolean guided;

        Shell(float speed, float gravity, float damage, float blast, float pierce, boolean guided) {
            this.speed = speed;
            this.gravity = gravity;
            this.damage = damage;
            this.blast = blast;
            this.pierce = pierce;
            this.guided = guided;
        }

        /** Balle de mitrailleuse (tirée avec les balles normales du mod). */
        public boolean bullet() {
            return this == MG_762 || this == MG_127 || this == MG_145;
        }

        /** Roquette / missile (traînée de fumée, détruit par le système de protection active). */
        public boolean rocket() {
            return (ordinal() >= ROCKET_122.ordinal() || this == REFLEKS) && this != NAVAL_HE && this != TORPEDO;
        }
    }

    public static MilAmmo byItem(Item item) {
        for (MilAmmo a : values()) {
            if (a.item() == item) {
                return a;
            }
        }
        return null;
    }
}
