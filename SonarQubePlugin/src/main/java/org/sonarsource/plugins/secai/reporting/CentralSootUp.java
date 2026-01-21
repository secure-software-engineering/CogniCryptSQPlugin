package org.sonarsource.plugins.secai.reporting;

import static guru.nidi.graphviz.attribute.Attributes.attr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import guru.nidi.graphviz.parse.Parser;
import sootup.codepropertygraph.ast.AstCreator;
import sootup.codepropertygraph.cdg.CdgCreator;
import sootup.codepropertygraph.cfg.CfgCreator;
import sootup.codepropertygraph.cpg.CpgCreator;
import sootup.codepropertygraph.ddg.DdgCreator;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.codepropertygraph.propertygraph.util.PropertyGraphToDotConverter;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.Type;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import guru.nidi.graphviz.model.*;

import java.util.*;
import java.util.stream.Stream;

public class CentralSootUp {

    private static CentralSootUp INSTANCE = null;

    private Logger LOGGER = LoggerFactory.getLogger(CentralSootUp.class);

    private Map<String, JavaSootClass> classes = new HashMap<>();
    private String baseDir;

    /**
     * Some source files contain multiple classes side-by-side instead of having a subclass structure.
     * In those cases we store actual relative source path, after finding it.
     */
    private Map<String, String> paths = new HashMap<>();

    private Map<ImmutablePair<MethodSignature, Stmt>, String> cpgs = new HashMap<>();

    private CentralSootUp(String jarPath) {
        AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(jarPath);
        JavaView view = new JavaView(inputLocation);

        for (JavaSootClass javaSootClass : view.getClasses().collect(Collectors.toList())) {
            classes.put(javaSootClass.getName(), javaSootClass);
        }
    }

    public static CentralSootUp getInstance() throws NoCentralSootUpInstanceException {
        if (INSTANCE == null) {
            throw new NoCentralSootUpInstanceException();
        }

        return INSTANCE;
    }

    public static CentralSootUp newInstance(String jarPath, String baseDir) {
        INSTANCE = new CentralSootUp(jarPath);
        INSTANCE.baseDir = baseDir;
        return INSTANCE;
    }

    public Map<String, JavaSootClass> getSootClasses() {
        return classes;
    }

    public JavaSootClass getSootClass(String name) {
        return classes.get(name);
    }

    /**
     *
     * @param methodSignature MethodSignature of the requested method
     * @param sootClass
     * @return
     */
    public JavaSootMethod getSootMethod(MethodSignature methodSignature, JavaSootClass sootClass) {
        if (methodSignature == null || sootClass == null) return null; // Defensive fix for malformed input

        return sootClass.getMethod(methodSignature.getSubSignature()).orElse(null);
    }

    public JavaSootMethod getSootMethod(MethodSignature methodSignature) {
        if (methodSignature == null) return null;

        return getSootMethod(methodSignature, classes.get(methodSignature.getDeclClassType().getFullyQualifiedName()));
    }

    /**
     *
     * @param method String "class: returnType method(param1,param2)" split by " ", the parameters are only the types
     * @param sootClass
     * @return
     */
    public JavaSootMethod getSootMethod(String[] method, JavaSootClass sootClass) {
        if (method.length != 3 || sootClass == null) return null; // Defensive fix for malformed input
        String returnType = method[1];
        String methodName = method[2].substring(0, method[2].indexOf("("));
        String[] methodParameters = method[2].substring(method[2].indexOf("(") + 1, method[2].indexOf(")")).split(",");
        List<String> parameterTypes = methodParameters[0].isBlank() ? List.of() : List.of(methodParameters);

        JavaSootMethod javaSootMethod = null;
        Set<JavaSootMethod> possibleMethods = sootClass.getMethodsByName(methodName);
        for (JavaSootMethod sootMethod : possibleMethods) {
            if (sootMethod.getReturnType().toString().equals(returnType)
                    && compareParameterTypes(sootMethod.getParameterTypes(), parameterTypes)) {
                javaSootMethod = sootMethod;
            }
        }
        return javaSootMethod;
    }

