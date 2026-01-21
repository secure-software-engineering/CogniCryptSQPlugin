# Integrating a New Analysis Tool

In order to integrate an additional analysis tool, support needs to added in several different places. In order to allow the user to select your tool, add an `enum` entry to `settings.AnalysisTool`. Make sure to also include a display name to be shown in the settings. If the tool analyses a Jar instead of the source code itself, add it to the array `toolsAnalyzingJars`.

---

## Setting Up the Basics

This section details how to structure the main components of the integration.

### File Management

Any additional files required for your analysis, including executables, should be placed in a new folder in the `resources` directory. Our plugin provides the class `utils.ResourceService` to extract files from the resource folder to a local directory on the user's computer. For executables extracting will likely be necessary as the plugin itself is already packaged as a Jar.

Should your analysis produce report files you can use the `utils.ReportService` to get the path to the report directory (created by our plugin) for the analyzed project and retrieve the generated report through a getter.

### Analysis

In the package `analysis` create a new package with the name of the new tool. The most important thing is to create a class that works as a launcher for your analysis tool. This guide will refer to this class as `NewTool`. Depending on your implementation, running your tool might involve launching an executable or, alternatively, adding the tool as a dependency and executing the tool's own analysis classes.

### Reporting

SonarQube offers the option of simply [importing SARIF reports](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/importing-external-issues/importing-issues-from-sarif-reports) or [generic formatted reports](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/importing-external-issues/generic-issue-import-format). You can either require that the user sets the `sonar.sarifReportPaths` or `sonar.externalIssuesReportPaths` property themselves or override it in your own code. If you choose to do the latter, make sure to **add** to the list instead of fully replacing it as the user may already be importing other reports.

However, if you want more customizability or your report does not fulfill the listed requirements, you might want to create a reporting class `NTIssueReporter`. Keep in mind that, if you had previously launched your analysis tool as a dependency, you can potentially use your own error classes when reporting instead of reading the generated report.

```java
package org.sonarsource.plugins.secai.reporting.newtool;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.plugins.secai.analysis.newtool.NewToolRulesDefinition;

public class NTIssueReporter {

    private final Logger LOGGER = LoggerFactory.getLogger(NTIssueReporter.class);

    private final SensorContext context;

    public NTIssueReporter(SensorContext sensorContext) {
        context = sensorContext;
    }

    // If you are using your tool as a dependency, consider passing your own error list instead of the report path
    public void parseAndReportIssues(String reportPath) throws IOException {
        // Get the list of issues from the given file 
        // (Note: the "getErrorListFromFile" method does not exist. Write your own solution depending on your report format)
        List<Map<String, Object>> errorList = getErrorListFromFile(reportPath);

        for (Map<String, Object> error : issueList) {
            // Note: it is assumed that all necessary information is included in the report and can simply be retrieved with "get". You may have to write your own logic
            RuleKey ruleKey = NewToolRulesDefinition.RULES.get(error.get("ruleKey"));
            if (ruleKey == null) continue;

            InputFile inputFile = context.fileSystem().inputFile(
                    context.fileSystem().predicates().hasPath(error.get("filePath"))
            );
            NewIssue issue = context.newIssue().forRule(ruleKey);

            // Set the main location of the issue
            NewIssueLocation location = issue.newLocation().on(inputFile)
                    .at(inputFile.newRange(error.get("startLine"), error.get("startCol"), error.get("endLine"), error.get("endCol")));
            issue.at(location);
            // It is also possible to add additional locations with 
            //issue.addLocation(newLocation);
            // Or add flows (basically ordered lists of issue locations)
            //issue.addFlow(locationList);

            // You can override impact/severity scores for different software qualities (if the default values defined in the rules are insufficient)
            issue.overrideImpact(SoftwareQuality.SECURITY, Severity.INFO);
            
            // At the end, save the issue
            issue.save();
        }
    }
}
```

In the class above you may have noticed the use of rule keys. This refers to the report concept of SonarQube where every issue has to be associated with a rule to further explain the problem. Should your tool detect issues not covered by existing SonarQube rules, you will need to create your own custom rules.

The first step is to extend `RulesDefinition` with a new class `NewToolRulesDefinition` where the repository for your custom rules is created and the rules are loaded. 

