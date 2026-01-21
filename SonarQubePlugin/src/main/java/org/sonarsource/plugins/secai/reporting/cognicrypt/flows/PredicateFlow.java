package org.sonarsource.plugins.secai.reporting.cognicrypt.flows;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import java.util.ArrayList;
import java.util.List;

public class PredicateFlow implements AbstractFlow {

    private NewIssue issue;
    private InputFile inputFile;
    private List<NewIssueLocation> locations = new ArrayList<>();

    public PredicateFlow(NewIssue issue, InputFile inputFile) {
        this.issue = issue;
        this.inputFile = inputFile;
    }

    public void addFlow(String violatedRule) {
        // TODO: do stuff to fill locations
        // add parameters through other methods or modify the interface

        issue.addFlow(locations, NewIssue.FlowType.EXECUTION, null);
    }
}
