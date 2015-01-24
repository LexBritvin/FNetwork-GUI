package GraphEditor;

/**
 * Created by Александр on 19.01.2015.
 */
public interface Observer {
    void configureElement(String x, int y);
    void updateData();
}
