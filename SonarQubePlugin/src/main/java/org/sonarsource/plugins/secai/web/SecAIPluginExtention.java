package org.sonarsource.plugins.secai.web;

import static org.sonar.api.web.page.Page.Qualifier.PROJECT;
import static org.sonar.api.web.page.Page.Scope.COMPONENT;

import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PageDefinition;

public class SecAIPluginExtention implements PageDefinition {

    @Override
    public void define(Context context) {
        context.addPage(Page.builder("secai/sec_ai")
        .setName("SecAI analysis")
        .setScope(COMPONENT)
        .setComponentQualifiers(PROJECT)
        .build());    
    }
    
}
