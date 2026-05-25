# Hotel Food Service — Refactor Mission Briefing

> You are the lead engineer of Hotel Food Service.
> The building's systems are tangled together and need to be separated
> into clean, independent modules before the hotel can fully operate.
> Complete each mission to restore order to the codebase.

---

## 🧑‍🍳 MISSION 1 — Liberate the Waiter

**Situation:** The Waiter's logic is still partially trapped inside `GamePanel.java`.
His movement, animation, and sprite are split between two files.
Your job is to complete his liberation so `GamePanel` only talks to him — never controls him directly.

---

### TASK 1.1 — Remove duplicate `update()` logic from `GamePanel`

**Location:** `GamePanel.java` → `update()` method, lines 214–230

**Problem:** The transport and animation loop still runs in `GamePanel`, not `Waiter`:

```java
// GamePanel.java — this block should not exist here
if (isRiding) {
    elevatorFrameCounter++;
    if (elevatorFrameCounter >= currentTransportSpeed) {
        playerRow += elevatorDirection;
        ...
    }
    advanceAnimation(WALK_FRAMES);
    return;
}
advanceAnimation(isMoving ? WALK_FRAMES : IDLE_FRAMES);
```

**Fix:** Replace the entire block above with a single call to `Waiter`:
```java
waiter.update();
```

`Waiter.update()` already handles both transport and animation internally.

---

### TASK 1.2 — Remove leftover player fields from `startGame()`

**Location:** `GamePanel.java` → `startGame()`, around lines 400–410

**Problem:** `startGame()` still resets raw player fields that no longer exist:
```java
playerRow       = 12;
playerCol       = 9;
playerDirection = DIR_DOWN;
isMoving        = false;
animFrame       = 0;
animTick        = 0;
isRiding        = false;
elevatorFrameCounter = 0;
```

**Fix:** Replace all of the above with one call:
```java
waiter.reset(12, 9);
```

---

### TASK 1.3 — Update delivery check to use `waiter.row` and `waiter.col`

**Location:** `GamePanel.java` → `update()`, delivery check loop

**Problem:** Still using raw `playerRow` / `playerCol`:
```java
if (playerRow == pos[0] && playerCol == pos[1])
```

**Fix:**
```java
if (waiter.row == pos[0] && waiter.col == pos[1])
```

---

### TASK 1.4 — Update `generateOrders()` to use `waiter.row` and `waiter.col`

**Location:** `GamePanel.java` → `generateOrders()`

**Problem:**
```java
(pos[0] == playerRow && pos[1] == playerCol)
```

**Fix:**
```java
(pos[0] == waiter.row && pos[1] == waiter.col)
```

---

### TASK 1.5 — Update `addNewOrder()` to use `waiter.row` and `waiter.col`

**Location:** `GamePanel.java` → `isPositionTaken()`

**Problem:**
```java
if (row == playerRow && col == playerCol) return true;
```

**Fix:**
```java
if (row == waiter.row && col == waiter.col) return true;
```

---

### TASK 1.6 — Remove `drawPlayer()` from `GamePanel` and call `waiter.draw()` instead

**Location:** `GamePanel.java` → `paintComponent()` and `drawPlayer()` method

**Problem:** `GamePanel` still has its own `drawPlayer()` method with sprite logic,
and `paintComponent` calls it instead of delegating to `Waiter`:
```java
drawPlayer(g);  // ← calls GamePanel's own method
```

**Fix 1:** Delete the entire `drawPlayer(Graphics g)` method from `GamePanel`.

**Fix 2:** Replace the call in `paintComponent` with:
```java
waiter.draw(g, tileSize);
```

---

### TASK 1.7 — Remove `spriteSheet` loading from `GamePanel.loadSprites()`

**Location:** `GamePanel.java` → `loadSprites()`

**Problem:** `loadSprites()` still loads `waiter.png` into a `spriteSheet` field
that no longer exists in `GamePanel`. `Waiter` already loads its own sprite internally.

**Fix:** Delete this block from `loadSprites()`:
```java
try {
    spriteSheet = makeTransparent(ImageIO.read(new File("assets/waiter.png")));
} catch (Exception ex) { ... }
```

