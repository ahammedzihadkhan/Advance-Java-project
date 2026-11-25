package cinema.model;

import java.time.LocalDateTime;

public final class Booking {
    public final int id;
    public final String customer;
    public final String phone;
    public final String title;
    public final LocalDateTime startsAt;
    public final String seats;
    public final int total;

    public Booking(int id, String customer, String phone, String title, LocalDateTime startsAt,
                   String seats, int total) {
        this.id = id;
        this.customer = customer;
        this.phone = phone;
        this.title = title;
        this.startsAt = startsAt;
        this.seats = seats;
        this.total = total;
    }

    @Override
    public String toString() {
        return "booking #" + id + " (" + customer + ", " + seats + ")";
    }
}
