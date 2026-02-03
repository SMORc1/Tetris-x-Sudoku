// File: SudokuBlocksGame.java
import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;

public class SudokuBlocksGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}

class GameFrame extends JFrame {
    private GameBoard gameBoard;
    private GamePanel gamePanel;
    private Timer gameTimer;
    private JLabel scoreLabel;
    private JLabel levelLabel;

    public GameFrame() {
        setTitle("Sudoku Blocks");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        gameBoard = new GameBoard();

        setupUI();
        setupGameTimer();
        setupKeyControls();
    }

    private void setupUI() {
        gamePanel = new GamePanel(gameBoard);
        add(gamePanel, BorderLayout.CENTER);

        JPanel sidePanel = createSidePanel();
        add(sidePanel, BorderLayout.EAST);
    }

    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(240, 240, 240));
        sidePanel.setPreferredSize(new Dimension(200, 700));

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        levelLabel = new JLabel("Level: 1");
        levelLabel.setFont(new Font("Arial", Font.BOLD, 16));
        levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea instructions = new JTextArea();
        instructions.setText("HOW TO PLAY:\n\n" +
                "← → : Move Block\n" +
                "↑ : Rotate Block\n" +
                "↓ : Soft Drop\n" +
                "SPACE: Hard Drop\n" +
                "P : Pause Game\n\n" +
                "RULES:\n" +
                "Fill rows, columns,\n" +
                "and 3x3 squares with\n" +
                "numbers 1-9 exactly\n" +
                "once to clear them!");
        instructions.setEditable(false);
        instructions.setFont(new Font("Arial", Font.PLAIN, 14));
        instructions.setBackground(new Color(240, 240, 240));

        JButton newGameBtn = new JButton("New Game");
        newGameBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        newGameBtn.addActionListener(e -> startNewGame());

        JButton pauseBtn = new JButton("Pause");
        pauseBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        pauseBtn.addActionListener(e -> togglePause());

        sidePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        sidePanel.add(scoreLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidePanel.add(levelLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 30)));
        sidePanel.add(instructions);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 30)));
        sidePanel.add(newGameBtn);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidePanel.add(pauseBtn);

        return sidePanel;
    }

    private void setupGameTimer() {
        gameTimer = new Timer(500, e -> {
            if (!gameBoard.isGameOver()) {
                gameBoard.moveBlockDown();
                updateDisplay();
                gamePanel.repaint();

                if (gameBoard.isGameOver()) {
                    gameTimer.stop();
                    JOptionPane.showMessageDialog(this,
                            "Game Over!\nFinal Score: " + gameBoard.getScore(),
                            "Game Over",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        gameTimer.start();
    }

    private void setupKeyControls() {
        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();

        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameBoard.isGameOver()) return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        gameBoard.moveBlockLeft();
                        break;
                    case KeyEvent.VK_RIGHT:
                        gameBoard.moveBlockRight();
                        break;
                    case KeyEvent.VK_DOWN:
                        gameBoard.moveBlockDown();
                        break;
                    case KeyEvent.VK_UP:
                        gameBoard.rotateBlock();
                        break;
                    case KeyEvent.VK_SPACE:
                        hardDrop();
                        break;
                    case KeyEvent.VK_P:
                        togglePause();
                        break;
                    case KeyEvent.VK_R:
                        startNewGame();
                        break;
                }
                updateDisplay();
                gamePanel.repaint();
            }
        });
    }

    private void hardDrop() {
        while (gameBoard.moveBlockDown()) {
            // Continue dropping until collision
        }
    }

    private void togglePause() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
        } else {
            gameTimer.start();
        }
    }

    private void startNewGame() {
        gameBoard = new GameBoard();
        gamePanel.setGameBoard(gameBoard);
        updateDisplay();
        if (!gameTimer.isRunning()) {
            gameTimer.start();
        }
        gamePanel.requestFocusInWindow();
    }

    private void updateDisplay() {
        scoreLabel.setText("Score: " + gameBoard.getScore());
        levelLabel.setText("Level: " + gameBoard.getLevel());
    }
}

class GameBoard {
    private int[][] sudokuGrid;
    private TetrisBlock currentBlock;
    private int score;
    private int level;
    private boolean gameOver;
    private Random random;

    public GameBoard() {
        sudokuGrid = new int[9][9];
        random = new Random();
        score = 0;
        level = 1;
        gameOver = false;
        spawnNewBlock();
    }

    public void spawnNewBlock() {
        currentBlock = new TetrisBlock();
        currentBlock.spawn(4, 0);
    }

