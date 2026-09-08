package request;

public class ToolCall {
    private String id;
    private String name;
    private String arguments;
    private String response;
    private Status status;
    private long durationMs;

    public enum Status {
        RUNNING,
        COMPLETED,
        FAILED
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public void appendArguments(String additionalArguments) {
        if (this.arguments == null || this.arguments.isEmpty()) {
            this.arguments = additionalArguments;
        } else {
            this.arguments += additionalArguments;
        }
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
