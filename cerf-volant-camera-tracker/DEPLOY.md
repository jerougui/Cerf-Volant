# Déploiement sur GitHub

Ce projet a été initialisé localement. Voici les étapes pour le publier sur votre compte GitHub.

## 1. Créer le dépôt distant

1. Allez sur https://github.com/new
2. Nom du dépôt : `Cerf-volant`
3. Description (optionnel) : `Suivi de cerf-volant par téléphone Android – estimation 3D et sonification en temps réel`
4. Choisissez **Public** ou **Private**
5. **Ne pas** cocher "Initialize this repository with a README", .gitignore ou license
6. Cliquez sur **Create repository**

## 2. Lier le dépôt local au distant

Dans le terminal (PowerShell ou CMD) à la racine du projet :

```powershell
# Vérifiez que le remote 'origin' est déjà configuré :
git remote -v
# Doit afficher : origin https://github.com/jerougui/Cerf-volant.git

# Si ce n'est pas le cas, ajoutez-le :
git remote add origin https://github.com/jerougui/Cerf-volant.git
```

## 3. Pousser le code

```powershell
# Pousser la branche master vers GitHub
git push -u origin master
```

**Authentification** : GitHub vous demandera votre nom d'utilisateur et un **Personal Access Token (PAT)**.
- Créez un token sur https://github.com/settings/tokens (permissions : `repo`)
- Utilisez ce token comme mot de passe lorsque prompted

## 4. Vérification

Ouvrez https://github.com/jerougui/Cerf-volant – vos fichiers doivent être visibles.

## Dépannage

| Problème | Solution |
|----------|----------|
| `fatal: remote origin already exists` | Le distant existe déjà ; exécutez `git remote set-url origin https://github.com/jerougui/Cerf-volant.git` |
| `remote: Repository not found` | Vérifiez que le repo a bien été créé sur GitHub et que vous utilisez le bon username |
| `Authentication failed` | Vérifiez votre PAT (expiration, permissions) ou utilisez `gh auth login` si GitHub CLI安装 |

## 5. (Optionnel) Configurer GitHub CLI

Si vous préférez utiliser `gh` pour l'authentification :

```powershell
winget install GitHub.cli  # ou choco install gh
gh auth login
# Suivez les étapes ( choisir HTTPS, authenticant avec browser ou PAT )
```

Ensuite, `git push` fonctionnera sans demander identifiants à chaque fois.

---

Une fois le dépôt poussé, vous pouvez partager la URL :
🔗 **https://github.com/jerougui/Cerf-volant**
