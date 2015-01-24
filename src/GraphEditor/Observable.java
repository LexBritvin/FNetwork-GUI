package GraphEditor;

/**
 * Created by Александр on 19.01.2015.
 */
public interface Observable {
    void registerObserver(Observer o);
    void removeObserver(Observer o);
    void notifyObservers(String k, int l);
    void notifyObserversOnUpdatedData();
}