```java
package org.sonarsource.plugins.secai.analysis.newtool;

import org.apache.commons.io.IOUtils;
import org.sonar.api.SonarRuntime;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;

import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NewToolRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY = "newtool";
    public static final String LANGUAGE = "language";
    
    // Directory (in the resource folder) containing your SonarQube rules
    private static final String SQ_RULE_DIRECTORY = "org/sonarsource/plugins/secai/newtool/sq_rules";

    // You might need access to the rule keys later. In this guide the map was used in NTIssueReporter
    /**
     * key: String version of the rule key, value: matching RuleKey object
     */
    public static final Map<String, RuleKey> RULES = new HashMap<>();

    // With the way this class is set up, you need access to a list of all rule key to be able to load all rules. For this purpose, we suggest simply adding a RuleList.txt file with all rule keys in the same folder as the SonarQube rules
    static {
        try {
            InputStream stream = CogniCryptRulesDefinition.class.getClassLoader().getResourceAsStream(SQ_RULE_DIRECTORY + "/RuleList.txt");
            if (stream == null) {
                throw new FileNotFoundException("Resource not found: RuleList.txt");
            }
            String ruleList = IOUtils.toString(stream, StandardCharsets.UTF_8);

            for (String fileName : ruleList.split(",")) {
                //System.out.println(fileName);
                RULES.put(fileName, RuleKey.of(REPOSITORY, fileName));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final SonarRuntime runtime;

    public NewToolRulesDefinition(SonarRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void define(Context context) {
        // Create repository
        NewRepository repository = context.createRepository(REPOSITORY, LANGUAGE)
                .setName("NewTool Rules");

        // Load rules
        RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(SQ_RULE_DIRECTORY, runtime);
        ruleMetadataLoader.addRulesByRuleKey(repository, List.of(RULES.keySet().toArray(new String[0])));

        repository.done();
    }
}

```

In order to make sure that issues can be reported for the new repository, the rules need to added to a quality profile. You can either add to the SecAI quality profile `languages.QualityProfile` or add your own. Here, you can use the _`RULES`_ map from the `NewToolRulesDefinition`. The user must then activate the quality profile for their project or individually activate the rules on a different quality profile.

