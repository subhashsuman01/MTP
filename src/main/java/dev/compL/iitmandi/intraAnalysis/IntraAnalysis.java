// todo handle this reference
// todo handle phatom node creation for a = method()/ a = b.f() / a = global
// todo handle escape stmts -: ret, func(a,b,c), global = a;
// todo refactor

package dev.compL.iitmandi.intraAnalysis;

import soot.*;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;


public class IntraAnalysis {
    public static void main(String[] args) {
        String sourceDir = System.getProperty("user.dir") + File.separator + "example" + File.separator + "IntraAnalysis";
        String className = "IntraAnalysis";
        G.reset();
        Options.v().set_whole_program(true);
        Options.v().app();
        Options.v().set_soot_classpath(sourceDir);
        Options.v().set_prepend_classpath(true);
        Options.v().set_keep_line_number(true);
//        Options.v().set_keep_offset(true);
        Options.v().set_main_class(className);
//        Options.v().setcl

        SootClass sc = Scene.v().loadClassAndSupport(className);
        Scene.v().loadNecessaryClasses();
//        sc.setApplicationClass();

        SootClass mainClass = Scene.v().getSootClass(className);
        SootMethod method = mainClass.getMethodByName("methodA");

        JimpleBody methodBody = (JimpleBody) method.retrieveActiveBody();
        UnitGraph unitGraph = new TrapUnitGraph(methodBody);

        SootClass st = Scene.v().getSootClass("IntraAnalysis$Obj");
        System.out.println(st.getFields());

        for (Unit unit : methodBody.getUnits()) {
//            System.out.println(unit);
            if (unit instanceof AssignStmt) {
//                System.out.println(unit);
                Value leftOp = ((AssignStmt) unit).getLeftOp();

                if (leftOp instanceof ArrayRef) {
                    System.out.println(unit);
                    ArrayRef ref = (ArrayRef) leftOp;
                    System.out.println(ref.getBase());
                }
            }

        }


    }
}
