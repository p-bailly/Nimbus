# Nimbus

Launcher Android personnel inspiré des fonctions de Smart Launcher Pro, écrit en
Kotlin avec Jetpack Compose (Material 3). Paquet : `fr.perso.nimbus`.

## Fonctionnalités

**Écran d'accueil (2 pages)**
- Page principale : horloge (appui : alarmes), date (appui : agenda), niveau de
  batterie, favoris (jusqu'à 5 par rangée) et barre de recherche.
- Page widgets : ajout de n'importe quel widget Android, redimensionnement
  (boutons ou poignée à faire glisser) et suppression, en mode « Modifier ».
- Gestes configurables : double tap, glisser vers le haut, glisser vers le bas
  (verrouiller l'écran, notifications, recherche, tiroir, paramètres).
- Appui long sur le fond : menu (ajouter un widget, fond d'écran, toutes les
  apps, paramètres).
- Bouton Accueil : referme tout et revient à la première page.

**Tiroir d'applications**
- Onglets de catégories automatiques (Communication, Social, Jeux…), balayables.
- Recherche instantanée insensible aux accents et à la casse (« eco » trouve
  « École »), y compris par initiales (« gm » → « Google Maps »), dans les
  contacts et sur le web. La touche Rechercher du clavier ouvre le premier résultat.
- Fermeture par glissé vers le bas ou par Retour.
- Tri alphabétique ou par fréquence d'utilisation.

**Icônes**
- Formes : squircle, cercle, carré arrondi, goutte, carré, forme du système.
- Packs d'icônes au format ADW / Nova (ceux du Play Store).
- Pastilles de notification, badge du profil professionnel.
- Appui long sur une icône : raccourcis de l'app, favori, catégorie, masquer,
  infos, désinstaller.

**Thèmes** : Ambiant (palette dérivée de la couleur du fond d'écran, mise à
jour quand il change), Material You (Android 12+), Sombre, Clair.

**Confidentialité** : apps masquées, absentes du tiroir et de la recherche,
dans un écran protégé par code PIN (4 à 8 chiffres, stocké haché en SHA-256).

## Compiler

Prérequis : JDK 17 ou plus récent, SDK Android avec la plateforme 35.

```bash
# Indiquer le SDK (ou créer local.properties avec sdk.dir=...)
export ANDROID_HOME=/chemin/vers/android-sdk
./gradlew assembleDebug
```

L'APK est produit dans `app/build/outputs/apk/debug/app-debug.apk`.
La version release (`./gradlew assembleRelease`) est signée avec la clé de
debug, ce qui suffit pour un usage personnel.

## Installer et configurer

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Puis, dans Nimbus (appui long sur l'accueil › Paramètres Nimbus) :

1. **Launcher par défaut** : bouton « Définir » en haut des paramètres. Indispensable
   pour le bouton Accueil et les raccourcis d'apps.
2. **Pastilles** : autoriser « Pastilles de notification Nimbus » dans l'accès
   aux notifications.
3. **Verrouillage par geste** : activer le service d'accessibilité « Gestes Nimbus ».
   Il ne lit rien à l'écran ; il sert uniquement à verrouiller et à ouvrir le
   volet de notifications.
4. **Contacts** : accepter la permission pour les voir dans la recherche.

## Architecture

```
app/src/main/java/fr/perso/nimbus/
├── MainActivity.kt          Activité unique : edge-to-edge, bouton Accueil,
│                            widgets (liaison puis configuration), permissions, rôle HOME
├── data/                    Modèles, réglages (Prefs), liste des apps, icônes,
│                            packs d'icônes, catégories, contacts
├── service/                 Pastilles (NotificationListener), gestes (accessibilité),
│                            actions système
├── widgets/                 Hôte des widgets (AppWidgetHost)
└── ui/
    ├── LauncherViewModel.kt État de l'interface, combinaison des flux Prefs et
    │                        AppRepository, couleur du fond d'écran, gestes
    ├── NimbusApp.kt         Racine Compose : accueil + couches au-dessus
    ├── LauncherHost.kt      Actions qui passent par l'activité
    ├── theme/Theme.kt       Thèmes Ambiant, Material You, Sombre, Clair
    ├── home/                Pager d'accueil, page principale, page widgets
    ├── drawer/              Tiroir, recherche sans accents
    ├── components/          Icône d'app, feuille d'actions, dialogues (choix, PIN),
    │                        sélecteur de widgets
    └── settings/            Paramètres et écran des apps masquées
```

Le flux de données est à sens unique : `Prefs` et `AppRepository` exposent des
`StateFlow`, que `LauncherViewModel` combine (catégories forcées, apps masquées,
tri par usage, favoris) en un `AppsState` lu par l'interface. Les changements de
forme, de pack ou de taille d'icône sont envoyés au dépôt sous forme d'`IconConfig`,
qui recharge les icônes.

## Versions

AGP 8.7.3, Gradle 8.11.1, Kotlin 2.1.0, Compose BOM 2024.12.01,
`minSdk` 28 (Android 9), `targetSdk` / `compileSdk` 35.
