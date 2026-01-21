package org.sonarsource.plugins.secai.reporting.cognicrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CCErrorType;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CogniCryptRulesDefinition;
import org.sonarsource.plugins.secai.api.ConfidenceScoreApiClient;
import org.sonarsource.plugins.secai.reporting.*;
import org.sonarsource.plugins.secai.reporting.cognicrypt.flows.OrderFlow;
import org.sonarsource.plugins.secai.settings.SecAISettings;
import org.sonarsource.plugins.secai.utils.DotPacking;
import org.sonarsource.plugins.secai.utils.TimeTracker;

import scala.util.parsing.json.JSON;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Position;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

import java.io.IOException;
import java.util.*;

public class NewCCIssue {

    private Logger LOGGER = LoggerFactory.getLogger(NewCCIssue.class);

    private JavaSootClass sootClass;
    private JavaSootMethod sootMethod;

    private CodeSnippet codeSnippet;
    private DeconstructedCCMessage ccMessage;
    private CCErrorType errorType;
    private String fullRuleName;
    private String violatedRule;
    private String methodCall;

    private Location location;
    private List<String> preceding;
    private List<String> subsequent;
    private boolean wasUpdated = false;
    private Severity severity;
    private Severity priority;
    private double confidence = 1.0;
    private String dotGraphB64Gz = null;

    private int errorID;

    QuickFix quickFixes = null;

    public NewCCIssue(String hashcode, CodeSnippet codeSnippet, DeconstructedCCMessage message, String errorType, String violatedRule,
                      String methodCall, List<String> precedingErrors, List<String> subsequentErrors) throws Exception {
        this.codeSnippet = codeSnippet;
        this.ccMessage = message;
        this.errorType = CCErrorType.byName(errorType);
        this.fullRuleName = violatedRule;
        this.violatedRule = violatedRule.substring(violatedRule.lastIndexOf(".") + 1);
        MethodSignature methodSignature = JavaIdentifierFactory.getInstance().parseMethodSignature(methodCall);
        this.methodCall = methodCall.startsWith("<") && methodCall.endsWith(">")
                ? methodCall.substring(1, methodCall.length() - 1) : methodCall;
        this.preceding = precedingErrors;
        this.subsequent = subsequentErrors;

        Parameter parameter = null;

        /* compute exact location based on parameter index:
         *      >= 0    => location of this parameter
         *      -1      => return value -> if assignment mark variable it was assigned to, else mark method call
         *      -5      => mark entire method call
         *      -10     => no useful info found -> mark entire line
         */
        if (ccMessage.parameterIndex >= 0) {
            parameter = codeSnippet.getParameters().get(ccMessage.parameterIndex);
            // -> now "parameter != null" implies that "ccMessage.parameterIndex >= 0"

            location = parameter.getLocation();
        } else if (ccMessage.parameterIndex == -1) {
            location = codeSnippet.getMethodBounds();
        } else if (ccMessage.parameterIndex == -5) {
            location = codeSnippet.getMethodBounds();
        } else if (ccMessage.parameterIndex == -10) {
            location = codeSnippet.getSnippetBounds();
        }

        // get the exact statement from SootUp
        CentralSootUp sootUp = CentralSootUp.getInstance();
        sootClass = sootUp.getSootClass(location.getClassName());
        sootMethod = sootUp.getSootMethod(methodSignature);
        Stmt violatingLine;

        if (parameter != null) {
            violatingLine = sootUp.getRelevantStmt(codeSnippet, sootMethod, parameter);
        } else {
            violatingLine = sootUp.getRelevantStmt(codeSnippet, sootMethod);
        }

        // compute severity score
        float time = System.nanoTime();
        this.severity = computeSeverity();
        priority = severity;
        TimeTracker.addSeverityTime((System.nanoTime() - time) / 1000000);

        // compute confidence score
        time = System.nanoTime();
        if (violatingLine == null){
            LOGGER.warn("Violating line is null for jimple statement (from CogniCrypt) \"{}\"", codeSnippet.getJimpleStatement());
            confidence = 0.0;
        } else {
            //confidence = computeConfidenceScore(sootUp.getCPG(this.methodCall, violatingLine.toString()));
            String dotGraph = sootUp.getCPG(methodSignature, violatingLine);

            // compress+encode to keep JSON small and avoid escaping pain
            dotGraphB64Gz = DotPacking.toGzipBase64(dotGraph);

            // You can uncomment the following lines to enable actual confidence computation via the web service

            // ConfidenceScoreApiClient apiClient = new ConfidenceScoreApiClient();
            // ConfidenceScoreApiClient.Response response = apiClient.getConfidenceResponse(hashcode, dotGraph);
            // if (response != null) {
            //     confidence = response.probability_score;
            //     hashcode = response.hashcode;
            // } else {
            //     confidence = 0.0;
            //     hashcode = response.hashcode;
            // }
        }
        TimeTracker.addConfidenceTime((System.nanoTime() - time) / 1000000);

        // add location of the origin of the parameter
        // -> can happen for:
        //      ConstraintError,
        //      ImpreciseValueExtractionError,
        //      RequiredPredicateError,
        //      AlternativeReqPredicateError
        if (parameter != null && parameter.isVariable() && violatingLine != null) {
            time = System.nanoTime();
            addInit(parameter, violatingLine);
            TimeTracker.addLocatingVarDefsTime((System.nanoTime() - time) / 1000000);
        }

        // quickfixes
        if (errorType.equals("ConstraintError") && parameter != null) {
            if (parameter.isVariable() && parameter.getSecondaryLoc() != null && !parameter.isFromMethodParameter()) {
                addConstraintQuickfixes(ccMessage.getViolatedConstraint(), parameter.getSecondaryLoc());
            } else if (!parameter.isVariable() || parameter.isFromMethodParameter()) {
                addConstraintQuickfixes(ccMessage.getViolatedConstraint(), location);
            }
        }
    }

