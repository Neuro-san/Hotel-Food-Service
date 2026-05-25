public class Room {

    String  number;      // e.g. "204"
    int     floor;       // 2 or 3
    int     row;         // tile row on the map
    int     col;         // tile col on the map
    boolean isOccupied;

    Room(String number, int floor, int row, int col) {
        this.number     = number;
        this.floor      = floor;
        this.row        = row;
        this.col        = col;
        this.isOccupied = false;
    }



    // ── debug method ──────────────────────────────────────────────────
    void printInfo() {
        System.out.println("Room " + this.number
            + " | Floor " + this.floor
            + " | Row "   + this.row
            + " | Col "   + this.col
            + " | Occupied: " + this.isOccupied);
    }
}
