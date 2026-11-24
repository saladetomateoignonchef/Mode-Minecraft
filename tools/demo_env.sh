#!/bin/bash
# Lance l'environnement de tournage : écran virtuel, serveur dédié (RCON) et client Minecraft (rendu logiciel Mesa).
# Le client rejoint le serveur local avec le joueur « Demo » (voir build.gradle : propriété -PdemoJoin).
cd "$(dirname "$0")/.."
pkill -f "forgeserverdev|forgeclientdev|runServer|runClient" 2>/dev/null; sleep 2
pgrep Xvfb >/dev/null || (Xvfb :99 -screen 0 1280x720x24 >/tmp/xvfb.log 2>&1 &)
mkdir -p run /tmp/mmclient; echo "eula=true" > run/eula.txt
[ -f run/server.properties ] || printf 'online-mode=false\nview-distance=6\nenable-rcon=true\nrcon.port=25575\nrcon.password=test\nspawn-protection=0\ngamemode=creative\nforce-gamemode=true\nlevel-type=minecraft\\:flat\nspawn-monsters=false\nallow-flight=true\nlevel-seed=24681\ngenerator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"structure_overrides":["modeminecraft:cities"]}\n' > run/server.properties
(timeout 6000 ./gradlew runServer --console=plain > /tmp/claude-0/srv.log 2>&1 &)
until grep -qE "Done \(|FAILED" /tmp/claude-0/srv.log; do sleep 3; done
(DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450 timeout 6000 ./gradlew runClient -PdemoJoin=1 --console=plain > /tmp/claude-0/cli.log 2>&1 &)
until grep -q "joined the game" /tmp/claude-0/srv.log; do sleep 3; done
echo "prêt"
