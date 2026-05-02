public class Waiter {
    int currentFloor;
    boolean isCarrying;
    FoodOrder currentOrder;

    Waiter(int startFloor) {
        this.currentFloor = startFloor;
        this.isCarrying = false;
        this.currentOrder = null;
    }

    void pickUp(FoodOrder order) {
        this.currentOrder = order;
        this.isCarrying = true;
        System.out.println("Order for room " + this.currentOrder.targetRoom + " picked up");
    }

    void deliverOrder() {
        if (this.isCarrying) {
            this.currentOrder.deliver();
            this.isCarrying = false;
            this.currentOrder = null;
        } else {
            System.out.println("Nothing to deliver!");
        }
    }

    void printStatus() {
        System.out.println("Current floor: " + this.currentFloor);
        System.out.println("Carrying order: " + this.isCarrying);
    }
}
