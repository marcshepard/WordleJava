import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


// A GUI for the wordle app
public class Gui extends JFrame implements KeyListener {
    WordleEntryPanel wordlePanel;       // Main grid for entering guesses
    MessageBox messageBox;              // A place to give the user feedback text
    WordleUserGame game;                // Underlying game engine
    boolean gameOver = false;

    // TODO - make these configurable in settings (hard mode and color blind mode)
    boolean hardMode = false;
    Color backgroundColor = Color.BLACK;
    Color forgroundColor = Color.WHITE;
    Color matchColor = Color.ORANGE;        // Color.GREEN;
    Color closeColor = Color.BLUE;          // Color.YELLOW;
    Color missColor = Color.DARK_GRAY;

    Gui () {
        game = new WordleUserGame(hardMode);

        setTitle("Yet Another Wordle App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 800);
        setBackground(backgroundColor);

        JPanel buttonPanel = new JPanel();
        JButton b = new JButton("New Game");
        buttonPanel.add(b);
        b = new JButton("Hint");
        buttonPanel.add(b);
        b = new JButton("Stats");
        buttonPanel.add(b);
        b = new JButton("Cheat");
        buttonPanel.add(b);
        b = new JButton("Settings");
        buttonPanel.add(b);
        b = new JButton("Opening words");
        buttonPanel.add(b);
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setPreferredSize(new Dimension (getWidth(), getHeight()/10));
        add (buttonPanel, BorderLayout.NORTH);

        wordlePanel = new WordleEntryPanel();
        wordlePanel.setBackground(Color.BLACK);
        add(wordlePanel, BorderLayout.CENTER);

        messageBox = new MessageBox();
        messageBox.setBackground(backgroundColor);
        messageBox.setForeground(forgroundColor);
        messageBox.setPreferredSize(new Dimension (getWidth(), getHeight()/10));
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

        setVisible(true);
        addKeyListener(this);
    }

    public static void main (String[] args) {
        new Gui();
    }

    /** Handle the key typed event from the text field. */
    public void keyTyped(KeyEvent e) {
    }

    /** Handle the key-pressed event from the text field. */
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
            if (pattern == WordleUserGame.WINNING_PATTERN) {
                gameOver = true;
                messageBox.setText("You won!");
                return;
            } else if (game.getTurn() > 6) {
                gameOver = true;
                messageBox.setText("You lost! The answer was " + game.getAnswer());
            } else {
                wordlePanel.nextRow();
            }
        }
    }

    /** Handle the key-released event from the text field. */
    public void keyReleased(KeyEvent e) {
    }
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
                b.setBackground(Color.DARK_GRAY);
                b.setForeground(Color.WHITE);
                b.setEnabled(false);
                b.setFont(font);
                if (r != 0)
                    b.setVisible(false);
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
class MessageBox extends JButton {
}
