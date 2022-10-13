import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

// A GUI for the wordle app
public class Gui extends JFrame implements KeyListener, ActionListener {
    private WordleEntryPanel wordlePanel;       // Main grid for entering guesses
    private ScrollableMessageBox messageBox;    // A place to give the user feedback text
    private GameEngine game;                    // Underlying game engine
    private JButton[] buttons;
    private PersistedState persistedState;
    private static final String[] buttonNames = {"New Game", "Hint", "Stats", "Cheat", "Settings"};
    private final static int NEW_GAME=0;
    private final static int HINT=1;
    private final static int STATS=2;
    private final static int CHEAT=3;
    private final static int SETTINGS=4;

    Gui () {
        setTitle("Yet Another Wordle App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 800);
        setBackground(ColorScheme.backgroundColor);

        persistedState = PersistedState.restore();

        JPanel buttonPanel = new JPanel();
        buttons = new JButton[buttonNames.length];
        for (int i = 0; i < buttonNames.length; i++) {
            buttons[i] = new JButton(buttonNames[i]);
            buttons[i].addActionListener(this);
            buttonPanel.add (buttons[i]);
        }
        buttonPanel.setBackground(ColorScheme.backgroundColor);
        buttonPanel.setPreferredSize(new Dimension (getWidth(), getHeight()/10));
        add (buttonPanel, BorderLayout.NORTH);

        wordlePanel = new WordleEntryPanel();
        wordlePanel.setBackground(ColorScheme.backgroundColor);
        add(wordlePanel, BorderLayout.CENTER);

        messageBox = new ScrollableMessageBox();
        messageBox.setPreferredSize(new Dimension (getWidth(), getHeight()/5));
        add (messageBox, BorderLayout.SOUTH);

        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension (getWidth()/10, getHeight()/8));
        p.setBackground(ColorScheme.backgroundColor);
        add (p, BorderLayout.EAST);
        p = new JPanel();
        p.setPreferredSize(new Dimension (getWidth()/10, getHeight()/8));
        p.setBackground(ColorScheme.backgroundColor);
        add (p, BorderLayout.WEST);

        startGame(false);
        
        setFocusable(true);
        requestFocus();
        addKeyListener(this);

        setVisible(true);

        messageBox.setText ("Just start typing to play a wordle game\n" +
            "For a progressive series of hints, click hint\n" + 
            "A hint before the first word is entered gives you insights to opening words\n" +
            "If you type a word and don't hit enter, hint will also analyze your word for comparison\n" +
            "Finally; cheat mode lets you cheat on the real wordle by entering the words you guessed and the colors you got back, and then you can request hints (kind of like a live wordlebot)\n" +
            "Good luck and enjoy"
        );
    }

    private void startGame(boolean cheatMode) {
        game = new GameEngine(persistedState.hardMode, cheatMode);  // Create a new game
        wordlePanel.reset(cheatMode);                     // Reset the entry screen
        messageBox.setText("");                     // Clear any messages
    }

    public static void main (String[] args) {
        new Gui();
    }

