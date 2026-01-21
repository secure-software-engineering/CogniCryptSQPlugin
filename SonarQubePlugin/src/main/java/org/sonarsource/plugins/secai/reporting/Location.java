package org.sonarsource.plugins.secai.reporting;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Location {

    private final int[] start;
    private final int[] end;
    private final String className;
    private final String filePath;

    private int[] singleLineIndex = null;

    /**
     * Combines key info about a location.
     * @param startLine starting line (first line of a file is line 1)
     * @param startOffset character offset within the starting line
     * @param endLine inclusive end line (first line of a file is line 1)
     * @param endOffset inclusive offset in the end line
     * @param className name of the class in which this position is located
     * @param filePath path of the file
     */
    public Location(int startLine, int startOffset, int endLine, int endOffset, String className, String filePath) {
        this.start = new int[]{startLine, startOffset};
        this.end = new int[]{endLine, endOffset};
        this.className = className;
        this.filePath = filePath;
    }

    public Location(int[][] position, String className, String filePath) {
        this(position[0][0], position[0][1], position[1][0], position[1][1], className, filePath);
    }

    /**
     * Constructor to use if your position info is based on a code snippet that could have multiple lines that were inlined using "\n".
     * @param startLine line where your code snippet starts (first line of a file is line 1)
     * @param index position within the inlined code snippet
     * @param codeSnippet inlined code snippet
     * @param className name of the class in which the position and code snippet are located
     * @param filePath path of the file (relative from base directory)
     */
    public Location(int startLine, int[] index, String codeSnippet, String className, String filePath) {
        this.singleLineIndex = Arrays.copyOf(index, index.length);
        int[][] position = getMultiLine(startLine, index, codeSnippet);
        this.start = position[0];
        this.end = position[1];
        this.className = className;
        this.filePath = filePath;
    }

    public Map<String, Object> getAsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("start", start);
        map.put("end", end);
        map.put("className", className);
        map.put("filePath", filePath);
        return map;
    }

    public int[] getStart() {
        return start;
    }

    public int[] getEnd() {
        return end;
    }

    public String getClassName() {
        return className;
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     * When reporting an issue to SonarQube a {@link TextRange} is needed.
     * @param inputFile SonarQube internal file representation of which to select the text range
     * @return {@link TextRange} of the location
     */
    public TextRange getTextRange(InputFile inputFile) {
        return inputFile.newRange(start[0], start[1], end[0], end[1] + 1);
    }

    private int[][] getMultiLine(int startLine, int[] index, String codeSnippet) {
        int[][] position = new int[2][2];

        if (!codeSnippet.contains("\n")) {
            position[0][0] = startLine;
            position[0][1] = index[0];
            position[1][0] = startLine;
            position[1][1] = index[1];
        } else {
            boolean startFound = false;
            boolean endFound = false;
            String[] lines = codeSnippet.split("\n");

            for (int i = 0; i < lines.length; i++) {
                if (!startFound && index[0] < lines[i].length()) {
                    position[0][0] = startLine + i;
                    position[0][1] = index[0];
                    startFound = true;
                }

                if (!endFound && index[1] < lines[i].length()) {
                    position[1][0] = startLine + i;
                    position[1][1] = index[1];
                    endFound = true;
                }

                if (startFound && endFound) {
                    break;
                } else {
                    // -1 because of the \n that was removed during the split
                    index[0] = index[0] - lines[i].length() - 1;
                    index[1] = index[1] - lines[i].length() - 1 ;
                }
            }
        }

        return position;
    }

    public int[] getSingleLineIndex() {
        return singleLineIndex;
    }

    @Override
    public String toString() {
        return "Location{" +
                "start=" + Arrays.toString(start) +
                ", end=" + Arrays.toString(end) +
                ", className='" + className + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Location location)) return false;
        return Objects.deepEquals(start, location.start) && Objects.deepEquals(end, location.end) && Objects.equals(className, location.className) && Objects.equals(filePath, location.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(start), Arrays.hashCode(end), className, filePath);
    }
}
