package org.sonarsource.plugins.secai.analysis.cognicrypt;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;

import de.fraunhofer.iem.scanner.ScannerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.plugins.secai.reporting.CentralSootUp;
import org.sonarsource.plugins.secai.utils.TimeTracker;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.ReportService;
import org.sonarsource.plugins.secai.utils.ResourceService;
import org.sonarsource.plugins.secai.reporting.CodeSnippet;
import org.sonarsource.plugins.secai.reporting.cognicrypt.DeconstructedCCMessage;
import org.sonarsource.plugins.secai.reporting.cognicrypt.NewCCIssue;
import org.sonarsource.plugins.secai.utils.SourceCodeService;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

public class CogniCrypt {
    private static final Logger LOGGER = LoggerFactory.getLogger(CogniCrypt.class);

    private final String crySlRules;
    private final String reportPath;
    private final String projectKey;

    public CogniCrypt() throws IOException {

        ResourceService resourceService = ResourceService.getInstance();
        crySlRules = resourceService.getCrySLRules();
        projectKey = SecAISettings.getInstance().getProjectKey();
        reportPath = ReportService.getInstance().getReportDir(projectKey);
    }

    /**
     * generates with help of CogniCrypt a ViolationReport object for better
     * processing of the violations within SonarQube
     *
     * @param corruptedJar path to the Jar to be analyzed
     * @param logHandler logHandler
     * @return the error report containing analysis information and API violations
     */
    public Map<String, NewCCIssue> generateWarnings(String corruptedJar, Consumer<String> logHandler) {

        try {
            String[] args = {
                    "--rulesDir", crySlRules,
                    "--appPath", corruptedJar,
                    "--reportFormat", "SARIF",
                    "--reportPath", reportPath
            };

            LOGGER.debug("Starting CogniCrypt analysis via HeadlessJavaScanner");

            float time = System.nanoTime();
            HeadlessJavaScanner scanner = HeadlessJavaScanner.createFromCLISettings(args);
            scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
            scanner.scan();
            LOGGER.info("CogniCrypt analysis (done) | time={} ms", (long) TimeTracker.setCcAnalysisTime((System.nanoTime() - time) / 1000000));
            TimeTracker.setCcStatistics(scanner.getStatistics());
            time = System.nanoTime();

            Table<WrappedClass, Method, Set<AbstractError>> errors = scanner.getCollectedErrors();

            // list that will be used to create SonarQube issues
            Map<String, NewCCIssue> issueList = new HashMap<>();

            // create new instance of CentralSootUp for the analyzed jar
            CentralSootUp.newInstance(corruptedJar, SourceCodeService.getInstance(projectKey).getSourceDir());

            for (WrappedClass wrappedClass : errors.rowKeySet()) {
                String className = wrappedClass.getFullyQualifiedName();

                for (Map.Entry<Method, Set<AbstractError>> entry : errors.row(wrappedClass).entrySet()) {
                    Method method = entry.getKey();
                    Set<AbstractError> methodErrors = entry.getValue();

                    for (AbstractError error : methodErrors) {
                        int line = error.getLineNumber();
                        String statement = error.getErrorStatement().toString();
                        String fullRuleName = error.getRule().getClassName();
                        String rule = fullRuleName.substring(fullRuleName.lastIndexOf(".") + 1);
                        String errorType = error.getClass().getSimpleName();
                        String hashcode = String.valueOf(error.hashCode());
                        LOGGER.debug("========= Handling error {} =========", hashcode);

                        CodeSnippet codeSnippet = new CodeSnippet(line, className, statement, rule);
                        DeconstructedCCMessage deconstructedCCMessage =
                                new DeconstructedCCMessage(error.toErrorMarkerString(), errorType, rule, codeSnippet);
                      
                        List<String> preceding = new ArrayList<>();
                        for (AbstractError e : error.getPrecedingErrors()) {
                            preceding.add(String.valueOf(e.hashCode()));
                        }

                        List<String> subsequent = new ArrayList<>();
                        for (AbstractError e : error.getSubsequentErrors()) {
                            subsequent.add(String.valueOf(e.hashCode()));
                        }

                        NewCCIssue ccIssue = new NewCCIssue(hashcode, codeSnippet, deconstructedCCMessage, errorType, fullRuleName,
                                method.toString(), preceding, subsequent);
                        ccIssue.setErrorID(error.getErrorId());
                        issueList.put(hashcode, ccIssue);
                    }
                }
            }
            LOGGER.info("Data collection for {} issues (done) | time={} ms", issueList.size(), (long) TimeTracker.setDatacollectionTime((System.nanoTime() - time) / 1000000));

            return issueList;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
