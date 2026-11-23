"""
Outils pour filmer le vrai jeu (client Minecraft sous Xvfb, rendu logiciel Mesa) : RCON pour les commandes du serveur,
saisie clavier / souris X11 (XTEST) pour jouer, ffmpeg (x11grab) pour enregistrer.
Voir tools/record_demos.py.
"""
import os
import socket
import struct
import subprocess
import time

from Xlib import X, display
from Xlib.ext import xtest
from Xlib import XK

DISPLAY = os.environ.get("DISPLAY", ":99")
GAME_X, GAME_Y, GAME_W, GAME_H = 160, 90, 960, 540


class Rcon:
    def __init__(self, port=25575, password="test"):
        self.s = socket.create_connection(("127.0.0.1", port))
        self.s.settimeout(120)
        self._send(1, 3, password)
        self._read()

    def _send(self, i, t, b):
        d = struct.pack("<ii", i, t) + b.encode() + b"\0\0"
        self.s.sendall(struct.pack("<i", len(d)) + d)

    def _read(self):
        n = struct.unpack("<i", self.s.recv(4))[0]
        d = b""
        while len(d) < n:
            d += self.s.recv(n - len(d))
        return d[8:-2].decode(errors="replace")

    def cmd(self, c):
        self._send(2, 2, c)
        return self._read()


class Pad:
    """Clavier et souris virtuels (XTEST) : les saisies vont au jeu comme celles d'un vrai joueur."""

    def __init__(self):
        self.d = display.Display(DISPLAY)

    def _key(self, name):
        return self.d.keysym_to_keycode(XK.string_to_keysym(name))

    def down(self, name):
        xtest.fake_input(self.d, X.KeyPress, self._key(name))
        self.d.sync()

    def up(self, name):
        xtest.fake_input(self.d, X.KeyRelease, self._key(name))
        self.d.sync()

    def tap(self, name, t=0.08):
        self.down(name)
        time.sleep(t)
        self.up(name)

    def hold(self, name, secs):
        self.down(name)
        time.sleep(secs)
        self.up(name)

    def mouse_down(self, b=1):
        xtest.fake_input(self.d, X.ButtonPress, b)
        self.d.sync()

    def mouse_up(self, b=1):
        xtest.fake_input(self.d, X.ButtonRelease, b)
        self.d.sync()

    def click(self, b=1, t=0.08):
        self.mouse_down(b)
        time.sleep(t)
        self.mouse_up(b)

    def move(self, dx, dy, steps=10, dt=0.02):
        """Déplacement relatif de la souris (regard), en plusieurs pas pour rester fluide."""
        for _ in range(steps):
            xtest.fake_input(self.d, X.MotionNotify, True, x=int(dx / steps), y=int(dy / steps))
            self.d.sync()
            time.sleep(dt)

    def release_all(self):
        for k in ("w", "a", "s", "d", "space", "Shift_L", "Control_L"):
            try:
                self.up(k)
            except Exception:
                pass
        for b in (1, 3):
            self.mouse_up(b)


class Recorder:
    def __init__(self, out, fps=20):
        self.out = out
        self.fps = fps
        self.p = None

    def start(self):
        os.makedirs(os.path.dirname(self.out), exist_ok=True)
        self.p = subprocess.Popen(["ffmpeg", "-y", "-loglevel", "error", "-f", "x11grab", "-framerate", str(self.fps), "-video_size", f"{GAME_W}x{GAME_H}",
                                   "-i", f"{DISPLAY}+{GAME_X},{GAME_Y}", "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-pix_fmt", "yuv420p", self.out],
                                  stdin=subprocess.PIPE)
        time.sleep(0.5)

    def stop(self):
        if self.p:
            self.p.stdin.write(b"q")
            self.p.stdin.flush()
            self.p.wait(timeout=30)
            self.p = None
