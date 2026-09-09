import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import request.ConversationTurn;
import request.ToolCall;

import java.util.ArrayList;
import java.util.List;

public final class ChatRenderer {

    private static final String USER_PREFIX_FIRST   = "❯ ";
    private static final String USER_PREFIX_CONT    = "  ";
    private static final String RESPONSE_PREFIX_FIRST = "⏺ ";
    private static final String RESPONSE_PREFIX_CONT = "  ";
    private static final String ERROR_PREFIX_FIRST = "⚠ Error: ";
    private static final String ERROR_PREFIX_CONT = "  ";
    private static final String TOOL_PREFIX         = "  ⚙ ";

    private static final Style USER_STYLE  = Style.EMPTY.fg(Color.BLACK).bg(Color.GRAY);
    private static final Style ERROR_STYLE = Style.EMPTY.fg(Color.RED).dim();
    private static final Style PROGRESS_STYLE = Style.EMPTY.fg(Color.YELLOW).dim();
    private static final Style TIME_STYLE = Style.EMPTY.fg(Color.GRAY).dim();
    private static final Style TOOL_RUNNING_STYLE   = Style.EMPTY.fg(Color.YELLOW);
    private static final Style TOOL_COMPLETED_STYLE = Style.EMPTY.fg(Color.GREEN).dim();
    private static final Style TOOL_FAILED_STYLE    = Style.EMPTY.fg(Color.RED);

    public static Text format(List<ConversationTurn> turns, int viewportWidth) {
        if (turns.isEmpty()) {
            return Text.from(Line.from(
                Span.raw("Hello! Ask me anything.").dim()
            ));
        }

        List<Line> lines = new ArrayList<>(turns.size() * 6);

        int animationFrameIndex = (int) ((System.currentTimeMillis() / 200) % 6);
        int toolAnimationFrameIndex = (int) ((System.currentTimeMillis() / 100) % 10);
        char[] animationFrames = {'·', '✢', '✳', '✶', '✻', '✽'};
        char[] toolAnimationFrames = {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};
        char animationFrame = animationFrames[animationFrameIndex];
        char toolAnimationFrame = toolAnimationFrames[toolAnimationFrameIndex];

        for (int i = 0; i < turns.size(); i++) {
            ConversationTurn turn = turns.get(i);

            if (turn.getInput() != null) {
                renderText(lines, viewportWidth, turn.getInput(), USER_PREFIX_FIRST, USER_PREFIX_CONT, USER_STYLE, false);
                lines.add(Line.from(Span.raw("")));
            }

            switch (turn.getStatus()) {
                case FAILED:
                    renderText(lines, viewportWidth, turn.getResponse(), ERROR_PREFIX_FIRST, ERROR_PREFIX_CONT, ERROR_STYLE, true);
                    break;
                case COMPLETED:
                    for (ToolCall tool : turn.getToolCalls()) {
                        lines.add(toolLine(tool, toolAnimationFrame));
                    }
                    renderText(lines, viewportWidth, turn.getResponse(), RESPONSE_PREFIX_FIRST, RESPONSE_PREFIX_CONT, Style.EMPTY, true);
                    lines.add(Line.from(Span.raw("")));
                    // ✻ Brewed for 1s · done 9:36 PM
                    lines.add(Line.from(Span.styled("✻ Brewed for " + turn.getDurationMs() + "ms · done " , TIME_STYLE)));
                    break;
                case RUNNING:
                    lines.add(Line.from(Span.styled(animationFrame + " Thinking…", PROGRESS_STYLE)));
                    renderText(lines, viewportWidth, turn.getReasoning(), "💭 ", "  ", Style.EMPTY.dim(), true);

                    for (ToolCall tool : turn.getToolCalls()) {
                        lines.add(toolLine(tool, toolAnimationFrame));
                    }

                    renderText(lines, viewportWidth, turn.getResponse(), RESPONSE_PREFIX_FIRST, RESPONSE_PREFIX_CONT, Style.EMPTY, true);

                    break;
            }

            if (i < turns.size() - 1) {
                lines.add(Line.empty());
            }
        }

        return Text.from(lines);
    }

    private static Line toolLine(ToolCall tool, char toolAnimationFrame) {
        if (tool.getStatus() == null) {
            return Line.from(Span.styled(TOOL_PREFIX + tool.getName() + "  ?", Style.EMPTY.dim()));
        }
        String status = switch (tool.getStatus()) {
            case RUNNING   -> toolAnimationFrame + " running";
            case COMPLETED -> "✓ done";
            case FAILED    -> "✗ failed";
            default        -> "?";
        };
        String duration = tool.getDurationMs() > 0
            ? " (" + tool.getDurationMs() + "ms)"
            : "";
        String label = TOOL_PREFIX + tool.getName() + "  " + status + duration;

        Style style = switch (tool.getStatus()) {
            case RUNNING   -> TOOL_RUNNING_STYLE;
            case COMPLETED -> TOOL_COMPLETED_STYLE;
            case FAILED    -> TOOL_FAILED_STYLE;
            default        -> Style.EMPTY.dim();
        };

        return Line.from(Span.styled(label, style));
    }

    private static String padToWidth(String text, int width) {
        if (width <= 0) return text;
        int len = text.length();
        if (len >= width) return text;
        return text + " ".repeat(width - len);
    }

    private static void renderText(List<Line> lines, int viewportWidth, String text, String firstPrefix, String contPrefix, Style style, boolean isRenderMarkdown) {
        if (text == null || text.isEmpty()) return;

        if (isRenderMarkdown) {
            var responses = MarkdownRenderer.render(text, firstPrefix, contPrefix);

            if (style.equals(Style.EMPTY)) {
                lines.addAll(responses);
            } else {
                for (var line : responses) {
                    lines.add(Line.from(Span.styled(line.rawContent(), style)));
                }
            }
            return;
        }

        String[] parts = text.split("\n", -1);
        for (int j = 0; j < parts.length; j++) {
            String prefix  = (j == 0) ? firstPrefix : contPrefix;
            String padded  = padToWidth(prefix + parts[j], viewportWidth);
            lines.add(Line.from(Span.styled(padded, style)));
        }
    }
}