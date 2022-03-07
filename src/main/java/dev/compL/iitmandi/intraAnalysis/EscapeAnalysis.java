package dev.compL.iitmandi.intraAnalysis;

import dev.compL.iitmandi.utils.ConnectionGraph;
import dev.compL.iitmandi.utils.ConnectionGraphNode;
import org.apache.commons.lang3.SerializationUtils;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EscapeAnalysis extends ForwardFlowAnalysis<Unit, ConnectionGraph> {
    AnalysisMode analysisMode;

    public EscapeAnalysis(DirectedGraph<Unit> graph, AnalysisMode analysisMode) {
        super(graph);
        this.analysisMode = analysisMode;

        doAnalysis();
    }

    @Override
    protected ConnectionGraph newInitialFlow() {
        return new ConnectionGraph();
    }

    @Override
    protected void copy(ConnectionGraph source, ConnectionGraph dest) {
        dest = SerializationUtils.clone(source);
    }

    @Override
    protected ConnectionGraph entryInitialFlow() {
        return super.entryInitialFlow();
    }

    @Override
    protected void merge(ConnectionGraph in1, ConnectionGraph in2, ConnectionGraph out) {

    }

    @Override
    protected void flowThrough(ConnectionGraph in, Unit unit, ConnectionGraph out) {

        out = SerializationUtils.clone(in);
        int lineNo = unit.getJavaSourceStartLineNumber();

        if (unit instanceof ReturnStmt) {
            // todo handle return

        } else if (unit instanceof AssignStmt) {
            AssignStmt stmt = (AssignStmt) unit;
            Type type = stmt.getLeftOp().getType();
            Value rightOp = stmt.getRightOp();
            Value leftOp = stmt.getLeftOp();


            // don't need to include primitive types as they are allocated on the stack
            if (type instanceof PrimType) {
                return;
            }

            List<ConnectionGraphNode> leftRefNodes = new ArrayList<>();
            if (leftOp instanceof ArrayRef) {
                // ref[imm]
                ArrayRef arrayRef = (ArrayRef) leftOp;
                // ref nodes having '#' are array ref
                leftRefNodes.add(new ConnectionGraphNode(arrayRef.getBase().toString() + "#" + arrayRef.getIndex().toString(), ConnectionGraph.NodeType.REF, -1));
                if (analysisMode == AnalysisMode.CONTEXT_SENSITIVE) {
                    out.byPass(leftRefNodes.get(0));
                }
            } else if (leftOp instanceof Local) {
                // ref
                leftRefNodes.add(new ConnectionGraphNode(leftOp.toString(), ConnectionGraph.NodeType.REF, -1));
                if (analysisMode == AnalysisMode.CONTEXT_SENSITIVE) {
                    out.byPass(leftRefNodes.get(0));
                }
            } else if (leftOp instanceof FieldRef) {
                // ref.field
                FieldRef fieldRefExp = (FieldRef) ((AssignStmt) unit).getLeftOp();
                ConnectionGraphNode fieldRefNode = new ConnectionGraphNode(fieldRefExp.getFieldRef().toString(), ConnectionGraph.NodeType.REF, -1);
                String fieldName = fieldRefExp.getField().toString();
                leftRefNodes.addAll(in.findFields(fieldRefNode, fieldName));
                // cannot bypass field ref
            } else {
                //field
                leftRefNodes.add(new ConnectionGraphNode(leftOp.toString(), ConnectionGraph.NodeType.REF, -1, true));
                if (analysisMode == AnalysisMode.CONTEXT_SENSITIVE) {
                    out.byPass(leftRefNodes.get(0));
                }
            }

            if (rightOp instanceof Local) {
                ConnectionGraphNode rightNode = new ConnectionGraphNode(rightOp.toString(), ConnectionGraph.NodeType.REF, -1);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    out.addEdge(leftRefNode, rightNode, ConnectionGraph.EdgeType.DEFERRED);
                }
            } else if (rightOp instanceof BinopExpr) {
                // phantom node starts with #
                ConnectionGraphNode rightNode = new ConnectionGraphNode("#" + type.toString(), ConnectionGraph.NodeType.OBJECT, lineNo);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    out.addEdge(leftRefNode, rightNode, ConnectionGraph.EdgeType.POINTSTO);
                }
                SootClass objClass = Scene.v().getSootClass(type.toString());
                Collection<SootField> fields = objClass.getFields().getElementsUnsorted();

                for (SootField field : fields) {
                    ConnectionGraphNode fieldNode = new ConnectionGraphNode(field.getName(), ConnectionGraph.NodeType.FIELD, lineNo);
                    out.addEdge(rightNode, fieldNode, ConnectionGraph.EdgeType.FIELD);
                }
            } else if (rightOp instanceof CastExpr) {
                // phantom node starts with #
                ConnectionGraphNode rightNode = new ConnectionGraphNode("#" + type.toString(), ConnectionGraph.NodeType.OBJECT, lineNo);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    out.addEdge(leftRefNode, rightNode, ConnectionGraph.EdgeType.POINTSTO);
                }
                SootClass objClass = Scene.v().getSootClass(type.toString());
                Collection<SootField> fields = objClass.getFields().getElementsUnsorted();

                for (SootField field : fields) {
                    ConnectionGraphNode fieldNode = new ConnectionGraphNode(field.getName(), ConnectionGraph.NodeType.FIELD, lineNo);
                    out.addEdge(rightNode, fieldNode, ConnectionGraph.EdgeType.FIELD);
                }
            } else if (rightOp instanceof InstanceOfExpr) {
                return;

            } else if (rightOp instanceof InvokeExpr) {
                // phantom node starts with #
                ConnectionGraphNode rightNode = new ConnectionGraphNode("#" + type.toString(), ConnectionGraph.NodeType.OBJECT, lineNo);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    out.addEdge(leftRefNode, rightNode, ConnectionGraph.EdgeType.POINTSTO);
                }
                SootClass objClass = Scene.v().getSootClass(type.toString());
                Collection<SootField> fields = objClass.getFields().getElementsUnsorted();

                for (SootField field : fields) {
                    ConnectionGraphNode fieldNode = new ConnectionGraphNode(field.getName(), ConnectionGraph.NodeType.FIELD, lineNo);
                    out.addEdge(rightNode, fieldNode, ConnectionGraph.EdgeType.FIELD);
                }

            } else if (rightOp instanceof NewExpr) {
                ConnectionGraphNode rightNode = new ConnectionGraphNode(type.toString(), ConnectionGraph.NodeType.OBJECT, lineNo);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    out.addEdge(leftRefNode, rightNode, ConnectionGraph.EdgeType.POINTSTO);
                }
                SootClass objClass = Scene.v().getSootClass(type.toString());
                Collection<SootField> fields = objClass.getFields().getElementsUnsorted();

                for (SootField field : fields) {
                    ConnectionGraphNode fieldNode = new ConnectionGraphNode(field.getName(), ConnectionGraph.NodeType.FIELD, lineNo);
                    out.addEdge(rightNode, fieldNode, ConnectionGraph.EdgeType.FIELD);
                }
            } else if (rightOp instanceof NewArrayExpr) {
                return;

            } else if (rightOp instanceof NewMultiArrayExpr) {
                return;
            } else if (rightOp instanceof NegExpr) {
                // phantom node starts with #
                ConnectionGraphNode rightNode = new ConnectionGraphNode("#" + type.toString(), ConnectionGraph.NodeType.OBJECT, lineNo);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    out.addEdge(leftRefNode, rightNode, ConnectionGraph.EdgeType.POINTSTO);
                }
                SootClass objClass = Scene.v().getSootClass(type.toString());
                Collection<SootField> fields = objClass.getFields().getElementsUnsorted();

                for (SootField field : fields) {
                    ConnectionGraphNode fieldNode = new ConnectionGraphNode(field.getName(), ConnectionGraph.NodeType.FIELD, lineNo);
                    out.addEdge(rightNode, fieldNode, ConnectionGraph.EdgeType.FIELD);
                }
            } else if (rightOp instanceof FieldRef) {
                FieldRef fieldRefExp = (FieldRef) rightOp;
                ConnectionGraphNode fieldRefNode = new ConnectionGraphNode(fieldRefExp.getFieldRef().toString(), ConnectionGraph.NodeType.REF, -1);
                String fieldName = fieldRefExp.getField().toString();
                List<ConnectionGraphNode> listFields = in.findFields(fieldRefNode, fieldName);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    for (ConnectionGraphNode fieldNode : listFields) {
                        out.addEdge(leftRefNode, fieldNode, ConnectionGraph.EdgeType.DEFERRED);
                    }
                }
            }
        }
//        TODO visitor pattern
//        unit.apply(new AbstractStmtSwitch() {
//            @Override
//            public void caseAssignStmt(AssignStmt stmt) {
//
//            }
//        });
    }

    enum AnalysisMode {
        CONTEXT_SENSITIVE, CONTEXT_INSENSITIVE
    }

}
