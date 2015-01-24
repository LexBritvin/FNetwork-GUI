package FunctionalNetwork;

import java.util.Date;
import java.util.Random;
import java.util.Vector;

/**
 * Created by Denis on 27.12.2014.
 */
public class Finder {
    public int n, interval, iter_limit;
    public double h, eps;
    Vector<Vector<Double>> answers = new Vector<Vector<Double>>();
    Vector<arc> a;								// вектор входящих в вершину ребер (содержит альфа и к каждого входящего ребра)
    double weight;					            // вес задаваемой вершины

    Finder(Vector<arc> a, double w){
        interval = 5;
        iter_limit = 20;								// количество итераций
        h = interval*0.3;								// шаг приближения
        eps = 0.001;									// эпсилон - точность
        this.a = a;
        this.weight = w;									// вес вершины, в которую входят дуги (далее - собирающая_вершина)
        n = a.size();
    }

    // поиск решения:
    public Vector<Double> handler() {
        int iteration = 0;					// iter_limit - количество попыток решения с подобранным начальным приближением
        double result = 0, min = 0;
        Vector<Double> x = new Vector<Double>(a.size());    // его занулить!!
        Vector<Double> deltas = new Vector<Double>();							// вектор невязок
        Vector<Vector<Double>> x0s = new Vector<Vector<Double>>();		// вектор начальных приближений
        for (int i = 0; i < a.size(); i++) {
            x.add(0.0);         // зануляем вектор (это обязательно!)
        }
        while (iteration < iter_limit)
        {
            Random rand = new Random(new Date().getTime());
            System.out.print("\nПопытка № " + iteration + ". Начальное приближение: (");
            for (int i=0; i<a.size(); i++)
            {
                try{
                    x.set(i, (double)(rand.nextInt(interval+1)));        // псевдослуч. число от 1 до interval
                }
                catch (Exception e){
                    System.out.println("R "+interval);
                    System.out.println(e);
                    System.exit(0);
                }
                System.out.print(x.get(i) + " ");
            }
            System.out.println(")");
            x0s.add(x);								// сохраняем пробное начальное приближение и пытаемся решить
            result = solve(x);
            deltas.add(result);						// после решения сохраняем значение невязки для выбора наименьшей
            if (result != 0){
                for (int i=0; i<a.size(); i++){
                    if (answers.get(answers.size() - 1).get(i) < 0.0 || answers.get(answers.size() - 1).get(i) < eps/100){	// если какая либо координата близка к нулю либо отрицательна, сужаем интервал.
                        interval *= 0.7;						// уменьшаем интервал на 30 %
                        h = interval*0.3;						// шаг приближения
                    }
                }
            }
            else {
                iteration = iter_limit;
            }
            iteration++;									// увеличиваем счетчик итераций (попытки)
        }

        if (result != 0){
            min = deltas.get(0);
            for (int i=1; i<deltas.size(); i++) {
                if (min > deltas.get(i)) {
                    min = deltas.get(i);
                    x = answers.get(i);
                }
            }
            System.out.println("===Попытка уточнения ответа===");
            System.out.print("Округление до целых результирующего вектора: \n( ");
            Vector <Double> newX = new Vector<Double>();
            for (int i=0; i<n; i++) {
                newX.add((double) Math.round(x.get(i)));
                System.out.print(newX.get(i) + " ");
            }
            System.out.print(")");
            if (solve(newX) < min) {
                x = answers.get(answers.size() - 1);
                System.out.print("\nУспех!\nУточненный ответ: (");
                for (int i=0; i<n; i++) {
                    System.out.print(x.get(i) + " ");
                }
                System.out.print(")\n");
            }
            else
                System.out.println();
                System.out.print("\nКорректировка не уточнила ответ...");
        }
        else {
            x = answers.get(answers.size() - 1);
        }
        Vector<Double> ret = new Vector<Double>();  // возвращает округленные результаты
        System.out.print("\n\n==========\nОтвет: (");
        for (int i=0; i<n; i++) {
            ret.add((double) Math.round(x.get(i)));
            //System.out.print(ret.get(i) + " ");
            System.out.print(x.get(i) + " ");   // в этой части вектор х является ответом
        }
        System.out.print("), при f(x)=");
        System.out.println(f(x));
        return x;
        //return ret;
    }

