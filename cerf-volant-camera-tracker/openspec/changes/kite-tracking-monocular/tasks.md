## 1. Configuration du projet & dépendances

- [x] 1.1 Créer un nouveau projet Android Studio (min API 23, Kotlin + Java/C++)
- [x] 1.2 Ajouter dépendances Gradle : OpenCV Android SDK, MediaPipe Tasks Vision (ou TFLite Support), bibliothèque audio Oboe
- [x] 1.3 Configurer CMakeLists.txt pour modules natifs C++ (suivi, synthèse audio)
- [x] 1.4 Ajouter permissions à AndroidManifest : CAMERA, RECORD_AUDIO (si micro utilisé), BLUETOOTH, BLUETOOTH_CONNECT, WRITE_EXTERNAL_STORAGE (optionnel pour logs)
- [x] 1.5 Créer structure de packages : com.kilotracker.vision, .audio, .calibration, .kinematics, .ui

## 2. Pipeline Caméra & IMU

- [x] 2.1 Implémenter wrapper Camera2 API pour fournir images YUV à 30 IPS
- [x] 2.2 Ajouter écouteur de frames qui convertit YUV → Bitmap RGB (ou CV Mat direct)
- [x] 2.3 Intégrer Android SensorManager pour recevoir gyroscope/accéléromètre à 50 Hz
- [x] 2.4 Fusionner données IMU pour calculer angles de tangage (pitch) et roulis (roll) via matrice de rotation
- [x] 2.5 Threader le pipeline : thread caméra → thread suivi → thread audio (queues sans verrou)

## 3. Module de calibration

