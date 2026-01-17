package catering.businesslogic.personnel;

/**
 * Eccezione specifica per operazioni del modulo Personnel Management.
 * 
 * Usata per segnalare violazioni di regole di business come:
 * - Permessi insufficienti (solo Proprietario può aggiungere, promuovere, approvare ferie)
 * - Collaboratore con incarichi futuri (non eliminabile)
 * - Monte ferie insufficiente
 * 
 * @see main.tex Sezione "Eccezioni" per la lista completa
 */
public class PersonnelException extends Exception {
    
    public PersonnelException(String message) {
        super(message);
    }
    
    public PersonnelException(String message, Throwable cause) {
        super(message, cause);
    }
}
