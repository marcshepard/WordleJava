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
    public enum Algo {LeastRemaining, MostLetters};
    // INSTANCE VARIABLES
    private boolean hardMode = false;           // Only guess remaining possible answers, or all answers?
    private Algo algo = Algo.LeastRemaining;    // Algorithm to use for solving
    private boolean verbose = false;            // Display each step of play to stdout
    private static String[] possibleAnswers = WordleWords.getPossibleAnswers(); // the 2309 answers in Wordle
    private String starterWord = null;          // Starter word - if null then calculate it via algo

    // CONSTANTS used for specifying the "pattern" of colors associated with a given guess
    public static final char MATCH = 'G'; // Right letter, right position (G = green)
    public static final char CLOSE = 'Y'; // Right letter, wrong position (Y = yellow)
    public static final char MISS =  'R'; // Wrong letter (R = red)
    public static final String WINNING_PATTERN = "GGGGG";
    public static final int WORD_SIZE = 5;
    public static final double MATCHING_WEIGHT = .8;

    public WordleGame (boolean hardMode, boolean verbose, Algo algo) {
        this.hardMode = hardMode;
        this.verbose = verbose;
        this.algo = algo;
    }

    public WordleGame () {
        this (false, true, Algo.LeastRemaining);
    }

    // PUBLIC METHODS
    // Unit tests
    public void test () {
        System.out.println("Testing the pattern matching method");
        Utils.verify (getPattern ("loyal", "album").equals ("YRRYR"));
        Utils.verify (getPattern ("melee", "peeps").equals ("RGRYR"));
        Utils.verify (getPattern ("crane", "ziggy").equals ("RRRRR"));
    }

    // Set the starting word (null = revert to default value)
    public void setStarterWord (String starterWord) {
        this.starterWord = starterWord;
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

        // Optimization to precompute initial word (makes evaluating algo over all answers way faster in hard mode)
        String bestGuess = starterWord;
        double score = possibleAnswers.length;
        if (starterWord == null) {
            if (algo == Algo.LeastRemaining) {
                bestGuess = "raise";
                score = 60.74404504114335;
            } else {
                bestGuess = "stare";
                score = 1.5272412299696931;
            }
        } else {
            if (algo == Algo.LeastRemaining) {
                score = expectedRemaining(starterWord, remainingAnswers);
            } else {
                score = expectedMatches(starterWord, remainingAnswers, MATCHING_WEIGHT);
            }
        }
 
        for (int turn = 1; turn < 20; turn++) {
            // Find the guess that will result in the lowest expected number of remainingAnswers
            if (turn > 1) {
                if (algo == Algo.LeastRemaining) {
                    score = remainingAnswers.size();
                    for (String guess : possibleGuesses) {
                        double expectedRemaining = expectedRemaining(guess, remainingAnswers);
                        if (expectedRemaining < score) {
                            score = expectedRemaining;
                            bestGuess = guess;
                        }
                    }
                } else {
                    score = 0;
                    for (String guess : possibleGuesses) {
                        double expectedMatches = expectedMatches(guess, remainingAnswers, MATCHING_WEIGHT);
                        if (expectedMatches > score) {
                            score = expectedMatches;
                            bestGuess = guess;
                        }
                    }
                }
            }

            // Print some information about the guess and it's results, and prune the possibleAnswers list
            String guess = bestGuess;
            trace ("Guess #" + turn + ": " + guess);
            trace ("\t" + (algo == Algo.LeastRemaining ? "Expected remaining words" : "Expected matches") + ": " + score);
            String pattern = getPattern(guess, answer);
            trace ("\t" + "Pattern: " + pattern);
            if (pattern.equals(WINNING_PATTERN)) {
                return turn;
            }
            remainingAnswers = pruneList (remainingAnswers, guess, pattern);
            if (hardMode)
                possibleGuesses = remainingAnswers;
            trace ("\t" + "Actual remaining words: " + remainingAnswers.size());
        }

        System.err.println ("Error: Failed to solve wordle in 20 turns!");
        return 20;
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
    // We look at what pattern the guess would create for each possible answer, then aggregate
    // the possible answers by those patterns, then figure out the  
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

    public static void cheat () {
        System.out.println ("Welcome to WordleAnalyzer cheat mode, to help you solve a live puzzle");
        System.out.println ("On each turn, you can type one of the following");
        System.out.println ("\tword pattern\twhere word is a valid wordle answer, and pattern is 5 letter R/Y/G");
        System.out.println ("\t\tR=wrong letter, Y=right letter, wrong position, G=right letter and position");
        System.out.println ("\t\tE.g., foyer RRYRG");
        System.out.println ("\th\tto get a hint - repeating this command gives bigger hints");
        System.out.println ("\tword\tto get feedback on the word you are considering playing");
        
        ArrayList<String> possibleWords = WordleWords.getPossibleAnswersArrayList();
        ArrayList<String> remainingAnswers = possibleWords;
        int turn = 1;
        int hintLevel = 0;

        while (true) {
            if (turn > 6) {
                System.out.println ("Out of turns - better luck next time!");
                return;
            }
            String command = Utils.input ("Turn " + turn + ". What do you want to do? ").trim().toLowerCase();

            if (command.indexOf(" ") > 0) {
                int i = command.indexOf(" ");
                String guess = command.substring(0, i);
                if (!WordleWords.getPossibleAnswersArrayList().contains(guess)) {
                    System.out.println (guess + " is not a valid wordle guess");
                    continue;
                }
                String pattern = command.substring(i + 1).trim().toUpperCase();
                if (pattern.length() != 5 || pattern.replace("R","").replace("G", "").replace("Y","").length() != 0) {
                    System.out.println (pattern + " is not a valid 5 letter pattern containing just R, G, or Y");
                    continue;
                }
                double expectedRemaining = expectedRemaining(guess, remainingAnswers);
                if (pattern.equals(WINNING_PATTERN)) {
                    System.out.println ("Nice job!");
                    return;
                }
                if (expectedRemaining == 0) {
                    System.out.println ("That is not possible - it would lead to no remaining answers but not a winning (GGGGG) pattern!!!");
                    continue;
                }
                remainingAnswers = pruneList(remainingAnswers, guess, pattern);
                turn++;
                hintLevel = 0;
            } else if (command.equals("h")) {
                switch (hintLevel) {
                    default:
                    case 2:
                        String bestGuess = remainingAnswers.get(0);
                        double bestExpectedRemaining = remainingAnswers.size();
                        for (String guess : possibleWords) {
                            double expectedRemaining = expectedRemaining(guess, remainingAnswers);
                            if (expectedRemaining < bestExpectedRemaining) {
                                bestExpectedRemaining = expectedRemaining;
                                bestGuess = guess;
                            }
                        }
                        System.out.printf ("Recommend guess: %s. It averages %.2f remaining words and %.2f matched letters, of which %.2f are exact matches (right letter and position)\n",
                            bestGuess, bestExpectedRemaining, expectedMatches(bestGuess, remainingAnswers, 1), expectedMatches(bestGuess, remainingAnswers, 0));
                    case 1:
                        System.out.print ("Remaining answers: ");
                        for (int i = 0; i < remainingAnswers.size(); i++) {
                            if (i > 10) {
                                System.out.print ("...");
                                break;
                            }
                            System.out.print (remainingAnswers.get(i) + " ");
                        }
                        System.out.println();
                    case 0:
                        System.out.println(remainingAnswers.size() + " remaining answers left");
                    hintLevel++;
                }
            } else {
                String guess = command;
                if (!WordleWords.getPossibleAnswersArrayList().contains(guess)) {
                    System.out.println (guess + " is not an allowed guess");
                    System.out.println ("This analyzer currently only allows guessing one of the 2309 possible wordle answers (until I implement a work-around for the JVM 65k byte static initializer limit to support guessing any word allowed by Wordle)");
                    continue;
                }
                System.out.println ("Some information about your guess " + guess + ":");
                if (!remainingAnswers.contains(guess)) {
                    System.out.println("\tIt is not one of the remaining possible answers");
                }
                System.out.printf ("\tExpected # matching letters: %.2f with %.2f in the right position\n",
                    expectedMatches(guess, remainingAnswers, 1), expectedMatches(guess, remainingAnswers, 0));
                System.out.printf ("\tExpected # remaining answers: %.2f\n", expectedRemaining(guess, remainingAnswers));
            }
        }
    }


    // PRIVATE SUPPORT METHODS
    // Prune a list of possible answers based on a guess and the pattern that guess returned
    // Possible answers that would not produce that pattern from that guess are removed
    private static ArrayList<String> pruneList (ArrayList<String> list, String guess, String pattern) {
        ArrayList<String> prunedList = new ArrayList<>();
        for (String word : list) {
            if (getPattern(guess, word).equals(pattern)) {
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
