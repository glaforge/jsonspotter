/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.glaforge.jsonspotter;

public class JsonSpotter {

    /**
     * Extracts the longest well-balanced JSON-like structure (objects using {} or arrays using [])
     * from the given input string.
     *
     * @param input The string containing the JSON payload.
     * @return The longest extracted JSON string, or empty string if none found.
     */
    public static String extractJson(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        int maxLength = 0;
        String bestMatch = "";

        for (int i = 0; i < input.length(); i++) {
            char startChar = input.charAt(i);
            if (startChar == '{' || startChar == '[') {
                int end = findBalancedEnd(input, i);
                if (end != -1) {
                    int len = end - i + 1;
                    if (len > maxLength) {
                        maxLength = len;
                        bestMatch = input.substring(i, end + 1);
                    }
                }
            }
        }
        return bestMatch;
    }

    private static int findBalancedEnd(String str, int start) {
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        boolean escapeNext = false;
        java.util.Stack<Character> stack = new java.util.Stack<>();

        for (int j = start; j < str.length(); j++) {
            char c = str.charAt(j);

            if (escapeNext) {
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                escapeNext = true;
                continue;
            }

            if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else {
                if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '{' || c == '[') {
                    stack.push(c);
                } else if (c == '}') {
                    if (stack.isEmpty() || stack.peek() != '{') return -1;
                    stack.pop();
                } else if (c == ']') {
                    if (stack.isEmpty() || stack.peek() != '[') return -1;
                    stack.pop();
                }

                if (stack.isEmpty()) {
                    return j;
                }
            }
        }
        return -1;
    }
}
