import dev.compL.iitmandi.utils.ConnectionGraph;
import dev.compL.iitmandi.utils.ConnectionGraphNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ConnectionGraphNodeTest {

    @Test
    void nodeEqualityPass(){
        assertEquals(new ConnectionGraphNode("name", ConnectionGraph.NodeType.OBJECT, 2), new ConnectionGraphNode("name", ConnectionGraph.NodeType.OBJECT, 2));
    }

//    @Test
//    void nodeEqualityFail(){
//        assertEquals(new ConnectionGraphNode("name", ConnectionGraph.NodeType.OBJECT, 2), new ConnectionGraphNode("name", ConnectionGraph.NodeType.FIELD, 2));
//    }
}
