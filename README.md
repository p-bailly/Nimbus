# Nimbus

Launcher Android personnel inspiré de Smart Launcher Pro et de Nova Launcher Prime,
écrit en Kotlin avec Jetpack Compose (Material 3). Paquet : `fr.perso.nimbus`.
Pensé pour un Pixel récent (Android 15 à 17) et utilisable dès Android 9.

## Fonctionnalités

**Écran d'accueil (2 pages)**
- Grille d'icônes à placement libre (3 à 8 colonnes, 3 à 14 lignes, noms optionnels) :
  appui long puis glisser pour déplacer une icône, l'icône de la case d'arrivée
  prend la place libérée.
- Dock (favoris) de 4 à 6 icônes par rangée.
- Horloge (appui : alarmes), date (appui : agenda), batterie, barre de recherche :
  chacun peut être masqué.
- Page widgets : ajout de n'importe quel widget, redimensionnement (boutons ou
  poignée), suppression, en mode « Modifier ».
- Bouton Accueil : referme tout et revient à la première page.

**Gestes (à la Nova)**
- Double tap, glisser vers le haut / le bas, glisser à deux doigts vers le haut /
  le bas, pincer, écarter.
- Actions : verrouiller, notifications, réglages rapides, applis récentes, capture
  d'écran, menu Marche/Arrêt, assistant, recherche, tiroir, page widgets,
  paramètres, ou ouvrir l'app de son choix.
- Glisser vers le haut sur une icône de l'accueil ou du dock : ouvre une seconde
  app (réglable par icône).
