import static dev.tamboui.toolkit.Toolkit.*;

import api.ChatSession;
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

import java.util.concurrent.atomic.AtomicInteger;

public class ClaudeTerminalUI extends ToolkitApp {

    // ── helpers.Config ────────────────────────────────────────────────────────────────
    private static final int AUTO_SCROLL_GAP_LINES = 3;
    private static final int MIN_INPUT_LINES       = 1;
    private static final int MAX_INPUT_LINES       = 4;
    private final Request request;
    private ChatSession session;

    private final TextAreaState inputState = new TextAreaState("");

    private final RichTextAreaElement chatArea = richTextArea()
        .wrapWord()
        .scrollbar()
        .rounded()
        .id("chat-log");

    private final AtomicInteger requestId = new AtomicInteger(0);

    private final InputHandler input = new InputHandler(
        inputState,
        this::isInputFocused,
        this::sendMessage,
        this::cancelCurrentRequest,
        this::quit
    );

    private boolean pendingScrollToBottom = true;

    public ClaudeTerminalUI(Request request) {
        super();

        this.session = request.createChatSession();
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
            text(session.getModel()).dim()
        ).rounded().borderColor(Color.CYAN).length(3);
    }

    private Element chatLog() {
        var chatText = ChatRenderer.format(session.getConversationHistory(), chatArea.state().viewportWidth());
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
        var turn = session.getCurrentTurn();
        if (turn == null) return false;
        return session.getCurrentTurn().getStatus() == ConversationTurn.TurnStatus.RUNNING;
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
        var ref = new Object() {
            String text = inputState.text().trim();
        };
        if (ref.text.isBlank() || isThinking()) return;
        ref.text = escJson(ref.text);
        if (ref.text.isBlank() || isThinking()) return;

        input.reset();
        requestScrollToBottom();

        int currentRequestId = requestId.incrementAndGet();

        Thread.ofVirtual().start(() -> {
            try {
                session.addMessage("user", ref.text);
                request.chat(session, runnable -> {
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
                    session.getCurrentTurn().setResponse(e.getMessage());
                    session.getCurrentTurn().setStatus(ConversationTurn.TurnStatus.FAILED);
                    if (nearBottom) requestScrollToBottom();
                });
            }
        });
    }

    private void cancelCurrentRequest() {
        requestId.incrementAndGet();
        if (session.getCurrentTurn() != null) {
            session.getCurrentTurn().setStatus(ConversationTurn.TurnStatus.FAILED);
            session.getCurrentTurn().setResponse("Request cancelled.");
        }
        request.cancelCurrentRequest();
        pendingScrollToBottom = true;
        input.reset();
    }

    private void newChat() {
        session = null;
        session = request.createChatSession();
        pendingScrollToBottom = true;
        input.reset();
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
