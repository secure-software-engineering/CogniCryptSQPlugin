package org.sonarsource.plugins.secai.reporting.cognicrypt.flows;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CryslAliasResolver {

    public static String resolveAliases(String orderExpr, Map<String, String> aliasMap) {
        Set<String> resolving = new HashSet<>();
        return resolve(orderExpr, aliasMap, resolving);
    }

    private static String resolve(String expr, Map<String, String> aliasMap, Set<String> resolving) {
        StringBuilder resolved = new StringBuilder();

        for (int i = 0; i < expr.length(); ) {
            char ch = expr.charAt(i);

            if (Character.isJavaIdentifierStart(ch)) {
                int start = i;
                while (i < expr.length() && Character.isJavaIdentifierPart(expr.charAt(i))) i++;
                String token = expr.substring(start, i);

                if (aliasMap.containsKey(token)) {
                    if (resolving.contains(token)) {
                        throw new RuntimeException("Cyclic alias reference: " + token);
                    }
                    resolving.add(token);
                    String resolvedToken = resolve(aliasMap.get(token), aliasMap, resolving);
                    resolving.remove(token);
                    resolved.append("(").append(resolvedToken).append(")");
                } else {
                    resolved.append(token);
                }
            } else {
                resolved.append(ch);
                i++;
            }
        }

        return resolved.toString();
    }
}
