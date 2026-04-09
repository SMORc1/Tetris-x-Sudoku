import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;

// ================================================================
//  T E T R I D O K U  v13
//  Rich pause menu: Resume | Restart | Settings | Main Menu
//  Settings panel: mute, speed, ghost piece toggle
// ================================================================

public class TetriDoku extends JFrame {

    static int highScore = 0;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> showMainMenu());
    }

    static void showMainMenu() {
        MainMenuDialog menu = new MainMenuDialog();
        menu.setVisible(true);
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
//  M A I N   M E N U
// ================================================================
class MainMenuDialog extends JFrame {

    private Timer animTimer;
    private long startMs = System.currentTimeMillis();
    private MenuPanel menuPanel;

    MainMenuDialog() {
        setTitle("TetriDoku");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setUndecorated(false);
        menuPanel = new MenuPanel();
        menuPanel.setPreferredSize(new Dimension(540, 580));
        add(menuPanel);
        pack();
        setLocationRelativeTo(null);
        animTimer = new Timer(33, e -> menuPanel.repaint());
        animTimer.start();
        SoundEngine.init();
        SoundEngine.startMenuBGM();
    }

    void launch() {
        animTimer.stop();
        SoundEngine.stopBGM();
        dispose();
        TetriDoku game = new TetriDoku();
        game.setVisible(true);
    }

    void openTutorial() {
        animTimer.stop();
        SoundEngine.stopBGM();
        dispose();
        TutorialDialog tut = new TutorialDialog(null, true);
        tut.setVisible(true);
        if (tut.shouldPlay()) {
            TetriDoku game = new TetriDoku();
            game.setVisible(true);
        } else {
            MainMenuDialog menu = new MainMenuDialog();
            menu.setVisible(true);
        }
    }

    class MenuPanel extends JPanel {
        // Floating piece animation state
        float[][] pieces = new float[6][6]; // x,y,vx,vy,type,rot
        Random rng = new Random(42);

        MenuPanel() {
            setBackground(new Color(6, 6, 10));
            setFocusable(true);

            for (int i = 0; i < pieces.length; i++) {
                pieces[i][0] = rng.nextFloat() * 540;
                pieces[i][1] = rng.nextFloat() * 580;
                pieces[i][2] = (rng.nextFloat() - 0.5f) * 0.7f;
                pieces[i][3] = (rng.nextFloat() - 0.5f) * 0.7f;
                pieces[i][4] = rng.nextInt(7);
                pieces[i][5] = rng.nextInt(4);
            }

            // Build buttons
            setLayout(null);

            JButton playBtn = makeMenuBtn("▶   PLAY GAME", new Color(30, 210, 100), new Color(0, 140, 60));
            JButton tutBtn  = makeMenuBtn("📖   HOW TO PLAY", new Color(80, 160, 255), new Color(30, 80, 200));
            JButton quitBtn = makeMenuBtn("✕   QUIT", new Color(200, 70, 70), new Color(130, 30, 30));

            playBtn.setBounds(135, 310, 270, 52);
            tutBtn .setBounds(135, 374, 270, 52);
            quitBtn.setBounds(135, 438, 270, 52);

            add(playBtn); add(tutBtn); add(quitBtn);

            playBtn.addActionListener(e -> launch());
            tutBtn .addActionListener(e -> openTutorial());
            quitBtn.addActionListener(e -> System.exit(0));
        }

        JButton makeMenuBtn(String text, Color fg, Color border) {
            JButton b = new JButton(text);
            b.setFont(new Font("Arial Black", Font.BOLD, 15));
            b.setForeground(fg);
            b.setBackground(new Color(14, 14, 22));
            b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 2),
                BorderFactory.createEmptyBorder(8, 24, 8, 24)
            ));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setOpaque(true);
            // Hover effect
            b.addMouseListener(new MouseAdapter() {
                Color origBg = new Color(14, 14, 22);
                public void mouseEntered(MouseEvent e) { b.setBackground(new Color(24, 24, 38)); }
                public void mouseExited(MouseEvent e)  { b.setBackground(origBg); }
            });
            return b;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            long now = System.currentTimeMillis();
            float t = (now - startMs) / 1000f;

            // Background gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(6, 6, 14), 0, getHeight(), new Color(10, 8, 20));
            g2.setPaint(gp); g2.fillRect(0, 0, getWidth(), getHeight());

            // Draw floating mini pieces in background
            drawFloatingPieces(g2, t);

            // Logo glow
            drawLogoSection(g2, t);

            // High score if set
            if (TetriDoku.highScore > 0) {
                g2.setFont(new Font("Arial Black", Font.BOLD, 13));
                String hs = "BEST  " + TetriDoku.highScore;
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(hs)) / 2;
                g2.setColor(new Color(255, 210, 50, 200));
                g2.drawString(hs, tx, 294);
            }

            // Version tag
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.setColor(new Color(40, 40, 65));
            g2.drawString("v10", getWidth() - 30, getHeight() - 8);
        }

        void drawFloatingPieces(Graphics2D g, float t) {
            // Update positions
            for (float[] p : pieces) {
                p[0] += p[2]; p[1] += p[3];
                if (p[0] < -60 || p[0] > getWidth() + 60) p[2] *= -1;
                if (p[1] < -60 || p[1] > getHeight() + 60) p[3] *= -1;
            }
            int[][][][] SHAPES = GamePanel.SHAPES;
            Color[] CLRS = GamePanel.PIECE_CLR;
            int SZ = 14;
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
            for (float[] p : pieces) {
                int type = (int) p[4], rot = (int) p[5];
                int[][] cells = SHAPES[type][rot];
                Color clr = CLRS[type];
                for (int[] c : cells) {
                    int x = (int) p[0] + c[1] * SZ, y = (int) p[1] + c[0] * SZ;
                    g.setColor(clr); g.fillRect(x, y, SZ - 1, SZ - 1);
                }
            }
            g.setComposite(old);
        }

        void drawLogoSection(Graphics2D g, float t) {
            int cx = getWidth() / 2;

            // Pulsing glow behind title
            float pulse = (float)(Math.sin(t * 1.4) * 0.15 + 0.85);
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f * pulse));
            g.setColor(new Color(0, 200, 230));
            g.fillOval(cx - 170, 60, 340, 110);
            g.setComposite(old);

            // T E T R I D O K U
            g.setFont(new Font("Arial Black", Font.BOLD, 56));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth("TETRIDOKU");
            // Shadow
            g.setColor(new Color(0, 180, 200, 60));
            g.drawString("TETRIDOKU", cx - tw / 2 + 3, 120 + 3);
            // Main gradient letter effect
            g.setPaint(new GradientPaint(cx - tw / 2, 70, new Color(0, 230, 255), cx + tw / 2, 120, new Color(0, 170, 230)));
            g.drawString("TETRIDOKU", cx - tw / 2, 120);

            // Tagline
            g.setFont(new Font("Arial", Font.ITALIC, 15));
            String tag = "Tetris  ×  Sudoku  —  two classics, one game";
            fm = g.getFontMetrics();
            g.setColor(new Color(100, 108, 168));
            g.drawString(tag, cx - fm.stringWidth(tag) / 2, 148);

            // Divider
            g.setColor(new Color(30, 30, 50));
            g.fillRect(80, 162, getWidth() - 160, 1);

            // Mini piece color strip
            Color[] clrs = GamePanel.PIECE_CLR;
            int stripW = 28, gap = 6, total = clrs.length * (stripW + gap) - gap;
            int sx = cx - total / 2;
            for (int i = 0; i < clrs.length; i++) {
                float phase = (float) Math.sin(t * 2.0 + i * 0.5) * 0.15f + 0.85f;
                Color c = clrs[i];
                g.setColor(new Color((int)(c.getRed()*phase), (int)(c.getGreen()*phase), (int)(c.getBlue()*phase)));
                g.fillRoundRect(sx + i * (stripW + gap), 172, stripW, 10, 4, 4);
            }

            // Feature tags
            String[] tags = {"🎮 Falling Pieces", "🔢 Sudoku Rules", "🏆 High Scores", "🎵 Live Music"};
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            fm = g.getFontMetrics();
            int tagY = 210;
            int tagTotalW = 0;
            for (String tg : tags) tagTotalW += fm.stringWidth(tg) + 22;
            tagTotalW -= 22;
            int tagX = cx - tagTotalW / 2;
            for (String tg : tags) {
                int tw2 = fm.stringWidth(tg) + 16;
                g.setColor(new Color(18, 18, 30));
                g.fillRoundRect(tagX, tagY - 12, tw2, 20, 6, 6);
                g.setColor(new Color(50, 50, 80));
                g.drawRoundRect(tagX, tagY - 12, tw2, 20, 6, 6);
                g.setColor(new Color(130, 135, 185));
                g.drawString(tg, tagX + 8, tagY + 2);
                tagX += tw2 + 6;
            }

            // "Use keyboard to play" hint
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            String hint = "Keyboard controlled — best on desktop";
            fm = g.getFontMetrics();
            g.setColor(new Color(50, 52, 85));
            g.drawString(hint, cx - fm.stringWidth(hint) / 2, 246);
        }
    }
}

// ================================================================
//  T U T O R I A L   D I A L O G
// ================================================================
class TutorialDialog extends JDialog {

    private boolean play = false;
    private int currentPage = 0;
    private static final int PAGES = 5;
    private TutorialPanel tutPanel;
    private final boolean showReturnToMenu;

    TutorialDialog(Frame owner, boolean showReturnToMenu) {
        super(owner, "How to Play TetriDoku", true);
        this.showReturnToMenu = showReturnToMenu;
        setBackground(new Color(8, 8, 12));
        setUndecorated(false);
        setResizable(false);

        tutPanel = new TutorialPanel();
        tutPanel.setPreferredSize(new Dimension(760, 500));
        add(tutPanel, BorderLayout.CENTER);

        // Bottom button bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        btnBar.setBackground(new Color(8, 8, 12));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 60)));

        JButton prevBtn = makeBtn("◀  Back", new Color(180, 185, 230));
        JButton nextBtn = makeBtn("Next  ▶", new Color(180, 185, 230));
        JButton playBtn = makeBtn("▶  PLAY NOW!", new Color(80, 255, 120));
        playBtn.setFont(new Font("Arial Black", Font.BOLD, 14));

        JButton backToMenuBtn = makeBtn("← Main Menu", new Color(120, 125, 175));

        prevBtn.addActionListener(e -> { if (currentPage > 0) { currentPage--; tutPanel.repaint(); updateButtons(prevBtn, nextBtn, playBtn); } });
        nextBtn.addActionListener(e -> { if (currentPage < PAGES - 1) { currentPage++; tutPanel.repaint(); updateButtons(prevBtn, nextBtn, playBtn); } });
        playBtn.addActionListener(e -> { play = true; dispose(); });

        JButton skipBtn = makeBtn("Skip →", new Color(80, 80, 110));
        skipBtn.addActionListener(e -> { play = true; dispose(); });

        if (showReturnToMenu) btnBar.add(backToMenuBtn);
        btnBar.add(prevBtn); btnBar.add(nextBtn); btnBar.add(playBtn); btnBar.add(skipBtn);
        add(btnBar, BorderLayout.SOUTH);

        backToMenuBtn.addActionListener(e -> { play = false; dispose(); });

        updateButtons(prevBtn, nextBtn, playBtn);
        pack();
        setLocationRelativeTo(null);
    }

    // Legacy constructor
    TutorialDialog(Frame owner) { this(owner, false); }