    public void assembleIssue(InputFile inputFile, NewIssue issue) throws IOException {
        String message = ccMessage.getEditedMessage(SecAISettings.getInstance().getCcMessageType());

        // override the default severity score
        // -> the previously (in constructor) computed severity should have been updated already w.r.t. error tree
        //   in IssueReporter
        issue.overrideImpact(SoftwareQuality.SECURITY, severity);

        // add actual issue location
        TextRange rangeLocation = location.getTextRange(inputFile);
        NewIssueLocation issueLocation = issue.newLocation().on(inputFile)
                .at(rangeLocation).message(message);
        issue.at(issueLocation);

        // add locations of additional relevant parameters
        if (ccMessage.getAdditionalParameters() != null) {
            for (Parameter p : ccMessage.getAdditionalParameters()) {
                // we assume that it's in the same file for now TODO
                issue.addLocation(issue.newLocation().on(inputFile)
                        .at(p.getLocation().getTextRange(inputFile))
                        .message("Relevant parameter"));
            }
        }

        // add origin of the problematic variable (if found/applicable)
        if (ccMessage.parameterIndex >= 0 &&
                codeSnippet.getParameters().get(ccMessage.parameterIndex).getSecondaryLoc() != null) {
            issue.addLocation(issue.newLocation().on(inputFile)
                    .at(codeSnippet.getParameters().get(ccMessage.parameterIndex).getSecondaryLoc().getTextRange(inputFile))
                    .message("Origin of variable \"" + codeSnippet.getParameters().get(ccMessage.parameterIndex).getValue() + "\""));
        }

        // add flows
        /*if (errorType == CCErrorType.INCOMPLETE_OPERATION || errorType == CCErrorType.TYPESTATE) {
            OrderFlow orderFlow = new OrderFlow(issue, inputFile, sootMethod);
            orderFlow.addFlow(violatedRule);
        }//*/

        // report quickfixes
        // Note: Currently SonarQube does not support custom quick fixes, however, if this  changes simply uncommenting the block below should work
        /*if (hasQuickFixes()) {
            issue.setQuickFixAvailable(true);
            quickFixes.forEach(quickFix -> quickFix.report(inputFile, issue));
        }*/
    }

