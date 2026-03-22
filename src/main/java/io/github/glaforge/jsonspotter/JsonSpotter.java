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
     * from the given input string. This uses a robust recursive descent parser to natively 
     * support lenient features like comments, unquoted keys, trailing commas, and single quotes.
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
                Parser parser = new Parser(input, i);
                if (parser.parseValue()) {
                    int len = parser.getIndex() - i;
                    if (len > maxLength) {
                        maxLength = len;
                        bestMatch = input.substring(i, parser.getIndex());
                    }
                    // Optimization: We successfully parsed a complete valid JSON structure.
                    // Any JSON inside it will by definition be shorter. 
                    // So we can safely fast-forward our main scanner to the end of this structure.
                    i = parser.getIndex() - 1;
                }
            }
        }
        return bestMatch;
    }

    private static class Parser {
        private final String input;
        private final int length;
        private int index;

        public Parser(String input, int startIndex) {
            this.input = input;
            this.length = input.length();
            this.index = startIndex;
        }

        public int getIndex() {
            return index;
        }

        private char peek() {
            if (index < length) {
                return input.charAt(index);
            }
            return '\0'; // End of string marker
        }

        private void consume() {
            index++;
        }

        private void skipWhitespaceAndComments() {
            while (index < length) {
                char c = input.charAt(index);
                if (Character.isWhitespace(c)) {
                    index++;
                } else if (c == '#') {
                    // YAML comment #
                    index++;
                    while (index < length && input.charAt(index) != '\n' && input.charAt(index) != '\r') {
                        index++;
                    }
                } else if (c == '/') {
                    if (index + 1 < length && input.charAt(index + 1) == '/') {
                        // Single-line comment //
                        index += 2;
                        while (index < length && input.charAt(index) != '\n' && input.charAt(index) != '\r') {
                            index++;
                        }
                    } else if (index + 1 < length && input.charAt(index + 1) == '*') {
                        // Multi-line comment /* */
                        index += 2;
                        while (index + 1 < length && !(input.charAt(index) == '*' && input.charAt(index + 1) == '/')) {
                            index++;
                        }
                        if (index + 1 < length) {
                            index += 2; // consume */
                        } else {
                            index = length; // reached end without closing
                        }
                    } else {
                        break; // Just a slash, not a comment
                    }
                } else {
                    break;
                }
            }
        }

        public boolean parseValue() {
            skipWhitespaceAndComments();
            char c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"' || c == '\'') return parseString();
            if (c == '+' || c == '-' || c == '.' || Character.isDigit(c) || c == 'I' || c == 'N') {
                if (parseNumber()) return true;
                // Note: I/N might be part of unquoted identifier later if not a number, but parseValue doesn't handle unquoted strings directly except as keys.
            }
            if (c == 't') return parseLiteral("true");
            if (c == 'f') return parseLiteral("false");
            if (c == 'n') return parseLiteral("null");
            return false;
        }

        private boolean parseObject() {
            if (peek() != '{') return false;
            consume(); // {
            skipWhitespaceAndComments();
            if (peek() == '}') {
                consume();
                return true;
            }

            while (true) {
                skipWhitespaceAndComments();
                if (!parseKey()) return false;
                
                skipWhitespaceAndComments();
                if (peek() != ':') return false;
                consume(); // :
                
                skipWhitespaceAndComments();
                if (!parseValue()) return false;
                
                skipWhitespaceAndComments();
                if (peek() == ',') {
                    consume(); // ,
                    skipWhitespaceAndComments();
                    if (peek() == '}') {
                        // Trailing comma
                        consume();
                        return true;
                    }
                } else if (peek() == '}') {
                    consume();
                    return true;
                } else {
                    return false;
                }
            }
        }

        private boolean parseArray() {
            if (peek() != '[') return false;
            consume(); // [
            skipWhitespaceAndComments();
            if (peek() == ']') {
                consume();
                return true;
            }

            while (true) {
                skipWhitespaceAndComments();
                if (peek() == ',') {
                    // Missing value (e.g. [1, , 3]), fall through to comma consumer
                } else if (peek() == ']') {
                    consume();
                    return true;
                } else {
                    if (!parseValue()) return false;
                }
                
                skipWhitespaceAndComments();
                if (peek() == ',') {
                    consume(); // ,
                    skipWhitespaceAndComments();
                    if (peek() == ']') {
                        // Trailing comma
                        consume();
                        return true;
                    }
                } else if (peek() == ']') {
                    consume();
                    return true;
                } else {
                    return false;
                }
            }
        }

        private boolean parseKey() {
            char c = peek();
            if (c == '"' || c == '\'') {
                return parseString();
            }
            // Unquoted key support
            if (isUnquotedChar(c)) {
                while (isUnquotedChar(peek())) {
                    consume();
                }
                return true;
            }
            return false;
        }

        private boolean isUnquotedChar(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$';
        }

        private boolean parseString() {
            char quote = peek();
            if (quote != '"' && quote != '\'') return false;
            consume(); // " or '
            
            while (index < length) {
                char c = peek();
                if (c == '\\') {
                    consume(); // escape char
                    if (index < length) consume(); // escaped char
                } else if (c == quote) {
                    consume(); // closing quote
                    return true;
                } else {
                    consume(); // normal char
                }
            }
            return false;
        }

        private boolean parseNumber() {
            // Support: -? \d+ (\. \d+)? ([eE] [+-]? \d+)?
            // Leniency: leading +, leading decimal, trailing decimal, NaN, Infinity
            int start = index;
            char c = peek();
            
            if (c == '+' || c == '-') consume();
            
            if (peek() == 'I' && parseLiteral("Infinity")) return true;
            if (peek() == 'N' && parseLiteral("NaN")) return true;

            boolean hasDigits = false;
            while (Character.isDigit(peek())) {
                consume();
                hasDigits = true;
            }
            
            if (peek() == '.') {
                consume();
                while (Character.isDigit(peek())) {
                    consume();
                    hasDigits = true;
                }
            }
            
            if (!hasDigits) {
                index = start;
                return false;
            }
            
            char e = peek();
            if (e == 'e' || e == 'E') {
                consume();
                char sign = peek();
                if (sign == '+' || sign == '-') consume();
                if (!Character.isDigit(peek())) {
                    index = start;
                    return false;
                }
                while (Character.isDigit(peek())) consume();
            }
            
            return true;
        }

        private boolean parseLiteral(String expected) {
            int start = index;
            for (int i = 0; i < expected.length(); i++) {
                if (peek() != expected.charAt(i)) {
                    index = start;
                    return false;
                }
                consume();
            }
            return true;
        }
    }
}
