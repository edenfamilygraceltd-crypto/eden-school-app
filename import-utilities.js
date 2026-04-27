/**
 * Import Utilities - Shared functions for importing students from Excel/Word/CSV
 * Version: 3.2 - RÈGLE DÉFINITIVE : les chiffres ne sont JAMAIS retirés d'un nom
 *
 * HISTORIQUE DES CORRECTIONS :
 * ─────────────────────────────────────────────────────────────────
 * v2.0 — BUG CRITIQUE : extractNameWithSmartHandling()
 *         Si le nom contenait UN chiffre → on gardait SEULEMENT les chiffres.
 *         "Jean 2ème"  →  "2"   ❌ FAUX
 *         "AGANZE 1er" →  "1"   ❌ FAUX
 *
 * v3.1 — Correction partielle :
 *         On gardait le texte complet SAUF si purement numérique.
 *         Mais la logique d'extraction de chiffres existait encore ailleurs.
 *
 * v3.2 — RÈGLE ABSOLUE et définitive dans normalizeNameField() :
 *         ✅ Texte conservé intégralement (chiffres inclus s'ils font partie du nom)
 *         ✅ Seul le préfixe de numérotation ("1.", "12)", "•") en DÉBUT est retiré
 *         ✅ Rejet UNIQUEMENT si la valeur est entièrement numérique (numéro de ligne parasite)
 *         ✅ extractNumbersOnly() n'est plus jamais appelée sur un champ NOM
 *
 * Exemples de comportement v3.2 :
 *   "1.  AGANZE SHIMWA VIVANT"  →  "AGANZE SHIMWA VIVANT"   ✅
 *   "JEAN 2ÈME"                 →  "JEAN 2ÈME"              ✅
 *   "42"                        →  ""  (numéro de ligne)     ✅
 *   "AGANZE SHIMWA VIVANT"      →  "AGANZE SHIMWA VIVANT"   ✅
 *
 * Autres corrections actives depuis v3.1 :
 *   - "CLASSE" ajouté dans COLUMN_MAP.class (était absent)
 *   - "NOM COMPLET" avec accents reconnu via normalizeColumnName()
 *   - Colonnes "__EMPTY_x" Excel filtrées
 *   - KEEP_UPPERCASE_NAMES=true → noms conservés en MAJUSCULES
 * ─────────────────────────────────────────────────────────────────
 */

'use strict';

// ═══════════════════════════════════════════════════════════════
// CONFIGURATION
// ═══════════════════════════════════════════════════════════════

/**
 * Si true  → noms conservés en MAJUSCULES  (ex: AGANZE SHIMWA VIVANT)
 * Si false → noms convertis en Title Case  (ex: Aganze Shimwa Vivant)
 */
const KEEP_UPPERCASE_NAMES = true;

// ═══════════════════════════════════════════════════════════════
// 1. UTILITAIRES CHAÎNES
// ═══════════════════════════════════════════════════════════════

/**
 * Normalise un nom de colonne : MAJUSCULES, sans accents, espaces collapsés.
 * Ex: "Nom Complet" → "NOM COMPLET"
 *     "ÉLÈVE"       → "ELEVE"
 *     "Téléphone"   → "TELEPHONE"
 */
const normalizeColumnName = function(name) {
    if (name === null || name === undefined) return '';
    return name.toString().trim().toUpperCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')  // supprime les diacritiques
        .replace(/\s+/g, ' ');
};

/**
 * Retire les préfixes de numérotation/puces en DÉBUT de chaîne.
 * Exemples :
 *   "1.       AGANZE SHIMWA VIVANT" → "AGANZE SHIMWA VIVANT"
 *   "12)  JEAN PIERRE"              → "JEAN PIERRE"
 *   "• Marie Dupont"                → "Marie Dupont"
 *   "- KARIM HASSAN"                → "KARIM HASSAN"
 *   "N°5 ALICE"                     → "ALICE"
 *   "AGANZE SHIMWA"                 → "AGANZE SHIMWA"  (inchangé)
 */
