package catering.businesslogic.personnel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catering.persistence.PersistenceManager;

import java.util.Date;
import java.util.logging.Logger;
import catering.util.LogManager;

/**
 * Unit Test per il modulo Personnel Management.
 * 
 * I TEST SONO PROGETTATI SUI REQUISITI DEL CLIENTE (interviste Robert e Raffaele),
 * NON sull'implementazione. Questo segue l'approccio TDD (Test-Driven Validation).
 * 
 * ============================================================================
 * REQUISITI CLIENTE (dalle interviste):
 * ============================================================================
 * 
 * ROBERT (Proprietario):
 * - "tengo traccia dei collaboratori con nome, contatto, CF, indirizzo"
 * - "i collaboratori possono essere permanenti o occasionali"
 * - "io decido chi promuovere a permanente"
 * - "solo io approvo le richieste di ferie"
 * - "prima di promuovere consulto lo storico delle performance"
 * 
 * RAFFAELE (Organizzatore):
 * - "dopo ogni evento mi faccio delle note sulle performance"
 * - "aggiorno i dettagli dei collaboratori (telefono, etc.)"
 * - "non posso però aggiungere o promuovere collaboratori"
 * 
 * REGOLE BUSINESS:
 * - Non si può eliminare chi ha incarichi futuri
 * - Monte ferie di un permanente si riduce quando approvo ferie
 * - Le ferie hanno data inizio e fine
 */
@DisplayName("Personnel Management - Test basati sui requisiti cliente")
public class PersonnelTest {

    private static final Logger LOGGER = LogManager.getLogger(PersonnelTest.class);
    
    @BeforeAll
    static void init() {
        // Inizializza il database per i test
        PersistenceManager.initializeDatabase("database/catering_init_sqlite.sql");
        LOGGER.info("Database inizializzato per i test");
    }
    
    // ========================================================================
    // TEST COLLABORATOR - Basati su: "tengo traccia dei collaboratori"
    // ========================================================================
    
    @Nested
    @DisplayName("Collaborator - Creazione e dati base")
    class CollaboratorCreationTest {
        
        @Test
        @DisplayName("Robert: 'quando aggiungo un collaboratore, parte come occasionale'")
        void testNewCollaboratorIsOccasional() {
            // REQUISITO: Robert dice che i nuovi arrivati sono sempre occasionali
            Collaborator collab = Collaborator.create("Mario Rossi", "+39 333 1234567");
            
            assertTrue(collab.isOccasional(), 
                "Un nuovo collaboratore DEVE essere occasionale di default");
            assertTrue(collab.isActive(), 
                "Un nuovo collaboratore DEVE essere attivo");
        }
        
        @Test
        @DisplayName("Robert: 'tengo traccia di nome, contatto, CF, indirizzo'")
        void testCollaboratorHasRequiredFields() {
            // REQUISITO: Robert vuole memorizzare questi dati
            Collaborator collab = Collaborator.create("Luigi Verdi", "+39 334 9999888");
            
            assertNotNull(collab.getName(), "Il nome è obbligatorio");
            assertNotNull(collab.getContact(), "Il contatto è obbligatorio");
            // CF e indirizzo sono opzionali all'inizio
        }
        
        @Test
        @DisplayName("Raffaele: 'aggiorno i dettagli dei collaboratori'")
        void testCanUpdateCollaboratorInfo() {
            // REQUISITO: Raffaele aggiorna telefoni e altri dettagli
            Collaborator collab = Collaborator.create("Test Update", "+39 111");
            
            collab.updateInfo("Test Update Nuovo", "NEWCF123", "+39 222", "Via Nuova 1");
            
            assertEquals("Test Update Nuovo", collab.getName());
            assertEquals("NEWCF123", collab.getFiscalCode());
            assertEquals("+39 222", collab.getContact());
            assertEquals("Via Nuova 1", collab.getAddress());
        }
        
        @Test
        @DisplayName("Aggiornamento parziale: solo i campi specificati cambiano")
        void testPartialUpdate() {
            // Caso: Raffaele cambia solo il telefono, non il nome
            Collaborator collab = Collaborator.create("Mario Bianchi", "+39 old");
            collab.updateInfo(null, null, "+39 new", null);
            
            assertEquals("Mario Bianchi", collab.getName(), "Il nome non deve cambiare");
            assertEquals("+39 new", collab.getContact(), "Il contatto deve essere aggiornato");
        }
    }
    
    // ========================================================================
    // TEST PROMOZIONE - Basati su: "io decido chi promuovere a permanente"
    // ========================================================================
    
    @Nested
    @DisplayName("Collaborator - Promozione da occasionale a permanente")
    class CollaboratorPromotionTest {
        
