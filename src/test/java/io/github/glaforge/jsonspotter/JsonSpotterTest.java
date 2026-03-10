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
        String input = "Here is the JSON you requested:\n```json\n{\n  \"name\": \"Gemini\",\n  \"type\": \"AI\"\n}\n```\nHope this helps!";
        String expected = "{\n  \"name\": \"Gemini\",\n  \"type\": \"AI\"\n}";
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
        // Since Jackson 3 uses JsonMapper.builder().build() we use that
        JsonMapper mapper = JsonMapper.builder().build();
        String rawOutput = "```json\n{\"test\": 123}\n```";
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

        String rawOutput = "I found this lenient JSON: " +
                "{ " +
                "  unquoted: 'single quotes', " +
                "  trailing: 'comma', " +
                "  leading_zero: 007, " +
                "  plus_sign: +42, " +
                "  decimal: .5, " +
                "  // java comment\n" +
                "  # yaml comment\n" +
                "  array: [1, , 3,], " +
                "}";

        String extracted = JsonSpotter.extractJson(rawOutput);
        
        try {
            JsonNode node = mapper.readTree(extracted);
            assertEquals("single quotes", node.get("unquoted").asText());
            assertEquals("comma", node.get("trailing").asText());
            assertEquals(7, node.get("leading_zero").asInt());
            assertEquals(42, node.get("plus_sign").asInt());
            assertEquals(0.5, node.get("decimal").asDouble());
            assertEquals(3, node.get("array").size());
            assertTrue(node.get("array").get(1).isNull());
        } catch (JacksonException e) {
            fail("Should have parsed leniently: " + e.getMessage());
        }
    }
}