const cleanNamePrefix = function(str) {
    if (!str) return '';
    return str
        .replace(/^(n[o°]?\s*\d+[\.\):\-]?\s*|\d+[\.\):\-]\s*)/i, '')
        .replace(/^[•–\-*·]\s*/, '')
        .replace(/\s+/g, ' ')
        .trim();
};

/** Extrait uniquement les chiffres. Garde "+" pour numéros internationaux. */
const extractNumbersOnly = function(value) {
    if (value === null || value === undefined) return '';
    const str = String(value).trim();
    const hasPlus = str.startsWith('+');
    const digits = str.replace(/\D/g, '');
    return hasPlus ? '+' + digits : digits;
};

/**
 * Normalise un nom d'élève ou parent.
 *
 * RÈGLE ABSOLUE POUR LES NOMS :
 *   → On garde TOUJOURS le texte complet, chiffres inclus s'ils font partie du nom.
 *   → On ne retire JAMAIS les chiffres d'un nom (ex: "Jean 2ème", "A1-Marie" restent intacts).
 *   → Seule exception : une valeur ENTIÈREMENT numérique (ex: "1", "42") est considérée
 *     comme un numéro de ligne parasite et rejetée.
 *   → Le préfixe de numérotation en DÉBUT de chaîne est retiré via cleanNamePrefix()
 *     (ex: "1. AGANZE SHIMWA" → "AGANZE SHIMWA"), mais le reste n'est pas touché.
 *
 * Exemples :
 *   "1.  AGANZE SHIMWA VIVANT"  →  "AGANZE SHIMWA VIVANT"   ✅ préfixe retiré
 *   "JEAN 2ÈME"                 →  "JEAN 2ÈME"              ✅ texte+chiffre conservé
 *   "A1-MARIE CLAIRE"           →  "A1-MARIE CLAIRE"        ✅ conservé
 *   "42"                        →  ""                        ✅ numéro de ligne rejeté
 *   "AGANZE SHIMWA VIVANT"      →  "AGANZE SHIMWA VIVANT"   ✅ inchangé
 */
const normalizeNameField = function(value) {
    if (value === null || value === undefined) return '';
    let str = String(value).trim();
    if (str.length === 0) return '';

    // Étape 1 : retirer uniquement le préfixe de numérotation/puce EN DÉBUT
    // ex: "1. NOM", "12) NOM", "• NOM" → "NOM"
    // Le reste du contenu (chiffres inclus) n'est PAS modifié.
    str = cleanNamePrefix(str);
    if (str.length === 0) return '';

    // Étape 2 : rejeter uniquement si la valeur est ENTIÈREMENT numérique
    // (numéro de ligne parasite qui a atterri dans la colonne nom)
    if (/^\d+$/.test(str)) return '';

    // Étape 3 : collaps des espaces multiples, puis formatage
    str = str.replace(/\s+/g, ' ').trim();

    // Étape 4 : formatage final — MAJUSCULES ou Title Case
    if (KEEP_UPPERCASE_NAMES) {
        return str.toUpperCase();
    } else {
        return str.replace(/\w\S*/g, w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase());
    }
}

/**
 * Normalise une classe : MAJUSCULES, espaces collapsés.
 * Ex: "  n2a  " → "N2A",  "grade 3" → "GRADE 3"
 */
const normalizeClassField = function(value) {
    if (value === null || value === undefined) return '';
    return String(value).trim().toUpperCase().replace(/\s+/g, ' ');
};

/**
 * Normalise un numéro de téléphone.
 * Garde "+", supprime formatage. Retourne vide si < 7 chiffres.
 */
const normalizePhoneNumber = function(value) {
    if (value === null || value === undefined) return '';
    const str = String(value).trim();
    const cleaned = extractNumbersOnly(str);
    const digitsOnly = cleaned.replace('+', '');
    if (digitsOnly.length < 7) return '';
    return cleaned;
};