    /**
     * The default severity for issues is "High". This method is used to override this severity score. The current
     * method is based on the paper <a href="https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=10063348">
     *     To Fix or Not to Fix: A Critical Study of Crypto-misuses in the Wild</a>. See
     * <a href="https://docs.sonarsource.com/sonarqube-server/10.8/instance-administration/analysis-functions/instance-mode/mqr-mode/">SonarQube documentation</a>
     * for supported severity levels
     */
    protected Severity computeSeverity() {
        Severity severity = Severity.MEDIUM;
        String violatedConstraint = ccMessage.getViolatedConstraint();
        List<String> expectedMethods = ccMessage.getExpectedMethods();
        List<String> violatedPredicates = ccMessage.getViolatedPredicates();

        try {
            switch (errorType) {
                case CONSTRAINT:
                    // severity is LOW
                    if (
                            // NeverTypeOfError: Usage of String (L)
                            violatedConstraint.contains("neverTypeOf") // NeverTypeOfError: Usage of String (L)
                            // Fewer than 10,000 iterations for PBE (L)
                            // (the other two constraints in PBEKeySpec are neverTypeOf (-> handled) and notHardCoded (-> excluded))
                            || ((violatedRule.equals("PBEKeySpec") || violatedRule.equals("PBEParameterSpec"))
                            && !violatedConstraint.contains("notHardCoded"))
                            // 64-bit block ciphers (L)
                            || (violatedConstraint.equals("algorithm in {\"AES\", \"Camellia\", \"Shacal2\", \"Shacal-2\" } => size in {128, 192, 256}")
                            || violatedConstraint.equals("algorithm in {\"AES\"} => keysize in {128, 192, 256}"))
                            // 64-bit authentication tag GCM (L)
                            || violatedConstraint.equals("tagLen in {96, 104, 112, 120, 128}")
                            // Insecure cryptographic ciphers (L)
                            || ((!violatedConstraint.contains("=>") && violatedConstraint.contains("transformation"))
                            || (violatedConstraint.contains("=>") && violatedConstraint.split("=>")[1].contains("transformation")))
                            // Insecure cryptographic signature (L)
                            || violatedRule.equals("Signature")
                            // Insecure cryptographic MAC (L)
                            || violatedRule.equals("Mac")
                    ) {
                        severity = Severity.LOW;
                    }

                    // default severity is MEDIUM so don't need to update the following:
                    // ECB mode in symmetric ciphers (M)
                    /*else if (violatedConstraint.equals("alg(transformation) in {\"AES\"} => mode(transformation) in { \"GCM\", \"CTR\", \"CTS\", \"CFB\", \"OFB\"}")) {
                        severity = Severity.MEDIUM;
                    }*/
                    // Trigger Exception (M) -> not sure how to detect

                    // severity is HIGH
                    else if (
                            // Predictable/constant crypto keys (H)
                            // Predictable/constant passwords (H)
                            violatedConstraint.contains("notHardCoded") // -> most violations will be required predicate "randomized"
                            // Insecure SSL/TLS standard (H)
                            || violatedConstraint.equals("elements(protocols) in {\"TLSv1.2\", \"TLSv1.3\"}")
                            || violatedConstraint.equals("protocol in {\"TLSv1.2\", \"TLSv1.3\"}")
                            // Insecure cryptographic hash (H) TODO: find constraints
                    ) {
                        severity = Severity.HIGH;
                    }
                    break;
                case INCOMPLETE_OPERATION:
                    // severity is LOW
                    // Missed to clear password (L)
                    if (expectedMethods.contains("clearPassword")) {
                        severity = Severity.LOW;
                    }

                    // default severity is MEDIUM so don't need to update the following:
                    // Missed to pass data (M)
                    /*else if (expectedMethods.contains("read") || expectedMethods.contains("write")) {
                        severity = Severity.MEDIUM;
                    }*/

                    // severity is HIGH
                    else if (
                            // Missed to finish a crypto function (H)
                            expectedMethods.contains("doFinal") // -> for Cipher and Mac
                            // Insecure SSL/TLS standard (H) TODO: is this IncompleteOperationError?
                    ) {
                        severity = Severity.HIGH;
                    }
                    break;
                case ALTERNATIVE_REQ_PREDICATE, REQUIRED_PREDICATE:
                    // severity is LOW
                    if (
                            // Insecure cryptographic ciphers (L) TODO: this probably isn't right
                            violatedPredicates.contains("generatedCipher")
                    ) {
                        severity = Severity.LOW;
                    }

                    // default severity is MEDIUM so don't need to update the following:
                    /*else if (
                            // Cryptographically insecure PRNGs (M)
                            violatedRule.equals("SecureRandom")
                            // Static Salts in PBE (M) -> randomized[salt]
                            || violatedRule.equals("PBEKeySpec") || violatedRule.equals("PBEParameterSpec")
                            // Static IVs in CBC mode symmetric ciphers (M)
                            || expectedMethods.contains("preparedIV")
                    ) {
                        severity = Severity.MEDIUM;
                    }//*/

                    // severity is HIGH
                    else if (
                            // Predicatable/constant crypto keys (H) -> more?
                            violatedPredicates.contains("randomized")
                            // Insecure TrustManager (H)
                            || violatedPredicates.contains("generatedTrustManager")
                            // Insecure cryptographic hash (H) TODO
                    ) {
                        severity = Severity.HIGH;
                    }
                    break;
                case FORBIDDEN_METHOD:
                    if (
                            // Predicatable/constant passwords for PBE (H)
                            violatedRule.equals("PBEKeySpec")
                            // Insecure SSL/TLS standard (H)
                            || violatedRule.equals("SSLContext")
                    ) {
                        severity = Severity.HIGH;
                    }
                    break;
                case TYPESTATE:
                    // default severity is MEDIUM so don't need to update the following:
                    // Missed to pass data (M)
                    /*if (expectedMethods.contains("read") || expectedMethods.contains("write")) {
                        severity = Severity.MEDIUM;
                    }//*/
                    // Trigger Exception (M) -> not sure how to detect

                    /*else*/ if (
                            // Predictable/constant crypto keys (H) TODO
                            // Missed to finish crypto function (H)
                            expectedMethods.contains("doFinal")
                            // Insecure SSL/TLS standard (H) TODO: is this TypestateError?
                    ) {
                        severity = Severity.HIGH;
                    }
                    break;
                case IMPRECISE_VALUE_EXTRACTION:
                    if (violatedRule.equals("Cipher")) {
                        severity = Severity.HIGH;
                    } else {
                        severity = Severity.INFO;
                    }
                    break;
            }
        } catch (NullPointerException e) {
            LOGGER.warn(e.getMessage() + ". Defaulting to severity MEDIUM");
        }

        return severity;
    }

