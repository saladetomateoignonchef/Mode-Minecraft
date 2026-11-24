#!/usr/bin/env python3
"""Régénère les GIF du README (docs/videos/jeu_*.gif) à partir des MP4 : 8 images/s, 400 px de large, 64 couleurs (3 à 5 fois plus légers)."""
import glob
import os
import subprocess

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
for mp4 in sorted(glob.glob(os.path.join(ROOT, "docs/videos/jeu_*.mp4"))):
    gif = mp4[:-4] + ".gif"
    before = os.path.getsize(gif) if os.path.exists(gif) else 0
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", mp4, "-vf",
                    "fps=8,scale=400:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=64:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=5",
                    "-loop", "0", gif], check=True)
    print(os.path.basename(gif), round(before / 1e6, 1), "->", round(os.path.getsize(gif) / 1e6, 1), "Mo")
