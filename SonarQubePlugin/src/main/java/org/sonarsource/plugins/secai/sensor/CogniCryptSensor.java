package org.sonarsource.plugins.secai.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CogniCrypt;
import org.sonarsource.plugins.secai.analysis.cognicrypt.SecaiMetrics;
import org.sonarsource.plugins.secai.analysis.AnalysisTool;
import org.sonarsource.plugins.secai.utils.TimeTracker;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CogniCryptRulesDefinition;
import org.sonarsource.plugins.secai.reporting.cognicrypt.CCIssueReporter;
import org.sonarsource.plugins.secai.utils.SourceCodeService;
import org.sonarsource.plugins.secai.utils.jargeneration.JarGenerator;
import org.sonarsource.plugins.secai.utils.exceptions.BaseDirNotSetException;
import org.sonarsource.plugins.secai.utils.exceptions.JarGenerationException;
import org.sonarsource.plugins.secai.utils.exceptions.MavenNotFoundException;
import org.sonarsource.plugins.secai.utils.exceptions.UnsupportedBuildSystemException;

import java.io.IOException;
import java.util.Map;

import org.sonarsource.plugins.secai.utils.ResourceService;
import org.sonarsource.plugins.secai.reporting.cognicrypt.NewCCIssue;

public class CogniCryptSensor implements Sensor {

    private final Logger LOGGER = LoggerFactory.getLogger(CogniCryptSensor.class);

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.name("CogniCryptSensor");
        sensorDescriptor.onlyOnLanguages("gradlegroovy", "java");
        sensorDescriptor.createIssuesForRuleRepositories(CogniCryptRulesDefinition.REPOSITORY);
    }

    @Override
    public void execute(SensorContext sensorContext) {
        FileSystem fileSystem = sensorContext.fileSystem();

        try {
            SecAISettings settings = SecAISettings.newInstance(sensorContext.config());
            ResourceService resourceService = ResourceService.getInstance();

            /* For the CogniCrypt analysis to be executed the user needs to:
             *      have selected CogniCrypt, or
             *      not have selected any analysis tool.
             */
            if (!settings.getTools().contains(AnalysisTool.COGNICRYPT) && !settings.getTools().isEmpty()) {
                LOGGER.info("CogniCrypt was not selected for analysis. Skipping Sensor.");
                return;
            }

            JarGenerator gen = JarGenerator.getInstance()
                    .setBaseDir(fileSystem.baseDir().getAbsolutePath());

            float time = System.nanoTime();
            String jarPath = gen.generateJar();
            TimeTracker.setJarGenerationTime((System.nanoTime() - time) / 1000000);

            LOGGER.info("jar to analyze: {}", jarPath);
            if (jarPath == null) {
                LOGGER.error("Something unhandled went wrong trying to build the test jar.");
            } else {
                CogniCrypt cogniCrypt = new CogniCrypt();
                Map<String, NewCCIssue> issueList = cogniCrypt.generateWarnings(jarPath, System.out::println);

                if (issueList != null ) {
                    TimeTracker.setIssueNumber(issueList.size());
                    time = System.nanoTime();
                    // Reporting detected violation from Helpermethod
                    CCIssueReporter reporter = new CCIssueReporter(sensorContext);
                    reporter.parseAndReportIssues(issueList, jarPath);

                    String fullJson = "{ \"timestamp\": " + System.currentTimeMillis() + ", \"issues\": " + reporter.getErrorJsonString() + "}";

                    if (fullJson != null && !fullJson.isEmpty()) {
                        sensorContext.<String>newMeasure()
                                .on(sensorContext.project())
                                .forMetric(SecaiMetrics.FULL_ERRORS_JSON)
                                .withValue(fullJson)
                                .save();
                    }
                    TimeTracker.setIssueReportingTime((System.nanoTime() - time) / 1000000);
                } else {
                    LOGGER.error("CogniCrypt generation failed.");
                }
            }
        } catch (JarGenerationException e) {
            LOGGER.error(e.getMessage());
            if (e.getCause() != null) {
                LOGGER.error("Caused by: " + e.getCause().getMessage());
            }
        } catch (UnsupportedBuildSystemException | MavenNotFoundException | BaseDirNotSetException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            // this is when something goes wrong with CogniCrypt creating temp files/directories
            LOGGER.error(e.getMessage());
        } finally {
            SourceCodeService.cleanAllOnExit();
            try {
                TimeTracker.toFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
}