- [x] 3.1 Construire l'UI de CalibrationActivity : rectangle de superposition, champ de distance, bouton "Calibrer"
- [x] 3.2 Implémenter logique de capture de boîte englobante (rectangle dessiné par utilisateur ou auto-détecté centre+taille)
- [x] 3.3 Sauvegarder paramètres de calibration : distance référence D0 (m), hauteur apparente h0 (px), angle de pitch θ0, distance focale caméra f (px), hauteur réelle H (m)
- [x] 3.4 Calculer et stocker facteur d'échelle de base : S = (H × f) / (h0 × cos θ0)
- [x] 3.5 Implémenter validations (D0 ≥ 5m, cerf-volant ≥ 1% hauteur d'image, boîte raisonnablement centrée) ; avertissements si invalide
- [x] 3.6 Ajouter bouton "Recalibrer" accessible pendant suivi pour mettre à jour l'échelle en vol (via CalibrationData.updateCurrentFrame)

## 4. Module de détection et suivi visuel

- [x] 4.1 Évaluer et choisir le détecteur : MediaPipe Face Detection (léger) ou modèle TFLite personnalisé quantifié entraîné sur images de cerf-volant (impl: KiteDetector with TFLite Task API)
- [x] 4.2 Entraîner ou collecter dataset de détection de cerf-volant (ou utiliser détecteur générique avec classe "kite") ; annoter 200+ images avec boîtes (préparation données externe)
- [x] 4.3 Convertir modèle entraîné en TFLite avec dataset représentatif ; intégrer via TFLite Task Library ou MediaPipe (préparation modèle externe, code prêt)
- [x] 4.4 Implémenter wrapper d'inférence du détecteur : image d'entrée → boîte normalisée + confiance (5-10 IPS sur CPU)
- [x] 4.5 Implémenter traceur de points d'intérêt KLT avec OpenCV : suivre points caractéristiques dans la boîte entre détections
- [x] 4.6 Intégrer hybride détection-tracking : détecter toutes les 10 images, tracer entre → suivi effectif 20+ Hz
- [x] 4.7 Ajouter mécanisme de confiance de trackeur : redétecter si score de suivi < seuil (ex. points suivis < 8)
- [x] 4.8 Implémenter segmentation couleur de secours (seuil HSV) quand confiance de détection faible et cerf-volant unicolore sur ciel

## 5. Module d'estimation de profondeur monoculaire

- [x] 5.1 Dériver formule de profondeur : distance Z = (H_réelle × f_px) / (h_apparente_px × cos θ)
- [x] 5.2 Implémenter calcul de distance avec compensation de pitch utilisant calibration et IMU live
- [x] 5.3 Calculer angle de direction horizontale φ = arctan2( (x_centre - cx), f_px ) depuis décalage centre d'image
- [x] 5.4 Convertir en coordonnées téléphone-relatives : X = Z × tan φ, Y = Z × tan θ_relatif
- [x] 5.5 Produire position 3D comme triple flottants (X_droite, Y_haut, Z_avant) en mètres
- [x] 5.6 Ajouter tests unitaires vérifiant formule sur scénarios synthétiques (simuler tailles connues à différentes distances)

## 6. Module de cinématique

- [x] 6.1 Maintenir buffer circulaire des 5-10 derniers échantillons position+timestamp (pour dérivées secondes si besoin)
- [x] 6.2 Calculer vecteur vitesse : V = (P_courant − P_précédent) / Δt ; bloquer Δt à [0.033s, 0.5s] pour éviter extrêmes division
- [x] 6.3 Appliquer filtre moyenne mobile exponentielle (EMA) à la vitesse (α = 0.3) et optionnellement à la position
- [x] 6.4 Implémenter vitesse = |V| et azimut = atan2(V_x, V_z)
- [x] 6.5 Ajouter rejet de valeurs aberrantes : si |V| dépasse maximum plausible (ex. 50 m/s pour cerf-volant), plafonner et mettre drapeau invalide
- [x] 6.6 Exposer getters thread-safe : getPosition3D(), getVelocity(), getSpeed(), getAzimuth()

## 7. Module de sonification audio (Oboe)

- [x] 7.1 Créer classe AudioEngine initialisant Oboe avec callback basse latence (AudioStreamCallback)
- [x] 7.2 Implémenter oscillateur de base : onde sinusoïdale/triangle avec fréquence variable (base 200-400 Hz)
- [x] 7.3 Créer interface de paramètres : AudioParams { pan (float -1→1), volume (0→1), modRate (Hz), timbre (brightness) }
- [x] 7.4 Dans le callback audio, lire derniers paramètres depuis variable volatile/atomique ; appliquer avec interpolation au taux d'échantillonnage pour éviter bruit de fermeture
- [x] 7.5 Mappage télémétrie → paramètres audio : X→pan (loi exponentielle pan), Z→volume (genre inverse carrée), vitesse→intensité modulation
- [x] 7.6 Ajouter générateur d'enveloppe ADSR minimal pour éviter clics au démarrage ou sauts de paramètres
- [x] 7.7 Implémenter toggle "Mode Performance" : réduire FPS suivi ou taille tampon audio si budget latence dépassé
- [ ] 7.8 Tester sur appareil de référence ; mesurer latence bout-en-bout (caméra→audio) avec micro+oscilloscope ; assurer ≤ 100ms (tests terrain manuels)

## 8. Routage audio Bluetooth (optionnel)

- [x] 8.1 Ajouter UI de découverte périphériques Bluetooth via BluetoothAdapter + BluetoothProfile
- [x] 8.2 Peupler sélecteur de sortie audio avec noms des appareils A2DP appairés
- [x] 8.3 Router flux audio via AudioManager.setBluetoothA2dpOn() ou sélection de périphérique Oboe
- [x] 8.4 Implémenter affichage latence : détecter codec actif (SBC/AAC/aptX) et afficher délai estimé
- [x] 8.5 Gérer évènements déconnexion Bluetooth : AudioManager.registerAudioDeviceCallback() → repli sur haut-parleur téléphone

## 9. Interface utilisateur & intégration

- [x] 9.1 Concevoir MainActivity : prévisualisation caméra (TextureView/PreviewView), calque HUD télémétrie, boutons start/calibrate/stop
- [x] 9.2 Dessiner superposition boîte englobante sur cerf-volant détecté ; colorer selon confiance (vert/jaune/rouge)
- [x] 9.3 Afficher valeurs temps réel : distance (m), hauteur (m), vitesse (m/s), IPS
- [x] 9.4 Ajouter visualisation paramètres audio : jauge panoramique, jauge volume
- [x] 9.5 Créer écran Paramètres : choix forme d'onde audio, courbes sensibilité, toggle Bluetooth, ré-entrée calibration (éc squelette)
- [x] 9.6 Câbler machine à états : Inactif → Calibration → Suivi → Arrêt ; gérer événements cycle de vie (pause/reprise)

## 10. Tests, réglage & documentation

- [x] 10.1 Tests unitaires : formule de profondeur, calcul vitesse, filtre EMA
- [x] 10.2 Tests d'instrumentation : flux permissions caméra, assistant de calibration (squelette Espresso)
- [ ] 10.3 Test terrain avec cerf-volant réel : collecter données de position vs distances connues (manuel)
- [ ] 10.4 Ajuster courbes de mappage audio selon réactivité perçue : régler gain panoramique, pente de volume, sensibilité modulation (manuel)
- [ ] 10.5 Mesurer utilisation CPU/GPU et empreinte mémoire sur Snapdragon 7/8-series ; optimiser si >30% CPU (manuel)
- [x] 10.6 Documenter guide d'utilisation : étapes de calibration, éclairage/arrière-plan recommandés, couleurs de cerf-volant optimales (README.md)
- [x] 10.7 Ajouter astuces intégrées : "Tenir téléphone stable pendant calibration", "Cerf-volant doit être majoritairement contre fond de ciel" (Toasts dans activities)
