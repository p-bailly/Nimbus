# Exercices des accueils papier

Chaque accueil papier finissait par un exercice. Ils ont été retirés des pages le 21/10/2026, pour
laisser la place à des logos d'applis plus grands. Les voici, avec leurs réponses, tels qu'ils
étaient affichés. Une pastille d'appli est notée entre crochets.

## Accueil 1 : Groupes

Notations de la page :

- D₃ = {*e*, *r*, *r*², *s*, *sr*, *sr*²} = {[YouTube], [Twitch], [Stremio], [Spotify], [Appareil photo], [Photos]} ;
- Jeux & perso ≅ ℤ/3ℤ : 0 = [Lichess], 1 = [Basket], 2 = [πr] (depuis, LoL Live s'y ajoute : ℤ/2ℤ × ℤ/2ℤ) ;
- Outils ≅ ℤ/3ℤ × ℤ/3ℤ : (*i*, *j*) en colonne *i*, ligne *j*, soit (0, 0) = [Claude], (1, 0) = [GitHub],
  (2, 0) = [Drive], (0, 1) = [Fichiers], (1, 1) = [Authenticator], (2, 1) = [Banque Populaire],
  (0, 2) = [Play Store], (1, 2) = [Amazon], (2, 2) = [PRONOTE].

**5 Exercices.**

a) Calculer [Spotify] · [Twitch] dans D₃.
b) Calculer [Basket] + [πr] dans ℤ/3ℤ.
c) Calculer [Banque Populaire] + [Play Store] dans ℤ/3ℤ × ℤ/3ℤ.

Réponses :

a) *s* · *r* = *sr* : [Appareil photo].
b) 1 + 2 = 0 : [Lichess].
c) (2, 1) + (0, 2) = (2, 0) : [Drive].

## Accueil 2 : Voronoï

**Exercice.** On prend Ω = ℝ², *A* = {*a*, *b*}, *a* = (0, 0) et *b* = (2, 0). Déterminer *V*(*a*).

Réponse : *V*(*a*) est l'ensemble des points plus proches de *a* que de *b*, le demi-plan fermé limité
par la médiatrice de [*a*, *b*] (la droite *x* = 1) : *V*(*a*) = {(*x*, *y*) ∈ ℝ² : *x* ≤ 1}.

## Accueil 3 : Ensembles

Sur la page, |*S*| = 7, |*M*| = 9, |*U*| = 15, |*S* ∩ *M*| = 3, |*S* ∩ *U*| = 2, |*M* ∩ *U*| = 3,
|*S* ∩ *M* ∩ *U*| = 1, d'où |*S* ∪ *M* ∪ *U*| = 24 (formule du crible).

**Exercice.** Déterminer le nombre d'applis qui sont dans un seul des ensembles *S*, *M*, *U*.

Réponse : 18. Seulement dans *S* : 7 − (3 + 2 − 1) = 3 ; seulement dans *M* : 9 − (3 + 3 − 1) = 4 ;
seulement dans *U* : 15 − (2 + 3 − 1) = 11 ; 3 + 4 + 11 = 18.

## Accueil 4 : Fourier

Sur la page : (1) *z*(*t*) = 20 e^{*it*} − 6 e^{−*it*} − 3 e^{3*it*} − 2 e^{−3*it*} et
(2) *c*ₖ = 1/(2π) ∫₀^{2π} *z*(*t*) e^{−*ikt*} d*t*.

**Exercice.** À l'aide de (1) et (2), calculer *c*₁, *c*₋₁, *c*₃ et *c*₋₃.

Réponse : 1/(2π) ∫₀^{2π} e^{*i*(*m* − *k*)*t*} d*t* vaut 1 si *m* = *k* et 0 sinon, donc *c*ₖ est le
coefficient de e^{*ikt*} dans (1) : *c*₁ = 20, *c*₋₁ = −6, *c*₃ = −3, *c*₋₃ = −2.
