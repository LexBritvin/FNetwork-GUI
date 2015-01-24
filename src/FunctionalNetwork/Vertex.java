package FunctionalNetwork;

import java.util.Arrays;

/**
 * Created by Denis on 18.12.2014.
 */
// класс для хранения вершин графа
public class Vertex {
    public int[] p, c;
    public double w;
    public Vertex (int[] p, int[] c, double w){
        this.p = p;
        this.c = c;
        this.w = w;
    }
    public void print(){
        System.out.println("Parents: "+ Arrays.toString(p)+" Children: "+Arrays.toString(c)+" Weight: "+w);
    }
}
