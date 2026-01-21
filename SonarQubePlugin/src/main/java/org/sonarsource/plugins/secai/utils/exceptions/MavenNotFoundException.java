package org.sonarsource.plugins.secai.utils.exceptions;

public class MavenNotFoundException extends Exception {

    public MavenNotFoundException() {
        super("Could not find find Maven. Please add the path in the settings.");
    }
}