    public JavaSootMethod getSootMethod(String[] method) {
        return getSootMethod(method, classes.get(method[0].substring(0, method[0].length() - 1)));
    }

    public String getCPG(MethodSignature methodSignature, Stmt violatingLine) throws Exception {
        ImmutablePair<MethodSignature, Stmt> key = ImmutablePair.of(methodSignature, violatingLine);

        if (!cpgs.containsKey(key)) {
            String dotGraph = null;

            AstCreator astCreator = new AstCreator();
            CfgCreator cfgCreator = new CfgCreator();
            CdgCreator cdgCreator = new CdgCreator();
            DdgCreator ddgCreator = new DdgCreator();

            CpgCreator cpgCreator = new CpgCreator(astCreator, cfgCreator, cdgCreator, ddgCreator);

            //Getting for key mapped Soot Method
            JavaSootMethod method = CentralSootUp.getInstance().getSootMethod(methodSignature);

            PropertyGraph cpg = cpgCreator.createCpg(method);

            dotGraph = new PropertyGraphToDotConverter().convert(cpg);

            MutableGraph graph = new Parser().read(dotGraph);

            //TODO: unknown why Error occurs attr unknown, although attr is set
            for (MutableNode node : graph.nodes()) {
                String rawLabel = node.get("label").toString();
                String label = StringEscapeUtils.unescapeHtml4(rawLabel);

                node.add(attr("violation", Objects.equals(label, violatingLine.toString()) ? 1 : 0));

                //Adding node type from cpg, as cpg cant create attribute "type" on its own
                for (PropertyGraphNode pnode : cpg.getNodes()) {
                    if (label.equals(pnode.toString()) || rawLabel.equals(pnode.toString())) {
                        node.add(attr("type", pnode.getClass().getSimpleName()));
                        break;
                    }
                }


            }

            dotGraph = Graphviz.fromGraph(graph).render(Format.DOT).toString();

            cpgs.put(key, dotGraph);
        }

        return cpgs.get(key);
    }

    /**
     * Find the SootUp Stmt that relates to the given CodeSnippet in the given JavaSootMethod and uses the specified parameter
     * @param codeSnippet
     * @param sootMethod
     * @param parameter
     * @return
     */
    public Stmt getRelevantStmt(CodeSnippet codeSnippet, JavaSootMethod sootMethod, Parameter parameter) throws MalformedInputException {
        LOGGER.debug("Looking for SootUp statement of '{}' with parameter {}", codeSnippet.getJimpleStatement(), parameter);
        int line = codeSnippet.getStartLine();
        int endLine = codeSnippet.getMethodBounds().getEnd()[0];
        String methodName = codeSnippet.getMethodName().startsWith("new ")
                ? "<init>"
                : codeSnippet.getMethodName();

        // varReplacerX -> literal? Have seen String literal and int literal -> can just take parameter.getValue()
        // e.g.:
        //  CogniCrypt: signature = getInstance(varReplacer11)
        //  SootUp: signature = staticinvoke <java.security.Signature: java.security.Signature getInstance(java.lang.String)>("SHA")
        //  Java: Signature signature = Signature.getInstance("SHA");
        String variable = parameter.getJimpleVar();
        if (variable.matches("varReplacer\\d+")) {
            variable = parameter.getValue();
        }

        for (Stmt stmt : sootMethod.getBody().getStmts()) {
            String statement = stmt.toString();
            LOGGER.trace("Current statement: {}", statement);

            // if multiple values are assigned to the same name they get numbered with "#i"
            // -> save last version of that -> since we go in order this should be the correct version (branching?)
            if (parameter.isVariable() && statement.startsWith(parameter.getValue() + "#")) {
                variable = statement.split(" :?= ")[0];
            }

            if (parameter.isVariable()) {
                if (statement.startsWith(variable + " = $") || statement.startsWith(variable + " = (")) {
                    // for array assignments and objects the later method call might use the preliminary jimple name
                    // instead of the name that was assigned at a later point
                    // -> save last string assigned to the name
                    variable = statement.split(" = ")[1];

                    // if the assigned value is an object, the right side might be "(fully.qualified.ClassName) jimpleVar"
                    // -> only take final part with the variable name
                    variable = variable.contains(" ")
                            ? variable.substring(variable.lastIndexOf(" ") + 1)
                            : variable;

                    // remove possible trailing ">"
                    variable = variable.endsWith(">") ? variable.substring(0, variable.length() - 1) : variable;
                } else if (statement.endsWith(variable)) {
                    // objects might be assigned to the actual parameter name and then later
                    // reassigned to a pointer "#lX" (first if will have saved the jimple object name)
                    variable = statement.split(" = ")[0];
                }
            }

            // if the line is within the bounds of the statement, and it contains both the parameter and the method we
            // are looking for, we found the statement containing the error line
            if (stmt.getPositionInfo().getStmtPosition().getFirstLine() >= line
                    && stmt.getPositionInfo().getStmtPosition().getLastLine() <= endLine + 1 // jimple positions have nextLine:-1 as lastLine:lastCol
                    && (containsParameterVersion(statement, parameter.getJimpleVar())
                        || containsParameterVersion(statement, variable))
                    && statement.contains(" " + methodName + "(")) {
                // don't want to save #lX pointers
                if (!variable.startsWith("#")) {
                    parameter.addJimpleVar(variable);
                }
                return stmt;
            }
        }

        // sometimes a variable is defined in Java but only an immediate String literal in jimple
        // e.g.:
        //  CogniCrypt: c = getInstance(alg)
        //  SootUp: c = staticinvoke <javax.crypto.Cipher: javax.crypto.Cipher getInstance(java.lang.String)>("AES")
        //  Java: String alg = "AES"; Cipher c = Cipher.getInstance(alg);
        //  -> try to find stmt without using the parameter
        return getRelevantStmt(codeSnippet, sootMethod);
    }

