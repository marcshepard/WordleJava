/*
WordleAnalyzer - a simple wordle game analyzer

This program can try to solve wordles, or help the user solve them

Author - Marc Shepard
*/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class WordleAnalyzer {
    public static void main (String[] args) {
        System.out.println ("Welcome to wordle analyzer! Available commands:");
        System.out.println ("\tp\thave the computer solve a random wordle");
        System.out.println ("\tp <x>\thave the computer try to solve wordle with your choice, <x>, of word");
        System.out.println ("\ta\tanalyze the solving algo across all possible answers");
        System.out.println ("\ts\tanalyze stats on starting words");
        System.out.println ("\ts <x>\tanalyze stats on your choice, <x>, of starting word");
        System.out.println ("\th\ttoggle hard mode (default is on, making algo much faster but slightly worse)");
        System.out.println ("\tq\tquit");
        boolean hardMode = true;

        while (true) {
            String command = Utils.input("\nWhat do you want to do? ").trim().toLowerCase();
            switch (command.substring(0, 1)) {
                case "q":
                    return;
                case "h":
                    hardMode = !hardMode;
                    System.out.println ("Hard mode is now " + hardMode);
                    break;
                case "p":
                    WordleGame wa = new WordleGame(hardMode, true);
                    if (command.length() == 1) {
                        wa.play();
                    } else {
                        String word = command.substring(1).trim();
                        if (!Arrays.asList(WordleWords.getPossibleAnswers()).contains(word)) {
                            System.out.println (word + " is not a valid Worldle answer");
                            continue;
                        }
                        wa.play(word);
                    }
                    break;
                case "a":
                    analyzeAlgo(hardMode);
                    break;
                case "s":
                    if (command.length() == 1) {
                        evaluateStartingWords();
                    } else {
                        String word = command.substring(1).trim();
                        if (!Arrays.asList(WordleWords.getPossibleAnswers()).contains(word)) {
                            System.out.println (word + " is not a valid Worldle answer");
                            continue;
                        }
                        evaluateStartingWord(word);
                    }
                    break;
                default:
                    System.out.println ("Invalid command");

            }
        }
    }

    // Analyze the currently selected algo: avg steps, # failures, hardest words
    public static void analyzeAlgo (boolean hardMode) {
        String[] possibleAnswers = WordleWords.getPossibleAnswers();
        int answersAnalyzed = 0;    // Analyze all possible answers
        int totalSteps = 0;         // Count total steps for all words (e.g., 3 + 4 + ...)
        int numFailures = 0;        // Failures are words that took more than 6 steps to solve
        ArrayList<String> hardestWords = new ArrayList<>(); // Keep track of the hardest words
        int maxSteps = 0;           // Defined by having the largest number of steps required to solve them

        for (String answer : possibleAnswers) {
            WordleGame g = new WordleGame(hardMode, false);
            int turns = g.play(answer);
            answersAnalyzed++;
            totalSteps += turns;
            if (turns > 6) {
                numFailures++;
            }
            if (turns > maxSteps) {
                maxSteps = turns;
                hardestWords.clear();
            }
            if (turns == maxSteps) {
                hardestWords.add(answer);
            }
            if (answersAnalyzed % 100 == 0)
                System.out.println ("Analyzed " + answersAnalyzed + " (out of 2309)...");
        }
        System.out.println("Avg steps to solve: " + (double)totalSteps/possibleAnswers.length);
        System.out.println("Number of failures (>6 steps): " + numFailures + " out of " + possibleAnswers.length);
        System.out.println("The hardest words took " + maxSteps + " steps: " + hardestWords);
    }

    // Evaluate a particular starting word
    public static void evaluateStartingWord(String starterWord) {
        ArrayList<String> allAnswers = new ArrayList<>(Arrays.asList(WordleWords.getPossibleAnswers()));
        double expectedRemaining = WordleGame.expectedRemaining(starterWord, allAnswers);
        double expectedMatches = WordleGame.expectedMatches(starterWord, allAnswers,.8);
        System.out.println ("Average remaining words after that guess: " + expectedRemaining);
        System.out.println ("Average letter matches for that guess (.8 for wrong position): " + expectedMatches);
    }
    
    // Evaluate opening words based on various statistics, including average numbers of letters matched, and
    // expected number of words remaining.
    public static void evaluateStartingWords() {
        HashMap<String, Double> expectedRemaining = new HashMap<>();
        HashMap<String, Double> expectedMatchCount = new HashMap<>();
        ArrayList<String> allAnswers = new ArrayList<>(Arrays.asList(WordleWords.getPossibleAnswers()));

        for (String starterWord : WordleWords.getPossibleAnswers()) {
            expectedRemaining.put(starterWord, WordleGame.expectedRemaining(starterWord, allAnswers));
            expectedMatchCount.put(starterWord, WordleGame.expectedMatches(starterWord, allAnswers, .8));
        }

        System.out.println ("15 starting words that lead to (on average) the smallest set of remaining answers");
        printTop (expectedRemaining, 15, false);

        System.out.println ("\n15 starting words that lead to (on average) the most matched letters");
        printTop (expectedMatchCount, 15, true);
    }

    // Print top n values in a hash map
    public static void printTop(HashMap<String, Double> h, int n, boolean largest) {
        String key = findTop(h, largest);
        Double value = h.get(key);
        System.out.println (key + "\t" + value);
        if (n > 0) {
            h.remove(key);
            printTop(h, n-1, largest);
            h.put(key, value);
        }
    }

    // Find the hashmap key with the smallest value
    public static String findTop (HashMap<String, Double> h, boolean largest) {
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
}