    /**
     * This method is used to update the priority that was originally the same as the severity. Currently, this is simply 
     * setting a new severity based on the severity of the issues subsequent issues. (This method is called in IssueReporter)
     * @param sev
     */
    public void updatePriority(Severity sev) {
        // TODO: include confidence score somewhere
        // only update if the priority is higher or equal
        // (if we didn't update when equal, then the node would be chosen again for propagating up)
        if (sev.compareTo(priority) >= 0) {
            priority = sev;
            wasUpdated = true;
        }
    }

    /**
     * Locating the origin of a variable with SootUp (limited to method scope)
     * @param parameter
     */
    private void addInit(Parameter parameter, Stmt violatingLine) throws IOException, MalformedInputException, CentralSootUp.NoCentralSootUpInstanceException {
        if (violatingLine == null)
            throw new MalformedInputException("Can't find additional locations because the violating line is null");
        String variable;
        String pointerName = null;

        // for global variables the jimple representation contains a jimple variable instead of the name of the global var
        // also check that the variable name is followed by a "," or a ")" because if a new value is assigned to a
        // variable name it's not "name = value1; name = value2;" but "name#0 = value1; name#1 = value2;"
        if (violatingLine.toString().contains(parameter.getValue() + ",")
                || violatingLine.toString().contains(parameter.getValue() + ")")) {
            //System.out.println("value");
            variable = parameter.getValue();
        } else if (violatingLine.toString().contains(parameter.getJimpleVar() + ",")
                || violatingLine.toString().contains(parameter.getJimpleVar() + ")")) {
            //System.out.println("jimple var");
            variable = parameter.getJimpleVar();
        } else {
            int index = violatingLine.toString().lastIndexOf("(");
            String[] params = violatingLine.toString().substring(index + 1, violatingLine.toString().length() - 1).split(", ");
            variable = params[parameter.getIndex()];

            if (variable.startsWith("#")) {
                // this is just a pointer -> proceed with actual jimple var
                //System.out.println("pointer");
                pointerName = variable;
                variable = parameter.getJimpleVar();
            } else {
                // sometimes a variable is defined in Java but only an immediate literal in jimple
                // -> can't get definition from SootUp bc it's not define anywhere
                //System.out.println("literal");
                // TODO: back search with strings?
                return;
            }
        }

        Local found = null;
        Local pointerFound = null;
        Set<Local> methodLocals = sootMethod.getBody().getLocals();
        for (Local local : methodLocals) {
            // look for the Local of the variable we're interested in
            if (local.getName().equals(variable)) {
                found = local;
            }

            // if the variable was assigned to a pointer that's used in the violating line, save the pointer too
            if (local.getName().equals(pointerName)) {
                pointerFound = local;
            }

            // break if all the needed locals have been found
            if (found != null && (pointerName == null || pointerFound != null)) {
                break;
            }
        }

        StmtGraph stmtGraph = sootMethod.getBody().getStmtGraph();
        List<Stmt> defs;
        // can't get local defs if violatingLine doesn't use the local and the pointer doesn't go back far enough
        if (pointerFound != null) {
            // get defs of pointer -> look for defs of the assigned jimple var instead
            defs = pointerFound.getDefsForLocalUse(stmtGraph, violatingLine);

            // there should only be one def
            String[] defParts = defs.get(0).toString().split(" ");
            String jimpleVar = defParts[defParts.length - 1];
            violatingLine = defs.get(0);

            for (Local local : methodLocals) {
                // look for the Local of the variable we're interested in
                if (local.getName().equals(jimpleVar)) {
                    //System.out.println("Name matched: " + local.getName());
                    found = local;
                    break;
                }
            }
        }

        defs = found.getDefsForLocalUse(stmtGraph, violatingLine);

        for (Stmt def : defs) {
            Position defPosition = def.getPositionInfo().getStmtPosition();

            // the definition must occur before the violating line and the violating line must be reachable from it
            if (defPosition.getFirstLine() <= getStartLine()
                    && stmtGraph.getExtendedBasicBlockPathBetween(def, violatingLine) != null) {
                Location loc = findLocation(parameter, def, found, defPosition);

                parameter.addSecondaryLoc(loc);

                // no breaking the loop bc we want to get the last possible viable definition
            }
        }
    }

