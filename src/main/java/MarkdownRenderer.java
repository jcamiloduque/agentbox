import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a markdown string into a list of TamboUI {@link Line}s.
 *
 * Supported constructs:
 *   Block:  # headings (h1–h6), fenced code blocks (```), bullet lists (- / *),
 *           ordered lists (1. 2.), blockquotes (>), horizontal rules (---, ***, ___),
 *           tables (| col | col |), blank lines
 *   Inline: **bold**, *italic*, _italic_, `code`, ~~strikethrough~~, [links](url),
 *           ![images](url), escaped characters, plain text
 *
 * Anything not matched renders as plain text — no crashes on unknown syntax.
 */
public final class MarkdownRenderer {

    // ── Styles ────────────────────────────────────────────────────────────────

    private static final Style H1    = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style H2    = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style H3    = Style.EMPTY.fg(Color.CYAN);
    private static final Style HN    = Style.EMPTY.fg(Color.CYAN).dim();
    private static final Style CODE_INLINE = Style.EMPTY.fg(Color.YELLOW);
    private static final Style CODE_BLOCK  = Style.EMPTY.fg(Color.YELLOW);
    private static final Style CODE_FENCE  = Style.EMPTY.fg(Color.DARK_GRAY);
    private static final Style BULLET      = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style ORDERED     = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style BLOCKQUOTE  = Style.EMPTY.fg(Color.GRAY).italic();
    private static final Style LINK        = Style.EMPTY.fg(Color.BLUE).underlined();
    private static final Style HR_STYLE    = Style.EMPTY.fg(Color.DARK_GRAY);
    private static final Style TABLE_HEADER = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style TABLE_BORDER = Style.EMPTY.fg(Color.DARK_GRAY);
    private static final Style PLAIN       = Style.EMPTY;

