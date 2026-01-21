package org.sonarsource.plugins.secai.utils;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class SarifDeserialization {

    public static class ViolationReport {
        @SerializedName("sarifVersion")
        private String sarifVersion;

        @SerializedName("runs")
        private List<Run> runs;

        // Getters and Setters
        public String getSarifVersion() {
            return sarifVersion;
        }

        public void setSarifVersion(String sarifVersion) {
            this.sarifVersion = sarifVersion;
        }

        public List<Run> getRuns() {
            return runs;
        }

        public void setRuns(List<Run> runs) {
            this.runs = runs;
        }
    }

    public static class Run {
        @SerializedName("summary")
        private Summary summary;

        @SerializedName("files")
        private Map<String, File> files;

        @SerializedName("resources")
        private Resources resources;

        @SerializedName("results")
        private List<Result> results;

        // Getter
        public Summary getSummary() {
            return summary;
        }

        public Map<String, File> getFiles() {
            return files;
        }

        public Resources getResources() {
            return resources;
        }

        public List<Result> getResults() {
            return results;
        }

    }

    public static class Summary {
        @SerializedName("errorCounts")
        private Map<String, Integer> errorCounts;

        @SerializedName("statistics")
        private Statistics statistics;

        // Getter
        public Map<String, Integer> getErrorCounts() {
            return errorCounts;
        }

        public Statistics getStatistics() {
            return statistics;
        }

    }

    public static class Statistics {
        @SerializedName("entryPoints")
        private int entryPoints;

        @SerializedName("analysisTime")
        private String analysisTime;

        @SerializedName("edgesInCallGraph")
        private int edgesInCallGraph;

        @SerializedName("callGraphConstructionTime")
        private String callGraphConstructionTime;

        @SerializedName("typestateAnalysisTime")
        private String typestateAnalysisTime;

        @SerializedName("reachableMethods")
        private int reachableMethods;

        // Getter
        public int getEntryPoints() {
            return entryPoints;
        }

        public String getAnalysisTime() {
            return analysisTime;
        }

        public int getEdgesInCallGraph() {
            return edgesInCallGraph;
        }

        public String getCallGraphConstructionTime() {
            return callGraphConstructionTime;
        }

        public String getTypestateAnalysisTime() {
            return typestateAnalysisTime;
        }

        public int getReachableMethods() {
            return reachableMethods;
        }

    }

    public static class File {
        @SerializedName("mimeType")
        private String mimeType;

        // Getter
        public String getMimeType() {
            return mimeType;
        }

    }

    public static class Resources {
        @SerializedName("rules")
        private List<String> rules;

        // Getter
        public List<String> getRules() {
            return rules;
        }

    }

    public static class Result {
        @SerializedName("violatedRule")
        private String violatedRule;

        @SerializedName("errorType")
        private String errorType;

        @SerializedName("locations")
        private List<List<Location>> locations;

        @SerializedName("message")
        private Message message;

        // Getter
        public String getViolatedRule() {
            return violatedRule;
        }

        public String getErrorType() {
            return errorType;
        }

        public List<List<Location>> getLocations() {
            return locations;
        }

        public Message getMessage() {
            return message;
        }

    }

    public static class Location {
        @SerializedName("physicalLocation")
        private PhysicalLocation physicalLocation;

        @SerializedName("fullyQualifiedLogicalName")
        private String fullyQualifiedLogicalName;

        // Getter
        public PhysicalLocation getPhysicalLocation() {
            return physicalLocation;
        }

        public String getFullyQualifiedLogicalName() {
            return fullyQualifiedLogicalName;
        }

    }

    public static class PhysicalLocation {
        @SerializedName("fileLocation")
        private FileLocation fileLocation;

        @SerializedName("region")
        private Region region;

        // Getter
        public FileLocation getFileLocation() {
            return fileLocation;
        }

        public Region getRegion() {
            return region;
        }
    }

    public static class FileLocation {
        @SerializedName("uri")
        private String uri;

        // Getter
        public String getUri() {
            return uri;
        }
    }

    public static class Region {
        @SerializedName("method")
        private String method;

        @SerializedName("startLine")
        private String startLine;

        @SerializedName("statement")
        private String statement;

        // Getter
        public String getMethod() {
            return method;
        }

        public String getStartLine() {
            return startLine;
        }

        public String getStatement() {
            return statement;
        }
    }

    public static class Message {
        @SerializedName("text")
        private String text;

        @SerializedName("richText")
        private String richText;

        // Getter
        public String getText() {
            return text;
        }

        public String getRichText() {
            return richText;
        }
    }
}

