import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Waiter {

    // ── Position ──────────────────────────────────────────────────────────────
    int row;
    int col;

    // ── Direction constants ───────────────────────────────────────────────────
    static final int DIR_DOWN  = 0;
    static final int DIR_LEFT  = 1;
    static final int DIR_RIGHT = 2;
    static final int DIR_UP    = 3;

    int direction = DIR_DOWN;

    // ── Movement ──────────────────────────────────────────────────────────────
    boolean isMoving = false;

    // ── Transport ─────────────────────────────────────────────────────────────
    static final int ESCALATOR_SPEED = 40;  // frames per row (UP only)
    static final int ELEVATOR_SPEED  = 20;  // frames per row (both directions)

    boolean isRiding              = false;
    int     elevatorDirection     = 0;
    int     elevatorTarget        = 0;
    int     elevatorFrameCounter  = 0;
    int     currentTransportSpeed = ELEVATOR_SPEED;

    // ── Sprite animation ──────────────────────────────────────────────────────
    // waiter.png: 2 cols × 4 rows, each frame 443×420px
    // Row 0=DOWN | Row 1=LEFT | Row 2=RIGHT | Row 3=UP
    static final int FRAME_W     = 443;
    static final int FRAME_H     = 420;
    static final int WALK_FRAMES = 2;
    static final int IDLE_FRAMES = 1;
    static final int ANIM_SPEED  = 8;

    int animFrame = 0;
    int animTick  = 0;

    BufferedImage spriteSheet;

    // ── Order state (legacy) ──────────────────────────────────────────────────
    boolean   isCarrying   = false;
    FoodOrder currentOrder = null;

    // ─────────────────────────────────────────────────────────────────────────

    Waiter(int startRow, int startCol) {
        this.row = startRow;
        this.col = startCol;
        loadSprite();
    }

    // ── loadSprite ────────────────────────────────────────────────────────────
    private void loadSprite() {
        try {
            spriteSheet = makeTransparent(ImageIO.read(new File("assets/waiter.png")));
        } catch (Exception ex) {
            System.out.println("Player sprite not found — rectangle fallback active. " + ex.getMessage());
        }
    }

    // ── makeTransparent ───────────────────────────────────────────────────────
    // Near-black threshold (< 20 per channel) — handles waiter.png background
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

    // ── update ────────────────────────────────────────────────────────────────
    // Handles transport movement and animation each game tick.
    void update() {
        if (isRiding) {
            elevatorFrameCounter++;
            if (elevatorFrameCounter >= currentTransportSpeed) {
                row += elevatorDirection;
                elevatorFrameCounter = 0;
                if (row == elevatorTarget) {
                    isRiding = false;
                    isMoving = false;
                }
            }
            advanceAnimation(WALK_FRAMES);
            return;
        }
        advanceAnimation(isMoving ? WALK_FRAMES : IDLE_FRAMES);
    }

    // ── draw ──────────────────────────────────────────────────────────────────
    void draw(Graphics g, int tileSize) {
        int px = col * tileSize;
        int py = row * tileSize;

        if (spriteSheet == null) {
            g.setColor(isRiding ? Color.CYAN : Color.BLUE);
            g.fillRect(px, py, tileSize, tileSize);
            return;
        }

        int srcX = animFrame * FRAME_W;
        int srcY = direction  * FRAME_H;

        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(spriteSheet,
            px,             py,
            px + tileSize,  py + tileSize,
            srcX,           srcY,
            srcX + FRAME_W, srcY + FRAME_H,
            null);
    }

    // ── reset ─────────────────────────────────────────────────────────────────
    // Resets the waiter to start position — called by GamePanel.startGame().
    void reset(int startRow, int startCol) {
        this.row                  = startRow;
        this.col                  = startCol;
        this.direction            = DIR_DOWN;
        this.isMoving             = false;
        this.animFrame            = 0;
        this.animTick             = 0;
        this.isRiding             = false;
        this.elevatorFrameCounter = 0;
        this.isCarrying           = false;
        this.currentOrder         = null;
    }

    // ── advanceAnimation ──────────────────────────────────────────────────────
    private void advanceAnimation(int totalFrames) {
        animTick++;
        if (animTick >= ANIM_SPEED) {
            animTick  = 0;
            animFrame = (animFrame + 1) % totalFrames;
        }
    }

    // ── Legacy order methods ──────────────────────────────────────────────────
    void pickUp(FoodOrder order) {
        this.currentOrder = order;
        this.isCarrying   = true;
        System.out.println("Order for room " + this.currentOrder.targetRoom + " picked up");
    }

    void deliverOrder() {
        if (this.isCarrying) {
            this.currentOrder.deliver();
            this.isCarrying   = false;
            this.currentOrder = null;
        } else {
            System.out.println("Nothing to deliver!");
        }
    }

    void printStatus() {
        System.out.println("Row: " + this.row + " | Col: " + this.col);
        System.out.println("Carrying order: " + this.isCarrying);
    }
}
