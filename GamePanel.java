import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;

public class GamePanel extends JPanel {
    final int WIDTH = 800;
    final int HEIGHT = 600;

    int playerCol = 5;
    int playerRow = 13; // player starting location

    int tileSize = 40;

    int[][] map =   {
                     { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },   // floor 3
                     { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                     { 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 },
                     { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                     { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
                     { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },   // floor 2
                     { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                     { 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 },
                     { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                     { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
                     { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },   // floor 1
                     { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                     { 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 },
                     { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                     { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
                    };


    GamePanel () {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        Thread gameLoop = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    update();
                    repaint();
                    try {
                        Thread.sleep(16);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        gameLoop.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();   // which key was pressed?
                int highestFloor = 2;
                int lowestFloor = 12;

                if (map[playerRow][playerCol] == 2) {
                    if (key == KeyEvent.VK_UP) {
                        if (playerRow != highestFloor) {
                            playerRow -= 5;
                        }
                    }
                }

                if (map[playerRow][playerCol] == 2) {
                    if (key == KeyEvent.VK_DOWN) {
                        if (playerRow != lowestFloor) {
                            playerRow += 5;
                        }
                    }
                }


                if (key == KeyEvent.VK_UP) {
                    if (map[playerRow - 1][playerCol] != 1) {
                        playerRow--;
                    }
                }
                

                if (key == KeyEvent.VK_DOWN) {
                    if (map[playerRow + 1][playerCol] != 1) {
                        playerRow++;
                    }
                }

                if (key == KeyEvent.VK_LEFT) {
                    if (map[playerRow][playerCol - 1] != 1) {
                        playerCol--;
                    }
                }

                if (key == KeyEvent.VK_RIGHT) {
                    if (map[playerRow][playerCol + 1] != 1) {
                        playerCol++;
                    }
                }            
            }
        });
        setFocusable(true);
    }

    public void update() {
        // empty for now
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (int row = 0; row < map.length; row++) {
            for (int column = 0; column < map[row].length; column++) {
                if (map[row][column] == 0) {
                    g.setColor(Color.BLACK);
                } else if (map[row][column] == 1) {                              //level maker
                    g.setColor(Color.DARK_GRAY);
                } else {
                    g.setColor(Color.YELLOW);
                }
                g.fillRect(column * tileSize, row * tileSize, tileSize, tileSize);
            }
        }

        g.setColor(Color.WHITE);
        g.drawString("Hotel Food Service", 620, 30);    //game title

        g.setColor(Color.BLUE);
        g.fillRect(playerCol * tileSize, playerRow * tileSize, tileSize, tileSize);     //movable player
    }

}