---

### TASK 1.8 — Remove `advanceAnimation()` and `makeTransparent()` from `GamePanel`

**Location:** `GamePanel.java`

**Problem:** Both methods exist in `Waiter.java` now. `GamePanel` has its own
copies that are no longer called by anything meaningful.

**Fix:** Delete both methods from `GamePanel.java`:
- `private void advanceAnimation(int totalFrames) { ... }`
- `private BufferedImage makeTransparent(BufferedImage src) { ... }`

---

### TASK 1.9 — Update riding status label in HUD to use `waiter`

**Location:** `GamePanel.java` → `paintComponent()`, HUD section

**Problem:**
```java
if (isRiding) {
    boolean onEsc = (currentTransportSpeed == ESCALATOR_SPEED);
    ...
}
```

**Fix:**
```java
if (waiter.isRiding) {
    boolean onEsc = (waiter.currentTransportSpeed == Waiter.ESCALATOR_SPEED);
    ...
}
```

---

### ✅ Mission 1 Complete Condition
`GamePanel.java` contains zero references to:
`playerRow`, `playerCol`, `isRiding`, `isMoving`, `animFrame`, `animTick`,
`elevatorDirection`, `elevatorTarget`, `elevatorFrameCounter`, `currentTransportSpeed`,
`spriteSheet`, `playerDirection`, `drawPlayer`, `advanceAnimation`, `makeTransparent`

---

---

## 🚪 MISSION 2 — Check In the Rooms

**Situation:** Hotel rooms are represented as two parallel lists —
`roomPositions` (coordinates) and `roomLabels` (names).
They should be one object. `Room.java` already exists but is empty.
Your job is to give it purpose.

---

### TASK 2.1 — Expand `Room.java` with the right fields

**Location:** `Room.java`

**Problem:** Current `Room.java` only has `number`, `floor`, `isOccupied`
from Phase 2. It needs tile coordinates too.

**Fix:** Update `Room.java` fields to:
```java
public class Room {
    String number;   // e.g. "204"
    int floor;       // 2 or 3
    int row;         // tile row on the map
    int col;         // tile col on the map
    boolean isOccupied;

    Room(String number, int floor, int row, int col) {
        this.number = number;
        this.floor  = floor;
        this.row    = row;
        this.col    = col;
        this.isOccupied = false;
    }
}
```

---

### TASK 2.2 — Replace parallel lists with `ArrayList<Room>`

**Location:** `GamePanel.java` — field declarations

**Problem:**
```java
ArrayList<int[]>  roomPositions = new ArrayList<>();
ArrayList<String> roomLabels    = new ArrayList<>();
```

**Fix:** Replace both with:
```java
ArrayList<Room> rooms = new ArrayList<>();
```

---

### TASK 2.3 — Update `buildRoomData()` to build `Room` objects

**Location:** `GamePanel.java` → `buildRoomData()`

**Problem:** Currently adds to two separate lists:
```java
roomPositions.add(new int[]{row, col});
roomLabels.add(String.format("3%02d", floor3Count));
```

**Fix:** Build one `Room` object instead:
```java
rooms.add(new Room(String.format("3%02d", floor3Count), 3, row, col));
```

---

### TASK 2.4 — Update `generateOrders()` to read from `rooms`

**Location:** `GamePanel.java` → `generateOrders()`

**Problem:**
```java
pick = rand.nextInt(roomPositions.size());
pos  = roomPositions.get(pick);
orderList.add(new FoodOrder(roomLabels.get(pick), floor, 0));
```

**Fix:**
```java
pick = rand.nextInt(rooms.size());
Room r = rooms.get(pick);
orderList.add(new FoodOrder(r.number, r.floor, 0));
orderPositions.add(new int[]{r.row, r.col});
```

---

### TASK 2.5 — Update `addNewOrder()` the same way

**Location:** `GamePanel.java` → `addNewOrder()`

Same fix as Task 2.4 — use `rooms.get(pick)` and access `.row`, `.col`, `.number`, `.floor`.

---

