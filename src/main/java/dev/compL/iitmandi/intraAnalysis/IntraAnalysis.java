// todo handle this reference
// todo handle phantom node creation for a = method()/ a = b.f() / a = global
// todo handle escape stmts -: ret, func(a,b,c), global = a;
// todo refactor

package dev.compL.iitmandi.intraAnalysis;

import dev.compL.iitmandi.utils.ConnectionGraph;
import dev.compL.iitmandi.utils.ConnectionGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.*;


public class IntraAnalysis {
    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(IntraAnalysis.class);

        List<ConnectionGraphNode> escapeList = new ArrayList<>();

        String sourceDir = System.getProperty("user.dir") + File.separator + "example" + File.separator + "IntraAnalysis";
        String className = "IntraAnalysis";
        G.reset();
        Options.v().set_whole_program(true);
        Options.v().app();
        Options.v().set_soot_classpath(sourceDir);
        Options.v().set_prepend_classpath(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_keep_offset(true);
        Options.v().set_main_class(className);

        SootClass sc = Scene.v().loadClassAndSupport(className);
        Scene.v().loadNecessaryClasses();
//        sc.setApplicationClass();

        SootClass mainClass = Scene.v().getSootClass(className);
        SootMethod method = mainClass.getMethodByName("method");


        JimpleBody methodBody = (JimpleBody) method.retrieveActiveBody();

        UnitGraph unitGraph = new TrapUnitGraph(methodBody);


        EscapeAnalysis analysis = new EscapeAnalysis(unitGraph, EscapeAnalysis.AnalysisMode.CONTEXT_INSENSITIVE);

//        for (Unit unit: methodBody.getUnits()){
//            logger.info("unit: {} --> {}", unit, analysis.getFlowAfter(unit));
//        }


        HashMap<Unit, String> branchInfo = new HashMap<>();
        // todo improve branchStartInfo
        HashMap<Unit, Stack<Integer>> branchStartInfo = new HashMap<>();

        HashSet<Unit> startElse = new HashSet<>(), endElse = new HashSet<>();
        Stack<Integer> tempStack = new Stack<>();
        StringBuilder temp = new StringBuilder("0");
        for (Unit unit: methodBody.getUnits()){
            if (unit instanceof IfStmt){
                IfStmt stmt = (IfStmt) unit;
                startElse.add(stmt.getTarget());
                temp.append('1');
                tempStack.add(unit.getJavaSourceStartLineNumber());
            }
            else if (unit instanceof GotoStmt){
                GotoStmt stmt = (GotoStmt) unit;
                endElse.add(stmt.getTarget());
                temp.deleteCharAt(temp.length() - 1);
                tempStack.pop();
            }
            else if (startElse.contains(unit)){
                temp.append('2');
                tempStack.add(unit.getJavaSourceStartLineNumber());
            } else if (endElse.contains(unit)){
                temp.deleteCharAt(temp.length() - 1);
                tempStack.pop();
            }
            branchInfo.put(unit, temp.toString());
            branchStartInfo.put(unit, (Stack<Integer>) tempStack.clone());

//            logger.info("{} ---> {} ;;; {}", unit, branchInfo.get(unit), branchStartInfo.get(unit));
        }

        HashMap<ConnectionGraphNode, HashSet<String>> escapingObjectInfo = new HashMap<>();

        for (Unit unit : methodBody.getUnits()){
            List<Value> escapingArgs = new ArrayList<>();

            if (unit instanceof AssignStmt){
                AssignStmt stmt = (AssignStmt) unit;
                Type type = stmt.getLeftOp().getType();
                Value leftOp = stmt.getLeftOp();
                Value rightOp = stmt.getRightOp();
                if (type instanceof PrimType){
                    continue;
                }
                // we only care about data classes
                SootClass objClass = Scene.v().getSootClass(type.toString());
                if (objClass.getMethods().size()>1){
//                logger.warn("Not a data class");
                    continue;
                }
                if (!(leftOp instanceof ArrayRef) && !(leftOp instanceof Local) && !(leftOp instanceof FieldRef)) {
                    escapingArgs.add(leftOp);
                }

                if (rightOp instanceof InvokeExpr){
                    InvokeExpr invokeExpr = (InvokeExpr) rightOp;
                    escapingArgs.addAll(invokeExpr.getArgs());
                } else if (rightOp instanceof NewExpr){
                    ConnectionGraphNode node = new ConnectionGraphNode(type.toString(), ConnectionGraph.NodeType.OBJECT, unit.getJavaSourceStartLineNumber());
                    escapingObjectInfo.put(node, new HashSet<>());
                }
            }
            if (unit instanceof RetStmt){
                RetStmt retStmt = (RetStmt) unit;
                escapingArgs.add(retStmt.getStmtAddress());
            }
            if (unit instanceof InvokeStmt){
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                escapingArgs.addAll(invokeStmt.getInvokeExpr().getArgs());
            }

            ConnectionGraph graph = analysis.getFlowAfter(unit);
            if (escapingArgs.isEmpty()) continue;
            for (Value ref: escapingArgs){
                HashSet<ConnectionGraphNode> st = graph.pointsTo(new ConnectionGraphNode(ref.toString(), ConnectionGraph.NodeType.REF, -1));
                logger.info("unit:{}, ref: {} --> {}",unit ,ref, st);
                for (ConnectionGraphNode objNode : st){
                    escapingObjectInfo.get(objNode).add(branchInfo.get(unit));
                }
            }

        }
    }

}

// todo make a better data structure to store startbranchinfo and branchinfo
// todo work on replacement using escapingObjectInfo
// todo do a dfs over entire cfg for every object and do replacement; work on replacement algo