    // Action listener for button press events
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        if (button == buttons[NEW_GAME]) { // New Game button
            if (!game.isGameOver()) {
                messageBox.setText ("Finish your current game before starting a new one");
            } else {
                startGame(false);
            }
        } else if (button == buttons[HINT]) {
            messageBox.setText(game.getHint(wordlePanel.getWord()));
        } else if (button == buttons[STATS]) {
            messageBox.setText(persistedState.getStatsString());
        } else if (button == buttons[CHEAT]) {
            if (!game.isGameOver() && game.getTurn() > 1 && !game.getCheatMode()) {
                messageBox.setText ("Finish your current game before starting a cheat mode game");
            } else {
                startGame(true);
                messageBox.setText ("Starting a new game in cheat mode\n" +
                    "Each turn, enter the word you gave wordle and the colors it returned\n" +
                    "You change colors by clicking the letters; all colors must be configured before you can hit enter"
                );
            }
        } else if (button == buttons[SETTINGS]) {
            JDialog d = new JDialog(this, "Settings", true);
            d.setLayout(new FlowLayout());  
            JButton b = new JButton ("Toggle hard mode");
            b.addActionListener (this);
            d.add (new JLabel ("Click to turn 'hard mode' " + (persistedState.hardMode ? "off" : "on") + " and reset your stats"));
            d.add (b);
            d.setSize(400, 100);
            d.setVisible(true);
        } else if (button.getText().equals ("Toggle hard mode")) {
            persistedState.hardMode = !persistedState.hardMode;
            persistedState.gameResults = new int[7];
            messageBox.setText("Hard mode is now " + (persistedState.hardMode ? "on" : "off"));
            persistedState.save();
            JDialog dialog = (JDialog) SwingUtilities.getRoot(button);
            dialog.dispose();
        }
        requestFocus();
    }

    // Key listener events - we just use KeyPressed (ignore KeyReleased and KeyTyped)
    public void keyPressed(KeyEvent e) {
        if (game.isGameOver())
            return;
    
        //messageBox.setText("");
        int code = e.getKeyCode();
        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z)
            wordlePanel.setChar(KeyEvent.getKeyText(code));

        if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE)
            wordlePanel.back();

        if (code == KeyEvent.VK_ENTER && !game.getCheatMode()) {
            String pattern = game.guess(wordlePanel.getWord());

            if (pattern == null) {
                messageBox.setText("Invalid guess");
                return;
            }
            for (int i = 0; i < pattern.length(); i++) {
                switch (pattern.charAt(i)) {
                    case GameEngine.MATCH:
                        wordlePanel.setColor(i, ColorScheme.matchColor);
                        break;
                    case GameEngine.CLOSE:
                        wordlePanel.setColor(i, ColorScheme.closeColor);
                        break;
                    case GameEngine.MISS:
                        wordlePanel.setColor(i, ColorScheme.missColor);
                        break;
                    default:
                        System.err.println ("Unknown pattern character " + pattern.charAt(i) + " in pattern string " + pattern);
                }
            }
            if (pattern.equals(GameEngine.WINNING_PATTERN)) {
                messageBox.setText("You won!");
                persistedState.gameResults[game.getTurn()]++;
                persistedState.save();
            } else if (game.getTurn() > 6) {
                messageBox.setText("You lost! The answer was " + game.getAnswer());
                persistedState.gameResults[0]++;
                persistedState.save();
            } else {
                wordlePanel.nextRow();
            }
        }

        if (code == KeyEvent.VK_ENTER && game.getCheatMode()) {
            // Ignore enter if they have not typed in all the letters of their guess yet
            String guess = wordlePanel.getWord();
            if (guess.length() < 5) {
                return;
            }
            
            if (!game.isValidGuess(guess)) {
                messageBox.setText("Invalid guess");
            }
            
            String pattern = wordlePanel.getPattern();
            if (pattern == null) {
                messageBox.setText("Letter colors need to be set (click to set)");
                return;
            }

            if (!game.guess(guess, pattern)) {
                messageBox.setText("Invalid word/color combination");
                return;
            }

            messageBox.setText("");

            if (pattern.equals(GameEngine.WINNING_PATTERN)) {
                messageBox.setText("Congrats (sort of, as you did cheat)");
            } else if (game.getTurn() > 6) {
                messageBox.setText("I can't beleive you lost even with my help!");
            } else {
                wordlePanel.nextRow();
            }
        }
    }

    // Unused Key Listener events
    public void keyTyped(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
}


// A panel representing a 6x6 grid for entering Wordle guesses
// Only the current and previous rows are visible
class WordleEntryPanel extends JPanel implements ActionListener {
    private static final int ROWS = 6;
    private static final int COLS = 5;
    private static Font font = new Font ("Arial", Font.PLAIN, 36);
    private JButton grid[][] = new JButton[ROWS][COLS];
    private int currentRow;
    private int currentCol;
    private boolean cheatMode;

