# Matinee: Cinema Box Office

A desktop application for selling cinema tickets, built with Java Swing (JFrame) and JDBC.
Box-office staff manage movies and showtimes, pick seats on a live seat map, and handle bookings and cancellations.

## How to run

You need JDK 11 or newer. The database (H2) is embedded and included in `lib`, so no database server is needed.

1. Double-click `run.bat`.
2. Sign in with username `admin` and password `admin123`.

To run the automated checks, run `test.bat`. To reset the demo data, close the app and delete the `data` folder.

## Windows app (.exe)

Double-click `package.bat` to build `dist\Matinee\Matinee.exe` and `dist\Matinee-portable.zip`.
The app includes its own Java runtime, so the PC running it does not need Java: unzip the folder anywhere and double-click `Matinee.exe`.
The packaged app keeps its data in `C:\Users\<you>\Matinee`; delete that folder to reset its demo data.

## Features

- Staff login with salted PBKDF2 password hashing
- Now Showing: upcoming shows as cards, with seats sold and today's ticket sales
- Seat map: click seats or use the arrow keys and Space; sold seats are marked with a cross
- Movies and Shows: add, edit, delete and search
- Bookings: search by customer, phone, movie or booking number, and cancel bookings
- Rules: one hall of 96 seats, at most 10 seats per booking, 15 minutes cleaning time between shows, and no booking or cancelling once a show has started

## Advanced Java concepts

| Concept | Where |
|---|---|
| JDBC connection with `DriverManager` | `src/cinema/db/Database.java` |
| `PreparedStatement` and `ResultSet` | every class in `src/cinema/dao/` |
| Transaction with commit and rollback | `book` in `BookingDao.java`, `save` in `ShowDao.java` |
| Batch insert (`addBatch` / `executeBatch`) | `book` in `BookingDao.java` |
| Generated keys | `add` in `MovieDao.java`, `book` in `BookingDao.java` |
| Primary key, foreign key, check and cascade-delete constraints | `SCHEMA` in `Database.java` |
| Joins, subqueries, `GROUP BY`, `LISTAGG`, `DATEADD` | `ShowDao.java`, `BookingDao.java` |
| DAO pattern and layered design | `model`, `dao`, `db` and `ui` packages |
| JFrame, CardLayout, JTable, dialogs | `src/cinema/ui/` |
| Custom painting and animation with Java 2D and a Swing `Timer` | `Backdrop.java`, `Glass.java`, `Motion.java`, `SeatMap.java` |
| Generics, inheritance and abstract classes | `CrudPanel.java` |
| Custom exceptions | `CinemaException.java` |

## How double-booking is prevented

The `booking_seats` table has the primary key `(show_id, seat)`, so the database itself refuses to store the same seat twice for one show.
`BookingDao.book` inserts the booking and all its seats in one transaction. If any seat is already sold, the insert fails, the whole transaction is rolled back, and nothing is saved.

## Database tables

- `users`: staff logins
- `movies`: title, genre, running time and rating
- `shows`: which movie plays when, and the ticket price
- `bookings`: customer, phone and total for one booking
- `booking_seats`: the seats in each booking
