package FunctionalNetwork;

/**
 * Created by Александр on 18.01.2015.
 */
public class Edge {
    public Integer parent, child;
    public Double alpha, k;
    public Edge(int x, int y, double alpha, double k) {
        this.parent = x;
        this.child = y;
        this.alpha = alpha;
        this.k = k;
    }
    public String edgeToString() {
        return parent + " -> " + child;
    }
    public String parametersToString() {
        return alpha + ", " + k;
    }
}
