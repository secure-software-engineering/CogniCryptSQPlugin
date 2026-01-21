package org.sonarsource.plugins.secai.languages;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.plugins.secai.rules.GradleGroovyRulesDefinition;

public class GradleGroovyQualityProfile implements BuiltInQualityProfilesDefinition {

    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile("Gradle (Groovy) Support", GradleGroovyLanguage.KEY);
        profile.setDefault(true);

        NewBuiltInActiveRule rule1 = profile.activateRule(GradleGroovyRulesDefinition.REPOSITORY, "line1");
        rule1.overrideSeverity("MAJOR");

        profile.done();
    }
}
