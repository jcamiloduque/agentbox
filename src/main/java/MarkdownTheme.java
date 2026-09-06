import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * Centralized markdown styling configuration.
 * Allows easy customization of colors and styles for all markdown elements.
 */
public class MarkdownTheme {

    // ── Heading Styles ────────────────────────────────────────────────────────

    public static final Style HEADING_1 = Style.EMPTY.fg(Color.CYAN).bold();
    public static final Style HEADING_2 = Style.EMPTY.fg(Color.CYAN).bold();
    public static final Style HEADING_3 = Style.EMPTY.fg(Color.CYAN);
    public static final Style HEADING_N = Style.EMPTY.fg(Color.CYAN).dim();

    // ── Code Styles ───────────────────────────────────────────────────────────

    public static final Style CODE_INLINE = Style.EMPTY.fg(Color.YELLOW);
    public static final Style CODE_BLOCK = Style.EMPTY.fg(Color.YELLOW);
    public static final Style CODE_FENCE = Style.EMPTY.fg(Color.DARK_GRAY);

    // ── Keyword Styles ────────────────────────────────────────────────────────

    public static final Style KEYWORD = Style.EMPTY.fg(Color.MAGENTA).bold();
    public static final Style STRING = Style.EMPTY.fg(Color.GREEN);
    public static final Style COMMENT = Style.EMPTY.fg(Color.DARK_GRAY).italic();
    public static final Style LINE_NUMBER = Style.EMPTY.fg(Color.DARK_GRAY);

    // ── List Styles ───────────────────────────────────────────────────────────

    public static final Style BULLET_MARKER = Style.EMPTY.fg(Color.CYAN).bold();
    public static final Style ORDERED_MARKER = Style.EMPTY.fg(Color.CYAN).bold();

    // ── Blockquote Styles ─────────────────────────────────────────────────────

    public static final Style BLOCKQUOTE_TEXT = Style.EMPTY.fg(Color.GRAY).italic();
    public static final Style BLOCKQUOTE_MARKER = Style.EMPTY.fg(Color.GRAY).italic();

    // ── Link & Image Styles ───────────────────────────────────────────────────

    public static final Style LINK = Style.EMPTY.fg(Color.BLUE).underlined();
    public static final Style IMAGE = Style.EMPTY.fg(Color.BLUE);

    // ── Table Styles ──────────────────────────────────────────────────────────

    public static final Style TABLE_HEADER = Style.EMPTY.fg(Color.CYAN).bold();
    public static final Style TABLE_CELL = Style.EMPTY;
    public static final Style TABLE_BORDER = Style.EMPTY.fg(Color.DARK_GRAY);

    // ── Horizontal Rule Styles ────────────────────────────────────────────────

    public static final Style HORIZONTAL_RULE = Style.EMPTY.fg(Color.DARK_GRAY);

    // ── Plain Text ────────────────────────────────────────────────────────────

    public static final Style PLAIN = Style.EMPTY;

    // ── Utility Methods ───────────────────────────────────────────────────────

    /**
     * Get heading style based on level (1-6+).
     */
    public static Style getHeadingStyle(int level) {
        return switch (level) {
            case 1 -> HEADING_1;
            case 2 -> HEADING_2;
            case 3 -> HEADING_3;
            default -> HEADING_N;
        };
    }

    /**
     * Create a light theme variant.
     */
    public static class LightTheme extends MarkdownTheme {
        // Can override styles here for light theme
    }

    /**
     * Create a dark theme variant.
     */
    public static class DarkTheme extends MarkdownTheme {
        // Can override styles here for dark theme
    }

    private MarkdownTheme() {}
}