### TASK 2.6 — Update `paintComponent()` room drawing loop

**Location:** `GamePanel.java` → `paintComponent()`, room drawing section

**Problem:**
```java
int rRow  = roomPositions.get(i)[0];
int rCol  = roomPositions.get(i)[1];
String label = roomLabels.get(i);
```

**Fix:**
```java
Room r = rooms.get(i);
int rRow  = r.row;
int rCol  = r.col;
String label = r.number;
```

---

### ✅ Mission 2 Complete Condition
`GamePanel.java` contains zero references to `roomPositions` or `roomLabels`.
All room data flows through `ArrayList<Room> rooms`.

---

---

## 🔊 MISSION 3 — Fix the Hotel's Sound System

**Situation:** Every sound in the hotel plays through copy-pasted try/catch blocks
scattered across `update()`. The sound system has no central control.
One bad file path and the whole game crashes silently.
Your job is to build a proper `SoundManager`.

---

### TASK 3.1 — Create `SoundManager.java`

**Location:** New file `SoundManager.java`

**Fix:** Create the class with three pre-loaded clips:

```java
import java.io.File;
import javax.sound.sampled.*;

public class SoundManager {

    private Clip deliverySuccess;
    private Clip orderExpiry;
    private Clip gameOver;

    SoundManager() {
        deliverySuccess = load("assets/delivery_success.wav");
        orderExpiry     = load("assets/order_expiry.wav");
        gameOver        = load("assets/game_over.wav");
    }

    private Clip load(String path) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(path));
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception ex) {
            System.out.println("Sound not found: " + path);
            return null;
        }
    }

    private void play(Clip clip) {
        if (clip == null) return;
        clip.setFramePosition(0);
        clip.start();
    }

    void playDeliverySuccess() { play(deliverySuccess); }
    void playOrderExpiry()     { play(orderExpiry);     }
    void playGameOver()        { play(gameOver);        }
}
```

---

### TASK 3.2 — Add `SoundManager` to `GamePanel`

**Location:** `GamePanel.java` — field declarations

**Fix:** Add one field:
```java
SoundManager sound = new SoundManager();
```

---

### TASK 3.3 — Remove sound try/catch from delivery check

**Location:** `GamePanel.java` → `update()`, delivery check

**Problem:**
```java
try {
    File audioFile = new File("assets/delivery_success.wav");
    AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
    Clip clip = AudioSystem.getClip();
    clip.open(audioStream);
    clip.start();
} catch (Exception e) { e.printStackTrace(); }
```

**Fix:** Replace the entire block with:
```java
sound.playDeliverySuccess();
```

---

### TASK 3.4 — Remove sound try/catch from order expiry

**Location:** `GamePanel.java` → `update()`, order expiry block

**Fix:** Replace with:
```java
sound.playOrderExpiry();
```

---

### TASK 3.5 — Remove sound try/catch from game over

**Location:** `GamePanel.java` → `update()`, dissatisfaction = 0 block

**Fix:** Replace with:
```java
sound.playGameOver();
```

---

### TASK 3.6 — Remove `javax.sound.sampled.*` import from `GamePanel`

**Location:** `GamePanel.java` — imports at the top

**Problem:**
```java
import javax.sound.sampled.*;
```

**Fix:** Delete this line from `GamePanel.java`.
It now belongs in `SoundManager.java` only.

---

### ✅ Mission 3 Complete Condition
`GamePanel.java` contains zero references to:
`AudioSystem`, `AudioInputStream`, `Clip`, `.wav`

All sound flows through `sound.playXxx()`.

---

---

## 🏆 Final Completion Condition

When all three missions are done, `GamePanel.java` should read like a story:

```java
void update() {
    waiter.update();
    checkOrders();
}

void paintComponent(Graphics g) {
    drawMap(g);
    waiter.draw(g, tileSize);
    hud.draw(g);
}

void keyPressed(KeyEvent e) {
    waiter.moveUp(map);
}
```

Clean. Readable. Each class owns exactly one thing.

> **Total tasks: 18**
> Waiter: 9 tasks | Room: 6 tasks | SoundManager: 6 tasks
