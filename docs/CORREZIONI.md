# ✏️ Correzioni da Apportare

## 📄 Correzioni nel file `main.tex`

---

### 1. Contratto `eliminaCollaboratore()` (riga ~461-470)

**Problema**: Controlla "disponibile per" invece di "assegnato a"

**Testo attuale:**
```latex
\item[-] non esiste un'istanza \texttt{\textit{t}} di \texttt{Turno} tale che 
\texttt{\textit{collab}} è \textbf{disponibile per} \texttt{\textit{t}} e \\
\texttt{t.data > data\_corrente}
```

**Cambiare in:**
```latex
\item[-] non esiste un'istanza \texttt{\textit{a}} di \texttt{Assignment} tale che 
\texttt{\textit{a}.cook = \textit{collab}} e \\
\texttt{\textit{a}.shift.data > data\_corrente}
```

---

### 2. Contratto `aggiornaStatoFerie()` - gestione periodo (riga ~485-501)

**Problema**: `inFerie = 'si'` non cattura il periodo

**Testo attuale:**
```latex
\item[-] \texttt{\textit{collab}.inFerie = 'si'}
```

**Cambiare in:**
```latex
\item[-] Per ogni data \texttt{d} tale che \texttt{\textit{rich}.dataInizio $\leq$ d $\leq$ \textit{rich}.dataFine}:
    \begin{itemize}
        \item[-] \texttt{\textit{collab}} risulta \textbf{in ferie} nella data \texttt{d}
    \end{itemize}
```

**Oppure (più semplice):**
```latex
\item[-] È stata creata un'associazione tra \texttt{\textit{collab}} e \texttt{\textit{rich}} 
che indica il periodo di ferie approvato
```

---

### 3. Eccezione 3a.1a - Allineare con nuovo contratto (riga ~221-235)

**Testo attuale:**
```latex
Blocca l'eliminazione. Comunica l'errore e mostra l'elenco degli incarichi futuri 
che impediscono l'operazione.
```

**Va bene così** ✅ - già parla di "incarichi futuri", coerente con la correzione del contratto.

---

## 🖼️ Correzioni nelle Immagini

---

### 4. `logPerformance.png` - Correggere controllo permessi

**Attuale:**
```
alt [!user.isChef() && !user.isOwner()]
    throw UseCaseLogicException
```

**Cambiare in:**
```
alt [!user.isOrganizer()]
    throw UseCaseLogicException
```

> **Logica**: Tutti gli organizzatori possono loggare performance (Raffaele lo fa). Il Proprietario è un tipo di Organizzatore quindi è incluso.

---

### 5. `updateCollaboratorInfo.png` - Rimuovere controllo permessi

**Attuale:**
```
alt [!user.isOwner()]
    throw UseCaseLogicException
```

**Cambiare in:**
```
alt [!user.isOrganizer()]
    throw UseCaseLogicException
```

> **Logica**: Tutti gli organizzatori possono modificare info (Raffaele aggiorna i numeri di telefono).

---

### 6. `removeCollaborator.png` - Cambiare controllo turni

**Attuale:**
```
shiftMgr.hasFutureShifts(collab)
```

**Cambiare in:**
```
collab.hasActiveAssignments()
```

oppure:

```
assignmentMgr.hasFutureAssignments(collab)
```

> **Logica**: Verifica gli incarichi confermati (Assignment), non le semplici disponibilità.

---

### 7. `DCD definitivo.jpg` - Aggiornare classe Collaborator

**Aggiungere metodo:**
```
+ hasActiveAssignments(): boolean
```

**Nota su `inFerie`**: Può rimanere booleano nel DCD se si accetta che il periodo è derivato dalle RichiestaFerie approvate con data corrente compresa tra dataInizio e dataFine.

---

### 8. `MD_3UC-U3.drawio.png` - Nessuna modifica necessaria

Il Modello di Dominio già contiene:
- `RichiestaFerie` con `dataInizio`, `dataFine`, `approvata` ✅
- La relazione "avanzata da" tra Collaboratore e RichiestaFerie ✅

Il campo `inFerie` può essere considerato un attributo derivato.

---

## 📝 Riepilogo Modifiche

| File | Tipo | Cosa cambiare |
|------|------|---------------|
| `main.tex` | Contratto 3a.1 | "disponibile per" → "assegnato a" (Assignment) |
| `main.tex` | Contratto 3c.3 | Periodo ferie tramite RichiestaFerie |
| `logPerformance.png` | DSD | `isChef() && isOwner()` → `isOrganizer()` |
| `updateCollaboratorInfo.png` | DSD | `isOwner()` → `isOrganizer()` |
| `removeCollaborator.png` | DSD | `hasFutureShifts()` → `hasActiveAssignments()` |
| `DCD definitivo.jpg` | Classe | Aggiungere `hasActiveAssignments()` |

---

## ✅ Decisione su Collaboratore vs User

**Confermato**: Un `Collaborator` **può** essere un `User` (se usa il sistema), ma **non è obbligatorio**.

Nel codice questo significa:
```java
public class Collaborator {
    private int id;
    private String name;
    private String contact;
    private String fiscalCode;
    private String address;
    private boolean occasional;  // true = occasionale, false = permanente
    private boolean active;      // false = eliminato (soft delete)
    private int vacationDays;    // monte ferie (solo per permanenti)
    
    private User user;  // OPZIONALE - null se non usa il sistema
}
```

---

## ✅ Validazioni Logiche Implementate (2026-01-18)

Le seguenti validazioni sono state implementate nel codice per prevenire inconsistenze:

### 1. Controllo Nomi Duplicati (`Collaborator.create`)

```java
// Prima di creare un nuovo collaboratore, verifica che non esista già un attivo con lo stesso nome
String query = "SELECT COUNT(*) FROM Collaborators WHERE name = ? AND active = 1";
if (count > 0) {
    throw new PersonnelException("Esiste già un collaboratore attivo con nome: " + name);
}
```

### 2. Controllo Turni Futuri (`Collaborator.deactivate`)

```java
// Prima di disattivare, verifica se ha turni futuri confermati
String query = "SELECT COUNT(*) FROM CollaboratorAvailability ca " +
               "JOIN Shifts s ON ca.shift_id = s.id " +
               "WHERE ca.collaborator_id = ? AND ca.confirmed = 1 AND s.date >= date('now')";
if (count > 0) {
    throw new PersonnelException("Impossibile eliminare: il collaboratore ha turni futuri assegnati.");
}
```

### 3. Controllo Ferie Sovrapposte (`LeaveRequest.save`)

```java
// Prima di salvare una nuova richiesta, verifica sovrapposizioni con ferie già approvate
String query = "SELECT COUNT(*) FROM LeaveRequests " +
               "WHERE collaborator_id = ? AND approved = 1 " +
               "AND (start_date <= ? AND end_date >= ?)";
if (count > 0) {
    throw new PersonnelException("Esiste già una richiesta ferie approvata per questo periodo.");
}
```

> **Test**: Queste validazioni sono testate in `PersonnelStressTest.java` con scenari limite.
