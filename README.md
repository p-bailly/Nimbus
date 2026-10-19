# Nimbus

Launcher Android personnel inspiré de Smart Launcher Pro et de Nova Launcher Prime,
écrit en Kotlin avec Jetpack Compose (Material 3). Paquet : `fr.perso.nimbus`.
Pensé pour un Pixel récent (Android 15 à 17) et utilisable dès Android 9.

## Fonctionnalités

**Écran d'accueil (plusieurs pages)**
- Pages d'icônes et de widgets : balayer à gauche ou à droite, des points sous la grille
  indiquent la page. Horloge, dock et barre de recherche restent fixes.
- Grille d'icônes à placement libre (3 à 8 colonnes, 3 à 14 lignes, noms optionnels) :
  appui long puis glisser pour déplacer une icône, l'icône de la case d'arrivée
  prend la place libérée.
- Pendant un glisser-déposer (icône, widget, ou app du tiroir), l'élément suit le doigt ;
  tenu contre le bord gauche ou droit, il passe à la page voisine, et au bord gauche de la
  première page ou au bord droit de la dernière une nouvelle page vide se crée. Il se pose
  au lâcher, sur la page affichée. Une page vidée disparaît (sauf la page principale).
- Page principale (maison dans les points) : celle du bouton Accueil et de l'ouverture.
  Appui long sur le fond d'une autre page › « Page principale » pour la changer.
- Dock (favoris) de 4 à 6 icônes par rangée.
- Horloge (appui : alarmes), date (appui : agenda), batterie, barre de recherche :
  chacun peut être masqué.
- Glisser-déposer depuis le tiroir : appui long sur une app puis glisser, le tiroir
  s'efface et l'app se pose sur la case où on la lâche.
- Widgets dans la grille (appui long sur l'accueil › Ajouter un widget) : posés sur la
  page affichée s'il y a la place, sinon sur la suivante qui en a, sinon sur une nouvelle.
  Appui long sur un widget : menu (Redimensionner, Supprimer) ; appui long puis glisser :
  déplacer ce widget. Les icônes qu'il recouvre se décalent vers les cases libres.
  Paramètres › Page d'accueil › Widgets : liste des widgets (avec leur page), « Supprimer ».
- Ajouter une app à l'accueil : première case libre de la page affichée, sinon des autres
  pages, sinon une nouvelle page.
- Les widgets de l'ancienne page widgets passent automatiquement sur la grille ; les
  widgets alloués mais affichés nulle part sont libérés au démarrage.
- Bouton Accueil : referme tout et revient à la page principale.

**Gestes (à la Nova)**
- Double tap, glisser vers le haut / le bas, glisser à deux doigts vers le haut /
  le bas, pincer, écarter.
- Actions : verrouiller, notifications, réglages rapides, applis récentes, capture
  d'écran, menu Marche/Arrêt, assistant, recherche, tiroir, ajouter un widget,
  paramètres, ou ouvrir l'app de son choix.
- Glisser vers le haut sur une icône de l'accueil ou du dock : ouvre une seconde
  app (réglable par icône).
- Appui long sur le fond : panneau « Page d'accueil » (ajouter une icône, un widget, fond
  d'écran, toutes les apps, réglages de la page, tous les paramètres).

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
- Appui long sur une app : petit pop-up (raccourcis de l'app, Infos, Accueil,
  Désinstaller) ; « Plus » ouvre le reste (dock, glissé, renommer, icône, catégorie, masquer).

**Pixel / Android récent**
- Espace privé (Android 15+) : section en bas du tiroir avec verrouillage /
  déverrouillage (authentification du système), installation d'apps, réglages.
  Verrouillé, il n'apparaît nulle part ailleurs ; il peut même être masqué et
  retrouvé en tapant « espace privé » dans la recherche. Le réglage système
  « masquer quand verrouillé » (Android 16+) est respecté.
- Profil professionnel : mise en pause / reprise, apps en pause grisées.
- Thème Material You (Android 12+) ou Ambiant (palette tirée du fond d'écran).
- Bibliothèque native alignée sur 16 Ko (pages mémoire 16 Ko des Pixel récents).

**Paramètres** à la manière de Smart Launcher : page principale avec bandeaux
(launcher par défaut, accès aux notifications, gestes système), puis Apparence globale
(avec aperçu), Gestes, Sauvegarde, et les pages Accueil, Applications, Recherche,
Notifications, Sécurité, À propos. Une page ouverte depuis le menu de l'accueil
(Apparence globale, Page d'accueil, Gestes) ramène à l'accueil avec Retour.
Icônes Material Symbols (Google, licence Apache 2.0).

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
  les colonnes du tiroir. Chaque page Smart Launcher devient une page Nimbus ; les
  widgets sont proposés à l'ajout un par un et reprennent leur page et leur place.
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
    ├── HomeDrag.kt          Glisser-déposer sur l'accueil (calque sous le doigt)
    ├── theme/Theme.kt       Thèmes Ambiant, Material You, Sombre, Clair
    ├── home/                Accueil, pages, grille d'icônes et de widgets
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
