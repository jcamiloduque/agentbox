package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Tool(name = "read_file", description = "Reads files from the disk storage system")
public class ReadFileTool implements ToolInterface {

    @NotNull
    @Contract(" -> new")
    public static ChatCompletionFunctionTool tool() {
        return ChatCompletionFunctionTool.builder()
            .function(FunctionDefinition.builder()
                .name("read_file")
                .description("Read and return the contents of a file")
                .parameters(FunctionParameters.builder()
                    .putAdditionalProperty("type", JsonValue.from("object"))
                    .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "file_path", Map.of(
                            "type", "string",
                            "description", "The path to the file to read"
                        )
                    )))
                    .putAdditionalProperty("required", JsonValue.from(List.of("file_path")))
                    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                    .build())
                .build())
            .build();
    }

    public String execute(JsonNode arguments) throws Exception {
        String filePath = arguments.get("file_path").asText();
        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        if (!java.nio.file.Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        if (!java.nio.file.Files.isReadable(path)) {
            throw new IllegalArgumentException("File is not readable: " + filePath);
        }
        return java.nio.file.Files.readString(path);
    }
}
