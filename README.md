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
- Boutons de navigation cachés sur l'accueil (réglable : Paramètres › Page d'accueil) : dessinés de la
  couleur du fond, ils restent à leur place et répondent au premier appui ; le tiroir et les
  paramètres les gardent lisibles.
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

**Accueils « papier » (style LaTeX)**
- Appui long sur le fond › « Style de l'accueil » (ou Paramètres › Page d'accueil) : la grille
  d'icônes, ou l'une des sept pages d'article : Groupes, Voronoï, Ensembles, Sierpiński, Fourier
  (les applis tournent sur des épicycles), Lapin de Douady (ensemble de Julia), Remerciements.
- Chaque page place 27 applis sur une figure mathématique : leurs places (WhatsApp, Messages…
  πr) sont remplies automatiquement avec les applis installées (paquets connus, puis nom) ; appui
  long sur une appli › « Changer », ou Paramètres › Page d'accueil › « Applis de la page ». Une place
  vide est en pointillés : un appui permet de choisir son appli.
- Voronoï : appui long sur une cellule puis glisser pour déplacer le site de son appli dans le
  carré Ω ; les cellules suivent le doigt, et les pastilles ne sortent pas du carré, ne se touchent
  pas et laissent le « Ω » libre. Appui long sur le fond › « Sites d'origine » pour tout remettre.
- Les applis sont dessinées avec leur silhouette monochrome (calque des icônes à thème du Pixel,
  sinon premier plan de l'icône, sinon l'initiale), teintée de la couleur de leur famille. La
  silhouette est redessinée à sa taille finale (nette même quand le motif est petit dans l'icône).
- Motif au choix (appui long › Plus › « Motif ») : silhouette de l'app, logo YouTube, lettres
  (« +x », « πr »…) ou silhouette d'une autre app. Par défaut, la place YouTube prend YouTube ReVanced
  avec le logo de YouTube, Banque Populaire s'écrit « +x » et la place πr (Prof de maths) « πr ».
- Le thème « Papier (LaTeX) » (choisi avec un accueil papier) met toute l'interface en Computer
  Modern sur fond crème. La page est dessinée sur un repère de 412 × 916 mis à l'échelle de l'écran.
- Pas d'en-tête (ni titre courant, ni numéro, ni heure : elle est dans la barre d'état) : la page
  grandit de la place gagnée, et ses marges latérales sont rognées (8 unités au lieu de 22).

**Page de déverrouillage (papier)**
- Au réveil, par-dessus l'écran verrouillé : une page d'article sans en-tête (l'heure en grand, la
  date), puis la météo comme l'écran Aujourd'hui de l'appli Météo (lieu, température,
  ressenti, ciel, maximum en orange et minimum en bleu, et son coloriage en figure), les matchs
  en direct des équipes favorites de l'appli Basket Live (trois au plus, mi-temps et fin de période
  comprises), l'exercice du jour (calcul de tête, huit familles à tour de rôle) et la solution de
  celui de la veille. Une section sans données (appli absente, rien en direct) disparaît.
- Glisser vers le haut, toucher le bas de la page ou Retour : le système demande l'empreinte, le
  visage ou le code, puis la page s'efface. Toucher la météo ouvre Météo, toucher les matchs ouvre
  Basket Live (après le déverrouillage). Un appel, une alarme ou l'appareil photo passent devant.
- Écran rallumé aussitôt après l'extinction, avant que le verrouillage n'arrive : la page est posée au
  réveil, par-dessus l'écran verrouillé (sauf double appui sur Marche/Arrêt, pour l'appareil photo).
- Activer : Paramètres › Sécurité et confidentialité › « Page de déverrouillage », avec le service
  d'accessibilité « Gestes Nimbus » (il pose la page à l'extinction de l'écran) et un verrouillage
  (code, schéma ou mot de passe). Désactivée par défaut ; le réglage est dans la sauvegarde.
