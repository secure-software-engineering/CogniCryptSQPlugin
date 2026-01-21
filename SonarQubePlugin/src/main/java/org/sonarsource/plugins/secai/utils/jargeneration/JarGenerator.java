package org.sonarsource.plugins.secai.utils.jargeneration;

import java.io.*;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.exceptions.BaseDirNotSetException;
import org.sonarsource.plugins.secai.utils.exceptions.JarGenerationException;
import org.sonarsource.plugins.secai.utils.exceptions.MavenNotFoundException;
import org.sonarsource.plugins.secai.utils.exceptions.UnsupportedBuildSystemException;

public class JarGenerator {

    private final Logger LOGGER = LoggerFactory.getLogger(JarGenerator.class);

    private final MavenGenerator MAVEN = new MavenGenerator();
    private final GradleGenerator GRADLE = new GradleGenerator();
    private String baseDir = null;

    private static JarGenerator INSTANCE;

    /**
     * Private constructor for creation of the singleton instance.
     */
    private JarGenerator() {
        if (System.getProperty("sonar.projectBaseDir") != null) {
            this.setBaseDir(System.getProperty("sonar.projectBaseDir"));
        }
    }

    /**
     * This method should only be used by the MavenGenerator and GradleGenerator
     * classes to get the current settings.
     * 
     * @return Singleton instance
     */
    public static JarGenerator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JarGenerator();
        }

        return INSTANCE;
    }

    /**
     * This method calls the required build functions. Depending on the value of
     * buildSystems
     * it tries to auto-detect which build system to use or tries to use the one
     * specified.
     * 
     * @return path to generated jar or null in case of error
     */
    public String generateJar() throws JarGenerationException, UnsupportedBuildSystemException, MavenNotFoundException,
            BaseDirNotSetException {
        if (baseDir == null) {
            LOGGER.error("base directory is null.");
            throw new BaseDirNotSetException();
        }

        BuildSystem buildSystem;
        try {
            buildSystem = SecAISettings.getInstance().getBuildSystem();
        } catch (NullPointerException e) {
            buildSystem = BuildSystem.AUTO;
        }

        switch (buildSystem) {
            case AUTO:
                LOGGER.debug("build system: AUTO");
                // try to detect pom.xml
                try (FileReader ignored = new FileReader(baseDir + "/pom.xml")) {
                    return MAVEN.start();
                } catch (IOException e) {
                    // Based on:
                    // https://docs.gradle.org/current/javadoc/org/gradle/tooling/GradleConnector.html
                    // try to connect to a gradle project in the base directory of the project
                    try (FileReader ignored = new FileReader(baseDir + "/build.gradle")) {
                        return GRADLE.start();
                    } catch (IOException ex) {
                        try (FileReader ignored = new FileReader(baseDir + "/build.gradle.kts")) {
                            return GRADLE.start();
                        } catch (IOException exc) {
                            LOGGER.error("couldn't find anything");
                            throw new UnsupportedBuildSystemException(baseDir);
                        }
                    }
                }
            case MAVEN:
                try (FileReader ignored = new FileReader(baseDir + "/pom.xml")) {
                    return MAVEN.start();
                } catch (IOException fileNotFoundException) {
                    throw new JarGenerationException(buildSystem, buildSystem, baseDir);
                }
            case GRADLE:
                try (FileReader ignored = new FileReader(baseDir + "/build.gradle")) {
                    return GRADLE.start();
                } catch (IOException e) {
                    try (FileReader ignored = new FileReader(baseDir + "/build.gradle.kts")) {
                        return GRADLE.start();
                    } catch (IOException ex) {
                        throw new JarGenerationException(buildSystem, buildSystem, baseDir);
                    }
                }
            default:
                return null;
        }
    }

    public String getBaseDir() {
        return baseDir;
    }

    public JarGenerator setBaseDir(String baseDir) {
        // in testing, this method crashes on NullPointerException (NPE), because if we pass null, it will try to .replace with null, that will cause NPE
        if (baseDir == null) {
            this.baseDir = null;
            return INSTANCE;
        }
        // string replace all \ to avoid problems with extending the path later
        this.baseDir = baseDir.replace("\\", "/");
        LOGGER.debug("project base directory: {}", baseDir);
        return INSTANCE;
    }

    public String getSelectedJar() {
        return MAVEN.getSelectedJar();
    }

    public void setSelectedJar(String selectedJar) {
        MAVEN.setSelectedJar(selectedJar);
    }

    public ArrayList<String> getGeneratedJars() {
        return MAVEN.getGeneratedJars();
    }
}
