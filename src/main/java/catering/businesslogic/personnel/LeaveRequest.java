package catering.businesslogic.personnel;

import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Rappresenta una Richiesta di Ferie nel sistema.
 * 
 * Mappa l'entità "RichiestaFerie" del Modello di Dominio (vedi MD_3UC-U3.drawio.png).
 * 
 * Attributi dal MD:
 * - dataInizio, dataFine: periodo di ferie richiesto
 * - approvata: stato della richiesta (pending, approvata, rifiutata)
 * 
 * Relazione: "avanzata da" Collaboratore
 * 
 * @see main.tex Estensione 3c per il flusso di gestione ferie
 */
public class LeaveRequest {
    
    private static final SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    private int id;
    private Collaborator collaborator;
    private Date startDate;
    private Date endDate;
    private int approved;        // 0=pending, 1=approved, -1=rejected
    private Date requestDate;
    
    // ==================== COSTRUTTORI ====================
    
    public LeaveRequest() {
        this.approved = 0;  // pending by default
        this.requestDate = new Date();
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Crea una nuova richiesta di ferie per un collaboratore.
     */
    public static LeaveRequest create(Collaborator collaborator, Date startDate, Date endDate) {
        LeaveRequest req = new LeaveRequest();
        req.collaborator = collaborator;
        req.startDate = startDate;
        req.endDate = endDate;
        req.approved = 0;  // pending
        req.requestDate = new Date();
        return req;
    }
    
    // ==================== METODI ====================
    
    /**
     * Calcola la durata in giorni della richiesta.
     * Usato per verificare il monte ferie nel contratto 3c.3.
     */
    public int getDuration() {
        if (startDate == null || endDate == null) return 0;
        long diff = endDate.getTime() - startDate.getTime();
        return (int) (diff / (1000 * 60 * 60 * 24)) + 1;  // +1 per includere entrambi i giorni
    }
    
    /**
     * Verifica se la richiesta è ancora in attesa di approvazione.
     */
    public boolean isPending() {
        return approved == 0;
    }
    
    /**
     * Verifica se la richiesta è stata approvata.
     */
    public boolean isApproved() {
        return approved == 1;
    }
    
    /**
     * Approva la richiesta di ferie.
     * 
     * Post-condizione dal main.tex (contratto 3c.3):
     * - rich.approvata = 'si'
     */
    public void approve() {
        this.approved = 1;
    }
    
    /**
     * Rifiuta la richiesta di ferie.
     */
    public void reject() {
        this.approved = -1;
    }
    
    // ==================== PERSISTENZA ====================
    
    private static String dateToStr(Date d) {
        if (d == null) return null;
        return SQL_DATE_FORMAT.format(d);
    }
    
    private static Date strToDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return SQL_DATE_FORMAT.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Salva una nuova richiesta nel database.
     */
    public void save() {
        String startStr = dateToStr(startDate);
        String endStr = dateToStr(endDate);
        String reqStr = dateToStr(requestDate);
        
        String query = "INSERT INTO LeaveRequests (collaborator_id, start_date, end_date, approved, request_date) " +
                      "VALUES (" + collaborator.getId() + ", " +
                      "'" + startStr + "', " +
                      "'" + endStr + "', " +
                      approved + ", " +
                      "'" + reqStr + "')";
        
        PersistenceManager.executeUpdate(query);
        this.id = PersistenceManager.getLastId();
    }
    
    /**
     * Aggiorna una richiesta esistente (tipicamente dopo approvazione/rifiuto).
     */
    public void update() {
        String query = "UPDATE LeaveRequests SET approved = " + approved + " WHERE id = " + this.id;
        PersistenceManager.executeUpdate(query);
    }
    
    /**
     * Carica tutte le richieste pending dal database.
     */
    public static ArrayList<LeaveRequest> loadPending() {
        return loadByStatus(0);
    }
    
    /**
     * Carica le richieste di un collaboratore specifico.
     */
    public static ArrayList<LeaveRequest> loadByCollaborator(Collaborator collab) {
        ArrayList<LeaveRequest> result = new ArrayList<>();
        String query = "SELECT * FROM LeaveRequests WHERE collaborator_id = " + collab.getId() + " ORDER BY request_date DESC";
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                LeaveRequest req = new LeaveRequest();
                req.id = rs.getInt("id");
                req.collaborator = collab;
                req.startDate = strToDate(rs.getString("start_date"));
                req.endDate = strToDate(rs.getString("end_date"));
                req.approved = rs.getInt("approved");
                req.requestDate = strToDate(rs.getString("request_date"));
                result.add(req);
            }
        });
        return result;
    }
    
    /**
     * Carica una richiesta per ID.
     */
    public static LeaveRequest loadById(int id) {
        final LeaveRequest[] result = {null};
        String query = "SELECT * FROM LeaveRequests WHERE id = " + id;
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                LeaveRequest req = new LeaveRequest();
                req.id = rs.getInt("id");
                req.collaborator = Collaborator.loadById(rs.getInt("collaborator_id"));
                req.startDate = strToDate(rs.getString("start_date"));
                req.endDate = strToDate(rs.getString("end_date"));
                req.approved = rs.getInt("approved");
                req.requestDate = strToDate(rs.getString("request_date"));
                result[0] = req;
            }
        });
        return result[0];
    }
    
    private static ArrayList<LeaveRequest> loadByStatus(int status) {
        ArrayList<LeaveRequest> result = new ArrayList<>();
        String query = "SELECT * FROM LeaveRequests WHERE approved = " + status + " ORDER BY request_date DESC";
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                LeaveRequest req = new LeaveRequest();
                req.id = rs.getInt("id");
                req.collaborator = Collaborator.loadById(rs.getInt("collaborator_id"));
                req.startDate = strToDate(rs.getString("start_date"));
                req.endDate = strToDate(rs.getString("end_date"));
                req.approved = rs.getInt("approved");
                req.requestDate = strToDate(rs.getString("request_date"));
                result.add(req);
            }
        });
        return result;
    }
    
    // ==================== GETTERS ====================
    
    public int getId() { return id; }
    public Collaborator getCollaborator() { return collaborator; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public int getApproved() { return approved; }
    public Date getRequestDate() { return requestDate; }
    
    // ==================== UTILITY ====================
    
    @Override
    public String toString() {
        String status = approved == 0 ? "PENDING" : (approved == 1 ? "APPROVED" : "REJECTED");
        return "LeaveRequest{" +
                "id=" + id +
                ", collaborator=" + (collaborator != null ? collaborator.getName() : "null") +
                ", period=" + startDate + " to " + endDate +
                ", status=" + status +
                '}';
    }
}
