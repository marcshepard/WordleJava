import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


// A GUI for the wordle app
public class Gui extends JFrame implements KeyListener, ActionListener {
    private WordleEntryPanel wordlePanel;       // Main grid for entering guesses
    private ScrollableMessageBox messageBox;    // A place to give the user feedback text
    private WordleUserGame game;                // Underlying game engine
    private boolean gameOver = false;
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
        ColorScheme.setColorblindMode (persistedState.colorBlindMode);

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

        startGame();
        
        setFocusable(true);
        requestFocus();
        addKeyListener(this);

        setVisible(true);
    }

    private void startGame() {
        game = new WordleUserGame(persistedState.hardMode);    // Create a new game
        wordlePanel.reset();                    // Reset the entry screen
        messageBox.setText("");            // Clear any messages
        buttons[NEW_GAME].setEnabled(false);  // Disable the "New Game" button while game is in progress
    }

    public static void main (String[] args) {
        new Gui();
    }

    // Action listener for button press events
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        if (button == buttons[NEW_GAME]) { // New Game button
            startGame();
            gameOver = false;
        } else if (button == buttons[HINT]) {
            messageBox.setText(game.getHint(wordlePanel.getWord()));
        } else if (button == buttons[STATS]) {
            messageBox.setText(persistedState.getStatsString());
        } else if (button == buttons[CHEAT]) {
            messageBox.setText("TODO - implement cheat");
        } else if (button == buttons[SETTINGS]) {
            messageBox.setText("TODO - implement settings");
        }
        requestFocus();
    }

    // Key listener events - we just use KeyPressed (ignore KeyReleased and KeyTyped)
    public void keyPressed(KeyEvent e) {
        if (gameOver)
            return;
    
        messageBox.setText("");
        int code = e.getKeyCode();
        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z)
            wordlePanel.setChar(KeyEvent.getKeyText(code));

        if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE)
            wordlePanel.back();

        if (code == KeyEvent.VK_ENTER) {
            String pattern = game.guess(wordlePanel.getWord());
            if (pattern == null) {
                messageBox.setText("Invalid guess");
                return;
            }
            for (int i = 0; i < pattern.length(); i++) {
                switch (pattern.charAt(i)) {
                    case WordleUserGame.MATCH:
                        wordlePanel.setColor(i, ColorScheme.matchColor);
                        break;
                    case WordleUserGame.CLOSE:
                        wordlePanel.setColor(i, ColorScheme.closeColor);
                        break;
                    case WordleUserGame.MISS:
                        wordlePanel.setColor(i, ColorScheme.missColor);
                        break;
                    default:
                        System.err.println ("Unknown pattern character " + pattern.charAt(i) + " in pattern string " + pattern);
                }
            }
            if (pattern.equals(WordleUserGame.WINNING_PATTERN)) {
                gameOver = true;
                messageBox.setText("You won!");
                persistedState.gameResults[game.getTurn()]++;
                persistedState.save();
                buttons[NEW_GAME].setEnabled(true);
            } else if (game.getTurn() > 6) {
                gameOver = true;
                messageBox.setText("You lost! The answer was " + game.getAnswer());
                persistedState.gameResults[0]++;
                persistedState.save();
                buttons[NEW_GAME].setEnabled(true);
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
class WordleEntryPanel extends JPanel {
    private static final int ROWS = 6;
    private static final int COLS = 5;
    private JButton grid[][] = new JButton[ROWS][COLS];
    private int currentRow;
    private int currentCol;
    private Font font;

    WordleEntryPanel() {
        GridLayout l = new GridLayout(ROWS, COLS, 2, 2);
        setLayout(l);

        font = new Font ("Arial", Font.PLAIN, 36);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                JButton b = new JButton();
                grid[r][c] = b;
                add(b);
                b.setBackground(ColorScheme.backgroundColor);
                b.setForeground(ColorScheme.foregroundColor);
                b.setEnabled(false);
                b.setFont(font);
            }
        }

        reset();
    }

    void reset () {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c].setText("");
                grid[r][c].setBackground(ColorScheme.backgroundColor);
                if (r != 0)
                    grid[r][c].setVisible(false);
            }
        }
        currentCol = 0;
        currentRow = 0;
    }

    // Enter a character at the current position and move forward one column
    void setChar (String letter) {
        letter = letter.toUpperCase();
        if (currentCol < COLS && letter.length() == 1 && letter.compareTo("A") >= 0 && letter.compareTo("Z") <= 0) {
            grid[currentRow][currentCol].setText(letter);
            currentCol++;
        }
    }

    // Remove the current position back one column, erasing that letter
    void back () {
        if (currentCol > 0)
            grid[currentRow][--currentCol].setText("");
    }

    // Start a new row
    void nextRow() {
        if (currentRow < ROWS - 1 && currentCol >= COLS && getWord().length() == COLS) {
            currentRow++;
            currentCol = 0;
        }
        for (int c = 0; c < COLS; c++)
            grid[currentRow][c].setVisible(true);
    }

    // Get the word at the current row
    String getWord() {
        String word = "";
        for (int i = 0; i < currentCol; i++) {
            word += grid[currentRow][i].getText();
        }
        return word;
    }

    // Set the color of the letter in column col based on the users guess
    void setColor(int col, Color color) {
        grid[currentRow][col].setBackground(color);
    }
}

// A class to use for providing the end-user with text feedback
class ScrollableMessageBox extends JScrollPane {
    private JTextArea textArea;

    ScrollableMessageBox() {
        textArea = new JTextArea();
        textArea.setBackground(ColorScheme.backgroundColor);
        textArea.setForeground(ColorScheme.foregroundColor);
        textArea.setEnabled(false);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);  
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setViewportView (textArea);
    }

    void setText (String text) {
        textArea.setText(text);
    }
}

class ColorScheme {
    static Color backgroundColor = Color.BLACK;
    static Color foregroundColor = Color.WHITE;
    static Color matchColor = Color.GREEN;
    static Color closeColor = Color.YELLOW;
    static Color missColor = Color.DARK_GRAY;

    static void setColorblindMode (boolean enabled) {
        if (enabled) {
            matchColor = Color.ORANGE;
            closeColor = Color.BLUE;
        } else {
            matchColor = Color.GREEN;
            closeColor = Color.YELLOW;
        }
    }
}

// PersistedState - state we persist across games
class PersistedState implements Serializable {
    boolean hardMode;
    boolean colorBlindMode = true;
    int[] gameResults = new int[7]; // Each index has count of games won in that many turns (index 0 is for lost games)
    private static String fileName = "GameState.ser";
  
    // Save state to GameState.ser
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
  
    // Returns GameState - serialized from GameState.ser if possible, else
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

        String statsString = String.format ("Total games: %d \n", numGames);
        statsString += String.format ("Games lost: %d (%d%%)\n", gameResults[0], 100*gameResults[0]/numGames);
        statsString += String.format ("Games won: %d\n", numGames - gameResults[0]);
        for (int ix = 1; ix < gameResults.length; ix++) {
            statsString += String.format ("\t%d:\t%d\n", ix, gameResults[ix]);
        }

        return statsString;
    }
  }
