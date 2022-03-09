package dev.compL.iitmandi;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import dev.compL.iitmandi.intraAnalysis.IntraAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static void main(String[] args){
        final Logger logger = LoggerFactory.getLogger(IntraAnalysis.class);



        if (args.length == 0){
            System.err.println("No argument provided. Read documentation to know about use cases");
            return;
        }
        String taskName = args[0];
        String[] restOfTheArgs = Arrays.copyOfRange(args, 1, args.length);
        if(taskName.equals("method-to-jimpleStmts")){
            //TODO
        }
        else if(taskName.equals("visualise-method-cfg")){
            //TODO
        }
        else if(taskName.equals("intra")){
            logger.info("hi");
            logger.error("hi");
            IntraAnalysis.main(restOfTheArgs);
        }
        else if (taskName.equals("inter")) {
            //TODO
        }
        else if (taskName.equals("partial-escape-analysis")) {
            //TODO
        }
        else {
            System.err.println("Invalid argument");
        }
    }
}
