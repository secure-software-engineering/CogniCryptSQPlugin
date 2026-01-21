package org.sonarsource.plugins.secai.utils.exceptions;

public class BaseDirNotSetException extends Exception {

    public BaseDirNotSetException() {
        super("Project base directory is not set.");
    }
}