// ═══════════════════════════════════════════════════════════════
// 2. PARSING DES DATES
// ═══════════════════════════════════════════════════════════════

const excelSerialToDate = function(serial) {
    const epoch = new Date(1899, 11, 30);
    const date = new Date(epoch.getTime() + serial * 86400000);
    if (isNaN(date.getTime())) return '';
    return `${String(date.getDate()).padStart(2,'0')}/${String(date.getMonth()+1).padStart(2,'0')}/${date.getFullYear()}`;
}

/**
 * Parse une date de naissance → DD/MM/YYYY.
 * Formats supportés : sériel Excel, Date JS, ISO, DD/MM/YYYY, texte français.
 */
const parseBirthDate = function(value) {
    if (value === null || value === undefined) return '';

    if (value instanceof Date) {
        if (isNaN(value.getTime())) return '';
        return `${String(value.getDate()).padStart(2,'0')}/${String(value.getMonth()+1).padStart(2,'0')}/${value.getFullYear()}`;
    }

    if (typeof value === 'number' && Number.isInteger(value) && value > 100 && value < 100000) {
        return excelSerialToDate(value);
    }

    const str = String(value).trim();
    if (!str) return '';

    // ISO : YYYY-MM-DD
    const isoMatch = str.match(/^(\d{4})[-\/](\d{1,2})[-\/](\d{1,2})$/);
    if (isoMatch) {
        return `${isoMatch[3].padStart(2,'0')}/${isoMatch[2].padStart(2,'0')}/${isoMatch[1]}`;
    }

    // DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY
    const dmyMatch = str.match(/^(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})$/);
    if (dmyMatch) {
        let yyyy = dmyMatch[3];
        if (yyyy.length === 2) yyyy = parseInt(yyyy) > 30 ? '19' + yyyy : '20' + yyyy;
        return `${dmyMatch[1].padStart(2,'0')}/${dmyMatch[2].padStart(2,'0')}/${yyyy}`;
    }

    // Mois français : "15 janvier 2005"
    const frMonths = {
        janvier:1, fevrier:2, mars:3, avril:4, mai:5, juin:6,
        juillet:7, aout:8, septembre:9, octobre:10, novembre:11, decembre:12
    };
    const txtMatch = str.toLowerCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g,'')
        .match(/(\d{1,2})\s+([a-z]+)\s+(\d{4})/);
    if (txtMatch && frMonths[txtMatch[2]]) {
        return `${txtMatch[1].padStart(2,'0')}/${String(frMonths[txtMatch[2]]).padStart(2,'0')}/${txtMatch[3]}`;
    }

    // Fallback : extraire groupes de chiffres
    const parts = str.match(/\d+/g);
    if (parts && parts.length >= 3) {
        const [a, b, c] = parts;
        if (a.length === 4) return `${b.padStart(2,'0')}/${c.padStart(2,'0')}/${a}`;
        const yyyy = c.length === 2 ? (parseInt(c) > 30 ? '19'+c : '20'+c) : c;
        return `${a.padStart(2,'0')}/${b.padStart(2,'0')}/${yyyy}`;
    }

    return '';
}

// ═══════════════════════════════════════════════════════════════
// 3. VALIDATION
// ═══════════════════════════════════════════════════════════════

const isValidData = function(value) {
    if (value === null || value === undefined) return false;
    return /[a-zA-ZÀ-ÿ\d]/.test(String(value));
}

const validateStudent = function(student) {
    const errors = [];
    if (!student.fullName || student.fullName.trim().length < 2) {
        errors.push('Nom complet manquant ou trop court');
    }
    if (!student.classes || student.classes.trim().length === 0) {
        errors.push('Classe manquante');
    }
    return { valid: errors.length === 0, errors };
}

