public class Room {
        String number;
        int floor;
        boolean isOccupied;

        Room(String number, int floor) {
            this.number = number;
            this.floor = floor;
            this.isOccupied = true;
        }

        void printInfo() {
            System.out.println("Room " + this.number + " | Floor " + this.floor + "| Occupied: " + this.isOccupied);
        }
}
