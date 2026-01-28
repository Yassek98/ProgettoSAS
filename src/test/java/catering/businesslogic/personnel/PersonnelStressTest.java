package catering.businesslogic.personnel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catering.persistence.PersistenceManager;

/**
 * STRESS & INTEGRATION TEST SUITE
 * 
 * Questo file contiene test progettati per "stressare" la logica del sistema
 * con scenari limite, interazioni complesse e verifica dell'integrità dei dati.
 */
@DisplayName("Stress & Logic Integrity Tests")
public class PersonnelStressTest {

    @BeforeAll
    static void init() {
        PersistenceManager.initializeDatabase("database/catering_init_sqlite.sql");
    }

    @Nested
    @DisplayName("Logica Ferie: Conflitti e Paradossi")
    class LeaveLogicStressTest {

        @Test
        @DisplayName("STRESS: Richiesta ferie con date sovrapposte a una già approvata")
        void testOverlappingLeaveRequests() throws PersonnelException {
            // Scenario: Un collaboratore ha ferie approvate per 1-10 Gennaio.
            // Chiede ferie per 5-15 Gennaio. Il sistema dovrebbe gestirlo (rifiutare o unire?).
            
            Collaborator collab = Collaborator.create("Overlap Test " + System.currentTimeMillis(), "111" + System.currentTimeMillis());
            collab.promote();
            collab.setVacationDays(30);
            collab.save();

            Date today = new Date();
            Date tenDaysFromNow = new Date(today.getTime() + 10L * 24 * 60 * 60 * 1000);
            
            // Richiesta 1: Approvata
            LeaveRequest req1 = LeaveRequest.create(collab, today, tenDaysFromNow); // Oggi -> +10gg
            req1.save();
            req1.approve();
            req1.update();
            collab.reduceVacationDays(req1.getDuration());

            // Richiesta 2: Sovrapposizione parziale (parte 5 giorni dopo oggi)
            Date midDate = new Date(today.getTime() + 5L * 24 * 60 * 60 * 1000);
            Date futureDate = new Date(today.getTime() + 15L * 24 * 60 * 60 * 1000);
            
            LeaveRequest req2 = LeaveRequest.create(collab, midDate, futureDate);
            
            // VERIFICA: Ora ci aspettiamo un'eccezione perché le ferie si sovrappongono
            assertThrows(PersonnelException.class, () -> {
                req2.save();
            }, "Il sistema dovrebbe impedire il salvataggio di ferie sovrapposte");
        }
    }

    @Nested
    @DisplayName("Integrazione: Collaboratori e Turni")
    class AssignmentIntegrationTest {

        @Test
        @DisplayName("CRITICAL: Impossibile eliminare collaboratore con turni futuri")
        void testCannotDeleteActiveCollaborator() throws PersonnelException {
            // 1. Creiamo un collaboratore
            Collaborator collab = Collaborator.create("Busy Worker " + System.currentTimeMillis(), "222" + System.currentTimeMillis());
            collab.save(); // ID assegnato
            
            // 2. Simuliamo un incarico futuro (CollaboratorAvailability)
            // Fix: ora usiamo CollaboratorAvailability come richiesto dalla nuova logica
            insertMockAvailability(collab.getId());

            // 3. Verifichiamo che il sistema RILEVI l'incarico
            boolean hasAssignments = collab.hasActiveAssignments();
            assertTrue(hasAssignments, "Il sistema deve rilevare i turni futuri");

            // 4. Proviamo a disattivare - Deve fallire
            assertThrows(PersonnelException.class, () -> {
                collab.deactivate();
            }, "Il collaboratore non deve essere disattivabile se ha turni futuri!");
            
            assertTrue(collab.isActive(), "Lo stato deve rimanere attivo dopo il tentativo fallito");
        }

        private void insertMockAvailability(int collabId) {
            // Helper per iniettare dati nel DB e simulare lo stato "impegnato" in CollaboratorAvailability
            
            // Creiamo un Shift futuro
            String sqlShift = "INSERT INTO Shifts (date, start_time, end_time) VALUES (date('now', '+5 days'), '10:00', '14:00')";
            PersistenceManager.executeUpdate(sqlShift);
            int shiftId = PersistenceManager.getLastId();

            // INSERT CollaboratorAvailability (confirmed = 1)
            String sqlAssign = "INSERT INTO CollaboratorAvailability (collaborator_id, shift_id, confirmed) " +
                               "VALUES (" + collabId + ", " + shiftId + ", 1)";
            PersistenceManager.executeUpdate(sqlAssign);
        }
    }
    
    @Nested
    @DisplayName("Integrità Dati")
    class DataIntegrityTest {
        
        @Test
        @DisplayName("STRESS: Duplicate Contacts Denied")
        void testDuplicateContactDenied() throws PersonnelException {
            // Cosa succede se creo un collaboratore con lo stesso contatto di uno eliminato?
            Collaborator c1 = Collaborator.create("Mario Rossi", "+39 555 0001");
            c1.save();
            
            // Tentativo di creare un secondo collaboratore attivo con lo stesso contatto
            assertThrows(PersonnelException.class, () -> {
                Collaborator.create("Luigi Bianchi", "+39 555 0001");
            }, "Il sistema deve impedire la creazione di collaboratori con lo stesso contatto");
            
            // Ma se disattivo il primo?
            c1.deactivate();
            c1.update();
            assertFalse(c1.isActive());
            
            // Ora dovrei poter creare un nuovo collaboratore con lo stesso contatto
            Collaborator c2 = Collaborator.create("Paolo Verdi", "+39 555 0001");
            c2.save();
            assertNotNull(c2);
        }
    }
}
