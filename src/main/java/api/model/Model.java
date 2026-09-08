package api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Model {
    private String id;
    private String object;
    private Long created;

    @JsonProperty("owned_by")
    private String ownedBy;

    private ModelMeta meta;
    private List<String> capabilities;
    private List<String> aliases;

    public String getId() {
        return id;
    }

    public String getObject() {
        return object;
    }

    public Long getCreated() {
        return created;
    }

    public String getOwnedBy() {
        return ownedBy;
    }

    public ModelMeta getMeta() {
        return meta;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }
}

