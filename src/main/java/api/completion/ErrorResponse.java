package api.completion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponse(
    Error error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(
        String message,
        String type,
        String param,
        String code
    ) {}
}
