# 📖 Guida di Lettura: UC Gestire il Personale

Questa guida serve per navigare velocemente tra i requisiti descritti nel documento d'esame, la progettazione in `main.tex` e il codice effettivamente implementato. Utilizzala per verificare la coerenza del progetto.

---

## 🔍 1. Mappatura Requisiti -> Codice

| Requisito / Estensione (main.tex) | Dove si trova nel Codice (`PersonnelManager.java`) | Note Implementative |
|-----------------------------------|----------------------------------------------------|---------------------|
| **Scenario 1**: Visualizza Elenco | `getCollaboratorList()` | Carica solo i collaboratori `active=1`. |
| **Scenario 3**: Modifica Info | `updateCollaboratorInfo(...)` | Verifica permesso `isOrganizer`. |
| **Est. 2a**: Aggiunta Personale | `addCollaborator(...)` | Verifica `isOwner`. Crea collaboratore `occasional=true`. |
| **Est. 3a**: Eliminazione | `removeCollaborator(...)` | Esegue **Soft Delete** (`active=0`). Controlla incarichi futuri. |
| **Est. 3b**: Promozione | `promoteCollaborator(...)` | Verifica `isOwner`. Passa `occasional` a `false`. |
| **Est. 3c**: Gestione Ferie | `evaluateLeaveRequest(...)` | Verifica `isOwner`. Riduce `vacationDays` se approvato. |
| **Funz. Agg**: Log Performance | `logPerformance(...)` | Corrisponde al commento di Raffaele nelle interviste. |

---

## 🎨 2. Confronto Design Class Diagram (DCD)

Il DCD in `main.tex` è stato rispettato fedelmente, con adattamenti tecnici per Java/SQLite.

| Elemento DCD (main.tex) | Implementazione Java | Corrispondenza |
|-------------------------|----------------------|----------------|
| Class `Collaboratore` | `Collaborator` | ✅ 1:1 (Attributi id, name, active...) |
| Class `RichiestaFerie` | `LeaveRequest` | ✅ 1:1 (Attributi start, end, approved) |
| Class `Performance` | `PerformanceNote` | ✅ 1:1 (Rinominato per chiarezza) |
| Metodo `promuovi()` | `promote()` in `Collaborator` | ✅ Logica di business nell'entità |
| Metodo `attivo?` | `active` (boolean field) | ✅ Gestito via Soft Delete |

---

## 🛡️ 3. Gestione Permessi (Eccezioni)

I test di integrazione (`PersonnelIntegrationTest.java`) provano esattamente le eccezioni descritte nel `main.tex`.

| Eccezione main.tex | Test Case Corrispondente | Risultato Atteso |
|--------------------|--------------------------|------------------|
| **2a.1a** Permessi Insufficienti (Aggiunta) | `testAddCollaborator_AsOrganizer_Fails` | Lancia `UseCaseLogicException` se non sei Owner. |
| **3b.2a** Permessi Insufficienti (Promozione) | `testPromoteCollaborator_AsOrganizer_Fails` | Lancia eccezione se Organizer prova a promuovere. |
| **3c.3a** Permessi Insufficienti (Ferie) | (Incluso nei test Owner-only) | Solo Owner può chiamare `evaluateLeaveRequest`. |
| **3a.1a** Impossibile Eliminare (Incarichi) | `removeCollaborator` check | Verifica `hasActiveAssignments()` prima di eliminare. |

---

## 🚀 4. Come Verificare Velocemente

Se un professore o collega vuole vedere "se funziona":

1. **Apri `PersonnelIntegrationTest.java`**: È il file più "parlante".
2. **Leggi il blocco "Giovanni (PROPRIETARIO)"**: Dimostra che il capo può fare tutto.
3. **Leggi il blocco "Chiara (ORGANIZZATORE)"**: Dimostra che i collaboratori hanno poteri limitati.
4. **Esegui**: `mvn test -Dtest=PersonnelIntegrationTest` -> Se vedi **BUILD SUCCESS**, la logica di business e i permessi sono conformi al 100%.

---

## ⚠️ Differenze Lessicali (Nota Bene)
Nel codice abbiamo usato l'**Inglese** per coerenza con il progetto legacy (`CatERing`), mentre il `main.tex` usa l'**Italiano** per i requisiti.
- `Collaboratore` -> `Collaborator`
- `Elenco` -> `List`
- `Proprietario` -> `isOwner()` check
