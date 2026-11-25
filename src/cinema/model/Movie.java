package cinema.model;

public final class Movie {
    public final int id;
    public final String title;
    public final String genre;
    public final int durationMin;
    public final String rating;

    public Movie(int id, String title, String genre, int durationMin, String rating) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.durationMin = durationMin;
        this.rating = rating;
    }

    @Override
    public String toString() {
        return title;
    }
}
