import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

// ================================================================
//  T E T R I D O K U  v4
//  True 9×9 board  —  the whole board IS the Sudoku grid.
//  Premium dark-glass UI.
//
//  CLEARING MECHANIC:
//  Full row → unique-number cells flash & clear.
//  Repeated-number cells stay frozen in place.
//  Perfect 1-9 row → entire row clears, massive bonus.
//  Complete a 3×3 box with 1-9 → instant box clear.
// ================================================================

public class TetriDoku extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TetriDoku().setVisible(true));
    }

    TetriDoku() {
        setTitle("TetriDoku");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setBackground(new Color(6, 6, 18));
        GamePanel gp = new GamePanel();
        add(gp);
        pack();
        setLocationRelativeTo(null);
        gp.requestFocusInWindow();
    }
}

// ================================================================
class GamePanel extends JPanel implements ActionListener {

    // ── Layout ──────────────────────────────────────────────────
    static final int COLS    = 9;
    static final int ROWS    = 9;          // true 9×9 board
    static final int CELL    = 62;         // px per cell
    static final int PAD     = 6;          // board inset
    static final int BOARD_W = COLS * CELL;
    static final int BOARD_H = ROWS * CELL;
    static final int SIDE_W  = 260;
    static final int W       = PAD + BOARD_W + PAD + SIDE_W;
    static final int H       = PAD + BOARD_H + PAD;

    // ── Premium colour palette ───────────────────────────────────
    static final Color C_BG       = new Color(6,   6,  18);
    static final Color C_PANEL    = new Color(14,  14,  34);
    static final Color C_BORDER   = new Color(55,  55, 120);
    static final Color C_BOX_LINE = new Color(80,  80, 170);
    static final Color C_CELL_BG  = new Color(10,  10,  28);
    static final Color C_GOLD     = new Color(255, 210,  80);
    static final Color C_SILVER   = new Color(180, 190, 210);
    static final Color C_DIM      = new Color( 80,  85, 110);

    // Per-digit hues — vivid jewel tones
    static final Color[] NUM_CLR = {
            null,
            new Color(255,  65,  65),   // 1  ruby
            new Color(255, 148,   0),   // 2  amber
            new Color(220, 220,  30),   // 3  gold
            new Color( 45, 220,  80),   // 4  emerald
            new Color(  0, 200, 255),   // 5  sapphire
            new Color(110,  90, 255),   // 6  amethyst
            new Color(200,  55, 255),   // 7  violet
            new Color(255,  80, 165),   // 8  rose
            new Color(100, 255, 190),   // 9  mint
    };

    // ── Tetromino shapes  [type][rot][cell]{dr,dc} ───────────────
    static final int[][][][] SHAPES = {
            // I – horizontal only (fits 9-wide better)
            { {{0,0},{0,1},{0,2},{0,3}}, {{0,0},{1,0},{2,0},{3,0}},
                    {{0,0},{0,1},{0,2},{0,3}}, {{0,0},{1,0},{2,0},{3,0}} },
            // O
            { {{0,0},{0,1},{1,0},{1,1}}, {{0,0},{0,1},{1,0},{1,1}},
                    {{0,0},{0,1},{1,0},{1,1}}, {{0,0},{0,1},{1,0},{1,1}} },
            // T
            { {{0,1},{1,0},{1,1},{1,2}}, {{0,0},{1,0},{1,1},{2,0}},
                    {{0,0},{0,1},{0,2},{1,1}}, {{0,1},{1,0},{1,1},{2,1}} },
            // S
            { {{0,1},{0,2},{1,0},{1,1}}, {{0,0},{1,0},{1,1},{2,1}},
                    {{0,1},{0,2},{1,0},{1,1}}, {{0,0},{1,0},{1,1},{2,1}} },
            // Z
            { {{0,0},{0,1},{1,1},{1,2}}, {{0,1},{1,0},{1,1},{2,0}},
                    {{0,0},{0,1},{1,1},{1,2}}, {{0,1},{1,0},{1,1},{2,0}} },
            // J
            { {{0,0},{1,0},{1,1},{1,2}}, {{0,0},{0,1},{1,0},{2,0}},
                    {{0,0},{0,1},{0,2},{1,2}}, {{0,1},{1,1},{2,0},{2,1}} },
            // L
            { {{0,2},{1,0},{1,1},{1,2}}, {{0,0},{1,0},{2,0},{2,1}},
                    {{0,0},{0,1},{0,2},{1,0}}, {{0,0},{0,1},{1,1},{2,1}} },
    };

