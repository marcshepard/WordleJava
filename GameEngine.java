/*
WordleUserGame - a game of wordle being played by the user

Author - Marc Shepard
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;

public class GameEngine {
    // INSTANCE VARIABLES
    private String answer;                      // Answer to this game
    private int turn;                           // Turn number (1-6 for active games, 7 if game was lost)
    private ArrayList<String> remainingAnswers; // The possible answers remaining (there are initially 2309 of them)
    private ArrayList<String> allowedGuesses;   // The possible guesses remaining (there are initially over 10k of them)
    private boolean hardMode;                   // Only guess remaining possible answers, or all answers?
    private boolean cheatMode;                  // Cheat mode means answer is unknown; user supplies guess and color pattern return from wordle
    private int hintLevel = 0;                  // Hint level, each hint request provides more detail
    private boolean gameOver = false;

    // CONSTANTS used for specifying the "pattern" of colors associated with a given guess
    public static final char MATCH = 'G'; // Right letter, right position (G = green)
    public static final char CLOSE = 'Y'; // Right letter, wrong position (Y = yellow)
    public static final char MISS =  'R'; // Wrong letter (R = red)
    public static final String WINNING_PATTERN = "GGGGG";
    public static final int WORD_SIZE = 5;
    public static final double MATCHING_WEIGHT = .8;

    // Constructor picks a random word as the puzzle answer, initializes allowed guesses and remaining answer lists
    public GameEngine (boolean hardMode, boolean cheatMode) {
        this.cheatMode = cheatMode;
        this.hardMode = hardMode;
        String[] possibleAnswers = WordleWords.getPossibleAnswers(); // the 2309 answers in Wordle
        answer = Utils.randChoice(possibleAnswers);
        remainingAnswers = WordleWords.getPossibleAnswersArrayList();
        allowedGuesses = WordleWords.getAllowedGuessesArrayList();
        turn = 1;
    }

    // Let the user take a guess against the current answer
    // Returns the color pattern, or null if invalid guess or out of turns
    public String guess (String guess) {
        // Are we in cheat mode? Then need to call the other version of guess
        if (cheatMode) {
            return null;
        }

        // Game alrady over? Then no more guesses allowed
        if (gameOver) {
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
        
        hintLevel = 0;      // Reset the hint level after each new guess

        if (!pattern.equals(WINNING_PATTERN)) {
            turn++;
        }
        
        if (pattern.equals(WINNING_PATTERN) || turn > 6) {
            gameOver = true;
        }
        
        return pattern;
    }

    public boolean isValidGuess (String guess) {
        if (allowedGuesses.contains(guess.toLowerCase())) {
            return true;
        }
        return false;
    }

    // In cheat mode (for live wordle), let the user enter their guess and the pattern they got back
    // Returns true if guess and pattern are valid entries
    public boolean guess (String guess, String pattern) {
        // Are not in cheat mode, then need to call the other version of guess
        if (!cheatMode) {
            return false;
        }
                
        // Game alrady over? Then no more guesses allowed
        if (gameOver) {
            return false;
        }
        
        // Invalid guess
        guess = guess.toLowerCase();
        if (!allowedGuesses.contains(guess)) {
            return false;
        }

        // Else calculate the pattern and prune the lists
        ArrayList<String> answersAfterGuess = pruneList(remainingAnswers, guess, pattern);
        if (answersAfterGuess.size() == 0)
            return false;   // Invalid pattern if it doesn't leave at least one possible remaining answer 

        remainingAnswers = answersAfterGuess;

        if (hardMode)
            allowedGuesses = pruneList(allowedGuesses, guess, pattern);
        
        hintLevel = 0;      // Reset the hint level after each new guess

        if (pattern != WINNING_PATTERN) {
            turn++;
        }
        
        if (pattern == WINNING_PATTERN || turn > 6) {
            gameOver = true;
        }
        
        return true;
    }

    // Get a hint on what to do next, optionally considering the users selection of possible word
    public String getHint(String word) {
        if (turn == 1) {
            return getOpeningWordAnalysis(word);
        }
        String hint = "There are " + remainingAnswers.size() + " remaining answers left.\n";
        if (hintLevel >= 1) {
            for (int i = 0; i < remainingAnswers.size(); i++) {
                if (i > 10) {
                    hint += "...";
                    break;
                }
                hint += remainingAnswers.get(i) + " ";
            }
            hint += "\n";
        }
        if (hintLevel >= 2) {
            String bestGuess = remainingAnswers.get(0);
            double bestExpectedRemaining = remainingAnswers.size();
            for (String guess : remainingAnswers) {
                double expectedRemaining = expectedRemaining(guess, remainingAnswers);
                if (expectedRemaining < bestExpectedRemaining) {
                    bestExpectedRemaining = expectedRemaining;
                    bestGuess = guess;
                }
            }
            hint += String.format ("Recommend guess: %s. Over all possible answers, it averages:\n\t%.2f remaining words\n\t%.2f matched letters, of which %.2f are exact matches (right letter and position)\n",
                bestGuess, bestExpectedRemaining, expectedMatches(bestGuess, remainingAnswers, 1), expectedMatches(bestGuess, remainingAnswers, 0));
            if (word != null) {
                word = word.toLowerCase();
                if (word.length() == 5 && !word.equals(bestGuess)) {
                    hint += String.format ("Your guess: %s. Over all possible answers, it averages:\n\t%.2f remaining words\n\t%.2f matched letters, of which %.2f are exact matches (right letter and position)\n",
                    word, expectedRemaining(word, remainingAnswers), expectedMatches(word, remainingAnswers, 1), expectedMatches(word, remainingAnswers, 0));
                    hint += "\tYou guess " + (remainingAnswers.contains(word) ? "is": "is not") + " one of the remaining possible answers";
                }
            }
        }
        if (hintLevel < 2) {
            hint += "The next hint will provide more information";
        }
        hintLevel++;
        return hint;
    }

    // Analyze top opening words, and optionally add a user-specified word for comparison
    public String getOpeningWordAnalysis (String word) {
        String s = "";

        HashMap<String, Double> expectedRemaining = new HashMap<>();
        HashMap<String, Double> expectedMatchCount = new HashMap<>();
        ArrayList<String> allAnswers = WordleWords.getPossibleAnswersArrayList();

        if (word != null && word.length() == 5) {
            word = word.toLowerCase();
            s += String.format ("For your starting word %s\n", word);
            s += String.format ("Average remaining answers: %.2f\n", expectedRemaining(word, allAnswers));
            s += String.format ("Average matched letters: %.2f\n", expectedMatches(word, allAnswers, .8));
        }

        for (String starterWord : WordleWords.getPossibleAnswers()) {
            expectedRemaining.put(starterWord, expectedRemaining(starterWord, allAnswers));
            expectedMatchCount.put(starterWord, expectedMatches(starterWord, allAnswers, .8));
        }

        s += "\n15 starting words that lead to (on average) the smallest set of remaining answers\n";
        s += getTop (expectedRemaining, 15, false);

        s += "\n15 starting words that lead to (on average) the most matched letters (counting 'right letter/wrong spot' as '.8' of a match\n";
        s += getTop (expectedMatchCount, 15, true);

        return s;
    }

    // Find the top n hashmap keys with the smallest or largest values
    private static String getTop(HashMap<String, Double> h, int n, boolean largest) {
        String result = "";
        String key = findTop(h, largest);
        Double value = h.get(key);
        result += String.format ("%s\t%.2f\n", key, value);
        if (n > 0) {
            h.remove(key);
            result += getTop(h, n-1, largest);
            h.put(key, value);
        }
        return result;
    }

    // Find the hashmap key with the smallest or largest value
    private static String findTop (HashMap<String, Double> h, boolean largest) {
        String topKey = null;
        for (String key : h.keySet()) {
            if (topKey == null)
                topKey = key;
            else if (!largest && h.get(key) < h.get(topKey))
                topKey = key;
            else if (largest && h.get(key) > h.get(topKey))
                topKey = key;
        }
        return topKey;
    }

    // Returns 1-6, or 7 if all guesses have been taken
    public int getTurn () {
        return turn;
    }

    public String getAnswer () {
        return answer;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean getCheatMode () {
        return cheatMode;
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

    // For a particular guess, calculate the expected number of letter matches
    // Exact matches (right letter, right slot) are given a weight of 1; user can define how
    // may points to give to close matches (right letter, wrong slot)
    private static double expectedMatches (String guess, ArrayList<String> remainingAnswers, double partialMatchWeight) {
        double matches = 0;
        for (String answer : remainingAnswers) {
            String pattern = calculatePattern(guess, answer);
            matches += Utils.count(pattern, MATCH);
            matches += partialMatchWeight * Utils.count(pattern, CLOSE);
        }
        matches /= remainingAnswers.size();
        return matches;
    }

    // For a particular guess, calculate the expected number of possible answers after that guess
    // We look at what pattern the guess would create for each possible answer, then aggregate
    // the possible answers by those patterns, then figure out the  
    private static double expectedRemaining (String guess, ArrayList<String> remainingAnswers) {
        // First create a hashmap to track the number of remainingAnswers that would produce each pattern
        HashMap<String, Integer> buckets = new HashMap<>();
        for (String answer : remainingAnswers) {
            String pattern = calculatePattern(guess, answer);
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
}
