import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

//================================================================================
// MAIN MENU PANEL - Entry point for all menu screens
//================================================================================

class MainMenu extends JPanel {
    private BufferedImage backgroundImage;
    private GameButton playBtn;
    private GameButton settingsBtn;
    private GameButton tutorialBtn;
    private GameButton aboutBtn;
    private Runnable onPlay;
    private Runnable onSettings;
    private Runnable onTutorial;
    private Runnable onAbout;

    MainMenu(Runnable onPlay, Runnable onSettings, Runnable onTutorial, Runnable onAbout) {
        this.onPlay = onPlay;
        this.onSettings = onSettings;
        this.onTutorial = onTutorial;
        this.onAbout = onAbout;

        try {
            File imageFile = new File("media/images/Tetridoku.png");
            if (!imageFile.exists()) {
                imageFile = new File("Tetridoku/media/images/Tetridoku.png");
            }
            if (!imageFile.exists()) {
                imageFile = new File("../media/images/Tetridoku.png");
            }
            if (!imageFile.exists()) {
                imageFile = new File("asset/Tetridoku.png");
            }
            if (imageFile.exists()) {
                backgroundImage = ImageIO.read(imageFile);
                System.out.println("Background image loaded from: " + imageFile.getAbsolutePath());
            } else {
                System.err.println("Could not find Tetridoku.png in any expected location");
            }
        } catch (Exception e) {
            System.err.println("Could not load background image: " + e.getMessage());
            e.printStackTrace();
        }

        this.setLayout(null);
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(new Color(15, 15, 25));

        playBtn = createMenuButton("PLAY", 300, 200);
        playBtn.addActionListener(e -> this.onPlay.run());

        settingsBtn = createMenuButton("SETTINGS", 300, 270);
        settingsBtn.addActionListener(e -> this.onSettings.run());

        tutorialBtn = createMenuButton("TUTORIAL", 300, 340);
        tutorialBtn.addActionListener(e -> this.onTutorial.run());

        aboutBtn = createMenuButton("ABOUT THE DEVELOPERS", 300, 410);
        aboutBtn.addActionListener(e -> this.onAbout.run());

        this.add(playBtn);
        this.add(settingsBtn);
        this.add(tutorialBtn);
        this.add(aboutBtn);
    }

    private GameButton createMenuButton(String text, int x, int y) {
        GameButton btn = new GameButton(text);
        btn.setBounds(x, y, 200, 45);
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (backgroundImage != null) {
            Image scaled = backgroundImage.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            g2.drawImage(scaled, 0, 0, null);
        } else {
            g2.setColor(new Color(15, 15, 25));
            g2.fillRect(0, 0, w, h);
            
            GradientPaint gp = new GradientPaint(0, 0, new Color(20, 20, 40),
                    w, h, new Color(40, 30, 60));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        }
    }
}

//================================================================================
// GAME BUTTON - Custom styled button with 3D effects, gradients, and glows
//================================================================================

class GameButton extends JButton {
    private boolean isHovered = false;
    private boolean isPressed = false;
    private float glowAlpha = 0f;
    private float scaleAmount = 1f;
    private Timer animationTimer;
    
    private static final Color PRIMARY_COLOR = new Color(30, 100, 200);
    private static final Color HOVER_COLOR = new Color(50, 130, 230);
    private static final Color PRESSED_COLOR = new Color(20, 80, 180);
    private static final Color GLOW_COLOR = new Color(100, 200, 255);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color TEXT_SHADOW = new Color(0, 0, 0, 100);
    
    private static final int BORDER_RADIUS = 12;
    private static final int BUTTON_HEIGHT = 45;
    private static final int BUTTON_WIDTH = 200;
    
    public GameButton(String text) {
        super(text);
        setupButton();
    }
    
