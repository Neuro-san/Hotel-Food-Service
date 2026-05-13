import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

public class GamePanel extends JPanel {

    final int WIDTH  = 800;
    final int HEIGHT = 600;
    int tileSize = 40;

    // GAME STATE
    String gameState = "PLAYING";

    // SCORE
    int score = 0;

    // TIMER — base 10s, grows by 5 every milestone
    int baseTime = 10;
    int timeLeft = baseTime;
    int frameCounter = 0;

    // DIFFICULTY — starts at 1 order, grows by 1 every milestone
    int activeOrderCount = 1;

    // ACTIVE ORDERS
    // Each entry is {row, col} on the map — parallel to orderList
    ArrayList<int[]>    orderPositions = new ArrayList<>();
    ArrayList<FoodOrder> orderList     = new ArrayList<>();

    Random rand = new Random();

    // ROOM POSITIONS (tile coordinates)
    int[][] rooms = {
        // FLOOR 3
        {1,  1}, {1,  3}, {1,  5}, {1,  7}, {1,  9}, {1, 11}, {1, 13},
        {3,  1}, {3,  3}, {3,  5}, {3,  7}, {3,  9}, {3, 11}, {3, 13},
        // FLOOR 2
        {6,  1}, {6,  3}, {6,  5}, {6,  7}, {6,  9}, {6, 11}, {6, 13},
        {8,  1}, {8,  3}, {8,  5}, {8,  7}, {8,  9}, {8, 11}, {8, 13},
        // FLOOR 1
        {11, 1}, {11, 3}, {11, 5}, {11, 7}, {11, 9}, {11,11}, {11,13},
        {13, 1}, {13, 3}, {13, 5}, {13, 7}, {13, 9}, {13,11}, {13,13},
    };

    String[] roomNames = {
        "301","302","303","304","305","306","307",
        "314","313","312","311","310","309","308",
        "201","202","203","204","205","206","207",
        "214","213","212","211","210","209","208",
        "101","102","103","104","105","106","107",
        "114","113","112","111","110","109","108",
    };

    int[][] map = {
        // FLOOR 3
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {2,0,0,0,0,0,0,0,0,0,0,0,0,0,2},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        // FLOOR 2
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {2,0,0,0,0,0,0,0,0,0,0,0,0,0,2},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        // FLOOR 1
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {2,0,0,0,0,0,0,0,0,0,0,0,0,0,2},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
    };

    // PLAYER
    int playerCol = 5;
    int playerRow = 13;

    // ELEVATOR
    boolean isRiding          = false;
    int     elevatorTarget    = 0;
    int     elevatorDirection = 0;
    int     elevatorFrameCounter = 0;
    final int ELEVATOR_SPEED  = 20;
    final int HIGHEST_FLOOR   = 2;
    final int LOWEST_FLOOR    = 12;

    // SPRITE ANIMATION
    // 011.png layout: 2 columns × 4 rows, each frame 32x32
    // Row 0 = DOWN | Row 1 = LEFT | Row 2 = RIGHT | Row 3 = UP
    static final int FRAME_W     = 32;
    static final int FRAME_H     = 32;
    static final int DIR_DOWN    = 0;
    static final int DIR_LEFT    = 1;
    static final int DIR_RIGHT   = 2;
    static final int DIR_UP      = 3;
    static final int WALK_FRAMES = 2;   // 011.png has 2 frames per direction
    static final int IDLE_FRAMES = 1;   // use frame 0 only when standing still
    static final int ANIM_SPEED  = 8;

    BufferedImage spriteSheet;          // single sheet replaces walk + idle

    int     playerDirection = DIR_DOWN;
    boolean isMoving        = false;
    int     animFrame       = 0;
    int     animTick        = 0;

    // ─────────────────────────────────────────────────────────────────────────

    GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        loadSprites();
        generateOrders();   // generate initial batch

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
                if (gameState.equals("GAME_OVER")) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) restartGame();
                    return;
                }
                if (isRiding) return;

                int key     = e.getKeyCode();
                int nextRow = playerRow;
                int nextCol = playerCol;

                // ON ELEVATOR TILE
                if (map[playerRow][playerCol] == 2) {
                    if (key == KeyEvent.VK_UP && playerRow != HIGHEST_FLOOR) {
                        playerDirection      = DIR_UP;
                        isRiding             = true;
                        elevatorDirection    = -1;
                        elevatorTarget       = playerRow - 5;
                        elevatorFrameCounter = 0;
                        isMoving             = true;
                        return;
                    }
                    if (key == KeyEvent.VK_DOWN && playerRow != LOWEST_FLOOR) {
                        playerDirection      = DIR_DOWN;
                        isRiding             = true;
                        elevatorDirection    = 1;
                        elevatorTarget       = playerRow + 5;
                        elevatorFrameCounter = 0;
                        isMoving             = true;
                        return;
                    }
                }

                // NORMAL MOVEMENT
                if      (key == KeyEvent.VK_UP)    { nextRow--; playerDirection = DIR_UP;    }
                else if (key == KeyEvent.VK_DOWN)  { nextRow++; playerDirection = DIR_DOWN;  }
                else if (key == KeyEvent.VK_LEFT)  { nextCol--; playerDirection = DIR_LEFT;  }
                else if (key == KeyEvent.VK_RIGHT) { nextCol++; playerDirection = DIR_RIGHT; }

                if (map[nextRow][nextCol] != 1) {
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

        // ELEVATOR
        if (isRiding) {
            elevatorFrameCounter++;
            if (elevatorFrameCounter >= ELEVATOR_SPEED) {
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

        // TIMER
        frameCounter++;
        if (frameCounter >= 60) {
            timeLeft--;
            frameCounter = 0;
        }
        if (timeLeft <= 0) {
            gameState = "GAME_OVER";
            return;
        }

        // DELIVERY CHECK — scan all active orders
        for (int i = orderPositions.size() - 1; i >= 0; i--) {
            int[] pos = orderPositions.get(i);
            if (playerRow == pos[0] && playerCol == pos[1]) {
                orderList.get(i).deliver();
                orderPositions.remove(i);
                orderList.remove(i);
                score++;
            }
        }

        // ALL ORDERS CLEARED — check milestone then generate next batch
        if (orderPositions.isEmpty()) {
            if (score > 0 && score % 5 == 0) {
                activeOrderCount++;   // +1 order per batch
                baseTime += 5;        // +5 seconds per batch
            }
            timeLeft = baseTime;      // reset timer for new batch
            frameCounter = 0;
            generateOrders();
        }
    }

    // ── generateOrders ────────────────────────────────────────────────────────
    // Fills orderPositions and orderList with activeOrderCount unique orders.
    // No two orders share the same tile, and none land on the player.
    private void generateOrders() {
        orderPositions.clear();
        orderList.clear();

        ArrayList<Integer> usedIndices = new ArrayList<>();

        for (int n = 0; n < activeOrderCount; n++) {
            int pick;
            int[] pos;

            // Keep picking until we get a unique tile not on the player
            do {
                pick = rand.nextInt(rooms.length);
                pos  = rooms[pick];
            } while (
                usedIndices.contains(pick) ||
                (pos[0] == playerRow && pos[1] == playerCol)
            );

            usedIndices.add(pick);
            orderPositions.add(new int[]{pos[0], pos[1]});

            int floor;
            if      (pos[0] <= 3) floor = 3;
            else if (pos[0] <= 8) floor = 2;
            else                  floor = 1;

            orderList.add(new FoodOrder(roomNames[pick], floor, baseTime));
        }
    }

    // ── advanceAnimation ──────────────────────────────────────────────────────
    private void advanceAnimation(int totalFrames) {
        animTick++;
        if (animTick >= ANIM_SPEED) {
            animTick  = 0;
            animFrame = (animFrame + 1) % totalFrames;
        }
    }

    // ── restartGame ───────────────────────────────────────────────────────────
    private void restartGame() {
        playerRow        = 13;
        playerCol        = 5;
        playerDirection  = DIR_DOWN;
        isMoving         = false;
        animFrame        = 0;
        animTick         = 0;
        score            = 0;
        baseTime         = 10;      // reset timer to default
        timeLeft         = baseTime;
        frameCounter     = 0;
        activeOrderCount = 1;       // reset order count to default
        isRiding         = false;
        elevatorFrameCounter = 0;
        gameState        = "PLAYING";
        generateOrders();
    }

    // ── Sprite loading ────────────────────────────────────────────────────────
    private void loadSprites() {
        try {
            spriteSheet = makeTransparent(ImageIO.read(new File("assets/011.png")));
        } catch (Exception ex) {
            System.out.println("Sprite not found — using rectangle fallback. " + ex.getMessage());
        }
    }

    // Makes near-black pixels transparent (threshold < 20 per channel)
    // 011.png background is (2,4,3) — not pure black, so exact match won't work
    private BufferedImage makeTransparent(BufferedImage src) {
        BufferedImage dst = new BufferedImage(
            src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int px = src.getRGB(x, y);
                int r  = (px >> 16) & 0xFF;
                int g  = (px >>  8) & 0xFF;
                int b  =  px        & 0xFF;
                boolean isBackground = r < 20 && g < 20 && b < 20;
                dst.setRGB(x, y, isBackground ? 0x00000000 : px);
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

        // MAP TILES
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if      (map[row][col] == 0) g.setColor(Color.BLACK);
                else if (map[row][col] == 1) g.setColor(Color.DARK_GRAY);
                else                         g.setColor(Color.YELLOW);
                g.fillRect(col * tileSize, row * tileSize, tileSize, tileSize);
            }
        }

        // ROOM TILES — highlight active order targets in red
        g.setFont(new Font("Arial", Font.BOLD, 12));
        for (int i = 0; i < rooms.length; i++) {
            int rRow = rooms[i][0];
            int rCol = rooms[i][1];

            // Check if this room is an active order target
            boolean isTarget = false;
            for (int[] pos : orderPositions) {
                if (pos[0] == rRow && pos[1] == rCol) { isTarget = true; break; }
            }

            g.setColor(isTarget ? Color.RED : Color.GREEN);
            g.fillRect(rCol * tileSize, rRow * tileSize, tileSize, tileSize);

            g.setColor(Color.BLACK);
            String label = roomNames[i];
            FontMetrics fm = g.getFontMetrics();
            int tx = (rCol * tileSize) + (tileSize - fm.stringWidth(label)) / 2;
            int ty = (rRow * tileSize) + ((tileSize - fm.getHeight()) / 2) + fm.getAscent();
            g.drawString(label, tx, ty);
        }

        // PLAYER
        drawPlayer(g);

        // HUD DIVIDER
        g.setColor(Color.WHITE);
        g.drawLine(600, 0, 600, HEIGHT);

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Hotel Food Service",             610, 25);
        g.drawString("Score: "     + score,            620, 55);
        g.drawString("Time Left: " + timeLeft + "s",   620, 75);
        g.drawString("Orders left: " + orderPositions.size()
                     + "/" + activeOrderCount,         620, 95);

        // LIST ALL ACTIVE ORDERS
        g.setColor(Color.RED);
        g.drawString("Deliver to:",                    620, 125);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        for (int i = 0; i < orderList.size(); i++) {
            FoodOrder o = orderList.get(i);
            g.setColor(Color.WHITE);
            g.drawString((i + 1) + ". Room " + o.targetRoom
                         + "  (Floor " + o.targetFloor + ")", 620, 145 + i * 18);
        }

        // ELEVATOR STATUS
        if (isRiding) {
            g.setColor(Color.CYAN);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString("[ Elevator... ]", 615, HEIGHT - 20);
        }

        // GAME OVER
        if (gameState.equals("GAME_OVER")) drawGameOver(g);
    }

    // ── drawPlayer ────────────────────────────────────────────────────────────
    private void drawPlayer(Graphics g) {
        int px = playerCol * tileSize;
        int py = playerRow * tileSize;

        // Fallback rectangle if sprite failed to load
        if (spriteSheet == null) {
            g.setColor(isRiding ? Color.CYAN : Color.BLUE);
            g.fillRect(px, py, tileSize, tileSize);
            return;
        }

        // 011.png has its own RIGHT row — no flipping needed
        int srcX = animFrame        * FRAME_W;   // column: 0 or 1
        int srcY = playerDirection  * FRAME_H;   // row: DIR_DOWN/LEFT/RIGHT/UP

        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(spriteSheet,
            px,            py,               // dest top-left
            px + tileSize, py + tileSize,    // dest bottom-right
            srcX,          srcY,             // source top-left
            srcX + FRAME_W, srcY + FRAME_H, // source bottom-right
            null);
    }

    // ── drawGameOver ──────────────────────────────────────────────────────────
    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        String title = "GAME OVER";
        FontMetrics fm1 = g.getFontMetrics();
        g.drawString(title, (WIDTH - fm1.stringWidth(title)) / 2, 200);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 26));
        String scoreText = "Final Score: " + score;
        FontMetrics fm2 = g.getFontMetrics();
        g.drawString(scoreText, (WIDTH - fm2.stringWidth(scoreText)) / 2, 260);

        int bw = 220, bh = 60, bx = (WIDTH - 220) / 2, by = 320;
        g.setColor(Color.GREEN);
        g.fillRect(bx, by, bw, bh);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        String btn = "PLAY AGAIN";
        FontMetrics fm3 = g.getFontMetrics();
        g.drawString(btn, bx + (bw - fm3.stringWidth(btn)) / 2, by + 38);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        String hint = "Press ENTER to restart";
        FontMetrics fm4 = g.getFontMetrics();
        g.drawString(hint, (WIDTH - fm4.stringWidth(hint)) / 2, 420);
    }
}