const studentDedupKey = function(student) {
    return `${(student.fullName||'').toUpperCase().replace(/\s+/g,'')}__${(student.classes||'').toUpperCase().replace(/\s+/g,'')}`;
}

// ═══════════════════════════════════════════════════════════════
// 4. MAPPING DES COLONNES
// ═══════════════════════════════════════════════════════════════

/**
 * Alias de colonnes acceptés par champ (MAJUSCULES, sans accents).
 * IMPORTANT : toutes les valeurs sont déjà normalisées (normalizeColumnName).
 *
 * FIX v3.1 :
 *   - "CLASSE" ajouté dans class  (était absent !)
 *   - "NOM COMPLET" confirmé dans fullName
 */
const COLUMN_MAP = {
    fullName: [
        'NOM COMPLET', 'NOMCOMPLET', 'NOM_COMPLET',
        'FULL NAME', 'FULLNAME', 'NAME',
        'STUDENT NAME', 'STUDENTNAME',
        'PRENOM NOM', 'PRENOMETNOM', 'NOM ET PRENOM',
        'ELEVE', 'NOM DE L ELEVE', 'STUDENT', 'NOM'
    ],
    parent: [
        'PARENT', 'PARENT NAME', 'PARENT FULL NAME',
        'PERE/MERE', 'PEREMERE',
        'PARENT COMPLET', 'TUTEUR', 'GUARDIAN',
        'NOM DU PARENT', 'NOM PARENT', 'RESPONSABLE'
    ],
    birthDate: [
        'BIRTH DATE', 'DATE OF BIRTH', 'DOB',
        'DATE NAISSANCE', 'DATE DE NAISSANCE',
        'NAISSANCE', 'DATE BIRTH', 'DATE NE', 'DATE NEE', 'DDN'
    ],
    phone: [
        'PARENT PHONE', 'TELEPHONE', 'PHONE',
        'TELEPHONE PARENT', 'CONTACT',
        'PHONE PARENT', 'TEL', 'TEL PARENT',
        'NUMERO', 'NUMERO DE TELEPHONE', 'MOBILE'
    ],
    class: [
        'CLASSE',           // ← FIX: était absent !
        'CLASS',
        'GRADE', 'LEVEL', 'SECTION',
        'CLASSROOM', 'EDUCATION LEVEL',
        'NIVEAU', 'NIVEAU SCOLAIRE',
        'ANNEE', 'PROMOTION'
    ]
};

const findFieldInRow = function(row, field) {
    for (const alias of (COLUMN_MAP[field] || [])) {
        const val = row[alias];
        if (val !== undefined && val !== null && String(val).trim() !== '') {
            return String(val).trim();
        }
    }
    return '';
}

/**
 * Diagnostic: affiche les colonnes reconnues d'une ligne Excel
 */
const diagnoseDiagnosticsRow = function(row, rowNum) {
    const result = {
        rowNum,
        allColumns: Object.keys(row),
        mappedFields: {}
    };
    
    for (const field of ['fullName', 'class', 'parent', 'birthDate', 'phone']) {
        const val = findFieldInRow(row, field);
        result.mappedFields[field] = {
            value: val,
            recognized: val !== ''
        };
    }
    
    return result;
}

const extractStudentDataFromRow = function(row) {
    return {
        fullName:  normalizeNameField(findFieldInRow(row, 'fullName')),
        classes:   normalizeClassField(findFieldInRow(row, 'class')),
        parent:    normalizeNameField(findFieldInRow(row, 'parent')),
        birthDate: parseBirthDate(findFieldInRow(row, 'birthDate')),
        phone:     normalizePhoneNumber(findFieldInRow(row, 'phone'))
    };
}

// ═══════════════════════════════════════════════════════════════
// 5. TRAITEMENT EXCEL
// ═══════════════════════════════════════════════════════════════

/**
 * Lit tous les sheets Excel, normalise les colonnes.
 * FIX : ignore les colonnes "__EMPTY_x" parasites.
 */
