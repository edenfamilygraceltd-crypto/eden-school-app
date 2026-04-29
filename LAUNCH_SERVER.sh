#!/bin/bash
# 🚀 Quick Start — Google Drive Upload System

echo "════════════════════════════════════════════════════════════════"
echo "  📚 Eden Family School — Google Drive Upload Setup"
echo "════════════════════════════════════════════════════════════════"
echo ""

# Vérifier Node.js
echo "✓ Vérification de Node.js..."
if ! command -v node &> /dev/null; then
    echo "❌ Node.js non installé. Télécharge-le : https://nodejs.org/"
    exit 1
fi
echo "✅ Node.js v$(node -v)"

# Vérifier npm
echo "✓ Vérification de npm..."
if ! command -v npm &> /dev/null; then
    echo "❌ npm non installé"
    exit 1
fi
echo "✅ npm v$(npm -v)"

# Vérifier drive-key.json
echo "✓ Vérification de drive-key.json..."
if [ ! -f "drive-key.json" ]; then
    echo "❌ drive-key.json manquant !"
    echo "   Suis le guide : GOOGLE_DRIVE_SETUP.md → Étape 1-3"
    exit 1
fi
echo "✅ drive-key.json trouvé"

# Créer le dossier uploads/
echo "✓ Création du dossier uploads/..."
mkdir -p uploads
echo "✅ uploads/ prêt"

# Installer les dépendances
echo "✓ Installation des dépendances..."
npm install --quiet
echo "✅ Dépendances installées"

# Afficher les instructions
echo ""
echo "════════════════════════════════════════════════════════════════"
echo "  🎯 Configuration Google Drive"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "⚠️  AVANT de lancer le serveur, vérifie :"
echo ""
echo "1. DOSSIERS GOOGLE DRIVE créés et IDs copiés"
echo "2. DRIVE_FOLDERS dans server.js mis à jour"
echo "3. PERMISSIONS : chaque dossier partagé avec la Service Account"
echo ""
echo "Pour plus de détails : GOOGLE_DRIVE_SETUP.md"
echo ""

# Option pour continuer ou non
read -p "✓ Tous les prérequis sont configurés ? (o/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Oo]$ ]]; then
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "  🚀 Démarrage du serveur..."
    echo "════════════════════════════════════════════════════════════════"
    echo ""
    npm start
else
    echo ""
    echo "❌ Annulé. Configure d'abord les prérequis."
    exit 1
fi
