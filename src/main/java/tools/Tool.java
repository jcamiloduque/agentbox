package tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Keeps the annotation available at runtime so your Registry can read it via Reflection
@Retention(RetentionPolicy.RUNTIME)
// Restricts this annotation so it can only be placed on Class, Interface, or Enum declarations
@Target(ElementType.TYPE)
public @interface Tool {

    // Defines the 'name' property (Required)
    String name();

    // Defines the 'description' property (Optional, defaults to an empty string)
    String description() default "";
}