const readAllExcelSheets = function(workbook) {
    let allRows = [];
    const sheetSummary = [];

    for (const sheetName of workbook.SheetNames) {
        const worksheet = workbook.Sheets[sheetName];
        const sheetData = XLSX.utils.sheet_to_json(worksheet, { raw: true, defval: '' });

        const normalizedRows = sheetData.map(row => {
            const newRow = {};
            for (const [key, value] of Object.entries(row)) {
                if (key.startsWith('__EMPTY')) continue; // ignorer colonnes sans titre
                const normKey = normalizeColumnName(key);
                if (normKey.length > 0) newRow[normKey] = value;
            }
            return newRow;
        });

        // Ignorer les rows entièrement vides
        const nonEmptyRows = normalizedRows.filter(row =>
            Object.values(row).some(v => isValidData(v))
        );

        sheetSummary.push({ sheet: sheetName, rowCount: nonEmptyRows.length });
        allRows = allRows.concat(nonEmptyRows);
    }

    return { rows: allRows, sheetSummary };
}

const processExcelFile = async function(file) {
    let workbook;
    try {
        const arrayBuffer = await file.arrayBuffer();
        workbook = XLSX.read(arrayBuffer, { type: 'array', cellDates: false });
    } catch (err) {
        return {
            success: false,
            message: `Impossible de lire le fichier Excel : ${err.message}`,
            students: [], invalidRows: [], totalRows: 0, sheetSummary: []
        };
    }

    const { rows: allJsonData, sheetSummary } = readAllExcelSheets(workbook);

    if (allJsonData.length === 0) {
        return {
            success: false,
            message: 'Aucune donnée trouvée dans le fichier Excel',
            students: [], invalidRows: [], totalRows: 0, sheetSummary
        };
    }

    const students    = [];
    const invalidRows = [];
    const seen        = new Set();

    allJsonData.forEach((row, index) => {
        const rowNum = index + 2;
        
        // Diagnostic pour la 1ère ligne (header)
        if (index === 0) {
            const diag = diagnoseDiagnosticsRow(row, rowNum);
            console.log('🔍 COLONNES RECONNUES (Ligne 1):', diag);
            console.log('   Toutes les colonnes du fichier:', Object.keys(row));
        }
        
        const data = extractStudentDataFromRow(row);
        const { valid, errors } = validateStudent(data);

        if (!valid) {
            const rawPreview = Object.values(row).filter(Boolean).slice(0,4).join(' | ');
            invalidRows.push({ row: rowNum, rawPreview, data, errors });
            return;
        }

        const key = studentDedupKey(data);
        if (seen.has(key)) {
            invalidRows.push({ row: rowNum, data, errors: ['Doublon (même nom + classe)'] });
            return;
        }
        seen.add(key);
        students.push({ ...data, rowNumber: rowNum, source: 'excel' });
    });

    const skipped = invalidRows.length;
    return {
        success: students.length > 0,
        message: students.length > 0
            ? `${students.length} élève(s) importé(s)${skipped > 0 ? ` (${skipped} ligne(s) ignorée(s))` : ''}`
            : `Aucun élève valide. ${skipped} ligne(s) ignorée(s). Vérifiez que les colonnes "NOM COMPLET" et "CLASSE" existent.`,
        students,
        invalidRows,
        totalRows: allJsonData.length,
        sheetSummary
    };
}

// ═══════════════════════════════════════════════════════════════
// 6. TRAITEMENT CSV
// ═══════════════════════════════════════════════════════════════

