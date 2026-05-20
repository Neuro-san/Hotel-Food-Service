import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

public class GamePanel extends JPanel {

    final int WIDTH  = 800;
    final int HEIGHT = 600;
    final int tileSize = 40;
    final int gapBetweenFloors = 2;

    // GAME STATE
    // States: "MENU" → "PLAYING" → "GAME_OVER" → "MENU"
    //         "MENU" → "HOW_TO_PLAY" → "MENU"
    String gameState = "MENU";

    // MENU
    static final String[] MENU_BUTTONS = { "Play", "How to Play", "Quit" };
    int selectedButton = 0;   // 0=Play  1=How To Play  2=Quit

    // HI-SCORE — loaded from hiscore.txt, saved on game over
    int highScore = 0;

    // SCORE
    int score = 0;

    // DISSATISFACTION BAR — 5 starts full, game over at 0, no way to restore
    int dissatisfaction = 5;
    static final int MAX_DISSATISFACTION = 5;

    // DIFFICULTY — starts at 1 order, grows by 1 every milestone
    int activeOrderCount = 1;

    // ACTIVE ORDERS — orderTimers is parallel to orderList/orderPositions
    ArrayList<int[]>     orderPositions = new ArrayList<>();
    ArrayList<FoodOrder> orderList      = new ArrayList<>();
    ArrayList<Integer>   orderTimers    = new ArrayList<>();  // frames remaining per order

    Random rand = new Random();

    // ── TILE TYPES ────────────────────────────────────────────────────────────
    // 0 = walkable floor       (black)
    // 1 = outer wall           (dark gray,  impassable)
    // 2 = escalator            (blue,       UP only,  40 frames/row)
    // 3 = elevator             (yellow,     both directions, 20 frames/row)
    // 4 = green room           (green,      delivery target, top/bottom entry only)
    // 5 = white wall separator (white,      impassable — blocks horizontal room entry)
    // 6 = kitchen              (gray,       impassable)
    // 7 = exit                 (brown,      decorative, walkable)
    // 8 = lounge               (black,      walkable)
    // ─────────────────────────────────────────────────────────────────────────

    // Tile colors
    static final Color C_FLOOR     = Color.BLACK;
    static final Color C_WALL      = Color.DARK_GRAY;
    static final Color C_ESCALATOR = Color.BLUE;
    static final Color C_ELEVATOR  = Color.YELLOW;
    static final Color C_ROOM      = Color.GREEN;
    static final Color C_ROOM_RED  = Color.RED;
    static final Color C_SEPARATOR = Color.WHITE;
    static final Color C_KITCHEN   = Color.GRAY;
    static final Color C_EXIT      = new Color(139, 90, 43);  // brown
    static final Color C_LOUNGE    = Color.BLACK;

    // ROOM POSITIONS — built dynamically by scanning map for tile 4
    // Adding or removing tile 4 from the map automatically updates rooms and labels
    ArrayList<int[]>  roomPositions = new ArrayList<>();
    ArrayList<String> roomLabels   = new ArrayList<>();