    // ── Board ────────────────────────────────────────────────────
    int[][]     board    = new int[ROWS][COLS];
    boolean[][] conflict = new boolean[ROWS][COLS];

    // ── Pieces ───────────────────────────────────────────────────
    Piece cur, nxt;

    // ── Game state ───────────────────────────────────────────────
    int     score, level, lines;
    boolean gameOver, paused;

    // ── Clear animation ──────────────────────────────────────────
    List<int[]> flashCells   = new ArrayList<>();
    List<int[]> pendingClear = new ArrayList<>();
    static final int FLASH_DUR = 14;
    int flashTick = 0;

    // ── Box-clear glow  {r0,c0,timestamp} ────────────────────────
    List<long[]> boxClears = new ArrayList<>();

    // ── Timers ───────────────────────────────────────────────────
    Timer gameTmr, animTmr;
    Random rng = new Random();

    // ── Particles for cleared cells ──────────────────────────────
    List<float[]> particles = new ArrayList<>();  // {x,y,vx,vy,life,r,g,b}

    // ── Pulse counter for glow animations ────────────────────────
    long tick = 0;

    // ════════════════════════════════════════════════════════════
    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(C_BG);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKey(e.getKeyCode()); }
        });
        gameTmr = new Timer(tickMs(), this);
        animTmr = new Timer(33, e -> { tick++; tickAnim(); repaint(); });
        animTmr.start();
        newGame();
    }

    int tickMs() { return Math.max(120, 750 - (level - 1) * 65); }

    void newGame() {
        board    = new int[ROWS][COLS];
        conflict = new boolean[ROWS][COLS];
        score = 0; level = 1; lines = 0;
        gameOver = paused = false;
        flashCells.clear(); pendingClear.clear(); flashTick = 0;
        boxClears.clear(); particles.clear();
        cur = makePiece(); nxt = makePiece();
        gameTmr.setDelay(tickMs());
        gameTmr.start();
    }

    Piece makePiece() {
        int t = rng.nextInt(7);
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 9; i++) pool.add(i);
        Collections.shuffle(pool, rng);
        int[] nums = { pool.get(0), pool.get(1), pool.get(2), pool.get(3) };
        return new Piece(t, 0, 0, (COLS - 4) / 2, nums);
    }

    // ── Input ────────────────────────────────────────────────────
    void onKey(int k) {
        if (gameOver) { if (k == KeyEvent.VK_R) newGame(); return; }
        if (k == KeyEvent.VK_P) {
            paused = !paused;
            if (paused) gameTmr.stop(); else gameTmr.start();
            repaint(); return;
        }
        if (paused || flashTick > 0) return;
        switch (k) {
            case KeyEvent.VK_LEFT:  shiftPiece(0,-1); break;
            case KeyEvent.VK_RIGHT: shiftPiece(0, 1); break;
            case KeyEvent.VK_DOWN:  softDrop();        break;
            case KeyEvent.VK_UP:    rotate(1);         break;
            case KeyEvent.VK_Z:     rotate(-1);        break;
            case KeyEvent.VK_SPACE: hardDrop();        break;
            case KeyEvent.VK_Q:     cycleNums(-1);     break;
            case KeyEvent.VK_E:     cycleNums( 1);     break;
        }
        repaint();
    }

    void shiftPiece(int dr, int dc) {
        if (fits(cur.cells(), cur.r+dr, cur.c+dc)) { cur.r+=dr; cur.c+=dc; }
    }

    void rotate(int d) {
        int nr = (cur.rot + d + 4) % 4;
        Piece p = new Piece(cur.type, nr, cur.r, cur.c, cur.nums.clone());
        if      (fits(p.cells(), p.r,   p.c  )) cur = p;
        else if (fits(p.cells(), p.r,   p.c-1)) { p.c--; cur = p; }
        else if (fits(p.cells(), p.r,   p.c+1)) { p.c++; cur = p; }
        else if (fits(p.cells(), p.r-1, p.c  )) { p.r--; cur = p; }
    }

    void cycleNums(int d) {
        int[] n = cur.nums;
        if (d>0){int t=n[3];n[3]=n[2];n[2]=n[1];n[1]=n[0];n[0]=t;}
        else    {int t=n[0];n[0]=n[1];n[1]=n[2];n[2]=n[3];n[3]=t;}
    }

    void softDrop() {
        if (fits(cur.cells(), cur.r+1, cur.c)) { cur.r++; score++; }
        else lock();
    }

    void hardDrop() {
        while (fits(cur.cells(), cur.r+1, cur.c)) { cur.r++; score+=2; }
        lock();
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (!gameOver && !paused && flashTick==0) { softDrop(); repaint(); }
    }

    void tickAnim() {
        if (flashTick > 0) { flashTick--; if (flashTick==0) doClears(); }
        long now = System.currentTimeMillis();
        boxClears.removeIf(bc -> now - bc[2] > 800);
        // update particles
        particles.removeIf(p -> p[4] <= 0);
        for (float[] p : particles) {
            p[0]+=p[2]; p[1]+=p[3]; p[3]+=0.4f; p[4]-=1f;
        }
    }

    // ── Game logic ───────────────────────────────────────────────
    void lock() {
        int[][] cells = cur.cells();
        for (int i = 0; i < 4; i++) {
            int r = cur.r+cells[i][0], c = cur.c+cells[i][1];
            if (r < 0) { gameOver=true; gameTmr.stop(); return; }
            board[r][c] = cur.nums[i];
        }
        markConflicts();
        checkRows();
        checkBoxes();
        if (flashCells.isEmpty()) spawnNext();
    }

    void spawnNext() {
        cur = nxt; nxt = makePiece();
        if (!fits(cur.cells(), cur.r, cur.c)) { gameOver=true; gameTmr.stop(); }
    }

    void checkRows() {
        boolean anyFull = false;
        for (int r = 0; r < ROWS; r++) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) if (board[r][c]==0) { full=false; break; }
            if (!full) continue;
            anyFull = true;
            int[] freq = new int[10];
            for (int c = 0; c < COLS; c++) freq[board[r][c]]++;
            boolean sudokuRow = true;
            int cleared = 0;
            for (int c = 0; c < COLS; c++) {
                int v = board[r][c];
                if (freq[v]==1) {
                    flashCells.add(new int[]{r,c});
                    pendingClear.add(new int[]{r,c});
                    cleared++;
                } else { sudokuRow=false; }
            }
            if (sudokuRow) { score+=500*level; lines++; }
            else            score+=cleared*40*level;
        }
        if (anyFull) { level=Math.max(1,lines/8+1); gameTmr.setDelay(tickMs()); }
        if (!flashCells.isEmpty()) flashTick=FLASH_DUR;
    }

    void doClears() {
        for (int[] cell : pendingClear) {
            int r=cell[0], c=cell[1];
            Color base = NUM_CLR[board[r][c]];
            // spawn particles
            int px = bx(c)+CELL/2, py = by(r)+CELL/2;
            for (int i=0;i<8;i++) {
                float ang = (float)(i*Math.PI/4);
                float spd = 2+rng.nextFloat()*3;
                particles.add(new float[]{px,py,
                        (float)Math.cos(ang)*spd,(float)Math.sin(ang)*spd,
                        18+rng.nextInt(10),base.getRed(),base.getGreen(),base.getBlue()});
            }
            board[r][c]=0;
        }
        pendingClear.clear(); flashCells.clear();
        markConflicts();
        spawnNext();
    }

    void checkBoxes() {
        for (int br=0;br<3;br++) for (int bc=0;bc<3;bc++) {
            int r0=br*3, c0=bc*3;
            Set<Integer> nums=new HashSet<>();
            boolean ok=true;
            outer:
            for (int dr=0;dr<3;dr++) for (int dc=0;dc<3;dc++) {
                int v=board[r0+dr][c0+dc];
                if (v==0||!nums.add(v)){ok=false;break outer;}
            }
            if (ok&&nums.size()==9) {
                score+=1000*level;
                boxClears.add(new long[]{r0,c0,System.currentTimeMillis()});
                for (int dr=0;dr<3;dr++) for (int dc=0;dc<3;dc++) {
                    board[r0+dr][c0+dc]=0; conflict[r0+dr][c0+dc]=false;
                }
            }
        }
    }

    void markConflicts() {
        for (boolean[] row:conflict) Arrays.fill(row,false);
        for (int r=0;r<ROWS;r++) {
            int[] cnt=new int[10];
            for (int c=0;c<COLS;c++) if (board[r][c]>0) cnt[board[r][c]]++;
            for (int c=0;c<COLS;c++) if (board[r][c]>0&&cnt[board[r][c]]>1) conflict[r][c]=true;
        }
        for (int c=0;c<COLS;c++) {
            int[] cnt=new int[10];
            for (int r=0;r<ROWS;r++) if (board[r][c]>0) cnt[board[r][c]]++;
            for (int r=0;r<ROWS;r++) if (board[r][c]>0&&cnt[board[r][c]]>1) conflict[r][c]=true;
        }
    }

    boolean fits(int[][] cells, int r, int c) {
        for (int[] cell:cells) {
            int nr=r+cell[0], nc=c+cell[1];
            if (nr>=ROWS||nc<0||nc>=COLS) return false;
            if (nr>=0&&board[nr][nc]!=0) return false;
        }
        return true;
    }

    // ── Coordinate helpers ───────────────────────────────────────
    int bx(int c) { return PAD + c*CELL; }
    int by(int r) { return PAD + r*CELL; }

    boolean isFlashing(int r, int c) {
        for (int[] fc:flashCells) if (fc[0]==r&&fc[1]==c) return true;
        return false;
    }

    // ════════════════════════════════════════════════════════════
    //  P A I N T
    // ════════════════════════════════════════════════════════════
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        // ── rich dark background gradient ──
        GradientPaint bgGrad = new GradientPaint(0,0,new Color(6,6,20),W,H,new Color(10,8,30));
        g2.setPaint(bgGrad); g2.fillRect(0,0,W,H);

        drawBoard(g2);
        drawBoxGlows(g2);
        drawParticles(g2);
        if (!gameOver) { drawGhost(g2); drawActivePiece(g2); }
        drawSidebar(g2);
        if (gameOver) drawOverlay(g2,"GAME OVER","Press R to restart",new Color(220,55,55));
        if (paused  ) drawOverlay(g2,"PAUSED",   "Press P to continue",new Color(100,110,255));
    }

    // ── Board ────────────────────────────────────────────────────
    void drawBoard(Graphics2D g) {
        // outer board glow
        for (int i=4;i>0;i--) {
            g.setColor(new Color(60,60,160,18*i));
            g.setStroke(new BasicStroke(i*1.5f));
            g.drawRoundRect(PAD-i*2,PAD-i*2,BOARD_W+i*4,BOARD_H+i*4,8,8);
        }

        // cell backgrounds
        for (int r=0;r<ROWS;r++) for (int c=0;c<COLS;c++) {
            int x=bx(c), y=by(r);
            // alternating subtle tint per 3×3 box
            int boxIdx = (r/3)*3+(c/3);
            Color cellBg = (boxIdx%2==0)
                    ? new Color(10,10,30) : new Color(13,12,36);
            g.setColor(cellBg);
            g.fillRect(x,y,CELL,CELL);

            int v=board[r][c];
            if (v>0) {
                if (isFlashing(r,c)) {
                    float a=Math.min(1f,(float)flashTick/(FLASH_DUR*0.6f));
                    // inner glow on flash
                    Color fc=NUM_CLR[v];
                    g.setColor(new Color(fc.getRed(),fc.getGreen(),fc.getBlue(),(int)(a*200)));
                    g.fillRect(x,y,CELL,CELL);
                    g.setColor(new Color(255,255,255,(int)(a*230)));
                    g.fillRoundRect(x+4,y+4,CELL-8,CELL-8,10,10);
                } else {
                    drawCell(g,x,y,v,conflict[r][c]);
                }
            }
        }

        // thin grid lines
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(30,30,65));
        for (int r=0;r<=ROWS;r++) g.drawLine(bx(0),by(r),bx(COLS),by(r));
        for (int c=0;c<=COLS;c++) g.drawLine(bx(c),by(0),bx(c),by(ROWS));

        // bold 3×3 box lines
        g.setStroke(new BasicStroke(2.2f));
        g.setColor(new Color(75,75,165));
        for (int i=0;i<=3;i++) {
            g.drawLine(bx(i*3),by(0),bx(i*3),by(ROWS));
            g.drawLine(bx(0),by(i*3),bx(COLS),by(i*3));
        }

        // board border
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(90,90,200));
        g.drawRect(PAD,PAD,BOARD_W,BOARD_H);
        g.setStroke(new BasicStroke(1f));
    }

    // ── Box-clear gold burst ─────────────────────────────────────
    void drawBoxGlows(Graphics2D g) {
        long now=System.currentTimeMillis();
        for (long[] bc:boxClears) {
            float alpha=Math.max(0f,1f-(now-bc[2])/800f);
            int x=bx((int)bc[1]), y=by((int)bc[0]);
            int bw=3*CELL, bh=3*CELL;
            // outer aura
            for (int i=3;i>0;i--) {
                g.setColor(new Color(1f,0.85f,0.1f,alpha*0.12f*i));
                g.fillRoundRect(x-i*3,y-i*3,bw+i*6,bh+i*6,16,16);
            }
            g.setColor(new Color(1f,0.85f,0.1f,alpha*0.35f));
            g.fillRoundRect(x+2,y+2,bw-4,bh-4,12,12);
            g.setColor(new Color(255,215,60,(int)(alpha*255)));
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(x+2,y+2,bw-4,bh-4,12,12);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // ── Particles ────────────────────────────────────────────────
    void drawParticles(Graphics2D g) {
        for (float[] p:particles) {
            float alpha=p[4]/28f;
            int sz=Math.max(2,(int)(p[4]/4));
            g.setColor(new Color((int)p[5],(int)p[6],(int)p[7],(int)(alpha*220)));
            g.fillOval((int)p[0]-sz/2,(int)p[1]-sz/2,sz,sz);
        }
    }

    // ── Single placed cell ───────────────────────────────────────
    void drawCell(Graphics2D g, int x, int y, int v, boolean hot) {
        Color base=NUM_CLR[v];
        int pad=4, arc=10;
        int iw=CELL-pad*2, ih=CELL-pad*2;

        if (hot) {
            double pulse=Math.sin(tick*0.18)*0.3+0.7;
            // dark conflicted cell
            g.setColor(new Color(30,4,4));
            g.fillRoundRect(x+pad,y+pad,iw,ih,arc,arc);
            // pulsing red outline
            g.setColor(new Color(200,20,20,(int)(pulse*255)));
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(x+pad,y+pad,iw,ih,arc,arc);
            g.setStroke(new BasicStroke(1f));
            // dim number
            g.setColor(new Color(255,80,80,180));
            drawNum(g,x,y,v,16);
        } else {
            // shadow
            g.setColor(new Color(0,0,0,80));
            g.fillRoundRect(x+pad+2,y+pad+3,iw,ih,arc,arc);
            // background gradient tile
            GradientPaint gp=new GradientPaint(
                    x+pad,y+pad, blend(base,0.28f),
                    x+pad,y+pad+ih, blend(base,0.12f));
            g.setPaint(gp);
            g.fillRoundRect(x+pad,y+pad,iw,ih,arc,arc);
            // top highlight sheen
            g.setColor(new Color(255,255,255,45));
            g.fillRoundRect(x+pad+3,y+pad+2,iw-6,(ih/3),arc-2,arc-2);
            // border
            g.setColor(base.brighter().brighter());
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x+pad,y+pad,iw,ih,arc,arc);
            g.setStroke(new BasicStroke(1f));
            // number
            g.setColor(Color.WHITE);
            drawNum(g,x,y,v,19);
        }
    }

    void drawNum(Graphics2D g, int x, int y, int v, int size) {
        g.setFont(new Font("Arial", Font.BOLD, size));
        FontMetrics fm=g.getFontMetrics();
        String s=String.valueOf(v);
        // subtle text shadow
        g.setColor(new Color(0,0,0,100));
        g.drawString(s, x+(CELL-fm.stringWidth(s))/2+1,
                y+(CELL+fm.getAscent()-fm.getDescent())/2);
        // main text
        g.setColor(Color.WHITE);
        g.drawString(s, x+(CELL-fm.stringWidth(s))/2,
                y+(CELL+fm.getAscent()-fm.getDescent())/2-1);
    }

    Color blend(Color c, float bright) {
        return new Color(
                Math.min(255,(int)(c.getRed()*bright+12)),
                Math.min(255,(int)(c.getGreen()*bright+12)),
                Math.min(255,(int)(c.getBlue()*bright+12)));
    }

    // ── Active piece ─────────────────────────────────────────────
    void drawActivePiece(Graphics2D g) {
        if (cur==null) return;
        int[][] cells=cur.cells();
        for (int i=0;i<4;i++) {
            int r=cur.r+cells[i][0], c=cur.c+cells[i][1];
            if (r<0) continue;
            drawCell(g,bx(c),by(r),cur.nums[i],false);
            // extra glow ring on active piece
            Color base=NUM_CLR[cur.nums[i]];
            double glow=Math.sin(tick*0.15)*0.15+0.65;
            g.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),(int)(glow*90)));
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(bx(c)+2,by(r)+2,CELL-4,CELL-4,12,12);
            g.setStroke(new BasicStroke(1f));
        }
    }

    // ── Ghost ────────────────────────────────────────────────────
    void drawGhost(Graphics2D g) {
        if (cur==null) return;
        int gr=cur.r;
        while (fits(cur.cells(),gr+1,cur.c)) gr++;
        if (gr==cur.r) return;
        int[][] cells=cur.cells();
        float[] dash={5f,4f};
        for (int i=0;i<4;i++) {
            int r=gr+cells[i][0], c=cur.c+cells[i][1];
            if (r<0) continue;
            int x=bx(c), y=by(r);
            Color base=NUM_CLR[cur.nums[i]];
            g.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),22));
            g.fillRoundRect(x+4,y+4,CELL-8,CELL-8,10,10);
            g.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),80));
            g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,4,dash,0));
            g.drawRoundRect(x+4,y+4,CELL-8,CELL-8,10,10);
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),75));
            drawNum(g,x,y,cur.nums[i],17);
        }
    }

    // ── Sidebar ──────────────────────────────────────────────────
    void drawSidebar(Graphics2D g) {
        int sx = PAD + BOARD_W + PAD;  // sidebar left edge (x pixel)
        int sw = SIDE_W - PAD;         // usable sidebar width

        // sidebar background
        GradientPaint sideGrad=new GradientPaint(sx,0,new Color(12,12,32),W,H,new Color(8,8,22));
        g.setPaint(sideGrad);
        g.fillRect(sx,0,W-sx,H);

        // separator line with glow
        for (int i=3;i>0;i--) {
            g.setColor(new Color(70,70,180,18*i));
            g.fillRect(sx-i,0,i*2,H);
        }
        g.setColor(new Color(80,80,200));
        g.fillRect(sx,0,1,H);

        int cx=sx+14; // content x
        int y=22;

        // ── TITLE BLOCK ──
        drawGlassCard(g,sx+6,y-14,sw-6,52);
        g.setFont(new Font("Arial",Font.BOLD,22));
        g.setColor(new Color(210,215,255));
        g.drawString("TetriDoku",cx,y+10); y+=22;
        g.setFont(new Font("Arial",Font.PLAIN,10));
        g.setColor(new Color(90,90,140));
        g.drawString("Tetris  ×  Sudoku",cx,y+4); y+=28;

        // ── STATS CARD ──
        drawGlassCard(g,sx+6,y-6,sw-6,95);
        y = drawPremiumStat(g,cx,y+4,"SCORE",score,C_GOLD);
        y = drawPremiumStat(g,cx,y+2,"LEVEL",level,C_SILVER);
        y = drawPremiumStat(g,cx,y+2,"LINES",lines,C_SILVER);
        y+=14;

        // ── NEXT PIECE CARD ──
        drawGlassCard(g,sx+6,y-6,sw-6,82);
        g.setFont(new Font("Arial",Font.BOLD,9));
        g.setColor(new Color(130,130,180));
        g.drawString("NEXT PIECE",cx,y+4); y+=8;
        if (nxt!=null) drawMini(g,nxt,cx,y);
        y+=60;

        // ── SHIFT MECHANIC BADGE ──
        drawGlassCard(g,sx+6,y-6,sw-6,44);
        g.setFont(new Font("Arial",Font.BOLD,11));
        g.setColor(new Color(100,230,120));
        g.drawString("Q  /  E  —  Shift Numbers",cx,y+6); y+=15;
        g.setFont(new Font("Arial",Font.PLAIN,9));
        g.setColor(new Color(55,140,65));
        g.drawString("Cycle digits before dropping",cx,y+3); y+=28;

        // ── CONFLICT INDICATOR ──
        boolean hasConflict=false;
        for (boolean[] row:conflict) for (boolean b:row) if (b){hasConflict=true;break;}
        if (hasConflict) {
            float p=(float)(Math.sin(tick*0.14)*0.35+0.65);
            drawGlassCard(g,sx+6,y-4,sw-6,26,new Color((int)(p*180),10,10,120));
            g.setFont(new Font("Arial",Font.BOLD,10));
            g.setColor(new Color(255,80,80,(int)(p*255)));
            g.drawString("⚠  CONFLICT DETECTED",cx,y+12);
            y+=30;
        } else { y+=4; }

        // ── CONTROLS CARD ──
        drawGlassCard(g,sx+6,y-6,sw-6,108);
        g.setFont(new Font("Arial",Font.BOLD,9));
        g.setColor(new Color(110,110,170));
        g.drawString("CONTROLS",cx,y+4); y+=14;
        String[][] ctrl={
                {"\u2190 \u2192","Move"},
                {"\u2191  /  Z","Rotate"},
                {"\u2193","Soft drop"},
                {"Space","Hard drop"},
                {"Q / E","Shift nums"},
                {"P","Pause"},{"R","Restart"}};
        for (String[] row:ctrl) {
            g.setFont(new Font("Arial",Font.BOLD,9));
            g.setColor(C_GOLD);
            g.drawString(row[0],cx,y);
            g.setFont(new Font("Arial",Font.PLAIN,9));
            g.setColor(C_DIM);
            g.drawString(row[1],cx+52,y);
            y+=12;
        }
        y+=10;

        // ── SCORING CARD ──
        drawGlassCard(g,sx+6,y-6,sw-6,72);
        g.setFont(new Font("Arial",Font.BOLD,9));
        g.setColor(new Color(110,110,170));
        g.drawString("SCORING",cx,y+4); y+=14;
        String[][] pts={
                {"Unique clear","40 × Lvl"},
                {"Perfect row","500 × Lvl"},
                {"3×3 box","1000 × Lvl"},
                {"Repeated ×","Stays on board"},
        };
        for (String[] row:pts) {
            g.setFont(new Font("Arial",Font.PLAIN,9));
            g.setColor(new Color(80,85,120));
            g.drawString(row[0],cx,y);
            g.setFont(new Font("Arial",Font.BOLD,9));
            g.setColor(row[1].contains("Stays")?new Color(200,80,80):C_GOLD);
            g.drawString(row[1],cx+75,y);
            y+=13;
        }
    }

    // ── Glass card helper ────────────────────────────────────────
    void drawGlassCard(Graphics2D g, int x, int y, int w, int h) {
        drawGlassCard(g,x,y,w,h,null);
    }

    void drawGlassCard(Graphics2D g, int x, int y, int w, int h, Color tint) {
        int arc=12;
        // shadow
        g.setColor(new Color(0,0,0,55));
        g.fillRoundRect(x+2,y+3,w,h,arc,arc);
        // background
        Color bg1 = tint!=null ? tint : new Color(18,18,45,200);
        Color bg2 = tint!=null ? tint.darker() : new Color(12,12,32,200);
        GradientPaint gp=new GradientPaint(x,y,bg1,x,y+h,bg2);
        g.setPaint(gp);
        g.fillRoundRect(x,y,w,h,arc,arc);
        // top sheen
        g.setColor(new Color(255,255,255,12));
        g.fillRoundRect(x+2,y+1,w-4,h/2,arc,arc);
        // border
        g.setColor(new Color(70,70,130,160));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x,y,w,h,arc,arc);
    }

    // ── Premium stat row ─────────────────────────────────────────
    int drawPremiumStat(Graphics2D g, int cx, int y, String label, int val, Color valClr) {
        g.setFont(new Font("Arial",Font.BOLD,8));
        g.setColor(new Color(90,90,135));
        g.drawString(label,cx,y+10);
        g.setFont(new Font("Arial",Font.BOLD,20));
        g.setColor(valClr);
        // subtle drop shadow
        g.setColor(new Color(0,0,0,120));
        g.drawString(String.valueOf(val),cx+1,y+28);
        g.setColor(valClr);
        g.drawString(String.valueOf(val),cx,y+27);
        return y+32;
    }

    // ── Mini next-piece preview ───────────────────────────────────
    void drawMini(Graphics2D g, Piece p, int sx, int sy) {
        int MC=22;
        int[][] cells=p.cells();
        int minR=99,minC=99;
        for (int[] c:cells){minR=Math.min(minR,c[0]);minC=Math.min(minC,c[1]);}
        for (int i=0;i<4;i++) {
            int r=cells[i][0]-minR, c=cells[i][1]-minC;
            int x=sx+c*MC, y=sy+r*MC;
            int v=p.nums[i]; Color base=NUM_CLR[v];
            // shadow
            g.setColor(new Color(0,0,0,70));
            g.fillRoundRect(x+3,y+3,MC-4,MC-4,6,6);
            GradientPaint gp=new GradientPaint(x,y,blend(base,0.32f),x,y+MC,blend(base,0.14f));
            g.setPaint(gp);
            g.fillRoundRect(x+2,y+2,MC-4,MC-4,6,6);
            g.setColor(new Color(255,255,255,40));
            g.fillRoundRect(x+3,y+3,MC-8,4,4,4);
            g.setColor(base.brighter());
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(x+2,y+2,MC-4,MC-4,6,6);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial",Font.BOLD,11));
            FontMetrics fm=g.getFontMetrics(); String s=String.valueOf(v);
            g.drawString(s,x+(MC-fm.stringWidth(s))/2,
                    y+(MC+fm.getAscent()-fm.getDescent())/2-1);
        }
    }

    // ── Overlay ──────────────────────────────────────────────────
    void drawOverlay(Graphics2D g, String title, String sub, Color clr) {
        // frosted glass effect over board
        g.setColor(new Color(4,4,16,210));
        g.fillRoundRect(PAD,PAD,BOARD_W,BOARD_H,16,16);
        // subtle vignette
        for (int i=0;i<4;i++) {
            g.setColor(new Color(clr.getRed(),clr.getGreen(),clr.getBlue(),20-i*5));
            g.setStroke(new BasicStroke(i*2f));
            g.drawRoundRect(PAD+i*2,PAD+i*2,BOARD_W-i*4,BOARD_H-i*4,16,16);
        }
        int cx=PAD+BOARD_W/2, cy=PAD+BOARD_H/2;
        // title glow
        g.setFont(new Font("Arial",Font.BOLD,36));
        FontMetrics fm=g.getFontMetrics();
        g.setColor(new Color(clr.getRed(),clr.getGreen(),clr.getBlue(),60));
        g.drawString(title,cx-fm.stringWidth(title)/2+2,cy-18+2);
        g.setColor(clr);
        g.drawString(title,cx-fm.stringWidth(title)/2,cy-18);

        g.setFont(new Font("Arial",Font.PLAIN,13));
        fm=g.getFontMetrics();
        g.setColor(new Color(200,200,220));
        g.drawString(sub,cx-fm.stringWidth(sub)/2,cy+18);

        if (gameOver) {
            g.setFont(new Font("Arial",Font.BOLD,16));
            fm=g.getFontMetrics();
            String sc="Final Score  "+score;
            g.setColor(new Color(255,215,60,80));
            g.drawString(sc,cx-fm.stringWidth(sc)/2+1,cy+52+1);
            g.setColor(C_GOLD);
            g.drawString(sc,cx-fm.stringWidth(sc)/2,cy+52);
        }
        g.setStroke(new BasicStroke(1f));
    }
}

// ================================================================
class Piece {
    int type,rot,r,c; int[] nums;
    Piece(int type,int rot,int r,int c,int[] nums){
        this.type=type;this.rot=rot;this.r=r;this.c=c;this.nums=nums;}
    int[][] cells(){return GamePanel.SHAPES[type][rot];}
}