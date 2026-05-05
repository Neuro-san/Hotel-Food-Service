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

    void moveFloor(int moveTo) {
        if (moveTo < 1) {
            System.out.println("Room does not exist");
        } else if (this.currentFloor == moveTo) {
            System.out.println("Already at floor " + moveTo);
        } else {
            if (this.currentFloor < moveTo) {
                System.out.println("Going up...");
            } else {
                System.out.println("Going down...");
            }
            this.currentFloor = moveTo;
            System.out.println("Arrived at floor " + moveTo);
            
        }
    }

    void printStatus() {
        System.out.println("Current floor: " + this.currentFloor);
        System.out.println("Carrying order: " + this.isCarrying);
    }
}