- Données : Météo et Basket Live (applis personnelles) les partagent avec Nimbus seul, en lecture
  (`content://io.github.pbailly.meteo.nimbus` : `now` et le coloriage `sketch` ;
  `content://com.pbailly.superfanbasket.nimbus/favorites`), et préviennent à chaque changement. Les
  scores viennent de l'API publique d'ESPN (`site.api.espn.com`, comme Basket Live) : relus à
  chaque affichage, puis toutes les 15 s tant qu'un match de favori est en cours et la page visible,
  toutes les 5 min sinon. Rien n'est lu écran éteint, sauf une fois à l'extinction pour être à jour
  au réveil.

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
- Sous le thème « Papier (LaTeX) », une table des matières : chaque catégorie est une section
  numérotée, chaque app une sous-section avec points de conduite et numéro de page ; sous le titre,
  une figure des cinq applis les plus utilisées qui ne sont pas déjà sur l'accueil (places de la
  page papier, ou grille et dock) ; l'espace privé en annexe A, et les notifications d'une
  app sont un appel de note. Devant chaque nom, le motif de l'app (le même que sur les pages
  papier) à sa propre couleur, la teinte dominante de son icône. Appui : ouvrir ; appui long :
  actions ou glisser vers l'accueil.
  Réglable (Paramètres › Page des applications › « Table des matières ») pour revenir à la grille.
- Onglets de catégories automatiques, balayables (catégorie modifiable par app, ou nouvelle
  catégorie créée depuis ce choix).
- Catégories personnalisables (Paramètres › Page des applications › Catégories) : renommer,
  réordonner, supprimer (sauf « Autres » : les apps reviennent au classement automatique), en
  créer ; « Catégories d'origine » rétablit tout. Elles font partie de la sauvegarde.
- Ligne « Fréquentes », défilement rapide A–Z, tri alphabétique ou par usage.
- Recherche insensible aux accents (« eco » trouve « École »), par initiales
  (« gm » → « Google Maps »), par ancien nom d'une app renommée, dans les
  contacts, et sur le web avec le moteur choisi (Google, DuckDuckGo, Qwant,
  Ecosia, Brave, Startpage ou l'appli du téléphone).
- Fermeture par glissé vers le bas ou par Retour.

**Icônes**
- Formes : squircle, cercle, carré arrondi, goutte, carré, forme du système.
- Icônes à thème façon Pixel (Android 13+), packs d'icônes ADW / Nova.
- Par app : nom et icône personnalisés (image de la galerie, ou icône d'une autre app).
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
En local, la version release (`./gradlew assembleRelease`) est signée avec la clé de
debug ; les releases publiées sont signées avec la clé du dépôt (voir ci-dessous).

Tests (Robolectric, sur la JVM) :

```bash
./gradlew testDebugUnitTest
# Pour vérifier aussi une vraie sauvegarde Smart Launcher (affiche ce qui serait importé) :
NIMBUS_SLBK=/chemin/Backup.slbk ./gradlew testDebugUnitTest -i
```

## Releases

À chaque envoi sur la branche par défaut, GitHub Actions (`.github/workflows/android.yml`)
lance les tests, compile l'APK signé et le publie dans la release **apk** (toujours la
même, mise à jour). Lien permanent vers la dernière version :
https://github.com/p-bailly/Nimbus/releases/latest/download/Nimbus.apk

Les versions suivantes s'installent par-dessus (mêmes réglages). Numéro de version : le
nombre de commits (`1.0.42`). Relancer à la main : onglet **Actions › APK Android › Run
workflow**. Il faut deux secrets du dépôt (Settings › Secrets and variables › Actions) :

| Secret | Contenu |
|---|---|
| `NIMBUS_KEYSTORE_BASE64` | keystore PKCS12 (alias `nimbus`) encodé en base64 |
| `NIMBUS_KEYSTORE_PASSWORD` | son mot de passe |

Toujours la même clé : Android refuse une mise à jour signée avec une autre clé. Une
nouvelle clé se crée ainsi (à garder hors du dépôt) :

```bash
keytool -genkeypair -keystore nimbus.jks -storetype PKCS12 -alias nimbus \
  -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Nimbus"
base64 -w0 nimbus.jks   # valeur de NIMBUS_KEYSTORE_BASE64
```

## Installer et configurer

Installer l'APK de la dernière release, ou depuis un ordinateur :

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Un APK signé avec une autre clé (version de debug, ancienne copie) ne s'installe pas
par-dessus une release : désinstaller d'abord, après une sauvegarde (Paramètres ›
Sauvegarde › Exporter) à réimporter ensuite.

