package catering.businesslogic.personnel;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.event.Event;
import catering.businesslogic.user.User;

import java.util.ArrayList;

/**
 * Controller principale per la gestione del personale.
 * 
 * Implementa tutte le operazioni dello Use Case "Gestire il Personale" (vedi main.tex).
 * 
 * Operazioni supportate:
 * - visualizzaElencoPersonale() - Scenario principale passo 1
 * - visualizzaProfiloCompleto() - Scenario principale passo 2
 * - modificaInfoProfilo() - Scenario principale passo 3
 * - addCollaborator() - Estensione 2a
 * - removeCollaborator() - Estensione 3a
 * - promoteCollaborator() - Estensione 3b
 * - evaluateLeaveRequest() - Estensione 3c
 * - logPerformance() - Funzionalità aggiuntiva (DSD logPerformance)
 * 
 * Usa il pattern Observer per notificare i cambiamenti a PersonnelPersistence.
 * 
 * @see DCD definitivo.jpg - PersonnelManager class
 * @see main.tex per i contratti delle operazioni
 */
public class PersonnelManager {
    
    private Collaborator currentCollaborator;  // collaboratore attualmente selezionato
    private ArrayList<PersonnelEventReceiver> eventReceivers;
    
    public PersonnelManager() {
        this.eventReceivers = new ArrayList<>();
    }
    
    // ==================== GESTIONE OBSERVER ====================
    
    public void addEventReceiver(PersonnelEventReceiver rec) {
        eventReceivers.add(rec);
    }
    
    public void removeEventReceiver(PersonnelEventReceiver rec) {
        eventReceivers.remove(rec);
    }
    
    private void notifyCollaboratorAdded(Collaborator collab) {
        for (PersonnelEventReceiver rec : eventReceivers) {
            rec.updateCollaboratorAdded(collab);
        }
    }
    
    private void notifyCollaboratorUpdated(Collaborator collab) {
        for (PersonnelEventReceiver rec : eventReceivers) {
            rec.updateCollaboratorUpdated(collab);
        }
    }
    
    private void notifyCollaboratorRemoved(Collaborator collab) {
        for (PersonnelEventReceiver rec : eventReceivers) {
            rec.updateCollaboratorRemoved(collab);
        }
    }
    
    private void notifyLeaveRequestUpdated(LeaveRequest req) {
        for (PersonnelEventReceiver rec : eventReceivers) {
            rec.updateLeaveRequestUpdated(req);
        }
    }
    
    private void notifyPerformanceLogged(Collaborator collab, PerformanceNote note) {
        for (PersonnelEventReceiver rec : eventReceivers) {
            rec.updatePerformanceLogged(collab, note);
        }
    }
    
    // ==================== OPERAZIONI DI QUERY ====================
    
    /**
     * Visualizza l'elenco del personale (scenario principale passo 1).
     * 
     * Contratto main.tex:
     * - Pre-condizioni: nessuna oltre a quella generale (utente autenticato)
     * - Post-condizioni: nessuna (operazione di interrogazione)
     * 
     * @return Lista di tutti i collaboratori attivi
     */
    public ArrayList<Collaborator> getCollaboratorList() {
        return Collaborator.loadActive();
    }
    
    /**
     * Visualizza il profilo completo di un collaboratore (scenario principale passo 2).
     * Imposta anche il collaboratore corrente per operazioni successive.
     * 
     * @param collab Il collaboratore da visualizzare
     * @return Il collaboratore con tutti i dati
     */
    public Collaborator getCollaboratorProfile(Collaborator collab) {
        this.currentCollaborator = collab;
        return collab;
    }
    
    /**
     * Visualizza lo storico delle performance (estensione 3b.1).
     * 
     * @param collab Il collaboratore
     * @return Lista delle note sulle performance
     */
    public ArrayList<PerformanceNote> getPerformanceHistory(Collaborator collab) {
        return PerformanceNote.loadByCollaborator(collab);
    }
    
    /**
     * Visualizza le richieste di ferie (estensione 3c.1).
     * 
     * @param collab Il collaboratore
     * @return Lista delle richieste ferie
     */
    public ArrayList<LeaveRequest> getLeaveRequests(Collaborator collab) {
        return LeaveRequest.loadByCollaborator(collab);
    }
    
