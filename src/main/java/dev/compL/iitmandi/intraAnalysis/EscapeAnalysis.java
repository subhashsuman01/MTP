package dev.compL.iitmandi.intraAnalysis;

import dev.compL.iitmandi.utils.ConnectionGraph;
import dev.compL.iitmandi.utils.ConnectionGraphNode;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EscapeAnalysis extends ForwardFlowAnalysis<Unit, ConnectionGraph> {
    final Logger logger = LoggerFactory.getLogger(IntraAnalysis.class);
    AnalysisMode analysisMode;

    public EscapeAnalysis(DirectedGraph<Unit> graph, AnalysisMode analysisMode) {
        super(graph);
        logger.info("Starting forward flow analysis with mode {}", analysisMode);
        this.analysisMode = analysisMode;
        doAnalysis();
    }

    @Override
    protected ConnectionGraph newInitialFlow() {
        logger.info("Performing newInitialFlow");
        return new ConnectionGraph();
    }

    @Override
    protected void copy(ConnectionGraph source, ConnectionGraph dest) {
        logger.info("Performing Copy");
        dest = SerializationUtils.clone(source);
    }

//    @Override
//    protected void doAnalysis() {
//
//    }

    @Override
    protected ConnectionGraph entryInitialFlow() {
        logger.info("performing entryInitFlow");
        return super.entryInitialFlow();
    }

    @Override
    protected boolean isForward() {
        return true;
    }

    @Override
    protected void merge(ConnectionGraph in1, ConnectionGraph in2, ConnectionGraph out) {
        //todo
        logger.info("Performing merge for in: {} \n in2: {} \n out: {}", in1, in2, out);
    }




    protected void flowThrough(ConnectionGraph in, Unit unit, ConnectionGraph out) {

        logger.info("flowthrough start for unit at line {}, {} \n in_graph {} \n out_graph {}", unit.getJavaSourceStartLineNumber(), unit, in, out);

        out.union(in);
        int lineNo = unit.getJavaSourceStartLineNumber();

        if (unit instanceof ReturnStmt) {
            logger.info("unit is an instance of ReturnStmt");
            // todo handle return
            Value retRef = ((ReturnStmt) unit).getOp();
            ConnectionGraphNode retRefNode = new ConnectionGraphNode(retRef.toString(), ConnectionGraph.NodeType.REF, -1);

        } else if (unit instanceof AssignStmt) {

            AssignStmt stmt = (AssignStmt) unit;
            Type type = stmt.getLeftOp().getType();
            Value rightOp = stmt.getRightOp();
            Value leftOp = stmt.getLeftOp();
            logger.info("unit is an instance of AssignStmt with type: {}, leftOp: {}, rightOp: {}", type, leftOp, rightOp);

            // don't need to include primitive types as they are allocated on the stack
            if (type instanceof PrimType) {
                logger.info("Primitive type assignment");
                return;
            }

            List<ConnectionGraphNode> leftRefNodes = new ArrayList<>();
            if (leftOp instanceof ArrayRef) {
                // ref[imm]
                ArrayRef arrayRef = (ArrayRef) leftOp;
                // ref nodes have # in them;  ref[imm] --> ref#imm
                leftRefNodes.add(new ConnectionGraphNode(arrayRef.getBase().toString() + "#" + arrayRef.getIndex().toString(), ConnectionGraph.NodeType.REF, -1));
                if (analysisMode == AnalysisMode.CONTEXT_SENSITIVE) {
                    out.byPass(leftRefNodes.get(0));
                }
                logger.info("leftOp is an instance of ArrayRef, leftOp {}", leftOp);
            } else if (leftOp instanceof Local) {
                // ref
                leftRefNodes.add(new ConnectionGraphNode(leftOp.toString(), ConnectionGraph.NodeType.REF, -1));
                if (analysisMode == AnalysisMode.CONTEXT_SENSITIVE) {
                    out.byPass(leftRefNodes.get(0));
                }
                logger.info("leftOp is an instance of local, leftOp {}", leftOp);
            } else if (leftOp instanceof FieldRef) {
                // ref.field
                FieldRef fieldRefExp = (FieldRef) ((AssignStmt) unit).getLeftOp();
                ConnectionGraphNode fieldRefNode = new ConnectionGraphNode(fieldRefExp.getFieldRef().toString(), ConnectionGraph.NodeType.REF, -1);
                String fieldName = fieldRefExp.getField().toString();
                leftRefNodes.addAll(in.findFields(fieldRefNode, fieldName));
                logger.info("leftOp is an instance of FieldRef");
                logger.info("All possible fields reachable by the ref - {}", leftRefNodes);
                // should not bypass field refs as it can point different objects depending on the context
            } else {
                //field
                ConnectionGraphNode refNode = new ConnectionGraphNode(leftOp.toString(), ConnectionGraph.NodeType.REF, -1);
                leftRefNodes.add(refNode);
                out.setEscaping(refNode);
                if (analysisMode == AnalysisMode.CONTEXT_SENSITIVE) {
                    out.byPass(leftRefNodes.get(0));
                }
                logger.info("leftOp is an instance of field, ref - {}", refNode);
            }
            if (rightOp instanceof Local) {
                ConnectionGraphNode rightNode = new ConnectionGraphNode(rightOp.toString(), ConnectionGraph.NodeType.REF, -1);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    out.addEdge(leftRefNode, rightNode, ConnectionGraph.EdgeType.DEFERRED);
                }
                logger.info("rightOp is an instance of local, ref - {}", rightNode);
            } else if (rightOp instanceof BinopExpr) {
                // conservative assumption that new object created after operator overloading
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
                logger.info("rightOp is an instance of BinopExpr, Created phantom object node");
            } else if (rightOp instanceof CastExpr) {
                // phantom node starts with # type cast expression
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
                logger.info("rightOp is an instance of CastExpr, Created phantom object node");
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
                logger.info("rightOp is an instance of InvokeExpr, Created phantom object node");

            } else if (rightOp instanceof NewExpr) {
                logger.info("rightOp is an instance of NewExpr");
                ConnectionGraphNode rightNode = new ConnectionGraphNode(type.toString(), ConnectionGraph.NodeType.OBJECT, lineNo);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    out.addEdge(leftRefNode, rightNode, ConnectionGraph.EdgeType.POINTSTO);
                }
                SootClass objClass = Scene.v().getSootClass(type.toString());
                Collection<SootField> fields = objClass.getFields().getElementsUnsorted();

                logger.info("rightOp is an instance of NewExpr, Creating new Object {} with fields {}", objClass, fields);
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
                logger.info("rightOp is an instance of NegExpr, Created phantom object node");
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
                // a = b.f
                // a can point to different objects based on context
                FieldRef fieldRefExp = (FieldRef) rightOp;
                ConnectionGraphNode fieldRefNode = new ConnectionGraphNode(fieldRefExp.getFieldRef().toString(), ConnectionGraph.NodeType.REF, -1);
                String fieldName = fieldRefExp.getField().toString();
                List<ConnectionGraphNode> listFields = in.findFields(fieldRefNode, fieldName);
                logger.info("rightOp is an instance of FieldRef, rightOp: {}, fieldName: {}, all references reachable: {}", rightOp, fieldName, listFields);
                for (ConnectionGraphNode leftRefNode : leftRefNodes) {
                    for (ConnectionGraphNode fieldNode : listFields) {
                        out.addEdge(leftRefNode, fieldNode, ConnectionGraph.EdgeType.DEFERRED);
                    }
                }
            }
        }

        logger.info("flowthrough end for unit at line {}, {} \n in_graph {} \n out_graph {}", unit.getJavaSourceStartLineNumber(), unit, in, out);

//        TODO covert to visitor pattern
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
