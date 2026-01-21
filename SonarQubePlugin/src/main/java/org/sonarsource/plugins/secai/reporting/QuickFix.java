package org.sonarsource.plugins.secai.reporting;

import org.apache.commons.lang3.math.NumberUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.fix.NewInputFileEdit;
import org.sonar.api.batch.sensor.issue.fix.NewQuickFix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is merely a container for quick fix information. SonarQube currently does not support custom plugins providing quick fixes. However, it is possible to display this information in a custom web page on the SonarQube server.
 */
public class QuickFix {

    private final Location location;
    private final String message;
    private List<Edit> edits = new ArrayList<>();

    public QuickFix(List<String> values, List<String> lines, Location location, String message) {
        this.location = location;
        this.message = message;

        if (!NumberUtils.isCreatable(values.get(0))) {
            for (String value : values) {
                value = "\"" + value + "\"";
                String edit = createEdit(lines, value);
                this.edits.add(new Edit<String>(value, edit));
            }
        } else {
            // I didn't see any floats in the CrySL rules. If there are some, this condition needs to be adjusted
            for (String value : values) {
                String edit = createEdit(lines, value);
                this.edits.add(new Edit<Integer>(value, edit));
            }
        }
    }

    private String createEdit(List<String> lines, String value) {

        return lines.get(0).substring(0, location.getStart()[1]) +
                value +
                lines.get(lines.size() - 1).substring(location.getEnd()[1] + 1);
    }

    public Map<String, Object> getAsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("edits", edits);
        map.put("message", message);
        map.put("location", location.getAsMap());
        return map;
    }

    /**
     * Report the quickfixes to SonarQube.
     * @param inputFile
     * @param issue
     */
    public void report(InputFile inputFile, NewIssue issue) {
        for (Edit edit : edits) {
            NewQuickFix quickFix = issue.newQuickFix().message(message + " " + edit.getValue());
            NewInputFileEdit inputFileEdit = quickFix.newInputFileEdit().on(inputFile);
            inputFileEdit.addTextEdit(inputFileEdit.newTextEdit().withNewText((String) edit.getValue())
                    .at(location.getTextRange(inputFile)));
            issue.addQuickFix(quickFix.addInputFileEdit(inputFileEdit));
        }
    }

    public Location getLocation() {
        return location;
    }

    public String getMessage() {
        return message;
    }

    public class Edit<T> {

        private T value;
        private final String edit;

        public Edit(String v, String edit) {
            this.edit = edit;

            // convert the value into the desired type
            try {
                value = (T) v;
            } catch (ClassCastException e) {
                System.out.println("oops");
            }
        }

        public T getValue() {
            return value;
        }

        public String getEdit() {
            return edit;
        }
    }
}