    // ==================== OPERAZIONI DI MODIFICA ====================
    
    /**
     * Aggiunge un nuovo collaboratore (estensione 2a).
     * 
     * Contratto main.tex (2a.1 + 2a.2):
     * - Pre-condizione: org ricopre un Ruolo con tipo = 'Proprietario'
     * - Post-condizioni:
     *   - collab.nome = nome
     *   - collab.contatto = contatto
     *   - collab.occasionale = 'si'
     *   - collab.attivo = 'si'
     * 
     * @param name Nome del collaboratore
     * @param contact Contatto (email/telefono)
     * @return Il nuovo collaboratore creato
     * @throws UseCaseLogicException se l'utente non è Proprietario
     */
    public Collaborator addCollaborator(String name, String contact) throws UseCaseLogicException {
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        
        // Verifica permessi: solo il Proprietario può aggiungere (eccezione 2a.1a)
        if (!isOwner(currentUser)) {
            throw new UseCaseLogicException("Permessi insufficienti: solo il Proprietario può aggiungere collaboratori");
        }
        
        // Crea il nuovo collaboratore (contratto 2a.2)
        Collaborator newCollab = Collaborator.create(name, contact);
        
        // Notifica per la persistenza
        notifyCollaboratorAdded(newCollab);
        
        return newCollab;
    }
    
    /**
     * Modifica le informazioni del profilo (scenario principale passo 3).
     * 
     * Contratto main.tex (3):
     * - Pre-condizione: è in corso la visualizzazione del profilo di collab
     * - Post-condizioni: [se specificato] collab.nome/codFisc/contatto/indirizzo = nuovo valore
     * 
     * NOTA: Secondo le interviste, tutti gli organizzatori possono modificare info.
     * 
     * @param collab Collaboratore da modificare
     * @param newName Nuovo nome (null per non modificare)
     * @param newFiscalCode Nuovo codice fiscale
     * @param newContact Nuovo contatto
     * @param newAddress Nuovo indirizzo
     * @throws UseCaseLogicException se l'utente non è Organizzatore
     */
    public void updateCollaboratorInfo(Collaborator collab, String newName, String newFiscalCode, 
                                        String newContact, String newAddress) throws UseCaseLogicException {
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        
        // Verifica permessi: tutti gli Organizzatori possono modificare (CORREZIONI.md)
        if (!isOrganizer(currentUser)) {
            throw new UseCaseLogicException("Permessi insufficienti: solo gli Organizzatori possono modificare");
        }
        
        // Aggiorna le info
        collab.updateInfo(newName, newFiscalCode, newContact, newAddress);
        
        // Notifica per la persistenza
        notifyCollaboratorUpdated(collab);
    }
    
    /**
     * Elimina un collaboratore - soft delete (estensione 3a).
     * 
     * Contratto main.tex (3a.1):
     * - Pre-condizione: è in corso la visualizzazione del profilo di collab
     * - Pre-condizione: non esiste Assignment con a.cook = collab e a.shift.data > oggi
     * - Post-condizione: collab.attivo = 'no'
     * 
     * @param collab Collaboratore da eliminare
     * @throws UseCaseLogicException se ha incarichi futuri (eccezione 3a.1a)
     */
    public void removeCollaborator(Collaborator collab) throws UseCaseLogicException {
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        
        // Verifica permessi
        if (!isOrganizer(currentUser)) {
            throw new UseCaseLogicException("Permessi insufficienti");
        }
        
        // Verifica incarichi futuri (eccezione 3a.1a)
        if (collab.hasActiveAssignments()) {
            throw new UseCaseLogicException("Impossibile eliminare: il collaboratore ha incarichi futuri");
        }
        
        // Disattiva (soft delete)
        collab.deactivate();
        
        // Notifica per la persistenza
        notifyCollaboratorRemoved(collab);
    }
    
