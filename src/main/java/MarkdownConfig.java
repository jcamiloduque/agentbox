/**
 * Configuration options for markdown rendering.
 */
public class MarkdownConfig {

    // ── Features ───────────────────────────────────────────────────────────────

    /** Enable syntax highlighting in code blocks */
    private boolean syntaxHighlighting = true;

    /** Show line numbers in code blocks */
    private boolean showLineNumbers = true;

    /** Render tables */
    private boolean renderTables = true;

    /** Render blockquotes */
    private boolean renderBlockquotes = true;

    /** Render ordered lists */
    private boolean renderOrderedLists = true;

    /** Render horizontal rules */
    private boolean renderHorizontalRules = true;

    /** Render links */
    private boolean renderLinks = true;

    /** Render images */
    private boolean renderImages = true;

    // ── Constructors ───────────────────────────────────────────────────────────

    public MarkdownConfig() {}

    public MarkdownConfig(MarkdownConfig other) {
        this.syntaxHighlighting = other.syntaxHighlighting;
        this.showLineNumbers = other.showLineNumbers;
        this.renderTables = other.renderTables;
        this.renderBlockquotes = other.renderBlockquotes;
        this.renderOrderedLists = other.renderOrderedLists;
        this.renderHorizontalRules = other.renderHorizontalRules;
        this.renderLinks = other.renderLinks;
        this.renderImages = other.renderImages;
    }

    // ── Builder Pattern ────────────────────────────────────────────────────────

    public static MarkdownConfig builder() {
        return new MarkdownConfig();
    }

    public MarkdownConfig withSyntaxHighlighting(boolean enabled) {
        this.syntaxHighlighting = enabled;
        return this;
    }

    public MarkdownConfig withLineNumbers(boolean enabled) {
        this.showLineNumbers = enabled;
        return this;
    }

    public MarkdownConfig withTables(boolean enabled) {
        this.renderTables = enabled;
        return this;
    }

    public MarkdownConfig withBlockquotes(boolean enabled) {
        this.renderBlockquotes = enabled;
        return this;
    }

    public MarkdownConfig withOrderedLists(boolean enabled) {
        this.renderOrderedLists = enabled;
        return this;
    }

    public MarkdownConfig withHorizontalRules(boolean enabled) {
        this.renderHorizontalRules = enabled;
        return this;
    }

    public MarkdownConfig withLinks(boolean enabled) {
        this.renderLinks = enabled;
        return this;
    }

    public MarkdownConfig withImages(boolean enabled) {
        this.renderImages = enabled;
        return this;
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public boolean isSyntaxHighlightingEnabled() {
        return syntaxHighlighting;
    }

    public boolean isLineNumbersEnabled() {
        return showLineNumbers;
    }

    public boolean isTablesEnabled() {
        return renderTables;
    }

    public boolean isBlockquotesEnabled() {
        return renderBlockquotes;
    }

    public boolean isOrderedListsEnabled() {
        return renderOrderedLists;
    }

    public boolean isHorizontalRulesEnabled() {
        return renderHorizontalRules;
    }

    public boolean isLinksEnabled() {
        return renderLinks;
    }

    public boolean isImagesEnabled() {
        return renderImages;
    }

    // ── Presets ────────────────────────────────────────────────────────────────

    /**
     * Minimal markdown support (only basic elements).
     */
    public static MarkdownConfig minimal() {
        return new MarkdownConfig()
            .withSyntaxHighlighting(false)
            .withLineNumbers(false)
            .withTables(false)
            .withBlockquotes(false)
            .withOrderedLists(false)
            .withHorizontalRules(false)
            .withLinks(false)
            .withImages(false);
    }

    /**
     * Full markdown support with all features enabled.
     */
    public static MarkdownConfig full() {
        return new MarkdownConfig()
            .withSyntaxHighlighting(true)
            .withLineNumbers(true)
            .withTables(true)
            .withBlockquotes(true)
            .withOrderedLists(true)
            .withHorizontalRules(true)
            .withLinks(true)
            .withImages(true);
    }

    @Override
    public String toString() {
        return "MarkdownConfig{" +
            "syntaxHighlighting=" + syntaxHighlighting +
            ", showLineNumbers=" + showLineNumbers +
            ", renderTables=" + renderTables +
            ", renderBlockquotes=" + renderBlockquotes +
            ", renderOrderedLists=" + renderOrderedLists +
            ", renderHorizontalRules=" + renderHorizontalRules +
            ", renderLinks=" + renderLinks +
            ", renderImages=" + renderImages +
            '}';
    }
}
