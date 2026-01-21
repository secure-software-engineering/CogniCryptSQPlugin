package org.sonarsource.plugins.secai.reporting;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.plugins.secai.utils.TimeTracker;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaIdentifierFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CodeSnippet {

    private final Logger LOGGER = LoggerFactory.getLogger(CodeSnippet.class);

    private int startLine;
    private final String filePath;
    private final String className;

    private String methodName;
    private String jimpleStatement;
    private Location methodBounds;
    private List<Parameter> parameters;
    private String object = null;

    private String codeSnippet;
    private String[] lines;
    private Location snippetBounds;

    /**
     * Fetches the lines of code involved in the given location
     * @param location
     * @throws CentralSootUp.NoCentralSootUpInstanceException
     * @throws IOException
     */
    public CodeSnippet(Location location, boolean assignment) throws CentralSootUp.NoCentralSootUpInstanceException, IOException {
        this.startLine = location.getStart()[0];
        this.filePath = location.getFilePath();
        this.className = location.getClassName();

        // we also use this constructor to more accurately locate variable assignments
        if (assignment) {
            // extract actual code snippet -> includes comment blanking
            extractCodeSnippet();

            // Some methods, e.g. the constructor of JavaGlobalVars, call this method ASSUMING that a value is assigned,
            // but we still need to check if this is actually the case
            if (codeSnippet.contains("=")) {
                // add assigned value to parameters
                parameters = new ArrayList<>();
                String value = codeSnippet.split("=\\s*")[1];
                value = value.substring(0, value.lastIndexOf(";")); // this should delete the ";" terminating the code snippet
                int start = codeSnippet.indexOf(value);
                Location l = new Location(startLine, new int[]{start, start + value.length() - 1}, codeSnippet,
                        location.getClassName(), location.getFilePath());
                parameters.add(new Parameter(l, value, 0));
            }
        } else {
            List<String> contents = FileUtils.readLines(CentralSootUp.getInstance().getFile(className), StandardCharsets.UTF_8);
            this.codeSnippet = String.join("\n", contents.subList(location.getStart()[0] - 1, location.getEnd()[0])); // TODO: blank comments
            this.lines = codeSnippet.split("\n");
        }
    }

    /**
     * Fetches the code snippet starting in the given line and the parameters used in the given method call
     * @param line starting line of the code snippet
     * @param className fully qualified class name
     * @param jimpleStatement jimple statement which includes the method call that is to be analyzed, just putting in a method name should work too
     * @param violatedRule the CrySL rule that was violated
     * @throws IOException if there were problems reading the contents of the given java file
     * @throws CentralSootUp.NoCentralSootUpInstanceException if SootUp was not set up beforehand
     */
    public CodeSnippet(int line, String className, String jimpleStatement, String violatedRule) throws IOException, CentralSootUp.NoCentralSootUpInstanceException, MalformedInputException {
        float time = System.nanoTime();

        this.startLine = line;
        this.filePath = CentralSootUp.getInstance().getPath(className);
        this.className = className;
        this.jimpleStatement = jimpleStatement;

        extractCodeSnippet();
        this.methodName = extractMethodFromJimple(jimpleStatement, codeSnippet, violatedRule);
        locateRelevantParametersAndMethod();
        addJimpleVars();
        extractObject();

        TimeTracker.addCodeSnippetTime((System.nanoTime() - time) / 1000000);
    }

    /**
     * Locates the parameters used in the given method call in the given code snippet
     * @param codeSnippet code snippet
     * @param line starting line of the code snippet
     * @param filePath path to the java file of the class
     * @param className fully qualified class name
     * @param methodName name of the method of which to locate the parameters
     */
    public CodeSnippet(String codeSnippet, int line, String filePath, String className, String methodName) throws MalformedInputException {
        float time = System.nanoTime();

        this.startLine = line;
        this.filePath = filePath;
        this.className = className;
        this.methodName = methodName;
        this.codeSnippet = codeSnippet;

        lines = codeSnippet.split("\n");
        snippetBounds = new Location(startLine, LocaterUtils.skipWhitespaces(codeSnippet, 0, false),
                startLine + lines.length - 1, lines[lines.length - 1].length() - 1, className, filePath);

        locateRelevantParametersAndMethod();
        extractObject();

        TimeTracker.addCodeSnippetTime((System.nanoTime() - time) / 1000000);
    }

    /**
     * Fetches the code snippet starting in the given line and the parameters used in the given method call
     * @param line starting line of the code snippet
     * @param className fully qualified class name
     * @param jimpleStatement jimple statement which includes the method call that is to be analyzed, just putting in a method name should work too
     * @throws IOException if there were problems reading the contents of the given java file
     * @throws CentralSootUp.NoCentralSootUpInstanceException if SootUp was not set up beforehand
     */
    public CodeSnippet(int line, String className, String jimpleStatement) throws IOException, CentralSootUp.NoCentralSootUpInstanceException, MalformedInputException {
        float time = System.nanoTime();

        this.startLine = line;
        this.filePath = CentralSootUp.getInstance().getPath(className);
        this.className = className;
        this.jimpleStatement = jimpleStatement;

        extractCodeSnippet();
        methodName = extractMethodFromJimple(jimpleStatement, codeSnippet, null);
        locateRelevantParametersAndMethod();
        addJimpleVars();
        extractObject();

        TimeTracker.addCodeSnippetTime((System.nanoTime() - time) / 1000000);
    }

    private void extractCodeSnippet() throws IOException, CentralSootUp.NoCentralSootUpInstanceException {
        List<String> contents = FileUtils.readLines(CentralSootUp.getInstance().getFile(className), StandardCharsets.UTF_8);
        ImmutablePair<String, Boolean> commentReplacement;

        commentReplacement = blankComments(contents.get(startLine - 1), false);
        codeSnippet = commentReplacement.getLeft();
        boolean openComment = commentReplacement.getRight();

        int counter = 0;
        boolean lineClosed = codeSnippet.contains(";");
        boolean openBracket = codeSnippet.lastIndexOf("{") > codeSnippet.lastIndexOf("}");
        while ((!lineClosed || openBracket || openComment) && startLine + counter < contents.size()) {
            commentReplacement = blankComments(contents.get(startLine + counter), openComment);

            openComment = commentReplacement.getRight();
            codeSnippet = codeSnippet + "\n" + commentReplacement.getLeft();
            counter++;
            openBracket = codeSnippet.lastIndexOf("{") > codeSnippet.lastIndexOf("}");
            lineClosed = codeSnippet.contains("}")
                    ? codeSnippet.lastIndexOf(";") > codeSnippet.lastIndexOf("}")
                    : codeSnippet.contains(";");
        }

        // with the new CogniCrypt version (5.0.1) errors are sometimes reported at the parameter location and
        // not the start -> search backwards if not all the brackets are closed
        while (!LocaterUtils.allBracketsClosed(codeSnippet) && startLine >= 2) {
            startLine--;
            commentReplacement = blankComments(contents.get(startLine - 1), false); // TODO: backwards open comment

            codeSnippet = commentReplacement.getLeft() + "\n" + codeSnippet;
            openComment = commentReplacement.getRight();
        }
        LOGGER.debug("Extracted snippet from lines {} to {}:\n{}", startLine, (startLine + counter), codeSnippet);

        lines = codeSnippet.split("\n");
        snippetBounds = new Location(startLine, LocaterUtils.skipWhitespaces(codeSnippet, 0, false),
                startLine + counter, lines[lines.length - 1].length() - 1, className, filePath);
    }

    private ImmutablePair<String, Boolean> blankComments(String line, boolean openComment) {
        if (line.isBlank()) {
            return ImmutablePair.of(line, openComment);
        }

        int startI = 0;
        int endI = -1;


        List<Integer> start = collectCommentStarts(line, startI);

        List<Integer> end = collectCommentEnds(line, endI);

        if (getLastOrElse(end, -1) == line.length()) {
            openComment = false;
        }

        if (start.isEmpty() && end.isEmpty()) {
            if (openComment) {
                line = " ".repeat(line.length());
            }
        } else {

            if (openComment || getFirstOrElse(end, -1) < getFirstOrElse(start, Integer.MAX_VALUE)) {
                start.add(0, 0);
            }

            if (getLastOrElse(end, -1) < getLastOrElse(start, Integer.MAX_VALUE)) {
                end.add(line.length());
                openComment = true;
            }

            line = blankBlockComments(line, end, start);

            if (end.get(end.size() - 1) < line.length()) {
                openComment = false;
            }
        }

        // if line has "//" replace from "//" until end with whitespace (not considering that it might be in a String literal)
        int index = line.indexOf("//");
        if (index != -1) {
            // 2nd if condition: no block comment started before line comment
            //      -> closed block comments will have already been removed
            //      -> newly opened block comments will have been commented out by the line comment
            openComment = false;

            line = line.substring(0, index) + " ".repeat(line.substring(index).length());
        }
        return ImmutablePair.of(line, openComment);
    }

    private List<Integer> collectCommentStarts(String line, int startI) {
        List<Integer> start = new ArrayList<>();
        while (line.indexOf("/*", startI + 2) != -1) {
            startI = line.indexOf("/*", startI + 2);
            start.add(startI);
        }
        return start;
    }

    private List<Integer> collectCommentEnds(String line, int endI) {
        List<Integer> end = new ArrayList<>();
        while (line.indexOf("*/", endI) != -1) {
            endI = line.indexOf("*/", endI) + 2; // also skip the "*/"
            end.add(endI);
        }
        return end;
    }

    private int getFirstOrElse(List<Integer> list, int alternative) {
        return list.isEmpty() ? alternative : list.get(0);
    }

    private int getLastOrElse(List<Integer> list, int alternative) {
        return list.isEmpty() ? alternative : list.get(list.size() - 1);
    }

    private String blankBlockComments(String line, List<Integer> end, List<Integer> start) {
        // while the end of a block comment will always be the end, there can be multiple starts
        int skipIndex = 0;

        for (int i = 0; i < end.size(); i++) {
            while (line.charAt(start.get(i + skipIndex)) == ' ' && start.get(i + skipIndex) != 0) {
                skipIndex++;
            }

            int startI = start.get(i + skipIndex);
            int endI = end.get(i);
            String head = startI > 0 ? line.substring(0, startI) : "";
            String tail = endI < line.length() ? line.substring(endI) : "";
            line = head + " ".repeat(line.substring(startI, endI).length()) + tail;
        }
        return line;
    }

    protected String extractMethodFromJimple(String jimpleStatement, String codeSnippet, String violatedRule) throws MalformedInputException {
        if (jimpleStatement == null || jimpleStatement.isBlank())
            throw new MalformedInputException("The given jimple statement is blank or null");

        String methodName;

        int sigStart;
        int sigEnd;
        if ((sigStart = jimpleStatement.indexOf("<")) != -1 && (sigEnd = jimpleStatement.indexOf(">")) != -1
                && (sigStart < jimpleStatement.indexOf("<init>") || !jimpleStatement.contains("<init>"))) {
            // if the given jimple statement is an actual jimple statement using a method, then it should contain a
            // parsable method signature bracketed by "<" and ">" (if these characters appear in a String literal, it will be AFTER the signature)
            // "<init>" needs to occur either after the opening tag or not at all
            MethodSignature sig = JavaIdentifierFactory.getInstance().parseMethodSignature(jimpleStatement.substring(sigStart, sigEnd + 1));
            methodName = sig.getName();
            if (methodName.equals("<init>")) {
                methodName = "new " + sig.getDeclClassType().getClassName();
            }
            return methodName;
        }

        // While CogniCrypt does return jimple-like statements, they are not properly formatted as Jimple, so we need to do some computing
        methodName = jimpleStatement;
        if (methodName.contains("<init>")) {
            int indexOfConstructor = -1;
            if (violatedRule != null) {
                // take the constructor matching the violated rule
                // can't just use the violated rule as is bc it might be an interface the correct one might be a subclass
                if (violatedRule.equals("Key") || violatedRule.equals("SecretKey")) {
                    // all known implementing classes
                    List<String> options = List.of("EncryptionKey", "KerberosKey", "SecretKeySpec");
                    int counter = 0;

                    while (indexOfConstructor == -1) {
                        indexOfConstructor = codeSnippet.indexOf("new " + options.get(counter));
                        counter++;
                    }
                }

                // if we didn't find the constructor or can just take the rule
                if (indexOfConstructor == -1) {
                    indexOfConstructor = codeSnippet.indexOf("new " + violatedRule);
                }
            }

            // without the rule we can't figure out which constructor this is -> might be multiple constructors
            // so we just take the first one (also if searching for the constructor of the violated rule returns -1)
            if (indexOfConstructor == -1) {
                indexOfConstructor = codeSnippet.indexOf("new ");
            }
            methodName = codeSnippet.substring(indexOfConstructor, codeSnippet.indexOf("(", indexOfConstructor));
        } else {
            // method calls will be on the right side of the "="
            if (methodName.contains(" = ")) {
                methodName = methodName.split(" = ")[1];
            }

            // in case right side is only a jimple reference we need to get the method from the actual code
            if (!(methodName.contains(".") || methodName.contains("("))) {
                methodName = codeSnippet.split("=")[1].trim();
                if (methodName.startsWith("new ")) {
                    methodName = methodName.substring(0, methodName.indexOf("("));
                }
            }

            // cut off the jimple object reference
            if (methodName.contains(".")) {
                methodName = methodName.split("\\.")[1];
            }

            // cut off the parameter list
            if (methodName.contains("(")) {
                methodName = methodName.substring(0, methodName.indexOf("("));
            }
        }

        // during testing the logger isn't always initialized
        if (LOGGER != null) {
            LOGGER.debug("methodName: {}", methodName);
        }
        return methodName;
    }

    private void extractObject() {
        int[] index = methodBounds.getSingleLineIndex();
        // if method is called on an object -> object.method
        if (codeSnippet.charAt(index[0] - 1) == '.') {
            object = codeSnippet.substring(codeSnippet.lastIndexOf(" ", index[0]), index[0] - 1).trim();
        } else if (codeSnippet.startsWith("new ", index[0]) && codeSnippet.substring(0, index[0]).matches(".*=\\s*")) {
            // if method is the constructor and is assigned to an object -> object = method
            object = codeSnippet.substring(0, codeSnippet.lastIndexOf("=")).trim();
            String[] parts = object.split("\\s+");
            object = parts.length > 1 ? parts[1] : parts[0];
        }

        LOGGER.debug("object name: {}", object == null ? "N/A" : object);
    }

    private void locateRelevantParametersAndMethod() throws MalformedInputException {
        int methodStart = codeSnippet.indexOf(methodName);
        if (methodStart == -1)
            throw new MalformedInputException("The given method does not occur in this code snippet");
        int parameterStart = codeSnippet.indexOf("(", methodStart);
        int parameterClose = codeSnippet.indexOf(")", methodStart);

        // get index span of where the parameters are
        while (!LocaterUtils.allBracketsClosed(codeSnippet, methodStart, parameterClose)) {
            parameterClose = codeSnippet.indexOf(")", parameterClose + 1);
        }
        String reducedCodeSnippet = codeSnippet.substring(0, parameterClose + 1);

        methodBounds = new Location(startLine, new int[]{methodStart, parameterClose}, reducedCodeSnippet,
                className, filePath);
        LOGGER.debug("methodBounds: {}", methodBounds);

        int start = LocaterUtils.skipWhitespaces(reducedCodeSnippet, parameterStart + 1, false);
        int comma = reducedCodeSnippet.indexOf(",", methodStart);
        int end;


        parameters = new ArrayList<>();
        if (comma == -1) {
            // if there is no comma then it's definitely at most one parameter
            if (!reducedCodeSnippet.substring(parameterStart + 1, parameterClose).isBlank()) {
                end = LocaterUtils.skipWhitespaces(reducedCodeSnippet, parameterClose - 1, true);
                parameters.add(new Parameter(new Location(startLine, new int[]{start, end},
                        reducedCodeSnippet, className, filePath),
                        reducedCodeSnippet.substring(start, parameterClose).trim(), parameters.size()));
                LOGGER.debug("Located parameter: {}", parameters.get(parameters.size() - 1));
            }

        } else {
            while (comma != -1 && !LocaterUtils.allBracketsClosed(reducedCodeSnippet, parameterStart, comma)) {
                if (LocaterUtils.allBracketsClosed(reducedCodeSnippet, start, comma)) {
                    end = LocaterUtils.skipWhitespaces(reducedCodeSnippet, comma - 1, true);
                    parameters.add(new Parameter(new Location(startLine, new int[]{start, end},
                            reducedCodeSnippet, className, filePath),
                            reducedCodeSnippet.substring(start, comma).trim(), parameters.size()));
                    start = LocaterUtils.skipWhitespaces(reducedCodeSnippet, comma + 1, false);
                    LOGGER.debug("Located parameter: {}", parameters.get(parameters.size() - 1));
                }

                comma = reducedCodeSnippet.indexOf(",", comma + 1);
            }

            if (LocaterUtils.allBracketsClosed(reducedCodeSnippet, start, parameterClose - 1)) {
                end = LocaterUtils.skipWhitespaces(reducedCodeSnippet, parameterClose - 1, true);
                parameters.add(new Parameter(new Location(startLine, new int[]{start, end},
                        reducedCodeSnippet, className, filePath),
                        reducedCodeSnippet.substring(start, parameterClose).trim(), parameters.size()));

                LOGGER.debug("Located parameter: {}", parameters.get(parameters.size() - 1));
            } else if (LocaterUtils.allBracketsClosed(reducedCodeSnippet, parameterStart, comma)) {
                end = LocaterUtils.skipWhitespaces(reducedCodeSnippet,
                        reducedCodeSnippet.lastIndexOf(")", comma) - 1, true);
                parameters.add(new Parameter(new Location(startLine, new int[]{start, end},
                        reducedCodeSnippet, className, filePath),
                        reducedCodeSnippet.substring(start, end).trim(), parameters.size()));

                LOGGER.debug("Located parameter: {}", parameters.get(parameters.size() - 1));
            }
        }
    }

    private void addJimpleVars() {
        if (jimpleStatement.contains("(")) {
            String jimpleStatementCopy = jimpleStatement;
            jimpleStatementCopy = jimpleStatementCopy.substring(jimpleStatementCopy.indexOf("(") + 1, jimpleStatementCopy.indexOf(")"));
            String[] jimpleParameters = jimpleStatementCopy.split(",");

            for (int i = 0; i < parameters.size(); i++) {
                parameters.get(i).addJimpleVar(jimpleParameters[i]);
            }
        }
    }

    public int getStartLine() {
        return startLine;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getJimpleStatement() {
        return jimpleStatement;
    }

    public Location getMethodBounds() {
        return methodBounds;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public String getObject() {
        return object;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public String[] getLines() {
        return lines;
    }

    /**
     * return the lines involved in a location
     * @param location
     * @return
     */
    public List<String> getLines(Location location) throws MalformedInputException {
        if (location.getStart()[0] < startLine || location.getStart()[0] >= startLine + lines.length - 1)
            throw new MalformedInputException("The given location is outside of the code snippet");
        // TODO: somehow return lines with comments not blanked
        return List.of(lines).subList(location.getStart()[0] - startLine, location.getEnd()[0] - startLine + 1);
    }

    public Location getSnippetBounds() {
        return snippetBounds;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "CodeSnippet{" +
                "codeSnippet='" + codeSnippet + '\'' +
                ", startLine=" + startLine +
                ", snippetBounds=" + snippetBounds +
                ", methodName='" + methodName + '\'' +
                ", methodBounds=" + methodBounds +
                ", jimpleStatement='" + jimpleStatement + '\'' +
                ", parameters=" + parameters +
                ", object='" + object + '\'' +
                '}';
    }
}
