package org.sonarsource.plugins.secai.analysis;

public enum AnalysisTool {
    COGNICRYPT ("CogniCrypt");

    private final String displayName;

    public static final AnalysisTool[] toolsAnalyzingJars = new AnalysisTool[]{
            COGNICRYPT
    };

    AnalysisTool(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static AnalysisTool getByDisplayName(String displayName) {
        for (AnalysisTool tool : values()) {
            if (tool.displayName().equals(displayName)) {
                return tool;
            }
        }
        return null;
    }
}
