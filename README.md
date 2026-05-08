# Cerf-Volant

Projet global de suivi de cerf-volant par vision monoculaire et génération audio en temps réel.

## Modules

- **cerf-volant-camera-tracker/** – Module principal de suivi par caméra Android
  - Détection hybride (TFLite + suivi KLT)
  - Estimation 3D par taille apparente
  - Sonification Oboe
  - Bluetooth A2DP

Voir leREADME du module pour plus de détails : [cerf-volant-camera-tracker/README.md](cerf-volant-camera-tracker/README.md)

## Build du module

```bash
cd cerf-volant-camera-tracker
./gradlew assembleDebug
```

## Workflow

Ce projet utilise OpenSpec (spec-driven) pour la gestion des spécifications et des tâches.

```bash
cd cerf-volant-camera-tracker/openspec
openspec status --change kite-tracking-monocular
```

---

Développé avec Kilo – agent de génie logiciel.