- Appui long sur le fond : menu (widget, fond d'écran, apps, paramètres).

**Tiroir d'applications**
- Onglets de catégories automatiques, balayables (catégorie modifiable par app).
- Ligne « Fréquentes », défilement rapide A–Z, tri alphabétique ou par usage.
- Recherche insensible aux accents (« eco » trouve « École »), par initiales
  (« gm » → « Google Maps »), par ancien nom d'une app renommée, dans les
  contacts, et sur le web avec le moteur choisi (Google, DuckDuckGo, Qwant,
  Ecosia, Brave, Startpage ou l'appli du téléphone).
- Fermeture par glissé vers le bas ou par Retour.

**Icônes**
- Formes : squircle, cercle, carré arrondi, goutte, carré, forme du système.
- Icônes à thème façon Pixel (Android 13+), packs d'icônes ADW / Nova.
- Par app : nom et icône personnalisés (image de la galerie).
- Pastilles de notification (nombre ou simple point).
- Appui long : raccourcis de l'app, accueil, dock, glissé, renommer, icône,
  catégorie, masquer, infos, désinstaller.

**Pixel / Android récent**
- Espace privé (Android 15+) : section en bas du tiroir avec verrouillage /
  déverrouillage (authentification du système), installation d'apps, réglages.
  Verrouillé, il n'apparaît nulle part ailleurs ; il peut même être masqué et
  retrouvé en tapant « espace privé » dans la recherche. Le réglage système
  « masquer quand verrouillé » (Android 16+) est respecté.
- Profil professionnel : mise en pause / reprise, apps en pause grisées.
- Thème Material You (Android 12+) ou Ambiant (palette tirée du fond d'écran).
- Bibliothèque native alignée sur 16 Ko (pages mémoire 16 Ko des Pixel récents).

**Confidentialité** : apps masquées dans un écran protégé par code PIN
(4 à 8 chiffres, stocké haché en SHA-256).

## Sauvegardes et import Smart Launcher

Paramètres › Sauvegarde :
- **Exporter la configuration** : fichier `.json` avec réglages, accueil, dock,
  catégories, apps masquées, noms, icônes personnalisées, gestes et fréquence
  d'utilisation. Ni le code PIN ni les autorisations n'y figurent.
- **Importer une configuration** : sauvegarde Nimbus (`.json`) ou Smart Launcher
  (`.slbk`). Depuis Smart Launcher sont repris la grille d'accueil (positions,
  taille, noms masqués), les catégories du tiroir, les apps masquées, la
  fréquence d'utilisation, les noms et icônes personnalisés, la forme des icônes,
  les colonnes du tiroir ; les widgets sont proposés à l'ajout un par un.
  Une app absente du téléphone est ignorée ; une app dont l'activité a changé est
  retrouvée par son paquet.
- **Sauvegarde automatique** : une fois par jour dans le dossier choisi, les 7
  dernières sont conservées.

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

Tests (Robolectric, sur la JVM) :

```bash
./gradlew testDebugUnitTest
# Pour vérifier aussi une vraie sauvegarde Smart Launcher (affiche ce qui serait importé) :
NIMBUS_SLBK=/chemin/Backup.slbk ./gradlew testDebugUnitTest -i
```

## Installer et configurer

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Puis, dans Nimbus (appui long sur l'accueil › Paramètres Nimbus) :

1. **Launcher par défaut** : bouton « Définir » en haut des paramètres. Indispensable
   pour le bouton Accueil, les raccourcis d'apps et l'espace privé.
2. **Importer** ta configuration Smart Launcher (Paramètres › Sauvegarde).
3. **Pastilles** : autoriser « Pastilles de notification Nimbus » dans l'accès
   aux notifications.
4. **Gestes système** : activer le service d'accessibilité « Gestes Nimbus ».
   Il ne lit rien à l'écran ; il sert uniquement au verrouillage, aux réglages
   rapides, aux applis récentes, à la capture d'écran et au menu Marche/Arrêt.
5. **Contacts** : accepter la permission pour les voir dans la recherche.

## Architecture

```
app/src/main/java/fr/perso/nimbus/
├── MainActivity.kt          Activité unique : edge-to-edge, bouton Accueil, widgets
│                            (liaison puis configuration, file après import), fichiers
│                            (export, import, dossier de sauvegarde, image d'icône),
│                            permissions, rôle HOME
├── data/                    Modèles, réglages (Prefs), apps et profils (AppRepository),
│                            icônes (IconFactory, CustomIcons, IconPack), catégories,
│                            contacts, sauvegardes (BackupManager, SmartLauncherImport)
├── service/                 Pastilles (NotificationListener), gestes (accessibilité),
│                            actions système
├── widgets/                 Hôte des widgets (AppWidgetHost)
└── ui/
    ├── LauncherViewModel.kt État de l'interface, combinaison des flux Prefs et
    │                        AppRepository, grille d'accueil, profils, gestes
    ├── NimbusApp.kt         Racine Compose : accueil + couches au-dessus
    ├── LauncherHost.kt      Actions qui passent par l'activité
    ├── theme/Theme.kt       Thèmes Ambiant, Material You, Sombre, Clair
    ├── home/                Pager d'accueil, grille d'icônes, page widgets
    ├── drawer/              Tiroir, recherche sans accents, défilement A–Z
    ├── components/          Icône d'app, feuille d'actions, dialogues (choix, PIN,
    │                        texte, choix d'app), sélecteur de widgets
    └── settings/            Paramètres et écran des apps masquées
```

Le flux de données est à sens unique : `Prefs` et `AppRepository` exposent des
`StateFlow`, que `LauncherViewModel` combine (noms et catégories personnalisés, apps
masquées, tri par usage, dock, grille, profils) en un `AppsState` lu par l'interface.
Les changements de forme, de pack, de taille ou de thème des icônes sont envoyés au
dépôt sous forme d'`IconConfig`, qui recharge les icônes.

## Versions

AGP 8.7.3, Gradle 8.11.1, Kotlin 2.1.0, Compose BOM 2024.12.01,
`minSdk` 28 (Android 9), `targetSdk` / `compileSdk` 35. Les API d'Android 16
(réglages de l'espace privé) sont appelées par réflexion pour garder `compileSdk` 35.
