package me.anno.utils.test.schur;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SchurComplement {

    public static void main(String[] args) {
        new SchurComplement().main();
    }

    void main() {

        n = 41;
        h = 1.0 / (n + 1);

        if (n % 2 == 0) throw new RuntimeException("[Schur] n muss ungerade sein!");

        int kx = 2, ky = kx;
        double[] f = allocate(n * n);

        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                double x = (i + 1) * h;
                double y = (j + 1) * h;
                f[i + j * n] = Math.sin(Math.PI * x * kx) * Math.sin(Math.PI * y * ky);
            }
        }

        showMatrix(f, n, n, "field.bmp");

        System.out.println("Start");

        int k = n * (n / 2);
        int l = k + n;

        // erst m, dann n,
        // also erst y, dann x
        TeilMatrix A_11 = new TeilMatrix(0, 0, k, k);
        TeilMatrix A_22 = new TeilMatrix(l, l, k, k);
        TeilMatrix A_1B = new TeilMatrix(0, k, k, n);
        // TeilMatrix A_2B = new TeilMatrix(k, l, n, k);// mistake in the homework description -.-
        TeilMatrix A_2B = new TeilMatrix(l, k, k, n);
        TeilMatrix A_BB = new TeilMatrix(k, k, n, n);

        // Grenze x Grenze
        SchurMatrix schurMatrix = new SchurMatrix(n, n);

        schurMatrix.A_1B = A_1B;
        schurMatrix.A_2B = A_2B;
        schurMatrix.A_BB = A_BB;
        schurMatrix.A_11 = A_11;
        schurMatrix.A_22 = A_22;

        int borderOffset = n * (n / 2);
        double[] f1 = copyWithOffset(f, 0, borderOffset);
        double[] fb = copyWithOffset(f, borderOffset, n);
        double[] f2 = copyWithOffset(f, borderOffset + n, borderOffset);

        double[] t1 = calculate_fs_term(A_1B, A_11, f1); // transpose(A_iB) * inv(A_i) * t1
        double[] t2 = calculate_fs_term(A_2B, A_22, f2);
        showMatrix(t1, 1, n, "fs-t1.bmp");
        showMatrix(t2, 1, n, "fs-t2.bmp");

        System.out.println("[Schur] 1. Schritt: f_s berechnen");

        double[] f_s = copy(fb, n);
        sub(f_s, t1, n);
        sub(f_s, t2, n);
        showMatrix(f_s, 1, n, "fs.bmp");


        System.out.println("[Schur] 2. Schritt: u_B mit Schurkomplement S berechen");

        double[] u_B = solve(schurMatrix, f_s);
        showMatrix(u_B, 1, n, "u_B.bmp");


        System.out.println("[Schur] 3. Schritt: u_i berechnen");

        double[] u1 = calculateUi(f1, A_11, A_1B, u_B);
        showMatrix(u1, n / 2, n, "u_1.bmp");

        double[] u2 = calculateUi(f2, A_22, A_2B, u_B);
        showMatrix(u2, n / 2, n, "u_2.bmp");

        System.out.println("[Schur] Finished calculation");

        double[] u = allocate(n * n);

        System.arraycopy(u1, 0, u, 0, n * (n / 2));
        System.arraycopy(u2, 0, u, borderOffset + n, n * (n / 2));
        System.arraycopy(u_B, 0, u, borderOffset, n);

        showMatrix(u, n, n, "solution.bmp");

        TeilMatrix ganzeMatrix = new TeilMatrix(0, 0, n * n, n * n);
        double error = getRSS(f, ganzeMatrix, u, n * n);

        System.out.println("[Schur] Finished with error " + error);

    }

    abstract static class Matrix {

        // A * b = c
        int bLength;// Matrix-Dimension n
        int cLength;// Matrix-Dimension m

        abstract double[] matrixMultiply(double[] v);

        abstract double[] matrixMultiply(double[] v, double[] dst);

    }

    static int n;
    static double h;

    class TeilMatrix extends Matrix {

        // Originale Benennung: int N, kc, nc, kb, nb;
        int cStart, bStart;

        // m sizes, then n sizes
        TeilMatrix(int cStart, int bStart, int cLength, int bLength) {
            this.cStart = cStart;
            this.bStart = bStart;
            this.cLength = cLength;
            this.bLength = bLength;
        }

        ;

        void transpose() {
            // A ist symmetrisch, deshalb müssen wir hier nur die Grenzen und Größen vertauschen und der Rest geht automatisch
            int tmp = cStart;
            cStart = bStart;
            bStart = tmp;
            tmp = cLength;
            cLength = bLength;
            bLength = tmp;
        }

        boolean canAccess(int j) {
            return j >= bStart && j < bStart + bLength;
        }

        // A: cStart until cStart+cLength, bStart until bStart+bLength
        // b: 0 until bLength
        // c: 0 until cLength
        @Override
        double[] matrixMultiply(double[] b, double[] c) {

            if (b.length != bLength || c.length != cLength)
                throw new RuntimeException("sizes don't match! " + c.length + " x " + b.length + " vs " + cLength + " x " + bLength);

            double invHSquared = 1.0 / (h * h);

            // Zeiger auf b
            int lx = cStart - bStart;
            for (int ly = 0; ly < cLength; ly++, lx++) {

                double sum = 0;
                int i = ly + cStart;

                // definiere hier implizit den Fünf-Punkt-Stern,
                // allerdings auf den Teilbereich beschnitten

                // um zu testen, ob es auf dem Feld einen Nachbarn gibt
                int x = i % n;
                int y = i / n;

                if (canAccess(i)) sum += 4 * b[lx];
                if (x > 0 && canAccess(i - 1)) sum -= b[lx - 1];
                if (x < n - 1 && canAccess(i + 1)) sum -= b[lx + 1];
                if (y > 0 && canAccess(i - n)) sum -= b[lx - n];
                if (y < n - 1 && canAccess(i + n)) sum -= b[lx + n];

                c[ly] = sum * invHSquared;

            }

            return c;

        }

        @Override
        double[] matrixMultiply(double[] b) {
            double[] c = allocate(cLength);
            matrixMultiply(b, c);
            return c;
        }

    }

    void free(double[] x) {
    }

    double[] allocate(int size) {
        return new double[size];
    }

    class SchurMatrix extends Matrix {

        TeilMatrix A_BB;
        TeilMatrix A_1B;
        TeilMatrix A_11;
        TeilMatrix A_2B;
        TeilMatrix A_22;

        SchurMatrix(int cLength, int bLength) {
            this.cLength = cLength;
            this.bLength = bLength;
        }

        double[] partialMultiply(TeilMatrix A_iB, TeilMatrix A_ii, double[] vector) {
            checkNaN(vector, bLength);
            // berechne t(A_iB) * inv(A_i) * A_iB * vector
            double[] t0 = A_iB.matrixMultiply(vector);
            double[] t1 = solve(A_ii, t0);
            free(t0);
            A_iB.transpose();
            double[] t2 = A_iB.matrixMultiply(t1);
            free(t1);
            A_iB.transpose();// restore it
            return t2;
        }

        @Override
        double[] matrixMultiply(double[] vector, double[] dst) {
            checkNaN(vector, bLength);
            // berechne A_BB*vector - sum(t(A_iB) * inv(A_i) * A_iB * vector)
            double[] p0 = A_BB.matrixMultiply(vector, dst);
            double[] p1 = partialMultiply(A_1B, A_11, vector);
            double[] p2 = partialMultiply(A_2B, A_22, vector);
            // vector ist n lang und A_BB ist eine n x n Matrix, also sind die Vektoren, die sich ergeben,
            // auch n lang
            sub(p0, p1, n);
            sub(p0, p2, n);
            return p0;
        }

        @Override
        double[] matrixMultiply(double[] vector) {
            double[] dst = allocate(A_BB.cLength);
            return matrixMultiply(vector, dst);
        }

    }

    double sq(double x) {
        return x * x;
    }

    double calculateError(double[] f, double[] u, int n, double h) {
        double error = 0;
        // Rand vernachlässigt zur Vereinfachung, da wir keine Geisterzone mehr haben
        double invHSquared = 1.0 / (h * h);
        for (int j = 1; j < n - 1; j++) {
            for (int i = 1; i < n - 1; i++) {
                double star = invHSquared * (4 * u[i + j * n] - u[i + j * n - 1] - u[i + j * n + 1] - u[i + j * n + n] - u[i + j * n - n]);
                error += sq(star - f[i + j * n]);
            }
        }
        return error;
    }

    // inv(A_i) * (f_i - A_iB * u_B)
    double[] calculateUi(double[] f_i, TeilMatrix A_ii, TeilMatrix A_iB, double[] u_B) {
        // f_i - A_iB * u_B
        double[] A_iB_x_u_B = allocate(A_iB.cLength);
        A_iB.matrixMultiply(u_B, A_iB_x_u_B);
        // inv(A_i) * (f_i - A_iB * u_B)
        double[] ui0 = A_iB_x_u_B;// wir können die Werte einfach überschreiben
        for (int i = 0; i < A_iB.cLength; i++) {
            ui0[i] = f_i[i] - A_iB_x_u_B[i];
        }
        return solve(A_ii, ui0);
    }

    double[] calculate_fs_term(TeilMatrix A_iB, TeilMatrix A_ii, double[] f_i) {
        System.out.println("[Schur-fs] called calculate_fs_term");
        double[] tmp = solve(A_ii, f_i);
        System.out.println("[Schur-fs] solved");
        A_iB.transpose();
        System.out.println("[Schur-fs] transposed");
        double[] fs_term = A_iB.matrixMultiply(tmp);
        free(tmp);
        System.out.println("[Schur-fs] matrixMultiplied");
        A_iB.transpose();
        return fs_term;
    }

    double[] copyWithOffset(double[] src, int offset) {
        return copyWithOffset(src, offset, src.length - offset);
    }

    double[] copyWithOffset(double[] src, int offset, int length) {
        double[] dst = new double[length];
        System.arraycopy(src, offset, dst, 0, length);
        return dst;
    }

    void showMatrix(double[] v, int m, int n, String name) {
        double max = 1e-100;
        for (int i = 0; i < m * n; i++) {
            max = Math.max(v[i], max);
        }
        BufferedImage image = new BufferedImage(n, m, 1);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < m; i++) {
                double value = v[i * n + j] / max;
                boolean negative = value < 0;
                int grayScale = (int) (Math.abs(value) * 255);
                if (Double.isNaN(value)) {
                    image.setRGB(j, i, 0x0077ff);
                } else {
                    image.setRGB(j, i, negative ? 0x10000 * grayScale : 0x10101 * grayScale);
                }
            }
        }
        System.out.println("[Image] Max(" + name + "): " + max);
        try {
            ImageIO.write(image, "png", new File("C:/Users/Antonio/Desktop/" + name + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void checkNaN(double[] v, int n) {
        if (v.length != n) throw new RuntimeException("sizes don't match " + v.length + " vs " + n);
        for (int i = 0; i < n; i++) {
            double vi = v[i];
            if (Double.isNaN(vi)) {
                throw new RuntimeException("Found NaN");
            }
        }
    }

    void sub(double[] a, double[] b, int n) {
        checkNaN(a, n);
        checkNaN(b, n);
        for (int i = 0; i < n; i++) {
            a[i] -= b[i];
        }
        checkNaN(a, n);
    }

    double[] copy(double[] src, int n) {
        if (src.length != n) throw new RuntimeException("sizes don't match, " + src.length + " vs " + n);
        double[] copy = allocate(n);
        System.arraycopy(src, 0, copy, 0, n);
        return copy;
    }


    double[] allocateZero(int n) {
        return allocate(n);
    }

    double[] allocateCopy(int n, double[] base) {
        return copy(base, n);
    }

    /**
     * berechnet b - alpha * A * x
     * alpha: Skalar
     * A: Matrix n x m
     * b: Vektor m
     * x0: Vektor n
     * wird für die Berechnung des Residuums verwendet
     */
    double[] bMinusAlphaAx(double[] b, double alpha, Matrix A, double[] x0, int n, double[] dst) {

        if (Double.isNaN(alpha)) throw new RuntimeException("alpha must not be NaN");
        checkNaN(b, n);
        checkNaN(x0, n);

        // dst darf hier noch nicht geschrieben werden!!,
        // da es b oder x0 ist
        double[] Ax = A.matrixMultiply(x0);

        checkNaN(Ax, n);

        for (int i = 0; i < n; i++) {
            dst[i] = b[i] - alpha * Ax[i];
        }

        checkNaN(dst, n);
        free(Ax);

        return dst;
    }

    // b = b * alpha + a
    // return b
    double[] reverseMultiplyAdd(double[] a, double alpha, double[] b, int n) {
        if (a.length != n || b.length != n)
            throw new RuntimeException("sizes don't match " + a.length + " vs " + b.length + " vs " + n);
        for (int i = 0; i < n; i++) {
            b[i] = alpha * b[i] + a[i];
        }
        return b;

    }

    // a += alpha * b
    // return a
    void multiplyAdd(double[] a, double alpha, double[] b, int n) {
        if (Double.isNaN(alpha)) throw new RuntimeException("alpha must not be NaN");
        checkNaN(a, n);
        checkNaN(b, n);
        for (int i = 0; i < n; i++) {
            a[i] += alpha * b[i];
        }
        checkNaN(a, n);
    }

    // Skalarprodukt von einem Vektor mit sich selbst
    // entspricht der Länge²
    double innerProduct(double[] a, int n) {
        if (a.length != n) throw new RuntimeException("sizes don't match! " + a.length + " vs " + n);
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double ai = a[i];
            sum += ai * ai;
        }
        return sum;
    }

    // pT * A * p
    double pAp(double[] p, Matrix A, int n) {

        // checkNaN(p, n);

        double[] Ap = A.matrixMultiply(p);

        // checkNaN(Ap, n);

        double scalarProduct = 0;
        for (int i = 0; i < n; i++) {
            scalarProduct += p[i] * Ap[i];
        }

        if (Double.isNaN(scalarProduct)) {
            showMatrix(p, n, 1, "p.bmp");
            showMatrix(Ap, n, 1, "Ap.bmp");
            throw new RuntimeException("pAp is somehow NaN");
        }

        free(Ap);

        return scalarProduct;

    }

    boolean firstIterationStep(double[] field, double[] r0, double[] alpha0, double[] RSS0, Matrix A, double[][] p0, double[] x0, int n) {

        bMinusAlphaAx(field, 1.0, A, x0, n, r0);

        // Startrichtung
        p0[0] = allocateCopy(n, r0);
        RSS0[0] = innerProduct(r0, n);

        if (Math.abs(RSS0[0]) < 1e-32) {
            System.out.println("[CG] System is already solved, RSS: " + RSS0[0]);
            return true;
        }

        double pApValue = pAp(p0[0], A, n);
        // System.out.println( "[CG:0] pAp: " + pApValue + ", RSS: " + RSS0 );

        alpha0[0] = RSS0[0] / pApValue;// optimale Schrittweite


        // verbesserter Wert
        /*double[] x1 =*/
        multiplyAdd(x0, alpha0[0], p0[0], n);
        // printMatrix(x1, n, n);

        return false;

    }

    // geht einen Iterationsschritt
    boolean iterationStep(boolean forceIterations, double[] r0, double[] alpha0, double[] RSS0, Matrix A, double[] p0, double[] x1, int n) {

        // calculate the residuum
        double[] r1 = bMinusAlphaAx(r0, alpha0[0], A, p0, n, r0);// r1 = r0 + a0 * A * p0
        double RSS1 = innerProduct(r1, n);// residual square sum
        if (RSS1 < 1e-32) {
            System.out.println("[CG] Residuum close to zero: " + RSS1);
            if (!forceIterations) return true; // unser Fehler ist kleiner als 1e-16
        }

        // how much has changed from the last step to this step
        double beta0 = RSS1 / RSS0[0];
        // System.out.println( "b: " + b0 );
        if (Math.abs(beta0) < 1e-16) {
            System.out.println("[CG] Residuum much smaller than previously: " + beta0 + ", from " + RSS0[0] + " to " + RSS1);
            RSS0[0] = RSS1;
            if (!forceIterations)
                return true;// fertig, denn unser Residuum ist 100.000.000x besser als das vorherige Residuum
        }

        // update den alten Wert für die nächsten Iterationen
        RSS0[0] = RSS1;

        double[] p1 = reverseMultiplyAdd(r1, beta0, p0, n);// p1 = r1 + b0 * p0; finde die optimale Richtung
        double pApValue = pAp(p1, A, n);

        // System.out.println( "[CG:i] pAp: " + pApValue + ", RSS1: " + RSS1 );

        double alpha1 = alpha0[0] = RSS1 / pApValue;// finde die optimale Entfernung
        // System.out.println( "a: " + a1 );
        /*double[] x2 =*/
        multiplyAdd(x1, alpha1, p1, n);// x2 = x1 + a1 * p1

        // printMatrix(x2, n, n);

        return false;

    }

    double getRSS(double[] field, Matrix A, double[] solution, int n) {
        double[] error = allocate(n);
        System.out.println("sizes: " + field.length + ", " + solution.length + ", " + A.cLength + " x " + A.bLength);
        bMinusAlphaAx(field, 1.0, A, solution, n, error);
        showMatrix(error, n, 1, "error.bmp");
        double rss = innerProduct(error, n);
        free(error);
        return rss;
    }

    // A * b = c (field)
    double[] solve(Matrix A, double[] field) {

        int n = A.cLength;

        System.out.println("gonna solve " + A.cLength + " x " + A.bLength + ", control: " + field.length);

        checkNaN(field, n);

        // showMatrix(field, n, 1, "field.bmp");

        int maxNumberOfSteps = 8;
        boolean forceIterations = false;

        double[] x0 = allocate(n);
        double[] r0 = allocate(n);

        ////////////////////////////////////////////
        // erster Schritt

        System.out.println("[CG] 1. Schritt, " + A.cLength + " x " + A.bLength + ", control: " + field.length);

        double[] alpha0 = new double[1], RSS0 = new double[1];
        double[][] p0 = new double[1][];

        boolean isDone = firstIterationStep(field, r0, alpha0, RSS0, A, p0, x0, n);

        /////////////////////////////////////////////
        // weitere Schritte

        int ctr = 1;
        for (; !isDone && ctr < maxNumberOfSteps; ctr++) {
            // System.out.println( ctr );
            isDone = iterationStep(forceIterations, r0, alpha0, RSS0, A, p0[0], x0, n);
            // System.out.println( "RSS: " + RSS0 ); 
        }

        double rss = getRSS(field, A, x0, n);
        System.out.println("[GC] RSS: " + rss + ", durch " + ctr + " Schritte");

        return x0;

    }


}