const parseCSV = function(text) {
    const lines = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
    if (lines.length < 2) return [];

    const header = lines[0];
    const delimiter = (header.split(';').length > header.split(',').length) ? ';' : ',';

    function parseLine(line) {
        const fields = [];
        let current = '', inQuotes = false;
        for (let i = 0; i < line.length; i++) {
            const ch = line[i];
            if (ch === '"') {
                if (inQuotes && line[i+1] === '"') { current += '"'; i++; }
                else { inQuotes = !inQuotes; }
            } else if (ch === delimiter && !inQuotes) {
                fields.push(current.trim()); current = '';
            } else {
                current += ch;
            }
        }
        fields.push(current.trim());
        return fields;
    }

    const headers = parseLine(lines[0]).map(normalizeColumnName);
    const rows = [];
    for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;
        const values = parseLine(line);
        const row = {};
        headers.forEach((h, idx) => { if (h) row[h] = values[idx] !== undefined ? values[idx] : ''; });
        rows.push(row);
    }
    return rows;
}

const processCSVFile = async function(file) {
    let text;
    try { text = await file.text(); }
    catch (err) {
        return { success: false, message: `Erreur CSV : ${err.message}`, students: [], invalidRows: [], totalRows: 0 };
    }

    const rows = parseCSV(text);
    if (!rows.length) {
        return { success: false, message: 'CSV vide', students: [], invalidRows: [], totalRows: 0 };
    }

    const students = [], invalidRows = [], seen = new Set();
    rows.forEach((row, index) => {
        const data = extractStudentDataFromRow(row);
        const { valid, errors } = validateStudent(data);
        if (!valid) { invalidRows.push({ row: index+2, data, errors }); return; }
        const key = studentDedupKey(data);
        if (seen.has(key)) { invalidRows.push({ row: index+2, data, errors: ['Doublon'] }); return; }
        seen.add(key);
        students.push({ ...data, rowNumber: index+2, source: 'csv' });
    });

    return {
        success: students.length > 0,
        message: `${students.length} élève(s) importé(s) depuis le CSV (${invalidRows.length} ignorée(s))`,
        students, invalidRows, totalRows: rows.length
    };
}

// ═══════════════════════════════════════════════════════════════
// 7. TRAITEMENT WORD (.docx)
// ═══════════════════════════════════════════════════════════════

