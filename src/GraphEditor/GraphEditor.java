package GraphEditor;

import FunctionalNetwork.Edge;
import GraphEditor.States.PaintState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * Класс отрисовки графического представления графа.
 */
public class GraphEditor extends JPanel implements Observable, MouseMotionListener {
    PaintState paintState  = PaintState.wait;       // Модель отрисовки (Ожидание, Полностью, (Часть*))

    boolean showNodeTitles = true;                  // Показывать имена вершин.
    boolean showNodeWeights = true;                 // Показывать веса вершин.
    boolean showEdgeParameters = true;              // Показывать параметры ребер.

    boolean addEdge = false;

    // Наблюдатели для информирования о действиях на канвасе
    private List<Observer> observers = new ArrayList<Observer>();

    // Вектор с распределенными по слоям вершинам. Для отрисовки графа и выбора размера вершин.
    Vector<Vector<Integer>> layers = new Vector<Vector<Integer>>();
    int maxOnLayer; // Максимальное количество вершин в слое.
    int nodesCount; // Количество вершин.
    int nodeSize = 50;   // Размер вершины в пикселях по умолчанию. Размер вершины считается в функции.
    Vector<Edge> edges;         // Список с данными о ребрах. Ссылка на список объекта JFrame.
    Vector<Double> nodeWeights; // Список с данными о весах. Ссылка на список объекта JFrame.
    Vector<String> nodeNames;   // Список с данными об именах. Ссылка на список объекта JFrame.

    Vector<Ellipse2D> nodes = new Vector<Ellipse2D>();                  // Граф. представление вершин.
    Vector<Rectangle2D> nodeWeightsText = new Vector<Rectangle2D>();    // Граф. представление областей с текстом весов.
    Vector<Line2D> edgeArrows = new Vector<Line2D>();                   // Граф. представление линий ребер.
    Vector<Shape> edgeArrowHeads = new Vector<Shape>();                 // Стрелка направления ребра.
    Vector<Rectangle2D> edgeParameters = new Vector<Rectangle2D>();     // Граф. представление областей с текстом параметров.

    Vector<Integer> markedNodes;

    Line2D newEdge;
    Shape newEdgeHead;

    Vector<Integer> selectedNodes = new Vector<Integer>();  // Список выбранных вершин.
    Vector<Integer> selectedEdge = new Vector<Integer>();   // Список выбранных ребер.

    // Отступ относительно клика мыши и левым верхним углом эллипса вершины.
    double xDiff = 0;
    double yDiff = 0;

    JPopupMenu popupMenuNode;
    JPopupMenu popupMenuEdge;
    JPopupMenu popupMenuCanvas;

    public GraphEditor() {
        setBackground(Color.white);
        initPopupMenus();
        initMouseAdapter();
        addMouseMotionListener(this);

    }

    /**
     * Метод полностью перерисовывает граф.
     */
    public void repaintFullGraph() {
        paintState = PaintState.full;
        repaint();
    }

    public void paint(Graphics g) {
        super.paint(g);
        switch (paintState) {
            case full:
                paintFullGraph(g);
                break;
        }
    }

    /**
     * Метод раскладывает вершины графа на слои для последующией отрисовки.
     * @param parents Родители каждой вершины.
     */
    private void getLayers(Vector<Integer[]> parents) {
        if (parents == null) {
            maxOnLayer = 0;
            nodesCount = 0;
            return;
        }
        layers.clear();
        int layersElementCount = 0;
        layers.add(new Vector<Integer>());
        for (int i = 0; i < parents.size(); i++) {
            if (parents.get(i).length == 0) {
                layers.get(0).add(i + 1);
                layersElementCount++;
            }
        }
        while (layersElementCount != parents.size()) {
            layers.add(new Vector<Integer>());
            boolean next = false;
            for (int i = 0; i < parents.size(); i++) {
                if ((parents.get(i).length > 0) && (!previousLayersContain(i + 1, layers.size() - 1))) {
                    Integer[] p = parents.get(i);
                    for (int j = 0; j < p.length; j++) {
                        if (!previousLayersContain(p[j], layers.size() - 1)) {
                            next = true;
                            break;
                        }
                    }
                    if (next) {
                        next = false;
                        continue;
                    }
                    else {
                        layers.lastElement().add(i + 1);
                        layersElementCount++;
                    }
                }
            }
        }
        nodesCount = parents.size();
        maxOnLayer = 0;
        for (int i = 0; i < layers.size(); i++) {
            int max = Collections.max(layers.get(i));
            maxOnLayer = max > maxOnLayer ? max : maxOnLayer;
        }
    }

