package Windows;

import FunctionalNetwork.Edge;
import FunctionalNetwork.FunctionalNetwork;
import FunctionalNetwork.Vertex;
import GraphEditor.GraphEditor;
import GraphEditor.Observer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.Vector;

/**
 * TODO: Откомментировать код.
 */
public class Frame extends JFrame implements Observer {
    // Размеры окна.
    int windowWidth;
    int windowHeight;
    // Положение окна.
    int locationX;
    int locationY;
    // Отступ элементов.
    int indent = 20;

    // Текст под JList о том, какой граф отображается.
    JLabel fnetState = new JLabel("Веса не подобраны");
    JMenuBar menuBar = new JMenuBar();

    // Используется для функции "Пересчет вершин". Хранит данные об измененных вершинах.
    Vector<Integer> markedNodes = new Vector<Integer>();

    // Вектора с данными о графе.
    Vector<String> nodeNames;
    Vector<Double> nodeWeights;
    Vector<Edge> edges;

    // Списки с прокруткой в правом углу.
    JLabel nodesListLabel = new JLabel("Вершины:");
    JList nodesList;
    JScrollPane scrollNodesList = new JScrollPane();
    int nodesListWidth = 150;
    int nodesListHeight = 150;

    JLabel edgesListLabel = new JLabel("Ребра:");
    JList edgesList;
    JScrollPane scrollEdgesList = new JScrollPane();
    int edgesListWidth = 150;
    int edgesListHeight = 150;

    // Объекты канваса и функциональной сети.
    GraphEditor graphEditor;
    FunctionalNetwork fnet;

    public Frame() throws HeadlessException {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(null);
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();
        int screenHeight = screenSize.height;
        int screenWidth = screenSize.width;

        windowWidth = screenWidth * 7 / 8;
        windowHeight = screenHeight * 7 / 8;
        locationX = screenWidth * 1 / 16;
        locationY = screenHeight * 1 / 16;

        setSize(windowWidth, windowHeight);
        setLocation(locationX, locationY);

        initMenu();
        initGraphEditor();
        initGraphLists();

        setVisible(true);
    }

