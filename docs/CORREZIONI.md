# ✏️ Correzioni da Apportare al `main.tex` e Diagrammi

> **Autore**: Analisi Automatizzata
> **Data**: 2026-01-19
> **Obiettivo**: Allineare la relazione d'esame (`main.tex`) con l'implementazione del codice e le best practices di documentazione UML/Use Case.

---

## 🎯 Sezione 1: Correzioni CRITICHE al `main.tex`

Queste correzioni riguardano **errori logici o disallineamenti** tra la documentazione e il codice implementato. Devono essere risolte prima della consegna.

---

### 1.1 Contratto `eliminaCollaboratore()` (Righe 461-470)

**❌ Problema**: La pre-condizione verifica che il collaboratore non sia "**disponibile per**" un turno futuro. Questo è **semanticamente errato**.

**Motivazione Tecnica**:
Nel modello del dominio e nel codice, la "disponibilità" (`CollaboratorAvailability`) indica solo che il collaboratore *potrebbe* lavorare in un turno. Il vincolo reale che impedisce l'eliminazione è che il collaboratore abbia un **incarico confermato** (`confirmed = 1`).

L'intervista a Robert dice: *"Solo quando sono chiamati per un turno [..] a quel punto sono vincolati alla presenza"*. "Chiamati" = incarico confermato.

**Codice Implementato** (`Collaborator.hasActiveAssignments`):
```java
String query = "SELECT COUNT(*) FROM CollaboratorAvailability ca " +
               "JOIN Shifts s ON ca.shift_id = s.id " +
               "WHERE ca.collaborator_id = ? AND ca.confirmed = 1 " +  // <-- confirmed!
               "  AND s.date >= date('now')";
```

**Testo Attuale LaTeX (Errato):**
```latex
\item[-] non esiste un'istanza \texttt{\textit{t}} di \texttt{Turno} tale che 
\texttt{\textit{collab}} è \textbf{disponibile per} \texttt{\textit{t}} e \\
\texttt{t.data > data\_corrente}
```

**✅ Testo Corretto:**
```latex
\item[-] non esiste un'istanza \texttt{\textit{a}} di \texttt{CollaboratorAvailability} tale che 
\texttt{\textit{a}.collaborator = \textit{collab}} e 
\texttt{\textit{a}.confirmed = 'si'} e \\
\texttt{\textit{a}.shift.data $>$ data\_corrente}
```

---

### 1.2 Contratto `aggiornaStatoFerie()` - Post-condizione (Righe 496-500)

**❌ Problema**: La post-condizione `collab.inFerie = 'si'` è un flag booleano istantaneo che **non cattura il periodo di ferie**.

**Motivazione Tecnica**:
Nel modello del dominio (`RichiestaFerie`) e nel codice, le ferie hanno un `dataInizio` e un `dataFine`. Il concetto di "in ferie" è **derivato**: un collaboratore è "in ferie" in una data `D` se e solo se esiste una `RichiestaFerie` approvata tale che `dataInizio <= D <= dataFine`.

Un semplice flag `inFerie = 'si'` non permette di sapere *quando* scadono le ferie, causando inconsistenze:
- Chi resetta il flag a 'no' quando le ferie finiscono?
- Come gestire ferie multiple non contigue?

**Testo Attuale LaTeX (Incompleto):**
```latex
\item[-] [se \underline{approvata} = 'si']
\begin{itemize}
    \item[-] \texttt{\textit{collab}.inFerie = 'si'}
    \item[-] \texttt{\textit{collab}.monteFerie = ...}
\end{itemize}
```

**✅ Testo Corretto:**
```latex
\item[-] [se \underline{approvata} = 'si']
\begin{itemize}
    \item[-] \texttt{\textit{rich}.approvata = 'si'} (indica che il periodo è confermato)
    \item[-] Per ogni data \texttt{d} tale che 
        \texttt{\textit{rich}.dataInizio $\leq$ d $\leq$ \textit{rich}.dataFine}:
        \texttt{\textit{collab}} risulta \textbf{in ferie} nella data \texttt{d}.
    \item[-] \texttt{\textit{collab}.monteFerie = \textit{collab}.monteFerie@pre - durata}
\end{itemize}
```

