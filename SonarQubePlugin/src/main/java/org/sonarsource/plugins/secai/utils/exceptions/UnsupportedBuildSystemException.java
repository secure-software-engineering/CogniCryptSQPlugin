package org.sonarsource.plugins.secai.utils.exceptions;

public class UnsupportedBuildSystemException extends Exception {

    /**
     * This exception is thrown when the auto-detection does not find build files for one of the supported build systems.
     */
    public UnsupportedBuildSystemException(String baseDir) {
        super("Could not find any supported build system in " + baseDir + ". Supported build systems are: Maven, Gradle");
    }
}
