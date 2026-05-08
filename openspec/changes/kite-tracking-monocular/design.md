## Context

Un téléphone Android unique doit suivre un cerf-volant en vol et fournir une télémétrie pour la sonification audio. La caméra du téléphone capture des images vidéo. Après une phase de calibration initiale, le système détecte continuellement le cerf-volant, estime sa position 3D (distance, hauteur, direction) et sa vitesse. Ces données alimentent un générateur audio en temps réel (cible : suivi >30 IPS, latence audio <100ms).

**Contexte matériel** : Téléphone Android milieu de gamme, caméra arrière (1080p ou plus), IMU intégré, Bluetooth 5.x optionnel. Utilisation en extérieur par lumière du jour.

**Problème** : La vision monoculaire ne peut pas mesurer directement la profondeur. Nous devons inférer la position 3D à partir de mesures 2D en utilisant des hypothèses, des a priori et des indices de mouvement.

## Goals / Non-Goals

**Objectifs :**
- Suivre la position et la vitesse 3D du cerf-volant à une fréquence interactive (≥20 Hz)
- Fonctionner avec une configuration minimale (phase de calibration unique)
- Produire une sortie audio à faible latence, réactive aux dynamiques de vol
- Marcher avec du matériel Android grand public standard

**Hors scope :**
- Métrologie haute précision (±10cm non requis)
- Fonctionnement en faible lumière, brouillard ou sur fonds complexes
- Suivi simultané de plusieurs cerfs-volants
- Utilisation de plusieurs téléphones ou caméras externes
- Enregistrement des données de vol (pourrait être ajouté ultérieurement)

## Decisions

### 1. Algorithme de suivi visuel : Détection + Tracking hybride

**Choix** : Utiliser un détecteur d'objet léger (MediaPipe Face Detection ou modèle TFLite personnalisé) pour l'initialisation et la ré-identification, combiné à un traceur de caractéristiques (KLT ou optique flow de Lucas-Kanade) pour l'estimation de mouvement inter-images fluide.

**Justification** :
- La détection pure tourne à ~5-10 IPS sur CPU ; trop lent pour des estimations de position fluides
- Le suivi pur par caractéristiques dérive ; nécessite une redétection périodique
- Approche hybride : Détecter tous les N images (ex. 10), tracer les caractéristiques entre les détections → taux de suivi effectif = FPS du détecteur
- Alternative : MediaPipe Objectron (détection 3D) rejetée — surdimensionné,utilisation CPU/GPU plus élevée, nécessite un modèle 3D du cerf-volant

**Alternatives envisagées** :
- YOLOv5 Nano sur TFLite : Meilleure précision mais modèle plus gros (20 Mo), plus de CPU
- Seuillage couleur simple : Trop fragile aux variations du ciel
- Traceur OpenCV CSRT : Précis mais ~2-3 IPS sur CPU mobile

### 2. Méthode d'estimation de profondeur : Taille-depuis-pixels-en-perspective (SfPiP)

**Choix** : Inférer la distance depuis la taille apparente du cerf-volant en utilisant une taille de référence connue (dimensions calibrées du cerf-volant) et la distance focale de la caméra, avec correction de l'angle de vue.

**Formule** : `distance = (hauteur_reelle_cerf * focal_length) / (hauteur_apparente_pixels) * cos(angle_pitch)`

**Justification** :
- Méthode la plus directe avec une seule caméra et une taille d'objet connue
- La distance focale peut être lue depuis EXIF ou un motif d'étalonnage
- L'angle de tangage (pitch) depuis l'IMU du téléphone compense l'inclinaison de la caméra
- Précision attendue : ±15-20%, suffisant pour le mappage audio

**Alternatives envisagées** :
- Structure-from-Motion (SfM) : Requiert une ligne de base de mouvement image-à-image significative, instable aux courtes lignes de base
- Réseaux de profondeur monoculaires (MiDaS) : Lent (~1 IPS), produit une profondeur relative pas métrique
- Parallaxe depuis un plan au sol : Requiert une référence de sol plane, le cerf-volant à hauteur inconnue par rapport au sol

### 3. Système de coordonnées : NED (Nord-Est-Bas) avec le téléphone comme origine