**Oppure (Forma Sintetica Equivalente):**
```latex
\item[-] L'associazione \texttt{\textit{rich}} (\texttt{RichiestaFerie}) tra 
    \texttt{\textit{collab}} e il periodo richiesto è ora marcata come \texttt{approvata = 'si'}.
```

---

### 1.3 Aggiunta Eccezione Mancante: Nomi Duplicati (Nuova Eccezione 2a.2a)

**❌ Problema**: Il main.tex non copre lo scenario in cui si cerca di aggiungere un collaboratore con un nome già esistente.

**Motivazione Tecnica**:
Nel codice, `Collaborator.create()` lancia `PersonnelException` se esiste già un collaboratore attivo con lo stesso nome. Questo vincolo non è documentato.

**Codice Implementato** (`Collaborator.create`):
```java
String query = "SELECT COUNT(*) FROM Collaborators WHERE name = ? AND active = 1";
if (count > 0) {
    throw new PersonnelException("Esiste già un collaboratore attivo con nome: " + name);
}
```

**✅ Aggiungere al main.tex dopo l'Eccezione 2a.1a (riga ~218):**
```latex
\noindent\textbf{\large\color{red} Eccezione 2a.2a - \textbf{Nome duplicato} }
\begin{center}
\renewcommand{\arraystretch}{1.5}
\begin{tabular}{|c|>{\raggedright\arraybackslash}p{0.45\textwidth}|>{\raggedright\arraybackslash}p{0.45\textwidth}|}
\hline
\rowcolor{ForestGreen!40}
\textbf{\#} & \multicolumn{1}{|c|}{\textbf{Attore}} & \multicolumn{1}{|c|}{\textbf{Sistema}} \\
\hline
   2a.2a.1& Inserisce nome e contatto di un nuovo collaboratore.& 
   Il sistema rileva che esiste già un collaboratore attivo con lo stesso nome. 
   Segnala l'errore e richiede di usare un nome diverso.\\
\hline
 & \textit{Il caso d'uso ritorna al passo 2a.1}&\\
\hline
\end{tabular}
\end{center}
```

---

### 1.4 Aggiunta Eccezione Mancante: Ferie Sovrapposte (Nuova Eccezione 3c.3c)

**❌ Problema**: Il main.tex non copre lo scenario in cui si approva una richiesta di ferie che si sovrappone a ferie già approvate.

**Motivazione Tecnica**:
Nel codice, `LeaveRequest.save()` controlla le sovrapposizioni prima di salvare. Ma `aggiornaStatoFerie()` potrebbe anche beneficiare di questo controllo a livello di use case.

**Codice Implementato** (`LeaveRequest.save`):
```java
String checkQuery = "SELECT COUNT(*) FROM LeaveRequests " +
    "WHERE collaborator_id = ? AND approved = 1 " +
    "AND (start_date <= ? AND end_date >= ?)";
if (count > 0) {
    throw new PersonnelException("Esiste già una richiesta ferie approvata per questo periodo.");
}
```

**✅ Aggiungere al main.tex dopo l'Eccezione 3c.3b (riga ~290):**
```latex
\noindent\textbf{\large\color{red} Eccezione 3c.3c - \textbf{Ferie sovrapposte} }
\begin{center}
\renewcommand{\arraystretch}{1.5}
\begin{tabular}{|c|>{\raggedright\arraybackslash}p{0.45\textwidth}|>{\raggedright\arraybackslash}p{0.45\textwidth}|}
\hline
\rowcolor{ForestGreen!40}
\textbf{\#} & \multicolumn{1}{|c|}{\textbf{Attore}} & \multicolumn{1}{|c|}{\textbf{Sistema}} \\
\hline
   3c.3c.1& Approva la richiesta di ferie.& 
   Il sistema rileva che il periodo richiesto si sovrappone a ferie già approvate per lo stesso collaboratore.
   Blocca l'approvazione e segnala il conflitto.\\
\hline
 & \textit{Termina il caso d'uso}&\\
\hline
\end{tabular}
\end{center}
```

