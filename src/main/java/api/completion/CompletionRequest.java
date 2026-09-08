package api.completion;

import api.ToolReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompletionRequest(
    String model,
    List<CompletionRequestMessage> messages,
    List<ToolReference> tools,
    boolean stream,
    @JsonProperty("max_tokens")
    Integer maxTokens,
    Double temperature,
    Double topP,
    Integer n,
    Boolean logprobs,
    Boolean echo,
    String stop
) {
    public CompletionRequest(String model, List<CompletionRequestMessage> messages, List<ToolReference> tools) {
        this(model, messages, tools, true, null, null, null, null, null, null, null);
    }

    public CompletionRequest(String model, List<CompletionRequestMessage> messages, List<ToolReference> tools, boolean stream) {
        this(model, messages, tools, stream, null, null, null, null, null, null, null);
    }
}
