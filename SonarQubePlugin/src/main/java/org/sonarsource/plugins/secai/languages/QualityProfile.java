package org.sonarsource.plugins.secai.languages;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CogniCryptRulesDefinition;

import java.util.Map;

import static org.sonarsource.plugins.secai.analysis.cognicrypt.CogniCryptRulesDefinition.CRYSL_RULES;

public class QualityProfile implements BuiltInQualityProfilesDefinition {

    public static final String QUALITYPROFILE = "SecAI";
    @Override
    public void define(Context context) {

        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(QUALITYPROFILE, "java");

        // Activating all CogniCrypt rules...
        for (Map.Entry<String, RuleKey> entry : CRYSL_RULES.entrySet()) {
            NewBuiltInActiveRule _rule = profile.activateRule(CogniCryptRulesDefinition.REPOSITORY,entry.getValue().rule());
        }

        profile.setDefault(true);

        profile.done();

    }
}
