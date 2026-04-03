import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;

// ================================================================
//  T E T R I D O K U  v9
//  Clean gameplay  |  1-min level progression  |  tutorial window
//  Pure black board  |  watermark  |  rich synthesized audio
// ================================================================

public class TetriDoku extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TutorialDialog tutorial = new TutorialDialog(null);
            tutorial.setVisible(true);
            if (tutorial.shouldPlay()) {
                new TetriDoku().setVisible(true);
            }
        });
    }

    TetriDoku() {
        setTitle("TetriDoku");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setBackground(Color.BLACK);
        GamePanel gp = new GamePanel();
        add(gp);
        pack();
        setLocationRelativeTo(null);
        gp.requestFocusInWindow();
    }
}

// ================================================================
//  T U T O R I A L   D I A L O G
// ================================================================
class TutorialDialog extends JDialog {

    private boolean play = false;
    private int currentPage = 0;
    private static final int PAGES = 4;
    private TutorialPanel tutPanel;

    TutorialDialog(Frame owner) {
        super(owner, "How to Play TetriDoku", true);
        setBackground(new Color(8, 8, 12));
        setUndecorated(false);
        setResizable(false);

        tutPanel = new TutorialPanel();
        tutPanel.setPreferredSize(new Dimension(720, 480));
        add(tutPanel, BorderLayout.CENTER);

        // Bottom button bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        btnBar.setBackground(new Color(8, 8, 12));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 60)));

        JButton prevBtn = makeBtn("◀  Back");
        JButton nextBtn = makeBtn("Next  ▶");
        JButton playBtn = makeBtn("▶  PLAY NOW!");
        playBtn.setForeground(new Color(80, 255, 120));
        playBtn.setFont(new Font("Arial Black", Font.BOLD, 14));

        prevBtn.addActionListener(e -> { if (currentPage > 0) { currentPage--; tutPanel.repaint(); updateButtons(prevBtn, nextBtn, playBtn); } });
        nextBtn.addActionListener(e -> { if (currentPage < PAGES - 1) { currentPage++; tutPanel.repaint(); updateButtons(prevBtn, nextBtn, playBtn); } });
        playBtn.addActionListener(e -> { play = true; dispose(); });

        JButton skipBtn = makeBtn("Skip Tutorial");
        skipBtn.setForeground(new Color(80, 80, 110));
        skipBtn.addActionListener(e -> { play = true; dispose(); });

        btnBar.add(prevBtn); btnBar.add(nextBtn); btnBar.add(playBtn); btnBar.add(skipBtn);
        add(btnBar, BorderLayout.SOUTH);

        updateButtons(prevBtn, nextBtn, playBtn);
        pack();
        setLocationRelativeTo(null);
    }

