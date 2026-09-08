package api.completion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionChunk(
    List<Choice> choices,
    long created,
    String id,
    String model,
    String systemFingerprint,
    String object,
    Timings timings
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
        String finishReason,
        int index,
        Delta delta
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Delta(
        String role,
        String content,
        @JsonProperty("reasoning_content")
        String reasoningContent,
        @JsonProperty("tool_calls")
        List<ToolCall> toolCalls
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCall(
        int index,
        String id,
        String type,
        Function function
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Function(
        String name,
        String arguments
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Timings(
        int cacheN,
        int promptN,
        double promptMs,
        double promptPerTokenMs,
        double promptPerSecond,
        int predictedN,
        double predictedMs,
        double predictedPerTokenMs,
        double predictedPerSecond
    ) {}
}