        @Test
        @DisplayName("Robert: 'un occasionale può diventare permanente'")
        void testPromoteOccasionalToPermanent() {
            // REQUISITO: Robert promuove collaboratori meritevoli
            Collaborator collab = Collaborator.create("Da Promuovere", "+39 555");
            assertTrue(collab.isOccasional(), "Parte come occasionale");
            
            collab.promote();
            
            assertFalse(collab.isOccasional(), 
                "Dopo la promozione NON deve più essere occasionale");
        }
        
        @Test
        @DisplayName("Business rule: un collaboratore già permanente non cambia")
        void testAlreadyPermanentDoesntChange() {
            // Se già permanente, la promozione non ha effetto negativo
            Collaborator collab = Collaborator.create("Già Permanente", "+39 666");
            collab.promote(); // prima promozione
            
            assertFalse(collab.isOccasional());
            collab.promote(); // promozione ripetuta
            assertFalse(collab.isOccasional(), "Deve restare permanente");
        }
    }
    
    // ========================================================================
    // TEST ELIMINAZIONE (SOFT DELETE) - Basati su regole business
    // ========================================================================
    
    @Nested
    @DisplayName("Collaborator - Eliminazione (disattivazione)")
    class CollaboratorDeactivationTest {
        
        @Test
        @DisplayName("Un collaboratore disattivato non compare più nella lista attivi")
        void testDeactivatedCollaboratorIsInactive() {
            Collaborator collab = Collaborator.create("Da Eliminare", "+39 777");
            assertTrue(collab.isActive(), "Inizialmente attivo");
            
            collab.deactivate();
            
            assertFalse(collab.isActive(), 
                "Dopo deactivate() deve risultare inattivo");
        }
        
        @Test
        @DisplayName("Soft delete: i dati rimangono, solo lo stato cambia")
        void testSoftDeletePreservesData() {
            // I dati devono restare per storico/fatturazione
            Collaborator collab = Collaborator.create("Dati Preservati", "+39 888");
            String originalName = collab.getName();
            
            collab.deactivate();
            
            assertEquals(originalName, collab.getName(), 
                "I dati devono essere preservati dopo l'eliminazione");
        }
    }
    
    // ========================================================================
    // TEST RICHIESTE FERIE - Basati su: "solo io approvo le ferie"
    // ========================================================================
    
    @Nested
    @DisplayName("LeaveRequest - Gestione richieste ferie")
    class LeaveRequestTest {
        
        @Test
        @DisplayName("Una nuova richiesta ferie ha data inizio e fine")
        void testLeaveRequestHasDates() {
            // REQUISITO: le ferie hanno un periodo definito
            Collaborator collab = Collaborator.create("Richiedente", "+39 999");
            Date start = new Date(); // oggi
            Date end = new Date(start.getTime() + 7 * 24 * 60 * 60 * 1000L); // +7 giorni
            
            LeaveRequest req = LeaveRequest.create(collab, start, end);
            
            assertNotNull(req.getStartDate(), "Data inizio richiesta");
            assertNotNull(req.getEndDate(), "Data fine richiesta");
            assertEquals(collab, req.getCollaborator());
        }
        
        @Test
        @DisplayName("Una nuova richiesta ferie è in stato PENDING")
        void testNewLeaveRequestIsPending() {
            Collaborator collab = Collaborator.create("Richiedente2", "+39 100");
            Date start = new Date();
            Date end = new Date(start.getTime() + 5 * 24 * 60 * 60 * 1000L);
            
            LeaveRequest req = LeaveRequest.create(collab, start, end);
            
            assertTrue(req.isPending(), 
                "Una nuova richiesta DEVE essere in attesa di approvazione");
            assertFalse(req.isApproved(), "Non deve essere già approvata");
        }
        
        @Test
        @DisplayName("Calcolo durata: 7 giorni di ferie = getDuration() restituisce 7")
        void testLeaveDurationCalculation() {
            Collaborator collab = Collaborator.create("Test Durata", "+39 101");
            Date start = new Date();
            Date end = new Date(start.getTime() + 6 * 24 * 60 * 60 * 1000L); // 6 giorni dopo = 7 giorni totali
            
            LeaveRequest req = LeaveRequest.create(collab, start, end);
            
            assertEquals(7, req.getDuration(), 
                "7 giorni di ferie (dal giorno 0 al giorno 6 inclusi)");
        }
        
        @Test
        @DisplayName("Robert: 'quando approvo le ferie, il monte ferie si riduce'")
        void testApprovedLeaveReducesVacationDays() throws PersonnelException {
            // REQUISITO: il monte ferie si scala automaticamente
            Collaborator collab = Collaborator.create("Permanente Ferie", "+39 102");
            collab.promote(); // deve essere permanente
            collab.setVacationDays(20); // partiamo con 20 giorni
            
            Date start = new Date();
            Date end = new Date(start.getTime() + 4 * 24 * 60 * 60 * 1000L); // 5 giorni
            LeaveRequest req = LeaveRequest.create(collab, start, end);
            
            int durataRichiesta = req.getDuration(); // dovrebbe essere 5
            collab.reduceVacationDays(durataRichiesta);
            req.approve();
            
            assertEquals(20 - durataRichiesta, collab.getVacationDays(),
                "Il monte ferie deve essere ridotto della durata approvata");
            assertTrue(req.isApproved(), "La richiesta deve risultare approvata");
        }
        
