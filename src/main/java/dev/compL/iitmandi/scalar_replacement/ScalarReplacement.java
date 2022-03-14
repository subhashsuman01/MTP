package dev.compL.iitmandi.scalar_replacement;

import dev.compL.iitmandi.intraAnalysis.IntraAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.ReturnStmt;
import soot.options.Options;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.Collections;

public class ScalarReplacement {
    public static void main(String[] args) {


        final Logger logger = LoggerFactory.getLogger(IntraAnalysis.class);

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

        SootMethod initMethod = mainClass.getMethodByName("<init>");
        SootMethod method = mainClass.getMethodByName("methodA");

        JimpleBody methodBody = (JimpleBody) method.retrieveActiveBody();
        JimpleBody initMethodBody = (JimpleBody) initMethod.retrieveActiveBody();


//        new ScalarTransform().internalTransform(methodBody, "jtb", Collections.emptyMap());

//        for (Unit unit: methodBody.getUnits()) {
//            System.out.println(unit);
//        }

    }

}