    private void setupButton() {
        setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        setFont(new Font("Arial", Font.BOLD, 14));
        setForeground(TEXT_COLOR);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        setUI(new GameButtonUI());
        
        animationTimer = new Timer(16, e -> {
            if (isHovered) {
                glowAlpha = Math.min(glowAlpha + 0.08f, 0.9f);
                scaleAmount = Math.min(scaleAmount + 0.02f, 1.08f);
            } else {
                glowAlpha = Math.max(glowAlpha - 0.08f, 0f);
                scaleAmount = Math.max(scaleAmount - 0.02f, 1f);
            }
            repaint();
        });
        animationTimer.start();
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
            }
        });
    }
    
    private class GameButtonUI extends BasicButtonUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int width = c.getWidth();
            int height = c.getHeight();
            
            if (scaleAmount != 1f) {
                g2.translate(width / 2, height / 2);
                g2.scale(scaleAmount, scaleAmount);
                g2.translate(-width / 2, -height / 2);
            }
            
            if (glowAlpha > 0) {
                drawGlow(g2, width, height);
            }
            
            drawButtonBackground(g2, width, height);
            draw3DEffect(g2, width, height);
            drawText(g2, c, width, height);
        }
        
        private void drawGlow(Graphics2D g2, int width, int height) {
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, glowAlpha * 0.6f));
            
            for (int i = 3; i >= 1; i--) {
                float fade = 1f - (i / 4f);
                g2.setColor(new Color(GLOW_COLOR.getRed(), GLOW_COLOR.getGreen(), GLOW_COLOR.getBlue(), (int)(100 * fade)));
                RoundRectangle2D glow = new RoundRectangle2D.Float(-i * 3, -i * 3, width + i * 6, height + i * 6, BORDER_RADIUS + i * 2, BORDER_RADIUS + i * 2);
                g2.fill(glow);
            }
            
            g2.setComposite(oldComposite);
        }
        
        private void drawButtonBackground(Graphics2D g2, int width, int height) {
            RoundRectangle2D button = new RoundRectangle2D.Float(0, 0, width - 1, height - 1, BORDER_RADIUS, BORDER_RADIUS);
            
            Color topColor, bottomColor, borderColor;
            if (isPressed) {
                topColor = PRESSED_COLOR;
                bottomColor = new Color(15, 50, 150);
                borderColor = new Color(80, 120, 200);
            } else if (isHovered) {
                topColor = HOVER_COLOR;
                bottomColor = new Color(30, 110, 200);
                borderColor = new Color(150, 200, 255);
            } else {
                topColor = PRIMARY_COLOR;
                bottomColor = new Color(20, 80, 180);
                borderColor = new Color(100, 150, 255);
            }
            
            GradientPaint gradient = new GradientPaint(0, 0, topColor, 0, height, bottomColor);
            g2.setPaint(gradient);
            g2.fill(button);
            
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(button);
        }
        
        private void draw3DEffect(Graphics2D g2, int width, int height) {
            Composite oldComposite = g2.getComposite();
            
            if (!isPressed) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.3f));
                g2.setColor(new Color(255, 255, 255));
                g2.fillRoundRect(3, 3, width - 6, height / 2 - 2, BORDER_RADIUS - 2, BORDER_RADIUS - 2);
                
                g2.setColor(new Color(0, 0, 0));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.15f));
                g2.fillRoundRect(3, height / 2 + 2, width - 6, height / 2 - 5, BORDER_RADIUS - 2, BORDER_RADIUS - 2);
            } else {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.15f));
                g2.setColor(new Color(0, 0, 0));
                g2.fillRoundRect(2, 2, width - 4, height - 4, BORDER_RADIUS, BORDER_RADIUS);
            }
            
            g2.setComposite(oldComposite);
        }
        
        private void drawText(Graphics2D g2, JComponent c, int width, int height) {
            String text = ((JButton) c).getText();
            Font font = c.getFont();
            FontMetrics fm = g2.getFontMetrics(font);
            
            int x = (width - fm.stringWidth(text)) / 2;
            int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
            
            g2.setColor(TEXT_SHADOW);
            g2.setFont(font);
            g2.drawString(text, x + 1, y + 2);
            
            g2.setColor(TEXT_COLOR);
            g2.drawString(text, x, y);
        }
    }
}

//================================================================================
// SETTINGS DIALOG - Volume control and game settings
//================================================================================

class SettingsDialog extends JDialog {
    private JSlider volumeSlider;
    private int currentVolume = 50;

