import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


// A GUI for the wordle app
public class Gui extends JFrame implements KeyListener, ActionListener {
    private WordleEntryPanel wordlePanel;       // Main grid for entering guesses
    private MessageBox messageBox;              // A place to give the user feedback text
    private WordleUserGame game;                // Underlying game engine
    private boolean gameOver = false;
    private JButton[] buttons;
    private static final String[] buttonNames = {"New Game", "Hint", "Stats", "Cheat", "Settings"};
    private final static int NEW_GAME=0;
    private final static int HINT=1;
    private final static int STATS=2;
    private final static int CHEAT=3;
    private final static int SETTINGS=4;

    // TODO - make these configurable in settings (hard mode and color blind mode)
    private boolean hardMode = false;
    private Color backgroundColor = Color.BLACK;
    private Color forgroundColor = Color.WHITE;
    private Color matchColor = Color.ORANGE;        // Color.GREEN;
    private Color closeColor = Color.BLUE;          // Color.YELLOW;
    private Color missColor = Color.DARK_GRAY;

    Gui () {
        setTitle("Yet Another Wordle App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 800);
        setBackground(backgroundColor);

        JPanel buttonPanel = new JPanel();
        buttons = new JButton[buttonNames.length];
        for (int i = 0; i < buttonNames.length; i++) {
            buttons[i] = new JButton(buttonNames[i]);
            buttons[i].addActionListener(this);
            buttonPanel.add (buttons[i]);
        }
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setPreferredSize(new Dimension (getWidth(), getHeight()/10));
        add (buttonPanel, BorderLayout.NORTH);

        wordlePanel = new WordleEntryPanel();
        wordlePanel.setBackground(Color.BLACK);
        add(wordlePanel, BorderLayout.CENTER);

        messageBox = new MessageBox();
        messageBox.setBackground(backgroundColor);
        messageBox.setForeground(forgroundColor);
        messageBox.setPreferredSize(new Dimension (getWidth(), getHeight()/5));
        messageBox.setEnabled(false);
        add (messageBox, BorderLayout.SOUTH);

        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension (getWidth()/10, getHeight()/8));
        p.setBackground(backgroundColor);
        add (p, BorderLayout.EAST);
        p = new JPanel();
        p.setPreferredSize(new Dimension (getWidth()/10, getHeight()/8));
        p.setBackground(backgroundColor);
        add (p, BorderLayout.WEST);

        startGame();
        
        setFocusable(true);
        requestFocus();
        addKeyListener(this);

        setVisible(true);
    }

    private void startGame() {
        game = new WordleUserGame(hardMode);    // Create a new game
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
            messageBox.setText("TODO - implement stats");
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
                        wordlePanel.setColor(i, matchColor);
                        break;
                    case WordleUserGame.CLOSE:
                        wordlePanel.setColor(i, closeColor);
                        break;
                    case WordleUserGame.MISS:
                        wordlePanel.setColor(i, missColor);
                        break;
                    default:
                        System.err.println ("Unknown pattern character " + pattern.charAt(i) + " in pattern string " + pattern);
                }
            }
            if (pattern.equals(WordleUserGame.WINNING_PATTERN)) {
                gameOver = true;
                messageBox.setText("You won!");
                buttons[NEW_GAME].setEnabled(true);
            } else if (game.getTurn() > 6) {
                gameOver = true;
                messageBox.setText("You lost! The answer was " + game.getAnswer());
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
    private static final Color backgroundColor = Color.DARK_GRAY;
    private static final Color foregroundColor = Color.DARK_GRAY;
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
                b.setBackground(backgroundColor);
                b.setForeground(foregroundColor);
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
                grid[r][c].setBackground(backgroundColor);
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
class MessageBox extends JTextArea {
    MessageBox() {
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
    }
}
