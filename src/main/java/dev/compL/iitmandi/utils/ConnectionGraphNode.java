package dev.compL.iitmandi.utils;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;

public class ConnectionGraphNode implements Serializable {
     String name;
     ConnectionGraph.NodeType nodeType;
     int lineNo;

    public int getLineNo() {
        return lineNo;
    }

    public ConnectionGraph.NodeType getNodeType() {
        return nodeType;
    }

    public String getName() {
        return name;
    }

    public ConnectionGraphNode(@NotNull String _name, @NotNull ConnectionGraph.NodeType _type, int _lineNo) {
        name = _name;
        nodeType = _type;
        lineNo = _lineNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnectionGraphNode)) return false;
        ConnectionGraphNode that = (ConnectionGraphNode) o;
        return getLineNo() == that.getLineNo() && getName().equals(that.getName()) && getNodeType() == that.getNodeType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getNodeType(), getLineNo());
    }

    @Override
    public String toString() {
        return "ConnectionGraphNode{" +
                "name='" + name + '\'' +
                ", nodeType=" + nodeType +
                ", lineNo=" + lineNo +
                '}';
    }
}
