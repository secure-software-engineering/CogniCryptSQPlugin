package org.sonarsource.plugins.secai.utils;

import crypto.listener.AnalysisStatistics;
import org.sonarsource.plugins.secai.settings.SecAISettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TimeTracker {

    private static Map<String, Float> otherTimings = new HashMap<>();

    // general
    private static float jarGeneration = -1;
    private static float issueReporting = -1;
    private static int issueNumber = 0;

    // for CogniCrypt
    private static float ccAnalysis = -1;
    private static AnalysisStatistics ccStatistics = null;
    private static float datacollection = -1; // includes all steps
    private static float codeSnippet = 0;
    private static float messageDeconstruction = 0;
    private static float locatingVarDefs = 0;
    private static float severity = 0;
    private static float confidence = 0;

    public static float setJarGenerationTime(float ms) {
        return TimeTracker.jarGeneration = ms;
    }

    public static float setIssueReportingTime(float ms) {
        return TimeTracker.issueReporting = ms;
    }

    public static void setIssueNumber(int i) {
        issueNumber = i;
    }

    public static float setCcAnalysisTime(float ms) {
        return TimeTracker.ccAnalysis = ms;
    }

    public static float setDatacollectionTime(float ms) {
        return TimeTracker.datacollection = ms;
    }

    public static void addSeverityTime(float ms) {
        TimeTracker.severity += ms;
    }

    public static void addConfidenceTime(float ms) {
        TimeTracker.confidence += ms;
    }

    public static void addCodeSnippetTime(float ms) {
        TimeTracker.codeSnippet += ms;
    }

    public static void addMessageDeconstructionTime(float ms) {
        TimeTracker.messageDeconstruction += ms;
    }

    public static void addLocatingVarDefsTime(float ms) {
        TimeTracker.locatingVarDefs += ms;
    }

    public static void addOther(String key, float ms) {
        otherTimings.put(key, ms);
    }

    public static void setCcStatistics(AnalysisStatistics stats) {
        ccStatistics = stats;
    }

    public static void toFile() throws IOException {
        StringBuilder out = new StringBuilder();

        out.append("Analysis on project ").append(SecAISettings.getInstance().getProjectKey()).append(":\n");

        out.append("\t- Jar generated in: ").append((jarGeneration == -1 ? "N/A" : jarGeneration + " ms")).append("\n");
        out.append("\t- CogniCrypt analysis finished in: ").append((ccAnalysis == -1 ? "N/A" : ccAnalysis + " ms")).append("\n");
        out.append("\t- Report data collected in: ").append((datacollection == -1 ? "N/A" : datacollection + " ms")).append("\n");
        out.append("\t- Issues reported to SonarQube in: ").append((issueReporting == -1 ? "N/A" : issueReporting + " ms")).append("\n");

        out.append("\nDetailed CogniCrypt statistics:\n");
        if (ccStatistics == null) {
            out.append("\tN/A");
        } else {
            out.append("\t- Call graph construction time: ").append(ccStatistics.getCallGraphTime()).append("\n");
            out.append("\t- Typestate analysis time: ").append(ccStatistics.getTypestateTime()).append("\n");
            out.append("\t- Analysis time: ").append(ccStatistics.getAnalysisTime()).append("\n");
        }

        out.append("\nTime spent on data collection steps (combined over all ").append(issueNumber).append(" issues):\n");
        out.append("\t- Extraction of code snippets: ").append((codeSnippet == 0 ? "N/A" : codeSnippet + " ms")).append("\n");
        out.append("\t- Analysis of CogniCrypt error messages: ").append((messageDeconstruction == 0 ? "N/A" : messageDeconstruction + " ms")).append("\n");
        out.append("\t- Locating of variable definitions: ").append((locatingVarDefs == 0 ? "N/A" : locatingVarDefs + " ms")).append("\n");
        out.append("\t- Severity scores: ").append((severity == 0 ? "N/A" : severity + " ms")).append("\n");
        out.append("\t- CPG generation: ").append((confidence == 0 ? "N/A" : confidence + " ms")).append("\n");

        out.append("\nOther:\n");
        if (otherTimings.isEmpty()) {
            out.append("\tN/A");
        } else {
            otherTimings.forEach((key, value) -> out.append("\t- ").append(key).append(": ").append(value).append(" ms\n"));
        }

        File f = new File(ReportService.getInstance().getReportDir(SecAISettings.getInstance().getProjectKey()),
                "timings_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt");
        try (FileWriter fileWriter = new FileWriter(f, false)) {
            fileWriter.write(out.toString());
        }
    }
}