---

## 🎯 Sezione 2: Correzioni alle IMMAGINI (DSD e DCD)

Queste correzioni riguardano i diagrammi di sequenza di progettazione che devono riflettere la logica implementata nel codice.

---

### 2.1 `logPerformance.png` - Controllo Permessi Errato

**❌ Attuale**: `alt [!user.isChef() && !user.isOwner()]`
**✅ Corretto**: `alt [!user.isOrganizer()]`

**Motivazione**: 
Dall'intervista a Raffaele: *"Dopo ogni evento mi faccio delle note"*. Raffaele è un Organizzatore, non uno Chef né il Proprietario. Nel codice:
```java
// PersonnelManager.logPerformance()
if (!isOrganizer(currentUser)) {
    throw new UseCaseLogicException("Permessi insufficienti");
}
```
Il metodo `isOrganizer()` include sia Giovanni (Owner, che è anche Organizer) sia Chiara/Francesca (Organizer puri).

---

### 2.2 `updateCollaboratorInfo.png` - Controllo Permessi Errato

**❌ Attuale**: `alt [!user.isOwner()]`
**✅ Corretto**: `alt [!user.isOrganizer()]`

**Motivazione**: 
Dall'intervista a Raffaele: *"aggiorno i dettagli di altri, ad esempio cambiando il numero di telefono"*.

Nel codice:
```java
// PersonnelManager.updateCollaboratorInfo()
if (!isOrganizer(currentUser)) { // <-- NON isOwner!
    throw new UseCaseLogicException("Permessi insufficienti");
}
```

---

### 2.3 `removeCollaborator.png` - Controllo Turni Errato

**❌ Attuale**: `shiftMgr.hasFutureShifts(collab)`
**✅ Corretto**: `collab.hasActiveAssignments()` oppure query diretta su `CollaboratorAvailability`

**Motivazione**: 
Il sistema non verifica i "turni futuri" in generale, ma gli **incarichi confermati** (`confirmed = 1`). Vedi Sezione 1.1.

Nel codice:
```java
// PersonnelManager.removeCollaborator()
if (collab.hasActiveAssignments()) { // <-- Metodo dell'entità, non del manager!
    throw new UseCaseLogicException("Impossibile eliminare...");
}
```

---

### 2.4 `DCD definitivo.jpg` - Classe Collaborator Incompleta

**❌ Problema**: Manca il metodo `hasActiveAssignments()`.

**✅ Aggiungere nella sezione metodi di `Collaborator`:**
```
+ hasActiveAssignments(): boolean
+ reduceVacationDays(days: int): void  // Già presente? Verificare
```

**Opzionale**: Aggiungere `PersonnelException` come classe nel DCD con associazione tratteggiata (`<<throws>>`) ai metodi che la lanciano.

---

### 2.5 `evalueteLeaveRequest.png` - Typo nel Nome File

**❌ Attuale**: `evalueteLeaveRequest.png` (con "ete" invece di "ate")
**✅ Corretto**: `evaluateLeaveRequest.png`

Questo richiede anche di aggiornare il riferimento in `main.tex` (riga 555):
```latex
% Attuale (errato)
\includegraphics[width=1.2\linewidth]{evalueteLeaveRequest.png}

% Corretto
\includegraphics[width=1.2\linewidth]{evaluateLeaveRequest.png}
```

---

## 🎯 Sezione 3: Correzioni STILISTICHE al `main.tex`

Queste correzioni migliorano la qualità formale del documento ma non sono bloccanti.

---

### 3.1 Caption Placeholder da Completare

Le seguenti figure hanno caption generiche "Enter Caption" che devono essere sostituite:

| Riga | File | Caption Attuale | Caption Suggerita |
|------|------|-----------------|-------------------|
| 545 | `removeCollaborator.png` | "Enter Caption" | "DSD **removeCollaborator(collab)**: Eliminazione (soft-delete) di un collaboratore" |
| 556 | `evalueteLeaveRequest.png` | "Enter Caption" | "DSD **evaluateLeaveRequest(req, approved)**: Approvazione o rifiuto di una richiesta ferie" |
| 567 | `promoteCollaborator.png` | "Enter Caption" | "DSD **promoteCollaborator(collab)**: Promozione da occasionale a permanente" |
| 578 | `logPerformance.png` | "Enter Caption" | "DSD **logPerformance(collab, event, text)**: Registrazione nota sulle performance" |