    // ── Regex Patterns ────────────────────────────────────────────────────────

    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\d+)\\.\\s+(.*)$");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\(([^\\)]+)\\)");
    private static final Pattern HR_PATTERN = Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$");
    private static final Pattern TABLE_PATTERN = Pattern.compile("^\\|(.+)\\|$");
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("\\\\(.)");

    // ── Syntax Highlighting Keywords ──────────────────────────────────────────

    private static final Map<String, List<String>> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("java", List.of("public", "private", "protected", "class", "interface", "extends", "implements",
            "void", "int", "String", "boolean", "static", "final", "new", "return", "if", "else", "for", "while",
            "try", "catch", "throw", "throws", "import", "package", "super", "this", "null", "true", "false"));
        KEYWORDS.put("python", List.of("def", "class", "import", "from", "if", "else", "elif", "for", "while",
            "return", "True", "False", "None", "and", "or", "not", "in", "is", "lambda", "try", "except", "finally"));
        KEYWORDS.put("bash", List.of("if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case",
            "esac", "function", "export", "declare", "local", "return", "echo", "read", "cd", "pwd"));
        KEYWORDS.put("javascript", List.of("function", "const", "let", "var", "class", "extends", "import", "export",
            "if", "else", "for", "while", "return", "true", "false", "null", "undefined", "new", "this", "async", "await"));
    }

    private static final Style KEYWORD_STYLE = Style.EMPTY.fg(Color.MAGENTA).bold();
    private static final Style STRING_STYLE = Style.EMPTY.fg(Color.GREEN);
    private static final Style COMMENT_STYLE = Style.EMPTY.fg(Color.DARK_GRAY).italic();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Render {@code markdown} into a list of {@link Line}s with an optional
     * left indent prefix applied to every non-fence line (e.g. {@code "Claude "}).
     */
    public static List<Line> render(String markdown, String firstPrefix, String contPrefix) {
        List<Line> out      = new ArrayList<>();
        String[]   rawLines = markdown.split("\n", -1);
        boolean    inCode   = false;
        String     lang     = "";
        int        lineNum  = 0;
        boolean    inTable  = false;
        List<String> tableLines = new ArrayList<>();

        for (int i = 0; i < rawLines.length; i++) {
            String raw      = rawLines[i];
            String trimmed  = raw.stripLeading();
            String prefix   = (i == 0) ? firstPrefix : contPrefix;

            // ── Fenced code block ─────────────────────────────────────────
            if (trimmed.startsWith("```")) {
                if (!inCode) {
                    lang   = trimmed.substring(3).trim();
                    inCode = true;
                    lineNum = 0;
                    String label = lang.isEmpty() ? "╶──" : "╶─ " + lang;
                    out.add(Line.from(
                        Span.styled(prefix, PLAIN),
                        Span.styled(label, CODE_FENCE)
                    ));
                } else {
                    inCode = false;
                    lang   = "";
                    out.add(Line.from(
                        Span.styled(prefix, PLAIN),
                        Span.styled("╶──", CODE_FENCE)
                    ));
                }
                continue;
            }

            if (inCode) {
                lineNum++;
                String lineNumStr = String.format("%2d │ ", lineNum);
                String codeLine = syntaxHighlight(raw, lang);
                out.add(Line.from(
                    Span.styled(prefix, PLAIN),
                    Span.styled(lineNumStr, CODE_FENCE),
                    Span.styled(codeLine, CODE_BLOCK)
                ));
                continue;
            }

            // ── Table handling ────────────────────────────────────────────
            if (TABLE_PATTERN.matcher(trimmed).matches()) {
                if (!inTable) {
                    inTable = true;
                    tableLines.clear();
                }
                tableLines.add(trimmed);
                
                // Check if next line is separator or end of table
                if (i + 1 < rawLines.length) {
                    String nextTrimmed = rawLines[i + 1].stripLeading();
                    if (!TABLE_PATTERN.matcher(nextTrimmed).matches()) {
                        renderTable(out, tableLines, prefix);
                        inTable = false;
                        tableLines.clear();
                    }
                } else {
                    renderTable(out, tableLines, prefix);
                    inTable = false;
                    tableLines.clear();
                }
                continue;
            } else if (inTable) {
                renderTable(out, tableLines, prefix);
                inTable = false;
                tableLines.clear();
            }

            // ── Horizontal rules ──────────────────────────────────────────
            if (HR_PATTERN.matcher(trimmed).matches()) {
                out.add(Line.from(
                    Span.styled(prefix, PLAIN),
                    Span.styled("─────────────────────────────────", HR_STYLE)
                ));
                continue;
            }

            // ── Heading ───────────────────────────────────────────────────
            if (trimmed.startsWith("#")) {
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') level++;
                if (level <= 6) {
                    String text  = trimmed.substring(level).stripLeading();
                    Style  style = switch (level) {
                        case 1  -> H1;
                        case 2  -> H2;
                        case 3  -> H3;
                        default -> HN;
                    };
                    List<Span> spans = new ArrayList<>();
                    spans.add(Span.styled(prefix, PLAIN));
                    spans.addAll(parseInline(text, style));
                    out.add(Line.from(spans));
                    continue;
                }
            }

            // ── Blockquote ────────────────────────────────────────────────
            if (trimmed.startsWith(">")) {
                int quoteLevel = 0;
                int idx = 0;
                while (idx < trimmed.length() && trimmed.charAt(idx) == '>') {
                    quoteLevel++;
                    idx++;
                }
                String quoteContent = trimmed.substring(idx).stripLeading();
                String indent = "  ".repeat(quoteLevel);
                List<Span> spans = new ArrayList<>();
                spans.add(Span.styled(prefix, PLAIN));
                spans.add(Span.styled(indent + "║ ", BLOCKQUOTE));
                spans.addAll(parseInline(quoteContent, BLOCKQUOTE));
                out.add(Line.from(spans));
                continue;
            }

            // ── Ordered list item ─────────────────────────────────────────
            Matcher orderedMatcher = ORDERED_LIST_PATTERN.matcher(trimmed);
            if (orderedMatcher.matches()) {
                String text = orderedMatcher.group(2);
                List<Span> spans = new ArrayList<>();
                spans.add(Span.styled(prefix, PLAIN));
                spans.add(Span.styled(orderedMatcher.group(1) + ". ", ORDERED));
                spans.addAll(parseInline(text, PLAIN));
                out.add(Line.from(spans));
                continue;
            }

            // ── Bullet list item ──────────────────────────────────────────
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                String text   = trimmed.substring(2);
                List<Span> spans = new ArrayList<>();
                spans.add(Span.styled(prefix, PLAIN));
                spans.add(Span.styled("• ", BULLET));
                spans.addAll(parseInline(text, PLAIN));
                out.add(Line.from(spans));
                continue;
            }

            // ── Blank line ────────────────────────────────────────────────
            if (trimmed.isEmpty()) {
                out.add(Line.empty());
                continue;
            }

            // ── Regular paragraph line ────────────────────────────────────
            List<Span> spans = new ArrayList<>();
            spans.add(Span.styled(prefix, PLAIN));
            spans.addAll(parseInline(trimmed, PLAIN));
            out.add(Line.from(spans));
        }

        return out;
    }

    // ── Table Renderer ────────────────────────────────────────────────────────

    private static void renderTable(List<Line> out, List<String> tableLines, String prefix) {
        if (tableLines.isEmpty()) return;

        List<List<String>> rows = new ArrayList<>();
        for (String line : tableLines) {
            String[] cells = line.split("\\|");
            List<String> row = new ArrayList<>();
            for (int i = 1; i < cells.length - 1; i++) {
                row.add(cells[i].trim());
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }

        if (rows.size() < 2) return;

        // Calculate column widths
        List<Integer> colWidths = new ArrayList<>();
        for (int i = 0; i < rows.get(0).size(); i++) {
            int maxWidth = 0;
            for (List<String> row : rows) {
                if (i < row.size()) {
                    maxWidth = Math.max(maxWidth, row.get(i).length());
                }
            }
            colWidths.add(maxWidth);
        }

        // Render header
        List<String> header = rows.get(0);
        StringBuilder headerLine = new StringBuilder();
        headerLine.append(prefix).append("┌");
        for (int i = 0; i < header.size(); i++) {
            headerLine.append("─".repeat(colWidths.get(i) + 2));
            if (i < header.size() - 1) headerLine.append("┬");
        }
        headerLine.append("┐");
        out.add(Line.from(Span.styled(headerLine.toString(), TABLE_BORDER)));

        // Render header cells
        List<Span> headerSpans = new ArrayList<>();
        headerSpans.add(Span.styled(prefix + "│ ", TABLE_BORDER));
        for (int i = 0; i < header.size(); i++) {
            String cell = header.get(i);
            int pad = colWidths.get(i) - cell.length();
            String padded = cell + " ".repeat(pad);
            headerSpans.add(Span.styled(padded, TABLE_HEADER));
            headerSpans.add(Span.styled(" │ ", TABLE_BORDER));
        }
        out.add(Line.from(headerSpans));

        // Render separator
        StringBuilder sepLine = new StringBuilder();
        sepLine.append(prefix).append("├");
        for (int i = 0; i < header.size(); i++) {
            sepLine.append("─".repeat(colWidths.get(i) + 2));
            if (i < header.size() - 1) sepLine.append("┼");
        }
        sepLine.append("┤");
        out.add(Line.from(Span.styled(sepLine.toString(), TABLE_BORDER)));

        // Render data rows
        for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
            List<String> row = rows.get(rowIdx);
            List<Span> rowSpans = new ArrayList<>();
            rowSpans.add(Span.styled(prefix + "│ ", TABLE_BORDER));
            for (int i = 0; i < header.size(); i++) {
                String cell = i < row.size() ? row.get(i) : "";
                int pad = colWidths.get(i) - cell.length();
                String padded = cell + " ".repeat(pad);
                rowSpans.add(Span.styled(padded, PLAIN));
                rowSpans.add(Span.styled(" │ ", TABLE_BORDER));
            }
            out.add(Line.from(rowSpans));
        }

        // Render footer
        StringBuilder footerLine = new StringBuilder();
        footerLine.append(prefix).append("└");
        for (int i = 0; i < header.size(); i++) {
            footerLine.append("─".repeat(colWidths.get(i) + 2));
            if (i < header.size() - 1) footerLine.append("┴");
        }
        footerLine.append("┘");
        out.add(Line.from(Span.styled(footerLine.toString(), TABLE_BORDER)));
    }

    // ── Syntax Highlighter ────────────────────────────────────────────────────

    private static String syntaxHighlight(String code, String lang) {
        if (lang.isEmpty() || !KEYWORDS.containsKey(lang)) {
            return code;
        }

        List<String> keywords = KEYWORDS.get(lang);
        String highlighted = code;

        // Highlight strings (simple: anything between quotes)
        highlighted = highlighted.replaceAll("(\"[^\"]*\"|'[^']*')", "\u001B[32m$1\u001B[0m");

        // Highlight comments
        if ("java".equals(lang) || "javascript".equals(lang)) {
            highlighted = highlighted.replaceAll("(//[^\n]*)", "\u001B[2m$1\u001B[0m");
        } else if ("python".equals(lang)) {
            highlighted = highlighted.replaceAll("(#[^\n]*)", "\u001B[2m$1\u001B[0m");
        } else if ("bash".equals(lang)) {
            highlighted = highlighted.replaceAll("(#[^\n]*)", "\u001B[2m$1\u001B[0m");
        }

        return highlighted;
    }

    // ── Inline parser ─────────────────────────────────────────────────────────

    /**
     * Parse inline markdown within a single line of text.
     * Supported: {@code **bold**}, {@code *italic*}, {@code _italic_},
     * {@code `code`}, {@code ~~strikethrough~~}, {@code [link](url)},
     * {@code ![image](url)}, {@code \escaped}
     */
    static List<Span> parseInline(String text, Style baseStyle) {
        List<Span> spans = new ArrayList<>();
        int        len   = text.length();
        int        pos   = 0;
        StringBuilder buf = new StringBuilder();

        while (pos < len) {
            // ── Images: ![alt](url) ───────────────────────────────────────
            if (pos < len && text.charAt(pos) == '!' && pos + 1 < len && text.charAt(pos + 1) == '[') {
                flushBuf(buf, baseStyle, spans);
                Matcher matcher = IMAGE_PATTERN.matcher(text.substring(pos));
                if (matcher.lookingAt()) {
                    String alt = matcher.group(1);
                    String url = matcher.group(2);
                    spans.add(Span.styled("[🖼 " + (alt.isEmpty() ? "image" : alt) + "]", LINK));
                    pos += matcher.group(0).length();
                    continue;
                }
                buf.append('!');
                pos++;
                continue;
            }

            // ── Links: [text](url) ────────────────────────────────────────
            if (text.charAt(pos) == '[') {
                flushBuf(buf, baseStyle, spans);
                Matcher matcher = LINK_PATTERN.matcher(text.substring(pos));
                if (matcher.lookingAt()) {
                    String linkText = matcher.group(1);
                    String url = matcher.group(2);
                    spans.add(Span.styled(linkText + " (" + url + ")", LINK));
                    pos += matcher.group(0).length();
                    continue;
                }
                buf.append('[');
                pos++;
                continue;
            }

            // ── Escaped characters: \x ───────────────────────────────────
            if (text.charAt(pos) == '\\' && pos + 1 < len) {
                flushBuf(buf, baseStyle, spans);
                buf.append(text.charAt(pos + 1));
                pos += 2;
                continue;
            }

            // ── ~~strikethrough~~ ─────────────────────────────────────────
            if (pos + 1 < len && text.charAt(pos) == '~' && text.charAt(pos + 1) == '~') {
                flushBuf(buf, baseStyle, spans);
                int end = text.indexOf("~~", pos + 2);
                if (end < 0) { buf.append(text, pos, len); pos = len; }
                else {
                    spans.add(Span.styled(text.substring(pos + 2, end),
                        baseStyle.crossedOut()));
                    pos = end + 2;
                }
                continue;
            }

            // ── **bold** ──────────────────────────────────────────────────
            if (pos + 1 < len && text.charAt(pos) == '*' && text.charAt(pos + 1) == '*') {
                flushBuf(buf, baseStyle, spans);
                int end = text.indexOf("**", pos + 2);
                if (end < 0) { buf.append(text, pos, len); pos = len; }
                else {
                    spans.add(Span.styled(text.substring(pos + 2, end),
                        baseStyle.bold()));
                    pos = end + 2;
                }
                continue;
            }

            // ── *italic* or _italic_ ──────────────────────────────────────
            char c = text.charAt(pos);
            if ((c == '*' || c == '_') && !(pos + 1 < len && text.charAt(pos + 1) == c && text.charAt(pos + 1) == '*')) {
                flushBuf(buf, baseStyle, spans);
                int end = text.indexOf(c, pos + 1);
                if (end < 0) { buf.append(text, pos, len); pos = len; }
                else {
                    spans.add(Span.styled(text.substring(pos + 1, end),
                        baseStyle.italic()));
                    pos = end + 1;
                }
                continue;
            }

            // ── `inline code` ─────────────────────────────────────────────
            if (c == '`') {
                flushBuf(buf, baseStyle, spans);
                int end = text.indexOf('`', pos + 1);
                if (end < 0) { buf.append(text, pos, len); pos = len; }
                else {
                    spans.add(Span.styled(text.substring(pos + 1, end), CODE_INLINE));
                    pos = end + 1;
                }
                continue;
            }

            buf.append(c);
            pos++;
        }

        flushBuf(buf, baseStyle, spans);
        return spans;
    }

    private static void flushBuf(StringBuilder buf, Style style, List<Span> out) {
        if (!buf.isEmpty()) {
            out.add(Span.styled(buf.toString(), style));
            buf.setLength(0);
        }
    }

    private MarkdownRenderer() {}
}
