# Guide de Test - Formulaire du Rapport

## Test 1: Remplissage du Formulaire

### Étapes:
1. Aller à la section **Rapports Financiers**
2. Sélectionner **Journalier**
3. Observer que les champs sont préremplis:
   - `Préparé par`: Affiche le nom de l'utilisateur
   - `Date du rapport`: Date actuelle
   - `Signature numérique`: "Username - COMPTABLE PRINCIPAL"

### Résultat attendu:
✅ Tous les champs sont accessibles et préremplis correctement

---

## Test 2: Saisie de Données

### Étapes:
1. Dans la section "Observations et Détails"
2. Remplir les champs:
   - Observations générales: "Journée normale"
   - Élèves présents: "150"
   - Travailleurs présents: "25"
   - Incidents: Sélectionner "Absence notable"
   - Commentaire incident: "Détails de l'absence"
   - Recommandations: "Renforcer le suivi"

### Résultat attendu:
✅ Les données sont acceptées et affichées

---

## Test 3: Enregistrement Firebase

### Étapes:
1. Cliquer le bouton **"Enregistrer Rapport"**
2. Attendre le message de succès
3. Vérifier dans Firebase Console:
   - Navigation: `Realtime Database > reports > dailyReports`
   - Vérifier la présence d'une nouvelle entrée

### Résultat attendu:
✅ Message "Rapport journalier enregistré avec succès!"
✅ Les données apparaissent dans Firebase
✅ La branche est sauvegardée

### Contenu Firebase attendu:
```json
{
  "date": "2025-01-18",
  "branch": "kacyiru",
  "preparedBy": "YourUsername",
  "reportDate": "2025-01-18",
  "generalObservations": "Journée normale",
  "studentsPresent": 150,
  "workersPresent": 25,
  "incidentType": "absence",
  "incidentComment": "Détails de l'absence",
  "recommendations": "Renforcer le suivi",
  "totalIncome": [calculated],
  "totalExpenses": [calculated],
  "closingBalance": [calculated],
  "generatedBy": "YourUsername",
  "type": "daily",
  "timestamp": [timestamp]
}
```

---

## Test 4: Export Excel

### Étapes:
1. Cliquer **"Exporter Excel"**
2. Ouvrir le fichier téléchargé `Rapport_Journalier_YYYY-MM-DD.xlsx`
3. Vérifier le contenu:

### Sections à vérifier:
- ✅ En-tête: Date, Branche, Utilisateur
- ✅ Sommaire financier: Soldes et totaux
- ✅ Détail des paiements: Minerval, Transport, etc.
- ✅ Section observations: Tous les champs remplis
- ✅ Formatage: Montants en RWF

### Résultat attendu:
✅ Fichier Excel complètement rempli et formaté
✅ Tous les calculs automatiques présents
✅ Mise en page professionnelle

---

## Test 5: Export PDF

### Étapes:
1. Cliquer **"Exporter PDF"**
2. Ouvrir le fichier `Rapport_Journalier_YYYY-MM-DD.pdf`
3. Vérifier le contenu et la mise en page

### Éléments à vérifier:
- ✅ Titre centré et mise en forme
- ✅ En-têtes et informations d'identification
- ✅ Tous les montants en RWF
- ✅ Sections bien organisées
- ✅ Données complètes

### Résultat attendu:
✅ PDF bien formaté et lisible
✅ Toutes les informations présentes
✅ Prêt pour archivage/impression

---

## Test 6: Persistance avec localStorage

### Étapes:
1. Remplir et enregistrer un rapport
2. Rafraîchir la page (F5)
3. Vérifier dans les DevTools (F12):
   - Tab "Application" > "Local Storage"
   - Chercher la clé: `dailyReport_2025-01-18`

### Résultat attendu:
✅ Les données sont toujours disponibles après rafraîchissement
✅ Clé localStorage présente avec les données JSON
✅ Aucune perte de données

---

## Test 7: Changement de Branche

### Étapes:
1. Sélectionner une branche dans "Configuration des Frais"
2. Exemple: "Eden Family School Gisozi Maternelle"
3. Enregistrer un rapport
4. Vérifier dans Firebase que la branche est incluse

### Résultat attendu:
✅ La branche "gisozi_maternelle" est sauvegardée avec le rapport
✅ localStorage contient `selectedBranch: "gisozi_maternelle"`
✅ Toutes les données sont associées à la branche

---

## Test 8: Historique des Rapports

### Étapes:
1. Enregistrer plusieurs rapports sur différentes dates
2. Vérifier dans Firebase Console:
   - Aller à `reportHistory`
3. Observer les entrées

### Résultat attendu:
✅ Chaque rapport est enregistré dans l'historique
✅ Les dates et utilisateurs sont corrects
✅ Les données complètes sont présentes

---

## Test 9: Gestion des Dépenses

### Étapes:
1. Ajouter des dépenses via "Ajouter une Dépense"
2. Exemples:
   - Électricité: 50000 RWF
   - Nourriture: 30000 RWF
3. Enregistrer le rapport
4. Exporter en Excel/PDF

### Résultat attendu:
✅ Les dépenses apparaissent dans le rapport
✅ Les totaux sont corrects
✅ Les exports incluent les dépenses
✅ Firebase contient l'array `dailyExpenses`

---

## Test 10: Validation des Champs

### Étapes:
1. Essayer de cliquer "Enregistrer" sans date
2. Essayer sans avoir chargé les dépenses

### Résultat attendu:
✅ Message d'erreur approprié
✅ Les champs requis sont identifiés
✅ Les données ne sont pas sauvegardées

---

## Checklist Complète

- [ ] Formulaire rempli correctement
- [ ] Firebase enregistre les données
- [ ] Excel export fonctionne et contient tous les champs
- [ ] PDF export est bien formaté
- [ ] localStorage persiste les données
- [ ] Branche est sauvegardée avec le rapport
- [ ] Historique capture tous les rapports
- [ ] Dépenses sont intégrées
- [ ] Validation empêche les données incomplètes
- [ ] Signature numérique est correcte

---

## Troubleshooting

### Problème: Rapport non sauvegardé
**Solution:** 
- Vérifier la connexion Firebase
- Vérifier les permissions Firebase Realtime Database
- Vérifier la console pour les erreurs

### Problème: Champs vides après rafraîchissement
**Solution:**
- Vérifier localStorage dans DevTools
- S'assurer que localStorage est activé
- Vérifier les permissions du navigateur

### Problème: Export Excel/PDF vide
**Solution:**
- S'assurer que des données sont chargées
- Vérifier que XLSX et jsPDF sont chargés
- Consulter la console pour les erreurs

---

## Contacts pour Support

Pour toute question sur l'intégration Firebase ou les exports:
- Consulter FIREBASE_DATA_STRUCTURE.md
- Vérifier les logs Firebase Console
- Examiner la console navigateur (F12)
