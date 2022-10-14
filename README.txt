This wordle app was build to explore wordle strategies and algorithms

The NY Times Wordlebot uses information theory to guess the word that minimizes the expected number of remaining steps.
It solves puzzles in an average of 3.5 turns (3.44 for regular mode, 3.55 for hard mode) using an algo that is incredibly
complicated - something no human could ever do.

I wanted to experiment with how simpler algorithms might compare (including more human-like algos), as well as the effects
of using different starter words (something endlessly debated on social media). To that end, I build two algos:
1) Minimize expected remaining. Selects each guess so as minimize the expected number of remaining answers (out of the
original 2309 possible answers). The actual remaining will depend on the color pattern returned, so there is some luck
here each game. This is much simpler than the wordlebot algo, but still beyond what a human could do.
2) Maximize expected matches. Selects each guess so as maximize the expected number of matched letters among the remaining
possible answers. This is much closer to what a human would do, based on their knowledge of letter frequencies and
letter combinations that are often seen together.

The program allows you to play wordle using a randomly generated word, and includes a "hint" button that uses the two
algos to evaluate candidate words and also a word the user might be considering. Using "hint" button on the fist word
gives a broad analysis of possible opening words.

The program also allows you to go into "cheat" mode, in which you enter both the guess and the colors that wordle returned,
and then use the hint button to get hints as to what to guess next.

Cheat mode results, as well as any results in which you use the hint button don't get saved in your statistics.

The program is made up of the following modules:
* Gui.java - The graphical user interface
* GameEngine.java - The underlying game enginer
* WordleWords.java - The list of possible answers and the list of allowed guesses
* Utils.java - a few common utilities
* WordleAnalyzer.java is an obsolete file that was the original command line interface I used to test and evaluate the
*   algorithms so see how they compared to wordlebots. After creating the gui, I renamed the "main" method here to
*   "old_main" (so GUI is now the main entry point), but it can be renamed and run in order evaluate the algorithms

Summary of insights (from WordleAnalyzer.java):
1) The "minimize expected remaining words" algo is only slightly worse than the NY Times wordlebot algo,
taking 3.52 and 3.62 steps (regular and hard mode) vs 3.44 and 3.55. This algorithm uses "raise" as the starting word, as
that word, on average leaves an average of 60 remaining possible answers afterwards (10 less than any other starter word).
2) The "maximize matches" algo (counting right letter/wrong spot as .8 match) takes about 3.62 steps on average in both
hard and regular mode. It uses "stare" as the starting word, which averages 1.53 matches.
3) The choice of starting words (so long as you pick a common starting word) doesn't have a huge impact. Although wordlebot's
old favorite starter word for regular mode ("crane") does lead to a very small reduction in avg steps to solve across
these other algos as well, suggesting it is a fine starting word (even though "c" is only the 11'th most common letter
to appear in potential wordle answers).

This app was built using Java 17, the current STL. I've not verified it against older versions of Java.
