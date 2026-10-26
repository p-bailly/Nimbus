# Nimbus

Launcher Android personnel inspiré de Smart Launcher Pro et de Nova Launcher, écrit en Kotlin avec
Jetpack Compose (Material 3). Paquet `fr.perso.nimbus`. Pensé pour un Pixel récent (Android 15 à 17),
utilisable dès Android 9.

- Accueil à plusieurs pages (icônes, widgets, dock) ou accueils « papier » au style LaTeX
  (Groupes, Voronoï, Ensembles, Fourier).
- Page de déverrouillage « papier » (météo, messages, musique, matchs, figure du jour) et écran verrouillé
  en fond d'écran animé.
- Tiroir d'applications : catégories, recherche (apps, contacts, fichiers, réglages, web), espace privé.
- Gestes à la Nova, icônes à thème et packs d'icônes, dictée vers Claude.
- Sauvegarde JSON et import d'une configuration Smart Launcher (`.slbk`).

## Installer

APK signé de la dernière version, mis à jour à chaque envoi sur la branche par défaut :
<https://github.com/p-bailly/Nimbus/releases/latest/download/Nimbus.apk>

Chaque version s'installe par-dessus la précédente. Un APK signé avec une autre clé (version de debug)
impose de désinstaller d'abord : exporter la configuration avant (Paramètres › Sauvegarde).

Puis, dans Nimbus (appui long sur l'accueil › Paramètres Nimbus) : « Définir » comme launcher par défaut,
importer sa configuration Smart Launcher (Paramètres › Sauvegarde), autoriser « Pastilles de notification Nimbus »
(accès aux notifications), activer le service d'accessibilité « Gestes Nimbus » et la permission Contacts.

## Compiler

Prérequis : JDK 17 ou plus récent, SDK Android avec la plateforme 35.

```bash
export ANDROID_HOME=/chemin/vers/android-sdk   # ou local.properties : sdk.dir=...
./gradlew assembleDebug                        # → app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest                    # tests JVM (Robolectric)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Pour aller plus loin

Architecture, release et signature, page de déverrouillage, accueils papier, sauvegarde : voir [CLAUDE.md](CLAUDE.md).

Icônes Material Symbols (Apache 2.0). Polices CMU Serif, Latin Modern Math et Google Sans Flex : licences
dans `design/polices/`.