    /**
     * Метод проверяет, содержал ли предыдущие слои обозначенную вершину.
     * @param id ИД вершины.
     * @param currentLayer До какого уровня проверять.
     * @return true, если содержит.
     */
    private boolean previousLayersContain(int id, int currentLayer) {
        for (int i = 0; i < currentLayer; i++) {
            if (layers.get(i).contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Полностью отрисовывает представление графа.
     * @param g
     */
    private void paintFullGraph(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        FontMetrics metrics = g.getFontMetrics();
        Color color;
        for (int i = 0; i < nodes.size(); i++) {
            color = selectedNodes.contains(i) ? Color.red : markedNodes.contains(i) ? Color.blue : Color.black;
            g2.setColor(color);
            g2.draw(nodes.get(i));
            if (showNodeTitles) {
                g2.drawString(nodeNames.get(i), (int) nodes.get(i).getCenterX(), (int) nodes.get(i).getCenterY());
            }
            if (showNodeWeights) {
                g2.drawString(nodeWeights.get(i).toString(), (int) nodeWeightsText.get(i).getX(),
                        (int) nodeWeightsText.get(i).getY() + metrics.getHeight());
            }
        }

        for (int i = 0; i < edges.size(); i++) {
            color = selectedEdge.contains(i) ? Color.red : Color.black;
            g2.setColor(color);
            if (showEdgeParameters) {
                String param = edges.get(i).parametersToString();
                g2.drawString(param, (int) edgeParameters.get(i).getX(), (int) edgeParameters.get(i).getY() + metrics.getHeight());
            }
            g2.draw(edgeArrows.get(i));
            g2.draw(edgeArrowHeads.get(i));
        }
        if (newEdge != null) {
            g2.draw(newEdge);
            g2.draw(newEdgeHead);
        }
    }

    /**
     * Выделение ноды.
     * @param nodeID ИД ноды.
     */
    public void selectNode(int nodeID) {
        selectedNodes.clear();
        selectedEdge.clear();
        if (nodeID >= 0) {
            selectedNodes.add(nodeID);
        }
        repaintFullGraph();
    }

    /**
     * Выделение ребра.
     * @param edgeID ИД ребра.
     */
    public void selectEdge(int edgeID) {
        selectedNodes.clear();
        selectedEdge.clear();
        if (edgeID >= 0) {
            selectedEdge.add(edgeID);
            selectedNodes.add(edges.get(edgeID).parent - 1);
            selectedNodes.add(edges.get(edgeID).child - 1);
        }
        repaintFullGraph();
    }

    /**
     * Снятие выделения со всех элементов.
     */
    public void deselectAll() {
        selectedNodes.clear();
        selectedEdge.clear();
        repaintFullGraph();
    }

    /**
     * Обновление данных о графе.
     * @param nodeNames Имена вершин.
     * @param nodeWeights Веса вершин.
     * @param edges Параметры ребер.
     */
    public void updateValues(Vector<String> nodeNames, Vector<Double> nodeWeights,
                             Vector<Edge> edges, Vector<Integer> markedNodes) {
        this.nodeNames = nodeNames;
        this.nodeWeights = nodeWeights;
        this.edges = edges;
        this.markedNodes = markedNodes;
        nodesCount = nodeNames.size();
    }

    /**
     * Сброс всех данных о графе.
     */
    public void clearValues() {
        maxOnLayer = 0;
        nodesCount = 0;
        nodeSize = 50;
        edges = null;
        nodeWeights = null;
        nodeNames = null;

        nodes.clear();
        nodeWeightsText.clear();
        edgeArrows.clear();
        edgeArrowHeads.clear();
        edgeParameters.clear();

        selectedNodes.clear();
        selectedEdge.clear();
        layers = new Vector<Vector<Integer>>();
    }

    /**
     * Возвращает представление графа.
     * @return вектор коэфициентов относительного положения вершин.
     */
    public Vector<Double[]> getNodePositions() {
        Vector<Double[]> result = new Vector<Double[]>();
        for (int i = 0; i < nodesCount; i++) {
            double x = nodes.get(i).getX() / getWidth();
            double y = nodes.get(i).getY() / getHeight();
            Double[] pos = {x, y};
            result.add(pos);
        }
        return result;
    }

    /**
     * Создает автоматическое представление графа.
     * @param parents
     */
    public void createGraph(Vector<Integer[]> parents) {
        if (nodeNames.isEmpty()) {
            return;
        }
        calculateElementPositions(parents);
    }

    /**
     * Загружает представление для отображения.
     * @param parents вектор родителей вершин.
     *                Необходим для определения размера вершины относительно их количества на слое.
     * @param nodePositions вектор коэфициентов относительного положения вершин.
     */
    public void loadGraph(Vector<Integer[]> parents, Vector<Double[]> nodePositions) {
        if (nodeNames.isEmpty()) {
            return;
        }
        getLayers(parents);

        int width = getWidth();
        int height = getHeight();

        int k = maxOnLayer > layers.size() ? maxOnLayer : layers.size();
        nodeSize = k < 7 ? height / 7 : height / k;
        nodeSize = nodeSize == 0 ? 70 : nodeSize;

        nodes.clear();
        nodeWeightsText.clear();
        for (int i = 0; i < this.nodeNames.size(); i++) {
            double x = nodePositions.get(i)[0] * width;
            double y = nodePositions.get(i)[1] * height;
            nodes.add(new Ellipse2D.Double(x, y, nodeSize, nodeSize));
            nodeWeightsText.add(getNodeWeightsText(nodes.lastElement(), nodeWeights.get(i)));
        }

        calculateEdges();
    }

    /**
     * Просчитывает положение элементов на канвасе.
     * @param parents вектор родителей вершин.
     *                Необходим для определения размера вершины относительно их количества на слое.
     */
    private void calculateElementPositions(Vector<Integer[]> parents) {
        getLayers(parents);

        int width = getWidth();
        int height = getHeight();

        int k = maxOnLayer > layers.size() ? maxOnLayer : layers.size();
        nodeSize = k < 7 ? height / 7 : height / k;
        int indentX = width / (layers.size() + 1);

        nodes.clear();
        nodeWeightsText.clear();

        nodes.setSize(nodesCount);
        nodeWeightsText.setSize(nodesCount);

        for (int i = 0; i < layers.size(); i++) {
            Vector<Integer> layer = layers.get(i);
            int indentY = height / (layer.size() + 1);
            for (int j = 0; j < layer.size(); j++) {
                int x = indentX * (i + 1) - nodeSize / 2;
                int y = indentY * (j + 1) - nodeSize / 2;
                int nodeID = layer.get(j) - 1;
                nodes.set(nodeID, new Ellipse2D.Double(x, y, nodeSize, nodeSize));
                nodeWeightsText.set(nodeID, getNodeWeightsText(nodes.get(nodeID), nodeWeights.get(nodeID)));
            }
        }

        calculateEdges();
    }

    private Rectangle2D getNodeWeightsText(Ellipse2D node, Double weight) {
        FontMetrics metrics = getGraphics().getFontMetrics();
        return new Rectangle2D.Double(node.getX(), node.getY() - 10 - metrics.getHeight(),
                metrics.stringWidth(weight.toString()), metrics.getHeight());
    }

    /**
     * Определяет графическое представление ребер относительно вершин.
     */
    private void calculateEdges() {

        edgeArrows.clear();
        edgeArrowHeads.clear();
        edgeParameters.clear();
        for (int i = 0; i < this.edges.size(); i++) {
            edgeArrows.add(getEdgeLine(nodes.get(this.edges.get(i).parent - 1), nodes.get(this.edges.get(i).child - 1)));

            edgeParameters.add(getEdgeParametersRect(edgeArrows.lastElement(), edges.get(i)));
            edgeArrowHeads.add(getArrowHeadShape(edgeArrows.lastElement()));
        }
    }

    private Rectangle2D getEdgeParametersRect(Line2D line, Edge edge) {
        FontMetrics metrics = getGraphics().getFontMetrics();
        int centerX = (int) (line.getX1() + line.getX2()) / 2;
        int centerY = (int) (line.getY1() + line.getY2()) / 2;
        String param = edge.parametersToString();
        return new Rectangle2D.Double(centerX - 20, centerY - 20 - metrics.getHeight(),
                metrics.stringWidth(param), metrics.getHeight());
    }

    /**
     * Создает стрелку направления.
     * @param line Линия, для которой строится стрелка.
     * @return повернутый относительно линии Shape.
     */
    public static Shape getArrowHeadShape(Line2D line) {

        double x1 = line.getX1(); double y1 = line.getY1();
        double x2 = line.getX2(); double y2 = line.getY2();

        Path2D arrowHead = new Path2D.Double();
        arrowHead.moveTo(6,6);
        arrowHead.lineTo(12, 0);
        arrowHead.lineTo(6,-6);

        double midX = (x1 + x2) / 2.0;
        double midY = (y1 + y2) / 2.0;

        double rotate = Math.atan2(y2 - y1, x2 - x1);

        AffineTransform transform = new AffineTransform();
        transform.translate(midX, midY);
        transform.rotate(rotate);

        return transform.createTransformedShape(arrowHead);
    }

    /**
     * Определяет линию от вершины до вершины.
     * @param nodeParent Вершина, из которой исходит ребро.
     * @param nodeChild Вершина, в которую входит ребро.
     * @return Объект линии.
     */
    private Line2D getEdgeLine(Ellipse2D nodeParent, Ellipse2D nodeChild) {
        double pX = nodeParent.getCenterX();    double pY = nodeParent.getCenterY();
        double cX = nodeChild.getCenterX();     double cY = nodeChild.getCenterY();
        double a = cY - pY;             double b = cX - pX;     double c = Math.sqrt(a * a + b * b);
        double k = nodeSize / 2.0 / c;  double a2 = a * k;      double b2 = b * k;

        return new Line2D.Double(pX + b2, pY + a2, cX - b2, cY - a2);
    }

    /**
     * Пересчитывает положение при перетаскивании относительно текущего положения мыши.
     * @param x мыши.
     * @param y мыши.
     */
    private void recalculateNodePosition(int x, int y) {
        int nodeID = selectedNodes.lastElement();
        Ellipse2D node = nodes.get(nodeID);
        Ellipse2D newNode = new Ellipse2D.Double(x - xDiff, y - yDiff, node.getHeight(), node.getHeight());
        nodes.set(nodeID, newNode);
        nodeWeightsText.set(nodeID, getNodeWeightsText(nodes.get(nodeID), nodeWeights.get(nodeID)));

        calculateEdges();
    }



    /**
     * Обновляет отрисовку.
     */
    public void update() {
        if (nodeNames == null) {
            System.out.println("Отсутсвуют вершины для отрисовки.");
            return;
        }
        repaintFullGraph();
    }

    /**
     * Включение/Выключение отображения имен вершин.
     * @param showNodeTitles Вкл/Выкл.
     */
    public void showNodeTitles(boolean showNodeTitles) {
        this.showNodeTitles = showNodeTitles;
        repaintFullGraph();
    }

    /**
     * Включение/Выключение отображения весов вершин.
     * @param showNodeWeights Вкл/Выкл.
     */
    public void showNodeWeights(boolean showNodeWeights) {
        this.showNodeWeights = showNodeWeights;
        repaintFullGraph();
    }

    /**
     * Включение/Выключение отображения параметров рёбер.
     * @param showEdgeParameters Вкл/Выкл.
     */
    public void showEdgeParameters(boolean showEdgeParameters) {
        this.showEdgeParameters = showEdgeParameters;
        repaintFullGraph();
    }

    public void mouseMoved(MouseEvent evt) {
        if (addEdge) {
            int nodeID = selectedNodes.lastElement();
            Ellipse2D nodeParent = nodes.get(nodeID);
            double pX = nodeParent.getCenterX();    double pY = nodeParent.getCenterY();
            double cX = evt.getX();                      double cY = evt.getY();
            double a = cY - pY;                     double b = cX - pX;     double c = Math.sqrt(a * a + b * b);
            double k = nodeSize / 2.0 / c;          double a2 = a * k;      double b2 = b * k;
            newEdge = new Line2D.Double(pX + b2, pY + a2, cX, cY);
            newEdgeHead = getArrowHeadShape(newEdge);
            repaintFullGraph();
        }
    }

    /**
     * Действие при перетаскивании мыши.
     * Перемещение выделенной вершины.
     * @param evt
     */
    public void mouseDragged(MouseEvent evt) {
        if (selectedNodes.size() == 1) {
            int x = evt.getX();
            int y = evt.getY();
            recalculateNodePosition(x, y);
            repaintFullGraph();
        }
    }

    /**
     * Инициализирует управление мышью для канваса.
     */
    private void initMouseAdapter() {
        addMouseListener(new MouseAdapter() {
            /**
             * Определение вершины по положению мыши.
             * @param x мыши.
             * @param y мыши.
             * @return ИД ноды. -1, если на (x,y) нет вершин.
             */
            private int identifyNode(int x, int y) {
                for (int i = 0; i < nodesCount; i++) {
                    if (nodes.get(i).contains(x, y)) {
                        return i;
                    }
                    if (nodeWeightsText.get(i).contains(x, y) && showNodeWeights) {
                        return i;
                    }
                }
                return -1;
            }

            /**
             * Определение ребра по положению мыши.
             * @param x мыши.
             * @param y мыши.
             * @return ИД ребра. -1, если на (x,y) нет ребра.
             */
            private int identifyEdge(int x, int y) {
                for (int i = 0; i < edges.size(); i++) {
                    if (edgeArrows.get(i).contains(x, y)) {
                        return i;
                    }
                    if (edgeParameters.get(i).contains(x, y) && showEdgeParameters) {
                        return i;
                    }
                }
                return -1;
            }

            /**
             * Выделение элемента по клику.
             * @param me
             */
            public void mousePressed(MouseEvent me) {
                if (nodeNames == null) {
                    return;
                }
                int x = me.getX();
                int y = me.getY();
                int nodeID = identifyNode(x, y);
                int edgeID = identifyEdge(x, y);
                if (addEdge) {
                    addEdge = false;
                    newEdge = null;
                    newEdgeHead = null;
                    if (nodeID >= 0) {
                        edges.add(new Edge(selectedNodes.lastElement() + 1, nodeID + 1, 0, 0));
                        edgeArrows.add(getEdgeLine(nodes.get(selectedNodes.lastElement()), nodes.get(nodeID)));
                        edgeArrowHeads.add(getArrowHeadShape(edgeArrows.lastElement()));
                        edgeParameters.add(getEdgeParametersRect(edgeArrows.lastElement(), edges.lastElement()));
                        notifyObserversOnUpdatedData();
                    }
                    deselectAll();
                    repaintFullGraph();
                    return;
                }
                if (nodeID >= 0) {
                    selectNode(nodeID);
                    xDiff = x - nodes.get(selectedNodes.lastElement()).getX();
                    yDiff = y - nodes.get(selectedNodes.lastElement()).getY();
                }
                if (edgeID >= 0) {
                    selectEdge(edgeID);
                }
                if (nodeID * edgeID > 0) {
                    deselectAll();
                }
            }

            public void mouseReleased(MouseEvent me) {
                if (nodeNames == null) {
                    return;
                }
                if (me.isPopupTrigger()) {
                    if (selectedNodes.size() == 1) {
                        popupMenuNode.show(GraphEditor.this, me.getX(), me.getY());
                    }
                    if (selectedNodes.size() == 0) {
                        popupMenuCanvas.show(GraphEditor.this, me.getX(), me.getY());
                        xDiff = me.getX();
                        yDiff = me.getY();
                    }
                    if (selectedEdge.size() == 1) {
                        popupMenuEdge.show(GraphEditor.this, me.getX(), me.getY());
                    }
                }
            }

            /**
             * Редактирование элементов по двойному клику.
             * @param me
             */
            @Override
            public void mouseClicked(MouseEvent me){
                if (nodeNames == null) {
                    return;
                }
                int x = me.getX();
                int y = me.getY();
                int nodeID = identifyNode(x, y);
                int edgeID = identifyEdge(x, y);
                switch (me.getClickCount()) {
                    case 2:
                        if (nodeID >= 0) {
                            notifyObservers("node", nodeID);
                        }
                        if (edgeID >= 0) {
                            notifyObservers("edge", edgeID);
                        }
                        break;
                }
            }
        });
    }

    @Override
    public void registerObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(String k, int l) {
        for (Observer observer : observers) {
            observer.configureElement(k, l);
        }
    }

    @Override
    public void notifyObserversOnUpdatedData() {
        for (Observer observer : observers) {
            observer.updateData();
        }
    }

    private void initPopupMenus() {
        popupMenuNode = new JPopupMenu();
        JMenuItem item;
        popupMenuNode.add(item = new JMenuItem("Пометить/Снять пометку"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (markedNodes.contains(selectedNodes.lastElement())) {
                    markedNodes.remove(selectedNodes.lastElement());
                }
                else {
                    markedNodes.add(selectedNodes.lastElement());
                }
                notifyObserversOnUpdatedData();
            }
        });

        popupMenuNode.add(item = new JMenuItem("Добавить ребро"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addEdge = true;
            }
        });
        popupMenuNode.add(item = new JMenuItem("Удалить вершину"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int nodeID = selectedNodes.lastElement();
                nodeWeights.remove(nodeID);
                nodeNames.remove(nodeID);
                nodes.remove(nodeID);
                nodeWeightsText.remove(nodeID);
                nodesCount--;
                boolean stop = false;

                while(!stop) {
                    for (int i = 0; i < edges.size(); i++) {
                        if ((edges.get(i).parent - 1 == nodeID) || (edges.get(i).child - 1 == nodeID)) {
                            edges.remove(i);
                            edgeArrows.remove(i);
                            edgeArrowHeads.remove(i);
                            edgeParameters.remove(i);
                            break;
                        }
                    }
                    stop = true;
                    for (int i = 0; i < edges.size(); i++) {
                        if ((edges.get(i).parent - 1 == nodeID) || (edges.get(i).child - 1 == nodeID)) {
                            stop = false;
                        }
                    }
                }
                for (int i = 0; i < edges.size(); i++) {
                    if (edges.get(i).parent - 1 >= nodeID) {
                        edges.get(i).parent--;
                    }
                    if (edges.get(i).child - 1 >= nodeID) {
                        edges.get(i).child--;
                    }
                }
                notifyObserversOnUpdatedData();
                repaintFullGraph();
            }
        });

        popupMenuCanvas = new JPopupMenu();
        popupMenuCanvas.add(item = new JMenuItem("Добавить вершину"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                nodeWeights.add(Double.NaN);
                nodeNames.add("");
                nodes.add(new Ellipse2D.Double(xDiff, yDiff, nodeSize, nodeSize));
                nodeWeightsText.add(getNodeWeightsText(nodes.lastElement(), nodeWeights.lastElement()));
                nodesCount++;
                notifyObserversOnUpdatedData();
                repaintFullGraph();
            }
        });

        popupMenuEdge = new JPopupMenu();
        popupMenuEdge.add(item = new JMenuItem("Удалить ребро"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int edgeID = selectedEdge.lastElement();
                edges.remove(edgeID);
                edgeArrows.remove(edgeID);
                edgeArrowHeads.remove(edgeID);
                edgeParameters.remove(edgeID);
                notifyObserversOnUpdatedData();
                repaintFullGraph();
            }
        });
    }
}