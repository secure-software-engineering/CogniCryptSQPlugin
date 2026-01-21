package org.sonarsource.plugins.secai.reporting.cognicrypt.flows;

import java.util.List;
import java.util.Set;

abstract class OrderNode {
    abstract boolean isSatisfied(Set<String> calledGroups);
    abstract void collectMissing(Set<String> calledGroups, Set<String> missingGroups);
}

class GroupNode extends OrderNode {
    String groupName;
    public GroupNode(String groupName) { this.groupName = groupName; }

    @Override
    boolean isSatisfied(Set<String> calledGroups) {
        return calledGroups.contains(groupName);
    }

    @Override
    void collectMissing(Set<String> calledGroups, Set<String> missingGroups) {
        if (!isSatisfied(calledGroups)) {
            missingGroups.add(groupName);
        }
    }
}

class OrNode extends OrderNode {
    List<OrderNode> children;
    public OrNode(List<OrderNode> children) { this.children = children; }

    @Override
    boolean isSatisfied(Set<String> calledGroups) {
        return children.stream().anyMatch(child -> child.isSatisfied(calledGroups));
    }

    @Override
    void collectMissing(Set<String> calledGroups, Set<String> missingGroups) {
        if (!isSatisfied(calledGroups)) {
            children.forEach(child -> child.collectMissing(calledGroups, missingGroups));
        }
    }
}

class SeqNode extends OrderNode {
    List<OrderNode> children;
    public SeqNode(List<OrderNode> children) { this.children = children; }

    @Override
    boolean isSatisfied(Set<String> calledGroups) {
        return children.stream().allMatch(child -> child.isSatisfied(calledGroups));
    }

    @Override
    void collectMissing(Set<String> calledGroups, Set<String> missingGroups) {
        for (OrderNode child : children) {
            if (!child.isSatisfied(calledGroups)) {
                child.collectMissing(calledGroups, missingGroups);
            }
        }
    }
}

class RepeatNode extends OrderNode {
    OrderNode child;
    int min;  // 0 for *, 1 for +

    public RepeatNode(OrderNode child, int min) {
        this.child = child;
        this.min = min;
    }

    @Override
    boolean isSatisfied(Set<String> calledGroups) {
        return min == 0 || child.isSatisfied(calledGroups);
    }

    @Override
    void collectMissing(Set<String> calledGroups, Set<String> missingGroups) {
        if (!isSatisfied(calledGroups)) {
            child.collectMissing(calledGroups, missingGroups);
        }
    }
}
