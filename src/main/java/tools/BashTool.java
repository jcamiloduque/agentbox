package tools;

import api.ToolReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Tool(name = "bash", description = "Executes bash commands")
public class BashTool implements ToolInterface {

    @NotNull
    @Contract(" -> new")
    public static ChatCompletionFunctionTool tool() {
        return ChatCompletionFunctionTool.builder()
            .function(FunctionDefinition.builder()
                .name("bash")
                .description("Executes bash commands")
                .parameters(FunctionParameters.builder()
                    .putAdditionalProperty("type", JsonValue.from("object"))
                    .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "command", Map.of(
                            "type", "string",
                            "description", "The bash command to execute"
                        )
                    )))
                    .putAdditionalProperty("required", JsonValue.from(List.of("command")))
                    .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                    .build())
                .build())
            .build();
    }

    static public ToolReference getReference() {
        return new ToolReference(
            "function",
            new ToolReference.Function(
                "bash",
                "Executes bash commands",
                new ToolReference.Parameters(
                    "object",
                    Map.of(
                        "command", new ToolReference.Property(
                            "string",
                            "The bash command to execute"
                        )
                    ),
                    List.of("command")
                )
            )
        );
    }

    public String execute(JsonNode arguments) throws Exception {
        String command = arguments.get("command").asText();
        Process process = new ProcessBuilder("/bin/bash", "-c", command)
                .redirectErrorStream(true)
                .start();
        java.io.InputStream is = process.getInputStream();
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String output = s.hasNext() ? s.next() : "";
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode + ": " + output);
        }
        return output;
    }
}
