import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import javax.swing.*;

public class GamePanel extends JPanel {

    final int WIDTH = 800;
    final int HEIGHT = 600;
    int tileSize = 40;

    // GAME STATE
    String gameState = "PLAYING";

    // SCORE
    int score = 0;

    // TIMER
    int time = 99;
    int timeLeft = time;
    int frameCounter = 0;

    // ORDER
    FoodOrder currentOrder;
    int orderRow;
    int orderCol;

    Random rand = new Random();

    // ROOM POSITIONS
    int[][] rooms = {

        // FLOOR 3
        {1, 1}, {1, 3}, {1, 5}, {1, 7}, {1, 9}, {1, 11}, {1, 13},
        {3, 1}, {3, 3}, {3, 5}, {3, 7}, {3, 9}, {3, 11}, {3, 13},

        // FLOOR 2
        {6, 1}, {6, 3}, {6, 5}, {6, 7}, {6, 9}, {6, 11}, {6, 13},
        {8, 1}, {8, 3}, {8, 5}, {8, 7}, {8, 9}, {8, 11}, {8, 13},

        // FLOOR 1
        {11, 1}, {11, 3}, {11, 5}, {11, 7}, {11, 9}, {11, 11}, {11, 13},
        {13, 1}, {13, 3}, {13, 5}, {13, 7}, {13, 9}, {13, 11}, {13, 13},
    };

    String[] roomNames = {
        "301", "302", "303", "304", "305", "306", "307",
        "314", "313", "312", "311", "310", "309", "308",

        "201", "202", "203", "204", "205", "206", "207",
        "214", "213", "212", "211", "210", "209", "208",

        "101", "102", "103", "104", "105", "106", "107",
        "114", "113", "112", "111", "110", "109", "108",
    };

    int[][] map = {

        // FLOOR 3
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
        {2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2},
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},