---

### 3.2 Label Duplicate

Le figure da riga 508 a 580 usano tutte `\label{fig:placeholder}`. Questo causa warning LaTeX e impedisce riferimenti incrociati corretti.

**✅ Usare label univoche:**
- `\label{fig:dsd-addCollab}`
- `\label{fig:dsd-updateInfo}`
- `\label{fig:dsd-removeCollab}`
- `\label{fig:dsd-evalLeave}`
- `\label{fig:dsd-promote}`
- `\label{fig:dsd-logPerf}`

---

### 3.3 Typo "sufficenti" → "sufficienti"

Nelle eccezioni (righe 206, 239, 256) compare "sufficenti" invece di "sufficienti".

---

### 3.4 Carattere Accentato Rotto

Alla riga 192 compare `gi`a` invece di `già`. Verificare l'encoding UTF-8.

---

## 📋 Riepilogo Modifiche Ordinate per Priorità

### 🔴 Priorità ALTA (Bloccanti per Correttezza)

| ID | Tipo | Dove | Cosa Fare |
|----|------|------|-----------|
| 1.1 | Contratto | `main.tex` L461-470 | "disponibile per Turno" → "assegnato (confirmed) a Shift" |
| 1.2 | Contratto | `main.tex` L496-500 | `inFerie = 'si'` → periodo derivato da RichiestaFerie |
| 1.3 | Eccezione | `main.tex` dopo L218 | Aggiungere Eccezione 2a.2a (Nome duplicato) |
| 1.4 | Eccezione | `main.tex` dopo L290 | Aggiungere Eccezione 3c.3c (Ferie sovrapposte) |
| 2.1 | Immagine | `logPerformance.png` | `isChef() && isOwner()` → `isOrganizer()` |
| 2.2 | Immagine | `updateCollaboratorInfo.png` | `isOwner()` → `isOrganizer()` |
| 2.3 | Immagine | `removeCollaborator.png` | `hasFutureShifts()` → `hasActiveAssignments()` |
| 2.4 | Immagine | `DCD definitivo.jpg` | Aggiungere `hasActiveAssignments()` a Collaborator |

### 🟡 Priorità MEDIA (Qualità Formale)

| ID | Tipo | Dove | Cosa Fare |
|----|------|------|-----------|
| 2.5 | Rinomina | `evalueteLeaveRequest.png` | Correggere typo → `evaluateLeaveRequest.png` |
| 3.1 | Caption | `main.tex` L545,556,567,578 | Sostituire "Enter Caption" con descrizioni reali |
| 3.2 | Label | `main.tex` L513-580 | Usare label univoche per ogni figura |

### 🟢 Priorità BASSA (Cosmetiche)

| ID | Tipo | Dove | Cosa Fare |
|----|------|------|-----------|
| 3.3 | Typo | `main.tex` L206,239,256 | "sufficenti" → "sufficienti" |
| 3.4 | Encoding | `main.tex` L192 | `` gi`a `` → `già` |

---

## ✅ Stato Attuale (Da Aggiornare Dopo le Modifiche)

- [ ] 1.1 Contratto eliminaCollaboratore
- [ ] 1.2 Contratto aggiornaStatoFerie
- [ ] 1.3 Eccezione Nome Duplicato
- [ ] 1.4 Eccezione Ferie Sovrapposte
- [ ] 2.1 DSD logPerformance
- [ ] 2.2 DSD updateCollaboratorInfo
- [ ] 2.3 DSD removeCollaborator
- [ ] 2.4 DCD Collaborator.hasActiveAssignments()
- [x] 2.5 Rinomina evaluateLeaveRequest
- [x] 3.1 Caption Figure
- [x] 3.2 Label Univoche
- [x] 3.3 Typo "sufficienti"
- [x] 3.4 Encoding "già"