    private JButton makeBtn(String label) {
        JButton b = new JButton(label);
        b.setBackground(new Color(22, 22, 32));
        b.setForeground(new Color(180, 185, 230));
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 50, 75), 1),
                BorderFactory.createEmptyBorder(6, 18, 6, 18)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void updateButtons(JButton prev, JButton next, JButton play) {
        prev.setEnabled(currentPage > 0);
        next.setVisible(currentPage < PAGES - 1);
        play.setVisible(currentPage == PAGES - 1);
    }

    boolean shouldPlay() { return play; }

    // ── Inner panel that draws each tutorial page ────────────────
    class TutorialPanel extends JPanel {
        TutorialPanel() { setBackground(new Color(8, 8, 12)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(8, 8, 12));
            g2.fillRect(0, 0, getWidth(), getHeight());
            switch (currentPage) {
                case 0: drawPage0(g2); break;
                case 1: drawPage1(g2); break;
                case 2: drawPage2(g2); break;
                case 3: drawPage3(g2); break;
            }
            drawPageDots(g2);
        }

        void drawPageDots(Graphics2D g) {
            int dotSize = 8, gap = 14, totalW = PAGES * dotSize + (PAGES - 1) * (gap - dotSize);
            int sx = (getWidth() - totalW) / 2, y = getHeight() - 18;
            for (int i = 0; i < PAGES; i++) {
                g.setColor(i == currentPage ? new Color(100, 200, 255) : new Color(40, 40, 60));
                g.fillOval(sx + i * gap, y, dotSize, dotSize);
            }
        }

        // Page 0: Welcome + concept
        void drawPage0(Graphics2D g) {
            // Title
            g.setFont(new Font("Arial Black", Font.BOLD, 38));
            drawShadowText(g, "TETRIDOKU", getWidth() / 2, 68, new Color(0, 215, 230), 3);

            g.setFont(new Font("Arial", Font.ITALIC, 16));
            drawCenteredText(g, "Tetris  ×  Sudoku  —  two classics, one game", getWidth() / 2, 100, new Color(140, 145, 190));

            // Divider
            g.setColor(new Color(35, 35, 55));
            g.fillRect(60, 112, getWidth() - 120, 1);

            // Two column concept boxes
            int bw = 265, bh = 175, by = 128;
            // Tetris box
            drawBox(g, 38, by, bw, bh, new Color(0, 215, 230), "TETRIS SIDE");
            String[] tetLines = {
                    "• Pieces fall from above",
                    "• 7 classic tetromino shapes",
                    "• Rotate (↑/Z) & move (←→)",
                    "• Hard drop with Space",
                    "• Speed increases every level"
            };
            drawBoxLines(g, 52, by + 42, tetLines, new Color(160, 220, 235));

            // Mini tetris pieces
            drawMiniPiece(g, 48, by + 148, new int[][]{{0,0},{0,1},{0,2},{0,3}}, new Color(0,215,230));

            // Sudoku box
            drawBox(g, 315 + 67, by, bw, bh, new Color(240, 200, 0), "SUDOKU SIDE");
            String[] sudLines = {
                    "• Each piece cell holds a digit (1–9)",
                    "• No repeats in any row or column",
                    "• Fill a 3×3 box for bonus points",
                    "• Use Q/E to reorder digits",
                    "• Conflicts glow red — avoid them!"
            };
            drawBoxLines(g, 395, by + 42, sudLines, new Color(240, 225, 160));

            // Mini sudoku grid
            int[] sudNums = {3,7,1,9,2,5,4,6,8};
            Color[] sudColors = {new Color(0,215,230), new Color(240,200,0), new Color(160,0,220)};
            for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
                int gx = 393 + j * 19, gy = by + 148 + i * 19;
                g.setColor(new Color(20,20,30));
                g.fillRect(gx, gy, 18, 18);
                g.setColor(new Color(40,40,55));
                g.drawRect(gx, gy, 18, 18);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.setColor(sudColors[j]);
                FontMetrics fm = g.getFontMetrics();
                String n = String.valueOf(sudNums[i*3+j]);
                g.drawString(n, gx+(18-fm.stringWidth(n))/2, gy+13);
            }

            // × symbol
            g.setFont(new Font("Arial Black", Font.BOLD, 28));
            drawShadowText(g, "×", getWidth()/2, by + 100, new Color(255, 255, 255), 2);

            // Bottom note
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            drawCenteredText(g, "Place pieces strategically — both Tetris skill AND Sudoku thinking matter!", getWidth()/2, 328, new Color(100, 105, 150));
        }

        // Page 1: Controls
        void drawPage1(Graphics2D g) {
            g.setFont(new Font("Arial Black", Font.BOLD, 24));
            drawShadowText(g, "CONTROLS", getWidth()/2, 44, new Color(100, 200, 255), 2);

            g.setColor(new Color(35, 35, 55));
            g.fillRect(60, 54, getWidth() - 120, 1);

            String[][] controls = {
                    {"←  →",        "Move piece left / right"},
                    {"↑  or  Z",    "Rotate clockwise / counter-clockwise"},
                    {"↓",           "Soft drop  (+1 point per row)"},
                    {"Space",       "Hard drop instantly  (+2 pts per row)"},
                    {"Q  /  E",     "Cycle digit order on active piece"},
                    {"–  /  =",     "Decrease / Increase game speed manually"},
                    {"P",           "Pause game  (also mutes music)"},
                    {"R",           "Restart game from scratch"},
            };

            Color[] catColors = {
                    new Color(0,215,230), new Color(0,215,230), new Color(0,215,230), new Color(0,215,230),
                    new Color(240,200,0),
                    new Color(80,160,80),
                    new Color(160,160,200), new Color(220,80,80)
            };

            int startY = 74;
            for (int i = 0; i < controls.length; i++) {
                int rowY = startY + i * 46;
                // Row bg
                g.setColor(i % 2 == 0 ? new Color(15,15,22) : new Color(11,11,17));
                g.fillRect(30, rowY, getWidth()-60, 40);
                // Color accent bar
                g.setColor(catColors[i]);
                g.fillRect(30, rowY, 4, 40);
                // Key badge
                g.setColor(new Color(20,20,30));
                g.fillRoundRect(42, rowY+6, 110, 28, 6, 6);
                g.setColor(catColors[i]);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(42, rowY+6, 110, 28, 6, 6);
                g.setStroke(new BasicStroke(1f));
                g.setFont(new Font("Courier New", Font.BOLD, 13));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(controls[i][0], 42+(110-fm.stringWidth(controls[i][0]))/2, rowY+25);
                // Description
                g.setFont(new Font("Arial", Font.PLAIN, 13));
                g.setColor(new Color(170,175,215));
                g.drawString(controls[i][1], 165, rowY+25);
            }

            // Q/E tip box
            int tipY = startY + controls.length * 46 + 4;
            g.setColor(new Color(15,22,15));
            g.fillRoundRect(30, tipY, getWidth()-60, 34, 8, 8);
            g.setColor(new Color(40,120,40));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(30, tipY, getWidth()-60, 34, 8, 8);
            g.setStroke(new BasicStroke(1f));
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.setColor(new Color(80,220,100));
            g.drawString("💡  Pro tip: Use Q/E before placing to put the right digit in the right cell — this is the key skill!", 44, tipY+21);
        }

        // Page 2: Scoring
        void drawPage2(Graphics2D g) {
            g.setFont(new Font("Arial Black", Font.BOLD, 24));
            drawShadowText(g, "SCORING", getWidth()/2, 44, new Color(255, 210, 55), 2);
            g.setColor(new Color(35, 35, 55));
            g.fillRect(60, 54, getWidth() - 120, 1);

            Object[][] scores = {
                    {"Soft drop",         "+1 per row",              new Color(160,165,200), "Move piece down one row manually"},
                    {"Hard drop",         "+2 × rows fallen",        new Color(0,215,230),   "Instantly drop — double the reward"},
                    {"Unique digit clear","40 × Level per cell",     new Color(80,210,80),   "Clear cells where each digit appears once only"},
                    {"3×3 Box complete",  "+1,000 × Level",          new Color(255,210,55),  "Fill any 3×3 zone with all 9 unique digits"},
                    {"Repeated digit",    "0 pts — stays on board",  new Color(220,70,70),   "Duplicate digits in a row/col stay & block you"},
            };

            int sy = 70;
            for (Object[] row : scores) {
                g.setColor(new Color(16,16,24));
                g.fillRoundRect(30, sy, getWidth()-60, 62, 8, 8);
                g.setColor(new Color(35,35,50));
                g.drawRoundRect(30, sy, getWidth()-60, 62, 8, 8);
                // Color dot
                g.setColor((Color)row[2]);
                g.fillOval(46, sy+22, 12, 12);
                // Label
                g.setFont(new Font("Arial Black", Font.BOLD, 13));
                g.setColor(Color.WHITE);
                g.drawString((String)row[0], 66, sy+20);
                // Sub text
                g.setFont(new Font("Arial", Font.PLAIN, 11));
                g.setColor(new Color(120,125,165));
                g.drawString((String)row[3], 66, sy+37);
                // Value badge
                Color rc = (Color)row[2];
                g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), (int)(0.15f * 255)));
                g.fillRoundRect(getWidth()-200, sy+12, 168, 30, 6, 6);
                g.setColor((Color)row[2]);
                g.setStroke(new BasicStroke(1.2f));
                g.drawRoundRect(getWidth()-200, sy+12, 168, 30, 6, 6);
                g.setStroke(new BasicStroke(1f));
                g.setFont(new Font("Arial Black", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                String val = (String)row[1];
                g.drawString(val, getWidth()-200+(168-fm.stringWidth(val))/2, sy+32);
                sy += 72;
            }
        }

        // Page 3: Level progression + quick-start
        void drawPage3(Graphics2D g) {
            g.setFont(new Font("Arial Black", Font.BOLD, 24));
            drawShadowText(g, "LEVELS & PROGRESSION", getWidth()/2, 44, new Color(80,200,80), 2);
            g.setColor(new Color(35, 35, 55));
            g.fillRect(60, 54, getWidth() - 120, 1);

            // Level diagram
            int lx = 38, ly = 68, lw = getWidth()-76, lh = 24;
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.setColor(new Color(100,105,150));
            g.drawString("SPEED INCREASES EVERY 60 SECONDS — LEVEL 1 TO 10", lx, ly-4);

            for (int lv = 1; lv <= 10; lv++) {
                float t = (lv-1)/9f;
                Color c = new Color(
                        Math.min(255,(int)(t*225+30)),
                        Math.min(255,(int)((1f-t)*200+55)), 22);
                int segW = (lw-18)/10;
                int sx = lx + (lv-1)*(segW+2);
                g.setColor(c); g.fillRect(sx, ly, segW, lh);
                g.setColor(Color.BLACK); g.fillRect(sx, ly, segW, 3); // top bevel
                g.setFont(new Font("Arial", Font.BOLD, 9));
                g.setColor(Color.BLACK);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(String.valueOf(lv), sx+(segW-fm.stringWidth(String.valueOf(lv)))/2, ly+16);
            }

            // Speed table
            int tableY = ly + lh + 16;
            g.setColor(new Color(35,35,55));
            g.fillRect(lx, tableY, lw, 1);
            tableY += 8;

            String[][] table = {
                    {"Level 1", "700ms/tick", "~1.4 drops/sec", "Easy warm-up"},
                    {"Level 2", "580ms/tick", "~1.7 drops/sec", "Steady pace"},
                    {"Level 3", "460ms/tick", "~2.2 drops/sec", "Think fast"},
                    {"Level 4", "340ms/tick", "~2.9 drops/sec", "Quite quick"},
                    {"Level 5", "260ms/tick", "~3.8 drops/sec", "No hesitation"},
                    {"Level 6", "200ms/tick", "~5 drops/sec",   "Expert zone"},
                    {"Level 7+","≤140ms/tick","7+ drops/sec",   "Survival mode!"},
            };
            Color[] rowColors = {new Color(0,215,230),new Color(60,200,60),new Color(60,200,60),
                    new Color(220,185,0),new Color(220,185,0),new Color(220,80,30),new Color(220,30,30)};

            for (int i = 0; i < table.length; i++) {
                int ry = tableY + i*30;
                g.setColor(i%2==0?new Color(14,14,20):new Color(10,10,15));
                g.fillRect(lx, ry, lw, 28);
                g.setColor(rowColors[i]); g.fillRect(lx, ry, 4, 28);
                g.setFont(new Font("Arial Black", Font.BOLD, 11));
                g.setColor(rowColors[i]); g.drawString(table[i][0], lx+12, ry+18);
                g.setFont(new Font("Courier New", Font.PLAIN, 11));
                g.setColor(new Color(160,165,210)); g.drawString(table[i][1], lx+98, ry+18);
                g.setFont(new Font("Arial", Font.PLAIN, 11));
                g.setColor(new Color(130,135,175)); g.drawString(table[i][2], lx+210, ry+18);
                g.setFont(new Font("Arial", Font.ITALIC, 11));
                g.setColor(new Color(90,95,135)); g.drawString(table[i][3], lx+360, ry+18);
            }

            int bottomY = tableY + table.length*30 + 14;
            // Ready banner
            g.setColor(new Color(12,22,12));
            g.fillRoundRect(lx, bottomY, lw, 42, 10, 10);
            g.setColor(new Color(40,160,50));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(lx, bottomY, lw, 42, 10, 10);
            g.setStroke(new BasicStroke(1f));
            g.setFont(new Font("Arial Black", Font.BOLD, 13));
            g.setColor(new Color(80,220,80));
            String ready = "You're ready! Press  ▶  PLAY NOW!  to start.";
            FontMetrics fm2 = g.getFontMetrics();
            g.drawString(ready, lx+(lw-fm2.stringWidth(ready))/2, bottomY+26);
        }

        // ── Helpers ─────────────────────────────────────────────
        void drawBox(Graphics2D g, int x, int y, int w, int h, Color accent, String title) {
            g.setColor(new Color(16,16,24)); g.fillRoundRect(x,y,w,h,10,10);
            g.setColor(accent); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(x,y,w,h,10,10);
            g.setStroke(new BasicStroke(1f));
            g.setColor(accent); g.fillRoundRect(x,y,w,22,10,10);
            g.setColor(new Color(8,8,12)); g.fillRect(x,y+11,w,11);
            g.setFont(new Font("Arial Black", Font.BOLD, 11));
            g.setColor(new Color(8,8,12));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, x+(w-fm.stringWidth(title))/2, y+15);
        }

        void drawBoxLines(Graphics2D g, int x, int y, String[] lines, Color clr) {
            g.setFont(new Font("Arial", Font.PLAIN, 12)); g.setColor(clr);
            for (int i = 0; i < lines.length; i++) g.drawString(lines[i], x, y+i*22);
        }

        void drawMiniPiece(Graphics2D g, int x, int y, int[][] cells, Color clr) {
            int sz = 14;
            for (int[] c : cells) {
                g.setColor(clr.darker()); g.fillRect(x+c[1]*sz, y+c[0]*sz, sz-1, sz-1);
                g.setColor(clr.brighter()); g.fillRect(x+c[1]*sz, y+c[0]*sz, sz-1,2);
                g.setColor(clr); g.fillRect(x+c[1]*sz+2, y+c[0]*sz+2, sz-5,sz-5);
            }
        }

        void drawShadowText(Graphics2D g, String text, int cx, int y, Color clr, int shadow) {
            FontMetrics fm = g.getFontMetrics();
            int tx = cx - fm.stringWidth(text)/2;
            g.setColor(new Color(0,0,0,100));
            g.drawString(text, tx+shadow, y+shadow);
            g.setColor(clr);
            g.drawString(text, tx, y);
        }

        void drawCenteredText(Graphics2D g, String text, int cx, int y, Color clr) {
            FontMetrics fm = g.getFontMetrics();
            g.setColor(clr);
            g.drawString(text, cx - fm.stringWidth(text)/2, y);
        }
    }
}