        // FLOOR 2
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
        {2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2},
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},

        // FLOOR 1
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
        {2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2},
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
    };

    // PLAYER
    int playerCol = 5;
    int playerRow = 13;

    // ELEVATOR
    boolean isRiding = false;       // is the elevator currently moving?
    int elevatorTarget = 0;         // which row are we heading to?
    int elevatorDirection = 0;      // -1 = going up, +1 = going down
    int elevatorFrameCounter = 0;   // separate counter from the timer
    final int ELEVATOR_SPEED = 20;  // move 1 row every 20 frames

    // ELEVATOR ROWS
    final int HIGHEST_FLOOR = 2;
    final int LOWEST_FLOOR = 12;

    GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        generateNewOrder();

        Thread gameLoop = new Thread(() -> {
            while (true) {
                update();
                repaint();
                try {
                    Thread.sleep(16);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        gameLoop.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                // RESTART GAME
                if (gameState.equals("GAME_OVER")) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        restartGame();
                    }
                    return;
                }

                // BLOCK ALL INPUT WHILE ELEVATOR IS MOVING
                if (isRiding) return;

                int key = e.getKeyCode();
                int nextRow = playerRow;
                int nextCol = playerCol;

                // ON ELEVATOR TILE — UP/DOWN starts the ride
                if (map[playerRow][playerCol] == 2) {

                    if (key == KeyEvent.VK_UP && playerRow != HIGHEST_FLOOR) {
                        isRiding = true;
                        elevatorDirection = -1;
                        elevatorTarget = playerRow - 5;
                        elevatorFrameCounter = 0;
                        return; // skip normal movement
                    }

                    if (key == KeyEvent.VK_DOWN && playerRow != LOWEST_FLOOR) {
                        isRiding = true;
                        elevatorDirection = 1;
                        elevatorTarget = playerRow + 5;
                        elevatorFrameCounter = 0;
                        return; // skip normal movement
                    }
                }

                // NORMAL MOVEMENT
                if (key == KeyEvent.VK_UP)         nextRow--;
                else if (key == KeyEvent.VK_DOWN)  nextRow++;
                else if (key == KeyEvent.VK_LEFT)  nextCol--;
                else if (key == KeyEvent.VK_RIGHT) nextCol++;

                // WALL COLLISION
                if (map[nextRow][nextCol] != 1) {
                    playerRow = nextRow;
                    playerCol = nextCol;
                }
            }
        });
    }

    public void update() {
        if (!gameState.equals("PLAYING")) return;

        // ELEVATOR MOVEMENT — runs independently from normal game logic
        if (isRiding) {
            elevatorFrameCounter++;

            if (elevatorFrameCounter >= ELEVATOR_SPEED) {
                playerRow += elevatorDirection; // move one row
                elevatorFrameCounter = 0;

                // ARRIVED AT TARGET FLOOR
                if (playerRow == elevatorTarget) {
                    isRiding = false;
                }
            }
            return; // skip timer and delivery check while riding
        }

        // TIMER
        frameCounter++;
        if (frameCounter >= 60) {
            timeLeft--;
            frameCounter = 0;
        }

        // GAME OVER
        if (timeLeft <= 0) {
            gameState = "GAME_OVER";
        }

        // DELIVERY CHECK
        if (playerRow == orderRow && playerCol == orderCol) {
            currentOrder.deliver();
            score++;
            generateNewOrder();
            timeLeft = time;
        }
    }

    private void restartGame() {
        playerRow = 13;
        playerCol = 5;
        score = 0;
        timeLeft = time;
        frameCounter = 0;
        isRiding = false;
        elevatorFrameCounter = 0;
        gameState = "PLAYING";
        generateNewOrder();
    }

    private void generateNewOrder() {
        int pick;

        do {
            pick = rand.nextInt(rooms.length);
            orderRow = rooms[pick][0];
            orderCol = rooms[pick][1];
        } while (playerRow == orderRow && playerCol == orderCol);

        int floor;
        if (orderRow <= 3)      floor = 3;
        else if (orderRow <= 8) floor = 2;
        else                    floor = 1;

        currentOrder = new FoodOrder(roomNames[pick], floor, 5);
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);

        // BACKGROUND
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // MAP
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {

                if (map[row][col] == 0)      g.setColor(Color.BLACK);
                else if (map[row][col] == 1) g.setColor(Color.DARK_GRAY);
                else                         g.setColor(Color.YELLOW);

                g.fillRect(col * tileSize, row * tileSize, tileSize, tileSize);
            }
        }

        // ROOMS
        for (int i = 0; i < rooms.length; i++) {

            int roomRow = rooms[i][0];
            int roomCol = rooms[i][1];

            // TARGET ROOM
            if (roomRow == orderRow && roomCol == orderCol) g.setColor(Color.RED);
            else                                            g.setColor(Color.GREEN);

            g.fillRect(roomCol * tileSize, roomRow * tileSize, tileSize, tileSize);

            // ROOM NUMBER
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));

            String roomText = roomNames[i];
            FontMetrics fm = g.getFontMetrics();

            int textX = (roomCol * tileSize) + (tileSize - fm.stringWidth(roomText)) / 2;
            int textY = (roomRow * tileSize) + ((tileSize - fm.getHeight()) / 2) + fm.getAscent();

            g.drawString(roomText, textX, textY);
        }

        // PLAYER — cyan while riding elevator, blue otherwise
        g.setColor(isRiding ? Color.CYAN : Color.BLUE);
        g.fillRect(playerCol * tileSize, playerRow * tileSize, tileSize, tileSize);

        // HUD DIVIDER
        g.setColor(Color.WHITE);
        g.drawLine(600, 0, 600, HEIGHT);

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));

        g.drawString("Hotel Food Service", 620, 30);
        g.drawString("Score: " + score, 620, 70);
        g.drawString("Time Left: " + timeLeft + "s", 620, 100);
        g.drawString("Current Order", 620, 160);
        g.drawString("Room: " + currentOrder.targetRoom, 620, 190);
        g.drawString("Floor: " + currentOrder.targetFloor, 620, 220);

        // ELEVATOR STATUS IN HUD
        if (isRiding) {
            g.setColor(Color.CYAN);
            g.drawString("[ Elevator riding... ]", 610, 260);
        }

        // GAME OVER SCREEN
        if (gameState.equals("GAME_OVER")) {

            // DARK OVERLAY
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // GAME OVER TITLE
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 42));
            String gameOverText = "GAME OVER";
            FontMetrics fm1 = g.getFontMetrics();
            g.drawString(gameOverText, (WIDTH - fm1.stringWidth(gameOverText)) / 2, 200);

            // FINAL SCORE
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 26));
            String scoreText = "Final Score: " + score;
            FontMetrics fm2 = g.getFontMetrics();
            g.drawString(scoreText, (WIDTH - fm2.stringWidth(scoreText)) / 2, 260);

            // PLAY AGAIN BUTTON
            int buttonWidth = 220;
            int buttonHeight = 60;
            int buttonX = (WIDTH - buttonWidth) / 2;
            int buttonY = 320;

            g.setColor(Color.GREEN);
            g.fillRect(buttonX, buttonY, buttonWidth, buttonHeight);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            String buttonText = "PLAY AGAIN";
            FontMetrics fm3 = g.getFontMetrics();
            int textX = buttonX + (buttonWidth - fm3.stringWidth(buttonText)) / 2;
            g.drawString(buttonText, textX, buttonY + 38);

            // ENTER TEXT
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            String restartText = "Press ENTER to restart";
            FontMetrics fm4 = g.getFontMetrics();
            g.drawString(restartText, (WIDTH - fm4.stringWidth(restartText)) / 2, 420);
        }
    }
}
