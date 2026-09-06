package request;

import java.util.ArrayList;
import java.util.List;

public class ConversationTurn {
    private String input;
    private List<ToolCall> toolCalls = new ArrayList<>();
    private Boolean showDetails;
    private String response;
    private long durationMs = -1;
    private TurnStatus status = TurnStatus.RUNNING;

    public enum TurnStatus {
        RUNNING,
        COMPLETED,
        FAILED
    }

    public String getInput() {
        return input;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Boolean getShowDetails() {
        return showDetails;
    }

    public void setShowDetails(Boolean showDetails) {
        this.showDetails = showDetails;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public TurnStatus getStatus() {
        return status;
    }

    public void setStatus(TurnStatus status) {
        this.status = status;
    }
}
