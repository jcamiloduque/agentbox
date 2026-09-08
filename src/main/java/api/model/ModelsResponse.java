package api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelsResponse(
    List<ModelInfo> models,
    String object,
    List<Model> data
) {}
