package catering.businesslogic.personnel;

import catering.businesslogic.event.Event;
import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Rappresenta una Nota sulle Performance di un collaboratore.
 * 
 * Mappa l'entità "Performance" del Modello di Dominio (vedi MD_3UC-U3.drawio.png).
 * 
 * Attributi dal MD:
 * - note: testo della valutazione
 * 
 * Come descritto nelle interviste:
 * - Raffaele: "dopo ogni evento mi faccio delle note"
 * - Robert: "consulto lo storico degli eventi e le note sulle performance"
 * 
 * @see main.tex Estensione 3b.1 e DSD logPerformance
 */
public class PerformanceNote {
    
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private int id;
    private Collaborator collaborator;
    private Event event;           // opzionale
    private User author;           // chi ha scritto la nota
    private String note;
    private Date createdAt;
    
    // ==================== COSTRUTTORI ====================
    
    public PerformanceNote() {
        this.createdAt = new Date();
    }
    
    // Helper per escape SQL
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Crea una nuova nota performance.
     * 
     * Corrisponde al DSD logPerformance.png:
     * - PersonnelManager.logPerformance(collab, event, text) chiama
     * - Note.create(text, event) -> newNote
     * - Collaborator.addPerformanceNote(newNote)
     * 
     * @param collaborator Collaboratore valutato
     * @param event Evento opzionale a cui si riferisce
     * @param author Utente che scrive la nota
     * @param note Testo della valutazione
     */
    public static PerformanceNote create(Collaborator collaborator, Event event, User author, String note) {
        PerformanceNote pn = new PerformanceNote();
        pn.collaborator = collaborator;
        pn.event = event;
        pn.author = author;
        pn.note = note;
        pn.createdAt = new Date();
        return pn;
    }
    
    // ==================== PERSISTENZA ====================
    
    /**
     * Salva la nota nel database.
     */
    public void save() {
        String createdStr = DATETIME_FORMAT.format(createdAt);
        
        String query = "INSERT INTO PerformanceNotes (collaborator_id, event_id, author_id, note, created_at) " +
                      "VALUES (" + collaborator.getId() + ", " +
                      (event != null ? event.getId() : "NULL") + ", " +
                      author.getId() + ", " +
                      "'" + escape(note) + "', " +
                      "'" + createdStr + "')";
        
        PersistenceManager.executeUpdate(query);
        this.id = PersistenceManager.getLastId();
    }
    
    /**
     * Carica tutte le note di un collaboratore.
     * Usato per visualizzaStoricoPerformance (estensione 3b.1).
     */
    public static ArrayList<PerformanceNote> loadByCollaborator(Collaborator collab) {
        ArrayList<PerformanceNote> result = new ArrayList<>();
        String query = "SELECT * FROM PerformanceNotes WHERE collaborator_id = " + collab.getId() + 
                      " ORDER BY created_at DESC";
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                PerformanceNote pn = new PerformanceNote();
                pn.id = rs.getInt("id");
                pn.collaborator = collab;
                
                int eventId = rs.getInt("event_id");
                if (!rs.wasNull()) {
                    pn.event = Event.loadById(eventId);
                }
                
                pn.author = User.load(rs.getInt("author_id"));
                pn.note = rs.getString("note");
                
                String createdStr = rs.getString("created_at");
                if (createdStr != null) {
                    try {
                        pn.createdAt = DATETIME_FORMAT.parse(createdStr);
                    } catch (Exception e) {
                        pn.createdAt = new Date();
                    }
                }
                
                result.add(pn);
            }
        });
        return result;
    }
    
    /**
     * Carica tutte le note relative a un evento.
     */
    public static ArrayList<PerformanceNote> loadByEvent(Event event) {
        ArrayList<PerformanceNote> result = new ArrayList<>();
        String query = "SELECT * FROM PerformanceNotes WHERE event_id = " + event.getId() + 
                      " ORDER BY created_at DESC";
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                PerformanceNote pn = new PerformanceNote();
                pn.id = rs.getInt("id");
                pn.collaborator = Collaborator.loadById(rs.getInt("collaborator_id"));
                pn.event = event;
                pn.author = User.load(rs.getInt("author_id"));
                pn.note = rs.getString("note");
                
                String createdStr = rs.getString("created_at");
                if (createdStr != null) {
                    try {
                        pn.createdAt = DATETIME_FORMAT.parse(createdStr);
                    } catch (Exception e) {
                        pn.createdAt = new Date();
                    }
                }
                
                result.add(pn);
            }
        });
        return result;
    }
    
    // ==================== GETTERS ====================
    
    public int getId() { return id; }
    public Collaborator getCollaborator() { return collaborator; }
    public Event getEvent() { return event; }
    public User getAuthor() { return author; }
    public String getNote() { return note; }
    public Date getCreatedAt() { return createdAt; }
    
    // ==================== UTILITY ====================
    
    @Override
    public String toString() {
        return "PerformanceNote{" +
                "id=" + id +
                ", collaborator=" + (collaborator != null ? collaborator.getName() : "null") +
                ", event=" + (event != null ? event.getName() : "null") +
                ", author=" + (author != null ? author.getUserName() : "null") +
                ", note='" + (note.length() > 50 ? note.substring(0, 50) + "..." : note) + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