// ================================================================
//  G A M E   P A N E L
// ================================================================
class GamePanel extends JPanel implements ActionListener {

    static final int COLS = 9, ROWS = 18, PAD = 8;
    static final int CELL, SIDE_W, BOARD_W, BOARD_H, W, H;

    static {
        Dimension scr;
        try { scr = Toolkit.getDefaultToolkit().getScreenSize(); }
        catch (Exception e) { scr = new Dimension(1440, 900); }
        int usableH = scr.height - 80, usableW = scr.width - 40;
        int side = Math.max(220, Math.min(290, usableW / 5));
        CELL = Math.max(34, Math.min(62, Math.min((usableH-PAD*2)/ROWS, (usableW-side-PAD*3)/COLS)));
        SIDE_W = side; BOARD_W = COLS*CELL; BOARD_H = ROWS*CELL;
        W = PAD+BOARD_W+PAD+SIDE_W; H = PAD+BOARD_H+PAD;
    }

    static final Color[] PIECE_CLR = {
            new Color(  0,215,230), // I cyan
            new Color(240,200,  0), // O yellow
            new Color(160,  0,220), // T purple
            new Color( 20,200, 55), // S green
            new Color(220, 30, 30), // Z red
            new Color( 20, 80,220), // J blue
            new Color(220,115,  0), // L orange
    };

    static final int[][][][] SHAPES = {
            {{{0,0},{0,1},{0,2},{0,3}},{{0,0},{1,0},{2,0},{3,0}},{{0,0},{0,1},{0,2},{0,3}},{{0,0},{1,0},{2,0},{3,0}}},
            {{{0,0},{0,1},{1,0},{1,1}},{{0,0},{0,1},{1,0},{1,1}},{{0,0},{0,1},{1,0},{1,1}},{{0,0},{0,1},{1,0},{1,1}}},
            {{{0,1},{1,0},{1,1},{1,2}},{{0,0},{1,0},{1,1},{2,0}},{{0,0},{0,1},{0,2},{1,1}},{{0,1},{1,0},{1,1},{2,1}}},
            {{{0,1},{0,2},{1,0},{1,1}},{{0,0},{1,0},{1,1},{2,1}},{{0,1},{0,2},{1,0},{1,1}},{{0,0},{1,0},{1,1},{2,1}}},
            {{{0,0},{0,1},{1,1},{1,2}},{{0,1},{1,0},{1,1},{2,0}},{{0,0},{0,1},{1,1},{1,2}},{{0,1},{1,0},{1,1},{2,0}}},
            {{{0,0},{1,0},{1,1},{1,2}},{{0,0},{0,1},{1,0},{2,0}},{{0,0},{0,1},{0,2},{1,2}},{{0,1},{1,1},{2,0},{2,1}}},
            {{{0,2},{1,0},{1,1},{1,2}},{{0,0},{1,0},{2,0},{2,1}},{{0,0},{0,1},{0,2},{1,0}},{{0,0},{0,1},{1,1},{2,1}}},
    };

    // Board
    int[][] board = new int[ROWS][COLS];
    int[][] boardColor = new int[ROWS][COLS];
    boolean[][] conflict = new boolean[ROWS][COLS];

    Piece cur, nxt;

    int score = 0, level = 1, lines = 0;
    boolean gameOver = false, paused = false;

    // ── Level progression: time-based (60 seconds per level) ─────
    long levelStartMs = 0;
    static final long LEVEL_DURATION_MS = 60_000L;

    // Level-up flash
    boolean levelUpFlash = false;
    int levelUpTick = 0;
    static final int LEVELUP_DUR = 50;

    // Flash / clear
    List<int[]> flashCells = new ArrayList<>(), pendingClear = new ArrayList<>();
    static final int FLASH_DUR = 12;
    int flashTick = 0;

    List<int[]> lockCells = new ArrayList<>();
    static final int LOCK_DUR = 10;
    int lockTick = 0;

    List<long[]>  shineItems = new ArrayList<>();
    List<long[]>  boxClears  = new ArrayList<>();
    List<float[]> particles  = new ArrayList<>();
    List<float[]> popups     = new ArrayList<>();

