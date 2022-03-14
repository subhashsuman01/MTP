// todo phantom nodes
// todo global escape (static alloc), return, invoke statement

package dev.compL.iitmandi.utils;

import com.google.common.collect.Sets;
import dev.compL.iitmandi.intraAnalysis.IntraAnalysis;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public final class ConnectionGraph implements Serializable {

    final Logger logger = LoggerFactory.getLogger(IntraAnalysis.class);

    HashMap<ConnectionGraphNode, Boolean> escaping = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> fieldEdge = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> reversePointsToEdge = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> forwardPointsToEdge = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> forwardDeferredEdge = new HashMap<>();
    HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> reverseDeferredEdge = new HashMap<>();

    void addEdgeHelper(ConnectionGraphNode n1, ConnectionGraphNode n2, HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> map) {
        logger.info("adding edge between n1: {}, n2:{}, map:{}", n1, n2, map);
        if (!escaping.containsKey(n1)) {
            escaping.put(n1, false);
        }
        if (!escaping.containsKey(n2)) {
            escaping.put(n2, false);
        }
        if (map.containsKey(n1)) {
            logger.info("map contains n1");
            map.get(n1).add(n2);
        } else {
            logger.info("map does not have n2");
            map.put(n1, Sets.newHashSet(n2));
        }
    }

    void removeEdgeHelper(ConnectionGraphNode node1, ConnectionGraphNode node2, HashMap<ConnectionGraphNode, HashSet<ConnectionGraphNode>> map) {
        logger.info("Trying to delete n1: {}, n2: {}, from map: {}", node1, node2, map);
        if (!map.containsKey(node1)) {
            System.err.println("Error can't find node1 ro be deleted");
        }
        map.get(node1).remove(node2);
        if (map.get(node1).isEmpty()) map.remove(node1);
    }

    public void addEdge(ConnectionGraphNode node1, ConnectionGraphNode node2, @NotNull EdgeType edgeType) {
        logger.info("addEdge between node1: {}, node2: {}, edgeType: {}", node1, node2, edgeType);
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

    public ConnectionGraph merge(ConnectionGraph graph2) {
        ConnectionGraph ret = new ConnectionGraph();

        for (ConnectionGraphNode key : forwardDeferredEdge.keySet()) {
            if (!ret.forwardDeferredEdge.containsKey(key)) {
                ret.forwardDeferredEdge.put(key, new HashSet<>());
            }
            ret.forwardDeferredEdge.get(key).addAll(forwardDeferredEdge.get(key));
            ret.forwardDeferredEdge.get(key).addAll(graph2.forwardDeferredEdge.get(key));

            if (!ret.reverseDeferredEdge.containsKey(key)) {
                ret.reverseDeferredEdge.put(key, new HashSet<>());
            }
            ret.reverseDeferredEdge.get(key).addAll(reverseDeferredEdge.get(key));
            ret.reverseDeferredEdge.get(key).addAll(graph2.reverseDeferredEdge.get(key));

            if (!ret.reversePointsToEdge.containsKey(key)) {
                ret.reversePointsToEdge.put(key, new HashSet<>());
            }
            ret.reversePointsToEdge.get(key).addAll(reversePointsToEdge.get(key));
            ret.reversePointsToEdge.get(key).addAll(graph2.reversePointsToEdge.get(key));

            if (!ret.forwardPointsToEdge.containsKey(key)) {
                ret.forwardPointsToEdge.put(key, new HashSet<>());
            }
            ret.forwardPointsToEdge.get(key).addAll(forwardPointsToEdge.get(key));
            ret.forwardPointsToEdge.get(key).addAll(graph2.forwardPointsToEdge.get(key));

            if (!ret.fieldEdge.containsKey(key)) {
                ret.fieldEdge.put(key, new HashSet<>());
            }
            ret.fieldEdge.get(key).addAll(fieldEdge.get(key));
            ret.fieldEdge.get(key).addAll(graph2.fieldEdge.get(key));
        }

        return ret;
    }

    public void setEscaping(ConnectionGraphNode node) {
        logger.info("Setting node:{} to be escaping", node);
        if (escaping.containsKey(node)) {
            System.err.println("Can't find Object to be set Escaping");
        }
        escaping.put(node, true);
    }

    public void removeEdge(ConnectionGraphNode node1, ConnectionGraphNode node2, EdgeType edgeType) {
        logger.info("removeEdge node1: {}, node2: {}, edgeType: {}", node1, node2, edgeType);
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
        logger.info("byPassing node: {}", node);

        if (reverseDeferredEdge.containsKey(node)) {
            logger.info("Node present in reverseDeferredEdge");
            List<ConnectionGraphNode> secondNodeList = new ArrayList<>(forwardDeferredEdge.get(node));
            List<ConnectionGraphNode> primaryNodeList = new ArrayList<>(reverseDeferredEdge.get(node));
            List<ConnectionGraphNode> objectList = new ArrayList<>();
            if (forwardPointsToEdge.containsKey(node)) {
                logger.info("Node present in forwardPointsToEdge");
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
        logger.info("finding all reachable fields from refNode {}, fieldName: {}", refNode, fieldName);
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

        logger.info("All possible objects node: {} points to, listObjects {}", node, ret);

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