    private boolean containsParameterVersion(String statement, String parameter) {
        boolean matchStart = statement.contains("(" + parameter) || statement.contains(", " + parameter);
        boolean matchEnd = statement.contains(parameter + ")") || statement.contains(parameter + ", ");
        return matchStart && matchEnd;
    }

    /**
     * Find the SootUp Stmt that relates to the given CodeSnippet in the given JavaSootMethod.
     * The CodeSnippet object must have a jimple statement
     * @param codeSnippet
     * @param sootMethod
     * @return
     */
    public Stmt getRelevantStmt(CodeSnippet codeSnippet, JavaSootMethod sootMethod) throws MalformedInputException {
        LOGGER.debug("Looking for SootUp statement of '{}' without a specified parameter", codeSnippet.getJimpleStatement());

        String jimpleStatement = codeSnippet.getJimpleStatement();
        if (jimpleStatement == null)
            throw new MalformedInputException("The CodeSnippet object must have a jimple statement");

        int line = codeSnippet.getStartLine();
        int endLine = codeSnippet.getMethodBounds().getEnd()[0];
        String methodName = codeSnippet.getMethodName().startsWith("new ")
                ? "<init>"
                : codeSnippet.getMethodName();

        // What the given Jimple statement thinks the object is
        String jimpleObject = jimpleStatement.contains(" ")     // TODO: handle actual Jimple statements
                ? jimpleStatement.substring(0, jimpleStatement.indexOf(" "))
                : jimpleStatement.substring(0, jimpleStatement.indexOf("."));
        String object = jimpleObject;

        // What the actual object in the Java code is (can be the same as the Jimple one)
        String javaObject = codeSnippet.getObject();

        // Numbered objects, like "cipher1", "cipher2", are instead numbered "cipher#0", "cipher#1" in Jimple
        String trimmedObject = javaObject.matches(".+\\d+")
                ? javaObject.replaceFirst("\\d+$", "")
                : javaObject;

        for (Stmt stmt : sootMethod.getBody().getStmts()) {
            String statement = stmt.toString();
            LOGGER.trace("Current statement: {}", statement);

            if (statement.endsWith(object)) {
                // if the object was assigned to a different variable/pointer,
                // that new variable will most likely be the one used in the statement
                object = statement.split(" = ")[0];
            } else if (statement.startsWith(object + " = $") || statement.startsWith(object + " = (")) {
                object = statement.split(" = ")[1];

                // depending on what was assigned, cut down to last section
                object = object.contains(" ")
                        ? object.substring(object.lastIndexOf(" ") + 1).replace(">", "")
                        : object;
            }

            // if the line is within the bounds of the statement, starts with the object and contains the method we
            // are looking for, we found the statement containing the error line
            if (stmt.getPositionInfo().getStmtPosition().getFirstLine() >= line
                    && stmt.getPositionInfo().getStmtPosition().getLastLine() <= endLine + 1 // jimple positions have nextLine:-1 as lastLine:lastCol
                    && (operatesOnObject(statement, object) || operatesOnObject(statement, jimpleObject)
                        || operatesOnObject(statement, javaObject) || operatesOnObject(statement, trimmedObject))
                    && statement.contains(" " + methodName + "(")) {
                return stmt;
            }
        }
        return null;
    }

