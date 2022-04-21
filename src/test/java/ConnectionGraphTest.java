import dev.compL.iitmandi.utils.ConnectionGraph;
import dev.compL.iitmandi.utils.ConnectionGraphNode;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.options.Options;

import java.io.File;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionGraphTest {

    private JimpleBody methodBody;
    private ConnectionGraph graph;

    void preprocessTest(){
        String sourceDir = System.getProperty("user.dir") + File.separator + "example" + File.separator + "Tests";
        String className = "ConnectionGraphExample";
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

        SootClass mainClass = Scene.v().getSootClass(className);
        SootMethod method = mainClass.getMethodByName("main");

        methodBody = (JimpleBody) method.retrieveActiveBody();
    }

    @Test
    void addEdgeTest(){
        ConnectionGraph graph1 = new ConnectionGraph();
        graph1.addEdge(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF,-1), new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1), ConnectionGraph.EdgeType.POINTSTO);

        ConnectionGraph graph2 = new ConnectionGraph();

        graph2.getForwardPointsToEdge().put(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF, -1), new HashSet<>());
        graph2.getForwardPointsToEdge().get(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF, -1)).add(new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1));

        graph2.getReversePointsToEdge().put(new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1), new HashSet<>());
        graph2.getReversePointsToEdge().get(new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1)).add(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF, -1));

        assertEquals(graph1, graph2);
    }

    @Test
    void removeEdgeTest(){
        ConnectionGraph graph1 = new ConnectionGraph();
        graph1.addEdge(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF,-1), new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1), ConnectionGraph.EdgeType.POINTSTO);

        ConnectionGraph graph2 = new ConnectionGraph();

        graph2.getForwardPointsToEdge().put(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF, -1), new HashSet<>());
        graph2.getForwardPointsToEdge().get(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF, -1)).add(new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1));

        graph2.getReversePointsToEdge().put(new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1), new HashSet<>());
        graph2.getReversePointsToEdge().get(new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1)).add(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF, -1));

        graph1.removeEdge(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF,-1), new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1), ConnectionGraph.EdgeType.POINTSTO);

        graph2.getForwardPointsToEdge().remove(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF,-1));
        graph2.getReversePointsToEdge().remove(new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1));
        assertEquals(graph1, graph2);
    }

    @Test
    void copyTest(){
        ConnectionGraph graph1 = new ConnectionGraph();
        graph1.addEdge(new ConnectionGraphNode("ref", ConnectionGraph.NodeType.REF,-1), new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1), ConnectionGraph.EdgeType.POINTSTO);


//        ConnectionGraph graph2 = SerializationUtils.clone(graph1);
        ConnectionGraph graph2 = new ConnectionGraph();
        graph2.extend(graph1);
        assertEquals(graph1, graph2);

        graph1.addEdge(new ConnectionGraphNode("ref2", ConnectionGraph.NodeType.REF,-1), new ConnectionGraphNode("obj1", ConnectionGraph.NodeType.OBJECT,1), ConnectionGraph.EdgeType.POINTSTO);

        assertNotEquals(graph1, graph2);
    }

}
