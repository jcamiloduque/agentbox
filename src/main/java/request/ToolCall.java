package request;

public class ToolCall {
    private String id;
    private String name;
    private String arguments;
    private String response;
    private Status status;
    private long durationMs;

    public enum Status {
        RUNNING,
        COMPLETED,
        FAILED
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    private StringBuilder argBuffer;

    public void appendArguments(String additionalArguments) {
        if (this.argBuffer == null) {
            this.argBuffer = new StringBuilder();
        }
        this.argBuffer.append(additionalArguments);

        // Check if the accumulated buffer is a complete JSON object.
        // A JSON object is complete when:
        //   1. It starts with '{'
        //   2. All braces {} and brackets [] are balanced
        //   3. We are not inside a string literal (unescaped quotes)
        // Escaped characters inside strings (e.g., \"}) don't affect parsing.
        if (this.argBuffer.length() > 0) {
            String buffer = this.argBuffer.toString();
            if (buffer.charAt(0) == '{') {
                // Recursively parse and track nesting depth
                int depth = parseAndCount(buffer, 0, 0, false, false);
                if (depth == 0) {
                    this.arguments = buffer;
                    this.argBuffer.setLength(0); // clear buffer
                }
            }
        }

        // Fallback: if we still don't have arguments, use the raw buffer.
        // This handles the case where the stream is truncated mid-JSON.
        if (this.arguments == null && this.argBuffer != null && this.argBuffer.length() > 0) {
            this.arguments = this.argBuffer.toString();
        }
    }

    /**
     * Recursively parses the buffer and returns the nesting depth at the given index.
     * Tracks whether we're inside a string and whether we've seen an escaped backslash.
     * Escaped braces inside strings (e.g., \"}) are ignored.
     *
     * @param str the full buffer string
     * @param i   current character index (passed through recursion)
     * @param depth current nesting depth of braces/brackets
     * @param inString whether we're currently inside a string literal
     * @param escaped whether the previous character was a backslash
     * @return the depth after processing character i (or 0 if we've closed everything)
     */
    private int parseAndCount(String str, int i, int depth, boolean inString, boolean escaped) {
        if (i >= str.length()) {
            return depth;
        }
        char c = str.charAt(i);
        if (escaped) {
            // Backslash escapes the next character — don't treat it specially
            return parseAndCount(str, i + 1, depth, inString, false);
        }
        if (c == '\\') {
            // Next character is escaped — skip it
            return parseAndCount(str, i + 1, depth, inString, true);
        }
        if (c == '"') {
            // Toggle string mode
            return parseAndCount(str, i + 1, depth, !inString, false);
        }
        if (inString) {
            // Inside a string — ignore braces/brackets
            return parseAndCount(str, i + 1, depth, true, false);
        }
        if (c == '{') {
            return parseAndCount(str, i + 1, depth + 1, false, false);
        }
        if (c == '}') {
            return parseAndCount(str, i + 1, depth - 1, false, false);
        }
        if (c == '[') {
            return parseAndCount(str, i + 1, depth + 1, false, false);
        }
        if (c == ']') {
            return parseAndCount(str, i + 1, depth - 1, false, false);
        }
        // Any other character — just move forward
        return parseAndCount(str, i + 1, depth, inString, false);
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
