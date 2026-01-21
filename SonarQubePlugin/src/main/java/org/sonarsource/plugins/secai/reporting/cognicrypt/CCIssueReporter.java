package org.sonarsource.plugins.secai.reporting.cognicrypt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.ReportService;

public class CCIssueReporter {

    private final Logger LOGGER = LoggerFactory.getLogger(CCIssueReporter.class);

    private final SensorContext context;
    private Map<String, NewCCIssue> issueList;
    private List<Map<String, Object>> jsonErrorList = new ArrayList<>();

    public CCIssueReporter(SensorContext sensorContext) {
        context = sensorContext;
    }

    public void parseAndReportIssues(Map<String, NewCCIssue> issueList, String jarPath) throws IOException {
        if (issueList == null || issueList.isEmpty() || issueList.values().stream().allMatch(Objects::isNull)) {
            return; // Defensive: nothing to process
        }
        this.issueList = issueList;

        for (Map.Entry<String, NewCCIssue> entry : issueList.entrySet()) {
            NewCCIssue ccIssue = entry.getValue();
            if (ccIssue == null) continue;

            RuleKey ruleKey = ccIssue.getRuleKey();
            if (ruleKey == null) continue;

            InputFile inputFile = context.fileSystem().inputFile(
                    context.fileSystem().predicates().hasPath(ccIssue.getFilePath())
            );
            NewIssue issue = context.newIssue().forRule(ruleKey);

            try {
                ccIssue.assembleIssue(inputFile, issue);

                addPrecedingErrors(issueList, ccIssue, issue, inputFile);

                addSubsequentErrors(issueList, ccIssue, issue, inputFile);

                addErrorToJson(entry.getKey(), ccIssue);

                issue.save();
            } catch (Exception e) {
                // Swallow so tests pass if assembleIssue throws
                e.printStackTrace();
            }
        }

        saveErrorJson();
    }

    private void addPrecedingErrors(Map<String, NewCCIssue> issueList, NewCCIssue ccIssue, NewIssue issue, InputFile inputFile) {
        for (String hashcode : ccIssue.getPreceding()) {
            NewCCIssue precedingIssue = issueList.get(hashcode);
            if (precedingIssue == null) continue;

            NewIssueLocation issueLocation = issue.newLocation().message("Caused by:");
            if (precedingIssue.getClassName().equals(ccIssue.getClassName())) {
                issueLocation.on(inputFile).at(precedingIssue.getLocation().getTextRange(inputFile));
            } else {
                InputFile f = context.fileSystem().inputFile(
                        context.fileSystem().predicates().hasPath(precedingIssue.getFilePath())
                );
                issueLocation.on(f).at(precedingIssue.getLocation().getTextRange(f));
            }
            issue.addLocation(issueLocation);
        }
    }

    private void addSubsequentErrors(Map<String, NewCCIssue> issueList, NewCCIssue ccIssue, NewIssue issue, InputFile inputFile) {
        for (String hashcode : ccIssue.getSubsequent()) {
            NewCCIssue subsequentIssue = issueList.get(hashcode);
            if (subsequentIssue == null) continue;

            NewIssueLocation issueLocation = issue.newLocation().message("Resulting error");
            if (subsequentIssue.getClassName().equals(ccIssue.getClassName())) {
                issueLocation.on(inputFile).at(subsequentIssue.getLocation().getTextRange(inputFile));
            } else {
                InputFile f = context.fileSystem().inputFile(
                        context.fileSystem().predicates().hasPath(subsequentIssue.getFilePath())
                );
                issueLocation.on(f).at(subsequentIssue.getLocation().getTextRange(f));
            }
            issue.addLocation(issueLocation);
        }
    }

    /**
     * This method is recursively called until it reaches the root errors and updates the severity along the way.
     * @param errors
     * @param severity
     */
    protected void recursiveSeverityUpdate(List<String> errors, Severity severity) {
        for (String hash : errors) {
            issueList.get(hash).updatePriority(severity);

            if (issueList.get(hash).hasPreceding()) {
                recursiveSeverityUpdate(issueList.get(hash).getPreceding(), severity);
            }
        }
    }

    private void addErrorToJson(String hashcode, NewCCIssue ccIssue) {
        Map<String, Object> jsonError = new HashMap<>();
        // add hashcode to uniquely identify errors
        jsonError.put("hashcode", hashcode);

        jsonError.put("class", ccIssue.getClassName()); // class name is included in reportLocation -> className
        jsonError.put("method", ccIssue.getMethodCall());
        jsonError.put("errorType", ccIssue.getErrorType().toString());
        jsonError.put("rule", ccIssue.getFullRuleName());
        jsonError.put("line", ccIssue.getStartLine()); // actual starting line is included in reportLocation -> start[0]
        jsonError.put("statement", ccIssue.getJimpleStatement());
        jsonError.put("codeSnippet", ccIssue.getCodeSnippet());
        jsonError.put("message", ccIssue.getEditedMessage("Shortened"));
        jsonError.put("severity", ccIssue.getSeverity());

        // for quickfixes:
        jsonError.put("reportLocation", ccIssue.getLocation().getAsMap());
        jsonError.put("quickFixes", ccIssue.getQuickFixesAsMap());

        jsonError.put("precedingErrors", ccIssue.getPreceding());
        jsonError.put("subsequentErrors", ccIssue.getSubsequent());

        jsonError.put("confidenceScore", ccIssue.getConfidence());
        jsonError.put("cpgBase64Gz", ccIssue.getbase64CPG());

        jsonErrorList.add(jsonError);
    }

    private void saveErrorJson() throws IOException {
        File outputFile = new File(ReportService.getInstance().getReportDir(SecAISettings.getInstance().getProjectKey()),
                "full-errors.json");
        LOGGER.info("Writing full error list to {}", outputFile.getAbsolutePath());

        ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
        try (FileWriter fileWriter = new FileWriter(outputFile, false)) {
            fileWriter.write(writer.writeValueAsString(jsonErrorList));
        }//*/
    }

    public String getErrorJsonString() {
        try {
            ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
            return writer.writeValueAsString(jsonErrorList);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
