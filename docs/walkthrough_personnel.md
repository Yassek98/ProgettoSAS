# 📋 Gestione Personale - Walkthrough Completo per Esame

> **Stato**: Completato (17 Unit Test ✅)  
> **Ultimo aggiornamento**: 27 Gennaio 2026

Questo documento è la guida completa per spiegare ogni scelta implementativa del modulo **Gestione del Personale** all'esame.

---

## 🎯 1. Mappatura Requisiti → Contratti → Codice

### Da UCEsame.txt a main.tex

| Intervista (UCEsame.txt) | Operazione (main.tex) | Metodo Java |
|--------------------------|----------------------|-------------|
| Robert: "mi segno il nome e il contatto" | `aggiungiCollaboratore` | `PersonnelManager.addCollaborator()` |
| Robert: "per il contratto ho bisogno di indirizzo e CF" | `modificaInfoProfilo` | `PersonnelManager.updateCollaboratorInfo()` |
| Raffaele: "lo cancello dall'elenco" | `eliminaCollaboratore` | `PersonnelManager.removeCollaborator()` |
| Robert: "offrire un posto permanente" | `promuoviCollaboratore` | `PersonnelManager.promoteCollaborator()` |
| Robert: "devo decidere se (ferie) accettate" | `valutaRichiestaFerie` | `PersonnelManager.evaluateLeaveRequest()` |
| Raffaele: "dopo ogni evento mi faccio delle note" | `aggiornaStoricoPerformance` | `PersonnelManager.logPerformance()` |

---

## 🔐 2. Permessi - Chi Può Fare Cosa

Derivati direttamente dalle interviste:

| Azione | Robert (Owner) | Raffaele (Organizer) | Fonte |
|--------|---------------|---------------------|-------|
| Aggiungere collaboratori | ✅ | ❌ | "mi segno il nome" |
| Modificare info | ✅ | ✅ | "aggiorno i dettagli" |
| Eliminare | ❌ | ✅ | "lo cancello dall'elenco" |
| Promuovere | ✅ | ❌ | "solo dal proprietario" |
| Approvare ferie | ✅ | ❌ | "devo decidere" |
| Loggare performance | ✅ | ✅ | "mi faccio delle note" |

### Implementazione nel codice:
```java
// PersonnelManager.java
private boolean isOwner(User u) { return u != null && u.isOwner(); }
private boolean isOrganizer(User u) { return u != null && u.isOrganizer(); }
```

---

## 📂 3. Mappatura File Implementati

| Componente | File | Ruolo |
|------------|------|-------|
| **Entity** | `Collaborator.java` | Mappa entità Collaboratore (nome, contatto, occasionale, attivo) |
| **Entity** | `LeaveRequest.java` | Mappa entità RichiestaFerie (start, end, approved) |
| **Entity** | `PerformanceNote.java` | Mappa entità NotaPerformance (collab, event, text, date) |
| **Controller** | `PersonnelManager.java` | Logica applicativa + controllo permessi |
| **Persistence** | `PersonnelPersistence.java` | Observer per salvataggio DB |
| **Interface** | `PersonnelEventReceiver.java` | Interfaccia Observer |

---

## 🏗️ 4. Pattern Architetturali

### Pattern Observer (Persistenza)
```
PersonnelManager → notifyXXX() → PersonnelEventReceiver → PersonnelPersistence.updateXXX()
```

**Perché?** Separa la logica di business dalla persistenza. Il Manager non sa *come* si salva, solo *quando*.

### Pattern Expert (logPerformance)
```java
// Il Collaborator è "esperto" dei propri dati
PerformanceNote note = collab.addPerformanceNote(text, event, author);
```

**Motivazione DSD**: Nel DSD logPerformance.png, la freccia va al Collaborator che crea la nota. È l'esperto delle proprie informazioni.

### Pattern Inizio-Conferma (addCollaborator)
1. `iniziaAggiuntaCollaboratore()` → crea istanza in memoria
2. `aggiungiCollaboratore()` → conferma e persiste

