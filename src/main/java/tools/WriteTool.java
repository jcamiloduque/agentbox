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

@Tool(name = "write_file", description = "Writes files to the disk storage system")
public class WriteTool implements ToolInterface {

    @NotNull
    @Contract(" -> new")
    public static ChatCompletionFunctionTool tool() {
        return ChatCompletionFunctionTool.builder()
            .function(FunctionDefinition.builder()
                .name("write_file")
                .description("Write contents to a file")
                .parameters(FunctionParameters.builder()
                    .putAdditionalProperty("type", JsonValue.from("object"))
                    .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "file_path", Map.of(
                            "type", "string",
                            "description", "The path to the file to write"
                        ),
                        "content", Map.of(
                            "type", "string",
                            "description", "The content to write to the file"
                        )
                    )))
                    .putAdditionalProperty("required", JsonValue.from(List.of("file_path", "content")))
                    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                    .build())
                .build())
            .build();
    }

    public String execute(JsonNode arguments) throws Exception {
        String filePath = arguments.get("file_path").asText();
        String content = arguments.get("content").asText();
        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        if (!java.nio.file.Files.exists(path)) {
            java.nio.file.Files.createFile(path);
        }
        if (!java.nio.file.Files.isWritable(path)) {
            throw new IllegalArgumentException("File is not writable: " + filePath);
        }
        java.nio.file.Files.writeString(path, content);
        return "File written successfully: " + filePath;
    }
}