    SettingsDialog(Frame owner) {
        super(owner, "Settings", true);
        this.setBackground(new Color(8, 8, 12));
        this.setUndecorated(false);
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(15, 15, 30));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(200, 220, 255));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        JLabel volumeLabel = new JLabel("Volume");
        volumeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        volumeLabel.setForeground(new Color(180, 200, 255));
        volumeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(volumeLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, currentVolume);
        volumeSlider.setMajorTickSpacing(10);
        volumeSlider.setMinorTickSpacing(1);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setBackground(new Color(15, 15, 30));
        volumeSlider.setForeground(new Color(100, 200, 255));
        volumeSlider.setFont(new Font("Arial", Font.PLAIN, 10));

        volumeSlider.addChangeListener(e -> {
            currentVolume = volumeSlider.getValue();
            SoundEngine.setVolume(currentVolume);
        });

        JPanel sliderPanel = new JPanel();
        sliderPanel.setBackground(new Color(15, 15, 30));
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.X_AXIS));
        sliderPanel.setMaximumSize(new Dimension(400, 50));
        sliderPanel.add(volumeSlider);
        mainPanel.add(sliderPanel);
        mainPanel.add(Box.createVerticalStrut(30));

        GameButton closeBtn = new GameButton("Done");
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.setMaximumSize(new Dimension(150, 40));
        closeBtn.addActionListener(e -> dispose());
        mainPanel.add(closeBtn);

        this.add(mainPanel);
        this.pack();
        this.setSize(450, 250);
        this.setLocationRelativeTo(owner);
    }

    public int getVolume() {
        return currentVolume;
    }
}

//================================================================================
// ABOUT DIALOG - Team information with background image
//================================================================================

class AboutDialog extends JDialog {
    private BufferedImage backgroundImage;

