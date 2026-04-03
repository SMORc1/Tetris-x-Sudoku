import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

// ================================================================
//  T E T R I D O K U  v8
//  9x18 board | auto level/speed progression | rich sounds
//  pure black board | watermark text | combo block
// ================================================================

public class TetriDoku extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TetriDoku().setVisible(true));
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
class GamePanel extends JPanel implements ActionListener {

    static final int COLS = 9;
    static final int ROWS = 18;
    static final int PAD  = 8;
    static final int CELL;
    static final int SIDE_W;
    static final int BOARD_W;
    static final int BOARD_H;
    static final int W;
    static final int H;

    static {
        Dimension scr;
        try { scr = Toolkit.getDefaultToolkit().getScreenSize(); }
        catch (Exception e) { scr = new Dimension(1440, 900); }
        int usableH = scr.height - 80;
        int usableW = scr.width  - 40;
        int side    = Math.max(220, Math.min(290, usableW / 5));
        int cellByH = (usableH - PAD * 2) / ROWS;
        int cellByW = (usableW - side - PAD * 3) / COLS;
        CELL    = Math.max(34, Math.min(62, Math.min(cellByH, cellByW)));
        SIDE_W  = side;
        BOARD_W = COLS * CELL;
        BOARD_H = ROWS * CELL;
        W       = PAD + BOARD_W + PAD + SIDE_W;
        H       = PAD + BOARD_H + PAD;
    }

    static final Color[] PIECE_CLR = {
        new Color(  0, 215, 230),
        new Color(240, 200,   0),
        new Color(160,   0, 220),
        new Color( 20, 200,  55),
        new Color(220,  30,  30),
        new Color( 20,  80, 220),
        new Color(220, 115,   0),
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

    int[][] board      = new int[ROWS][COLS];
    int[][] boardColor = new int[ROWS][COLS];
    boolean[][] conflict = new boolean[ROWS][COLS];

    Piece cur, nxt;

    int     score, level = 1, lines;
    boolean gameOver, paused;

    // Auto-level: every PIECES_PER_LEVEL pieces locked, level goes up
    int     piecesSinceLevelUp = 0;
    static final int PIECES_PER_LEVEL = 10;
    boolean levelUpFlash = false;
    int     levelUpTick  = 0;
    static final int LEVELUP_DUR = 45;

    // Combo block
    int     clearStreak  = 0;
    boolean comboPending = false;

    // Combo wave
    List<int[]> comboWaveCells = new ArrayList<>();
    int comboWaveTick = 0;
    static final int COMBO_WAVE_DUR = 55;

    List<int[]>  flashCells   = new ArrayList<>();
    List<int[]>  pendingClear = new ArrayList<>();
    static final int FLASH_DUR = 14;
    int flashTick = 0;

    List<int[]> lockCells = new ArrayList<>();
    static final int LOCK_DUR = 12;
    int lockTick = 0;

    List<long[]>  shineItems = new ArrayList<>();
    boolean superPower   = false;
    boolean superPending = false;
    static final int SUPER_DUR = 88;
    int superTick = 0;

    List<long[]>  boxClears  = new ArrayList<>();
    List<float[]> particles  = new ArrayList<>();
    List<float[]> popups     = new ArrayList<>();

    int shakeTick = 0, shakeX = 0, shakeY = 0;
    static final int SHAKE_DUR = 22;

    Timer  gameTmr, animTmr;
    Random rng  = new Random();
    long   tick = 0;

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
        animTmr.start();
        newGame();
    }

    int tickMs() { return Math.max(80, 800 - (level - 1) * 80); }

    void newGame() {
        board = new int[ROWS][COLS]; boardColor = new int[ROWS][COLS];
        conflict = new boolean[ROWS][COLS];
        score = 0; lines = 0; level = 1; piecesSinceLevelUp = 0;
        gameOver = paused = superPower = superPending = false;
        comboPending = false; clearStreak = 0;
        comboWaveCells.clear(); comboWaveTick = 0;
        flashCells.clear(); pendingClear.clear(); flashTick = 0;
        lockCells.clear(); lockTick = 0; shineItems.clear(); boxClears.clear();
        particles.clear(); popups.clear(); shakeTick = 0;
        levelUpFlash = false; levelUpTick = 0;
        cur = makePiece(); nxt = makePiece();
        gameTmr.setDelay(tickMs()); gameTmr.start();
        SoundEngine.startBGM();
    }

