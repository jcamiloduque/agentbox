package api;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import helpers.Config;
import request.ConversationTurn;
import tools.BashTool;
import tools.ReadFileTool;
import tools.ToolRegistry;
import tools.WriteTool;
import request.ToolCall;

import java.util.function.Consumer;

public class Request {

    protected final OpenAIClient client;
    protected ChatCompletionCreateParams.Builder params;
    protected final ToolRegistry toolRegistry = new ToolRegistry();

    public Request() {
        String apiKey = Config.get("OPENROUTER_API_KEY");
        String baseUrl = Config.get("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1");
        String model = Config.get("OPENROUTER_MODEL", "anthropic/claude-haiku-4.5");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        toolRegistry.registerTool(ReadFileTool.class);
        toolRegistry.registerTool(WriteTool.class);
        toolRegistry.registerTool(BashTool.class);

        this.client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();

        initializeParams(model);
    }

    private void initializeParams(String model) {
        this.params = ChatCompletionCreateParams.builder().model(model);

        for (var tool : toolRegistry.getAllTools()) {
            params.addTool(tool);
        }
    }

    public void send(ConversationTurn turn, Consumer<Runnable> onUiUpdate) {
        params.addUserMessage(turn.getInput());

        long startTime = System.currentTimeMillis();

        while (true) {
            boolean hasToolCalls = false;

            ChatCompletion response = client.chat().completions().create(params.build());

            if (response.choices().isEmpty()) {
                throw new RuntimeException("no choices in response");
            }

            for (var choice : response.choices()) {
                var message = choice.message();
                params.addMessage(message);

                var toolCallsOpt = message.toolCalls();

                // ── No tool calls → final assistant response ───────────────
                if (toolCallsOpt.isEmpty() || toolCallsOpt.get().isEmpty()) {
                    message.content().ifPresent(content -> {
                        onUiUpdate.accept(() -> {
                            turn.setResponse(content);
                            turn.setStatus(ConversationTurn.TurnStatus.COMPLETED);
                        });
                    });
                    continue;
                }

                // ── Tool calls → execute each, update turn live ────────────
                hasToolCalls = true;
                for (var toolCall : toolCallsOpt.get()) {
                    if (!toolCall.isFunction()) continue;

                    var asFunction   = toolCall.asFunction();
                    var functionCall = asFunction.function();

                    // Create the ToolCall record and mark it RUNNING
                    ToolCall tc = new ToolCall();
                    tc.setName(functionCall.name());
                    tc.setArguments(functionCall.arguments());
                    tc.setStatus(ToolCall.Status.RUNNING);

                    onUiUpdate.accept(() -> turn.getToolCalls().add(tc));

                    long start = System.currentTimeMillis();
                    try {
                        String result = toolRegistry.executeTool(
                            functionCall.name(),
                            functionCall.arguments()
                        );
                        long elapsed = System.currentTimeMillis() - start;

                        params.addMessage(ChatCompletionToolMessageParam.builder()
                            .toolCallId(asFunction.id())
                            .content(result)
                            .build());

                        onUiUpdate.accept(() -> {
                            tc.setResponse(result);
                            tc.setStatus(ToolCall.Status.COMPLETED);
                            tc.setDurationMs(elapsed);
                        });

                    } catch (Exception e) {
                        long elapsed = System.currentTimeMillis() - start;
                        String errMsg = "Error executing tool " + functionCall.name()
                            + ": " + e.getMessage();

                        params.addMessage(ChatCompletionToolMessageParam.builder()
                            .toolCallId(asFunction.id())
                            .content(errMsg)
                            .build());

                        onUiUpdate.accept(() -> {
                            tc.setResponse(errMsg);
                            tc.setStatus(ToolCall.Status.FAILED);
                            tc.setDurationMs(elapsed);
                        });
                    }
                }
            }

            if (!hasToolCalls) break;
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        onUiUpdate.accept(() -> {
            turn.setDurationMs(totalElapsed);
            turn.setStatus(ConversationTurn.TurnStatus.COMPLETED);
        });

    }
}
