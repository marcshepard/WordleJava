/*
WordleGame - a game of wordle being played by the computer

This is a support library for WordleAnalyzer, whose goal is to evaluate AI algos for wordle to provide users with
insights into optimized strategies. This class represents a single instance of a game.

Author - Marc Shepard
*/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Math;

public class WordleGame {
    // INSTANCE VARIABLES
    private boolean hardMode = false;           // Is hard mode enables for this game?
    private boolean verbose = false;            // Display each step of play to stdout
    private static String[] possibleAnswers = WordleWords.getPossibleAnswers(); // the 2309 answers in Wordle
    private String starterWord = null;          // Starter word - if null then calculate it via algo

    // CONSTANTS used for specifying the "pattern" of colors associated with a given guess
    public static final char MATCH = 'G'; // Right letter, right position (G = green)
    public static final char CLOSE = 'Y'; // Right letter, wrong position (Y = yellow)
    public static final char MISS =  'R'; // Wrong letter (R = red)
    public static final String WINNING_PATTERN = "GGGGG";
    public static final int WORD_SIZE = 5;

    public WordleGame (boolean hardMode, boolean verbose, String starterWord) {
        this.hardMode = hardMode;
        this.verbose = verbose;
        this.starterWord = starterWord;
    }

    public WordleGame (boolean hardMode, boolean verbose) {
        this (hardMode, verbose, null);
    }

    public WordleGame () {
        this (false, true, null);
    }

    // PUBLIC METHODS
    // Unit tests
    public void test () {
        System.out.println("Testing the pattern matching method");
        Utils.verify (getPattern ("loyal", "album").equals ("YRRYR"));
        Utils.verify (getPattern ("melee", "peeps").equals ("RGRYR"));
        Utils.verify (getPattern ("crane", "ziggy").equals ("RRRRR"));
    }

    // Have wordle analyzer play the game, trying to guess a randomly generated answer
    // Returns the number of turns taken to solve the puzzle; over 6 indicates a failure
    public int play () {
        return (play (Utils.randChoice(possibleAnswers)));
    }

    // Have wordle analyzer play the game, trying to guess a specific answer
    // Returns the number of turns taken to solve the puzzle; over 6 indicates a failure
    public int play (String answer) {
        ArrayList<String> remainingAnswers = new ArrayList<>(Arrays.asList(possibleAnswers));
        ArrayList<String> possibleGuesses = hardMode ? remainingAnswers : new ArrayList<>(Arrays.asList(possibleAnswers));

        if (starterWord == null) {
            starterWord = "raise";
        }
 
        for (int turn = 1; ; turn++) {
            // Find the guess that will result in the lowest expected number of remainingAnswers
            String bestGuess = starterWord;
            double bestExpectedRemaining = possibleAnswers.length;
            if (turn > 1) {
                for (String guess : possibleGuesses) {
                    double expectedRemaining = expectedRemaining(guess, remainingAnswers);
                    if (expectedRemaining < bestExpectedRemaining) {
                        bestExpectedRemaining = expectedRemaining;
                        bestGuess = guess;
                    }
                }
            }

            // Print some information about the guess and it's results, and prune the possibleAnswers list
            String guess = bestGuess;
            trace ("Guess #" + turn + ": " + guess);
            trace ("\t" + "Expected remaining words: " + bestExpectedRemaining);
            String pattern = getPattern(guess, answer);
            trace ("\t" + "Pattern: " + pattern);
            pruneList (remainingAnswers, guess, pattern);
            if (pattern.equals(WINNING_PATTERN)) {
                return turn;
            }
            trace ("\t" + "Actual remaining words: " + remainingAnswers.size());
        }
    }

    // PUBLIC analysis methods
    // For a particular guess, calculte the expected number of letter matches
    // Exact matches (right letter, right slot) are given a weight of 1; user can define how
    // may points to give to close matches (right letter, wrong slot)
    public static double expectedMatches (String guess, ArrayList<String> remainingAnswers, double partialMatchWeight) {
        double matches = 0;
        for (String answer : remainingAnswers) {
            String pattern = getPattern(guess, answer);
            matches += Utils.count(pattern, MATCH);
            matches += partialMatchWeight * Utils.count(pattern, CLOSE);
        }
        matches /= remainingAnswers.size();
        return matches;
    }

    // For a particular guess, calculate the expected number of possible answers after that guess
    public static double expectedRemaining (String guess, ArrayList<String> remainingAnswers) {
        // First create a hashmap to track the number of remainingAnswers that would produce each pattern
        HashMap<String, Integer> buckets = new HashMap<>();
        for (String answer : remainingAnswers) {
            String pattern = getPattern(guess, answer);
            if (!buckets.containsKey(pattern)) {
                buckets.put(pattern, 1);
            } else {
                buckets.put(pattern, buckets.get(pattern) + 1);
            }
        }

        // Next, find the expected value of remaining answers
        // This is done by multiplying the probability of each bucket (=# answers in that bucket/total remaining answer)
        // with it's size.
        double expected = 0;
        if (buckets.containsKey(WINNING_PATTERN)) {
            buckets.put(WINNING_PATTERN, 0);    // Winning pattern means nothing left to guess after
        }
        for (String pattern : buckets.keySet()) {
            double bucketSize = buckets.get(pattern);
            expected += (bucketSize/remainingAnswers.size()) * bucketSize;
        }

        return expected;
    }

    // PRIVATE SUPPORT METHODS
    // Prune a list based on guess and pattern
    private static void pruneList (ArrayList<String> list, String guess, String pattern) {
        for (int i = list.size()-1; i >= 0; i--) {
            String word = list.get(i);
            if (!getPattern(guess, word).equals(pattern)) {
                list.remove(i);
            }
        }
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

    // Get the pattern for a given guess and answer.
    // Currently a wrapper for calculatePattern, but will likely eventually use serialization to cache the results
    // In order to make the code faster (as was done for the Python version of this analyzer)
    private static String getPattern (String guess, String answer) {
        return calculatePattern (guess, answer);
    }

    // Conditional output tracing
    private void trace (String s) {
        if (verbose)
            System.out.println(s);
    }
}
