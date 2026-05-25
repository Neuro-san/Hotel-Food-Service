import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.*;

public class GamePanel extends JPanel {

    final int WIDTH  = 800;
    final int HEIGHT = 600;
    final int tileSize = 40;
    final int gapBetweenFloors = 2;

    // GAME STATE
    String gameState = "MENU";

    // MENU
    static final String[] MENU_BUTTONS = { "Play", "How to Play", "Quit" };
    int selectedButton = 0;

    // HI-SCORE
    int highScore = 0;

    // SCORE
    int score = 0;

    // DISSATISFACTION BAR
    int dissatisfaction = 5;
    static final int MAX_DISSATISFACTION = 5;

    // DIFFICULTY
    int activeOrderCount = 1;

    // ACTIVE ORDERS
    ArrayList<int[]>     orderPositions = new ArrayList<>();
    ArrayList<FoodOrder> orderList      = new ArrayList<>();
    ArrayList<Integer>   orderTimers    = new ArrayList<>();

    Random rand = new Random();

    // Tile colors
    static final Color C_FLOOR     = Color.BLACK;
    static final Color C_WALL      = Color.DARK_GRAY;
    static final Color C_ESCALATOR = Color.BLUE;
    static final Color C_ELEVATOR  = Color.YELLOW;
    static final Color C_ROOM      = Color.GREEN;
    static final Color C_ROOM_RED  = Color.RED;
    static final Color C_SEPARATOR = Color.WHITE;
    static final Color C_KITCHEN   = Color.GRAY;
    static final Color C_EXIT      = new Color(139, 90, 43);
    static final Color C_LOUNGE    = Color.BLACK;

    // Mission 2: single list replaces roomPositions + roomLabels
    ArrayList<Room> rooms = new ArrayList<>();

    int[][] map = {
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 4, 5, 4, 5, 4, 5, 4, 5, 4, 5, 4, 5, 4, 1},
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
        {1, 5, 4, 5, 4, 5, 2, 5, 3, 5, 4, 5, 4, 5, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 5, 4, 5, 4, 5, 2, 5, 3, 5, 4, 5, 4, 5, 1},
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
        {1, 5, 4, 5, 4, 5, 2, 5, 3, 5, 4, 5, 4, 5, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 6, 6, 6, 8, 8, 2, 8, 3, 8, 8, 8, 8, 8, 1},
        {1, 6, 6, 6, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},
        {1, 6, 6, 6, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
    };

    // Map-level transport constants (floor rows)
    final int HIGHEST_FLOOR = 2;
    final int LOWEST_FLOOR  = 12;

    // Door sprites
    HashMap<String, BufferedImage> doorSprites = new HashMap<>();

    // Environment sprites
    BufferedImage wallSprite;
    BufferedImage floorSprite;
    BufferedImage elevatorSprite;
    BufferedImage escalatorSprite;
    BufferedImage kitchenSheet;
    static final int KIT_SRC_X  = 124;
    static final int KIT_SRC_Y  = 160;
    static final int KIT_CELL_W = 258;
    static final int KIT_CELL_H = 225;
    static final int KIT_ROW    = 11;
    static final int KIT_COL    = 1;

    // Mission 1: Waiter owns all player state
    Waiter waiter = new Waiter(12, 9);

    // Mission 3: centralised sound
    SoundManager sound = new SoundManager();

    // -------------------------------------------------------------------------

    GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        loadSprites();
        loadHighScore();
        buildRoomData();

