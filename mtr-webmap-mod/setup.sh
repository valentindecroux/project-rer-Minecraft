#!/bin/bash
# ─────────────────────────────────────────────────────
#  MTR Web Map — Script de setup (Mac/Linux)
#  Lance ce script UNE SEULE FOIS pour tout préparer.
# ─────────────────────────────────────────────────────

set -e
cd "$(dirname "$0")"

echo ""
echo "════════════════════════════════════════"
echo "  MTR Web Map — Setup"
echo "════════════════════════════════════════"
echo ""

# 1. Vérifier Java 17+
echo "▶ Vérification Java..."
if ! command -v java &>/dev/null; then
  echo "❌ Java introuvable ! Installe Java 17 depuis :"
  echo "   https://adoptium.net/temurin/releases/?version=17"
  exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*version "\([0-9]*\).*/\1/')
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
  echo "❌ Java $JAVA_VER détectë, Java 17 requis."
  echo "   Installe Java 17 depuis https://adoptium.net/temurin/releases/?version=17"
  exit 1
fi
echo "   ✅ Java $JAVA_VER OK"

# 2. Télécharger gradle-wrapper.jar si absent
echo ""
echo "▶ Vérification Gradle wrapper..."
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "   Téléchargement de gradle-wrapper.jar..."
  mkdir -p gradle/wrapper
  # Télécharge depuis le repo officiel Gradle
  if command -v curl &>/dev/null; then
    curl -fsSL \
      "https://raw.githubusercontent.com/gradle/gradle/v8.1.0/gradle/wrapper/gradle-wrapper.jar" \
      -o "$WRAPPER_JAR"
  elif command -v python3 &>/dev/null; then
    python3 -c "
import urllib.request
url = 'https://raw.githubusercontent.com/gradle/gradle/v8.1.0/gradle/wrapper/gradle-wrapper.jar'
urllib.request.urlretrieve(url, '$WRAPPER_JAR')
print('Téléchargé !')
"
  else
    echo "❌ curl et python3 introuvables. Installe curl avec Homebrew :"
    echo "   brew install curl"
    exit 1
  fi
  echo "   ✅ gradle-wrapper.jar téléchargé"
else
  echo "   ✅ gradle-wrapper.jar déjà présent"
fi

# 3. Rendre gradlew exécutable
chmod +x gradlew
echo "   ✅ gradlew prêt"

# 4. Compiler
echo ""
echo "▶ Compilation du mod (peut prendre 5-10 min la première fois)..."
echo "   (Forge doit être téléchargé — besoin d'internet)"
echo ""
./gradlew build --no-daemon

echo ""
echo "════════════════════════════════════════"
echo "  ✅ MOD COMPILÉ !"
echo "════════════════════════════════════════"
echo ""
echo "Le fichier JAR se trouve ici :"
find build/libs -name "*.jar" | grep -v sources | grep -v javadoc
echo ""
echo "➡️  Copie ce fichier dans le dossier mods/ de ton serveur Exaroton."
echo ""
