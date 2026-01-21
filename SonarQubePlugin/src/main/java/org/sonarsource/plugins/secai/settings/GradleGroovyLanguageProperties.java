package org.sonarsource.plugins.secai.settings;

import static java.util.Arrays.asList;

import java.util.List;

import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class GradleGroovyLanguageProperties {

    public static final String FILE_SUFFIXES_KEY = "sonar.gradlegroovy.file.suffixes";
    public static final String FILE_SUFFIXES_DEFAULT_VALUE = ".gradle,.properties";

    private GradleGroovyLanguageProperties() {
        // only statics
    }

    public static List<PropertyDefinition> getProperties() {
        return asList(PropertyDefinition.builder(FILE_SUFFIXES_KEY)
                .multiValues(true)
                .defaultValue(FILE_SUFFIXES_DEFAULT_VALUE)
                .category("SecAI")
                .subCategory("Compilation")
                .name("Gradle (Groovy) Support")
                //.name("File Suffixes")
                .description("Files with these suffixes are required by the SecAI plugin for full functionality, " +
                        "namely the support of Gradle with Groovy DSL. They are not actually analyzed and this \"language\" " +
                        "merely serves as a way to include them in the component tree. If files with these suffixes are " +
                        "to be analyzed by a different plugin, simply remove them from this list.")
                .onQualifiers(Qualifiers.PROJECT)
                .build());
    }
}
