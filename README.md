# Cerf-volant – Suivi de cerf-volant par téléphone Android

Application de suivi en temps réel d'un cerf-volant utilisant la caméra d'un téléphone Android pour estimer position 3D et vitesse, afin de générer de l'audio sonifié.

> **Note** : Ce projet a été développé selon une approche spec-driven avec OpenSpec. Les spécifications se trouvent dans `openspec/changes/kite-tracking-monocular/`.

## Fonctionnalités

- **Calibration** : Définir la distance initiale et la taille du cerf-volant ; la caméra est étalonnée une fois.
- **Suivi visuel** : Détection hybride détection d'objet (TFLite) + suivi optique KLT OpenCV pour un flux fluide (20-30 IPS).
- **Estimation 3D** : Calcul approximatif de distance, hauteur, direction depuis la taille apparente et la perspective.
- **Cinématique** : Vitesse et azimut calculés avec filtrage EMA.
- **Sonification** : Oboe (C++) génère un ton continu où la panoramique, volume et modulation suivent le vol du cerf-volant.
- **Bluetooth optionnel** : Routage audio vers casque/enceinte Bluetooth (avertissement latence).

## Déploiement rapide

```bash
# Cloner et builder
git clone https://github.com/jerougui/Cerf-volant.git
cd Cerf-volant
./gradlew assembleDebug

# Installation sur appareil connecté
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Prérequis

- Android 6.0 (API 23) minimum
- Téléphone avec caméra arrière
- **Modèle de détection** : `kite_detect.tflite` à placer dans `app/src/main/assets/` (à entraîner séparément – voir ci-dessous)
- SDK Android, Gradle, NDK (installés via Android Studio)

## Construction

```bash
# Version debug
./gradlew assembleDebug

# Version release (signature manuelle)
./gradlew assembleRelease
```

**Dépendances principales** :
- OpenCV 4.9 (via Maven)
- TensorFlow Lite Task Vision
- Oboe (bibliothèque native incluse dans `app/src/main/cpp/`)

## Guide de test (première version)

Voir le fichier [TESTING.md](TESTING.md) pour une checklist complète de validation fonctionnelle.

**Points critiques à vérifier :**
1. Calibration : distance initiale saisie correctement enregistrée
2. Suivi : la boîte verte suit le cerf-volant avec ≤ 3px d'erreur
3. Distance : lue à ±20% de la vraie distance
4. Audio : panoramique et volume réagissent en temps réel (< 100ms perçu)
5. Bluetooth : routage fonctionne, fallback automatique en cas de déconnexion

## Modèle de détection

L'application utilise un modèle TFLite pour la détection du cerf-volant.

**Pour générer `kite_detect.tflite` :**
1. Collecter 200+ images du cerf-volant contre ciel (divers angles, distances, luminosités)
2. Annoter avec boundings boxes (format Pascal VOC ou YOLO)
3. Entraîner avec TensorFlow Lite Model Maker (Python) ou convertisseur TF → TFLite
4. Placer le fichier `.tflite` dans `app/src/main/assets/`

**Mode dégradé (sans modèle)** :  
Le code inclut un détecteur couleur HSV (rouge par défaut). Pour l'utiliser, modifier `TrackingManager.kt` :
```kotlin
val normalizedBbox = ColorFallbackDetector.detect(mat)  // au lieu de detector.detect()
```
Ajuster les plages HSV dans `ColorFallbackDetector.kt` si votre cerf-volant n'est pas rouge.

## Architecture du code

```
app/src/main/java/com/kilotracker/
├── audio/        → AudioEngine (Oboe JNI), TelemetryMapper, BluetoothRouter
├── calibration/  → SensorFusion (IMU), CalibrationData (SharedPreferences)
├── kinematics/   → DepthEstimator (profondeur monoculaire), Kinematics (vitesse)
├── ui/           → MainActivity, CalibrationActivity, SettingsActivity
└── vision/       → CameraManager (Camera2 API), KiteDetector (TFLite), KLTTracker (OpenCV), TrackingManager
```

Module natif C++ (`app/src/main/cpp/`) :
- `audio_synth.cpp` : Synthé audio basse latence (callback Oboe)
- `tracking_utils.cpp` : Fonctions mathématiques (vitesse, EMA)

## Paramètres OpenSpec

Les spécifications formelles, le design technique et la liste des tâches sont dans :
- `openspec/changes/kite-tracking-monocular/proposal.md`
- `openspec/changes/kite-tracking-monocular/design.md`
- `openspec/changes/kite-tracking-monocular/specs/*/spec.md`
- `openspec/changes/kite-tracking-monocular/tasks.md`

## Limitations connues

- Précision de distance : ~±20% (dépend de la calibration et de la constance de la taille réelle)
- Performances dégradées en contre-jour, ciel uniforme ou faible luminosité
- Bluetooth A2DP : latence 100-200 ms (déconseillé pour usage critique)
- Ne suit qu'un seul cerf-volant à la fois
- Nécessite une calibration initiale avec distance mesurée

## Workflow OpenSpec

Ce projet a été développé en utilisant le workflow **spec-driven** d'OpenSpec :

```bash
# Création de la proposition
openspec propose "Suivi cerf-volant avec téléphone unique..."

# Génération des specs et tâches (déjà fait)
# Phase d'implémentation :
./opsx:apply

# Une fois les tests terrain effectués :
./opsx:archive
```

Voir `openspec/` pour les workflows complets.

## Crédits

- **Bibliothèques** : OpenCV, TensorFlow Lite, Oboe
- **Workflow** : OpenSpec (spec-driven development)
- **Auteur** : Développé avec Kilo (agent de génie logiciel)

## Licence

MIT – libre d'usage, modification et redistribution.