Now that the surrounding setup is done, the only thing left is to create the rules. These will be placed in the `resource` directory under the path defined in `NewToolRulesDefinition`. Each rule consists of two files: an html file for the static description and a json file for metadata. To get an idea on how to structure both of those files look into [this repository](https://github.com/SonarSource/sonar-java/tree/master/sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java) for a list of all Java rules or refer to the [docs of the rspec repository](https://github.com/SonarSource/rspec/tree/master/docs). Note that while SonarQube does offer a tutorial on creating custom rules, it is centered around creating executable checks for these rules which, in this case, is covered by your analysis tool.

### Custom Settings

Should your tool require additional user settings or if you wish to add customizability, you can create custom settings that are accessible through SonarQube.

For this you either add to the existing `settings.SecAIProperties` file or create your own property file in this package. See [here](custom-properties.md) for instructions on creating your own SonarQube properties.

Within the Java code, the properties are loaded into the singleton class `settings.SecAISettings`, though it has to be initialized first using either a `Configuration` from the `SensorContext` or a `Request` from an API call. If you need access to additional or custom properties add fields and getters for each setting while making sure the fields are set in both constructors.

---

## Triggering the Analysis

Now that all the prerequisites have been created the analysis can be triggered in two different ways, though reporting the resulting errors is only possible with the first option.

### Through a Custom `Sensor` (recommended)

Typically, the analysis is triggered through a `Sensor` that is executed when the user runs the general SonarQube analysis. Create a custom `Sensor` to run your analysis tool. This class calls your analysis class `NewTool` and reports the resulting issues to SonarQube using your `NTIssueReporter`.

```java
package org.sonarsource.plugins.secai.sensor;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.fs.FileSystem;
import org.sonarsource.plugins.secai.analysis.AnalysisTool;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.analysis.newtool.NewTool;
import org.sonarsource.plugins.secai.analysis.newtool.NewToolRulesDefinition;
import org.sonarsource.plugins.secai.reporting.newtool.NTIssueReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewToolSensor implements Sensor {

    private final Logger LOGGER = LoggerFactory.getLogger(NewToolSensor.class);
    
    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.name("NewToolSensor");
        sensorDescriptor.onlyOnLanguages("language(s) to analyze");
        
        // If you are using custom rules, specify the repository here
        sensorDescriptor.createIssuesForRuleRepositories(NewToolRulesDefinition.REPOSITORY);
    }

    @Override
    public void execute(SensorContext sensorContext) {
        FileSystem fileSystem = sensorContext.fileSystem();

        try {
            SecAISettings settings = SecAISettings.newInstance(sensorContext.config());
            
            // Skip running the Sensor if your tool is not selected
            if (!settings.getTools().contains(AnalysisTool.NEWTOOL)) {
                LOGGER.info(AnalysisTool.NEWTOOL.displayName() + "was not selected for analysis. Skipping Sensor.");
                return;
            }
            
            // If your tool analyzes Jars, generate the Jar first before running the analysis
            JarGenerator gen = JarGenerator.getInstance(settings)
                    .setBaseDir(fileSystem.baseDir().getAbsolutePath());
            String jarPath = gen.generateJar();
            
            // Run your analysis tool
            NewTool newTool = NewTool();
            String reportPath = newTool.runAnalysis(jarPath);
            
            // Report issues
            NTIssueReporter reporter = new NTIssueReporter(sensorContext);
            reporter.parseAndReportIssues(reportPath);
        } catch (JarGenerationException e) {
            LOGGER.error(e.getMessage());
            if (e.getCause() != null) {
                LOGGER.error("Caused by: " + e.getCause().getMessage());
            }
        } catch (UnsupportedBuildSystemException | MavenNotFoundException | BaseDirNotSetException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
```

### Over the Web API Call `api/secai/triggerAnalysis`

This call runs the class `analysis.Analyzer`. In order to use your tool, add an if statement checking for the tool’s presence in `settings.getTools()` and then execute the analysis class that you created in the previous step. However, it is not possible to report the resulting issues to the SonarQube server, though you may be able to display some results if you [extend the web view](https://docs.sonarsource.com/sonarqube-server/extension-guide/developing-a-plugin/adding-pages-to-the-webapp). By default, this API is not called anywhere in our plugin.

---

## Finalizing the integration

In the class `SecAIPlugin` add the following classes as extensions:

- `NewToolSensor`
- `NewToolRulesDefinition`
- If you created a new properties class, add that here too, otherwise all properties are included in `SecAIProperties`. Note: if the `getProperties()` of your tool’s properties class also returns a *list* of property definitions, then it needs to be added as the only argument of `context.addExtensions` (plural!)

---

## Helpful Utilities

- `utils.jargeneration.JarGenerator`: [This class](../../SonarQubePlugin/src/main/java/org/sonarsource/plugins/secai/utils/jargeneration/JarGenerator.java) can be used to build the project to be analyzed. The build systems Maven and Gradle are supported. Note that the base directory must be manually set to the project's root directory before calling `generateJar`. See [here](jar-generation.md) for more information.
- `utils.SourceCodeService`: After getting an instance of [this class](../../SonarQubePlugin/src/main/java/org/sonarsource/plugins/secai/utils/SourceCodeService.java) with the project key, this class can be utilized to load the source code into a temporary directory. The call to `loadSources` requires either the absolute path to the project base directory during the analysis or a web service client (`WsClientFactories.getLocal().newClient(request.localConnector());`) if the request is made after an API call. If your project analyzes a Jar, then during the analysis the source code will already have been copied to the temp directory to generate the Jar. You can get the directory path with `SourceCodeService.getInstance(projectKey).getSourceDir()`.
- `utils.SarifDeserialization`: [This class](../../SonarQubePlugin/src/main/java/org/sonarsource/plugins/secai/utils/SarifDeserialization.java) can be used to deserialize SARIF report with version 2.0.0.
- `reporting`: The top level of the reporting package contains a few general, though Java-specific, classes that could help with additional steps between your analysis and reporting. Note that SonarQube does not support custom quick fixes and the Java class in this directory is merely there to hold information. For our CogniCrypt integration we displayed the quick fixes in a [custom web page](https://docs.sonarsource.com/sonarqube-server/extension-guide/developing-a-plugin/adding-pages-to-the-webapp).


---

*For additional support or feature requests, please refer to the [Contributing Guide](../contributing.md) or create an issue in the project repository.*