        Thread gameLoop = new Thread(() -> {
            while (true) {
                update();
                repaint();
                try { Thread.sleep(16); }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        gameLoop.start();

        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();

                if (gameState.equals("MENU")) {
                    if (key == KeyEvent.VK_UP) {
                        selectedButton = (selectedButton - 1 + MENU_BUTTONS.length) % MENU_BUTTONS.length;
                    } else if (key == KeyEvent.VK_DOWN) {
                        selectedButton = (selectedButton + 1) % MENU_BUTTONS.length;
                    } else if (key == KeyEvent.VK_ENTER) {
                        switch (selectedButton) {
                            case 0 -> startGame();
                            case 1 -> gameState = "HOW_TO_PLAY";
                            case 2 -> System.exit(0);
                        }
                    }
                    return;
                }

                if (gameState.equals("HOW_TO_PLAY")) {
                    if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) gameState = "MENU";
                    return;
                }

                if (gameState.equals("GAME_OVER")) {
                    if (key == KeyEvent.VK_ENTER) gameState = "MENU";
                    return;
                }

                // Mission 1: guard via waiter field
                if (waiter.isRiding) return;

                int nextRow     = waiter.row;
                int nextCol     = waiter.col;
                int currentTile = map[waiter.row][waiter.col];

                // ESCALATOR (tile 2) — UP only
                if (currentTile == 2 && key == KeyEvent.VK_UP && waiter.row != HIGHEST_FLOOR) {
                    waiter.direction             = Waiter.DIR_UP;
                    waiter.isRiding              = true;
                    waiter.elevatorDirection     = -1;
                    waiter.elevatorTarget        = waiter.row - (gapBetweenFloors + 1 + 1);
                    waiter.elevatorFrameCounter  = 0;
                    waiter.currentTransportSpeed = Waiter.ESCALATOR_SPEED;
                    waiter.isMoving              = true;
                    return;
                }

                // ELEVATOR (tile 3) — both directions
                if (currentTile == 3) {
                    if (key == KeyEvent.VK_UP && waiter.row != HIGHEST_FLOOR) {
                        waiter.direction             = Waiter.DIR_UP;
                        waiter.isRiding              = true;
                        waiter.elevatorDirection     = -1;
                        waiter.elevatorTarget        = waiter.row - (gapBetweenFloors + 1 + 1);
                        waiter.elevatorFrameCounter  = 0;
                        waiter.currentTransportSpeed = Waiter.ELEVATOR_SPEED;
                        waiter.isMoving              = true;
                        return;
                    }
                    if (key == KeyEvent.VK_DOWN && waiter.row != LOWEST_FLOOR) {
                        waiter.direction             = Waiter.DIR_DOWN;
                        waiter.isRiding              = true;
                        waiter.elevatorDirection     = 1;
                        waiter.elevatorTarget        = waiter.row + (gapBetweenFloors + 1 + 1);
                        waiter.elevatorFrameCounter  = 0;
                        waiter.currentTransportSpeed = Waiter.ELEVATOR_SPEED;
                        waiter.isMoving              = true;
                        return;
                    }
                }

                // NORMAL MOVEMENT
                if      (key == KeyEvent.VK_UP)    { nextRow--; waiter.direction = Waiter.DIR_UP;    }
                else if (key == KeyEvent.VK_DOWN)  { nextRow++; waiter.direction = Waiter.DIR_DOWN;  }
                else if (key == KeyEvent.VK_LEFT)  { nextCol--; waiter.direction = Waiter.DIR_LEFT;  }
                else if (key == KeyEvent.VK_RIGHT) { nextCol++; waiter.direction = Waiter.DIR_RIGHT; }

                int targetTile = map[nextRow][nextCol];
                boolean passable = targetTile != 1 && targetTile != 5 && targetTile != 6;

                if (passable) {
                    waiter.row      = nextRow;
                    waiter.col      = nextCol;
                    waiter.isMoving = true;
                } else {
                    waiter.isMoving = false;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_UP   || key == KeyEvent.VK_DOWN ||
                    key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                    waiter.isMoving  = false;
                    waiter.animFrame = 0;
                    waiter.animTick  = 0;
                }
            }
        });
    }

    // ── update ────────────────────────────────────────────────────────────────
    public void update() {
        if (!gameState.equals("PLAYING")) return;

        // Mission 1: transport + animation fully inside Waiter
        waiter.update();

        // PER-ORDER TIMERS
        for (int i = orderTimers.size() - 1; i >= 0; i--) {
            orderTimers.set(i, orderTimers.get(i) - 1);

            if (orderTimers.get(i) <= 0) {
                orderPositions.remove(i);
                orderList.remove(i);
                orderTimers.remove(i);
                dissatisfaction--;

                if (dissatisfaction <= 0) {
                    saveHighScore();
                    gameState = "GAME_OVER";
                    sound.playGameOver();  // Mission 3
                    return;
                }

                addNewOrder();
                sound.playOrderExpiry();  // Mission 3
            }
        }

        // DELIVERY CHECK
        for (int i = orderPositions.size() - 1; i >= 0; i--) {
            int[] pos = orderPositions.get(i);
            // Mission 1: waiter.row / waiter.col
            if (waiter.row == pos[0] && waiter.col == pos[1]) {
                orderList.get(i).deliver();
                orderPositions.remove(i);
                orderList.remove(i);
                orderTimers.remove(i);
                score++;
                sound.playDeliverySuccess();  // Mission 3
            }
        }

        // BATCH CLEARED
        if (orderPositions.isEmpty()) {
            if (score > 0 && score % 5 == 0) activeOrderCount++;
            generateOrders();
        }
    }

    // ── buildRoomData ─────────────────────────────────────────────────────────
    // Mission 2: builds Room objects instead of two parallel lists
    private void buildRoomData() {
        rooms.clear();
        int floor3Count = 0;
        int floor2Count = 0;

        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col] == 4) {
                    if (row < 5) {
                        floor3Count++;
                        rooms.add(new Room(String.format("3%02d", floor3Count), 3, row, col));
                    } else if (row < 10) {
                        floor2Count++;
                        rooms.add(new Room(String.format("2%02d", floor2Count), 2, row, col));
                    }
                }
            }
        }
    }

    // ── generateOrders ────────────────────────────────────────────────────────
    private void generateOrders() {
        orderPositions.clear();
        orderList.clear();
        orderTimers.clear();

        ArrayList<Integer> usedIndices = new ArrayList<>();

        for (int n = 0; n < activeOrderCount; n++) {
            int  pick;
            Room r;
            do {
                pick = rand.nextInt(rooms.size());
                r    = rooms.get(pick);
            } while (
                usedIndices.contains(pick) ||
                (r.row == waiter.row && r.col == waiter.col)
            );

            usedIndices.add(pick);
            orderPositions.add(new int[]{r.row, r.col});
            orderList.add(new FoodOrder(r.number, r.floor, 0));
            orderTimers.add(randomOrderTimer());
        }
    }

    // ── addNewOrder ───────────────────────────────────────────────────────────
    private void addNewOrder() {
        int  pick;
        Room r;
        do {
            pick = rand.nextInt(rooms.size());
            r    = rooms.get(pick);
        } while (isPositionTaken(r.row, r.col));

        orderPositions.add(new int[]{r.row, r.col});
        orderList.add(new FoodOrder(r.number, r.floor, 0));
        orderTimers.add(randomOrderTimer());
    }

    private int randomOrderTimer() {
        return rand.nextInt(301) + 300;
    }

    private boolean isPositionTaken(int row, int col) {
        if (row == waiter.row && col == waiter.col) return true;
        for (int[] pos : orderPositions) {
            if (pos[0] == row && pos[1] == col) return true;
        }
        return false;
    }

    // ── startGame ─────────────────────────────────────────────────────────────
    private void startGame() {
        // Mission 1: single reset call
        waiter.reset(12, 9);
        score            = 0;
        dissatisfaction  = MAX_DISSATISFACTION;
        activeOrderCount = 1;
        selectedButton   = 0;
        gameState        = "PLAYING";
        generateOrders();
    }

    // ── loadHighScore / saveHighScore ─────────────────────────────────────────
    private void loadHighScore() {
        try {
            Scanner sc = new Scanner(new File("hiscore.txt"));
            if (sc.hasNextInt()) highScore = sc.nextInt();
            sc.close();
        } catch (Exception ex) { highScore = 0; }
    }

    private void saveHighScore() {
        if (score > highScore) {
            highScore = score;
            try {
                PrintWriter pw = new PrintWriter(new File("hiscore.txt"));
                pw.println(highScore);
                pw.close();
            } catch (Exception ex) {
                System.out.println("Could not save hi-score: " + ex.getMessage());
            }
        }
    }

    // ── loadSprites ───────────────────────────────────────────────────────────
    // Mission 1: waiter.png block removed — Waiter loads its own sprite
    private void loadSprites() {
        try {
            kitchenSheet = ImageIO.read(new File("assets/kitchen.png"));
        } catch (Exception ex) { System.out.println("Kitchen sprite not found: " + ex.getMessage()); }
        try { wallSprite      = ImageIO.read(new File("assets/wood_walls.png"));   } catch (Exception ex) { System.out.println("wall sprite missing");     }
        try { floorSprite     = ImageIO.read(new File("assets/marble_floors.png")); } catch (Exception ex) { System.out.println("floor sprite missing");    }
        try { elevatorSprite  = ImageIO.read(new File("assets/elevator.png"));     } catch (Exception ex) { System.out.println("elevator sprite missing");  }
        try { escalatorSprite = ImageIO.read(new File("assets/escalator.png"));    } catch (Exception ex) { System.out.println("escalator sprite missing"); }
        loadDoorSprites();
    }

    private void loadDoorSprites() {
        loadDoorSheet("assets/room_201-204.png",
            new String[]{"201","202","203","204"},    266, 395, 250, 237);
        loadDoorSheet("assets/room_205-208.png",
            new String[]{"205","206","207","208"},    198, 326, 284, 305);
        loadDoorSheet("assets/room_301-306.png",
            new String[]{"301","302","303","304","305","306"}, 79, 411, 230, 211);
        loadDoorSheet("assets/room_307.png",
            new String[]{"307"},                       56,  36, 280, 303);
        loadDoorSheet("assets/room_308-311.png",
            new String[]{"308","309","310","311"},    267, 396, 250, 236);
    }

    private void loadDoorSheet(String path, String[] labels,
                                int srcX, int srcY, int cellW, int cellH) {
        try {
            BufferedImage sheet = ImageIO.read(new File(path));
            for (int i = 0; i < labels.length; i++) {
                BufferedImage cell = sheet.getSubimage(srcX + i * cellW, srcY, cellW, cellH);
                doorSprites.put(labels[i], cell);
            }
        } catch (Exception ex) {
            System.out.println("Could not load door sheet: " + path + " — " + ex.getMessage());
        }
    }

    // ── paintComponent ────────────────────────────────────────────────────────
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        if (gameState.equals("MENU"))        { drawMenu(g);      return; }
        if (gameState.equals("HOW_TO_PLAY")) { drawHowToPlay(g); return; }

        // MAP TILES
        Graphics2D g2d = (Graphics2D) g;
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                int tx   = col * tileSize;
                int ty   = row * tileSize;
                int tile = map[row][col];

                if (tile == 0 && floorSprite != null) {
                    g2d.drawImage(floorSprite, tx, ty, tx + tileSize, ty + tileSize,
                        0, 0, floorSprite.getWidth(), floorSprite.getHeight(), null);
                    continue;
                }
                if (tile == 2 && escalatorSprite != null) {
                    g2d.drawImage(escalatorSprite, tx, ty, tx + tileSize, ty + tileSize,
                        576, 324, 576 + 386, 324 + 372, null);
                    continue;
                }
                if (tile == 3 && elevatorSprite != null) {
                    g2d.drawImage(elevatorSprite, tx, ty, tx + tileSize, ty + tileSize,
                        544, 294, 544 + 450, 294 + 393, null);
                    continue;
                }
                if (tile == 5 && wallSprite != null) {
                    g2d.drawImage(wallSprite, tx, ty, tx + tileSize, ty + tileSize,
                        0, 0, wallSprite.getWidth(), wallSprite.getHeight(), null);
                    continue;
                }
                if (tile == 8 && floorSprite != null) {
                    g2d.drawImage(floorSprite, tx, ty, tx + tileSize, ty + tileSize,
                        0, 0, floorSprite.getWidth(), floorSprite.getHeight(), null);
                    continue;
                }
                if (tile == 6 && kitchenSheet != null) {
                    int kitRow = row - KIT_ROW;
                    int kitCol = col - KIT_COL;
                    int srcX   = KIT_SRC_X + kitCol * KIT_CELL_W;
                    int srcY   = KIT_SRC_Y + kitRow * KIT_CELL_H;
                    g2d.drawImage(kitchenSheet,
                        tx, ty, tx + tileSize, ty + tileSize,
                        srcX, srcY, srcX + KIT_CELL_W, srcY + KIT_CELL_H, null);
                    continue;
                }

                switch (tile) {
                    case 0: g.setColor(C_FLOOR);        break;
                    case 1: g.setColor(C_WALL);         break;
                    case 2: g.setColor(C_ESCALATOR);    break;
                    case 3: g.setColor(C_ELEVATOR);     break;
                    case 4: g.setColor(C_ROOM);         break;
                    case 5: g.setColor(C_SEPARATOR);    break;
                    case 6: g.setColor(C_KITCHEN);      break;
                    case 7: g.setColor(C_EXIT);         break;
                    case 8: g.setColor(C_LOUNGE);       break;
                    default: g.setColor(Color.MAGENTA); break;
                }
                g.fillRect(tx, ty, tileSize, tileSize);
            }
        }

        // ROOM TILES — Mission 2: iterate rooms list
        g.setFont(new Font("Arial", Font.BOLD, 10));
        for (int i = 0; i < rooms.size(); i++) {
            Room r       = rooms.get(i);
            int rRow     = r.row;
            int rCol     = r.col;
            int tx       = rCol * tileSize;
            int ty       = rRow * tileSize;
            String label = r.number;

            boolean isTarget = false;
            for (int[] pos : orderPositions) {
                if (pos[0] == rRow && pos[1] == rCol) { isTarget = true; break; }
            }

            BufferedImage door = doorSprites.get(label);

            if (door != null) {
                g2d.drawImage(door, tx, ty, tx + tileSize, ty + tileSize,
                    0, 0, door.getWidth(), door.getHeight(), null);
                if (isTarget) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                    g2d.setColor(Color.RED);
                    g2d.fillRect(tx, ty, tileSize, tileSize);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                }
            } else {
                g.setColor(isTarget ? C_ROOM_RED : C_ROOM);
                g.fillRect(tx, ty, tileSize, tileSize);
                g.setColor(Color.BLACK);
                FontMetrics fm = g.getFontMetrics();
                int lx = tx + (tileSize - fm.stringWidth(label)) / 2;
                int ly = ty + ((tileSize - fm.getHeight()) / 2) + fm.getAscent();
                g.drawString(label, lx, ly);
            }
        }

        // PLAYER — Mission 1: delegate to waiter
        waiter.draw(g, tileSize);

        // HUD DIVIDER
        g.setColor(Color.WHITE);
        g.drawLine(600, 0, 600, HEIGHT);

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Hotel Food Service",       610, 25);
        g.drawString("Score: " + score,          620, 45);
        g.drawString("Orders: " + orderPositions.size() + "/" + activeOrderCount, 620, 63);

        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        for (int b = 0; b < MAX_DISSATISFACTION; b++) {
            g.setColor(b < dissatisfaction ? Color.RED : Color.DARK_GRAY);
            g.drawString("💢", 615 + b * 22, 88);
        }

        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(C_ESCALATOR);
        g.fillRect(618, 98, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(" Escalator (↑ only)", 628, 108);
        g.setColor(C_ELEVATOR);
        g.fillRect(618, 115, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(" Elevator  (↑ ↓)", 628, 125);

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Deliver to:", 620, 148);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        for (int i = 0; i < orderList.size(); i++) {
            FoodOrder o    = orderList.get(i);
            int framesLeft = orderTimers.get(i);
            int secsLeft   = (int) Math.ceil(framesLeft / 60.0);
            boolean urgent = secsLeft <= 3;
            g.setColor(urgent ? Color.RED : Color.WHITE);
            g.drawString((i + 1) + ". Room " + o.targetRoom
                + " (F" + o.targetFloor + ")  " + secsLeft + "s",
                620, 166 + i * 18);
        }

        // Riding status — Mission 1: use waiter fields
        if (waiter.isRiding) {
            boolean onEsc = (waiter.currentTransportSpeed == Waiter.ESCALATOR_SPEED);
            g.setColor(onEsc ? C_ESCALATOR : C_ELEVATOR);
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            g.drawString(onEsc ? "[ Escalator... ]" : "[ Elevator... ]", 612, HEIGHT - 20);
        }

        // GAME OVER OVERLAY
        if (gameState.equals("GAME_OVER")) drawGameOver(g);
    }

    // ── drawMenu ──────────────────────────────────────────────────────────────
    private void drawMenu(Graphics g) {
        g.setColor(new Color(15, 15, 25));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(80, 80, 120));
        g.drawRect(30, 20, WIDTH - 60, HEIGHT - 40);
        g.drawRect(34, 24, WIDTH - 68, HEIGHT - 48);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        String title = "Hotel Food Service";
        FontMetrics fmTitle = g.getFontMetrics();
        g.drawString(title, (WIDTH - fmTitle.stringWidth(title)) / 2, 160);

        g.setColor(new Color(160, 160, 200));
        g.setFont(new Font("Arial", Font.ITALIC, 15));
        String sub = "Deliver fast. Stay calm. Don't burn out.";
        FontMetrics fmSub = g.getFontMetrics();
        g.drawString(sub, (WIDTH - fmSub.stringWidth(sub)) / 2, 190);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        String hi = "Hi-Score: " + highScore;
        FontMetrics fmHi = g.getFontMetrics();
        g.drawString(hi, (WIDTH - fmHi.stringWidth(hi)) / 2, 230);

        int bw = 220, bh = 45, bx = (WIDTH - bw) / 2;
        int[] buttonY = { 268, 328, 388 };
        for (int i = 0; i < MENU_BUTTONS.length; i++) {
            boolean sel = (i == selectedButton);
            g.setColor(sel ? new Color(230, 200, 40) : new Color(45, 45, 65));
            g.fillRect(bx, buttonY[i], bw, bh);
            g.setColor(sel ? Color.WHITE : new Color(100, 100, 140));
            g.drawRect(bx, buttonY[i], bw, bh);
            g.setColor(sel ? Color.BLACK : Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            int tx = bx + (bw - fm.stringWidth(MENU_BUTTONS[i])) / 2;
            int ty = buttonY[i] + (bh + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(MENU_BUTTONS[i], tx, ty);
            if (sel) {
                g.setColor(Color.BLACK);
                g.drawString("▶", bx - 22, ty);
                g.drawString("◀", bx + bw + 6, ty);
            }
        }

        g.setColor(new Color(120, 120, 150));
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        String hint = "↑ ↓ to navigate   ENTER to select";
        FontMetrics fmHint = g.getFontMetrics();
        g.drawString(hint, (WIDTH - fmHint.stringWidth(hint)) / 2, HEIGHT - 35);
    }

    // ── drawHowToPlay ─────────────────────────────────────────────────────────
    private void drawHowToPlay(Graphics g) {
        g.setColor(new Color(15, 15, 25));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(80, 80, 120));
        g.drawRect(30, 20, WIDTH - 60, HEIGHT - 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        String title = "How To Play";
        FontMetrics fmT = g.getFontMetrics();
        g.drawString(title, (WIDTH - fmT.stringWidth(title)) / 2, 70);
        g.setColor(new Color(80, 80, 120));
        g.drawLine(80, 82, WIDTH - 80, 82);

        String[][] sections = {
            { "Controls",
              "Arrow Keys       Move the waiter",
              "UP on Escalator  Go up one floor (slow)",
              "UP on Elevator   Go up one floor (fast)",
              "DOWN on Elevator Go down one floor (fast)" },
            { "Objective",
              "Deliver food orders to the highlighted rooms",
              "Each order has a countdown — deliver before it expires",
              "Expiring an order depletes one 💢 from the bar",
              "All 5 bars drained = Game Over" },
            { "Difficulty",
              "Every 5 deliveries, an extra order is added to the batch",
              "Use the elevator (yellow) when speed matters",
              "Use the escalator (blue) only when going up" },
        };

        int y = 110;
        for (String[] section : sections) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 15));
            g.drawString(section[0], 80, y);
            y += 4;
            g.setColor(new Color(80, 80, 120));
            g.drawLine(80, y, WIDTH - 80, y);
            y += 16;
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            for (int i = 1; i < section.length; i++) {
                g.drawString("• " + section[i], 95, y);
                y += 19;
            }
            y += 10;
        }

        g.setColor(new Color(120, 120, 150));
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        String hint = "Press ENTER or ESC to go back";
        FontMetrics fmH = g.getFontMetrics();
        g.drawString(hint, (WIDTH - fmH.stringWidth(hint)) / 2, HEIGHT - 35);
    }

    // ── drawGameOver ──────────────────────────────────────────────────────────
    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        String title = "GAME OVER";
        FontMetrics fm1 = g.getFontMetrics();
        g.drawString(title, (WIDTH - fm1.stringWidth(title)) / 2, 180);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        String scoreText = "Score: " + score;
        FontMetrics fm2 = g.getFontMetrics();
        g.drawString(scoreText, (WIDTH - fm2.stringWidth(scoreText)) / 2, 230);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        String hiText = "Hi-Score: " + highScore;
        FontMetrics fm3 = g.getFontMetrics();
        g.drawString(hiText, (WIDTH - fm3.stringWidth(hiText)) / 2, 260);

        int bw = 240, bh = 50, bx = (WIDTH - 240) / 2, by = 310;
        g.setColor(new Color(60, 60, 60));
        g.fillRect(bx, by, bw, bh);
        g.setColor(Color.WHITE);
        g.drawRect(bx, by, bw, bh);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        String btn = "Press ENTER → Main Menu";
        FontMetrics fm4 = g.getFontMetrics();
        g.drawString(btn, bx + (bw - fm4.stringWidth(btn)) / 2, by + 32);
    }
}
