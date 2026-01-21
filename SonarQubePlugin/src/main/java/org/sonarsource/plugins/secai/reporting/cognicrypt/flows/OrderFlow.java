package org.sonarsource.plugins.secai.reporting.cognicrypt.flows;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarsource.plugins.secai.utils.ResourceService;
import crysl.CrySLParser;
import crysl.parsing.CrySLParserException;
import crysl.rule.CrySLMethod;
import crysl.rule.CrySLRule;
import crysl.rule.StateMachineGraph;
import crysl.rule.TransitionEdge;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.InvokableStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.java.core.JavaSootMethod;

public class OrderFlow implements AbstractFlow {

    private NewIssue issue;
    private InputFile inputFile;
    private List<NewIssueLocation> locations = new ArrayList<>();
    private JavaSootMethod sootMethod;

    public OrderFlow(NewIssue issue, InputFile inputFile, JavaSootMethod sootMethod) {
        this.issue = issue;
        this.inputFile = inputFile;
        this.sootMethod = sootMethod;
    }

    public void addFlow(String violatedRule) throws IOException {
        CrySLParser parser = new CrySLParser();
        try {
            CrySLRule rule = parser.parseRuleFromFile(
                    new File(ResourceService.getInstance().getCrySLRules(), violatedRule + ".crysl"));
            StateMachineGraph usage = rule.getUsagePattern();

            Map<String, List<String>> groupedExpectedMethods = new LinkedHashMap<>();

            // Group methods by base name
            for (TransitionEdge edge : usage.getAllTransitions()) {
                for (CrySLMethod method : edge.getLabel()) {
                    String name = method.getName();
                    String key = name.split("\\(")[0]; // e.g. "init"
                    groupedExpectedMethods.computeIfAbsent(key, k -> new ArrayList<>()).add(name);
                }
            }

            // Collect actually called methods
            Set<String> actualMethods = new HashSet<>();
            for (Stmt stmt : sootMethod.getBody().getStmts()) {
                if (stmt.isInvokableStmt()) {
                    InvokableStmt invokableStmt = (InvokableStmt) stmt;
                    Optional<AbstractInvokeExpr> optionalInvoke = invokableStmt.getInvokeExpr();
                    optionalInvoke.ifPresent(expr -> actualMethods.add(expr.getMethodSignature().getName()));
                }
            }

            // Identify missing required groups
            Set<String> missingRequiredGroups = new LinkedHashSet<>();
            for (String groupKey : groupedExpectedMethods.keySet()) {
                boolean satisfied = groupedExpectedMethods.get(groupKey)
                        .stream()
                        .anyMatch(actualMethods::contains);
                if (!satisfied) {
                    missingRequiredGroups.add(groupKey);
                }
            }

            // Developer-friendly short summaries per group
            Map<String, String> groupSummaries = Map.of(
                    "Init", "Cipher must be initialized using `init(...)`.",
                    "Get", "Obtain a Cipher instance using `getInstance(...)`.",
                    "Update", "Call `update(...)` to process data before `doFinal()`.",
                    "DoFinal", "Finalize the encryption or decryption with `doFinal(...)`.",
                    "wrap", "Wrap a key for secure transport or storage.",
                    "updateAAD", "Update additional authenticated data."
            );

            // Compose a single, super-readable message block
            StringBuilder mainMsg = new StringBuilder();
            mainMsg.append("*Missing required steps for* `").append(rule.getClassName()).append("` usage:\n\n");

            int count = 1;
            for (String groupName : missingRequiredGroups) {
                String groupSummary = groupSummaries.getOrDefault(groupName, "-");
                mainMsg.append(count).append(". **").append(groupName).append("** — ").append(groupSummary).append("\n");
                count++;
            }

            // Compose the GitHub link to the CrySL rule file
            String cryslClass = rule.getClassName();
            String ruleFileName = cryslClass.substring(cryslClass.lastIndexOf('.') + 1) + ".crysl";
            String docLink = "https://github.com/CROSSINGTUD/CogniCrypt_DOC/blob/master/crysl_rules/" + ruleFileName;

            // Add a clickable markdown link
            mainMsg.append("\n[View CrySL rule on GitHub](").append(docLink).append(")\n");

            // Only report if something is missing!
            if (!missingRequiredGroups.isEmpty()) {
                int line = sootMethod.getBody().getPosition().getFirstLine();
                TextRange range = inputFile.selectLine(line);
                locations.clear(); // Only one location for all groups
                locations.add(issue.newLocation()
                        .on(inputFile)
                        .at(range)
                        .message(mainMsg.toString()));

                issue.addFlow(locations, NewIssue.FlowType.EXECUTION, "ORDER-based group validation");
            }

        } catch (CrySLParserException e) {
            throw new IOException("Failed to parse CrySL rule: " + violatedRule, e);
        }
    }
}



