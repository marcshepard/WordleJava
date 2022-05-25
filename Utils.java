/*
Utils - utility methods

Simple and common methods that most other programming languages provide, but Java doesn't for some reason

Author - Marc Shepard
*/

import java.util.Scanner;

public class Utils {
    private static Scanner scan = new Scanner(System.in);

    // Ask a question, get an answer
    public static String input (String question) {
        System.out.print (question);
        String answer = scan.nextLine();
        return answer;
    }

    // Pick a random integer in a given range
    public static int randInt (int min, int max) {
        return (int)(Math.random() * (max-min+1)) + min;
    }

    // Pick a random String from an array
    public static String randChoice (String[] strings) {
        return strings[randInt(0, strings.length - 1)];
    }

    // Count the number of character occurances in a String
    public static int count (String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (c == s.charAt(i)) {
                count++;
            }
        }
        return count;
    }

    // Run-time assertions for test harness (vs normal assertions that are compiled out)
    public static void verify (boolean condition) {
        if (condition == false) {
            throw new RuntimeException("Assertion failed");
        }
    }
}