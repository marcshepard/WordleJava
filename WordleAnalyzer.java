/*
WordleAnalyzer - a simple wordle game analyzer

This program can try to solve wordles, or help the user solve them

Author - Marc Shepard
*/

import java.util.ArrayList;
import java.util.HashMap;

public class WordleAnalyzer {
    private static String help =
        "WordleAnalyzer implements some basic AI algos to analyze wordle puzzles.\n" +
        "One algo selects guesses designed to minimize the number of remaining words.\n" +
        "The other picks a guesses designed to maximize the number of matches letters.\n" +
        "You will see output of 5 letter 'patterns' for each guess; R(red)=miss, Y(yellow)=right letter, wrong slot, G(green)=right letter, right slot\n" +
        "Available commands:\n" +
        " h     toggle hard mode (default is on, making algo much faster but slightly worse)\n" +
        " a     toggle algo (default is least remaining words, which solves quicker)\n" +
        " w [x] override starting word to your choice for 'x', or leave out 'x' to revert to algo default\n" +
        "\n" +
        " p [w] have current algo/mode/starter word try to solve a random wordle, or optionally specify the puzzle answer, w\n" +
        " e     evaluate the current algo/mode/starter word across all possible answers (avg steps to solve, etc)\n" +
        "\n" +
        " s [w] analyze stats on top starting words, or optionally your choice of starter word, w\n" +
        " c     cheat by having wordle analyzer help you with a live wordle\b" +
        " q     quit\n";
    public static void old_main (String[] args) {
        System.out.println ("Welcome to wordle analyzer, which uses one of two AI algos to solve wordle puzzles.");

        boolean hardMode = true;
        WordleGame.Algo algo = WordleGame.Algo.LeastRemaining;
        String startingWord = null;

        while (true) {
            String command = Utils.input("\nWhat do you want to do (type ? for help)? ").trim().toLowerCase();
            switch (command.substring(0, 1)) {
                case "q":
                    return;
                case "?":
                    System.out.println (help);
                    break;
                case "h":
                    hardMode = !hardMode;
                    System.out.println ("Hard mode is now " + hardMode);
                    break;
                case "a":
                    if (algo == WordleGame.Algo.LeastRemaining) {
                        algo = WordleGame.Algo.MostLetters;
                        System.out.println ("Algo will select a guess to maximize expected number of matching letters");
                    } else {
                        algo = WordleGame.Algo.LeastRemaining;
                        System.out.println ("Algo will select a guess to minimize expected number of remaining words");
                    }
                    break;
                case "w":
                    if (command.length() == 1) {
                        startingWord = null;
                        System.out.println ("Reset starting word to algo default");
                    } else {
                        String word = command.substring(1).trim();
                        if (!WordleWords.getPossibleAnswersArrayList().contains(word)) {
                            System.out.println (word + " is not a valid Worldle answer");
                            continue;
                        }
                        startingWord = word;
                        System.out.println ("Starting word set to " + word);
                    }
                    break;
                case "p":
                    WordleGame wa = new WordleGame(hardMode, true, algo);
                    wa.setStarterWord(startingWord);
                    if (command.length() == 1) {
                        wa.play();
                    } else {
                        String word = command.substring(1).trim();
                        if (!WordleWords.getPossibleAnswersArrayList().contains(word)) {
                            System.out.println (word + " is not a valid Worldle answer");
                            continue;
                        }
                        wa.play(word);
                    }
                    break;
                case "e":
                    evaluateAlgo(hardMode, algo, startingWord);
                    break;
                case "s":
                    if (command.length() == 1) {
                        evaluateStartingWords();
                    } else {
                        String word = command.substring(1).trim();
                        if (!WordleWords.getAllowedGuessesArrayList().contains(word)) {
                            System.out.println (word + " is not a valid Worldle guess");
                            continue;
                        }
                        evaluateStartingWord(word);
                    }
                    break;
                case "c":
                    WordleGame.cheat();
                    break;
                default:
                    System.out.println ("Invalid command");

            }
        }
    }

    // Analyze the currently selected algo: avg steps, # failures, hardest words
    public static void evaluateAlgo (boolean hardMode, WordleGame.Algo algo, String starterWord) {
        String[] possibleAnswers = WordleWords.getPossibleAnswers();
        int answersAnalyzed = 0;    // Analyze all possible answers
        int totalSteps = 0;         // Count total steps for all words (e.g., 3 + 4 + ...)
        int numFailures = 0;        // Failures are words that took more than 6 steps to solve
        ArrayList<String> hardestWords = new ArrayList<>(); // Keep track of the hardest words
        int maxSteps = 0;           // Defined by having the largest number of steps required to solve them

        for (String answer : possibleAnswers) {
            WordleGame g = new WordleGame(hardMode, false, algo);
            g.setStarterWord(starterWord);
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
        ArrayList<String> allAnswers = WordleWords.getPossibleAnswersArrayList();
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
        ArrayList<String> allAnswers = WordleWords.getPossibleAnswersArrayList();

        for (String starterWord : WordleWords.getPossibleAnswers()) {
            expectedRemaining.put(starterWord, WordleGame.expectedRemaining(starterWord, allAnswers));
            expectedMatchCount.put(starterWord, WordleGame.expectedMatches(starterWord, allAnswers, .8));
        }

        System.out.println ("15 starting words that lead to (on average) the smallest set of remaining answers");
        printTop (expectedRemaining, 15, false);

        System.out.println ("\n15 starting words that lead to (on average) the most matched letters (counting 'right letter/wrong spot' as '.8' of a match");
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

