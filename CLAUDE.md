# Consignes de travail (demandées par le propriétaire du dépôt)

- À chaque demande de modification, de correction ou d'ajout : faire le travail, vérifier que le projet compile
  (`./gradlew compileJava`), puis **commit, push, ouvrir la pull request vers `claude/trusting-edison-z1avyc` et la fusionner**,
  ce qui déclenche automatiquement la publication d'une nouvelle version (`.github/workflows/release.yml`).
- Ensuite, contrôler que la compilation de la release a réussi et le dire clairement (version, réussite ou échec).
- Les images et vidéos du README se régénèrent avec `python3 tools/readme_images.py` et `python3 tools/readme_videos.py`.
- Les modèles 3D des véhicules viennent de `tools/generate_military_vehicles.py` (fichier `MilGeometry.java` généré : ne pas modifier à la main).
