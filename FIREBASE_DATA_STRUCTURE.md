# Structure des Données Firebase - Eden Family School

## Collections Principales

### 1. Teachers (Enseignants)
**Chemin:** `teachers/{uid}`
```json
{
  "id": "uid",
  "name": "Nom complet",
  "email": "email@example.com",
  "phone": "+250...",
  "role": "teacher",
  "classe": "1ère A Maternelle",
  "section": "maternelle",
  "createdAt": "timestamp",
  "status": "active"
}
```

### 2. Secretaries (Secrétaires/Comptables)
**Chemin:** `secretaries/{uid}`
```json
{
  "id": "uid",
  "name": "Nom complet",
  "email": "email@example.com",
  "phone": "+250...",
  "role": "secretary" | "accountant",
  "department": "administration" | "finance",
  "createdAt": "timestamp",
  "status": "active"
}
```

### 3. Directors (Directeurs)
**Chemin:** `directors/{uid}`
```json
{
  "id": "uid",
  "name": "Nom complet",
  "email": "email@example.com",
  "role": "director",
  "createdAt": "timestamp"
}
```

### 4. Students (Élèves)
**Chemin:** `students/{uid}`
```json
{
  "nom": "Nom de famille",
  "prenom": "Prénom",
  "code": "EDF-KA-1A-1234",
  "classe": "1ère A Maternelle",
  "section": "maternelle" | "primaire",
  "schoolCode": "EDF-KA",
  "createdAt": "timestamp",
  "bulletinCompleted": false
}
```

### 5. Bulletins
**Chemin:** `bulletins/{bulletinId}`
```json
{
  "student": {
    "id": "studentId",
    "code": "EDF-KA-1A-1234",
    "name": "Nom Prénom",
    "classe": "1ère A Maternelle"
  },
  "type": "conduite" | "academique",
  "trimestre": 1 | 2 | 3,
  "dateCreation": "ISO string",
  "dateProclamation": "YYYY-MM-DD",
  "teacher": {
    "id": "teacherId",
    "name": "Nom Enseignant"
  },
  "anneeScolaire": "2025-2026",
  "statut": "complet",
  "conduite": {
    "Critère 1": "E" | "TB" | "B"
  },
  "academique": {
    "Matière": {
      "travaux": 50,
      "travaux_max": 60,
      "examen": 30,
      "examen_max": 40,
      "total": 80,
      "total_max": 100
    }
  },
  "commentaire": "Commentaire de l'enseignant"
}
```

### 6. Payments (Paiements)
**Chemin:** `payments/{paymentId}`
```json
{
  "id": "paymentId",
  "studentName": "Nom de l'élève",
  "type": "minerval" | "transport" | "coaching" | "autres",
  "amount": 50000,
  "status": "completed" | "pending" | "partial",
  "date": "YYYY-MM-DD",
  "method": "cash" | "mobile" | "bank" | "cheque",
  "reference": "Référence paiement",
  "class": "1ère A Maternelle",
  "notes": "Notes additionnelles",
  "createdBy": "uid",
  "createdByEmail": "email@example.com",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### 7. Config (Configuration)
**Chemin:** `config/`
```json
{
  "matieres": {
    "maternelle": [
      "Pre-mathématique",
      "Français",
      "Étude du milieu",
      "Français langage",
      "Art & Culture",
      "Activité Physique et Motrice"
    ],
    "primaire": [...]
  },
  "criteresConduite": [
    "Respect des enseignants et des camarades",
    "Politesse et bonnes manières",
    "Obéissance aux consignes",
    "Discipline en classe",
    "Ponctualité et assiduité",
    "Propreté et hygiène personnelle",
    "Participation aux activités",
    "Esprit de partage et de collaboration"
  ],
  "classes": {
    "maternelle": [
      { "id": "1A", "name": "1ère A Maternelle", "niveau": "1A" },
      { "id": "1B", "name": "1ère B Maternelle", "niveau": "1B" },
      { "id": "2", "name": "2ème Maternelle", "niveau": "2" },
      { "id": "3", "name": "3ème Maternelle", "niveau": "3" }
    ],
    "primaire": [
      { "id": "P1", "name": "Primaire - P1", "niveau": "P1" },
      ...
    ]
  },
  "anneeScolaire": "2025-2026"
}
```

## Notes Importantes

1. **Tous les emails sont stockés en minuscules** pour faciliter les recherches
2. **Les timestamps utilisent** `firebase.database.ServerValue.TIMESTAMP`
3. **Les recherches par email** doivent utiliser `.toLowerCase()` pour correspondre
4. **Les classes** sont stockées avec leur nom complet (ex: "1ère A Maternelle")
5. **Les codes élèves** suivent le format: `EDF-KA-{CLASSE}-{4 chiffres}`

