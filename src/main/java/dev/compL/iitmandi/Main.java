package dev.compL.iitmandi;

import dev.compL.iitmandi.intraAnalysis.IntraAnalysis;
import dev.compL.iitmandi.methodToJimple.MethodToJimple;
import dev.compL.iitmandi.scalar_replacement.ScalarReplacement;
import dev.compL.iitmandi.scalar_replacement.ScalarTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(IntraAnalysis.class);

        if (args.length == 0) {
            logger.error("No arguments provided");
            System.err.println("No argument provided. Read documentation to know about use cases");
            return;
        }
        logger.info("Starting Analysis with args: {}", Arrays.toString(args));

        String taskName = args[0];
        String[] restOfTheArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (taskName) {
            case "jimple":
                // to play with Jimple body of methods
                logger.info("Executing method-to-jimpleStmts");
                MethodToJimple.main(args);
                break;
            case "visualise-method-cfg":
                // visualise the CFG of a method
                logger.info("Executing visualisation");
                //TODO
                break;
            case "intra":
                // perform intra procedural analysis on the given class files
                logger.info("Executing Intra-Procedural Analysis");
                IntraAnalysis.main(restOfTheArgs);
                break;
            case "inter":
                // perform inter-procedural analysis on given class files.
                // Need to supply the result of Inter-procedural Analysis.
                logger.info("Executing Inter-Procedural Analysis");
                //TODO
                break;
            case "scalar":
                // Perform scalar replacement over given class files
                // Need to provide the objects that are not escaping as arguments
                logger.info("Perform Scalar Replacement");
                ScalarReplacement.main(restOfTheArgs);
                break;
            case "partial-escape-analysis":
                logger.info("Performing complete Partial Escape analysis");
                // complete partial analysis
                //TODO
                break;
            default:
                System.err.println("Invalid argument");
                break;
        }
    }
}
