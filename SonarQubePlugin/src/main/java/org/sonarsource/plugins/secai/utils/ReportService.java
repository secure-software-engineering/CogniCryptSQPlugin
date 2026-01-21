package org.sonarsource.plugins.secai.utils;

import java.io.File;
import java.io.IOException;

public class ReportService {

    private static final String REPORT_DIR = System.getProperty("user.home") + File.separator + "secai"
            + File.separator + "reports";

    private static ReportService INSTANCE;

    private ReportService() throws IOException {

        // Creating permanent directories
        createDirectory(REPORT_DIR);
    }

    /**
     * Creates a directory if it does not exist.
     * @param path Directory path
     */
    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Created directory: " + path);
        }
    }

    public static ReportService getInstance() throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new ReportService();
        }

        return INSTANCE;
    }


    public String getReportDir() {
        return REPORT_DIR;
    }

    /**
     * To sanitize project key and replacing any non-word or numbers with _
     * @param projectKey DEFAULT: SonarAnalyzer:ProjectName
     * @return directory path with safeProjectKey
     */
    public String getReportDir(String projectKey) {
        String safeProjectKey = projectKey.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    
        File dir = new File(REPORT_DIR, safeProjectKey);
        dir.mkdir();
        return dir.getAbsolutePath();
    }
    

    public String getCogniCryptReport(String projectKey) {
        String safeProjectKey = projectKey.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        return REPORT_DIR + File.separator + safeProjectKey + File.separator + "CryptoAnalysis-Report.json";
    }
}
