public class Main {
    public static void main(String[] args){
        Waiter player = new Waiter(1);
        FoodOrder order1 = new FoodOrder("305", 3, 50);

        player.printStatus();
        player.pickUp(order1);
        player.printStatus();
        player.deliverOrder();
        player.printStatus();
    }
}