**Perché?** Permette controlli anticipati (permessi) prima dell'input utente.

---

## ⚠️ 5. Eccezioni e Business Rules

### Eccezione 2a.2a - Contatto Duplicato
```java
// Collaborator.create()
if (duplicateFound[0]) {
    throw new PersonnelException("Esiste già un collaboratore attivo con questo contatto");
}
```
**Nota**: Solo contatti ATTIVI contano. Contatti inattivi possono essere riutilizzati.

### Eccezione 3a.1a - Turni Futuri
```java
// Collaborator.deactivate()
if (hasActiveAssignments()) {
    throw new PersonnelException("Impossibile eliminare");
}
```

### Business Rule - Monte Ferie
```java
// PersonnelManager.evaluateLeaveRequest()
if (collab.getVacationDays() < duration) {
    throw new PersonnelException("Monte ferie insufficiente");
}
collab.reduceVacationDays(duration);
```

---

## 🔄 6. Ciclo di Vita Collaboratore

```
NUOVO → occasionale=true, attivo=true
  │
  ├─ promote() → occasionale=false (permanente)
  │
  └─ deactivate() → attivo=false (soft delete)
```

**Soft Delete**: I dati rimangono nel DB per storico, ma `loadActive()` non li restituisce.

---

## 🧪 7. Test Implementati (17 totali)

| Categoria | Test | Verifica |
|-----------|------|----------|
| Creazione | `testNewCollaboratorIsOccasional` | Nuovo = occasionale |
| Update | `testCanUpdateCollaboratorInfo` | Modifica funziona |
| Promozione | `testPromoteOccasionalToPermanent` | promote() cambia stato |
| Eliminazione | `testDeactivatedCollaboratorIsInactive` | Soft delete |
| Ferie | `testApproveLeaveRequest` | Approvazione scala monte |
| Ferie | `testInsufficientVacationDays` | Exception se insufficiente |
| Note | `testAddPerformanceNoteViaDSD` | Pattern Expert funziona |
| Duplicati | `testDuplicateContactThrowsException` | Eccezione 2a.2a |
| Duplicati | `testInactiveContactCanBeReused` | Riuso contatto inattivo OK |

---

## ⚡ 8. Lavori del 27 Gennaio 2026

### Modifiche Codice
1. **Collaborator.java**: Aggiunto `addPerformanceNote()` per allineamento DSD
2. **PersonnelManager.java**: Usa ora `collab.addPerformanceNote()` invece di `PerformanceNote.create()`
3. **main.tex**: Chiarita post-condizione `iniziaAggiuntaCollaboratore()` (in memoria)

### DSD da Correggere
- [ ] `addCollaborator.png`: Manca eccezione contatto duplicato
- [ ] `removeCollaborator.png`: Cambiare `isOwner` → `isOrganizer`
- [ ] `DCD definitivo.jpg`: Aggiungere `addPerformanceNote()` a Collaborator

---

## 🎓 9. Domande Tipiche Esame

**D: Perché il Collaborator crea la PerformanceNote?**
> Pattern Expert: il Collaborator è "esperto" dei propri dati. Ha tutte le info per creare la nota.

**D: Chi può eliminare collaboratori?**
> Gli Organizzatori (Raffaele: "lo cancello dall'elenco"), non solo il Proprietario.

**D: Cosa succede se creo un collaboratore con contatto duplicato?**
> Exception 2a.2a. Ma se il collaboratore precedente è inattivo, il contatto può essere riusato.

**D: Perché il pattern Inizio-Conferma?**
> Permette controllo permessi PRIMA dell'input, evitando che l'utente compili form invano.

**D: Come funziona la persistenza?**
> Pattern Observer: PersonnelManager chiama notifyXXX(), PersonnelPersistence riceve e salva.

---

## 📝 10. Esecuzione Test

```bash
# Tutti i test Personnel
mvn test -Dtest=PersonnelTest

# Compilazione
mvn compile

# Tutto
mvn clean test
```