    private Location findLocation(Parameter parameter, Stmt def, Local local, Position defPosition) throws MalformedInputException, CentralSootUp.NoCentralSootUpInstanceException, IOException {
        Location loc;

        // check if global
        if (def.containsFieldRef()) {
            // search for the global var in the declaring class
            // Note: parameter.isVariable() (called before entering this function) doesn't allow periods
            //  so global variables from other classes must have been assigned to another variable to
            //  end up in this function
            loc = JavaGlobalVars.getGlobalsForClass(sootClass.getName()).getVariable(def.getFieldRef().getFieldSignature().getName());
        } else // check if from method parameter
            if (sootMethod.getBody().getParameterLocals().contains(local)) {
            // for local parameters the jimple line is "variable := @parameterX: type"
            // -> split of X for parameter index
            String temp = def.toString().split("@parameter")[1];
            int index = Integer.parseInt(temp.substring(0, temp.indexOf(":")));

            parameter.setFromMethodParameter(true);
            loc = MethodSnippet.get(methodCall).getParameters().get(index).getLocation();
        } else {
            // by creating a new CodeSnippet this way the assigned value is added to the new CodeSnippet as a parameter
            //      -> we can use the location of that
            CodeSnippet cs = new CodeSnippet(new Location(defPosition.getFirstLine(), defPosition.getFirstCol(),
                        defPosition.getLastLine(), defPosition.getLastCol(),
                        sootClass.getName(), parameter.getLocation().getFilePath()), true);

            if (cs.getParameters() == null) {
                loc = cs.getSnippetBounds();
            } else {
                loc = cs.getParameters().get(0).getLocation();
            }
        }
        return loc;
    }

