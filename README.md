# JsonSpotter

Lenient JSON Spotter is a lightweight Java utility designed to extract the longest well-balanced JSON-like structure (objects using `{}` or arrays using `[]`) from a given text string. This is particularly useful when working with outputs from Large Language Models (LLMs) that often wrap JSON payloads in Markdown code blocks or include conversational text before and after the actual JSON.

## Features

- Extracts JSON objects (`{...}`) and arrays (`[...]`) from arbitrary text.
- Accurately handles nested structures, strings with escaped quotes, and characters like `}` or `]` inside strings.
- Simple, dependency-free core logic.
- Pairs excellently with lenient JSON parsers like Jackson 3 for handling imperfect LLM outputs.

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

### Advanced Usage with Jackson 3

When dealing with LLMs, the extracted "JSON" might still not be strictly valid JSON (e.g., missing quotes around keys, trailing commas, comments). You can use Jackson 3's lenient parsing capabilities on the extracted string:

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
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
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