const processWordFile = async function(file) {
    let html;
    try {
        const arrayBuffer = await file.arrayBuffer();
        const result = await mammoth.convertToHtml({ arrayBuffer });
        html = result.value;
    } catch (err) {
        return { success: false, message: `Erreur Word : ${err.message}`, students: [], invalidRows: [], totalRows: 0 };
    }

    const parser = new DOMParser();
    const doc    = parser.parseFromString(html, 'text/html');
    const table  = doc.querySelector('table');
    const students = [], invalidRows = [], seen = new Set();

    if (table) {
        // Mode tableau structuré
        const allRows = Array.from(table.querySelectorAll('tr'));
        if (!allRows.length) {
            return { success: false, message: 'Tableau Word vide', students: [], invalidRows: [], totalRows: 0 };
        }

        const firstCells = Array.from(allRows[0].querySelectorAll('td, th'))
            .map(td => normalizeColumnName(td.textContent));

        const isHeader = firstCells.some(cell =>
            COLUMN_MAP.fullName.includes(cell) || COLUMN_MAP.class.includes(cell) ||
            /^(NOM|NAME|CLASSE|CLASS|ELEVE|STUDENT|GRADE)/.test(cell)
        );

        let headers = isHeader ? firstCells : ['NOM COMPLET','CLASSE','PARENT','DATE DE NAISSANCE','TELEPHONE PARENT'];
        const dataRows = allRows.slice(isHeader ? 1 : 0);

        dataRows.forEach((tr, index) => {
            const cells = Array.from(tr.querySelectorAll('td, th'))
                .map(td => td.textContent.replace(/\s+/g,' ').trim());
            if (cells.every(c => !c)) return;

            const row = {};
            headers.forEach((h, i) => { row[h] = cells[i] || ''; });

            const data = extractStudentDataFromRow(row);
            const { valid, errors } = validateStudent(data);
            if (!valid) { invalidRows.push({ row: index+1, data, errors }); return; }

            const key = studentDedupKey(data);
            if (seen.has(key)) { invalidRows.push({ row: index+1, data, errors: ['Doublon'] }); return; }
            seen.add(key);
            students.push({ ...data, rowNumber: index+1, source: 'word-table' });
        });

    } else {
        // Mode liste de noms
        const elements = Array.from(doc.querySelectorAll('p, li, h3, h4'));
        let rowNum = 0;

        elements.forEach(el => {
            let rawText = cleanNamePrefix(el.textContent.replace(/\s+/g,' ').trim());
            if (!rawText || rawText.length < 2 || !isValidData(rawText)) return;

            // Séparer nom et classe si séparés par tabulation ou espaces multiples
            // Ex: "AGANZE SHIMWA VIVANT    N2A" ou "AGANZE SHIMWA VIVANT\tN2A"
            const tabSplit = rawText.split(/\t+/);
            let nameText = rawText, classText = 'UNASSIGNED';

            if (tabSplit.length >= 2) {
                nameText  = tabSplit[0].trim();
                classText = tabSplit[1].trim();
            } else {
                // Dernier mot court tout-en-majuscules/chiffres = classe probable (ex: N2A, P3B, S4)
                const words = rawText.split(/\s+/);
                const last  = words[words.length - 1];
                if (words.length > 1 && /^[A-Z0-9]{1,6}$/.test(last)) {
                    classText = last;
                    nameText  = words.slice(0, -1).join(' ');
                }
            }

            rowNum++;
            const data = {
                fullName:  normalizeNameField(nameText),
                class:     normalizeClassField(classText),
                parent: '', birthDate: '', phone: ''
            };
            if (!data.fullName) return;

            const key = studentDedupKey(data);
            if (seen.has(key)) return;
            seen.add(key);
            students.push({ ...data, rowNumber: rowNum, source: 'word-list' });
        });
    }

    return {
        success: students.length > 0,
        message: students.length > 0
            ? `${students.length} élève(s) importé(s) depuis le document Word (${invalidRows.length} ignorée(s))`
            : 'Aucun élève valide dans le document Word',
        students, invalidRows,
        totalRows: students.length + invalidRows.length
    };
}

// ═══════════════════════════════════════════════════════════════
// 8. POINT D'ENTRÉE PRINCIPAL
// ═══════════════════════════════════════════════════════════════

/**
 * Importe un fichier d'élèves. Détecte le format automatiquement.
 * Formats supportés : .xlsx, .xls, .csv, .docx
 *
 * @param {File} file
 * @returns {Promise<{success, message, students, invalidRows, totalRows}>}
 */
const importStudentFile = async function(file) {
    if (!file || !(file instanceof File)) {
        return { success: false, message: 'Fichier invalide ou non fourni', students: [], invalidRows: [], totalRows: 0 };
    }
    const ext = file.name.split('.').pop().toLowerCase();
    switch (ext) {
        case 'xlsx': case 'xls': return await processExcelFile(file);
        case 'csv':              return await processCSVFile(file);
        case 'docx':             return await processWordFile(file);
        default:
            return { success: false, message: `Format ".${ext}" non supporté. Utilisez .xlsx, .xls, .csv ou .docx`, students: [], invalidRows: [], totalRows: 0 };
    }
};

// ═══════════════════════════════════════════════════════════════
// 9. EXPORTS (Node.js / module + Navigateur)
// ═══════════════════════════════════════════════════════════════

// Export automatique pour Node.js / modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        importStudentFile,
        processExcelFile, processCSVFile, processWordFile,
        normalizeColumnName, normalizeNameField, normalizeClassField,
        normalizePhoneNumber, parseBirthDate, cleanNamePrefix,
        extractStudentDataFromRow, validateStudent, studentDedupKey,
        isValidData, parseCSV, readAllExcelSheets,
        COLUMN_MAP, KEEP_UPPERCASE_NAMES
    };
}