    Timer gameTmr, animTmr, levelTmr;
    Random rng = new Random();
    long tick = 0;

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        SoundEngine.init();
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKey(e.getKeyCode()); }
        });
        gameTmr = new Timer(tickMs(), this);
        animTmr = new Timer(33, e -> { tick++; tickAnim(); repaint(); });
        levelTmr = new Timer(1000, e -> checkLevelTimer());
        animTmr.start();
        newGame();
    }

    int tickMs() {
        return Math.max(80, 700 - (level - 1) * 68);
    }

    void newGame() {
        board = new int[ROWS][COLS]; boardColor = new int[ROWS][COLS];
        conflict = new boolean[ROWS][COLS];
        score = 0; lines = 0; level = 1;
        gameOver = false; paused = false;
        flashCells.clear(); pendingClear.clear(); flashTick = 0;
        lockCells.clear(); lockTick = 0;
        shineItems.clear(); boxClears.clear(); particles.clear(); popups.clear();
        levelUpFlash = false; levelUpTick = 0;
        levelStartMs = System.currentTimeMillis();
        cur = makePiece(); nxt = makePiece();
        gameTmr.setDelay(tickMs()); gameTmr.start();
        levelTmr.start();
        SoundEngine.startBGM();
    }

    void checkLevelTimer() {
        if (gameOver || paused) return;
        long elapsed = System.currentTimeMillis() - levelStartMs;
        int newLevel = Math.min(10, (int)(elapsed / LEVEL_DURATION_MS) + 1);
        if (newLevel > level) {
            level = newLevel;
            gameTmr.setDelay(tickMs());
            levelUpFlash = true; levelUpTick = LEVELUP_DUR;
            SoundEngine.play("levelup");
            popups.add(new float[]{ PAD+BOARD_W/2f, PAD+BOARD_H/4f, -3f, 255f, -(float)level });
        }
    }

    float levelProgress() {
        long elapsed = System.currentTimeMillis() - levelStartMs;
        long posInLevel = elapsed % LEVEL_DURATION_MS;
        return (float) posInLevel / LEVEL_DURATION_MS;
    }

    int secondsToNextLevel() {
        long elapsed = System.currentTimeMillis() - levelStartMs;
        long posInLevel = elapsed % LEVEL_DURATION_MS;
        return (int)((LEVEL_DURATION_MS - posInLevel) / 1000);
    }

    Piece makePiece() {
        int t = rng.nextInt(7);
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 9; i++) pool.add(i);
        Collections.shuffle(pool, rng);
        return new Piece(t, 0, 0, (COLS-4)/2, new int[]{pool.get(0),pool.get(1),pool.get(2),pool.get(3)});
    }

    void onKey(int k) {
        if (gameOver) { if (k == KeyEvent.VK_R) newGame(); return; }
        if (k == KeyEvent.VK_P) {
            paused = !paused;
            if (paused) { gameTmr.stop(); levelTmr.stop(); SoundEngine.stopBGM(); }
            else        { gameTmr.start(); levelTmr.start(); SoundEngine.startBGM();
                levelStartMs = System.currentTimeMillis() - (level-1)*LEVEL_DURATION_MS; }
            repaint(); return;
        }
        if (paused || flashTick > 0) return;
        switch (k) {
            case KeyEvent.VK_LEFT:   shiftPiece(0,-1); break;
            case KeyEvent.VK_RIGHT:  shiftPiece(0, 1); break;
            case KeyEvent.VK_DOWN:   softDrop();        break;
            case KeyEvent.VK_UP:     rotate(1);         break;
            case KeyEvent.VK_Z:      rotate(-1);        break;
            case KeyEvent.VK_SPACE:  hardDrop();        break;
            case KeyEvent.VK_Q:      cycleNums(-1);     break;
            case KeyEvent.VK_E:      cycleNums( 1);     break;
            case KeyEvent.VK_MINUS:  adjustLevel(-1);   break;
            case KeyEvent.VK_EQUALS: adjustLevel( 1);   break;
        }
        repaint();
    }

    void adjustLevel(int d) {
        level = Math.max(1, Math.min(10, level+d));
        gameTmr.setDelay(tickMs());
        levelStartMs = System.currentTimeMillis() - (level-1)*LEVEL_DURATION_MS;
    }

    void shiftPiece(int dr,int dc) {
        if (fits(cur.cells(),cur.r+dr,cur.c+dc)) { cur.r+=dr; cur.c+=dc; SoundEngine.play("move"); }
    }

    void rotate(int d) {
        int nr=(cur.rot+d+4)%4;
        Piece p=new Piece(cur.type,nr,cur.r,cur.c,cur.nums.clone());
        boolean ok=false;
        if      (fits(p.cells(),p.r,  p.c  )){ cur=p; ok=true; }
        else if (fits(p.cells(),p.r,  p.c-1)){ p.c--; cur=p; ok=true; }
        else if (fits(p.cells(),p.r,  p.c+1)){ p.c++; cur=p; ok=true; }
        else if (fits(p.cells(),p.r-1,p.c  )){ p.r--; cur=p; ok=true; }
        if (ok) SoundEngine.play("rotate");
    }

    void cycleNums(int d) {
        int[] n=cur.nums;
        if (d>0){int t=n[3];n[3]=n[2];n[2]=n[1];n[1]=n[0];n[0]=t;}
        else    {int t=n[0];n[0]=n[1];n[1]=n[2];n[2]=n[3];n[3]=t;}
        SoundEngine.play("cycle");
    }

    void softDrop() {
        if (fits(cur.cells(),cur.r+1,cur.c)){cur.r++; score++;}
        else lock();
    }

    void hardDrop() {
        int d=0;
        while (fits(cur.cells(),cur.r+1,cur.c)){cur.r++; d++;}
        score+=d*2; SoundEngine.play("drop"); lock();
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (!gameOver && !paused && flashTick==0){ softDrop(); repaint(); }
    }

    void tickAnim() {
        if (lockTick  >0) lockTick--;
        if (flashTick >0){ flashTick--; if(flashTick==0) doClears(); }
        if (levelUpTick>0){ levelUpTick--; if(levelUpTick==0) levelUpFlash=false; }

        long now=System.currentTimeMillis();
        shineItems.removeIf(s->now-s[2]>650);
        boxClears.removeIf(bc->now-bc[2]>800);

        Iterator<float[]> ip=particles.iterator();
        while(ip.hasNext()){float[]p=ip.next();p[0]+=p[2];p[1]+=p[3];p[3]+=0.45f;p[4]-=1f;if(p[4]<=0)ip.remove();}
        Iterator<float[]> iu=popups.iterator();
        while(iu.hasNext()){float[]p=iu.next();p[1]+=p[2];p[3]-=5f;if(p[3]<=0)iu.remove();}
    }

    void lock() {
        int[][]cells=cur.cells(); lockCells.clear();
        for(int i=0;i<4;i++){
            int r=cur.r+cells[i][0], c=cur.c+cells[i][1];
            if(r<0){gameOver=true;gameTmr.stop();levelTmr.stop();SoundEngine.stopBGM();SoundEngine.play("gameover");return;}
            board[r][c]=cur.nums[i]; boardColor[r][c]=cur.type+1;
            lockCells.add(new int[]{r,c});
            Color bc=PIECE_CLR[cur.type];
            shineItems.add(new long[]{bx(c),by(r),System.currentTimeMillis(),bc.getRed(),bc.getGreen(),bc.getBlue()});
        }
        lockTick=LOCK_DUR;
        for(int[]lc:lockCells) spawnParticles(lc[0],lc[1],cur.type,4,2.4f);
        SoundEngine.play("lock");
        markConflicts(); checkRows(); checkBoxes();
        if(flashCells.isEmpty()) spawnNext();
    }

    void spawnNext() {
        cur=nxt; nxt=makePiece();
        if(!fits(cur.cells(),cur.r,cur.c)){
            gameOver=true; gameTmr.stop(); levelTmr.stop();
            SoundEngine.stopBGM(); SoundEngine.play("gameover");
        }
    }

    void checkRows() {
        boolean anyFull=false;
        for(int r=0;r<ROWS;r++){
            boolean full=true;
            for(int c=0;c<COLS;c++) if(board[r][c]==0){full=false;break;}
            if(!full) continue;
            anyFull=true;
            int[]freq=new int[10];
            for(int c=0;c<COLS;c++) freq[board[r][c]]++;
            int cleared=0;
            for(int c=0;c<COLS;c++){
                if(freq[board[r][c]]==1){flashCells.add(new int[]{r,c});pendingClear.add(new int[]{r,c});cleared++;}
            }
            score+=cleared*40*level; lines++;
        }
        if(!flashCells.isEmpty()) flashTick=FLASH_DUR;
    }

    void doClears(){
        int total=0;
        for(int[]cell:pendingClear){
            int r=cell[0],c=cell[1],t=boardColor[r][c]-1;
            if(t>=0&&t<7) spawnParticles(r,c,t,8,4f);
            board[r][c]=0; boardColor[r][c]=0; total++;
        }
        if(total>0) popups.add(new float[]{PAD+BOARD_W/2f,PAD+BOARD_H/2f,-2.5f,255f,total*40*level});
        pendingClear.clear(); flashCells.clear();
        applyGravity(); markConflicts();
        SoundEngine.play("clear"); spawnNext();
    }

    void applyGravity(){
        for(int c=0;c<COLS;c++){
            int w=ROWS-1;
            for(int r=ROWS-1;r>=0;r--) if(board[r][c]!=0){
                if(r!=w){board[w][c]=board[r][c];boardColor[w][c]=boardColor[r][c];board[r][c]=0;boardColor[r][c]=0;}w--;
            }
            for(int r=w;r>=0;r--){board[r][c]=0;boardColor[r][c]=0;}
        }
    }

    void checkBoxes(){
        for(int br=0;br<ROWS/3;br++) for(int bc=0;bc<3;bc++){
            int r0=br*3,c0=bc*3; Set<Integer>nums=new HashSet<>(); boolean ok=true;
            outer: for(int dr=0;dr<3;dr++) for(int dc=0;dc<3;dc++){
                int v=board[r0+dr][c0+dc]; if(v==0||!nums.add(v)){ok=false;break outer;}
            }
            if(ok&&nums.size()==9){
                score+=1000*level; boxClears.add(new long[]{r0,c0,System.currentTimeMillis()});
                for(int dr=0;dr<3;dr++) for(int dc=0;dc<3;dc++){
                    int t=boardColor[r0+dr][c0+dc]-1;
                    if(t>=0&&t<7) spawnParticles(r0+dr,c0+dc,t,8,4f);
                    board[r0+dr][c0+dc]=0; boardColor[r0+dr][c0+dc]=0; conflict[r0+dr][c0+dc]=false;
                }
                SoundEngine.play("box");
            }
        }
    }

    void markConflicts(){
        for(boolean[]row:conflict) Arrays.fill(row,false);
        for(int r=0;r<ROWS;r++){
            int[]cnt=new int[10];
            for(int c=0;c<COLS;c++) if(board[r][c]>0) cnt[board[r][c]]++;
            for(int c=0;c<COLS;c++) if(board[r][c]>0&&cnt[board[r][c]]>1) conflict[r][c]=true;
        }
        for(int c=0;c<COLS;c++){
            int[]cnt=new int[10];
            for(int r=0;r<ROWS;r++) if(board[r][c]>0) cnt[board[r][c]]++;
            for(int r=0;r<ROWS;r++) if(board[r][c]>0&&cnt[board[r][c]]>1) conflict[r][c]=true;
        }
    }

    boolean fits(int[][]cells,int r,int c){
        for(int[]cell:cells){int nr=r+cell[0],nc=c+cell[1];
            if(nr>=ROWS||nc<0||nc>=COLS)return false; if(nr>=0&&board[nr][nc]!=0)return false;}
        return true;
    }

    void spawnParticles(int r,int c,int type,int count,float speed){
        Color base=(type>=0&&type<PIECE_CLR.length)?PIECE_CLR[type]:Color.WHITE;
        int px=bx(c)+CELL/2,py=by(r)+CELL/2;
        for(int i=0;i<count;i++){
            double ang=rng.nextDouble()*Math.PI*2; float spd=speed*(0.5f+rng.nextFloat());
            particles.add(new float[]{px,py,(float)(Math.cos(ang)*spd),(float)(Math.sin(ang)*spd-0.5f),
                    16+rng.nextInt(12),base.getRed(),base.getGreen(),base.getBlue()});
        }
    }

    int bx(int c){return PAD+c*CELL;} int by(int r){return PAD+r*CELL;}
    boolean isFlashing(int r,int c){for(int[]fc:flashCells)if(fc[0]==r&&fc[1]==c)return true;return false;}

    // ════════════════════════════════════════════════════════════
    //  P A I N T
    // ════════════════════════════════════════════════════════════
    @Override protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        g2.setColor(Color.BLACK); g2.fillRect(0,0,W,H);
        drawWatermark(g2); drawBoard(g2); drawBoxGlows(g2); drawShines(g2);
        if(!gameOver){drawGhost(g2); drawActivePiece(g2);}
        drawLockEffect(g2); drawParticles(g2); drawPopups(g2); drawSidebar(g2);
        if(levelUpFlash) drawLevelUpOverlay(g2);
        if(gameOver) drawOverlay(g2,"GAME OVER","Press R to restart",new Color(220,45,45));
        if(paused)   drawOverlay(g2,"PAUSED","Press P to continue",new Color(90,105,255));
    }

    void drawWatermark(Graphics2D g){
        Shape oldClip=g.getClip(); g.setClip(PAD,PAD,BOARD_W,BOARD_H);
        Composite old=g.getComposite(); g.setColor(Color.WHITE);
        g.setFont(new Font("Arial Black",Font.BOLD,Math.max(18,BOARD_W/5)));
        FontMetrics fm=g.getFontMetrics(); int tw=fm.stringWidth("TETRIDOKU");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.028f));
        g.drawString("TETRIDOKU",PAD+(BOARD_W-tw)/2,PAD+BOARD_H/2+fm.getAscent()/2);
        g.setFont(new Font("Arial Black",Font.BOLD,Math.max(11,BOARD_W/10)));
        fm=g.getFontMetrics(); tw=fm.stringWidth("TETRIDOKU");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.016f));
        g.drawString("TETRIDOKU",PAD+(BOARD_W-tw)/2,PAD+BOARD_H/4+fm.getAscent()/2);
        g.drawString("TETRIDOKU",PAD+(BOARD_W-tw)/2,PAD+BOARD_H*3/4+fm.getAscent()/2);
        g.setComposite(old); g.setClip(oldClip);
    }

    void drawBoard(Graphics2D g){
        g.setColor(Color.BLACK); g.fillRect(PAD,PAD,BOARD_W,BOARD_H);
        for(int r=0;r<ROWS;r++) for(int c=0;c<COLS;c++){
            int x=bx(c),y=by(r),v=board[r][c];
            if(v>0){
                if(isFlashing(r,c)){
                    int ct=boardColor[r][c]-1; Color base=(ct>=0&&ct<7)?PIECE_CLR[ct]:Color.WHITE;
                    float a=Math.min(1f,(float)flashTick/(FLASH_DUR*0.55f));
                    g.setColor(new Color((int)(base.getRed()+(255-base.getRed())*a),(int)(base.getGreen()+(255-base.getGreen())*a),(int)(base.getBlue()+(255-base.getBlue())*a)));
                    g.fillRect(x+1,y+1,CELL-2,CELL-2);
                } else drawCell(g,x,y,v,boardColor[r][c]-1,conflict[r][c]);
            }
        }
        g.setStroke(new BasicStroke(2f)); g.setColor(new Color(28,28,42));
        g.drawRect(PAD,PAD,BOARD_W,BOARD_H); g.setStroke(new BasicStroke(1f));
    }

    void drawCell(Graphics2D g,int x,int y,int digit,int type,boolean hot){
        Color base=(type>=0&&type<PIECE_CLR.length)?PIECE_CLR[type]:Color.GRAY;
        if(hot){
            float p=(float)(Math.sin(tick*0.18)*0.3+0.7);
            g.setColor(base.darker().darker()); g.fillRect(x+1,y+1,CELL-2,CELL-2);
            g.setColor(new Color(220,18,18,(int)(p*255))); g.setStroke(new BasicStroke(2.5f));
            g.drawRect(x+1,y+1,CELL-2,CELL-2); g.setStroke(new BasicStroke(1f));
            drawNum(g,x,y,digit,CELL*19/56,new Color(255,130,130));
        } else {
            g.setColor(base.darker().darker()); g.fillRect(x+1,y+1,CELL-2,CELL-2);
            g.setColor(base.brighter()); g.fillRect(x+2,y+2,CELL-4,3); g.fillRect(x+2,y+2,3,CELL-4);
            g.setColor(base.darker()); g.fillRect(x+2,y+CELL-4,CELL-4,2); g.fillRect(x+CELL-4,y+2,2,CELL-4);
            g.setColor(base); g.fillRect(x+4,y+4,CELL-8,CELL-8);
            drawNum(g,x,y,digit,CELL*19/56,Color.WHITE);
        }
    }

    void drawNum(Graphics2D g,int x,int y,int v,int size,Color clr){
        size=Math.max(10,size); g.setFont(new Font("Arial",Font.BOLD,size));
        FontMetrics fm=g.getFontMetrics(); String s=String.valueOf(v);
        int tx=x+(CELL-fm.stringWidth(s))/2, ty=y+(CELL+fm.getAscent()-fm.getDescent())/2-1;
        g.setColor(Color.BLACK); g.drawString(s,tx+1,ty+1); g.setColor(clr); g.drawString(s,tx,ty);
    }

    void drawActivePiece(Graphics2D g){
        if(cur==null)return; int[][]cells=cur.cells();
        for(int i=0;i<4;i++){
            int r=cur.r+cells[i][0],c=cur.c+cells[i][1]; if(r<0)continue;
            int x=bx(c),y=by(r); drawCell(g,x,y,cur.nums[i],cur.type,false);
            Color b=PIECE_CLR[cur.type]; float gl=(float)(Math.sin(tick*0.13)*0.2+0.6);
            g.setColor(new Color(b.brighter().getRed(),b.brighter().getGreen(),b.brighter().getBlue(),(int)(gl*155)));
            g.setStroke(new BasicStroke(1.8f)); g.drawRect(x,y,CELL-1,CELL-1); g.setStroke(new BasicStroke(1f));
        }
    }

    void drawGhost(Graphics2D g){
        if(cur==null)return; int gr=cur.r;
        while(fits(cur.cells(),gr+1,cur.c)) gr++;
        if(gr==cur.r)return;
        int[][]cells=cur.cells(); float[]dash={5f,4f}; Color base=PIECE_CLR[cur.type];
        for(int i=0;i<4;i++){
            int r=gr+cells[i][0],c=cur.c+cells[i][1]; if(r<0)continue;
            int x=bx(c),y=by(r);
            g.setColor(base.darker().darker().darker()); g.fillRect(x+2,y+2,CELL-4,CELL-4);
            g.setColor(base); g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,4,dash,0));
            g.drawRect(x+2,y+2,CELL-4,CELL-4); g.setStroke(new BasicStroke(1f));
            drawNum(g,x,y,cur.nums[i],CELL*19/56,new Color(base.getRed(),base.getGreen(),base.getBlue(),170));
        }
    }

    void drawShines(Graphics2D g){
        if(shineItems.isEmpty())return; long now=System.currentTimeMillis();
        Shape oldClip=g.getClip(); Composite oldComp=g.getComposite();
        for(long[]sh:shineItems){
            long age=now-sh[2]; if(age>=650)continue;
            float progress=age/650f,alpha=0.58f*(1f-progress);
            int x=(int)sh[0],y=(int)sh[1];
            g.setClip(x+1,y+1,CELL-2,CELL-2);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha)); g.setColor(Color.WHITE);
            int bandCX=x+(int)((progress*1.7f-0.2f)*CELL); int bw=Math.max(8,CELL/5);
            g.translate(bandCX,y+CELL/2); g.rotate(-Math.PI/4.0);
            g.fillRect(-bw/2,-CELL*2,bw,CELL*5);
            g.rotate(Math.PI/4.0); g.translate(-bandCX,-(y+CELL/2)); g.setClip(oldClip);
        }
        g.setComposite(oldComp); g.setClip(oldClip);
    }

    void drawLockEffect(Graphics2D g){
        if(lockTick<=0||lockCells.isEmpty())return;
        float a=(float)lockTick/LOCK_DUR; int expand=(int)((1f-a)*12);
        Composite old=g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,a*0.45f));
        for(int[]lc:lockCells){g.setColor(Color.WHITE);g.fillRect(bx(lc[1])+2,by(lc[0])+2,CELL-4,CELL-4);}
        g.setComposite(old); g.setColor(new Color(255,255,255,(int)(a*80))); g.setStroke(new BasicStroke(1.8f));
        for(int[]lc:lockCells) g.drawRect(bx(lc[1])-expand,by(lc[0])-expand,CELL+expand*2,CELL+expand*2);
        g.setStroke(new BasicStroke(1f));
    }

    void drawBoxGlows(Graphics2D g){
        long now=System.currentTimeMillis();
        for(long[]bc:boxClears){
            float alpha=Math.max(0f,1f-(now-bc[2])/800f);
            int x=bx((int)bc[1]),y=by((int)bc[0]),bw=3*CELL,bh=3*CELL;
            Composite old=g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha*0.26f));
            g.setColor(new Color(255,210,50)); g.fillRect(x+1,y+1,bw-2,bh-2); g.setComposite(old);
            g.setColor(new Color(255,215,50,(int)(alpha*255))); g.setStroke(new BasicStroke(2.5f));
            g.drawRect(x+1,y+1,bw-2,bh-2); g.setStroke(new BasicStroke(1f));
        }
    }

    void drawParticles(Graphics2D g){
        for(float[]p:particles){
            float a=p[4]/28f; int sz=Math.max(3,(int)(p[4]/3.5f));
            g.setColor(new Color((int)p[5],(int)p[6],(int)p[7],(int)(a*215)));
            g.fillOval((int)p[0]-sz/2,(int)p[1]-sz/2,sz,sz);
        }
    }

    void drawPopups(Graphics2D g){
        for(float[]p:popups){
            int alpha=Math.min(255,(int)p[3]);
            if(p[4]<0){
                int lv=(int)(-p[4]); String s="LEVEL "+lv+"!";
                g.setFont(new Font("Arial Black",Font.BOLD,26)); FontMetrics fm=g.getFontMetrics();
                int tx=(int)p[0]-fm.stringWidth(s)/2;
                g.setColor(new Color(0,0,0,alpha/2)); g.drawString(s,tx+2,(int)p[1]+2);
                g.setColor(new Color(255,220,0,alpha)); g.drawString(s,tx,(int)p[1]);
            } else {
                String s="+"+(int)p[4]; g.setFont(new Font("Arial",Font.BOLD,20));
                FontMetrics fm=g.getFontMetrics(); int tx=(int)p[0]-fm.stringWidth(s)/2;
                g.setColor(new Color(0,0,0,alpha/2)); g.drawString(s,tx+1,(int)p[1]+1);
                g.setColor(new Color(255,225,45,alpha)); g.drawString(s,tx,(int)p[1]);
            }
        }
    }

    void drawLevelUpOverlay(Graphics2D g){
        float t=(float)levelUpTick/LEVELUP_DUR;
        float a=Math.min(1f,t<0.35f?t/0.35f:t)*0.20f;
        Composite old=g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,a));
        g.setColor(Color.getHSBColor(((tick*6)%360)/360f,0.8f,1f));
        g.fillRect(PAD,PAD,BOARD_W,BOARD_H); g.setComposite(old);
        int cx=PAD+BOARD_W/2,cy=PAD+BOARD_H/2,bw=BOARD_W-40,bh=52;
        g.setColor(new Color(5,5,18,230)); g.fillRoundRect(cx-bw/2,cy-bh/2,bw,bh,12,12);
        float pulse=(float)(Math.sin(tick*0.3)*0.3+0.7);
        g.setColor(new Color(255,(int)(195*pulse),0,(int)(220*pulse)));
        g.setStroke(new BasicStroke(2.5f)); g.drawRoundRect(cx-bw/2,cy-bh/2,bw,bh,12,12); g.setStroke(new BasicStroke(1f));
        String msg="LEVEL  "+level+"  —  SPEED UP!";
        g.setFont(new Font("Arial Black",Font.BOLD,20)); FontMetrics fm=g.getFontMetrics();
        g.setColor(Color.getHSBColor(((tick*6)%360)/360f,0.9f,1f));
        g.drawString(msg,cx-fm.stringWidth(msg)/2,cy+fm.getAscent()/2-4);
    }

    // ── Sidebar ──────────────────────────────────────────────────
    void drawSidebar(Graphics2D g){
        int sx=PAD+BOARD_W+PAD, cx=sx+12, cw=SIDE_W-18;
        g.setColor(new Color(10,10,13)); g.fillRect(sx,0,W-sx,H);
        g.setColor(new Color(36,36,52)); g.fillRect(sx,0,2,H);
        int y=14;

        // Title
        drawCard(g,sx+4,y-8,cw,50);
        g.setFont(new Font("Arial Black",Font.BOLD,20)); g.setColor(new Color(210,215,255));
        g.drawString("TetriDoku",cx,y+12); y+=22;
        g.setFont(new Font("Arial",Font.PLAIN,10)); g.setColor(new Color(60,60,95));
        g.drawString("Tetris  \u00d7  Sudoku",cx,y+4); y+=26;

        // Stats
        drawCard(g,sx+4,y-4,cw,107);
        y=drawStat(g,cx,y+4,"SCORE",score,new Color(255,210,55));
        y=drawStat(g,cx,y+2,"LEVEL",level,speedColor(level));
        y=drawStat(g,cx,y+2,"LINES",lines,new Color(170,180,210)); y+=12;

        // Time-based level progress bar
        drawCard(g,sx+4,y-4,cw,52);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(90,95,145));
        int secs = level < 10 ? secondsToNextLevel() : 0;
        String nextLvLabel = level < 10 ? "NEXT LEVEL IN  " + secs + "s" : "MAX LEVEL!";
        g.drawString(nextLvLabel, cx, y+8); y+=12;
        int barW=cw-24,barH=10,barX=cx,barY=y+2;
        g.setColor(new Color(22,22,32)); g.fillRoundRect(barX,barY,barW,barH,5,5);
        if(level<10){
            float prog=levelProgress();
            int fw=(int)(prog*barW);
            if(fw>0){ g.setColor(speedColor(level)); g.fillRoundRect(barX,barY,fw,barH,5,5); }
        } else {
            g.setColor(new Color(255,180,0)); g.fillRoundRect(barX,barY,barW,barH,5,5);
        }
        g.setColor(new Color(40,40,60)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(barX,barY,barW,barH,5,5);
        y+=26;

        // Speed bar
        drawCard(g,sx+4,y-4,cw,66);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(90,95,145));
        g.drawString("GAME SPEED",cx,y+7); y+=15;
        int segTot=10,segH=11,segW=(cw-24)/segTot;
        for(int i=1;i<=segTot;i++){
            g.setColor(i<=level?speedColor(i):new Color(24,24,34)); g.fillRect(cx+(i-1)*(segW+2),y,segW,segH);
            if(i<=level){g.setColor(new Color(255,255,255,50));g.fillRect(cx+(i-1)*(segW+2)+1,y+1,segW-2,3);}
        }
        y+=segH+4;
        g.setFont(new Font("Arial Black",Font.BOLD,24)); g.setColor(speedColor(level));
        g.drawString(String.valueOf(level),cx+3,y+20);
        g.setFont(new Font("Arial",Font.PLAIN,8)); g.setColor(new Color(70,75,120));
        g.drawString("[\u2013] Slower",cx+28,y+8); g.drawString("[=] Faster",cx+28,y+18); y+=26;

        // Next piece
        drawCard(g,sx+4,y-4,cw,74);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(90,95,145));
        g.drawString("NEXT",cx,y+7); y+=9;
        if(nxt!=null) drawMini(g,nxt,cx,y); y+=56;

        // Q/E badge
        drawCard(g,sx+4,y-4,cw,40);
        g.setFont(new Font("Arial",Font.BOLD,10)); g.setColor(new Color(55,200,80));
        g.drawString("Q / E  \u2014  Shift Numbers",cx,y+9); y+=16;
        g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(30,120,48));
        g.drawString("Rearrange digits on piece",cx,y+4); y+=26;

        // Conflict indicator
        boolean hasConflict=false;
        for(boolean[]row:conflict) for(boolean b:row) if(b){hasConflict=true;break;}
        if(hasConflict){
            float p=(float)(Math.sin(tick*0.15)*0.35+0.65);
            drawCard(g,sx+4,y-2,cw,26,new Color((int)(p*125),8,8));
            g.setFont(new Font("Arial",Font.BOLD,10)); g.setColor(new Color(255,65,65,(int)(p*255)));
            g.drawString("\u26a0  CONFLICT DETECTED",cx,y+14); y+=30;
        } else y+=4;

        // Controls
        drawCard(g,sx+4,y-4,cw,120);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(80,85,140));
        g.drawString("CONTROLS",cx,y+7); y+=15;
        String[][]ctrl={{"\u2190\u2192","Move"},{"\u2191/Z","Rotate"},{"\u2193","Soft drop"},
                {"Space","Hard drop"},{"Q/E","Shift nums"},{"\u2013/=","Speed"},{"P","Pause"},{"R","Restart"}};
        for(String[]row:ctrl){
            g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(225,185,45)); g.drawString(row[0],cx,y);
            g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(55,60,100)); g.drawString(row[1],cx+48,y); y+=13;
        }
        y+=6;

        // Scoring
        drawCard(g,sx+4,y-4,cw,76);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(80,85,140));
        g.drawString("SCORING",cx,y+7); y+=15;
        String[]sl={"Unique clear","3\u00d73 box","Repeated \u2192"};
        String[]sv={"40\u00d7Lvl","1000\u00d7Lvl","Stays"};
        Color[]sc={new Color(180,180,55),new Color(255,210,0),new Color(185,60,60)};
        for(int i=0;i<sl.length;i++){
            g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(55,60,100)); g.drawString(sl[i],cx,y);
            g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(sc[i]); g.drawString(sv[i],cx+90,y); y+=13;
        }
    }

    void drawCard(Graphics2D g,int x,int y,int w,int h){drawCard(g,x,y,w,h,new Color(18,18,24));}
    void drawCard(Graphics2D g,int x,int y,int w,int h,Color bg){
        g.setColor(bg); g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(new Color(36,36,54)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(x,y,w,h,8,8);
    }

    int drawStat(Graphics2D g,int cx,int y,String label,int val,Color clr){
        g.setFont(new Font("Arial",Font.BOLD,8)); g.setColor(new Color(60,65,108)); g.drawString(label,cx,y+10);
        g.setFont(new Font("Arial Black",Font.BOLD,20)); g.setColor(Color.BLACK); g.drawString(String.valueOf(val),cx+1,y+28);
        g.setColor(clr); g.drawString(String.valueOf(val),cx,y+27); return y+32;
    }

    void drawMini(Graphics2D g,Piece p,int sx,int sy){
        int MC=21; int[][]cells=p.cells(); int minR=99,minC=99;
        for(int[]c:cells){minR=Math.min(minR,c[0]);minC=Math.min(minC,c[1]);}
        Color base=PIECE_CLR[p.type];
        for(int i=0;i<4;i++){
            int r=cells[i][0]-minR,c=cells[i][1]-minC,x=sx+c*MC,y=sy+r*MC;
            g.setColor(base.darker().darker()); g.fillRect(x+1,y+1,MC-2,MC-2);
            g.setColor(base.brighter()); g.fillRect(x+2,y+2,MC-4,2); g.fillRect(x+2,y+2,2,MC-4);
            g.setColor(base.darker()); g.fillRect(x+2,y+MC-3,MC-4,1); g.fillRect(x+MC-3,y+2,1,MC-4);
            g.setColor(base); g.fillRect(x+3,y+3,MC-6,MC-6);
            g.setFont(new Font("Arial",Font.BOLD,10)); FontMetrics fm=g.getFontMetrics(); String s=String.valueOf(p.nums[i]);
            g.setColor(Color.BLACK); g.drawString(s,x+(MC-fm.stringWidth(s))/2+1,y+(MC+fm.getAscent()-fm.getDescent())/2);
            g.setColor(Color.WHITE); g.drawString(s,x+(MC-fm.stringWidth(s))/2,y+(MC+fm.getAscent()-fm.getDescent())/2-1);
        }
    }

    Color speedColor(int lv){
        float t=(lv-1)/9f;
        return new Color(Math.min(255,(int)(t*225+30)),Math.min(255,(int)((1f-t)*200+55)),22);
    }

    void drawOverlay(Graphics2D g,String title,String sub,Color clr){
        g.setColor(new Color(0,0,0,200)); g.fillRect(PAD,PAD,BOARD_W,BOARD_H);
        int cx=PAD+BOARD_W/2,cy=PAD+BOARD_H/2;
        g.setFont(new Font("Arial Black",Font.BOLD,36)); FontMetrics fm=g.getFontMetrics();
        g.setColor(new Color(clr.getRed(),clr.getGreen(),clr.getBlue(),50));
        g.drawString(title,cx-fm.stringWidth(title)/2+2,cy-18+2);
        g.setColor(clr); g.drawString(title,cx-fm.stringWidth(title)/2,cy-18);
        g.setFont(new Font("Arial",Font.PLAIN,13)); fm=g.getFontMetrics();
        g.setColor(new Color(190,190,215)); g.drawString(sub,cx-fm.stringWidth(sub)/2,cy+18);
        if(gameOver){
            g.setFont(new Font("Arial Black",Font.BOLD,16)); fm=g.getFontMetrics();
            String sc="Final Score  "+score; g.setColor(new Color(255,215,50));
            g.drawString(sc,cx-fm.stringWidth(sc)/2,cy+52);
        }
    }
}

