// todo handle this reference
// todo handle phantom node creation for a = method()/ a = b.f() / a = global
// todo handle escape stmts -: ret, func(a,b,c), global = a;
// todo refactor

package dev.compL.iitmandi.intraAnalysis;

import dev.compL.iitmandi.scalar_replacement.ScalarTransform;
import dev.compL.iitmandi.utils.BranchInfo;
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


        Stack<BranchInfo> branchStack = new Stack<>();
        BranchInfo baseBranch = new BranchInfo("base", 0, 0, -1, unitGraph.getHeads().get(0), null);
        branchStack.add(baseBranch);

        HashSet<Unit> startElse = new HashSet<>(), endElse = new HashSet<>();

        HashMap<Unit, BranchInfo> unitBranchInfoHashMap = new HashMap<>();

        for (Unit unit: methodBody.getUnits()){
            BranchInfo head = branchStack.peek();
            unitBranchInfoHashMap.put(unit, head);
            if (unit instanceof IfStmt){
                IfStmt stmt = (IfStmt) unit;
                startElse.add(stmt.getTarget());
                BranchInfo newBranch = new BranchInfo("if", branchStack.size(), unit.getJavaSourceStartLineNumber(), -1, unit, null);
                head.addChild(newBranch);
                branchStack.add(newBranch);
            }
            else if (unit instanceof GotoStmt){
                branchStack.pop();
                head.setEndUnit(unit);
                head.setEndLine(unit.getJavaSourceStartLineNumber());
                GotoStmt stmt = (GotoStmt) unit;
                endElse.add(stmt.getTarget());
            }
            else if (startElse.contains(unit)){
                BranchInfo newBranch = new BranchInfo("else", branchStack.size(), unit.getJavaSourceStartLineNumber(), -1, unit, null);
                head.addChild(newBranch);
                branchStack.add(newBranch);
            } else if (endElse.contains(unit)){
                branchStack.pop();
                head.setEndLine(unit.getJavaSourceStartLineNumber());
                head.setEndUnit(unit);
            }

//            logger.info("{} ---> {} ;;; {}", unit, branchInfo.get(unit), branchStartInfo.get(unit));
        }

        HashMap<ConnectionGraphNode, Unit> objectAllocMap = new HashMap<>();

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
                    objectAllocMap.put(node, unit);
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
//                logger.info("unit:{}, ref: {} --> {}", unit, ref, st);
                for (ConnectionGraphNode objNode : st){
                    unitBranchInfoHashMap.get(unit).markEscapingObject(objNode);
                }
            }


        }



        BranchInfo.dfsMarkEscaping(baseBranch);

        List<Unit> escapingFieldRefs = new ArrayList<>();

        logger.info("iiiiiiiiiiii");
        for (Unit unit : methodBody.getUnits()){

            if (unit instanceof AssignStmt){
                AssignStmt stmt = (AssignStmt) unit;
                Type type = stmt.getLeftOp().getType();
                Value leftOp = stmt.getLeftOp();
                Value rightOp = stmt.getRightOp();
                if (leftOp instanceof FieldRef){
                    FieldRef fref = (FieldRef) leftOp;
                   ConnectionGraph graph = analysis.getFlowAfter(unit);
                    List<ValueBox> val = fref.getUseBoxes();
                    if(val.size()!=1){
                        continue;
                    }
                    Value ref = val.get(0).getValue();
                    Set<ConnectionGraphNode> pointsTo = graph.pointsTo(new ConnectionGraphNode(ref.toString(), ConnectionGraph.NodeType.REF, -1));
                    Set<ConnectionGraphNode> escapingInBranch = unitBranchInfoHashMap.get(unit).getEscapingObjects();
                    if(pointsTo.stream().noneMatch(escapingInBranch::contains)){
                        escapingFieldRefs.add(unit);
                        logger.info(unit+"--------->"+leftOp+"------>"+ref);
                    }
                }
                if (rightOp instanceof FieldRef){
                    FieldRef fref = (FieldRef) rightOp;
                    ConnectionGraph graph = analysis.getFlowAfter(unit);
                    List<ValueBox> val = fref.getUseBoxes();
                    if(val.size()!=1){
                        continue;
                    }
                    Value ref = val.get(0).getValue();
                    Set<ConnectionGraphNode> pointsTo = graph.pointsTo(new ConnectionGraphNode(ref.toString(), ConnectionGraph.NodeType.REF, -1));
                    Set<ConnectionGraphNode> escapingInBranch = unitBranchInfoHashMap.get(unit).getEscapingObjects();
                    if(pointsTo.stream().noneMatch(escapingInBranch::contains)){
                        escapingFieldRefs.add(unit);
                        logger.info(unit+"--------->"+rightOp+"------>"+ref);
                    }
                }
            }
        }
        logger.info("iiiiiiiiiiii");


        for (Unit unit: methodBody.getUnits()){
            logger.info(unit.toString());
        }

        logger.info("-----------------");

        ScalarTransform scalarTransform = new ScalarTransform(methodBody, baseBranch, objectAllocMap, unitBranchInfoHashMap, escapingFieldRefs);
        scalarTransform.internalTransform();

        logger.info("-----------------");

        for (Unit unit: methodBody.getUnits()){
            logger.info(unit.toString());
        }

    }

}

