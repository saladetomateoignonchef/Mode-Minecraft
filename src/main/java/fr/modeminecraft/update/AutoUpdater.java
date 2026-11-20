package fr.modeminecraft.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.modeminecraft.ModConfigs;
import fr.modeminecraft.ModeMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Mise à jour automatique : plus besoin de réinstaller le mod à chaque nouvelle version.
 *
 * 1. Au lancement du jeu, on regarde la dernière "Release" publiée sur GitHub (le workflow .github/workflows/release.yml
 *    en crée une à chaque envoi de code).
 * 2. Si elle est plus récente que la version installée, son .jar est téléchargé dans le dossier mods/ sous un nom
 *    que le jeu ignore (".jar.update"), pendant qu'on joue.
 * 3. Quand on ferme le jeu, un petit script attend que le jeu soit vraiment fermé (Windows verrouille le .jar),
 *    supprime l'ancienne version et met la nouvelle à sa place. Au lancement suivant, c'est la nouvelle version.
 *
 * Désactivable dans config/modeminecraft-client.toml (autoUpdate = false). Ne fait rien en développement.
 */
public final class AutoUpdater {
    private static final String RELEASES = "https://api.github.com/repos/saladetomateoignonchef/Mode-Minecraft/releases/latest";
    private static volatile boolean started;

    public static void start() {
        if (started || !FMLEnvironment.production || !ModConfigs.AUTO_UPDATE.get()) {
            return;
        }
        started = true;
        Thread t = new Thread(AutoUpdater::run, "ModeMinecraft-Updater");
        t.setDaemon(true);
        t.start();
    }

    private static void run() {
        try {
            var file = ModList.get().getModFileById(ModeMinecraft.MODID);
            if (file == null) {
                return;
            }
            Path current = file.getFile().getFilePath();
            if (!Files.isRegularFile(current) || !current.toString().endsWith(".jar")) {
                return; // pas lancé depuis un .jar (développement)
            }
            String installed = file.versionString();
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(RELEASES))
                    .header("Accept", "application/vnd.github+json").header("User-Agent", "ModeMinecraft-Updater")
                    .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() != 200) {
                ModeMinecraft.LOGGER.info("Mise à jour : pas de version publiée (HTTP {})", res.statusCode());
                return;
            }
            JsonObject release = JsonParser.parseString(res.body()).getAsJsonObject();
            String latest = release.get("tag_name").getAsString().replaceFirst("^v", "");
            if (new DefaultArtifactVersion(latest).compareTo(new DefaultArtifactVersion(installed)) <= 0) {
                ModeMinecraft.LOGGER.info("Mode Minecraft est à jour ({})", installed);
                return;
            }
            String url = null;
            for (var a : release.getAsJsonArray("assets")) {
                String name = a.getAsJsonObject().get("name").getAsString();
                if (name.endsWith(".jar")) {
                    url = a.getAsJsonObject().get("browser_download_url").getAsString();
                }
            }
            if (url == null) {
                return;
            }
            Path mods = current.getParent();
            Path target = mods.resolve("modeminecraft-" + latest + ".jar");
            Path pending = mods.resolve("modeminecraft-" + latest + ".jar.update");
            HttpResponse<InputStream> dl = http.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "ModeMinecraft-Updater")
                    .timeout(Duration.ofMinutes(5)).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            if (dl.statusCode() != 200) {
                return;
            }
            Path tmp = mods.resolve("modeminecraft-" + latest + ".download");
            try (InputStream in = dl.body()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, pending, StandardCopyOption.REPLACE_EXISTING);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> install(current, pending, target), "ModeMinecraft-Install"));
            ModeMinecraft.LOGGER.info("Mode Minecraft {} téléchargé : installé à la fermeture du jeu", latest);
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().getToasts().addToast(SystemToast.multiline(Minecraft.getInstance(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.translatable("toast.modeminecraft.update.title"),
                    Component.translatable("toast.modeminecraft.update.text", installed, latest))));
        } catch (Exception e) {
            ModeMinecraft.LOGGER.warn("Mise à jour automatique impossible : {}", e.toString());
        }
    }

    /** À la fermeture : un script remplace l'ancien .jar par le nouveau une fois le jeu vraiment fermé. */
    private static void install(Path current, Path pending, Path target) {
        try {
            boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
            Path script = Files.createTempFile("modeminecraft-update", windows ? ".bat" : ".sh");
            String text;
            if (windows) {
                text = "@echo off\r\n"
                        + "chcp 65001 >nul\r\n"            // chemins avec accents (C:\\Users\\Prénom...)
                        + "set n=0\r\n"
                        + ":wait\r\n"
                        + "timeout /t 2 /nobreak >nul\r\n"
                        + "set /a n+=1\r\n"
                        + "del /f /q \"" + current + "\" >nul 2>&1\r\n"
                        + "if exist \"" + current + "\" if %n% lss 60 goto wait\r\n"
                        + "move /y \"" + pending + "\" \"" + target + "\" >nul\r\n"
                        + "del \"%~f0\"\r\n";
                Files.writeString(script, text, StandardCharsets.UTF_8);
                new ProcessBuilder("cmd", "/c", "start", "\"\"", "/min", script.toString()).start();
            } else {
                long pid = ProcessHandle.current().pid();
                text = "#!/bin/sh\n"
                        + "while kill -0 " + pid + " 2>/dev/null; do sleep 1; done\n"
                        + "rm -f '" + current + "'\n"
                        + "mv -f '" + pending + "' '" + target + "'\n"
                        + "rm -f \"$0\"\n";
                Files.writeString(script, text, StandardCharsets.UTF_8);
                new ProcessBuilder("sh", script.toString()).start();
            }
        } catch (IOException e) {
            ModeMinecraft.LOGGER.warn("Installation de la mise à jour impossible : {}", e.toString());
        }
    }

    private AutoUpdater() {}
}
