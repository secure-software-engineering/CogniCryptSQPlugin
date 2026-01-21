package org.sonarsource.plugins.secai.reporting;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.model.Position;
import sootup.java.core.JavaSootMethod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MethodSnippet {

    /**
     * The key should be formatted as "fully.qualified.ClassName: returnType methodName(p1,p2)", parameters as types, not names
     */
    private static Map<String, MethodSnippet> methods = new HashMap<>();

    private Logger LOGGER = LoggerFactory.getLogger(MethodSnippet.class);

    private String methodName;
    private String fullyQualifiedClassName;
    private String returnType;
    private JavaSootMethod sootMethod;
    private String filePath;

    private List<Parameter> parameters = new ArrayList<>();
    private Location methodBody;
    //private List<> annotations; // TODO: add this in the future
    private Location returnTypeLoc;
    private Location methodNameLoc;

    private MethodSnippet(String method, String returnType, List<String> parameters, String fullyQualifiedClassName) throws CentralSootUp.NoCentralSootUpInstanceException, IOException, MalformedInputException {
        this.methodName = method;
        this.returnType = returnType;
        this.fullyQualifiedClassName = fullyQualifiedClassName;

        this.sootMethod = CentralSootUp.getInstance().getSootMethod(new String[]{fullyQualifiedClassName + ":", returnType,
                        methodName + "(" + String.join(",", parameters) + ")"});
        this.filePath = CentralSootUp.getInstance().getPath(fullyQualifiedClassName);

        findMethodBody();
        findMethodHeader();
    }

    private void findMethodBody() {
        Position pos = sootMethod.getBody().getPosition();
        methodBody = new Location(pos.getFirstLine(), pos.getFirstCol(), pos.getLastLine(), pos.getLastCol(),
                fullyQualifiedClassName, filePath);
    }

    private void findMethodHeader() throws CentralSootUp.NoCentralSootUpInstanceException, IOException, MalformedInputException {
        if (methodBody == null) return;

        List<String> contents = FileUtils.readLines(
                CentralSootUp.getInstance().getFile(fullyQualifiedClassName), StandardCharsets.UTF_8);
        int line = methodBody.getStart()[0] - 1; // -1 bc lines in file start from 1, in list from 0

        // from the first line of the method body search back for the method name
        if (!contents.get(line).contains(methodName) || !contents.get(line).contains(returnType)) {
            line--;
        }
        String snippet = contents.get(line);

        // TODO: search for return type isn't a regex -> "char[ ]" isn't found
        boolean allFound = snippet.contains(methodName)
                && snippet.indexOf(returnType) < snippet.indexOf("(");
        while (!allFound) {
            line--;
            snippet = contents.get(line) + "\n" + snippet;
            allFound = snippet.contains(methodName)
                    && snippet.indexOf(returnType) < snippet.indexOf("(");
        }

        // increase the line by 1 again bc line numbers in files start from 1 not 0
        line++;

        // get exact location where the return type is defined
        int returnTypeIndex = snippet.indexOf(returnType);
        returnTypeLoc = new Location(line, new int[] {returnTypeIndex, returnTypeIndex + returnType.length()}, snippet,
                fullyQualifiedClassName, filePath);

        // get exact location of the method name
        int methodNameIndex = snippet.indexOf(methodName);
        methodNameLoc = new Location(line, new int[] {methodNameIndex, methodNameIndex + methodName.length()}, snippet,
                fullyQualifiedClassName, filePath);

        parameters = new CodeSnippet(snippet, line, filePath, fullyQualifiedClassName, methodName).getParameters();
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public String getReturnType() {
        return returnType;
    }

    public JavaSootMethod getSootMethod() {
        return sootMethod;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public Location getMethodBody() {
        return methodBody;
    }

    public Location getReturnTypeLoc() {
        return returnTypeLoc;
    }

    public Location getMethodNameLoc() {
        return methodNameLoc;
    }

    public static MethodSnippet get(String key) throws MalformedInputException, CentralSootUp.NoCentralSootUpInstanceException, IOException {
        if (!methods.containsKey(key)) {
            if (!correctKeyFormat(key))
                throw new MalformedInputException("The key should be formatted as " +
                        "\"fully.qualified.ClassName: returnType methodName(p1,p2)\" but was \"" + key + "\"");

            String[] parts = key.split(" ");
            methods.put(key, new MethodSnippet(
                    parts[2].substring(0, parts[2].indexOf("(")),
                    parts[1],
                    List.of(parts[2].substring(parts[2].indexOf("(") + 1, parts[2].indexOf(")")).split(",")),
                    parts[0].substring(0, parts[0].length() - 1)));
        }

        return methods.get(key);
    }

    public static MethodSnippet get(String fullyQualifiedClassName, int line) {
        // TODO: get method from line
        // TODO: if method not in -> get
        return methods.get(fullyQualifiedClassName); // placeholder to be compilable
    }

    /**
     * The key should be formatted as "fully.qualified.ClassName: returnType methodName(p1,p2)", parameters as types, not names
     */
    public static boolean correctKeyFormat(String key) {
        String[] parts = key.split(" ");

        // need 3 parts
        if (parts.length != 3) return false;

        // ":" for formating & has to be fully qualified name -> probably is if there's a dot in the name
        String className = parts[0];
        if (!(className.endsWith(":") && className.contains("."))) return false;

        String methodCall = parts[2];
        // needs to contain both parentheses -> not checking actual parameters
        if (!(methodCall.contains("(") && methodCall.contains(")"))) return false;
        // methodName must be only word characters
        if (!methodCall.substring(0, methodCall.indexOf("(")).matches("\\w+")) return false;

        // not checking returnType or contents of parameter list -> too many possible types in Java
        return true;
    }
}