    public boolean moveBlockDown() {
        if (!checkCollision(0, 1)) {
            currentBlock.moveDown();
            return true;
        } else {
            lockBlock();
            checkCompletedAreas();
            spawnNewBlock();
            if (checkCollision(0, 0)) {
                gameOver = true;
            }
            return false;
        }
    }

    public void moveBlockLeft() {
        if (!checkCollision(-1, 0)) {
            currentBlock.moveLeft();
        }
    }

    public void moveBlockRight() {
        if (!checkCollision(1, 0)) {
            currentBlock.moveRight();
        }
    }

    public void rotateBlock() {
        currentBlock.rotate();
        if (checkCollision(0, 0)) {
            currentBlock.rotateBack();
        }
    }

    private boolean checkCollision(int dx, int dy) {
        int[][] shape = currentBlock.getShape();
        int x = currentBlock.getX();
        int y = currentBlock.getY();

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0) {
                    int newX = x + col + dx;
                    int newY = y + row + dy;

                    if (newX < 0 || newX >= 9 || newY >= 9) {
                        return true;
                    }

                    if (newY >= 0 && sudokuGrid[newY][newX] != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void lockBlock() {
        int[][] shape = currentBlock.getShape();
        int x = currentBlock.getX();
        int y = currentBlock.getY();
        int value = currentBlock.getValue();

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0) {
                    int gridX = x + col;
                    int gridY = y + row;

                    if (gridY >= 0 && gridY < 9 && gridX >= 0 && gridX < 9) {
                        sudokuGrid[gridY][gridX] = value;
                    }
                }
            }
        }
    }

    private void checkCompletedAreas() {
        int pointsEarned = 0;

        for (int row = 0; row < 9; row++) {
            if (isRowComplete(row)) {
                clearRow(row);
                pointsEarned += 100;
            }
        }

        for (int col = 0; col < 9; col++) {
            if (isColumnComplete(col)) {
                clearColumn(col);
                pointsEarned += 100;
            }
        }

        for (int subGrid = 0; subGrid < 9; subGrid++) {
            if (isSubGridComplete(subGrid)) {
                clearSubGrid(subGrid);
                pointsEarned += 300;
            }
        }

        score += pointsEarned * level;
        level = (score / 1000) + 1;
    }

    private boolean isRowComplete(int row) {
        boolean[] found = new boolean[10];
        for (int col = 0; col < 9; col++) {
            int val = sudokuGrid[row][col];
            if (val == 0 || found[val]) {
                return false;
            }
            found[val] = true;
        }
        return true;
    }

    private boolean isColumnComplete(int col) {
        boolean[] found = new boolean[10];
        for (int row = 0; row < 9; row++) {
            int val = sudokuGrid[row][col];
            if (val == 0 || found[val]) {
                return false;
            }
            found[val] = true;
        }
        return true;
    }

    private boolean isSubGridComplete(int subGrid) {
        int startRow = (subGrid / 3) * 3;
        int startCol = (subGrid % 3) * 3;
        boolean[] found = new boolean[10];

        for (int row = startRow; row < startRow + 3; row++) {
            for (int col = startCol; col < startCol + 3; col++) {
                int val = sudokuGrid[row][col];
                if (val == 0 || found[val]) {
                    return false;
                }
                found[val] = true;
            }
        }
        return true;
    }

    private void clearRow(int row) {
        for (int col = 0; col < 9; col++) {
            sudokuGrid[row][col] = 0;
        }
    }

    private void clearColumn(int col) {
        for (int row = 0; row < 9; row++) {
            sudokuGrid[row][col] = 0;
        }
    }

    private void clearSubGrid(int subGrid) {
        int startRow = (subGrid / 3) * 3;
        int startCol = (subGrid % 3) * 3;

        for (int row = startRow; row < startRow + 3; row++) {
            for (int col = startCol; col < startCol + 3; col++) {
                sudokuGrid[row][col] = 0;
            }
        }
    }

    public int[][] getGrid() { return sudokuGrid; }
    public TetrisBlock getCurrentBlock() { return currentBlock; }
    public int getScore() { return score; }
    public int getLevel() { return level; }
    public boolean isGameOver() { return gameOver; }
}

class TetrisBlock {
    private int[][] shape;
    private Color color;
    private int x, y;
    private int value;
    private Random random;

    private int[][][] shapes = {
            {{1, 1, 1, 1}},
            {{2, 2}, {2, 2}},
            {{0, 3, 0}, {3, 3, 3}},
            {{4, 4, 0}, {0, 4, 4}},
            {{0, 5, 5}, {5, 5, 0}},
            {{6, 0, 0}, {6, 6, 6}},
            {{0, 0, 7}, {7, 7, 7}}
    };

