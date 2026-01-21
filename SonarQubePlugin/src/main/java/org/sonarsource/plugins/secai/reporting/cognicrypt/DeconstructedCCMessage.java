package org.sonarsource.plugins.secai.reporting.cognicrypt;

import crysl.CrySLParser;
import crysl.parsing.CrySLParserException;
import crysl.rule.CrySLMethod;
import crysl.rule.CrySLRule;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.plugins.secai.analysis.cognicrypt.CCErrorType;
import org.sonarsource.plugins.secai.reporting.*;
import org.sonarsource.plugins.secai.utils.ResourceService;
import org.sonarsource.plugins.secai.utils.TimeTracker;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeconstructedCCMessage {

    private final Logger LOGGER = LoggerFactory.getLogger(DeconstructedCCMessage.class);

    public int parameterIndex;
    private final String violatedRule;
    private final CCErrorType errorType;

    private final CodeSnippet codeSnippet;
    private String violatedConstraint = null;
    private List<String> expectedMethods = null;
    private List<String> violatedPredicates = null;
    private List<Parameter> additionalParameters = new ArrayList<>();

    /**
     * Contains different versions of the error message
     * <ul>
     *     <li>index 0: original CogniCrypt message</li>
     *     <li>index 1: shortened message</li>
     *     <li>index 2: summary</li>
     * </ul>
     */
    private String[] messages = new String[3];

    public DeconstructedCCMessage(String message, String errorType, String violatedRule, CodeSnippet codeSnippet) throws IOException, CentralSootUp.NoCentralSootUpInstanceException, MalformedInputException {
        if (message == null || message.isBlank()) throw new MalformedInputException("The message to deconstruct was empty");
        float time = System.nanoTime();

        // save parameters
        this.errorType = CCErrorType.byName(errorType);
        this.messages[0] = checkMessage(message);
        this.codeSnippet = codeSnippet;
        this.violatedRule = violatedRule;

        // separate constraint from error message (if present)
        message = shortenConstraint(message);

        // figure out which parameter (of the violating method) is the problematic one
        if (message.toLowerCase().contains("return value")) {
            parameterIndex = -1;
        } else if (errorType.equals("AlternativeReqPredicateError") || errorType.equals("RequiredPredicateError")) {
            // these messages start with the parameter location
            parameterIndex = getStringAsIndex(message);
        } else if (message.toLowerCase().contains(" parameter ")
                // because of possible callTo on right side -> can't locate something that's not there
                && !message.endsWith("is missing")
                // parameter location must refer to the right side
                && message.toLowerCase().lastIndexOf(" parameter ") > message.toLowerCase().indexOf("the right side")
                // right side/single constraint can't be comparison constraint
                && message.toLowerCase().lastIndexOf("extracted the following") <= message.toLowerCase().indexOf("the right side")
        ) {
            // for some messages there is a ":" immediately before the parameter index not a " "
            int from1 = message.lastIndexOf(" ", message.lastIndexOf(" parameter ") - 1);
            int from2 = message.lastIndexOf(":", message.lastIndexOf(" parameter ") - 1);
            parameterIndex = getStringAsIndex(message.substring(from1 > from2 ? from1 + 1 : from2 + 1));
        } else if (errorType.equals("ConstraintError") || errorType.equals("ImpreciseValueExtractionError")) {
            // this is called for ConstraintErrors only if the message doesn't include the position
            parameterIndex = extractParameterLocationFromConstraint(violatedRule);
        } else if (errorType.equals("TypestateError") || errorType.equals("IncompleteOperationError") || errorType.equals("ForbiddenMethodError")){
            parameterIndex = -5;
        } else {
            parameterIndex = -10;
        }
        LOGGER.debug("{} on parameter {}", errorType, parameterIndex);

        // generate other message versions
        // first perform shortening operations
        // then edit the resulting message to replace jimple references
        // -> some reference replacements could interfere with the removal of fully qualified names
        this.messages[1] = editOriginalMessage(message);
        this.messages[2] = summarizeOriginalMessage();

        TimeTracker.addMessageDeconstructionTime((System.nanoTime() - time) / 1000000);
    }

    /**
     * Check if the message includes all required parts needed for the given error type.
     * Only returns a value if the check was successful
     * @param message message to check
     * @return unchanged message
     * @throws MalformedInputException if the message is missing an important component
     */
    private String checkMessage(String message) throws MalformedInputException {
        switch (errorType) {
            // RequiredPredicateError and AlternativeReqPredicateError need to start with parameter reference TODO check for predicates
            case ALTERNATIVE_REQ_PREDICATE, REQUIRED_PREDICATE:
                if (getStringAsIndex(message) == -10) {
                    throw new MalformedInputException("Message format does not match the given error type:" +
                            "\n\tMessage: " + StringEscapeUtils.escapeJava(message) + "\n\tError type: " + errorType);
                }
                break;
            // ImpreciseValueExtractionError/ConstraintError should include the constraint (we don't check the constraint itself)
            case IMPRECISE_VALUE_EXTRACTION, CONSTRAINT:
                int firstQuote = message.indexOf("\""); // There can be a stackoverflow for large inputs so we don't match whole string
                if (!message.substring(0, message.indexOf("\"", firstQuote + 1) + 1).toLowerCase().matches(".*constraint \".+\"")) {
                    throw new MalformedInputException("Message format does not match the given error type:" +
                            "\n\tMessage: " + StringEscapeUtils.escapeJava(message) + "\n\tError type: " + errorType);
                }
                break;
            // IncompleteOperationError needs to include missing methods TODO (we don't check that given method references are correctly formatted)
            case INCOMPLETE_OPERATION:
                if (!message.toLowerCase().matches(".*methods \\{.*}.*")) {
                    throw new MalformedInputException("Message format does not match the given error type:" +
                            "\n\tMessage: " + StringEscapeUtils.escapeJava(message) + "\n\tError type: " + errorType);
                }
                break;
            // TypestateError needs to include reference to unexpected method
            case TYPESTATE:
                if (!message.toLowerCase().matches(".*method \"\\w+\".*")) {
                    throw new MalformedInputException("Message format does not match the given error type:" +
                            "\n\tMessage: " + StringEscapeUtils.escapeJava(message) + "\n\tError type: " + errorType);
                }
                break;
            // ForbiddenMethodError needs to include reference to forbidden method (we don't check given method signature)
            case FORBIDDEN_METHOD:
                if (!message.toLowerCase().matches(".*forbidden method <.*>.*")) {
                    throw new MalformedInputException("Message format does not match the given error type:" +
                            "\n\tMessage: " + StringEscapeUtils.escapeJava(message) + "\n\tError type: " + errorType);
                }
                break;
        }
        return message;
    }

    /**
     * Some editing has to be done to the error message from CogniCrypt
     *
     * @param type         setting that determines the message length and content
     * @return
     */
    public String getEditedMessage(String type) {
        switch (type) {
            case "Original":
                return messages[0];
            case "Shortened":
                return messages[1];
            case "Summary":
                return messages[2];
            default:
                LOGGER.warn("Unknown message setting: {}", type);
                return messages[0];
        }
    }

    private String editOriginalMessage(String message) throws IOException, CentralSootUp.NoCentralSootUpInstanceException, MalformedInputException {
        // shorten text
        message = message.replaceFirst("is violated due to the following reason:(\n\\|- )?", "was violated because: ");

        // if the error is a ConstraintError with an implication only include the error message for the right side
        if (errorType == CCErrorType.CONSTRAINT && message.contains("=>")) {
            int colon = message.indexOf(":");
            int i = message.lastIndexOf("The right side \"") + "The right side \"".length();
            String rightSide = message.substring(i, message.indexOf("\"", i + 1));
            String eval = message.substring(message.lastIndexOf("|-") + 3);
            message = message.substring(0, colon + 2) + getConstraintMessage(rightSide, eval);
        }

        // remove superfluous location info -> assumes that it's always at the end of the message
        if (message.contains("(extracted @")) {
            int index = message.indexOf("(extracted @");
            message = message.substring(0, index - 1);
        }

        message = collapseMethodCallLists(message);

        message = shortenFullyQualifiedNames(message);

        // second part of ImpreciseValueExtractionError message contains position info that's unnecessary in our case
        if (errorType == CCErrorType.IMPRECISE_VALUE_EXTRACTION) {
            message = message.substring(0, message.indexOf(":\n|-"));
        }

        String code = codeSnippet.getCodeSnippet();
        int[] index = codeSnippet.getMethodBounds().getSingleLineIndex();

        if (index[0] != -1) {
            message = replaceJimpleReferences(message, code, index);

            // ImpreciseValueExtractionErrors don't have a parameter reference -> add that
            if (errorType == CCErrorType.IMPRECISE_VALUE_EXTRACTION) {
                message = addParameterReference(message);
            }

            // ForbiddenMethodError mention the called method in a weird format
            if (errorType == CCErrorType.FORBIDDEN_METHOD) {
                String methodCall = message.substring(message.indexOf(" ", message.indexOf("<")) + 1, message.indexOf(">"));
                message = message.substring(0, message.indexOf("<")) + "\"" + methodCall + "\"" + message.substring(message.indexOf(">") + 1);
            }

            if (errorType == CCErrorType.TYPESTATE) {
                message = "Wrong typestate: " + message;
            }
        }

        if (errorType == CCErrorType.ALTERNATIVE_REQ_PREDICATE) {
            message = getIndexAsString(parameterIndex) + " was not properly generated as "
                    + String.join(" OR ", getViolatedPredicates());
        }
        return message;
    }

    /**
     * Remove fully formulated constraints
     * @param message
     * @return
     */
    private String shortenConstraint(String message) {
        // remove fully formulated constraints (should only occur in these two error types)
        if (errorType == CCErrorType.IMPRECISE_VALUE_EXTRACTION || errorType == CCErrorType.CONSTRAINT) {
            int firstQuote = message.indexOf("\"");
            violatedConstraint = message.substring(firstQuote + 1, message.indexOf("\"", firstQuote + 1));
            message = message.substring(0, firstQuote) + message.substring(message.indexOf("\"", firstQuote + 1) + 2);
        }
        return message;
    }

    /**
     * Some error messages contain references to Jimple objects/parameters.
     * This method replaces these references with their Java counterparts.
     *
     * @param message
     * @return Modified error message
     */
    private String replaceJimpleReferences(String message, String code, int[] index) throws IOException, CentralSootUp.NoCentralSootUpInstanceException, MalformedInputException {
        Pattern parameterPattern = Pattern.compile("((\\$?stack\\d+)|(varReplacer\\d+)|(\\w+))");
        Pattern jimpleVarPattern = Pattern.compile("((\\$?stack\\d+)|(varReplacer\\d+))");

        // Some error messages contain a reference to the object
        if (message.contains("on object") && !message.contains("on object of type")) {
            message = replaceObjectReference(message, jimpleVarPattern);
        }

        // Some messages (for notHardCoded and neverTypeOf) reference the specific (jimple) method call with parameters to
        // point to the one that shouldn't be hardcoded
        if (message.contains("parameter @")) {
            String methodCall = getMethodCall(code, index);
            message = message.replaceAll(
                    "@ " + parameterPattern + "\\.((<init>)|\\w+)\\((" + parameterPattern + "(," + parameterPattern + ")*)?\\)",
                    "of " + methodCall);
        }

        // parameter is sometimes explicitly named -> replace jimple reference
        if (message.contains("parameter \"") || message.contains("parameter  \"")) {
            message = replaceParameterReference(message, parameterPattern);
        }

        // Some messages reference the constructor method but use "<init>" instead -> replace this
        message = message.replaceAll("<init>", violatedRule);
        return message;
    }

    /**
     * Some messages contain references to the jimple parameters which need to be replaced.
     * @param message message to edit
     * @param parameterPattern
     * @return edited message
     */
    private String replaceParameterReference(String message, Pattern parameterPattern) throws IOException, CentralSootUp.NoCentralSootUpInstanceException, MalformedInputException {
        // for set constraints
        if (message.contains("should be any of")) {
            LOGGER.trace("Replacing parameter reference for set constraint");
            message = message.replaceFirst("parameter \""+ parameterPattern +"\" with value ((\\d+L?)|(\"[^\"]*\"))",
                    "parameter '" + codeSnippet.getParameters().get(parameterIndex).getValue() + "'");
        }
        // for comparison constraints (if a comparison has multiple parameters there will be multiple evaluations)
        else if (message.contains("Extracted the following violating values for parameter  \"")) {
            LOGGER.trace("Replacing parameter reference for comparison constraint");
            StringBuilder newMessage = new StringBuilder(message.substring(0, message.indexOf(":") + 1));

            Set<String> methodReferences = new HashSet<>();

            Pattern pattern = Pattern.compile("@" + parameterPattern + "\\.((<init>)|\\w+)\\((" + parameterPattern + "(," + parameterPattern + ")*)?\\)");
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                String s = matcher.group();
                methodReferences.add(s.substring(1)); // skip the @
                message = message.substring(0, matcher.start()) + codeSnippet.getMethodName() + message.substring(matcher.end() + 1);
                matcher.reset(message);
            }

            // start with first method call "In "methodCall" " and start adding parameter assignments as you go over the parameters.
            // for next method add " and in "methodCall" " instead

            boolean first = true;
            for (String ref : methodReferences) {
                if (first) {
                    first = false;
                    newMessage.append(" In ");
                } else {
                    newMessage.append(" and in ");
                }

                CodeSnippet cs;
                // if the method reference doesn't match the codeSnippet this current issue is based on, get that snippet instead
                // Note: Currently the CrySL rules never make comparisons between values from different method calls
                if (ref.equals(codeSnippet.getJimpleStatement())) {
                    cs = codeSnippet;
                } else {
                    // here we are searching messages[0] (the original message) and not the current one because we need
                    // the fully qualified class name and that has been removed from the current one already
                    int start = messages[0].indexOf(ref);
                    int lineFrom = messages[0].indexOf("@ line ", start) + "@ line ".length();
                    int line = Integer.parseInt(messages[0].substring(lineFrom, messages[0].indexOf(" ", lineFrom)));
                    int classFrom = messages[0].indexOf("in class ") + "in class ".length();
                    String className = messages[0].substring(classFrom, messages[0].indexOf(" ", classFrom));
                    cs = new CodeSnippet(line, className, ref);
                }

                List<Parameter> parameters = cs.getParameters();

                newMessage.append("'").append(cs.getMethodName()).append("(");

                boolean firstFound = true;
                StringBuilder parameterString = new StringBuilder();
                // if the message contains a parameter's jimpleVar then it should go in the message
                // TODO: for some constraints not all relevant parameters are mentioned, e.g.: GCMParameterSpec "length[src] >= offset + len"
                for (Parameter p : parameters) {
                    String search = "\"" + p.getJimpleVar() + "\"";
                    if (message.contains(search)) {
                        newMessage.append(p.getValue());

                        int i = message.indexOf(" = ", message.indexOf(search)) + " = ".length();
                        String value = message.substring(i, message.indexOf(" ", i)); // TODO: what about new byte[]?

                        if (firstFound) {
                            firstFound = false;
                            parameterString.append(" the ");
                        } else {
                            parameterString.append(" and the ");
                        }
                        parameterString.append(getIndexAsString(p.getIndex()).toLowerCase())
                                .append(" evaluates to ")
                                .append((value.contains("$stack") || value.contains("varReplacer")) ? p.getValue() : value);
                        additionalParameters.add(p);
                    } else {
                        newMessage.append("{param").append((p.getIndex() + 1)).append("}");
                    }

                    if (p.getIndex() < parameters.size() - 1) {
                        newMessage.append(", ");
                    }
                }

                newMessage.append(")'").append(parameterString);
            }

            if (!additionalParameters.isEmpty()) {
                // remove the parameter that will serve as the main report location for this issue
                additionalParameters.remove(codeSnippet.getParameters().get(parameterIndex));
            }

            if (additionalParameters.isEmpty()) {
                // this means that the only parameter is the main parameter, and it was compared to a literal
                // -> add this to the message
                String[] parts = violatedConstraint.split(" ");
                if (parts.length >= 3) {
                    newMessage.append(parts[0].startsWith("length[")
                            ? ". The length should be"
                            : ". It should be");
                    newMessage.append(switch(parts[1]) { // extract the relation
                        case ">" -> " greater than ";
                        case ">=" -> " greater or equal to ";
                        case "<" -> " lesser than ";
                        case "<=" -> " lesser or equal to ";
                        default -> " ";
                    });

                    for (int i = 2; i < parts.length; i++) {
                        newMessage.append(parts[i]); // append the value
                    }
                }
            } else {
                newMessage.insert("Constraint ".length(), "on relation between parameters ");
            }

            message = newMessage.toString();
        }
        return message;
    }

    /**
     * Some messages contain references to the jimple object which need to be replaced.
     * @param message message to edit
     * @param jimpleVarPattern
     * @return edited message
     */
    private String replaceObjectReference(String message, Pattern jimpleVarPattern) {
        int objectLoc = message.indexOf("on object ") + "on object ".length();
        String currentobj = message.substring(objectLoc, message.indexOf(" ", objectLoc));

        String objectName = codeSnippet.getObject();
        if (currentobj.matches("\\w+") && !currentobj.matches("(\\$?stack\\d+)|(varReplacer\\d+)")) {
            // add quotes for object names already added by CC for unity
            message = message.substring(0, objectLoc) + "'" + currentobj + "'" + message.substring(objectLoc + currentobj.length());
        } else if (objectName != null) {
            // if the object was already found in the code snippet
            message = message.replaceFirst("on object " + jimpleVarPattern, "on object '" + objectName + "'");
        }
        return message;
    }

    private String getMethodCall(String code, int[] index) {
        StringBuilder methodCall = new StringBuilder("'" + code.substring(code.lastIndexOf(" ", index[0]), code.indexOf("(", index[0])).trim() + "(");
        for (int i = 0; i < codeSnippet.getParameters().size(); i++) {
            if (i > 0) {
                methodCall.append(", ");
            }

            if (i != parameterIndex) {
                methodCall.append("{param").append(i + 1).append("}");
            } else {
                methodCall.append(codeSnippet.getParameters().get(parameterIndex).getValue());
            }
        }
        methodCall.append(")'");
        return methodCall.toString();
    }

    private String addParameterReference(String message) {
        if (message.contains("\"")) {
            int start = message.lastIndexOf("\"");
            message = message.substring(0, start + 1) + " on " + getIndexAsString(parameterIndex).toLowerCase() + message.substring(start + 1);
        } else {
            int start = message.indexOf("constraint") + "constraint".length();
            message = message.substring(0, start) + " on " + getIndexAsString(parameterIndex).toLowerCase() + message.substring(start);
        }
        return message;
    }

    private String getConstraintMessage(String constraint, String eval) {
        String code = codeSnippet.getCodeSnippet();
        int[] index = codeSnippet.getMethodBounds().getSingleLineIndex();
        String parameterIndexStr = getIndexAsString(parameterIndex);

        if (constraint.contains(" in {")) {
            // set constraint
            String values = constraint.substring(constraint.indexOf("{"));

            String res = parameterIndexStr;
            if (constraint.matches("alg\\(transformation\\) in \\{.*}")) {
                res = "Algorithm of " + res.toLowerCase();
            } else if (constraint.matches("mode\\(transformation\\) in \\{.*}")) {
                res = "Mode of " + res.toLowerCase();
            } else if (constraint.matches("pad\\(transformation\\) in \\{.*}")) {
                res = "Padding of " + res.toLowerCase();
            }

            return res + " '" +  codeSnippet.getParameters().get(parameterIndex).getValue()
                    + "' should be any of " + values;
        } else if (constraint.contains("==") || constraint.contains("<=") || constraint.contains(">=")
                || constraint.contains("<") || constraint.contains(">") || constraint.contains("!=")) {
            // comparison constraints -> proper formatting will happen later in replaceJimpleReferences
            return eval;
        } else if (constraint.contains("[")) {
            if (constraint.startsWith("notHardCoded")) {
                String methodCall = getMethodCall(code, index);

                return parameterIndexStr + "of " + methodCall + " should never be hard coded";
            } else if (constraint.startsWith("neverTypeOf")) {
                String methodCall = getMethodCall(code, index);
                String type = constraint.split(" ")[1];
                type = type.substring(0, type.length() - 1);

                return parameterIndexStr + "of " + methodCall + " should never be of type " + type;
            } else if (constraint.startsWith("noCallTo")) {
                String list = collapseMethodCallLists(constraint);
                return "Call to " + list.substring(list.indexOf("[")) + " not allowed";
            } else if (constraint.startsWith("callTo")) {
                String list = collapseMethodCallLists(constraint);
                return "Call to one of the methods " + list.substring(list.indexOf("[")) + " is missing";
            } else if (constraint.startsWith("instanceOf")) {
                // currently this doesn't occur on a right side or even alone
                return eval;
            }
        }
        return constraint;
    }

    private String collapseMethodCallLists(String message) {
        // collapse method call lists
        switch (errorType) {
            case IMPRECISE_VALUE_EXTRACTION, CONSTRAINT:
                // Constraints can have method lists in noCallTo[] and CallTo[]
                if (message.contains("noCallTo") || message.contains("callTo")) {
                    int start = message.indexOf("To[") + 3;
                    int end = message.indexOf("]", start);

                    while (start != 2) { // not found = -1 -> + 3 = 2
                        List<String> list = extractMethodNames(message.substring(start, end));
                        message = message.substring(0, start - 1) + list + message.substring(end + 1);

                        start = message.indexOf("To[", start) + 3;
                        end = message.indexOf("]", start);
                    }
                }
                break;
            case FORBIDDEN_METHOD:
                if (!message.contains("{")) {
                    break;
                }
            case INCOMPLETE_OPERATION, TYPESTATE:
                int start = message.indexOf("{") + 1;
                int end = message.indexOf("}");
                List<String> list = extractMethodNames(message.substring(start, end));
                message = message.substring(0, start - 1) + list + message.substring(end + 1);
                break;
        }
        return message;
    }

    private String shortenFullyQualifiedNames(String message) {
        // no fully qualified names, only class names: javax.crypto.Cipher -> Cipher
        Pattern pattern = Pattern.compile("\\b([a-z]+\\.)+[A-Z]");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String s = matcher.group();
            message = message.substring(0, matcher.start()) + s.charAt(s.length() - 1) + message.substring(matcher.end());
            matcher.reset(message);
        }
        return message;
    }

    /**
     * Replace longer messages with summaries
     *
     * @return Modified error message
     */
    private String summarizeOriginalMessage() {
        String message = this.messages[0];
        switch (errorType) {
            // RequiredPredicateError and AlternativeReqPredicateError are already short enough and don't contain jimple references
            case ALTERNATIVE_REQ_PREDICATE, REQUIRED_PREDICATE:
                break;
            // ImpreciseValueExtractionError includes constraints that could be long
            case IMPRECISE_VALUE_EXTRACTION:
                message = "Constraint on " + getIndexAsString(parameterIndex).toLowerCase() + " could not be evaluated";
                break;
            case INCOMPLETE_OPERATION:
                message = "Missing required operation(s) for type " + violatedRule + ": " + extractMethodNames(message.substring(message.indexOf("{") + 1, message.indexOf("}")));
                break;
            case TYPESTATE:
                message = "Unexpected order of required operations";
                break;
            case FORBIDDEN_METHOD:
                message = "Forbidden method called";
                break;
            case CONSTRAINT:
                message = "Constraint violated in " + getIndexAsString(parameterIndex).toLowerCase();
                break;
        }

        return message;
    }

    /**
     * Collect the distinct method names from the given list string
     *
     * @param list String of comma-separated method calls
     * @return List object with the method names
     */
    private List<String> extractMethodNames(String list) {
        if (!list.isBlank() && expectedMethods == null) {
            String[] methods = list.split("\\), ");

            expectedMethods = new ArrayList<>();
            for (String m : methods) {
                m = m.substring(0, m.indexOf("("));
                if (!expectedMethods.contains(m)) {
                    expectedMethods.add(m);
                }
            }
            LOGGER.debug("Expected methods: {}", expectedMethods);
            return expectedMethods;
        } else {
            return new ArrayList<>();
        }
    }

    private int getStringAsIndex(String index) {
        if (index.length() <= 10) return -10;
        index = index.substring(0, index.indexOf(" ", 10));
        switch (index) {
            case "Return value":
                return -1;
            case "First parameter":
                return 0;
            case "Second parameter":
                return 1;
            case "Third parameter":
                return 2;
            case "Fourth parameter":
                return 3;
            case "Fifth parameter":
                return 4;
            case "Sixth parameter":
                return 5;
            default:
                if (!index.contains("th")) {
                    return -10;
                } else {
                    return Integer.parseInt(index.substring(0, index.indexOf("th"))) - 1;
                }
        }
    }

    private String getIndexAsString(int index) {
        switch (index) {
            case -1:
                return "Return value";
            case 0:
                return "First parameter";
            case 1:
                return "Second parameter";
            case 2:
                return "Third parameter";
            case 3:
                return "Fourth parameter";
            case 4:
                return "Fifth parameter";
            case 5:
                return "Sixth parameter";
            default:
                return (index + 1) + "th parameter";
        }
    }

    /**
     * Some error messages do not contain the location of the violating parameter
     *
     * @param violatedRule
     * @return <ul>
     *     <li>The parameter index >= 0 if there was a violating parameter found</li>
     *     <li>-5 in case of a noCallTo</li>
     *     <li>-10 if nothing helpful could be found</li></ul>
     * @throws IOException
     */
    private int extractParameterLocationFromConstraint(String violatedRule) throws IOException {
        // get generalized parameter name from constraint from message
        String constraint = violatedConstraint;

        // if the constraint is an implication then the problem is only with the right side
        if (constraint.contains("=>")) {
            String left = constraint.substring(0, constraint.indexOf("=>"));
            constraint = constraint.substring(constraint.indexOf("=>") + 3);

            // if the right side is a callTo constraint we have nothing to mark as the call is obviously missing.
            // Instead, we want to mark the cause
            if (constraint.startsWith("callTo")) {
                constraint = left;
            }
        }

        return matchCrySLEvent(violatedRule, extractParameters(constraint));
    }

    private List<String> extractParameters(String constraint) {
        List<String> parameters = new ArrayList<>();

        String[] indivConstraints = constraint.split(" (&&)|(\\|\\|) ");

        for (String c : indivConstraints) {
            String parameter;
            // this gets the first parameter mentioned in the current constraint
            // constraints such as neverTypeOf[..], notHardcoded[..], instanceOf[..]
            if (c.contains("[")) {
                int start = c.indexOf("[") + 1;
                int end;
                if (c.startsWith("callTo") || c.startsWith("noCallTo")) {
                    // these two contain method references not parameter references
                    break;
                } else if (c.contains("notHardCoded") || c.startsWith("length")) {
                    end = c.indexOf("]");
                } else {
                    // neverTypeOf and instanceOf (which are the only ones left) have the parameter reference as the
                    // first of two parameters
                    end = c.indexOf(",");
                }
                parameter = c.substring(start, end);
            } else {
                parameter = c.split(" ")[0];
            }
            parameter = parameter.matches("((mode)|(alg)|(pad))\\(transformation\\)") ? "transformation" : parameter;
            parameters.add(parameter);

            // comparison constraints often have more than one parameter reference
            // having previously removed implications this should only hold true for comparison constraints
            if (c.contains(">") || c.contains("<") || c.contains("=")) {
                String[] parts = c.split(" ([<>]=?)|(==)|[+-] ");

                for (String p : Arrays.asList(parts).subList(1, parts.length)) {
                    p = p.trim();
                    // check if it's a number (BigInteger constraints like "p > 1^2048" show up as "p > 1")
                    if (p.matches("\\d+")) {
                        break;
                    }

                    // check if it's the length -> only take the part within the brackets
                    if (p.startsWith("length[")) {
                        p = p.substring(p.indexOf("[") + 1, p.length() - 1);
                    }

                    // all that remains should be the parameter name
                    parameters.add(p);
                }
            }
        }

        return parameters;
    }

    private int matchCrySLEvent(String violatedRule, List<String> parameters) throws IOException {
        CrySLParser parser = new CrySLParser();
        try {
            CrySLRule rule = parser.parseRuleFromFile(new File(ResourceService.getInstance().getCrySLRules(), violatedRule + ".crysl"));
            String method = codeSnippet.getMethodName();
            // identify event that uses this parameter
            // if the method is the constructor -> remove the "new " first
            method = method.startsWith("new ") ? method.substring(4) : method;
            for (CrySLMethod event : rule.getEvents()) {
                if (event.getShortMethodName().equals(method) && event.getParameters().size() == codeSnippet.getParameters().size()) {
                    for (int i = 0; i < event.getParameters().size(); i++) {
                        if (matchesOneOf(event.getParameters().get(i).getKey(), parameters)) {
                            // return parameter index under the assumption that parameter names
                            // don't repeat across different events
                            return i;
                        }
                    }
                }
            }
        } catch (CrySLParserException e) {
            throw new RuntimeException(e);
        }
        return -10;
    }

    private boolean matchesOneOf(String str, List<String> compareTo) {
        for (String comparison : compareTo) {
            if (str.equals(comparison)) {
                return true;
            }
        }

        return false;
    }

    public String getViolatedConstraint() {
        return violatedConstraint;
    }

    public List<String> getExpectedMethods() {
        return expectedMethods;
    }

    public List<String> getViolatedPredicates() {
        if (violatedPredicates == null) {
            String predicateString = messages[0].replaceFirst(getIndexAsString(parameterIndex)
                    + "( parameter)? was not properly generated as ", "");
            if (errorType == CCErrorType.REQUIRED_PREDICATE) {
                violatedPredicates = List.of(predicateString);
            } else if (errorType == CCErrorType.ALTERNATIVE_REQ_PREDICATE) {
                String[] predicates = predicateString.split(" OR ");
                violatedPredicates = new ArrayList<>(List.of(predicates));
                violatedPredicates.sort(null);
            }
        }

        return violatedPredicates;
    }

    public List<Parameter> getAdditionalParameters() {
        return additionalParameters.isEmpty() ? null : additionalParameters;
    }
}
