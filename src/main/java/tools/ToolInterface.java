package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;

public interface ToolInterface {
    static ChatCompletionFunctionTool tool() {
        throw new UnsupportedOperationException("ToolInterface.tool() is not implemented");
    }
    String execute(JsonNode arguments) throws Exception;
}
