# 🧠 Analisi Retrospettiva: Implementazione vs Main.tex

Questo documento analizza come l'implementazione reale del codice ci aiuta a capire come migliorare e rendere più "intelligente" il documento di analisi (`main.tex`).

## 1. Cosa abbiamo "Immaginato" vs "Realtà"

### A. La gestione dei Ruoli (Owner vs Organizer)
- **Main.tex**: Tratta Proprietario e Organizzatore quasi come attori distinti o estensioni rigide.
- **Realtà del Codice**: In `CatERing` esisteva già un sistema `User` con Enum `Role`. Abbiamo dovuto implementare `isOwner()` e `isOrganizer()` come metodi booleani dinamicamente verificati.
- **Suggerimento per main.tex**: Specificare nelle "Regole di Business" o nel "Modello di Dominio" che un Utente può avere **molteplici ruoli** (es. Giovanni è sia Owner che Organizer che Staff). Questo giustifica perché l'Owner può fare anche le cose dell'Organizer.

### B. Il concetto di "Eliminazione"
- **Main.tex**: Parla genericamente di "rimuovere dell'elenco" (Estensione 3a).
- **Realtà del Codice**: Abbiamo implementato un **Soft Delete** (`active = 0`). Non si cancella mai veramente un dipendente dal DB (per integrità referenziale sullo storico turni).
- **Miglioramento Smart**: Aggiornare il `main.tex` (post-condizioni) specificando: *"Il sistema marca il collaboratore come archiviato/inattivo mantenendo lo storico degli eventi passati, invece di eliminarlo fisicamente."* Fa sembrare l'analisi molto più professionale e consapevole dei problemi reali dei DB.

### C. Persistenza e Observer
- **Main.tex**: Ignora "come" i dati vengono salvati aggiornati.
- **Realtà del Codice**: L'uso del pattern Observer (`PersonnelEventReceiver`) si è rivelato fondamentale per tenere separata la logica di business (`PersonnelManager`) dal salvataggio SQL (`PersonnelPersistence`).
- **Miglioramento Smart**: Aggiungere una nota tecnica o un diagramma di architettura che mostri il disaccoppiamento. Non è obbligatorio per l'UC, ma alza il livello tecnico percepito.

### D. Le "Note Performance" e gli Eventi
- **Main.tex**: Dice "dopo ogni evento mi faccio delle note".
- **Realtà del Codice**: Abbiamo creato `PerformanceNote` collegata a `Event` (opzionale) e `User` (autore).
- **Conferma**: Questa intuizione era corretta. Il modello dati implementato (`PerformanceNotes` table) supporta perfettamente la User Story di Raffaele.
- **Suggerimento**: Nel DCD del `main.tex`, assicurarsi che la relazione tra `Performance` e `Evento` sia esplicita (0..1), non solo implicita nel testo.

## 2. Punti di Attrito (Dove il main.tex era "ingenuo")

| Punto nel Main.tex | Problema Riscontrato nell'Implementazione | Soluzione Adottata |
|--------------------|-------------------------------------------|-------------------|
| **Aggiunta Personale** | Sembrava un'azione atomica. | Richiede creazione oggetto, validazione, salvataggio, e gestione ID autoincrement. |
| **Utenti/Login** | Dava per scontato "l'attore è loggato". | Abbiamo dovuto gestire il `fakeLogin` nei test per simulare il contesto. |
| **Date Ferie** | "Periodo di ferie". | Gestire `Date` in Java/SQL è complesso. Abbiamo creato helper per formato `yyyy-MM-dd`. |

## 3. Verdetto Finale: Come aggiornare il Main.tex?

Per rendere il `main.tex` "a prova di proiettile" basandoci sul codice funzionante:

1.  **Raffinare le Pre-condizioni**: Aggiungere che per l'eliminazione, il sistema verifica l'assenza di turni *futuri* confermati (non solo assegnati). Questo è quello che fa `hasActiveAssignments()`.
2.  **Esplicitare il Soft Delete**: Cambiare "Rimuove l'utente" in "Disattiva l'utente".
3.  **Dettagliare le Eccezioni**: Le eccezioni 2a.1a e 3b.2a sono perfette. Abbiamo confermato che bloccare a livello di Manager è il posto giusto.
4.  **Promozione**: Specificare che la promozione non cambia solo un flag, ma abilita il collaboratore a logiche di "stipendi/contratti" (anche se non implementati, concettualmente è quello lo scopo).

**Conclusione**: L'immaginazione dello sviluppo era buona all'80%. Il 20% mancante riguarda la gestione pratica della persistenza e la natura "soft" delle cancellazioni per integrità dati.