    // поиск точки:
    public double solve(Vector<Double> x) {
        boolean run = true;									// переменная запуска и останова цикла while
        double r, norma, FX, FZ;
        Vector<Double> grad = new Vector<Double>(a.size());
        Vector<Double> v = new Vector<Double>(a.size());    // если чё, занулить
        Vector<Double> z = new Vector<Double>(a.size());
        FX = f(x);								// находим значение FX = f(x), x - начальное приближение

        while (run) {
            grad = gradient(x);					// вектор градиента в точке х
            norma = 1/norm(grad);							// 1 / || grad(x) ||

            for (int i=0; i<n; i++) {
                v.add(0.0); z.add(0.0);
                v.set(i, norma*grad.get(i));
                z.set(i, x.get(i) - h* v.get(i));						// делаем шаг в направление антиградиента
            }

            FZ = f(z);							// проверяем условие останова
            if (FZ < FX) {
                x = z;
                r = Math.abs(FZ) - Math.abs(FX);
                if (Math.abs(r) < eps/10){					// условие 1 - расстояние между точками стремится к нулю
                    run = false;							// завершаем цикл
                    System.out.println("Выход по базовому условию: |(|FX|-|FZ|)| < eps/100, eps/100 = " + eps/100);
                }
                FX = FZ;
            }
            else {
                if (h < eps/4) {								// условие 2 - если шаг стал очень маленьким
                    run = false;							// завершаем цикл
                    System.out.println("Выход по условию: шаг достиг минимума: h < eps/4, eps/4 = " + eps/4 );
                }
                else
                    h = h/3;								// иначе - уменьшаем h втрое - по условию алгоритма
            }
        }
        answers.add(x);
        System.out.print("Найденные точки при заданном приближении: (");
        for (int i=0; i<n; i++) {
            System.out.print(x.get(i)+" ");
        }
        System.out.println(")");
        double answ = f(x);
        System.out.println("Значение F(x) = " + answ);
        return answ;
    }

    // вычисление значения функции в точке х:
    double f(Vector<Double> x){
        double sum = 0;
        for (int i=0; i<a.size(); i++)
        {												                // F(x) - заданный вес вершины, в которую входят ребра
            sum += a.get(i).alpha*Math.pow(x.get(i), a.get(i).k);		// F(x) = alpha1*w1^k1 + ... + alphaN*wN^kN
        }												                // модуль необходим для поиска положительных значений узлов (ограничение)
        sum = Math.abs(sum - weight);						            // |alpha1*w1^k1 + ... + alphaN*wN^kN - F(x)| => 0
        return sum;
    }

    public Vector<Double> gradient(Vector<Double> x){
        Vector <Double> g = new Vector<Double>(a.size());
        for (int i = 0; i < a.size(); i++) {
            g.add(0.0);
        }
        int sign = 1;									// sign нужен для поиска градиента
        if (f(x) < 0)
            sign = -1;

        for (int i=0; i<a.size(); i++)
        {												// градиент в точке xi = alpha_i * k_i * x_i^(k-1) - производная по i-му элементу
            g.set(i, a.get(i).alpha * a.get(i).k * Math.pow(x.get(i), a.get(i).k - 1) * sign);
        }
        return g;
    }

    // вычисление евклидовой нормы
    double norm(Vector<Double> x) {
        double temp = 0;
        for (int i=0; i<x.size(); i++) {
            temp += Math.pow(x.get(i), 2);
        }
        return Math.sqrt(temp);
    }
}