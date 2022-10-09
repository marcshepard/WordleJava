/*
WordleUserGame - a game of wordle being played by the user

Author - Marc Shepard
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;

public class WordleUserGame {
    // INSTANCE VARIABLES
    private String answer;                      // Answer to this game
    private int turn;                           // Turn number (1-6 for active games, 7 if game was lost)
    private ArrayList<String> remainingAnswers; // The possible answers remaining (there are initially 2309 of them)
    private ArrayList<String> allowedGuesses;   // The possible guesses remaining (there are initially over 10k of them)
    private boolean hardMode;                   // Only guess remaining possible answers, or all answers?

    // CONSTANTS used for specifying the "pattern" of colors associated with a given guess
    public static final char MATCH = 'G'; // Right letter, right position (G = green)
    public static final char CLOSE = 'Y'; // Right letter, wrong position (Y = yellow)
    public static final char MISS =  'R'; // Wrong letter (R = red)
    public static final String WINNING_PATTERN = "GGGGG";
    public static final int WORD_SIZE = 5;
    public static final double MATCHING_WEIGHT = .8;

    // Constructor picks a random word as the puzzle answer, initializes allowed guesses and remaining answer lists
    public WordleUserGame (boolean hardMode) {
        this.hardMode = hardMode;
        String[] possibleAnswers = WordleWords.getPossibleAnswers(); // the 2309 answers in Wordle
        answer = Utils.randChoice(possibleAnswers);
        remainingAnswers = WordleWords.getPossibleAnswersArrayList();
        allowedGuesses = WordleWords.getAllowedGuessesArrayList();
        turn = 1;
    }

    // Let the user take a guess
    // Returns the color pattern, or null if invalid guess or out of turns
    public String guess (String guess) {
        // Out of turns
        if (turn > 6) {
            return null;
        }
        
        // Invalid guess
        guess = guess.toLowerCase();
        if (!allowedGuesses.contains(guess)) {
            return null;
        }

        // Else calculate the pattern and prune the lists
        String pattern = calculatePattern(guess, answer);
        remainingAnswers = pruneList(remainingAnswers, guess, pattern);
        if (hardMode)
            allowedGuesses = pruneList(allowedGuesses, guess, pattern);
        turn++;
        return pattern;
    }

    // Returns 1-6, or 7 if all guesses have been taken
    public int getTurn () {
        return turn;
    }

    public String getAnswer () {
        return answer;
    }

    // PRIVATE SUPPORT METHODS
    // Prune a list of possible answers based on a guess and the pattern that guess returned
    // Possible answers that would not produce that pattern from that guess are removed
    private static ArrayList<String> pruneList (ArrayList<String> list, String guess, String pattern) {
        ArrayList<String> prunedList = new ArrayList<>();
        for (String word : list) {
            if (calculatePattern(guess, word).equals(pattern)) {
                prunedList.add(word);
            }
        }
        return prunedList;
    }

    // Get pattern that a given guess would yield if a given word was the answer
    // Algo was surprisingly hard to code.
    // The tricky part is if a given letter occurs multiple times in a guess, deciding which indices to color
    // "CLOSE" vs "MISS" as that requires first determining the number of exact matches, and then coloring
    // some "CLOSE" (to match number of occurances in the answer) and others "MISS".
    // For example, guessing melee when the answer is beeps; 1st e should be MATCH, 2st CLOSE, 3rd MISS
    private static String calculatePattern (String guess, String answer) {
        char[] pattern = new char[WORD_SIZE]; // To store the pattern we'll return
        HashMap <Character, Integer> closeCount = new HashMap <Character, Integer>();
        HashMap <Character, Integer> matchCount = new HashMap <Character, Integer>();

        // First calculate which indexes are MATCHES, which are MISSES, and rest we won't know yet
        // We'll keep track of the latter (ones we don't know yet) in "closeCount", so we can figure out how to color
        // these next. We also need to keep counts for letters that produced exact matches in matchCount.
        for (int i = 0; i < WORD_SIZE; i++) {
            char letter = guess.charAt(i);
            if (letter == answer.charAt(i)) {
                pattern[i] = MATCH;
                if (!matchCount.containsKey(letter)) {
                    matchCount.put(letter, 1);
                } else {
                    matchCount.put(letter, matchCount.get(letter) + 1);
                }
            } else if (answer.indexOf(letter) < 0) {
                pattern[i] = MISS;
            } else {
                closeCount.put(letter, 1);
            }
        }

        // Next, keep track of how many "yellows" (CLOSE) we need to mark for each letter:
        for (char letter : closeCount.keySet()) {
            int count = Math.min(Utils.count(answer, letter), Utils.count(guess, letter));
            if (!matchCount.containsKey(letter)) {
                closeCount.put(letter, count);
            } else {
                closeCount.put(letter, count - matchCount.get(letter));
            }
        }

        // Finally, mark the null values in "pattern" with either CLOSE or MISS...
        for (int i = 0; i < WORD_SIZE; i++) {
            if (pattern[i] == 0) {
                char letter = guess.charAt(i);
                int count = closeCount.get(letter);
                if (count > 0) {
                    pattern[i] = CLOSE;
                    closeCount.put(letter, count-1);
                } else {
                    pattern[i] = MISS;
                }
            }
        }

        return new String(pattern);
    }
}