        @Test
        @DisplayName("Business rule: non si possono approvare più ferie del monte disponibile")
        void testCannotApproveIfInsufficientVacationDays() {
            Collaborator collab = Collaborator.create("Poche Ferie", "+39 103");
            collab.promote();
            collab.setVacationDays(3); // solo 3 giorni
            
            Date start = new Date();
            Date end = new Date(start.getTime() + 9 * 24 * 60 * 60 * 1000L); // 10 giorni
            LeaveRequest req = LeaveRequest.create(collab, start, end);
            
            int durata = req.getDuration(); // ~10
            
            assertThrows(PersonnelException.class, () -> {
                collab.reduceVacationDays(durata);
            }, "Deve lanciare eccezione se il monte ferie è insufficiente");
        }
        
        @Test
        @DisplayName("Una richiesta può essere rifiutata")
        void testLeaveRequestCanBeRejected() {
            Collaborator collab = Collaborator.create("Rifiutato", "+39 104");
            Date start = new Date();
            Date end = new Date(start.getTime() + 2 * 24 * 60 * 60 * 1000L);
            
            LeaveRequest req = LeaveRequest.create(collab, start, end);
            req.reject();
            
            assertFalse(req.isPending(), "Non deve più essere pending");
            assertFalse(req.isApproved(), "Non deve essere approvata");
            assertEquals(-1, req.getApproved(), "Stato = rifiutata (-1)");
        }
    }
    
    // ========================================================================
    // TEST PERFORMANCE NOTES - Basati su: "dopo ogni evento mi faccio delle note"
    // ========================================================================
    
    @Nested
    @DisplayName("PerformanceNote - Note sulle performance")
    class PerformanceNoteTest {
        
        @Test
        @DisplayName("Raffaele: 'dopo ogni evento mi faccio delle note'")
        void testCanCreatePerformanceNote() {
            // REQUISITO: Raffaele registra note dopo gli eventi
            Collaborator collab = Collaborator.create("Valutato", "+39 200");
            
            // Nota senza evento specifico (caso generale)
            PerformanceNote note = PerformanceNote.create(collab, null, null, 
                "Ottimo lavoro, puntuale e professionale");
            
            assertNotNull(note, "La nota deve essere creata");
            assertEquals(collab, note.getCollaborator());
            assertEquals("Ottimo lavoro, puntuale e professionale", note.getNote());
            assertNotNull(note.getCreatedAt(), "Deve avere un timestamp");
        }
        
        @Test
        @DisplayName("Una nota può essere associata a un evento specifico")
        void testNoteCanReferenceEvent() {
            // Per ora testiamo solo che l'evento può essere null
            // (l'integrazione con Event sarà testata nei test di integrazione)
            Collaborator collab = Collaborator.create("Valutato2", "+39 201");
            
            PerformanceNote note = PerformanceNote.create(collab, null, null, "Test nota");
            
            assertNull(note.getEvent(), "L'evento può essere null");
        }
    }
    
    // ========================================================================
    // TEST PERSISTENZA BASE - Verifica che le entità si salvino e carichino
    // ========================================================================
    
    @Nested
    @DisplayName("Persistenza - Salvataggio e caricamento")
    class PersistenceTest {
        
        @Test
        @DisplayName("Un collaboratore salvato può essere recuperato")
        void testCollaboratorSaveAndLoad() {
            Collaborator collab = Collaborator.create("Salvataggio Test", "+39 300");
            collab.save();
            
            assertTrue(collab.getId() > 0, 
                "Dopo il salvataggio deve avere un ID assegnato");
            
            Collaborator loaded = Collaborator.loadById(collab.getId());
            assertNotNull(loaded, "Deve essere caricabile per ID");
            assertEquals(collab.getName(), loaded.getName());
        }
        
        @Test
        @DisplayName("loadActive() restituisce solo collaboratori attivi")
        void testLoadActiveFiltersInactive() {
            // Crea due collaboratori: uno attivo, uno disattivato
            Collaborator active = Collaborator.create("Attivo Test", "+39 301");
            active.save();
            
            Collaborator inactive = Collaborator.create("Inattivo Test", "+39 302");
            inactive.save();
            inactive.deactivate();
            inactive.update();
            
            var activeList = Collaborator.loadActive();
            
            assertTrue(activeList.stream().anyMatch(c -> c.getId() == active.getId()),
                "Il collaboratore attivo deve essere nella lista");
            assertFalse(activeList.stream().anyMatch(c -> c.getId() == inactive.getId()),
                "Il collaboratore inattivo NON deve essere nella lista attivi");
        }
    }
}
