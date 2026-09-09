import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

import java.util.*;
import java.util.regex.*;

public final class MarkdownRenderer {

    // ── Styles ────────────────────────────────────────────────────────────────

    private static final Style H1           = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style H2           = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style H3           = Style.EMPTY.fg(Color.CYAN);
    private static final Style HN           = Style.EMPTY.fg(Color.CYAN).dim();
    private static final Style CODE_INLINE  = Style.EMPTY.fg(Color.YELLOW);
    private static final Style CODE_BLOCK   = Style.EMPTY.fg(Color.YELLOW);
    private static final Style CODE_FENCE   = Style.EMPTY.fg(Color.DARK_GRAY);
    private static final Style BULLET       = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style ORDERED      = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style BLOCKQUOTE   = Style.EMPTY.fg(Color.GRAY).italic();
    private static final Style LINK         = Style.EMPTY.fg(Color.BLUE).underlined();
    private static final Style HR_STYLE     = Style.EMPTY.fg(Color.DARK_GRAY);
    private static final Style TABLE_HEADER = Style.EMPTY.fg(Color.CYAN).bold();
    private static final Style TABLE_BORDER = Style.EMPTY.fg(Color.DARK_GRAY);
    private static final Style TABLE_ROW    = Style.EMPTY.fg(Color.WHITE);
    private static final Style PLAIN        = Style.EMPTY;
    private static final Style KEYWORD_STYLE = Style.EMPTY.fg(Color.MAGENTA).bold();
    private static final Style STRING_STYLE  = Style.EMPTY.fg(Color.GREEN);
    private static final Style COMMENT_STYLE = Style.EMPTY.fg(Color.DARK_GRAY).italic();
    private static final Style NUMBER_STYLE  = Style.EMPTY.fg(Color.GREEN);
    private static final Style OPERATOR_STYLE = Style.EMPTY.fg(Color.MAGENTA);
    private static final Style IDENT_STYLE   = Style.EMPTY.fg(Color.WHITE);

    // ── Patterns ──────────────────────────────────────────────────────────────

    private static final Pattern ORDERED_LIST = Pattern.compile("^(\\d+)\\.\\s+(.*)$");
    private static final Pattern LINK_PAT     = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern IMAGE_PAT    = Pattern.compile("!\\[([^\\]]*)]\\(([^)]+)\\)");
    private static final Pattern HR_PAT       = Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$");
    private static final Pattern TABLE_PAT    = Pattern.compile("^\\|(.*)\\|$");
    private static final Pattern SEP_PAT      = Pattern.compile("^\\|?([-: ]+\\|)+[-: ]+\\|?$");

    // ── Keywords ──────────────────────────────────────────────────────────────