    /**
     * Метод инициализирует списки JLists для вершин и ребер.
     */
    private void initGraphLists() {
        nodesList = new JList();
        nodesListLabel.setSize(100, 15);
        nodesListLabel.setLocation(windowWidth - nodesListWidth - 2 * indent + 5, 20);
        scrollNodesList.setViewportView(nodesList);
        scrollNodesList.setSize(nodesListWidth, nodesListHeight);
        scrollNodesList.setLocation(windowWidth - nodesListWidth - 2 * indent, 40);

        // Выделение по 1 клику вершины, по 2 кликам Редактирование вершины.
        nodesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                int id = list.getSelectedIndex();
                if (id < 0) {
                    return;
                }
                switch (evt.getClickCount()) {
                    case 1:
                        graphEditor.selectNode(id);
                        break;

                    case 2:
                        configureNodeDialog(id);
                        break;
                }
            }
        });


        edgesList = new JList();
        edgesListLabel.setSize(100, 15);
        edgesListLabel.setLocation(windowWidth - edgesListWidth - 2 * indent + 5, nodesListHeight + 3 * indent);
        scrollEdgesList.setViewportView(edgesList);

        scrollEdgesList.setSize(edgesListWidth, edgesListHeight);
        scrollEdgesList.setLocation(windowWidth - edgesListWidth - 2 * indent, nodesListHeight + 4 * indent);
        // Выделение по 1 клику ребра, по 2 кликам Редактирование ребра.
        edgesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                int id = list.getSelectedIndex();
                if (id < 0) {
                    return;
                }
                switch (evt.getClickCount()) {
                    case 1:
                        graphEditor.selectEdge(id);
                        break;

                    case 2:
                        configureEdgeDialog(id);
                        break;
                }
            }
        });
        // Добавление текста о состоянии графа.
        fnetState.setLocation(windowWidth - edgesListWidth - 2 * indent + 5, nodesListHeight * 2 + 5 * indent);
        fnetState.setSize(150, 15);

        add(fnetState);

        add(nodesListLabel);
        add(scrollNodesList);

        add(edgesListLabel);
        add(scrollEdgesList);
    }

    /**
     * Инициализация канваса.
     */
    private void initGraphEditor() {
        graphEditor = new GraphEditor();
        graphEditor.setSize(windowWidth - nodesListWidth - 4 * indent, windowHeight - 5 * indent);
        graphEditor.setLocation(indent, indent);

        add(graphEditor);
        graphEditor.registerObserver(this);
    }

    /**
     * Инициализация меню и его функционала.
     */
    private void initMenu() {
        JMenu menu;
        JMenuItem menuItem;
        menu = new JMenu("Файл");
        menuBar.add(menu);
        // Создание нового файла с графом.
        menuItem = new JMenuItem("Создать");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser createFnet = new JFileChooser();
                createFnet.setFileFilter(new FileNameExtensionFilter("Database source", "db"));
                createFnet.setSelectedFile(new File("my_fnet.db"));
                createFnet.setCurrentDirectory(new File(System.getProperty("user.dir")));
                int result = createFnet.showSaveDialog(Frame.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = createFnet.getSelectedFile();
                    try {
                        FileWriter fw = new FileWriter(createFnet.getSelectedFile());
                        fw.close();
                        loadFunctionalNetwork(createFnet.getSelectedFile().getPath());
                    }
                    catch (Exception ex) {
                        System.out.println(ex);
                    }

                    menuBar.getMenu(1).getItem(1).setSelected(true);
                }
                menuBar.getMenu(1).getItem(0).setSelected(true);

            }
        });
        menu.add(menuItem);

        // Загрузка существующего графа.
        menuItem = new JMenuItem("Загрузить");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser loadFnet = new JFileChooser();
                loadFnet.setFileFilter(new FileNameExtensionFilter("Database source", "db"));
                loadFnet.setCurrentDirectory(new File(System.getProperty("user.dir")));

                int result = loadFnet.showOpenDialog(Frame.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String path = loadFnet.getSelectedFile().getAbsolutePath().replace("\\", "/");
                    loadFunctionalNetwork(path);
                    // Устанавливаем в меню режим "Просмотр".
                    menuBar.getMenu(1).getItem(0).setSelected(true);
                }
            }
        });
        menu.add(menuItem);

        // Сохранение графического представления графа.
        menuItem = new JMenuItem("Сохранить");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Vector<Vertex> vertices = collectVertexData();
                    fnet.writeDBVertices(nodeNames, vertices, "vertices");
                    fnet.writeDBEdges(edges);
                    fnet.writeDBNodePositions(graphEditor.getNodePositions());
                }
                catch (Exception ex) {
                    System.out.println("Не удалось записать ф. сеть: " + ex);
                }
            }
        });
        // По умолчанию недоуступно (при загрузке окна не загружен ни один граф).
        menuItem.setEnabled(false);
        menu.add(menuItem);

        // По умолчанию недоуступно (при загрузке окна не загружен ни один граф).
        menuItem.setEnabled(false);
        menu.add(menuItem);
        // Меню Вид для настройки особенностей представления.
        menu = new JMenu("Вид");
        menu.setEnabled(false);
        menuBar.add(menu);

        // Включение/Выключение отображения надписей на графе.
        menuItem = new JCheckBoxMenuItem("Имена вершин", true);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBoxMenuItem mItem = (JCheckBoxMenuItem) e.getSource();
                graphEditor.showNodeTitles(mItem.isSelected());
            }
        });
        menu.add(menuItem);

        menuItem = new JCheckBoxMenuItem("Веса вершин", true);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBoxMenuItem mItem = (JCheckBoxMenuItem) e.getSource();
                graphEditor.showNodeWeights(mItem.isSelected());
            }
        });
        menu.add(menuItem);
        menuItem = new JCheckBoxMenuItem("Параметры рёбер", true);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBoxMenuItem mItem = (JCheckBoxMenuItem) e.getSource();
                graphEditor.showEdgeParameters(mItem.isSelected());
            }
        });
        menu.add(menuItem);
        // Подсчет вершин графа.
        menu = new JMenu("Функции");
        menu.setEnabled(false);
        menuBar.add(menu);
        menuItem = new JMenuItem("Подобрать веса");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    fnet.writeDBVertices(nodeNames, collectVertexData(), "vertices");
                    fnet.writeDBEdges(edges);
                }
                catch (Exception e) {}
                fnet.assortValues();
                updateWeightValues();
                loadGraphLists();
                graphEditor.updateValues(nodeNames, nodeWeights, edges, markedNodes);
            }
        });
        menu.add(menuItem);
        menuItem = new JMenuItem("Пересчитать веса");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    fnet.writeDBVertices(nodeNames, collectVertexData(), "vertices");
                    fnet.writeDBEdges(edges);
                }
                catch (Exception e) {}
                int[] ids = new int[markedNodes.size()];
                double[] newWeights = new double[markedNodes.size()];
                for (int i = 0; i < newWeights.length; i++) {
                    ids[i] = markedNodes.get(i) + 1;
                    newWeights[i] = nodeWeights.get(ids[i] - 1);
                }
                fnet.assortValuesGradient(ids, newWeights);
                updateWeightValues();
                graphEditor.updateValues(nodeNames, nodeWeights, edges, markedNodes);
                graphEditor.update();
            }
        });
        menu.add(menuItem);

        setJMenuBar(menuBar);
    }

    /**
     * Загружает ф. сеть из файла и отображает ее.
     * @param file Файл *.db ф. сети.
     */
    private void loadFunctionalNetwork(String file) {
        setTitle(file);
        if (fnet != null) {
            fnet.CloseDB();
            graphEditor.clearValues();
            markedNodes.clear();
        }
        fnet = new FunctionalNetwork(file);
        // Включаем доступ к "Сохранить".
        menuBar.getMenu(0).getItem(2).setEnabled(true);
        // Включаем доступ к меню "Вид" и "Функции".
        menuBar.getMenu(1).setEnabled(true);
        menuBar.getMenu(2).setEnabled(true);

        nodeNames = fnet.getVertexNames();
        updateWeightValues();
        edges = fnet.getEdges();
        Vector<Integer[]> parents = fnet.getVertexParents();
        Vector<Integer[]> children = fnet.getVertexChildren();

        loadGraphLists();
        graphEditor.updateValues(nodeNames, nodeWeights, edges, markedNodes);
        if (fnet.hasNodePositions()) {
            Vector<Double[]> nodePositions = fnet.getNodePositions();
            graphEditor.loadGraph(parents, nodePositions);
        }
        else {
            graphEditor.createGraph(parents);
        }
        graphEditor.update();
    }

    /**
     * Метод обновляет список JList для вершин и ребер.
     */
    private void loadGraphLists() {
        String[] graphListStrings = new String[nodeNames.size()];
        for(int i = 0; i < graphListStrings.length; i++) {
            String changed = markedNodes.contains(i) ? "*" : "";
            nodeNames.get(i);
            nodeWeights.get(i);
            graphListStrings[i] = nodeNames.get(i) + " (" + nodeWeights.get(i) + ") " + changed;
        }
        nodesList.setListData(graphListStrings);

        String[] edgesListStrings = new String[edges.size()];
        for(int i = 0; i < edgesListStrings.length; i++) {
            int id = edges.get(i).parent - 1;
            String parent = nodeNames.get(id);
            id = edges.get(i).child - 1;
            String child = nodeNames.get(id);
            edgesListStrings[i] = parent + " -> " + child + " (" + edges.get(i).parametersToString() + ")";
        }
        edgesList.setListData(edgesListStrings);
    }

    /**
     * Метод интерфейса Observer. Вызывается, когда по элементу графа кликнули 2 раза.
     * Открывает диалоговое окно редактирования элемента.
     * @param type Тип элемента
     * @param elementID ID элемента.
     */
    public void configureElement(String type, int elementID) {
        if (type.equals("node")) {
            configureNodeDialog(elementID);
        }
        if (type.equals("edge")) {
            configureEdgeDialog(elementID);
        }
    }

    @Override
    public void updateData() {
        loadGraphLists();
    }

    /**
     * Вызывает модальное окно редактирования ребра и записывает измененные значения.
     * @param id ИД ребра.
     */
    private void configureEdgeDialog(int id) {
        ConfigureEdge dialog = new ConfigureEdge(edges.get(id));
        dialog.setTitle("Ребро " + edges.get(id).edgeToString());
        if (dialog.showDialog()) {
            loadGraphLists();
            graphEditor.update();
        }
    }

    /**
     * Вызывает модальное окно редактирования вершины и записывает значения.
     * @param id ИД вершины.
     */
    private void configureNodeDialog(int id) {
        ConfigureNode dialog = new ConfigureNode(nodeNames.get(id), nodeWeights.get(id));
        dialog.setTitle("Вершина " + nodeNames.get(id));
        Object[] result;
        if ((result = dialog.showDialog()) != null) {
            nodeNames.set(id, (String) result[0]);
            nodeWeights.set(id, (Double) result[1]);
            loadGraphLists();
            graphEditor.update();
        }
    }

    /**
     * Загружает обновленные данные из подключенного файла ф. сети.
     */
    private void updateWeightValues() {
        if (fnet.isCountedNetwork()) {
            fnetState.setText("Веса подобраны");
            nodeWeights = fnet.getVertexWeights("network");
        }
        else {
            fnetState.setText("Веса не подобраны");
            nodeWeights = fnet.getVertexWeights("vertices");
        }
    }

    private Vector<Vertex> collectVertexData() {
        Vector<Vector<Integer>> parents = new Vector<Vector<Integer>>();
        Vector<Vector<Integer>> children = new Vector<Vector<Integer>>();
        for (int i = 0; i < nodeNames.size(); i++) {
            parents.add(new Vector<Integer>());
            children.add(new Vector<Integer>());
        }
        for (int i = 0; i < edges.size(); i++) {
            int child = edges.get(i).child - 1;
            int parent = edges.get(i).parent - 1;
            parents.get(child).add(parent + 1);
            children.get(parent).add(child + 1);
        }
        Vector<Vertex> vertices = new Vector<Vertex>();
        for (int i = 0; i < nodeNames.size(); i++) {
            int[] p = new int[parents.get(i).size()];
            for (int j = 0; j < p.length; j++) {
                p[j] = parents.get(i).get(j);
            }
            int[] c = new int[children.get(i).size()];
            for (int j = 0; j < c.length; j++) {
                c[j] = children.get(i).get(j);
            }
            vertices.add(new Vertex(p, c, nodeWeights.get(i)));
        }
        return vertices;
    }
}