    /**
     * Checks if the given Jimple statement assigns a value to the given object or calls one of its methods
     * @param statement Jimple statement
     * @param object name of an object
     * @return
     */
    private boolean operatesOnObject(String statement, String object) {
        return (statement.matches("\\Q" + object + "\\E(#\\d+)? = .*")
                || statement.matches(".*virtualinvoke \\Q" + object + "\\E(#\\d+)?[.].*")
                || statement.matches(".*specialinvoke \\Q" + object + "\\E(#\\d+)?[.].*")
                || statement.matches(".*staticinvoke \\Q" + object + "\\E(#\\d+)?[.].*"));
    }

    public File getFile(String className) {
        if (checkPath(className, getPath(className))) {
            return new File(baseDir, getPath(className));
        }

        return null;
    }

    public String getPath(String className) {
        if (paths.containsKey(className)) {
            return paths.get(className);
        } else {
            String path = classes.get(className).getClassSource().getSourcePath().toString();
            path = "src/main/java" + path.replaceFirst("(\\$\\w+)?\\.class", ".java");

            if (checkPath(className, path)) {
                return paths.get(className);
            } else {
                return null;
            }
        }
    }

    /**
     * Check if the given relative path is valid or if the correct path can be found.
     * @param className
     * @param path
     * @return
     */
    private boolean checkPath(String className, String path) {
        File file = new File(baseDir, path);

        if (file.exists()) {
            paths.putIfAbsent(className, path);
            return true;
        } else {
            // If there are two classes in one source file, the path from SootUp doesn't match, as it references the class files
            // This means that we have to search all source files in the package
            JavaSootClass sootClass = classes.get(className);
            String name = sootClass.getType().getClassName();

            LOGGER.debug("Searching for actual source of class '{}' in package '{}'", // TODO: change to debug/trace
                    name, sootClass.getType().getPackageName());

            String packagePath = "src/main/java/" + sootClass.getType().getPackageName().getName().replace(".", "/");
            File dir = new File(baseDir, packagePath);

            if (dir.isDirectory()) {
                File[] files = dir.listFiles();

                for (File f : files) {
                    try (Stream<String> lines = Files.lines(Path.of(f.toURI()))){
                        boolean initsClass = lines.anyMatch(
                                l -> l.matches(".*class\\s+" + name + "\\b.*"));

                        if (initsClass) {
                            paths.putIfAbsent(className, packagePath + "/" + f.getName());
                            return true;
                        }
                    } catch (IOException e) {
                        // if there's a problem with the file it obviously isn't the one we want
                    }
                }
            }
        }

        return false;
    }

    protected boolean compareParameterTypes(List<Type> found, List<String> wanted) {
        if (found.size() != wanted.size()) return false;
        for (int i = 0; i < wanted.size(); i++) {
            if (!found.get(i).toString().equals(wanted.get(i))) return false;
        }
        return true;
    }

    public static class NoCentralSootUpInstanceException extends Exception {

        public NoCentralSootUpInstanceException() {
            super("No CentralSootUp instance created with a jar path.");
        }
    }
}
