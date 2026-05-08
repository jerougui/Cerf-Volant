# Instructions de build – Cerf-volant (Windows)

## Prérequis

1. **Java 17** (ou 21) installé
   - Télécharger : https://adoptium.net/
   - Vérifier : `java -version` dans PowerShell

2. **Android SDK** (avec plateforme API 34, build-tools 34.0.0)
   - Installé via Android Studio
   - Variables d'environnement :
     - `ANDROID_HOME` = `C:\Users\VOTRE_NOM\AppData\Local\Android\Sdk`
     - Ajouter `%ANDROID_HOME%\platform-tools` au PATH

3. **Gradle wrapper** (inclus) – pas besoin d'installer Gradle séparément

---

## Build en ligne de commande (PowerShell)

```powershell
# Se placer dans le module
cd cerf-volant-camera-tracker

# Premier build (télécharge Gradle 8.5 automatiquement, ~5 min)
.\gradlew.bat assembleDebug

# Si erreur de permissions, débloquer le script :
# Unblock-File .\gradlew.bat

# L'APK se trouve ici :
# cerf-volant-camera-tracker\app\build\outputs\apk\debug\app-debug.apk
```

**Note** : La première exécution télécharge Gradle (environ 100 MB). Les suivantes seront rapides.

---

## Build via Android Studio (recommandé)

1. Ouvrir Android Studio → **File** → **Open** → sélectionner `cerf-volant-camera-tracker/`
2. Attendre la synchro Gradle (clique sur **Sync Now** si demandé)
3. Menu **Build** → **Make Project** (ou `Ctrl+F9`)
4. L'APK est généré dans `app/build/outputs/apk/debug/`
5. Glisser-déposer l'APK sur le téléphone, ou utiliser le Device File Explorer d'Android Studio

---

## Dépannage build

| Erreur | Cause probable | Solution |
|--------|----------------|----------|
| `java not found` | Java pas dans PATH | Installer JDK 17, redémarrer PowerShell |
| `ANDROID_HOME not set` | SDK non configuré | Définir variable d'environnement, ou configurer dans `local.properties` |
| `Failed to find target with hash string 'android-34'` | API 34 non installée | Android Studio → SDK Manager → installer Android 34 |
| `Could not resolve all files for configuration` | Problème de dépendances | `.\gradlew.bat --refresh-dependencies` |
| `Execution failed for task ':app:mergeDebugNativeLibs'` | Conflit NDK | Installer NDK via SDK Manager (r25+) |

---

## Installation de l'APK

Une fois l'APK généré :

```powershell
# Si adb installé (optionnel)
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Sans adb : copier manuellement l'APK sur le téléphone (voir README.md)
```

---

## Nettoyage

```powershell
.\gradlew.bat clean
```

Supprime tous les fichiers build.

---

## Build release (signature)

⚠️ Non couvert par ce guide. Pour distribution, créer un keystore et configurer `signingConfigs` dans `app/build.gradle`.

---

**Build réussi ?** Passez à `TESTING.md` pour valider l'application.
