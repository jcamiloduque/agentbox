import static dev.tamboui.toolkit.Toolkit.*;

import api.Request;
import dev.tamboui.layout.Flex;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.RichTextAreaElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextAreaState;
import request.ConversationTurn;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ClaudeTerminalUI extends ToolkitApp {

    // ── helpers.Config ────────────────────────────────────────────────────────────────

    private static final String API_KEY = System.getenv("ANTHROPIC_API_KEY");
    private static final String MODEL   = "claude-sonnet-4-6";
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String SYSTEM  = "You are a helpful, concise assistant. Keep replies short.";
    private static final int AUTO_SCROLL_GAP_LINES = 3;
    private static final int MIN_INPUT_LINES       = 1;
    private static final int MAX_INPUT_LINES       = 4;
    private final Request request;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Single source of truth — drives both rendering and API calls. */
    private final List<ConversationTurn> turns = new ArrayList<>();

    private final TextAreaState inputState = new TextAreaState("");

    private final RichTextAreaElement chatArea = richTextArea()
        .wrapWord()
        .scrollbar()
        .rounded()
        .id("chat-log");

    private final HttpClient    http      = HttpClient.newHttpClient();
    private final AtomicInteger requestId = new AtomicInteger(0);

    private final InputHandler input = new InputHandler(
        inputState,
        this::isInputFocused,
        this::sendMessage,
        this::newChat,
        this::quit
    );

    private boolean pendingScrollToBottom = true;

    public ClaudeTerminalUI(Request request) {
        super();

        this.request = request;
    }

    // ── TUI config ────────────────────────────────────────────────────────────

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
            .mouseCapture(true)
            .bracketedPaste(true)
            .build();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    protected Element render() {
        return column(
            headerPanel(),
            chatLog(),
            inputPanel(),
            hintBar()
        ).onKeyEvent(this::handleGlobalKey);
    }

    // ── Layout sections ───────────────────────────────────────────────────────

    private Element headerPanel() {
        return panel("Claude",
            text(MODEL).dim()
        ).rounded().borderColor(Color.CYAN).length(3);
    }

    private Element chatLog() {
        var chatText = ChatRenderer.format(turns, chatArea.state().viewportWidth());
        chatArea.text(chatText);
        if (pendingScrollToBottom) {
            chatArea.state().setContentHeight(chatText.height());
            chatArea.state().scrollToBottom();
            if (chatArea.state().viewportHeight() > 0) {
                pendingScrollToBottom = false;
            }
        }
        return chatArea.fill();
    }

    private boolean isThinking() {
        if (turns.isEmpty()) return false;
        ConversationTurn last = turns.get(turns.size() - 1);
        return last.getResponse() == null;
    }

    private Element inputPanel() {
        return panel("",
            row(
                text("❯ ").bold().white().fit(),
                textArea(inputState)
                    .placeholder("Type a message… (Enter to send, Alt/Option+Enter for newline)")
                    .id("chat-input")
                    .onTextChange(ignored -> input.handleTextChange())
                    .fill()
            ).flex(Flex.START)
        )
            .rounded()
            .borderColor(isThinking() ? Color.YELLOW : Color.DARK_GRAY)
            .focusedBorderColor(Color.CYAN)
            .id("input-panel")
            .length(inputHeight());
    }

    private Element hintBar() {
        return row(
            text("[Enter] Send").dim().fit(),
            spacer(),
            text("[Alt+Enter] New line").dim().fit(),
            spacer(),
            text("[Ctrl+C] Clear / quit").dim().fit(),
            spacer(),
            text("[Esc] Clear  [n] New  [q] Quit").dim().fit()
        ).length(1);
    }

    // ── Event routing ─────────────────────────────────────────────────────────

    private EventResult handleGlobalKey(KeyEvent event) {
        return input.handleKey(event);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = inputState.text().trim();
        if (text.isBlank() || isThinking()) return;

        ConversationTurn turn = new ConversationTurn();
        turn.setInput(text);
        turns.add(turn);
        input.reset();
        requestScrollToBottom();

        int currentRequestId = requestId.incrementAndGet();

        Thread.ofVirtual().start(() -> {
            try {
                request.send(turn, runnable -> {
                    runner().runOnRenderThread(() -> {
                        if (currentRequestId != requestId.get()) return;
                        boolean nearBottom = isNearBottom();
                        runnable.run();
                        if (nearBottom) requestScrollToBottom();
                    });
                });
            } catch (Exception e) {
                runner().runOnRenderThread(() -> {
                    if (currentRequestId != requestId.get()) return;
                    boolean nearBottom = isNearBottom();
                    turn.setResponse(e.getMessage());
                    turn.setStatus(ConversationTurn.TurnStatus.FAILED);
                    if (nearBottom) requestScrollToBottom();
                });
            }
        });
    }

    private void newChat() {
        turns.clear();
        pendingScrollToBottom = true;
        input.reset();
    }

    // ── API call ──────────────────────────────────────────────────────────────

    private String callApi() throws Exception {
        StringBuilder msgs = new StringBuilder("[");
        for (int i = 0; i < turns.size(); i++) {
            ConversationTurn turn = turns.get(i);
            if (i > 0) msgs.append(",");
            msgs.append("{\"role\":\"user\",\"content\":\"")
                .append(escJson(turn.getInput()))
                .append("\"}");
            if (turn.getResponse() != null) {
                msgs.append(",{\"role\":\"assistant\",\"content\":\"")
                    .append(escJson(turn.getResponse()))
                    .append("\"}");
            }
        }
        msgs.append("]");

        String body = """
                {"model":"%s","max_tokens":1024,"system":"%s","messages":%s}
                """.formatted(MODEL, escJson(SYSTEM), msgs).trim();

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("x-api-key", API_KEY)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        return extractText(res.body());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String extractText(String json) {
        int idx = json.indexOf("\"text\":");
        if (idx < 0) return "(no response)";
        int start = json.indexOf('"', idx + 7) + 1;
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        return json.substring(start, end)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    private static String escJson(String s) {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private void requestScrollToBottom() {
        pendingScrollToBottom = true;
    }

    private boolean isNearBottom() {
        int gap = chatArea.state().maxScrollRow() - chatArea.state().scrollRow();
        return gap <= AUTO_SCROLL_GAP_LINES;
    }

    private boolean isInputFocused() {
        if (runner() == null) return false;
        return "chat-input".equals(runner().focusManager().focusedId());
    }

    private int inputHeight() {
        int lines = Math.clamp(inputState.lineCount(), MIN_INPUT_LINES, MAX_INPUT_LINES);
        return lines + 2;
    }
}