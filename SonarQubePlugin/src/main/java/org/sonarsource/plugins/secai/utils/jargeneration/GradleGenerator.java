package org.sonarsource.plugins.secai.utils.jargeneration;

import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.SourceCodeService;
import org.sonarsource.plugins.secai.utils.exceptions.JarGenerationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.sonarsource.plugins.secai.utils.jargeneration.BuildSystem.GRADLE;

public class GradleGenerator {

    private final Logger LOGGER = LoggerFactory.getLogger(GradleGenerator.class);

    /**
     * This method attempts to build a Gradle project.
     * @return absolute path to the generated jar
     * @throws JarGenerationException if the Gradle build is not successful
     */
    public String start() throws JarGenerationException {
        LOGGER.debug("starting Gradle");
        // retrieve current settings for buildSystem (for error reporting) and baseDir
        BuildSystem buildSystem = SecAISettings.getInstance().getBuildSystem();
        String baseDir = JarGenerator.getInstance().getBaseDir();

        String sourceDir;
        /* Gradle cannot connect to the same project twice so we load the sources into a separate
         * temp directory. This is only necessary, if we are working locally through a Sensor.
         */
        try {
            if (!baseDir.contains("source-dir")) {
                String projectKey = SecAISettings.getInstance().getProjectKey();
                SourceCodeService.getInstance(projectKey).loadSources(baseDir);
                sourceDir = SourceCodeService.getInstance(projectKey).getSourceDir();
            } else {
                sourceDir = baseDir;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String output;
        try {
            output = build(out, sourceDir);
        } catch (GradleConnectionException e) {
            throw new JarGenerationException(buildSystem, GRADLE, baseDir, e);
        }

        // get the exact name and path of the generated jar from the output of the jar task
        LOGGER.debug("Gradle build successful");

        String jarPath = getJarPath(output);
        if (new File(jarPath).exists()) {
            return jarPath;
        } else {
            return null;
        }
    }

    /**
     * Executes the gradle build on the project.
     * @param out output stream for the Gradle execution
     * @param sourceDir location of the gradle project
     * @return output of the Gradle execution
     * @throws GradleConnectionException if the connection fails
     */
    private String build(ByteArrayOutputStream out, String sourceDir) throws GradleConnectionException {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(new File(sourceDir))
                .connect()) {
            LOGGER.debug("executing gradle build");

            /* When running the Sensor, the system property is set to the JRE included in
             * SonarQube, which means there wouldn't be a Java compiler available. If this
             * is the case we have to try and use the environment variable.
             */
            String javaHome;
            if (System.getProperty("java.home").contains("jre")) {
                javaHome = System.getenv("JAVA_HOME");
                // TODO: proper checks whether the given java has a compiler
                //  + better error handling
                if (javaHome == null) {
                    throw new RuntimeException("Please set JAVA_HOME to a JDK. A system restart may be necessary");
                }
            } else {
                javaHome = System.getProperty("java.home");
            }
            LOGGER.debug("javaHome={}", javaHome);

            connection.newBuild().forTasks("clean", "jar", "properties")
                    .withArguments("--info")
                    .setStandardOutput(out)
                    .setStandardError(err)
                    .setJavaHome(new File(javaHome))
                    .run();
        }

        return out.toString();
    }

    /**
     * This method assembles the jar name and path from different gradle properties
     * @param output Output of the Gradle execution
     * @return jar path
     */
    private String getJarPath(String output) {
        output = output.split("> Task :properties")[1];

        int offset = "archivesBaseName:".length();
        int i = output.indexOf("archivesBaseName");
        String archivesBaseName = output.substring(i + offset, output.indexOf("\n", i)).strip();

        offset = "version:".length();
        i = output.indexOf("version:");
        String version = output.substring(i + offset, output.indexOf("\n", i)).strip();

        offset = "archiveClassifier:".length();
        i = output.indexOf("archiveClassifier");
        String archiveClassifier = i != -1
                ? output.substring(i + offset, output.indexOf("\n", i)).strip()
                : null;

        offset = "archiveAppendix:".length();
        i = output.indexOf("archiveAppendix");
        String archiveAppendix = i != -1
                ? output.substring(i + offset, output.indexOf("\n", i)).strip()
                : null;

        String archiveFileName =  archivesBaseName + "-";
        if (archiveAppendix != null) {
            archiveFileName += archiveAppendix + "-";
        }
        archiveFileName += version;
        if (archiveClassifier != null) {
            archiveFileName += "-" + archiveClassifier;
        }
        archiveFileName += ".jar";

        offset = "buildDir:".length();
        i = output.indexOf("buildDir:");
        String buildDir = output.substring(i + offset, output.indexOf("\n", i)).strip();

        offset = "libsDirName:".length();
        i = output.indexOf("libsDirName:");
        String libsDirName = output.substring(i + offset, output.indexOf("\n", i)).strip();
        return buildDir + File.separator + libsDirName + File.separator + archiveFileName;
    }
}