// ================================================================
//  S O U N D   E N G I N E
// ================================================================
class SoundEngine {
    static final int SR=44100;
    static final Map<String,byte[]> cache=new HashMap<>();
    static volatile boolean ready=false;
    static volatile boolean bgmRunning=false;
    static Thread bgmThread=null;
    static long bgmSample=0;

    static final int BPM=165, BEAT=SR*60/BPM, BAR=BEAT*4;
    static final float[]MEL ={440f,329.6f,349.2f,392f,349.2f,329.6f,293.7f,261.6f,261.6f,329.6f,392f,440f,493.9f,440f,392f,329.6f};
    static final float[]BASS={110f,130.8f,87.3f,98f,  130.8f,87.3f,65.4f,98f,   65.4f,87.3f,110f,130.8f, 123.5f,110f,98f,87.3f};

    static void init(){
        if(ready)return;
        Thread t=new Thread(()->{
            try{
                cache.put("move",    sine(880,13,0.07f,14f));
                cache.put("rotate",  sweep(420,840,36,0.14f));
                cache.put("cycle",   chime());
                cache.put("lock",    lockSnd());
                cache.put("drop",    dropSnd());
                cache.put("clear",   clearSnd());
                cache.put("box",     boxSnd());
                cache.put("gameover",gameOverSnd());
                cache.put("levelup", levelUpSnd());
                ready=true;
            }catch(Exception ignored){}
        },"snd-init");
        t.setDaemon(true); t.start();
    }

