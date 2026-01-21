package org.sonarsource.plugins.secai.settings;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonarsource.plugins.secai.analysis.AnalysisTool;
import org.sonarsource.plugins.secai.utils.jargeneration.BuildSystem;

public class SecAIProperties {

    // category names
    private static final String CATEGORY = "SecAI";
    private static final String COMPILATION = "Compilation";
    private static final String COGNICRYPT = "CogniCrypt";
    private static final String AI = " AI Integration";

    private SecAIProperties() {
        // only statics
    }

    public static List<PropertyDefinition> getProperties() {
        return asList(buildSystem(),
                mavenHome(),
                tools(),
                ccMessages(),
                githubUsername(),
                githubRepoUrl(),
                githubPATtoken()
        );
    }

    /**
     * Creates a setting to specify which build system to use.
     * @return property
     */
    private static PropertyDefinition buildSystem() {
        final String KEY = "sonar.secai.build.system";
        final BuildSystem DEFAULT_VALUE = BuildSystem.AUTO;
        List<String> options = new ArrayList<>();
        for (BuildSystem value : BuildSystem.values()) {
            options.add(value.toString());
        }
        return PropertyDefinition.builder(KEY)
                .index(20)
                .type(PropertyType.SINGLE_SELECT_LIST)
                .defaultValue(DEFAULT_VALUE.toString())
                .options(options)
                .category(CATEGORY)
                .subCategory(COMPILATION)
                .name("Build System")
                .description("During the SecAI analysis with CogniCrypt the plugin attempts to create a Jar of the project. " +
                        "This property specifies whether the plugin should determine the build system automatically based on " +
                        "the files present in the project base directory, or only try to build using the given system.")
                .onQualifiers(Qualifiers.PROJECT)
                .build();
    }

    /**
     * Creates a setting for the user to give a path to check for the Maven installation.
     * @return property
     */
    private static PropertyDefinition mavenHome() {
        final String KEY = "sonar.secai.maven.home";
        return PropertyDefinition.builder(KEY)
                .index(21)
                .multiValues(true)
                .category(CATEGORY)
                .subCategory(COMPILATION)
                .name("Maven Home")
                .description("The SecAI plugin may have difficulties locating your Maven installation. " +
                        "Here you can manually add the path. Multiple values are possible.")
                .onQualifiers(Qualifiers.PROJECT)
                .build();
    }

    /**
     * Creates settings to activate/deactivate different analysis tools.
     * @return property
     */
    private static PropertyDefinition tools() {
        final String KEY = "sonar.secai.tools";
        final String DEFAULT_VALUE = "CogniCrypt";
        List<String> options = new ArrayList<>();
        for (AnalysisTool tool : AnalysisTool.values()) {
            options.add(tool.displayName());
        }
        return PropertyDefinition.builder(KEY)
                .index(0)
                .multiValues(true)
                .defaultValue(DEFAULT_VALUE)
                .type(PropertyType.SINGLE_SELECT_LIST)
                .options(options)
                .category(CATEGORY)
                .subCategory("General")
                .name("Analysis Tools")
                .description("Here you can activate and deactivate different analysis tools. " +
                        "If no tool is selected, CogniCrypt will be used by default.")
                .onQualifiers(Qualifiers.PROJECT)
                .build();
    }

    /**
     * Creates setting to specify the confidence threshold
     * @return property
     */
    private static PropertyDefinition confidenceThreshold() {
        final String KEY = "sonar.secai.confidence.threshold";
        final String DEFAULT_VALUE = "50";
        return PropertyDefinition.builder(KEY)
                .defaultValue(DEFAULT_VALUE)
                .type(PropertyType.FLOAT)
                .category(CATEGORY)
                .subCategory("False Positives")
                .name("Confidence Threshold")
                .description("Different issues have different confidence scores regarding whether or not this truly is a problem. " +
                        "Here you can configure the threshold for how confident the analysis must be about an issue for it to be shown. " +
                        "Rerun the analysis after changing this setting.")
                .onQualifiers(Qualifiers.PROJECT)
                .build();
    }

    /**
     * Creates setting to configure the message length and content for CogniCrypt error messages
     * @return property
     */
    private static PropertyDefinition ccMessages() {
        final String KEY = "sonar.secai.cognicrypt.messages";
        final String DEFAULT_VALUE = "Shortened";
        return PropertyDefinition.builder(KEY)
                .defaultValue(DEFAULT_VALUE)
                .type(PropertyType.SINGLE_SELECT_LIST)
                .options("Shortened", "Summary")
                .category(CATEGORY)
                .subCategory(COGNICRYPT)
                .name("CogniCrypt Error Messages")
                .description("The error messages returned by CogniCrypt can at times be of considerable length. " +
                        "This is in part due to the amount of information included in the message, such as all possible " +
                        "parameter configurations for a missing method. Changes to this setting only take effect after the next analysis.")
                .onQualifiers(Qualifiers.PROJECT)
                .build();
    }

    private static PropertyDefinition githubUsername() {
        final String FILE_KEY = "sonar.secai.pullrequest.githubusername";
        return PropertyDefinition.builder(FILE_KEY)
                .type(PropertyType.STRING)
                .name("Username")
                .description("GitHub username for authentication")
                .defaultValue("none")
                .category(CATEGORY)
                .subCategory("GitHub Pull Request")
                .onlyOnQualifiers(Qualifiers.PROJECT)
                .build();
    }

    private static PropertyDefinition githubRepoUrl() {
        final String FILE_KEY = "sonar.secai.pullrequest.githubrepourl";
        return PropertyDefinition.builder(FILE_KEY)
                .type(PropertyType.STRING)
                .name("Repository URL")
                .description("URL of the GitHub repository for pull request integration")
                .defaultValue("none")
                .category(CATEGORY)
                .subCategory("GitHub Pull Request")
                .onlyOnQualifiers(Qualifiers.PROJECT)
                .build();
    }
    private static PropertyDefinition githubPATtoken() {
        final String FILE_KEY = "sonar.secai.pullrequest.githubpattoken";
        return PropertyDefinition.builder(FILE_KEY)
                .type(PropertyType.PASSWORD)
                .name("PAT Token")
                .description("Personal Access Token for GitHub API access")
                .defaultValue("none")
                .category(CATEGORY)
                .subCategory("GitHub Pull Request")
                .onlyOnQualifiers(Qualifiers.PROJECT)
                .build();
    }
}
