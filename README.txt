This is a wordle analyzer I wrote as I was reading about optimimal wordle strategies, and wanted to learn more.

Wordlebot uses information theory to guess the word that minimizes the expected number of remaining steps.
That algo aways uses "raise" in regular mode, and "dealt" in hard mode, and solves all puzzles in an average
of 3.5 turns (3.44 for regular mode, 3.55 for hard mode). The algo is incredibly complicated, and no human
could ever use it.

Slightly less complicated is to minimize the average number of remaining words in a game. I coded this up to
see how it would work and compare. It's competative and less complicated, but a bit worse. However this is also
beyond what any human could come close to doing.

Finally, I wondered what would happen if words were selected based on trying to maximize the number of "right"
letters guessed. This is basically what humans do.

I also wanted to do some analysis on the effects of various starter words.

Finally, I coded up an assist mode for players who are stuck on a real wordle. It has different levels of hints
depending how much information the user wants:
* Number of possible answers left
* Evaluation of a user-defined guess
* Letter frequency of top 15 unguessed letters
* Actual list of possible answers
* Recommended next word to guess

The above analysis are available for interactive exploration by running this program.