    static void play(String n){
        if(!ready)return; byte[]d=cache.get(n); if(d==null)return;
        new Thread(()->emit(d),"snd").start();
    }

    static void emit(byte[]data){
        try{AudioFormat fmt=new AudioFormat(SR,16,1,true,false);
            SourceDataLine l=AudioSystem.getSourceDataLine(fmt);
            l.open(fmt,2048);l.start();l.write(data,0,data.length);l.drain();l.close();
        }catch(Exception ignored){}
    }

    static void startBGM(){
        if(bgmRunning)return; bgmRunning=true;
        bgmThread=new Thread(()->{
            try{AudioFormat fmt=new AudioFormat(SR,16,1,true,false);
                SourceDataLine l=AudioSystem.getSourceDataLine(fmt);
                l.open(fmt,4096);l.start();
                while(bgmRunning)l.write(bgmFrame(),0,1024);
                l.drain();l.close();
            }catch(Exception ignored){}
        },"bgm");
        bgmThread.setDaemon(true); bgmThread.start();
    }

    static void stopBGM(){ bgmRunning=false; }

    static byte[] bgmFrame(){
        int n=512; byte[]buf=new byte[n*2]; int pat=BEAT*16;
        for(int i=0;i<n;i++){
            long s=bgmSample+i; int pos=(int)(s%pat),ni=pos/BEAT,pin=pos%BEAT;
            float nt=(float)pin/BEAT;
            float mf=MEL[ni%MEL.length],mEnv=melEnv(nt),mPh=(float)(s*mf/SR)%1f;
            float mel=(mPh<0.5f?mPh*4f-1f:3f-mPh*4f)*mEnv*0.15f;
            float mPh2=(float)(s*mf*2/SR)%1f; mel+=(mPh2<0.5f?mPh2*4f-1f:3f-mPh2*4f)*mEnv*0.05f;
            float bf=BASS[ni%BASS.length],bPh=(float)(s*bf/SR)%1f;
            float bass=(bPh*2f-1f)*bassEnv(nt)*0.12f;
            float hh=0f; int ph=pin%(BEAT/2);
            if(ph<BEAT/8){float t2=(float)ph/(BEAT/8);long seed=(s/64)*6364136223846793005L+1442695040888963407L;
                float noise=((seed>>>33)/(float)(1L<<31)-1f); hh=noise*(float)Math.exp(-8*t2)*0.036f;}
            float kick=0f; int pib=(int)(s%BAR),kz=BEAT/5;
            if(pib<kz||(pib>=BEAT*2&&pib<BEAT*2+kz)){int kp=pib%BEAT;float kt=(float)kp/kz;
                kick=(float)Math.sin(2*Math.PI*80f*(float)Math.exp(-kt*8)*kp/SR)*(float)Math.exp(-kt*4)*0.19f;}
            float snare=0f;
            if(pib>=BEAT&&pib<BEAT+BEAT/5){int sp=pib-BEAT;float st=(float)sp/(BEAT/5);
                long ss=(s/32)*2862933555777941757L+3037000499L;
                snare=((ss>>>33)/(float)(1L<<31)-1f)*(float)Math.exp(-6*st)*0.025f;}
            float mix=Math.max(-0.92f,Math.min(0.92f,mel+bass+hh+kick+snare));
            short v=(short)(mix*32767); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        bgmSample+=n; return buf;
    }

    static float melEnv(float t){if(t<0.04f)return t/0.04f;if(t<0.78f)return 1f;return(float)Math.exp(-5*(t-0.78f)/0.22f);}
    static float bassEnv(float t){if(t<0.015f)return t/0.015f;return(float)Math.exp(-4.5f*t);}

    static byte[] chime(){
        int n=SR*110/1000; byte[]buf=new byte[n*2];
        float[]f={1318.5f,1760f}; float[]a={0.38f,0.24f};
        for(int i=0;i<n;i++){float t=i/(float)n,env=(float)Math.exp(-5.5f*t)*(1f-(float)Math.exp(-40f*t)),s=0;
            for(int h=0;h<f.length;h++) s+=(float)Math.sin(2*Math.PI*f[h]*i/SR)*a[h]; s*=env;
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);}
        return buf;
    }

