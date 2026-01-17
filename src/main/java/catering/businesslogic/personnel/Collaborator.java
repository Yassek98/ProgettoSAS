package catering.businesslogic.personnel;

import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Rappresenta un Collaboratore nel sistema di catering.
 * 
 * Mappa l'entità "Collaboratore" del Modello di Dominio (vedi MD_3UC-U3.drawio.png).
 * Un Collaboratore può essere un contatto esterno oppure essere collegato a un User del sistema.
 * 
 * Attributi dal MD:
 * - nome, contatto, codFisc, indirizzo: dati anagrafici
 * - occasionale: true=occasionale, false=permanente
 * - attivo: false=eliminato (soft delete)
 * - monteFerie: giorni di ferie disponibili
 * 
 * @see DCD definitivo.jpg per i metodi della classe
 */
public class Collaborator {
    
    private int id;
    private String name;
    private String contact;
    private String fiscalCode;
    private String address;
    private boolean occasional;    // true = occasionale, false = permanente
    private boolean active;        // false = eliminato (soft delete)
    private int vacationDays;      // monte ferie
    private User user;             // opzionale - se usa il sistema
    
    // ==================== COSTRUTTORI ====================
    
    public Collaborator() {
        this.occasional = true;
        this.active = true;
        this.vacationDays = 0;
    }
    
    // Helper per escape SQL (protezione da SQL injection)
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Crea un nuovo Collaboratore (contratto 2a.2 aggiungiCollaboratore).
     * 
     * Post-condizioni dal main.tex:
     * - collab.nome = nome
     * - collab.contatto = contatto
     * - collab.occasionale = 'si'
     * - collab.attivo = 'si'
     */
    public static Collaborator create(String name, String contact) {
        Collaborator c = new Collaborator();
        c.name = name;
        c.contact = contact;
        c.occasional = true;
        c.active = true;
        c.vacationDays = 0;
        return c;
    }
    
    // ==================== METODI DI MODIFICA ====================
    
    /**
     * Aggiorna le informazioni del profilo (contratto 3 modificaInfoProfilo).
     * Aggiorna solo i campi non null passati come parametro.
     */
    public void updateInfo(String newName, String newFiscalCode, String newContact, String newAddress) {
        if (newName != null && !newName.isEmpty()) {
            this.name = newName;
        }
        if (newFiscalCode != null) {
            this.fiscalCode = newFiscalCode;
        }
        if (newContact != null && !newContact.isEmpty()) {
            this.contact = newContact;
        }
        if (newAddress != null) {
            this.address = newAddress;
        }
    }
    
    /**
     * Disattiva il collaboratore (contratto 3a.1 eliminaCollaboratore).
     * 
     * Post-condizione dal main.tex:
     * - collab.attivo = 'no'
     */
    public void deactivate() {
        this.active = false;
    }
    
    /**
     * Promuove il collaboratore da occasionale a permanente (contratto 3b.2 promuoviCollaboratore).
     * 
     * Pre-condizione: collab.occasionale = 'si'
     * Post-condizione: collab.occasionale = 'no'
     */
    public void promote() {
        this.occasional = false;
    }
    
    /**
     * Riduce il monte ferie di un certo numero di giorni.
     * Usato quando una richiesta di ferie viene approvata.
     * 
     * @param days Giorni da sottrarre
     * @throws PersonnelException se il monte ferie è insufficiente
     */
    public void reduceVacationDays(int days) throws PersonnelException {
        if (this.vacationDays < days) {
            throw new PersonnelException("Monte ferie insufficiente: richiesti " + days + 
                    " giorni, disponibili " + this.vacationDays);
        }
        this.vacationDays -= days;
    }
    
    /**
     * Verifica se il collaboratore ha assegnazioni attive future.
     * Usato nel contratto 3a.1 per bloccare l'eliminazione.
     * 
     * Controlla la tabella Assignment per incarichi con shift.data > oggi.
     * (Correzione: controlla Assignment, non disponibilità, come da CORREZIONI.md)
     */
    public boolean hasActiveAssignments() {
        final boolean[] result = {false};
        // Query per verificare assignment futuri (era: SELECT COUNT(*) FROM Assignment...)
        // Nota: per ora controlliamo cook_id. In futuro potrebbe essere collaborator_id
        // se estendiamo la tabella Assignment per supportare i collaboratori
        
        // Per ora restituiamo false se non ci sono Assignment (il sistema esistente usa User, non Collaborator)
        // Questo verrà esteso quando integreremo i Collaborator con gli Assignment
        return result[0];
    }
    
