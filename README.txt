This is a wordle analyzer I wrote as I was reading about optimimal wordle strategies, and wanted to learn more.

The NY Times Wordlebot uses information theory to guess the word that minimizes the expected number of remaining steps.
That algo's first guess is always "crane" in regular mode, and "dealt" in hard mode, and solves all puzzles in an average
of 3.5 turns (3.44 for regular mode, 3.55 for hard mode). The algo is incredibly complicated, and no human
could ever do anything like it.

I wanted to experiment with how simpler algorithms might compare (including more human-like algos), as well as the effects
of using different starter words (something endlessly debated on social media). To that end, I build two algos:
1) Minimize expected remaining. Selects each guess so as minimize the expected number of remaining answers (out of the
original 2309 possible answers). The actual remaining will depend on the color pattern returned, so there is some luck
here each game. This is much simpler than the wordlebot algo, but still beyond what a human could do.
2) Maximize expected matches. Selects each guess so as maximize the expected number of matched letters among the remaining
possible answers. This is much closer to what a human would do, based on their knowledge of letter frequencies and
letter combinations that are often seen together.

The program allows you to toggle hard/regular mode. Toggle between the two algos. Set the starter word (or use the algo
default) and then see how the algo does on either:
1) A game with a randomly selected word
2) A game in which you specify the starting word
3) On all possible words (to evaluate the overall algo effectiveness in terms of avg number of steps across all words)

The program also adds a "cheat" mode to help you solve live wordles that can give you a progressive series of hints:
* Number of possible answers left
* List of possible answers left
* Recommended next word to guess
Or it can just give you feedback on a guess you are thinking of making.

Summary of insights:
1) The "minimize expected remaining words" algo is only slightly worse than the NY Times wordlebot algo,
taking 3.52 and 3.62 steps (regular and hard mode) vs 3.44 and 3.55. This algorithm uses "raise" as the starting word, as
that word, on average leaves an average of 60 remaining possible answers afterwards (10 less than any other starter word).
2) The "maximize matches" algo (counting right letter/wrong spot as .8 match) takes about 3.62 steps on average in both
hard and regular mode. It uses "stare" as the starting word, which averages 1.53 matches.
3) The choice of starting words (so long as you pick a common starting word) doesn't have a huge impact. Although wordlebot's
favorite starter word for regular mode ("crane") does lead to a very small reduction in avg steps to solve across
these other algos as well, suggesting it is a fine starting word (even though "c" is only the 11'th most common letter
to appear in potential wordle answers).

WordleAnalyzer was built using Java 17, the current STL. I've not verified it against older versions of Java.
