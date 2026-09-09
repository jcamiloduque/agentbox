package api.completion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompletionRequestMessage {
    private String role;
    private String content;
    private String id;
    private String name;
    @JsonProperty("tool_calls")
    private List<ChatCompletionChunk.ToolCall> toolCalls;

    public CompletionRequestMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public CompletionRequestMessage(String id, String name, String role, String content) {
        this.role = role;
        this.content = content;
        this.id = id;
        this.name = name;
    }

    public CompletionRequestMessage(List<ChatCompletionChunk.ToolCall> toolCalls) {
        this.role = "assistant";
        this.toolCalls = toolCalls;
        this.content = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ChatCompletionChunk.ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ChatCompletionChunk.ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }
}
