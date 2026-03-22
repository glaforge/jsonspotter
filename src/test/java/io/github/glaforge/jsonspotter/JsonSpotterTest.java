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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;

import static org.junit.jupiter.api.Assertions.*;

public class JsonSpotterTest {

    @Test
    public void testExtractJsonWithMarkdown() {
        String input = """
                Here is the JSON you requested:
                ```json
                {
                  "name": "Gemini",
                  "type": "AI"
                }
                ```
                Hope this helps!""";
        String expected = """
                {
                  "name": "Gemini",
                  "type": "AI"
                }""";
        String result = JsonSpotter.extractJson(input);
        assertEquals(expected, result);
    }

    @Test
    public void testExtractJsonArray() {
        String input = "Some text before [1, 2, {\"a\": 3}, 4] some text after.";
        String expected = "[1, 2, {\"a\": 3}, 4]";
        String result = JsonSpotter.extractJson(input);
        assertEquals(expected, result);
    }

    @Test
    public void testExtractJsonWithQuotesInside() {
        String input = "Here is an object: {\"key\": \"value with } and ] inside\", 'other': '{'}";
        String expected = "{\"key\": \"value with } and ] inside\", 'other': '{'}";
        String result = JsonSpotter.extractJson(input);
        assertEquals(expected, result);
    }

    @Test
    public void testExtractJsonWithEscapedQuotes() {
        String input = "Text... {\"key\": \"escaped \\\" quote\"} Text...";
        String expected = "{\"key\": \"escaped \\\" quote\"}";
        String result = JsonSpotter.extractJson(input);
        assertEquals(expected, result);
    }

    @Test
    public void testExtractLongestStructure() {
        String input = "Not this { small } object, but this {\"real\": {\"json\": true}} structure.";
        String expected = "{\"real\": {\"json\": true}}";
        String result = JsonSpotter.extractJson(input);
        assertEquals(expected, result);
    }

    @Test
    public void testInvalidStructure() {
        String input = "There is no json here.";
        String expected = "";
        String result = JsonSpotter.extractJson(input);
        assertEquals(expected, result);
    }

    @Test
    public void testJackson3Integration() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        String rawOutput = """
                ```json
                {"test": 123}
                ```""";
        String extracted = JsonSpotter.extractJson(rawOutput);
        
        try {
            JsonNode node = mapper.readTree(extracted);
            assertEquals(123, node.get("test").asInt());
        } catch (JacksonException e) {
            fail("Should have parsed successfully: " + e.getMessage());
        }
    }

    @Test
    public void testLenientJackson3Integration() throws Exception {
        JsonMapper mapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                .build();

        String rawOutput = """
                I found this lenient JSON: {
                  unquoted: 'single quotes',
                  trailing: 'comma',
                  leading_zero: 007,
                  plus_sign: +42,
                  decimal: .5,
                  // java comment
                  # yaml comment
                  array: [1, , 3,]
                }""";

        String extracted = JsonSpotter.extractJson(rawOutput);
        
        try {
            JsonNode node = mapper.readTree(extracted);
            assertEquals("single quotes", node.get("unquoted").asString());
            assertEquals("comma", node.get("trailing").asString());
            assertEquals(7, node.get("leading_zero").asInt());
            assertEquals(42, node.get("plus_sign").asInt());
            assertEquals(0.5, node.get("decimal").asDouble());
            assertEquals(3, node.get("array").size());
            assertTrue(node.get("array").get(1).isNull());
        } catch (JacksonException e) {
            fail("Should have parsed leniently: " + e.getMessage());
        }
    }

    @Test
    public void testJavaAndYamlComments() {
        String input = """
                Text before {
                  // single line java
                  "a": 1,
                  /* multi-line
                     java */
                  "b": 2,
                  # yaml comment
                  "c": 3
                } Text after""";
        String expected = """
                {
                  // single line java
                  "a": 1,
                  /* multi-line
                     java */
                  "b": 2,
                  # yaml comment
                  "c": 3
                }""";
        assertEquals(expected, JsonSpotter.extractJson(input));
    }

    @Test
    public void testUnquotedKeysAndSingleQuotes() {
        String input = "  { unquoted_key$1: 'single quoted string', another: 42 }  ";
        String expected = "{ unquoted_key$1: 'single quoted string', another: 42 }";
        assertEquals(expected, JsonSpotter.extractJson(input));
    }

    @Test
    public void testTrailingCommasAndMissingValues() {
        String input = "Array: [1, 2, , 4,], Object: {\"a\": 1, }";
        // Longest is the array (14 chars) vs object (11 chars)
        assertEquals("[1, 2, , 4,]", JsonSpotter.extractJson(input));
        
        // Also test the object by itself
        assertEquals("{\"a\": 1, }", JsonSpotter.extractJson("Object: {\"a\": 1, }"));
    }

    @Test
    public void testNonStandardNumbers() {
        String input = "{\"plus\": +42, \"decimal\": .5, \"nan\": NaN, \"inf\": Infinity, \"minusInf\": -Infinity}";
        String expected = "{\"plus\": +42, \"decimal\": .5, \"nan\": NaN, \"inf\": Infinity, \"minusInf\": -Infinity}";
        assertEquals(expected, JsonSpotter.extractJson(input));
    }

    @Test
    public void testRejectsFakeJson() {
        String input = "This { is not { valid } json }"; 
        assertEquals("", JsonSpotter.extractJson(input));
    }

    @Test
    public void testPerformanceScalability() {
        // Construct a massive string of broken brackets
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            sb.append("{ broken ");
        }
        sb.append(" {\"valid\": true}");
        
        long start = System.currentTimeMillis();
        String result = JsonSpotter.extractJson(sb.toString());
        long duration = System.currentTimeMillis() - start;
        
        assertEquals("{\"valid\": true}", result);
        assertTrue(duration < 2000, "Should complete quickly, took " + duration + "ms");
    }
}
