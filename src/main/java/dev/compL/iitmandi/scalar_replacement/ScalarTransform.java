// todo bool -> if_escaping()

// todo reInitFields()
// todo replace useboxes()
// todo initialise fields based on initialiser
// todo check object is not an interface-implementation or extended

package dev.compL.iitmandi.scalar_replacement;

import dev.compL.iitmandi.intraAnalysis.IntraAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.util.Chain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ScalarTransform extends BodyTransformer {

    private PatchingChain<Unit> units;

    public PatchingChain<Unit> getUnits() {
        return units;
    }

    static int counter = 0;

    final Logger logger = LoggerFactory.getLogger(IntraAnalysis.class);

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        units = b.getUnits();
        Chain<Local> locals = b.getLocals();

        for (Unit unit : b.getUnits()) {
            System.out.println(unit);
        }

        System.out.println("--------------------------------------");
        System.out.println("--------------------------------------");
        System.out.println("--------------------------------------");
        System.out.println("--------------------------------------");

        for (Unit unit : b.getUnits()) {

            if ((unit instanceof AssignStmt) && (((AssignStmt) unit).getRightOp() instanceof NewExpr)) {
                Value leftRef = ((AssignStmt) unit).getLeftOp();
                Type type = leftRef.getType();
                int lineNo = unit.getJavaSourceStartLineNumber();
                if(!type.toString().equals("ScalarTransform$Obj")){
                  continue;
                }

                SootClass sootClass = Scene.v().getSootClass(type.toString());
                Chain<SootField> fields = sootClass.getFields();
                for (SootField field : fields) {
                    if (Objects.equals(field.getName(), "this")) {
                        continue;
                    }
                    Local tmpLocal = Jimple.v().newLocal("tmp" + (counter++) + field.getName(), field.getType());
                    locals.add(tmpLocal);

//                    InstanceFieldRef fieldValueBox = Jimple.v().newInstanceFieldRef(leftRef, field.makeRef());

//                    b.getUseAndDefBoxes().stream().filter(valueBox -> valueBox.getValue().toString().equals(fieldValueBox.toString())).forEach(valueBox -> valueBox.setValue(tmpLocal));

                }
            }
        }

        for (Unit unit : b.getUnits()) {
            System.out.println(unit);
        }

//        Local tmpLocal = Jimple.v().newLocal("tmp1", IntType.v());
//        locals.add(tmpLocal);
//        AssignStmt assignStmt = Jimple.v().newAssignStmt(tmpLocal, IntConstant.v(3));
//        System.out.println(assignStmt);
//        units.addFirst(assignStmt);
    }

    // list of units to be removed
    public void removeUnits(List<Unit> lst){
        lst.forEach(unit -> {units.remove(unit);});
    }

    private void findAndReplace(Body b, String type, int line_no) {

    }
}
