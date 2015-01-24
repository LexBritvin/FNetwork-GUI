package FunctionalNetwork;

import java.sql.*;
import java.util.Arrays;
import java.util.Vector;

public class FunctionalNetwork {
    public Connection conn;
    public Statement stat;
    public ResultSet results;
    public Vector<Integer> marked = new Vector<Integer>();
    public static Vector<String> names = new Vector<String>();
    private boolean counted;
    private boolean hasPositions;

    private boolean gradient = false;

    public FunctionalNetwork(String file) {
        // --------ПОДКЛЮЧЕНИЕ К БАЗЕ ДАННЫХ--------
        conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + file);
            //conn = DriverManager.getConnection("jdbc:sqlite:src\\Fnetwork.db");
            //conn = DriverManager.getConnection("jdbc:sqlite:src\\fn.db");
            //conn = DriverManager.getConnection("jdbc:sqlite:src\\fn4.db");
            //conn = DriverManager.getConnection("jdbc:sqlite:src\\fn7.db");
            //conn = DriverManager.getConnection("jdbc:sqlite:src\\fn5.db");
            //conn = DriverManager.getConnection("jdbc:sqlite:src\\fn3.db");
            System.out.println("База Подключена!");
            stat = conn.createStatement();
            results = stat.executeQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'vertices'");
            boolean newFnet = counted = results.getInt(1) == 0;
            if (!newFnet) {
                results = stat.executeQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'network'");
                counted = results.getInt(1) > 0;
                if (counted) {
                    results = stat.executeQuery("SELECT count(*) FROM vertices");
                    int vcount = results.getInt(1);
                    results = stat.executeQuery("SELECT count(*) FROM network");
                    int ncount = results.getInt(1);
                    counted = vcount == ncount;
                }
                results = stat.executeQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'node_positions'");
                hasPositions = results.getInt(1) > 0;
                if (hasPositions) {
                    results = stat.executeQuery("SELECT count(*) FROM node_positions");
                    hasPositions = results.getInt(1) > 0;
                }
            }
            else {
                stat.execute("CREATE TABLE if not exists 'vertices' ('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "'name' VARCHAR, 'parents' VARCHAR, 'children' VARCHAR, 'weight' REAL);");
                stat.execute("CREATE TABLE if not exists 'parameters' ('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "'parent_id' INTEGER, 'child_id' INTEGER, 'alpha' REAL, 'k' INTEGER);");
                counted = false;
            }
        }
        catch (Exception e){
            System.out.println("Ошибка при подключении к БД!");
            System.exit(0);
        }
    }

    public boolean isCountedNetwork() {
        return counted;
    }

    // --------Создание и заполнение таблицы network--------
    public void WriteDB(Vector<Vertex> vertices, String name) throws SQLException
    {
        // создадим таблицу с "идеальной сетью"
        stat.execute("CREATE TABLE if not exists '" + name + "' ('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "'name' VARCHAR, 'parents' VARCHAR, 'children' VARCHAR, 'weight' REAL);");
        // и заполним её
        for (int i = 0; i < vertices.size(); i++) {
            stat.execute("INSERT INTO '" + name + "' ('name', 'parents', 'children', 'weight') VALUES ('" + names.get(i) + "', '" +
                    returnValue(Arrays.toString(vertices.get(i).p)) + "', '" +
                    returnValue(Arrays.toString(vertices.get(i).c)) + "', " +
                    vertices.get(i).w + "); ");
        }
        counted = true;
    }

    public void writeDBVertices(Vector<String> nodeNames, Vector<Vertex> vertices, String name) throws SQLException {
        stat.execute("DROP TABLE IF EXISTS " + name);
        stat.execute("CREATE TABLE if not exists '" + name + "' ('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "'name' VARCHAR, 'parents' VARCHAR, 'children' VARCHAR, 'weight' REAL);");
        for (int i = 0; i < vertices.size(); i++) {
            String weight = Double.toString(vertices.get(i).w);
            weight = weight.equals("NaN") ? "null" : weight;
            stat.execute("INSERT INTO '" + name + "' ('name', 'parents', 'children', 'weight') VALUES ('" + nodeNames.get(i) + "', '" +
                    returnValue(Arrays.toString(vertices.get(i).p)) + "', '" +
                    returnValue(Arrays.toString(vertices.get(i).c)) + "', " +
                    weight + "); ");
        }
    }

    public void writeDBEdges(Vector<Edge> edges) throws SQLException {
        stat.execute("DROP TABLE IF EXISTS parameters");
        stat.execute("CREATE TABLE if not exists 'parameters' ('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "'parent_id' INTEGER, 'child_id' INTEGER, 'alpha' REAL, 'k' INTEGER);");
        for (int i = 0; i < edges.size(); i++) {
            stat.execute("INSERT INTO 'parameters' ('parent_id', 'child_id', 'alpha', 'k') VALUES ('" +
                    edges.get(i).parent + "', '" +
                    edges.get(i).child + "', '" +
                    edges.get(i).alpha.toString() + "', " +
                    edges.get(i).k.toString() + "); ");
        }
    }

    public void writeDBNodePositions(Vector<Double[]> positions) throws SQLException
    {
        stat.execute("DROP TABLE IF EXISTS node_positions");
        // создадим таблицу с "идеальной сетью"
        stat.execute("CREATE TABLE if not exists 'node_positions' ('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "'x' REAL, 'y' REAL);");
        // и заполним её
        for (int i = 0; i < positions.size(); i++) {
            stat.execute("INSERT INTO 'node_positions' ('x', 'y') VALUES (" +
                    "'" + positions.get(i)[0] + "'," +
                    "'" + positions.get(i)[1] + "');");
        }
        counted = true;
    }

    public boolean hasNodePositions() {
        return hasPositions;
    }
    public Vector<Double[]> getNodePositions(){
        try{

            Vector<Double[]> v = new Vector<Double[]>();
            results = stat.executeQuery("SELECT * FROM node_positions");
            while (results.next()) {
                Double[] res = new Double[2];
                res[0] = results.getDouble("x");
                res[1] = results.getDouble("y");
                v.add(res);
            }
            return v;
        }
        catch (Exception e){
            System.out.println("getNodePositions: " + e.toString());
            return null;
        }
    }

    // -------- Вывод таблиц--------
    public void PrintDB() throws ClassNotFoundException, SQLException
    {
        System.out.println("ВЫВОД ТАБЛИЦ:\nТаблица parameters\n-----------------------------------------");
        results = stat.executeQuery("SELECT * FROM parameters");
        while(results.next())
        {
            int id = results.getInt("id");
            int parent = results.getInt("parent_id");
            int child = results.getInt("child_id");
            double alpha = results.getDouble("alpha");
            int k = results.getInt("k");
            System.out.println("ID = " + id + " | parent = " + parent + " | child = " + child +
                    " | alpha = " + alpha + " | k = " + k);
        }
        System.out.println("\nТаблица vertices\n-----------------------------------------");

        results = stat.executeQuery("SELECT * FROM vertices");
        while(results.next())
        {
            int id = results.getInt("id");
            String parents = results.getString("parents");
            String children = results.getString("children");
            int w = results.getInt("weight");
            System.out.println( "ID = " + id + " | parents = " + parents + " | children = " + children +
                    " | weight = " + w);
        }
    }

    // --------Закрытие--------
    public void CloseDB()
    {
        try {
            conn.close();
            stat.close();
            results.close();
            System.out.println("Соединения закрыты");
        }
        catch (Exception e){
            System.out.println("Ошибка закрытия соединения: "+e);
        }
    }

    public String returnValue (String a){
        if (a.length() == 2)
            return "";
        else
            return a.substring(1, a.length()-1);
    }

    public int[] getIntArray(String[] str){
        int[] a = new int[str.length];
        if (str.length != 0){
            for (int i = 0; i < str.length; i++) {
                a[i] = Integer.parseInt(str[i]);
            }
            return a;
        }
        else {
            return a;
        }
    }

    public Vector<Vertex> getVertices() {
        try{
            Vector<Vertex> v = new Vector<Vertex>();
            results = stat.executeQuery("SELECT * FROM vertices");
            marked.clear();
            while (results.next()) {
                names.add(results.getString("name"));
                String p = results.getString("parents");
                String[] parents = p != null && !p.equals("") ? p.split(", ") : new String[0];
                String c = results.getString("children");
                String[] children = c != null && !c.equals("") ? c.split(", ") : new String[0];
                double weight;
                weight = results.getDouble("weight");
                if (!gradient) {
                    if ((results.getInt("weight") != 0) && getIntArray(parents).length == 0) {
                        marked.add(results.getInt("id"));
                    } else {
                        weight = -100000;
                    }
                }
                v.add(new Vertex(getIntArray(parents), getIntArray(children), weight));
            }
            return v;
        }
        catch (Exception e){
            System.out.println("getVertices: " + e.toString());
            return null;
        }
    }

    public Vector<String> getVertexNames(){
        try{
            Vector<String> v = new Vector<String>();
            results = stat.executeQuery("SELECT * FROM vertices");
            while (results.next()) {
                v.add(results.getString("name") != null ? results.getString("name") : new String());
            }
            return v;
        }
        catch (Exception e){
            System.out.println("getVertexNames: " + e.toString());
            return null;
        }
    }

    public Vector<Double> getVertexWeights(String table) {
        try{
            Vector<Double> v = new Vector<Double>();
            results = stat.executeQuery("SELECT * FROM " + table);
            while (results.next()) {
                v.add(results.getString("weight") != null ? results.getDouble("weight") : Double.NaN);
            }
            return v;
        }
        catch (Exception e){
            System.out.println("getVertexWeights: " + e.toString());
            return null;
        }
    }

    public Vector<Integer[]> getVertexParents() {
        try{
            Vector<Integer[]> v = new Vector<Integer[]>();
            results = stat.executeQuery("SELECT * FROM vertices");
            while (results.next()) {
                String p = results.getString("parents");
                String[] parents = p != null && !p.equals("")? p.split(", ") : null;
                Integer[] parentsInt;
                if (parents != null) {
                    parentsInt = new Integer[parents.length];
                    for (int i = 0; i < parentsInt.length; i++) {
                        parentsInt[i] = Integer.parseInt(parents[i]);
                    }
                }
                else {
                   parentsInt = new Integer[0];
                }
                v.add(parentsInt);
            }
            return v;
        }
        catch (Exception e){
            System.out.println("getVertexParents: " + e.toString());
            return null;
        }
    }

    public Vector<Integer[]> getVertexChildren() {
        try{
            Vector<Integer[]> v = new Vector<Integer[]>();
            results = stat.executeQuery("SELECT * FROM vertices");
            while (results.next()) {
                String c = results.getString("children");
                String[] children = c != null && !c.equals("") ? c.split(", ") : null;
                Integer[] childrenInt;
                if (children != null) {
                    childrenInt = new Integer[children.length];
                    for (int i = 0; i < childrenInt.length; i++) {
                        childrenInt[i] = Integer.parseInt(children[i]);
                    }
                }
                else {
                    childrenInt = new Integer[0];
                }
                v.add(childrenInt);
            }
            return v;
        }
        catch (Exception e){
            System.out.println("getVertexChildren: " + e.toString());
            return null;
        }
    }

    public Vector<Edge> getEdges() {
        try{
            Vector<Edge> v = new Vector<Edge>();
            results = stat.executeQuery("SELECT * FROM parameters");
            while (results.next()) {
                v.add(new Edge(results.getInt("parent_id"), results.getInt("child_id"),
                        results.getDouble("alpha"), results.getDouble("k")));
            }
            return v;
        }
        catch (Exception e){
            System.out.println("getEdges: " + e.toString());
            return null;
        }
    }

    public void assortValues() {
        try {
            stat.execute("DROP TABLE IF EXISTS network");
        }
        catch (Exception e){
            System.out.println("Ошибка при сбросе таблицы network! " + e);
            return;
        }
        gradient = false;
        Vector<Vertex> vertices = getVertices();        // двух динамических массивов (чтобы не обращаться к БД)
        int n = vertices.size();
        System.out.println("Количество вершин сети: "+n);
        System.out.println("Изначально вес задан у вершин: "+marked);
        Vector<Vertex> ideal = createIdeal(vertices);
        System.out.println("Идеальная:");
        for (int i = 0; i < ideal.size(); i++) {
            System.out.println("Имя вершины: " + names.get(i));
            ideal.get(i).print();
        }
        try {
            WriteDB(ideal, "network");      // запись результатов в таблице network
            System.out.println("Результат записан в таблицу network");
        } catch (SQLException e) {
            System.out.println("Не получилось записать результат в БД: "+e);
        }

    }

    public void assortValuesGradient(int[] ids, double[] weights) {
        gradient = true;
        countVertices(ids, weights);
    }

    void countVertices(int[] id, double[] w){
        try {
            stat.execute("DROP TABLE IF EXISTS network");
        }
        catch (Exception e){
            System.out.println("Ошибка при сбросе таблицы network!");
            System.exit(0);
        }
        Vector<Vertex> newVertices = getVertices();
        marked.clear();
        for (int i = 0; i < newVertices.size(); i++) {
            newVertices.get(i).w = -100000.0;
        }
        for (int i = 0; i < id.length; i++) {
            newVertices.get(id[i]-1).w = w[i];
            marked.add(id[i]);
        }
        System.out.println("Начальная:");
        for (int i = 0; i < newVertices.size(); i++) {
            System.out.print((i + 1) + ": ");
            newVertices.get(i).print();
        }
        while (marked.size() < newVertices.size()){
            boolean exitFor = false;
            boolean exitWhile = true;
            for (int i = 0; i < newVertices.size(); i++) {
                if (newVertices.get(i).w != -100000.0 && newVertices.get(i).p.length != 0){
                    double tempW = newVertices.get(i).w;
                    Vector<arc> va = new Vector<arc>();
                    Vector<Integer> ids = new Vector<Integer>();
                    int count = 0;
                    for (int j = 0; j < newVertices.get(i).p.length; j++) {
                        int par = newVertices.get(i).p[j];
                        try {
                            results = stat.executeQuery("SELECT alpha, k FROM parameters WHERE id = " + par);
                            if (marked.contains(par)) {
                                tempW -= results.getDouble("alpha") * Math.pow(newVertices.get(par - 1).w, results.getInt("k"));
                                count++;
                            }
                            else{
                                va.add(new arc(results.getDouble("alpha"), results.getInt("k")));
                                ids.add(par);
                            }
                        } catch (SQLException e) {
                            System.out.println("countVertices: "+e);
                        }
                    }
                    if (count != newVertices.get(i).p.length) {
                        Finder f = new Finder(va, tempW);
                        System.out.println("-----------------Расчеты для "+(i+1)+" вершины (w="+newVertices.get(i).w+")---------------");
                        Vector<Double> res = f.handler();
                        exitWhile = false;
                        for (int j = 0; j < ids.size(); j++) {
                            marked.add(ids.get(j));
                            newVertices.get(ids.get(j) - 1).w = res.get(j);
                            exitFor = true;
                        }
                    }
                }
                if (exitFor)
                    break;
            }
            if (exitWhile)
                break;
        }
        Vector<Vertex> answer = createIdeal(newVertices);
        System.out.println("Рассчитанная:");
        for (int i = 0; i < answer.size(); i++) {
            System.out.print((i+1) + ": ");
            answer.get(i).print();
        }
        try {
            WriteDB(answer, "network");
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public Vector<Vertex> createIdeal(Vector<Vertex> vertices){
        while (marked.size() < vertices.size()){  // пока количество вершин с весами меньше количества всех вершин
            for (int i = 0; i < vertices.size(); i++) { // идем по всем вершинам
                Vertex child = vertices.get(i);         // для удобства переприсваиваем
                if (child.w == 0 || child.w == -100000){      // если вес вершины не задан
                    int h = 0;
                    String count = "0";
                    String parents = "";
                    for (int j = 0; j < child.p.length; j++) {  // пробегаемся по списку вершин-родителей
                        Vertex parent = vertices.get(child.p[j]-1);   // для удобства (child.p[j]-1, т.к. индексы вектора с нуля)
                        if (marked.contains(child.p[j])) {    // если номер родителя есть в списке номеров вершин с весами
                            try {
                                results = stat.executeQuery("SELECT * FROM parameters WHERE parent_id = " + child.p[j] +
                                        " AND child_id = "+(i+1));
                                double a = results.getDouble("alpha");        // берем альфа и ка
                                int k = results.getInt("k");
                                h += a*Math.pow(parent.w, k);   // постепенно считаем вес
                                count += " + "+a+"*("+parent.w+"^"+k+")";
                                parents += " "+child.p[j];
                            } catch (SQLException e) {      // если не получилось прочитать из базы
                                System.out.println("Ошибка при чтении из базы: "+e);
                                CloseDB();
                                System.exit(0);
                            }
                        }
                        else {
                            h = 0;
                            break;      // если номера нет, то не считаем вес этой вершины (т.е. child-а), идем дальше
                        }
                    }
                    if (h!=0){   // все родители проверены и вес отличается от 0 (т.е. у вершин не м.б. вес = 0)
                        //System.out.println("Расчитан вес вершины "+(i+1)+": H="+h+" как "+count+" (parents ="+parents+")");
                        vertices.get(i).w = h;      // задаем вес вершине
                        marked.add(i+1);            // добавляем номер вершины в массив номеров вершин с весами
                    }
                }
            }
        }
        return vertices;
    }


}