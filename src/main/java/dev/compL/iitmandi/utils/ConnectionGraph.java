// todo phantom nodes
// todo global escape (static alloc), return, invoke statement

package dev.compL.iitmandi.utils;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public final class ConnectionGraph implements Serializable {

    HashMap<ConnectionGraphNode, Boolean> escaping = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> fieldEdge = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> reversePointsToEdge = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> forwardPointsToEdge = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> forwardDeferredEdge = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> reverseDeferredEdge = new HashMap<>();

    void addEdgeHelper(ConnectionGraphNode n1, ConnectionGraphNode n2, HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> map) {
        if (!escaping.containsKey(n1)) {
            escaping.put(n1, false);
        }
        if (!escaping.containsKey(n2)) {
            escaping.put(n2, false);
        }
        if (map.containsKey(n1)) {
            map.get(n1).add(n2);
        } else {
            map.put(n1, Sets.newHashSet(n2));
        }
    }

    void removeEdgeHelper(ConnectionGraphNode node1, ConnectionGraphNode node2, HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> map) {
        map.get(node1).remove(node2);
        if (map.get(node1).isEmpty()) map.remove(node1);
    }

    public void addEdge(ConnectionGraphNode node1, ConnectionGraphNode node2, @NotNull EdgeType edgeType) {
        switch (edgeType) {
            case FIELD:
                addEdgeHelper(node1, node2, fieldEdge);
                break;
            case DEFERRED:
                addEdgeHelper(node1, node2, forwardDeferredEdge);
                addEdgeHelper(node2, node1, reverseDeferredEdge);
                break;
            case POINTSTO:
                addEdgeHelper(node1, node2, forwardPointsToEdge);
                addEdgeHelper(node2, node1, reversePointsToEdge);
                break;
        }
    }

    public void setEscaping(ConnectionGraphNode node) {
        if (escaping.containsKey(node)) {
            escaping.put(node, true);
        }
        else {
            escaping.put(node, true);
        }
    }

    public void removeEdge(ConnectionGraphNode node1, ConnectionGraphNode node2, EdgeType edgeType) {
        switch (edgeType) {
            case DEFERRED:
                removeEdgeHelper(node1, node2, forwardDeferredEdge);
                removeEdgeHelper(node2, node1, reverseDeferredEdge);
                break;
            case POINTSTO:
                removeEdgeHelper(node1, node2, forwardPointsToEdge);
                removeEdgeHelper(node2, node1, reversePointsToEdge);
                break;
        }
    }

    public void byPass(ConnectionGraphNode node) {

        if (reverseDeferredEdge.containsKey(node)) {
            List<ConnectionGraphNode> secondNodeList = new ArrayList<>(forwardDeferredEdge.get(node));
            List<ConnectionGraphNode> primaryNodeList = new ArrayList<>(reverseDeferredEdge.get(node));
            List<ConnectionGraphNode> objectList = new ArrayList<>();
            if (forwardPointsToEdge.containsKey(node)) {
                objectList = new ArrayList<>(forwardPointsToEdge.get(node));
            }

            primaryNodeList.forEach(primaryNode -> secondNodeList.forEach(secondNode -> addEdge(primaryNode, secondNode, EdgeType.DEFERRED)));
            List<ConnectionGraphNode> finalObjectList = objectList;
            primaryNodeList.forEach(primaryNode -> finalObjectList.forEach(object -> addEdge(primaryNode, object, EdgeType.POINTSTO)));

            primaryNodeList.forEach(primaryNode -> removeEdge(primaryNode, node, EdgeType.DEFERRED));
            secondNodeList.forEach(secondNode -> removeEdge(node, secondNode, EdgeType.DEFERRED));
            objectList.forEach(object -> removeEdge(node, object, EdgeType.POINTSTO));
        }

    }

    public List<ConnectionGraphNode> findFields(ConnectionGraphNode refNode, String fieldName) {
        return pointsTo(refNode).stream().map(obj -> new ConnectionGraphNode(fieldName, NodeType.FIELD, obj.lineNo)).collect(Collectors.toList());
    }

    public HashSet<ConnectionGraphNode> pointsTo(ConnectionGraphNode node) {
        HashSet<ConnectionGraphNode> ret = new HashSet<>();

        LinkedList<ConnectionGraphNode> queue = new LinkedList<>();
        HashSet<ConnectionGraphNode> visited = new HashSet<>();
        queue.add(node);
        visited.add(node);

        while (!queue.isEmpty()) {
            ConnectionGraphNode frontNode = queue.pop();
            if (forwardPointsToEdge.containsKey(frontNode)) {
                ret.addAll(forwardPointsToEdge.get(node));
            }
            if (forwardDeferredEdge.containsKey(frontNode)) {
                forwardDeferredEdge.get(frontNode).stream().filter(visited::contains).forEach(nxtNode -> {
                    queue.push(nxtNode);
                    visited.add(nxtNode);
                });
            }
        }

        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnectionGraph)) return false;
        ConnectionGraph that = (ConnectionGraph) o;
        return fieldEdge.equals(that.fieldEdge) && reversePointsToEdge.equals(that.reversePointsToEdge) && forwardPointsToEdge.equals(that.forwardPointsToEdge) && forwardDeferredEdge.equals(that.forwardDeferredEdge) && reverseDeferredEdge.equals(that.reverseDeferredEdge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldEdge, reversePointsToEdge, forwardPointsToEdge, forwardDeferredEdge, reverseDeferredEdge);
    }

    @Override
    public String toString() {
        return "ConnectionGraph{" + "fieldEdge=" + fieldEdge + ", reversePointsToEdge=" + reversePointsToEdge + ", forwardPointsToEdge=" + forwardPointsToEdge + ", forwardDeferredEdge=" + forwardDeferredEdge + ", reverseDeferredEdge=" + reverseDeferredEdge + '}';
    }

    public enum NodeType {OBJECT, REF, FIELD, GLOBAL}

    public enum EdgeType {POINTSTO, DEFERRED, FIELD}
}
