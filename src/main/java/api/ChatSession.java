package api;

import api.completion.ChatCompletionChunk;
import api.completion.CompletionRequest;
import api.completion.CompletionRequestMessage;
import request.ConversationTurn;

import java.util.ArrayList;
import java.util.List;

public class ChatSession {
    List<CompletionRequestMessage> messages = new ArrayList<>();
    private final CompletionRequest requestPayload;
    private final List<ConversationTurn> conversationHistory = new ArrayList<>();
    private ConversationTurn currentTurn;
    private final String model;

    public ChatSession(String model, final List<ToolReference> toolReferences) {
        this.requestPayload = new CompletionRequest(model, messages, toolReferences, true);
        this.model = model;
    }

    public void addMessage(String role, String content) {
        messages.add(new CompletionRequestMessage(role, content));
        if (role.equals("user")) {
            ConversationTurn turn = new ConversationTurn(content);
            conversationHistory.add(turn);
            currentTurn = turn;
        }
    }

    public void addMessage(String id, String name, String role, String content) {
        messages.add(new CompletionRequestMessage(id, name, role, content));
        if (role.equals("user")) {
            ConversationTurn turn = new ConversationTurn(content);
            conversationHistory.add(turn);
            currentTurn = turn;
        }
    }

    public void addToolCallMessage(List<ChatCompletionChunk.ToolCall> toolCalls) {
        messages.add(new CompletionRequestMessage(toolCalls));
    }

    public CompletionRequest getRequestPayload() {
        return requestPayload;
    }

    public ConversationTurn getCurrentTurn() {
        return currentTurn;
    }

    public List<ConversationTurn> getConversationHistory() {
        return conversationHistory;
    }

    public String getModel() {
        return model;
    }
}
