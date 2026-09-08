package api;

import java.util.List;
import java.util.Map;

public record ToolReference(
    String type,
    Function function
) {
    public record Function(
        String name,
        String description,
        Parameters parameters
    ) {}

    public record Parameters(
        String type,
        Map<String, Property> properties,
        List<String> required
    ) {}

    public record Property(
        String type,
        String description
    ) {}
}
