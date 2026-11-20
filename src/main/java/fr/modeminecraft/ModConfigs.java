package fr.modeminecraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Fichiers de configuration du mod (dossier config/ du jeu) :
 * - modeminecraft-common.toml : manuel d'armement offert à la première connexion ;
 * - modeminecraft-client.toml : mises à jour automatiques, panneau des recettes.
 */
public final class ModConfigs {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec.BooleanValue GIVE_MANUAL;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue AUTO_UPDATE;
    public static final ModConfigSpec.BooleanValue RECIPE_PANEL;

    static {
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        GIVE_MANUAL = common.comment("Donner le Manuel d'armement aux joueurs lors de leur première connexion dans un monde")
                .define("giveManualOnFirstJoin", true);
        COMMON_SPEC = common.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        AUTO_UPDATE = client.comment("Vérifier au lancement s'il existe une nouvelle version du mod sur GitHub,",
                        "la télécharger, et l'installer automatiquement quand on ferme le jeu")
                .define("autoUpdate", true);
        RECIPE_PANEL = client.comment("Afficher le panneau des recettes à droite de l'inventaire (touche O pour le masquer)")
                .define("recipePanel", true);
        CLIENT_SPEC = client.build();
    }

    private ModConfigs() {}
}
