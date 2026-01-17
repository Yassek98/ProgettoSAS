package catering.businesslogic.personnel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;

import java.util.Date;
import java.util.logging.Logger;
import catering.util.LogManager;

/**
 * Test di Integrazione per PersonnelManager.
 * 
 * Testa il flusso completo con login utente e verifica dei permessi.
 * 
 * Utenti dal database:
 * - Giovanni (id=7) = PROPRIETARIO (può fare tutto)
 * - Chiara (id=6) = ORGANIZZATORE (può solo modificare/eliminare/logPerformance)
 * - Francesca (id=8) = ORGANIZZATORE
 * - Luca (id=3) = CUOCO (non può fare nulla)
 * 
 * @see main.tex per le eccezioni: 2a.1a, 3b.2a, 3c.3a
 */
@DisplayName("Personnel Integration - Test con Login e Permessi")
public class PersonnelIntegrationTest {

    private static final Logger LOGGER = LogManager.getLogger(PersonnelIntegrationTest.class);
    private static CatERing app;
    
    @BeforeAll
    static void init() {
        PersistenceManager.initializeDatabase("database/catering_init_sqlite.sql");
        app = CatERing.getInstance();
        LOGGER.info("Applicazione inizializzata per test integrazione");
    }
    
    // ========================================================================
    // TEST OWNER - Giovanni (PROPRIETARIO)
    // ========================================================================
    
    @Nested
    @DisplayName("Giovanni (PROPRIETARIO) - Operazioni Owner-only")
    class OwnerOperationsTest {
        
        @BeforeEach
        void loginAsOwner() throws UseCaseLogicException {
            // Login come Giovanni (PROPRIETARIO)
            app.getUserManager().fakeLogin("Giovanni");
            User currentUser = app.getUserManager().getCurrentUser();
            assertNotNull(currentUser, "Giovanni deve essere caricato");
            assertTrue(currentUser.isOwner(), "Giovanni deve essere PROPRIETARIO (roles=" + currentUser.getRoles() + ")");
            LOGGER.info("Logged in as: " + currentUser.getUserName() + " (isOwner=" + currentUser.isOwner() + ")");
        }
        
        @Test
        @DisplayName("addCollaborator: Owner può aggiungere collaboratori (estensione 2a)")
        void testAddCollaborator_AsOwner_Success() throws UseCaseLogicException {
            PersonnelManager pm = app.getPersonnelManager();
            
            // Act
            Collaborator newCollab = pm.addCollaborator("Test Owner Add", "+39 111222333");
            
            // Assert
            assertNotNull(newCollab, "Il nuovo collaboratore deve essere creato");
            assertTrue(newCollab.getId() > 0, "Deve avere un ID assegnato");
            assertEquals("Test Owner Add", newCollab.getName());
            assertTrue(newCollab.isOccasional(), "Deve partire come occasionale");
            assertTrue(newCollab.isActive(), "Deve essere attivo");
            
            LOGGER.info("Collaboratore aggiunto: " + newCollab);
        }
        
        @Test
        @DisplayName("promoteCollaborator: Owner può promuovere (estensione 3b.2)")
        void testPromoteCollaborator_AsOwner_Success() throws UseCaseLogicException {
            PersonnelManager pm = app.getPersonnelManager();
            
            // Arrange: crea un collaboratore occasionale
            Collaborator collab = pm.addCollaborator("Da Promuovere Owner", "+39 444");
            assertTrue(collab.isOccasional());
            
            // Act
            pm.promoteCollaborator(collab);
            
            // Assert
            assertFalse(collab.isOccasional(), "Deve essere permanente dopo la promozione");
            LOGGER.info("Collaboratore promosso: " + collab);
        }
        
        @Test
        @DisplayName("evaluateLeaveRequest: Owner può approvare ferie (estensione 3c.3)")
        void testEvaluateLeaveRequest_AsOwner_Success() throws UseCaseLogicException, PersonnelException {
            PersonnelManager pm = app.getPersonnelManager();
            
            // Arrange
            Collaborator collab = pm.addCollaborator("Ferie Test", "+39 555");
            collab.promote();
            collab.setVacationDays(20);
            collab.update();
            
            Date start = new Date();
            Date end = new Date(start.getTime() + 4 * 24 * 60 * 60 * 1000L); // 5 giorni
            LeaveRequest req = LeaveRequest.create(collab, start, end);
            req.save();
            
            int initialDays = collab.getVacationDays();
            int duration = req.getDuration();
            
            // Act
            pm.evaluateLeaveRequest(req, true);
            
            // Assert
            assertTrue(req.isApproved());
            assertEquals(initialDays - duration, collab.getVacationDays());
            LOGGER.info("Ferie approvate. Monte ferie: " + initialDays + " -> " + collab.getVacationDays());
        }
    }
    
    // ========================================================================
    // TEST ORGANIZER - Chiara (solo ORGANIZZATORE)
    // ========================================================================
    
    @Nested
    @DisplayName("Chiara (ORGANIZZATORE) - Operazioni Organizer")
    class OrganizerOperationsTest {
        
        @BeforeEach
        void loginAsOrganizer() throws UseCaseLogicException {
            // Login come Chiara (ORGANIZZATORE, non PROPRIETARIO)
            app.getUserManager().fakeLogin("Chiara");
            User currentUser = app.getUserManager().getCurrentUser();
            assertNotNull(currentUser, "Chiara deve essere caricata");
            assertTrue(currentUser.isOrganizer(), "Chiara deve essere ORGANIZZATORE (roles=" + currentUser.getRoles() + ")");
            assertFalse(currentUser.isOwner(), "Chiara NON deve essere PROPRIETARIO (roles=" + currentUser.getRoles() + ")");
            LOGGER.info("Logged in as: " + currentUser.getUserName() + 
                       " (isOrganizer=" + currentUser.isOrganizer() + ", isOwner=" + currentUser.isOwner() + ")");
        }
        
