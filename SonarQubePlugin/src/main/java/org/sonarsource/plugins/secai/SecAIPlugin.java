package org.sonarsource.plugins.secai;

import org.sonar.api.Plugin;
import org.sonarsource.plugins.secai.api.SecAIWebService;
import org.sonarsource.plugins.secai.analysis.cognicrypt.SecaiMetrics;
import org.sonarsource.plugins.secai.languages.QualityProfile;
import org.sonarsource.plugins.secai.languages.GradleGroovyLanguage;
import org.sonarsource.plugins.secai.languages.GradleGroovyQualityProfile;
import org.sonarsource.plugins.secai.rules.GradleGroovyRulesDefinition;
import org.sonarsource.plugins.secai.sensor.CogniCryptSensor;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CogniCryptRulesDefinition;
import org.sonarsource.plugins.secai.settings.GradleGroovyLanguageProperties;
import org.sonarsource.plugins.secai.settings.SecAIProperties;
import org.sonarsource.plugins.secai.web.SecAIPluginExtention;

public class SecAIPlugin implements Plugin {

    @Override
    public void define(Context context) {
        context.addExtension(SecAIPluginExtention.class);
        context.addExtension(SecAIWebService.class);
        context.addExtension(CogniCryptSensor.class);
        context.addExtension(CogniCryptRulesDefinition.class);
        context.addExtension(QualityProfile.class);

        // .gradle language
        context.addExtensions(GradleGroovyLanguage.class,
                GradleGroovyQualityProfile.class, GradleGroovyRulesDefinition.class);
        context.addExtensions(GradleGroovyLanguageProperties.getProperties());

        // other SecAI properties/settings
        context.addExtensions(SecAIProperties.getProperties());
        context.addExtension(SecaiMetrics.class);
    }
    
}