    /**
     *
     * @param location exact location of the value to be changed (JUST the value, not "parameter = value")
     */
    private void addConstraintQuickfixes(String violatedConstraint, Location location) throws IOException, CentralSootUp.NoCentralSootUpInstanceException {
        int lastSet = violatedConstraint.lastIndexOf("{");
        // if there is no set or the right side of an implication doesn't have a set, we don't know what to do for quickfixes
        if (lastSet == -1 || lastSet < violatedConstraint.lastIndexOf("=>")) {
            return;
        }

        List<String> lines;
        try {
            lines = codeSnippet.getLines(location);
        } catch (MalformedInputException e) {
            lines = List.of(new CodeSnippet(location, false).getLines());
        }

        // transformation in Cipher is a more complicated matter -> use secure default
        if (violatedConstraint.substring(0, lastSet).endsWith("transformation) in ")
                || violatedConstraint.substring(0, violatedConstraint.lastIndexOf("{", lastSet + 1)).endsWith("transformation) in ")) {
            LOGGER.debug("Added default quickfix for \"transformation\"");
            quickFixes = new QuickFix(List.of("AES/GCM/NoPadding"), lines, location,
                    "Change value to");
            return;
        }

        // for all other quickfixes -> create fix for each mentioned value
        String[] values = violatedConstraint.substring(lastSet + 1, violatedConstraint.lastIndexOf("}")).split(",\\s");
        LOGGER.debug("Adding {} quickfixes for values {}", values.length, Arrays.toString(values));

        quickFixes = new QuickFix(List.of(values), lines, location, "Change value to");
    }

    public String getFilePath() {
        return codeSnippet.getFilePath();
    }

    public String getClassName() {
        return codeSnippet.getClassName();
    }

    public RuleKey getRuleKey() {
        String ruleKeyString = switch (errorType) {
            case IMPRECISE_VALUE_EXTRACTION -> errorType.toString();
            case INCOMPLETE_OPERATION, TYPESTATE -> "OrderError_" + violatedRule;
            case ALTERNATIVE_REQ_PREDICATE -> "RequiredPredicateError_" + violatedRule;
            default -> errorType + "_" + violatedRule;
        };
        RuleKey ruleKey = CogniCryptRulesDefinition.CRYSL_RULES.get(ruleKeyString);
        if (ruleKey == null) {
            LOGGER.error("Unknown rule key: " + ruleKeyString);
            return null;
        }
        return ruleKey;
    }

    public String getMethodCall() {
        return methodCall;
    }

    public CCErrorType getErrorType() {
        return errorType;
    }

    public List<String> getSubsequent() {
        return subsequent;
    }

    public boolean hasSubsequent() {
        return !subsequent.isEmpty();
    }

    public List<String> getPreceding() {
        return preceding;
    }

    public boolean hasPreceding() {
        return !preceding.isEmpty();
    }

    public Location getLocation() {
        return location;
    }

    public boolean wasUpdated() {
        return wasUpdated;
    }

    public Severity getSeverity() {
        return severity;
    }

    public QuickFix getQuickFixes() {
        return quickFixes;
    }

    public Map<String, Object> getQuickFixesAsMap() {
        return quickFixes != null ? quickFixes.getAsMap() : new HashMap<>();
    }

    public boolean hasQuickFixes() {
        return quickFixes != null;
    }

    public String getJimpleStatement() {
        return codeSnippet.getJimpleStatement();
    }

    public String getCodeSnippet() {
        return codeSnippet.getCodeSnippet();
    }

    public String getEditedMessage(String version) {
        return ccMessage.getEditedMessage(version);
    }

    public String getFullRuleName() {
        return fullRuleName;
    }

    public int getStartLine() {
        return codeSnippet.getStartLine();
    }

    public double getConfidence() {
        return confidence;
    }

    public String getbase64CPG() {
        return dotGraphB64Gz;
    }

    public DeconstructedCCMessage getCcMessage() {
        return ccMessage;
    }

    public void setErrorID(int id) {
        errorID = id;
    }

    public int getErrorID() {
        return errorID;
    }
}