**Choix** : Position 3D estimée exprimée en coordonnées relatives au téléphone (X droite, Y bas d'écran, Z avant), puis rotationnée en coordonnées monde NED utilisant la boussole/IMU du téléphone.

**Justification** :
- Simplifie le calcul de position initial depuis les coordonnées d'image
- Permet aux paramètres audio d'être mappés directement à la position d'écran (pan stéréo gauche/droite)
- Rotation en coordonnées monde effectuée via `getRotationMatrix()` + `remapCoordinateSystem()` d'Android

### 4. Calcul de vitesse : Position différenciée avec filtrage passe-bas

**Choix** : Vitesse = (position_courante - position_précédente) / delta_t, avec lissage EMA (α=0.3) pour réduire le jitter.

**Justification** :
- Simple, robuste et à faible latence
- Alternative : Filtre de Kalman surdimensionné pour cette application ; introduit du délai
- Estimation d'accélération par différenciation secondaire (optionnel, ajoute du bruit)

### 5. Sortie audio : Oboe + Synthèse temps réel

**Choix** : Bibliothèque Oboe pour audio basse latence (C++/JNI) avec synthèse en temps réel d'onde sinusoïdale/triangle, paramètres mis à jour par image de suivi.

**Justification** :
- Oboe fournit une latence <20ms sur la plupart des appareils Android
- Synthèse simple suffisante pour la sonification (hauteur X position, amplitude Z distance, timbre vitesse)
- Alternative : Jetpack Compose Media3 rejetée — latence plus élevée
- Sortie Bluetooth possible via le routage audio Android standard, mais ajoute ~100-200ms

## Risks / Trade-offs

**R1** : Profondeur monoculaire très sensible à une erreur de calibration de taille du cerf-volant
- [Mitigation] L'assistant de calibration utilise un objet de référence visible (ex. mètre ruban) ou demande à l'utilisateur de saisir les dimensions connues du cerf-volant

**R2** : Perte de suivi lorsque le cerf-volant se présente de profil (profil mince)
- [Mitigation] Le modèle de détection entraîné sur multiples orientations ; recours à la segmentation couleur+motion quand la confiance de détection est faible

**R3** : Dérive de l'IMU dans les angles de tangage (pitch) et roulis (roll)
- [Mitigation] Utiliser le gyroscope seulement pour l'inclinaison haute-fréquence, réaligner périodiquement via détection de l'horizon (optionnel) ou supposer une pose initiale à niveau

**R4** : Éblouissement sol / saturation du ciel qui efface le cerf-volant
- [Mitigation] Utiliser l'espace couleur HSV pour le masque de couleur du cerf-volant en secours ; demander à l'utilisateur de choisir une couleur à haut contraste

**R5** : Latence audio du Bluetooth (délai codec A2DP)
- [Mitigation] Recommander écouteurs filaires ou haut-parleurs locaux ; profil de latence détectable dans les paramètres

## Migration Plan

**Phase 1** : Validation prototype (2-3 semaines)
- Implémenter détection+suivi sur vidéos de test, vérifier la qualité de position
- Tester la précision de la formule de profondeur avec des distances connues sur le terrain

**Phase 2** : Calibration & UI (1 semaine)
- Construire le flux d'assistant de calibration avec superpositions visuelles
- Intégrer l'IMU pour compensation de tangage

**Phase 3** : Intégration audio (1 semaine)
- Connecter le flux de télémétrie au synthétiseur Oboe
- Mappage des paramètres : X → pan stéréo, Z → volume/amplitude, vitesse → taux de modulation

**Phase 4** : Tests terrain (1-2 semaines)
- Sessions de vol en extérieur, collecte de données de précision
- Ajuster les courbes de mappage audio selon la réactivité perçue

## Open Questions

- Quelle est l'altitude de suivi maximale attendue ? Cela affecte le FOV et les exigences de résolution de la caméra — probablement gamme 50-200m.
- Le système doit-il stocker la télémétrie de vol pour relecture ultérieure ? (Hors scope Phase 1, pourrait être ajouté comme capacité séparée)
- Orientation du cerf-volant (roulis/tangage) suivie ? Seule la position et la vitesse sont nécessaires pour l'audio.
- Couleur du cerf-volant : supposer couleur unie et à haut contraste pour le secours de segmentation ? Palette sélectionnable par l'utilisateur ?
- Niveau d'API Android cible ? Minimum API 23 (Android 6.0) pour Camera2.
- Génération audio sur téléphone vs Bluetooth LE MIDI vers synthé externe ? Synthèse locale plus simple.
