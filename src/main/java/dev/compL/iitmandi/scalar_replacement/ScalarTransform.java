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

        if (!root.getType().equals("base")){
            BranchInfo parent = root.getParent();
            root.getEscapingObjects().forEach(obj -> {
                if(!parent.getEscapingObjects().contains(obj)){
                    removeUnit.add(objectAllocMap.get(obj));
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

    public void internalTransform(){
        moveAllocStmts(baseBranch);
        logger.info(insertAfterUnit.toString());
        logger.info(removeUnit.toString());
        for (Unit unit: removeUnit){
            units.remove(unit);
        }
        for (Pair<Unit,Unit> pair: insertAfterUnit){
            AssignStmt stmt = (AssignStmt) pair.getLeft();
            AssignStmt newStmt = Jimple.v().newAssignStmt(stmt.getLeftOp(), stmt.getRightOp());
            units.insertAfter(newStmt, pair.getRight());
        }
        removeUnit.clear();
        insertAfterUnit.clear();
        scalarReplacement();
        logger.info(insertAfterUnit.toString());
        logger.info(removeUnit.toString());
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
