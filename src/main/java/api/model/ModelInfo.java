package api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelInfo(
    String name,
    List<String> capabilities,
    String parameters
) {}