    AboutDialog(Frame owner) {
        super(owner, "About the Developers", true);
        this.setBackground(new Color(8, 8, 12));
        this.setUndecorated(false);
        this.setResizable(true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        try {
            File imageFile = new File("media/images/tetridokuempty.jpeg");
            if (!imageFile.exists()) {
                imageFile = new File("Tetridoku/media/images/tetridokuempty.jpeg");
            }
            if (!imageFile.exists()) {
                imageFile = new File("../media/images/tetridokuempty.jpeg");
            }
            if (imageFile.exists()) {
                backgroundImage = ImageIO.read(imageFile);
                System.out.println("About dialog background image loaded from: " + imageFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Could not load about dialog background image: " + e.getMessage());
        }

        AboutBackgroundPanel bgPanel = new AboutBackgroundPanel();
        bgPanel.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(0, 0, 0, 0));
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("About the Developers");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(80, 180, 255));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        JLabel groupLabel = new JLabel("Group Tree");
        groupLabel.setFont(new Font("Arial", Font.BOLD, 20));
        groupLabel.setForeground(new Color(150, 220, 150));
        groupLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(groupLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        JLabel descLabel = new JLabel("<html><body style='width: 650px; color: #D0D8FF;'>This game was created as our final project for our Data Structures and Algorithms course.</body></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        descLabel.setForeground(new Color(208, 216, 255));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(descLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        addTeamMember(mainPanel, "Manuelito Bermeo", "Came up with the original game concept and developed 50% of the core game code.");
        mainPanel.add(Box.createVerticalStrut(20));

        addTeamMember(mainPanel, "Rojan Ace Romaraog", "Supported the project's direction and focused on building the game menus.");
        mainPanel.add(Box.createVerticalStrut(20));

        addTeamMember(mainPanel, "Marc Joseph Soriano", "Contributed to the brainstorming process and helped with troubleshooting to ensure the game runs smoothly.");
        mainPanel.add(Box.createVerticalStrut(40));

        GameButton closeBtn = new GameButton("Done");
        closeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        closeBtn.setMaximumSize(new Dimension(150, 40));
        closeBtn.addActionListener(e -> dispose());
        mainPanel.add(closeBtn);

        mainPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        bgPanel.add(scrollPane, BorderLayout.CENTER);
        this.add(bgPanel);
        
        this.setSize(800, 650);
        this.setLocationRelativeTo(owner);
    }

    private class AboutBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            GradientPaint bgGradient = new GradientPaint(0, 0, new Color(8, 8, 16), 
                                                            w, h, new Color(20, 15, 40));
            g2.setPaint(bgGradient);
            g2.fillRect(0, 0, w, h);

            if (backgroundImage != null) {
                Image scaled = backgroundImage.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g2.drawImage(scaled, 0, 0, null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }

            Color[] pieceColors = {
                new Color(255, 100, 100),
                new Color(100, 255, 100),
                new Color(100, 100, 255),
                new Color(255, 255, 100),
                new Color(255, 100, 255),
                new Color(100, 255, 255),
                new Color(255, 150, 100)
            };
            
            int blockSize = h / 7;
            for (int i = 0; i < 7; i++) {
                g2.setColor(pieceColors[i]);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                g2.fillRect(0, i * blockSize, 15, blockSize);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
            g2.setColor(new Color(80, 180, 255));
            g2.fillRect(w - 10, 0, 10, h);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            g2.setColor(new Color(0, 0, 0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.fillRect(0, 0, w, h);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    private void addTeamMember(JPanel panel, String name, String description) {
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(new Color(100, 210, 255));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(nameLabel);

        JLabel descLabel = new JLabel("<html><body style='width: 700px; margin-top: 5px; color: #C8D8FF;'>" + description + "</body></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        descLabel.setForeground(new Color(200, 216, 255));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(descLabel);
    }
}

//================================================================================
// TUTORIAL DIALOG - Game instructions with multiple pages
//================================================================================

class TutorialDialog extends JDialog {
    private boolean play = false;
    private int currentPage = 0;
    private static final int PAGES = 4;
    private TutorialPanel tutPanel;

    TutorialDialog(Frame owner) {
        super(owner, "How to Play TetriDoku", true);
        this.setBackground(new Color(8, 8, 12));
        this.setUndecorated(false);
        this.setResizable(false);
        this.tutPanel = new TutorialPanel(this);
        this.tutPanel.setPreferredSize(new Dimension(720, 480));
        this.add(this.tutPanel, "Center");
        
        JPanel btnBar = new JPanel(new FlowLayout(1, 16, 10));
        btnBar.setBackground(new Color(8, 8, 12));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 60)));
        
        GameButton prevBtn = this.makeBtn("\u25c0  Back");
        GameButton nextBtn = this.makeBtn("Next  \u25b6");
        GameButton playBtn = this.makeBtn("\u25b6  PLAY NOW!");
        GameButton skipBtn = this.makeBtn("Skip Tutorial");
        
        prevBtn.addActionListener(e -> {
            if (this.currentPage > 0) {
                --this.currentPage;
                this.tutPanel.repaint();
                this.updateButtons(prevBtn, nextBtn, playBtn);
            }
        });
        nextBtn.addActionListener(e -> {
            if (this.currentPage < 3) {
                ++this.currentPage;
                this.tutPanel.repaint();
                this.updateButtons(prevBtn, nextBtn, playBtn);
            }
        });
        playBtn.addActionListener(e -> {
            this.play = true;
            this.dispose();
        });
        skipBtn.addActionListener(e -> {
            this.play = true;
            this.dispose();
        });
        
        btnBar.add(prevBtn);
        btnBar.add(nextBtn);
        btnBar.add(playBtn);
        btnBar.add(skipBtn);
        this.add(btnBar, "South");
        this.updateButtons(prevBtn, nextBtn, playBtn);
        this.pack();
        this.setLocationRelativeTo(null);
    }

    private GameButton makeBtn(String label) {
        return new GameButton(label);
    }

    private void updateButtons(GameButton prev, GameButton next, GameButton play) {
        prev.setEnabled(this.currentPage > 0);
        next.setVisible(this.currentPage < 3);
        play.setVisible(this.currentPage == 3);
    }

    boolean shouldPlay() {
        return this.play;
    }

    private class TutorialPanel extends JPanel {
        private BufferedImage backgroundImage;

        TutorialPanel(TutorialDialog dialog) {
            this.setBackground(new Color(10, 10, 16));
            
            try {
                File imageFile = new File("media/images/tetridokuempty.jpeg");
                if (!imageFile.exists()) {
                    imageFile = new File("Tetridoku/media/images/tetridokuempty.jpeg");
                }
                if (!imageFile.exists()) {
                    imageFile = new File("../media/images/tetridokuempty.jpeg");
                }
                if (imageFile.exists()) {
                    backgroundImage = ImageIO.read(imageFile);
                    System.out.println("Tutorial background image loaded from: " + imageFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Could not load tutorial background image: " + e.getMessage());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            
            GradientPaint bgGradient = new GradientPaint(0, 0, new Color(10, 10, 16), 
                                                         w, h, new Color(20, 15, 35));
            g2.setPaint(bgGradient);
            g2.fillRect(0, 0, w, h);

            if (backgroundImage != null) {
                Image scaled = backgroundImage.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.drawImage(scaled, 0, 0, null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }

            Color[] pieceColors = {
                new Color(255, 100, 100),
                new Color(100, 255, 100),
                new Color(100, 100, 255),
                new Color(255, 255, 100),
                new Color(255, 100, 255),
                new Color(100, 255, 255),
                new Color(255, 150, 100)
            };
            
            int blockSize = h / 7;
            for (int i = 0; i < 7; i++) {
                g2.setColor(pieceColors[i]);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                g2.fillRect(0, i * blockSize, 20, blockSize);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }

            g2.setColor(new Color(0, 0, 0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.fillRect(0, 0, w, h);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            int panelX = 28;
            int panelY = 26;
            int panelW = w - 56;
            int panelH = h - 52;
            
            GradientPaint panelGradient = new GradientPaint(
                panelX, panelY, new Color(25, 28, 45),
                panelX, panelY + panelH, new Color(35, 40, 60)
            );
            g2.setPaint(panelGradient);
            g2.fillRoundRect(panelX, panelY, panelW, panelH, 16, 16);
            
            g2.setColor(new Color(80, 120, 200));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(panelX - 2, panelY - 2, panelW + 4, panelH + 4, 18, 18);
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2.setColor(new Color(100, 150, 220));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(panelX, panelY, panelW, panelH, 16, 16);

            GradientPaint accentGradient = new GradientPaint(
                panelX, panelY, new Color(80, 150, 255),
                panelX, panelY + 60, new Color(25, 28, 45)
            );
            g2.setPaint(accentGradient);
            g2.fillRoundRect(panelX + 4, panelY + 4, panelW - 8, 55, 12, 12);

            String[] titles = {
                "Welcome to TetriDoku",
                "Controls",
                "Sudoku Rules",
                "Scoring"
            };
            String[] body = {
                "Stack tetrominoes with digits 1-9.\\nClear unique rows and complete 3x3 boxes.",
                "Left/Right: Move\\nUp or Z: Rotate\\nSpace: Hard drop\\nQ/E: Rotate piece digits",
                "A full row clears only cells with unique digits.\\nDuplicate digits are conflicts and stay.",
                "Unique clear: 40 x level per cell\\n3x3 box clear: 1000 x level\\nSpeed increases each level"
            };

            int page = Math.max(0, Math.min(currentPage, PAGES - 1));
            
            g2.setFont(new Font("Arial Black", Font.BOLD, 28));
            g2.setColor(new Color(0, 0, 0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2.drawString(titles[page], 55, 88);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            
            g2.setColor(new Color(220, 240, 255));
            g2.drawString(titles[page], 52, 85);

            g2.setFont(new Font("Arial", Font.PLAIN, 17));
            g2.setColor(new Color(0, 0, 0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            int y = 134;
            for (String line : body[page].split("\\\\n")) {
                g2.drawString(line, 55, y);
                y += 30;
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            
            g2.setColor(new Color(190, 210, 240));
            y = 132;
            for (String line : body[page].split("\\\\n")) {
                g2.drawString(line, 52, y);
                y += 30;
            }

            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(new Color(0, 0, 0));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            g2.drawString("Page " + (page + 1) + " / " + PAGES, 55, h - 44);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            
            g2.setColor(new Color(120, 150, 200));
            g2.drawString("Page " + (page + 1) + " / " + PAGES, 52, h - 46);
        }
    }
}
