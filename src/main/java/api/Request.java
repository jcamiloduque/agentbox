package api;

import api.completion.ChatCompletionChunk;
import api.completion.ErrorResponse;
import api.model.Model;
import api.model.ModelsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.Config;
import request.ConversationTurn;
import request.ToolCall;
import tools.ToolRegistry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Request {
    private final HttpClient httpClient;

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    protected final List<ToolReference> tools;
    private final ToolRegistry toolRegistry;

    private final ObjectMapper mapper;

    CompletableFuture<HttpResponse<Stream<String>>> promiseResponse;

    public Request() {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = Config.get("OPENROUTER_API_KEY");
        this.baseUrl = Config.get("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1");
        this.model = Config.get("OPENROUTER_MODEL", "anthropic/claude-haiku-4.5");
        this.mapper = new ObjectMapper();
        this.toolRegistry = new ToolRegistry();
        this.tools = toolRegistry.getAllToolReferences();
    }

    public List<Model> getModels() {
        HttpRequest.Builder request = createRequest("/models").GET();
        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            ModelsResponse modelsResponse = mapper.readValue(response.body(), ModelsResponse.class);

            var models = modelsResponse.data().stream().filter(model -> model.getObject().equals("model")).toList();

            for (var modelData : modelsResponse.models()) {
                for (Model model : models) {
                    if (model.getId().equals(modelData.name()) || model.getAliases().contains(modelData.name())) {
                        model.setCapabilities(modelData.capabilities());
                        break;
                    }
                }
            }

            return models;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void chat(ChatSession session, Consumer<Runnable> onUiUpdate) {
        boolean shouldContinue;
        do {
            shouldContinue = doChat(session, onUiUpdate);
            // need to append the tool responses to the session messages for the next turn
            if (shouldContinue) {
                List<ChatCompletionChunk.ToolCall> toolCalls = new ArrayList<>();
                session.addToolCallMessage(toolCalls);
                int i = 0;
                for (ToolCall toolCall : session.getCurrentTurn().getToolCalls()) {
                    ChatCompletionChunk.ToolCall toolCallMessage = new ChatCompletionChunk.ToolCall(
                        i,
                        toolCall.getId(),
                        "function",
                        new ChatCompletionChunk.Function(
                            toolCall.getName(),
                            toolCall.getArguments()
                        )
                    );
                    toolCalls.add(toolCallMessage);
                    session.addMessage(toolCall.getId(), toolCall.getName(), "tool", toolCall.getResponse());
                    i++;
                }

                onUiUpdate.accept(() -> {
                    var turn = session.getCurrentTurn();
                    turn.appendReasoning("\n");
                    for (ToolCall toolCall : turn.getToolCalls()) {
                        turn.appendReasoning("Tool " + toolCall.getName() + " executed with status: " + toolCall.getStatus() + "\n");
                        turn.appendReasoning("Response: " + toolCall.getResponse() + "\n");
                    }
                });

            }
        } while (shouldContinue);
    }

    private boolean doChat(ChatSession session, Consumer<Runnable> onUiUpdate) {
        long startTime = System.currentTimeMillis();
        var currentTurn = session.getCurrentTurn();
        boolean shouldContinue = false;
        onUiUpdate.accept(() -> {
            currentTurn.setStatus(ConversationTurn.TurnStatus.RUNNING);
        });

        String body;
        try {
            body = mapper.writeValueAsString(session.getRequestPayload());
        } catch (JsonProcessingException e) {
            onUiUpdate.accept(() -> {
                currentTurn.setStatus(ConversationTurn.TurnStatus.FAILED);
                currentTurn.setResponse("Failed to serialize request payload: " + e.getMessage());
            });
            return false;
        }

        HttpRequest.Builder request = createRequest("/chat/completions").POST(
            HttpRequest.BodyPublishers.ofString(body)
        );
        try {
            CompletableFuture<HttpResponse<Stream<String>>> promiseResponse = httpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.ofLines());

            HttpResponse<Stream<String>> response = promiseResponse.join();

            if (response.statusCode() != 200) {
                response.body().forEach(line -> {
                    try {
                        ErrorResponse error = mapper.readValue(line, ErrorResponse.class);
                        onUiUpdate.accept(() -> {
                            currentTurn.setStatus(ConversationTurn.TurnStatus.FAILED);
                            currentTurn.setResponse("API Error: " + response.statusCode() + ": " + error.error().message());
                        });
                    } catch (JsonProcessingException e) {
                        onUiUpdate.accept(() -> {
                            currentTurn.setStatus(ConversationTurn.TurnStatus.FAILED);
                            currentTurn.setResponse("API Error: " + response.statusCode() + ": " + line);
                        });
                    }
                });
                return false;
            }

            List<ToolCall> toolCalls = new ArrayList<>();
            response.body().forEach(line -> {
                ChatCompletionChunk chunk = getChatCompletionChunk(line);
                if (chunk != null) {
                    var choices = chunk.choices();
                    if (choices == null || choices.isEmpty()) {
                        onUiUpdate.accept(() -> {
                            currentTurn.setStatus(ConversationTurn.TurnStatus.FAILED);
                            currentTurn.setResponse("No choices in response chunk.");
                        });
                        return;
                    }
                    String reasoningContent = choices.get(0).delta().reasoningContent();
                    if (reasoningContent != null) {
                        onUiUpdate.accept(() -> {
                            currentTurn.appendReasoning(reasoningContent);
                        });
                    }
                    String content = choices.get(0).delta().content();
                    if (content != null) {
                        onUiUpdate.accept(() -> {
                            currentTurn.appendResponse(content);
                        });
                    }

                    List<ChatCompletionChunk.ToolCall> toolCallsChunk = choices.get(0).delta().toolCalls();
                    if (toolCallsChunk != null) {
                        for (int i = 0; i < toolCallsChunk.size(); i++) {
                            var toolCallChunk = toolCallsChunk.get(i);

                            ToolCall toolCall;
                            if (toolCalls.size() == i) {
                                toolCall = new ToolCall();
                                toolCall.setName(toolCallChunk.function().name());
                                toolCalls.add(toolCall);
                                toolCall.setId(toolCallChunk.id());
                            } else {
                                toolCall = toolCalls.get(i);
                            }
                            toolCall.appendArguments(toolCallChunk.function().arguments());
                        }
                    }
                }
            });

            if (!toolCalls.isEmpty()) {
                currentTurn.setToolCalls(toolCalls);
                // start executing tools
                List<Future<?>> futures = new ArrayList<>();
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    for (ToolCall toolCall : toolCalls) {
                        futures.add(executor.submit(() -> {
                            onUiUpdate.accept(() -> {
                                toolCall.setStatus(ToolCall.Status.RUNNING);
                            });
                            long toolStartTime = System.currentTimeMillis();
                            try {
                                String toolResponse = toolRegistry.executeTool(toolCall.getName(), toolCall.getArguments());
                                long toolEndTime = System.currentTimeMillis();
                                onUiUpdate.accept(() -> {
                                    toolCall.setStatus(ToolCall.Status.COMPLETED);
                                    toolCall.setResponse(toolResponse);
                                    toolCall.setDurationMs(toolEndTime - toolStartTime);
                                });
                            } catch (Exception e) {
                                long toolEndTime = System.currentTimeMillis();
                                onUiUpdate.accept(() -> {
                                    toolCall.setStatus(ToolCall.Status.FAILED);
                                    toolCall.setResponse("Tool execution failed: " + e.getMessage());
                                    toolCall.setDurationMs(toolEndTime - toolStartTime);
                                });
                            }
                        }));
                    }
                }

                for (Future<?> future : futures) {
                    future.get();
                }

                shouldContinue = true;
            }
        } catch (Exception e) {
            onUiUpdate.accept(() -> {
                currentTurn.setStatus(ConversationTurn.TurnStatus.FAILED);
                currentTurn.setResponse("Exception: " + e.getMessage());
            });
        }

        long endTime = System.currentTimeMillis();
        onUiUpdate.accept(() -> {
            currentTurn.setStatus(ConversationTurn.TurnStatus.COMPLETED);
            currentTurn.setDurationMs(endTime - startTime);
            String response = currentTurn.getResponse();
            if (response != null && !response.isEmpty()) {
                session.addMessage("assistant", currentTurn.getResponse());
            }
        });

        return shouldContinue;
    }

    public ChatSession createChatSession() {
        return new ChatSession(model, tools);
    }

    protected ChatCompletionChunk getChatCompletionChunk(String line) {
        // SSE streams can split JSON objects across multiple lines.
        // Reassemble until we hit "[DONE]" or a complete JSON object.
        StringBuilder chunk = new StringBuilder(line.length() + 6);
        String prefix = "data: ";
        boolean done = false;

        if (line.startsWith(prefix)) {
            String json = line.substring(prefix.length());

            if (!json.equals("[DONE]")) {
                chunk.append(json);
                try {
                    return mapper.readValue(chunk.toString(), ChatCompletionChunk.class);
                } catch (JsonProcessingException e) {
                    System.err.println("Error parsing JSON chunk: " + chunk.toString() + " → " + e.getMessage());
                    return null;
                }
            }

            // [DONE] signals the end of the stream — discard this chunk
        }

        return null;
    }

    protected HttpRequest.Builder createRequest(String endpoint) {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json");
        if (!apiKey.isEmpty()) {
            request.header("Authorization", "Bearer " + apiKey);
        }
        return request;
    }

    public void cancelCurrentRequest() {
        if (promiseResponse != null) {
            promiseResponse.cancel(true);
        }
    }

}
