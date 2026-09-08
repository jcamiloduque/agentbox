package api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelMeta(
    @JsonProperty("vocab_type")
    Integer vocabType,

    @JsonProperty("n_vocab")
    Integer vocabSize,

    @JsonProperty("n_ctx")
    Integer contextLength,

    @JsonProperty("n_ctx_train")
    Integer trainingContextLength,

    @JsonProperty("n_embd")
    Integer embeddingSize,

    @JsonProperty("n_params")
    Long parameterCount,

    Long size,

    String ftype
) {}
