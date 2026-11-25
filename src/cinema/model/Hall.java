package cinema.model;

import java.util.Comparator;

/** The single screening hall: rows A to H, 12 seats each. Seats are named like "E7". */
public final class Hall {
    public static final String ROWS = "ABCDEFGH";
    public static final int SEATS_PER_ROW = 12;
    public static final int CAPACITY = ROWS.length() * SEATS_PER_ROW;
    public static final int MAX_SEATS_PER_BOOKING = 10;
    /** Minutes the hall needs between shows for cleaning. */
    public static final int CLEANING_MIN = 15;

    /** Sorts E2 before E10, unlike plain text order. */
    public static final Comparator<String> SEAT_ORDER = Comparator
        .comparing((String s) -> s.charAt(0))
        .thenComparingInt(s -> Integer.parseInt(s.substring(1)));

    private Hall() { }

    /** row is 0-based, number is 1-based. */
    public static String seat(int row, int number) {
        return ROWS.charAt(row) + String.valueOf(number);
    }

    public static boolean isValidSeat(String seat) {
        if (seat == null || !seat.matches("[A-Z]\\d{1,2}") || ROWS.indexOf(seat.charAt(0)) < 0) {
            return false;
        }
        int number = Integer.parseInt(seat.substring(1));
        return number >= 1 && number <= SEATS_PER_ROW;
    }
}