    private static final Map<String, List<String>> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("java",       List.of("public","private","protected","class","interface","extends",
            "implements","void","int","String","boolean","static","final","new","return","if","else",
            "for","while","try","catch","throw","throws","import","package","super","this","null","true","false"));
        KEYWORDS.put("python",     List.of("def","class","import","from","if","else","elif","for","while",
            "return","True","False","None","and","or","not","in","is","lambda","try","except","finally"));
        KEYWORDS.put("bash",       List.of("if","then","else","elif","fi","for","while","do","done","case",
            "esac","function","export","declare","local","return","echo","read","cd","pwd"));
        KEYWORDS.put("javascript", List.of("function","const","let","var","class","extends","import","export",
            "if","else","for","while","return","true","false","null","undefined","new","this","async","await"));
        KEYWORDS.put("typescript", List.of("function","const","let","var","class","extends","import","export",
            "if","else","for","while","return","true","false","null","undefined","new","this","async","await",
            "type","interface","enum","readonly","as","keyof","typeof","never","any","unknown"));
        KEYWORDS.put("go",         List.of("func","var","const","type","struct","interface","package",
            "import","return","if","else","for","defer","go","chan","select","case","default","range"));
        KEYWORDS.put("rust",       List.of("fn","struct","impl","trait","mut","let","match","if","else",
            "const","static","pub","crate","mod","use","type","enum","loop","while","for","return","self"));
        KEYWORDS.put("c",          List.of("int","char","float","double","void","return","if","else","for",
            "while","do","switch","case","break","continue","struct","typedef","include","define","NULL"));
        KEYWORDS.put("cpp",        List.of("class","public","private","protected","virtual","override","const",
            "template","typename","namespace","using","include","nullptr","auto","new","delete"));
    }

    private static final Set<Character> OPERATORS = Set.of(
        '+','-','*','/','=','<','>','!','&','|','%','^','~',
        '(',')','[',']','{','}',',',';',':','.',  '?'
    );

    // ── Public API ────────────────────────────────────────────────────────────

    public static List<Line> render(String markdown, String firstPrefix, String contPrefix) {
        List<Line>   out      = new ArrayList<>();
        String[]     rawLines = markdown.split("\n", -1);
        boolean      inCode   = false;
        String       lang     = "";
        int          lineNum  = 0;
        boolean      inTable  = false;
        List<String> tableBuf = new ArrayList<>();

        for (int i = 0; i < rawLines.length; i++) {
            String raw     = rawLines[i];
            String trimmed = raw.stripLeading();
            String prefix  = (i == 0) ? firstPrefix : contPrefix;

            // ── Fenced code block ─────────────────────────────────────────
            if (trimmed.startsWith("```")) {
                // Flush any pending table first
                if (inTable) { renderTable(out, tableBuf, prefix); inTable = false; tableBuf.clear(); }

                if (!inCode) {
                    lang    = trimmed.substring(3).trim().toLowerCase(Locale.ROOT);
                    inCode  = true;
                    lineNum = 0;
                    String label = lang.isEmpty() ? "╶──" : "╶─ " + lang;
                    out.add(Line.from(Span.styled(prefix, PLAIN), Span.styled(label, CODE_FENCE)));
                } else {
                    inCode = false;
                    lang   = "";
                    out.add(Line.from(Span.styled(prefix, PLAIN), Span.styled("╶──", CODE_FENCE)));
                }
                continue;
            }

            if (inCode) {
                lineNum++;
                String lineNumStr = String.format("%3d│ ", lineNum);
                List<Span> codeSpans = new ArrayList<>();
                codeSpans.add(Span.styled(prefix, PLAIN));
                codeSpans.add(Span.styled(lineNumStr, CODE_FENCE));
                // FIX: syntaxHighlight now returns List<Span> so per-token styles are preserved
                codeSpans.addAll(syntaxHighlight(raw, lang));
                out.add(Line.from(codeSpans));
                continue;
            }

            // ── Table accumulation ────────────────────────────────────────
            if (TABLE_PAT.matcher(trimmed).matches()) {
                if (!inTable) { inTable = true; tableBuf.clear(); }
                tableBuf.add(trimmed);
                boolean nextIsTable = (i + 1 < rawLines.length)
                    && TABLE_PAT.matcher(rawLines[i + 1].stripLeading()).matches();
                if (!nextIsTable) {
                    renderTable(out, tableBuf, prefix);
                    inTable = false;
                    tableBuf.clear();
                }
                continue;
            } else if (inTable) {
                renderTable(out, tableBuf, prefix);
                inTable = false;
                tableBuf.clear();
            }

            // ── Horizontal rule ───────────────────────────────────────────
            if (HR_PAT.matcher(trimmed).matches()) {
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
                if (level <= 6 && level < trimmed.length() && trimmed.charAt(level) == ' ') {
                    String text  = trimmed.substring(level + 1);
                    Style  style = switch (level) {
                        case 1 -> H1; case 2 -> H2; case 3 -> H3; default -> HN;
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
                while (quoteLevel < trimmed.length() && trimmed.charAt(quoteLevel) == '>') quoteLevel++;
                String content = trimmed.substring(quoteLevel).stripLeading();
                List<Span> spans = new ArrayList<>();
                spans.add(Span.styled(prefix, PLAIN));
                spans.add(Span.styled("  ".repeat(quoteLevel) + "║ ", BLOCKQUOTE));
                spans.addAll(parseInline(content, BLOCKQUOTE));
                out.add(Line.from(spans));
                continue;
            }

            // ── Ordered list ──────────────────────────────────────────────
            Matcher om = ORDERED_LIST.matcher(trimmed);
            if (om.matches()) {
                List<Span> spans = new ArrayList<>();
                spans.add(Span.styled(prefix, PLAIN));
                spans.add(Span.styled(om.group(1) + ". ", ORDERED));
                spans.addAll(parseInline(om.group(2), PLAIN));
                out.add(Line.from(spans));
                continue;
            }

            // ── Bullet list ───────────────────────────────────────────────
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                List<Span> spans = new ArrayList<>();
                spans.add(Span.styled(prefix, PLAIN));
                spans.add(Span.styled("• ", BULLET));
                spans.addAll(parseInline(trimmed.substring(2), PLAIN));
                out.add(Line.from(spans));
                continue;
            }

            // ── Blank line ────────────────────────────────────────────────
            if (trimmed.isEmpty()) {
                out.add(Line.empty());
                continue;
            }

            // ── Paragraph ─────────────────────────────────────────────────
            List<Span> spans = new ArrayList<>();
            spans.add(Span.styled(prefix, PLAIN));
            spans.addAll(parseInline(trimmed, PLAIN));
            out.add(Line.from(spans));
        }

        return out;
    }

    // ── Table Renderer ────────────────────────────────────────────────────────
    private static final int MAX_COL_WIDTH = 38;

    private static void renderTable(List<Line> out, List<String> tableLines, String prefix) {
        if (tableLines.size() < 2) return;

        List<List<String>> rows     = new ArrayList<>();
        int                sepIndex = -1;

        for (int i = 0; i < tableLines.size(); i++) {
            if (SEP_PAT.matcher(tableLines.get(i)).matches()) {
                sepIndex = i;
                continue;
            }
            String[] cells = tableLines.get(i).split("\\|", -1);
            List<String> row = new ArrayList<>();
            for (int j = 1; j < cells.length - 1; j++) row.add(cells[j].trim());
            rows.add(row);
        }

        if (rows.isEmpty()) return;

        int colCount = rows.stream().mapToInt(List::size).max().orElse(0);

        // Measure using visible (stripped) length, then clamp
        int[] widths = new int[colCount];
        for (List<String> row : rows) {
            for (int c = 0; c < row.size(); c++) {
                widths[c] = Math.max(widths[c], visibleLength(row.get(c)));
            }
        }
        for (int c = 0; c < colCount; c++) {
            widths[c] = Math.min(widths[c], MAX_COL_WIDTH);
        }

        out.add(Line.from(Span.styled(buildBorderLine(prefix, widths, "┌", "┬", "┐", "─"), TABLE_BORDER)));

        for (int r = 0; r < rows.size(); r++) {
            List<String> row      = rows.get(r);
            boolean      isHeader = (sepIndex > 0 && r == 0);

            List<Span> spans = new ArrayList<>();
            spans.add(Span.styled(prefix + "│", TABLE_BORDER));

            for (int c = 0; c < colCount; c++) {
                String raw = c < row.size() ? row.get(c) : "";

                spans.add(Span.styled(" ", TABLE_BORDER));

                if (isHeader) {
                    // Headers are plain text — safe to truncate directly and pad
                    String cell = padVisible(truncateVisible(raw, widths[c]), widths[c]);
                    spans.add(Span.styled(cell, TABLE_HEADER));
                } else {
                    // For data cells: clip the raw markdown to visible width,
                    // then parse inline so **bold** / `code` still render correctly
                    String clipped = truncateRawToVisible(raw, widths[c]);
                    List<Span> cellSpans = parseInline(clipped, TABLE_ROW);
                    // Pad to fill the column: measure how many visible chars parseInline produced
                    int rendered = visibleLength(clipped);
                    int pad      = widths[c] - rendered;
                    spans.addAll(cellSpans);
                    if (pad > 0) spans.add(Span.styled(" ".repeat(pad), TABLE_ROW));
                }

                spans.add(Span.styled(" │", TABLE_BORDER));
            }

            out.add(Line.from(spans));

            if (isHeader) {
                out.add(Line.from(Span.styled(buildBorderLine(prefix, widths, "├", "┼", "┤", "─"), TABLE_BORDER)));
            } else if (r == rows.size() - 1) {
                out.add(Line.from(Span.styled(buildBorderLine(prefix, widths, "└", "┴", "┘", "─"), TABLE_BORDER)));
            }
        }
    }

    // ── Width helpers ─────────────────────────────────────────────────────────

    /**
     * Visible length of a markdown string: strip inline markers so that
     * **bold** counts as 4 chars, not 8, and `code` counts as 4, not 6.
     */
    private static int visibleLength(String s) {
        return stripMarkdown(s).length();
    }

    /**
     * Strip markdown inline markers from a string, leaving only the visible text.
     * Order matters: process ** before * to avoid partial matches.
     */
    private static String stripMarkdown(String s) {
        return s.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
            .replaceAll("~~(.+?)~~",         "$1")
            .replaceAll("`(.+?)`",           "$1")
            .replaceAll("\\*(.+?)\\*",       "$1")
            .replaceAll("_(.+?)_",           "$1")
            .replaceAll("!?\\[([^\\]]*)]\\([^)]*\\)", "$1");
    }

    /**
     * Truncate plain visible text to maxWidth, appending "…" if cut.
     */
    private static String truncateVisible(String s, int maxWidth) {
        if (s.length() <= maxWidth) return s;
        return s.substring(0, Math.max(0, maxWidth - 1)) + "…";
    }

    /**
     * Pad a plain string to exactly width visible chars with trailing spaces.
     */
    private static String padVisible(String s, int width) {
        int vlen = s.length(); // already plain text here
        return vlen >= width ? s : s + " ".repeat(width - vlen);
    }

    /**
     * Truncate a raw markdown string so that its VISIBLE length ≤ maxWidth.
     * Walks the stripped version char-by-char to find the cutoff point in the
     * original, preserving inline markers for any content that fits.
     *
     * e.g. truncateRawToVisible("`syntaxHighlight()`", 10)
     *      → "`syntaxHig…`"  (visible: "syntaxHig…" = 10)
     */
    private static String truncateRawToVisible(String raw, int maxWidth) {
        String stripped = stripMarkdown(raw);
        if (stripped.length() <= maxWidth) return raw; // fits — no truncation needed

        // We need to find where to cut the raw string so visible chars ≤ maxWidth-1,
        // then append "…". Walk both strings in parallel.
        int visCount  = 0;
        int rawPos    = 0;
        int rawLen    = raw.length();
        int targetVis = maxWidth - 1; // leave room for "…"

        while (rawPos < rawLen && visCount < targetVis) {
            // Skip a markdown marker if one starts here, copy it verbatim
            String remaining = raw.substring(rawPos);

            if (remaining.startsWith("**")) {
                // Check if closing ** exists within remaining
                int close = raw.indexOf("**", rawPos + 2);
                if (close >= 0) {
                    // Count the inner visible chars
                    String inner = raw.substring(rawPos + 2, close);
                    if (visCount + inner.length() <= targetVis) {
                        // Whole bold token fits
                        visCount += inner.length();
                        rawPos = close + 2;
                        continue;
                    }
                    // Partial bold: drop the marker, take as plain
                }
            }
            if (remaining.startsWith("`")) {
                int close = raw.indexOf('`', rawPos + 1);
                if (close >= 0) {
                    String inner = raw.substring(rawPos + 1, close);
                    if (visCount + inner.length() <= targetVis) {
                        visCount += inner.length();
                        rawPos = close + 1;
                        continue;
                    }
                }
            }
            if (remaining.startsWith("~~")) {
                int close = raw.indexOf("~~", rawPos + 2);
                if (close >= 0) {
                    String inner = raw.substring(rawPos + 2, close);
                    if (visCount + inner.length() <= targetVis) {
                        visCount += inner.length();
                        rawPos = close + 2;
                        continue;
                    }
                }
            }

            // Plain character — counts as one visible char
            visCount++;
            rawPos++;
        }

        return raw.substring(0, rawPos) + "…";
    }

    private static String buildBorderLine(String prefix, int[] widths, String l, String m, String r, String fill) {
        StringBuilder sb = new StringBuilder(prefix).append(l);
        for (int i = 0; i < widths.length; i++) {
            sb.append(fill.repeat(widths[i] + 2));
            sb.append(i < widths.length - 1 ? m : r);
        }
        return sb.toString();
    }

    private static String pad(String s, int width) {
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }

    // ── Syntax Highlighter ────────────────────────────────────────────────────

    /**
     * FIX: Returns List<Span> instead of String so per-token styles are
     * actually applied in the rendered output — not lost inside a single Span.
     */
    private static List<Span> syntaxHighlight(String code, String lang) {
        List<String> langKeywords = KEYWORDS.get(lang); // null = no highlighting
        List<Span>   spans        = new ArrayList<>();
        int          pos          = 0;
        int          len          = code.length();

        while (pos < len) {
            char c = code.charAt(pos);

            // Line comment (//)
            if (c == '/' && pos + 1 < len && code.charAt(pos + 1) == '/') {
                spans.add(Span.styled(code.substring(pos), COMMENT_STYLE));
                break;
            }
            // Block comment (/* ... */)
            if (c == '/' && pos + 1 < len && code.charAt(pos + 1) == '*') {
                int end = code.indexOf("*/", pos + 2);
                end = end < 0 ? len : end + 2;
                spans.add(Span.styled(code.substring(pos, end), COMMENT_STYLE));
                pos = end;
                continue;
            }
            // Hash comment (#)
            if (c == '#' && (lang.equals("python") || lang.equals("bash"))) {
                spans.add(Span.styled(code.substring(pos), COMMENT_STYLE));
                break;
            }
            // Double-quoted string
            if (c == '"') {
                int end = pos + 1;
                while (end < len && !(code.charAt(end) == '"' && code.charAt(end - 1) != '\\')) end++;
                end = Math.min(end + 1, len);
                spans.add(Span.styled(code.substring(pos, end), STRING_STYLE));
                pos = end;
                continue;
            }
            // Single-quoted string
            if (c == '\'') {
                int end = pos + 1;
                while (end < len && !(code.charAt(end) == '\'' && code.charAt(end - 1) != '\\')) end++;
                end = Math.min(end + 1, len);
                spans.add(Span.styled(code.substring(pos, end), STRING_STYLE));
                pos = end;
                continue;
            }
            // Number
            if (Character.isDigit(c)) {
                int end = pos;
                while (end < len && (Character.isDigit(code.charAt(end)) || code.charAt(end) == '.')) end++;
                spans.add(Span.styled(code.substring(pos, end), NUMBER_STYLE));
                pos = end;
                continue;
            }
            // Identifier or keyword
            if (Character.isLetter(c) || c == '_' || c == '$') {
                int end = pos;
                while (end < len && (Character.isLetterOrDigit(code.charAt(end))
                    || code.charAt(end) == '_' || code.charAt(end) == '$')) end++;
                String word = code.substring(pos, end);
                boolean isKw = langKeywords != null && langKeywords.contains(word);
                spans.add(Span.styled(word, isKw ? KEYWORD_STYLE : IDENT_STYLE));
                pos = end;
                continue;
            }
            // Operator
            if (OPERATORS.contains(c)) {
                spans.add(Span.styled(String.valueOf(c), OPERATOR_STYLE));
                pos++;
                continue;
            }
            // Whitespace / other — plain
            spans.add(Span.styled(String.valueOf(c), CODE_BLOCK));
            pos++;
        }

        return spans.isEmpty() ? List.of(Span.styled(code, CODE_BLOCK)) : spans;
    }

    // ── Inline Parser ─────────────────────────────────────────────────────────

    static List<Span> parseInline(String text, Style baseStyle) {
        List<Span>    spans = new ArrayList<>();
        int           len   = text.length();
        int           pos   = 0;
        StringBuilder buf   = new StringBuilder();

        while (pos < len) {
            char c = text.charAt(pos);

            // Image: ![alt](url)
            if (c == '!' && pos + 1 < len && text.charAt(pos + 1) == '[') {
                flushBuf(buf, baseStyle, spans);
                Matcher m = IMAGE_PAT.matcher(text).region(pos, len);
                if (m.lookingAt()) {
                    String alt = m.group(1);
                    spans.add(Span.styled("[🖼 " + (alt.isEmpty() ? "image" : alt) + "]", LINK));
                    pos += m.group(0).length();
                    continue;
                }
            }

            // Link: [text](url)
            if (c == '[') {
                flushBuf(buf, baseStyle, spans);
                Matcher m = LINK_PAT.matcher(text).region(pos, len);
                if (m.lookingAt()) {
                    spans.add(Span.styled(m.group(1) + " (" + m.group(2) + ")", LINK));
                    pos += m.group(0).length();
                    continue;
                }
            }

            // Escape: \x
            if (c == '\\' && pos + 1 < len) {
                flushBuf(buf, baseStyle, spans);
                buf.append(text.charAt(pos + 1));
                pos += 2;
                continue;
            }

            // ~~strikethrough~~
            if (c == '~' && pos + 1 < len && text.charAt(pos + 1) == '~') {
                flushBuf(buf, baseStyle, spans);
                int end = text.indexOf("~~", pos + 2);
                if (end < 0) { buf.append(text, pos, len); pos = len; }
                else { spans.add(Span.styled(text.substring(pos + 2, end), Style.EMPTY.crossedOut())); pos = end + 2; }
                continue;
            }

            // **bold**
            if (c == '*' && pos + 1 < len && text.charAt(pos + 1) == '*') {
                flushBuf(buf, baseStyle, spans);
                int end = text.indexOf("**", pos + 2);
                if (end < 0) { buf.append(text, pos, len); pos = len; }
                else { spans.add(Span.styled(text.substring(pos + 2, end), baseStyle.bold())); pos = end + 2; }
                continue;
            }

            // *italic* or _italic_
            // FIX: original guard `text.charAt(pos+1) == c && text.charAt(pos+1) == '*'`
            // was never true for '_', meaning _italic_ was silently swallowed.
            if ((c == '*' || c == '_') && !(pos + 1 < len && text.charAt(pos + 1) == c)) {
                flushBuf(buf, baseStyle, spans);
                int end = text.indexOf(c, pos + 1);
                if (end < 0) { buf.append(text, pos, len); pos = len; }
                else { spans.add(Span.styled(text.substring(pos + 1, end), baseStyle.italic())); pos = end + 1; }
                continue;
            }

            // `code`
            if (c == '`') {
                flushBuf(buf, baseStyle, spans);
                int end = text.indexOf('`', pos + 1);
                if (end < 0) { buf.append(text, pos, len); pos = len; }
                else { spans.add(Span.styled(text.substring(pos + 1, end), CODE_INLINE)); pos = end + 1; }
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