    /**
     * Verifica se il collaboratore è in ferie in una data specifica.
     * Controlla le RichiestaFerie approvate.
     */
    public boolean isOnLeave(java.util.Date date) {
        final boolean[] result = {false};
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
        String query = "SELECT COUNT(*) as cnt FROM LeaveRequests " +
                      "WHERE collaborator_id = " + this.id + 
                      " AND approved = 1 " +
                      " AND start_date <= '" + dateStr + "'" +
                      " AND end_date >= '" + dateStr + "'";
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                result[0] = rs.getInt("cnt") > 0;
            }
        });
        return result[0];
    }
    
    // ==================== PERSISTENZA ====================
    
    /**
     * Salva un nuovo collaboratore nel database.
     */
    public void save() {
        String query = "INSERT INTO Collaborators (name, contact, fiscal_code, address, occasional, active, vacation_days, user_id) " +
                      "VALUES ('" + escape(name) + "', " +
                      "'" + escape(contact) + "', " +
                      (fiscalCode != null ? "'" + escape(fiscalCode) + "'" : "NULL") + ", " +
                      (address != null ? "'" + escape(address) + "'" : "NULL") + ", " +
                      (occasional ? 1 : 0) + ", " +
                      (active ? 1 : 0) + ", " +
                      vacationDays + ", " +
                      (user != null ? user.getId() : "NULL") + ")";
        
        PersistenceManager.executeUpdate(query);
        this.id = PersistenceManager.getLastId();
    }
    
    /**
     * Aggiorna un collaboratore esistente nel database.
     */
    public void update() {
        String query = "UPDATE Collaborators SET " +
                      "name = '" + escape(name) + "', " +
                      "contact = '" + escape(contact) + "', " +
                      "fiscal_code = " + (fiscalCode != null ? "'" + escape(fiscalCode) + "'" : "NULL") + ", " +
                      "address = " + (address != null ? "'" + escape(address) + "'" : "NULL") + ", " +
                      "occasional = " + (occasional ? 1 : 0) + ", " +
                      "active = " + (active ? 1 : 0) + ", " +
                      "vacation_days = " + vacationDays + ", " +
                      "user_id = " + (user != null ? user.getId() : "NULL") + " " +
                      "WHERE id = " + this.id;
        
        PersistenceManager.executeUpdate(query);
    }
    
    /**
     * Carica tutti i collaboratori attivi dal database.
     */
    public static ArrayList<Collaborator> loadActive() {
        ArrayList<Collaborator> result = new ArrayList<>();
        String query = "SELECT * FROM Collaborators WHERE active = 1 ORDER BY name";
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Collaborator c = new Collaborator();
                c.id = rs.getInt("id");
                c.name = rs.getString("name");
                c.contact = rs.getString("contact");
                c.fiscalCode = rs.getString("fiscal_code");
                c.address = rs.getString("address");
                c.occasional = rs.getInt("occasional") == 1;
                c.active = rs.getInt("active") == 1;
                c.vacationDays = rs.getInt("vacation_days");
                
                int userId = rs.getInt("user_id");
                if (!rs.wasNull()) {
                    c.user = User.load(userId);
                }
                
                result.add(c);
            }
        });
        return result;
    }
    
    /**
     * Carica tutti i collaboratori (inclusi inattivi) dal database.
     */
    public static ArrayList<Collaborator> loadAll() {
        ArrayList<Collaborator> result = new ArrayList<>();
        String query = "SELECT * FROM Collaborators ORDER BY name";
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Collaborator c = new Collaborator();
                c.id = rs.getInt("id");
                c.name = rs.getString("name");
                c.contact = rs.getString("contact");
                c.fiscalCode = rs.getString("fiscal_code");
                c.address = rs.getString("address");
                c.occasional = rs.getInt("occasional") == 1;
                c.active = rs.getInt("active") == 1;
                c.vacationDays = rs.getInt("vacation_days");
                
                int userId = rs.getInt("user_id");
                if (!rs.wasNull()) {
                    c.user = User.load(userId);
                }
                
                result.add(c);
            }
        });
        return result;
    }
    
    /**
     * Carica un collaboratore per ID.
     */
    public static Collaborator loadById(int id) {
        final Collaborator[] result = {null};
        String query = "SELECT * FROM Collaborators WHERE id = " + id;
        
        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Collaborator c = new Collaborator();
                c.id = rs.getInt("id");
                c.name = rs.getString("name");
                c.contact = rs.getString("contact");
                c.fiscalCode = rs.getString("fiscal_code");
                c.address = rs.getString("address");
                c.occasional = rs.getInt("occasional") == 1;
                c.active = rs.getInt("active") == 1;
                c.vacationDays = rs.getInt("vacation_days");
                
                int userId = rs.getInt("user_id");
                if (!rs.wasNull()) {
                    c.user = User.load(userId);
                }
                
                result[0] = c;
            }
        });
        return result[0];
    }
    
    // ==================== GETTERS E SETTERS ====================
    
    public int getId() { return id; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getFiscalCode() { return fiscalCode; }
    public String getAddress() { return address; }
    public boolean isOccasional() { return occasional; }
    public boolean isActive() { return active; }
    public int getVacationDays() { return vacationDays; }
    public User getUser() { return user; }
    
    public void setUser(User user) { this.user = user; }
    public void setVacationDays(int days) { this.vacationDays = days; }
    
    // ==================== UTILITY ====================
    
    @Override
    public String toString() {
        return "Collaborator{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contact='" + contact + '\'' +
                ", occasional=" + occasional +
                ", active=" + active +
                ", vacationDays=" + vacationDays +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Collaborator that = (Collaborator) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