    Piece makePiece() {
        if (comboPending) { comboPending = false; return makeComboPiece(); }
        int t = rng.nextInt(7);
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 9; i++) pool.add(i);
        Collections.shuffle(pool, rng);
        int[] nums = { pool.get(0), pool.get(1), pool.get(2), pool.get(3) };
        return new Piece(t, 0, 0, (COLS - 4) / 2, nums, false);
    }

    Piece makeComboPiece() {
        return new Piece(1, 0, 0, (COLS - 4) / 2, new int[]{ 8, 8, 8, 8 }, true);
    }

    void advancePieceCount() {
        piecesSinceLevelUp++;
        if (piecesSinceLevelUp >= PIECES_PER_LEVEL && level < 10) {
            level++; piecesSinceLevelUp = 0;
            gameTmr.setDelay(tickMs());
            levelUpFlash = true; levelUpTick = LEVELUP_DUR;
            SoundEngine.play("levelup");
            popups.add(new float[]{ PAD + BOARD_W / 2f, PAD + BOARD_H / 4f, -2.8f, 255f, -level });
        }
    }

    void onKey(int k) {
        if (superPower || comboWaveTick > 0) return;
        if (gameOver) { if (k == KeyEvent.VK_R) newGame(); return; }
        if (k == KeyEvent.VK_P) {
            paused = !paused;
            if (paused) { gameTmr.stop(); SoundEngine.stopBGM(); }
            else        { gameTmr.start(); SoundEngine.startBGM(); }
            repaint(); return;
        }
        if (paused || flashTick > 0) return;
        switch (k) {
            case KeyEvent.VK_LEFT:   shiftPiece(0, -1); break;
            case KeyEvent.VK_RIGHT:  shiftPiece(0,  1); break;
            case KeyEvent.VK_DOWN:   softDrop();         break;
            case KeyEvent.VK_UP:     rotate(1);          break;
            case KeyEvent.VK_Z:      rotate(-1);         break;
            case KeyEvent.VK_SPACE:  hardDrop();         break;
            case KeyEvent.VK_Q:      cycleNums(-1);      break;
            case KeyEvent.VK_E:      cycleNums( 1);      break;
            case KeyEvent.VK_MINUS:  adjustLevel(-1);    break;
            case KeyEvent.VK_EQUALS: adjustLevel( 1);    break;
        }
        repaint();
    }

    void adjustLevel(int d) {
        level = Math.max(1, Math.min(10, level + d));
        gameTmr.setDelay(tickMs());
    }

    void shiftPiece(int dr, int dc) {
        if (fits(cur.cells(), cur.r + dr, cur.c + dc)) {
            cur.r += dr; cur.c += dc; SoundEngine.play("move");
        }
    }

    void rotate(int d) {
        if (cur.isCombo) return;
        int nr = (cur.rot + d + 4) % 4;
        Piece p = new Piece(cur.type, nr, cur.r, cur.c, cur.nums.clone(), false);
        boolean ok = false;
        if      (fits(p.cells(), p.r,   p.c  )) { cur = p; ok = true; }
        else if (fits(p.cells(), p.r,   p.c-1)) { p.c--; cur = p; ok = true; }
        else if (fits(p.cells(), p.r,   p.c+1)) { p.c++; cur = p; ok = true; }
        else if (fits(p.cells(), p.r-1, p.c  )) { p.r--; cur = p; ok = true; }
        if (ok) SoundEngine.play("rotate");
    }

    void cycleNums(int d) {
        if (cur.isCombo) return;
        int[] n = cur.nums;
        if (d > 0) { int t = n[3]; n[3]=n[2]; n[2]=n[1]; n[1]=n[0]; n[0]=t; }
        else       { int t = n[0]; n[0]=n[1]; n[1]=n[2]; n[2]=n[3]; n[3]=t; }
        SoundEngine.play("cycle");
    }

    void softDrop() {
        if (fits(cur.cells(), cur.r + 1, cur.c)) { cur.r++; score++; }
        else lock();
    }

    void hardDrop() {
        int d = 0;
        while (fits(cur.cells(), cur.r + 1, cur.c)) { cur.r++; d++; }
        score += d * 2; SoundEngine.play("drop"); lock();
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (!gameOver && !paused && !superPower && flashTick == 0 && comboWaveTick == 0)
            { softDrop(); repaint(); }
    }

    void tickAnim() {
        if (lockTick    > 0) lockTick--;
        if (flashTick   > 0) { flashTick--;   if (flashTick   == 0) doClears(); }
        if (superPower)      { superTick--;   if (superTick   == 0) finishSuperPower(); }
        if (levelUpTick > 0) { levelUpTick--; if (levelUpTick == 0) levelUpFlash = false; }
        if (shakeTick   > 0) {
            shakeTick--;
            float inten = shakeTick * 3.5f / SHAKE_DUR;
            shakeX = (int)((rng.nextFloat() - 0.5f) * 2 * inten);
            shakeY = (int)((rng.nextFloat() - 0.5f) * 2 * inten);
            if (shakeTick == 0) { shakeX = 0; shakeY = 0; }
        }
        if (comboWaveTick > 0) { comboWaveTick--; if (comboWaveTick == 0) finishComboClear(); }

        long now = System.currentTimeMillis();
        shineItems.removeIf(s -> now - s[2] > 650);
        boxClears.removeIf(bc -> now - bc[2] > 800);

        Iterator<float[]> ip = particles.iterator();
        while (ip.hasNext()) {
            float[] p = ip.next();
            p[0]+=p[2]; p[1]+=p[3]; p[3]+=0.45f; p[4]-=1f;
            if (p[4] <= 0) ip.remove();
        }
        Iterator<float[]> iu = popups.iterator();
        while (iu.hasNext()) {
            float[] p = iu.next(); p[1]+=p[2]; p[3]-=4.5f;
            if (p[3] <= 0) iu.remove();
        }
    }

    void lock() {
        int[][] cells = cur.cells(); lockCells.clear();
        if (cur.isCombo) {
            for (int i = 0; i < 4; i++) {
                int r = cur.r+cells[i][0], c = cur.c+cells[i][1];
                if (r < 0) { gameOver=true; gameTmr.stop(); SoundEngine.play("gameover"); return; }
                board[r][c]=9; boardColor[r][c]=8; lockCells.add(new int[]{r,c});
            }
            shakeTick = SHAKE_DUR; SoundEngine.play("combo_place");
            triggerComboClear(); advancePieceCount(); return;
        }
        for (int i = 0; i < 4; i++) {
            int r = cur.r+cells[i][0], c = cur.c+cells[i][1];
            if (r < 0) { gameOver=true; gameTmr.stop(); SoundEngine.play("gameover"); return; }
            board[r][c]=cur.nums[i]; boardColor[r][c]=cur.type+1;
            lockCells.add(new int[]{r,c});
            Color bc = PIECE_CLR[cur.type];
            shineItems.add(new long[]{bx(c), by(r), System.currentTimeMillis(),
                                       bc.getRed(), bc.getGreen(), bc.getBlue()});
        }
        lockTick = LOCK_DUR;
        for (int[] lc : lockCells) spawnParticles(lc[0], lc[1], cur.type, 4, 2.5f);
        SoundEngine.play("lock"); markConflicts(); checkRows(); checkBoxes();
        advancePieceCount();
        if (flashCells.isEmpty() && !superPower) spawnNext();
    }

    void spawnNext() {
        cur = nxt; nxt = makePiece();
        if (!fits(cur.cells(), cur.r, cur.c)) {
            gameOver=true; gameTmr.stop(); SoundEngine.play("gameover");
        }
    }

    void checkRows() {
        boolean anyFull = false, scored = false;
        for (int r = 0; r < ROWS; r++) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) if (board[r][c]==0) { full=false; break; }
            if (!full) continue;
            anyFull=true; scored=true;
            int[] freq = new int[10];
            for (int c = 0; c < COLS; c++) freq[board[r][c]]++;
            boolean perfect = true; int cleared = 0;
            for (int c = 0; c < COLS; c++) {
                if (freq[board[r][c]]==1) { flashCells.add(new int[]{r,c}); pendingClear.add(new int[]{r,c}); cleared++; }
                else perfect=false;
            }
            if (perfect) { score+=2000*level; lines++; superPending=true; }
            else          score +=cleared*40*level;
        }
        if (scored) { clearStreak++; if (clearStreak>=3&&!comboPending) comboPending=true; }
        else if (!anyFull) clearStreak=0;
        if (anyFull) { level=Math.max(1,Math.min(10,lines/8+1)); gameTmr.setDelay(tickMs()); }
        if (!flashCells.isEmpty()) flashTick=FLASH_DUR;
    }

    void doClears() {
        int total = 0;
        for (int[] cell : pendingClear) {
            int r=cell[0], c=cell[1], t=boardColor[r][c]-1;
            if (t>=0&&t<7) spawnParticles(r,c,t,8,4f);
            board[r][c]=0; boardColor[r][c]=0; total++;
        }
        if (total>0) popups.add(new float[]{PAD+BOARD_W/2f, PAD+BOARD_H/2f, -2.5f, 255f, total*40*level});
        pendingClear.clear(); flashCells.clear(); applyGravity(); markConflicts();
        if (superPending) { superPending=false; SoundEngine.play("super"); triggerSuperPower(); }
        else { SoundEngine.play("clear"); spawnNext(); }
    }

    void applyGravity() {
        for (int c = 0; c < COLS; c++) {
            int w = ROWS-1;
            for (int r = ROWS-1; r >= 0; r--) {
                if (board[r][c]!=0) {
                    if (r!=w) { board[w][c]=board[r][c]; boardColor[w][c]=boardColor[r][c];
                                board[r][c]=0; boardColor[r][c]=0; }
                    w--;
                }
            }
            for (int r=w;r>=0;r--) { board[r][c]=0; boardColor[r][c]=0; }
        }
    }

    void triggerSuperPower() {
        superPower=true; superTick=SUPER_DUR; gameTmr.stop();
        popups.add(new float[]{PAD+BOARD_W/2f, PAD+BOARD_H/3f, -2f, 255f, 2000*level});
    }

    void finishSuperPower() {
        superPower=false;
        for (int r=0;r<ROWS;r++) for (int c=0;c<COLS;c++)
            if (board[r][c]>0) spawnParticles(r,c,rng.nextInt(7),7,4.5f);
        board=new int[ROWS][COLS]; boardColor=new int[ROWS][COLS]; conflict=new boolean[ROWS][COLS];
        gameTmr.setDelay(tickMs()); gameTmr.start(); spawnNext();
    }

    void triggerComboClear() {
        gameTmr.stop(); comboWaveTick=COMBO_WAVE_DUR; comboWaveCells.clear();
        int cr=ROWS/2, cc=COLS/2;
        List<int[]> cells = new ArrayList<>();
        for (int r=0;r<ROWS;r++) for (int c=0;c<COLS;c++)
            if (board[r][c]>0) cells.add(new int[]{r,c,(r-cr)*(r-cr)+(c-cc)*(c-cc)});
        cells.sort((a,b)->Integer.compare(a[2],b[2])); comboWaveCells=cells;
        int bonus = cells.size()*50*level+3000*level; score+=bonus;
        popups.add(new float[]{PAD+BOARD_W/2f, PAD+BOARD_H/3f, -2.5f, 255f, bonus});
        for (int[] cell : cells) {
            int t=boardColor[cell[0]][cell[1]]-1;
            if (t>=0&&t<7) spawnParticles(cell[0],cell[1],t,5,4.5f);
            else spawnRainbowParticles(cell[0],cell[1]);
        }
        SoundEngine.play("combo_clear");
    }

    void finishComboClear() {
        board=new int[ROWS][COLS]; boardColor=new int[ROWS][COLS]; conflict=new boolean[ROWS][COLS];
        clearStreak=0; comboWaveCells.clear();
        gameTmr.setDelay(tickMs()); gameTmr.start(); spawnNext();
    }

    void checkBoxes() {
        for (int br=0;br<ROWS/3;br++) for (int bc=0;bc<3;bc++) {
            int r0=br*3, c0=bc*3; Set<Integer> nums=new HashSet<>(); boolean ok=true;
            outer: for (int dr=0;dr<3;dr++) for (int dc=0;dc<3;dc++) {
                int v=board[r0+dr][c0+dc];
                if (v==0||v==9||!nums.add(v)){ok=false;break outer;}
            }
            if (ok&&nums.size()==9) {
                score+=1000*level; boxClears.add(new long[]{r0,c0,System.currentTimeMillis()});
                for (int dr=0;dr<3;dr++) for (int dc=0;dc<3;dc++) {
                    int t=boardColor[r0+dr][c0+dc]-1;
                    if (t>=0&&t<7) spawnParticles(r0+dr,c0+dc,t,8,4f);
                    board[r0+dr][c0+dc]=0; boardColor[r0+dr][c0+dc]=0; conflict[r0+dr][c0+dc]=false;
                }
                SoundEngine.play("box");
            }
        }
    }

    void markConflicts() {
        for (boolean[] row:conflict) Arrays.fill(row,false);
        for (int r=0;r<ROWS;r++) {
            int[] cnt=new int[11];
            for (int c=0;c<COLS;c++) if (board[r][c]>0&&board[r][c]<9) cnt[board[r][c]]++;
            for (int c=0;c<COLS;c++) if (board[r][c]>0&&board[r][c]<9&&cnt[board[r][c]]>1) conflict[r][c]=true;
        }
        for (int c=0;c<COLS;c++) {
            int[] cnt=new int[11];
            for (int r=0;r<ROWS;r++) if (board[r][c]>0&&board[r][c]<9) cnt[board[r][c]]++;
            for (int r=0;r<ROWS;r++) if (board[r][c]>0&&board[r][c]<9&&cnt[board[r][c]]>1) conflict[r][c]=true;
        }
    }

    boolean fits(int[][] cells,int r,int c) {
        for (int[] cell:cells) {
            int nr=r+cell[0], nc=c+cell[1];
            if (nr>=ROWS||nc<0||nc>=COLS) return false;
            if (nr>=0&&board[nr][nc]!=0) return false;
        }
        return true;
    }

    void spawnParticles(int r,int c,int type,int count,float speed) {
        Color base=(type>=0&&type<PIECE_CLR.length)?PIECE_CLR[type]:Color.WHITE;
        int px=bx(c)+CELL/2, py=by(r)+CELL/2;
        for (int i=0;i<count;i++) {
            double ang=rng.nextDouble()*Math.PI*2; float spd=speed*(0.5f+rng.nextFloat());
            particles.add(new float[]{px,py,(float)(Math.cos(ang)*spd),(float)(Math.sin(ang)*spd-0.5f),
                16+rng.nextInt(12),base.getRed(),base.getGreen(),base.getBlue()});
        }
    }

    void spawnRainbowParticles(int r,int c) {
        int px=bx(c)+CELL/2, py=by(r)+CELL/2;
        for (int i=0;i<10;i++) {
            double ang=rng.nextDouble()*Math.PI*2; float spd=4.5f*(0.5f+rng.nextFloat());
            Color col=Color.getHSBColor(rng.nextFloat(),0.9f,1f);
            particles.add(new float[]{px,py,(float)(Math.cos(ang)*spd),(float)(Math.sin(ang)*spd-0.5f),
                20+rng.nextInt(14),col.getRed(),col.getGreen(),col.getBlue()});
        }
    }

    int bx(int c) { return PAD+c*CELL+shakeX; }
    int by(int r) { return PAD+r*CELL+shakeY; }
    boolean isFlashing(int r,int c) { for (int[] fc:flashCells) if (fc[0]==r&&fc[1]==c) return true; return false; }

    // ════════════════════════════════════════════════════════════
    //  P A I N T
    // ════════════════════════════════════════════════════════════
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g2.setColor(Color.BLACK); g2.fillRect(0,0,W,H);
        drawWatermark(g2);
        drawBoard(g2); drawBoxGlows(g2); drawShines(g2);
        if (!gameOver&&!superPower&&comboWaveTick==0) { drawGhost(g2); drawActivePiece(g2); }
        drawComboWave(g2); drawLockEffect(g2); drawParticles(g2); drawPopups(g2);
        drawSidebar(g2);
        if (levelUpFlash) drawLevelUpOverlay(g2);
        if (superPower)   drawSuperPower(g2);
        if (gameOver)     drawOverlay(g2,"GAME OVER","Press R to restart",new Color(220,45,45));
        if (paused)       drawOverlay(g2,"PAUSED","Press P to continue",new Color(90,105,255));
    }

    // ── Watermark "TETRIDOKU" ghosted behind board ────────────────
    void drawWatermark(Graphics2D g) {
        Shape oldClip = g.getClip();
        g.setClip(PAD, PAD, BOARD_W, BOARD_H);
        Composite old = g.getComposite();
        g.setColor(Color.WHITE);

        // Large center text
        g.setFont(new Font("Arial Black", Font.BOLD, Math.max(18, BOARD_W / 5)));
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth("TETRIDOKU");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.030f));
        g.drawString("TETRIDOKU", PAD + (BOARD_W - textW) / 2, PAD + BOARD_H / 2 + fm.getAscent() / 2);

        // Smaller repeated top & bottom
        g.setFont(new Font("Arial Black", Font.BOLD, Math.max(11, BOARD_W / 10)));
        fm = g.getFontMetrics(); textW = fm.stringWidth("TETRIDOKU");
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.018f));
        g.drawString("TETRIDOKU", PAD + (BOARD_W - textW) / 2, PAD + BOARD_H / 4 + fm.getAscent() / 2);
        g.drawString("TETRIDOKU", PAD + (BOARD_W - textW) / 2, PAD + BOARD_H * 3 / 4 + fm.getAscent() / 2);

        g.setComposite(old); g.setClip(oldClip);
    }

    // ── Pure black board, no grid lines ──────────────────────────
    void drawBoard(Graphics2D g) {
        g.setColor(Color.BLACK); g.fillRect(PAD+shakeX, PAD+shakeY, BOARD_W, BOARD_H);
        for (int r=0;r<ROWS;r++) for (int c=0;c<COLS;c++) {
            int x=bx(c), y=by(r), v=board[r][c];
            if (v>0) {
                if (v==9) drawComboCell(g,x,y);
                else if (isFlashing(r,c)) {
                    int ct=boardColor[r][c]-1;
                    Color base=(ct>=0&&ct<7)?PIECE_CLR[ct]:Color.WHITE;
                    float a=Math.min(1f,(float)flashTick/(FLASH_DUR*0.55f));
                    g.setColor(new Color((int)(base.getRed()+(255-base.getRed())*a),
                        (int)(base.getGreen()+(255-base.getGreen())*a),
                        (int)(base.getBlue()+(255-base.getBlue())*a)));
                    g.fillRect(x+1,y+1,CELL-2,CELL-2);
                } else drawCell(g,x,y,v,boardColor[r][c]-1,conflict[r][c]);
            }
        }
        // Combo-incoming warning border
        if (nxt!=null&&nxt.isCombo) {
            float pulse=(float)(Math.sin(tick*0.22)*0.4+0.6);
            g.setColor(new Color(255,210,0,(int)(pulse*180)));
            g.setStroke(new BasicStroke(3f));
            g.drawRect(PAD+shakeX-1,PAD+shakeY-1,BOARD_W+2,BOARD_H+2);
            g.setStroke(new BasicStroke(1f));
        }
        g.setStroke(new BasicStroke(2f)); g.setColor(new Color(28,28,42));
        g.drawRect(PAD+shakeX,PAD+shakeY,BOARD_W,BOARD_H); g.setStroke(new BasicStroke(1f));
    }

    void drawComboCell(Graphics2D g,int x,int y) {
        float hue=((tick*5)%360)/360f; Color col=Color.getHSBColor(hue,0.85f,1f);
        float pulse=(float)(Math.sin(tick*0.3)*0.3+0.7);
        g.setColor(col.darker()); g.fillRect(x+1,y+1,CELL-2,CELL-2);
        g.setColor(col); g.fillRect(x+4,y+4,CELL-8,CELL-8);
        g.setColor(new Color(255,255,255,(int)(pulse*180)));
        g.setStroke(new BasicStroke(2f)); g.drawRect(x+1,y+1,CELL-3,CELL-3); g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Arial",Font.BOLD,Math.max(10,CELL*16/56))); FontMetrics fm=g.getFontMetrics();
        String s="\u2605"; g.setColor(Color.WHITE);
        g.drawString(s,x+(CELL-fm.stringWidth(s))/2,y+(CELL+fm.getAscent()-fm.getDescent())/2-1);
    }

    void drawComboWave(Graphics2D g) {
        if (comboWaveTick<=0||comboWaveCells.isEmpty()) return;
        float progress=1f-(float)comboWaveTick/COMBO_WAVE_DUR; int total=comboWaveCells.size();
        for (int i=0;i<total;i++) {
            int[] cell=comboWaveCells.get(i); float cellProg=(float)i/Math.max(1,total);
            if (cellProg>progress+0.18f) continue;
            float localAge=Math.max(0,progress-cellProg), alpha=Math.max(0f,1f-localAge*4f);
            Color col=Color.getHSBColor((cellProg+tick*0.02f)%1f,0.95f,1f);
            Composite old=g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha*0.92f));
            g.setColor(col); int x=bx(cell[1]),y=by(cell[0]);
            g.fillRect(x+1,y+1,CELL-2,CELL-2);
            g.setColor(new Color(255,255,255,(int)(alpha*160)));
            g.fillRect(x+CELL/3,y+CELL/3,CELL/3,CELL/3); g.setComposite(old);
        }
        if (progress<0.25f) {
            float a=(0.25f-progress)/0.25f*0.35f;
            Composite old=g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,a));
            g.setColor(Color.getHSBColor((tick*7%360)/360f,0.8f,1f));
            g.fillRect(bx(0),by(0),BOARD_W,BOARD_H); g.setComposite(old);
        }
    }

    void drawCell(Graphics2D g,int x,int y,int digit,int type,boolean hot) {
        Color base=(type>=0&&type<PIECE_CLR.length)?PIECE_CLR[type]:Color.GRAY;
        if (hot) {
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

    void drawNum(Graphics2D g,int x,int y,int v,int size,Color clr) {
        size=Math.max(10,size); g.setFont(new Font("Arial",Font.BOLD,size));
        FontMetrics fm=g.getFontMetrics(); String s=String.valueOf(v);
        int tx=x+(CELL-fm.stringWidth(s))/2, ty=y+(CELL+fm.getAscent()-fm.getDescent())/2-1;
        g.setColor(Color.BLACK); g.drawString(s,tx+1,ty+1);
        g.setColor(clr); g.drawString(s,tx,ty);
    }

    void drawActivePiece(Graphics2D g) {
        if (cur==null) return; int[][] cells=cur.cells();
        if (cur.isCombo) {
            for (int i=0;i<4;i++) {
                int r=cur.r+cells[i][0], c=cur.c+cells[i][1];
                if (r<0) continue; drawComboPieceCell(g,bx(c),by(r),i);
            }
        } else {
            for (int i=0;i<4;i++) {
                int r=cur.r+cells[i][0], c=cur.c+cells[i][1]; if (r<0) continue;
                int x=bx(c),y=by(r); drawCell(g,x,y,cur.nums[i],cur.type,false);
                Color b=PIECE_CLR[cur.type]; float gl=(float)(Math.sin(tick*0.13)*0.2+0.6);
                g.setColor(new Color(b.brighter().getRed(),b.brighter().getGreen(),b.brighter().getBlue(),(int)(gl*160)));
                g.setStroke(new BasicStroke(2f)); g.drawRect(x,y,CELL-1,CELL-1); g.setStroke(new BasicStroke(1f));
            }
        }
    }

    void drawComboPieceCell(Graphics2D g,int x,int y,int idx) {
        float hue=((tick*5+idx*25)%360)/360f; Color col=Color.getHSBColor(hue,0.9f,1f);
        g.setColor(col.darker()); g.fillRect(x+1,y+1,CELL-2,CELL-2);
        g.setColor(col.brighter()); g.fillRect(x+2,y+2,CELL-4,3); g.fillRect(x+2,y+2,3,CELL-4);
        g.setColor(col); g.fillRect(x+4,y+4,CELL-8,CELL-8);
        float pulse=(float)(Math.sin(tick*0.25)*0.4+0.6);
        g.setColor(new Color(255,255,255,(int)(pulse*220))); g.setStroke(new BasicStroke(2.5f));
        g.drawRect(x+1,y+1,CELL-3,CELL-3); g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Arial",Font.BOLD,Math.max(10,CELL*16/56))); FontMetrics fm=g.getFontMetrics();
        String s="\u2605"; g.setColor(Color.BLACK);
        g.drawString(s,x+(CELL-fm.stringWidth(s))/2+1,y+(CELL+fm.getAscent()-fm.getDescent())/2);
        g.setColor(Color.WHITE);
        g.drawString(s,x+(CELL-fm.stringWidth(s))/2,y+(CELL+fm.getAscent()-fm.getDescent())/2-1);
    }

    void drawGhost(Graphics2D g) {
        if (cur==null||cur.isCombo) return; int gr=cur.r;
        while (fits(cur.cells(),gr+1,cur.c)) gr++;
        if (gr==cur.r) return; int[][] cells=cur.cells(); float[] dash={5f,4f};
        Color base=PIECE_CLR[cur.type];
        for (int i=0;i<4;i++) {
            int r=gr+cells[i][0], c=cur.c+cells[i][1]; if (r<0) continue;
            int x=bx(c),y=by(r);
            g.setColor(base.darker().darker().darker()); g.fillRect(x+2,y+2,CELL-4,CELL-4);
            g.setColor(base); g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,4,dash,0));
            g.drawRect(x+2,y+2,CELL-4,CELL-4); g.setStroke(new BasicStroke(1f));
            drawNum(g,x,y,cur.nums[i],CELL*19/56,new Color(base.getRed(),base.getGreen(),base.getBlue(),175));
        }
    }

    void drawShines(Graphics2D g) {
        if (shineItems.isEmpty()) return; long now=System.currentTimeMillis();
        Shape oldClip=g.getClip(); Composite oldComp=g.getComposite();
        for (long[] sh:shineItems) {
            long age=now-sh[2]; if (age>=650) continue;
            float progress=age/650f, alpha=0.60f*(1f-progress);
            int x=(int)sh[0],y=(int)sh[1];
            g.setClip(x+1,y+1,CELL-2,CELL-2);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha));
            g.setColor(Color.WHITE);
            int bandCX=x+(int)((progress*1.7f-0.2f)*CELL); int bw=Math.max(8,CELL/5);
            g.translate(bandCX,y+CELL/2); g.rotate(-Math.PI/4.0);
            g.fillRect(-bw/2,-CELL*2,bw,CELL*5);
            g.rotate(Math.PI/4.0); g.translate(-bandCX,-(y+CELL/2));
            g.setClip(oldClip);
        }
        g.setComposite(oldComp); g.setClip(oldClip);
    }

    void drawLockEffect(Graphics2D g) {
        if (lockTick<=0||lockCells.isEmpty()) return;
        float a=(float)lockTick/LOCK_DUR; int expand=(int)((1f-a)*14);
        Composite old=g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,a*0.5f));
        for (int[] lc:lockCells) { g.setColor(Color.WHITE); g.fillRect(bx(lc[1])+2,by(lc[0])+2,CELL-4,CELL-4); }
        g.setComposite(old); g.setColor(new Color(255,255,255,(int)(a*90))); g.setStroke(new BasicStroke(2f));
        for (int[] lc:lockCells) g.drawRect(bx(lc[1])-expand,by(lc[0])-expand,CELL+expand*2,CELL+expand*2);
        g.setStroke(new BasicStroke(1f));
    }

    void drawBoxGlows(Graphics2D g) {
        long now=System.currentTimeMillis();
        for (long[] bc:boxClears) {
            float alpha=Math.max(0f,1f-(now-bc[2])/800f);
            int x=bx((int)bc[1]),y=by((int)bc[0]),bw=3*CELL,bh=3*CELL;
            Composite old=g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,alpha*0.28f));
            g.setColor(new Color(255,210,50)); g.fillRect(x+1,y+1,bw-2,bh-2); g.setComposite(old);
            g.setColor(new Color(255,215,50,(int)(alpha*255))); g.setStroke(new BasicStroke(2.5f));
            g.drawRect(x+1,y+1,bw-2,bh-2); g.setStroke(new BasicStroke(1f));
        }
    }

    void drawParticles(Graphics2D g) {
        for (float[] p:particles) {
            float a=p[4]/28f; int sz=Math.max(3,(int)(p[4]/3.5f));
            g.setColor(new Color((int)p[5],(int)p[6],(int)p[7],(int)(a*220)));
            g.fillOval((int)p[0]-sz/2,(int)p[1]-sz/2,sz,sz);
        }
    }

    void drawPopups(Graphics2D g) {
        for (float[] p:popups) {
            int alpha=Math.min(255,(int)p[3]);
            if (p[4]<0) { // level-up label
                int lv=(int)(-p[4]); String s="LEVEL "+lv+"!";
                g.setFont(new Font("Arial Black",Font.BOLD,28)); FontMetrics fm=g.getFontMetrics();
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

    void drawLevelUpOverlay(Graphics2D g) {
        float t=(float)levelUpTick/LEVELUP_DUR;
        float a=Math.min(1f,t<0.4f?t/0.4f:t)*0.22f;
        Composite old=g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,a));
        g.setColor(Color.getHSBColor(((tick*6)%360)/360f,0.8f,1f));
        g.fillRect(bx(0),by(0),BOARD_W,BOARD_H); g.setComposite(old);
        int cx=PAD+BOARD_W/2,cy=PAD+BOARD_H/2,bw=BOARD_W-40,bh=56;
        g.setColor(new Color(5,5,18,230)); g.fillRoundRect(cx-bw/2,cy-bh/2,bw,bh,12,12);
        float pulse=(float)(Math.sin(tick*0.3)*0.3+0.7);
        g.setColor(new Color(255,(int)(195*pulse),0,(int)(220*pulse))); g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx-bw/2,cy-bh/2,bw,bh,12,12); g.setStroke(new BasicStroke(1f));
        String msg="LEVEL  "+level+"  —  SPEED UP!";
        g.setFont(new Font("Arial Black",Font.BOLD,22)); FontMetrics fm=g.getFontMetrics();
        g.setColor(Color.getHSBColor(((tick*6)%360)/360f,0.9f,1f));
        g.drawString(msg,cx-fm.stringWidth(msg)/2,cy+fm.getAscent()/2-2);
    }

    void drawSuperPower(Graphics2D g) {
        float progress=(float)(SUPER_DUR-superTick)/SUPER_DUR;
        for (int r=0;r<ROWS;r++) for (int c=0;c<COLS;c++) {
            float hue=((tick*6+r*22+c*15)%360)/360f; Color col=Color.getHSBColor(hue,1f,1f);
            float a=0.20f+0.16f*(float)Math.sin(tick*0.22);
            g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),(int)(a*255)));
            g.fillRect(bx(c),by(r),CELL,CELL);
        }
        int cx=PAD+BOARD_W/2,cy=PAD+BOARD_H/2,cw=Math.min(BOARD_W-20,380),ch=138;
        g.setColor(new Color(5,5,18,240)); g.fillRoundRect(cx-cw/2,cy-ch/2,cw,ch,18,18);
        float pulse=(float)(Math.sin(tick*0.22)*0.38+0.62);
        g.setColor(new Color(255,(int)(195*pulse),0,(int)(240*pulse))); g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(cx-cw/2,cy-ch/2,cw,ch,18,18); g.setStroke(new BasicStroke(1f));
        float hueT=(tick*5%360)/360f;
        g.setFont(new Font("Arial Black",Font.BOLD,30)); FontMetrics fm=g.getFontMetrics();
        g.setColor(Color.getHSBColor(hueT,0.95f,1f));
        String t1="\u2726  SUPER POWER!  \u2726";
        g.drawString(t1,cx-fm.stringWidth(t1)/2,cy-20);
        g.setFont(new Font("Arial",Font.BOLD,12)); fm=g.getFontMetrics();
        g.setColor(new Color(190,195,230));
        String t2="Perfect 1\u20139 row!  Board wipes clean.";
        g.drawString(t2,cx-fm.stringWidth(t2)/2,cy+8);
        int bx2=cx-(cw-44)/2,by2=cy+25,bw2=cw-44,bh2=10;
        g.setColor(new Color(20,18,45)); g.fillRoundRect(bx2,by2,bw2,bh2,5,5);
        int filled=(int)(progress*bw2);
        g.setColor(Color.getHSBColor((tick*4%360)/360f,0.9f,1f));
        if (filled>0) g.fillRoundRect(bx2,by2,filled,bh2,5,5);
        g.setColor(new Color(255,215,70,140)); g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(bx2,by2,bw2,bh2,5,5); g.setStroke(new BasicStroke(1f));
    }

    void drawSidebar(Graphics2D g) {
        int sx=PAD+BOARD_W+PAD, cx=sx+12, cw=SIDE_W-18;
        g.setColor(new Color(10,10,13)); g.fillRect(sx,0,W-sx,H);
        g.setColor(new Color(36,36,52)); g.fillRect(sx,0,2,H);
        int y=14;
        drawCard(g,sx+4,y-8,cw,50);
        g.setFont(new Font("Arial Black",Font.BOLD,20)); g.setColor(new Color(210,215,255));
        g.drawString("TetriDoku",cx,y+12); y+=22;
        g.setFont(new Font("Arial",Font.PLAIN,10)); g.setColor(new Color(60,60,95));
        g.drawString("Tetris  \u00d7  Sudoku",cx,y+4); y+=26;
        drawCard(g,sx+4,y-4,cw,107);
        y=drawStat(g,cx,y+4,"SCORE",score,new Color(255,210,55));
        y=drawStat(g,cx,y+2,"LEVEL",level,speedColor(level));
        y=drawStat(g,cx,y+2,"LINES",lines,new Color(170,180,210)); y+=12;
        // Auto-level progress bar
        drawCard(g,sx+4,y-4,cw,44);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(90,95,145));
        g.drawString("NEXT LEVEL",cx,y+8); y+=12;
        int barW=cw-24,barH=9,barX=cx,barY=y+2;
        g.setColor(new Color(22,22,32)); g.fillRoundRect(barX,barY,barW,barH,4,4);
        int filledW=(int)((float)piecesSinceLevelUp/PIECES_PER_LEVEL*barW);
        if (filledW>0) { g.setColor(speedColor(level)); g.fillRoundRect(barX,barY,filledW,barH,4,4); }
        g.setColor(new Color(40,40,60)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(barX,barY,barW,barH,4,4);
        g.setFont(new Font("Arial",Font.PLAIN,8)); g.setColor(new Color(70,75,120));
        String pl=piecesSinceLevelUp+" / "+PIECES_PER_LEVEL+" pieces";
        g.drawString(pl,cx+barW-g.getFontMetrics().stringWidth(pl),barY+barH+10); y+=28;
        // Speed bar
        drawCard(g,sx+4,y-4,cw,66);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(90,95,145));
        g.drawString("GAME SPEED",cx,y+7); y+=15;
        int segTot=10,segH=11,segW=(cw-24)/segTot;
        for (int i=1;i<=segTot;i++) {
            g.setColor(i<=level?speedColor(i):new Color(24,24,34));
            g.fillRect(cx+(i-1)*(segW+2),y,segW,segH);
            if (i<=level) { g.setColor(new Color(255,255,255,55)); g.fillRect(cx+(i-1)*(segW+2)+1,y+1,segW-2,3); }
        }
        y+=segH+4;
        g.setFont(new Font("Arial Black",Font.BOLD,24)); g.setColor(speedColor(level));
        g.drawString(String.valueOf(level),cx+3,y+20);
        g.setFont(new Font("Arial",Font.PLAIN,8)); g.setColor(new Color(70,75,120));
        g.drawString("[\u2013] Slower",cx+28,y+8); g.drawString("[=] Faster",cx+28,y+19); y+=26;
        // Streak + next
        drawCard(g,sx+4,y-4,cw,90);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(90,95,145));
        g.drawString("CLEAR STREAK",cx,y+8);
        for (int i=0;i<3;i++) {
            if (i<clearStreak) g.setColor(Color.getHSBColor(((tick*4+i*40)%360)/360f,0.9f,1f));
            else g.setColor(new Color(24,24,34));
            g.fillOval(cx+72+i*18,y+1,12,12);
        }
        if (clearStreak>=3) {
            float pulse2=(float)(Math.sin(tick*0.28)*0.4+0.6);
            g.setFont(new Font("Arial Black",Font.BOLD,8));
            g.setColor(new Color(255,210,0,(int)(pulse2*255)));
            g.drawString("\u2605COMBO!",cx+125,y+11);
        }
        y+=16;
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(90,95,145));
        g.drawString("NEXT",cx,y+7); y+=9;
        if (nxt!=null) { if (nxt.isCombo) drawMiniCombo(g,cx,y); else drawMini(g,nxt,cx,y); }
        y+=56;
        drawCard(g,sx+4,y-4,cw,40);
        g.setFont(new Font("Arial",Font.BOLD,10)); g.setColor(new Color(55,200,80));
        g.drawString("Q / E  \u2014  Shift Numbers",cx,y+9); y+=16;
        g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(30,120,48));
        g.drawString("Rearrange digits on piece",cx,y+4); y+=26;
        boolean hasConflict=false;
        for (boolean[] row:conflict) for (boolean b:row) if(b){hasConflict=true;break;}
        if (hasConflict) {
            float p=(float)(Math.sin(tick*0.15)*0.35+0.65);
            drawCard(g,sx+4,y-2,cw,26,new Color((int)(p*125),8,8));
            g.setFont(new Font("Arial",Font.BOLD,10)); g.setColor(new Color(255,65,65,(int)(p*255)));
            g.drawString("\u26a0  CONFLICT DETECTED",cx,y+14); y+=30;
        } else y+=4;
        drawCard(g,sx+4,y-4,cw,120);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(80,85,140));
        g.drawString("CONTROLS",cx,y+7); y+=15;
        String[][] ctrl={{"\u2190\u2192","Move"},{"\u2191 / Z","Rotate"},{"\u2193","Soft drop"},
            {"Space","Hard drop"},{"Q / E","Shift nums"},{"\u2013 / =","Speed \u2212/+"},{"P","Pause"},{"R","Restart"}};
        for (String[] row:ctrl) {
            g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(225,185,45)); g.drawString(row[0],cx,y);
            g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(55,60,100)); g.drawString(row[1],cx+50,y); y+=13;
        }
        y+=6;
        drawCard(g,sx+4,y-4,cw,88);
        g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(new Color(80,85,140));
        g.drawString("SCORING",cx,y+7); y+=15;
        String[] sl={"Unique clear","SUPER POWER!","3\u00d73 box","\u2605COMBO (3+)","Repeated \u2192"};
        String[] sv={"40\u00d7Lvl","2000\u00d7Lvl","1000\u00d7Lvl","Board wipe!","Stays"};
        Color[] sc={new Color(180,180,55),new Color(255,210,0),new Color(255,210,0),new Color(255,225,50),new Color(185,60,60)};
        for (int i=0;i<sl.length;i++) {
            g.setFont(new Font("Arial",Font.PLAIN,9)); g.setColor(new Color(55,60,100)); g.drawString(sl[i],cx,y);
            g.setFont(new Font("Arial",Font.BOLD,9)); g.setColor(sc[i]); g.drawString(sv[i],cx+95,y); y+=13;
        }
    }

    void drawCard(Graphics2D g,int x,int y,int w,int h) { drawCard(g,x,y,w,h,new Color(18,18,24)); }
    void drawCard(Graphics2D g,int x,int y,int w,int h,Color bg) {
        g.setColor(bg); g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(new Color(36,36,54)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(x,y,w,h,8,8);
    }

    int drawStat(Graphics2D g,int cx,int y,String label,int val,Color clr) {
        g.setFont(new Font("Arial",Font.BOLD,8)); g.setColor(new Color(60,65,108)); g.drawString(label,cx,y+10);
        g.setFont(new Font("Arial Black",Font.BOLD,20)); g.setColor(Color.BLACK); g.drawString(String.valueOf(val),cx+1,y+28);
        g.setColor(clr); g.drawString(String.valueOf(val),cx,y+27); return y+32;
    }

    void drawMini(Graphics2D g,Piece p,int sx,int sy) {
        int MC=21; int[][] cells=p.cells(); int minR=99,minC=99;
        for (int[] c:cells) { minR=Math.min(minR,c[0]); minC=Math.min(minC,c[1]); }
        Color base=PIECE_CLR[p.type];
        for (int i=0;i<4;i++) {
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

    void drawMiniCombo(Graphics2D g,int sx,int sy) {
        int MC=21; int[][] pos={{0,0},{0,1},{1,0},{1,1}};
        for (int i=0;i<4;i++) {
            int x=sx+pos[i][1]*MC,y=sy+pos[i][0]*MC;
            float hue=((tick*5+i*25)%360)/360f; Color col=Color.getHSBColor(hue,0.9f,1f);
            g.setColor(col.darker()); g.fillRect(x+1,y+1,MC-2,MC-2);
            g.setColor(col); g.fillRect(x+3,y+3,MC-6,MC-6);
            float pulse=(float)(Math.sin(tick*0.25)*0.4+0.6);
            g.setColor(new Color(255,255,255,(int)(pulse*190))); g.setStroke(new BasicStroke(1.4f));
            g.drawRect(x+1,y+1,MC-3,MC-3); g.setStroke(new BasicStroke(1f));
            g.setFont(new Font("Arial",Font.BOLD,9)); FontMetrics fm=g.getFontMetrics(); String s="\u2605";
            g.setColor(Color.WHITE); g.drawString(s,x+(MC-fm.stringWidth(s))/2,y+(MC+fm.getAscent()-fm.getDescent())/2-1);
        }
        g.setFont(new Font("Arial Black",Font.BOLD,8));
        g.setColor(Color.getHSBColor(((tick*4)%360)/360f,0.9f,1f));
        g.drawString("COMBO!",sx+46,sy+22);
    }

    Color speedColor(int lv) {
        float t=(lv-1)/9f;
        return new Color(Math.min(255,(int)(t*225+30)),Math.min(255,(int)((1f-t)*200+55)),22);
    }

    void drawOverlay(Graphics2D g,String title,String sub,Color clr) {
        g.setColor(new Color(0,0,0,200)); g.fillRect(PAD,PAD,BOARD_W,BOARD_H);
        int cx=PAD+BOARD_W/2,cy=PAD+BOARD_H/2;
        g.setFont(new Font("Arial Black",Font.BOLD,36)); FontMetrics fm=g.getFontMetrics();
        g.setColor(new Color(clr.getRed(),clr.getGreen(),clr.getBlue(),50));
        g.drawString(title,cx-fm.stringWidth(title)/2+2,cy-18+2);
        g.setColor(clr); g.drawString(title,cx-fm.stringWidth(title)/2,cy-18);
        g.setFont(new Font("Arial",Font.PLAIN,13)); fm=g.getFontMetrics();
        g.setColor(new Color(190,190,215)); g.drawString(sub,cx-fm.stringWidth(sub)/2,cy+18);
        if (gameOver) {
            g.setFont(new Font("Arial Black",Font.BOLD,16)); fm=g.getFontMetrics();
            String sc="Final Score  "+score; g.setColor(new Color(255,215,50));
            g.drawString(sc,cx-fm.stringWidth(sc)/2,cy+52);
        }
    }
}

// ================================================================
//  S O U N D   E N G I N E — Synthesized SFX + looping BGM
// ================================================================
class SoundEngine {
    static final int SR = 44100;
    static final Map<String,byte[]> cache = new HashMap<>();
    static volatile boolean ready = false;

    static volatile boolean bgmRunning = false;
    static Thread bgmThread = null;
    static long bgmSample = 0;

    static final int BPM = 160, BEAT_SAMP = SR*60/BPM, BAR_SAMP = BEAT_SAMP*4;
    static final float[] MEL  = {440f,329.6f,349.2f,392f, 349.2f,329.6f,293.7f,261.6f, 261.6f,329.6f,392f,440f, 493.9f,440f,392f,329.6f};
    static final float[] BASS = {110f,130.8f,87.3f,98f,   130.8f,87.3f,65.4f,98f,     65.4f,87.3f,110f,130.8f,  123.5f,110f,98f,87.3f};

    static void init() {
        if (ready) return;
        Thread t=new Thread(()->{
            try {
                cache.put("move",       sine(880,14,0.07f,12f));
                cache.put("rotate",     sweep(400,800,38,0.15f));
                cache.put("cycle",      chimeSound());
                cache.put("lock",       lockSound());
                cache.put("drop",       dropSound());
                cache.put("clear",      clearSound());
                cache.put("box",        boxSound());
                cache.put("super",      superSound());
                cache.put("gameover",   gameOverSound());
                cache.put("levelup",    levelUpSound());
                cache.put("combo_place",comboPlaceSound());
                cache.put("combo_clear",comboClearSound());
                ready=true;
            } catch (Exception ignored) {}
        },"snd-init");
        t.setDaemon(true); t.start();
    }

    static void play(String name) {
        if (!ready) return; byte[] data=cache.get(name); if (data==null) return;
        new Thread(()->emit(data),"snd").start();
    }

    static void emit(byte[] data) {
        try {
            AudioFormat fmt=new AudioFormat(SR,16,1,true,false);
            SourceDataLine line=AudioSystem.getSourceDataLine(fmt);
            line.open(fmt,2048); line.start(); line.write(data,0,data.length);
            line.drain(); line.close();
        } catch (Exception ignored) {}
    }

    static void startBGM() {
        if (bgmRunning) return; bgmRunning=true;
        bgmThread=new Thread(()->{
            try {
                AudioFormat fmt=new AudioFormat(SR,16,1,true,false);
                SourceDataLine line=AudioSystem.getSourceDataLine(fmt);
                line.open(fmt,4096); line.start();
                while (bgmRunning) line.write(bgmFrame(),0,1024);
                line.drain(); line.close();
            } catch (Exception ignored) {}
        },"bgm");
        bgmThread.setDaemon(true); bgmThread.start();
    }

    static void stopBGM() { bgmRunning=false; }

    static byte[] bgmFrame() {
        int n=512; byte[] buf=new byte[n*2]; int patLen=BEAT_SAMP*16;
        for (int i=0;i<n;i++) {
            long s=bgmSample+i; int pos=(int)(s%patLen), ni=pos/BEAT_SAMP, pin=pos%BEAT_SAMP;
            float nt=(float)pin/BEAT_SAMP;
            // Triangle melody
            float mf=MEL[ni%MEL.length], mEnv=melEnv(nt), mPh=(float)(s*mf/SR)%1f;
            float mel=(mPh<0.5f?mPh*4f-1f:3f-mPh*4f)*mEnv*0.16f;
            float mPh2=(float)(s*mf*2/SR)%1f; mel+=(mPh2<0.5f?mPh2*4f-1f:3f-mPh2*4f)*mEnv*0.055f;
            // Sawtooth bass
            float bf=BASS[ni%BASS.length], bEnv=bassEnv(nt), bPh=(float)(s*bf/SR)%1f;
            float bass=(bPh*2f-1f)*bEnv*0.13f;
            // Hi-hat (deterministic noise)
            float hihat=0f; int pinH=pin%(BEAT_SAMP/2);
            if (pinH<BEAT_SAMP/8) {
                float hh=(float)pinH/(BEAT_SAMP/8);
                long seed=(s/64)*6364136223846793005L+1442695040888963407L;
                float noise=((seed>>>33)/(float)(1L<<31)-1f);
                hihat=noise*(float)Math.exp(-8*hh)*0.038f;
            }
            // Kick on beats 1&3
            float kick=0f; int posInBar=(int)(s%BAR_SAMP), kickZone=BEAT_SAMP/5;
            if (posInBar<kickZone||(posInBar>=BEAT_SAMP*2&&posInBar<BEAT_SAMP*2+kickZone)) {
                int kp=posInBar%BEAT_SAMP; float kt=(float)kp/kickZone;
                float kf=80f*(float)Math.exp(-kt*8);
                kick=(float)Math.sin(2*Math.PI*kf*kp/SR)*(float)Math.exp(-kt*4)*0.20f;
            }
            // Snare on beat 2
            float snare=0f;
            if (posInBar>=BEAT_SAMP&&posInBar<BEAT_SAMP+BEAT_SAMP/5) {
                int sp=posInBar-BEAT_SAMP; float st=(float)sp/(BEAT_SAMP/5);
                long snSeed=(s/32)*2862933555777941757L+3037000499L;
                float snNoise=((snSeed>>>33)/(float)(1L<<31)-1f);
                snare=snNoise*(float)Math.exp(-6*st)*0.028f;
            }
            float mix=Math.max(-0.93f,Math.min(0.93f,mel+bass+hihat+kick+snare));
            short v=(short)(mix*32767); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        bgmSample+=n; return buf;
    }

    static float melEnv(float t) { if (t<0.04f) return t/0.04f; if (t<0.78f) return 1f; return (float)Math.exp(-5*(t-0.78f)/0.22f); }
    static float bassEnv(float t) { if (t<0.015f) return t/0.015f; return (float)Math.exp(-4.5f*t); }

    static byte[] chimeSound() {
        int n=SR*120/1000; byte[] buf=new byte[n*2];
        float[] freqs={1318.5f,1760f}; float[] amps={0.40f,0.26f};
        for (int i=0;i<n;i++) {
            float t=i/(float)n, env=(float)Math.exp(-5.5f*t)*(1f-(float)Math.exp(-40f*t)), s=0;
            for (int h=0;h<freqs.length;h++) s+=(float)Math.sin(2*Math.PI*freqs[h]*i/SR)*amps[h];
            s*=env; short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        return buf;
    }

    static byte[] lockSound() {
        int n=SR*90/1000; byte[] buf=new byte[n*2];
        for (int i=0;i<n;i++) {
            float t=i/(float)n, env=(float)Math.exp(-9f*t);
            float s=(float)(Math.sin(2*Math.PI*120*i/SR)+Math.sin(2*Math.PI*240*i/SR)*0.30f+Math.sin(2*Math.PI*480*i/SR)*0.12f+Math.sin(2*Math.PI*960*i/SR)*0.04f)*env*0.32f;
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        return buf;
    }

    static byte[] dropSound() {
        int n=SR*110/1000; byte[] buf=new byte[n*2]; double phase=0;
        for (int i=0;i<n;i++) {
            float t=i/(float)n, freq=300-220*t; phase+=2*Math.PI*freq/SR;
            float env=(float)Math.exp(-5f*t)*(1f-(float)Math.exp(-30f*t));
            float s=(float)Math.sin(phase)*env*0.42f;
            if (t<0.05f) { long seed=(long)i*6364136223846793005L; float noise=((seed>>>33)/(float)(1L<<31)-1f); s+=noise*(1f-t/0.05f)*0.08f; }
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        return buf;
    }

    static byte[] clearSound() {
        int[] freqs={523,659,784,1047}; int noteMs=72, total=SR*(noteMs*4+130)/1000;
        byte[] buf=new byte[total*2];
        for (int ni=0;ni<freqs.length;ni++) {
            int start=SR*ni*noteMs/1000, dur=SR*(noteMs+100)/1000;
            for (int i=start;i<Math.min(total,start+dur);i++) {
                int loc=i-start; float t=loc/(float)dur;
                float att=Math.min(1f,loc/(float)(SR*5/1000)), env=att*(float)Math.exp(-4.5f*t);
                float s=(float)(Math.sin(2*Math.PI*freqs[ni]*i/SR)+Math.sin(2*Math.PI*freqs[ni]*2*i/SR)*0.22f)*env*0.44f;
                addSample(buf,i,s);
            }
        }
        return buf;
    }

    static byte[] boxSound() {
        int n=SR*320/1000; byte[] buf=new byte[n*2];
        float[] hf={523f,659f,784f,1047f,1318f}; float[] ha={0.38f,0.28f,0.20f,0.12f,0.06f};
        for (int i=0;i<n;i++) {
            float t=i/(float)n, att=Math.min(1f,i/(float)(SR*8/1000)), env=att*(float)Math.exp(-3.5f*t), s=0;
            for (int h=0;h<hf.length;h++) s+=(float)Math.sin(2*Math.PI*hf[h]*i/SR)*ha[h];
            s*=env; short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        return buf;
    }

    static byte[] superSound() {
        int[] noteF={262,330,392,523,659,784}; int noteMs=95,finalMs=400;
        int total=SR*(noteMs*noteF.length+finalMs)/1000; byte[] buf=new byte[total*2];
        for (int ni=0;ni<noteF.length;ni++) {
            int start=SR*ni*noteMs/1000, dur=(ni==noteF.length-1)?SR*(noteMs+finalMs)/1000:SR*(noteMs+28)/1000;
            for (int i=start;i<Math.min(total,start+dur);i++) {
                int loc=i-start; float t=loc/(float)dur;
                float att=Math.min(1f,loc/(float)(SR*8/1000)), rel=t<0.72f?1f:(float)Math.exp(-4f*(t-0.72f)/0.28f), env=att*rel;
                float s=(float)(Math.sin(2*Math.PI*noteF[ni]*i/SR)+Math.sin(2*Math.PI*noteF[ni]*2*i/SR)*0.18f+Math.sin(2*Math.PI*noteF[ni]*3*i/SR)*0.06f)*env*0.38f;
                addSample(buf,i,s);
            }
        }
        return buf;
    }

    static byte[] levelUpSound() {
        int[] noteF={523,784,1047,1318}; int noteMs=60,gap=30;
        int total=SR*((noteMs+gap)*noteF.length+80)/1000; byte[] buf=new byte[total*2];
        for (int ni=0;ni<noteF.length;ni++) {
            int start=SR*ni*(noteMs+gap)/1000, dur=SR*(noteMs+(ni==noteF.length-1?160:0))/1000;
            for (int i=start;i<Math.min(total,start+dur);i++) {
                int loc=i-start; float t=loc/(float)dur;
                float att=Math.min(1f,loc/(float)(SR*5/1000)), env=att*(float)Math.exp(-5.5f*t);
                float s=(float)(Math.sin(2*Math.PI*noteF[ni]*i/SR)+Math.sin(2*Math.PI*noteF[ni]*2*i/SR)*0.20f)*env*0.42f;
                addSample(buf,i,s);
            }
        }
        return buf;
    }

    static byte[] gameOverSound() {
        int[] notes={330,294,262,247,220}; int noteMs=145, total=SR*noteMs*notes.length/1000;
        byte[] buf=new byte[total*2];
        for (int ni=0;ni<notes.length;ni++) {
            int start=SR*ni*noteMs/1000, dur=SR*(noteMs-12)/1000;
            for (int i=start;i<Math.min(total,start+dur);i++) {
                int loc=i-start; float t=loc/(float)dur;
                float att=Math.min(1f,loc/(float)(SR*8/1000)), env=att*(float)Math.exp(-3.0f*t);
                float s=(float)(Math.sin(2*Math.PI*notes[ni]*i/SR)+Math.sin(2*Math.PI*notes[ni]*1.5*i/SR)*0.14f)*env*0.38f;
                addSample(buf,i,s);
            }
        }
        return buf;
    }

    static byte[] comboPlaceSound() {
        int n=SR*280/1000; byte[] buf=new byte[n*2]; double phase=0;
        for (int i=0;i<n;i++) {
            float t=i/(float)n, freq=150f+1600f*t; phase+=2*Math.PI*freq/SR;
            float env=(float)Math.exp(-2.5f*t)*(1f-(float)Math.exp(-25f*t));
            float s=(float)Math.sin(phase)*env*0.30f+(float)Math.sin(phase*2)*env*0.14f+(float)Math.sin(phase*3)*env*0.06f;
            long seed=(long)i*2862933555777941757L; float noise=((seed>>>33)/(float)(1L<<31)-1f);
            s+=noise*env*0.09f; short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        return buf;
    }

    static byte[] comboClearSound() {
        int ms=1900, n=SR*ms/1000; byte[] buf=new byte[n*2];
        // Sub boom
        for (int i=0;i<n;i++) {
            float t=i/(float)n, freq=55f*(float)Math.exp(-t*5), env=(float)Math.exp(-2.0f*t);
            addSample(buf,i,(float)Math.sin(2*Math.PI*freq*i/SR)*env*0.50f);
        }
        // Ascending arpeggio
        float[] arp={220f,261.6f,293.7f,329.6f,392f,440f,523f,659f,784f,1047f}; int arpMs=80;
        for (int ni=0;ni<arp.length;ni++) {
            int start=SR*ni*arpMs/1000, dur=SR*(arpMs+140)/1000;
            for (int i=start;i<Math.min(n,start+dur);i++) {
                int loc=i-start; float t=loc/(float)dur;
                float att=Math.min(1f,loc/(float)(SR*8/1000)), env=att*(float)Math.exp(-3.8f*t);
                addSample(buf,i,(float)(Math.sin(2*Math.PI*arp[ni]*i/SR)+Math.sin(2*Math.PI*arp[ni]*2*i/SR)*0.18f)*env*0.26f);
            }
        }
        // Sparkle
        int sparkEnd=SR*500/1000;
        for (int i=0;i<sparkEnd;i++) {
            float t=i/(float)sparkEnd, env=(float)Math.exp(-7f*t);
            long seed=(long)i*6364136223846793005L+1442695040888963407L;
            addSample(buf,i,((seed>>>33)/(float)(1L<<31)-1f)*env*0.07f);
        }
        // Bell chord at 700ms
        int bellStart=SR*700/1000; float[] bells={523f,659f,784f,1047f,1318f}; float[] bellA={0.28f,0.22f,0.18f,0.12f,0.07f};
        for (int bi=0;bi<bells.length;bi++) for (int i=bellStart;i<n;i++) {
            float t=(i-bellStart)/(float)(n-bellStart), env=(float)Math.exp(-3.2f*t);
            addSample(buf,i,(float)Math.sin(2*Math.PI*bells[bi]*i/SR)*env*bellA[bi]);
        }
        return buf;
    }

    static byte[] sine(float freq,int ms,float vol,float decay) {
        int n=SR*ms/1000; byte[] buf=new byte[n*2];
        for (int i=0;i<n;i++) {
            float env=(float)Math.exp(-decay*(float)i/n), s=(float)Math.sin(2*Math.PI*freq*i/SR)*env*vol;
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        return buf;
    }

    static byte[] sweep(float f1,float f2,int ms,float vol) {
        int n=SR*ms/1000; byte[] buf=new byte[n*2]; double phase=0;
        for (int i=0;i<n;i++) {
            float t=i/(float)n, env=(float)Math.exp(-4.5f*t)*(1f-(float)Math.exp(-30f*t)), freq=f1+(f2-f1)*t;
            phase+=2*Math.PI*freq/SR; float s=(float)Math.sin(phase)*env*vol;
            short v=clamp(s); buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
        }
        return buf;
    }

    static short clamp(float s) { return (short)Math.max(-32767,Math.min(32767,(int)(s*32767))); }

    static void addSample(byte[] buf,int i,float s) {
        if (i*2+1>=buf.length) return;
        short existing=(short)((buf[i*2+1]<<8)|(buf[i*2]&0xFF));
        int sum=existing+(int)(s*32767); short v=(short)Math.max(-32767,Math.min(32767,sum));
        buf[i*2]=(byte)(v&0xFF); buf[i*2+1]=(byte)((v>>8)&0xFF);
    }
}

// ================================================================
class Piece {
    int type,rot,r,c; int[] nums; boolean isCombo;
    Piece(int type,int rot,int r,int c,int[] nums,boolean isCombo) {
        this.type=type; this.rot=rot; this.r=r; this.c=c; this.nums=nums; this.isCombo=isCombo;
    }
    int[][] cells() { return GamePanel.SHAPES[type][rot]; }
}
