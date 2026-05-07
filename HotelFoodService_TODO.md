# Hotel Food Service — Developer Handoff

## Project Overview
A 2D Java waiter simulator built with `javax.swing` and `java.awt` (no external libraries).
The player controls a waiter navigating a multi-floor hotel, picking up food orders, and
delivering them to the correct room before a countdown timer expires.

**IDE:** VS Code  
**Language:** Java (standard libraries only)  
**Window size:** 800 x 600px  
**Tile size:** 40px — map is 15 columns x 15 rows

---

## Current File Structure
```
HotelFoodService/
├── Main.java         ← creates JFrame, attaches GamePanel
├── GamePanel.java    ← game loop, map rendering, input handling
├── Waiter.java       ← player entity (currentFloor, isCarrying, currentOrder)
├── FoodOrder.java    ← order entity (targetRoom, targetFloor, timeLimit, isDelivered)
└── Room.java         ← room entity (number, floor, isOccupied)
```

---

## What Is Already Done ✅

### Phase 1 — Java Foundations
Variables, conditionals, loops, methods, ArrayLists. All implemented and tested.

### Phase 2 — OOP Classes
- `Room.java` — fields: `number` (String), `floor` (int), `isOccupied` (boolean)
- `FoodOrder.java` — fields: `targetRoom` (String), `targetFloor` (int), `timeLimit` (int),
  `isDelivered` (boolean). Methods: `deliver()`, `printStatus()`
- `Waiter.java` — fields: `currentFloor` (int), `isCarrying` (boolean),
  `currentOrder` (FoodOrder). Methods: `pickUp(FoodOrder)`, `deliverOrder()`,
  `moveFloor(int)`, `printStatus()`

### Phase 3 — Game Window
- `JFrame` window 800x600 with `pack()`
- `GamePanel extends JPanel` with game loop running at ~60fps (`Thread.sleep(16)`)
- Black background rendered via `paintComponent`

### Phase 4 (Partial) — Core Mechanics
- **Tile map** — 3-floor hotel rendered as `int[][]` grid
  - `0` = walkable (black), `1` = wall (dark gray), `2` = elevator door (yellow)
  - Elevator rows: **2, 7, 12** | Elevator cols: **0 and 14**
- **Player movement** — arrow key input via `KeyAdapter`, wall collision detection
- **Elevator logic** — standing on a `2` tile, UP/DOWN teleports ±5 rows between floors,
  LEFT/RIGHT always move freely regardless of tile

---

## Remaining To-Do List 🔧

### 4.5 — Food Order on Screen
- Add `FoodOrder currentOrder` field to `GamePanel`
  - Example: `new FoodOrder("201", 2, 60)`
- Add `int orderRow` and `int orderCol` fields for the target tile position
  - Floor 2 left door = row `7`, col `0`
- In `paintComponent`, draw a **red rectangle** at `orderCol * tileSize, orderRow * tileSize`
- Draw `g.drawString("Room: " + currentOrder.targetRoom, ...)` above the red marker
- Red marker must be drawn **after** the map but **before or after** the player (designer's choice)

### 4.6 — Delivery Detection
- In `update()`, check if `playerRow == orderRow && playerCol == orderCol`
- If true, call `currentOrder.deliver()` and print a confirmation
- After delivery, generate a **new random order** targeting a different floor/door
  - Elevator doors available: `{row:2, col:0}`, `{row:2, col:14}`, `{row:7, col:0}`,
    `{row:7, col:14}`, `{row:12, col:0}`, `{row:12, col:14}`
  - Use `java.util.Random` to pick one randomly, avoiding the player's current position

### 4.7 — Countdown Timer
- Add `int timeLeft` field to `GamePanel`, initialised from `currentOrder.timeLimit`
- In `update()`, decrement `timeLeft` every ~60 frames (one second at 60fps)
  - Use a frame counter: increment each `update()` call, subtract from `timeLeft` every 60 ticks
- Draw `"Time: " + timeLeft + "s"` in the HUD area (right side, x > 600)
- When `timeLeft <= 0`, trigger game over state (see Phase 5)

---

### Phase 5 — Polish & Complete Game

#### 5.1 — HUD (Heads-Up Display)
- Draw a vertical divider line at x=600 separating map from HUD
- HUD panel (x: 600–800) should display:
  - Game title
  - Current order: room number and floor
  - Time remaining
  - Score / deliveries completed

#### 5.2 — Score System
- Add `int score` field to `GamePanel`
- Increment score on each successful delivery
- Display score in HUD

#### 5.3 — Game States
Implement an `enum` or `String gameState` with three states:
- `"PLAYING"` — normal gameplay
- `"GAME_OVER"` — timer hit zero; show "Game Over" screen with final score
- `"WIN"` — optional: reached a target score; show "You Win!" screen

In `paintComponent`, branch on `gameState` to render the correct screen.
In `update()`, only run game logic when state is `"PLAYING"`.

#### 5.4 — Multiple Orders (Optional but recommended)
- After each delivery, generate the next order immediately
- Consider an `ArrayList<FoodOrder>` queue for future expansion

#### 5.5 — Sound (Optional)
- Use `javax.sound.sampled` (no external libraries needed)
- Add a delivery success sound and a time-up sound
- `.wav` files only (Java's built-in sound API does not support `.mp3`)

#### 5.6 — Package as .jar
```bash
# From project root in terminal:
javac *.java
jar cfe HotelFoodService.jar Main *.class
java -jar HotelFoodService.jar
```
This produces a single shareable `.jar` file that runs on any machine with Java installed.

---

## Map Reference

```
Row  0: { 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1 }  ← floor 3 ceiling
Row  1: { 1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 }
Row  2: { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,2 }  ← ELEVATOR ROW (floor 3)
Row  3: { 1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 }
Row  4: { 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1 }  ← floor 3/2 boundary
Row  5: { 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1 }
Row  6: { 1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 }
Row  7: { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,2 }  ← ELEVATOR ROW (floor 2)
Row  8: { 1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 }
Row  9: { 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1 }  ← floor 2/1 boundary
Row 10: { 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1 }
Row 11: { 1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 }
Row 12: { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,2 }  ← ELEVATOR ROW (floor 1)
Row 13: { 1,0,0,0,0,0,0,0,0,0,0,0,0,0,1 }
Row 14: { 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1 }  ← floor 1 bottom wall
```

Tile values: `0` = floor (black) | `1` = wall (dark gray) | `2` = elevator door (yellow)

---

## Key Design Decisions Already Made
- Elevator doors double as delivery target tiles — orders always point to a `2` tile
- HUD lives in the 200px gap on the right (x: 600–800)
- Player starts at `playerRow=13`, `playerCol=5` (floor 1 walkable tile)
- Game loop uses `Thread.sleep(16)` (~60fps) inside a `Runnable` thread

---

*Handoff prepared for intermediate Java developer. All existing code uses standard*
*`javax.swing` / `java.awt` — no build tools, no external dependencies.*
