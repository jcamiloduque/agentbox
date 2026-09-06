import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextAreaState;

/**
 * Handles all input-field logic for ClaudeTerminalUI:
 *   - global key routing (Ctrl+C, Escape, Enter, q, n)
 *   - submit-on-Enter vs. newline-on-Alt+Enter
 *   - paste safety: multi-char inserts never trigger auto-send
 *   - paste newline preservation: pasted text keeps its \n intact
 *   - hard-wrap of manually typed long lines
 */
public final class InputHandler {

    // ── Tuning constants ──────────────────────────────────────────────────────

    private static final long CTRL_C_QUIT_WINDOW_MS  = 1200;
    private static final long ESCAPE_ENTER_WINDOW_MS = 250;

    // ── Callbacks wired by the app ────────────────────────────────────────────

    /** Called when the user confirms a message (Enter). */
    public interface SendAction   { void send(); }
    /** Called when the user requests a new chat (n). */
    public interface NewChatAction { void newChat(); }
    /** Called when the app should quit (q / double Ctrl+C). */
    public interface QuitAction   { void quit(); }
    /** Returns true when the chat-input element currently has focus. */
    public interface FocusQuery   { boolean isInputFocused(); }


    // ── Wired dependencies ────────────────────────────────────────────────────

    private final TextAreaState  state;
    private final SendAction     onSend;
    private final NewChatAction  onNewChat;
    private final QuitAction     onQuit;
    private final FocusQuery     focusQuery;

    // ── Internal state ────────────────────────────────────────────────────────

    private long   lastCtrlCAtMs     = 0;
    private long   pendingEscapeAtMs = 0;
    private long   lastChangeAtMs    = 0;
    private String lastText          = "";

    // If two onTextChange callbacks arrive within this window we're in a
    // paste burst — don't submit until typing goes quiet.
    private static final long PASTE_BURST_WINDOW_MS = 30;

    // ── Constructor ───────────────────────────────────────────────────────────

    public InputHandler(
        TextAreaState  state,
        FocusQuery     focusQuery,
        SendAction     onSend,
        NewChatAction  onNewChat,
        QuitAction     onQuit) {
        this.state      = state;
        this.focusQuery = focusQuery;
        this.onSend     = onSend;
        this.onNewChat  = onNewChat;
        this.onQuit     = onQuit;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Call this from your column's {@code onKeyEvent} handler.
     */
    public EventResult handleKey(KeyEvent event) {
        // Ctrl+C: clear or quit
        if (event.hasCtrl() && event.isCharIgnoreCase('c')) {
            return handleCtrlC();
        }

        if (focusQuery.isInputFocused()) {
            // Escape: clear input
            if (event.code() == KeyCode.ESCAPE) {
                pendingEscapeAtMs = System.currentTimeMillis();
                clearInput();
                return EventResult.HANDLED;
            }
            // Plain Enter: send
            if (event.isConfirm() && !event.hasAlt() && !event.hasCtrl() && !event.hasShift()) {
                onSend.send();
                return EventResult.HANDLED;
            }
            // Everything else (q, n, arrow keys, Home/End, Cmd+Left/Right…)
            // must reach the textarea element — do NOT consume here.
            return EventResult.UNHANDLED;
        }

        // Input NOT focused — shortcut keys are safe to consume
        if (event.isCharIgnoreCase('q')) { onQuit.quit();       return EventResult.HANDLED; }
        if (event.isChar('n'))           { onNewChat.newChat(); return EventResult.HANDLED; }
        return EventResult.UNHANDLED;
    }

    /**
     * Call this from {@code textArea(...).onTextChange(...)}.
     *
     * <p>Behaviour:
     * <ul>
     *   <li>If the change was a paste (bulk insert) → preserve newlines, skip auto-send.</li>
     *   <li>If the change was a single-char trailing newline → submit (unless escaped).</li>
     *   <li>Otherwise → hard-wrap long typed lines.</li>
     * </ul>
     */
    public void handleTextChange() {
        long   now     = System.currentTimeMillis();
        String current = state.text();

        boolean isBurst = (now - lastChangeAtMs) < PASTE_BURST_WINDOW_MS;
        lastChangeAtMs = now;

        // Bulk insert (bracketed paste) or rapid-fire callbacks (char-by-char paste)
        // → keep text exactly as-is, never auto-send
        if (isBurst || countNewChars(lastText, current) > 1) {
            lastText = current;
            return;
        }

        // Single Enter → submit (unless Alt+Enter escape window is open)
        if (shouldSubmitOnTrailingNewline(lastText, current)) {
            if (!isEscapedEnter()) {
                onSend.send();
                return;
            }
            pendingEscapeAtMs = 0;
        }

        lastText = current;
    }

    /**
     * Reset all transient state. Call from {@code newChat()} and after send.
     */
    public void reset() {
        clearInput();
        pendingEscapeAtMs = 0;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private EventResult handleCtrlC() {
        long now = System.currentTimeMillis();
        if (now - lastCtrlCAtMs <= CTRL_C_QUIT_WINDOW_MS) {
            onQuit.quit();
            lastCtrlCAtMs = 0;
        } else {
            lastCtrlCAtMs = now;
            clearInput();
        }
        return EventResult.HANDLED;
    }

    private void clearInput() {
        state.clear();
        lastText = "";
        lastChangeAtMs = 0;
        pendingEscapeAtMs = 0;
    }

    /**
     * Returns the net number of characters added between {@code before} and
     * {@code after}. A single Enter keypress returns 1; a paste returns > 1.
     */
    private static int countNewChars(String before, String after) {
        if (before == null || after == null) return 0;
        return Math.max(0, after.length() - before.length());
    }

    private boolean shouldSubmitOnTrailingNewline(String previous, String current) {
        if (current == null || previous == null) return false;
        String prev = normalizeEol(previous);
        String curr = normalizeEol(current);
        if (!curr.endsWith("\n")) return false;
        return curr.substring(0, curr.length() - 1).equals(prev);
    }

    private boolean isEscapedEnter() {
        if (pendingEscapeAtMs == 0) return false;
        return System.currentTimeMillis() - pendingEscapeAtMs <= ESCAPE_ENTER_WINDOW_MS;
    }

    private static String normalizeEol(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }
}