package org.sonarsource.plugins.secai.utils.exceptions;

import org.sonarsource.plugins.secai.utils.jargeneration.BuildSystem;

public class JarGenerationException extends Exception {

    private final BuildSystem buildMode;
    private final BuildSystem buildSystem;
    private final String baseDir;

    /**
     * This exception is thrown when an error occurred while generating the jar.
     * @param buildMode whether the build system is detected automatically ("AUTO") or specified ("MAVEN", "GRADLE")
     * @param buildSystem build system that was used during the generation attempt
     * @param baseDir the base directory of the project
     */
    public JarGenerationException(BuildSystem buildMode, BuildSystem buildSystem, String baseDir) {
        this.buildMode = buildMode;
        this.buildSystem = buildSystem;
        this.baseDir = baseDir;    
    }

    /**
     * This exception is thrown when an error occurred while generating the jar. It also includes the error that causes this problem.
     * @param buildMode whether the build system is detected automatically ("AUTO") or specified ("MAVEN", "GRADLE")
     * @param buildSystem build system that was used during the generation attempt
     * @param baseDir the base directory of the project
     * @param err the error that occurred during jar generation
     */
    public JarGenerationException(BuildSystem buildMode, BuildSystem buildSystem, String baseDir, Throwable err) {
        super(err);
        this.buildMode = buildMode;
        this.buildSystem = buildSystem;
        this.baseDir = baseDir;
    }

    public String getMessage() {
        String m = "Something went wrong when building the jar. Please confirm that the following settings are correct and that the correct build files are present:\n";
        m += "project base directory: " + baseDir + "\n";
        m += "build system: ";
        if (buildMode == BuildSystem.AUTO) {
            m += "detected " + buildSystem;
        } else {
            m += buildSystem;
        }
        return m;
    }

    public BuildSystem getBuildMode() {
        return buildMode;
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    public String getBaseDir() {
        return baseDir;
    }
}