    /**
     * Promuove un collaboratore da occasionale a permanente (estensione 3b.2).
     * 
     * Contratto main.tex (3b.2):
     * - Pre-condizione: è in corso la visualizzazione del profilo di collab
     * - Pre-condizione: org ricopre un Ruolo con tipo = 'Proprietario'
     * - Pre-condizione: collab.occasionale = 'si'
     * - Post-condizione: collab.occasionale = 'no'
     * 
     * @param collab Collaboratore da promuovere
     * @throws UseCaseLogicException se non è Proprietario (eccezione 3b.2a) o collaboratore già permanente
     */
    public void promoteCollaborator(Collaborator collab) throws UseCaseLogicException {
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        
        // Verifica permessi: solo Proprietario (eccezione 3b.2a)
        if (!isOwner(currentUser)) {
            throw new UseCaseLogicException("Permessi insufficienti: solo il Proprietario può promuovere");
        }
        
        // Verifica che sia occasionale
        if (!collab.isOccasional()) {
            throw new UseCaseLogicException("Il collaboratore è già permanente");
        }
        
        // Promuovi
        collab.promote();
        
        // Notifica per la persistenza
        notifyCollaboratorUpdated(collab);
    }
    
    /**
     * Valuta e approva/rifiuta una richiesta di ferie (estensione 3c.3).
     * 
     * Contratto main.tex (3c.3):
     * - Pre-condizione: org ricopre un Ruolo con tipo = 'Proprietario'
     * - Pre-condizione: rich.approvata = 'no' (pendente)
     * - Pre-condizione: [se approvata] collab.monteFerie >= durata richiesta
     * - Post-condizioni:
     *   - rich.approvata = approvata
     *   - [se approvata] collab.monteFerie ridotto della durata
     * 
     * @param req Richiesta da valutare
     * @param approved true per approvare, false per rifiutare
     * @throws UseCaseLogicException se non è Proprietario o monte ferie insufficiente
     * @throws PersonnelException se errore nella riduzione monte ferie
     */
    public void evaluateLeaveRequest(LeaveRequest req, boolean approved) 
            throws UseCaseLogicException, PersonnelException {
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        
        // Verifica permessi (eccezione 3c.3a)
        if (!isOwner(currentUser)) {
            throw new UseCaseLogicException("Permessi insufficienti: solo il Proprietario può approvare ferie");
        }
        
        // Verifica che sia pendente
        if (!req.isPending()) {
            throw new UseCaseLogicException("La richiesta non è più pendente");
        }
        
        Collaborator collab = req.getCollaborator();
        
        if (approved) {
            // Verifica monte ferie (eccezione 3c.3b)
            int duration = req.getDuration();
            if (collab.getVacationDays() < duration) {
                throw new PersonnelException("Monte ferie insufficiente: richiesti " + duration + 
                        " giorni, disponibili " + collab.getVacationDays());
            }
            
            // Riduci monte ferie
            collab.reduceVacationDays(duration);
            req.approve();
        } else {
            req.reject();
        }
        
        // Notifica per la persistenza
        notifyLeaveRequestUpdated(req);
    }
    
    /**
     * Registra una nota sulle performance (DSD logPerformance).
     * 
     * Secondo le interviste e CORREZIONI.md:
     * - Tutti gli Organizzatori possono loggare performance
     * - (Raffaele: "dopo ogni evento mi faccio delle note")
     * 
     * @param collab Collaboratore a cui si riferisce
     * @param event Evento opzionale
     * @param text Testo della nota
     * @return La nota creata
     * @throws UseCaseLogicException se non è Organizzatore
     */
    public PerformanceNote logPerformance(Collaborator collab, Event event, String text) 
            throws UseCaseLogicException {
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        
        // Verifica permessi: tutti gli Organizzatori (CORREZIONI.md)
        if (!isOrganizer(currentUser)) {
            throw new UseCaseLogicException("Permessi insufficienti: solo gli Organizzatori possono loggare performance");
        }
        
        // Crea la nota
        PerformanceNote note = PerformanceNote.create(collab, event, currentUser, text);
        
        // Notifica per la persistenza
        notifyPerformanceLogged(collab, note);
        
        return note;
    }
    
    // ==================== HELPER PERMESSI ====================
    
    /**
     * Verifica se l'utente è Proprietario.
     * Delega a User.isOwner().
     */
    private boolean isOwner(User user) {
        return user != null && user.isOwner();
    }
    
    /**
     * Verifica se l'utente è Organizzatore.
     * Delega a User.isOrganizer() che include anche Proprietario.
     */
    private boolean isOrganizer(User user) {
        return user != null && user.isOrganizer();
    }
    
    // ==================== GETTERS ====================
    
    public Collaborator getCurrentCollaborator() {
        return currentCollaborator;
    }
}
