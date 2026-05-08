# Guide de test – Cerf-volant v0.1

## Préparation

### 1. Modèle de détection (obligatoire)
L'application nécessite un modèle TFLite `kite_detect.tflite` dans `app/src/main/assets/`.

**Option recommandée** (test sans modèle) :
Modifier `TrackingManager.kt` ligne 40 pour utiliser le détecteur couleur :
```kotlin
val normalizedBbox = ColorFallbackDetector.detect(mat)  // au lieu de detector.detect()
```
Cela utilise la segmentation HSV (couleur rouge par défaut). Si votre cerf-volant n'est pas rouge, ajuster `lowerRed1/upperRed1` et `lowerRed2/upperRed2` dans `ColorFallbackDetector.kt`.

**Option avec modèle ML** :
- Collecter 200+ photos de cerf-volant contre ciel varié
- Annoter avec labelImg (format Pascal VOC ou YOLO)
- Entraîner avec TensorFlow Lite Model Maker (Python) :
```python
from tflite_model_maker import image_classifier
# ou object_detector avec custom dataset
```
- Exporter en `.tflite` et copier dans `app/src/main/assets/kite_detect.tflite`

### 2. Build et installation
```bash
# compiler
./gradlew assembleDebug

# installer sur appareil connecté
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Permissions
Au premier lancement, accorder :
- Caméra
- Audio (optionnel)
- Bluetooth (si casque BT)

---

## Checklist de test manuel

### A. Calibration initiale
- [ ] Lancer l'app → CalibrationActivity s'affiche
- [ ] Preview caméra fonctionne (arrière)
- [ ] Overlay vert rectangulaire centré visible
- [ ] Saisir distance réelle (ex. 20.0 m)
- [ ] Si distance < 5m : warning "Veuillez reculer" s'affiche ✓
- [ ] Si OK : bouton "Calibrer" → Toast "Calibration enregistrée" ✓
- [ ] Passage automatique à MainActivity

### B. Suivi visuel
- [ ] MainActivity : bouton "Démarrer" lance la caméra
- [ ] Boîte de suivi verte apparaît sur le cerf-volant
- [ ] La boîte suit le cerf-volant avec un lag ≤ 3 images
- [ ] En nivelant le téléphone, la boîte ne perd pas la cible
- [ ] Si le cerf-volant tourne de profil : le suivi peut se brièvement perdre, mais doit se ré-acquérir en < 1 seconde

### C. Estimation distance / hauteur
- [ ] Valeur "Dist:" affichée en mètres
- [ ]À la calibration (20m) : distance lue ≈ 20 ± 4m (±20%)
- [ ] En s'éloignant (30-40m) : distance augmente
- [ ] En se rapprochant (10-15m) : distance diminue
- [ ] Valeur "H:" (hauteur) évolue de manière monotone quand le cerf-volant monte/descend

### D. Vitesse et direction
- [ ] "Spd:" affiche une valeur > 0 m/s en vol
- [ ] Pas de spikes brutaux (pas de 200 m/s soudains)
- [ ] Vitesse augmente progressivement lors des accélérations du cerf-volant
- [ ] En tenant le cerf-volant immobile : vitesse ≤ 0.2 m/s

### E. Audio sonification
- [ ] Ton continu audible (sine wave ~220 Hz)
- [ ] **Panoramique** : lorsque le cerf-volant passe à droite, le son se déplace vers l'oreille droite (et inversement)
- [ ] **Volume** : quand le cerf-volant s'éloigne, le volume diminue ; quand il se rapproche, le volume augmente
- [ ] **Modulation** : en augmentant la vitesse (coup de vent), le tremblement (vibrato) du ton s'intensifie
- [ ] Aucun clic ni pop au démarrage/arrêt

### F. Recalibration en vol
- [ ] Bouton "Recalibrer" → dialogue (à implémenter) ou logique simplifiée
- [ ] Si distance affichée dérive, recalibrer : entrer nouvelle distance mesurée
- [ ] L'estimation se corrige dans les 5 secondes suivantes

### G. Bluetooth (optionnel)
- [ ] Pairer un casque/enceinte Bluetooth
- [ ] Dans l'app, le device apparaît dans le spinner
- [ ] Sélection → audio bascule vers BT
- [ ] Message "Bluetooth latency: ~XXXms" s'affiche
- [ ] Déconnecter le BT → audio bascule automatiquement sur haut-parleur téléphone

### H. Performance et latence
- [ ] FPS affiché ≥ 20 (idéalement 25-30)
- [ ] CPU (via `adb shell top`) ≤ 30% sur Snapdragon 7/8-series
- [ ] Latence perçue : mouvement→son ≤ 100 ms (subjectif)

### I. Robustesse
- [ ] Passage d'un nuage : le suivi ne saute pas
- [ ] Contre-jour partiel : la boîte reste accrochée
- [ ] Si perte totale > 2s : message "Tracking lost" apparaît

---

## Scénarios d'échec courants et correctifs

| Symptôme | Cause probable | Correctif |
|----------|----------------|-----------|
| Aucune boîte détectée | `kite_detect.tflite` absent | Utiliser fallback couleur (modif TrackingManager) |
| Distance complètement fausse (>2x) | Mauvaise calibration (taille réelle H incorrecte) | Recalibrer avec distance exacte, vérifier hauteur réelle du cerf-volant |
| Audio ne sort pas | Oboe natif pas chargé | Vérifier `System.loadLibrary("audio_synth")`, logcat erreurs JNI |
| tremblement du son | Pan/volume trop brutaux | Augmenter `envelopeRate` dans `audio_synth.cpp` |
| Latence élevée Bluetooth | Codec SBC (180ms) | Basculer sur API 26+ avec AAC si supporté, ou utiliser filaire |

---

## collecte de données pour validation (optionnel)

```bash
# Enregistrer logcat pendant un vol
adb logcat -s "KiteTracker" > kite_log.txt

# Extraire les valeurs de distance/vitesse (si vous ajoutez des logs dans le code)
```

À comparer avec un télémètre laser ou mesures au sol (mètre ruban à distances connues).

---

 Une fois les tests concluants, vous pouvez :
1. Marquer les tâches 10.3, 10.4, 10.5 comme complétées dans `openspec/changes/kite-tracking-monocular/tasks.md`
2. Archiver le changement : `/opsx:archive`
