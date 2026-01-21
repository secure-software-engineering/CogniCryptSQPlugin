package org.sonarsource.plugins.secai.analysis.cognicrypt;

import org.apache.commons.io.IOUtils;
import org.sonar.api.SonarRuntime;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;

import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CogniCryptRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY = "cognicrypt";
    public static final String JAVA_LANGUAGE = "java";

    // Rule IDs for all CrySL violations
    public static final Map<String, RuleKey> CRYSL_RULES = new HashMap<>();

    private final SonarRuntime runtime;

    private static final String SQ_RULE_DIRECTORY = "org/sonarsource/plugins/secai/cognicrypt/sq_rules";

    static {
        try {
            InputStream stream = CogniCryptRulesDefinition.class.getClassLoader().getResourceAsStream(SQ_RULE_DIRECTORY + "/RuleList.txt");
            if (stream == null) {
                throw new FileNotFoundException("Resource not found: RuleList.txt");
            }
            String errorList = IOUtils.toString(stream, StandardCharsets.UTF_8);

            for (String fileName : errorList.split(",")) {
                CRYSL_RULES.put(fileName, RuleKey.of(REPOSITORY, fileName));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CogniCryptRulesDefinition(SonarRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void define(Context context) {
        // Create repository
        NewRepository repository = context.createRepository(REPOSITORY, JAVA_LANGUAGE)
                .setName("CogniCrypt Security Rules");

        // Load rules
        RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(SQ_RULE_DIRECTORY, runtime);
        ruleMetadataLoader.addRulesByRuleKey(repository, List.of(CRYSL_RULES.keySet().toArray(new String[0])));

        repository.done();
    }
}
