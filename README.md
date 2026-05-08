# Kite Tracker – Suivi de cerf-volant par téléphone Android

Application de suivi en temps réel d'un cerf-volant utilisant la caméra d'un téléphone Android pour estimer position 3D et vitesse, afin de générer de l'audio sonifié.

## Fonctionnalités

- **Calibration** : Définir la distance initiale et la taille du cerf-volant ; la caméra est étalonnée une fois.
- **Suivi visuel** : Détection hybride détection d'objet (TFLite) + suivi optique KLT OpenCV pour un flux fluide (20-30 IPS).
- **Estimation 3D** : Calcul approximatif de distance, hauteur, direction depuis la taille apparente et la透视.
- **Cinématique** : Vitesse et azimut calculés avec filtrage EMA.
- **Sonification** : Oboe (C++) génère un ton continu où la panoramique, volume et modulation suivent le vol du cerf-volant.
- **Bluetooth optionnel** : Routage audio vers casque/enceinte Bluetooth (avertissement latence).

## Prérequis

- Android 6.0 (API 23) minimum
- Téléphone avec caméra arrière
- Modèle de détection `kite_detect.tflite` à placer dans `app/src/main/assets/` (à entraîner séparément)

## Construction

```bash
# Assurez-vous d'avoir le SDK Android et Gradle installés
./gradlew assembleDebug
```

Le projet utilise :
- OpenCV 4.9 (via Maven)
- TensorFlow Lite Task Vision
- Oboe (bibliothèque native incluse comme sous-module – à ajouter si absent)

## Utilisation

1. Lancer l'application
2. Première utilisation : **Calibration**
   - Placer le cerf-volant à distance mesurée (ex. 20 m)
   - Pointer la caméra ; l'overlay vert montre la boîte englobante
   - Entrer la distance (m) et appuyer **Calibrer**
3. **Démarrer le suivi**
   - Le système détecte et suit le cerf-volant
   - Les valeurs de distance, hauteur, vitesse s'affichent
   - Un ton audio se met à jour selon la position
4. **Paramètres** (bouton en bas) :
   - Choisir la forme d'onde
   - Ajuster sensibilité
   - Basculer Mode Performance si latence
   - Sélectionner sortie Bluetooth (avec affichage latence estimée)

## Calibration de secours

Pendant le suivi, bouton **Recalibrer** permet de mettre à jour l'échelle si la précision dérive (ex. après changement d'altitude important).

## Architecture (ouverts)

- `com.kilotracker.vision` – Camera2, détection, suivi KLT
- `com.kilotracker.calibration` – SensorFusion (IMU), stockage paramètres
- `com.kilotracker.kinematics` – DepthEstimator, Kinematics
- `com.kilotracker.audio` – AudioEngine Oboe, BluetoothRouter, TelemetryMapper
- `com.kilotracker.ui` – Activités Android (Main, Calibration, Settings)

## Limitations connues

- Précision de distance ~±20% (taille réelle du cerf-volant doit être connue)
- Performances dégradées en contre-jour ou ciel uniforme
- Bluetooth A2DP ajoute 100-200 ms de latence audio

## Crédits

Développé avec OpenSpec (spec-driven) et Oboe.