    // MAP (15 cols × 15 rows)
    //
    // Room row    : { 1, 4, 5, 4, 5, 4, 5, 4, 5, 4, 5, 4, 5, 4, 1 }
    //   Rooms at odd cols (1,3,5,7,9,11,13)
    //   White wall separators at even cols (2,4,6,8,10,12)
    //
    // Corridor    : { 1, 0, 0, 0, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 1 }
    //   Escalator (2) at col 5, Elevator (3) at col 7
    int[][] map = {

        // ── FLOOR 3 ──────────────────────────────────────────────────────────
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},  // row  0  outer wall
        {1, 4, 5, 4, 5, 4, 5, 4, 5, 4, 5, 4, 5, 4, 1},  // row  1  rooms
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},  // row  2  corridor
        {1, 5, 4, 5, 4, 5, 2, 5, 3, 5, 4, 5, 4, 5, 1},  // row  3  rooms
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},  // row  4  outer wall

        // ── FLOOR 2 ──────────────────────────────────────────────────────────
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},  // row  5  outer wall
        {1, 5, 4, 5, 4, 5, 2, 5, 3, 5, 4, 5, 4, 5, 1},  // row  6  rooms
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},  // row  7  corridor
        {1, 5, 4, 5, 4, 5, 2, 5, 3, 5, 4, 5, 4, 5, 1},  // row  8  rooms
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},  // row  9  outer wall

        // ── FLOOR 1 ──────────────────────────────────────────────────────────
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},  // row 10  outer wall
        {1, 6, 6, 6, 8, 8, 2, 8, 3, 8, 8, 8, 8, 8, 1},  // row 11  kitchen(1-3), lounge
        {1, 6, 6, 6, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},  // row 12  kitchen(1-3), corridor
        {1, 6, 6, 6, 8, 8, 8, 7, 8, 8, 8, 8, 8, 8, 1},  // row 13  kitchen(1-3), exit(7), lounge
        {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},  // row 14  outer wall
    };

    // PLAYER — starts in floor 1 corridor, right of elevator
    int playerCol = 9;
    int playerRow = 12;

    // TRANSPORT
    static final int ESCALATOR_SPEED = 40;  // frames per row (UP only)
    static final int ELEVATOR_SPEED  = 20;  // frames per row (both directions)
    final int HIGHEST_FLOOR = 2;            // corridor row of floor 3
    final int LOWEST_FLOOR  = 12;           // corridor row of floor 1

    boolean isRiding              = false;
    int     elevatorDirection     = 0;
    int     elevatorTarget        = 0;
    int     elevatorFrameCounter  = 0;
    int     currentTransportSpeed = ELEVATOR_SPEED;

    // SPRITE ANIMATION
    // 011.png: 2 cols × 4 rows, 32x32 per frame
    // Row 0=DOWN | Row 1=LEFT | Row 2=RIGHT | Row 3=UP
    static final int FRAME_W     = 32;
    static final int FRAME_H     = 32;
    static final int DIR_DOWN    = 0;
    static final int DIR_LEFT    = 1;
    static final int DIR_RIGHT   = 2;
    static final int DIR_UP      = 3;
    static final int WALK_FRAMES = 2;
    static final int IDLE_FRAMES = 1;
    static final int ANIM_SPEED  = 8;

    BufferedImage spriteSheet;

    // Kitchen sprite sheet — sliced from assets/kitchen.png
    // Source image content area: left=124, top=160, each cell=258x225px
    BufferedImage kitchenSheet;
    static final int KIT_SRC_X  = 124;   // content left edge in image
    static final int KIT_SRC_Y  = 160;   // content top edge in image
    static final int KIT_CELL_W = 258;   // width of one cell in image
    static final int KIT_CELL_H = 225;   // height of one cell in image
    static final int KIT_ROW    = 11;    // map row where kitchen starts
    static final int KIT_COL    = 1;     // map col where kitchen starts

    int     playerDirection = DIR_DOWN;
    boolean isMoving        = false;
    int     animFrame       = 0;
    int     animTick        = 0;

    // ─────────────────────────────────────────────────────────────────────────

    GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        loadSprites();
        loadHighScore();
        buildRoomData();
        // generateOrders() is NOT called here — called by startGame() when Play is pressed

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

                // ── MENU ─────────────────────────────────────────────────────
                if (gameState.equals("MENU")) {
                    if (key == KeyEvent.VK_UP) {
                        selectedButton = (selectedButton - 1 + MENU_BUTTONS.length)
                                         % MENU_BUTTONS.length;
                    } else if (key == KeyEvent.VK_DOWN) {
                        selectedButton = (selectedButton + 1) % MENU_BUTTONS.length;
                    } else if (key == KeyEvent.VK_ENTER) {
                        switch (selectedButton) {
                            case 0 -> startGame();// Play
                            case 1 -> gameState = "HOW_TO_PLAY";  // How To Play
                            case 2 -> System.exit(0);  // Quit
                        }
                    }
                    return;
                }

                // ── HOW TO PLAY ───────────────────────────────────────────────
                if (gameState.equals("HOW_TO_PLAY")) {
                    if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) {
                        gameState = "MENU";
                    }
                    return;
                }

                // ── GAME OVER ─────────────────────────────────────────────────
                if (gameState.equals("GAME_OVER")) {
                    if (key == KeyEvent.VK_ENTER) gameState = "MENU";
                    return;
                }

                if (isRiding) return;

                int nextRow     = playerRow;
                int nextCol     = playerCol;
                int currentTile = map[playerRow][playerCol];

                // ── ESCALATOR (tile 2) — UP only ─────────────────────────────
                if (currentTile == 2 && key == KeyEvent.VK_UP 
                        && playerRow != HIGHEST_FLOOR) {
                    playerDirection       = DIR_UP;
                    isRiding              = true;
                    elevatorDirection     = -1;
                    elevatorTarget        = playerRow - (gapBetweenFloors + 1 + 1);     // calculation: gapBetweenFloors + escalatorTileUp + escalatorTileBottom
                    elevatorFrameCounter  = 0;
                    currentTransportSpeed = ESCALATOR_SPEED;
                    isMoving              = true;
                    return;
                }

                // ── ELEVATOR (tile 3) — both directions ──────────────────────
                if (currentTile == 3) {
                    if (key == KeyEvent.VK_UP && playerRow != HIGHEST_FLOOR) {
                        playerDirection       = DIR_UP;
                        isRiding              = true;
                        elevatorDirection     = -1;
                        elevatorTarget        = playerRow - (gapBetweenFloors + 1 + 1);     // calculation: gapBetweenFloors - elevatorTileUp - elevatorTileBottom;
                        elevatorFrameCounter  = 0;
                        currentTransportSpeed = ELEVATOR_SPEED;
                        isMoving              = true;
                        return;
                    }
                    if (key == KeyEvent.VK_DOWN && playerRow != LOWEST_FLOOR) {
                        playerDirection       = DIR_DOWN;
                        isRiding              = true;
                        elevatorDirection     = 1;
                        elevatorTarget        = playerRow + (gapBetweenFloors + 1 + 1);     // calculation: gapBetweenFloors - elevatorTileUp - elevatorTileBottom;
                        elevatorFrameCounter  = 0;
                        currentTransportSpeed = ELEVATOR_SPEED;
                        isMoving              = true;
                        return;
                    }
                }

                // ── NORMAL MOVEMENT ───────────────────────────────────────────
                if      (key == KeyEvent.VK_UP)    { nextRow--; playerDirection = DIR_UP;    }
                else if (key == KeyEvent.VK_DOWN)  { nextRow++; playerDirection = DIR_DOWN;  }
                else if (key == KeyEvent.VK_LEFT)  { nextCol--; playerDirection = DIR_LEFT;  }
                else if (key == KeyEvent.VK_RIGHT) { nextCol++; playerDirection = DIR_RIGHT; }

                int targetTile = map[nextRow][nextCol];
                boolean passable = targetTile != 1   // outer wall
                                && targetTile != 5   // white room separator
                                && targetTile != 6;  // kitchen

                if (passable) {
                    playerRow = nextRow;
                    playerCol = nextCol;
                    isMoving  = true;
                } else {
                    isMoving = false;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_UP   || key == KeyEvent.VK_DOWN ||
                    key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                    isMoving  = false;
                    animFrame = 0;
                    animTick  = 0;
                }
            }
        });
    }

    // ── update ────────────────────────────────────────────────────────────────
    public void update() {
        if (!gameState.equals("PLAYING")) return;

        // TRANSPORT (escalator or elevator)
        if (isRiding) {
            elevatorFrameCounter++;
            if (elevatorFrameCounter >= currentTransportSpeed) {
                playerRow += elevatorDirection;
                elevatorFrameCounter = 0;
                if (playerRow == elevatorTarget) {
                    isRiding = false;
                    isMoving = false;
                }
            }
            advanceAnimation(WALK_FRAMES);
            return;
        }

        // ANIMATION
        advanceAnimation(isMoving ? WALK_FRAMES : IDLE_FRAMES);

        // PER-ORDER DISSATISFACTION TIMERS — tick each order down independently
        for (int i = orderTimers.size() - 1; i >= 0; i--) {
            orderTimers.set(i, orderTimers.get(i) - 1);

            if (orderTimers.get(i) <= 0) {
                // Order expired — remove it, lose 1 bar, replace with fresh order
                orderPositions.remove(i);
                orderList.remove(i);
                orderTimers.remove(i);
                dissatisfaction--;

                if (dissatisfaction <= 0) {
                    saveHighScore();
                    gameState = "GAME_OVER";

                   try {
                        File audioFile = new File("assets/game_over.wav");
                        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                        Clip clip = AudioSystem.getClip();
                        clip.open(audioStream);
                        clip.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }

                addNewOrder();  // immediately replace the expired order
                try {
                    File audioFile = new File("assets/order_expiry.wav");
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    clip.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            }
        }

        // DELIVERY CHECK — scan all active orders backwards
        for (int i = orderPositions.size() - 1; i >= 0; i--) {
            int[] pos = orderPositions.get(i);
            if (playerRow == pos[0] && playerCol == pos[1]) {
                orderList.get(i).deliver();
                orderPositions.remove(i);
                orderList.remove(i);
                orderTimers.remove(i);
                score++;
                try {
                    File audioFile = new File("assets/delivery_success.wav");
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    clip.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // BATCH CLEARED — milestone check then generate next batch
        if (orderPositions.isEmpty()) {
            if (score > 0 && score % 5 == 0) {
                activeOrderCount++;
            }
            generateOrders();
        }
    }

    // ── buildRoomData ─────────────────────────────────────────────────────────
    // Floor 3 (rows 0-4): 301, 302, 303...
    // Floor 2 (rows 5-9): 201, 202, 203...
    private void buildRoomData() {
        roomPositions.clear();
        roomLabels.clear();

        int floor3Count = 0;
        int floor2Count = 0;

        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col] == 4) {
                    roomPositions.add(new int[]{row, col});
                    if (row < 5) {
                        floor3Count++;
                        roomLabels.add(String.format("3%02d", floor3Count));
                    } else if (row < 10) {
                        floor2Count++;
                        roomLabels.add(String.format("2%02d", floor2Count));
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
            int   pick;
            int[] pos;
            do {
                pick = rand.nextInt(roomPositions.size());
                pos  = roomPositions.get(pick);
            } while (
                usedIndices.contains(pick) ||
                (pos[0] == playerRow && pos[1] == playerCol)
            );

            usedIndices.add(pick);
            orderPositions.add(new int[]{pos[0], pos[1]});

            int floor = (pos[0] < 5) ? 3 : 2;
            orderList.add(new FoodOrder(roomLabels.get(pick), floor, 0));
            orderTimers.add(randomOrderTimer());
        }
    }

    // ── addNewOrder ───────────────────────────────────────────────────────────
    // Adds one replacement order, avoiding all currently active positions
    // and the player's current tile.
    private void addNewOrder() {
        int   pick;
        int[] pos;
        do {
            pick = rand.nextInt(roomPositions.size());
            pos  = roomPositions.get(pick);
        } while (isPositionTaken(pos[0], pos[1]));

        orderPositions.add(new int[]{pos[0], pos[1]});
        int floor = (pos[0] < 5) ? 3 : 2;
        orderList.add(new FoodOrder(roomLabels.get(pick), floor, 0));
        orderTimers.add(randomOrderTimer());
    }

    // Returns a random frame count between 5 and 10 seconds (300–600 frames at 60fps)
    private int randomOrderTimer() {
        return rand.nextInt(301) + 300;
    }

    // Returns true if the given tile is already an active order target or the player's tile
    private boolean isPositionTaken(int row, int col) {
        if (row == playerRow && col == playerCol) return true;
        for (int[] pos : orderPositions) {
            if (pos[0] == row && pos[1] == col) return true;
        }
        return false;
    }

    // ── advanceAnimation ──────────────────────────────────────────────────────
    private void advanceAnimation(int totalFrames) {
        animTick++;
        if (animTick >= ANIM_SPEED) {
            animTick  = 0;
            animFrame = (animFrame + 1) % totalFrames;
        }
    }

    // ── startGame ─────────────────────────────────────────────────────────────
    // Called when Play is selected from the menu — resets everything fresh.
    private void startGame() {
        playerRow            = 12;
        playerCol            = 9;
        playerDirection      = DIR_DOWN;
        isMoving             = false;
        animFrame            = 0;
        animTick             = 0;
        score                = 0;
        dissatisfaction      = MAX_DISSATISFACTION;
        activeOrderCount     = 1;
        isRiding             = false;
        elevatorFrameCounter = 0;
        selectedButton       = 0;
        gameState            = "PLAYING";
        generateOrders();
    }

    // ── loadHighScore / saveHighScore ─────────────────────────────────────────
    private void loadHighScore() {
        try {
            Scanner sc = new Scanner(new File("hiscore.txt"));
            if (sc.hasNextInt()) highScore = sc.nextInt();
            sc.close();
        } catch (Exception ex) {
            highScore = 0;  // file not found yet — first run
        }
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

    // ── Sprite loading ────────────────────────────────────────────────────────
    private void loadSprites() {
        try {
            spriteSheet = makeTransparent(ImageIO.read(new File("assets/011.png")));
        } catch (Exception ex) {
            System.out.println("Player sprite not found — rectangle fallback active. " + ex.getMessage());
        }
        try {
            kitchenSheet = ImageIO.read(new File("assets/kitchen.png"));
        } catch (Exception ex) {
            System.out.println("Kitchen sprite not found — solid colour fallback active. " + ex.getMessage());
        }
    }

    // Near-black threshold (< 20 per channel) — handles 011.png background (2,4,3)
    private BufferedImage makeTransparent(BufferedImage src) {
        BufferedImage dst = new BufferedImage(
            src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int px = src.getRGB(x, y);
                int r  = (px >> 16) & 0xFF;
                int g  = (px >>  8) & 0xFF;
                int b  =  px        & 0xFF;
                dst.setRGB(x, y, (r < 20 && g < 20 && b < 20) ? 0x00000000 : px);
            }
        }
        return dst;
    }

    // ── paintComponent ────────────────────────────────────────────────────────
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // BACKGROUND
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Route to correct screen
        if (gameState.equals("MENU"))        { drawMenu(g);      return; }
        if (gameState.equals("HOW_TO_PLAY")) { drawHowToPlay(g); return; }

        // MAP TILES
        Graphics2D g2d = (Graphics2D) g;
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                int tx = col * tileSize;
                int ty = row * tileSize;

                // Tile 6 (kitchen) — draw sprite sheet cell if loaded
                if (map[row][col] == 6 && kitchenSheet != null) {
                    int kitRow = row - KIT_ROW;
                    int kitCol = col - KIT_COL;
                    int srcX   = KIT_SRC_X + kitCol * KIT_CELL_W;
                    int srcY   = KIT_SRC_Y + kitRow * KIT_CELL_H;
                    g2d.drawImage(kitchenSheet,
                        tx,             ty,
                        tx + tileSize,  ty + tileSize,
                        srcX,           srcY,
                        srcX + KIT_CELL_W, srcY + KIT_CELL_H,
                        null);
                    continue;
                }

                switch (map[row][col]) {
                    case 0: g.setColor(C_FLOOR);     break;
                    case 1: g.setColor(C_WALL);      break;
                    case 2: g.setColor(C_ESCALATOR); break;
                    case 3: g.setColor(C_ELEVATOR);  break;
                    case 4: g.setColor(C_ROOM);      break;
                    case 5: g.setColor(C_SEPARATOR); break;
                    case 6: g.setColor(C_KITCHEN);   break;  // fallback if no sprite
                    case 7: g.setColor(C_EXIT);      break;
                    case 8: g.setColor(C_LOUNGE);    break;
                    default: g.setColor(Color.MAGENTA); break;
                }
                g.fillRect(tx, ty, tileSize, tileSize);
            }
        }

        // ROOM TILES — drawn from map scan, not hardcoded positions
        g.setFont(new Font("Arial", Font.BOLD, 10));
        for (int i = 0; i < roomPositions.size(); i++) {
            int rRow = roomPositions.get(i)[0];
            int rCol = roomPositions.get(i)[1];

            boolean isTarget = false;
            for (int[] pos : orderPositions) {
                if (pos[0] == rRow && pos[1] == rCol) { isTarget = true; break; }
            }

            if (isTarget) {
                g.setColor(C_ROOM_RED);
                g.fillRect(rCol * tileSize, rRow * tileSize, tileSize, tileSize);
            }

            g.setColor(Color.BLACK);
            String label = roomLabels.get(i);
            FontMetrics fm = g.getFontMetrics();
            int tx = (rCol * tileSize) + (tileSize - fm.stringWidth(label)) / 2;
            int ty = (rRow * tileSize) + ((tileSize - fm.getHeight()) / 2) + fm.getAscent();
            g.drawString(label, tx, ty);
        }

        // TRANSPORT LABELS
        g.setFont(new Font("Arial", Font.BOLD, 8));
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col] == 2) {
                    g.setColor(Color.WHITE);
                    g.drawString("ESC", col * tileSize + 4, row * tileSize + 14);
                    g.drawString("↑",   col * tileSize + 11, row * tileSize + 28);
                } else if (map[row][col] == 3) {
                    g.setColor(Color.BLACK);
                    g.drawString("ELV", col * tileSize + 4, row * tileSize + 14);
                    g.drawString("↕",   col * tileSize + 11, row * tileSize + 28);
                }
            }
        }

        // FLOOR 1 ZONE LABELS
        g.setFont(new Font("Arial", Font.BOLD, 9));
        g.setColor(Color.WHITE);
        g.drawString("KITCHEN", 1 * tileSize + 2,  12 * tileSize + 20);
        g.drawString("EXIT",    7 * tileSize + 6,  13 * tileSize + 24);
        g.setColor(Color.GRAY);
        g.drawString("LOUNGE", 10 * tileSize,      12 * tileSize + 20);

        // PLAYER
        drawPlayer(g);

        // HUD DIVIDER
        g.setColor(Color.WHITE);
        g.drawLine(600, 0, 600, HEIGHT);

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Hotel Food Service",            610, 25);
        g.drawString("Score: "      + score,          620, 45);
        g.drawString("Orders: " + orderPositions.size()
                     + "/" + activeOrderCount,         620, 63);

        // DISSATISFACTION BAR — 💢 per remaining bar, grayed out when lost
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        for (int b = 0; b < MAX_DISSATISFACTION; b++) {
            g.setColor(b < dissatisfaction ? Color.RED : Color.DARK_GRAY);
            g.drawString("💢", 615 + b * 22, 88);
        }

        // Transport legend
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(C_ESCALATOR);
        g.fillRect(618, 98, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(" Escalator (↑ only)", 628, 108);
        g.setColor(C_ELEVATOR);
        g.fillRect(618, 115, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(" Elevator  (↑ ↓)",    628, 125);

        // Active orders list with per-order dissatisfaction timers
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Deliver to:", 620, 148);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        for (int i = 0; i < orderList.size(); i++) {
            FoodOrder o       = orderList.get(i);
            int framesLeft    = orderTimers.get(i);
            int secsLeft      = (int) Math.ceil(framesLeft / 60.0);
            boolean urgent    = secsLeft <= 3;

            // Timer colour: red when urgent, white otherwise
            g.setColor(urgent ? Color.RED : Color.WHITE);
            g.drawString(
                (i + 1) + ". Room " + o.targetRoom
                + " (F" + o.targetFloor + ")  " + secsLeft + "s",
                620, 166 + i * 18
            );
        }

        // Riding status — bottom of HUD
        if (isRiding) {
            boolean onEsc = (currentTransportSpeed == ESCALATOR_SPEED);
            g.setColor(onEsc ? C_ESCALATOR : C_ELEVATOR);
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            g.drawString(onEsc ? "[ Escalator... ]" : "[ Elevator... ]",
                         612, HEIGHT - 20);
        }

        // GAME OVER OVERLAY
        if (gameState.equals("GAME_OVER")) drawGameOver(g);
    }

    // ── drawPlayer ────────────────────────────────────────────────────────────
    private void drawPlayer(Graphics g) {
        int px = playerCol * tileSize;
        int py = playerRow * tileSize;

        if (spriteSheet == null) {
            g.setColor(isRiding ? Color.CYAN : Color.BLUE);
            g.fillRect(px, py, tileSize, tileSize);
            return;
        }

        int srcX = animFrame       * FRAME_W;
        int srcY = playerDirection * FRAME_H;

        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(spriteSheet,
            px,            py,
            px + tileSize, py + tileSize,
            srcX,          srcY,
            srcX + FRAME_W, srcY + FRAME_H,
            null);
    }

    // ── drawMenu ──────────────────────────────────────────────────────────────
    private void drawMenu(Graphics g) {

        // Background
        g.setColor(new Color(15, 15, 25));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Decorative border
        g.setColor(new Color(80, 80, 120));
        g.drawRect(30, 20, WIDTH - 60, HEIGHT - 40);
        g.drawRect(34, 24, WIDTH - 68, HEIGHT - 48);

        // Game title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        String title = "Hotel Food Service";
        FontMetrics fmTitle = g.getFontMetrics();
        g.drawString(title, (WIDTH - fmTitle.stringWidth(title)) / 2, 160);

        // Subtitle
        g.setColor(new Color(160, 160, 200));
        g.setFont(new Font("Arial", Font.ITALIC, 15));
        String sub = "Deliver fast. Stay calm. Don't burn out.";
        FontMetrics fmSub = g.getFontMetrics();
        g.drawString(sub, (WIDTH - fmSub.stringWidth(sub)) / 2, 190);

        // Hi-score
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        String hi = "Hi-Score: " + highScore;
        FontMetrics fmHi = g.getFontMetrics();
        g.drawString(hi, (WIDTH - fmHi.stringWidth(hi)) / 2, 230);

        // Buttons
        int bw = 220, bh = 45, bx = (WIDTH - bw) / 2;
        int[] buttonY = { 268, 328, 388 };

        for (int i = 0; i < MENU_BUTTONS.length; i++) {
            boolean sel = (i == selectedButton);

            // Button background
            g.setColor(sel ? new Color(230, 200, 40) : new Color(45, 45, 65));
            g.fillRect(bx, buttonY[i], bw, bh);

            // Button border
            g.setColor(sel ? Color.WHITE : new Color(100, 100, 140));
            g.drawRect(bx, buttonY[i], bw, bh);

            // Button text
            g.setColor(sel ? Color.BLACK : Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            int tx = bx + (bw - fm.stringWidth(MENU_BUTTONS[i])) / 2;
            int ty = buttonY[i] + (bh + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(MENU_BUTTONS[i], tx, ty);

            // Selection arrows
            if (sel) {
                g.setColor(Color.BLACK);
                g.drawString("▶", bx - 22, ty);
                g.drawString("◀", bx + bw + 6, ty);
            }
        }

        // Navigation hint
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

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        String title = "How To Play";
        FontMetrics fmT = g.getFontMetrics();
        g.drawString(title, (WIDTH - fmT.stringWidth(title)) / 2, 70);

        // Divider
        g.setColor(new Color(80, 80, 120));
        g.drawLine(80, 82, WIDTH - 80, 82);

        // Content
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
            // Section heading
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 15));
            g.drawString(section[0], 80, y);
            y += 4;
            g.setColor(new Color(80, 80, 120));
            g.drawLine(80, y, WIDTH - 80, y);
            y += 16;

            // Section lines
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            for (int i = 1; i < section.length; i++) {
                g.drawString("• " + section[i], 95, y);
                y += 19;
            }
            y += 10;
        }

        // Back hint
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