    WordleEntryPanel() {
        GridLayout l = new GridLayout(ROWS, COLS, 2, 2);
        setLayout(l);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                JButton b = new JButton();
                grid[r][c] = b;
                add(b);
                b.setBackground(ColorScheme.backgroundColor);
                b.setForeground(ColorScheme.foregroundColor);
                b.setEnabled(false);
                if (r != 0) {
                    b.setVisible(false);
                }
                b.setFont(font);
                b.setName(String.format("%d%d", r, c));
                b.addActionListener(this);
            }
        }
    }

    // Reset the panel to start a new game in either regular or cheat mode
    void reset (boolean cheatMode) {
        this.cheatMode = cheatMode;
        for (int r = 0; r < ROWS; r++) {
            for (JButton b : grid[r]) {
                b.setBackground(ColorScheme.backgroundColor);
                b.setText("");
                b.setEnabled(cheatMode && r == 0);
                b.setVisible(r == 0);
            }
        }
    }

    // Enter a character at the current position and move forward one column
    void setChar (String letter) {
        letter = letter.toUpperCase();
        if (currentCol < COLS && letter.length() == 1 && letter.compareTo("A") >= 0 && letter.compareTo("Z") <= 0) {
            grid[currentRow][currentCol].setText(letter);
            currentCol++;
        }
    }

    // Move the current position back one column, erasing the previous letter
    void back() {
        if (currentCol > 0)
            grid[currentRow][--currentCol].setText("");
    }

    // Start a new row
    void nextRow() {
        if (currentRow < ROWS - 1 && currentCol >= COLS && getWord().length() == COLS) {
            currentRow++;
            for (int c = 0; c < COLS; c++) {
                grid[currentRow][c].setVisible(true);
                grid[currentRow][c].setEnabled(cheatMode);
                grid[currentRow-1][c].setEnabled(false);
            }
            currentCol = 0;
        }
    }

    // Get the word at the current row
    String getWord() {
        String word = "";
        for (int i = 0; i < currentCol; i++) {
            word += grid[currentRow][i].getText();
        }
        return word;
    }

    // Get the color pattern of the current row
    String getPattern() {
        String pattern = "";
        for (int c = 0; c < COLS; c++) {
            if (grid[currentRow][c].getBackground() == ColorScheme.missColor) {
                pattern += GameEngine.MISS;
            } else if (grid[currentRow][c].getBackground() == ColorScheme.closeColor) {
                pattern += GameEngine.CLOSE;
            } else if (grid[currentRow][c].getBackground() == ColorScheme.matchColor) {
                pattern += GameEngine.MATCH;
            } else {
                return null;    // One of the rows colors has not been set
            }
        }

        return pattern;
    }

    // Set the color of the letter in column col based on the users guess
    void setColor(int col, Color color) {
        grid[currentRow][col].setBackground(color);
    }

    // Action listener for button press events
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        int row = button.getName().charAt(0) - '0';
        if (row == currentRow && button.getText().length() > 0) {
            if (button.getBackground().equals(ColorScheme.missColor)) {
                button.setBackground(ColorScheme.closeColor);
            } else if (button.getBackground().equals(ColorScheme.closeColor)) {
                button.setBackground(ColorScheme.matchColor);
            } else {
                button.setBackground(ColorScheme.missColor);
            }
        }
        
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        topFrame.requestFocus();
    }
}

// A class to use for providing the end-user with text feedback
class ScrollableMessageBox extends JScrollPane {
    private static final Font font = new Font ("Arial", Font.PLAIN, 14);
    private JTextArea textArea;

    ScrollableMessageBox() {
        textArea = new JTextArea();
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        textArea.setBackground(ColorScheme.backgroundColor);
        textArea.setForeground(ColorScheme.foregroundColor);
        textArea.setFont(font);
        textArea.setEnabled(false);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);  
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setViewportView (textArea);
    }

    void setText (String text) {
        textArea.setText(text);
        textArea.setCaretPosition(0);
    }
}

class ColorScheme {
    static Color backgroundColor = Color.BLACK;
    static Color foregroundColor = Color.WHITE;
    static Color matchColor = new Color (0, 200, 0); // dark green (easier for color blind folks like me)
    static Color closeColor = Color.YELLOW;
    static Color missColor = Color.DARK_GRAY;
}

// PersistedState - state we persist across games
class PersistedState implements Serializable {
    boolean hardMode;
    int[] gameResults = new int[7]; // Each index has count of games won in that many turns (index 0 is for lost games)
    private static String fileName = System.getProperty("user.home") + File.separatorChar + "WordleAppState.ser";
  
    // Save state to file
    boolean save () {      
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
            fos.close();
            return true;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }
    }
  
    // Returns GameState - serialized from saved file if possible, else
    // create a new default GameState object 
    static PersistedState restore () {      
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            PersistedState s = (PersistedState) ois.readObject();
            ois.close();
            fis.close();
            return s;
        } catch(Exception e) { // IOException, ClassNotFoundException
            return new PersistedState();
        }
    }

    String getStatsString () {
        int numGames = 0;
        for (int games : gameResults) {
            numGames += games;
        }
        if (numGames == 0) {
            return "No games have been played";
        }

        int gamesWon = numGames - gameResults[0];
        String statsString = String.format ("Games won: %d out of %d (%d%%)\n", gamesWon, numGames, 100*gamesWon/numGames);
        for (int ix = 1; ix < gameResults.length; ix++) {
            statsString += String.format ("\t%d:\t%d\n", ix, gameResults[ix]);
        }
        int numTurns = 0;
        numTurns += gameResults[0]*7; // Count losses as 7 turns
        for (int i = 1; i < gameResults.length; i++)
            numTurns += gameResults[i]*i; 
        statsString += String.format ("Average # turns (counting losses as 7 turns): %.1f\n", (double)numTurns/numGames);

        return statsString;
    }
  }
