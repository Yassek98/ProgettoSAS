package catering.businesslogic.shift;

import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;
import catering.util.LogManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Shift {
    private static final Logger LOGGER = LogManager.getLogger(Shift.class);

    private int id;
    private Date date;
    private Time startTime;
    private Time endTime;
    private Map<Integer, User> bookedUsers;

    private Shift() {
        bookedUsers = new HashMap<>();
    }

    public Shift(Date date, Time startTime, Time endTime) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        bookedUsers = new HashMap<>();
    }

    /**
     * Sets the ID of this shift.
     * Used when updating an existing shift.
     * 
     * @param id The ID to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Sets the end time for this shift
     * 
     * @param endTime The new end time
     */
    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    // STATIC METHODS FOR PERSISTENCE

    public static ArrayList<Shift> getShiftTable() {
        return loadAllShifts();
    }

    public static ArrayList<Shift> loadAllShifts() {
        String query = "SELECT * FROM Shifts";
        ArrayList<Shift> shiftArrayList = new ArrayList<>();

        LOGGER.info("Loading all shifts from database");

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Shift s = new Shift();
                s.id = rs.getInt("id");

                // Use safe date/time handling for SQLite
                try {
                    String dateStr = rs.getString("date");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            if (dateStr.matches("\\d+")) { // If it's a numeric timestamp
                                s.date = new Date(Long.parseLong(dateStr));
                            } else {
                                s.date = Date.valueOf(dateStr);
                            }
                        } catch (Exception e) {
                            LOGGER.warning("Could not parse date '" + dateStr + "' for shift ID " + s.id);
                        }
                    }

                    String startStr = rs.getString("start_time");
                    if (startStr != null && !startStr.isEmpty()) {
                        try {
                            if (startStr.matches("\\d+")) {
                                s.startTime = new Time(Long.parseLong(startStr));
                            } else {
                                if (startStr.length() == 5) startStr += ":00";
                                s.startTime = Time.valueOf(startStr);
                            }
                        } catch (Exception e) {
                            LOGGER.warning("Could not parse start_time '" + startStr + "' for shift ID " + s.id);
                        }
                    }

                    String endStr = rs.getString("end_time");
                    if (endStr != null && !endStr.isEmpty()) {
                        try {
                            if (endStr.matches("\\d+")) {
                                s.endTime = new Time(Long.parseLong(endStr));
                            } else {
                                if (endStr.length() == 5) endStr += ":00";
                                s.endTime = Time.valueOf(endStr);
                            }
                        } catch (Exception e) {
                            LOGGER.warning("Could not parse end_time '" + endStr + "' for shift ID " + s.id);
                        }
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "SQL Error parsing date/time in Shift for ID " + s.id, ex);
                }

                s.bookedUsers = loadBookings(s);
                shiftArrayList.add(s);
            }
        });

        // Sort the shifts by date and time, handling nulls
        shiftArrayList.sort((a, b) -> {
            if (a.getDate() == null || b.getDate() == null) return 0;
            
            if (a.getDate().before(b.getDate()))
                return -1;
            else if (a.getDate().after(b.getDate()))
                return 1;
            
            if (a.getStartTime() == null || b.getStartTime() == null) return 0;
            
            if (a.getStartTime().before(b.getStartTime()))
                return -1;
            else if (a.getStartTime().after(b.getStartTime()))
                return 1;
            else
                return 0;
        });

        LOGGER.info("Loaded " + shiftArrayList.size() + " shifts");
        return shiftArrayList;
    }

    public static Shift loadItemById(int id) {
        String query = "SELECT * FROM Shifts WHERE id = ?";
        Shift[] shiftHolder = new Shift[1]; // Use array to allow modification in lambda

        LOGGER.fine("Loading shift with ID " + id);

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Shift s = new Shift();
                s.id = rs.getInt("id");

                // Use safe date/time handling for SQLite
                try {
                    Object dateObj = rs.getObject("date");
                    if (dateObj instanceof String) {
                        s.date = Date.valueOf((String) dateObj);
                    } else if (dateObj instanceof Date) {
                        s.date = (Date) dateObj;
                    }

                    Object startObj = rs.getObject("start_time");
                    if (startObj instanceof String) {
                        s.startTime = Time.valueOf((String) startObj);
                    } else if (startObj instanceof Time) {
                        s.startTime = (Time) startObj;
                    }

                    Object endObj = rs.getObject("end_time");
                    if (endObj instanceof String) {
                        s.endTime = Time.valueOf((String) endObj);
                    } else if (endObj instanceof Time) {
                        s.endTime = (Time) endObj;
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.WARNING, "Error parsing date/time in Shift for ID " + s.id + ": " + ex.getMessage());
                }

                shiftHolder[0] = s;
            }
        }, id); // Pass id as parameter

        Shift s = shiftHolder[0];
        if (s != null && s.id == id) { // Check if we found the shift
            s.bookedUsers = loadBookings(s);
            return s;
        }

        LOGGER.warning("Shift with ID " + id + " not found");
        return null; // Return null if shift not found
    }

    private static Map<Integer, User> loadBookings(Shift s) {
        Map<Integer, User> bookings = new HashMap<>();
        String query = "SELECT user_id FROM ShiftBookings WHERE shift_id = ?";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                int userId = rs.getInt("user_id");
                User user = User.load(userId);
                if (user != null) {
                    bookings.put(userId, user);
                }
            }
        }, s.id); // Pass s.id as parameter

        LOGGER.fine("Loaded " + bookings.size() + " bookings for shift ID " + s.id);
        return bookings;
    }

    public static Shift createShift(Date date, Time startTime, Time endTime) {
        Shift s = new Shift();
        s.date = date;
        s.startTime = startTime;
        s.endTime = endTime;
        s.bookedUsers = new HashMap<>();

        String query = "INSERT INTO Shifts (date, start_time, end_time) VALUES (?, ?, ?)";

        PersistenceManager.executeUpdate(query,
                s.date.toString(),
                s.startTime.toString(),
                s.endTime.toString());

        s.id = PersistenceManager.getLastId();

        LOGGER.info("Created new shift ID " + s.id + " on " + s.date);
        return s;
    }

    // Save a new shift to the database
    public void saveShift() {
        if (this.id > 0) {
            updateShift(); // If id exists, update instead of insert
            return;
        }

        String query = "INSERT INTO Shifts (date, start_time, end_time) VALUES (?, ?, ?)";
        PersistenceManager.executeUpdate(query,
                date.toString(),
                startTime.toString(),
                endTime.toString());

        this.id = PersistenceManager.getLastId();
    }

    // Update an existing shift
    public void updateShift() {
        if (this.id <= 0) {
            saveShift(); // If no id, insert instead of update
            return;
        }

        String query = "UPDATE Shifts SET date = ?, start_time = ?, end_time = ? WHERE id = ?";
        PersistenceManager.executeUpdate(query,
                date.toString(),
                startTime.toString(),
                endTime.toString(),
                this.id);
    }

    // Save a booking to the database
    public void saveBooking(User user) {
        String query = "INSERT INTO ShiftBookings (shift_id, user_id) VALUES (?, ?)";
        PersistenceManager.executeUpdate(query, this.id, user.getId());

        // Update local cache
        bookedUsers.put(user.getId(), user);
    }

    // Remove a booking from the database
    public void removeBooking(User user) {
        String query = "DELETE FROM ShiftBookings WHERE shift_id = ? AND user_id = ?";
        PersistenceManager.executeUpdate(query, this.id, user.getId());

        // Update local cache
        bookedUsers.remove(user.getId());
    }

    // INSTANCE METHODS

    public Date getDate() {
        return date;
    }

    public Time getStartTime() {
        return startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void addBooking(User u) {
        if (this.bookedUsers.containsKey(u.getId())) {
            LOGGER.warning("User " + u.getUserName() + " is already booked for this shift");
            return;
        }

        String query = "INSERT INTO ShiftBookings (shift_id, user_id) VALUES (?, ?)";
        PersistenceManager.executeUpdate(query, this.id, u.getId());

        this.bookedUsers.put(u.getId(), u);
        LOGGER.info("Added booking for user " + u.getUserName() + " to shift ID " + this.id);
    }

    public User removeBookedUser(User u) {
        if (!this.bookedUsers.containsKey(u.getId())) {
            LOGGER.warning("User " + u.getUserName() + " is not booked for this shift");
            return null;
        }

        String query = "DELETE FROM ShiftBookings WHERE shift_id = ? AND user_id = ?";
        int rowsAffected = PersistenceManager.executeUpdate(query, this.id, u.getId());

        if (rowsAffected > 0) {
            User removed = this.bookedUsers.remove(u.getId());
            LOGGER.info("Removed booking for user " + u.getUserName() + " from shift ID " + this.id);
            return removed;
        } else {
            LOGGER.warning("Failed to remove booking for user " + u.getUserName() + " from shift ID " + this.id);
            return null;
        }
    }

    public boolean isBooked(User u) {
        return bookedUsers.containsValue(u);
    }

    public int getId() {
        return id;
    }

    public Map<Integer, User> getBookedUsers() {
        return new HashMap<>(bookedUsers); // Return a copy to prevent modification
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(date)
                .append(" | <")
                .append(startTime)
                .append(" - ")
                .append(endTime)
                .append(">");

        if (!bookedUsers.isEmpty()) {
            for (User u : bookedUsers.values()) {
                sb.append("\n\t - ").append(u.toString());
            }
        }

        return sb.toString();
    }
}
