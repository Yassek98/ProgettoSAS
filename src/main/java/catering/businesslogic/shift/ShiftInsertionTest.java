package catering.businesslogic.shift;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;

public class ShiftInsertionTest {
    public static void main(String[] args) {
        System.out.println("--- Test Inserimento Turni ---");
        
        // Pulizia iniziale per evitare duplicati sporchi
        System.out.println("Pulizia tabella Shifts...");
        catering.persistence.PersistenceManager.executeUpdate("DELETE FROM ShiftBookings");
        catering.persistence.PersistenceManager.executeUpdate("DELETE FROM Shifts");
        catering.persistence.PersistenceManager.executeUpdate("DELETE FROM sqlite_sequence WHERE name='Shifts'");
        
        // Creazione di alcuni turni di esempio
        // Turno 1: Domani, mattina
        Date date1 = Date.valueOf("2026-01-19");
        Time start1 = Time.valueOf("08:00:00");
        Time end1 = Time.valueOf("12:00:00");
        
        // Turno 2: Domani, pomeriggio
        Date date2 = Date.valueOf("2026-01-19");
        Time start2 = Time.valueOf("14:00:00");
        Time end2 = Time.valueOf("18:00:00");
        
        // Turno 3: Dopodomani, sera
        Date date3 = Date.valueOf("2026-01-20");
        Time start3 = Time.valueOf("19:00:00");
        Time end3 = Time.valueOf("23:00:00");

        System.out.println("Inserimento Turno 1: " + date1 + " " + start1 + "-" + end1);
        Shift.createShift(date1, start1, end1);
        
        System.out.println("Inserimento Turno 2: " + date2 + " " + start2 + "-" + end2);
        Shift.createShift(date2, start2, end2);
        
        System.out.println("Inserimento Turno 3: " + date3 + " " + start3 + "-" + end3);
        Shift.createShift(date3, start3, end3);

        System.out.println("\nVerifica turni caricati dal DB:");
        ArrayList<Shift> allShifts = Shift.loadAllShifts();
        for (Shift s : allShifts) {
            System.out.println(s);
        }
        
        System.out.println("--- Inserimento completato ---");
    }
}
