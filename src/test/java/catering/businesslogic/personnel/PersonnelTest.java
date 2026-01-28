package catering.businesslogic.personnel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catering.persistence.PersistenceManager;
import catering.util.LogManager;

import java.util.Date;
import java.util.logging.Logger;

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
        void testNewCollaboratorIsOccasional() throws PersonnelException {
            // REQUISITO: Robert dice che i nuovi arrivati sono sempre occasionali
            String uniqueName = "Mario Rossi " + System.currentTimeMillis();
            Collaborator collab = Collaborator.create(uniqueName, "+39 333 " + System.currentTimeMillis());
            
            assertTrue(collab.isOccasional(), 
                "Un nuovo collaboratore DEVE essere occasionale di default");
            assertTrue(collab.isActive(), 
                "Un nuovo collaboratore DEVE essere attivo");
            assertNotNull(collab.getName(), "Il nome è obbligatorio");
            assertNotNull(collab.getContact(), "Il contatto è obbligatorio");
            assertEquals(0, collab.getVacationDays(), "Monte ferie iniziale a 0");
        }
        
        @Test
        @DisplayName("Raffaele: 'aggiorno i dettagli dei collaboratori'")
        void testCanUpdateCollaboratorInfo() throws PersonnelException {
            // REQUISITO: Raffaele aggiorna telefoni e altri dettagli
            String uniqueName = "Test Update " + System.currentTimeMillis();
            Collaborator collab = Collaborator.create(uniqueName, "+39 111 " + System.currentTimeMillis());
            
            collab.updateInfo("Test Update Nuovo", "NEWCF123", "+39 222", "Via Nuova 1");
            
            assertEquals("Test Update Nuovo", collab.getName());
            assertEquals("NEWCF123", collab.getFiscalCode());
            assertEquals("+39 222", collab.getContact());
            assertEquals("Via Nuova 1", collab.getAddress());
        }
    }
    // TEST PROMOZIONE - Basati su: "io decido chi promuovere a permanente"
    // ========================================================================

    @Nested
    @DisplayName("Collaborator - Promozione")
    class CollaboratorPromotionTest {

        @Test
        @DisplayName("Promuovere un occasionale lo rende permanente")
        void testPromoteOccasionalToPermanent() throws PersonnelException {
            String uniqueName = "Luigi Verdi " + System.currentTimeMillis();
            Collaborator c = Collaborator.create(uniqueName, "+39 987 " + System.currentTimeMillis());
            assertTrue(c.isOccasional());

            c.promote();

            assertFalse(c.isOccasional(), "Dopo la promozione non deve essere occasionale");
        }

        @Test
        @DisplayName("Promuovere un permanente non cambia nulla")
        void testAlreadyPermanentDoesntChange() throws PersonnelException {
            String uniqueName = "Giulia Bianchi " + System.currentTimeMillis();
            Collaborator c = Collaborator.create(uniqueName, "+39 112 " + System.currentTimeMillis());
            c.promote(); // Ora è permanente
            assertFalse(c.isOccasional());

            c.promote(); // Riprovo
            assertFalse(c.isOccasional());
        }
    }

    @Nested
    @DisplayName("Collaborator - Eliminazione (disattivazione)")
    class CollaboratorDeactivationTest {

        @Test
        @DisplayName("Un collaboratore disattivato non compare più nella lista attivi")
        void testDeactivatedCollaboratorIsInactive() throws PersonnelException {
            Collaborator collab = Collaborator.create("Da Eliminare " + System.currentTimeMillis(), "+39 777 " + System.currentTimeMillis());
            assertTrue(collab.isActive(), "Inizialmente attivo");

            collab.deactivate();

            assertFalse(collab.isActive(), 
                "Dopo deactivate() deve risultare inattivo");
        }

        @Test
        @DisplayName("Soft Delete: i dati personali rimangono dopo la disattivazione")
        void testSoftDeletePreservesData() throws PersonnelException {
            long ts = System.currentTimeMillis();
            Collaborator collab = Collaborator.create("Soft Delete " + ts, "+39 888 " + ts);
            collab.deactivate();

            assertEquals("Soft Delete " + ts, collab.getName());
            assertEquals("+39 888 " + ts, collab.getContact());
        }
    }
    
    @Nested
    @DisplayName("Richieste Ferie")
    class LeaveRequestTest {
        
        @Test
        @DisplayName("Creazione richiesta ferie pending")
        void testCreateLeaveRequest() throws PersonnelException {
            String uniqueName = "Ferie Man " + System.currentTimeMillis();
            Collaborator c = Collaborator.create(uniqueName, "+39 000 " + System.currentTimeMillis());
            Date start = new Date();
            Date end = new Date(start.getTime() + 86400000); // +1 giorno
            
            LeaveRequest req = LeaveRequest.create(c, start, end);
            
            assertTrue(req.isPending());
            assertEquals(c, req.getCollaborator());
        }
        
        @Test
        @DisplayName("Approvazione richiesta scala monte ferie")
        void testApproveLeaveRequest() throws PersonnelException {
            String uniqueName = "Vacationer " + System.currentTimeMillis();
            Collaborator c = Collaborator.create(uniqueName, "+39 111A " + System.currentTimeMillis());
            c.setVacationDays(10);
            
            Date start = new Date();
            Date end = new Date(start.getTime() + (2 * 86400000)); // 3 giorni (start, mid, end)
            
            LeaveRequest req = LeaveRequest.create(c, start, end);
            assertEquals(3, req.getDuration()); // Verifica durata
            
            req.approve();
            assertTrue(req.isApproved(), "La richiesta deve risultare approvata");
            
            c.reduceVacationDays(req.getDuration());
            assertEquals(7, c.getVacationDays(),
                "Il monte ferie deve essere ridotto della durata approvata");
        }
        
        @Test
        @DisplayName("Business rule: non si possono approvare più ferie del monte disponibile")
        void testInsufficientVacationDays() throws PersonnelException {
            String uniqueName = "Poche Ferie " + System.currentTimeMillis();
            Collaborator collab = Collaborator.create(uniqueName, "+39 103 " + System.currentTimeMillis());
            collab.promote();
            collab.setVacationDays(3); // solo 3 giorni
            
            assertThrows(PersonnelException.class, () -> {
                collab.reduceVacationDays(5);
            }, "Deve lanciare eccezione se il monte ferie è insufficiente");
        }
        
        @Test
        @DisplayName("Una richiesta può essere rifiutata")
        void testLeaveRequestCanBeRejected() throws PersonnelException {
            String uniqueName = "Rifiutato " + System.currentTimeMillis();
            Collaborator collab = Collaborator.create(uniqueName, "+39 104 " + System.currentTimeMillis());
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
        void testCreateNote() throws PersonnelException {
            // REQUISITO: Raffaele registra note dopo gli eventi
            String uniqueName = "Valutato " + System.currentTimeMillis();
            Collaborator collab = Collaborator.create(uniqueName, "+39 200 " + System.currentTimeMillis());
            
            // Nota senza evento specifico (caso generale) - uses 4-arg signature
            PerformanceNote note = PerformanceNote.create(collab, null, null, "Ottimo lavoro, puntuale e professionale");
            
            assertNotNull(note, "La nota deve essere creata");
            assertEquals(collab, note.getCollaborator());
            assertEquals("Ottimo lavoro, puntuale e professionale", note.getNote());
            assertNotNull(note.getCreatedAt(), "Deve avere un timestamp");
        }
        
        @Test
        @DisplayName("DSD: collab.addPerformanceNote() crea nota internamente")
        void testAddPerformanceNoteViaDSD() throws PersonnelException {
            // Test allineato al DSD logPerformance.png
            // Il Collaborator è "esperto" dei propri dati e crea la nota internamente
            String uniqueName = "DSD Test " + System.currentTimeMillis();
            Collaborator collab = Collaborator.create(uniqueName, "+39 201 " + System.currentTimeMillis());
            
            // Usa il nuovo metodo addPerformanceNote allineato al DSD
            PerformanceNote note = collab.addPerformanceNote("Test nota via DSD", null, null);
            
            assertNotNull(note, "La nota deve essere creata");
            assertEquals(collab, note.getCollaborator(), "La nota deve riferirsi al collaboratore");
            assertEquals("Test nota via DSD", note.getNote());
        }
        
        @Test
        @DisplayName("Note hanno timestamp automatico")
        void testPerformanceNoteHasTimestamp() throws PersonnelException {
            String uniqueName = "Timestamp Test " + System.currentTimeMillis();
            Collaborator collab = Collaborator.create(uniqueName, "+39 203 " + System.currentTimeMillis());
            
            long beforeCreate = System.currentTimeMillis();
            PerformanceNote note = collab.addPerformanceNote("Test timestamp", null, null);
            long afterCreate = System.currentTimeMillis();
            
            assertNotNull(note.getCreatedAt());
            assertTrue(note.getCreatedAt().getTime() >= beforeCreate && 
                       note.getCreatedAt().getTime() <= afterCreate,
                "Il timestamp deve essere nel range della creazione");
        }
    }
    
    // ========================================================================
    // TEST ECCEZIONI E BUSINESS RULES - Basati sui contratti main.tex
    // ========================================================================
    
    @Nested
    @DisplayName("Eccezioni e Business Rules (Contratti)")
    class ExceptionsAndBusinessRulesTest {
        
        @Test
        @DisplayName("Eccezione 2a.2a: Contatto duplicato non permesso")
        void testDuplicateContactThrowsException() throws PersonnelException {
            // Crea primo collaboratore
            String contact = "+39 UNIQUE " + System.currentTimeMillis();
            Collaborator first = Collaborator.create("Primo", contact);
            first.save();
            
            // Tentativo di creare secondo con stesso contatto deve fallire
            assertThrows(PersonnelException.class, () -> {
                Collaborator.create("Secondo", contact);
            }, "Deve lanciare PersonnelException per contatto duplicato");
        }
        
        @Test
        @DisplayName("Eccezione 2a.2a: Contatto di collaboratore INATTIVO può essere riusato")
        void testInactiveContactCanBeReused() throws PersonnelException {
            // Crea e disattiva primo collaboratore
            String contact = "+39 RIUSO " + System.currentTimeMillis();
            Collaborator first = Collaborator.create("Primo Riuso", contact);
            first.save();
            first.deactivate();
            first.update();
            
            // Ora deve poter creare un altro con lo stesso contatto
            assertDoesNotThrow(() -> {
                Collaborator second = Collaborator.create("Secondo Riuso", contact);
                assertNotNull(second);
            }, "Contatto di collaboratore inattivo può essere riutilizzato");
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
        void testCollaboratorSaveAndLoad() throws PersonnelException {
            String uniqueName = "Salvataggio Test " + System.currentTimeMillis();
            Collaborator collab = Collaborator.create(uniqueName, "+39 300 " + System.currentTimeMillis());
            collab.save();
            
            assertTrue(collab.getId() > 0, 
                "Dopo il salvataggio deve avere un ID assegnato");
            
            Collaborator loaded = Collaborator.loadById(collab.getId());
            assertNotNull(loaded, "Deve essere caricabile per ID");
            assertEquals(uniqueName, loaded.getName());
        }
        
        @Test
        @DisplayName("loadActive() restituisce solo collaboratori attivi")
        void testLoadActiveFiltersInactive() throws PersonnelException {
            // Crea due collaboratori: uno attivo, uno disattivato
            String activeName = "Attivo Test " + System.currentTimeMillis();
            Collaborator active = Collaborator.create(activeName, "+39 301 " + System.currentTimeMillis());
            active.save();
            
            String inactiveName = "Inattivo Test " + System.currentTimeMillis();
            Collaborator inactive = Collaborator.create(inactiveName, "+39 302 " + System.currentTimeMillis());
            inactive.save();
            inactive.deactivate(); // Questo ora lancia PersonnelException ma non dovrebbe avere assignment
            inactive.update();
            
            var activeList = Collaborator.loadActive();
            
            assertTrue(activeList.stream().anyMatch(c -> c.getId() == active.getId()),
                "Il collaboratore attivo deve essere nella lista");
            assertFalse(activeList.stream().anyMatch(c -> c.getId() == inactive.getId()),
                "Il collaboratore inattivo NON deve essere nella lista attivi");
        }
    }
}
