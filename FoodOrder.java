public class FoodOrder {
    String targetRoom;
    int targetFloor;
    int timeLimit;
    boolean isDelivered;

    FoodOrder(String targetRoom, int targetFloor, int timeLimit) {
        this.targetRoom = targetRoom;
        this.targetFloor = targetFloor;
        this.timeLimit = timeLimit;
        this.isDelivered = false;
    }

    void deliver() {
        this.isDelivered = true;
        System.out.println("Order for room " + this.targetRoom + " delivered!");
    }

    void printStatus() {
        System.out.println("    Order for room " + this.targetRoom);
        System.out.println("Room: " + this.targetRoom);
        System.out.println("Floor: " + this.targetFloor);
        System.out.println("Time: " + this.timeLimit + "s");
        System.out.println("Order status: " + this.isDelivered);
    }
}
