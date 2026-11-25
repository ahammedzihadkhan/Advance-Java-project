# Matinee: Video Script

**Length:** about 7 minutes
**What to record:** your screen and your voice
**Tip:** you don't need to read every word exactly. Say it in your own words, slowly and clearly.

---

## Before you record (checklist)

1. Close the Matinee app if it is open.
2. Delete the `data` folder in the project. This resets the demo data, so the show times match today.
3. Open the project in your editor (VS Code), so you can show the code later.
4. Close other apps and notifications, so the video stays smooth.
5. Start recording:
   - **Snipping Tool**: open it, choose the video camera icon, select the screen area, turn on the microphone, press Start.
   - or press **Win + Alt + R** (Xbox Game Bar) to record the active window with your microphone.

---

## Part 1: Introduction (30 seconds)

**Do:** Double-click `run.bat`. Wait for the login window to fade in.

**Say:**
> "Assalamu alaikum. My name is ______, and this is my Advanced Java project.
> The project is called **Matinee**. It is a cinema box-office system.
> Staff use it to add movies, plan show times, and sell tickets by choosing seats on a seat map.
> It is a Java desktop application. The user interface is built with **Swing and JFrame**, and all data is saved in a database using **JDBC**."

---

## Part 2: Login and security (40 seconds)

**Do:** Type username `admin` and a wrong password, then click **Sign in**. The error message appears.

**Say:**
> "First, the staff member has to log in. If the password is wrong, the app shows an error."

**Do:** Type the correct password `admin123` and click **Sign in**.

**Say:**
> "Passwords are not saved as plain text. They are hashed with **PBKDF2** and a random salt.
> The login check uses a **PreparedStatement**, so SQL injection attacks do not work."

---

## Part 3: Now Showing page (40 seconds)

**Do:** Let the cards slide in. Move the mouse over a card so it glows.

**Say:**
> "This is the Now Showing page. Each glass card is one upcoming show, with the time, the movie, the price, and how many seats are sold.
> The line at the top shows today's shows, tickets sold and ticket sales.
> These numbers come from SQL queries using **COUNT, SUM and subqueries**."

---

## Part 4: Booking seats (1 minute 15 seconds)

**Do:** Click **Choose seats** on any card.

**Say:**
> "This is the seat map. It is drawn with **Java 2D**. Seats with a cross are already sold."

**Do:** Click three free seats. Show the total changing on the right. Type a customer name and a phone number, then click **Confirm booking**.

**Say:**
> "I choose three seats, and the total updates. Then I enter the customer's name and phone number and confirm.
> When I confirm, the booking and all its seats are saved in **one JDBC transaction**, using a **batch insert**.
> The database has a **primary key on show and seat**, so the same seat can never be sold twice.
> If any seat is already sold, the whole booking is **rolled back**, and nothing is saved."

**Do:** Click **OK** on the confirmation message.

---

## Part 5: Movies (40 seconds)

**Do:** Click **Movies** in the sidebar. Notice the highlight sliding.

**Say:**
> "On the Movies page I can add, edit, delete and search movies."

**Do:** Select **Iron Orchard**, click **Delete**, then click **Yes**. An error appears.

**Say:**
> "If I try to delete a movie that already has shows, the **foreign key** in the database stops it, and the app shows a clear message."

---

## Part 6: Shows (50 seconds)

**Do:** Click **Shows**. Click **Add show**, and then click **OK** without changing anything. An error appears that says the hall is busy.

**Say:**
> "The Shows page is the timetable for the cinema hall.
> Before a show is saved, the program runs a SQL query that checks if another show is already using the hall at that time, including fifteen minutes for cleaning.
> Here it found a clash, so the show was not saved."

**Do:** Click **OK** on the error. In the form, change the date to a few days later, then click **OK**. The new show appears in the table.

**Say:**
> "When I pick a free time, the show is saved."

---

## Part 7: Bookings (40 seconds)

**Do:** Click **Bookings**. Type the customer name you used in Part 4 into **Search**.

**Say:**
> "Here I can search bookings by customer, phone number, movie or booking number. This is the booking I made earlier."

**Do:** Select it, click **Cancel booking**, then click **Yes**.

**Say:**
> "When I cancel a booking, it is deleted, and **ON DELETE CASCADE** frees its seats automatically, so they can be sold again."

---

## Part 8: The code (1 minute 30 seconds)

**Do:** Switch to VS Code. Open each file below while you talk.

**`src/cinema/db/Database.java`** (scroll to `SCHEMA`)
> "This class opens the JDBC connection with **DriverManager** and creates the five tables: users, movies, shows, bookings and booking seats. Here is the primary key on show and seat that blocks double-booking."

**`src/cinema/dao/BookingDao.java`** (method `book`)
> "This is the booking transaction. I turn off auto-commit, insert the booking, add every seat to a **batch**, and then commit. If anything fails, it rolls back."

**`src/cinema/dao/ShowDao.java`** (method `checkHallFree`)
> "This SQL query finds shows that overlap in time, using a **JOIN** and **DATEADD**."

**`src/cinema/ui` folder**
> "The user interface uses **JFrame**, **CardLayout** to switch pages, and **JTable**.
> The glass panels, the moving background and the seat map are custom-painted with Java 2D, and a **Swing Timer** drives the animation."

**Folders `model`, `dao`, `db`, `ui`**
> "The project follows the **DAO pattern**, so database code, data classes and screens are kept separate."

---

## Part 9: Tests and closing (30 seconds)

**Do:** Double-click `test.bat` (or run it in the terminal). Scroll through the output to "All 32 checks passed."

**Say:**
> "I also wrote automated checks for the database layer. For example, they check that booking a sold seat is rejected and rolled back, and that shows cannot overlap. All 32 checks pass.
> That's my project, Matinee. Thank you for watching."

**Do:** Stop the recording.

---

## If something goes wrong while recording

- **The app says it can't open the database:** another copy is already running. Close it and start again.
- **The show times look old:** close the app, delete the `data` folder, and run `run.bat` again.
- **You make a mistake:** keep going, or pause and record that part again. Small mistakes are fine.
