package cinema.dao;

/** A box-office rule was broken, for example a seat is already sold. The message is shown to the user. */
public class CinemaException extends Exception {
    private static final long serialVersionUID = 1L;

    public CinemaException(String message) {
        super(message);
    }
}
