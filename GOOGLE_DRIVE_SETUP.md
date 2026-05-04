# Guide d'installation — Eden Family School (Google Drive Upload)

## ETAPE 1 — Installer et demarrer le serveur Node.js

Placez server.js et package.json dans un dossier (ex: eden-server/).
Ouvrez un terminal dans ce dossier et executez :

```bash
# 1. Installer les dependances
npm install

# 2. Demarrer le serveur
node server.js
```

Vous devez voir :

```text
✅ Backend Node.js demarre sur le port 3000
🔑 Cle Drive: trouvee
```

Remarque : si vous voyez EADDRINUSE sur le port 3000, un autre serveur tourne deja sur ce port.

## ETAPE 2 — Ajouter votre cle Google Drive

1. Allez sur https://console.cloud.google.com
2. Creez un projet puis activez Google Drive API
3. Creez un Compte de service puis telechargez la cle JSON
4. Renommez le fichier en drive-key.json
5. Placez drive-key.json dans le meme dossier que server.js
6. Utilisez des dossiers situes dans un Shared Drive (pas My Drive).
7. Ajoutez le compte de service comme membre du Shared Drive (role Contributeur/Content manager).
8. Mettez les IDs de ces dossiers Shared Drive dans server.js (DRIVE_FOLDERS).

## ETAPE 3 — Modifier secretary.html

### 3a. Remplacer les onglets academiques

Cherchez dans secretary.html :

```html
<div class="academic-tabs" id="academicTabs">
```

Remplacez tout le bloc (jusqu'au </div> fermant les onglets) par PATCH 1 de patch_secretary.html.

### 3b. Supprimer l'ancien onglet Documents Drive

Cherchez et supprimez tout le bloc :

```html
<div id="academic-documents" class="academic-content" style="display: none;">
  ...
</div>
```

### 3c. Ajouter les nouveaux onglets Photos et Presentations

Juste avant la balise fermante de la card-body de la section academique,
collez PATCH 2 de patch_secretary.html
(les deux blocs academic-photos et academic-presentations).

### 3d. Ajouter les fonctions JavaScript corrigees

Juste avant </body> (ou a la fin du script principal),
collez PATCH 3 (le bloc script de patch_secretary.html).

Attention : PATCH 3 redefinit switchAcademic, loadDocuments,
saveDocumentToFirebase et deleteDocument. Supprimez les anciennes definitions
pour eviter les doublons.

## ETAPE 4 — Verification

1. Demarrez le serveur : node server.js
2. Ouvrez secretary.html dans le navigateur
3. Allez dans Academique puis onglet Photos
4. Deposez une image : elle doit apparaitre dans la galerie
5. Allez dans Presentations puis deposez une video

## Structure des dossiers Drive configures

| Type | Dossier Drive ID |
|---|---|
| Devoirs | 19Z-HL0ttxFCoIJeSwp1MxYuzzGe5Ipd- |
| Tests | 1k_jBwSr0nASiQ27ow6Tib7Ukug24xIO0 |
| Examens | 1HLflt6qTu_UDtbTLjQU6UNHNS0MEHbA0 |
| Travaux/Vac. | 1pybPOwSy7-Gp4Kx-sWfm5x5dBKR_ekIV |
| Photos | 1T17ES75mQyAWrzCB8KZuZQDAPU3hKSCw |
| Presentations | 1upIgjidP2I9yA2N4BqxB26JkUqIu60O2 |

## Erreurs courantes

| Erreur | Solution |
|---|---|
| ERR_CONNECTION_REFUSED | Le serveur n'est pas demarre : node server.js |
| drive-key.json introuvable | Placez le fichier dans le dossier du serveur |
| Erreur serveur 500 | Verifiez que le compte de service a acces aux dossiers Drive |
| db is not defined | Remplacez db.ref(...) par database.ref(...) (PATCH 3) |
| EADDRINUSE:3000 | Un autre process utilise le port 3000. Arreter ce process ou changer PORT |
