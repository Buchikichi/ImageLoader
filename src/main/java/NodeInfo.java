import org.w3c.dom.Node;

public class NodeInfo {
    private final int depth;
    private final Node node;

    public int getDepth() {
        return depth;
    }

    public Node getNode() {
        return node;
    }

    public NodeInfo(int depth, Node node) {
        this.depth = depth;
        this.node = node;
    }
}
