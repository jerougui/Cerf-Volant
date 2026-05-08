## Why

Le suivi manuel d'un cerf-volant ou l'utilisation d'équipements spécialisés coûteux limite l'accessibilité. Un téléphone Android unique peut fournir un suivi en temps réel de la position 3D (distance, altitude, direction) et de la vitesse d'un cerf-volant grâce à la vision par ordinateur, permettant des applications de génération musicale live comme la sonification des dynamiques de vol, des expériences musicales immersives ou des installations artistiques. Cette solution démocratise la télémétrie des cerfs-volants en exploitant le matériel smartphone omniprésent.

## What Changes

- **Module de calibration** : Définition de la distance initiale, marquage de la boîte englobante du cerf-volant, définition de l'orientation de la caméra.
- **Module de détection et suivi visuel** : Détection d'objet et suivi continu du cerf-volant grâce à des algorithmes de vision par ordinateur.
- **Module d'estimation 3D** : Position approximative (distance, hauteur, direction) basée sur la taille apparente, la perspective et le mouvement relatif.
- **Module de calcul de vitesse et direction** : Vecteur de vitesse et direction de vol à partir de l'historique de positions.
- **Module de sortie audio** : Génération audio locale ou transmission Bluetooth des données de télémétrie.

## Capabilities

### New Capabilities

- `kite-visual-tracking` : Détecter et suivre en continu un objet cerf-volant spécifique dans les flux vidéo grâce à l'appariement de caractéristiques ou la détection d'objet.
- `monocular-depth-estimation` : Estimer la position 3D approximative (distance, hauteur, direction) à partir d'une seule caméra en utilisant la taille connue de l'objet, la géométrie de perspective et le mouvement parallaxe.
- `kite-kinematics` : Calculer le vecteur de vitesse instantanée, l'amplitude de la vitesse et la direction de vol à partir des données de position.
- `calibration-phase` : Configuration initiale guidée pour établir les paramètres de référence (distance au sol, taille du cerf-volant, angle de la caméra).
- `audio-sonification` : Convertir les données de position et de vitesse du cerf-volant en signaux audio (fréquence, amplitude, modulation) pour une génération en temps réel.
- `bluetooth-audio-streaming` : Transmettre optionnellement l'audio ou les données de contrôle vers des périphériques audio Bluetooth externes.

### Modified Capabilities

*(aucune)*

## Impact

- **Plateforme** : Application Android utilisant Camera2 API, MediaPipe/OpenCV pour la vision, Oboe pour l'audio basse latence, API Bluetooth Android.
- **Dépendances** : TensorFlow Lite ou MediaPipe pour la détection d'objet, potentiellement ARCore pour l'estimation de pose, bibliothèques de synthèse audio.
- **Matériel** : Utilise la caméra du téléphone (n'importe quelle résolution), IMU pour la stabilisation, haut-parleurs ou sortie Bluetooth.
- **Contraintes** : Limitée par la vision monoculaire (pas de profondeur réelle), nécessite un bon éclairage, le cerf-volant doit être visuellement distinct du ciel/arrière-plan.
