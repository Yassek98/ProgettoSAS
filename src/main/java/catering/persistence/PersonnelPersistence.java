package catering.persistence;

import catering.businesslogic.personnel.*;

/**
 * Implementazione di PersonnelEventReceiver per la persistenza.
 * 
 * Riceve notifiche dal PersonnelManager e salva le modifiche nel database.
 * Segue lo stesso pattern di MenuPersistence e KitchenTaskPersistence.
 * 
 * @see PersonnelEventReceiver
 */
public class PersonnelPersistence implements PersonnelEventReceiver {
    
    @Override
    public void updateCollaboratorAdded(Collaborator collab) {
        collab.save();
    }
    
    @Override
    public void updateCollaboratorUpdated(Collaborator collab) {
        collab.update();
    }
    
    @Override
    public void updateCollaboratorRemoved(Collaborator collab) {
        // Soft delete: aggiorna active=false
        collab.update();
    }
    
    @Override
    public void updateLeaveRequestUpdated(LeaveRequest req) {
        req.update();
        // Aggiorna anche il monte ferie del collaboratore
        req.getCollaborator().update();
    }
    
    @Override
    public void updatePerformanceLogged(Collaborator collab, PerformanceNote note) {
        note.save();
    }
}
