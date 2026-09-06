package helpers;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    static Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .ignoreIfMalformed()
            .load();

    public static String get(String key) {
        return dotenv.get(key, System.getenv(key));
    }

    public static String get(String key, String fallback) {
        String value = get(key);

        if (value == null || value.isEmpty()) {
            return fallback;
        }

        return value;
    }
}
