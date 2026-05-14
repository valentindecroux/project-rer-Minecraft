# MTR Web Map — Guide d'installation

## Ce que ça fait

1. **Le mod Forge** tourne sur ton serveur Minecraft à côté du mod MTR.
   - Il lit les données MTR (stations, lignes) via l'API interne de MTR
   - Il les envoie vers un fichier `data.json` sur GitHub toutes les 2 minutes
   - Il ajoute la commande `/map` qui donne un lien cliquable dans le chat

2. **Le site GitHub Pages** (dossier `mtr-webmap-site/`) se charge dans le
   navigateur et affiche :
   - Une carte interactive du réseau (pan + zoom)
   - Un calculateur d'itinéraire
   - Un tableau des prochains départs par station

---

## Étape 1 — Préparer GitHub Pages

1. Dans ton repo GitHub `valentindecroux/project-rer-Minecraft` :
   - Crée le dossier `webmap/` à la racine
   - Copie les fichiers de `mtr-webmap-site/` dans ce dossier
   - Copie aussi un fichier vide `webmap/data.json` (déjà fourni)

2. Active GitHub Pages sur ce repo :
   - Settings → Pages → Source : "Deploy from branch"
   - Branch : `main`, dossier : `/(root)` ou `webmap/` selon ta config

3. L'URL du site sera : `https://valentindecroux.github.io/project-rer-Minecraft/webmap/`

---

## Étape 2 — Créer un token GitHub

1. Va sur https://github.com/settings/tokens
2. "Generate new token (classic)"
3. Nom : `mtrwebmap-server`
4. Scope : coche uniquement **`repo`** (Public repo uniquement = `public_repo`)
5. Génère et copie le token (il commence par `ghp_...`)

---

## Étape 3 — Compiler le mod

**Pré-requis :** Java 17 JDK installé

```bash
cd mtr-webmap-mod/

# Télécharger le wrapper Gradle
./gradlew build    # sur Mac/Linux
gradlew.bat build  # sur Windows
```

> La première compilation prend ~5 min (télécharge Forge).
> Le JAR final se trouve dans `build/libs/mtrwebmap-1.0.0.jar`

---

## Étape 4 — Installer sur le serveur

1. Copie `mtrwebmap-1.0.0.jar` dans le dossier `mods/` de ton serveur Exaroton
   (au même endroit que `MTR-forge-4.0.4+1.19.2.jar`)

2. Démarre le serveur une première fois pour générer le fichier de config.

3. Édite `config/mtrwebmap-server.toml` :

```toml
[github]
  token = "ghp_XXXX..."         # ton token GitHub
  repo = "valentindecroux/project-rer-Minecraft"
  branch = "main"
  dataPath = "webmap/data.json"

[webapp]
  siteUrl = "https://valentindecroux.github.io/project-rer-Minecraft/webmap/"
  serverName = "ProjectRer"

[export]
  pushIntervalSeconds = 120     # toutes les 2 minutes
  dimension = "minecraft:overworld"
```

4. Redémarre le serveur.

---

## Commandes disponibles

| Commande | Permission | Description |
|---|---|---|
| `/map` | Tous | Affiche le lien vers la carte dans le chat |
| `/mtrexport` | Ops (level 2) | Déclenche un export immédiat |

---

## Vérifier que ça marche

Après le démarrage du serveur :
1. Attends 30 secondes
2. Lance `/mtrexport` en jeu
3. Regarde les logs du serveur : tu dois voir `✅ Données exportées vers GitHub`
4. Va sur l'URL GitHub Pages → la carte doit afficher les stations

---

## Dépannage

**"Port MTR non disponible"** → MTR n'est pas encore initialisé. Attends 30-60s après le démarrage.

**"Token GitHub non configuré"** → Éditer `config/mtrwebmap-server.toml`

**"HTTP 401"** → Token expiré ou mauvais scope. Regénérer sur GitHub.

**"Aucune station"** → Vérifier que tu as des stations MTR créées dans le monde
et que la dimension dans la config est correcte (`minecraft:overworld`).
