package cinema.model;

import java.time.LocalDateTime;

/** A screening of a movie, joined with the movie details and the number of seats sold. */
public final class Show {
    public final int id;
    public final int movieId;
    public final String title;
    public final String genre;
    public final int durationMin;
    public final String rating;
    public final LocalDateTime startsAt;
    public final int price;
    public final int sold;

    public Show(int id, int movieId, String title, String genre, int durationMin, String rating,
                LocalDateTime startsAt, int price, int sold) {
        this.id = id;
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.durationMin = durationMin;
        this.rating = rating;
        this.startsAt = startsAt;
        this.price = price;
        this.sold = sold;
    }

    public int seatsLeft() {
        return Hall.CAPACITY - sold;
    }

    @Override
    public String toString() {
        return title + " at " + startsAt;
    }
}
