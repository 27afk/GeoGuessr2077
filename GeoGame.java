/*
location 1
location 2 kabuki centrtal
location 3 jig jig street    
*/


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections; // Added for shuffling
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoGame extends JFrame {
    private CardLayout cardLayout = new CardLayout();  
    private JPanel mainContainer = new JPanel(cardLayout);
    
    private int currentRound = 0;
    private int totalScore = 0;
    private Location currentLoc;
    private ArrayList<Location> levelList = new ArrayList<>();

    private PanoramaPanel panoramaDisplay = new PanoramaPanel();
    private JLabel roundResultText = new JLabel("", SwingConstants.CENTER);
    private JLabel finalScoreLabel = new JLabel("", SwingConstants.CENTER);

    public GeoGame() {
        setTitle("Java GeoGuessr - Cyberpunk Edition");
        setSize(1000, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Load all 10 locations from your JSON config
        loadLocationsFromJson("Locations.json");

        setupMenu();
        setupGameScreen();
        setupResultsScreen();

        add(mainContainer);
        setLocationRelativeTo(null);
    }

    private void loadLocationsFromJson(String filePath) {
        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                jsonContent.append(line);
            }
            
            // Regex to parse the JSON objects cleanly without external library jars
            Pattern pattern = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"x\"\\s*:\\s*(\\d+)\\s*,\\s*\"y\"\\s*:\\s*(\\d+)\\s\\}");
            Matcher matcher = pattern.matcher(jsonContent.toString());
            
            while (matcher.find()) {
                String name = matcher.group(1);
                int x = Integer.parseInt(matcher.group(2));
                int y = Integer.parseInt(matcher.group(3));
                levelList.add(new Location(name, x, y));
            }
            System.out.println("Loaded " + levelList.size() + " positions from " + filePath);
            
        } catch (Exception e) {
            System.err.println("Could not read JSON file, loading hardcoded fallback locations: " + e.getMessage());
            levelList.add(new Location("Location1", 728, 360)); 
            levelList.add(new Location("Location2", 150, 40));
        }
    }

    private void setupMenu() {
        JPanel menuPanel = new JPanel(new GridBagLayout());
        JLabel title = new JLabel("JAVA GEOGUESSR");
        title.setFont(new Font("Arial", Font.BOLD, 40));
        
        JButton startBtn = new JButton("START GAME");
        startBtn.addActionListener(e -> startGame());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.gridy = 0; menuPanel.add(title, gbc);
        gbc.gridy = 1; menuPanel.add(startBtn, gbc);

        mainContainer.add(menuPanel, "MENU");
    }

    private void setupGameScreen() {
        JPanel gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(panoramaDisplay, BorderLayout.CENTER);

        JPanel bottomUI = new JPanel(new BorderLayout());
        JPanel navButtons = new JPanel();
        
        JButton left = new JButton("<< Pan Left");
        JButton right = new JButton("Pan Right >>");

        Timer panTimer = new Timer(15, null);
        
        left.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                for(var al : panTimer.getActionListeners()) panTimer.removeActionListener(al);
                panTimer.addActionListener(ae -> panoramaDisplay.updateScroll(-5));
                panTimer.start();
            }
            public void mouseReleased(MouseEvent e) { panTimer.stop(); }
        });

        right.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                for(var al : panTimer.getActionListeners()) panTimer.removeActionListener(al);
                panTimer.addActionListener(ae -> panoramaDisplay.updateScroll(5));
                panTimer.start();
            }
            public void mouseReleased(MouseEvent e) { panTimer.stop(); }
        });
        
        navButtons.add(left); 
        navButtons.add(right);

        JPanel mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(20, 20, 30));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.CYAN);
                g.drawRect(0, 0, getWidth()-1, getHeight()-1);
                g.drawString("CLICK TO GUESS", 100, 90);
            }
        };
        mapPanel.setPreferredSize(new Dimension(300, 200));
        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { 
                panTimer.stop();
                handleGuess(e.getX(), e.getY()); 
            }
        });

        bottomUI.add(navButtons, BorderLayout.WEST);
        bottomUI.add(mapPanel, BorderLayout.EAST);
        gamePanel.add(bottomUI, BorderLayout.SOUTH);
        mainContainer.add(gamePanel, "GAME");
    }

    private void setupResultsScreen() {
        JPanel resultsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        roundResultText.setFont(new Font("Arial", Font.PLAIN, 24));
        finalScoreLabel.setFont(new Font("Arial", Font.BOLD, 32));

        JButton nextBtn = new JButton("CONTINUE");
        nextBtn.addActionListener(e -> {
            currentRound++;
            if (currentRound < levelList.size()) {
                loadRound();
                cardLayout.show(mainContainer, "GAME");
            } else {
                cardLayout.show(mainContainer, "MENU");
            }
        });

        gbc.gridy = 0; resultsPanel.add(roundResultText, gbc);
        gbc.gridy = 1; resultsPanel.add(finalScoreLabel, gbc);
        gbc.gridy = 2; resultsPanel.add(nextBtn, gbc);
        mainContainer.add(resultsPanel, "RESULTS");
    }

    private void startGame() {
        currentRound = 0;
        totalScore = 0;
        
        // Randomizes the order of the locations loaded from your JSON file
        Collections.shuffle(levelList);
        
        loadRound();
        cardLayout.show(mainContainer, "GAME");
    }

    private void loadRound() {
        currentLoc = levelList.get(currentRound);
        panoramaDisplay.setImage("Pictures/" + currentLoc.imgPrefix + ".png");
    }

    private void handleGuess(int x, int y) {
        double dist = Math.sqrt(Math.pow(x - currentLoc.mapX, 2) + Math.pow(y - currentLoc.mapY, 2));
        int points = Math.max(0, 5000 - (int)dist * 15);
        totalScore += points;
        roundResultText.setText("You were " + (int)dist + "px away!");
        finalScoreLabel.setText("Current Total: " + totalScore + " pts");
        cardLayout.show(mainContainer, "RESULTS");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GeoGame().setVisible(true));
    }
}

class PanoramaPanel extends JPanel {
    private Image img;
    private int scrollOffset = 0;

    public PanoramaPanel() { setDoubleBuffered(true); }

    public void setImage(String path) {
        this.img = new ImageIcon(path).getImage();
        this.scrollOffset = 0;
        repaint();
    }

    public void updateScroll(int delta) {
        if (img == null) return;
        int imgWidth = img.getWidth(null);
        if (imgWidth <= 0) return;
        
        scrollOffset = (scrollOffset + delta + imgWidth) % imgWidth;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (img == null || img.getWidth(null) <= 0) {
            g.drawString("Loading image assets...", 20, 20);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int imgWidth = img.getWidth(null);
        int panelHeight = getHeight();

        g2d.drawImage(img, -scrollOffset, 0, imgWidth, panelHeight, null);
        g2d.drawImage(img, imgWidth - scrollOffset, 0, imgWidth, panelHeight, null);
    }
}

class Location {
    String imgPrefix;
    int mapX, mapY;
    public Location(String p, int x, int y) { this.imgPrefix = p; this.mapX = x; this.mapY = y; }
}