# 📝 CORREZIONI da Applicare - 27 Gennaio 2026

Questo documento elenca tutte le modifiche da applicare ai **file PNG** (DSD, SSD) per allinearli al codice e ai contratti.

---

## ✅ Modifiche Codice Già Applicate

| File | Modifica |
|------|----------|
| `docs/main.tex` | Chiarita post-condizione `iniziaAggiuntaCollaboratore()` (in memoria) |
| `Collaborator.java` | Aggiunto metodo `addPerformanceNote(text, event, author)` |
| `PersonnelManager.java` | `logPerformance()` ora usa `collab.addPerformanceNote()` |

---

## ⚠️ MODIFICHE PNG DA FARE

### 1. `addCollaborator.png` (DSD)

**Problema**: Manca l'eccezione per contatto duplicato (Eccezione 2a.2a di main.tex)

**Azione**:
- Aggiungere frammento `alt` dopo `create(name, contact)`:
```
alt [duplicateContact]
    ← throw PersonnelException("Contatto duplicato")
[else]
    → prosegui con add(newCollab)
```

---

### 2. `removeCollaborator.png` (DSD)

**Problema**: Guardia permessi sbagliata

**Azione**:
- **Cambiare** `[!user.isOwner()]` → `[!user.isOrganizer()]`

**Motivazione** (UCEsame.txt, Raffaele):
> "lo cancello dall'elenco" → Organizzatori possono eliminare

---

### 3. `promoteCollaborator.png` (DSD)

**Problema minore**: Nome metodo diverso

| Nel DSD | Nel Codice |
|---------|------------|
| `setPermanent()` | `promote()` |

**Azione (opzionale)**: Per coerenza, cambiare nel DSD: `setPermanent()` → `promote()`

---

## 🔍 Verifiche Aggiuntive Consigliate

### main.tex
- ✅ Modifica "Nome duplicato" → "Contatto duplicato" (riga 222) già applicata

### DCD definitivo.jpg
- ⚠️ Verificare che `PerformanceNote` sia visibile come classe (sembra mancare nel diagramma)
- ⚠️ Aggiungere metodo `addPerformanceNote()` alla classe `Collaborator`

### Test Mancanti
- ❌ **Non esistono test per `PerformanceNote`**
- Da creare: test per `logPerformance()` e persistenza note

---

## 📋 Checklist Modifiche PNG

- [ ] `addCollaborator.png` - Aggiungere eccezione contatto duplicato
- [ ] `removeCollaborator.png` - Cambiare `isOwner` → `isOrganizer`
- [ ] `promoteCollaborator.png` - (opzionale) Rinominare `setPermanent` → `promote`
- [ ] `DCD definitivo.jpg` - Aggiungere `addPerformanceNote()` a Collaborator