    private Color[] colors = {
            Color.CYAN, Color.YELLOW, Color.MAGENTA,
            Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE
    };

    public TetrisBlock() {
        random = new Random();
        int index = random.nextInt(shapes.length);
        shape = shapes[index];
        color = colors[index];
        value = index + 1;
        x = 0;
        y = 0;
    }

    public void spawn(int gridWidth, int gridHeight) {
        x = gridWidth / 2 - shape[0].length / 2;
        y = 0;
    }

    public void rotate() {
        int[][] rotated = new int[shape[0].length][shape.length];

        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                rotated[j][shape.length - 1 - i] = shape[i][j];
            }
        }
        shape = rotated;
    }

    public void rotateBack() {
        rotate();
        rotate();
        rotate();
    }

    public void moveDown() { y++; }
    public void moveLeft() { x--; }
    public void moveRight() { x++; }

    public int[][] getShape() { return shape; }
    public Color getColor() { return color; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getValue() { return value; }
}

class GamePanel extends JPanel {
    private GameBoard gameBoard;

    public GamePanel(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
        setBackground(Color.BLACK);
    }

    public void setGameBoard(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameBoard == null) return;

        int cellSize = 40;
        int offsetX = 50;
        int offsetY = 50;

        drawGrid(g, cellSize, offsetX, offsetY);
        drawBlocks(g, cellSize, offsetX, offsetY);
        drawCurrentBlock(g, cellSize, offsetX, offsetY);

        if (gameBoard.isGameOver()) {
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String text = "GAME OVER";
            int textWidth = g.getFontMetrics().stringWidth(text);
            g.drawString(text, getWidth()/2 - textWidth/2, getHeight()/2);
        }
    }

    private void drawGrid(Graphics g, int cellSize, int offsetX, int offsetY) {
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= 9; i++) {
            int lineWidth = (i % 3 == 0) ? 3 : 1;
            ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));

            g.drawLine(offsetX + i * cellSize, offsetY,
                    offsetX + i * cellSize, offsetY + 9 * cellSize);
            g.drawLine(offsetX, offsetY + i * cellSize,
                    offsetX + 9 * cellSize, offsetY + i * cellSize);
        }

        int[][] grid = gameBoard.getGrid();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (grid[row][col] != 0) {
                    Color[] colors = {
                            null, Color.CYAN, Color.YELLOW, Color.MAGENTA,
                            Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE,
                            Color.PINK, Color.GRAY
                    };
                    Color cellColor = colors[grid[row][col]];
                    if (cellColor != null) {
                        g.setColor(cellColor);
                        g.fillRect(offsetX + col * cellSize + 1,
                                offsetY + row * cellSize + 1,
                                cellSize - 2, cellSize - 2);
                    }

                    g.setColor(Color.BLACK);
                    g.setFont(new Font("Arial", Font.BOLD, 20));
                    String num = String.valueOf(grid[row][col]);
                    FontMetrics fm = g.getFontMetrics();
                    int numWidth = fm.stringWidth(num);
                    int numHeight = fm.getAscent();
                    g.drawString(num,
                            offsetX + col * cellSize + (cellSize - numWidth) / 2,
                            offsetY + row * cellSize + (cellSize + numHeight) / 2 - 2);
                }
            }
        }
        ((Graphics2D)g).setStroke(new BasicStroke(1));
    }

    private void drawCurrentBlock(Graphics g, int cellSize, int offsetX, int offsetY) {
        TetrisBlock block = gameBoard.getCurrentBlock();
        if (block == null) return;

        int[][] shape = block.getShape();
        Color color = block.getColor();
        int x = block.getX();
        int y = block.getY();

        g.setColor(color);

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0) {
                    int drawX = offsetX + (x + col) * cellSize;
                    int drawY = offsetY + (y + row) * cellSize;

                    g.fillRect(drawX + 1, drawY + 1, cellSize - 2, cellSize - 2);

                    g.setColor(color.darker());
                    g.drawRect(drawX + 1, drawY + 1, cellSize - 2, cellSize - 2);
                    g.setColor(color);

                    g.setColor(Color.BLACK);
                    g.setFont(new Font("Arial", Font.BOLD, 20));
                    String num = String.valueOf(block.getValue());
                    FontMetrics fm = g.getFontMetrics();
                    int numWidth = fm.stringWidth(num);
                    int numHeight = fm.getAscent();
                    g.drawString(num,
                            drawX + (cellSize - numWidth) / 2,
                            drawY + (cellSize + numHeight) / 2 - 2);
                }
            }
        }
    }

    private void drawBlocks(Graphics g, int cellSize, int offsetX, int offsetY) {
        // Empty for now
    }
}