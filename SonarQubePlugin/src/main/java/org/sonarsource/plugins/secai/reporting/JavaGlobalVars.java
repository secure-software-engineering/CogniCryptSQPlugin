package org.sonarsource.plugins.secai.reporting;

import org.apache.commons.io.FileUtils;
import sootup.java.core.JavaSootField;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JavaGlobalVars {

    private static final Map<String, JavaGlobalVars> classMap = new HashMap<>();

    private final Map<String, Location> vars = new HashMap<>();

    public static JavaGlobalVars getGlobalsForClass(String fullyQualifiedName) {
        try {
            if (!classMap.containsKey(fullyQualifiedName)) {
                classMap.put(fullyQualifiedName, new JavaGlobalVars(fullyQualifiedName));
            }

            return classMap.get(fullyQualifiedName);
        } catch (CentralSootUp.NoCentralSootUpInstanceException | IOException e) {
            return null;
        }
    }

    private JavaGlobalVars(String fullyQualifiedName)
            throws CentralSootUp.NoCentralSootUpInstanceException, IOException {
        CentralSootUp centralSootUp = CentralSootUp.getInstance();
        List<String> contents = FileUtils.readLines(centralSootUp.getFile(fullyQualifiedName), StandardCharsets.UTF_8);
        String filePath = centralSootUp.getPath(fullyQualifiedName);

        int bodyStart = 0;
        while (bodyStart < contents.size() && !contents.get(bodyStart)
                .contains("class " + fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf(".") + 1))) {
            bodyStart++;
        }

        if (bodyStart != contents.size()) {
            for (JavaSootField field : centralSootUp.getSootClass(fullyQualifiedName).getFields()) {
                String type = field.getType().toString();
                // no fully qualified names, only class names: javax.crypto.Cipher -> Cipher
                type = type.substring(type.lastIndexOf(".") + 1);
                String name = field.getName();

                int line = bodyStart;
                int counter = 0;
                boolean found = false;
                while (!found && line < contents.size()) {
                    if (contents.get(line).matches(".*\\s" + toRegExPattern(type + " " + name) + "[\\s;].*")) {
                        found = true;
                    }
                    line++;
                }

                if (found) {
                    // Because the CodeSnippet constructor being used in the addVar method, we don't need the exact location
                    // only the start line, fullyQualifiedName, and filePath must be correct
                    Location loc = new Location(line, 0,
                            line + counter + 1, -1, fullyQualifiedName, filePath);
                    addVar(loc, name);
                }
            }
        }
    }

    private void addVar(Location location, String name) throws CentralSootUp.NoCentralSootUpInstanceException, IOException {
        // create new CodeSnippet on this line
        CodeSnippet codeSnippet = new CodeSnippet(location, true);

        // the assigned value will be added as the only parameter
        if (codeSnippet.getParameters() == null) {
            // if no value was assigned, use the general bounds of the snippets
            vars.put(name, codeSnippet.getSnippetBounds());
        } else {
            vars.put(name, codeSnippet.getParameters().get(0).getLocation());
        }
    }

    /**
     * Changes strings such as "char[] variable" to "char\\s*[\\s*]\\s* variable" to
     * account for alternative spacing.
     * 
     * @param s
     * @return
     */
    private String toRegExPattern(String s) {
        s = s.replace("[", "\\\\s*\\\\[\\\\s*");
        s = s.replace("]", "\\\\s*\\\\]\\\\s*");
        s = s.replace("(", "\\\\s*\\\\(\\\\s*");
        s = s.replace(")", "\\\\s*\\\\)\\\\s*");
        s = s.replace("{", "\\\\s*\\\\{\\\\s*");
        s = s.replace("}", "\\\\s*\\\\}\\\\s*");
        s = s.replace("<", "\\\\s*<\\\\s*");
        s = s.replace(">", "\\\\s*>\\\\s*");
        s = s.replace(",", "\\\\s*,\\\\s*");
        s = s.replace(".", "\\\\s*\\\\.\\\\s*");
        s = s.replace(" ", "\\\\s+");
        return s;
    }

    public Location getVariable(String name) {
        return vars.get(name);
    }
}