    private JButton makeBtn(String label, Color fg) {
        JButton b = new JButton(label);
        b.setBackground(new Color(22, 22, 32));
        b.setForeground(fg);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 50, 75), 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
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
            drawPageBackground(g2, currentPage);
            switch (currentPage) {
                case 0: drawPage0(g2); break;
                case 1: drawPage1(g2); break;
                case 2: drawPage2(g2); break;
                case 3: drawPage4(g2); break;   // power-ups first
                case 4: drawPage3(g2); break;   // then levels
            }
            drawPageDots(g2);
        }

        // ── Per-page background art ───────────────────────────────
        void drawPageBackground(Graphics2D g, int page) {
            int W = getWidth(), H = getHeight();
            Composite old = g.getComposite();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            switch (page) {
                case 0: drawBgIntro(g, W, H); break;
                case 1: drawBgControls(g, W, H); break;
                case 2: drawBgScoring(g, W, H); break;
                case 3: drawBgPowerUps(g, W, H); break;
                case 4: drawBgLevels(g, W, H); break;
            }
            g.setComposite(old);
        }

        // BG 0 — Intro: faint falling tetromino silhouettes + digit grid
        void drawBgIntro(Graphics2D g, int W, int H) {
            Composite old = g.getComposite();
            // Subtle digit grid
            g.setFont(new Font("Arial", Font.BOLD, 13));
            int[] digits = {1,2,3,4,5,6,7,8,9};
            Color[] dclrs = {new Color(0,215,230), new Color(240,200,0), new Color(160,0,220),
                             new Color(20,200,55),  new Color(220,30,30), new Color(20,80,220), new Color(220,115,0)};
            java.util.Random rng = new java.util.Random(77);
            for (int col = 0; col < W; col += 38) {
                for (int row = 0; row < H; row += 38) {
                    int d = digits[rng.nextInt(9)];
                    Color c = dclrs[rng.nextInt(7)];
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.045f));
                    g.setColor(c);
                    g.drawString(String.valueOf(d), col + rng.nextInt(12), row + rng.nextInt(12));
                }
            }
            // Faint tetromino block silhouettes in corners
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f));
            drawSilhouette(g, 10, H-80, new int[][]{{0,0},{0,1},{0,2},{0,3}}, new Color(0,215,230), 18);
            drawSilhouette(g, W-85, 20, new int[][]{{0,0},{0,1},{1,0},{1,1}}, new Color(240,200,0), 18);
            drawSilhouette(g, W-65, H-65, new int[][]{{0,1},{1,0},{1,1},{1,2}}, new Color(160,0,220), 18);
            drawSilhouette(g, 15, 20, new int[][]{{0,0},{1,0},{1,1},{2,1}}, new Color(20,200,55), 18);
            g.setComposite(old);
        }

        // BG 1 — Controls: faint keyboard rows hinting at keys
        void drawBgControls(Graphics2D g, int W, int H) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.04f));
            // Ghosted key grid top-right corner
            String[] keys = {"Q","W","E","R","T","A","S","D","F","G","Z","X","C","V","B"};
            g.setFont(new Font("Arial Black", Font.BOLD, 11));
            int kx = W - 120, ky = H - 100;
            for (int i = 0; i < keys.length; i++) {
                int col = i % 5, row = i / 5;
                int bx = kx + col*22, by = ky + row*22;
                g.setColor(new Color(80,100,200));
                g.fillRoundRect(bx, by, 18, 18, 4, 4);
                g.setColor(new Color(180,200,255));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(keys[i], bx+(18-fm.stringWidth(keys[i]))/2, by+13);
            }
            // Faint horizontal scan lines
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.025f));
            g.setColor(new Color(60,80,180));
            for (int y = 0; y < H; y += 6) g.fillRect(0, y, W, 1);
            g.setComposite(old);
        }

        // BG 2 — Scoring: faint 9x18 board grid with a few glowing cells
        void drawBgScoring(Graphics2D g, int W, int H) {
            Composite old = g.getComposite();
            int cellSz = 28, cols = 9, rows = 8;
            int gridW = cols * cellSz, gridH = rows * cellSz;
            int gx = (W - gridW) / 2, gy = (H - gridH) / 2;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f));
            g.setColor(new Color(30,35,70));
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    g.drawRect(gx + c*cellSz, gy + r*cellSz, cellSz, cellSz);
            // A few accent cells
            Color[] glows = {new Color(0,215,230,60), new Color(240,200,0,50), new Color(80,210,80,55), new Color(220,70,70,45)};
            int[][] lit = {{1,2},{2,5},{4,1},{3,7},{5,4},{6,2},{0,8}};
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            for (int i = 0; i < lit.length; i++) {
                g.setColor(glows[i % glows.length]);
                g.fillRect(gx + lit[i][1]*cellSz+1, gy + lit[i][0]*cellSz+1, cellSz-2, cellSz-2);
            }
            g.setComposite(old);
        }

        // BG 3 — Power-ups: starfield + faint energy rings
        void drawBgPowerUps(Graphics2D g, int W, int H) {
            Composite old = g.getComposite();
            java.util.Random rng = new java.util.Random(42);
            // Stars
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            for (int i = 0; i < 80; i++) {
                int sx = rng.nextInt(W), sy = rng.nextInt(H);
                int sz = rng.nextInt(2) + 1;
                float bright = rng.nextFloat() * 0.5f + 0.3f;
                g.setColor(new Color(bright, bright, bright * 1.3f > 1f ? 1f : bright * 1.3f));
                g.fillOval(sx, sy, sz, sz);
            }
            // Subtle energy rings behind each card position
            Color[] ringClrs = {new Color(100,200,255,18), new Color(255,175,40,18), new Color(220,80,200,18)};
            int[] ringYs = {130, 260, 390};
            g.setStroke(new BasicStroke(18f));
            for (int i = 0; i < 3; i++) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.07f));
                g.setColor(ringClrs[i]);
                g.drawOval(W/2-180, ringYs[i]-60, 360, 120);
            }
            g.setStroke(new BasicStroke(1f));
            g.setComposite(old);
        }

        // BG 4 — Levels: speed lines radiating from center-left
        void drawBgLevels(Graphics2D g, int W, int H) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.055f));
            int ox = W / 2, oy = H / 2;
            java.util.Random rng = new java.util.Random(13);
            for (int i = 0; i < 32; i++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                float len = 180f + rng.nextFloat() * 280f;
                float width = 0.8f + rng.nextFloat() * 2.5f;
                int endX = (int)(ox + Math.cos(angle) * len);
                int endY = (int)(oy + Math.sin(angle) * len);
                float t = i / 31f;
                g.setColor(new Color(
                    Math.min(255,(int)(t*200+55)),
                    Math.min(255,(int)((1f-t)*180+55)), 20));
                g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.drawLine(ox, oy, endX, endY);
            }
            g.setStroke(new BasicStroke(1f));
            g.setComposite(old);
        }

        // Helper: draw a tetromino silhouette (filled blocks only, no text)
        void drawSilhouette(Graphics2D g, int ox, int oy, int[][] cells, Color clr, int sz) {
            g.setColor(clr);
            for (int[] c : cells)
                g.fillRoundRect(ox + c[1]*sz, oy + c[0]*sz, sz-2, sz-2, 3, 3);
        }

        void drawPageDots(Graphics2D g) {
            int dotSize = 8, gap = 16, totalW = PAGES * dotSize + (PAGES - 1) * (gap - dotSize);
            int sx = (getWidth() - totalW) / 2, y = getHeight() - 16;
            for (int i = 0; i < PAGES; i++) {
                g.setColor(i == currentPage ? new Color(100, 200, 255) : new Color(40, 40, 60));
                g.fillOval(sx + i * gap, y, dotSize, dotSize);
            }
        }

        // Page 0: Welcome + concept
        void drawPage0(Graphics2D g) {
            int W = getWidth(), H = getHeight();
            // Reserve 28px at bottom for dots, 8px padding top
            int drawH = H - 28;

            // ── Title block ──────────────────────────────────────
            int titleY = 20;
            g.setFont(new Font("Arial Black", Font.BOLD, 32));
            drawShadowText(g, "TETRIDOKU", W / 2, titleY + 26, new Color(0, 215, 230), 3);

            g.setFont(new Font("Arial", Font.ITALIC, 13));
            drawCenteredText(g, "Tetris  \u00d7  Sudoku  \u2014  two classics, one game",
                    W / 2, titleY + 48, new Color(140, 145, 190));

            int divY = titleY + 56;
            g.setColor(new Color(35, 35, 55));
            g.fillRect(32, divY, W - 64, 1);

            // ── Two columns ──────────────────────────────────────
            // Total available height for boxes + note: drawH - divY - 6
            int boxTop   = divY + 8;
            int noteH    = 18;     // note text at bottom
            int boxH     = drawH - boxTop - noteH - 6;  // whatever remains
            boxH         = Math.min(boxH, 210);          // cap so it's never too tall

            int margin   = 20;
            int midGap   = 30;    // gap between boxes (where × lives)
            int bw       = (W - margin * 2 - midGap) / 2;
            int leftX    = margin;
            int rightX   = margin + bw + midGap;

            // ── Left box: Tetris ─────────────────────────────────
            drawBox(g, leftX, boxTop, bw, boxH, new Color(0, 215, 230), "TETRIS SIDE");
            String[] tetLines = {
                "\u2022 Pieces fall from above",
                "\u2022 7 classic tetromino shapes",
                "\u2022 Rotate (\u2191/Z) & move (\u2190\u2192)",
                "\u2022 Hard drop with Space",
                "\u2022 Speed increases per level"
            };
            int lineH = 20, textTop = boxTop + 40;
            drawBoxLines(g, leftX + 12, textTop, tetLines, new Color(160, 220, 235));

            // Mini piece — only draw if there's room
            int miniTop = textTop + tetLines.length * lineH + 4;
            if (miniTop + 16 < boxTop + boxH - 4) {
                drawMiniPiece(g, leftX + 12, miniTop,
                    new int[][]{{0,0},{0,1},{0,2},{0,3}}, new Color(0, 215, 230));
            }

            // ── × symbol — exactly centered horizontally and vertically in mid gap ──
            int crossX = leftX + bw + midGap / 2;   // horizontal center of gap
            int crossY = boxTop + boxH / 2 + 9;      // vertical center of box
            g.setFont(new Font("Arial Black", Font.BOLD, 24));
            FontMetrics crossFm = g.getFontMetrics();
            g.setColor(new Color(0, 0, 0, 80));
            g.drawString("\u00d7", crossX - crossFm.stringWidth("\u00d7")/2 + 2, crossY + 2);
            g.setColor(Color.WHITE);
            g.drawString("\u00d7", crossX - crossFm.stringWidth("\u00d7")/2, crossY);

            // ── Right box: Sudoku ────────────────────────────────
            drawBox(g, rightX, boxTop, bw, boxH, new Color(240, 200, 0), "SUDOKU SIDE");
            String[] sudLines = {
                "\u2022 Each cell holds a digit (1\u20139)",
                "\u2022 No repeats in any row/column",
                "\u2022 Fill a 3\u00d73 box for bonus pts",
                "\u2022 Use Q/E to reorder digits",
                "\u2022 Conflicts glow red \u2014 avoid!"
            };
            drawBoxLines(g, rightX + 12, textTop, sudLines, new Color(240, 225, 160));

            // Mini sudoku grid — only if room
            int gridTop = textTop + sudLines.length * lineH + 4;
            if (gridTop + 62 < boxTop + boxH - 4) {
                int[] sudNums = {3,7,1,9,2,5,4,6,8};
                Color[] sudColors = {new Color(0,215,230), new Color(240,200,0), new Color(160,0,220)};
                for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
                    int gx = rightX + 12 + j * 20, gy = gridTop + i * 20;
                    g.setColor(new Color(20,20,30)); g.fillRect(gx, gy, 19, 19);
                    g.setColor(new Color(40,40,55)); g.drawRect(gx, gy, 19, 19);
                    g.setFont(new Font("Arial", Font.BOLD, 11));
                    g.setColor(sudColors[j]);
                    FontMetrics fm = g.getFontMetrics();
                    String n = String.valueOf(sudNums[i*3+j]);
                    g.drawString(n, gx+(19-fm.stringWidth(n))/2, gy+14);
                }
            }

            // ── Bottom note ──────────────────────────────────────
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            drawCenteredText(g, "Place pieces strategically \u2014 Tetris skill AND Sudoku thinking matter!",
                    W/2, boxTop + boxH + 14, new Color(90, 95, 140));
        }

        // Page 1: Controls
        void drawPage1(Graphics2D g) {
            int W = getWidth();
            g.setFont(new Font("Arial Black", Font.BOLD, 24));
            drawShadowText(g, "CONTROLS", W/2, 40, new Color(100, 200, 255), 2);
            g.setColor(new Color(35, 35, 55)); g.fillRect(40, 50, W - 80, 1);

            String[][] controls = {
                {"\u2190  \u2192",     "Move piece left / right"},
                {"\u2191  or  Z",      "Rotate clockwise / counter-clockwise"},
                {"\u2193",             "Soft drop  (+1 point per row)"},
                {"Space",              "Hard drop instantly  (+2 pts per row)"},
                {"Q  /  E",            "Cycle digit order on active piece"},
                {"F",                  "Time Freeze \u2014 stops auto-drop (8s)"},
                {"S",                  "Slow-Mo \u2014 halves fall speed (15s)"},
                {"B",                  "Clear Bomb \u2014 blasts bottom 3 rows"},
                {"\u2013  /  =",       "Decrease / Increase speed manually"},
                {"M",                  "Mute / unmute music"},
                {"P",                  "Pause game"},
                {"R",                  "Restart from scratch"},
            };

            Color[] catColors = {
                new Color(0,215,230), new Color(0,215,230), new Color(0,215,230), new Color(0,215,230),
                new Color(240,200,0),
                new Color(100,200,255), new Color(255,175,40), new Color(220,80,200),
                new Color(80,160,80),
                new Color(160,200,255), new Color(160,160,200), new Color(220,80,80)
            };

            int startY = 58, rowH = 36;
            for (int i = 0; i < controls.length; i++) {
                int rowY = startY + i * rowH;
                g.setColor(i % 2 == 0 ? new Color(15,15,22) : new Color(11,11,17));
                g.fillRect(24, rowY, W - 48, rowH - 2);
                g.setColor(catColors[i]); g.fillRect(24, rowY, 4, rowH - 2);
                // Key badge
                g.setColor(new Color(20,20,30)); g.fillRoundRect(34, rowY+4, 118, 24, 6, 6);
                g.setColor(catColors[i]); g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(34, rowY+4, 118, 24, 6, 6); g.setStroke(new BasicStroke(1f));
                g.setFont(new Font("Courier New", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(controls[i][0], 34+(118-fm.stringWidth(controls[i][0]))/2, rowY+21);
                // Description
                g.setFont(new Font("Arial", Font.PLAIN, 12)); g.setColor(new Color(170,175,215));
                g.drawString(controls[i][1], 162, rowY+21);
            }

            int tipY = startY + controls.length * rowH + 4;
            if (tipY + 30 < getHeight() - 20) {
                g.setColor(new Color(15,22,15)); g.fillRoundRect(24, tipY, W-48, 28, 8, 8);
                g.setColor(new Color(40,120,40)); g.setStroke(new BasicStroke(1.2f));
                g.drawRoundRect(24, tipY, W-48, 28, 8, 8); g.setStroke(new BasicStroke(1f));
                g.setFont(new Font("Arial", Font.BOLD, 11)); g.setColor(new Color(80,220,100));
                g.drawString("\u2b50  Pro tip: Q/E before placing \u2014 put the right digit in the right cell!", 38, tipY+18);
            }
        }

        // Page 2: Scoring
        void drawPage2(Graphics2D g) {
            int W = getWidth();
            g.setFont(new Font("Arial Black", Font.BOLD, 24));
            drawShadowText(g, "SCORING", W/2, 44, new Color(255, 210, 55), 2);
            g.setColor(new Color(35, 35, 55)); g.fillRect(50, 54, W - 100, 1);

            Object[][] scores = {
                {"Soft drop",         "+1 per row",              new Color(160,165,200), "Move piece down one row manually"},
                {"Hard drop",         "+2 × rows fallen",        new Color(0,215,230),   "Instantly drop — double the reward"},
                {"Unique digit clear","40 × Level per cell",     new Color(80,210,80),   "Clear cells where each digit appears once only"},
                {"3×3 Box complete",  "+1,000 × Level",          new Color(255,210,55),  "Fill any 3×3 zone with all 9 unique digits"},
                {"Repeated digit",    "0 pts — stays on board",  new Color(220,70,70),   "Duplicate digits in a row/col stay & block you"},
            };

            int sy = 68, rowH = 68, badgeW = 180;
            for (Object[] row : scores) {
                g.setColor(new Color(16,16,24)); g.fillRoundRect(28, sy, W-56, rowH-6, 8, 8);
                g.setColor(new Color(35,35,50)); g.drawRoundRect(28, sy, W-56, rowH-6, 8, 8);
                g.setColor((Color)row[2]); g.fillOval(44, sy+20, 12, 12);
                g.setFont(new Font("Arial Black", Font.BOLD, 13)); g.setColor(Color.WHITE);
                g.drawString((String)row[0], 64, sy+19);
                g.setFont(new Font("Arial", Font.PLAIN, 11)); g.setColor(new Color(120,125,165));
                g.drawString((String)row[3], 64, sy+35);
                Color rc = (Color)row[2];
                g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 38));
                g.fillRoundRect(W-28-badgeW, sy+14, badgeW, 28, 6, 6);
                g.setColor((Color)row[2]); g.setStroke(new BasicStroke(1.2f));
                g.drawRoundRect(W-28-badgeW, sy+14, badgeW, 28, 6, 6); g.setStroke(new BasicStroke(1f));
                g.setFont(new Font("Arial Black", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                String val = (String)row[1];
                g.drawString(val, W-28-badgeW+(badgeW-fm.stringWidth(val))/2, sy+33);
                sy += rowH;
            }
        }

        // Page 3: Level progression
        void drawPage3(Graphics2D g) {
            int W = getWidth();
            g.setFont(new Font("Arial Black", Font.BOLD, 24));
            drawShadowText(g, "LEVELS & PROGRESSION", W/2, 44, new Color(80,200,80), 2);
            g.setColor(new Color(35, 35, 55)); g.fillRect(50, 54, W - 100, 1);

            int lx = 28, ly = 68, lw = W-56, lh = 24;
            g.setFont(new Font("Arial", Font.BOLD, 10)); g.setColor(new Color(100,105,150));
            g.drawString("SPEED INCREASES EVERY 60 SECONDS — LEVEL 1 TO 10", lx, ly-4);

            for (int lv = 1; lv <= 10; lv++) {
                float t = (lv-1)/9f;
                Color c = new Color(Math.min(255,(int)(t*225+30)),Math.min(255,(int)((1f-t)*200+55)),22);
                int segW = (lw-18)/10, sx = lx + (lv-1)*(segW+2);
                g.setColor(c); g.fillRect(sx, ly, segW, lh);
                g.setColor(Color.BLACK); g.fillRect(sx, ly, segW, 3);
                g.setFont(new Font("Arial", Font.BOLD, 9)); g.setColor(Color.BLACK);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(String.valueOf(lv), sx+(segW-fm.stringWidth(String.valueOf(lv)))/2, ly+16);
            }

            int tableY = ly + lh + 14;
            g.setColor(new Color(35,35,55)); g.fillRect(lx, tableY, lw, 1); tableY += 6;

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
                int ry = tableY + i*28;
                g.setColor(i%2==0?new Color(14,14,20):new Color(10,10,15));
                g.fillRect(lx, ry, lw, 26);
                g.setColor(rowColors[i]); g.fillRect(lx, ry, 4, 26);
                g.setFont(new Font("Arial Black", Font.BOLD, 11)); g.setColor(rowColors[i]);
                g.drawString(table[i][0], lx+12, ry+17);
                g.setFont(new Font("Courier New", Font.PLAIN, 11)); g.setColor(new Color(160,165,210));
                g.drawString(table[i][1], lx+95, ry+17);
                g.setFont(new Font("Arial", Font.PLAIN, 11)); g.setColor(new Color(130,135,175));
                g.drawString(table[i][2], lx+210, ry+17);
                g.setFont(new Font("Arial", Font.ITALIC, 11)); g.setColor(new Color(90,95,135));
                g.drawString(table[i][3], lx+360, ry+17);
            }

            int bottomY = tableY + table.length*28 + 10;
            g.setColor(new Color(12,22,12)); g.fillRoundRect(lx, bottomY, lw, 40, 10, 10);
            g.setColor(new Color(40,160,50)); g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(lx, bottomY, lw, 40, 10, 10); g.setStroke(new BasicStroke(1f));
            g.setFont(new Font("Arial Black", Font.BOLD, 13)); g.setColor(new Color(80,220,80));
            String ready = "You're ready! Press  ▶  PLAY NOW!  to start.";
            FontMetrics fm2 = g.getFontMetrics();
            g.drawString(ready, lx+(lw-fm2.stringWidth(ready))/2, bottomY+24);
        }

        // Page 4: Power-ups
        void drawPage4(Graphics2D g) {
            int W = getWidth();
            g.setFont(new Font("Arial Black", Font.BOLD, 24));
            drawShadowText(g, "POWER-UPS", W/2, 40, new Color(180, 100, 255), 2);
            g.setColor(new Color(35, 35, 55)); g.fillRect(40, 50, W - 80, 1);

            g.setFont(new Font("Arial", Font.ITALIC, 12));
            drawCenteredText(g, "Earn charges by playing well \u2014 activate them with a single key press!", W/2, 68, new Color(130, 100, 200));

            Object[][] pups = {
                // {name, key, accentR,G,B, earnDesc, effectDesc, earnIcon}
                new Object[]{"Time Freeze", "F", 100, 200, 255,
                    "Earned every 4 lines cleared  (up to 3 charges)",
                    "Stops the piece from auto-dropping for 8 seconds. The board gets an icy blue glow and a countdown bar appears at the top. Use it when you need time to plan your next move.",
                    "\u2744"},
                new Object[]{"Slow-Mo", "S", 255, 175, 40,
                    "Earned every 8 pieces locked  (up to 3 charges)",
                    "Halves the fall speed for 15 seconds. Works at any level \u2014 even level 10 becomes manageable. An amber glow and countdown bar show how long remains.",
                    "\u25d0"},
                new Object[]{"Clear Bomb", "B", 220, 80, 200,
                    "Earned every time you complete a 3\u00d73 box  (up to 3 charges)",
                    "Instantly clears all filled cells in the bottom 3 rows with a flash and particle burst, then gravity pulls everything down. Perfect for breaking a deadlock when the board is getting full.",
                    "\u25ce"},
            };

            int sy = 80;
            for (Object[] pu : pups) {
                String name = (String)pu[0], key = (String)pu[1];
                int cr=(int)pu[2], cg=(int)pu[3], cb=(int)pu[4];
                String earnDesc=(String)pu[5], effectDesc=(String)pu[6], icon=(String)pu[7];
                Color accent = new Color(cr,cg,cb);

                int cardH = 118;
                // Card background
                g.setColor(new Color(14,14,22)); g.fillRoundRect(24, sy, W-48, cardH, 10, 10);
                g.setColor(new Color(cr/4+10, cg/4+10, cb/4+10)); g.fillRoundRect(24, sy, W-48, cardH, 10, 10);
                g.setColor(accent); g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(24, sy, W-48, cardH, 10, 10); g.setStroke(new BasicStroke(1f));
                // Left accent bar
                g.setColor(accent); g.fillRoundRect(24, sy, 5, cardH, 4, 4);

                // Custom drawn logo instead of unicode
                drawPowerUpLogo(g, 36, sy+10, 38, (int)pu[2], (int)pu[3], (int)pu[4], (String)pu[1]);

                // Key badge
                g.setColor(new Color(20,20,34)); g.fillRoundRect(36, sy+54, 38, 20, 5, 5);
                g.setColor(accent); g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(36, sy+54, 38, 20, 5, 5); g.setStroke(new BasicStroke(1f));
                g.setFont(new Font("Courier New", Font.BOLD, 13)); g.setColor(accent);
                FontMetrics kfm = g.getFontMetrics();
                g.drawString(key, 36+(38-kfm.stringWidth(key))/2, sy+68);

                // Name
                g.setFont(new Font("Arial Black", Font.BOLD, 15)); g.setColor(Color.WHITE);
                g.drawString(name, 84, sy+26);

                // Earn description
                g.setFont(new Font("Arial", Font.BOLD, 10)); g.setColor(accent);
                g.drawString(earnDesc, 84, sy+42);

                // Effect description (word-wrapped to fit)
                g.setFont(new Font("Arial", Font.PLAIN, 11)); g.setColor(new Color(160,162,205));
                drawWrappedText(g, effectDesc, 84, sy+58, W-48-84-16, 15);

                sy += cardH + 10;
            }
        }

        // Wrap text within maxWidth pixels, drawing lines top-down from (x,y)
        void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int lineH) {
            FontMetrics fm = g.getFontMetrics();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int curY = y;
            for (String word : words) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(test) > maxWidth && line.length() > 0) {
                    g.drawString(line.toString(), x, curY);
                    curY += lineH;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (line.length() > 0) g.drawString(line.toString(), x, curY);
        }

        // Draw a custom power-up logo inside a circle of given size at (ox,oy)
        void drawPowerUpLogo(Graphics2D g, int ox, int oy, int size, int cr, int cg, int cb, String key) {
            Color accent = new Color(cr, cg, cb);
            Color dim    = new Color(cr/5+8, cg/5+8, cb/5+8);
            int cx = ox + size/2, cy = oy + size/2, r = size/2;

            // Circle background
            g.setColor(dim); g.fillOval(ox, oy, size, size);
            g.setColor(accent); g.setStroke(new BasicStroke(2f));
            g.drawOval(ox, oy, size, size); g.setStroke(new BasicStroke(1f));

            Composite old = g.getComposite();
            g.setClip(new java.awt.geom.Ellipse2D.Float(ox+1, oy+1, size-2, size-2));

            if (key.equals("F")) {
                // ── Snowflake (Time Freeze) ───────────────────────
                g.setColor(accent); g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // 6 arms
                for (int arm = 0; arm < 6; arm++) {
                    double angle = arm * Math.PI / 3;
                    int ex = cx + (int)(Math.cos(angle) * (r-5));
                    int ey = cy + (int)(Math.sin(angle) * (r-5));
                    g.drawLine(cx, cy, ex, ey);
                    // Two side branches on each arm
                    for (int b = 1; b >= -1; b -= 2) {
                        double ba = angle + b * Math.PI / 6;
                        int mx = cx + (int)(Math.cos(angle) * (r-11));
                        int my = cy + (int)(Math.sin(angle) * (r-11));
                        int bex = mx + (int)(Math.cos(ba) * 5);
                        int bey = my + (int)(Math.sin(ba) * 5);
                        g.drawLine(mx, my, bex, bey);
                    }
                }
                // Centre dot
                g.setColor(new Color(220, 240, 255));
                g.fillOval(cx-3, cy-3, 6, 6);
                g.setStroke(new BasicStroke(1f));

            } else if (key.equals("S")) {
                // ── Hourglass (Slow-Mo) ──────────────────────────
                g.setColor(new Color(cr, cg, cb, 90));
                // Top half fill (sand)
                int[] topX = {cx-9, cx+9, cx};
                int[] topY = {cy-11, cy-11, cy};
                g.fillPolygon(topX, topY, 3);
                // Bottom half fill (sand at bottom)
                int[] botX = {cx-9, cx+9, cx};
                int[] botY = {cy+11, cy+11, cy};
                g.fillPolygon(botX, botY, 3);
                // Hourglass outline
                g.setColor(accent); g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int[] hgx = {cx-10, cx+10, cx+2, cx+10, cx-10, cx-2, cx-10};
                int[] hgy = {cy-12, cy-12, cy,   cy+12,  cy+12, cy,   cy-12};
                g.drawPolyline(hgx, hgy, 7);
                // Top and bottom bars
                g.drawLine(cx-10, cy-12, cx+10, cy-12);
                g.drawLine(cx-10, cy+12, cx+10, cy+12);
                // Tiny falling sand dot
                g.setColor(new Color(255, 220, 100));
                g.fillOval(cx-2, cy-3, 4, 4);
                g.setStroke(new BasicStroke(1f));

                // Speed arrows on side to reinforce "slow"
                g.setColor(new Color(cr, cg, cb, 130));
                g.setStroke(new BasicStroke(1.2f));
                // Left slow-down chevron
                g.drawLine(cx-16, cy-3, cx-12, cy);
                g.drawLine(cx-16, cy+3, cx-12, cy);
                // Right slow-down chevron
                g.drawLine(cx+12, cy-3, cx+16, cy);
                g.drawLine(cx+12, cy+3, cx+16, cy);
                g.setStroke(new BasicStroke(1f));

            } else if (key.equals("B")) {
                // ── Bomb with explosion sparks (Clear Bomb) ───────
                // Fuse wire
                g.setColor(new Color(200, 160, 80));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(cx-2, cy-r+2, 10, 10, 0, 200);
                // Fuse spark
                g.setColor(new Color(255, 230, 50));
                g.fillOval(cx+5, cy-r+2, 5, 5);
                g.setStroke(new BasicStroke(1f));
                // Bomb body
                int br = r - 10;
                g.setColor(new Color(30, 25, 35)); g.fillOval(cx-br, cy-br+3, br*2, br*2);
                g.setColor(new Color(cr, cg, cb, 80)); g.fillOval(cx-br+1, cy-br+4, br*2-2, br*2-2);
                g.setColor(accent); g.setStroke(new BasicStroke(2f));
                g.drawOval(cx-br, cy-br+3, br*2, br*2); g.setStroke(new BasicStroke(1f));
                // Shine highlight
                g.setColor(new Color(255,255,255,60));
                g.fillOval(cx-br+3, cy-br+5, br/2, br/3);
                // 6 explosion sparks radiating outward
                g.setColor(new Color(255, 200, 60));
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int sp = 0; sp < 6; sp++) {
                    double sa = sp * Math.PI / 3 + Math.PI/6;
                    int sx2 = cx + (int)(Math.cos(sa) * (br+2));
                    int sy2 = cy + 3 + (int)(Math.sin(sa) * (br+2));
                    int ex2 = cx + (int)(Math.cos(sa) * (r-2));
                    int ey2 = cy + 3 + (int)(Math.sin(sa) * (r-2));
                    g.drawLine(sx2, sy2, ex2, ey2);
                }
                g.setStroke(new BasicStroke(1f));
            }

            g.setClip(null);
            g.setComposite(old);
        }

        // ── Helpers ──────────────────────────────────────────────
        void drawBox(Graphics2D g, int x, int y, int w, int h, Color accent, String title) {
            g.setColor(new Color(16,16,24)); g.fillRoundRect(x,y,w,h,10,10);
            g.setColor(accent); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(x,y,w,h,10,10);
            g.setStroke(new BasicStroke(1f));
            g.setColor(accent); g.fillRoundRect(x,y,w,22,10,10);
            g.setColor(new Color(8,8,12)); g.fillRect(x,y+11,w,11);
            g.setFont(new Font("Arial Black", Font.BOLD, 11)); g.setColor(new Color(8,8,12));
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
                g.setColor(clr.brighter()); g.fillRect(x+c[1]*sz, y+c[0]*sz, sz-1, 2);
                g.setColor(clr); g.fillRect(x+c[1]*sz+2, y+c[0]*sz+2, sz-5, sz-5);
            }
        }

        void drawShadowText(Graphics2D g, String text, int cx, int y, Color clr, int shadow) {
            FontMetrics fm = g.getFontMetrics();
            int tx = cx - fm.stringWidth(text)/2;
            g.setColor(new Color(0,0,0,100)); g.drawString(text, tx+shadow, y+shadow);
            g.setColor(clr); g.drawString(text, tx, y);
        }

        void drawCenteredText(Graphics2D g, String text, int cx, int y, Color clr) {
            FontMetrics fm = g.getFontMetrics();
            g.setColor(clr); g.drawString(text, cx - fm.stringWidth(text)/2, y);
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
        int side = Math.max(220, Math.min(300, usableW / 5));
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
    boolean gameOver = false, paused = false, muted = false;
    boolean showGhost = true;  // settings: ghost piece on/off

    // ── Pause menu state ─────────────────────────────────────────
    static final int PAUSE_RESUME = 0, PAUSE_RESTART = 1, PAUSE_SETTINGS = 2, PAUSE_MAINMENU = 3;
    int pauseSel = 0;          // currently highlighted menu item
    boolean inSettings = false; // true = showing settings sub-panel
    int settingsSel = 0;        // highlighted setting row (0=mute,1=ghost,2=speed)

    // ── Power-up state ───────────────────────────────────────────
    // Time Freeze  (key: F) — stops auto-drop timer for 8 seconds
    boolean freezeActive = false;
    long freezeEndMs = 0;
    int freezeCharges = 0;
    static final long FREEZE_DURATION_MS = 8_000L;

    // Slow-Mo  (key: S) — halves fall speed for 15 seconds
    boolean slowMoActive = false;
    long slowMoEndMs = 0;
    int slowMoCharges = 0;
    static final long SLOWMO_DURATION_MS = 15_000L;

    // Clear Bomb  (key: B) — clears all cells in the bottom 3 rows
    int bombCharges = 0;

    // Charges earned counters
    int linesForFreeze = 0;   // every 4 lines cleared → +1 freeze
    int piecesForSlowMo = 0;  // every 8 pieces locked → +1 slow-mo
    int boxesForBomb = 0;     // every 3×3 box cleared → +1 bomb

    // Power-up announce banner
    String puAnnounce = "";
    long puAnnounceMs = 0;
    static final long PU_ANNOUNCE_DUR = 2000L;

    // Level progression: time-based (60 seconds per level)
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
        int base = Math.max(80, 700 - (level - 1) * 68);
        return slowMoActive ? Math.min(900, base * 2) : base;
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
        // Reset power-ups
        freezeActive = false; freezeCharges = 0; linesForFreeze = 0;
        slowMoActive = false; slowMoCharges = 0; piecesForSlowMo = 0;
        bombCharges = 0; boxesForBomb = 0;
        puAnnounce = ""; puAnnounceMs = 0;
        levelStartMs = System.currentTimeMillis();
        cur = makePiece(); nxt = makePiece();
        gameTmr.setDelay(tickMs()); gameTmr.start();
        levelTmr.start();
        if (!muted) SoundEngine.startBGM();
    }

    void checkLevelTimer() {
        if (gameOver || paused) return;
        tickPowerUps();
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
        // ── Game over screen ──────────────────────────────────────
        if (gameOver) {
            if (k == KeyEvent.VK_R) newGame();
            if (k == KeyEvent.VK_ESCAPE) goToMainMenu();
            return;
        }

        // ── Pause key — toggle pause ──────────────────────────────
        if (k == KeyEvent.VK_P || (paused && !inSettings && k == KeyEvent.VK_ESCAPE)) {
            if (!paused) {
                paused = true; pauseSel = 0; inSettings = false;
                gameTmr.stop(); levelTmr.stop(); SoundEngine.stopBGM();
            } else if (!inSettings) {
                resumeGame();
            }
            repaint(); return;
        }

        // ── Settings sub-panel navigation ─────────────────────────
        if (paused && inSettings) {
            if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_BACK_SPACE) {
                inSettings = false; repaint(); return;
            }
            if (k == KeyEvent.VK_UP)   { settingsSel = (settingsSel + 2) % 3; repaint(); return; }
            if (k == KeyEvent.VK_DOWN) { settingsSel = (settingsSel + 1) % 3; repaint(); return; }
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
                applySettingAction(); repaint(); return;
            }
            if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_MINUS) {
                if (settingsSel == 2) { adjustLevel(-1); repaint(); } return;
            }
            if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_EQUALS) {
                if (settingsSel == 2) { adjustLevel( 1); repaint(); } return;
            }
            return;
        }

        // ── Pause menu navigation ─────────────────────────────────
        if (paused) {
            if (k == KeyEvent.VK_UP)   { pauseSel = (pauseSel + 3) % 4; repaint(); return; }
            if (k == KeyEvent.VK_DOWN) { pauseSel = (pauseSel + 1) % 4; repaint(); return; }
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
                applyPauseMenuAction(); repaint(); return;
            }
            return;
        }

        // ── Active gameplay ───────────────────────────────────────
        if (k == KeyEvent.VK_M) {
            muted = !muted;
            if (muted) SoundEngine.stopBGM(); else SoundEngine.startBGM();
            repaint(); return;
        }
        if (flashTick > 0) return;
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
            case KeyEvent.VK_F:      activateFreeze();  break;
            case KeyEvent.VK_S:      activateSlowMo();  break;
            case KeyEvent.VK_B:      activateBomb();    break;
        }
        repaint();
    }

    void resumeGame() {
        paused = false; inSettings = false;
        gameTmr.start(); levelTmr.start();
        if (!muted) SoundEngine.startBGM();
        levelStartMs = System.currentTimeMillis() - (level-1)*LEVEL_DURATION_MS;
    }

    void applyPauseMenuAction() {
        switch (pauseSel) {
            case PAUSE_RESUME:   resumeGame(); break;
            case PAUSE_RESTART:  paused = false; inSettings = false; newGame(); break;
            case PAUSE_SETTINGS: inSettings = true; settingsSel = 0; break;
            case PAUSE_MAINMENU: goToMainMenu(); break;
        }
    }

    void applySettingAction() {
        switch (settingsSel) {
            case 0: // Mute
                muted = !muted;
                if (muted) SoundEngine.stopBGM(); else SoundEngine.startBGM();
                break;
            case 1: // Ghost piece
                showGhost = !showGhost;
                break;
            case 2: // Speed — handled by left/right, enter does nothing extra
                break;
        }
    }

    void goToMainMenu() {
        gameTmr.stop(); levelTmr.stop(); SoundEngine.stopBGM();
        // Replace the game window with the main menu
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) frame.dispose();
        SwingUtilities.invokeLater(() -> {
            MainMenuDialog menu = new MainMenuDialog();
            menu.setVisible(true);
        });
    }

    void adjustLevel(int d) {
        level = Math.max(1, Math.min(10, level+d));
        gameTmr.setDelay(tickMs());
        levelStartMs = System.currentTimeMillis() - (level-1)*LEVEL_DURATION_MS;
    }

    // ── Power-up activations ─────────────────────────────────────
    void activateFreeze() {
        if (freezeCharges <= 0) return;
        freezeCharges--;
        freezeActive = true;
        freezeEndMs = System.currentTimeMillis() + FREEZE_DURATION_MS;
        gameTmr.stop();  // stop the auto-drop
        showPuAnnounce("TIME FREEZE! +" + FREEZE_DURATION_MS/1000 + "s");
        SoundEngine.play("powerup_freeze");
    }

    void activateSlowMo() {
        if (slowMoCharges <= 0) return;
        slowMoCharges--;
        slowMoActive = true;
        slowMoEndMs = System.currentTimeMillis() + SLOWMO_DURATION_MS;
        gameTmr.setDelay(tickMs());
        showPuAnnounce("SLOW-MO! +" + SLOWMO_DURATION_MS/1000 + "s");
        SoundEngine.play("powerup_slow");
    }

    void activateBomb() {
        if (bombCharges <= 0) return;
        bombCharges--;
        // Collect all filled cells in the bottom 3 rows for a flash-then-clear
        for (int r = ROWS-3; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] != 0) {
                    flashCells.add(new int[]{r,c});
                    pendingClear.add(new int[]{r,c});
                }
            }
        }
        if (!flashCells.isEmpty()) flashTick = FLASH_DUR;
        showPuAnnounce("CLEAR BOMB! Bottom 3 rows blasted!");
        SoundEngine.play("powerup_bomb");
    }

    void showPuAnnounce(String msg) { puAnnounce = msg; puAnnounceMs = System.currentTimeMillis(); }

    // Called each second by levelTmr to also tick power-up timers
    void tickPowerUps() {
        long now = System.currentTimeMillis();
        if (freezeActive && now >= freezeEndMs) {
            freezeActive = false;
            if (!gameOver && !paused) gameTmr.start(); // resume drops
        }
        if (slowMoActive && now >= slowMoEndMs) {
            slowMoActive = false;
            gameTmr.setDelay(tickMs()); // restore normal speed
        }
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
            if(r<0){triggerGameOver(); return;}
            board[r][c]=cur.nums[i]; boardColor[r][c]=cur.type+1;
            lockCells.add(new int[]{r,c});
            Color bc=PIECE_CLR[cur.type];
            shineItems.add(new long[]{bx(c),by(r),System.currentTimeMillis(),bc.getRed(),bc.getGreen(),bc.getBlue()});
        }
        lockTick=LOCK_DUR;
        for(int[]lc:lockCells) spawnParticles(lc[0],lc[1],cur.type,4,2.4f);
        SoundEngine.play("lock");
        // Slow-mo: every 8 pieces locked
        piecesForSlowMo++;
        if (piecesForSlowMo >= 8) { piecesForSlowMo = 0; slowMoCharges = Math.min(3, slowMoCharges+1); showPuAnnounce("SLOW-MO ready!  Press S"); }
        markConflicts(); checkRows(); checkBoxes();
        if(flashCells.isEmpty()) spawnNext();
    }

    void triggerGameOver() {
        if (score > TetriDoku.highScore) TetriDoku.highScore = score;
        gameOver=true; gameTmr.stop(); levelTmr.stop();
        SoundEngine.stopBGM(); SoundEngine.play("gameover");
    }

    void spawnNext() {
        cur=nxt; nxt=makePiece();
        if(!fits(cur.cells(),cur.r,cur.c)) triggerGameOver();
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
            // Freeze: every 4 lines cleared
            linesForFreeze++;
            if (linesForFreeze >= 4) { linesForFreeze = 0; freezeCharges = Math.min(3, freezeCharges+1); showPuAnnounce("TIME FREEZE ready!  Press F"); }
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
                // Bomb: every box completed
                boxesForBomb++;
                if (boxesForBomb >= 1) { boxesForBomb = 0; bombCharges = Math.min(3, bombCharges+1); showPuAnnounce("CLEAR BOMB ready!  Press B"); }
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
        if(!gameOver){if(showGhost)drawGhost(g2); drawActivePiece(g2);}
        drawLockEffect(g2); drawParticles(g2); drawPopups(g2);
        drawPowerUpOverlays(g2);
        drawSidebar(g2);
        if(levelUpFlash) drawLevelUpOverlay(g2);
        if(gameOver) drawGameOverOverlay(g2);
        if(paused)   drawPauseMenu(g2);
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

    // ── Power-up visual overlays ─────────────────────────────────
    void drawPowerUpOverlays(Graphics2D g) {
        long now = System.currentTimeMillis();
        Composite old = g.getComposite();

        // ── Freeze: icy blue tint + edge frost ──
        if (freezeActive) {
            long remaining = freezeEndMs - now;
            float pulse = (float)(Math.sin(tick * 0.12) * 0.06 + 0.10);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
            g.setColor(new Color(80, 180, 255));
            g.fillRect(PAD, PAD, BOARD_W, BOARD_H);
            g.setComposite(old);
            // Border glow
            float glow = (float)(Math.sin(tick * 0.18) * 0.4 + 0.6);
            g.setColor(new Color(120, 210, 255, (int)(glow * 200)));
            g.setStroke(new BasicStroke(3.5f));
            g.drawRect(PAD, PAD, BOARD_W, BOARD_H);
            g.setStroke(new BasicStroke(1f));
            // Timer bar at top of board
            float frac = (float) remaining / FREEZE_DURATION_MS;
            g.setColor(new Color(20, 20, 40));
            g.fillRect(PAD, PAD, BOARD_W, 5);
            g.setColor(new Color(100, 200, 255));
            g.fillRect(PAD, PAD, (int)(frac * BOARD_W), 5);
            // Label
            g.setFont(new Font("Arial Black", Font.BOLD, 11));
            String ftxt = "FREEZE  " + (remaining/1000+1) + "s";
            FontMetrics fm = g.getFontMetrics();
            g.setColor(new Color(0,0,0,120));
            g.drawString(ftxt, PAD + BOARD_W/2 - fm.stringWidth(ftxt)/2 + 1, PAD + 18 + 1);
            g.setColor(new Color(140, 220, 255));
            g.drawString(ftxt, PAD + BOARD_W/2 - fm.stringWidth(ftxt)/2, PAD + 18);
        }

        // ── Slow-Mo: warm amber ripple tint ──
        if (slowMoActive) {
            long remaining = slowMoEndMs - now;
            float pulse = (float)(Math.sin(tick * 0.09) * 0.04 + 0.07);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
            g.setColor(new Color(255, 160, 30));
            g.fillRect(PAD, PAD, BOARD_W, BOARD_H);
            g.setComposite(old);
            // Border
            float glow = (float)(Math.sin(tick * 0.14) * 0.3 + 0.7);
            g.setColor(new Color(255, 180, 50, (int)(glow * 180)));
            g.setStroke(new BasicStroke(3f));
            g.drawRect(PAD, PAD, BOARD_W, BOARD_H);
            g.setStroke(new BasicStroke(1f));
            // Timer bar at top
            float frac = (float) remaining / SLOWMO_DURATION_MS;
            g.setColor(new Color(30, 20, 10));
            g.fillRect(PAD, PAD, BOARD_W, 5);
            g.setColor(new Color(255, 170, 40));
            g.fillRect(PAD, PAD, (int)(frac * BOARD_W), 5);
            // Label
            g.setFont(new Font("Arial Black", Font.BOLD, 11));
            String stxt = "SLOW-MO  " + (remaining/1000+1) + "s";
            FontMetrics fm = g.getFontMetrics();
            g.setColor(new Color(0,0,0,120));
            g.drawString(stxt, PAD + BOARD_W/2 - fm.stringWidth(stxt)/2 + 1, PAD + 18 + 1);
            g.setColor(new Color(255, 200, 80));
            g.drawString(stxt, PAD + BOARD_W/2 - fm.stringWidth(stxt)/2, PAD + 18);
        }

        // ── Power-up announce banner ──
        if (!puAnnounce.isEmpty()) {
            long age = now - puAnnounceMs;
            if (age < PU_ANNOUNCE_DUR) {
                float fade = age < 300 ? age / 300f : 1f - (float)(age - 300) / (PU_ANNOUNCE_DUR - 300);
                int alpha = (int)(fade * 240);
                int bw = BOARD_W - 40, bh = 36;
                int bx2 = PAD + BOARD_W/2 - bw/2, by2 = PAD + BOARD_H - 60;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade * 0.88f));
                g.setColor(new Color(8, 8, 20));
                g.fillRoundRect(bx2, by2, bw, bh, 10, 10);
                g.setComposite(old);
                g.setColor(new Color(100, 220, 100, alpha));
                g.setStroke(new BasicStroke(1.8f));
                g.drawRoundRect(bx2, by2, bw, bh, 10, 10);
                g.setStroke(new BasicStroke(1f));
                g.setFont(new Font("Arial Black", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(new Color(130, 255, 130, alpha));
                g.drawString(puAnnounce, bx2 + (bw - fm.stringWidth(puAnnounce))/2, by2 + 23);
            } else {
                puAnnounce = "";
            }
        }
        g.setComposite(old);
    }

    // ── Sidebar ──────────────────────────────────────────────────
    void drawSidebar(Graphics2D g){
        int sx=PAD+BOARD_W+PAD, cw=SIDE_W-16, cx=sx+8;
        g.setColor(new Color(10,10,14)); g.fillRect(sx,0,W-sx,H);
        g.setColor(new Color(36,36,52)); g.fillRect(sx,0,2,H);

        int y=10;

        // ── Title card ──
        drawCard(g,sx+4,y,cw,48);
        g.setFont(new Font("Arial Black",Font.BOLD,18)); g.setColor(new Color(200,210,255));
        FontMetrics fm=g.getFontMetrics();
        String title="TetriDoku";
        g.drawString(title, cx, y+22);
        g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(55,55,90));
        g.drawString("Tetris  \u00d7  Sudoku", cx, y+38);
        // Mute icon aligned to right of card
        String muteIcon = muted ? "🔇" : "🔊";
        g.setFont(new Font("Arial",Font.PLAIN,14));
        g.setColor(muted ? new Color(100,40,40) : new Color(60,110,60));
        g.drawString(muteIcon, sx+cw-14, y+22);
        y += 56;

        // ── Stats card ──
        drawCard(g,sx+4,y,cw,102);
        y=drawStat(g,cx,y+4,"SCORE",score,new Color(255,210,55));
        y=drawStat(g,cx,y,"LEVEL",level,speedColor(level));
        y=drawStat(g,cx,y,"LINES",lines,new Color(160,175,220));
        y+=8;

        // ── High Score ──
        if (TetriDoku.highScore > 0) {
            drawCard(g,sx+4,y,cw,24);
            g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(180,150,30));
            g.drawString("BEST", cx, y+10);
            g.setFont(new Font("Arial Black",Font.BOLD,9)); g.setColor(new Color(255,210,50));
            g.drawString(String.valueOf(TetriDoku.highScore), cx+32, y+10);
            y+=30;
        }

        // ── Level progress bar ──
        drawCard(g,sx+4,y,cw,50);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(85,90,140));
        int secs = level < 10 ? secondsToNextLevel() : 0;
        String nextLvLabel = level < 10 ? "NEXT LEVEL  " + secs + "s" : "MAX LEVEL!";
        g.drawString(nextLvLabel, cx, y+12);
        int barW=cw-16, barH=9, barX=cx, barY=y+18;
        g.setColor(new Color(20,20,30)); g.fillRoundRect(barX,barY,barW,barH,4,4);
        if(level<10){
            int fw=(int)(levelProgress()*barW);
            if(fw>0){ g.setColor(speedColor(level)); g.fillRoundRect(barX,barY,fw,barH,4,4); }
        } else {
            g.setColor(new Color(255,180,0)); g.fillRoundRect(barX,barY,barW,barH,4,4);
        }
        g.setColor(new Color(38,38,58)); g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(barX,barY,barW,barH,4,4);
        y+=56;

        // ── Speed bar ──
        drawCard(g,sx+4,y,cw,60);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(85,90,140));
        g.drawString("GAME SPEED", cx, y+12);
        int segTot=10, segH=10, segW=(cw-16)/segTot;
        int segY=y+18;
        for(int i=1;i<=segTot;i++){
            g.setColor(i<=level?speedColor(i):new Color(22,22,32));
            g.fillRect(cx+(i-1)*(segW+2),segY,segW,segH);
            if(i<=level){g.setColor(new Color(255,255,255,45));g.fillRect(cx+(i-1)*(segW+2)+1,segY+1,segW-2,3);}
        }
        // Level number and speed hints
        g.setFont(new Font("Arial Black",Font.BOLD,20)); g.setColor(speedColor(level));
        g.drawString(String.valueOf(level), cx, segY+segH+18);
        g.setFont(new Font("Arial",Font.PLAIN,8)); g.setColor(new Color(65,70,115));
        g.drawString("[\u2013] Slower", cx+26, segY+segH+8);
        g.drawString("[=] Faster",  cx+26, segY+segH+18);
        y+=66;

        // ── Next piece ──
        drawCard(g,sx+4,y,cw,72);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(85,90,140));
        g.drawString("NEXT PIECE", cx, y+12);
        if(nxt!=null) drawMini(g,nxt,cx,y+14);
        y+=78;

        // ── Q/E hint ──
        drawCard(g,sx+4,y,cw,36);
        g.setFont(new Font("Arial",Font.BOLD,10)); g.setColor(new Color(55,200,80));
        g.drawString("Q / E  \u2014  Shift Numbers", cx, y+13);
        g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(25,110,45));
        g.drawString("Rearrange digits on piece", cx, y+27);
        y+=42;

        // ── Power-ups panel ──
        long now = System.currentTimeMillis();
        drawCard(g,sx+4,y,cw,100);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(75,80,135));
        g.drawString("POWER-UPS", cx, y+12); y+=16;

        Object[][] puRows = {
            new Object[]{"Time Freeze","F", freezeCharges, freezeActive ? (freezeEndMs-now) : 0L, FREEZE_DURATION_MS, freezeActive, 100,200,255},
            new Object[]{"Slow-Mo",    "S", slowMoCharges, slowMoActive ? (slowMoEndMs-now) : 0L, SLOWMO_DURATION_MS, slowMoActive, 255,175,40},
            new Object[]{"Clear Bomb", "B", bombCharges,   0L,                                    1L,                false,         220,80,200},
        };
        for (Object[] pu : puRows) {
            String puName = (String)pu[0];
            String puKey  = (String)pu[1];
            int charges   = (int)pu[2];
            long remMs    = (long)pu[3];
            long totMs    = (long)pu[4];
            boolean active= (boolean)pu[5];
            Color puClr   = new Color((int)pu[6],(int)pu[7],(int)pu[8]);

            int rowY = y;
            int rowH = active ? 20 : 17;

            // Active glow behind row
            if (active) {
                float gp2 = (float)(Math.sin(tick*0.2)*0.3+0.7);
                Composite oldc = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f*gp2));
                g.setColor(puClr); g.fillRoundRect(cx-2, rowY-1, cw-6, rowH+2, 4, 4);
                g.setComposite(oldc);
                if (totMs > 0 && remMs > 0) {
                    float frac = Math.min(1f, (float)remMs / totMs);
                    g.setColor(new Color(20,20,30)); g.fillRect(cx-2, rowY+15, cw-6, 3);
                    g.setColor(puClr); g.fillRect(cx-2, rowY+15, (int)(frac*(cw-6)), 3);
                }
            }

            // Name (left side, leaves room for key + pips on right)
            g.setFont(new Font("Arial",Font.BOLD,10));
            g.setColor(active ? puClr : (charges>0 ? new Color(180,185,220) : new Color(70,72,110)));
            g.drawString(puName, cx, rowY+11);

            // Key badge — anchored 48px from right edge of card
            int badgeRight = sx + cw - 4;       // right edge of card (inside border)
            int pipsTotalW = 3*8 + 2*3;         // 3 pips × 8px + 2 gaps × 3px = 30px
            int keyX = badgeRight - pipsTotalW - 6 - 18; // key badge 18px wide
            g.setColor(charges>0||active ? new Color(30,30,48) : new Color(18,18,26));
            g.fillRoundRect(keyX, rowY+1, 16, 11, 3, 3);
            g.setColor(charges>0||active ? new Color(60,60,100) : new Color(35,35,55));
            g.drawRoundRect(keyX, rowY+1, 16, 11, 3, 3);
            g.setFont(new Font("Courier New",Font.BOLD,8));
            FontMetrics kfm2 = g.getFontMetrics();
            g.setColor(charges>0||active ? new Color(200,170,40) : new Color(60,60,80));
            g.drawString(puKey, keyX+(16-kfm2.stringWidth(puKey))/2, rowY+10);

            // Charge pips — right-aligned inside card
            int pipsX = badgeRight - pipsTotalW;
            for (int pip=0; pip<3; pip++) {
                boolean filled = pip < charges;
                int px2 = pipsX + pip*11, py2 = rowY+2;
                g.setColor(filled ? puClr : new Color(28,28,44));
                g.fillRoundRect(px2, py2, 8, 8, 2, 2);
                if (!filled) { g.setColor(new Color(50,50,75)); g.drawRoundRect(px2,py2,8,8,2,2); }
            }
            y += rowH + 2;
        }
        y += 8;

        // ── Conflict indicator ──
        boolean hasConflict=false;
        for(boolean[]row:conflict) for(boolean b:row) if(b){hasConflict=true;break;}
        if(hasConflict){
            float p=(float)(Math.sin(tick*0.15)*0.35+0.65);
            drawCard(g,sx+4,y,cw,24,new Color((int)(p*120),6,6));
            g.setFont(new Font("Arial",Font.BOLD,10)); g.setColor(new Color(255,60,60,(int)(p*255)));
            g.drawString("\u26a0  CONFLICT DETECTED", cx, y+16);
            y+=30;
        } else { y+=4; }

        // ── Controls ──
        drawCard(g,sx+4,y,cw,174);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(75,80,135));
        g.drawString("CONTROLS", cx, y+12);
        y+=14;
        String[][]ctrl={
            {"\u2190\u2192","Move left/right"},
            {"\u2191 / Z","Rotate"},
            {"\u2193","Soft drop"},
            {"Space","Hard drop"},
            {"Q/E","Shift digits"},
            {"F","Time Freeze"},
            {"S","Slow-Mo"},
            {"B","Clear Bomb"},
            {"\u2013/=","Speed"},
            {"M","Mute music"},
            {"P","Pause"},
            {"R","Restart"}
        };
        for(String[]row:ctrl){
            // Key badge
            g.setFont(new Font("Courier New",Font.BOLD,9));
            FontMetrics kfm=g.getFontMetrics();
            int kw=kfm.stringWidth(row[0])+8, kh=13;
            g.setColor(new Color(22,22,34)); g.fillRoundRect(cx,y,kw,kh,3,3);
            g.setColor(new Color(55,55,85)); g.drawRoundRect(cx,y,kw,kh,3,3);
            g.setColor(new Color(210,175,45)); g.drawString(row[0], cx+4, y+kh-2);
            // Description
            g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(55,58,100));
            g.drawString(row[1], cx+kw+6, y+kh-2);
            y+=14;
        }
        y+=4;

        // ── Scoring quick-ref ──
        int scoringH = 74;
        if (y + scoringH < H - 4) {
            drawCard(g,sx+4,y,cw,scoringH);
            g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(75,80,135));
            g.drawString("SCORING", cx, y+12); y+=14;
            String[]sl={"Unique clear","3\u00d73 box","Repeated \u2192"};
            String[]sv={"40\u00d7Lvl","1000\u00d7Lvl","0 pts"};
            Color[]sc={new Color(175,175,50),new Color(255,210,0),new Color(190,55,55)};
            for(int i=0;i<sl.length;i++){
                g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(55,58,100));
                g.drawString(sl[i], cx, y);
                g.setFont(new Font("Arial Black",Font.BOLD,9)); g.setColor(sc[i]);
                FontMetrics sfm=g.getFontMetrics(); int sw=sfm.stringWidth(sv[i]);
                g.drawString(sv[i], sx+cw-sw, y);
                y+=14;
            }
        }
    }

    void drawCard(Graphics2D g,int x,int y,int w,int h){ drawCard(g,x,y,w,h,new Color(17,17,24)); }
    void drawCard(Graphics2D g,int x,int y,int w,int h,Color bg){
        g.setColor(bg); g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(new Color(34,34,52)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(x,y,w,h,8,8);
    }

    int drawStat(Graphics2D g,int cx,int y,String label,int val,Color clr){
        g.setFont(new Font("Arial",Font.BOLD,8)); g.setColor(new Color(56,60,105)); g.drawString(label,cx,y+10);
        g.setFont(new Font("Arial Black",Font.BOLD,19)); g.setColor(Color.BLACK); g.drawString(String.valueOf(val),cx+1,y+27);
        g.setColor(clr); g.drawString(String.valueOf(val),cx,y+26); return y+30;
    }

    void drawMini(Graphics2D g,Piece p,int sx,int sy){
        int MC=20; int[][]cells=p.cells(); int minR=99,minC=99;
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
            g.setColor(Color.WHITE);  g.drawString(s,x+(MC-fm.stringWidth(s))/2,  y+(MC+fm.getAscent()-fm.getDescent())/2-1);
        }
    }

    Color speedColor(int lv){
        float t=(lv-1)/9f;
        return new Color(Math.min(255,(int)(t*225+30)),Math.min(255,(int)((1f-t)*200+55)),22);
    }

    // ── Rich pause menu ───────────────────────────────────────────
    void drawPauseMenu(Graphics2D g) {
        // Darkened board backdrop
        g.setColor(new Color(0,0,0,200)); g.fillRect(PAD,PAD,BOARD_W,BOARD_H);

        int cx = PAD + BOARD_W/2;
        int cardW = Math.min(BOARD_W - 40, 320);
        int cardX = cx - cardW/2;

        if (inSettings) {
            drawSettingsPanel(g, cx, cardX, cardW);
        } else {
            drawPauseMenuPanel(g, cx, cardX, cardW);
        }
    }

    void drawPauseMenuPanel(Graphics2D g, int cx, int cardX, int cardW) {
        // ── Card ──────────────────────────────────────────────────
        int cardH = 250, cardY = PAD + BOARD_H/2 - cardH/2;
        g.setColor(new Color(10, 10, 18));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
        g.setColor(new Color(90,100,220)); g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);
        g.setStroke(new BasicStroke(1f));

        // Top accent bar
        g.setColor(new Color(90,100,220));
        g.fillRoundRect(cardX, cardY, cardW, 4, 16, 16);
        g.fillRect(cardX, cardY+4, cardW, 4);  // fill the rounded top corners

        // ── Title ─────────────────────────────────────────────────
        g.setFont(new Font("Arial Black", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(80,80,120,60));
        g.drawString("PAUSED", cx - fm.stringWidth("PAUSED")/2 + 2, cardY + 44 + 2);
        g.setColor(new Color(140,150,255));
        g.drawString("PAUSED", cx - fm.stringWidth("PAUSED")/2, cardY + 44);

        // Score snapshot
        g.setFont(new Font("Arial", Font.BOLD, 11)); g.setColor(new Color(100,105,160));
        String snap = "Score " + score + "  •  Level " + level + "  •  Lines " + lines;
        fm = g.getFontMetrics();
        g.drawString(snap, cx - fm.stringWidth(snap)/2, cardY + 62);

        // Divider
        g.setColor(new Color(35,35,60)); g.fillRect(cardX+20, cardY+70, cardW-40, 1);

        // ── Menu items ────────────────────────────────────────────
        String[] labels = {"\u25b6  Resume",  "\u21ba  Restart", "\u2699  Settings", "\u2302  Main Menu"};
        Color[]  colors = {new Color(80,220,110), new Color(255,200,60), new Color(120,180,255), new Color(220,100,100)};
        int itemH = 40, itemY = cardY + 80;

        for (int i = 0; i < labels.length; i++) {
            boolean sel = (i == pauseSel);
            int iy = itemY + i * itemH;

            // Highlight background
            if (sel) {
                g.setColor(new Color(colors[i].getRed()/5, colors[i].getGreen()/5, colors[i].getBlue()/5, 180));
                g.fillRoundRect(cardX+12, iy+3, cardW-24, itemH-6, 8, 8);
                g.setColor(colors[i]); g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(cardX+12, iy+3, cardW-24, itemH-6, 8, 8);
                g.setStroke(new BasicStroke(1f));
                // Selection indicator triangle
                g.setColor(colors[i]);
                int[] tx = {cardX+16, cardX+22, cardX+16};
                int[] ty = {iy+itemH/2-5, iy+itemH/2, iy+itemH/2+5};
                g.fillPolygon(tx, ty, 3);
            }

            g.setFont(new Font("Arial Black", Font.BOLD, sel ? 14 : 13));
            fm = g.getFontMetrics();
            g.setColor(sel ? colors[i] : new Color(130,132,170));
            g.drawString(labels[i], cx - fm.stringWidth(labels[i])/2, iy + itemH/2 + fm.getAscent()/2 - 2);
        }

        // ── Footer hint ───────────────────────────────────────────
        g.setFont(new Font("Arial", Font.PLAIN, 10)); g.setColor(new Color(55,57,90));
        String hint = "\u2191\u2193 navigate    Enter select    P / Esc resume";
        fm = g.getFontMetrics();
        g.drawString(hint, cx - fm.stringWidth(hint)/2, cardY + cardH - 10);
    }

    void drawSettingsPanel(Graphics2D g, int cx, int cardX, int cardW) {
        int cardH = 230, cardY = PAD + BOARD_H/2 - cardH/2;

        g.setColor(new Color(8, 10, 18));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
        g.setColor(new Color(120,160,255)); g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);
        g.setStroke(new BasicStroke(1f));

        g.setColor(new Color(120,160,255));
        g.fillRoundRect(cardX, cardY, cardW, 4, 16, 16);
        g.fillRect(cardX, cardY+4, cardW, 4);

        // Title
        g.setFont(new Font("Arial Black", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(140,180,255));
        g.drawString("\u2699  SETTINGS", cx - fm.stringWidth("\u2699  SETTINGS")/2, cardY + 38);

        g.setColor(new Color(35,35,60)); g.fillRect(cardX+20, cardY+48, cardW-40, 1);

        // ── Setting rows ──────────────────────────────────────────
        // Row 0: Mute music
        // Row 1: Ghost piece
        // Row 2: Game speed
        String[] rowLabels = {"Music", "Ghost piece", "Game speed"};
        int rowH = 48, rowY = cardY + 58;

        for (int i = 0; i < 3; i++) {
            boolean sel = (i == settingsSel);
            int ry = rowY + i * rowH;

            if (sel) {
                g.setColor(new Color(20,30,55,200));
                g.fillRoundRect(cardX+10, ry+2, cardW-20, rowH-4, 8, 8);
                g.setColor(new Color(100,150,255)); g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(cardX+10, ry+2, cardW-20, rowH-4, 8, 8);
                g.setStroke(new BasicStroke(1f));
            }

            // Row label
            g.setFont(new Font("Arial Black", Font.BOLD, 12));
            g.setColor(sel ? new Color(180,210,255) : new Color(100,105,155));
            g.drawString(rowLabels[i], cardX+24, ry+20);

            // Row value / control
            if (i == 0) {
                // Mute toggle — pill button
                drawToggle(g, cardX + cardW - 80, ry+8, !muted, new Color(80,220,100));
                g.setFont(new Font("Arial", Font.PLAIN, 10)); g.setColor(new Color(80,85,130));
                g.drawString("Enter to toggle", cardX+24, ry+36);
            } else if (i == 1) {
                // Ghost piece toggle
                drawToggle(g, cardX + cardW - 80, ry+8, showGhost, new Color(80,180,255));
                g.setFont(new Font("Arial", Font.PLAIN, 10)); g.setColor(new Color(80,85,130));
                g.drawString("Enter to toggle", cardX+24, ry+36);
            } else {
                // Speed control — segmented bar + level number
                int segCount = 10, segW2 = (cardW - 90) / segCount;
                int barX = cardX + 24;
                for (int s = 1; s <= segCount; s++) {
                    float t = (s-1)/9f;
                    Color sc = s <= level
                        ? new Color(Math.min(255,(int)(t*225+30)), Math.min(255,(int)((1-t)*200+55)), 22)
                        : new Color(22,22,34);
                    g.setColor(sc);
                    g.fillRect(barX + (s-1)*(segW2+2), ry+10, segW2, 10);
                }
                g.setFont(new Font("Arial Black", Font.BOLD, 18));
                g.setColor(speedColor(level));
                g.drawString(String.valueOf(level), cardX+cardW-60, ry+24);
                g.setFont(new Font("Arial", Font.PLAIN, 10)); g.setColor(new Color(80,85,130));
                g.drawString("\u2190\u2192 adjust speed", cardX+24, ry+36);
            }
        }

        // Back hint
        g.setFont(new Font("Arial", Font.PLAIN, 10)); g.setColor(new Color(55,57,90));
        String hint = "Esc / Backspace \u2014 back to pause menu";
        fm = g.getFontMetrics();
        g.drawString(hint, cx - fm.stringWidth(hint)/2, cardY + cardH - 10);
    }

    // Draw an on/off toggle pill
    void drawToggle(Graphics2D g, int x, int y, boolean on, Color onColor) {
        int tw = 54, th = 24;
        Color bg = on ? new Color(onColor.getRed()/4, onColor.getGreen()/4, onColor.getBlue()/4) : new Color(20,20,30);
        g.setColor(bg); g.fillRoundRect(x, y, tw, th, th, th);
        g.setColor(on ? onColor : new Color(60,62,90)); g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, tw, th, th, th); g.setStroke(new BasicStroke(1f));
        // Knob
        int knobX = on ? x + tw - th + 3 : x + 3;
        g.setColor(on ? onColor : new Color(70,72,100));
        g.fillOval(knobX, y+3, th-6, th-6);
        // Label
        g.setFont(new Font("Arial Black", Font.BOLD, 9));
        g.setColor(on ? onColor : new Color(60,62,90));
        g.drawString(on ? "ON" : "OFF", on ? x+6 : x+26, y+16);
    }

    void drawOverlay(Graphics2D g,String title,String sub,Color clr){
        g.setColor(new Color(0,0,0,190)); g.fillRect(PAD,PAD,BOARD_W,BOARD_H);
        int cx=PAD+BOARD_W/2,cy=PAD+BOARD_H/2;
        g.setFont(new Font("Arial Black",Font.BOLD,36)); FontMetrics fm=g.getFontMetrics();
        g.setColor(new Color(clr.getRed(),clr.getGreen(),clr.getBlue(),45));
        g.drawString(title,cx-fm.stringWidth(title)/2+2,cy-16+2);
        g.setColor(clr); g.drawString(title,cx-fm.stringWidth(title)/2,cy-16);
        g.setFont(new Font("Arial",Font.PLAIN,13)); fm=g.getFontMetrics();
        g.setColor(new Color(190,190,215)); g.drawString(sub,cx-fm.stringWidth(sub)/2,cy+18);
    }

    void drawGameOverOverlay(Graphics2D g){
        g.setColor(new Color(0,0,0,210)); g.fillRect(PAD,PAD,BOARD_W,BOARD_H);
        int cx=PAD+BOARD_W/2, cy=PAD+BOARD_H/2;

        int cardW=BOARD_W-40, cardH=180, cardX=cx-cardW/2, cardY=cy-cardH/2;
        g.setColor(new Color(14,8,10));
        g.fillRoundRect(cardX, cardY, cardW, cardH, 14, 14);
        g.setColor(new Color(180,30,30)); g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 14, 14);
        g.setStroke(new BasicStroke(1f));
        // Top accent
        g.setColor(new Color(180,30,30));
        g.fillRoundRect(cardX, cardY, cardW, 4, 14, 14);
        g.fillRect(cardX, cardY+4, cardW, 4);

        g.setFont(new Font("Arial Black",Font.BOLD,34));
        FontMetrics fm=g.getFontMetrics();
        String titleStr="GAME OVER";
        g.setColor(new Color(180,30,30,60)); g.drawString(titleStr,cx-fm.stringWidth(titleStr)/2+2,cardY+48+2);
        g.setColor(new Color(220,55,55)); g.drawString(titleStr,cx-fm.stringWidth(titleStr)/2,cardY+48);

        g.setFont(new Font("Arial Black",Font.BOLD,18)); fm=g.getFontMetrics();
        String scStr="Score  "+score;
        g.setColor(new Color(255,215,50));
        g.drawString(scStr, cx-fm.stringWidth(scStr)/2, cardY+80);

        boolean isNew = score > 0 && score == TetriDoku.highScore;
        g.setFont(new Font("Arial",Font.BOLD,12)); fm=g.getFontMetrics();
        String hsStr = isNew ? "\u2b50  New Best Score!" : "Best  " + TetriDoku.highScore;
        g.setColor(isNew ? new Color(255,200,0) : new Color(100,100,140));
        g.drawString(hsStr, cx-fm.stringWidth(hsStr)/2, cardY+102);

        g.setFont(new Font("Arial",Font.PLAIN,11)); fm=g.getFontMetrics();
        String lvStr="Reached Level "+level+"  \u2022  Lines Cleared: "+lines;
        g.setColor(new Color(90,95,140));
        g.drawString(lvStr, cx-fm.stringWidth(lvStr)/2, cardY+124);

        // Two action buttons — R to restart, Esc for menu
        int btnY = cardY + 142, btnH = 26, btnGap = 10;
        int btnW = (cardW - 48 - btnGap) / 2;
        // Restart button
        g.setColor(new Color(15,35,15));
        g.fillRoundRect(cardX+20, btnY, btnW, btnH, 7, 7);
        g.setColor(new Color(80,200,100)); g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(cardX+20, btnY, btnW, btnH, 7, 7); g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Arial Black",Font.BOLD,11)); fm=g.getFontMetrics();
        String rs="R  \u2014  Play Again";
        g.drawString(rs, cardX+20+(btnW-fm.stringWidth(rs))/2, btnY+18);
        // Main menu button
        int btn2X = cardX+20+btnW+btnGap;
        g.setColor(new Color(30,15,15));
        g.fillRoundRect(btn2X, btnY, btnW, btnH, 7, 7);
        g.setColor(new Color(200,80,80)); g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(btn2X, btnY, btnW, btnH, 7, 7); g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Arial Black",Font.BOLD,11)); fm=g.getFontMetrics();
        String mm="Esc  \u2014  Main Menu"; g.setColor(new Color(200,80,80));
        g.drawString(mm, btn2X+(btnW-fm.stringWidth(mm))/2, btnY+18);
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
    static volatile boolean menuBgmRunning=false;
    static Thread bgmThread=null;
    static Thread menuBgmThread=null;
    static long bgmSample=0;
    static long menuBgmSample=0;

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
                cache.put("powerup_freeze", freezeSnd());
                cache.put("powerup_slow",   slowSnd());
                cache.put("powerup_bomb",   bombSnd());
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

    static void startMenuBGM(){
        if(menuBgmRunning)return; menuBgmRunning=true; menuBgmSample=0;
        menuBgmThread=new Thread(()->{
            try{AudioFormat fmt=new AudioFormat(SR,16,1,true,false);
                SourceDataLine l=AudioSystem.getSourceDataLine(fmt);
                l.open(fmt,4096);l.start();
                while(menuBgmRunning)l.write(menuBgmFrame(),0,1024);
                l.drain();l.close();
            }catch(Exception ignored){}
        },"menu-bgm");
        menuBgmThread.setDaemon(true); menuBgmThread.start();
    }

    static void stopBGM(){ bgmRunning=false; menuBgmRunning=false; }

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

    // Menu BGM — same melody, 110 BPM, no drums, lush pad layers, gentle
    static final int MBPM=110, MBEAT=SR*60/MBPM, MBAR=MBEAT*4;
    static final float[]MMEL={329.6f,261.6f,293.7f,329.6f,349.2f,329.6f,261.6f,246.9f,
                               261.6f,293.7f,329.6f,392f,  349.2f,329.6f,293.7f,261.6f};

    static byte[] menuBgmFrame(){
        int n=512; byte[]buf=new byte[n*2]; int pat=MBEAT*16;
        for(int i=0;i<n;i++){
            long s=menuBgmSample+i; int pos=(int)(s%pat),ni=pos/MBEAT,pin=pos%MBEAT;
            float nt=(float)pin/MBEAT;
            // Main melody — soft triangle wave
            float mf=MMEL[ni%MMEL.length],mEnv=melEnv(nt);
            float mPh=(float)(s*mf/SR)%1f;
            float mel=(mPh<0.5f?mPh*4f-1f:3f-mPh*4f)*mEnv*0.11f;
            // Octave up layer — very soft
            float mPh2=(float)(s*mf*2/SR)%1f;
            mel+=(mPh2<0.5f?mPh2*4f-1f:3f-mPh2*4f)*mEnv*0.04f;
            // Slow pad — same note, sine wave, very slow attack
            float padEnv=(float)(0.5-0.5*Math.cos(Math.PI*Math.min(1.0,nt*3)));
            float padPh=(float)(s*mf*0.5/SR)%1f;
            float pad=(float)Math.sin(padPh*2*Math.PI)*padEnv*0.07f;
            // Gentle bass
            float bf=MMEL[ni%MMEL.length]*0.25f,bPh=(float)(s*bf/SR)%1f;
            float bass=(bPh*2f-1f)*bassEnv(nt)*0.06f;
            // Soft hi-freq shimmer
            float shim=0f; int sp=pin%(MBEAT/4);
            if(sp<MBEAT/16){float st=(float)sp/(MBEAT/16);
                shim=(float)Math.sin(2*Math.PI*3200f*s/SR)*(float)Math.exp(-12f*st)*0.012f;}
            float mix=Math.max(-0.88f,Math.min(0.88f,mel+pad+bass+shim));
            short v=(short)(mix*32767); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        menuBgmSample+=n; return buf;
    }

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

    // ── Power-up sounds ──────────────────────────────────────────

    // Freeze: high crystalline descending shimmer — two pure tones sweep down
    static byte[] freezeSnd() {
        int n = SR * 320 / 1000; byte[] buf = new byte[n * 2]; double ph1=0, ph2=0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float env = (float)Math.exp(-3.2f * t) * (1f - (float)Math.exp(-40f * t));
            float f1 = 1800f - 900f * t;   // sweep 1800→900
            float f2 = 2400f - 1200f * t;  // sweep 2400→1200 (harmony)
            ph1 += 2 * Math.PI * f1 / SR;
            ph2 += 2 * Math.PI * f2 / SR;
            // Add subtle shimmer noise
            long seed = (long)i * 1664525L + 1013904223L;
            float noise = ((seed >>> 16 & 0xFFFF) / 32767.5f - 1f) * 0.04f;
            float s = ((float)Math.sin(ph1) * 0.38f + (float)Math.sin(ph2) * 0.22f + noise) * env;
            short v = clamp(s); buf[i*2] = (byte)(v&0xFF); buf[i*2+1] = (byte)((v>>8)&0xFF);
        }
        return buf;
    }

    // Slow-Mo: warm lazy downward pitch bend — thick low sweep, like time stretching
    static byte[] slowSnd() {
        int n = SR * 380 / 1000; byte[] buf = new byte[n * 2]; double ph=0, ph2=0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float env = (float)Math.exp(-2.8f * t) * (1f - (float)Math.exp(-25f * t));
            float freq = 520f - 280f * t;         // slow descend
            float freq2 = freq * 1.498f;           // perfect fifth above
            ph  += 2 * Math.PI * freq  / SR;
            ph2 += 2 * Math.PI * freq2 / SR;
            // Vibrato wobble to feel "slow"
            float vib = (float)Math.sin(2 * Math.PI * 5.5f * t) * 0.015f;
            float s = ((float)Math.sin(ph + vib) * 0.35f + (float)Math.sin(ph2 + vib) * 0.15f
                     + (float)Math.sin(ph * 2 + vib) * 0.08f) * env;
            short v = clamp(s); buf[i*2] = (byte)(v&0xFF); buf[i*2+1] = (byte)((v>>8)&0xFF);
        }
        return buf;
    }

    // Bomb: percussive impact boom + upward tail — low thud then rising whoosh
    static byte[] bombSnd() {
        int n = SR * 420 / 1000; byte[] buf = new byte[n * 2];
        // Kick-style low thud (first 120ms)
        int thudN = SR * 120 / 1000; double thudPh = 0;
        for (int i = 0; i < thudN; i++) {
            float t = i / (float) thudN;
            float freq = 160f * (float)Math.exp(-12f * t) + 40f;
            thudPh += 2 * Math.PI * freq / SR;
            float env = (float)Math.exp(-8f * t);
            long seed = (long)i * 6364136223846793005L + 1;
            float noise = ((seed >>> 33) / (float)(1L<<31) - 1f) * 0.12f * (float)Math.exp(-20f*t);
            float s = ((float)Math.sin(thudPh) * 0.50f + noise) * env;
            short v = clamp(s); buf[i*2] = (byte)(v&0xFF); buf[i*2+1] = (byte)((v>>8)&0xFF);
        }
        // Rising whoosh tail (overlaps from 60ms, lasts rest of clip)
        int whooshStart = SR * 60 / 1000; double wPh = 0;
        for (int i = whooshStart; i < n; i++) {
            float t = (i - whooshStart) / (float)(n - whooshStart);
            float freq = 180f + 620f * t;
            wPh += 2 * Math.PI * freq / SR;
            float env = t < 0.15f ? t/0.15f : (float)Math.exp(-5f * (t - 0.15f));
            long seed = (long)i * 2862933555777941757L + 3037000499L;
            float noise = ((seed >>> 33) / (float)(1L<<31) - 1f) * 0.18f;
            float s = ((float)Math.sin(wPh) * 0.12f + noise) * env * 0.55f;
            addSample(buf, i, s);
        }
        return buf;
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