Puis, dans Nimbus (appui long sur l'accueil › Paramètres Nimbus) :

1. **Launcher par défaut** : bouton « Définir » en haut des paramètres. Indispensable
   pour le bouton Accueil, les raccourcis d'apps et l'espace privé.
2. **Importer** ta configuration Smart Launcher (Paramètres › Sauvegarde).
3. **Pastilles** : autoriser « Pastilles de notification Nimbus » dans l'accès
   aux notifications.
4. **Gestes système** : activer le service d'accessibilité « Gestes Nimbus ».
   Il ne lit rien à l'écran ; il sert uniquement au verrouillage, aux réglages
   rapides, aux applis récentes, à la capture d'écran, au menu Marche/Arrêt et à la
   page de déverrouillage.
5. **Contacts** : accepter la permission pour les voir dans la recherche.

## Polices

Les pages « papier » utilisent CMU Serif (Computer Modern Unicode, licence SIL OFL 1.1), complétée
des symboles mathématiques de Latin Modern Math (licence GUST) : fichiers `app/src/main/res/font/papier_*.ttf`,
renommés « Nimbus Papier » comme le demandent les licences. Licences et script de fusion : `design/polices/`.

## Captures d'écran (JVM)

```bash
./gradlew recordRoborazziDebug --tests 'fr.perso.nimbus.screens.paper.*'
# → app/build/outputs/roborazzi/paper/*.png (pages « papier » avec des applis fictives ;
#   h8_*.png : page de déverrouillage à 20 h 35 avec les données de la maquette, et sans ses sections)
```

## Architecture

```
app/src/main/java/fr/perso/nimbus/
├── MainActivity.kt          Activité unique : edge-to-edge, bouton Accueil, widgets
│                            (liaison puis configuration, file après import), fichiers
│                            (export, import, dossier de sauvegarde, image d'icône),
│                            permissions, rôle HOME
├── data/                    Modèles, réglages (Prefs), apps et profils (AppRepository),
│                            icônes (IconFactory, CustomIcons, IconPack), catégories,
│                            contacts, sauvegardes (BackupManager, SmartLauncherImport),
│                            places des accueils papier (PaperSlots), silhouettes (GlyphFactory),
│                            page de déverrouillage (MeteoShare, BasketShare, DailyExercise)
├── service/                 Pastilles (NotificationListener), gestes (accessibilité),
│                            actions système
├── widgets/                 Hôte des widgets (AppWidgetHost)
└── ui/
    ├── LauncherViewModel.kt État de l'interface, combinaison des flux Prefs et
    │                        AppRepository, grille d'accueil, profils, gestes
    ├── NimbusApp.kt         Racine Compose : accueil + couches au-dessus
    ├── LauncherHost.kt      Actions qui passent par l'activité
    ├── HomeDrag.kt          Glisser-déposer sur l'accueil (calque sous le doigt)
    ├── theme/Theme.kt       Thèmes Ambiant, Material You, Sombre, Clair, Papier (PaperTheme.kt)
    ├── home/                Accueil, pages, grille d'icônes et de widgets
    ├── paper/               Accueils « papier » : repère, en-tête, pastilles, sept pages (layouts/)
    ├── drawer/              Tiroir, recherche sans accents, défilement A–Z, sommaire (DrawerToc)
    ├── unlock/              Page de déverrouillage : activité par-dessus l'écran verrouillé,
    │                        page, données (UnlockFeed), lancement à l'extinction (UnlockLauncher)
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
