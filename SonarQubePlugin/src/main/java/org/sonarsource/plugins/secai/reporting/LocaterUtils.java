package org.sonarsource.plugins.secai.reporting;

import java.util.Stack;

/**
 * This class contains methods to help with locating a specific code snippet and
 * converting indices.
 */
public class LocaterUtils {

    public static boolean allBracketsClosed(String str) {
        return allBracketsClosed(str, 0, str.length() - 1);
    }

    /**
     * Correctly checks if all bracket types ({}, [], ()) are balanced and properly
     * nested.
     * Now handles character literals and string literals properly.
     */
    public static boolean allBracketsClosed(String str, int start, int stop) {
        if (str == null || str.isEmpty()) {
            return true;
        }

        start = start < 0 ? 0 : start;
        stop = stop > str.length() - 1 ? str.length() - 1 : stop;
        try {
            str = str.substring(start, stop + 1);
        } catch (IndexOutOfBoundsException e) {
            // TODO: does it make sense to return true or should we return false?
            return true;
        }

        Stack<Character> stack = new Stack<>();
        boolean inString = false;
        boolean inChar = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // Handle string literals
            if (c == '"' && !inChar && (i == 0 || str.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            // Handle character literals
            if (c == '\'' && !inString && (i == 0 || str.charAt(i - 1) != '\\')) {
                inChar = !inChar;
                continue;
            }

            // Skip bracket checking inside strings or character literals
            if (inString || inChar) {
                continue;
            }

            if (c == '(' || c == '{' || c == '[') {
                stack.push(c);
            } else if (c == ')' && !stack.isEmpty() && stack.peek() == '(') {
                stack.pop();
            } else if (c == '}' && !stack.isEmpty() && stack.peek() == '{') {
                stack.pop();
            } else if (c == ']' && !stack.isEmpty() && stack.peek() == '[') {
                stack.pop();
            } else if (c == ')' || c == '}' || c == ']') {
                return false; // Closing bracket with no matching opening bracket
            }
        }
        return stack.isEmpty(); // If stack is empty, all brackets were matched
    }

    /**
     * Skip all leading/trailing whitespace characters until a non-blank character is found or the end of the snippet is reached.
     * @param snippet snippet that the index refers to
     * @param index index of the first whitespace character from which to start skipping
     * @param reverse direction in which to skip -> true: towards 0, false: towards end of snippet
     * @return index of the first non-blank character in the given direction
     */
    public static int skipWhitespaces(String snippet, int index, boolean reverse) {
        if (snippet.isEmpty()) return 0;

        // Handle negative indices
        if (index < 0) {
            index = 0;
        }

        if (index >= snippet.length()) {
            index = snippet.length() - 1;
        }

        if (reverse) {
            while (index > 0 && Character.isWhitespace(snippet.charAt(index))) {
                index--;
            }
        } else {
            while (index < snippet.length() - 1 && Character.isWhitespace(snippet.charAt(index))) {
                index++;
            }
        }
        return index;
    }

}
