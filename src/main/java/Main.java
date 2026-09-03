import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import tools.BashTool;
import tools.ReadFileTool;
import tools.ToolRegistry;
import tools.WriteTool;

import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        String apiKey = Config.get("OPENROUTER_API_KEY");
        String baseUrl = Config.get("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1");
        String model = Config.get("OPENROUTER_MODEL", "anthropic/claude-haiku-4.5");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerTool(ReadFileTool.class);
        toolRegistry.registerTool(WriteTool.class);
        toolRegistry.registerTool(BashTool.class);

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        var params = ChatCompletionCreateParams.builder()
                .model(model)
                .addUserMessage(prompt);

        for (var tool : toolRegistry.getAllTools()) {
            params.addTool(tool);
        }

        while (true) {
            AtomicBoolean exit = new AtomicBoolean(true);
            ChatCompletion response = client.chat().completions().create(params.build());

            if (response.choices().isEmpty()) {
                throw new RuntimeException("no choices in response");
            }

            response.choices().forEach(choice -> {
                var message = choice.message();
                params.addMessage(message);
                var toolCallsBase = message.toolCalls();
                if (toolCallsBase.isEmpty() || toolCallsBase.get().isEmpty()) {
                    message.content().ifPresent(System.out::println);
                }
                toolCallsBase.ifPresent(toolCalls -> {
                    toolCalls.forEach(toolCall -> {
                        if (toolCall.isFunction()) {
                            var asFunction = toolCall.asFunction();
                            var functionCall = asFunction.function();
                            try {
                                String toolResult = toolRegistry.executeTool(functionCall.name(), functionCall.arguments());
                                params.addMessage(ChatCompletionToolMessageParam.builder()
                                    .toolCallId(asFunction.id())
                                    .content(toolResult)
                                    .build());
                            } catch (Exception e) {
                                params.addMessage(ChatCompletionToolMessageParam.builder()
                                    .toolCallId(asFunction.id())
                                    .content("Error executing tool " + functionCall.name() + ": " + e.getMessage())
                                    .build());
                            } finally {
                                exit.set(false);
                            }
                        }
                    });
                });
            });

            if (exit.get()) {
                break;
            }
        }

    }
}
