package org.sonarsource.plugins.secai.languages;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;
import org.sonarsource.plugins.secai.settings.GradleGroovyLanguageProperties;

public class GradleGroovyLanguage  extends AbstractLanguage {

    public static final String NAME = "Gradle (Groovy)";
    public static final String KEY = "gradlegroovy";

    private final Configuration config;

    public GradleGroovyLanguage(Configuration config) {
        super(KEY, NAME);
        this.config = config;
    }

    @Override
    public String[] getFileSuffixes() {
        return config.getStringArray(GradleGroovyLanguageProperties.FILE_SUFFIXES_KEY);
    }
}
