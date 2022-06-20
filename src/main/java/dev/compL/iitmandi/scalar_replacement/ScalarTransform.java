package dev.compL.iitmandi.scalar_replacement;

import dev.compL.iitmandi.intraAnalysis.IntraAnalysis;
import dev.compL.iitmandi.utils.BranchInfo;
import dev.compL.iitmandi.utils.ConnectionGraphNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScalarTransform extends BodyTransformer {

    private PatchingChain<Unit> units;
    private Chain<Local> locals;
    private List<Unit> escapingFieldRefs;
    private HashMap<ConnectionGraphNode, Unit> objectAllocMap;
    private BranchInfo baseBranch;
    private HashMap<Unit, BranchInfo> unitBranchInfoHashMap;

    public void setUnits(Body b) {
        this.units = b.getUnits();
    }

    public void setBaseBranch(BranchInfo baseBranch) {
        this.baseBranch = baseBranch;
    }

    public void setObjectAllocMap(HashMap<ConnectionGraphNode, Unit> objectAllocMap) {
        this.objectAllocMap = objectAllocMap;
    }

    public void setUnitBranchInfoHashMap(HashMap<Unit, BranchInfo> unitBranchInfoHashMap) {
        this.unitBranchInfoHashMap = unitBranchInfoHashMap;
    }

    final Logger logger = LoggerFactory.getLogger(IntraAnalysis.class);

    private List<Pair<Unit,Unit>> insertAfterUnit = new ArrayList<>();
    private List<Unit> removeUnit = new ArrayList<>();

    public ScalarTransform(){

    }
    public ScalarTransform(Body b, BranchInfo baseBranch, HashMap<ConnectionGraphNode, Unit> objectAllocMap, HashMap<Unit, BranchInfo> unitBranchInfoHashMap, List<Unit> escapingFieldRefs){
        units = b.getUnits();
        locals = b.getLocals();
        this.objectAllocMap = objectAllocMap;
        this.unitBranchInfoHashMap = unitBranchInfoHashMap;
        this.baseBranch = baseBranch;
        this.escapingFieldRefs = escapingFieldRefs;
    }

    private void moveAllocStmts(BranchInfo root){
//        logger.error(root.toString());
        if (!root.getType().equals("base")){
            BranchInfo parent = root.getParent();
            root.getEscapingObjects().forEach(obj -> {
                if(!parent.getEscapingObjects().contains(obj)){
                    removeUnit.add(objectAllocMap.get(obj));
                    removeUnit.add(units.getSuccOf(objectAllocMap.get(obj)));
                    insertAfterUnit.add(new ImmutablePair<>(objectAllocMap.get(obj) ,root.getStartUnit()));
//                    units.remove(objectAllocMap.get(obj));
//                    units.insertAfter(objectAllocMap.get(obj) ,root.getStartUnit());
                }
            });
        }
        for (BranchInfo child: root.getChildren()){
            moveAllocStmts(child);
        }
    }

    private void scalarReplacement(){
        for (Unit unit : escapingFieldRefs){
            AssignStmt assignStmt = (AssignStmt) unit;
            if (assignStmt.getLeftOp() instanceof FieldRef){
                FieldRef fref = (FieldRef) assignStmt.getLeftOp();
                String fieldName = fref.getField().getName();
                List<ValueBox> val = fref.getUseBoxes();
                Value ref = val.get(0).getValue();
                Local local = Jimple.v().newLocal(ref.toString()+"#"+fieldName,fref.getField().getType());
                locals.add(local);
                removeUnit.add(unit);
                AssignStmt newStmt = Jimple.v().newAssignStmt(local, assignStmt.getRightOp());
                insertAfterUnit.add(new ImmutablePair<>(newStmt ,unit));
            } else {
                FieldRef fref = (FieldRef) assignStmt.getRightOp();
                String fieldName = fref.getField().getName();
                List<ValueBox> val = fref.getUseBoxes();
                Value ref = val.get(0).getValue();
                Local local = Jimple.v().newLocal(ref.toString()+"#"+fieldName,fref.getField().getType());
                locals.add(local);
                removeUnit.add(unit);
                AssignStmt newStmt = Jimple.v().newAssignStmt(assignStmt.getLeftOp(), local);
                insertAfterUnit.add(new ImmutablePair<>(newStmt ,unit));
            }

        }
    }

    public void assignmentTransform(){
        for (Unit unit: units.getElementsUnsorted()){
            if (unit instanceof AssignStmt){
                AssignStmt stmt = (AssignStmt) unit;
                Type type = stmt.getLeftOp().getType();
                Value left = stmt.getLeftOp();
                Value right = stmt.getRightOp();
                if (type instanceof PrimType) continue;

                if((left instanceof Local) && (right instanceof Local)){
                    SootClass sc = Scene.v().getSootClass(type.toString());
                    removeUnit.add(unit);
                    for(SootField sf: sc.getFields()){
                        Local local1 = Jimple.v().newLocal(left.toString()+"#"+sf.getName(),sf.getType());
                        Local local2 = Jimple.v().newLocal(right.toString()+"#"+sf.getName(),sf.getType());
                        locals.add(local1);
                        locals.add(local2);
                        AssignStmt newStmt = Jimple.v().newAssignStmt(local1, local2);
                        insertAfterUnit.add(new ImmutablePair<>(newStmt ,unit));
                    }
                }
            }
        }
    }

    public void ifStmtTransform(){
        for (Unit unit : units.getElementsUnsorted()){
            if (unit instanceof IfStmt){
                IfStmt ifStmt = (IfStmt) unit;
                NopStmt newStmt = Jimple.v().newNopStmt();
                Stmt target = ifStmt.getTarget();
                units.insertBefore(newStmt,target);
                ifStmt.setTarget(newStmt);
            }
        }
    }

    public void internalTransform(){
        ifStmtTransform();
        moveAllocStmts(baseBranch);
//        logger.info(insertAfterUnit.toString());
//        logger.info(removeUnit.toString());
        Value tmp;
        for (Unit unit: removeUnit){
            List<Unit> body = new ArrayList<>();
            if (unit instanceof InvokeStmt){
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                for (Value val : invokeStmt.getInvokeExpr().getArgs()){
                    Local l = Jimple.v().newLocal(invokeStmt.getInvokeExpr().getMethodRef().getName() + "#" + val, val.getType());
                    locals.add(l);
//                    units.insertAfter(Jimple.v().newAssignStmt(l, val),unit);
                }
            }
            units.remove(unit);
        }
        for (Pair<Unit,Unit> pair: insertAfterUnit){
            try {
                AssignStmt stmt = (AssignStmt) pair.getLeft();
                AssignStmt newStmt = Jimple.v().newAssignStmt(stmt.getLeftOp(), stmt.getRightOp());

                InvokeStmt invStmt = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr((Local) stmt.getLeftOp(),
                        Scene.v().getSootClass(stmt.getLeftOp().getType().toString()).getMethodByName("<init>").makeRef()));

                List<Unit> toInsert = new ArrayList<>();
                toInsert.add(stmt);
                toInsert.add(invStmt);
                units.insertAfter(toInsert, pair.getRight());

            } catch (Exception e){

            }
        }
        removeUnit.clear();
        insertAfterUnit.clear();
        scalarReplacement();
        assignmentTransform();
//        logger.info(insertAfterUnit.toString());
//        logger.info(removeUnit.toString());
        for (Pair<Unit,Unit> pair: insertAfterUnit){
            units.insertAfter(pair.getLeft(), pair.getRight());
        }
        for (Unit unit: removeUnit){
            units.remove(unit);
        }

    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        internalTransform();
    }

}