    static byte[] lockSnd(){
        int n=SR*85/1000; byte[]buf=new byte[n*2];
        for(int i=0;i<n;i++){float t=i/(float)n,env=(float)Math.exp(-9f*t);
            float s=(float)(Math.sin(2*Math.PI*115*i/SR)+Math.sin(2*Math.PI*230*i/SR)*0.28f+Math.sin(2*Math.PI*460*i/SR)*0.10f)*env*0.30f;
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);}
        return buf;
    }

    static byte[] dropSnd(){
        int n=SR*105/1000; byte[]buf=new byte[n*2]; double ph=0;
        for(int i=0;i<n;i++){float t=i/(float)n,freq=290-210*t; ph+=2*Math.PI*freq/SR;
            float env=(float)Math.exp(-5f*t)*(1f-(float)Math.exp(-28f*t));
            float s=(float)Math.sin(ph)*env*0.40f;
            if(t<0.04f){long seed=(long)i*6364136223846793005L;float noise=((seed>>>33)/(float)(1L<<31)-1f);s+=noise*(1f-t/0.04f)*0.07f;}
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);}
        return buf;
    }

    static byte[] clearSnd(){
        int[]freqs={523,659,784,1047}; int noteMs=70,total=SR*(noteMs*4+120)/1000; byte[]buf=new byte[total*2];
        for(int ni=0;ni<freqs.length;ni++){
            int start=SR*ni*noteMs/1000,dur=SR*(noteMs+95)/1000;
            for(int i=start;i<Math.min(total,start+dur);i++){
                int loc=i-start; float t=loc/(float)dur,att=Math.min(1f,loc/(float)(SR*5/1000)),env=att*(float)Math.exp(-4.5f*t);
                float s=(float)(Math.sin(2*Math.PI*freqs[ni]*i/SR)+Math.sin(2*Math.PI*freqs[ni]*2*i/SR)*0.20f)*env*0.42f;
                addSample(buf,i,s);}}
        return buf;
    }

    static byte[] boxSnd(){
        int n=SR*300/1000; byte[]buf=new byte[n*2];
        float[]hf={523f,659f,784f,1047f,1318f}; float[]ha={0.36f,0.26f,0.18f,0.11f,0.06f};
        for(int i=0;i<n;i++){float t=i/(float)n,att=Math.min(1f,i/(float)(SR*7/1000)),env=att*(float)Math.exp(-3.4f*t),s=0;
            for(int h=0;h<hf.length;h++) s+=(float)Math.sin(2*Math.PI*hf[h]*i/SR)*ha[h]; s*=env;
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);}
        return buf;
    }

    static byte[] levelUpSnd(){
        int[]nf={523,784,1047,1318}; int noteMs=58,gap=28,total=SR*((noteMs+gap)*nf.length+100)/1000; byte[]buf=new byte[total*2];
        for(int ni=0;ni<nf.length;ni++){
            int start=SR*ni*(noteMs+gap)/1000,dur=SR*(noteMs+(ni==nf.length-1?180:0))/1000;
            for(int i=start;i<Math.min(total,start+dur);i++){
                int loc=i-start; float t=loc/(float)dur,att=Math.min(1f,loc/(float)(SR*5/1000)),env=att*(float)Math.exp(-5.0f*t);
                float s=(float)(Math.sin(2*Math.PI*nf[ni]*i/SR)+Math.sin(2*Math.PI*nf[ni]*2*i/SR)*0.18f)*env*0.40f;
                addSample(buf,i,s);}}
        return buf;
    }

    static byte[] gameOverSnd(){
        int[]notes={330,294,262,247,220}; int noteMs=140,total=SR*noteMs*notes.length/1000; byte[]buf=new byte[total*2];
        for(int ni=0;ni<notes.length;ni++){
            int start=SR*ni*noteMs/1000,dur=SR*(noteMs-10)/1000;
            for(int i=start;i<Math.min(total,start+dur);i++){
                int loc=i-start; float t=loc/(float)dur,att=Math.min(1f,loc/(float)(SR*8/1000)),env=att*(float)Math.exp(-3.0f*t);
                float s=(float)(Math.sin(2*Math.PI*notes[ni]*i/SR)+Math.sin(2*Math.PI*notes[ni]*1.5*i/SR)*0.13f)*env*0.36f;
                addSample(buf,i,s);}}
        return buf;
    }

    static byte[] sine(float freq,int ms,float vol,float decay){
        int n=SR*ms/1000; byte[]buf=new byte[n*2];
        for(int i=0;i<n;i++){float env=(float)Math.exp(-decay*(float)i/n),s=(float)Math.sin(2*Math.PI*freq*i/SR)*env*vol;
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);}
        return buf;
    }

    static byte[] sweep(float f1,float f2,int ms,float vol){
        int n=SR*ms/1000; byte[]buf=new byte[n*2]; double ph=0;
        for(int i=0;i<n;i++){float t=i/(float)n,env=(float)Math.exp(-4.5f*t)*(1f-(float)Math.exp(-30f*t)),freq=f1+(f2-f1)*t;
            ph+=2*Math.PI*freq/SR; float s=(float)Math.sin(ph)*env*vol;
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);}
        return buf;
    }

    static short clamp(float s){return(short)Math.max(-32767,Math.min(32767,(int)(s*32767)));}

    static void addSample(byte[]buf,int i,float s){
        if(i*2+1>=buf.length)return;
        short e=(short)((buf[i*2+1]<<8)|(buf[i*2]&0xFF));
        int sum=e+(int)(s*32767); short v=(short)Math.max(-32767,Math.min(32767,sum));
        buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
    }
}

// ================================================================
class Piece {
    int type,rot,r,c; int[]nums;
    Piece(int type,int rot,int r,int c,int[]nums){
        this.type=type;this.rot=rot;this.r=r;this.c=c;this.nums=nums;
    }
    int[][]cells(){return GamePanel.SHAPES[type][rot];}
}