package catering.businesslogic.personnel;

/**
 * Interface per il pattern Observer nel modulo Personnel Management.
 * 
 * Chi implementa questa interface riceve notifiche quando:
 * - Un collaboratore viene aggiunto, modificato o eliminato
 * - Una richiesta ferie viene aggiornata
 * - Viene loggata una nota sulle performance
 * 
 * Segue lo stesso pattern di MenuEventReceiver e KitchenTaskEventReceiver.
 * 
 * @see DCD definitivo.jpg - PersonnelEventReceiver interface
 */
public interface PersonnelEventReceiver {
    
    /**
     * Chiamato quando un nuovo collaboratore viene aggiunto.
     * Corrisponde all'operazione aggiungiCollaboratore (estensione 2a).
     */
    void updateCollaboratorAdded(Collaborator collab);
    
    /**
     * Chiamato quando le informazioni di un collaboratore vengono modificate.
     * Corrisponde all'operazione modificaInfoProfilo (passo 3) o promozione (3b.2).
     */
    void updateCollaboratorUpdated(Collaborator collab);
    
    /**
     * Chiamato quando un collaboratore viene eliminato (soft delete).
     * Corrisponde all'operazione eliminaCollaboratore (estensione 3a).
     */
    void updateCollaboratorRemoved(Collaborator collab);
    
    /**
     * Chiamato quando una richiesta di ferie viene approvata o rifiutata.
     * Corrisponde all'operazione aggiornaStatoFerie (estensione 3c).
     */
    void updateLeaveRequestUpdated(LeaveRequest req);
    
    /**
     * Chiamato quando viene loggata una nota sulle performance.
     * Corrisponde al DSD logPerformance.
     */
    void updatePerformanceLogged(Collaborator collab, PerformanceNote note);
}