        @Test
        @DisplayName("addCollaborator: Organizer NON può aggiungere (eccezione 2a.1a)")
        void testAddCollaborator_AsOrganizer_Fails() {
            PersonnelManager pm = app.getPersonnelManager();
            
            // Act & Assert
            UseCaseLogicException exception = assertThrows(
                UseCaseLogicException.class,
                () -> pm.addCollaborator("Should Fail", "+39 000"),
                "Organizzatore non può aggiungere collaboratori"
            );
            
            assertTrue(exception.getMessage().contains("Proprietario"));
            LOGGER.info("Eccezione corretta: " + exception.getMessage());
        }
        
        @Test
        @DisplayName("updateCollaboratorInfo: Organizer può modificare info")
        void testUpdateInfo_AsOrganizer_Success() throws UseCaseLogicException {
            PersonnelManager pm = app.getPersonnelManager();
            
            // Arrange: usa un collaboratore esistente dal DB
            var collaborators = pm.getCollaboratorList();
            assertFalse(collaborators.isEmpty(), "Devono esserci collaboratori");
            Collaborator collab = collaborators.get(0);
            
            // Act
            pm.updateCollaboratorInfo(collab, null, null, "+39 999888777", null);
            
            // Assert
            assertEquals("+39 999888777", collab.getContact());
            LOGGER.info("Info aggiornate: " + collab);
        }
        
        @Test
        @DisplayName("promoteCollaborator: Organizer NON può promuovere (eccezione 3b.2a)")
        void testPromoteCollaborator_AsOrganizer_Fails() {
            PersonnelManager pm = app.getPersonnelManager();
            
            // Arrange: trova un collaboratore occasionale
            var collaborators = pm.getCollaboratorList();
            Collaborator occasional = collaborators.stream()
                    .filter(Collaborator::isOccasional)
                    .findFirst()
                    .orElse(null);
            
            if (occasional != null) {
                // Act & Assert
                UseCaseLogicException exception = assertThrows(
                    UseCaseLogicException.class,
                    () -> pm.promoteCollaborator(occasional),
                    "Organizzatore non può promuovere"
                );
                
                assertTrue(exception.getMessage().contains("Proprietario"));
                LOGGER.info("Eccezione corretta: " + exception.getMessage());
            }
        }
        
        @Test
        @DisplayName("logPerformance: Organizer può loggare performance")
        void testLogPerformance_AsOrganizer_Success() throws UseCaseLogicException {
            PersonnelManager pm = app.getPersonnelManager();
            
            // Arrange
            var collaborators = pm.getCollaboratorList();
            assertFalse(collaborators.isEmpty());
            Collaborator collab = collaborators.get(0);
            
            // Act
            PerformanceNote note = pm.logPerformance(collab, null, "Ottimo lavoro durante l'evento!");
            
            // Assert
            assertNotNull(note);
            assertTrue(note.getId() > 0, "Deve avere un ID assegnato");
            assertEquals("Ottimo lavoro durante l'evento!", note.getNote());
            LOGGER.info("Performance loggata: " + note);
        }
        
        @Test
        @DisplayName("removeCollaborator: Organizer può eliminare (se senza incarichi)")
        void testRemoveCollaborator_AsOrganizer_Success() throws UseCaseLogicException {
            PersonnelManager pm = app.getPersonnelManager();
            
            // Prima devo fare login come Owner per creare un collaboratore da eliminare
            app.getUserManager().fakeLogin("Giovanni");
            Collaborator toRemove = pm.addCollaborator("Da Eliminare Org", "+39 666");
            
            // Torno come Organizer
            app.getUserManager().fakeLogin("Chiara");
            
            // Act
            pm.removeCollaborator(toRemove);
            
            // Assert
            assertFalse(toRemove.isActive(), "Deve essere inattivo");
            LOGGER.info("Collaboratore eliminato: " + toRemove);
        }
    }
    
    // ========================================================================
    // TEST CUOCO - Luca (nessun permesso per Personnel)
    // ========================================================================
    
    @Nested
    @DisplayName("Luca (CUOCO) - Nessun permesso Personnel")
    class CookOperationsTest {
        
        @BeforeEach
        void loginAsCook() throws UseCaseLogicException {
            // Login come Luca (CUOCO)
            app.getUserManager().fakeLogin("Luca");
            User currentUser = app.getUserManager().getCurrentUser();
            assertNotNull(currentUser, "Luca deve essere caricato");
            assertFalse(currentUser.isOrganizer(), "Luca NON deve essere ORGANIZZATORE");
            assertFalse(currentUser.isOwner(), "Luca NON deve essere PROPRIETARIO");
            LOGGER.info("Logged in as: " + currentUser.getUserName());
        }
        
        @Test
        @DisplayName("updateCollaboratorInfo: Cuoco NON può modificare")
        void testUpdateInfo_AsCook_Fails() {
            PersonnelManager pm = app.getPersonnelManager();
            
            var collaborators = Collaborator.loadActive();
            if (!collaborators.isEmpty()) {
                Collaborator collab = collaborators.get(0);
                
                UseCaseLogicException exception = assertThrows(
                    UseCaseLogicException.class,
                    () -> pm.updateCollaboratorInfo(collab, "NuovoNome", null, null, null),
                    "Cuoco non può modificare collaboratori"
                );
                
                assertTrue(exception.getMessage().contains("Organizzatori"));
                LOGGER.info("Eccezione corretta: " + exception.getMessage());
            }
        }
    }
}
