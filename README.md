# JsonSpotter

Lenient JSON Spotter is a lightweight Java utility designed to extract the longest well-balanced JSON-like structure (objects using `{}` or arrays using `[]`) from a given text string. This is particularly useful when working with outputs from Large Language Models (LLMs) that often wrap JSON payloads in Markdown code blocks or include conversational text before and after the actual JSON.

## Features

- Extracts JSON objects (`{...}`) and arrays (`[...]`) from arbitrary text using a purely native, dependency-free **Recursive Descent Parser**.
- **Natively Lenient:** Understands and structurally validates lenient JSON features during extraction. It properly interacts with Java/YAML comments, unquoted keys, single quotes, trailing commas, missing array values, and non-standard number formats (`NaN`, `Infinity`, `.5`, `+42`).
- **Resilient & Fast:** Guaranteed $O(N)$ extraction performance, securely handling massive or heavily malformed payloads without Denial-of-Service ($O(N^2)$) risks.
- Strictly rejects non-JSON text that simply happens to contain coincidentally balanced brackets.

## Usage

### Basic Extraction

```java
import io.github.glaforge.jsonspotter.JsonSpotter;

public class Example {
    public static void main(String[] args) {
        String llmOutput = "Here is the JSON you requested:\n" +
                           "```json\n" +
                           "{\n" +
                           "  \"name\": \"Gemini\",\n" +
                           "  \"type\": \"AI\"\n" +
                           "}\n" +
                           "```\n" +
                           "Hope this helps!";

        String extractedJson = JsonSpotter.extractJson(llmOutput);
        System.out.println(extractedJson);
        // Output:
        // {
        //   "name": "Gemini",
        //   "type": "AI"
        // }
    }
}
```

### Advanced Usage with Lenient Parsers (like Jackson 3)

Because LLM outputs often contain imperfect JSON (e.g., missing quotes around keys, trailing commas, code comments), `JsonSpotter` was designed to fully understand and gracefully extract these lenient structures dynamically block by block. However, to effectively *deserialize* that extracted string into actual Java Objects or `JsonNode`s, you should pair it with a lenient parser like Jackson 3:

```java
import io.github.glaforge.jsonspotter.JsonSpotter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.core.json.JsonReadFeature;

public class LenientParsingExample {
    public static void main(String[] args) throws Exception {
        String rawOutput = "I found this lenient JSON: { unquoted: 'single quotes', trailing: 'comma', // java comment\n }";

        // Extract the JSON-like structure
        String extracted = JsonSpotter.extractJson(rawOutput);

        // Configure a lenient Jackson 3 JsonMapper
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

        // Parse the lenient JSON
        JsonNode node = mapper.readTree(extracted);
        System.out.println(node.get("unquoted").asText()); // single quotes
    }
}
```

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).

## Disclaimer

This is not an official Google project.