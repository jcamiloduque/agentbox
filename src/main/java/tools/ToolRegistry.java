package tools;

import api.ToolReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ToolRegistry {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Class<? extends ToolInterface>> toolsClasses = new HashMap<>();
    private final Map<String, ChatCompletionFunctionTool> toolSchemas = new HashMap<>();
    private final Map<String, ToolReference> toolReferences = new HashMap<>();

    public ToolRegistry() {
        // Register default tools
        registerTool(WriteTool.class);
        registerTool(ReadFileTool.class);
        registerTool(BashTool.class);
    }

    public void registerTool(Class<? extends ToolInterface> toolClass) {
        if (!toolClass.isAnnotationPresent(Tool.class)) {
            throw new IllegalArgumentException("Class " + toolClass.getSimpleName() + " is missing the @Tool annotation");
        }
        Tool toolAnnotation = toolClass.getAnnotation(Tool.class);
        String toolName = toolAnnotation.name();
        toolsClasses.put(toolName, toolClass);

        try {
            toolSchemas.put(toolName, (ChatCompletionFunctionTool) toolClass.getDeclaredMethod("tool").invoke(null));
            toolReferences.put(toolName, (ToolReference) toolClass.getDeclaredMethod("getReference").invoke(null));
        } catch (Exception e) {
            throw new RuntimeException("Failed to register tool: " + toolName, e);
        }
    }

    @NotNull
    public Iterable<ChatCompletionFunctionTool> getAllTools() {
        return toolSchemas.values();
    }

    public List<ToolReference> getAllToolReferences() {
        return new ArrayList<>(toolReferences.values());
    }

    public String executeTool(String toolName, String jsonArguments) throws Exception {
        Class<? extends ToolInterface> toolClass = toolsClasses.get(toolName);
        if (toolClass == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }
        ToolInterface toolInstance = toolClass.getDeclaredConstructor().newInstance();
        return toolInstance.execute(mapper.readTree(jsonArguments));
    }
}
