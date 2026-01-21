package org.sonarsource.plugins.secai.reporting.cognicrypt.flows;

import java.util.ArrayList;
import java.util.List;

public class CryslOrderParser {

    private String input;
    private int pos;

    public OrderNode parse(String input) {
        this.input = input.replaceAll("\\s+", ""); // remove spaces
        this.pos = 0;
        return parseSequence();
    }

    private OrderNode parseSequence() {
        List<OrderNode> children = new ArrayList<>();
        children.add(parseOr());

        while (match(',')) {
            children.add(parseOr());
        }

        return children.size() == 1 ? children.get(0) : new SeqNode(children);
    }

    private OrderNode parseOr() {
        List<OrderNode> children = new ArrayList<>();
        children.add(parseRepeat());

        while (match('|')) {
            children.add(parseRepeat());
        }

        return children.size() == 1 ? children.get(0) : new OrNode(children);
    }

    private OrderNode parseRepeat() {
        OrderNode base = parseGroupOrParen();

        if (match('+')) return new RepeatNode(base, 1);
        if (match('*')) return new RepeatNode(base, 0);
        return base;
    }

    private OrderNode parseGroupOrParen() {
        if (match('(')) {
            OrderNode inside = parseSequence();
            expect(')');
            return inside;
        }

        return new GroupNode(parseGroupName());
    }

    private String parseGroupName() {
        int start = pos;
        while (pos < input.length() && Character.isJavaIdentifierPart(input.charAt(pos))) {
            pos++;
        }
        return input.substring(start, pos);
    }

    private boolean match(char expected) {
        if (pos < input.length() && input.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(char expected) {
        if (!match(expected)) {
            throw new RuntimeException("Expected '" + expected + "' at position " + pos);
        }
    }
}
